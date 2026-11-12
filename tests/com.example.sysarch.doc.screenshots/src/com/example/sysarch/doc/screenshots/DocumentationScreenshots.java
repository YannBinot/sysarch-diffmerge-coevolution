package com.example.sysarch.doc.screenshots;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.ide.IDE;
import org.eclipse.uml2.uml.Model;
import org.junit.Test;

import com.example.sysarch.Component;
import com.example.sysarch.ComponentStatus;
import com.example.sysarch.NamedElement;
import com.example.sysarch.SysarchFactory;
import com.example.sysarch.SystemModel;
import com.example.sysarch.coevolution.CoevolutionProject;
import com.example.sysarch.coevolution.CoevolutionService;
import com.example.sysarch.coevolution.IConflictHandler;
import com.example.sysarch.coevolution.SyncReport;
import com.example.sysarch.coevolution.UmlSources;
import com.example.sysarch.coevolution.ui.CoevolutionRunner;
import com.example.sysarch.coevolution.ui.ImportUmlWizard;
import com.example.sysarch.coevolution.ui.ReportDialog;
import com.example.sysarch.coevolution.ui.UiConflictHandler;
import com.example.sysarch.presentation.SysarchEditor;

/**
 * Drives the real application through the documented scenario and saves screenshots
 * in docs/images. Not a regression test: run with {@code mvn verify -Pscreenshots}.
 */
public class DocumentationScreenshots {

  private final Display display = Display.getDefault();
  private final File images = new File(System.getProperty("images.dir", "docs/images"));
  private final File samples = new File(System.getProperty("samples.dir", "samples"));
  private final CoevolutionService service = new CoevolutionService();

  @Test
  public void captureScenario() throws Exception {
    images.mkdirs();
    IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
    window.getShell().setSize(1500, 920);
    window.getShell().setLocation(20, 20);
    pump(1500);

    File uml = new File(Files.createTempDirectory("demo").toFile(), "vehicle.uml");
    Files.copy(new File(samples, "vehicle-v1.uml").toPath(), uml.toPath(), StandardCopyOption.REPLACE_EXISTING);

    // 1. Import wizard
    ImportUmlWizard wizard = new ImportUmlWizard();
    wizard.setInitialFile(uml.getAbsolutePath());
    WizardDialog wd = new WizardDialog(window.getShell(), wizard);
    wd.setBlockOnOpen(false);
    wd.setPageSize(820, 300);
    wd.open();
    pump(1500);
    capture(wd.getShell(), "01-assistant-import");
    wd.close();

    // 2. Import of the 3 models (same code path as the wizard, without the modal progress dialog)
    Resource umlRes = UmlSources.load(URI.createFileURI(uml.getAbsolutePath()));
    List<SyncReport> imports = new ArrayList<>();
    for (Model m : UmlSources.findModels(umlRes)) {
      IProject p = project(UmlSources.projectNameFor(m));
      imports.add(service.synchronize(cp(p), m, IConflictHandler.ABORT, null));
    }
    refresh();

    // 3. Local edits in two projects, then show the workbench with the editor
    edit("VehicleSystem", root -> {
      Component engine = find(root, Component.class, "Engine");
      engine.setStatus(ComponentStatus.VALIDATED);
      engine.setNotes("Validé en revue du 12/11");
      engine.setDescription("Moteur thermique (description du projet)");
      find(root, Component.class, "Wheel").setName("RoadWheel");
      Component battery = SysarchFactory.eINSTANCE.createComponent();
      battery.setName("Battery");
      find(root, com.example.sysarch.Package.class, "powertrain").getTypes().add(battery);
      EcoreUtil.delete(find(root, com.example.sysarch.Attribute.class, "wheels"));
    });
    edit("Telemetry", root -> {
      find(root, com.example.sysarch.Attribute.class, "sampleRate").setName("rate");
      find(root, Component.class, "DataLogger").setDescription("Enregistreur (description du projet)");
    });
    refresh();
    SysarchEditor editor = openEditor("VehicleSystem", "Engine");
    pump(1500);
    capture(window.getShell(), "02-projets-importes-editeur");

    // 4. The external UML evolves (v2), synchronization of the two projects with conflicts
    window.getActivePage().closeAllEditors(false);
    Files.copy(new File(samples, "vehicle-v2.uml").toPath(), uml.toPath(), StandardCopyOption.REPLACE_EXISTING);
    List<SyncReport> syncs = new ArrayList<>();
    syncs.add(service.synchronize(cp(project("VehicleSystem")),
        screenshotHandler("VehicleSystem", "03-conflits-vehiclesystem", "04-diffmerge-vehiclesystem",
            IConflictHandler.PROJECT_WINS), null));
    syncs.add(service.synchronize(cp(project("Telemetry")),
        screenshotHandler("Telemetry", "05-conflits-telemetry", "06-diffmerge-telemetry",
            IConflictHandler.UML_WINS), null));
    syncs.add(service.synchronize(cp(project("ChargingInfrastructure")), IConflictHandler.ABORT, null));
    refresh();

    // 5. Report dialog + console
    StringBuilder text = new StringBuilder();
    syncs.forEach(text::append);
    ReportDialog report = new ReportDialog(window.getShell(), "Synchronisation UML", text.toString());
    report.setBlockOnOpen(false);
    report.open();
    pump(1000);
    capture(report.getShell(), "07-rapport");
    report.close();

    StringBuilder importText = new StringBuilder();
    imports.forEach(importText::append);
    CoevolutionRunner.log("Import UML vehicle.uml", importText.toString());
    CoevolutionRunner.log("Synchronisation UML", text.toString());
    window.getActivePage().showView(IConsoleConstants.ID_CONSOLE_VIEW);
    openEditor("VehicleSystem", "Engine");
    pump(1500);
    capture(window.getShell(), "08-apres-synchronisation-console");

    // 6. Next iteration: up to date
    SyncReport again = service.synchronize(cp(project("VehicleSystem")), IConflictHandler.ABORT, null);
    ReportDialog upToDate = new ReportDialog(window.getShell(), "Re-synchronisation", again.toString());
    upToDate.setBlockOnOpen(false);
    upToDate.open();
    pump(800);
    capture(upToDate.getShell(), "09-resynchronisation-a-jour");
    upToDate.close();
  }

  // --------------------------------------------------------------------------------- helpers

  private IConflictHandler screenshotHandler(String project, String strategyImage, String diffMergeImage,
      IConflictHandler finalChoice) {
    return (comparison, conflicts, monitor) -> {
      UiConflictHandler ui = new UiConflictHandler(project);
      Shell parent = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
      MessageDialog strategy = ui.createStrategyDialog(parent, conflicts);
      strategy.setBlockOnOpen(false);
      strategy.open();
      pump(800);
      capture(strategy.getShell(), strategyImage);
      strategy.close();

      UiConflictHandler.CoevolutionUpdateDialog dm = ui.createDiffMergeDialog(parent, comparison);
      dm.setBlockOnOpen(false);
      dm.open();
      dm.getShell().setSize(1500, 880);
      dm.getShell().setLocation(30, 30);
      pump(1500);
      try {
        org.eclipse.emf.diffmerge.ui.viewers.ComparisonTreeViewer tv =
            ((org.eclipse.emf.diffmerge.ui.viewers.ComparisonViewer) dm.getComparisonViewer())
              .getSynthesisViewer().getInnerViewer();
        tv.expandAll();
        pump(300);
        org.eclipse.jface.viewers.TreePath path = new org.eclipse.jface.viewers.TreePath(
            new Object[] { tv.getTree().getItem(0).getData() });
        org.eclipse.jface.viewers.TreePath target = null;
        for (org.eclipse.jface.viewers.TreePath p = tv.getNextUserDifference(path); p != null;
             p = tv.getNextUserDifference(p)) {
          if (target == null) {
            target = p;
          }
          if (String.valueOf(p.getLastSegment()).contains("Engine")
              || String.valueOf(p.getLastSegment()).contains("DataLogger")) {
            target = p;
            break;
          }
        }
        if (target != null) {
          tv.setSelection(new org.eclipse.jface.viewers.TreeSelection(target), true);
          pump(300);
          org.eclipse.swt.widgets.Tree tree = tv.getTree();
          if (tree.getSelectionCount() > 0) {
            org.eclipse.swt.widgets.Event ev = new org.eclipse.swt.widgets.Event();
            ev.item = tree.getSelection()[0];
            tree.setFocus();
            tree.notifyListeners(org.eclipse.swt.SWT.Selection, ev);
          }
        }
      } catch (RuntimeException e) {
        e.printStackTrace();
      }
      pump(2000);
      capture(dm.getShell(), diffMergeImage);
      dm.close();
      IStatus status = finalChoice.resolve(comparison, conflicts, monitor);
      return status;
    };
  }

  private SysarchEditor openEditor(String project, String select) throws Exception {
    IFile file = project(project).getFile(project + ".sysarch");
    IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
    SysarchEditor editor = (SysarchEditor) IDE.openEditor(page, file, true);
    pump(500);
    if (editor.getViewer() instanceof TreeViewer) {
      ((TreeViewer) editor.getViewer()).expandAll();
    }
    for (Resource r : editor.getEditingDomain().getResourceSet().getResources()) {
      if (!r.getContents().isEmpty()) {
        Component c = findOrNull(r.getContents().get(0), Component.class, select);
        if (c != null) {
          editor.setSelectionToViewer(List.of(c));
        }
      }
    }
    page.activate(editor);
    return editor;
  }

  private IProject project(String name) throws Exception {
    IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
    if (!p.exists()) {
      p.create(null);
    }
    if (!p.isOpen()) {
      p.open(null);
    }
    return p;
  }

  private static CoevolutionProject cp(IProject p) {
    return CoevolutionRunner.toCoevolutionProject(p);
  }

  private void edit(String projectName, Consumer<SystemModel> change) throws Exception {
    ResourceSet rs = CoevolutionProject.newResourceSet();
    Resource r = rs.getResource(cp(project(projectName)).getModelUri(), true);
    change.accept((SystemModel) r.getContents().get(0));
    r.save(CoevolutionProject.saveOptions());
  }

  private void refresh() throws Exception {
    ResourcesPlugin.getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
    pump(300);
  }

  private void pump(long millis) {
    long end = System.currentTimeMillis() + millis;
    while (System.currentTimeMillis() < end) {
      if (!display.readAndDispatch()) {
        try {
          Thread.sleep(15);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }

  private void capture(Shell shell, String name) {
    shell.setMinimized(false);
    shell.moveAbove(null);
    shell.forceActive();
    shell.setFocus();
    pump(600);
    org.eclipse.swt.graphics.Rectangle b = shell.getBounds();
    try {
      java.awt.Robot robot = new java.awt.Robot();
      java.awt.image.BufferedImage img = robot.createScreenCapture(new java.awt.Rectangle(b.x, b.y, b.width, b.height));
      javax.imageio.ImageIO.write(img, "png", new File(images, name + ".png"));
      System.out.println("screenshot " + name);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private static <T extends NamedElement> T find(EObject root, Class<T> type, String name) {
    T t = findOrNull(root, type, name);
    if (t == null) {
      throw new IllegalStateException(type.getSimpleName() + " " + name);
    }
    return t;
  }

  private static <T extends NamedElement> T findOrNull(EObject root, Class<T> type, String name) {
    if (type.isInstance(root) && name.equals(((NamedElement) root).getName())) {
      return type.cast(root);
    }
    for (var it = root.eAllContents(); it.hasNext();) {
      EObject o = it.next();
      if (type.isInstance(o) && name.equals(((NamedElement) o).getName())) {
        return type.cast(o);
      }
    }
    return null;
  }

  static int ok() {
    return Window.OK;
  }
}
