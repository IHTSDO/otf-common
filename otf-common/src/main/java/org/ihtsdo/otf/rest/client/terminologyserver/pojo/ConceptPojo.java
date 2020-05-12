package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"conceptId", "effectiveTime", "active", "released", "inactivationIndicator", "moduleId", "definitionStatus",
		"descriptions", "classAxioms", "gciAxioms", "relationships"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConceptPojo implements SnomedComponent {

	private String conceptId;

	private String effectiveTime;
	
	private boolean released;

	private boolean active;
	
	private String moduleId;

	private DefinitionStatus definitionStatus;

	private String inactivationIndicator;

	private Map<String, String[]> associationTargets;

	private Set<DescriptionPojo> descriptions;
	
	private Set<AxiomPojo> classAxioms;
	
	private Set<AxiomPojo> gciAxioms;

	private Set<RelationshipPojo> relationships;
	
	public ConceptPojo() {
		active = true;
	}

	@Override
	public String getId() {
		return conceptId;
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

	public void addClassAxiom(AxiomPojo axiomPojo) {
		if (classAxioms == null) {
			classAxioms = new HashSet<>();
		}
		classAxioms.add(axiomPojo);
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

	public Set<AxiomPojo> getClassAxioms() {
		return classAxioms;
	}

	public void setClassAxioms(Set<AxiomPojo> classAxioms) {
		this.classAxioms = classAxioms;
	}

	public Set<AxiomPojo> getGciAxioms() {
		return gciAxioms;
	}

	public void setGciAxioms(Set<AxiomPojo> gciAxioms) {
		this.gciAxioms = gciAxioms;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ConceptPojo that = (ConceptPojo) o;
		return released == that.released &&
				active == that.active &&
				Objects.equals(conceptId, that.conceptId) &&
				Objects.equals(effectiveTime, that.effectiveTime) &&
				Objects.equals(moduleId, that.moduleId) &&
				definitionStatus == that.definitionStatus &&
				Objects.equals(inactivationIndicator, that.inactivationIndicator) &&
				Objects.equals(associationTargets, that.associationTargets) &&
				Objects.equals(descriptions, that.descriptions) &&
				Objects.equals(classAxioms, that.classAxioms) &&
				Objects.equals(gciAxioms, that.gciAxioms) &&
				Objects.equals(relationships, that.relationships);
	}

	@Override
	public int hashCode() {

		return Objects.hash(conceptId, effectiveTime, released, active, moduleId, definitionStatus, inactivationIndicator, associationTargets, descriptions, classAxioms, gciAxioms, relationships);
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
				", classAxioms=" + classAxioms +
				", gciAxioms=" + gciAxioms +
				", relationships=" + relationships +
				'}';
	}
}
