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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((acceptabilityMap == null) ? 0 : acceptabilityMap.hashCode());
		result = prime * result + (active ? 1231 : 1237);
		result = prime * result + ((caseSignificance == null) ? 0 : caseSignificance.hashCode());
		result = prime * result + ((conceptId == null) ? 0 : conceptId.hashCode());
		result = prime * result + ((descriptionId == null) ? 0 : descriptionId.hashCode());
		result = prime * result + ((effectiveTime == null) ? 0 : effectiveTime.hashCode());
		result = prime * result + ((inactivationIndicator == null) ? 0 : inactivationIndicator.hashCode());
		result = prime * result + ((lang == null) ? 0 : lang.hashCode());
		result = prime * result + ((moduleId == null) ? 0 : moduleId.hashCode());
		result = prime * result + (released ? 1231 : 1237);
		result = prime * result + ((term == null) ? 0 : term.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		DescriptionPojo other = (DescriptionPojo) obj;
		if (acceptabilityMap == null) {
			if (other.acceptabilityMap != null)
				return false;
		} else if (!acceptabilityMap.equals(other.acceptabilityMap))
			return false;
		if (active != other.active)
			return false;
		if (caseSignificance == null) {
			if (other.caseSignificance != null)
				return false;
		} else if (!caseSignificance.equals(other.caseSignificance))
			return false;
		if (conceptId == null) {
			if (other.conceptId != null)
				return false;
		} else if (!conceptId.equals(other.conceptId))
			return false;
		if (descriptionId == null) {
			if (other.descriptionId != null)
				return false;
		} else if (!descriptionId.equals(other.descriptionId))
			return false;
		if (effectiveTime == null) {
			if (other.effectiveTime != null)
				return false;
		} else if (!effectiveTime.equals(other.effectiveTime))
			return false;
		if (inactivationIndicator == null) {
			if (other.inactivationIndicator != null)
				return false;
		} else if (!inactivationIndicator.equals(other.inactivationIndicator))
			return false;
		if (lang == null) {
			if (other.lang != null)
				return false;
		} else if (!lang.equals(other.lang))
			return false;
		if (moduleId == null) {
			if (other.moduleId != null)
				return false;
		} else if (!moduleId.equals(other.moduleId))
			return false;
		if (released != other.released)
			return false;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DescriptionPojo{" +
				"descriptionId='" + descriptionId + '\'' +
				", active=" + active +
				", effectiveTime='" + effectiveTime + '\'' +
				", released=" + released +
				", term='" + term + '\'' +
				", conceptId='" + conceptId + '\'' +
				", moduleId='" + moduleId + '\'' +
				", lang='" + lang + '\'' +
				", type='" + type + '\'' +
				", caseSignificance='" + caseSignificance + '\'' +
				", acceptabilityMap=" + acceptabilityMap +
				", inactivationIndicator='" + inactivationIndicator + '\'' +
				'}';
	}
}
