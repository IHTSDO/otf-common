package org.ihtsdo.otf.rest.client.snowowl.pojo;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"conceptId", "effectiveTime", "active", "released", "inactivationIndicator", "moduleId", "definitionStatus", "descriptions", "relationships"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptPojo {

	private String conceptId;

	private String effectiveTime;
	
	private boolean released;

	private boolean active;
	
	private String moduleId;

	private DefinitionStatus definitionStatus;

	private String inactivationIndicator;

	private Map<String, String[]> associationTargets;

	private Set<DescriptionPojo> descriptions;

	private Set<RelationshipPojo> relationships;
	
	public ConceptPojo() {
		active = true;
	}

	public String getFsn() {
		if (descriptions != null) {
			for (DescriptionPojo description : descriptions) {
				if ("FSN".equals(description.getType()) && description.isActive()) {
					return description.getTerm();
				}
			}
		}
		return null;
	}

	public void add(RelationshipPojo relationship) {
		if (relationships == null) {
			relationships = new HashSet<>();
		}
		relationships.add(relationship);
	}

	public void add(DescriptionPojo description) {
		if (descriptions == null) {
			descriptions = new HashSet<>();
		}
		descriptions.add(description);
	}

	public String getConceptId() {
		return conceptId;
	}

	public void setConceptId(String conceptId) {
		this.conceptId = conceptId;
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public DefinitionStatus getDefinitionStatus() {
		return definitionStatus;
	}

	public void setDefinitionStatus(DefinitionStatus definitionStatus) {
		this.definitionStatus = definitionStatus;
	}

	public String getInactivationIndicator() {
		return inactivationIndicator;
	}

	public void setInactivationIndicator(String inactivationIndicator) {
		this.inactivationIndicator = inactivationIndicator;
	}

	public Map<String, String[]> getAssociationTargets() {
		return associationTargets;
	}

	public void setAssociationTargets(Map<String, String[]> associationTargets) {
		this.associationTargets = associationTargets;
	}

	public Set<DescriptionPojo> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(Set<DescriptionPojo> descriptions) {
		this.descriptions = descriptions;
	}

	public Set<RelationshipPojo> getRelationships() {
		return relationships;
	}

	public void setRelationships(Set<RelationshipPojo> relationships) {
		this.relationships = relationships;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (active ? 1231 : 1237);
		result = prime * result + ((associationTargets == null) ? 0 : associationTargets.hashCode());
		result = prime * result + ((conceptId == null) ? 0 : conceptId.hashCode());
		result = prime * result + ((definitionStatus == null) ? 0 : definitionStatus.hashCode());
		result = prime * result + ((descriptions == null) ? 0 : descriptions.hashCode());
		result = prime * result + ((effectiveTime == null) ? 0 : effectiveTime.hashCode());
		result = prime * result + ((inactivationIndicator == null) ? 0 : inactivationIndicator.hashCode());
		result = prime * result + ((moduleId == null) ? 0 : moduleId.hashCode());
		result = prime * result + ((relationships == null) ? 0 : relationships.hashCode());
		result = prime * result + (released ? 1231 : 1237);
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
		ConceptPojo other = (ConceptPojo) obj;
		if (active != other.active)
			return false;
		if (associationTargets == null) {
			if (other.associationTargets != null)
				return false;
		} else if (!associationTargets.equals(other.associationTargets))
			return false;
		if (conceptId == null) {
			if (other.conceptId != null)
				return false;
		} else if (!conceptId.equals(other.conceptId))
			return false;
		if (definitionStatus != other.definitionStatus)
			return false;
		if (descriptions == null) {
			if (other.descriptions != null)
				return false;
		} else if (!descriptions.equals(other.descriptions))
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
		if (moduleId == null) {
			if (other.moduleId != null)
				return false;
		} else if (!moduleId.equals(other.moduleId))
			return false;
		if (relationships == null) {
			if (other.relationships != null)
				return false;
		} else if (!relationships.equals(other.relationships))
			return false;
		if (released != other.released)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "ConceptPojo{" +
				"conceptId='" + conceptId + '\'' +
				", effectiveTime='" + effectiveTime + '\'' +
				", released=" + released +
				", active=" + active +
				", moduleId='" + moduleId + '\'' +
				", definitionStatus=" + definitionStatus +
				", inactivationIndicator='" + inactivationIndicator + '\'' +
				", associationTargets=" + associationTargets +
				", descriptions=" + descriptions +
				", relationships=" + relationships +
				'}';
	}
}
