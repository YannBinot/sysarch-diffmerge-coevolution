# Guide développeur : écrire un importeur itératif avec EMF DiffMerge

Ce guide explique comment reproduire le comportement du prototype Sysarch pour **n'importe quel couple source → cible** :

- un fichier externe (UML ou autre) qui contient plusieurs modèles est importé en **plusieurs projets** ;
- les imports suivants sont **itératifs** :
  - les changements faits dans la source sont appliqués ;
  - les changements faits dans les projets sont conservés ;
  - les conflits sont présentés à l'utilisateur.

Voir aussi : la [référence détaillée des classes DiffMerge](API_DIFFMERGE.md) et le [guide utilisateur illustré](GUIDE_UTILISATEUR.md).

Le code de référence se trouve dans `bundles/com.example.sysarch.coevolution` (cœur, sans UI) et dans `bundles/com.example.sysarch.coevolution.ui` (UI).

---

## 1. Concepts

### 1.1 Les trois versions de chaque import

| Rôle DiffMerge | Contenu | Où il vit |
|---|---|---|
| **REFERENCE** | résultat **frais** de la transformation de la source actuelle | en mémoire (scope intermédiaire) |
| **TARGET** | contenu **actuel** du projet, éventuellement modifié par l'utilisateur | `<projet>/<projet>.sysarch` |
| **ANCESTOR** | résultat de la transformation lors du **dernier import** (*baseline*) | `<projet>/.coevolution/baseline.sysarch` |

En comparant ces trois versions, on sait **qui** a modifié chaque information depuis le dernier import :

| Source a changé ? | Projet a changé ? | Décision |
|---|---|---|
| oui | non | appliquer au projet (automatique) |
| non | oui | conserver le projet (automatique) |
| oui | oui (différemment) | **conflit** : l'utilisateur décide |

> ⚠️ Le bridge incrémental fourni par DiffMerge (`EMFIncrementalBridge`) ne compare que deux versions, REFERENCE et TARGET. Sans ancêtre, il ne sait pas d'où vient une différence. Il faut donc l'étendre (section 4.3).

### 1.2 Les traces : la mémoire de l'appariement

L'appariement entre éléments ne se fait **pas** par nom. Chaque élément cible est associé à sa **cause** :

```
cause = Source[<symbole de l'élément source>]_Rule[<id de la règle>]
```

- Le symbole d'un élément source EMF est son ID (`EcoreUtil.getID`) ou, à défaut, son fragment d'URI. Pour UML, c'est le `xmi:id`.
- Une trace (`org.eclipse.emf.diffmerge.bridge.traces.gen…Trace`) associe *symbole de l'élément cible → cause*. Le symbole cible est l'ID EMF de l'élément cible.
- Deux éléments de deux versions différentes sont appariés **si et seulement s'ils ont la même cause**.

Il existe une trace pour chaque version :
- **REFERENCE** : la trace de l'exécution courante, créée par le bridge ;
- **TARGET** : `.coevolution/trace.bridgetraces`, la trace du projet, maintenue au fil des imports ;
- **ANCESTOR** : `.coevolution/baseline.bridgetraces`.

**Conséquences pratiques :**
- les **ids des règles** et les **ids des éléments source** font partie du format persistant : ne les changez pas d'une version à l'autre ;
- les éléments créés à la main dans le projet n'ont pas de cause, donc pas d'appariement : ils sont vus comme des ajouts du projet et conservés.

---

## 2. Plateforme cible

| Composant | Version | Dépôt p2 |
|---|---|---|
| Eclipse Platform / EMF / UML2 / EMF Transaction | 2024-12 | `https://download.eclipse.org/releases/2024-12/202412041000/` |
| EMF DiffMerge (core + UI) | 0.14.1 | `https://download.eclipse.org/diffmerge/releases/0.14.1/emf-diffmerge-site/` |
| DiffMerge co-evolution (bridge) | 0.14.1 | `https://download.eclipse.org/diffmerge/releases/0.14.1/edm-coevolution-site/` |
| `org.apache.log4j` 1.2.24 (requis par le bridge) | Orbit | `https://download.eclipse.org/tools/orbit/downloads/drops/R20230302014618/repository` |

Bundles DiffMerge utilisés :

| Bundle | Rôle |
|---|---|
| `org.eclipse.emf.diffmerge` | comparaison et fusion EMF |
| `org.eclipse.emf.diffmerge.generic` | API générique : IComparison, IDifference, Role… |
| `org.eclipse.emf.diffmerge.bridge` | API bridge, traces, causes |
| `org.eclipse.emf.diffmerge.bridge.mapping` | écriture de la transformation (queries + rules) |
| `org.eclipse.emf.diffmerge.bridge.incremental` | `EMFIncrementalBridge` |
| `org.eclipse.emf.diffmerge.bridge.traces.gen` | modèle EMF des traces (persistable) |
| `org.eclipse.emf.diffmerge.ui` + `bridge.interactive` | UI de comparaison (`EMFDiffNode`, `UpdateDialog`) |

Pièges rencontrés :
- **Tycho exige le même `includeMode` pour toutes les localisations.** Mettez-les toutes en `planner`.
- **N'incluez pas `org.eclipse.emf.diffmerge.bridge.log4j`.** Il exige un bundle `org.apache.log4j1.2.15…` introuvable. Conséquence sans gravité : un message `log4j:ERROR … BridgeLoggingAppender` au démarrage.
- **Lanceur natif.** Ajoutez `org.eclipse.equinox.executable.feature.group` à la cible pour pouvoir produire un exécutable.

---

## 3. Préparer le métamodèle cible

1. **Donnez un ID EMF à toutes les classes**, par exemple un attribut `id` avec `iD="true"` sur une super-classe abstraite. Le symbole cible des traces repose dessus.
2. **Générez l'ID à la création**, en modifiant le constructeur généré et en le marquant `@generated NOT` :
   ```java
   protected NamedElementImpl() {
     super();
     id = EcoreUtil.generateUUID();
   }
   ```
3. **Identifiez les données propres au projet**, celles que la source ne produit jamais (dans le prototype : `Component.status` et `Component.notes`). Elles seront exclues de la comparaison (section 4.2).

---

## 4. Le cœur (bundle sans UI)

### 4.1 La transformation : `EMFMappingBridge` + `QueryAndRule`

Fichier de référence : `bridge/UmlToSysarchBridge.java`.

```java
public class UmlToSysarchBridge extends EMFMappingBridge<Model, IEditableModelScope> {
  public UmlToSysarchBridge() {
    // Racine : Model -> SystemModel
    modelRule = new QueryAndRule<Model, Model, SystemModel>(this, "SystemModel") {
      public Iterable<Model> evaluate(Model m, IQueryExecution qe) { return Collections.singleton(m); }
      public SystemModel createTarget(Model m, IQueryExecution qe) { return F.createSystemModel(); }
      public void defineTarget(Model m, SystemModel t, IQueryExecution qe, IMappingExecution me) {
        t.setName(m.getName());
        IEditableModelScope scope = me.getTargetDataSet();
        scope.add(t);                         // les racines s'ajoutent au scope cible
      }
    };
    // Sous-requête : les attributs d'une classe
    attributeRule = new QueryAndRule<Class, Property, Attribute>(componentRule, "Attribute") { ... };
  }
}
```

Règles à respecter :
- **Deux phases.** `createTarget` ne fait qu'instancier. Tous les liens (containment, références) se posent dans `defineTarget`, une fois tous les éléments créés.
- **Retrouver la cible d'un élément source** : `me.get(source, autreRegle.getMainRule())`. Le résultat est `null` si l'élément n'a pas été transformé, par exemple s'il appartient à un autre modèle : gérez ce cas.
- **Le parent d'un élément** : récupérez-le via `me.get(owner, ruleDuParent)` ou via `qe.get(requeteParente)`.
- **Racines** : ajoutez-les avec `((IEditableModelScope) me.getTargetDataSet()).add(root)`.
- **Ids des règles** (`"SystemModel"`, `"Attribute"`…) : ils sont stables et persistés dans les traces.
- **Une même source peut produire plusieurs cibles via plusieurs règles.** Exemple : `DefaultPackage` crée un package pour les éléments posés directement à la racine du modèle.
- **Délimitation des modèles.** Les requêtes parcourent les contenus du modèle en **élaguant** les modèles imbriqués, car chacun devient un autre projet (voir `UmlHelper.contents`).

### 4.2 Les politiques DiffMerge

**Politique de match** : `policies/TraceBasedThreeWayMatchPolicy`.

```java
public Object getMatchID(EObject e, ITreeDataScope<EObject> scope) {
  IBridgeTrace t = scope == createdScope ? createdTrace
                 : scope == ancestorScope ? ancestorTrace
                 : existingTrace;
  return t == null ? null : t.getCause(e);
}
```

**Politique de diff** : `policies/SysarchDiffPolicy`, qui hérite de `DefaultDiffPolicy`.
- `coverFeature(f)` renvoie `false` pour l'ID technique et les **données propres au projet**.
- `considerOrderedFeature(f)` renvoie `false` : l'ordre de la source n'est pas significatif.

**Origine d'une différence** : `policies/ChangeOrigin`.

```java
if (d.isConflicting()) return CONFLICT;
Role presence = ((IPresenceDifference<?>) d).getPresenceRole();
Role origin   = d.isAlignedWithAncestor() ? presence.opposite() : presence;
return origin == Role.REFERENCE ? SOURCE : PROJECT;
```

`isAlignedWithAncestor()` vaut vrai si l'ancêtre possède la même valeur que le côté où la valeur est présente. Le changement a donc eu lieu **de l'autre côté**.

**Sélecteur de merge automatique** : `policies/UmlChangesMergeSelector`. Il renvoie `Role.TARGET` (fusionner dans le projet) seulement si l'origine est la source, et `null` sinon.

### 4.3 Le bridge incrémental 3-way

Fichier de référence : `SysarchIncrementalBridge.java`, qui hérite de `EMFIncrementalBridge<SD, IEditableModelScope>`.

| Méthode redéfinie | Rôle |
|---|---|
| constructeur | `super(transfo, new MaDiffPolicy(), null, new MonMergeSelector())`, puis mémorise l'ancêtre et sa trace |
| `intermediateDataSetFilled(created)` | **copie** le résultat frais (`EcoreUtil.copyAll`) avant toute fusion : c'est la prochaine baseline |
| `compare(...)` | `new EComparisonImpl(existing, created, ancestor)` + `compute(matchPolicy3Way, diffPolicy, mergePolicy, monitor)` |
| `mergeAutomatically(...)` | classe les différences (pour le rapport) **avant** d'appeler `super`, qui applique le sélecteur |
| `isAlwaysInteractive()` | `false` |
| `mergeInteractively(...)` | s'il reste des conflits non fusionnés, appelle le gestionnaire de conflits. S'il répond OK, appelle `handleMergedDifferences(...)`, qui met à jour la trace du projet pour les éléments ajoutés ou supprimés. Mémorise le statut (CANCEL = ne rien sauver). |

### 4.4 L'orchestration : `CoevolutionService.synchronize(project, model, handler, monitor)`

Cette orchestration remplace le `BridgeJob` de DiffMerge, qui ne gère ni le 3-way ni la baseline. Voir [Choix de conception : pourquoi pas `BridgeJob`](CHOIX_BRIDGEJOB.md).

```
1. Charger : <P>.sysarch (TARGET), trace.bridgetraces, baseline.sysarch + baseline.bridgetraces (ANCESTOR)
   (premier import : ressources vides, pas de trace, pas d'ancêtre)
2. target = new RootedModelScope(modelResource.getContents(), true)  // opère directement sur la ressource
3. bridge.executeOn(sourceModel, target, null, projectTrace, false, monitor)
     - pas de trace projet ou cible vide  -> exécution directe dans la cible (premier import)
     - sinon -> transfo dans un scope intermédiaire, puis compare / merge (notre 3-way)
4. Si le gestionnaire de conflits a annulé : RIEN n'est sauvé
5. Premier import : trace projet = trace d'exécution ; baseline = copie du projet + copie de la trace
   Import suivant : la trace projet a été mise à jour en place ; baseline = getNewBaseline(),
                    trace baseline = trace d'exécution
6. Sauver : projet, trace projet, baseline, trace baseline, link.properties (source + id du modèle)
```

Points d'attention :
- **Resource set.** Enregistrez explicitement les fabriques XMI et les packages, y compris `BridgetracesPackage`, pour fonctionner aussi en test headless (voir `CoevolutionProject.newResourceSet()`).
- **Ancêtre isolé.** Chargez la baseline dans **un autre resource set** que le projet.
- **Plusieurs modèles par fichier.** `UmlSources.findModels` renvoie les modèles racines et imbriqués. On appelle `synchronize` une fois par modèle, avec un projet par modèle.
- **Identification persistante du modèle source** : `resource.getURIFragment(model)`, stocké dans `link.properties` pour les resynchronisations.

### 4.5 Le gestionnaire de conflits : `IConflictHandler`

```java
IStatus resolve(EComparison comparison, List<Change> conflicts, IProgressMonitor monitor);
```

Le gestionnaire fusionne lui-même ce qu'il veut vers `Role.TARGET`, puis renvoie OK, ou CANCEL pour tout annuler. Stratégies fournies :
- `PROJECT_WINS` : ne rien faire ;
- `UML_WINS` : `comparison.merge(d -> d.isConflicting() ? Role.TARGET : null, true, monitor)` ;
- `ABORT` : renvoyer `CANCEL_STATUS` ;
- UI : `UiConflictHandler`, décrit en section 5.

---

## 5. L'UI

| Classe | Rôle |
|---|---|
| `ImportUmlWizard` | choix du fichier, une ligne par modèle : nouveau projet, synchronisation d'un projet existant, ou projet existant non lié (ignoré) |
| `SynchronizeHandler` | resynchronise les projets sélectionnés, ou tous les projets liés si rien n'est sélectionné |
| `CoevolutionRunner` | sauve les éditeurs ouverts, exécute dans un `ProgressMonitorDialog` (thread séparé), fait `refreshLocal`, puis affiche le rapport (console + dialogue) |
| `UiConflictHandler` | demande la stratégie ; « Résoudre dans DiffMerge » ouvre le `UpdateDialog` du bridge interactif |

Configuration du dialogue DiffMerge (identique à `EMFInteractiveBridge`) :

```java
EMFDiffNode node = new EMFDiffNode(comparison, null, true, false);
node.setReferenceRole(Role.TARGET);
node.setDrivingRole(Role.TARGET);
node.setLeftRole(Role.REFERENCE);     // source à gauche, projet à droite
new UpdateDialog(shell, title, node) {
  @Override protected Button createOpenEditorButton(Composite p) { return null; }
}.open();
```

Le gestionnaire est appelé depuis un thread de travail : les dialogues passent par `Display.syncExec`.

**Écriture des fichiers.** Le service écrit avec des URI `file:` (`project.getLocation()`), puis le workspace est rafraîchi. On évite ainsi de garder le verrou du workspace pendant que les dialogues sont ouverts.

**Comportement actuel.** Un dialogue s'ouvre **par projet en conflit**. Pour une vue d'ensemble, une évolution possible est un récapitulatif global des conflits de tous les projets, avec DiffMerge ouvert seulement là où l'utilisateur le demande.

---

## 6. Tester

`tests/com.example.sysarch.coevolution.tests` (tycho-surefire, headless) rejoue le scénario sur `samples/vehicle-v1.uml`, puis `vehicle-v2.uml` :

1. Un premier import crée un projet par modèle, modèle imbriqué compris.
2. Un ré-import sans changement donne `UP_TO_DATE`.
3. On modifie le projet, on passe à la v2, puis on synchronise avec `PROJECT_WINS` :
   - les changements de la source sont appliqués ;
   - les changements du projet sont conservés ;
   - les deux conflits attendus sont détectés : modification/modification, et suppression/modification.
4. Une resynchronisation donne `UP_TO_DATE`, sans conflit.
5. Avec `UML_WINS`, les conflits prennent la valeur de la source, et les données propres au projet restent intactes.
6. Avec `ABORT`, le fichier du projet est identique octet pour octet.

**Conseil.** Développez et validez d'abord le cœur avec ce type de test headless, avant toute UI.

---

## 7. Checklist pour un nouvel importeur

- [ ] Métamodèle cible : ID EMF généré à la création, liste des données propres au projet.
- [ ] Source : identifiants persistants sur les éléments (`xmi:id` ou équivalent).
- [ ] Transformation `EMFMappingBridge` : ids de règles stables, liens posés dans `defineTarget`, racines ajoutées au scope.
- [ ] `DiffPolicy` : exclure l'ID et les données propres au projet, ignorer l'ordre si pertinent.
- [ ] Match policy 3-way fondée sur les traces.
- [ ] Sous-classe de `EMFIncrementalBridge` : snapshot de la baseline, `compare` en 3-way, sélecteur « source seulement », gestionnaire de conflits.
- [ ] Persistance par projet : données, trace, baseline, trace de baseline, lien vers la source.
- [ ] Annulation : ne rien écrire si le gestionnaire renvoie CANCEL.
- [ ] Tests headless du scénario itératif complet.

## 8. Limites connues

- Les éléments source **sans identifiant persistant** sont appariés via leur chemin : un réordonnancement casse l'appariement.
- Une **référence vers un élément d'un autre modèle** (donc d'un autre projet) n'est pas reliée : seul un nom est copié.
- Un conflit non résolu, quand l'utilisateur garde la version du projet, n'est **plus signalé** aux imports suivants. La baseline avance, et la valeur du projet devient alors un simple changement local.
