package com.example.sysarch.coevolution.ui;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/** Logging helper (no bundle activator needed). */
public final class Activator {

  public static final String ID = "com.example.sysarch.coevolution.ui";

  private Activator() {
  }

  public static void log(Throwable t) {
    ILog.of(Activator.class).log(new Status(IStatus.ERROR, ID, String.valueOf(t.getMessage()), t));
  }
}
