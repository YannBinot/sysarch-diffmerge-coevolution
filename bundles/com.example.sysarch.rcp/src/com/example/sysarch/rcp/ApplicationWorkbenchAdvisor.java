package com.example.sysarch.rcp;

import java.net.URL;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.application.IWorkbenchConfigurer;
import org.eclipse.ui.application.IWorkbenchWindowConfigurer;
import org.eclipse.ui.application.WorkbenchAdvisor;
import org.eclipse.ui.application.WorkbenchWindowAdvisor;
import org.eclipse.ui.ide.IDE;
import org.osgi.framework.Bundle;

/**
 * Workbench advisor of an RCP application working on workspace projects
 * (resource adapters, project images, workspace saved on exit).
 */
public class ApplicationWorkbenchAdvisor extends WorkbenchAdvisor {

  static final String PERSPECTIVE_ID = "com.example.sysarch.rcp.perspective";

  @Override
  public void initialize(IWorkbenchConfigurer configurer) {
    super.initialize(configurer);
    configurer.setSaveAndRestore(false);
    IDE.registerAdapters();
    Bundle ide = Platform.getBundle("org.eclipse.ui.ide");
    declare(configurer, ide, IDE.SharedImages.IMG_OBJ_PROJECT, "icons/full/obj16/prj_obj");
    declare(configurer, ide, IDE.SharedImages.IMG_OBJ_PROJECT_CLOSED, "icons/full/obj16/cprj_obj");
  }

  private void declare(IWorkbenchConfigurer configurer, Bundle bundle, String key, String pathWithoutExtension) {
    if (bundle == null) {
      return;
    }
    for (String ext : new String[] { ".svg", ".png", ".gif" }) {
      URL url = FileLocator.find(bundle, new Path(pathWithoutExtension + ext), null);
      if (url != null) {
        configurer.declareImage(key, ImageDescriptor.createFromURL(url), true);
        return;
      }
    }
  }

  @Override
  public WorkbenchWindowAdvisor createWorkbenchWindowAdvisor(IWorkbenchWindowConfigurer configurer) {
    return new ApplicationWorkbenchWindowAdvisor(configurer);
  }

  @Override
  public String getInitialWindowPerspectiveId() {
    return PERSPECTIVE_ID;
  }

  @Override
  public IAdaptable getDefaultPageInput() {
    return ResourcesPlugin.getWorkspace().getRoot();
  }

  @Override
  public boolean preShutdown() {
    try {
      ResourcesPlugin.getWorkspace().save(true, null);
    } catch (CoreException e) {
      Platform.getLog(getClass()).log(e.getStatus());
    }
    return true;
  }
}
