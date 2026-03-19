package org.snomed;

import java.util.Arrays;

public enum FileType {

	DELTA("Delta"),
	SNAPSHOT("Snapshot"),
	FULL("Full");

	private final String displayName;

	FileType(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Canonical external representation (Delta, Snapshot, Full).
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Safe parsing from string (case-insensitive).
	 */
	public static FileType fromString(String value) {
		if (value == null) {
			throw new IllegalArgumentException("FileType cannot be null");
		}

		return Arrays.stream(values())
				.filter(ft -> ft.displayName.equalsIgnoreCase(value.trim()))
				.findFirst()
				.orElseThrow(() ->
						new IllegalArgumentException("Unknown FileType: " + value));
	}

	@Override
	public String toString() {
		return displayName;
	}
}