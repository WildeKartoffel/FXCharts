package com.pixelduke.javafx.chart;

import javafx.scene.input.ScrollEvent;

public class WheelZoomer {

	private XYChartConfigurator config;

	public WheelZoomer(XYChartConfigurator config) {
		this.config = config;
	}

	public void onScroll(ScrollEvent event) {
		boolean isShortcutDown = event.isShortcutDown();
		if (ChartUtils.isInXAndYBounds(config.getChart(), event.getX(), event.getY())) {
			double[] d = ChartUtils.sceneToChartValues(event.getX(), event.getY(), config.getXAxis(),
					config.getYAxis());
			double mouseXDiffToLowerBound = d[0] - config.getXAxis().getLowerBound();
			double mouseXDiffToUpperBound = config.getXAxis().getUpperBound() - d[0];

			double zoomFactor = isShortcutDown ? 0.5 : 0.1;
			double newLowerBound;
			double newUpperBound;

			if (event.getDeltaY() > 0) {
				newLowerBound = config.getXAxis().getLowerBound() + mouseXDiffToLowerBound * zoomFactor;
				newUpperBound = config.getXAxis().getUpperBound() - mouseXDiffToUpperBound * zoomFactor;
			} else {
				newLowerBound = config.getXAxis().getLowerBound() - mouseXDiffToLowerBound * zoomFactor;
				newUpperBound = config.getXAxis().getUpperBound() + mouseXDiffToUpperBound * zoomFactor;
			}
			ChartUtils.setLowerXBoundWithinRange(config.getChart(), newLowerBound);
			ChartUtils.setUpperXBoundWithinRange(config.getChart(), newUpperBound);
		}
	}
}
