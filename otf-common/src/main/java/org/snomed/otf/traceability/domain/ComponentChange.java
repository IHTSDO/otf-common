package org.snomed.otf.traceability.domain;

import java.util.Objects;

public class ComponentChange {

	private String componentId;
	private ChangeType changeType;
	private ComponentType componentType;
	private Long componentSubType;
	private Boolean superseded;

	public ComponentChange() {
	}

	public ComponentChange(String componentId, ChangeType changeType, ComponentType componentType, Long componentSubType, Boolean superseded) {
		this.componentId = componentId;
		this.changeType = changeType;
		this.componentType = componentType;
		this.componentSubType = componentSubType;
		this.superseded = superseded;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public ChangeType getChangeType() {
		return changeType;
	}

	public void setChangeType(ChangeType changeType) {
		this.changeType = changeType;
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	public void setComponentType(ComponentType componentType) {
		this.componentType = componentType;
	}

	public Long getComponentSubType() {
		return componentSubType;
	}

	public void setComponentSubType(Long componentSubType) {
		this.componentSubType = componentSubType;
	}

	public Boolean superseded() {
		return superseded;
	}

	public void setSuperseded(Boolean superseded) {
		this.superseded = superseded;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		ComponentChange that = (ComponentChange) o;

		if (!Objects.equals(componentId, that.componentId)) return false;
		if (componentType != that.componentType) return false;
		if (!Objects.equals(componentSubType, that.componentSubType)) return false;
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
				"componentId='" + componentId + '\'' +
				", changeType=" + changeType +
				", componentType=" + componentType +
				", componentSubType=" + componentSubType +
				'}';
	}
}
