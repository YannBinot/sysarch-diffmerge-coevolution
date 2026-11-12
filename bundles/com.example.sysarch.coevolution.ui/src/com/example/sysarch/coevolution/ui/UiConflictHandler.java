package com.example.sysarch.coevolution.ui;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.diffmerge.bridge.interactive.UpdateDialog;
import org.eclipse.emf.diffmerge.diffdata.EComparison;
import org.eclipse.emf.diffmerge.generic.api.Role;
import org.eclipse.emf.diffmerge.ui.viewers.EMFDiffNode;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.example.sysarch.coevolution.Change;
import com.example.sysarch.coevolution.IConflictHandler;

/**
 * Interactive conflict resolution. The user chooses a global strategy or resolves each
 * conflict in the EMF DiffMerge comparison dialog of the co-evolution bridge
 * ({@link UpdateDialog}: left = new UML result, right = project, 3-way with the last import).
 * May be called from a non-UI thread.
 */
public class UiConflictHandler implements IConflictHandler {

  private static final int CHOICE_DIFFMERGE = 0;
  private static final int CHOICE_PROJECT = 1;
  private static final int CHOICE_UML = 2;

  private final String projectName;

  public UiConflictHandler(String projectName) {
    this.projectName = projectName;
  }

  @Override
  public IStatus resolve(EComparison comparison, List<Change> conflicts, IProgressMonitor monitor) {
    Display display = Display.getDefault();
    int[] choice = { -1 };
    display.syncExec(() -> choice[0] = askStrategy(display.getActiveShell(), conflicts));
    switch (choice[0]) {
      case CHOICE_DIFFMERGE:
        int[] rc = { IDialogConstants.CANCEL_ID };
        display.syncExec(() -> rc[0] = openDiffMergeDialog(display.getActiveShell(), comparison));
        return rc[0] == IDialogConstants.OK_ID ? Status.OK_STATUS : Status.CANCEL_STATUS;
      case CHOICE_PROJECT:
        return IConflictHandler.PROJECT_WINS.resolve(comparison, conflicts, monitor);
      case CHOICE_UML:
        return IConflictHandler.UML_WINS.resolve(comparison, conflicts, monitor);
      default:
        return Status.CANCEL_STATUS;
    }
  }

  private int askStrategy(Shell shell, List<Change> conflicts) {
    String list = conflicts.stream().limit(12)
        .map(c -> " • " + c.getDescription().replace("[CONFLIT] ", ""))
        .collect(Collectors.joining("\n"));
    if (conflicts.size() > 12) {
      list += "\n • ... (" + (conflicts.size() - 12) + " autres)";
    }
    String message = conflicts.size() + " conflit(s) entre le projet '" + projectName
        + "' et le modèle UML :\nla même information a été modifiée des deux côtés depuis le dernier import.\n\n"
        + list + "\n\nLes autres changements (UML seul ou projet seul) sont déjà réconciliés automatiquement.";
    MessageDialog dialog = new MessageDialog(shell, "Conflits de co-évolution - " + projectName, null, message,
        MessageDialog.WARNING, CHOICE_DIFFMERGE,
        "Résoudre dans DiffMerge...", "Garder le projet", "Prendre l'UML", "Annuler la synchronisation");
    return dialog.open();
  }

  /** Same configuration as DiffMerge's EMFInteractiveBridge: UML result on the left, project on the right. */
  private int openDiffMergeDialog(Shell shell, EComparison comparison) {
    EMFDiffNode node = new EMFDiffNode(comparison, null, true, false);
    node.setReferenceRole(Role.TARGET);
    node.setDrivingRole(Role.TARGET);
    node.setLeftRole(Role.REFERENCE);
    UpdateDialog dialog = new UpdateDialog(shell,
        "Co-évolution UML -> " + projectName + " : fusionner vers la droite ce qui doit venir de l'UML", node) {
      @Override
      protected Button createOpenEditorButton(Composite parent) {
        return null; // the result is applied by the synchronization itself, not in a compare editor
      }
    };
    return dialog.open();
  }
}
