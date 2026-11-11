package org.snomed.otf.script.dao;

import com.google.api.services.sheets.v4.model.Color;
import org.ihtsdo.otf.RF2Constants.Severity;

/**
 * RP-1177 Background colour a report can give a row as it writes it.
 * Applied as a static fill, so users can still recolour the row by hand.
 */
public enum Highlight {
	RED(0.96f, 0.8f, 0.8f),
	ORANGE(0.99f, 0.9f, 0.8f),
	YELLOW(1.0f, 0.95f, 0.8f),
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

	/**
	 * @return the highlight for a row reported at this severity, or null if the row isn't to be highlighted
	 */
	public static Highlight forSeverity(Severity severity) {
		if (severity == null) {
			return null;
		}
		return switch (severity) {
			case CRITICAL -> RED;
			case HIGH -> ORANGE;
			case MEDIUM -> YELLOW;
			default -> null;
		};
	}

	/**
	 * @return the highlight for the first severity found in a row's details (searching any nested arrays),
	 * or null if there's no severity to highlight
	 */
	public static Highlight forDetails(Object... details) {
		if (details == null) {
			return null;
		}
		for (Object detail : details) {
			if (detail instanceof Severity severity) {
				return forSeverity(severity);
			} else if (detail instanceof Object[] nested) {
				Highlight highlight = forDetails(nested);
				if (highlight != null) {
					return highlight;
				}
			}
		}
		return null;
	}
}
