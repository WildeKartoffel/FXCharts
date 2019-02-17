package com.pixelduke.javafx.chart;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class XYChartConfigurator {

	private ValueAxis xAxis;
	private ValueAxis yAxis;

	private Rectangle zoomRectangle;
	private Group group;

	private XYChart chart;

	private long interval = 0;

	private AxisConstraint axisConstraint = AxisConstraint.Horizontal;

	private SimpleObjectProperty<MouseMode> mouseModeProperty = new SimpleObjectProperty<MouseMode>();

	private SimpleBooleanProperty isZoomingEnabled = new SimpleBooleanProperty(true);

	// temporary variables used with zooming
	double initialMouseX;
	double initialMouseY;
	double initialMouseSceneX;
	double initialMouseSceneY;

	double lastMouseDragX;
	double lastMouseDragY;

	Cursor previousMouseCursor;

	boolean isShowingOnlyYPositiveValues;
	private boolean isVerticalPanningAllowed;

	public XYChartConfigurator(XYChart chart) {
		this.chart = chart;

		xAxis = (ValueAxis) chart.getXAxis();
		yAxis = (ValueAxis) chart.getYAxis();

		setupZoom();
	}

	public Node getNodeRepresentation() {
//		return chartGroup;
		return group;
	}

	public XYChart getChart() {
		return chart;
	}

	public void setAxisConstraint(AxisConstraint axisConstraint) {
		this.axisConstraint = axisConstraint;
	}

	private Rectangle createZoomRectangle() {
		final Rectangle zoomArea = new Rectangle();
		zoomArea.setStrokeWidth(0);
		Color BLUE_COLOR = Color.LIGHTBLUE;
		Color zoomAreaFill = new Color(BLUE_COLOR.getRed(), BLUE_COLOR.getGreen(), BLUE_COLOR.getBlue(), 0.5);
		zoomArea.setFill(zoomAreaFill);
		return zoomArea;
	}

	private void setupZoom() {
		zoomRectangle = createZoomRectangle();

		group = new Group();
		group.getStylesheets()
				.add(getClass().getResource("/com/pixelduke/javafx/styles/chartStyles.css").toExternalForm());
		group.getChildren().add(zoomRectangle);

		zoomRectangle.getStyleClass().add("zoom-rectangle");

		zoomRectangle.setVisible(false);
//		chartGroup = new Group();
//		chartGroup.getChildren().addAll(zoomRectangle, chart);

		chart.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				setMouseMode(mouseEvent);
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
					stayInHorizontalBounds();
				}
			}
		});

		chart.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				setChartCursor(previousMouseCursor);
				if (mouseModeProperty.get() != MouseMode.ZOOM)
					return;

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

			}
		});
	}

	public void stayInHorizontalBounds() {
		long oldestTime = getOldestTime();
		long latestTime = getLatestTime();
		if (xAxis.getLowerBound() < oldestTime)
			xAxis.setLowerBound(oldestTime);
		if (xAxis.getUpperBound() > latestTime)
			xAxis.setUpperBound(latestTime);
	}

	public void zoomOut() {
		xAxis.setLowerBound(getOldestTime());
		xAxis.setUpperBound(getLatestTime());
	}

	private long getOldestTime() {
		ObservableList<Series<Long, Number>> data = chart.getData();
		if (data != null && data.size() >= 1 && data.get(0).getData().size() >= 1)
			return data.get(0).getData().get(0).getXValue() - getInterval();
		return System.currentTimeMillis();
	}

	private long getLatestTime() {
		ObservableList<Series<Long, Number>> data = chart.getData();
		if (data != null && data.size() >= 1 && data.get(0).getData().size() >= 1)
			return data.get(0).getData().get(data.get(0).getData().size() - 1).getXValue() + getInterval();
		return System.currentTimeMillis();
	}

	private long getInterval() {
		if (isIntervalCalculated())
			return interval;
		ObservableList<Series<Long, Number>> data = chart.getData();
		if (data != null && data.size() >= 1 && data.get(0).getData().size() >= 2) {
			interval = data.get(0).getData().get(1).getXValue() - data.get(0).getData().get(0).getXValue();
			return interval;
		}
		return 86400000l;
	}

	private boolean isIntervalCalculated() {
		return interval != 0;
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

	public void setIsZoomingEnabled(boolean isEnabled) {
		isZoomingEnabled.set(isEnabled);
	}

	public boolean getIsZoomingEnabled() {
		return isZoomingEnabled.get();
	}

	public SimpleBooleanProperty isZoomingEnabledProperty() {
		return isZoomingEnabled;
	}

	public MouseMode getMouseMode() {
		return mouseModeProperty.get();
	}

	public void setMouseMode(MouseEvent event) {
		if (event.isShortcutDown())
			setMouseMode(MouseMode.PAN);
		else
			setMouseMode(MouseMode.ZOOM);
	}

	public void setMouseMode(MouseMode mode) {
		if (mode == this.getMouseMode())
			return;

		this.mouseModeProperty.set(mode);
		if (mouseModeProperty.get() == MouseMode.ZOOM) {
			setIsZoomingEnabled(true);
			setChartCursor(Cursor.DEFAULT);
		} else {
			setIsZoomingEnabled(false);
			if (mouseModeProperty.get() == MouseMode.PAN)
				setChartCursor(Cursor.OPEN_HAND);
		}
	}

	public SimpleObjectProperty<MouseMode> mouseModeProperty() {
		return mouseModeProperty;
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

	public void setPrefWidth(double size) {
		chart.setPrefWidth(size);
	}

	public void setPrefHeight(double size) {
		chart.setPrefHeight(size);
	}

	public void setMaxWidth(double size) {
		chart.setMaxWidth(size);
	}

	public void setMaxHeight(double size) {
		chart.setMaxHeight(size);
	}

	public void setChartCursor(Cursor cursor) {
		Pane chartPane = (Pane) ReflectionUtils.forceFieldCall(Chart.class, "chartContent", chart);
		chartPane.setCursor(cursor);
	}

	public void setIsShowingOnlyYPositiveValues(boolean onlyYPositive) {
		isShowingOnlyYPositiveValues = onlyYPositive;
	}
}
