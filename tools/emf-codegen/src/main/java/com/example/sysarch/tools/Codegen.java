package com.example.sysarch.tools;

import java.io.File;

import org.eclipse.emf.codegen.ecore.generator.Generator;
import org.eclipse.emf.codegen.ecore.genmodel.GenModel;
import org.eclipse.emf.codegen.ecore.genmodel.GenModelPackage;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenBaseGeneratorAdapter;
import org.eclipse.emf.codegen.ecore.genmodel.generator.GenModelGeneratorAdapterFactory;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

/**
 * Standalone (non-OSGi) EMF generator for the sysarch metamodel.
 * Maps platform:/resource/&lt;bundle&gt;/ onto ./bundles/&lt;bundle&gt;/ so that the
 * genmodel directories (/com.example.sysarch/src-gen, ...) are honoured.
 */
public class Codegen {

  private static final String[] BUNDLES = {
      "com.example.sysarch", "com.example.sysarch.edit", "com.example.sysarch.editor" };

  public static void main(String[] args) throws Exception {
    File root = new File(args.length > 0 ? args[0] : ".").getCanonicalFile();
    File bundles = new File(root, "bundles");
    for (String b : BUNDLES) {
      URI fileUri = URI.createFileURI(new File(bundles, b).getAbsolutePath() + File.separator);
      EcorePlugin.getPlatformResourceMap().put(b, fileUri);
      URIConverter.URI_MAP.put(URI.createPlatformResourceURI(b + "/", false), fileUri);
    }
    EcorePackage.eINSTANCE.eClass();
    GenModelPackage.eINSTANCE.eClass();

    ResourceSet rs = new ResourceSetImpl();
    rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("ecore", new EcoreResourceFactoryImpl());
    rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
    rs.getPackageRegistry().put(GenModelPackage.eNS_URI, GenModelPackage.eINSTANCE);
    rs.getPackageRegistry().put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);

    File genmodelFile = new File(bundles, "com.example.sysarch/model/sysarch.genmodel");
    Resource res = rs.getResource(URI.createFileURI(genmodelFile.getAbsolutePath()), true);
    GenModel genModel = (GenModel) res.getContents().get(0);
    genModel.reconcile();
    genModel.setCanGenerate(true);
    genModel.setValidateModel(false);

    Generator generator = new Generator();
    generator.getAdapterFactoryDescriptorRegistry().addDescriptor(
        GenModelPackage.eNS_URI, GenModelGeneratorAdapterFactory.DESCRIPTOR);
    generator.setInput(genModel);
    String[] types = {
        GenBaseGeneratorAdapter.MODEL_PROJECT_TYPE,
        GenBaseGeneratorAdapter.EDIT_PROJECT_TYPE,
        GenBaseGeneratorAdapter.EDITOR_PROJECT_TYPE };
    int severity = Diagnostic.OK;
    for (String type : types) {
      Diagnostic d = generator.generate(genModel, type, new BasicMonitor.Printing(System.out));
      System.out.println(type + " -> " + d.getSeverity() + " " + d.getMessage());
      for (Diagnostic c : d.getChildren()) {
        System.out.println("  " + c.getSeverity() + " " + c.getMessage());
      }
      severity = Math.max(severity, d.getSeverity());
    }
    if (severity >= Diagnostic.ERROR) {
      System.exit(1);
    }
  }
}
