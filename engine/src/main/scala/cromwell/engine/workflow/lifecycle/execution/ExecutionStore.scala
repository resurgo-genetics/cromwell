package cromwell.engine.workflow.lifecycle.execution

import cromwell.backend.BackendJobDescriptorKey
import cromwell.core.ExecutionStatus._
import cromwell.core.{CallKey, JobKey}
import cromwell.engine.workflow.lifecycle.execution.ExecutionStore.{FqnIndex, RunnableScopes}
import cromwell.engine.workflow.lifecycle.execution.WorkflowExecutionActor.{apply => _, _}
import wdl._
import wom.callable.WorkflowDefinition
import wom.executable.Executable.ResolvedExecutableInputs
import wom.graph._


object ExecutionStore {
  case class RunnableScopes(scopes: List[JobKey], truncated: Boolean)
  
  private type FqnIndex = (String, Option[Int])

  def empty = ExecutionStore(Map.empty[JobKey, ExecutionStatus], hasNewRunnables = false)

  def apply(workflow: WorkflowDefinition, workflowCoercedInputs: ResolvedExecutableInputs) = {
    // Only add direct children to the store, the rest is dynamically created when necessary
//    val keys = workflow.children map {
//      case call: WdlTaskCall => Option(BackendJobDescriptorKey(call, None, 1))
//      case call: WdlWorkflowCall => Option(SubWorkflowKey(call, None, 1))
//      case scatter: Scatter => Option(ScatterKey(scatter))
//      case conditional: If => Option(ConditionalKey(conditional, None))
//      case declaration: Declaration => Option(DeclarationKey(declaration, None, workflowCoercedInputs))
//      case _ => None
//    }
    
    val keys = workflow.innerGraph.nodes collect {
      case call: TaskCallNode => Option(BackendJobDescriptorKey(call, None, 1))
      case declaration: ExpressionNode => Option(ExpressionKey(declaration, None))
        // Note that PortBasedGraphOutputNodes don't need to be added in the store.
        // They simply act as a proxy for another output port in the graph.
        // When we reach the end of the workflow, we'll simply look for the source output port in the output store
      case expressionOutputNode: ExpressionBasedGraphOutputNode => Option(ExpressionKey(expressionOutputNode))
    }
    
    // There are potentially resolved workflow inputs that are default WomExpressions.
    // For now assume that those are call inputs that will be evaluated in the CallPreparation.
    // If they are actually workflow declarations then we would need to add them to the ExecutionStore so they can be evaluated.
    // In that case we would want InstantiatedExpressions so we can create an InstantiatedExpressionNode and add a DeclarationKey
    
    new ExecutionStore(keys.flatten.map(_ -> NotStarted).toMap, keys.nonEmpty)
  }
  
  val MaxJobsToStartPerTick = 1000
}

final case class ExecutionStore(private val statusStore: Map[JobKey, ExecutionStatus], hasNewRunnables: Boolean) {

  // View of the statusStore more suited for lookup based on status
  lazy val store: Map[ExecutionStatus, List[JobKey]] = statusStore.groupBy(_._2).mapValues(_.keys.toList)
  // Takes only keys that are done, and creates a map such that they're indexed by fqn and index
  // This allows for quicker lookup (by hash) instead of traversing the whole list and yields
  // significant improvements at large scale (run ExecutionStoreBenchmark)
  lazy val (doneKeys, terminalKeys) = {
    def toMapEntry(key: JobKey) = (key.node.fullyQualifiedName, key.index) -> key

    store.foldLeft((Map.empty[FqnIndex, JobKey], Map.empty[FqnIndex, JobKey]))({
      case ((done, terminal), (status, keys))  =>
        lazy val newMapEntries = keys map toMapEntry
        val newDone = if (status.isDoneOrBypassed) done ++ newMapEntries else done
        val newTerminal = if (status.isTerminal) terminal ++ newMapEntries else terminal

        newDone -> newTerminal
    })
  }

  private def keysWithStatus(status: ExecutionStatus) = store.getOrElse(status, List.empty)

  def isBypassedConditional(jobKey: JobKey, conditional: If): Boolean = {
    keysWithStatus(Bypassed).exists {
      case key: ConditionalKey =>
        key.node.fullyQualifiedName.equals(conditional.fullyQualifiedName) &&
          key.index.equals(jobKey.index)
      case _ => false
    }
  }

  def hasActiveJob: Boolean = {
    def upstreamFailed(scope: GraphNode): Boolean = scope match {
      case node: GraphNode => node.upstreamAncestry exists hasFailedScope
    }

    keysWithStatus(QueuedInCromwell).nonEmpty ||
      keysWithStatus(Starting).nonEmpty ||
      keysWithStatus(Running).nonEmpty ||
      keysWithStatus(NotStarted).exists(jobKey => !upstreamFailed(jobKey.node))
  }

  def jobStatus(jobKey: JobKey): Option[ExecutionStatus] = statusStore.get(jobKey)

  def startedJobs: List[BackendJobDescriptorKey] = {
    store.filterNot({ case (s, _) => s == NotStarted}).values.toList.flatten collect {
      case k: BackendJobDescriptorKey => k
    }
  }

  private def hasFailedScope(s: GraphNode): Boolean = keysWithStatus(Failed).exists(_.node == s)

  def hasFailedJob: Boolean = keysWithStatus(Failed).nonEmpty

  override def toString = store.map { case (j, s) => s"$j -> $s" } mkString System.lineSeparator()

  def add(values: Map[JobKey, ExecutionStatus]) = {
    this.copy(statusStore = statusStore ++ values, hasNewRunnables = hasNewRunnables || values.values.exists(_.isTerminalOrRetryable))
  }

  /**
    * Returns the list of jobs ready to be run, along with a Boolean indicating whether or not the list has been truncated.
    * The size of the list will be MaxJobsToStartPerTick at most. If more jobs where found runnable, the boolean will be true, otherwise false.
    */
  def runnableScopes: RunnableScopes = {
    val readyToStart = keysWithStatus(NotStarted).toStream filter arePrerequisitesDone
    // Compute the first ExecutionStore.MaxJobsToStartPerTick + 1 runnable scopes
    val scopesToStartPlusOne = readyToStart.take(ExecutionStore.MaxJobsToStartPerTick + 1).toList
    // Only take the first ExecutionStore.MaxJobsToStartPerTick from the above list.
    // Use the fact that we took one more to determine whether or not we truncated the result.
    RunnableScopes(scopesToStartPlusOne.take(ExecutionStore.MaxJobsToStartPerTick), scopesToStartPlusOne.size > ExecutionStore.MaxJobsToStartPerTick)
  }

  def findCompletedShardsForOutput(key: CollectorKey): List[JobKey] = doneKeys.values.toList collect {
    case k @ (_: CallKey | _:IntermediateValueKey) if k.node == key.node && k.isShard => k
  }

  private def emulateShardEntries(key: CollectorKey): Set[FqnIndex] = {
    (0 until key.scatterWidth).toSet map { i: Int => key.node match {
      case c: CallNode => c.fullyQualifiedName -> Option(i)
      case d: ExpressionNode => d.fullyQualifiedName -> Option(i)
      case _ => throw new RuntimeException("Don't collect that.")
    }}
  }

  private def arePrerequisitesDone(key: JobKey): Boolean = {
    val upstreamAreDone = key.node.upstream forall {
      case n @ (_: TaskCallNode | _: ScatterNode | _: ExpressionNode) => upstreamIsDone(key, n)
      case _ => true
    }

    val shardEntriesForCollectorAreTerminal: Boolean = key match {
      case collector: CollectorKey => emulateShardEntries(collector).diff(terminalKeys.keys.toSet).isEmpty
      case _ => true
    }

    shardEntriesForCollectorAreTerminal && upstreamAreDone
  }

  private def upstreamIsDone(entry: JobKey, prerequisiteScope: GraphNode): Boolean = {
//    prerequisiteScope.closestCommonAncestor(entry.scope) match {
//      /*
//        * If this entry refers to a Scope which has a common ancestor with prerequisiteScope
//        * and that common ancestor is a Scatter block, then find the shard with the same index
//        * as 'entry'.  In other words, if you're in the same scatter block as your pre-requisite
//        * scope, then depend on the shard (with same index).
//        *
//        * NOTE: this algorithm was designed for ONE-LEVEL of scattering and probably does not
//        * work as-is for nested scatter blocks
//        */
//      case Some(_: Scatter) => doneKeys.contains(prerequisiteScope.fullyQualifiedName -> entry.index)
//
//      /*
//        * Otherwise, simply refer to the collector entry.  This means that 'entry' depends
//        * on every shard of the pre-requisite scope to finish.
//        */
//      case _ => doneKeys.contains(prerequisiteScope.fullyQualifiedName -> None)
//    }
    // TODO WOM: fix scatter
    doneKeys.contains(prerequisiteScope.fullyQualifiedName -> None)
  }
}
