package com.example.sysarch.coevolution.policies;

import org.eclipse.emf.diffmerge.generic.api.IMergeSelector;
import org.eclipse.emf.diffmerge.generic.api.Role;
import org.eclipse.emf.diffmerge.generic.api.diff.IDifference;
import org.eclipse.emf.ecore.EObject;

/**
 * Automatic (non-interactive) merge selector of the co-evolution:
 * <ul>
 * <li>changes made in UML since the last import are merged into the project (TARGET);</li>
 * <li>changes made in the project are preserved (no merge);</li>
 * <li>conflicts are left unmerged for the conflict handler (interactive or strategy).</li>
 * </ul>
 */
public class UmlChangesMergeSelector implements IMergeSelector<EObject> {

  @Override
  public Role getMergeDirection(IDifference<EObject> difference) {
    return ChangeOrigin.of(difference) == ChangeOrigin.UML ? Role.TARGET : null;
  }
}
