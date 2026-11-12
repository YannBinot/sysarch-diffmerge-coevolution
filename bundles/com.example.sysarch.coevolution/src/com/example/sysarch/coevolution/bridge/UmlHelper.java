package com.example.sysarch.coevolution.bridge;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.uml2.uml.Behavior;
import org.eclipse.uml2.uml.Comment;
import org.eclipse.uml2.uml.Element;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.Stereotype;

/**
 * Navigation helpers on UML models. Everything is scoped to one {@link Model}:
 * nested Models are separate sources (they become separate projects).
 */
public final class UmlHelper {

  private UmlHelper() {
  }

  /** All elements of the given type contained in the model, excluding nested Models and Profiles. */
  public static <T extends EObject> List<T> contents(Model model, Class<T> type, Predicate<? super T> filter) {
    List<T> result = new ArrayList<>();
    TreeIterator<EObject> it = model.eAllContents();
    while (it.hasNext()) {
      EObject o = it.next();
      if (o instanceof Model || o instanceof Profile) {
        it.prune();
        continue;
      }
      if (type.isInstance(o)) {
        T t = type.cast(o);
        if (filter == null || filter.test(t)) {
          result.add(t);
        }
      }
    }
    return result;
  }

  /** Whether the element is directly owned by a UML package (or by the model itself). */
  public static boolean isPackaged(EObject o) {
    return o.eContainer() instanceof org.eclipse.uml2.uml.Package;
  }

  /** UML classes mapped to sysarch Components (behaviors and stereotypes are excluded). */
  public static boolean isComponentClass(org.eclipse.uml2.uml.Class c) {
    return !(c instanceof Behavior) && !(c instanceof Stereotype) && isPackaged(c);
  }

  /** UML data types that are neither enumerations nor classes. */
  public static boolean isPlainDataType(org.eclipse.uml2.uml.DataType d) {
    return !(d instanceof org.eclipse.uml2.uml.Enumeration) && isPackaged(d);
  }

  /** Concatenation of the bodies of the owned comments, or null. */
  public static String documentation(Element e) {
    if (e.getOwnedComments().isEmpty()) {
      return null;
    }
    String doc = e.getOwnedComments().stream()
        .map(Comment::getBody)
        .filter(b -> b != null && !b.isBlank())
        .collect(Collectors.joining("\n"));
    return doc.isEmpty() ? null : doc;
  }

  /** Upper bound with UML "unlimited" (*) mapped to -1. */
  public static int upper(org.eclipse.uml2.uml.MultiplicityElement m) {
    return m.getUpper();
  }
}
