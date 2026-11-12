package com.example.sysarch.coevolution.policies;

import java.util.Set;

import org.eclipse.emf.diffmerge.impl.policies.DefaultDiffPolicy;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.example.sysarch.SysarchPackage;

/**
 * Diff policy of the UML co-evolution.
 * <ul>
 * <li>Technical identifiers are ignored (elements are matched through traces).</li>
 * <li>Project-owned features ({@code Component.status}, {@code Component.notes}) are not
 *     produced by the transformation: they are excluded from the comparison so that they
 *     are never reset by an import.</li>
 * <li>Ordering is ignored: the UML element order is not significant in the projects.</li>
 * </ul>
 */
public class SysarchDiffPolicy extends DefaultDiffPolicy {

  private static final Set<EStructuralFeature> PROJECT_OWNED = Set.of(
      SysarchPackage.Literals.NAMED_ELEMENT__ID,
      SysarchPackage.Literals.COMPONENT__STATUS,
      SysarchPackage.Literals.COMPONENT__NOTES);

  /** Whether the feature belongs to the project and is never synchronized with UML. */
  public static boolean isProjectOwned(EStructuralFeature feature) {
    return PROJECT_OWNED.contains(feature);
  }

  @Override
  protected boolean coverFeature(EStructuralFeature feature) {
    return !isProjectOwned(feature) && super.coverFeature(feature);
  }

  @Override
  protected boolean considerOrderedFeature(EStructuralFeature feature) {
    return false;
  }
}
