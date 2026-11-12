package com.example.sysarch.coevolution.policies;

import org.eclipse.emf.diffmerge.generic.api.Role;
import org.eclipse.emf.diffmerge.generic.api.diff.IDifference;
import org.eclipse.emf.diffmerge.generic.api.diff.IPresenceDifference;

/**
 * Side that originated a difference in the 3-way comparison
 * (TARGET = project, REFERENCE = new UML transformation result, ANCESTOR = last import).
 */
public enum ChangeOrigin {
  /** The external UML model changed since the last import: the update can be applied. */
  UML,
  /** The project was edited since the last import: the local change is preserved. */
  PROJECT,
  /** Both sides changed the same information differently: decision required. */
  CONFLICT;

  /**
   * In a three-way comparison, a presence difference "aligned with the ancestor" means the
   * ancestor holds the same value as the presence side: the change was therefore made on the
   * opposite side. Otherwise the value appeared on the presence side.
   */
  public static ChangeOrigin of(IDifference<?> difference) {
    if (difference.isConflicting()) {
      return CONFLICT;
    }
    if (!difference.getComparison().isThreeWay()) {
      // No baseline: everything is considered as coming from UML except project-only presences
      if (difference instanceof IPresenceDifference<?>
          && ((IPresenceDifference<?>) difference).getPresenceRole() == Role.TARGET) {
        return PROJECT;
      }
      return UML;
    }
    if (difference instanceof IPresenceDifference<?>) {
      Role presence = ((IPresenceDifference<?>) difference).getPresenceRole();
      Role origin = difference.isAlignedWithAncestor() ? presence.opposite() : presence;
      return origin == Role.REFERENCE ? UML : PROJECT;
    }
    return PROJECT;
  }
}
