package com.example.sysarch.coevolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.diffmerge.api.scopes.IEditableModelScope;
import org.eclipse.emf.diffmerge.bridge.api.IBridgeTrace;
import org.eclipse.emf.diffmerge.bridge.api.incremental.IIncrementalBridgeExecution;
import org.eclipse.emf.diffmerge.bridge.incremental.EMFIncrementalBridge;
import org.eclipse.emf.diffmerge.diffdata.EComparison;
import org.eclipse.emf.diffmerge.diffdata.impl.EComparisonImpl;
import org.eclipse.emf.diffmerge.generic.api.IMatchPolicy;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Model;

import com.example.sysarch.coevolution.bridge.UmlToSysarchBridge;
import com.example.sysarch.coevolution.policies.ChangeOrigin;
import com.example.sysarch.coevolution.policies.SysarchDiffPolicy;
import com.example.sysarch.coevolution.policies.TraceBasedThreeWayMatchPolicy;
import com.example.sysarch.coevolution.policies.UmlChangesMergeSelector;

/**
 * Incremental UML -> Sysarch bridge with <b>three-way</b> reconciliation.
 * <p>
 * DiffMerge's {@link EMFIncrementalBridge} re-runs the transformation into an intermediate
 * scope and compares it (REFERENCE) with the existing target (TARGET), matching elements
 * through the bridge traces. Being two-way, it cannot tell a UML change from a project change.
 * This subclass adds the result of the previous import (the <i>baseline</i>) as ANCESTOR,
 * so that every difference gets an origin:
 * <ul>
 * <li>UML change -> merged automatically into the project ({@link UmlChangesMergeSelector})</li>
 * <li>project change -> preserved</li>
 * <li>both -> conflict, delegated to an {@link IConflictHandler} (DiffMerge UI or strategy)</li>
 * </ul>
 * The fresh transformation result becomes the next baseline.
 */
public class SysarchIncrementalBridge extends EMFIncrementalBridge<Model, IEditableModelScope> {

  private final IEditableModelScope ancestorScope;
  private final IBridgeTrace ancestorTrace;
  private final IConflictHandler conflictHandler;

  private List<EObject> newBaseline = Collections.emptyList();
  private List<Change> changes = Collections.emptyList();
  private IStatus mergeStatus = Status.OK_STATUS;

  /**
   * @param transformation the UML -> Sysarch transformation
   * @param ancestorScope the baseline (previous transformation result), may be null (2-way)
   * @param ancestorTrace the trace of the baseline, may be null
   * @param conflictHandler how to resolve conflicts, may be null (project version kept)
   */
  public SysarchIncrementalBridge(UmlToSysarchBridge transformation, IEditableModelScope ancestorScope,
      IBridgeTrace ancestorTrace, IConflictHandler conflictHandler) {
    super(transformation, new SysarchDiffPolicy(), null, new UmlChangesMergeSelector());
    this.ancestorScope = ancestorScope;
    this.ancestorTrace = ancestorTrace;
    this.conflictHandler = conflictHandler != null ? conflictHandler : IConflictHandler.PROJECT_WINS;
  }

  /** Snapshot of the fresh transformation result, before any merge: the next baseline. */
  @Override
  protected void intermediateDataSetFilled(IEditableModelScope created) {
    newBaseline = new ArrayList<>(EcoreUtil.copyAll(created.getContents()));
  }

  @Override
  protected EComparison compare(IEditableModelScope created, IEditableModelScope existing,
      IBridgeTrace createdTrace, IBridgeTrace existingTrace, IProgressMonitor monitor) {
    EComparison result = ancestorScope == null
        ? new EComparisonImpl(existing, created)
        : new EComparisonImpl(existing, created, ancestorScope);
    IMatchPolicy<EObject> matchPolicy = new TraceBasedThreeWayMatchPolicy(
        created, createdTrace, ancestorScope, ancestorTrace, existingTrace);
    result.compute(matchPolicy, getDiffPolicy(), getMergePolicy(), monitor);
    return result;
  }

  @Override
  protected IStatus mergeAutomatically(EComparison comparison, IProgressMonitor monitor) {
    // Classify every difference before merging (origin is computed on the pristine comparison)
    changes = Change.group(comparison.getRemainingDifferences());
    return super.mergeAutomatically(comparison, monitor);
  }

  @Override
  protected boolean isAlwaysInteractive() {
    return false;
  }

  /**
   * Conflicts are delegated to the conflict handler; then the project trace is updated
   * for the elements added to / removed from the project.
   */
  @Override
  public IStatus mergeInteractively(IIncrementalBridgeExecution execution, IProgressMonitor monitor) {
    IStatus result = Status.OK_STATUS;
    Object data = execution.getInteractiveMergeData();
    if (data instanceof EComparison) {
      EComparison comparison = (EComparison) data;
      List<Change> conflicts = getConflicts();
      if (conflicts.stream().anyMatch(c -> !c.isMerged())) {
        result = conflictHandler.resolve(comparison, conflicts, monitor);
      }
      if (result.isOK()) {
        handleMergedDifferences(comparison, execution.getTrace(), execution.getReferenceTrace());
      }
    }
    mergeStatus = result;
    return result;
  }

  // ------------------------------------------------------------------ results

  /** Fresh transformation result (copy), to be stored as the next baseline. */
  public List<EObject> getNewBaseline() {
    return newBaseline;
  }

  /** All user-level changes found by the last execution. */
  public List<Change> getChanges() {
    return changes;
  }

  public List<Change> getConflicts() {
    return changes.stream().filter(c -> c.getOrigin() == ChangeOrigin.CONFLICT).collect(Collectors.toList());
  }

  /** OK, or CANCEL when the conflict handler aborted the synchronization. */
  public IStatus getMergeStatus() {
    return mergeStatus;
  }
}
