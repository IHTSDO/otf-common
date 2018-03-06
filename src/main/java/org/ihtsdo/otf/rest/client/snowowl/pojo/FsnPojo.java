package org.ihtsdo.otf.rest.client.snowowl.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id, active, effectivetime, moduleid, term, conceptid"})
@JsonIgnoreProperties(ignoreUnknown = true)
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("FsnPojo [");
		if (id != null)
			builder.append("id=").append(id).append(", ");
		if (term != null)
			builder.append("term=").append(term).append(", ");
		if (conceptId != null)
			builder.append("conceptId=").append(conceptId);
		builder.append("]");
		return builder.toString();
	}
	
	
}
