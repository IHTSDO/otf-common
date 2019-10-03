package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationshipPojo {

	private String relationshipId;

	private boolean active;
	
	private String effectiveTime;
	
	private boolean released;

	private String moduleId;

	private String sourceId;

	private ConceptMiniPojo target;

	private int groupId;

	private ConceptMiniPojo type;

	private String characteristicType;

	private String modifier;

	public RelationshipPojo() {
		active = true;
	}

	public RelationshipPojo(int groupId, String typeId, String targetId, String characteristicType) {
		this();
		this.groupId = groupId;
		this.type = new ConceptMiniPojo(typeId);
		this.target = new ConceptMiniPojo(targetId);
		this.characteristicType = characteristicType;
	}

	public String getRelationshipId() {
		return relationshipId;
	}

	public void setRelationshipId(String relationshipId) {
		this.relationshipId = relationshipId;
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

	public String getModuleId() {
		return moduleId;
	}

	public void setModuleId(String moduleId) {
		this.moduleId = moduleId;
	}

	public String getSourceId() {
		return sourceId;
	}

	public void setSourceId(String sourceId) {
		this.sourceId = sourceId;
	}

	public ConceptMiniPojo getTarget() {
		return target;
	}

	public void setTarget(ConceptMiniPojo target) {
		this.target = target;
	}

	public ConceptMiniPojo getType() {
		return type;
	}

	public void setType(ConceptMiniPojo type) {
		this.type = type;
	}

	public int getGroupId() {
		return groupId;
	}

	public void setGroupId(int groupId) {
		this.groupId = groupId;
	}

	public String getCharacteristicType() {
		return characteristicType;
	}

	public void setCharacteristicType(String characteristicType) {
		this.characteristicType = characteristicType;
	}

	public String getModifier() {
		return modifier;
	}

	public void setModifier(String modifier) {
		this.modifier = modifier;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (active ? 1231 : 1237);
		result = prime * result + ((characteristicType == null) ? 0 : characteristicType.hashCode());
		result = prime * result + groupId;
		result = prime * result + ((moduleId == null) ? 0 : moduleId.hashCode());
		result = prime * result + ((relationshipId == null) ? 0 : relationshipId.hashCode());
		result = prime * result + ((sourceId == null) ? 0 : sourceId.hashCode());
		result = prime * result + ((target == null) ? 0 : target.hashCode());
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
		RelationshipPojo other = (RelationshipPojo) obj;
		if (active != other.active)
			return false;
		if (characteristicType == null) {
			if (other.characteristicType != null)
				return false;
		} else if (!characteristicType.equals(other.characteristicType))
			return false;
		if (groupId != other.groupId)
			return false;
		if (moduleId == null) {
			if (other.moduleId != null)
				return false;
		} else if (!moduleId.equals(other.moduleId))
			return false;
		if (relationshipId == null) {
			if (other.relationshipId != null)
				return false;
		} else if (!relationshipId.equals(other.relationshipId))
			return false;
		if (sourceId == null) {
			if (other.sourceId != null)
				return false;
		} else if (!sourceId.equals(other.sourceId))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		} else if (!target.equals(other.target))
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
		return "RelationshipPojo{" +
				"relationshipId='" + relationshipId + '\'' +
				", active=" + active +
				", effectiveTime='" + effectiveTime + '\'' +
				", released=" + released +
				", moduleId='" + moduleId + '\'' +
				", sourceId='" + sourceId + '\'' +
				", target=" + target +
				", groupId=" + groupId +
				", type=" + type +
				", characteristicType='" + characteristicType + '\'' +
				", modifier='" + modifier + '\'' +
				'}';
	}
}
