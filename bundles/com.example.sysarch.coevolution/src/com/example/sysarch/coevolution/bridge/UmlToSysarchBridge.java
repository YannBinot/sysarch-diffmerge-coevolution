package com.example.sysarch.coevolution.bridge;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.diffmerge.api.scopes.IEditableModelScope;
import org.eclipse.emf.diffmerge.bridge.mapping.api.IMappingExecution;
import org.eclipse.emf.diffmerge.bridge.mapping.api.IQueryExecution;
import org.eclipse.emf.diffmerge.bridge.mapping.impl.QueryAndRule;
import org.eclipse.emf.diffmerge.bridge.mapping.impl.emf.EMFMappingBridge;
import org.eclipse.uml2.uml.EnumerationLiteral;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.ParameterDirectionKind;
import org.eclipse.uml2.uml.Property;

import com.example.sysarch.Attribute;
import com.example.sysarch.Component;
import com.example.sysarch.DataType;
import com.example.sysarch.Enumeration;
import com.example.sysarch.Interface;
import com.example.sysarch.NamedElement;
import com.example.sysarch.Operation;
import com.example.sysarch.Parameter;
import com.example.sysarch.SysarchFactory;
import com.example.sysarch.SystemModel;
import com.example.sysarch.Type;
import com.example.sysarch.TypedElement;

/**
 * Declarative UML -> Sysarch transformation written with the EMF DiffMerge
 * "mapping bridge" API (queries + rules).
 * <p>
 * Source data set: one UML {@link Model}. Target data set: an editable model scope
 * (the project content on first import, an intermediate scope on updates).
 * <p>
 * Every target element is created by a rule from a source element: the pair
 * (source element xmi:id, rule id) is the <i>cause</i> stored in the bridge trace.
 * Rule ids are therefore part of the persistent format and must remain stable.
 * <pre>
 * uml::Model                         -> SystemModel         (rule "SystemModel")
 * uml::Model with direct classifiers -> Package "&lt;model&gt;"  (rule "DefaultPackage")
 * uml::Package                       -> Package             (rule "Package")
 * uml::Class                         -> Component           (rule "Component")
 *   uml::Property (owned attribute)  ->   Attribute         (rule "Attribute")
 * uml::Interface                     -> Interface           (rule "Interface")
 * uml::Enumeration                   -> Enumeration         (rule "Enumeration")
 * uml::DataType / PrimitiveType      -> DataType            (rule "DataType")
 * uml::Operation (class/interface)   -> Operation           (rule "Operation")
 *   uml::Parameter (non-return)      ->   Parameter         (rule "Parameter")
 * </pre>
 */
public class UmlToSysarchBridge extends EMFMappingBridge<Model, IEditableModelScope> {

  static final SysarchFactory F = SysarchFactory.eINSTANCE;

  final QueryAndRule<Model, Model, SystemModel> modelRule;
  final QueryAndRule<Model, Model, com.example.sysarch.Package> defaultPackageRule;
  final QueryAndRule<Model, org.eclipse.uml2.uml.Package, com.example.sysarch.Package> packageRule;
  final QueryAndRule<Model, org.eclipse.uml2.uml.Class, Component> componentRule;
  final QueryAndRule<org.eclipse.uml2.uml.Class, Property, Attribute> attributeRule;
  final QueryAndRule<Model, org.eclipse.uml2.uml.Interface, Interface> interfaceRule;
  final QueryAndRule<Model, org.eclipse.uml2.uml.Enumeration, Enumeration> enumerationRule;
  final QueryAndRule<Model, org.eclipse.uml2.uml.DataType, DataType> dataTypeRule;
  final QueryAndRule<Model, org.eclipse.uml2.uml.Operation, Operation> operationRule;
  final QueryAndRule<org.eclipse.uml2.uml.Operation, org.eclipse.uml2.uml.Parameter, Parameter> parameterRule;

  public UmlToSysarchBridge() {
    super();

    // ---- Model -> SystemModel (root of the project)
    modelRule = new QueryAndRule<Model, Model, SystemModel>(this, "SystemModel") {
      @Override
      public Iterable<Model> evaluate(Model model, IQueryExecution qe) {
        return Collections.singleton(model);
      }
      @Override
      public SystemModel createTarget(Model model, IQueryExecution qe) {
        return F.createSystemModel();
      }
      @Override
      public void defineTarget(Model model, SystemModel target, IQueryExecution qe, IMappingExecution me) {
        setNamed(target, model.getName(), model);
        target.setSourceModelName(model.getQualifiedName());
        IEditableModelScope scope = me.getTargetDataSet();
        scope.add(target);
      }
    };

    // ---- Classifiers owned directly by the model are gathered in a default package
    defaultPackageRule = new QueryAndRule<Model, Model, com.example.sysarch.Package>(this, "DefaultPackage") {
      @Override
      public Iterable<Model> evaluate(Model model, IQueryExecution qe) {
        boolean hasDirectTypes = model.getOwnedTypes().stream().anyMatch(UmlToSysarchBridge.this::isMappedType);
        return hasDirectTypes ? Collections.singleton(model) : Collections.<Model>emptyList();
      }
      @Override
      public com.example.sysarch.Package createTarget(Model model, IQueryExecution qe) {
        return F.createPackage();
      }
      @Override
      public void defineTarget(Model model, com.example.sysarch.Package target, IQueryExecution qe, IMappingExecution me) {
        target.setName(model.getName());
        target.setDescription("Elements defined at the root of UML model " + model.getName());
        me.get(model, modelRule.getMainRule()).getPackages().add(target);
      }
    };

    // ---- Package -> Package (nested packages become sub-packages)
    packageRule = new QueryAndRule<Model, org.eclipse.uml2.uml.Package, com.example.sysarch.Package>(this, "Package") {
      @Override
      public Iterable<org.eclipse.uml2.uml.Package> evaluate(Model model, IQueryExecution qe) {
        return UmlHelper.contents(model, org.eclipse.uml2.uml.Package.class, null);
      }
      @Override
      public com.example.sysarch.Package createTarget(org.eclipse.uml2.uml.Package source, IQueryExecution qe) {
        return F.createPackage();
      }
      @Override
      public void defineTarget(org.eclipse.uml2.uml.Package source, com.example.sysarch.Package target,
          IQueryExecution qe, IMappingExecution me) {
        setNamed(target, source.getName(), source);
        org.eclipse.uml2.uml.Package owner = source.getNestingPackage();
        if (owner instanceof Model) {
          me.get((Model) owner, modelRule.getMainRule()).getPackages().add(target);
        } else {
          me.get(owner, packageRule.getMainRule()).getSubPackages().add(target);
        }
      }
    };

    // ---- Class -> Component
    componentRule = new QueryAndRule<Model, org.eclipse.uml2.uml.Class, Component>(this, "Component") {
      @Override
      public Iterable<org.eclipse.uml2.uml.Class> evaluate(Model model, IQueryExecution qe) {
        return UmlHelper.contents(model, org.eclipse.uml2.uml.Class.class, UmlHelper::isComponentClass);
      }
      @Override
      public Component createTarget(org.eclipse.uml2.uml.Class source, IQueryExecution qe) {
        return F.createComponent();
      }
      @Override
      public void defineTarget(org.eclipse.uml2.uml.Class source, Component target, IQueryExecution qe,
          IMappingExecution me) {
        setNamed(target, source.getName(), source);
        target.setAbstract(source.isAbstract());
        for (org.eclipse.uml2.uml.Class general : source.getSuperClasses()) {
          Component superComponent = me.get(general, componentRule.getMainRule());
          if (superComponent != null) {
            target.getSuperComponents().add(superComponent);
          }
        }
        for (org.eclipse.uml2.uml.Interface contract : source.getImplementedInterfaces()) {
          Interface provided = me.get(contract, interfaceRule.getMainRule());
          if (provided != null) {
            target.getProvidedInterfaces().add(provided);
          }
        }
        containerOf(source, me).getTypes().add(target);
      }
    };

    // ---- Class::ownedAttribute -> Component::attributes (sub-query of the class query)
    attributeRule = new QueryAndRule<org.eclipse.uml2.uml.Class, Property, Attribute>(componentRule, "Attribute") {
      @Override
      public Iterable<Property> evaluate(org.eclipse.uml2.uml.Class owner, IQueryExecution qe) {
        return owner.getOwnedAttributes();
      }
      @Override
      public Attribute createTarget(Property source, IQueryExecution qe) {
        return F.createAttribute();
      }
      @Override
      public void defineTarget(Property source, Attribute target, IQueryExecution qe, IMappingExecution me) {
        setNamed(target, source.getName(), source);
        setType(target, source.getType(), me);
        target.setLowerBound(source.getLower());
        target.setUpperBound(UmlHelper.upper(source));
        org.eclipse.uml2.uml.Class owner = qe.get(componentRule);
        me.get(owner, componentRule.getMainRule()).getAttributes().add(target);
      }
    };

    // ---- Interface -> Interface
    interfaceRule = new QueryAndRule<Model, org.eclipse.uml2.uml.Interface, Interface>(this, "Interface") {
      @Override
      public Iterable<org.eclipse.uml2.uml.Interface> evaluate(Model model, IQueryExecution qe) {
        return UmlHelper.contents(model, org.eclipse.uml2.uml.Interface.class, UmlHelper::isPackaged);
      }
      @Override
      public Interface createTarget(org.eclipse.uml2.uml.Interface source, IQueryExecution qe) {
        return F.createInterface();
      }
      @Override
      public void defineTarget(org.eclipse.uml2.uml.Interface source, Interface target, IQueryExecution qe,
          IMappingExecution me) {
        setNamed(target, source.getName(), source);
        containerOf(source, me).getTypes().add(target);
      }
    };

    // ---- Enumeration -> Enumeration
    enumerationRule = new QueryAndRule<Model, org.eclipse.uml2.uml.Enumeration, Enumeration>(this, "Enumeration") {
      @Override
      public Iterable<org.eclipse.uml2.uml.Enumeration> evaluate(Model model, IQueryExecution qe) {
        return UmlHelper.contents(model, org.eclipse.uml2.uml.Enumeration.class, UmlHelper::isPackaged);
      }
      @Override
      public Enumeration createTarget(org.eclipse.uml2.uml.Enumeration source, IQueryExecution qe) {
        return F.createEnumeration();
      }
      @Override
      public void defineTarget(org.eclipse.uml2.uml.Enumeration source, Enumeration target, IQueryExecution qe,
          IMappingExecution me) {
        setNamed(target, source.getName(), source);
        List<String> literals = source.getOwnedLiterals().stream()
            .map(EnumerationLiteral::getName).collect(Collectors.toList());
        target.getLiterals().addAll(literals);
        containerOf(source, me).getTypes().add(target);
      }
    };

    // ---- DataType / PrimitiveType -> DataType
    dataTypeRule = new QueryAndRule<Model, org.eclipse.uml2.uml.DataType, DataType>(this, "DataType") {
      @Override
      public Iterable<org.eclipse.uml2.uml.DataType> evaluate(Model model, IQueryExecution qe) {
        return UmlHelper.contents(model, org.eclipse.uml2.uml.DataType.class, UmlHelper::isPlainDataType);
      }
      @Override
      public DataType createTarget(org.eclipse.uml2.uml.DataType source, IQueryExecution qe) {
        return F.createDataType();
      }
      @Override
      public void defineTarget(org.eclipse.uml2.uml.DataType source, DataType target, IQueryExecution qe,
          IMappingExecution me) {
        setNamed(target, source.getName(), source);
        containerOf(source, me).getTypes().add(target);
      }
    };

    // ---- Operation (of a class or an interface) -> Operation
    operationRule = new QueryAndRule<Model, org.eclipse.uml2.uml.Operation, Operation>(this, "Operation") {
      @Override
      public Iterable<org.eclipse.uml2.uml.Operation> evaluate(Model model, IQueryExecution qe) {
        return UmlHelper.contents(model, org.eclipse.uml2.uml.Operation.class,
            op -> op.getOwner() instanceof org.eclipse.uml2.uml.Interface
               || op.getOwner() instanceof org.eclipse.uml2.uml.Class
                  && UmlHelper.isComponentClass((org.eclipse.uml2.uml.Class) op.getOwner()));
      }
      @Override
      public Operation createTarget(org.eclipse.uml2.uml.Operation source, IQueryExecution qe) {
        return F.createOperation();
      }
      @Override
      public void defineTarget(org.eclipse.uml2.uml.Operation source, Operation target, IQueryExecution qe,
          IMappingExecution me) {
        setNamed(target, source.getName(), source);
        setType(target, source.getType(), me);
        if (source.getOwner() instanceof org.eclipse.uml2.uml.Interface) {
          me.get((org.eclipse.uml2.uml.Interface) source.getOwner(), interfaceRule.getMainRule())
            .getOperations().add(target);
        } else {
          me.get((org.eclipse.uml2.uml.Class) source.getOwner(), componentRule.getMainRule())
            .getOperations().add(target);
        }
      }
    };

    // ---- Operation::ownedParameter (except return) -> Operation::parameters
    parameterRule = new QueryAndRule<org.eclipse.uml2.uml.Operation, org.eclipse.uml2.uml.Parameter, Parameter>(
        operationRule, "Parameter") {
      @Override
      public Iterable<org.eclipse.uml2.uml.Parameter> evaluate(org.eclipse.uml2.uml.Operation op, IQueryExecution qe) {
        return op.getOwnedParameters().stream()
            .filter(p -> p.getDirection() != ParameterDirectionKind.RETURN_LITERAL)
            .collect(Collectors.toList());
      }
      @Override
      public Parameter createTarget(org.eclipse.uml2.uml.Parameter source, IQueryExecution qe) {
        return F.createParameter();
      }
      @Override
      public void defineTarget(org.eclipse.uml2.uml.Parameter source, Parameter target, IQueryExecution qe,
          IMappingExecution me) {
        setNamed(target, source.getName(), source);
        setType(target, source.getType(), me);
        me.get(source.getOperation(), operationRule.getMainRule()).getParameters().add(target);
      }
    };
  }

  // ------------------------------------------------------------------ helpers

  boolean isMappedType(org.eclipse.uml2.uml.Type t) {
    if (t instanceof org.eclipse.uml2.uml.Class) {
      return UmlHelper.isComponentClass((org.eclipse.uml2.uml.Class) t);
    }
    return t instanceof org.eclipse.uml2.uml.Interface || t instanceof org.eclipse.uml2.uml.DataType;
  }

  static void setNamed(NamedElement target, String name, org.eclipse.uml2.uml.Element source) {
    target.setName(name);
    target.setDescription(UmlHelper.documentation(source));
  }

  /** Target package that must contain the target of the given packaged UML classifier. */
  com.example.sysarch.Package containerOf(org.eclipse.uml2.uml.Type source, IMappingExecution me) {
    org.eclipse.uml2.uml.Package owner = source.getPackage();
    if (owner instanceof Model) {
      return me.get((Model) owner, defaultPackageRule.getMainRule());
    }
    return me.get(owner, packageRule.getMainRule());
  }

  /**
   * Sets the type reference when the UML type is mapped in the same model,
   * and always records the type name (types from other models / primitive libraries).
   */
  void setType(TypedElement target, org.eclipse.uml2.uml.Type umlType, IMappingExecution me) {
    if (umlType == null) {
      target.setTypeName(null);
      target.setType(null);
      return;
    }
    target.setTypeName(umlType.getName());
    target.setType(resolveType(umlType, me));
  }

  Type resolveType(org.eclipse.uml2.uml.Type t, IMappingExecution me) {
    if (t instanceof org.eclipse.uml2.uml.Enumeration) {
      return me.get((org.eclipse.uml2.uml.Enumeration) t, enumerationRule.getMainRule());
    } else if (t instanceof org.eclipse.uml2.uml.DataType) {
      return me.get((org.eclipse.uml2.uml.DataType) t, dataTypeRule.getMainRule());
    } else if (t instanceof org.eclipse.uml2.uml.Interface) {
      return me.get((org.eclipse.uml2.uml.Interface) t, interfaceRule.getMainRule());
    } else if (t instanceof org.eclipse.uml2.uml.Class) {
      return me.get((org.eclipse.uml2.uml.Class) t, componentRule.getMainRule());
    }
    return null;
  }
}
