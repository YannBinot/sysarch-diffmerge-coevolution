package com.example.sysarch.rcp;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;

/** Entry point of the Sysarch RCP application. */
public class Application implements IApplication {

  @Override
  public Object start(IApplicationContext context) {
    Display display = PlatformUI.createDisplay();
    try {
      int rc = PlatformUI.createAndRunWorkbench(display, new ApplicationWorkbenchAdvisor());
      return rc == PlatformUI.RETURN_RESTART ? IApplication.EXIT_RESTART : IApplication.EXIT_OK;
    } finally {
      display.dispose();
    }
  }

  @Override
  public void stop() {
    if (!PlatformUI.isWorkbenchRunning()) {
      return;
    }
    IWorkbench workbench = PlatformUI.getWorkbench();
    Display display = workbench.getDisplay();
    display.syncExec(() -> {
      if (!display.isDisposed()) {
        workbench.close();
      }
    });
  }
}
