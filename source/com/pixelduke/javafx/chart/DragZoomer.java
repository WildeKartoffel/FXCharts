package com.pixelduke.javafx.chart;

import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Rectangle;

public class DragZoomer {

	private XYChartConfigurator config;

	private double initialMouseX;
	private double initialMouseSceneX;
	private double initialMouseSceneY;
	private Rectangle zoomRectangle;
	private boolean shouldZoom = false;

	public DragZoomer(XYChartConfigurator xyChartConfigurator) {
		this.config = xyChartConfigurator;
		createZoomRectangle();
	}

	public Node getNode() {
		return zoomRectangle;
	}

	public void startDragZoom(MouseEvent mouseEvent) {
		shouldZoom = true;
		storeLastDragZoomPositions(mouseEvent);
		zoomRectangle.setX(initialMouseX);
		zoomRectangle.setY(
				config.getChart().getHeight() - config.getXAxis().boundsInParentProperty().getValue().getMaxY() - 4);

	}

	private void storeLastDragZoomPositions(MouseEvent mouseEvent) {
		initialMouseX = mouseEvent.getX();
		initialMouseSceneX = mouseEvent.getSceneX();
		initialMouseSceneY = mouseEvent.getSceneY();
	}

	public void onDrag(MouseEvent mouseEvent) {
		if (shouldZoom) {
			zoomRectangle.toFront();
			zoomRectangle.setVisible(true);

			zoomRectangle.setWidth(mouseEvent.getSceneX() - initialMouseSceneX);
			zoomRectangle.setHeight(config.getYAxis().getHeight());
		}
	}

	public void zoomInHorizontally(double newMouseX, double newMouseY) {
		double[] newLower = ChartUtils.sceneToChartValues(initialMouseSceneX, newMouseY, config.getXAxis(),
				config.getYAxis());
		double[] newUpper = ChartUtils.sceneToChartValues(newMouseX, initialMouseSceneY, config.getXAxis(),
				config.getYAxis());

		ChartUtils.setLowerXBoundWithinRange(config.getChart(), newLower[0]);
		ChartUtils.setUpperXBoundWithinRange(config.getChart(), newUpper[0]);
	}

	public void onMouseRelease(MouseEvent mouseEvent) {
		if (shouldZoom) {
			System.out.println("released");
			zoomRectangle.setVisible(false);

			double newMouseX = mouseEvent.getSceneX();
			double newMouseY = mouseEvent.getSceneY();

			if (newMouseX < initialMouseSceneX) {
				ChartUtils.zoomOutHorizontally(config.getChart());
			} else if (newMouseX > initialMouseSceneX) {
				zoomInHorizontally(newMouseX, newMouseY);
			}
		}
		shouldZoom = false;
	}

	public void stopZooming() {
		zoomRectangle.setVisible(false);
		shouldZoom = false;
	}

	private void createZoomRectangle() {
		zoomRectangle = new Rectangle();
		zoomRectangle.getStyleClass().add("zoom-rectangle");
		zoomRectangle.setVisible(false);
	}
}
