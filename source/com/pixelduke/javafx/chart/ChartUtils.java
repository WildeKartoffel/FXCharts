package com.pixelduke.javafx.chart;

import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.chart.Chart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.layout.Pane;

public class ChartUtils {

	public static Number getOldestX(XYChart<Number, Number> chart) {
		ObservableList<Series<Number, Number>> data = chart.getData();
		if (data != null && data.size() >= 1 && data.get(0).getData().size() >= 1)
			return data.get(0).getData().get(0).getXValue().doubleValue() - getInterval(chart).doubleValue();
		return System.currentTimeMillis();
	}

	public static Number getLatestX(XYChart<Number, Number> chart) {
		ObservableList<Series<Number, Number>> data = chart.getData();
		if (data != null && data.size() >= 1 && data.get(0).getData().size() >= 1)
			return data.get(0).getData().get(data.get(0).getData().size() - 1).getXValue().doubleValue()
					+ getInterval(chart).doubleValue();
		return System.currentTimeMillis();
	}

	public static Number getInterval(XYChart<Number, Number> chart) {
		ObservableList<Series<Number, Number>> data = chart.getData();
		if (data != null && data.size() >= 1 && data.get(0).getData().size() >= 2) {
			return data.get(0).getData().get(1).getXValue().doubleValue()
					- data.get(0).getData().get(0).getXValue().doubleValue();
		}
		return 86400000l;
	}

	public static void zoomOutHorizontally(XYChart<Number, Number> chart) {
		ValueAxis xAxis = (ValueAxis) chart.getXAxis();
		xAxis.setLowerBound(ChartUtils.getOldestX(chart).doubleValue());
		xAxis.setUpperBound(ChartUtils.getLatestX(chart).doubleValue());
	}

	public static double[] sceneToChartValues(double sceneX, double sceneY, ValueAxis xAxis, ValueAxis yAxis) {
		double xDataLenght = xAxis.getUpperBound() - xAxis.getLowerBound();
		double yDataLenght = yAxis.getUpperBound() - yAxis.getLowerBound();
		double xPixelLenght = xAxis.getWidth();
		double yPixelLenght = yAxis.getHeight();

		Point2D leftBottomChartPos = xAxis.localToScene(0, 0);
		double xMinPixelCoord = leftBottomChartPos.getX();
		double yMinPixelCoord = leftBottomChartPos.getY();

		double chartXCoord = xAxis.getLowerBound() + ((sceneX - xMinPixelCoord) * xDataLenght / xPixelLenght);
		double chartYcoord = yAxis.getLowerBound() + ((yMinPixelCoord - sceneY) * yDataLenght / yPixelLenght);
		return new double[] { chartXCoord, chartYcoord };
	}

	public static Dimension2D sceneToChartDistance(double sceneX, double sceneY, ValueAxis xAxis, ValueAxis yAxis) {
		double xDataLenght = xAxis.getUpperBound() - xAxis.getLowerBound();
		double yDataLenght = yAxis.getUpperBound() - yAxis.getLowerBound();
		double xPixelLenght = xAxis.getWidth();
		double yPixelLenght = yAxis.getHeight();

		double chartXDistance = sceneX * xDataLenght / xPixelLenght;
		double chartYDistance = sceneY * yDataLenght / yPixelLenght;
		return new Dimension2D(chartXDistance, chartYDistance);
	}

	public static void stayInHorizontalBounds(XYChart chart) {
		ValueAxis xAxis = (ValueAxis) chart.getXAxis();

		double oldestTime = ChartUtils.getOldestX(chart).doubleValue();
		double latestTime = ChartUtils.getLatestX(chart).doubleValue();
		if (xAxis.getLowerBound() < oldestTime)
			xAxis.setLowerBound(oldestTime);
		if (xAxis.getUpperBound() > latestTime)
			xAxis.setUpperBound(latestTime);
	}

	public static void setCursor(Chart chart, Cursor cursor) {
		Pane chartPane = (Pane) ReflectionUtils.forceFieldCall(Chart.class, "chartContent", chart);
		chartPane.setCursor(cursor);
	}

	public static void setLowerXBoundWithinRange(XYChart chart, double lowerBound) {
		chart.getXAxis().setAutoRanging(false);

		double lowerBoundWithinRange = Math.max(getOldestX(chart).doubleValue(), lowerBound);
		((ValueAxis) chart.getXAxis()).setLowerBound(lowerBoundWithinRange);
	}

	public static void setUpperXBoundWithinRange(XYChart chart, double upperBound) {
		chart.getXAxis().setAutoRanging(false);

		double upperBoundWithinRange = Math.min(getLatestX(chart).doubleValue(), upperBound);
		((ValueAxis) chart.getXAxis()).setUpperBound(upperBoundWithinRange);
	}

	public static boolean isInXBounds(ValueAxis axis, double value) {
		return axis.getLowerBound() <= value && axis.getUpperBound() >= value;
	}

	public static boolean isInXAndYBounds(XYChart chart, double xScene, double yScene) {
		ValueAxis xAxis = (ValueAxis) chart.getXAxis();
		ValueAxis yAxis = (ValueAxis) chart.getYAxis();
		double[] mousePositionOnChart = ChartUtils.sceneToChartValues(xScene, yScene, xAxis, yAxis);
		if (mousePositionOnChart[0] < xAxis.getLowerBound())
			return false;
		if (mousePositionOnChart[0] > xAxis.getUpperBound())
			return false;
		if (mousePositionOnChart[1] < yAxis.getLowerBound())
			return false;
		if (mousePositionOnChart[1] > yAxis.getUpperBound())
			return false;
		return true;
	}
}
