package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.google.common.collect.Sets;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AxiomPojo implements SnomedComponent {
	
	private String axiomId;

	private String conceptId;

	private String moduleId;
	
	private boolean active;
	
	private boolean released;

	private String owlExpression;

	private Boolean gci;

	private String definitionStatusId;
	
	private Set<RelationshipPojo> relationships;
	
	private String effectiveTime;

	public AxiomPojo() {
		relationships = new HashSet<>();
	}

	public AxiomPojo(RelationshipPojo... relationships) {
		this.relationships = new HashSet<>(Arrays.asList(relationships));
	}

	public void add(RelationshipPojo relationshipPojo) {
		relationships.add(relationshipPojo);
	}

	@Override
	public String getId() {
		return getAxiomId();
	}

	public String getAxiomId() {
		return axiomId;
	}

	public void setAxiomId(String axiomId) {
		this.axiomId = axiomId;
	}

	@Override
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

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public boolean isReleased() {
		return released;
	}

	public void setReleased(boolean released) {
		this.released = released;
	}

	public String getOwlExpression() {
		return owlExpression;
	}

	public void setOwlExpression(String owlExpression) {
		this.owlExpression = owlExpression;
	}

	public Boolean getGci() {
		return gci;
	}

	public void setGci(Boolean gci) {
		this.gci = gci;
	}

	public String getDefinitionStatusId() {
		return definitionStatusId;
	}

	public void setDefinitionStatusId(String definitionStatusId) {
		this.definitionStatusId = definitionStatusId;
	}

	public Set<RelationshipPojo> getRelationships() {
		return relationships;
	}

	public void setRelationships(Set<RelationshipPojo> relationships) {
		this.relationships = relationships;
	}

	public String getEffectiveTime() {
		return effectiveTime;
	}

	public void setEffectiveTime(String effectiveTime) {
		this.effectiveTime = effectiveTime;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		AxiomPojo axiomPojo = (AxiomPojo) o;
		return active == axiomPojo.active &&
				released == axiomPojo.released &&
				Objects.equals(axiomId, axiomPojo.axiomId) &&
				Objects.equals(conceptId, axiomPojo.conceptId) &&
				Objects.equals(moduleId, axiomPojo.moduleId) &&
				Objects.equals(owlExpression, axiomPojo.owlExpression) &&
				Objects.equals(definitionStatusId, axiomPojo.definitionStatusId) &&
				Objects.equals(relationships, axiomPojo.relationships) &&
				Objects.equals(effectiveTime, axiomPojo.effectiveTime);
	}

	@Override
	public int hashCode() {
		return Objects.hash(axiomId, conceptId, moduleId, active, released, owlExpression, definitionStatusId, relationships, effectiveTime);
	}
}
