package org.snomed.otf.traceability.domain;

public enum ComponentSubType {
	// NB - New enums must go at the end of the list because of the JPA integer mapping
	STATED_RELATIONSHIP, INFERRED_RELATIONSHIP, FSN_DESCRIPTION, SYNONYM_DESCRIPTION

}
