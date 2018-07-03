package org.ihtsdo.otf.rest.client.snowowl.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "active", "moduleId", "fsn", "descriptions"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleConceptPojo {

	private static final String FULLY_DEFINED_STATUS_ID = "900000000000073002";

	private static final String PRIMITIVE_DEFINITION_STATUS_ID = "900000000000074008";

	private static final String PRIMITIVE = "PRIMITIVE";

	private String id;
	
	private boolean active;
	
	private String moduleId;
	
	private String definitionStatus;
	
	private FsnPojo fsn;	
	
	private SimpleDescriptionResponse descriptions;
	


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	public String getDefinitionStatus() {
		return definitionStatus;
	}

	public void setDefinitionStatus(String definitionStatus) {
		this.definitionStatus = definitionStatus;
	}
	
	public String getDefinitionStatusId() {
		
		if (PRIMITIVE.equals(definitionStatus)) {
			return PRIMITIVE_DEFINITION_STATUS_ID;
		} 
		return FULLY_DEFINED_STATUS_ID;
	}

	
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (active ? 1231 : 1237);
		result = prime * result + ((definitionStatus == null) ? 0 : definitionStatus.hashCode());
		result = prime * result + ((descriptions == null) ? 0 : descriptions.hashCode());
		result = prime * result + ((fsn == null) ? 0 : fsn.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
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
		SimpleConceptPojo other = (SimpleConceptPojo) obj;
		if (active != other.active)
			return false;
		if (definitionStatus == null) {
			if (other.definitionStatus != null)
				return false;
		} else if (!definitionStatus.equals(other.definitionStatus))
			return false;
		if (descriptions == null) {
			if (other.descriptions != null)
				return false;
		} else if (!descriptions.equals(other.descriptions))
			return false;
		if (fsn == null) {
			if (other.fsn != null)
				return false;
		} else if (!fsn.equals(other.fsn))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (moduleId == null) {
			if (other.moduleId != null)
				return false;
		} else if (!moduleId.equals(other.moduleId))
			return false;
		return true;
	}

	public FsnPojo getFsn() {
		return fsn;
	}

	public void setFsn(FsnPojo fsn) {
		this.fsn = fsn;
	}

	public SimpleDescriptionResponse getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(SimpleDescriptionResponse descriptionsResponse) {
		this.descriptions = descriptionsResponse;
	}
	

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleConceptPojo [");
		if (id != null)
			builder.append("id=").append(id).append(", ");
		builder.append("active=").append(active).append(", ");
		if (fsn != null)
			builder.append("fsn=").append(fsn);
		builder.append("]");
		return builder.toString();
	}
	
	public class FsnPojo {
		
		private String id;
		private boolean active;
		private String effectiveTime;
		private String moduleId;
		private String term;
		private String conceptId;
		
		public String getId() {
			return id;
		}
		
		public void setId(String id) {
			this.id = id;
		}
		
		public String getEffectiveTime() {
			return effectiveTime;
		}
		
		public void setEffectiveTime(String effectiveTime) {
			this.effectiveTime = effectiveTime;
		}
		
		public String getModuleId() {
			return moduleId;
		}
		
		public void setModuleId(String moduleId) {
			this.moduleId = moduleId;
		}
		
		public String getTerm() {
			return term;
		}
		
		public void setTerm(String term) {
			this.term = term;
		}
		
		public String getConceptId() {
			return conceptId;
		}
		
		public void setConceptId(String conceptId) {
			this.conceptId = conceptId;
		}
		
		public boolean isActive() {
			return active;
		}

		public void setActive(boolean active) {
			this.active = active;
		}
	}
}