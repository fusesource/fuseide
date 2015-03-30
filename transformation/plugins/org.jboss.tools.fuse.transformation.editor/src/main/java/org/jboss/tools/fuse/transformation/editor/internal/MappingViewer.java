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

import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.jboss.mapper.Expression;
import org.jboss.mapper.MappingOperation;
import org.jboss.mapper.MappingType;
import org.jboss.mapper.Variable;
import org.jboss.mapper.model.Model;
import org.jboss.tools.fuse.transformation.editor.Activator;
import org.jboss.tools.fuse.transformation.editor.internal.util.TransformationConfig;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util;
import org.jboss.tools.fuse.transformation.editor.internal.util.Util.Colors;

abstract class MappingViewer {

    final TransformationConfig config;
    MappingOperation<?, ?> mapping;
    Text sourceText, targetText;
    DropTarget sourceDropTarget, targetDropTarget;
    final List<PotentialDropTarget> potentialDropTargets;

    MappingViewer(final TransformationConfig config,
                  final List<PotentialDropTarget> potentialDropTargets) {
        this.config = config;
        this.potentialDropTargets = potentialDropTargets;
    }

    void createSourceText(final Composite parent) {
        sourceText = createText(parent);
        setSourceText();
        sourceDropTarget = new DropTarget(sourceText, DND.DROP_MOVE);
        sourceDropTarget.setTransfer(new Transfer[] {LocalSelectionTransfer.getTransfer()});
        sourceDropTarget.addDropListener(new DropListener(sourceText) {

            @Override
            void drop() throws Exception {
                dropOnSource();
            }

            @Override
            boolean draggingFromValidObject() {
                return Util.draggingFromValidSource(config);
            }
        });
        potentialDropTargets.add(new PotentialDropTarget(sourceText) {

            @Override
            public boolean valid() {
                return mapping.getType() != MappingType.CUSTOM
                       && Util.draggingFromValidSource(config);
            }
        });
    }

    void createTargetText(final Composite parent) {
        targetText = createText(parent);
        setTargetText();
        targetDropTarget = new DropTarget(targetText, DND.DROP_MOVE);
        targetDropTarget.setTransfer(new Transfer[] {LocalSelectionTransfer.getTransfer()});
        targetDropTarget.addDropListener(new DropListener(targetText) {

            @Override
            void drop() throws Exception {
                dropOnTarget();
            }

            @Override
            boolean draggingFromValidObject() {
                return Util.draggingFromValidTarget(config);
            }
        });
        potentialDropTargets.add(new PotentialDropTarget(targetText) {

            @Override
            public boolean valid() {
                return mapping.getType() != MappingType.CUSTOM
                       && Util.draggingFromValidTarget(config);
            }
        });
    }

    Text createText(final Composite parent) {
        final Text text = new Text(parent, SWT.BORDER);
        text.setEditable(false);
        return text;
    }

    void dropOnSource() throws Exception {
        mapping = config.setSource(mapping, Util.draggedObject());
        config.save();
    }

    void dropOnTarget() throws Exception {
        mapping = config.setTarget(mapping, (Model)Util.draggedObject());
        config.save();
    }

    String name(final Object object) {
        if (object instanceof Model) return ((Model)object).getName();
        if (object instanceof Variable) return "${" + ((Variable)object).getName() + "}";
        if (object instanceof Expression) return ((Expression)object).getLanguage();
        return "";
    }

    void setSourceText() {
        setText(sourceText, mapping.getSource());
    }

    void setTargetText() {
        setText(targetText, mapping.getTarget());
    }

    private void setText(final Text text,
                         final Object object) {
        text.setText(name(object));
        if (object instanceof Model) {
            text.setToolTipText(config.fullyQualifiedName((Model)object));
            if (mapping.getType() == MappingType.CUSTOM) text.setBackground(Colors.FUNCTION);
            else text.setBackground(Colors.BACKGROUND);
            text.setForeground(Colors.FOREGROUND);
        } else if (object instanceof Variable) {
            text.setToolTipText("\"" + ((Variable)object).getValue() + "\"");
            text.setBackground(Colors.BACKGROUND);
            text.setForeground(Colors.VARIABLE);
        } else if (object instanceof Expression) {
            text.setToolTipText(((Expression)object).getExpression());
            text.setBackground(Colors.BACKGROUND);
            text.setForeground(Colors.EXPRESSION);
        } else {
            text.setToolTipText("");
            text.setBackground(Colors.BACKGROUND);
            text.setForeground(Colors.FOREGROUND);
        }
    }

    abstract class DropListener extends DropTargetAdapter {

        private final Text dropText;
        private Color background, foreground;

        DropListener(final Text dropText) {
            this.dropText = dropText;
        }

        @Override
        public final void dragEnter(final DropTargetEvent event) {
            background = dropText.getBackground();
            foreground = dropText.getForeground();
            if (mapping.getType() != MappingType.CUSTOM && draggingFromValidObject()) {
                dropText.setBackground(Colors.DROP_TARGET_BACKGROUND);
                dropText.setForeground(Colors.DROP_TARGET_FOREGROUND);
            }
        }

        abstract boolean draggingFromValidObject();

        @Override
        public final void dragLeave(final DropTargetEvent event) {
            dropText.setBackground(background);
            dropText.setForeground(foreground);
        }

        @Override
        public final void drop(final DropTargetEvent event) {
            try {
                if (draggingFromValidObject()) drop();
            } catch (final Exception e) {
                Activator.error(e);
            }
        }

        abstract void drop() throws Exception;
    }
}
