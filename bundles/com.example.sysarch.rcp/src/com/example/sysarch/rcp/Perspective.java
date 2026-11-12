package com.example.sysarch.rcp;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;

/** Project Explorer | editors | Outline, with Properties and Console at the bottom. */
public class Perspective implements IPerspectiveFactory {

  @Override
  public void createInitialLayout(IPageLayout layout) {
    String editors = layout.getEditorArea();
    layout.setEditorAreaVisible(true);
    layout.addView(IPageLayout.ID_PROJECT_EXPLORER, IPageLayout.LEFT, 0.22f, editors);
    layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, 0.78f, editors);
    IFolderLayout bottom = layout.createFolder("bottom", IPageLayout.BOTTOM, 0.68f, editors);
    bottom.addView(IPageLayout.ID_PROP_SHEET);
    bottom.addView(IConsoleConstants.ID_CONSOLE_VIEW);

    layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.project");
    layout.addNewWizardShortcut("com.example.sysarch.presentation.SysarchModelWizardID");
    layout.addShowViewShortcut(IPageLayout.ID_PROJECT_EXPLORER);
    layout.addShowViewShortcut(IPageLayout.ID_PROP_SHEET);
    layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
    layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
  }
}
