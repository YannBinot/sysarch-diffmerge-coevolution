package com.example.sysarch.coevolution;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

/**
 * Loading of external UML files and discovery of the models they contain.
 * A file may hold several models: several root {@code uml:Model} elements and/or
 * models nested in other models. Each of them is imported as a separate project.
 */
public final class UmlSources {

  private UmlSources() {
  }

  /** Loads a UML file in a fresh resource set. */
  public static Resource load(URI umlFile) {
    ResourceSet rs = new ResourceSetImpl();
    if (!EMFPlugin.IS_ECLIPSE_RUNNING) {
      UMLResourcesUtil.init(rs);
    }
    return rs.getResource(umlFile, true);
  }

  /** All the models of the resource, roots first, then nested ones (depth-first). */
  public static List<Model> findModels(Resource resource) {
    List<Model> result = new ArrayList<>();
    for (EObject root : resource.getContents()) {
      if (root instanceof Model) {
        result.add((Model) root);
      }
      TreeIterator<EObject> it = root.eAllContents();
      while (it.hasNext()) {
        EObject o = it.next();
        if (o instanceof Model) {
          result.add((Model) o);
        } else if (!(o instanceof org.eclipse.uml2.uml.Package)) {
          it.prune(); // models can only be nested in packages
        }
      }
    }
    return result;
  }

  /** Persistent identifier of a model in its file (its xmi:id when present). */
  public static String idOf(Model model) {
    return model.eResource().getURIFragment(model);
  }

  /** Finds a model by persistent identifier, or null. */
  public static Model findModel(Resource resource, String modelId) {
    EObject o = resource.getEObject(modelId);
    return o instanceof Model ? (Model) o : null;
  }

  /** A valid project name derived from a model name. */
  public static String projectNameFor(Model model) {
    String name = model.getName() == null ? "model" : model.getName();
    return name.trim().replaceAll("[^A-Za-z0-9._-]+", "_");
  }
}
