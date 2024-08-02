package org.snomed.otf.traceability.domain;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum ComponentChangeType {
	// NB - New enums must go at the end of the list because of the JPA integer mapping
	CREATE, UPDATE, INACTIVATE, DELETE, @JsonEnumDefaultValue UKNOWN_COMPONENT_CHANGE_TYPE

}
