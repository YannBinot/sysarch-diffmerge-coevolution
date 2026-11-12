package com.example.sysarch.coevolution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.diffmerge.generic.api.IMatch;
import org.eclipse.emf.diffmerge.generic.api.Role;
import org.eclipse.emf.diffmerge.generic.api.diff.IDifference;
import org.eclipse.emf.diffmerge.generic.api.diff.IElementPresence;
import org.eclipse.emf.diffmerge.generic.api.diff.IElementRelativeDifference;
import org.eclipse.emf.diffmerge.generic.api.diff.IValuePresence;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;

import com.example.sysarch.NamedElement;
import com.example.sysarch.coevolution.policies.ChangeOrigin;

/**
 * A user-level change: one or several DiffMerge differences on the same setting
 * (e.g. the two symmetrical value presences of a renamed element), with its origin.
 */
public final class Change {

  private final ChangeOrigin origin;
  private final String description;
  private final List<IDifference<EObject>> differences;

  private Change(ChangeOrigin origin, String description, List<IDifference<EObject>> differences) {
    this.origin = origin;
    this.description = description;
    this.differences = differences;
  }

  public ChangeOrigin getOrigin() {
    return origin;
  }

  public String getDescription() {
    return description;
  }

  public List<IDifference<EObject>> getDifferences() {
    return Collections.unmodifiableList(differences);
  }

  public boolean isMerged() {
    return differences.stream().anyMatch(IDifference::isMerged);
  }

  @Override
  public String toString() {
    return description;
  }

  // ---------------------------------------------------------------- grouping

  /** Groups the given differences into user-level changes, in a stable order. */
  public static List<Change> group(Collection<IDifference<EObject>> diffs) {
    Map<Object, List<IDifference<EObject>>> groups = new LinkedHashMap<>();
    for (IDifference<EObject> d : diffs) {
      groups.computeIfAbsent(key(d), k -> new ArrayList<>()).add(d);
    }
    List<Change> result = new ArrayList<>();
    for (List<IDifference<EObject>> g : groups.values()) {
      ChangeOrigin origin = g.stream().anyMatch(IDifference::isConflicting)
          ? ChangeOrigin.CONFLICT : ChangeOrigin.of(g.get(0));
      result.add(new Change(origin, describe(origin, g), g));
    }
    return result;
  }

  private static Object key(IDifference<EObject> d) {
    if (d instanceof IValuePresence<?>) {
      IValuePresence<EObject> v = (IValuePresence<EObject>) d;
      if (v.getFeature() instanceof EReference && ((EReference) v.getFeature()).isContainment()
          && v.getValue() instanceof EObject) {
        // Ownership of an added/removed element: same user-level change as the element presence
        IMatch<EObject> valueMatch = v.getComparison().getMapping().getMatchFor(v.getValue(), v.getPresenceRole());
        if (valueMatch != null) {
          return valueMatch;
        }
      }
      List<Object> k = new ArrayList<>();
      k.add(v.getElementMatch());
      k.add(v.getFeature());
      if (v.isManyFeature() && !v.isOrder()) {
        k.add(v.getValue());
      }
      return k;
    }
    if (d instanceof IElementRelativeDifference<?>) {
      return ((IElementRelativeDifference<EObject>) d).getElementMatch();
    }
    return d;
  }

  private static String describe(ChangeOrigin origin, List<IDifference<EObject>> g) {
    IDifference<EObject> first = g.stream().filter(d -> d instanceof IElementPresence<?>).findFirst().orElse(g.get(0));
    String prefix;
    switch (origin) {
      case UML: prefix = "[UML]     "; break;
      case PROJECT: prefix = "[PROJET]  "; break;
      default: prefix = "[CONFLIT] ";
    }
    if (first instanceof IElementPresence<?>) {
      IElementPresence<EObject> p = (IElementPresence<EObject>) first;
      boolean inProject = p.getPresenceRole() == Role.TARGET;
      String what;
      if (origin == ChangeOrigin.UML) {
        what = inProject ? "suppression de " : "ajout de ";
      } else if (origin == ChangeOrigin.PROJECT) {
        what = inProject ? "ajout local de " : "suppression locale de ";
      } else {
        what = inProject ? "présent dans le projet seulement : " : "présent dans l'UML seulement : ";
      }
      return prefix + what + label(p.getElement());
    }
    if (first instanceof IValuePresence<?>) {
      IValuePresence<EObject> v = (IValuePresence<EObject>) first;
      IMatch<EObject> match = v.getElementMatch();
      EObject element = match.get(Role.TARGET) != null ? match.get(Role.TARGET) : match.get(Role.REFERENCE);
      String featureName = v.getFeature() instanceof EStructuralFeature
          ? ((EStructuralFeature) v.getFeature()).getName() : String.valueOf(v.getFeature());
      StringBuilder sb = new StringBuilder(prefix).append(label(element)).append('.').append(featureName);
      if (v.isManyFeature() && !v.isOrder()) {
        boolean inProject = v.getPresenceRole() == Role.TARGET;
        sb.append(inProject ? " : valeur présente seulement dans le projet = "
                            : " : valeur présente seulement dans l'UML = ")
          .append(value(v.getValue()));
      } else {
        sb.append(" : projet = ").append(settingValue(match.get(Role.TARGET), v.getFeature()))
          .append(" | UML = ").append(settingValue(match.get(Role.REFERENCE), v.getFeature()));
        if (match.get(Role.ANCESTOR) != null) {
          sb.append(" | dernier import = ").append(settingValue(match.get(Role.ANCESTOR), v.getFeature()));
        }
      }
      return sb.toString();
    }
    return prefix + first;
  }

  private static String settingValue(EObject element, Object feature) {
    if (element == null || !(feature instanceof EStructuralFeature)) {
      return "-";
    }
    return value(element.eGet((EStructuralFeature) feature));
  }

  static String value(Object v) {
    if (v == null) {
      return "∅";
    }
    if (v instanceof EObject) {
      return label((EObject) v);
    }
    if (v instanceof String) {
      return "\"" + v + "\"";
    }
    return String.valueOf(v);
  }

  public static String label(EObject e) {
    if (e == null) {
      return "∅";
    }
    String name = e instanceof NamedElement ? ((NamedElement) e).getName() : null;
    return e.eClass().getName() + " '" + (name == null ? "?" : name) + "'";
  }
}
