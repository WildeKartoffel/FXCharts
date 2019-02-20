package com.pixelduke.javafx.chart;

import javafx.scene.input.MouseEvent;

public enum MouseButton {
	PRIMARY, SECONDARY, MIDDLE;

	public static boolean isPressed(MouseButton btn, MouseEvent event) {
		switch (btn) {
		case MIDDLE:
			return event.isMiddleButtonDown();
		case PRIMARY:
			return event.isPrimaryButtonDown();
		case SECONDARY:
			return event.isSecondaryButtonDown();
		default:
			return false;
		}
	}
}
