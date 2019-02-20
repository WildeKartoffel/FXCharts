package com.pixelduke.javafx.chart;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class DragZoomer {

	private SimpleBooleanProperty isZoomingEnabled = new SimpleBooleanProperty(true);
	private Rectangle zoomRectangle;
	private boolean isZooming = false;
	
	public Node getNode() {
		return zoomRectangle;
	}
	
	private void setupZoom() {
		zoomRectangle = createZoomRectangle();

		group = new Group();
		group.getStylesheets()
				.add(getClass().getResource("/com/pixelduke/javafx/styles/chartStyles.css").toExternalForm());
		group.getChildren().add(zoomRectangle);

		zoomRectangle.setVisible(false);
		zoomRectangle.getStyleClass().add("zoom-rectangle");

		chart.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (isZooming)
					return;
				isZooming = true;
				configureMouseMode(mouseEvent);
				previousMouseCursor = chart.getCursor();

				initialMouseX = mouseEvent.getX();
				initialMouseY = mouseEvent.getY();
				initialMouseSceneX = mouseEvent.getSceneX();
				initialMouseSceneY = mouseEvent.getSceneY();

				lastMouseDragX = initialMouseSceneX;
				lastMouseDragY = initialMouseSceneY;

				if (mouseModeProperty.get() == MouseMode.ZOOM) {
					if (axisConstraint == AxisConstraint.Vertical)
						zoomRectangle.setX(0);// TODO not handled yet
					else
						zoomRectangle.setX(initialMouseX);
					if (axisConstraint == AxisConstraint.Horizontal) {
						setChartCursor(Cursor.W_RESIZE);
						zoomRectangle.setY(chart.getHeight() - xAxis.boundsInParentProperty().getValue().getMaxY() - 4);
					} else
						zoomRectangle.setY(initialMouseY);
				} else if (mouseModeProperty.get() == MouseMode.PAN) {
					setChartCursor(Cursor.CLOSED_HAND);
				}
			}
		});
		chart.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				double mouseSceneX = mouseEvent.getSceneX();
				double mouseSceneY = mouseEvent.getSceneY();
				double dragX = mouseSceneX - lastMouseDragX;
				double dragY = mouseSceneY - lastMouseDragY;

				lastMouseDragX = mouseSceneX;
				lastMouseDragY = mouseSceneY;

				if (mouseModeProperty.get() == MouseMode.ZOOM) {
					zoomRectangle.toFront();
					zoomRectangle.setVisible(true);

					if (axisConstraint == AxisConstraint.Vertical)
						zoomRectangle.setWidth(xAxis.getWidth());
					else
						zoomRectangle.setWidth(mouseSceneX - initialMouseSceneX);

					if (axisConstraint == AxisConstraint.Horizontal)
						zoomRectangle.setHeight(yAxis.getHeight());
					else
						zoomRectangle.setHeight(mouseSceneY - initialMouseSceneY);
				} else if (mouseModeProperty.get() == MouseMode.PAN) {
					setAutoRanging(false);

					Dimension2D chartDrag = sceneToChartDistance(dragX, dragY);

					xAxis.setLowerBound(xAxis.getLowerBound() - chartDrag.getWidth());
					xAxis.setUpperBound(xAxis.getUpperBound() - chartDrag.getWidth());

					if (isVerticalPanningAllowed) {
						double newYLowerBound = yAxis.getLowerBound() + chartDrag.getHeight();
						double newYUpperBound = yAxis.getUpperBound() + chartDrag.getHeight();

						if (!isShowingOnlyYPositiveValues) {
							yAxis.setLowerBound(newYLowerBound);
							yAxis.setUpperBound(newYUpperBound);
						} else {
							yAxis.setLowerBound(newYLowerBound < 0 ? 0 : newYLowerBound);
							yAxis.setUpperBound(newYUpperBound < 0 ? 0 : newYUpperBound);
						}
					}
					stayInHorizontalMaximumBounds();
				}
			}
		});

		chart.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				setChartCursor(previousMouseCursor);
				if (mouseModeProperty.get() == MouseMode.ZOOM) {

					zoomRectangle.setVisible(false);

					double newMouseX = mouseEvent.getSceneX();
					double newMouseY = mouseEvent.getSceneY();

					if (newMouseX < initialMouseSceneX) {
						zoomOut();
					}

					else if (newMouseX > initialMouseSceneX && axisConstraint == AxisConstraint.Horizontal) {
						// zoom in horizontally
						setAutoRanging(false);

						double[] newLower = sceneToChartValues(initialMouseSceneX, newMouseY);
						double[] newUpper = sceneToChartValues(newMouseX, initialMouseSceneY);

						if (axisConstraint != AxisConstraint.Vertical) {
							xAxis.setLowerBound(newLower[0]);
							xAxis.setUpperBound(newUpper[0]);
						}
						if (axisConstraint != AxisConstraint.Horizontal) {
							if (!isShowingOnlyYPositiveValues) {
								yAxis.setLowerBound(newLower[1]);
								yAxis.setUpperBound(newUpper[1]);
							} else {
								yAxis.setLowerBound(newLower[1] < 0 ? 0 : newLower[1]);
								yAxis.setUpperBound(newUpper[1] < 0 ? 0 : newUpper[1]);
							}
						}
					}
					isZooming = false;
				}
			}
		});
	}

	private Rectangle createZoomRectangle() {
		final Rectangle zoomArea = new Rectangle();
		zoomArea.setStrokeWidth(0);
		Color BLUE_COLOR = Color.LIGHTBLUE;
		Color zoomAreaFill = new Color(BLUE_COLOR.getRed(), BLUE_COLOR.getGreen(), BLUE_COLOR.getBlue(), 0.5);
		zoomArea.setFill(zoomAreaFill);
		return zoomArea;
	}
}
