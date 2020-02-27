package org.snomed.otf.traceability.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
public class ComponentChange {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	@JsonIgnore
	private Long id;

	@ManyToOne
	@JoinColumn(name = "concept_change_id")
	@JsonIgnore
	private ConceptChange conceptChange;

	private String componentId;

	@Enumerated
	private ComponentType componentType;

	@Enumerated
	private ComponentSubType componentSubType;

	@Enumerated
	private ComponentChangeType changeType;

	public ComponentChange() {
	}

	public ComponentChange(String componentId, ComponentChangeType changeType, ComponentType componentType, ComponentSubType componentSubType) {
		this.componentId = componentId;
		this.changeType = changeType;
		this.componentType = componentType;
		this.componentSubType = componentSubType;
	}

	public void setConceptChange(ConceptChange conceptChange) {
		this.conceptChange = conceptChange;
	}

	public String getComponentId() {
		return componentId;
	}

	public ComponentChangeType getChangeType() {
		return changeType;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public ComponentSubType getComponentSubType() {
		return componentSubType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ComponentChange that = (ComponentChange) o;

		if (componentId != null ? !componentId.equals(that.componentId) : that.componentId != null) return false;
		if (componentType != that.componentType) return false;
		if (componentSubType != that.componentSubType) return false;
		return changeType == that.changeType;

	}

	@Override
	public int hashCode() {
		int result = componentId != null ? componentId.hashCode() : 0;
		result = 31 * result + (componentType != null ? componentType.hashCode() : 0);
		result = 31 * result + (componentSubType != null ? componentSubType.hashCode() : 0);
		result = 31 * result + (changeType != null ? changeType.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ComponentChange{" +
				"id=" + id +
				", componentId='" + componentId + '\'' +
				", componentType=" + componentType +
				", componentSubType=" + componentSubType +
				", changeType=" + changeType +
				'}';
	}
}
