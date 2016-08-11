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
package org.fusesource.ide.camel.editor.utils;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.graphiti.datatypes.IDimension;
import org.eclipse.graphiti.features.IDirectEditingInfo;
import org.eclipse.graphiti.features.IFeatureProvider;
import org.eclipse.graphiti.features.context.IAddContext;
import org.eclipse.graphiti.mm.algorithms.GraphicsAlgorithm;
import org.eclipse.graphiti.mm.algorithms.Image;
import org.eclipse.graphiti.mm.algorithms.RoundedRectangle;
import org.eclipse.graphiti.mm.algorithms.Text;
import org.eclipse.graphiti.mm.algorithms.styles.Color;
import org.eclipse.graphiti.mm.algorithms.styles.Font;
import org.eclipse.graphiti.mm.algorithms.styles.Orientation;
import org.eclipse.graphiti.mm.algorithms.styles.Style;
import org.eclipse.graphiti.mm.pictograms.ChopboxAnchor;
import org.eclipse.graphiti.mm.pictograms.ContainerShape;
import org.eclipse.graphiti.mm.pictograms.Diagram;
import org.eclipse.graphiti.services.Graphiti;
import org.eclipse.graphiti.services.IGaService;
import org.eclipse.graphiti.services.IPeCreateService;
import org.eclipse.graphiti.ui.services.GraphitiUi;
import org.fusesource.ide.camel.editor.features.custom.CollapseFeature;
import org.fusesource.ide.camel.editor.provider.ImageProvider;
import org.fusesource.ide.camel.model.service.core.model.AbstractCamelModelElement;
import org.fusesource.ide.camel.model.service.core.model.CamelRouteElement;

/**
 * this factory provides the draw functionality for diagram figures
 * 
 * @author lhein
 */
public class FigureUIFactory {
	
	// new statics
	public static final int DEFAULT_LABEL_OFFSET_H = 5;
	public static final int DEFAULT_LABEL_OFFSET_V = 5;
	public static final int FONT_SPACING_V = 4;
	public static final int IMAGE_DEFAULT_WIDTH = 32;
	public static final int IMAGE_DEFAULT_HEIGHT = 34;
	public static final int FIGURE_MAX_WIDTH = 200;
	public static final int BREAKPOINT_DECORATOR_SPACE = 25;
	public static final int CORNER_WIDTH = 15;
	public static final int CORNER_HEIGHT = 15;
	public static final int BORDER_SIZE = 1;

	/**
	 * 
	 * @param context
	 * @param fp
	 * @param containerShape
	 * @param element
	 * @param diagram
	 * @param defaultLabel
	 */
	public static void createFigureUI(IAddContext context, IFeatureProvider fp, ContainerShape containerShape, AbstractCamelModelElement element, Diagram diagram, String defaultLabel) {
		if (element instanceof CamelRouteElement) {
			// special handling for route figures
			paintRouteFigure(context, fp, containerShape, element, diagram, defaultLabel);
		} else if (element.getUnderlyingMetaModelObject().canHaveChildren() == false) {
			// special handling for child figures
			paintChildFigure(context, fp, containerShape, element, diagram, defaultLabel);
		} else if (element.getUnderlyingMetaModelObject().canHaveChildren() == true) {
			// special handling for container figures
			paintContainerFigure(context, fp, containerShape, element, diagram, defaultLabel);
		} else {
			// fall back to default figure painting
			paintDefaultFigure(context, fp, containerShape, element, diagram, defaultLabel);
		}
	}
	
	/**
	 * paints a default figure
	 * 
	 * @param context
	 * @param fp
	 * @param containerShape
	 * @param element
	 * @param diagram
	 * @param defaultLabel
	 */
	private static void paintDefaultFigure(IAddContext context, IFeatureProvider fp, ContainerShape containerShape, AbstractCamelModelElement element, Diagram diagram, String defaultLabel) {
		// as default we paint child figures for now
		paintChildFigure(context, fp, containerShape, element, diagram, defaultLabel);
	}
	
	/**
	 * paints a container figure
	 * 
	 * @param context
	 * @param fp
	 * @param containerShape
	 * @param element
	 * @param diagram
	 * @param defaultLabel
	 */
	private static void paintContainerFigure(IAddContext context, IFeatureProvider fp, ContainerShape containerShape, AbstractCamelModelElement element, Diagram diagram, String defaultLabel) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();

		// determine font dimensions
		Font f = StyleUtil.getStyleForCamelText(diagram).getFont();
		IDimension fontDimension = GraphitiUi.getUiLayoutService().calculateTextSize(defaultLabel, f);
		int label_width = fontDimension.getWidth();
		int label_height = fontDimension.getHeight()+FONT_SPACING_V;

		Dimension imageDimension = ImageUtils.getImageSize(ImageProvider.getKeyForDiagramIcon(element.isEndpointElement(), element.getIconName()));
		int image_width = imageDimension.width;
		int image_height = imageDimension.height;

		// we will draw a rounded rectangle with a header bar containing icon 
		// and label - the lower section will be empty by default

		// baseRect is the rounded rectangle
		org.eclipse.swt.graphics.Rectangle baseRect = new org.eclipse.swt.graphics.Rectangle(context.getX(), 
																							 context.getY(), 
																							 FIGURE_MAX_WIDTH + BORDER_SIZE + BORDER_SIZE, 
																							 IMAGE_DEFAULT_HEIGHT * 2 + DEFAULT_LABEL_OFFSET_V);

		// the container for the child elements	
		org.eclipse.swt.graphics.Rectangle contentSection = new org.eclipse.swt.graphics.Rectangle(	BORDER_SIZE, 
																									IMAGE_DEFAULT_HEIGHT - BORDER_SIZE, 
																								 	FIGURE_MAX_WIDTH - BORDER_SIZE, 
																								 	baseRect.height - IMAGE_DEFAULT_HEIGHT - BORDER_SIZE);
				
		IGaService gaService = Graphiti.getGaService();

		Color titleSectionColor = gaService.manageColor(diagram, StyleUtil.CONTAINER_FIGURE_BORDER_COLOR);
		Color contentSectionColor = gaService.manageColor(diagram, StyleUtil.CONTAINER_FIGURE_BACKGROUND_COLOR);
				
		// create invisible outer rectangle expanded by the width needed for the anchor
		RoundedRectangle baseFigure = gaService.createRoundedRectangle(containerShape, CORNER_WIDTH, CORNER_HEIGHT);
		gaService.setLocationAndSize(baseFigure, baseRect.x, baseRect.y, baseRect.width, baseRect.height);
		baseFigure.setBackground(titleSectionColor);
		baseFigure.setFilled(true);
		markFigureHeaderArea(baseFigure, IMAGE_DEFAULT_HEIGHT);		
		
		// rearrange container
		containerShape.getGraphicsAlgorithm().setLineVisible(true);
		containerShape.getGraphicsAlgorithm().setForeground(titleSectionColor);
		containerShape.getGraphicsAlgorithm().setLineWidth(BORDER_SIZE);
		
		// create and set image
		Image image = gaService.createImage(baseFigure, ImageProvider.getKeyForDiagramIcon(element.isEndpointElement(), element.getIconName()));
		gaService.setLocationAndSize(image, DEFAULT_LABEL_OFFSET_H * 3, (IMAGE_DEFAULT_HEIGHT-image_height)/2, image_width, image_height);
		
		// create and set text graphics algorithm
		Text text = gaService.createDefaultText(diagram, baseFigure, defaultLabel);
		Style style = StyleUtil.getStyleForCamelText(diagram);
		text.setParentGraphicsAlgorithm(baseFigure);
		text.setStyle(style);
		text.setHorizontalAlignment(Orientation.ALIGNMENT_LEFT);
		text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
		text.setFont(style.getFont());
		gaService.setLocationAndSize(text, DEFAULT_LABEL_OFFSET_H * 4 + image_width, (IMAGE_DEFAULT_HEIGHT-label_height)/2, Math.min(label_width, baseRect.width - image_width - 6 * DEFAULT_LABEL_OFFSET_H), label_height);
		markFigureTitleArea(text, image_width);
		
		// the content section figure
		RoundedRectangle contentRectangle = gaService.createRoundedRectangle(baseFigure, CORNER_WIDTH, CORNER_HEIGHT);
		contentRectangle.setParentGraphicsAlgorithm(baseFigure);
		contentRectangle.setStyle(StyleUtil.getStyleForCamelClass(diagram));
		contentRectangle.setBackground(contentSectionColor);
		contentRectangle.setLineWidth(BORDER_SIZE);
		contentRectangle.setLineVisible(false);
		contentRectangle.setFilled(true);
		gaService.setLocationAndSize(contentRectangle, contentSection.x, contentSection.y, contentSection.width, contentSection.height);
		markExpandableFigureArea(contentRectangle, IMAGE_DEFAULT_HEIGHT);
		
		// provide information to support direct-editing directly
		// after object creation (must be activated additionally)
		IDirectEditingInfo directEditingInfo = fp.getDirectEditingInfo();
		// set container shape for direct editing after object creation
		directEditingInfo.setMainPictogramElement(containerShape);
		// set shape and graphics algorithm where the editor for
		// direct editing shall be opened after object creation
		directEditingInfo.setPictogramElement(containerShape);
		directEditingInfo.setGraphicsAlgorithm(text);

		// add a chopbox anchor to the shape
		ChopboxAnchor ca = peCreateService.createChopboxAnchor(containerShape);
		fp.link(ca, element); 
		
		Graphiti.getPeService().setPropertyValue(containerShape, CollapseFeature.PROP_COLLAPSED_WIDTH, "" + containerShape.getGraphicsAlgorithm().getWidth());
		Graphiti.getPeService().setPropertyValue(containerShape, CollapseFeature.PROP_COLLAPSED_HEIGHT, "" + IMAGE_DEFAULT_HEIGHT);
	}
	
	/**
	 * paints a figure for Camel Route elements
	 * 
	 * @param context
	 * @param fp
	 * @param containerShape
	 * @param element
	 * @param diagram
	 * @param defaultLabel
	 */
	private static void paintRouteFigure(IAddContext context, IFeatureProvider fp, ContainerShape containerShape, AbstractCamelModelElement element, Diagram diagram, String defaultLabel) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();

		// calculate label width and height
		Font f = StyleUtil.getStyleForCamelText(diagram).getFont();
		IDimension fontDimension = GraphitiUi.getUiLayoutService().calculateTextSize(defaultLabel, f);
		int label_width = fontDimension.getWidth();
		int label_height = fontDimension.getHeight()+FONT_SPACING_V;

		// we draw a filled rounded rectangle with a title label 
		org.eclipse.swt.graphics.Rectangle baseRect = new org.eclipse.swt.graphics.Rectangle(context.getX(), 
																							 context.getY(), 
																							 FIGURE_MAX_WIDTH, 
																							 IMAGE_DEFAULT_HEIGHT + label_height + DEFAULT_LABEL_OFFSET_V);
	
		IGaService gaService = Graphiti.getGaService();

		Color figureBackgroundColor = gaService.manageColor(diagram, StyleUtil.CONTAINER_FIGURE_BACKGROUND_COLOR);
		Color figureBorderColor = gaService.manageColor(diagram, StyleUtil.CONTAINER_FIGURE_BORDER_COLOR);
		
		// create invisible outer rectangle expanded by the width needed for the anchor
		RoundedRectangle baseFigure = gaService.createRoundedRectangle(containerShape, CORNER_WIDTH, CORNER_HEIGHT);
		gaService.setLocationAndSize(baseFigure, baseRect.x, baseRect.y, baseRect.width, baseRect.height);
		baseFigure.setBackground(figureBackgroundColor);
		baseFigure.setFilled(true);
		markFigureHeaderArea(baseFigure, IMAGE_DEFAULT_HEIGHT);	
		
		// rearrange container
		containerShape.getGraphicsAlgorithm().setLineVisible(true);
		containerShape.getGraphicsAlgorithm().setForeground(figureBorderColor);
		containerShape.getGraphicsAlgorithm().setLineWidth(BORDER_SIZE);
		
		// create and set text graphics algorithm
		Text text = gaService.createDefaultText(diagram, baseFigure, defaultLabel);
		Style style = StyleUtil.getStyleForCamelText(diagram);
		text.setParentGraphicsAlgorithm(baseFigure);
		text.setStyle(style);
		text.setHorizontalAlignment(Orientation.ALIGNMENT_LEFT);
		text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
		text.setFont(style.getFont());
		text.setForeground(GraphitiUi.getGaService().manageColor(diagram, StyleUtil.CONTAINER_FIGURE_TEXT_COLOR));
		gaService.setLocationAndSize(text, DEFAULT_LABEL_OFFSET_H * 3, (IMAGE_DEFAULT_HEIGHT-label_height)/2, baseRect.width - DEFAULT_LABEL_OFFSET_H - DEFAULT_LABEL_OFFSET_H, label_height);
		markFigureTitleArea(text, 24);
		
		// provide information to support direct-editing directly
		// after object creation (must be activated additionally)
		IDirectEditingInfo directEditingInfo = fp.getDirectEditingInfo();
		// set container shape for direct editing after object creation
		directEditingInfo.setMainPictogramElement(containerShape);
		// set shape and graphics algorithm where the editor for
		// direct editing shall be opened after object creation
		directEditingInfo.setPictogramElement(containerShape);
		directEditingInfo.setGraphicsAlgorithm(text);

		// add a chopbox anchor to the shape
		ChopboxAnchor ca = peCreateService.createChopboxAnchor(containerShape);
		fp.link(ca, element); 
		
		Graphiti.getPeService().setPropertyValue(containerShape, CollapseFeature.PROP_COLLAPSED_WIDTH, "" + containerShape.getGraphicsAlgorithm().getWidth());
		Graphiti.getPeService().setPropertyValue(containerShape, CollapseFeature.PROP_COLLAPSED_HEIGHT, "" + IMAGE_DEFAULT_HEIGHT);
	}
	
	
	/**
	 * paints the non-container figures
	 * 
	 * @param context
	 * @param fp
	 * @param containerShape
	 * @param element
	 * @param diagram
	 * @param defaultLabel
	 */
	private static void paintChildFigure(IAddContext context, IFeatureProvider fp, ContainerShape containerShape, AbstractCamelModelElement element, Diagram diagram, String defaultLabel) {
		IPeCreateService peCreateService = Graphiti.getPeCreateService();

		Dimension imageDimension = ImageUtils.getImageSize(ImageProvider.getKeyForDiagramIcon(element.isEndpointElement(), element.getIconName()));
		int image_width = imageDimension.width;
		int image_height = imageDimension.height;
		
		// calculate the label width and height
		Font f = StyleUtil.getStyleForCamelText(diagram).getFont();
		IDimension fontDimension = GraphitiUi.getUiLayoutService().calculateTextSize(defaultLabel, f);
		int label_width = fontDimension.getWidth();
		int label_height = fontDimension.getHeight()+FONT_SPACING_V;

		// a child figure is drawn as a rounded rectangle containing a big 
		// descriptive icon and below the icon there will be a label shown
		// describing the element
		org.eclipse.swt.graphics.Rectangle baseRect = new org.eclipse.swt.graphics.Rectangle(context.getX(), 
																							 context.getY(), 
																							 Math.min(12*DEFAULT_LABEL_OFFSET_H+image_width+label_width, FIGURE_MAX_WIDTH), 
																							 IMAGE_DEFAULT_HEIGHT);
		
		IGaService gaService = Graphiti.getGaService();

		Color figureBackgroundColor = computeBackGroundColor(element, diagram, gaService);
		
		// create invisible outer rectangle expanded by the width needed for the anchor
		RoundedRectangle baseFigure = gaService.createRoundedRectangle(containerShape, CORNER_WIDTH, CORNER_HEIGHT);
		gaService.setLocationAndSize(baseFigure, baseRect.x, baseRect.y, baseRect.width, baseRect.height);
		baseFigure.setBackground(figureBackgroundColor);
		baseFigure.setFilled(true);
				
		// rearrange container
		containerShape.getGraphicsAlgorithm().setLineVisible(false);
		containerShape.getGraphicsAlgorithm().setLineWidth(BORDER_SIZE);
		
		// create and set image
		Image image = gaService.createImage(baseFigure, ImageProvider.getKeyForDiagramIcon(element.isEndpointElement(), element.getIconName()));
		gaService.setLocationAndSize(image, DEFAULT_LABEL_OFFSET_H * 3, (baseRect.height-image_height)/2, image_width, image_height);

		// create and set text graphics algorithm
		Text text = gaService.createDefaultText(diagram, baseFigure, defaultLabel);
		Style style = StyleUtil.getStyleForCamelText(diagram);
		text.setParentGraphicsAlgorithm(baseFigure);
		text.setStyle(style);
		text.setHorizontalAlignment(Orientation.ALIGNMENT_LEFT);
		text.setVerticalAlignment(Orientation.ALIGNMENT_CENTER);
		text.setFont(style.getFont());
		text.setForeground(GraphitiUi.getGaService().manageColor(diagram, StyleUtil.CONTAINER_FIGURE_TEXT_COLOR));
		gaService.setLocationAndSize(text, DEFAULT_LABEL_OFFSET_H * 4 + image_width, (baseRect.height-label_height)/2, Math.min(label_width, baseRect.width - image_width - 6 * DEFAULT_LABEL_OFFSET_H), label_height);
		markFigureTitleArea(text, image_width);

		// provide information to support direct-editing directly
		// after object creation (must be activated additionally)
		IDirectEditingInfo directEditingInfo = fp.getDirectEditingInfo();
		// set container shape for direct editing after object creation
		directEditingInfo.setMainPictogramElement(containerShape);
		// set shape and graphics algorithm where the editor for
		// direct editing shall be opened after object creation
		directEditingInfo.setPictogramElement(containerShape);
		directEditingInfo.setGraphicsAlgorithm(text);

		// add a chopbox anchor to the shape
		ChopboxAnchor ca = peCreateService.createChopboxAnchor(containerShape);
		fp.link(ca, element); 
	}

	/**
	 * @param element
	 * @param diagram
	 * @param gaService
	 * @return
	 */
	public static Color computeBackGroundColor(AbstractCamelModelElement element, Diagram diagram, IGaService gaService) {
		Color figureBackgroundColor = null;
		if (element.isEndpointElement()) {
			// endpoint
			if (element.getNodeTypeId().equalsIgnoreCase(AbstractCamelModelElement.ENDPOINT_TYPE_FROM)) {
				// from endpoint
				figureBackgroundColor = gaService.manageColor(diagram, StyleUtil.FROM_FIGURE_BACKGROUND_COLOR);
			} else {
				// to endpoint
				figureBackgroundColor = gaService.manageColor(diagram, StyleUtil.TO_FIGURE_BACKGROUND_COLOR);
			}
		} else {
			// not an endpoint
			figureBackgroundColor = gaService.manageColor(diagram, StyleUtil.EIP_FIGURE_BACKGROUND_COLOR);
		}
		return figureBackgroundColor;
	}
	
	/**
	 * marks the header area of the figure
	 * 
	 * @param area		the header section of the figure
	 * @param collapsedHeight	the initial area height in collapsed state
	 */
	public static void markFigureHeaderArea(GraphicsAlgorithm area, int collapsedHeight) {
		Graphiti.getPeService().setPropertyValue(area, DiagramUtils.PROP_ORIGINAL_SECTION_HEIGHT, "" + collapsedHeight);
		Graphiti.getPeService().setPropertyValue(area, DiagramUtils.PROP_SHAPE_KIND, DiagramUtils.PROP_SHAPE_KIND_HEADER);
	}
	
	/**
	 * marks the title area of the figure
	 * 
	 * @param area		the title / label graphics algorithm
	 * @param iconWidth	the width of the icon
	 */
	public static void markFigureTitleArea(GraphicsAlgorithm area, int iconWidth) {
		Graphiti.getPeService().setPropertyValue(area, DiagramUtils.PROP_IMG_WIDTH, "" + iconWidth);
		Graphiti.getPeService().setPropertyValue(area, DiagramUtils.PROP_SHAPE_KIND, DiagramUtils.PROP_SHAPE_KIND_TITLE);
	}
	
	/**
	 * marks an area of the figure as expandable area
	 * 
	 * @param area	the area to mark
	 * @param collapsedHeight	the initial area height in collapsed state
	 */
	public static void markExpandableFigureArea(GraphicsAlgorithm area, int collapsedHeight) {
		Graphiti.getPeService().setPropertyValue(area, DiagramUtils.PROP_SHAPE_KIND, DiagramUtils.PROP_SHAPE_KIND_EXPANDABLE);
		Graphiti.getPeService().setPropertyValue(area, DiagramUtils.PROP_ORIGINAL_SECTION_HEIGHT, "" + collapsedHeight);
	}
	
	/**
	 * stores the width and height of the given shape as the new collapsed width/height
	 * 
	 * @param containerShape
	 */
	public static void storeCollapsedSize(ContainerShape containerShape) {
		Graphiti.getPeService().setPropertyValue(containerShape, CollapseFeature.PROP_COLLAPSED_WIDTH, String.valueOf(containerShape.getGraphicsAlgorithm().getWidth()));
		Graphiti.getPeService().setPropertyValue(containerShape, CollapseFeature.PROP_COLLAPSED_HEIGHT, String.valueOf(containerShape.getGraphicsAlgorithm().getHeight()));
	}
	
}
