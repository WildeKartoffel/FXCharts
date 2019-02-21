package com.pixelduke.javafx.chart;

import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.chart.ValueAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;

public class XYChartConfigurator {

	private ValueAxis xAxis;
	private ValueAxis yAxis;

	private Group group;

	private XYChart chart;

	private MouseMode mouseMode = MouseMode.NO_MODE;
	private Cursor previousCursor;

	private DragZoomer dragZoomer;
	private MouseButton dragZoomBtn = MouseButton.PRIMARY;

	private Panner panner;

	private WheelZoomer wheelZoomer;

	public XYChartConfigurator(XYChart chart) {
		this.chart = chart;

		xAxis = (ValueAxis) chart.getXAxis();
		yAxis = (ValueAxis) chart.getYAxis();

		setup();

		dragZoomer = new DragZoomer(this);
		group.getChildren().add(dragZoomer.getNode());

		panner = new Panner(this);

		wheelZoomer = new WheelZoomer(this);
	}

	public Node getNodeRepresentation() {
		return group;
	}

	public XYChart getChart() {
		return chart;
	}

	public ValueAxis getXAxis() {
		return xAxis;
	}

	public ValueAxis getYAxis() {
		return yAxis;
	}

	private void setup() {
		group = createGroup();

		setupOnMousePress();
		setupOnMouseDrag();
		setupOnMouseRelease();
		setupOnScroll();
	}

	private Group createGroup() {
		Group group = new Group();
		group.getStylesheets()
				.add(getClass().getResource("/com/pixelduke/javafx/styles/chartStyles.css").toExternalForm());
		return group;
	}

	private void setupOnMousePress() {
		chart.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {

				configureMouseModeAndCursorOnMousePress(mouseEvent);

				if (mouseMode == MouseMode.ZOOM)
					dragZoomer.startDragZoom(mouseEvent);
				else if (mouseMode == MouseMode.PAN)
					panner.onMousePressed(mouseEvent);

			}
		});
	}

	private void setupOnMouseDrag() {
		chart.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (mouseMode == MouseMode.ZOOM) {
					dragZoomer.onDrag(mouseEvent);
				} else if (mouseMode == MouseMode.PAN) {
					panner.onDrag(mouseEvent);
				}
			}
		});
	}

	private void setupOnMouseRelease() {
		chart.setOnMouseReleased(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				resetCursor();
				if (mouseMode == MouseMode.ZOOM) {
					dragZoomer.onMouseRelease(mouseEvent);
				}
			}
		});
	}

	private void setupOnScroll() {
		chart.setOnScroll(new EventHandler<ScrollEvent>() {
			@Override
			public void handle(ScrollEvent event) {
				wheelZoomer.onScroll(event);
			}
		});
	}

	private void configureMouseModeAndCursorOnMousePress(MouseEvent event) {
		if (!ChartUtils.isInXAndYBounds(chart, event.getX(), event.getY()))
			mouseMode = MouseMode.NO_MODE;
		else if (event.isShortcutDown())
			mouseMode = MouseMode.PAN;
		else if (event.getButton().equals(dragZoomBtn))
			mouseMode = MouseMode.ZOOM;
		else
			mouseMode = MouseMode.NO_MODE;
		storeLastCursor();
		changeCursor();
	}

	private void storeLastCursor() {
		previousCursor = chart.getCursor();
	}

	private void changeCursor() {
		if (mouseMode == MouseMode.ZOOM) {
			ChartUtils.setCursor(chart, Cursor.W_RESIZE);
		} else if (mouseMode == MouseMode.PAN)
			ChartUtils.setCursor(chart, Cursor.OPEN_HAND);
	}

	private void resetCursor() {
		ChartUtils.setCursor(chart, previousCursor);
	}
}
