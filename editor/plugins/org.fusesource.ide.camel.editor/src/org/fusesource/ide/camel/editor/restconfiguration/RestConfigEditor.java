/*******************************************************************************
 * Copyright (c) 2018 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.fusesource.ide.camel.editor.restconfiguration;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.EditorPart;
import org.fusesource.ide.camel.editor.CamelEditor;
import org.fusesource.ide.camel.editor.internal.CamelEditorUIActivator;
import org.fusesource.ide.camel.editor.internal.UIMessages;
import org.fusesource.ide.camel.model.service.core.model.CamelBasicModelElement;
import org.fusesource.ide.camel.model.service.core.model.CamelFile;
import org.fusesource.ide.camel.model.service.core.model.ICamelModelListener;
import org.fusesource.ide.foundation.core.util.Strings;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * @author bfitzpat
 */
public class RestConfigEditor extends EditorPart implements ICamelModelListener, ISelectionProvider {

	private CamelEditor parentEditor;
	private Composite parent;
	private ScrolledForm form;
	private FormToolkit toolkit;
	private Map<String, List<Object>> model;
	private ImageRegistry mImageRegistry;	
	private ListenerList<ISelectionChangedListener> listeners = new ListenerList<>();
	private Object selection;
	private Control selectedControl;
	private Combo componentCombo;
	private Text contextPathText;
	private Text portText;
	private Combo bindingModeCombo;
	private Text hostText;
	private Composite restOpsSection;
	private ListViewer restList;
	private RestEditorColorManager colorManager = new RestEditorColorManager();

	/**
	 *
	 * @param parentEditor
	 */
	public RestConfigEditor(CamelEditor parentEditor) {
		this.parentEditor = parentEditor;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		this.parentEditor.doSave(monitor);
	}

	@Override
	public void doSaveAs() {
		this.parentEditor.doSaveAs();
	}

	@Override
	public void init(IEditorSite editorSite, IEditorInput input)
			throws PartInitException {
		setSite(editorSite);
		setInput(input);
		getSite().setSelectionProvider(this);
		setSelection(StructuredSelection.EMPTY);
	}

	@Override
	public boolean isDirty() {
		return parentEditor.isDirty();
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite p) {
		getImages();

		this.parent = new Composite(p, SWT.FLAT);

		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 10;

		this.parent.setLayout(gl);
		createContents();

		reload();
		CamelFile designEditorModel = parentEditor.getDesignEditor().getModel();
		if (designEditorModel != null) {
			designEditorModel.addModelListener(this);
		}
	}

	@Override
	public void dispose() {
		if (parentEditor != null && parentEditor.getDesignEditor() != null && parentEditor.getDesignEditor().getModel() != null) {
			parentEditor.getDesignEditor().getModel().removeModelListener(this);
		}
		mImageRegistry.dispose();
		super.dispose();
	}

	@Override
	public void modelChanged() {
		// empty
	}

	@Override
	public void setFocus() {
		// empty
	}

	private void createContents() {
		toolkit = new FormToolkit(Display.getDefault());

		form = toolkit.createScrolledForm(parent);
		form.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, true, 1, 10));
		form.getBody().setLayout(new GridLayout(2, false));
		
		createRestConfigurationTabSection();
		
		createRestTabSection();
		restOpsSection = createRestOperationTabSection();

		form.layout();
		toolkit.decorateFormHeading(form.getForm());
	}

	private String compareTextAndTag(String text, String tag) {
		if (text == null || tag == null) {
			return null;
		}
		if (tag.equals(text) || text.startsWith(tag)) {
			return tag;
		}
		return null;
	}
	
	private String getTextForImage(String text) {
		if (compareTextAndTag(text, RestConfigConstants.GET_VERB) != null) {
			return RestConfigConstants.GET_VERB;
		}
		if (compareTextAndTag(text, RestConfigConstants.PUT_VERB) != null) {
			return RestConfigConstants.PUT_VERB;
		}
		if (compareTextAndTag(text, RestConfigConstants.POST_VERB) != null) {
			return RestConfigConstants.POST_VERB;
		}
		if (compareTextAndTag(text, RestConfigConstants.PATCH_VERB) != null) {
			return RestConfigConstants.PATCH_VERB;
		}
		if (compareTextAndTag(text, RestConfigConstants.DELETE_VERB) != null) {
			return RestConfigConstants.DELETE_VERB;
		}
		if (compareTextAndTag(text, RestConfigConstants.HEAD_VERB) != null) {
			return RestConfigConstants.HEAD_VERB;
		}
		if (compareTextAndTag(text, RestConfigConstants.TRACE_VERB) != null) {
			return RestConfigConstants.TRACE_VERB;
		}
		if (compareTextAndTag(text, RestConfigConstants.CONNECT_VERB) != null) {
			return RestConfigConstants.CONNECT_VERB;
		}
		if (compareTextAndTag(text, RestConfigConstants.OPTIONS_VERB) != null) {
			return RestConfigConstants.OPTIONS_VERB;
		}
		return null;
	}
	
	private Composite createVerbComposite(Composite parent, String labelText, String content) {
		Composite client=toolkit.createComposite(parent,SWT.BORDER);
		client.setBackground(colorManager.get(RestConfigConstants.REST_COLOR_LIGHT_BLUE));
		client.setLayout(new GridLayout(2, false));
		GridData gd = GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create();
		client.setLayoutData(gd);

		String graphicLabel = getTextForImage(labelText);
		Color imageColor = colorManager.getImageColorForType(graphicLabel);
		Color backgroundColor = colorManager.getBackgroundColorForType(graphicLabel);
		Color foregroundColor = colorManager.getForegroundColorForType(graphicLabel);
		client.setBackground(backgroundColor);
		client.addListener(SWT.MouseDown, new RestVerbSelectionListener());

		Label image=new Label(client,SWT.WRAP | SWT.BOLD | SWT.CENTER);
		image.setText(graphicLabel);
		image.setBackground(imageColor);
		image.setForeground(foregroundColor);
		image.setLayoutData(GridDataFactory.fillDefaults().hint(50, SWT.DEFAULT).create());
		image.addListener(SWT.MouseDown, new RestVerbSelectionListener());

		Label label=new Label(client,SWT.WRAP);
		label.setText(content);
		label.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,false));
		label.setBackground(client.getBackground());
		label.addListener(SWT.MouseDown, new RestVerbSelectionListener());

		return client;
	}

	private Object getDataFromSelectedUIElement(Control control) {
		Node data = null;
		if (!control.isDisposed()) {
			if (control.getData(RestConfigConstants.REST_VERB_FLAG) != null) {
				data = (Node) control.getData(RestConfigConstants.REST_VERB_FLAG);
			} else if (control.getData(RestConfigConstants.REST_TAG) != null) {
				data = (Node) control.getData(RestConfigConstants.REST_TAG);
			} else if (control.getData(RestConfigConstants.REST_CONFIGURATION_TAG) != null) {
				data = (Node) control.getData(RestConfigConstants.REST_CONFIGURATION_TAG);
			}
			if (data != null) {
				return new CamelBasicModelElement(null, data);
			}
			if (control.getParent() != null) {
				return getDataFromSelectedUIElement(control.getParent());
			}
		}
		return null;
	}

	class PushAction extends Action {
		public PushAction ( ) {
			super(null, IAction.AS_PUSH_BUTTON);
		}
	}

	class AddAction extends PushAction {
		@Override
		public void run() {
			MessageBox box = new MessageBox(Display.getCurrent().getActiveShell(), SWT.CANCEL | SWT.OK);
			box.setText("Add something"); //$NON-NLS-1$
			box.setMessage("In place of this message, we will actually add something."); //$NON-NLS-1$
			box.open();
		}
		@Override
		public String getToolTipText() {
			return "Add..."; //$NON-NLS-1$
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return mImageRegistry.getDescriptor(RestConfigConstants.IMG_DESC_ADD);
		}
	}

	class DeleteAction extends PushAction {
		@Override
		public void run() {
			MessageBox box = new MessageBox(Display.getCurrent().getActiveShell(), SWT.CANCEL | SWT.OK);
			box.setText("Delete something"); //$NON-NLS-1$
			box.setMessage("In place of this message, we will actually delete something."); //$NON-NLS-1$
			box.open();
		}
		@Override
		public String getToolTipText() {
			return "Delete..."; //$NON-NLS-1$
		}
		@Override
		public ImageDescriptor getImageDescriptor() {
			return mImageRegistry.getDescriptor(RestConfigConstants.IMG_DESC_DELETE);
		}
	}

	private void getImages() {
		mImageRegistry = new ImageRegistry();
		mImageRegistry.put(RestConfigConstants.IMG_DESC_ADD, ImageDescriptor
				.createFromURL(CamelEditorUIActivator.getDefault().getBundle()
						.getEntry(RestConfigConstants.IMG_DESC_ADD)));	
		mImageRegistry.put(RestConfigConstants.IMG_DESC_DELETE, ImageDescriptor
				.createFromURL(CamelEditorUIActivator.getDefault().getBundle()
						.getEntry(RestConfigConstants.IMG_DESC_DELETE)));	
	}

	private ToolBar createToolbar(Composite parent) {
		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		ToolBar toolbar = toolBarManager.createControl(parent);
		toolbar.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));

		// Add action to the tool bar
		AddAction action = new AddAction();
		toolBarManager.add(action);
		action.setEnabled(false);
		DeleteAction daction = new DeleteAction();
		toolBarManager.add(daction);
		daction.setEnabled(false);

		toolBarManager.update(true);
		return toolbar;
	}
	
	private Composite createRestTabSection() {
		Section section = toolkit.createSection(form.getBody(), Section.EXPANDED | Section.TWISTIE | Section.TITLE_BAR | Section.DESCRIPTION);
		section.setText(UIMessages.restConfigEditorRestSectionLabelText);
		section.setDescription(UIMessages.restConfigEditorRestTabDescription);
		section.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(1, 5).create());
		section.setLayout(new GridLayout(2, false));

		ToolBar toolbar = createToolbar(section);
		section.setTextClient(toolbar);
		
		Composite client=toolkit.createComposite(section,SWT.NONE);
		client.setLayout(new GridLayout(1, false));
		client.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(1, 5).create());
		client.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		
		restList = new ListViewer(client, SWT.V_SCROLL | SWT.SINGLE | SWT.BORDER);
		restList.getControl().setLayoutData(GridDataFactory.fillDefaults().grab(true, true).span(1, 5).create());
		restList.setContentProvider(ArrayContentProvider.getInstance());
		restList.setLabelProvider(new RestLabelProvider());
		restList.addSelectionChangedListener(event -> {
			if (event.getStructuredSelection().getFirstElement() instanceof Element) {
				clearUI();
				
				Element restElement = (Element) event.getStructuredSelection().getFirstElement();
				// TODO call setSelection(new StructuredSelection(new CamelBasicModelElement(null, restElement)))
				// in next iteration
				
				if (restElement.getChildNodes().getLength() > 0) {
					for (int i = 0; i < restElement.getChildNodes().getLength(); i++) {
						Node child = restElement.getChildNodes().item(i);
						if (child instanceof Element) {
							Element elChild = (Element) child;
							String verbUri = elChild.getAttribute("uri"); //$NON-NLS-1$
							Composite operation = createVerbComposite(restOpsSection, elChild.getTagName(), verbUri);
							operation.setData(RestConfigConstants.REST_VERB_FLAG, elChild);
						}
					}
				}
				restOpsSection.layout();
			}
		});

		section.setClient(client);
		
		return client;
	}
	
	private Composite createRestOperationTabSection() {
		Section section = toolkit.createSection(form.getBody(), Section.EXPANDED | Section.TWISTIE | Section.TITLE_BAR | Section.DESCRIPTION);
		section.setText(UIMessages.restConfigEditorRestSectionLabel);
		section.setDescription(UIMessages.restConfigEditorRestOperationTabDescription);
		GridData gd = new GridData(SWT.FILL,SWT.FILL,true,true);
		section.setLayoutData(gd);
		section.setLayout(new GridLayout(2, false));

		ToolBar toolbar = createToolbar(section);
		section.setTextClient(toolbar);

		Composite client=toolkit.createComposite(section,SWT.BORDER);
		client.setLayout(new GridLayout(1, false));
		client.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		client.setLayoutData(new GridData(SWT.FILL,SWT.FILL,true,true));
		
		section.setClient(client);
		
		return client;
	}

	class RestLabelProvider extends LabelProvider {

		@Override
		public String getText(Object element) {
			if (element instanceof Element) {
				Element restElement = (Element) element;
				if (getAttrValue(restElement, "path") != null) { //$NON-NLS-1$
					return getAttrValue(restElement, "path"); //$NON-NLS-1$
				}
			}
			return super.getText(element);
		}
		
	}

	private Composite createRestConfigurationTabSection() {
		Section section = toolkit.createSection(form.getBody(), Section.EXPANDED | Section.TWISTIE | Section.TITLE_BAR | Section.DESCRIPTION);
		section.setText(UIMessages.restConfigEditorRestConfigSectionLabelText);
		section.setDescription(UIMessages.restConfigEditorRestConfigurationTabDescription);
		GridData gd = GridDataFactory.fillDefaults().grab(true, false).span(2, 1).create();
		section.setLayoutData(gd);
		section.setLayout(new GridLayout(2, false));

		ToolBar toolbar = createToolbar(section);
		section.setTextClient(toolbar);

		Composite client=toolkit.createComposite(section,SWT.NONE);
		client.setLayout(new GridLayout(2, false));
		client.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
		
		toolkit.createLabel(client, UIMessages.restConfigEditorComponentLabel);
		componentCombo = new Combo(client, SWT.DROP_DOWN);
		componentCombo.add("netty-http"); //$NON-NLS-1$
		componentCombo.add("netty4-http"); //$NON-NLS-1$
		componentCombo.add("jetty"); //$NON-NLS-1$
		componentCombo.add("restlet"); //$NON-NLS-1$
		componentCombo.add("servlet"); //$NON-NLS-1$
		componentCombo.add("spark-rest"); //$NON-NLS-1$
		componentCombo.add("undertow"); //$NON-NLS-1$
		componentCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		componentCombo.setEnabled(false);
		
		toolkit.createLabel(client, UIMessages.restConfigEditorContextPathLabel);
		contextPathText = toolkit.createText(client, "", SWT.BORDER); //$NON-NLS-1$
		contextPathText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		contextPathText.setEnabled(false);
		
		toolkit.createLabel(client, UIMessages.restConfigEditorPortLabel);
		portText = toolkit.createText(client, "", SWT.BORDER); //$NON-NLS-1$
		portText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		portText.setEnabled(false);

		toolkit.createLabel(client, UIMessages.restConfigEditorBindingModeLabel);
		bindingModeCombo = new Combo(client, SWT.DROP_DOWN);
		bindingModeCombo.add("auto"); //$NON-NLS-1$
		bindingModeCombo.add("json"); //$NON-NLS-1$
		bindingModeCombo.add("json_xml"); //$NON-NLS-1$
		bindingModeCombo.add("off"); //$NON-NLS-1$
		bindingModeCombo.add("xml"); //$NON-NLS-1$
		bindingModeCombo.setText("off"); //$NON-NLS-1$
		bindingModeCombo.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		bindingModeCombo.setEnabled(false);

		toolkit.createLabel(client, UIMessages.restConfigEditorHostLabel);
		hostText = toolkit.createText(client, "", SWT.BORDER); //$NON-NLS-1$
		hostText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
		hostText.setEnabled(false);
		
		section.setClient(client);
		
		return client;
	}

	private void buildModel() {
		CamelFile cf = parentEditor.getDesignEditor().getModel();
		model = new RestModelBuilder().build(cf);
	}

	private void clearUI() {
		if (restOpsSection != null && !restOpsSection.isDisposed()) {
			Control[] children = restOpsSection.getChildren();
			for (int i = 0; i < children.length; i++) {
				Control child = children[i];
				child.dispose();
			}
			restOpsSection.layout();
		}
	}

	private String getAttrValue(Element element, String attrName) {
		if (element.getAttribute(attrName) != null) {
			String tempValue = element.getAttribute(attrName);
			if (!Strings.isEmpty(tempValue)) {
				return tempValue;
			}
		}
		return null;
	}
	
	private void refreshRestConfigurationSection() {
		componentCombo.setText("");
		bindingModeCombo.setText("");
		portText.setText("");
		contextPathText.setText("");
		hostText.setText("");
		
		if (!getModel().get(RestConfigConstants.REST_CONFIGURATION_TAG).isEmpty()) {
			Element restConfig = (Element) getModel().get(RestConfigConstants.REST_CONFIGURATION_TAG).get(0);
			
			String component = ""; //$NON-NLS-1$
			if (getAttrValue(restConfig, "component") != null) { //$NON-NLS-1$
				component = getAttrValue(restConfig, "component"); //$NON-NLS-1$
			}
			
			String bindingMode = "off"; //$NON-NLS-1$
			if (getAttrValue(restConfig, "bindingMode") != null) { //$NON-NLS-1$
				bindingMode = getAttrValue(restConfig, "bindingMode"); //$NON-NLS-1$
			}
			String port = ""; //$NON-NLS-1$
			if (getAttrValue(restConfig, "port") != null) { //$NON-NLS-1$
				port = getAttrValue(restConfig, "port"); //$NON-NLS-1$
			}
			String contextPath = ""; //$NON-NLS-1$
			if (getAttrValue(restConfig, "contextPath") != null) { //$NON-NLS-1$
				contextPath = getAttrValue(restConfig, "contextPath"); //$NON-NLS-1$
			}
			String host = ""; //$NON-NLS-1$
			if (getAttrValue(restConfig, "host") != null) { //$NON-NLS-1$
				host = getAttrValue(restConfig, "host"); //$NON-NLS-1$
			}
			
			componentCombo.setText(component);
			bindingModeCombo.setText(bindingMode);
			portText.setText(port);
			contextPathText.setText(contextPath);
			hostText.setText(host);
		}
	}
	
	private void refreshRestSection() {
		restList.setInput(null);
		if (!getModel().get(RestConfigConstants.REST_TAG).isEmpty()) {
			restList.setInput(getModel().get(RestConfigConstants.REST_TAG));
			restList.setSelection(new StructuredSelection(restList.getElementAt(0)), true);
		}
	}	
	
	public void reload() {
		buildModel();
		refreshRestConfigurationSection();
		clearUI();
		refreshRestSection();
		form.layout(true);
		toolkit.decorateFormHeading(form.getForm());
		setSelection(StructuredSelection.EMPTY);
	}

	/**
	 * @return the model
	 */
	public Map<String, List<Object>> getModel() {
		return model;
	}

	@Override
	public void addSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.add(listener);
	}

	@Override
	public ISelection getSelection() {
		// TODO return selection if not null return new StructuredSelection(selection)
		// in next iteration
		return StructuredSelection.EMPTY;
	}

	@Override
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		listeners.remove(listener);		
	}

	@Override
	public void setSelection(ISelection selection) {
		Object[] list = listeners.getListeners();
		for (int i = 0; i < list.length; i++) {
			((ISelectionChangedListener) list[i]).selectionChanged(new SelectionChangedEvent(this, selection));
		}
	}

	class RestVerbSelectionListener implements Listener {
		@Override
		public void handleEvent(Event event) {
			Control newControl = (Control) event.widget;
			updateSelectionDisplay(selectedControl, newControl);
			selectedControl = newControl;
			selection = getDataFromSelectedUIElement(newControl);
			if (selection != null) {
				// TODO call setSelection (new StructuredSelection(selection) ) in next iteration
			}
		}

		private void updateSelectionDisplay(Control oldControl, Control newControl) {
			if (oldControl != null && getDataFromSelectedUIElement(oldControl) != null) {
				CamelBasicModelElement node = (CamelBasicModelElement) getDataFromSelectedUIElement(oldControl);
				Color background = colorManager.getBackgroundColorForType(""); //$NON-NLS-1$
				if (node != null && node.getXmlNode() != null) {
					background = colorManager.getBackgroundColorForType(node.getXmlNode().getNodeName());
				}
				updateBorder(oldControl, background);
			}
			updateBorder(newControl, newControl.getDisplay().getSystemColor(SWT.COLOR_LIST_SELECTION));
		}
		
		private void updateBorder(Control control, Color color) {
			if (control instanceof Composite) {
				Composite composite = (Composite) control;
				composite.setBackground(color);
			} else if (control.getParent() != null) {
				updateBorder(control.getParent(), color);
			}
		}
	}
	
}
