package com.example.sysarch.coevolution;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Properties;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ExtensibleURIConverterImpl;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;

import org.eclipse.emf.diffmerge.bridge.traces.gen.bridgetraces.BridgetracesPackage;

import com.example.sysarch.SysarchPackage;

/**
 * Storage layout of a project created by a UML import.
 * <pre>
 * &lt;project&gt;/
 *   &lt;project&gt;.sysarch                 the project data (edited by the user)
 *   .coevolution/
 *     link.properties                 UML source file + model id
 *     trace.bridgetraces              DiffMerge bridge trace: project element -> cause
 *     baseline.sysarch                transformation result of the last import (common ancestor)
 *     baseline.bridgetraces           trace of the baseline
 * </pre>
 */
public class CoevolutionProject {

  public static final String MODEL_EXTENSION = "sysarch";
  public static final String TRACE_EXTENSION = "bridgetraces";
  public static final String META_FOLDER = ".coevolution";

  static final String KEY_SOURCE = "source.uri";
  static final String KEY_MODEL_ID = "source.modelId";
  static final String KEY_MODEL_NAME = "source.modelName";
  static final String KEY_LAST_SYNC = "lastSync";

  private final URI folder;
  private final String name;

  /**
   * @param folder URI of the project folder (file: or platform:/resource), without trailing slash
   * @param name project name
   */
  public CoevolutionProject(URI folder, String name) {
    this.folder = folder.hasTrailingPathSeparator() ? folder.trimSegments(1) : folder;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public URI getFolder() {
    return folder;
  }

  public URI getModelUri() {
    return folder.appendSegment(name + "." + MODEL_EXTENSION);
  }

  public URI getTraceUri() {
    return folder.appendSegment(META_FOLDER).appendSegment("trace." + TRACE_EXTENSION);
  }

  public URI getBaselineUri() {
    return folder.appendSegment(META_FOLDER).appendSegment("baseline." + MODEL_EXTENSION);
  }

  public URI getBaselineTraceUri() {
    return folder.appendSegment(META_FOLDER).appendSegment("baseline." + TRACE_EXTENSION);
  }

  public URI getLinkUri() {
    return folder.appendSegment(META_FOLDER).appendSegment("link.properties");
  }

  // ------------------------------------------------------------ persistence

  /** A resource set able to read/write the project files whatever the registry state. */
  public static ResourceSet newResourceSet() {
    ResourceSet rs = new ResourceSetImpl();
    XMIResourceFactoryImpl xmi = new XMIResourceFactoryImpl();
    rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(MODEL_EXTENSION, xmi);
    rs.getResourceFactoryRegistry().getExtensionToFactoryMap().put(TRACE_EXTENSION, xmi);
    rs.getPackageRegistry().put(SysarchPackage.eNS_URI, SysarchPackage.eINSTANCE);
    rs.getPackageRegistry().put(BridgetracesPackage.eNS_URI, BridgetracesPackage.eINSTANCE);
    rs.getLoadOptions().put(XMLResource.OPTION_DEFER_IDREF_RESOLUTION, Boolean.TRUE);
    return rs;
  }

  public static Map<Object, Object> saveOptions() {
    return Map.of(XMLResource.OPTION_ENCODING, "UTF-8",
        Resource.OPTION_SAVE_ONLY_IF_CHANGED, Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);
  }

  public boolean exists(URI uri) {
    return new ExtensibleURIConverterImpl().exists(uri, null);
  }

  /** Whether the project was already imported from UML (it has a link and a trace). */
  public boolean isLinked() {
    return exists(getLinkUri()) && exists(getTraceUri()) && exists(getModelUri());
  }

  public Properties readLink() throws IOException {
    Properties p = new Properties();
    URIConverter c = new ExtensibleURIConverterImpl();
    try (InputStream in = c.createInputStream(getLinkUri())) {
      p.load(in);
    }
    return p;
  }

  public void writeLink(URI source, String modelId, String modelName) throws IOException {
    Properties p = new Properties();
    p.setProperty(KEY_SOURCE, source.toString());
    p.setProperty(KEY_MODEL_ID, modelId);
    p.setProperty(KEY_MODEL_NAME, modelName == null ? "" : modelName);
    p.setProperty(KEY_LAST_SYNC, java.time.OffsetDateTime.now().toString());
    URIConverter c = new ExtensibleURIConverterImpl();
    try (OutputStream out = c.createOutputStream(getLinkUri())) {
      p.store(out, "Link between this project and its external UML model (EMF DiffMerge co-evolution)");
    }
  }

  public URI getSourceUri() throws IOException {
    return URI.createURI(readLink().getProperty(KEY_SOURCE));
  }

  public String getSourceModelId() throws IOException {
    return readLink().getProperty(KEY_MODEL_ID);
  }
}
