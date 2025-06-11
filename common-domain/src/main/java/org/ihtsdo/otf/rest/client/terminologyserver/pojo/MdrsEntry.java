package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

//id	effectiveTime	active	moduleId	refsetId	referencedComponentId	sourceEffectiveTime	targetEffectiveTime
public class MdrsEntry extends RefsetMember {

	private static final Logger LOGGER = LoggerFactory.getLogger(MdrsEntry.class);

	//The target module id is held as the referenced component id
	public static final String SOURCE_EFFECTIVE_TIME = "sourceEffectiveTime";
	public static final String TARGET_EFFECTIVE_TIME = "targetEffectiveTime";

	protected static String[] additionalFieldNames = new String[] {
			SOURCE_EFFECTIVE_TIME,
			TARGET_EFFECTIVE_TIME
	};

	public MdrsEntry clone(boolean keepIds) {
		MdrsEntry clone = new MdrsEntry();
		if (keepIds) {
			clone.id = this.getId();
		} else {
			clone.id = UUID.randomUUID().toString();
		}
		clone.effectiveTime = null;
		clone.moduleId = this.moduleId;
		clone.active = this.active;
		clone.refsetId = SCTID_MODULE_DEPENDENCY_REFSET;
		clone.referencedComponentId = this.referencedComponentId;
		clone.setTargetEffectiveTime(this.getEffectiveTime());
		clone.setSourceEffectiveTime(this.getSourceEffectiveTime());
		clone.isDirty = true; //New components need to be written to any delta
		clone.released = this.released;
		return clone;
	}

	@Override
	public String toString() {
		String activeIndicator = isActiveSafely()?"":"*";
		return "[" + activeIndicator + "MDRS]" + id + " " + refsetId + "( " + getSourceEffectiveTime() + ") --> " + referencedComponentId + "( " + getTargetEffectiveTime() + ")";
	}

	public static MdrsEntry fromRf2(String[] lineItems) {
		MdrsEntry m = new MdrsEntry();
		m.setId(lineItems[MDRS_IDX_ID]);
		m.setEffectiveTime(lineItems[MDRS_IDX_EFFECTIVETIME]);
		m.setActive(lineItems[MDRS_IDX_ACTIVE].equals("1"));
		m.setModuleId(lineItems[MDRS_IDX_MODULEID]);
		m.setRefsetId(lineItems[MDRS_IDX_REFSETID]);
		m.setReferencedComponentId(lineItems[MDRS_IDX_REFCOMPID]);
		if (lineItems.length <= MDRS_IDX_TARGET_EFFECTIVE_TIME) {
			LOGGER.warn("MDRS {} is missing target effective time", lineItems[MDRS_IDX_ID]);
		} else {
			m.setSourceEffectiveTime(lineItems[MDRS_IDX_SOURCE_EFFECTIVE_TIME]);
			m.setTargetEffectiveTime(lineItems[MDRS_IDX_TARGET_EFFECTIVE_TIME]);
		}
		return m;
	}

	@Override
	public String[] toRF2() {
		return new String[] { id,
				(effectiveTime==null?"":effectiveTime),
				(isActiveSafely()?"1":"0"),
				moduleId, refsetId,
				referencedComponentId,
				getSourceEffectiveTime(),
				getTargetEffectiveTime()
		};
	}

	@Override
	public String[] toRF2Deletion() {
		return new String[] { id,
				(effectiveTime==null?"":effectiveTime),
				deletionEffectiveTime,
				(isActiveSafely()?"1":"0"),
				"1",
				moduleId, refsetId,
				referencedComponentId,
				getSourceEffectiveTime(),
				getTargetEffectiveTime()
		};
	}

	public String getTargetEffectiveTime() {
		return getField(TARGET_EFFECTIVE_TIME);
	}
	public void setTargetEffectiveTime(String targetEffectiveTime) {
		setField(TARGET_EFFECTIVE_TIME, targetEffectiveTime);
	}

	public String getSourceEffectiveTime() {
		return getField(SOURCE_EFFECTIVE_TIME);
	}

	public void setSourceEffectiveTime(String sourceEffectiveTime) {
		setField(SOURCE_EFFECTIVE_TIME, sourceEffectiveTime);
	}

	@Override
	public ComponentType getComponentType() {
		return ComponentType.MDRS_REFSET_MEMBER;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof MdrsEntry mdrsEntry) {
			return this.getId().equals(mdrsEntry.getId());
		}
		return false;
	}

	//Note that because Java does not support polymorphism of variables, only methods,
	//we need to call this method to pick up the field names of descendant types.
	@Override
	public String[] getAdditionalFieldNames() {
		return additionalFieldNames;
	}

	public static MdrsEntry create(IConcept sourceModule, IConcept targetModule) {
		MdrsEntry a = new MdrsEntry();
		a.setId(UUID.randomUUID().toString());
		a.setModuleId(sourceModule.getConceptId());
		a.setRefsetId(SCTID_MODULE_DEPENDENCY_REFSET);
		a.setReferencedComponentId(targetModule.getConceptId());
		a.setActive(true);
		a.setDirty();
		return a;
	}

}
