package com.example.sysarch.coevolution;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.example.sysarch.coevolution.policies.ChangeOrigin;

/**
 * Outcome of an import / synchronization of one project with its UML model.
 */
public class SyncReport {

  /** Final state of the operation. */
  public enum Outcome { CREATED, UPDATED, UP_TO_DATE, CANCELLED }

  private final String projectName;
  private final String sourceModelName;
  private Outcome outcome;
  private int createdElements;
  private final List<Change> umlChanges = new ArrayList<>();
  private final List<Change> projectChanges = new ArrayList<>();
  private final List<Change> conflicts = new ArrayList<>();

  public SyncReport(String projectName, String sourceModelName) {
    this.projectName = projectName;
    this.sourceModelName = sourceModelName;
  }

  void setOutcome(Outcome outcome) {
    this.outcome = outcome;
  }

  void setCreatedElements(int n) {
    this.createdElements = n;
  }

  void setChanges(List<Change> changes) {
    umlChanges.clear();
    projectChanges.clear();
    conflicts.clear();
    for (Change c : changes) {
      switch (c.getOrigin()) {
        case UML: umlChanges.add(c); break;
        case PROJECT: projectChanges.add(c); break;
        default: conflicts.add(c);
      }
    }
  }

  public String getProjectName() {
    return projectName;
  }

  public String getSourceModelName() {
    return sourceModelName;
  }

  public Outcome getOutcome() {
    return outcome;
  }

  public int getCreatedElements() {
    return createdElements;
  }

  /** Changes made in UML since the last import, applied automatically. */
  public List<Change> getUmlChanges() {
    return umlChanges;
  }

  /** Local changes of the project, preserved. */
  public List<Change> getProjectChanges() {
    return projectChanges;
  }

  /** Concurrent changes on the same information. */
  public List<Change> getConflicts() {
    return conflicts;
  }

  /** Conflicts finally resolved in favour of UML (merged into the project). */
  public List<Change> getConflictsResolvedWithUml() {
    return conflicts.stream().filter(Change::isMerged).collect(Collectors.toList());
  }

  /** Conflicts for which the project version was kept. */
  public List<Change> getConflictsKeptInProject() {
    return conflicts.stream().filter(c -> !c.isMerged()).collect(Collectors.toList());
  }

  public long count(ChangeOrigin origin) {
    switch (origin) {
      case UML: return umlChanges.size();
      case PROJECT: return projectChanges.size();
      default: return conflicts.size();
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Projet '").append(projectName).append("' <- modèle UML '").append(sourceModelName).append("' : ");
    switch (outcome) {
      case CREATED:
        sb.append("créé (").append(createdElements).append(" éléments importés)\n");
        return sb.toString();
      case UP_TO_DATE:
        sb.append("déjà à jour\n");
        break;
      case CANCELLED:
        sb.append("ANNULÉ (aucune modification enregistrée)\n");
        break;
      default:
        sb.append("mis à jour\n");
    }
    sb.append("  Changements UML appliqués      : ").append(umlChanges.size()).append('\n');
    sb.append("  Changements projet conservés   : ").append(projectChanges.size()).append('\n');
    sb.append("  Conflits                       : ").append(conflicts.size())
      .append(" (résolus avec l'UML : ").append(getConflictsResolvedWithUml().size())
      .append(", version projet gardée : ").append(getConflictsKeptInProject().size()).append(")\n");
    for (Change c : umlChanges) {
      sb.append("    ").append(c).append('\n');
    }
    for (Change c : projectChanges) {
      sb.append("    ").append(c).append('\n');
    }
    for (Change c : conflicts) {
      sb.append("    ").append(c).append(c.isMerged() ? "  => UML retenu" : "  => projet conservé").append('\n');
    }
    return sb.toString();
  }
}
