package com.pixelduke.javafx.chart;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.chart.ValueAxis;

public class DataHelper {

	public static void invalidateYAxis(long[] xDataPoints, double[] yDataPoints, ValueAxis<Number> yAxis,
			long xLowerBound, long xUpperBound) {

		if (xDataPoints.length != yDataPoints.length)
			throw new UnsupportedOperationException(
					"xDataPoints.length = " + xDataPoints.length + " != yDataPoints.length = " + yDataPoints.length);

		List<Number> yData = new ArrayList<>();

		for (int i = 0; i < yDataPoints.length; i++) {
			double yDataPoint = yDataPoints[i];
			long xDataPoint = xDataPoints[i];

			if (xDataPoint > xLowerBound && xDataPoint < xUpperBound)
				yData.add(yDataPoint);
		}

		yAxis.invalidateRange(yData);

	}
}
