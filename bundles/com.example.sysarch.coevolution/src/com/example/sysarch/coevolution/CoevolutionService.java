package com.example.sysarch.coevolution;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.diffmerge.api.scopes.IEditableModelScope;
import org.eclipse.emf.diffmerge.bridge.api.IBridgeTrace;
import org.eclipse.emf.diffmerge.bridge.api.incremental.IIncrementalBridgeExecution;
import org.eclipse.emf.diffmerge.bridge.traces.gen.bridgetraces.Trace;
import org.eclipse.emf.diffmerge.impl.scopes.RootedModelScope;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Model;

import com.example.sysarch.coevolution.bridge.UmlToSysarchBridge;

/**
 * Entry point of the co-evolution: imports a UML model into a project the first time,
 * then keeps the project synchronized with it, iteratively.
 * <p>
 * Each run executes the DiffMerge incremental bridge:
 * <ol>
 * <li>first import: the transformation writes directly into the project; the trace and a copy
 *     of the result (baseline) are stored;</li>
 * <li>next runs: the transformation is replayed in an intermediate scope, compared in 3-way
 *     with the project (TARGET) and the baseline (ANCESTOR), UML changes are merged, project
 *     changes are kept and conflicts are given to the {@link IConflictHandler}.</li>
 * </ol>
 * Nothing is written when the conflict handler cancels.
 */
public class CoevolutionService {

  /** Imports or synchronizes all the models of a UML file into projects located in a parent folder. */
  public List<SyncReport> importAll(URI umlFile, URI parentFolder, IConflictHandler handler, IProgressMonitor monitor)
      throws IOException {
    Resource uml = UmlSources.load(umlFile);
    List<SyncReport> reports = new ArrayList<>();
    for (Model model : UmlSources.findModels(uml)) {
      String name = UmlSources.projectNameFor(model);
      CoevolutionProject project = new CoevolutionProject(parentFolder.appendSegment(name), name);
      reports.add(synchronize(project, model, handler, monitor));
    }
    return reports;
  }

  /** Re-reads the UML source of an already imported project and synchronizes it. */
  public SyncReport synchronize(CoevolutionProject project, IConflictHandler handler, IProgressMonitor monitor)
      throws IOException {
    if (!project.isLinked()) {
      throw new IOException("Le projet " + project.getName() + " n'est pas issu d'un import UML");
    }
    Resource uml = UmlSources.load(project.getSourceUri());
    Model model = UmlSources.findModel(uml, project.getSourceModelId());
    if (model == null) {
      throw new IOException("Le modèle " + project.getSourceModelId() + " n'existe plus dans "
          + project.getSourceUri());
    }
    return synchronize(project, model, handler, monitor);
  }

  /**
   * Imports (first time) or synchronizes (next times) the given project with the given UML model.
   */
  public SyncReport synchronize(CoevolutionProject project, Model umlModel, IConflictHandler handler,
      IProgressMonitor monitor) throws IOException {
    if (monitor == null) {
      monitor = new NullProgressMonitor();
    }
    SyncReport report = new SyncReport(project.getName(), umlModel.getName());
    boolean linked = project.isLinked();

    ResourceSet rs = CoevolutionProject.newResourceSet();
    Resource modelRes = linked ? rs.getResource(project.getModelUri(), true) : rs.createResource(project.getModelUri());
    Resource traceRes = linked ? rs.getResource(project.getTraceUri(), true) : rs.createResource(project.getTraceUri());
    Trace existingTrace = linked ? (Trace) traceRes.getContents().get(0) : null;

    // Common ancestor = result of the previous import (separate resource set: independent copy)
    IEditableModelScope ancestor = null;
    IBridgeTrace ancestorTrace = null;
    if (linked && project.exists(project.getBaselineUri()) && project.exists(project.getBaselineTraceUri())) {
      ResourceSet baseRs = CoevolutionProject.newResourceSet();
      Resource baseRes = baseRs.getResource(project.getBaselineUri(), true);
      Resource baseTraceRes = baseRs.getResource(project.getBaselineTraceUri(), true);
      ancestor = new RootedModelScope(new ArrayList<>(baseRes.getContents()));
      ancestorTrace = (Trace) baseTraceRes.getContents().get(0);
    }

    // The project scope operates directly on the resource content
    IEditableModelScope target = new RootedModelScope(modelRes.getContents(), true);
    SysarchIncrementalBridge bridge = new SysarchIncrementalBridge(
        new UmlToSysarchBridge(), ancestor, ancestorTrace, handler);
    IIncrementalBridgeExecution execution = bridge.executeOn(umlModel, target, null, existingTrace, false, monitor);

    if (!bridge.getMergeStatus().isOK()) {
      report.setChanges(bridge.getChanges());
      report.setOutcome(SyncReport.Outcome.CANCELLED);
      return report;
    }

    Trace executionTrace = (Trace) execution.getTrace();
    Collection<EObject> baseline;
    Trace baselineTrace;
    if (execution.isActuallyIncremental()) {
      // Project trace was updated in place (added/removed elements); new baseline = fresh result
      baseline = bridge.getNewBaseline();
      baselineTrace = executionTrace;
      report.setChanges(bridge.getChanges());
      boolean umlSideChanged = !report.getUmlChanges().isEmpty() || !report.getConflicts().isEmpty();
      report.setOutcome(umlSideChanged ? SyncReport.Outcome.UPDATED : SyncReport.Outcome.UP_TO_DATE);
    } else {
      // First import: the transformation wrote directly into the project
      traceRes.getContents().clear();
      traceRes.getContents().add(executionTrace);
      baseline = EcoreUtil.copyAll(modelRes.getContents());
      baselineTrace = EcoreUtil.copy(executionTrace);
      report.setOutcome(SyncReport.Outcome.CREATED);
      int count = 0;
      for (var it = modelRes.getAllContents(); it.hasNext(); it.next()) {
        count++;
      }
      report.setCreatedElements(count);
    }

    // Persist project data, project trace, baseline and baseline trace
    ResourceSet outRs = CoevolutionProject.newResourceSet();
    Resource baseOut = outRs.createResource(project.getBaselineUri());
    baseOut.getContents().addAll(baseline);
    Resource baseTraceOut = outRs.createResource(project.getBaselineTraceUri());
    baseTraceOut.getContents().add(baselineTrace);

    modelRes.save(CoevolutionProject.saveOptions());
    traceRes.save(CoevolutionProject.saveOptions());
    baseOut.save(CoevolutionProject.saveOptions());
    baseTraceOut.save(CoevolutionProject.saveOptions());
    project.writeLink(umlModel.eResource().getURI(), UmlSources.idOf(umlModel), umlModel.getName());
    return report;
  }
}
