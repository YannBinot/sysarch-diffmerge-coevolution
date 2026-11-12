package com.example.sysarch.coevolution.ui;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.eclipse.uml2.uml.Model;

import com.example.sysarch.coevolution.CoevolutionProject;
import com.example.sysarch.coevolution.CoevolutionService;
import com.example.sysarch.coevolution.SyncReport;

/**
 * Runs imports / synchronizations of workspace projects with progress, then refreshes the
 * workspace and reports the outcome (console "Co-évolution UML" + summary dialog).
 */
public final class CoevolutionRunner {

  private static final String CONSOLE_NAME = "Co-évolution UML";

  /** One unit of work: a project and, for an import, the UML model (null = re-read the linked source). */
  public static final class Task {
    final IProject project;
    final Model model;

    public Task(IProject project, Model model) {
      this.project = project;
      this.model = model;
    }
  }

  private CoevolutionRunner() {
  }

  public static CoevolutionProject toCoevolutionProject(IProject project) {
    return new CoevolutionProject(URI.createFileURI(project.getLocation().toOSString()), project.getName());
  }

  public static void run(Shell shell, String title, List<Task> tasks) {
    // Make sure no editor holds unsaved changes on the projects to update
    IResource[] projects = tasks.stream().map(t -> t.project).toArray(IResource[]::new);
    if (!org.eclipse.ui.ide.IDE.saveAllEditors(projects, true)) {
      return;
    }
    List<SyncReport> reports = new ArrayList<>();
    List<String> errors = new ArrayList<>();
    try {
      new ProgressMonitorDialog(shell).run(true, false, monitor -> {
        SubMonitor sub = SubMonitor.convert(monitor, title, tasks.size());
        CoevolutionService service = new CoevolutionService();
        for (Task task : tasks) {
          sub.subTask(task.project.getName());
          CoevolutionProject cp = toCoevolutionProject(task.project);
          UiConflictHandler handler = new UiConflictHandler(task.project.getName());
          try {
            SyncReport report = task.model != null
                ? service.synchronize(cp, task.model, handler, sub.split(1))
                : service.synchronize(cp, handler, sub.split(1));
            reports.add(report);
          } catch (Exception e) {
            errors.add(task.project.getName() + " : " + e);
            Activator.log(e);
          }
        }
      });
    } catch (InvocationTargetException e) {
      Activator.log(e.getCause());
      errors.add(String.valueOf(e.getCause()));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    for (Task task : tasks) {
      try {
        task.project.refreshLocal(IResource.DEPTH_INFINITE, null);
      } catch (CoreException e) {
        Activator.log(e);
      }
    }
    publish(shell, title, reports, errors);
  }

  private static void publish(Shell shell, String title, List<SyncReport> reports, List<String> errors) {
    StringBuilder text = new StringBuilder();
    for (SyncReport r : reports) {
      text.append(r);
    }
    for (String e : errors) {
      text.append("ERREUR ").append(e).append('\n');
    }
    log(title, text.toString());
    if (!errors.isEmpty()) {
      ErrorDialog.openError(shell, title, "Erreurs pendant la co-évolution (voir console)",
          new org.eclipse.core.runtime.Status(org.eclipse.core.runtime.IStatus.ERROR, Activator.ID,
              String.join("\n", errors)));
    }
    if (!reports.isEmpty()) {
      new ReportDialog(shell, title, text.toString()).open();
    }
  }

  /** Appends a report to the "Co-évolution UML" console. */
  public static void log(String title, String text) {
    MessageConsoleStream out = console().newMessageStream();
    out.println("===== " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " - " + title + " =====");
    out.println(text);
  }

  private static MessageConsole console() {
    IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
    for (IConsole c : manager.getConsoles()) {
      if (CONSOLE_NAME.equals(c.getName()) && c instanceof MessageConsole) {
        manager.showConsoleView(c);
        return (MessageConsole) c;
      }
    }
    MessageConsole console = new MessageConsole(CONSOLE_NAME, null);
    manager.addConsoles(new IConsole[] { console });
    manager.showConsoleView(console);
    return console;
  }

  static void info(Shell shell, String title, String message) {
    MessageDialog.openInformation(shell != null ? shell : PlatformUI.getWorkbench().getDisplay().getActiveShell(),
        title, message);
  }
}
