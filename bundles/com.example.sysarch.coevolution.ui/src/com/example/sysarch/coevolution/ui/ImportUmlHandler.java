package com.example.sysarch.coevolution.ui;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/** Opens the UML import wizard. */
public class ImportUmlHandler extends AbstractHandler {

  @Override
  public Object execute(ExecutionEvent event) {
    ImportUmlWizard wizard = new ImportUmlWizard();
    wizard.init(PlatformUI.getWorkbench(), StructuredSelection.EMPTY);
    WizardDialog dialog = new WizardDialog(HandlerUtil.getActiveShell(event), wizard);
    dialog.setPageSize(800, 350);
    dialog.open();
    return null;
  }
}
