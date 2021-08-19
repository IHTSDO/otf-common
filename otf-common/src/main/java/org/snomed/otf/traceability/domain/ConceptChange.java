package org.snomed.otf.traceability.domain;

import java.util.HashSet;
import java.util.Set;

public class ConceptChange {
	private Long conceptId;
	private final Set<ComponentChange> componentChanges = new HashSet<>();

	public ConceptChange() {
	}

	public ConceptChange(Long conceptId, Set<ComponentChange> componentChanges) {
		this.conceptId = conceptId;
		if (componentChanges != null) {
			this.componentChanges.addAll(componentChanges);
		}
	}

	public Long getConceptId() {
		return conceptId;
	}

	public void setConceptId(Object conceptId) {
		if (conceptId instanceof Double) {
			Double asDouble = (Double) conceptId;
			this.conceptId = asDouble.longValue();
		} else if (conceptId instanceof Long) {
			Long asLong = (Long) conceptId;
			this.conceptId = asLong;
		}
	}

	public Set<ComponentChange> getComponentChanges() {
		return componentChanges;
	}

	public void setComponentChanges(Set<ComponentChange> componentChanges) {
		if (componentChanges != null) {
			this.componentChanges.addAll(componentChanges);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ConceptChange that = (ConceptChange) o;

		if (conceptId != null ? !conceptId.equals(that.conceptId) : that.conceptId != null) return false;
		return componentChanges != null ? componentChanges.equals(that.componentChanges) : that.componentChanges == null;

	}

	@Override
	public int hashCode() {
		int result = conceptId != null ? conceptId.hashCode() : 0;
		result = 31 * result + (componentChanges != null ? componentChanges.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ConceptChange{" +
				"conceptId=" + conceptId +
				", componentChanges=" + componentChanges +
				'}';
	}
}
