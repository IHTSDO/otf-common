package org.ihtsdo.otf.rest.client.snowowl.pojo;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonPropertyOrder({"conceptId", "effectiveTime", "active", "inactivationIndicator", "moduleId", "definitionStatus", "descriptions", "relationships"})
public class ConceptPojo {

	private String conceptId;

	private String effectiveTime;

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
}
