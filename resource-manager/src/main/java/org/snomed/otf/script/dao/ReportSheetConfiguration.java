package org.snomed.otf.script.dao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportSheetConfiguration {
	public static final Integer DEFAULT_COLUMN_WIDTH_MINIMUM = 100;
	public static final Integer DEFAULT_COLUMN_WIDTH_MAXIMUM = 800;
	public static final double DEFAULT_COLUMN_WIDTH_FONT_RATIO = 9.5; // Equates to average width of character in pixels.

	@Value("${column.width.minimum:100}")
	Integer columnWidthMinimum = DEFAULT_COLUMN_WIDTH_MINIMUM;

	@Value("${column.width.maximum:800}")
	Integer columnWidthMaximum = DEFAULT_COLUMN_WIDTH_MAXIMUM;

	@Value("${column.width.font_ratio:9.5}")
	Double columnWidthFontRatio = DEFAULT_COLUMN_WIDTH_FONT_RATIO;

	public Integer getColumnWidthMinimum() {
		return columnWidthMinimum;
	}

	public Integer getColumnWidthMaximum() {
		return columnWidthMaximum;
	}

	public double getColumnWidthFontRatio() {
		return columnWidthFontRatio;
	}
}
