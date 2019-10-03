package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptMiniPojo {

	private String conceptId;

	@JsonDeserialize(using = DescriptionDeserializer.class)
	private String fsn;
	
	private String moduleId;
	
	private String definitionStatus;
	
	public ConceptMiniPojo() {
	}

	public ConceptMiniPojo(String conceptId) {
		this.conceptId = conceptId;
	}

	public String getConceptId() {
		return conceptId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
	}

	public String getFsn() {
		return fsn;
	}

	public void setFsn(String fsn) {
		this.fsn = fsn;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public String getDefinitionStatus() {
		return definitionStatus;
	}

	public void setDefinitionStatus(String definitionStatus) {
		this.definitionStatus = definitionStatus;
	}
	
	

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ConceptMiniPojo [");
		if (conceptId != null)
			builder.append("conceptId=").append(conceptId).append(", ");
		if (fsn != null)
			builder.append("fsn=").append(fsn);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((conceptId == null) ? 0 : conceptId.hashCode());
		result = prime * result + ((definitionStatus == null) ? 0 : definitionStatus.hashCode());
		result = prime * result + ((fsn == null) ? 0 : fsn.hashCode());
		result = prime * result + ((moduleId == null) ? 0 : moduleId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ConceptMiniPojo other = (ConceptMiniPojo) obj;
		if (conceptId == null) {
			if (other.conceptId != null)
				return false;
		} else if (!conceptId.equals(other.conceptId))
			return false;
		if (definitionStatus == null) {
			if (other.definitionStatus != null)
				return false;
		} else if (!definitionStatus.equals(other.definitionStatus))
			return false;
		if (fsn == null) {
			if (other.fsn != null)
				return false;
		} else if (!fsn.equals(other.fsn))
			return false;
		if (moduleId == null) {
			if (other.moduleId != null)
				return false;
		} else if (!moduleId.equals(other.moduleId))
			return false;
		return true;
	}
}
