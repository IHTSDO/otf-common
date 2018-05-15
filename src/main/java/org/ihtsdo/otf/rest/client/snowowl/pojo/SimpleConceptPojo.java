package org.ihtsdo.otf.rest.client.snowowl.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "active", "fsn", "descriptions"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleConceptPojo {

	private static final String FULLY_DEFINED_STATUS_ID = "900000000000073002";

	private static final String PRIMITIVE_DEFINITION_STATUS_ID = "900000000000074008";

	private static final String PRIMITIVE = "PRIMITIVE";

	private String id;
	
	private boolean active;
	
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