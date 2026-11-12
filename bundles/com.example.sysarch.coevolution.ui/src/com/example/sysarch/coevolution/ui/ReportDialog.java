package com.example.sysarch.coevolution.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Read-only, resizable display of the synchronization reports. */
public class ReportDialog extends Dialog {

  private final String title;
  private final String text;

  public ReportDialog(Shell parent, String title, String text) {
    super(parent);
    this.title = title;
    this.text = text;
    setShellStyle(getShellStyle() | SWT.RESIZE | SWT.MAX);
  }

  @Override
  protected void configureShell(Shell shell) {
    super.configureShell(shell);
    shell.setText(title);
  }

  @Override
  protected Control createDialogArea(Composite parent) {
    Composite area = (Composite) super.createDialogArea(parent);
    Text t = new Text(area, SWT.MULTI | SWT.READ_ONLY | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
    t.setFont(JFaceResources.getTextFont());
    t.setText(text);
    GridData gd = new GridData(GridData.FILL_BOTH);
    gd.widthHint = 1000;
    gd.heightHint = 450;
    t.setLayoutData(gd);
    return area;
  }

  @Override
  protected void createButtonsForButtonBar(Composite parent) {
    createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
  }
}
