package com.eclipseuzmani.migrationtools.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.BeansObservables;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.xml.sax.SAXException;

import com.eclipseuzmani.migrationtools.Activator;
import com.eclipseuzmani.migrationtools.model.MigrationToolsModel;
import com.eclipseuzmani.migrationtools.model.MigrationToolsModelDAO;

public class MigrationToolsEditor extends EditorPart {
	private boolean dirty;
	private Control mainControl;
	private final MigrationToolsModelDAO migrationToolsDAO = new MigrationToolsModelDAO();
	private final MigrationToolsModel model = new MigrationToolsModel();
	private final PropertyChangeListener propertyChangeListener = new PropertyChangeListener() {
		@Override
		public void propertyChange(PropertyChangeEvent e) {
			setDirty(true);
		}
	};

	private final IResourceChangeListener resourceChangeListener = new IResourceChangeListener() {

		@Override
		public void resourceChanged(final IResourceChangeEvent event) {
			if (getEditorInput() == null)
				return;
			final IFileEditorInput editorInput = (IFileEditorInput) getEditorInput()
					.getAdapter(IFileEditorInput.class);
			if (editorInput == null)
				return;
			final IPath filePath = editorInput.getFile().getFullPath();
			if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
				if (editorInput.getFile().getProject()
						.equals(event.getResource())) {
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							IWorkbenchPage[] pages = getSite()
									.getWorkbenchWindow().getPages();
							for (int i = 0; i < pages.length; i++) {
								IEditorPart editorPart = pages[i]
										.findEditor(getEditorInput());
								pages[i].closeEditor(editorPart, true);
							}
						}
					});
				}
			} else {
				final IResourceDelta mainDelta = event.getDelta();
				if (mainDelta == null)
					return;
				final IResourceDelta affectedElement = mainDelta
						.findMember(filePath);
				if (affectedElement == null)
					return;
				switch (affectedElement.getKind()) {
				case IResourceDelta.REMOVED:
					if ((IResourceDelta.MOVED_TO & affectedElement.getFlags()) != 0) {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								IPath path = affectedElement.getMovedToPath();
								IFile newFile = affectedElement.getResource()
										.getWorkspace().getRoot().getFile(path);
								if (newFile != null) {
									setInput(new FileEditorInput(newFile), false);
								}
							}
						});
					} else {
						Display.getDefault().asyncExec(new Runnable() {
							public void run() {
								IWorkbenchPage[] pages = getSite()
										.getWorkbenchWindow().getPages();
								for (int i = 0; i < pages.length; i++) {
									IEditorPart editorPart = pages[i]
											.findEditor(getEditorInput());
									pages[i].closeEditor(editorPart, true);
								}
							}
						});
					}
					break;
				case IResourceDelta.CHANGED:
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if (isDirty()) {
								MessageDialog dialog = new MessageDialog(
										getSite().getShell(),
										"Resource Changed",
										null,
										"File has been changed outside of editor. "
												+ "Would you like to replace editors content from file.",
										MessageDialog.QUESTION, new String[] {
												IDialogConstants.YES_LABEL,
												IDialogConstants.NO_LABEL }, 0) {
									protected int getShellStyle() {
										return SWT.NONE | SWT.TITLE
												| SWT.BORDER
												| SWT.APPLICATION_MODAL
												| SWT.SHEET
												| getDefaultOrientation();
									}
								};
								if (dialog.open() != 0)
									return;
							}
							loadFile();
						}
					});
					break;
				}
			}
		}
	};

	public MigrationToolsEditor() {
		model.addPropertyChangeListener(propertyChangeListener);
		ResourcesPlugin.getWorkspace().addResourceChangeListener(
				resourceChangeListener);
	}

	@Override
	public void createPartControl(Composite parent) {
		DataBindingContext bindingContext = new DataBindingContext();
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		Form form = toolkit.createForm(parent);
		form.setText("Migration tools editor.");
		toolkit.decorateFormHeading(form);
		form.setLayoutData(new GridData(GridData.FILL_BOTH));
		form.getBody().setLayout(new GridLayout(1, false));
		{
			Section section = toolkit.createSection(form.getBody(),
					Section.DESCRIPTION | Section.TITLE_BAR | Section.EXPANDED);
			section.setText("Pre detail section");
			section.setDescription("Text within the pre "
					+ "detail section will be present "
					+ "at the top of the output file. "
					+ "Can be used for create table scripts.");
			Composite composite = toolkit.createComposite(section);
			composite.setLayout(new GridLayout(1, false));
			Text text = toolkit.createText(composite, "", SWT.MULTI
					| SWT.BORDER);
			GridDataFactory.defaultsFor(section).grab(true, true)
					.applyTo(section);
			GridDataFactory.defaultsFor(text).grab(true, true).applyTo(text);
			section.setClient(composite);
			bindingContext.bindValue(
					SWTObservables.observeText(text, SWT.Modify),
					BeansObservables.observeValue(model, "pre"));
		}
		{
			Section section = toolkit.createSection(form.getBody(),
					Section.DESCRIPTION | Section.TITLE_BAR | Section.EXPANDED);
			section.setText("Pre detail section");
			section.setDescription("Text within the "
					+ "detail section will repeated for "
					+ "each row in input. "
					+ "Main migration script is generated here.");
			Composite composite = toolkit.createComposite(section);
			composite.setLayout(new GridLayout(1, false));
			Text text = toolkit.createText(composite, "", SWT.MULTI
					| SWT.BORDER);
			GridDataFactory.defaultsFor(section).grab(true, true)
					.applyTo(section);
			GridDataFactory.defaultsFor(text).grab(true, true).applyTo(text);
			section.setClient(composite);
			bindingContext.bindValue(
					SWTObservables.observeText(text, SWT.Modify),
					BeansObservables.observeValue(model, "detail"));
			mainControl = text;
		}
		{
			Section section = toolkit.createSection(form.getBody(),
					Section.DESCRIPTION | Section.TITLE_BAR | Section.EXPANDED);
			section.setText("Post detail section");
			section.setDescription("Text within the post "
					+ "detail section will be present "
					+ "at the bottom of the output file. "
					+ "Can be used for cleanup.");
			Composite composite = toolkit.createComposite(section);
			composite.setLayout(new GridLayout(1, false));
			Text text = toolkit.createText(composite, "", SWT.MULTI
					| SWT.BORDER);
			GridDataFactory.defaultsFor(section).grab(true, true)
					.applyTo(section);
			GridDataFactory.defaultsFor(text).grab(true, true).applyTo(text);
			section.setClient(composite);
			bindingContext.bindValue(
					SWTObservables.observeText(text, SWT.Modify),
					BeansObservables.observeValue(model, "post"));
		}
	}

	@Override
	public void dispose() {
		model.removePropertyChangeListener(propertyChangeListener);
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(
				resourceChangeListener);
		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		monitor.beginTask("Saving", 2);
		try {
			IFileEditorInput editorInput = (IFileEditorInput) getEditorInput()
					.getAdapter(IFileEditorInput.class);
			IFile file = editorInput.getFile();
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			migrationToolsDAO.write(baos, model);
			new SubProgressMonitor(monitor, 1).worked(1);
			file.setContents(new ByteArrayInputStream(baos.toByteArray()),
					IFile.KEEP_HISTORY, new SubProgressMonitor(monitor, 1));
			setDirty(false);
		} catch (CoreException e) {
			logAndShow(e);
		} catch (TransformerConfigurationException e) {
			logAndShow(e);
		} catch (TransformerFactoryConfigurationError e) {
			logAndShow(e);
		} catch (SAXException e) {
			logAndShow(e);
		} finally {
			monitor.done();
		}
	}

	@Override
	public void doSaveAs() {
		Shell shell = getSite().getWorkbenchWindow().getShell();
		SaveAsDialog dialog = new SaveAsDialog(shell);
		IFileEditorInput editorInput = (IFileEditorInput) getEditorInput()
				.getAdapter(IFileEditorInput.class);
		dialog.setOriginalFile(editorInput.getFile());
		dialog.open();
		IPath path = dialog.getResult();
		if (path == null)
			return;
		final IFile file = ResourcesPlugin.getWorkspace().getRoot()
				.getFile(path);
		try {
			new ProgressMonitorDialog(shell).run(false, false,
					new WorkspaceModifyOperation() {
						public void execute(final IProgressMonitor monitor)
								throws CoreException,
								InvocationTargetException, InterruptedException {
							try {
								ByteArrayOutputStream baos = new ByteArrayOutputStream();
								migrationToolsDAO.write(baos, model);
								monitor.worked(1);
								if (file.exists())
									file.setContents(new ByteArrayInputStream(
											baos.toByteArray()),
											IFile.KEEP_HISTORY,
											new SubProgressMonitor(monitor, 1));
								else
									file.create(
											new ByteArrayInputStream(baos
													.toByteArray()), false,
											monitor);
								setDirty(false);
							} catch (TransformerConfigurationException e) {
								throw new CoreException(new Status(
										IStatus.ERROR, Activator.PLUGIN_ID, e
												.getMessage(), e));
							} catch (TransformerFactoryConfigurationError e) {
								throw new CoreException(new Status(
										IStatus.ERROR, Activator.PLUGIN_ID, e
												.getMessage(), e));
							} catch (SAXException e) {
								throw new CoreException(new Status(
										IStatus.ERROR, Activator.PLUGIN_ID, e
												.getMessage(), e));
							}
						}
					});
			setInput(new FileEditorInput(file), false);
		} catch (InterruptedException e) {
			logAndShow(e);
		} catch (InvocationTargetException e) {
			logAndShow(e);
		}
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input, true);
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	private void loadFile() {
		try {
			IFileEditorInput editorInput = (IFileEditorInput) getEditorInput()
					.getAdapter(IFileEditorInput.class);
			InputStream in = editorInput.getFile().getContents();
			try {
				migrationToolsDAO.read(in, model);
				setDirty(false);
			} finally {
				in.close();
			}
		} catch (SAXException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	private void logAndShow(Throwable e) {
		final IStatus status;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else {
			status = new Status(IStatus.ERROR, Activator.PLUGIN_ID,
					e.getMessage(), e);
		}
		Activator.getDefault().getLog().log(status);
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				ErrorDialog.openError(getSite().getShell(), "Error", "Error",
						status);
			}
		};
		if (getSite().getShell().getDisplay().getThread() == Thread
				.currentThread()) {
			runnable.run();
		} else {
			getSite().getShell().getDisplay().syncExec(runnable);
		}
	}

	private void setDirty(boolean dirty) {
		if (this.dirty != dirty) {
			this.dirty = dirty;
			firePropertyChange(IEditorPart.PROP_DIRTY);
		}
	}

	@Override
	public void setFocus() {
		mainControl.setFocus();
	}

	@Override
	protected void setInput(IEditorInput input) {
		setInput(input, true);
	}

	protected void setInput(IEditorInput input, boolean loadFile) {
		super.setInput(input);
		IFileEditorInput fileEditorInput = (IFileEditorInput) getEditorInput()
				.getAdapter(IFileEditorInput.class);
		setPartName(fileEditorInput.getName());
		if (loadFile)
			loadFile();
	}
}
