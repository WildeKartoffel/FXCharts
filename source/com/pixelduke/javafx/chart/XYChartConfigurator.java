package com.pixelduke.javafx.chart;

import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.Chart;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

public class XYChartConfigurator {

	private ValueAxis xAxis;
	private ValueAxis yAxis;

	private Group group;

	private XYChart chart;

	private Number interval = 0;

	private AxisConstraint axisConstraint = AxisConstraint.Horizontal;

	private MouseMode mouseMode;

	private Rectangle zoomRectangle;
	private MouseButton dragZoomBtn = MouseButton.PRIMARY;

	// temporary variables used with zooming
	double initialMouseX;
	double initialMouseY;
	double initialMouseSceneX;
	double initialMouseSceneY;

	double lastMouseDragX;
	double lastMouseDragY;

	Cursor previousCursor;

	boolean isShowingOnlyYPositiveValues;
	private boolean isVerticalPanningAllowed;

	public XYChartConfigurator(XYChart chart) {
		this.chart = chart;

		xAxis = (ValueAxis) chart.getXAxis();
		yAxis = (ValueAxis) chart.getYAxis();

		setupZoom();
	}

	public Node getNodeRepresentation() {
		return group;
	}

	public void setAxisConstraint(AxisConstraint axisConstraint) {
		this.axisConstraint = axisConstraint;
	}

	public void stayInHorizontalMaximumBounds() {
		double oldestTime = getOldestX().doubleValue();
		double latestTime = getLatestX().doubleValue();
		if (xAxis.getLowerBound() < oldestTime)
			xAxis.setLowerBound(oldestTime);
		if (xAxis.getUpperBound() > latestTime)
			xAxis.setUpperBound(latestTime);
	}

	public void zoomOut() {
		xAxis.setLowerBound(getOldestX().doubleValue());
		xAxis.setUpperBound(getLatestX().doubleValue());
	}

	private void setupZoom() {
		initializeNodes();

		chart.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {

				configureMouseModeAndCursor(mouseEvent);
				storeLastPositions(mouseEvent);

				if (mouseMode == MouseMode.ZOOM) {
					startDragZoom();
				} else if (mouseMode == MouseMode.PAN) {
					startPan();
				}
			}
		});
		chart.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				double mouseSceneX = mouseEvent.getSceneX();
				double mouseSceneY = mouseEvent.getSceneY();

				if (mouseMode == MouseMode.ZOOM) {
					updateZoomNode(mouseSceneX, mouseSceneY);
				} else if (mouseMode == MouseMode.PAN) {
					doDrag(mouseSceneX, mouseSceneY);
				}
			}
		});

		chart.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				resetCursor();
				if (mouseMode == MouseMode.ZOOM) {
					zoomRectangle.setVisible(false);

					double newMouseX = mouseEvent.getSceneX();
					double newMouseY = mouseEvent.getSceneY();

					if (newMouseX < initialMouseSceneX) {
						zoomOut();
					} else if (newMouseX > initialMouseSceneX && axisConstraint == AxisConstraint.Horizontal) {
						zoomInHorizontally(newMouseX, newMouseY);
					}
				}
			}
		});
	}

	private void initializeNodes() {
		group = createGroup();
		zoomRectangle = createZoomRectangle();
		group.getChildren().add(zoomRectangle);
	}

	private Group createGroup() {
		Group group = new Group();
		group.getStylesheets()
				.add(getClass().getResource("/com/pixelduke/javafx/styles/chartStyles.css").toExternalForm());
		return group;
	}

	private Rectangle createZoomRectangle() {
		final Rectangle zoomArea = new Rectangle();
		zoomArea.getStyleClass().add("zoom-rectangle");
		zoomArea.setVisible(false);
		return zoomArea;
	}

	private void storeLastPositions(MouseEvent mouseEvent) {
		initialMouseX = mouseEvent.getX();
		initialMouseY = mouseEvent.getY();
		initialMouseSceneX = mouseEvent.getSceneX();
		initialMouseSceneY = mouseEvent.getSceneY();

		lastMouseDragX = initialMouseSceneX;
		lastMouseDragY = initialMouseSceneY;
	}

	private void startDragZoom() {
		if (axisConstraint == AxisConstraint.Horizontal) {
			zoomRectangle.setX(initialMouseX);
			setChartCursor(Cursor.W_RESIZE);
			zoomRectangle.setY(chart.getHeight() - xAxis.boundsInParentProperty().getValue().getMaxY() - 4);
		}
	}

	private void startPan() {
		setChartCursor(Cursor.CLOSED_HAND);
	}

	private void updateZoomNode(double newMouseX, double newMouseY) {
		zoomRectangle.toFront();
		zoomRectangle.setVisible(true);

		if (axisConstraint == AxisConstraint.Vertical)
			zoomRectangle.setWidth(xAxis.getWidth());
		else
			zoomRectangle.setWidth(newMouseX - initialMouseSceneX);

		if (axisConstraint == AxisConstraint.Horizontal)
			zoomRectangle.setHeight(yAxis.getHeight());
		else
			zoomRectangle.setHeight(newMouseY - initialMouseSceneY);
	}

	private void doDrag(double newMouseX, double newMouseY) {
		double dragX = newMouseX - lastMouseDragX;
		double dragY = newMouseY - lastMouseDragY;

		lastMouseDragX = newMouseX;
		lastMouseDragY = newMouseY;
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

	private void zoomInHorizontally(double newMouseX, double newMouseY) {
		// zoom in horizontally
		setAutoRanging(false);

		double[] newLower = sceneToChartValues(initialMouseSceneX, newMouseY);
		double[] newUpper = sceneToChartValues(newMouseX, initialMouseSceneY);

		xAxis.setLowerBound(newLower[0]);
		xAxis.setUpperBound(newUpper[0]);
	}

	private Number getOldestX() {
		ObservableList<Series<Number, Number>> data = chart.getData();
		if (data != null && data.size() >= 1 && data.get(0).getData().size() >= 1)
			return data.get(0).getData().get(0).getXValue().doubleValue() - getInterval().doubleValue();
		return System.currentTimeMillis();
	}

	private Number getLatestX() {
		ObservableList<Series<Number, Number>> data = chart.getData();
		if (data != null && data.size() >= 1 && data.get(0).getData().size() >= 1)
			return data.get(0).getData().get(data.get(0).getData().size() - 1).getXValue().doubleValue()
					+ getInterval().doubleValue();
		return System.currentTimeMillis();
	}

	private Number getInterval() {
		if (isIntervalCalculated())
			return interval;
		ObservableList<Series<Number, Number>> data = chart.getData();
		if (data != null && data.size() >= 1 && data.get(0).getData().size() >= 2) {
			interval = data.get(0).getData().get(1).getXValue().doubleValue()
					- data.get(0).getData().get(0).getXValue().doubleValue();
			return interval;
		}
		return 86400000l;
	}

	private boolean isIntervalCalculated() {
		return interval.doubleValue() != 0;
	}

	private double[] sceneToChartValues(double sceneX, double sceneY) {
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

	private Dimension2D sceneToChartDistance(double sceneX, double sceneY) {
		double xDataLenght = xAxis.getUpperBound() - xAxis.getLowerBound();
		double yDataLenght = yAxis.getUpperBound() - yAxis.getLowerBound();
		double xPixelLenght = xAxis.getWidth();
		double yPixelLenght = yAxis.getHeight();

		double chartXDistance = sceneX * xDataLenght / xPixelLenght;
		double chartYDistance = sceneY * yDataLenght / yPixelLenght;
		return new Dimension2D(chartXDistance, chartYDistance);
	}

	private void configureMouseModeAndCursor(MouseEvent event) {
		if (event.isShortcutDown())
			mouseMode = MouseMode.PAN;
		else
			mouseMode = MouseMode.ZOOM;
		storeLastCursor();
		changeCursor();
	}

	private void storeLastCursor() {
		previousCursor = chart.getCursor();
	}

	private void changeCursor() {
		if (mouseMode == MouseMode.ZOOM) {
			setChartCursor(Cursor.DEFAULT);
		} else if (mouseMode == MouseMode.PAN)
			setChartCursor(Cursor.OPEN_HAND);
	}

	private void resetCursor() {
		setChartCursor(previousCursor);
	}

	private void setAutoRanging(boolean isAutoRanging) {
		if (axisConstraint == AxisConstraint.Both) {
			xAxis.setAutoRanging(isAutoRanging);
			yAxis.setAutoRanging(isAutoRanging);
		} else if (axisConstraint == AxisConstraint.Horizontal)
			xAxis.setAutoRanging(isAutoRanging);
		else if (axisConstraint == AxisConstraint.Vertical)
			yAxis.setAutoRanging(isAutoRanging);
	}

	private void setChartCursor(Cursor cursor) {
		Pane chartPane = (Pane) ReflectionUtils.forceFieldCall(Chart.class, "chartContent", chart);
		chartPane.setCursor(cursor);
	}
}
