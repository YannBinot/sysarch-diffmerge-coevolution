# Choix de conception : pourquoi pas `BridgeJob` ?

EMF DiffMerge fournit, dans le bundle `org.eclipse.emf.diffmerge.bridge.interactive`, une classe prête à l'emploi pour exécuter un bridge depuis l'IDE : `BridgeJob`. Le prototype **ne l'utilise pas**. Ce document explique pourquoi, et décrit ce qui la remplace.

Il s'agit d'un **choix de conception**, pas d'une impossibilité technique : la section 5 décrit comment revenir à `BridgeJob` si besoin.

---

## 1. Ce que fait `BridgeJob` (DiffMerge 0.14.1)

`BridgeJob<SD>` est un `Job` Eclipse abstrait. On lui donne la donnée source et l'URI du modèle cible ; il enchaîne :

| Étape | Méthode | Comportement par défaut |
|---|---|---|
| Charger ou créer la cible | `getCreateTargetResource(uri)` | ressource dans un `ResourceSet` propre au job |
| Charger ou créer la trace | `getTraceURI()`, `getCreateTraceResource(uri)` | **une** trace, à côté de la cible (`<cible>.<ext>.bridgetraces`) |
| Construire le scope cible | `getTargetScope(resource)` | `FragmentedModelScope` |
| Exécuter le bridge | `getBridge()` (abstraite), `handleMainPart(...)` | `bridge.executeOn(...)` dans une commande de l'`EditingDomain` |
| Partie interactive | `handleInteractivePart(...)` | `bridge.mergeInteractively(...)` |
| Sauvegarder | `saveAndClose(...)` | sauve la cible et la trace, puis ferme les ressources |

`getBridge()` doit renvoyer un **`EMFInteractiveBridge`**, sous-classe d'`EMFIncrementalBridge` qui ouvre l'`UpdateDialog` dès qu'il reste des différences à traiter.

---

## 2. Pourquoi il ne convient pas tel quel

### 2.1 Il est conçu pour un bridge 2-way

`EMFInteractiveBridge`, comme `EMFIncrementalBridge`, compare **deux** versions : le résultat frais de la transformation (REFERENCE) et la cible existante (TARGET). Il ne sait donc pas dire **qui** a changé :

| Situation | 2-way (DiffMerge natif) | 3-way (prototype) |
|---|---|---|
| L'UML a changé | différence | changement UML → fusionné automatiquement |
| Le projet a changé | différence (même chose) | changement projet → conservé |
| Les deux ont changé | différence (même chose) | **conflit** → gestionnaire de conflits |

Le cœur du prototype est cette distinction. Elle exige un troisième rôle, ANCESTOR, tenu par la **baseline** : le résultat de la transformation lors du dernier import. `SysarchIncrementalBridge` l'ajoute en redéfinissant `compare(...)`.

### 2.2 Il ne gère qu'une cible et une trace

L'état de co-évolution d'un projet comporte **quatre** fichiers :

```
<projet>/
  <projet>.sysarch                 données du projet (TARGET)
  .coevolution/
    link.properties                fichier UML source + id du modèle
    trace.bridgetraces             trace du projet
    baseline.sysarch               dernier résultat de la transformation (ANCESTOR)
    baseline.bridgetraces          trace de la baseline
```

`BridgeJob` ne connaît que la cible et sa trace. Il faudrait redéfinir `getTraceURI`, `getCreateTargetResource` et surtout `saveAndClose` pour charger et sauver la baseline, avec une règle importante : **rien n'est écrit si l'utilisateur annule la résolution des conflits**. Au final, presque tout le job aurait été réécrit.

### 2.3 Le cœur doit être testable sans interface

`BridgeJob` vit dans un bundle d'interface (dépendances `org.eclipse.ui`, `dm.ui`). Or la logique de co-évolution (3-way, classement des changements, conflits, persistance) doit pouvoir s'exécuter **sans interface**, pour être testée automatiquement par `mvn verify`. Elle se trouve dans le bundle `com.example.sysarch.coevolution`, qui ne dépend d'aucune interface graphique.

### 2.4 Un fichier UML donne plusieurs projets

Un fichier UML contenant N modèles produit N projets. Le prototype les traite en une seule passe, avec une seule barre de progression et **un rapport consolidé** (console + dialogue). `BridgeJob` correspond à un job par cible, chacun avec son propre dialogue.

### 2.5 Le dialogue ne doit s'ouvrir qu'en cas de conflit

Avec `EMFInteractiveBridge`, l'utilisateur voit le dialogue de fusion dès qu'il reste des différences. Dans le prototype, les changements UML sont fusionnés automatiquement et les changements du projet sont conservés : l'utilisateur n'est sollicité que pour les **vrais conflits**.

---

## 3. Ce qui le remplace

Le rôle de `BridgeJob` est réparti entre quatre classes, séparées en un cœur sans interface et une couche d'interface.

```
 Couche UI (com.example.sysarch.coevolution.ui)
 ┌──────────────────────────────────────────────────────────────┐
 │ CoevolutionRunner      ≈ BridgeJob.run()                     │
 │   sauve les éditeurs, ProgressMonitorDialog, N projets,      │
 │   refreshLocal, rapport consolidé                            │
 │ UiConflictHandler      ≈ partie interactive d'EMFInteractive-│
 │   Bridge : choix de stratégie, puis UpdateDialog si demandé  │
 └──────────────────────────────┬───────────────────────────────┘
                                │ IConflictHandler
 Cœur sans UI (com.example.sysarch.coevolution)
 ┌──────────────────────────────▼───────────────────────────────┐
 │ CoevolutionService     ≈ BridgeJob (chargement, exécution,   │
 │   sauvegarde) + gestion de la baseline                       │
 │ SysarchIncrementalBridge ≈ EMFInteractiveBridge, en 3-way    │
 └──────────────────────────────────────────────────────────────┘
```

### Correspondance détaillée

| `BridgeJob` / `EMFInteractiveBridge` | Remplacé par | Différence |
|---|---|---|
| `BridgeJob.run()` (Job Eclipse) | `CoevolutionRunner.run(shell, title, tasks)` | `ProgressMonitorDialog` sur N projets ; sauvegarde préalable des éditeurs ouverts ; rafraîchissement du workspace ; rapport consolidé |
| `getCreateTargetResource`, `getCreateTraceResource`, `getTraceURI` | `CoevolutionService.synchronize(...)` + `CoevolutionProject` (emplacements des fichiers) | charge aussi la baseline et sa trace, dans un **resource set séparé** |
| `getTargetScope` (`FragmentedModelScope`) | `new RootedModelScope(resource.getContents(), true)` | opère directement sur le contenu de la ressource du projet |
| `handleMainPart` → `executeOn(...)` | `bridge.executeOn(umlModel, target, null, existingTrace, false, monitor)` | identique, hors `EditingDomain` (le service travaille sur des fichiers, pas sur un éditeur ouvert) |
| `EMFInteractiveBridge` (2-way) | `SysarchIncrementalBridge extends EMFIncrementalBridge` | `compare(...)` 3-way avec ANCESTOR = baseline ; `mergeAutomatically` classe chaque changement (UML / projet / conflit) ; `isAlwaysInteractive() = false` |
| `handleInteractivePart` → `handleInteractiveMerge` → `UpdateDialog` | `SysarchIncrementalBridge.mergeInteractively` → `IConflictHandler.resolve(...)` | appelé seulement s'il reste des conflits ; stratégies `PROJECT_WINS`, `UML_WINS`, `ABORT` ou `UiConflictHandler` |
| `createDiffNode` + `createMergeDialog` | `UiConflictHandler.createDiffMergeDialog(...)` | même configuration d'`EMFDiffNode` qu'`EMFInteractiveBridge` (UML à gauche, projet à droite), dialogue `UpdateDialog` sans bouton « ouvrir dans un éditeur » |
| `saveAndClose` | fin de `CoevolutionService.synchronize(...)` | sauve projet, trace, baseline, trace de baseline et `link.properties` ; **rien** si annulation |

Ce qui est **réutilisé tel quel** de DiffMerge : `EMFMappingBridge` (transformation), `EMFIncrementalBridge` (mécanique incrémentale, traces, `handleMergedDifferences`), le modèle de trace `bridgetraces`, `EComparisonImpl`, `EMFDiffNode` et `UpdateDialog`.

---

## 4. Ce que l'on perd

- **Exécution en tâche de fond** : `BridgeJob` tourne dans la vue Progress, alors que `CoevolutionRunner` utilise un dialogue de progression modal. C'est volontaire (on évite toute modification des projets pendant la synchronisation), mais moins souple.
- **Bascule vers l'éditeur de comparaison** (`openInEditor`, bouton de l'`UpdateDialog`) : retirée, car le résultat est appliqué par la synchronisation elle-même.
- **Intégration à l'`EditingDomain`** : la fusion n'est pas une commande annulable dans un éditeur ouvert. Les éditeurs sont sauvegardés avant la synchronisation, puis le workspace est rafraîchi.

---

## 5. Revenir à `BridgeJob` si besoin

Si l'on souhaite le comportement standard de DiffMerge, une refonte est possible sans perdre le 3-way :

1. `SysarchIncrementalBridge` hérite d'`EMFInteractiveBridge` au lieu d'`EMFIncrementalBridge`, en gardant ses redéfinitions (`compare`, `mergeAutomatically`, `mergeInteractively`, `isAlwaysInteractive`).
2. Un `SysarchBridgeJob extends BridgeJob<Model>` redéfinit :
   - `getBridge()` : construit le bridge avec la baseline et sa trace ;
   - `getTraceURI()` : `.coevolution/trace.bridgetraces` ;
   - `getTargetScope(...)` : `RootedModelScope` ;
   - `saveAndClose(...)` : sauve aussi la baseline, sa trace et `link.properties`, et ne sauve rien si le statut de fusion est CANCEL.
3. `CoevolutionService` reste le point d'entrée sans interface, utilisé par les tests.

Voir aussi : [Guide développeur](GUIDE_IMPORTEUR.md), [Référence API EMF DiffMerge](API_DIFFMERGE.md).
