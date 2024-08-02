package org.snomed.otf.traceability.domain;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum ChangeType {
	CREATE, DELETE, INACTIVATE, UPDATE, @JsonEnumDefaultValue UKNOWN_CHANGE_TYPE
}
