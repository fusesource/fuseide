/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.projecttemplates.wizards.pages;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jst.server.core.FacetUtil;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.runtime.RuntimeManager;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeLifecycleListener;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.server.ui.ServerUIUtil;
import org.fusesource.ide.camel.model.service.core.catalog.CamelModelFactory;
import org.fusesource.ide.foundation.core.util.Strings;
import org.fusesource.ide.foundation.ui.util.Widgets;
import org.fusesource.ide.projecttemplates.internal.Messages;
import org.fusesource.ide.projecttemplates.internal.ProjectTemplatesActivator;


/**
 * @author lhein
 */
public class FuseIntegrationProjectWizardRuntimeAndCamelPage extends WizardPage {

	private static final String UNKNOWN_CAMEL_VERSION = "unknown";
	private static final String FUSE_RUNTIME_PREFIX = "org.fusesource.ide.fuseesb.runtime.";
	private static final String EAP_RUNTIME_PREFIX = "org.jboss.ide.eclipse.as.runtime.eap.";
	private static final String CAMEL_CORE_LIB_PREFIX = "camel-core-";
	private static final String CAMEL_CORE_LIB_SUFFIX = ".jar";

	
	private Label runtimeLabel;
	private Combo runtimeCombo;
	private Button runtimeNewButton;
	
	private Map<String, IRuntime> serverRuntimes;
	private IRuntimeLifecycleListener listener;
	private String lastSelectedRuntime;
	
	private Label camelVersionLabel;
	private Combo camelVersionCombo;
	private StyledText camelInfoText;
	private Label warningIconLabel;
	
	/**
	 * 
	 */
	public FuseIntegrationProjectWizardRuntimeAndCamelPage() {
		super(Messages.newProjectWizardRuntimePageName);
		setTitle(Messages.newProjectWizardRuntimePageTitle);
		setDescription(Messages.newProjectWizardRuntimePageDescription);
		setImageDescriptor(ProjectTemplatesActivator.imageDescriptorFromPlugin(ProjectTemplatesActivator.PLUGIN_ID, ProjectTemplatesActivator.IMAGE_CAMEL_PROJECT_ICON));
		setPageComplete(false);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		container.setLayout(new GridLayout(3, false));

		Group runtimeGrp = new Group(container, SWT.NONE);
		GridData runtimeGrpData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		runtimeGrp.setLayout(new GridLayout(3, false));
		runtimeGrp.setLayoutData(runtimeGrpData);
		runtimeGrp.setText(Messages.newProjectWizardRuntimePageRuntimeGroupLabel);
		
		runtimeLabel = new Label(runtimeGrp, SWT.NONE);
		GridData runtimeLabelData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		runtimeLabel.setLayoutData(runtimeLabelData);
		runtimeLabel.setText(Messages.newProjectWizardRuntimePageRuntimeLabel);

		runtimeCombo = new Combo(runtimeGrp, SWT.NONE | SWT.READ_ONLY);
		GridData runtimeComboData = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
		runtimeCombo.setLayoutData(runtimeComboData);
		runtimeCombo.setToolTipText(Messages.newProjectWizardRuntimePageRuntimeDescription);
		runtimeCombo.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			@Override
			public void modifyText(ModifyEvent e) {
				lastSelectedRuntime = runtimeCombo.getText();
				preselectCamelVersionForRuntime(determineRuntimeCamelVersion(getSelectedRuntime()));
				validate();
			}
		});

		try {
			configureRuntimeCombo();
		} catch (CoreException ex) {
			ProjectTemplatesActivator.pluginLog().logError(ex);
		}

		runtimeNewButton = new Button(runtimeGrp, SWT.NONE);
		GridData runtimeNewButtonData = new GridData(SWT.FILL, SWT.CENTER, false, false);
		runtimeNewButton.setLayoutData(runtimeNewButtonData);
		runtimeNewButton.setText(Messages.newProjectWizardRuntimePageRuntimeNewButtonLabel);
		runtimeNewButton.setToolTipText(Messages.newProjectWizardRuntimePageRuntimeNewButtonDescription);
		runtimeNewButton.addSelectionListener(new SelectionAdapter() {
			/*
			 * (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				String[] oldRuntimes = runtimeCombo.getItems();
				boolean created = ServerUIUtil.showNewRuntimeWizard(getShell(), null, null);
				if (created) {
					String[] newRuntimes = runtimeCombo.getItems();
					String newRuntime = getNewRuntime(oldRuntimes, newRuntimes);
					for (int i=0; i<runtimeCombo.getItemCount(); i++) {
						if (runtimeCombo.getItem(i).equalsIgnoreCase(newRuntime)) {
							runtimeCombo.select(i);
							break;
						}
					}
				}
			}
		});
		
		new Label(runtimeGrp, SWT.None);
		
		Group camelGrp = new Group(container, SWT.NONE);
		GridData camelGrpData = new GridData(SWT.FILL, SWT.FILL, true, true, 3, 20);
		camelGrp.setLayout(new GridLayout(3, false));
		camelGrp.setLayoutData(camelGrpData);
		camelGrp.setText(Messages.newProjectWizardRuntimePageCamelGroupLabel);
		
		camelVersionLabel = new Label(camelGrp, SWT.NONE);
		GridData camelLabelData = new GridData(SWT.FILL, SWT.CENTER, true, false, 3, 1);
		camelVersionLabel.setLayoutData(camelLabelData);
		camelVersionLabel.setText(Messages.newProjectWizardRuntimePageCamelLabel);

		camelVersionCombo = new Combo(camelGrp, SWT.RIGHT | SWT.READ_ONLY);
		GridData camelComboData = new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1);
		camelVersionCombo.setLayoutData(camelComboData);
		camelVersionCombo.setItems(getSupportedCamelVersions());
		camelVersionCombo.select(Math.max(camelVersionCombo.getItemCount()-1, 0));
		camelVersionCombo.setToolTipText(Messages.newProjectWizardRuntimePageCamelDescription);
		camelVersionCombo.addSelectionListener(new SelectionAdapter() {
			/* (non-Javadoc)
			 * @see org.eclipse.swt.events.SelectionAdapter#widgetSelected(org.eclipse.swt.events.SelectionEvent)
			 */
			@Override
			public void widgetSelected(SelectionEvent e) {
				validate();
			}
		});
// TODO: leaving that out until we decide to support other camel versions than the ones we ship		
//		camelVersionCombo.addFocusListener(new FocusAdapter() {
//			/* (non-Javadoc)
//			 * @see org.eclipse.swt.events.FocusAdapter#focusLost(org.eclipse.swt.events.FocusEvent)
//			 */
//			@Override
//			public void focusLost(FocusEvent e) {
//				super.focusLost(e);
//				validate();
//			}
//			
//			/* (non-Javadoc)
//			 * @see org.eclipse.swt.events.FocusAdapter#focusGained(org.eclipse.swt.events.FocusEvent)
//			 */
//			@Override
//			public void focusGained(FocusEvent e) {
//				super.focusGained(e);
//				setPageComplete(false);
//			}
//		});

		new Label(camelGrp, SWT.None);
	
		warningIconLabel = new Label(camelGrp, SWT.None);
		GridData camelLblData = new GridData(SWT.FILL, SWT.TOP, false, true, 1, 20);
		camelLblData.verticalIndent = 20;
		warningIconLabel.setImage(getSWTImage(SWT.ICON_WARNING));
		warningIconLabel.setLayoutData(camelLblData);
		warningIconLabel.setVisible(false);
		
		camelInfoText = new StyledText(camelGrp, SWT.WRAP | SWT.MULTI);
		GridData camelInfoData = new GridData(SWT.FILL, SWT.TOP, true, true, 2, 20);
		camelInfoData.verticalIndent = 0;
		camelInfoData.heightHint = 150;
		camelInfoText.setLayoutData(camelInfoData);
		camelInfoText.setEnabled(false);
		camelInfoText.setEditable(false);
		camelInfoText.setBackground(container.getBackground());

		new Label(camelGrp, SWT.None);
		
		setControl(container);
		
		listener = new IRuntimeLifecycleListener() {
			
			@Override
			public void runtimeRemoved(IRuntime runtime) {
				runInUIThread();
			}
			
			@Override
			public void runtimeChanged(IRuntime runtime) {
				runInUIThread();
			}
			
			@Override
			public void runtimeAdded(IRuntime runtime) {
				runInUIThread();
			}
			
			private void runInUIThread() {
				Display.getDefault().asyncExec(new Runnable() {
					
					@Override
					public void run() {
						try {
							configureRuntimeCombo();
						} catch (CoreException ex) {
							ProjectTemplatesActivator.pluginLog().logError("Unable to handle runtime change event", ex);
						}
					}
				});
			}
			
		};
		ServerCore.addRuntimeLifecycleListener(listener);
		validate();
	}
	
	private String[] getSupportedCamelVersions() {
		return CamelModelFactory.getSupportedCamelVersions().stream().sorted().toArray(String[]::new);
	}
	
	private void configureRuntimeCombo() throws CoreException {
		if (Widgets.isDisposed(runtimeCombo)) {
			return;
		}
		int i =0, selectedRuntimeIdx = 0;
		String lastUsedRuntime = lastSelectedRuntime;

		serverRuntimes = getServerRuntimes(null);
		runtimeCombo.removeAll();
		runtimeCombo.add(Messages.newProjectWizardRuntimePageNoRuntimeSelectedLabel);
		runtimeCombo.select(0);
		for (Map.Entry<String, IRuntime> entry : serverRuntimes.entrySet()) {
			runtimeCombo.add(entry.getKey());
			++i;
			IRuntime runtime = entry.getValue();
			if (lastUsedRuntime != null && lastUsedRuntime.equals(runtime.getId())) {
				selectedRuntimeIdx = i;
			}
		}
				
		if (selectedRuntimeIdx > 0) {
			runtimeCombo.select(selectedRuntimeIdx);
		}
	}
	
	private Map<String, IRuntime> getServerRuntimes(IProjectFacetVersion facetVersion) {
		Set<org.eclipse.wst.common.project.facet.core.runtime.IRuntime> runtimesSet;
		if (facetVersion == null) {
			runtimesSet = RuntimeManager.getRuntimes();
		} else {
			runtimesSet = RuntimeManager.getRuntimes(Collections.singleton(facetVersion));
		}
		
		Map<String, IRuntime> runtimesMap = new LinkedHashMap<>();
		for (org.eclipse.wst.common.project.facet.core.runtime.IRuntime r : runtimesSet) {
			IRuntime serverRuntime = FacetUtil.getRuntime(r);
			if (serverRuntime != null) {
				runtimesMap.put(r.getLocalizedName(), serverRuntime);
			}
		}
		return runtimesMap;
	}
	
	private String getNewRuntime(String[] oldRuntimes, String[] newRuntimes) {
		for (String newRuntime : newRuntimes) {
			boolean found = false;
			for (String oldRuntime : oldRuntimes) {
				if (newRuntime.equals(oldRuntime)) {
					found = true;
					break;
				}
			}
			if (!found) return newRuntime;
		}
		return Messages.newProjectWizardRuntimePageNoRuntimeSelectedLabel;
	}

	private void validate() {
		// if runtime is selected other than NO RUNTIME
		if (getSelectedRuntime() != null) {
			// determine the camel version of that runtime
			String runtimeCamelVersion = determineRuntimeCamelVersion(getSelectedRuntime());

			if (UNKNOWN_CAMEL_VERSION.equals(runtimeCamelVersion)) {
				if (!Widgets.isDisposed(camelVersionCombo)) camelVersionCombo.setEnabled(true);
			} else {
				// and compare if selected camel version fits that version
				if (!isCompatible(runtimeCamelVersion, getSelectedCamelVersion())) {
					// Display warning and suggest the correct version
					camelInfoText.setText(NLS.bind(Messages.newProjectWizardRuntimePageCamelVersionsDontMatchWarning, runtimeCamelVersion));
				} else {
					camelInfoText.setText("");
				}
				if (!Widgets.isDisposed(camelVersionCombo)) camelVersionCombo.setEnabled(false);
			}
		} else {
			if (!Widgets.isDisposed(camelVersionCombo)) camelVersionCombo.setEnabled(true);

			if (!Widgets.isDisposed(camelInfoText)) {
				camelInfoText.setText("");
			}			
		}
// TODO: leaving that out until we decide to support other camel versions than the ones we ship
//		try {
//			getWizard().getContainer().run(false, false, new IRunnableWithProgress() {
//				@Override
//				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
//					monitor.beginTask(Messages.newProjectWizardRuntimePageResolveDependencyStatus, IProgressMonitor.UNKNOWN);
//					validateCamelVersion();
//					monitor.done();
//				}
//			});
//		} catch (Exception ex) {
//			ProjectTemplatesActivator.pluginLog().logError(ex);
//		}
		
		if (!Widgets.isDisposed(warningIconLabel) && !Widgets.isDisposed(camelInfoText)) { 
			warningIconLabel.setVisible(camelInfoText.getText().length()>0);
		}
		
		if (!Widgets.isDisposed(runtimeCombo) && !Widgets.isDisposed(camelVersionCombo)) {
			setPageComplete(!Strings.isBlank(runtimeCombo.getText()) && 
							!Strings.isBlank(camelVersionCombo.getText()) &&
							!warningIconLabel.isVisible());
		}
	}
	
	private void validateCamelVersion() {
		if (getSelectedCamelVersion() != null && !isCamelVersionValid(getSelectedCamelVersion())) {
			if (!Widgets.isDisposed(camelInfoText)) {
				camelInfoText.setText(NLS.bind(Messages.newProjectWizardRuntimePageCamelVersionInvalidWarning, getSelectedCamelVersion()));
				setPageComplete(false);
			}
		} else {
			if (!Widgets.isDisposed(camelInfoText)) {
				camelInfoText.setText("");
			}	
		}
		if (!Widgets.isDisposed(warningIconLabel) && !Widgets.isDisposed(camelInfoText)) { 
			warningIconLabel.setVisible(camelInfoText.getText().length()>0);
		}
	}
	
	private boolean isCamelVersionValid(String camelVersion) {
		// return true for supported versions from the combo list
		for (String supportedVersion : camelVersionCombo.getItems()) {
			if (supportedVersion.equals(camelVersion)) return true;
		}
		
		boolean camelVersionAvailable = false;
		try {
			Artifact camelCore = MavenPlugin.getMaven().resolve("org.apache.camel", 
																"camel-core", 
																camelVersion, 
																"jar", 
																null, 
																null, 
																new NullProgressMonitor());
			camelVersionAvailable = camelCore != null;
		} catch (CoreException ex) {
			camelVersionAvailable = false;
		}	
		return camelVersionAvailable;
	}
	
	public void preselectCamelVersionForRuntime(String runtimeCamelVersion) {
		if (Widgets.isDisposed(camelVersionCombo)) return;
		
		if (UNKNOWN_CAMEL_VERSION.equals(runtimeCamelVersion)) {
			camelVersionCombo.setEnabled(true);
		} else {
			List<String> compatibleVersions = Arrays.stream(camelVersionCombo.getItems())
					.filter(camelVersion -> isCompatible(runtimeCamelVersion, camelVersion))
					.collect(Collectors.toList());
			if (!compatibleVersions.isEmpty()) {
				if (compatibleVersions.contains(runtimeCamelVersion)) {
					camelVersionCombo.setText(runtimeCamelVersion);
				} else {
					Collections.sort(compatibleVersions);
					camelVersionCombo.setText(compatibleVersions.get(compatibleVersions.size()-1));
				}
			} else {
				camelVersionCombo.select(Math.max(0, camelVersionCombo.getItemCount()-1));
			}
		}		
	}
	
	private String determineRuntimeCamelVersion(IRuntime runtime) {
		if (runtime != null) {
			if (isJBossFuseRuntime(runtime)) {
				File camelFolder = runtime.getLocation().append("system").append("org").append("apache").append("camel").append("camel-core").toFile();
				String[] versions = camelFolder.list();
				if (versions.length==1) {
					return versions[0];
				}
			} else if (isFuseOnEAPRuntime(runtime)) {
				File camelFolder = runtime.getLocation().append("modules").append("system").append("layers").append("fuse").append("org").append("apache").append("camel").append("core").append("main").toFile();
				String[] versions = camelFolder.list(new FilenameFilter() {
					@Override
					public boolean accept(File dir, String name) {
						return 	name.toLowerCase().startsWith(CAMEL_CORE_LIB_PREFIX) &&
								name.toLowerCase().endsWith(CAMEL_CORE_LIB_SUFFIX);
					}
				});
				if (versions.length==1) {
					String jarName = versions[0];
					String version = jarName.substring(jarName.indexOf(CAMEL_CORE_LIB_PREFIX)+CAMEL_CORE_LIB_PREFIX.length(), jarName.indexOf(CAMEL_CORE_LIB_SUFFIX));
					return version;
				}
			}
		}
		return UNKNOWN_CAMEL_VERSION;
	}
	
	/**
	 * checks whether the given runtime is an EAP runtime
	 * 
	 * @param runtime	the runtime to check
	 * @return	true if the runtime is an EAP runtime
	 */
	private boolean isFuseOnEAPRuntime(IRuntime runtime) {
		return runtime.getRuntimeType().getId().startsWith(EAP_RUNTIME_PREFIX);
	}
	
	/**
	 * checks whether the given runtime is a fuse runtime
	 * 
	 * @param runtime	the runtime to check
	 * @return	true if the runtime is a jboss fuse runtime
	 */
	private boolean isJBossFuseRuntime(IRuntime runtime) {
		return runtime.getRuntimeType().getId().startsWith(FUSE_RUNTIME_PREFIX);
	}
	
	/**
	 * checks if the two versions are identical for major and minor version
	 * 
	 * @param runtimeCamelVersion	the camel version in the runtime
	 * @param selectedCamelVersion	the camel version selected in the wizard
	 * @return	true if compatible
	 */
	private boolean isCompatible(String runtimeCamelVersion, String selectedCamelVersion) {
		String[] runtimeVersionParts = runtimeCamelVersion.split("\\.");
		String[] camelVersionParts = selectedCamelVersion.split("\\.");
		boolean rh_branded_rcv = runtimeCamelVersion.indexOf(".redhat-") != -1; 
		boolean rh_branded_scv = selectedCamelVersion.indexOf(".redhat-") != -1;
		
		return runtimeVersionParts.length>1 && camelVersionParts.length>1 &&
			   runtimeVersionParts[0].equals(camelVersionParts[0]) &&
			   runtimeVersionParts[1].equals(camelVersionParts[1]) && 
			   rh_branded_rcv == rh_branded_scv;
	}
	
	/**
	 * returns the selected runtime
	 * 
	 * @return
	 */
	public IRuntime getSelectedRuntime() {
		if (!Widgets.isDisposed(runtimeCombo)) {
			String runtimeId = runtimeCombo.getText();
			if (!runtimeId.equalsIgnoreCase(Messages.newProjectWizardRuntimePageNoRuntimeSelectedLabel)) {
				return serverRuntimes.get(runtimeId);	
			}			
		}
		return null;
	}
	
	/**
	 * returns the selected camel version
	 * 
	 * @return
	 */
	public String getSelectedCamelVersion() {
		if (!Widgets.isDisposed(camelVersionCombo)) {
			return camelVersionCombo.getText();
		}
		return null;
	}
	
	/**
	 * Get an <code>Image</code> from the provide SWT image constant.
	 *
	 * @param imageID
	 *            the SWT image constant
	 * @return image the image
	 */
	private Image getSWTImage(final int imageID) {
		Shell shell = getShell();
		final Display display;
		if (!Widgets.isDisposed(shell)) {
			shell = shell.getParent().getShell();
		}
		if (Widgets.isDisposed(shell)) {
			display = Display.getCurrent();
			// The dialog should be always instantiated in UI thread.
			// However it was possible to instantiate it in other threads
			// (the code worked in most cases) so the assertion covers
			// only the failing scenario. See bug 107082 for details.
			Assert.isNotNull(display,
					"The dialog should be created in UI thread"); //$NON-NLS-1$
		} else {
			display = shell.getDisplay();
		}

		final Image[] image = new Image[1];
		display.syncExec(new Runnable() {
			@Override
			public void run() {
				image[0] = display.getSystemImage(imageID);
			}
		});

		return image[0];
	}
}
