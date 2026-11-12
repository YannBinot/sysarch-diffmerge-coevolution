package com.example.sysarch.coevolution.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.uml2.uml.Model;

import com.example.sysarch.coevolution.CoevolutionProject;
import com.example.sysarch.coevolution.UmlSources;

/**
 * Imports the models of an external UML file: one project per model.
 * When the project already exists and is linked to that model, it is synchronized instead.
 */
public class ImportUmlWizard extends Wizard implements IImportWizard {

  /** A row of the model table. */
  static final class Row {
    final Model model;
    final String projectName;
    final String status;
    final boolean importable;

    Row(Model model) {
      this.model = model;
      this.projectName = UmlSources.projectNameFor(model);
      IProject p = root().getProject(projectName);
      if (!p.exists()) {
        status = "Nouveau projet";
        importable = true;
      } else if (CoevolutionRunner.toCoevolutionProject(p).isLinked()) {
        status = "Projet existant : synchronisation itérative";
        importable = true;
      } else {
        status = "Projet existant non issu d'un import : ignoré";
        importable = false;
      }
    }
  }

  private ModelsPage page;

  public ImportUmlWizard() {
    setWindowTitle("Import UML - un projet par modèle (EMF DiffMerge)");
    setNeedsProgressMonitor(false);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection selection) {
    // nothing
  }

  @Override
  public void addPages() {
    page = new ModelsPage();
    addPage(page);
  }

  @Override
  public boolean performFinish() {
    List<CoevolutionRunner.Task> tasks = new ArrayList<>();
    try {
      for (Object o : page.viewer.getCheckedElements()) {
        Row row = (Row) o;
        if (!row.importable) {
          continue;
        }
        IProject project = root().getProject(row.projectName);
        if (!project.exists()) {
          project.create(null);
        }
        if (!project.isOpen()) {
          project.open(null);
        }
        tasks.add(new CoevolutionRunner.Task(project, row.model));
      }
    } catch (CoreException e) {
      MessageDialog.openError(getShell(), "Import UML", e.getMessage());
      return false;
    }
    if (tasks.isEmpty()) {
      return false;
    }
    CoevolutionRunner.run(getShell(), "Import UML " + page.file.getName(), tasks);
    return true;
  }

  static IWorkspaceRoot root() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  // ---------------------------------------------------------------------------------------

  private final class ModelsPage extends WizardPage {
    private Text fileText;
    private CheckboxTableViewer viewer;
    private File file;

    ModelsPage() {
      super("models");
      setTitle("Modèles du fichier UML");
      setDescription("Chaque modèle UML (racine ou imbriqué) devient un projet. "
          + "Un ré-import synchronise le projet en 3-way : changements UML, changements projet, conflits.");
      setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
      Composite c = new Composite(parent, SWT.NONE);
      c.setLayout(new GridLayout(3, false));
      new Label(c, SWT.NONE).setText("Fichier UML :");
      fileText = new Text(c, SWT.BORDER);
      fileText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
      fileText.addModifyListener(e -> load(fileText.getText()));
      Button browse = new Button(c, SWT.PUSH);
      browse.setText("Parcourir...");
      browse.addListener(SWT.Selection, e -> {
        FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
        fd.setFilterExtensions(new String[] { "*.uml", "*.*" });
        String f = fd.open();
        if (f != null) {
          fileText.setText(f);
        }
      });

      viewer = CheckboxTableViewer.newCheckList(c, SWT.BORDER | SWT.FULL_SELECTION);
      GridData gd = new GridData(GridData.FILL_BOTH);
      gd.horizontalSpan = 3;
      gd.heightHint = 220;
      viewer.getTable().setLayoutData(gd);
      viewer.getTable().setHeaderVisible(true);
      viewer.getTable().setLinesVisible(true);
      column("Modèle UML", 220, r -> r.model.getQualifiedName());
      column("Projet", 200, r -> r.projectName);
      column("Action", 330, r -> r.status);
      viewer.setContentProvider(ArrayContentProvider.getInstance());
      viewer.addCheckStateListener(e -> updateCompletion());
      setControl(c);
    }

    private void column(String title, int width, java.util.function.Function<Row, String> text) {
      TableViewerColumn col = new TableViewerColumn(viewer, SWT.NONE);
      col.getColumn().setText(title);
      col.getColumn().setWidth(width);
      col.setLabelProvider(new ColumnLabelProvider() {
        @Override
        public String getText(Object element) {
          return text.apply((Row) element);
        }
      });
    }

    private void load(String path) {
      viewer.setInput(new Object[0]);
      file = new File(path);
      if (!file.isFile()) {
        setErrorMessage(path.isEmpty() ? null : "Fichier introuvable");
        updateCompletion();
        return;
      }
      try {
        Resource res = UmlSources.load(URI.createFileURI(file.getAbsolutePath()));
        List<Row> rows = new ArrayList<>();
        for (Model m : UmlSources.findModels(res)) {
          rows.add(new Row(m));
        }
        viewer.setInput(rows);
        viewer.setCheckedElements(rows.stream().filter(r -> r.importable).toArray());
        setErrorMessage(rows.isEmpty() ? "Aucun modèle UML dans ce fichier" : null);
        setMessage(rows.size() + " modèle(s) trouvé(s). Les projets créés contiendront un fichier ."
            + CoevolutionProject.MODEL_EXTENSION + " et un dossier " + CoevolutionProject.META_FOLDER
            + " (trace + baseline).");
      } catch (RuntimeException e) {
        setErrorMessage("Fichier UML illisible : " + e.getMessage());
      }
      updateCompletion();
    }

    private void updateCompletion() {
      setPageComplete(viewer.getCheckedElements().length > 0 && getErrorMessage() == null);
    }
  }
}
