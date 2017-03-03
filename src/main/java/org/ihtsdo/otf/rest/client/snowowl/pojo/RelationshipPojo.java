package org.ihtsdo.otf.rest.client.snowowl.pojo;

public class RelationshipPojo {

	private String relationshipId;

	private boolean active;

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
}
