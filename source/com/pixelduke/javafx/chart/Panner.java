package com.pixelduke.javafx.chart;

import javafx.geometry.Dimension2D;
import javafx.scene.input.MouseEvent;

public class Panner {

	private double lastMouseDragX;

	private XYChartConfigurator config;

	public Panner(XYChartConfigurator xyChartConfigurator) {
		this.config = xyChartConfigurator;
	}

	public void onMousePressed(MouseEvent mouseEvent) {
		storeLastPanPositions(mouseEvent);
	}

	private void storeLastPanPositions(MouseEvent mouseEvent) {
		lastMouseDragX = mouseEvent.getSceneX();
	}

	public void onDrag(MouseEvent mouseEvent) {
		double mouseSceneX = mouseEvent.getSceneX();

		double dragX = mouseSceneX - lastMouseDragX;

		lastMouseDragX = mouseSceneX;

		Dimension2D chartDrag = ChartUtils.sceneToChartDistance(dragX, 0, config.getXAxis(), config.getYAxis());

		ChartUtils.setLowerXBoundWithinRange(config.getChart(),
				config.getXAxis().getLowerBound() - chartDrag.getWidth());
		ChartUtils.setUpperXBoundWithinRange(config.getChart(),
				config.getXAxis().getUpperBound() - chartDrag.getWidth());
	}
}
