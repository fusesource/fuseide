/******************************************************************************
 * Copyright (c) 2015 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors: JBoss by Red Hat - Initial implementation.
 *****************************************************************************/
package org.jboss.tools.fuse.transformation.editor.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.jboss.tools.fuse.transformation.editor.Activator;
import org.jboss.tools.fuse.transformation.editor.internal.util.TransformationConfig;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util.Colors;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util.Decorations;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util.Images;
import org.jboss.tools.fuse.transformation.model.Model;

/**
 *
 */
public class ModelViewer extends Composite {

    private static final String PREFERENCE_PREFIX = ModelViewer.class.getName() + ".";
    private static final String FILTER_MAPPED_FIELDS_PREFERENCE = ".filterMappedFields";
    private static final String FILTER_TYPES_PREFERENCE = ".filterTypes";

    final TransformationConfig config;
    Model rootModel;
    boolean showFieldTypes;
    boolean hideMappedFields;
    final Map<String, List<Model>> searchMap = new HashMap<>();
    final Set<Model> searchResults = new HashSet<>();
    protected boolean showMappedFieldsButton = true;
    private ToolItem filterMappedFieldsButton;
    protected boolean showSearchField = true;
    private Text searchText;
    private Label searchLabel;
    private Label clearSearchLabel;

    /**
     *
     */
    protected final TreeViewer treeViewer;

    /**
     * @param config
     * @param parent
     * @param rootModel
     * @param potentialDropTargets
     * @param preferenceId
     */
    public ModelViewer(final TransformationConfig config,
                       final Composite parent,
                       final Model rootModel,
                       final List<PotentialDropTarget> potentialDropTargets,
                       final String preferenceId) {
        super(parent, SWT.BORDER);
        setBackground(Colors.BACKGROUND);
        setViewOptions();

        this.config = config;
        this.rootModel = rootModel;

        updateSearchMap(rootModel);

        setLayout(GridLayoutFactory.swtDefaults().numColumns(2).create());
        final IPreferenceStore prefs = Activator.plugin().getPreferenceStore();

        final ToolBar toolBar = new ToolBar(this, SWT.NONE);
        toolBar.setBackground(getBackground());
        final ToolItem collapseAllButton = new ToolItem(toolBar, SWT.PUSH);
        collapseAllButton.setImage(Images.COLLAPSE_ALL);
        final ToolItem filterTypesButton = new ToolItem(toolBar, SWT.CHECK);
        filterTypesButton.setImage(Images.FILTER);
        filterTypesButton.setToolTipText("Show types");

        if (showMappedFieldsButton) {
            filterMappedFieldsButton = new ToolItem(toolBar, SWT.CHECK);
            filterMappedFieldsButton.setImage(Images.HIDE_MAPPED);
            filterMappedFieldsButton.setToolTipText("Hide mapped properties");
        }

        if (showSearchField) {
            Composite searchPane = new Composite(this, SWT.NONE);
            searchPane.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
            searchPane.setLayout(GridLayoutFactory.swtDefaults().numColumns(3).create());
            searchPane.setToolTipText("Search");
            searchPane.setBackground(getBackground());
            searchLabel = new Label(searchPane, SWT.NONE);
            searchLabel.setImage(Images.SEARCH);
            searchLabel.setToolTipText("Search");
            searchLabel.setBackground(getBackground());
            searchText = new Text(searchPane, SWT.NONE);
            searchText.setLayoutData(GridDataFactory.fillDefaults().grab(true, false).create());
            searchText.setToolTipText("Search");
            clearSearchLabel = new Label(searchPane, SWT.NONE);
            clearSearchLabel.setImage(Images.CLEAR);
            clearSearchLabel.setToolTipText("Search");
            clearSearchLabel.setBackground(getBackground());
            searchPane.addPaintListener(Util.ovalBorderPainter());
        }

        treeViewer = new TreeViewer(this, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
        treeViewer.getTree().setLayoutData(GridDataFactory.fillDefaults()
                                                          .span(2, 1)
                                                          .grab(true, true)
                                                          .create());
        treeViewer.setComparator(new ViewerComparator() {

            @Override
            public int compare(final Viewer viewer,
                               final Object model1,
                               final Object model2) {
                if (model1 instanceof Model && model2 instanceof Model) {
                    return ((Model) model1).getName().compareTo(((Model) model2).getName());
                }
                return 0;
            }
        });
        treeViewer.setLabelProvider(new LabelProvider());
        ColumnViewerToolTipSupport.enableFor(treeViewer);
        treeViewer.setContentProvider(new ContentProvider());
        if (showSearchField) {
            treeViewer.addFilter(new ViewerFilter() {

                @Override
                public boolean select(final Viewer viewer,
                                      final Object parentElement,
                                      final Object element) {
                    return show(element, !searchText.getText().trim().isEmpty());
                }
            });
        }
        if (potentialDropTargets != null) {
            treeViewer.addDragSupport(DND.DROP_MOVE,
                                      new Transfer[] {LocalSelectionTransfer.getTransfer()},
                                      new DragSourceAdapter() {

                Color color;
                List<Control> controls = new ArrayList<>();
                private final MouseMoveListener mouseMoveListener = new MouseMoveListener() {

                    @Override
                    public void mouseMove(final MouseEvent event) {
                        for (final Control control : controls) {
                            control.redraw();
                        }
                        if (color == Colors.POTENTIAL_DROP_TARGET1) {
                            color = Colors.POTENTIAL_DROP_TARGET2;
                        } else if (color == Colors.POTENTIAL_DROP_TARGET2) {
                            color = Colors.POTENTIAL_DROP_TARGET3;
                        } else if (color == Colors.POTENTIAL_DROP_TARGET3) {
                            color = Colors.POTENTIAL_DROP_TARGET4;
                        } else if (color == Colors.POTENTIAL_DROP_TARGET4) {
                            color = Colors.POTENTIAL_DROP_TARGET5;
                        } else if (color == Colors.POTENTIAL_DROP_TARGET5) {
                            color = Colors.POTENTIAL_DROP_TARGET6;
                        } else if (color == Colors.POTENTIAL_DROP_TARGET6) {
                            color = Colors.POTENTIAL_DROP_TARGET7;
                        } else if (color == Colors.POTENTIAL_DROP_TARGET7) {
                            color = Colors.POTENTIAL_DROP_TARGET8;
                        } else {
                            color = Colors.POTENTIAL_DROP_TARGET1;
                        }
                    }
                };
                private final PaintListener paintListener = new PaintListener() {

                    @Override
                    public void paintControl(final PaintEvent event) {
                        event.gc.setForeground(color);
                        final Rectangle bounds = ((Control)event.widget).getBounds();
                        event.gc.drawRectangle(0, 0, bounds.width - 1, bounds.height - 1);
                    }
                };

                @Override
                public void dragFinished(final DragSourceEvent event) {
                    treeViewer.getTree().removeMouseMoveListener(mouseMoveListener);
                    for (final Control control : controls) {
                        control.removePaintListener(paintListener);
                        control.redraw();
                    }
                    controls.clear();
                }

                @Override
                public void dragStart(final DragSourceEvent event) {
                    final IStructuredSelection selection =
                        (IStructuredSelection)treeViewer.getSelection();
                    LocalSelectionTransfer.getTransfer().setSelection(selection);
                    color = Colors.POTENTIAL_DROP_TARGET1;
                    for (final PotentialDropTarget potentialDropTarget : potentialDropTargets) {
                        if (potentialDropTarget.valid()) {
                            controls.add(potentialDropTarget.control);
                            potentialDropTarget.control.addPaintListener(paintListener);
                        }
                    }
                    treeViewer.getTree().addMouseMoveListener(mouseMoveListener);
                }
            });
        }

        collapseAllButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                treeViewer.collapseAll();
            }
        });
        filterTypesButton.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent event) {
                showFieldTypes = filterTypesButton.getSelection();
                filterTypesButton.setToolTipText((showFieldTypes ? "Hide" : "Show") + " types");
                treeViewer.refresh();
                if (preferenceId != null)
                    prefs.setValue(PREFERENCE_PREFIX + preferenceId + FILTER_TYPES_PREFERENCE,
                                   showFieldTypes);
            }
        });
        if (preferenceId != null) {
            showFieldTypes = prefs.getBoolean(PREFERENCE_PREFIX
                                              + preferenceId
                                              + FILTER_TYPES_PREFERENCE);
            filterTypesButton.setSelection(showFieldTypes);
        }
        if (showMappedFieldsButton) {
            filterMappedFieldsButton.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(final SelectionEvent event) {
                    hideMappedFields = filterMappedFieldsButton.getSelection();
                    filterMappedFieldsButton.setToolTipText((hideMappedFields ? "Show" : "Hide")
                                                            + " mapped properties");
                    treeViewer.refresh();
                    if (preferenceId != null)
                        prefs.setValue(PREFERENCE_PREFIX
                                       + preferenceId
                                       + FILTER_MAPPED_FIELDS_PREFERENCE,
                                       hideMappedFields);
                }
            });
            if (preferenceId != null) {
                hideMappedFields = prefs.getBoolean(PREFERENCE_PREFIX
                                                    + preferenceId
                                                    + FILTER_MAPPED_FIELDS_PREFERENCE);
                filterMappedFieldsButton.setSelection(hideMappedFields);
            }
        }
        if (showSearchField) {
            searchLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseUp(final MouseEvent event) {
                    searchText.setFocus();
                }
            });
            clearSearchLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseUp(final MouseEvent event) {
                    searchText.setText("");
                }
            });
            searchText.addModifyListener(new ModifyListener() {

                @Override
                public void modifyText(final ModifyEvent event) {
                    searchResults.clear();
                    final List<Model> models = searchMap.get(searchText.getText().trim().toLowerCase());
                    if (models != null) {
                        for (final Model model : models) {
                            searchResults.add(model);
                            for (Model parent = model.getParent();
                                 parent != null;
                                 parent = parent.getParent()) {
                                searchResults.add(parent);
                            }
                        }
                    }
                    treeViewer.refresh();
                }
            });
        }

        if (rootModel != null) {
            treeViewer.setInput("root");
        }

        if (config != null) {
            config.addListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(final PropertyChangeEvent event) {
                    if (event.getPropertyName().equals(TransformationConfig.MAPPING)) {
                        if (!treeViewer.getControl().isDisposed()) {
                            treeViewer.refresh();
                        }
                    }
                }
            });
        }
    }

    private void expand(final Model model) {
        if (model == null) {
            return;
        }
        expand(model.getParent());
        treeViewer.expandToLevel(model, 0);
    }

    boolean mapped(final Model model) {
        if (config == null) {
            return false;
        }
        return rootModel.equals(config.getSourceModel())
               ? !config.getMappingsForSource(model).isEmpty()
               : !config.getMappingsForTarget(model).isEmpty();
    }

    private boolean mappedOrFullyMappedParent(final Model model) {
        final List<Model> children = model.getChildren();
        for (final Model child : children) {
            if (!mappedOrFullyMappedParent(child)) {
                return false;
            }
        }
        return (mapped(model)) ? true : !children.isEmpty();
    }

    void select(final Model model) {
        if (model == null) {
            return;
        }
        final List<Model> models = searchMap.get(model.getName().toLowerCase());
        if (models == null) {
            return;
        }
        for (final Model actualModel : models) {
            if (actualModel.equals(model)) {
                expand(actualModel.getParent());
                treeViewer.setSelection(new StructuredSelection(actualModel), true);
                return;
            }
        }
    }

    public void setModel(final Model model) {
        rootModel = model;
        if (model != null) {
            treeViewer.setInput("root");
        } else {
            treeViewer.setInput(null);
        }
    }

    boolean show(final Object element,
                 final boolean searching) {
        if (hideMappedFields && mappedOrFullyMappedParent((Model)element)) {
            return false;
        }
        return !searching || searchResults.contains(element);
    }

    void updateSearchMap(final Model model) {
        if (model == null) {
            return;
        }
        final StringCharacterIterator iter =
            new StringCharacterIterator(model.getName().toLowerCase());
        final StringBuilder builder = new StringBuilder();
        for (char chr = iter.first(); chr != StringCharacterIterator.DONE; chr = iter.next()) {
            builder.append(chr);
            final String key = builder.toString();
            List<Model> models = searchMap.get(key);
            if (models == null) {
                models = new ArrayList<>();
                searchMap.put(key, models);
            }
            models.add(model);
        }
        for (final Model child : model.getChildren()) {
            updateSearchMap(child);
        }
    }

    class ContentProvider implements ITreeContentProvider {

        @Override
        public void dispose() {}

        @Override
        public Object[] getChildren(final Object parentElement) {
            return ((Model) parentElement).getChildren().toArray();
        }

        @Override
        public Object[] getElements(final Object inputElement) {
            return new Object[] {rootModel};
        }

        @Override
        public Object getParent(final Object element) {
            return element instanceof Model ? ((Model) element).getParent() : null;
        }

        @Override
        public boolean hasChildren(final Object element) {
            return getChildren(element).length > 0;
        }

        @Override
        public void inputChanged(final Viewer viewer,
                                 final Object oldInput,
                                 final Object newInput) {}
    }

    class LabelProvider extends StyledCellLabelProvider {

        private static final String LIST_OF = "list of ";

        private Image getImage(final Object element) {
            final Model model = (Model) element;
            Image img = model.getChildren() != null && model.getChildren().size() > 0
                        ? Images.ELEMENT
                        : Images.ATTRIBUTE;
            if (model.isCollection()) {
                img = new DecorationOverlayIcon(img,
                                                Decorations.COLLECTION,
                                                IDecoration.BOTTOM_RIGHT).createImage();
            }
            if (mapped((Model)element)) {
                return new DecorationOverlayIcon(img,
                                                 Decorations.MAPPED,
                                                 IDecoration.TOP_RIGHT).createImage();
            }
            return img;
        }

        private String getText(final Object element,
                               final StyledString text,
                               final boolean showFieldTypesInLabel) {
            final Model model = (Model) element;
            text.append(model.getName());
            if (showFieldTypesInLabel) {
                final String type = model.getType();
                if (type.startsWith("[")) {
                    text.append(":", StyledString.DECORATIONS_STYLER);
                    text.append(" " + LIST_OF, StyledString.QUALIFIER_STYLER);
                    text.append(type.substring(1, type.length() - 1),
                                StyledString.DECORATIONS_STYLER);
                } else {
                    text.append(": " + type, StyledString.DECORATIONS_STYLER);
                }
            }
            return text.getString();
        }

        @Override
        public String getToolTipText(final Object element) {
            return getText(element, new StyledString(), true);
        }

        @Override
        public void update(final ViewerCell cell) {
            final Object element = cell.getElement();
            final StyledString text = new StyledString();
            cell.setImage(getImage(element));
            cell.setText(getText(element, text, showFieldTypes));
            cell.setStyleRanges(text.getStyleRanges());
            super.update(cell);
        }
    }

    /**
     * Provides a method implementers can override to hide certain items.
     */
    protected void setViewOptions() {
        // anything that needs to be overridden, like showMappedFieldsButton
        // can be overridden here in an extender.
    }
}
