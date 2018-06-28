package org.ihtsdo.otf.rest.client.snowowl.pojo;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DescriptionPojo {

	private String descriptionId;

	private boolean active;
	
	private String effectiveTime;
	
	private boolean released;
	
	private String term;

	private String conceptId;

	private String moduleId;

	private String lang;

	private String type;

	private String caseSignificance;

	private Map<String, String> acceptabilityMap;

	private String inactivationIndicator;
	
	public DescriptionPojo() {
		active = true;
		acceptabilityMap = new HashMap<>();
	}

	public String getDescriptionId() {
		return descriptionId;
	}

	public void setDescriptionId(String descriptionId) {
		this.descriptionId = descriptionId;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
	
	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	public boolean isReleased() {
		return released;
	}

	public void setReleased(boolean released) {
		this.released = released;
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

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public String getLang() {
		return lang;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCaseSignificance() {
		return caseSignificance;
	}

	public void setCaseSignificance(String caseSignificance) {
		this.caseSignificance = caseSignificance;
	}

	public Map<String, String> getAcceptabilityMap() {
		return acceptabilityMap;
	}

	public void setAcceptabilityMap(Map<String, String> acceptabilityMap) {
		this.acceptabilityMap = acceptabilityMap;
	}

	public String getInactivationIndicator() {
		return inactivationIndicator;
	}

	public void setInactivationIndicator(String inactivationIndicator) {
		this.inactivationIndicator = inactivationIndicator;
	}
}
