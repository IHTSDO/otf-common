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
public class ConceptPojo implements SnomedComponent, IConcept {

	public enum InactivationIndicator {

		DUPLICATE("900000000000482003"), OUTDATED("900000000000483008"), AMBIGUOUS("900000000000484002"), ERRONEOUS("900000000000485001"), LIMITED("900000000000486000"),
		MOVED_ELSEWHERE("900000000000487009"), PENDING_MOVE("900000000000492006"), INAPPROPRIATE("900000000000494007"), CONCEPT_NON_CURRENT("900000000000495008"),
		NONCONFORMANCE_TO_EDITORIAL_POLICY("723277005"), NOT_SEMANTICALLY_EQUIVALENT("723278000"), GRAMMATICAL_DESCRIPTION_ERROR("1217318005"), CLASSIFICATION_DERIVED_COMPONENT("1186917008"),
		MEANING_OF_COMPONENT_UNKNOWN("1186919006");

		private final String conceptId;

		InactivationIndicator(String conceptId) {
			this.conceptId = conceptId;
		}

		public static InactivationIndicator fromConceptId(String conceptId) {
			for (InactivationIndicator value : values()) {
				if (value.conceptId.equals(conceptId)) {
					return value;
				}
			}
			return null;
		}

		public String getConceptId() {
			return conceptId;
		}
	}

	public enum HistoricalAssociation {

		POSSIBLY_EQUIVALENT_TO("900000000000523009"), MOVED_TO("900000000000524003"), MOVED_FROM("900000000000525002"), REPLACED_BY("900000000000526001"), SAME_AS("900000000000527005"),
		WAS_A("900000000000528000"), SIMILAR_TO("900000000000529008"), ALTERNATIVE("900000000000530003"), REFERS_TO("900000000000531004"), PARTIALLY_EQUIVALENT_TO("1186924009"),
		POSSIBLY_REPLACED_BY("1186921001");

		private final String conceptId;

		HistoricalAssociation(String conceptId) {
			this.conceptId = conceptId;
		}

		public static HistoricalAssociation fromConceptId(String conceptId) {
			for (HistoricalAssociation value : values()) {
				if (value.conceptId.equals(conceptId)) {
					return value;
				}
			}
			return null;
		}


		public String getConceptId() {
			return conceptId;
		}
	}

	private String conceptId;

	private String effectiveTime;
	
	private boolean released;

	private boolean active;
	
	private String moduleId;

	private DefinitionStatus definitionStatus;

	private InactivationIndicator inactivationIndicator;

	private Map<HistoricalAssociation, Set<String>> associationTargets;

	private Set<DescriptionPojo> descriptions;

	private Set<AnnotationPojo> annotations;
	
	private Set<AxiomPojo> classAxioms;
	
	private Set<AxiomPojo> gciAxioms;

	private Set<RelationshipPojo> relationships;
	
	public ConceptPojo() {
		active = true;
	}

	public ConceptPojo(String conceptId) {
		this();
		this.conceptId = conceptId;
	}

	@Override
	public String getId() {
		return conceptId;
	}

	public String getFsn() {
		if (descriptions != null) {
			for (DescriptionPojo description : descriptions) {
				if (DescriptionPojo.Type.FSN.equals(description.getType()) && description.isActive()) {
					return description.getTerm();
				}
			}
		}
		return null;
	}

	public ConceptPojo add(RelationshipPojo relationship) {
		if (relationships == null) {
			relationships = new HashSet<>();
		}
		relationships.add(relationship);
		return this;
	}

	public ConceptPojo add(DescriptionPojo description) {
		if (descriptions == null) {
			descriptions = new HashSet<>();
		}
		descriptions.add(description);
		return this;
	}

	public ConceptPojo addClassAxiom(AxiomPojo axiomPojo) {
		if (classAxioms == null) {
			classAxioms = new HashSet<>();
		}
		classAxioms.add(axiomPojo);
		return this;
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

	public InactivationIndicator getInactivationIndicator() {
		return inactivationIndicator;
	}

	public void setInactivationIndicator(InactivationIndicator inactivationIndicator) {
		this.inactivationIndicator = inactivationIndicator;
	}

	public Map<HistoricalAssociation, Set<String>> getAssociationTargets() {
		return associationTargets;
	}

	public void setAssociationTargets(Map<HistoricalAssociation, Set<String>> associationTargets) {
		this.associationTargets = associationTargets;
	}

	public Set<DescriptionPojo> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(Set<DescriptionPojo> descriptions) {
		this.descriptions = descriptions;
	}

	public Set<AnnotationPojo> getAnnotations() {
		return annotations;
	}

	public void setAnnotations(Set<AnnotationPojo> annotations) {
		this.annotations = annotations;
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
		return getId() + "|" + getFsn() + "|";
	}

	public String toStringFull() {
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

	@Override
	public String getFsnTerm() {
		for (DescriptionPojo d : getDescriptions()) {
			if (d.isActive() && d.getLang().equals("en") && d.getType().equals(DescriptionPojo.Type.FSN)) {
				return d.getTerm();
			}
		}
		return "";
	}
}
