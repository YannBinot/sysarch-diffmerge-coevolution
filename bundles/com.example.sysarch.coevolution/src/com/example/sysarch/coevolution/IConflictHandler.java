package com.example.sysarch.coevolution;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.diffmerge.diffdata.EComparison;
import org.eclipse.emf.diffmerge.generic.api.Role;

/**
 * Resolution of the conflicts that remain after the automatic merge of the UML changes.
 * Implementations merge the differences they want into the project (TARGET role) and
 * return OK, or return CANCEL to abort the whole synchronization (nothing is saved).
 */
public interface IConflictHandler {

  IStatus resolve(EComparison comparison, List<Change> conflicts, IProgressMonitor monitor);

  /** Keep the project version for every conflict. */
  IConflictHandler PROJECT_WINS = (comparison, conflicts, monitor) -> Status.OK_STATUS;

  /** Take the UML version for every conflict. */
  IConflictHandler UML_WINS = (comparison, conflicts, monitor) -> {
    comparison.merge(
        d -> d.isConflicting() && !d.isMerged() ? Role.TARGET : null, true, monitor);
    return Status.OK_STATUS;
  };

  /** Abort the synchronization as soon as a conflict exists. */
  IConflictHandler ABORT = (comparison, conflicts, monitor) -> Status.CANCEL_STATUS;
}
