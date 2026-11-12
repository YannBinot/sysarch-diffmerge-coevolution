package com.example.sysarch.coevolution.policies;

import java.util.Comparator;

import org.eclipse.emf.diffmerge.bridge.api.IBridgeTrace;
import org.eclipse.emf.diffmerge.generic.api.IMatchPolicy;
import org.eclipse.emf.diffmerge.generic.api.scopes.ITreeDataScope;
import org.eclipse.emf.ecore.EObject;

/**
 * Three-way extension of DiffMerge's {@code BridgeTraceBasedMatchPolicy}.
 * <p>
 * Elements are matched by the <i>cause</i> that produced them (UML element id + rule id),
 * looked up in the trace associated with the scope they belong to:
 * <ul>
 * <li>REFERENCE: freshly transformed UML model -> trace of the current bridge execution</li>
 * <li>TARGET: project content -> persistent project trace</li>
 * <li>ANCESTOR: result of the previous import (baseline) -> baseline trace</li>
 * </ul>
 * Elements created by hand in the project have no cause, hence no match: they are
 * seen as project-side additions and are left untouched.
 */
public class TraceBasedThreeWayMatchPolicy implements IMatchPolicy<EObject> {

  private final ITreeDataScope<EObject> createdScope;
  private final IBridgeTrace createdTrace;
  private final ITreeDataScope<EObject> ancestorScope;
  private final IBridgeTrace ancestorTrace;
  private final IBridgeTrace existingTrace;

  public TraceBasedThreeWayMatchPolicy(ITreeDataScope<EObject> createdScope, IBridgeTrace createdTrace,
      ITreeDataScope<EObject> ancestorScope, IBridgeTrace ancestorTrace, IBridgeTrace existingTrace) {
    this.createdScope = createdScope;
    this.createdTrace = createdTrace;
    this.ancestorScope = ancestorScope;
    this.ancestorTrace = ancestorTrace;
    this.existingTrace = existingTrace;
  }

  @Override
  public Object getMatchID(EObject element, ITreeDataScope<EObject> scope) {
    IBridgeTrace trace;
    if (scope == createdScope) {
      trace = createdTrace;
    } else if (ancestorScope != null && scope == ancestorScope) {
      trace = ancestorTrace;
    } else {
      trace = existingTrace;
    }
    return trace == null ? null : trace.getCause(element);
  }

  @Override
  public Comparator<?> getMatchIDComparator() {
    return IMatchPolicy.NATURAL_ORDER_COMPARATOR; // causes are Strings
  }

  @Override
  public boolean keepMatchIDs() {
    return true; // lets the DiffMerge UI explain how elements were matched
  }
}
