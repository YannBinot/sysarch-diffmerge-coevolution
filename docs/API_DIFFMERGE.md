# Référence : les classes EMF DiffMerge à utiliser (0.14.1)

L'API d'EMF DiffMerge est peu documentée. Cette page décrit les classes réellement utilisées pour un importeur itératif : **ce qu'elles font**, **comment elles s'enchaînent** et **les pièges rencontrés**.

Les informations viennent de la lecture des sources 0.14.1 (bundles `*.source` des sites de mise à jour) et de leur mise en œuvre dans ce prototype.

Conventions :
- `E` désigne le type des éléments comparés ; pour EMF, c'est `EObject`.
- Les noms de packages sont abrégés : `dm` = `org.eclipse.emf.diffmerge`.

---

## 0. Vue d'ensemble

```
                 ┌──────────────────────────── bridge (transformation + traces) ─────────────────────────────┐
 source (UML) ──►│ EMFMappingBridge  ── queries/rules ──►  cibles + IBridgeTrace (cible → cause)             │
                 │        ▲ enveloppé par                                                                     │
                 │ EMFIncrementalBridge : 1er run = transfo directe ; runs suivants = transfo dans un scope   │
                 │   intermédiaire puis compare() / merge() avec la cible existante                           │
                 └────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                                              │ utilise
                 ┌──────────────────────────── core (comparaison / fusion) ──────────────────────────────────┐
                 │ scopes (IEditableModelScope) ─► EComparisonImpl.compute(matchPolicy, diffPolicy, mergePolicy)│
                 │   ─► IMapping / IMatch ─► IDifference (présences d'éléments / de valeurs)                   │
                 │   ─► comparison.merge(selector) / difference.mergeTo(role)                                  │
                 └────────────────────────────────────────────┬──────────────────────────────────────────────┘
                                                              │ affiché par
                 ┌──────────────────────────── ui ───────────────────────────────────────────────────────────┐
                 │ EMFDiffNode (comparaison + rôles gauche/droite) ─► UpdateDialog / DiffMergeDialog           │
                 └────────────────────────────────────────────────────────────────────────────────────────────┘
```

| Bundle | Contenu principal |
|---|---|
| `dm.generic` | API générique : `IComparison`, `IMatch`, `IMapping`, `IDifference`, `Role`, les interfaces de policies |
| `dm` (core) | Implémentation EMF : `EComparison`, `EComparisonImpl`, scopes, policies par défaut |
| `dm.bridge` | API bridge : `IBridge`, `IBridgeTrace`, `ICause`, symboles |
| `dm.bridge.mapping` | Écriture de transformations : `EMFMappingBridge`, `Query`, `Rule`, `QueryAndRule` |
| `dm.bridge.incremental` | `EMFIncrementalBridge`, `BridgeTraceBasedMatchPolicy`, `IntermediateModelScope` |
| `dm.bridge.traces.gen` | Modèle EMF des traces : `Trace`, `BridgetracesFactory` |
| `dm.ui`, `dm.bridge.interactive` | `EMFDiffNode`, `DiffMergeDialog`, `UpdateDialog`, `EMFInteractiveBridge` |

---

## 1. Rôles : `dm.generic.api.Role`

Enum à trois valeurs. **Tout** est exprimé en fonction de ces rôles.

| Rôle | Dans un importeur |
|---|---|
| `TARGET` | les données existantes, c'est-à-dire le projet. C'est la destination des fusions. |
| `REFERENCE` | le nouveau résultat de la transformation |
| `ANCESTOR` | la version commune, c'est-à-dire le résultat du dernier import. Uniquement en 3-way. |

`role.opposite()` échange TARGET et REFERENCE.

---

## 2. Les scopes : ce que l'on compare

Un *scope* est un ensemble d'éléments, organisé en arbre, que DiffMerge sait parcourir et modifier.

| Classe | Rôle | Quand l'utiliser |
|---|---|---|
| `dm.api.scopes.IModelScope` | scope EMF en lecture : `getContents()` (racines), `getAllContents()` | type de base |
| `dm.api.scopes.IEditableModelScope` | scope modifiable : `add(EObject)` ajoute une racine, ainsi que `add/remove/move` sur des features | type des cibles du bridge |
| `dm.impl.scopes.RootedModelScope(List<EObject> roots)` | scope défini par une liste de racines, **copiée** | ancêtre, résultats intermédiaires |
| `RootedModelScope(List<EObject> roots, true)` | idem, mais opère **directement** sur la liste fournie | cible = `resource.getContents()`. Les racines ajoutées par une fusion vont dans la ressource. |
| `dm.impl.scopes.FragmentedModelScope(Resource, readOnly)` | scope couvrant une ressource et ses fragments | modèles multi-fichiers. Attention : il tente de charger la ressource, ce qui échoue si elle n'existe pas encore. |
| `dm.bridge.incremental.IntermediateModelScope` | `RootedModelScope` créé par le bridge incrémental pour recevoir la transformation rejouée | créé automatiquement par `createIntermediateDataSet` |

> 💡 **Recommandation.** Utilisez `new RootedModelScope(resource.getContents(), true)` pour la cible : cela marche à l'identique pour une ressource vide (premier import) et pour une ressource chargée.

---

## 3. La comparaison

### 3.1 `dm.diffdata.EComparison` / `dm.diffdata.impl.EComparisonImpl`

La comparaison EMF. `EComparison` étend `GComparison`, qui implémente `IComparison<EObject>` (API générique).

```java
EComparison c = new EComparisonImpl(targetScope, referenceScope);                // 2-way
EComparison c = new EComparisonImpl(targetScope, referenceScope, ancestorScope); // 3-way
IStatus s = c.compute(matchPolicy, diffPolicy, mergePolicy, monitor);            // calcule matches + différences
```

Le calcul se fait en deux étapes :
1. **Matching** : la `IMatchPolicy` associe les éléments des 2 ou 3 scopes. Le résultat est un `IMapping` de `IMatch`.
2. **Différences** : pour chaque match, la `IDiffPolicy` décide des features à comparer. Le résultat est un ensemble de `IDifference`.

Méthodes utiles de `IComparison<E>` :

| Méthode | Description |
|---|---|
| `isThreeWay()` | vrai si un ancêtre est fourni |
| `getMapping()` | le `IMapping` : tous les matches |
| `getRemainingDifferences()` | les différences **non fusionnées et non ignorées** |
| `hasRemainingDifferences()` | raccourci |
| `getDifferences(Role)` | différences dont la présence est du côté donné |
| `getScope(Role)` | le scope d'un rôle |
| `merge(IMergeSelector, updateReferences, monitor)` | fusionne chaque différence selon le sélecteur (voir 6) |
| `merge(Collection<IDifference>, Role, updateReferences, monitor)` | fusionne une liste de différences vers un rôle |
| `merge(Role, updateReferences, monitor)` | fusionne **tout** vers un rôle |

`updateReferences = true` : après la fusion, les références qui pointaient vers des éléments de l'autre côté sont rebranchées. Mettez-le toujours à `true`.

### 3.2 `dm.generic.api.IMapping` / `IMatch`

- **`IMapping<E>`** est l'ensemble des matches.
  - `getContents()` : tous les matches.
  - `getMatchFor(element, role)` : le match d'un élément, à partir de son rôle.
- **`IMatch<E>`** est un élément vu dans les 2 ou 3 versions.
  - `get(Role)` : l'élément de ce côté, ou `null` s'il est absent de ce côté.
  - `getElementPresenceDifference()` : la différence « l'élément n'existe que d'un côté », si elle existe.
  - `getAllDifferences()` : toutes les différences portées par l'élément.
  - `isAMove()` : l'élément a changé de conteneur.

---

## 4. Les différences : `dm.generic.api.diff.*`

Toute différence est une **présence** : quelque chose existe d'un côté et pas de l'autre.

| Interface | Signification | Méthodes clés |
|---|---|---|
| `IDifference<E>` | base | `isMerged()`, `getMergeDestination()`, `isIgnored()`, `isConflicting()`, `isAlignedWithAncestor()`, `canMergeTo(Role)` |
| `IPresenceDifference<E>` | présence d'un côté | `getPresenceRole()` : le côté où la chose existe |
| `IElementRelativeDifference<E>` | différence attachée à un élément | `getElementMatch()` |
| `IElementPresence<E>` | **l'élément** n'existe que d'un côté (ajout ou suppression) | `getElement()`, `getOwnerMatch()`, `isRoot()` |
| `IValuePresence<E>` | **une valeur** d'une feature n'existe que d'un côté | `getFeature()`, `getValue()`, `isManyFeature()`, `isOrder()`, `getSymmetrical()` |
| `IAttributeValuePresence<E>` | valeur d'attribut | idem |
| `IReferenceValuePresence<E>` | valeur de référence. Pour un containment, c'est l'*ownership*, qui indique où est rangé l'élément. | idem |
| `IMergeableDifference<E>` | différence fusionnable | `mergeTo(Role)` ; `getRequiresDependencies(Role)` / `getImpliesDependencies(Role)` donnent les fusions entraînées |

**Comment lire une modification.** Un attribut mono-valué renommé `A` → `B` donne **deux** `IAttributeValuePresence` symétriques : `A` présent côté TARGET, `B` présent côté REFERENCE. `getSymmetrical()` relie les deux. Pour un affichage « utilisateur », regroupez-les par `(match, feature)`, comme le fait `Change.group` dans ce prototype.

**Un ajout d'élément** donne une `IElementPresence`, plus une `IReferenceValuePresence` d'ownership sur le conteneur. Regroupez-les aussi.

### 4.1 Le 3-way : `isAlignedWithAncestor()` et `isConflicting()`

C'est la partie la plus mal documentée, et la plus importante.

- **`isAlignedWithAncestor()`** vaut vrai si **l'ancêtre a la même chose que le côté de présence**. La présence existait déjà au dernier import : c'est donc **l'autre côté** qui a changé (il a supprimé ou modifié la valeur).
  - Dans une comparaison 2-way, elle renvoie toujours `true`.
- **`isConflicting()`** vaut vrai si les deux côtés ont modifié la même chose différemment depuis l'ancêtre.
  - Invariant : `isConflicting()` implique `!isAlignedWithAncestor()`.
  - DiffMerge détecte aussi les conflits suppression/modification : élément supprimé d'un côté, modifié de l'autre.

**Origine d'un changement** (voir `policies/ChangeOrigin`) :

```java
if (d.isConflicting()) → CONFLIT
Role presence = ((IPresenceDifference<?>) d).getPresenceRole();
Role auteur   = d.isAlignedWithAncestor() ? presence.opposite() : presence;
auteur == REFERENCE → changement de la source ; auteur == TARGET → changement du projet
```

Exemples avec TARGET = projet, REFERENCE = UML et ANCESTOR = dernier import :

| Situation | Présence | Aligné ? | Auteur |
|---|---|---|---|
| L'UML ajoute un élément | REFERENCE | non (absent de l'ancêtre) | UML |
| L'UML supprime un élément | TARGET | oui (présent dans l'ancêtre) | UML |
| Le projet ajoute un élément | TARGET | non | projet |
| Le projet supprime un élément | REFERENCE | oui | projet |
| L'UML renomme `A` → `B` | `B` côté REFERENCE (non aligné) ; `A` côté TARGET (aligné) | — | UML pour les deux |

---

## 5. Les policies (stratégies de comparaison)

### 5.1 `IMatchPolicy<E>` : qui correspond à qui

```java
Object getMatchID(E element, ITreeDataScope<E> scope); // null = jamais apparié
Comparator<?> getMatchIDComparator();                   // optionnel : TreeMap au lieu de HashMap
boolean keepMatchIDs();                                 // garder les IDs (utile pour l'UI)
```

Deux éléments, issus de scopes différents, sont appariés si et seulement si leurs match IDs sont égaux et non nuls. Dans un même scope, les IDs doivent être uniques.

Implémentations disponibles :

| Classe | Principe |
|---|---|
| `dm.impl.policies.DefaultMatchPolicy` | ID EMF / xmi:id, puis URI |
| `dm.impl.policies.ConfigurableMatchPolicy` | critères configurables (ID, nom, chemin…) |
| `dm.bridge.incremental.BridgeTraceBasedMatchPolicy` | **par traces** : `trace.getCause(element)`. 2-way seulement. |
| `TraceBasedThreeWayMatchPolicy` (ce prototype) | idem, avec une trace par scope, ancêtre compris |

### 5.2 `IDiffPolicy<E>` / `dm.impl.policies.DefaultDiffPolicy` : quoi comparer

Héritez de `DefaultDiffPolicy` et surchargez ces méthodes :

| Méthode | Usage |
|---|---|
| `coverFeature(EStructuralFeature)` | `false` = feature ignorée : IDs, données propres au projet |
| `considerOrderedFeature(EStructuralFeature)` | `false` = l'ordre des listes n'est pas une différence |
| `coverIDAttributes()` | `false` par défaut : les attributs ID ne sont pas comparés |
| `considerEqual(v1, v2, attribute, scope)` | égalité personnalisée : casse, espaces… |
| `coverMatch(IMatch)` | exclure certains éléments de la comparaison |
| `coverOutOfScopeValue(...)` | gestion des références vers l'extérieur des scopes |

### 5.3 `IMergePolicy<E>` / `dm.impl.policies.DefaultMergePolicy` : comment fusionner

Cette policy contrôle les effets de bord des fusions. Passer `null` au bridge revient à utiliser la policy par défaut.

| Méthode | Usage |
|---|---|
| `copyAttribute` / `copyReference` | quelles features copier lors d'un ajout d'élément |
| `getAdditionGroup` / `getDeletionGroup` | éléments à ajouter ou supprimer ensemble |
| `isMandatoryForAddition` / `isMandatoryForDeletion` | références obligatoires qui entraînent d'autres fusions |
| `bindPresenceToOwnership` | lie l'ajout d'un élément à son rangement |

---

## 6. Fusionner

### 6.1 `IMergeSelector<E>` : fusion automatique

```java
Role getMergeDirection(IDifference<E> d); // TARGET, REFERENCE, ou null = ne pas fusionner
```

Utilisé par `comparison.merge(selector, true, monitor)`. C'est l'endroit où l'on code la règle métier « appliquer ce qui vient de la source ». Voir `UmlChangesMergeSelector` :

```java
return ChangeOrigin.of(d) == UML ? Role.TARGET : null;
```

Les dépendances sont gérées par DiffMerge : fusionner l'ajout d'un attribut entraîne l'ajout de son propriétaire s'il manque.

### 6.2 Fusion unitaire

`((IMergeableDifference<E>) d).mergeTo(Role.TARGET)` fusionne une seule différence et renvoie toutes les différences fusionnées en conséquence.

---

## 7. Le bridge : transformation + traces

### 7.1 Interfaces de base (`dm.bridge.api`)

| Interface | Rôle |
|---|---|
| `IBridge<SD, TD>` | une transformation de `SD` (source) vers `TD` (cible). `executeOn(source, target, execution, monitor)`, `createExecution(trace)`. |
| `IBridgeExecution` | une exécution : `getTrace()`, `getStatus()` |
| `ICause<S>` | la raison de l'existence d'une cible. `getSourceElements()`. Sous-interface `ICause.Symbolic` : `getSymbol(ISymbolFunction)`. |
| `IBridgeTrace` | trace **cible → cause** : `getCause(target)` |
| `IBridgeTrace.Editable` | `putCause(cause, target)`, `removeTarget(target)` |
| `ISymbolFunction` / `dm.bridge.impl.emf.EMFSymbolFunction` | transforme un objet en symbole persistant (String). Pour un `EObject` : `EcoreUtil.getID`, sinon fragment d'URI (donc xmi:id), sinon `identityHashCode`. Ce dernier cas n'est **pas persistant**. |

### 7.2 Le modèle de trace persistant (`dm.bridge.traces.gen.bridgetraces`)

- `Trace` (EObject) implémente `IBridgeTrace.Editable`. Il contient une `EMap<String, String> targetToCause` qui associe le symbole de la cible au symbole de la cause.
- `BridgetracesFactory.eINSTANCE.createTrace()` crée une trace vide.
- `BridgetracesPackage.eNS_URI` = `http://www.eclipse.org/emf/diffmerge/bridge/1.0.0/traces`. Enregistrez-le dans votre `ResourceSet` hors OSGi.
- **Persistance** : comme tout EObject, ajoutez la trace à une ressource XMI et sauvegardez-la. L'attribut `symbolFunction` est transient.

Format d'une cause produite par le mapping bridge :

```
Source[<symbole source>]_Rule[<id de règle>]
```

### 7.3 Écrire la transformation (`dm.bridge.mapping`)

| Classe | Rôle |
|---|---|
| `impl.emf.EMFMappingBridge<SD, TD extends IEditableModelScope>` | la transformation. Son constructeur déclare les requêtes racines (`new Query(this, …)`). |
| `impl.Query<I, O>` | requête : `Iterable<O> evaluate(I input, IQueryExecution qe)`. Peut avoir des sous-requêtes : `new Query(parentQuery, …)`. |
| `impl.Rule<S, T>` | règle attachée à une requête : `T createTarget(S, qe)` puis `defineTarget(S, T, qe, me)` |
| `impl.QueryAndRule<S, X, T>` | **raccourci** : une requête et sa règle principale en une seule classe. `getMainRule()` renvoie la règle, utilisée pour les recherches. |
| `api.IQueryExecution` | contexte de requête : `get(query)` renvoie la valeur courante d'une requête englobante (le parent) |
| `api.IMappingExecution` | contexte global : `get(source, rule)` renvoie la cible créée par la règle pour cette source, ou `null` ; `getTargetDataSet()` renvoie le scope cible |
| `impl.MappingCause` | la cause créée automatiquement pour chaque couple (source, règle) |

**Déroulement d'une exécution** (`MappingBridgeOperation`) :
1. Toutes les requêtes sont évaluées récursivement, et pour chaque résultat, chaque règle appelle `createTarget`.
2. `targetsCreated(scope)` est appelé.
3. Pour chaque cible, `defineTarget` est appelé. C'est là qu'on pose les attributs, les références et le containment, puisque toutes les cibles existent déjà. Ensuite, le couple (cause, cible) est enregistré dans la trace.
4. `targetsDefined(scope)` est appelé.

**Pièges :**
- La cible n'a **pas de conteneur** à la création. C'est à `defineTarget` de la ranger : `parent.getChildren().add(t)`, ou `((IEditableModelScope) me.getTargetDataSet()).add(t)` pour une racine.
- La trace est remplie **après** `defineTarget` et utilise l'**ID de la cible** : celui-ci doit être fixé au plus tard dans `defineTarget`. L'idéal est de le fixer dans le constructeur EMF.
- L'**ID d'une règle** (chaîne passée au constructeur) fait partie de la cause, donc du format persistant. Changer cet ID revient à faire croire que tout a été supprimé puis recréé.

### 7.4 Le bridge incrémental (`dm.bridge.incremental`)

`EMFIncrementalBridge<SD, TD extends IEditableModelScope>` **enveloppe** une transformation (`IBridge`) :

```java
new EMFIncrementalBridge<>(transformation, diffPolicy, mergePolicy, mergeSelector);
bridge.executeOn(source, target, null /*execution*/, referenceTrace, false /*defer interactive*/, monitor);
```

Comportement de `executeOn` (`IncrementalWrappingBridgeOperation`) :

| Condition | Comportement |
|---|---|
| `referenceTrace == null` **ou** cible vide | **non incrémental** : transformation directe dans la cible ; la trace d'exécution décrit la cible |
| sinon | **incrémental** : transformation dans un `IntermediateModelScope`, puis `merge(intermediaire, cible, execution)` |

Étapes de `merge(...)` dans le cas incrémental, chacune redéfinissable :

| Méthode | Par défaut | Ce prototype |
|---|---|---|
| `intermediateDataSetFilled(created)` | rien | copie le résultat frais, qui deviendra la nouvelle baseline |
| `compare(created, existing, createdTrace, existingTrace, monitor)` | `EComparisonImpl(existing, created)` **2-way** + `BridgeTraceBasedMatchPolicy` | 3-way avec l'ancêtre et `TraceBasedThreeWayMatchPolicy` |
| `mergeAutomatically(comparison, monitor)` | `comparison.merge(mergeSelector, true, monitor)` | classe les différences, puis appelle `super` |
| `mergeInteractively(execution, monitor)` | si `isAlwaysInteractive()` ou s'il reste des différences : `handleInteractiveMerge`, puis `handleMergedDifferences` | appelle le gestionnaire de conflits s'il reste des conflits |
| `handleMergedDifferences(comparison, createdTrace, existingTrace)` | **met à jour la trace existante** : `putCause` pour les éléments ajoutés, `removeTarget` pour les éléments supprimés | inchangé (à appeler impérativement) |

`IIncrementalBridgeExecution` (le résultat d'`executeOn`) :

| Méthode | Description |
|---|---|
| `getTrace()` | trace de **cette** exécution. En incrémental, elle décrit le scope intermédiaire. |
| `getReferenceTrace()` | la trace existante, mise à jour en place |
| `isActuallyIncremental()` | vrai si on est passé par compare/merge |
| `getInteractiveMergeData()` | la `EComparison` |

> ⚠️ **Le bridge incrémental natif est 2-way.** Il ne sait pas si une différence vient de la source ou de la cible. Avec un sélecteur « tout vers TARGET », les modifications du projet sont écrasées ; avec un sélecteur nul, rien n'est appliqué. Le 3-way (ancêtre = baseline) est indispensable pour une vraie co-évolution. Voir `SysarchIncrementalBridge`.

---

## 8. L'interface de comparaison (`dm.ui`, `dm.bridge.interactive`)

| Classe | Rôle |
|---|---|
| `dm.ui.viewers.EMFDiffNode` | entrée des viewers : la comparaison, l'editing domain (peut être `null`), l'éditabilité gauche/droite. `setLeftRole`, `setReferenceRole` (côté de référence pour les couleurs), `setDrivingRole`. |
| `dm.ui.util.DiffMergeDialog` | dialogue modal qui contient un `ComparisonViewer` : synthèse, deux modèles, détails, boutons de fusion |
| `dm.bridge.interactive.UpdateDialog` | `DiffMergeDialog` du bridge, avec un bouton « ouvrir dans un éditeur », que l'on peut masquer en redéfinissant `createOpenEditorButton`. Il renvoie `OK` ou `CANCEL`. Les fusions faites par l'utilisateur sont appliquées directement aux scopes. |
| `dm.bridge.interactive.EMFInteractiveBridge` | `EMFIncrementalBridge` qui ouvre **toujours** l'`UpdateDialog` (2-way). C'est un bon exemple de configuration de l'`EMFDiffNode`. |
| `dm.ui.viewers.ComparisonViewer` | `getSynthesisViewer().getInnerViewer()` renvoie le `ComparisonTreeViewer`, qui offre `expandAll()` et `getNextUserDifference(path)` |

Configuration recommandée : source à gauche, projet à droite et modifiable.

```java
EMFDiffNode node = new EMFDiffNode(comparison, null, true, false);
node.setReferenceRole(Role.TARGET);
node.setDrivingRole(Role.TARGET);
node.setLeftRole(Role.REFERENCE);
int rc = new UpdateDialog(shell, "titre", node).open();
```

Les dialogues doivent être ouverts dans le thread UI : passez par `Display.syncExec` si la synchronisation tourne dans un job.

---

## 9. Dépendances et pièges de packaging

- Les bundles `bridge*` importent `org.apache.log4j` (API 1.2). Ce package est fourni par Orbit `org.apache.log4j 1.2.24` (dépôt R20230302014618).
- `org.eclipse.emf.diffmerge.bridge.log4j` exige un bundle introuvable (`org.apache.log4j1.2.15…`) : ne l'incluez pas. Conséquence : un message `log4j:ERROR … BridgeLoggingAppender` sans gravité.
- `dm.ui` dépend de `org.eclipse.emf.workspace` (EMF Transaction).
- DiffMerge 0.14.1 (2023) fonctionne sur Eclipse 2024-12 avec Java 21.
