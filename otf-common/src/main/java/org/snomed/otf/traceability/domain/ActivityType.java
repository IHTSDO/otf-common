package org.snomed.otf.traceability.domain;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum ActivityType {
	// NB - New enums must go at the end of the list because of the JPA integer mapping
	CONTENT_CHANGE, CLASSIFICATION_SAVE, REBASE, PROMOTION, @JsonEnumDefaultValue UKNOWN_ACTIVITY_TYPE
}
