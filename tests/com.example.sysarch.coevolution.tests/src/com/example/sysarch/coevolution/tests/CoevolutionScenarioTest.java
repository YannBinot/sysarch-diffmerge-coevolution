package com.example.sysarch.coevolution.tests;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.example.sysarch.Attribute;
import com.example.sysarch.Component;
import com.example.sysarch.ComponentStatus;
import com.example.sysarch.Enumeration;
import com.example.sysarch.NamedElement;
import com.example.sysarch.SysarchFactory;
import com.example.sysarch.SystemModel;
import com.example.sysarch.coevolution.Change;
import com.example.sysarch.coevolution.CoevolutionProject;
import com.example.sysarch.coevolution.CoevolutionService;
import com.example.sysarch.coevolution.IConflictHandler;
import com.example.sysarch.coevolution.SyncReport;

/**
 * Iterative co-evolution scenario between one external UML file holding three models
 * and the three projects created from it.
 */
public class CoevolutionScenarioTest {

  private static final String PROJECT_DESCRIPTION = "Moteur thermique (description du projet)";
  private static final String UML_V2_DESCRIPTION = "Combustion engine - Euro 7 compliant";

  private File work;
  private File umlFile;
  private URI umlUri;
  private URI workspace;
  private final CoevolutionService service = new CoevolutionService();

  @Before
  public void setUp() throws IOException {
    work = Files.createTempDirectory("coevo").toFile();
    umlFile = new File(work, "external/vehicle.uml");
    umlFile.getParentFile().mkdirs();
    useUmlVersion("vehicle-v1.uml");
    umlUri = URI.createFileURI(umlFile.getAbsolutePath());
    workspace = URI.createFileURI(new File(work, "workspace").getAbsolutePath());
  }

  @After
  public void tearDown() throws IOException {
    try (Stream<java.nio.file.Path> s = Files.walk(work.toPath())) {
      s.sorted(Comparator.reverseOrder()).map(java.nio.file.Path::toFile).forEach(File::delete);
    }
  }

  // ======================================================================= tests

  @Test
  public void firstImportCreatesOneProjectPerModel() throws Exception {
    List<SyncReport> reports = service.importAll(umlUri, workspace, IConflictHandler.ABORT, null);
    print("Import initial", reports);
    assertEquals(3, reports.size());
    assertEquals(List.of("VehicleSystem", "Telemetry", "ChargingInfrastructure"),
        reports.stream().map(SyncReport::getProjectName).collect(Collectors.toList()));
    reports.forEach(r -> assertEquals(SyncReport.Outcome.CREATED, r.getOutcome()));

    SystemModel vehicle = load("VehicleSystem");
    assertEquals("VehicleSystem", vehicle.getName());
    assertEquals(java.util.Set.of("powertrain", "chassis", "VehicleSystem"),
        vehicle.getPackages().stream().map(NamedElement::getName).collect(Collectors.toSet()));
    Component engine = find(vehicle, Component.class, "Engine");
    assertEquals("Combustion engine", engine.getDescription());
    assertEquals(2, engine.getAttributes().size());
    assertEquals("Boolean", find(engine, com.example.sysarch.Operation.class, "start").getTypeName());
    assertEquals("IStartable", engine.getProvidedInterfaces().get(0).getName());
    Component electric = find(vehicle, Component.class, "ElectricEngine");
    assertEquals(engine, electric.getSuperComponents().get(0));
    Attribute fuel = find(engine, Attribute.class, "fuel");
    assertEquals(find(vehicle, Enumeration.class, "FuelType"), fuel.getType());
    // Nested model -> separate project, its content is not in the parent project
    assertNull(findOrNull(vehicle, Component.class, "Sensor"));
    assertNotNull(find(load("Telemetry"), Component.class, "Sensor"));
    // Cross-package reference inside the same model
    assertEquals(engine, find(find(vehicle, Component.class, "Vehicle"), Attribute.class, "engine").getType());
  }

  @Test
  public void reimportWithoutChangeIsUpToDate() throws Exception {
    service.importAll(umlUri, workspace, IConflictHandler.ABORT, null);
    List<SyncReport> reports = service.importAll(umlUri, workspace, IConflictHandler.ABORT, null);
    print("Ré-import sans modification", reports);
    reports.forEach(r -> assertEquals(SyncReport.Outcome.UP_TO_DATE, r.getOutcome()));
  }

  /** The heart of the prototype: changes on both sides, conflicts kept in favour of the project. */
  @Test
  public void iterativeSynchronizationProjectWins() throws Exception {
    service.importAll(umlUri, workspace, IConflictHandler.ABORT, null);
    editProjectLocally();
    useUmlVersion("vehicle-v2.uml");

    List<SyncReport> reports = service.importAll(umlUri, workspace, IConflictHandler.PROJECT_WINS, null);
    print("Synchronisation v2 (conflits -> projet)", reports);
    SyncReport vehicleReport = reports.get(0);
    assertEquals(SyncReport.Outcome.UPDATED, vehicleReport.getOutcome());
    assertTrue("conflict on Engine.description expected", contains(vehicleReport.getConflicts(), "Engine'.description"));
    assertTrue("delete/modify conflict on Chassis.wheels expected", contains(vehicleReport.getConflicts(), "wheels"));
    assertEquals(2, vehicleReport.getConflicts().size());
    assertTrue(contains(vehicleReport.getProjectChanges(), "RoadWheel"));
    assertTrue(contains(vehicleReport.getProjectChanges(), "Battery"));
    assertTrue(contains(vehicleReport.getUmlChanges(), "EMotor"));

    SystemModel vehicle = load("VehicleSystem");
    Component engine = find(vehicle, Component.class, "Engine");
    // conflict -> project version kept
    assertEquals(PROJECT_DESCRIPTION, engine.getDescription());
    // project-owned data untouched
    assertEquals(ComponentStatus.VALIDATED, engine.getStatus());
    assertEquals("Validé en revue du 12/11", engine.getNotes());
    // project changes kept
    assertNotNull(find(vehicle, Component.class, "RoadWheel"));
    assertNull(findOrNull(vehicle, Component.class, "Wheel"));
    assertNotNull(find(vehicle, Component.class, "Battery"));
    // UML changes applied
    assertNotNull(find(engine, Attribute.class, "torque"));
    assertNotNull(find(vehicle, Component.class, "EMotor"));
    assertNull(findOrNull(vehicle, Component.class, "ElectricEngine"));
    assertEquals(engine, find(vehicle, Component.class, "EMotor").getSuperComponents().get(0));
    assertTrue(find(vehicle, Enumeration.class, "FuelType").getLiterals().contains("HYDROGEN"));
    assertNotNull(find(find(vehicle, com.example.sysarch.Package.class, "electronics"), Component.class, "ECU"));
    // Other projects of the same UML file
    assertNull(findOrNull(load("ChargingInfrastructure"), Component.class, "Connector"));
    assertNotNull(find(load("Telemetry"), Attribute.class, "samplingRate"));

    // Next iteration with the same UML: everything was reconciled, nothing to do, no conflict anymore
    List<SyncReport> again = service.importAll(umlUri, workspace, IConflictHandler.ABORT, null);
    print("Re-synchronisation v2", again);
    again.forEach(r -> assertEquals(r.toString(), SyncReport.Outcome.UP_TO_DATE, r.getOutcome()));
    again.forEach(r -> assertEquals(r.toString(), 0, r.getConflicts().size()));
    again.forEach(r -> assertEquals(r.toString(), 0, r.getUmlChanges().size()));
    assertEquals(PROJECT_DESCRIPTION, find(load("VehicleSystem"), Component.class, "Engine").getDescription());
  }

  @Test
  public void iterativeSynchronizationUmlWins() throws Exception {
    service.importAll(umlUri, workspace, IConflictHandler.ABORT, null);
    editProjectLocally();
    useUmlVersion("vehicle-v2.uml");

    List<SyncReport> reports = service.importAll(umlUri, workspace, IConflictHandler.UML_WINS, null);
    print("Synchronisation v2 (conflits -> UML)", reports);
    assertFalse(reports.get(0).getConflictsResolvedWithUml().isEmpty());

    SystemModel vehicle = load("VehicleSystem");
    Component engine = find(vehicle, Component.class, "Engine");
    assertEquals(UML_V2_DESCRIPTION, engine.getDescription());
    assertEquals(ComponentStatus.VALIDATED, engine.getStatus()); // never overwritten
    assertNotNull(find(vehicle, Component.class, "RoadWheel")); // non conflicting project change
    assertNotNull(find(vehicle, Component.class, "Battery"));
  }

  @Test
  public void cancelledSynchronizationLeavesProjectUntouched() throws Exception {
    service.importAll(umlUri, workspace, IConflictHandler.ABORT, null);
    editProjectLocally();
    useUmlVersion("vehicle-v2.uml");
    File modelFile = new File(new File(work, "workspace/VehicleSystem"), "VehicleSystem.sysarch");
    byte[] before = Files.readAllBytes(modelFile.toPath());

    SyncReport report = service.synchronize(project("VehicleSystem"), IConflictHandler.ABORT, null);
    print("Synchronisation annulée", List.of(report));
    assertEquals(SyncReport.Outcome.CANCELLED, report.getOutcome());
    assertArrayEquals(before, Files.readAllBytes(modelFile.toPath()));
  }

  // ===================================================================== helpers

  /** Local edits of the VehicleSystem project, as a user would do in the editor. */
  private void editProjectLocally() throws IOException {
    CoevolutionProject p = project("VehicleSystem");
    ResourceSet rs = CoevolutionProject.newResourceSet();
    Resource r = rs.getResource(p.getModelUri(), true);
    SystemModel vehicle = (SystemModel) r.getContents().get(0);
    Component engine = find(vehicle, Component.class, "Engine");
    engine.setStatus(ComponentStatus.VALIDATED);                 // project-owned data
    engine.setNotes("Validé en revue du 12/11");                  // project-owned data
    engine.setDescription(PROJECT_DESCRIPTION);                   // will conflict with UML v2
    find(vehicle, Component.class, "Wheel").setName("RoadWheel"); // local rename, UML unchanged
    Component battery = SysarchFactory.eINSTANCE.createComponent(); // local addition
    battery.setName("Battery");
    find(vehicle, com.example.sysarch.Package.class, "powertrain").getTypes().add(battery);
    Attribute wheels = find(find(vehicle, Component.class, "Chassis"), Attribute.class, "wheels");
    EcoreUtil.delete(wheels);                                     // local deletion (UML v2 modifies it)
    r.save(CoevolutionProject.saveOptions());
  }

  private void useUmlVersion(String sample) throws IOException {
    File samples = new File(System.getProperty("samples.dir", "../../samples"));
    Files.copy(new File(samples, sample).toPath(), umlFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
  }

  private CoevolutionProject project(String name) {
    return new CoevolutionProject(workspace.appendSegment(name), name);
  }

  private SystemModel load(String projectName) {
    ResourceSet rs = CoevolutionProject.newResourceSet();
    return (SystemModel) rs.getResource(project(projectName).getModelUri(), true).getContents().get(0);
  }

  private static <T extends NamedElement> T find(EObject root, Class<T> type, String name) {
    T result = findOrNull(root, type, name);
    assertNotNull(type.getSimpleName() + " " + name + " not found", result);
    return result;
  }

  private static <T extends NamedElement> T findOrNull(EObject root, Class<T> type, String name) {
    for (var it = root.eAllContents(); it.hasNext();) {
      EObject o = it.next();
      if (type.isInstance(o) && name.equals(((NamedElement) o).getName())) {
        return type.cast(o);
      }
    }
    return null;
  }

  private static boolean contains(List<Change> changes, String text) {
    return changes.stream().anyMatch(c -> c.getDescription().contains(text));
  }

  private static void print(String title, List<SyncReport> reports) {
    System.out.println("\n===== " + title + " =====");
    reports.forEach(System.out::print);
  }
}
