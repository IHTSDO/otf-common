package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

public enum DefinitionStatus {

	FULLY_DEFINED("900000000000073002"), PRIMITIVE("900000000000074008");

	private String conceptId;

	DefinitionStatus(String conceptId) {
		this.conceptId = conceptId;
	}

	public String getConceptId() {
		return conceptId;
	}
}
