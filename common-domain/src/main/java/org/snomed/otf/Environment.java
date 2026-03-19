package org.snomed.otf;

import java.util.List;
import java.util.Locale;

public enum Environment {

	DEV,
	UAT,
	PROD;

	private final String lowerName;
	private List<String> readFallback;

	static {
		DEV.readFallback = List.of(DEV.lowerName, PROD.lowerName);
		UAT.readFallback = List.of(UAT.lowerName, PROD.lowerName);
		PROD.readFallback = List.of(PROD.lowerName);
	}

	Environment() {
		this.lowerName = name().toLowerCase(Locale.ROOT);
	}

	public static Environment fromString(String value) {
		if (value == null) {
			throw new IllegalArgumentException("Environment cannot be null");
		}
		return valueOf(value.trim().toUpperCase(Locale.ROOT));
	}

	public String getEnvironmentName() {
		return lowerName;
	}

	public List<String> getReadFallback() {
		return readFallback;
	}
}