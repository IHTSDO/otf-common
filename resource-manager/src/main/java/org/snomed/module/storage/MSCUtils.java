package org.snomed.module.storage;

import java.net.URI;

public class MSCUtils {
	private static final String EDITION_URI_TEMPLATE =
			"http://snomed.info/sct/%s";

	private static final String VERSIONED_EDITION_URI_TEMPLATE =
			"http://snomed.info/sct/%s/version/%s";

	public static URI editionUri(String moduleId, Object effectiveTime) {
		return effectiveTime == null
				? URI.create(EDITION_URI_TEMPLATE.formatted(moduleId))
				: URI.create(VERSIONED_EDITION_URI_TEMPLATE.formatted(moduleId, effectiveTime));
	}
}
