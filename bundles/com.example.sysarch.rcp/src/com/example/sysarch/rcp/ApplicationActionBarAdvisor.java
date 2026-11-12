package com.example.sysarch.rcp;

import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.actions.ActionFactory.IWorkbenchAction;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.application.ActionBarAdvisor;
import org.eclipse.ui.application.IActionBarConfigurer;

/** Menus File / Edit / (contributions) / Window / Help and the main toolbar. */
public class ApplicationActionBarAdvisor extends ActionBarAdvisor {

  private IWorkbenchAction newWizard;
  private IWorkbenchAction importWizard;
  private IWorkbenchAction save;
  private IWorkbenchAction saveAll;
  private IWorkbenchAction close;
  private IWorkbenchAction quit;
  private IWorkbenchAction undo;
  private IWorkbenchAction redo;
  private IWorkbenchAction cut;
  private IWorkbenchAction copy;
  private IWorkbenchAction paste;
  private IWorkbenchAction delete;
  private IWorkbenchAction selectAll;
  private IWorkbenchAction resetPerspective;
  private IWorkbenchAction about;

  public ApplicationActionBarAdvisor(IActionBarConfigurer configurer) {
    super(configurer);
  }

  @Override
  protected void makeActions(IWorkbenchWindow window) {
    newWizard = register(ActionFactory.NEW.create(window));
    importWizard = register(ActionFactory.IMPORT.create(window));
    save = register(ActionFactory.SAVE.create(window));
    saveAll = register(ActionFactory.SAVE_ALL.create(window));
    close = register(ActionFactory.CLOSE.create(window));
    quit = register(ActionFactory.QUIT.create(window));
    undo = register(ActionFactory.UNDO.create(window));
    redo = register(ActionFactory.REDO.create(window));
    cut = register(ActionFactory.CUT.create(window));
    copy = register(ActionFactory.COPY.create(window));
    paste = register(ActionFactory.PASTE.create(window));
    delete = register(ActionFactory.DELETE.create(window));
    selectAll = register(ActionFactory.SELECT_ALL.create(window));
    resetPerspective = register(ActionFactory.RESET_PERSPECTIVE.create(window));
    about = register(ActionFactory.ABOUT.create(window));
  }

  private IWorkbenchAction register(IWorkbenchAction action) {
    register((org.eclipse.jface.action.IAction) action);
    return action;
  }

  @Override
  protected void fillMenuBar(IMenuManager menuBar) {
    IWorkbenchWindow window = getActionBarConfigurer().getWindowConfigurer().getWindow();

    MenuManager file = new MenuManager("&Fichier", IWorkbenchActionConstants.M_FILE);
    file.add(new GroupMarker(IWorkbenchActionConstants.FILE_START));
    file.add(newWizard);
    file.add(importWizard);
    file.add(new Separator());
    file.add(save);
    file.add(saveAll);
    file.add(close);
    file.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    file.add(new Separator());
    file.add(quit);
    file.add(new GroupMarker(IWorkbenchActionConstants.FILE_END));

    MenuManager edit = new MenuManager("&Edition", IWorkbenchActionConstants.M_EDIT);
    edit.add(new GroupMarker(IWorkbenchActionConstants.EDIT_START));
    edit.add(undo);
    edit.add(redo);
    edit.add(new GroupMarker(IWorkbenchActionConstants.UNDO_EXT));
    edit.add(new Separator());
    edit.add(cut);
    edit.add(copy);
    edit.add(paste);
    edit.add(new GroupMarker(IWorkbenchActionConstants.CUT_EXT));
    edit.add(new Separator());
    edit.add(delete);
    edit.add(selectAll);
    edit.add(new GroupMarker(IWorkbenchActionConstants.ADD_EXT));
    edit.add(new GroupMarker(IWorkbenchActionConstants.EDIT_END));
    edit.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    MenuManager windowMenu = new MenuManager("Fe&nêtre", IWorkbenchActionConstants.M_WINDOW);
    MenuManager showView = new MenuManager("Afficher la vue");
    showView.add(ContributionItemFactory.VIEWS_SHORTLIST.create(window));
    windowMenu.add(showView);
    windowMenu.add(resetPerspective);
    windowMenu.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

    MenuManager help = new MenuManager("&Aide", IWorkbenchActionConstants.M_HELP);
    help.add(new GroupMarker(IWorkbenchActionConstants.HELP_START));
    help.add(about);
    help.add(new GroupMarker(IWorkbenchActionConstants.HELP_END));

    menuBar.add(file);
    menuBar.add(edit);
    menuBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
    menuBar.add(windowMenu);
    menuBar.add(help);
  }

  @Override
  protected void fillCoolBar(ICoolBarManager coolBar) {
    IToolBarManager toolbar = new ToolBarManager(SWT.FLAT | SWT.RIGHT);
    toolbar.add(newWizard);
    toolbar.add(save);
    toolbar.add(saveAll);
    coolBar.add(new ToolBarContributionItem(toolbar, "main"));
    coolBar.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
  }
}
