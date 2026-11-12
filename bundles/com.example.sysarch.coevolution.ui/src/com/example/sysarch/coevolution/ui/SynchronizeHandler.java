package com.example.sysarch.coevolution.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Synchronizes the selected projects (or every imported project when nothing is selected)
 * with the UML model they were imported from.
 */
public class SynchronizeHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) {
    Shell shell = HandlerUtil.getActiveShell(event);
    Set<IProject> candidates = new LinkedHashSet<>();
    ISelection selection = HandlerUtil.getCurrentSelection(event);
    if (selection instanceof IStructuredSelection) {
      for (Object o : ((IStructuredSelection) selection).toList()) {
        IResource r = Adapters.adapt(o, IResource.class);
        if (r != null) {
          candidates.add(r.getProject());
        }
      }
    }
    if (candidates.isEmpty()) {
      for (IProject p : ResourcesPlugin.getWorkspace().getRoot().getProjects()) {
        candidates.add(p);
      }
    }
    List<CoevolutionRunner.Task> tasks = new ArrayList<>();
    for (IProject p : candidates) {
      if (p.isOpen() && CoevolutionRunner.toCoevolutionProject(p).isLinked()) {
        tasks.add(new CoevolutionRunner.Task(p, null));
      }
    }
    if (tasks.isEmpty()) {
      CoevolutionRunner.info(shell, "Synchroniser avec l'UML",
          "Aucun projet issu d'un import UML dans la sélection.\nUtilisez d'abord « Importer un fichier UML... ».");
      return null;
    }
    CoevolutionRunner.run(shell, "Synchronisation UML", tasks);
    return null;
  }
}
