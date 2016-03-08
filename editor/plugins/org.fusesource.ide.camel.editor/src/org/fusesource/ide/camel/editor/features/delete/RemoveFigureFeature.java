/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.fusesource.ide.camel.editor.features.delete;

import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IRemoveContext;
import org.eclipse.graphiti.features.context.impl.CustomContext;
import org.eclipse.graphiti.features.impl.DefaultRemoveFeature;
import org.eclipse.graphiti.mm.pictograms.PictogramElement;
import org.fusesource.ide.camel.editor.commands.DiagramOperations;
import org.fusesource.ide.camel.editor.features.custom.DeleteEndpointBreakpointFeature;
import org.fusesource.ide.camel.editor.internal.CamelEditorUIActivator;
import org.fusesource.ide.camel.editor.utils.CamelUtils;
import org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement;
import org.fusesource.ide.camel.model.service.core.model.CamelContextElement;
import org.fusesource.ide.camel.model.service.core.model.CamelElementConnection;

/**
 * @author lhein
 */
public class RemoveFigureFeature extends DefaultRemoveFeature {

	/**
	 * @param fp
	 */
	public RemoveFigureFeature(IFeatureProvider fp) {
		super(fp);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.impl.DefaultRemoveFeature#preRemove(org.eclipse.graphiti.features.context.IRemoveContext)
	 */
	@Override
	public void preRemove(IRemoveContext context) {
		PictogramElement pe = context.getPictogramElement();
		removeBreakpoint(pe);
		super.preRemove(context);
		deleteBusinessObjectFromModel(pe);
	}

	/**
	 * @param pe
	 */
	private void deleteBusinessObjectFromModel(PictogramElement pe) {
		Object[] businessObjectsForPictogramElement = getAllBusinessObjectsForPictogramElement(pe);
		if (businessObjectsForPictogramElement != null && businessObjectsForPictogramElement.length > 0) {
			Object bo = businessObjectsForPictogramElement[0];
			if (bo instanceof CamelElementConnection) {
				deleteFlowFromModel((CamelElementConnection) bo);
			} else if (bo instanceof AbstractCamelModelElement) {
				deleteBOFromModel((AbstractCamelModelElement)bo);
			} else {
				CamelEditorUIActivator.pluginLog().logWarning("Cannot figure out Node or Flow from BO: " + bo);
			}
		}
	}

	/**
	 * @param pe
	 */
	private void removeBreakpoint(PictogramElement pe) {
		final DeleteEndpointBreakpointFeature deleteEndpointBreakpointFeature = new DeleteEndpointBreakpointFeature(getFeatureProvider());
		final CustomContext context = new CustomContext(new PictogramElement[] { pe });
		deleteEndpointBreakpointFeature.execute(context);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.graphiti.features.impl.DefaultRemoveFeature#postRemove(org.eclipse.graphiti.features.context.IRemoveContext)
	 */
	@Override
	public void postRemove(IRemoveContext context) {
		super.postRemove(context);
		DiagramOperations.layoutDiagram(CamelUtils.getDiagramEditor());
	}

	private void deleteBOFromModel(AbstractCamelModelElement nodeToRemove) {
		// we can't remove null objects or the root of the routes
		if (nodeToRemove == null || nodeToRemove instanceof CamelContextElement) return;

		// lets remove all connections
		if (nodeToRemove.getParent() != null) nodeToRemove.getParent().removeChildElement(nodeToRemove);
		if (nodeToRemove.getInputElement() != null) nodeToRemove.getInputElement().setOutputElement(null);
		if (nodeToRemove.getOutputElement() != null) nodeToRemove.getOutputElement().setInputElement(null);
	}

	private void deleteFlowFromModel(CamelElementConnection bo) {
		bo.disconnect();
	}
}
