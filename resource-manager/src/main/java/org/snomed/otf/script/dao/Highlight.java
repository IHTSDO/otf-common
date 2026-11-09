package org.snomed.otf.script.dao;

import com.google.api.services.sheets.v4.model.Color;

/**
 * RP-1177 Background colour a report can give a row as it writes it.
 * Applied as a static fill, so users can still recolour the row by hand.
 */
public enum Highlight {
	YELLOW(1.0f, 0.95f, 0.8f),
	RED(0.96f, 0.8f, 0.8f),
	GREEN(0.85f, 0.92f, 0.83f);

	private final float red;
	private final float green;
	private final float blue;

	Highlight(float red, float green, float blue) {
		this.red = red;
		this.green = green;
		this.blue = blue;
	}

	public Color toColor() {
		return new Color().setRed(red).setGreen(green).setBlue(blue);
	}
}
