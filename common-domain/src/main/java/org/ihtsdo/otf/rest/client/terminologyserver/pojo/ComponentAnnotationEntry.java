package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.TermServerScriptException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//id	effectiveTime	active	moduleId	refsetId	referencedComponentId	acceptabilityId
public class ComponentAnnotationEntry extends RefsetMember implements RF2Constants {

	public static final String LANGUAGE_DIALECT_CODE = "languageDialectCode";

	//Adding these redundant fields, so that we serialize/deserialize correctly
	//Alternative is to add a custom (de)serializer
	@Expose
	@SerializedName("typeId")
	private String typeId;

	@Expose
	@SerializedName("value")
	private String value;

	@Expose
	@SerializedName("annotationId")
	private String annotationId;

	@Expose
	@SerializedName("languageDialectCode")
	private String languageDialectCode;

	public String getLanguageDialectCode() {
		return getField(LANGUAGE_DIALECT_CODE);
	}

	public void setLanguageDialectCode(String languageDialectCode) {
		setField(LANGUAGE_DIALECT_CODE, languageDialectCode);
	}

	public String getTypeId() {
		return getField(COL_TYPE_ID);
	}

	public void setTypeId(String typeId) {
		this.typeId = typeId;
		setField(COL_TYPE_ID, typeId);
	}

	public String getValue() {
		return getField(COL_VALUE);
	}

	public void setValue(String value) {
		this.value = value;
		setField(COL_VALUE, value);
	}

	public ComponentAnnotationEntry clone(String descriptionSctId, boolean keepIds) {
		ComponentAnnotationEntry clone = new ComponentAnnotationEntry();
		clone.id = keepIds ? this.id : UUID.randomUUID().toString();
		clone.effectiveTime = keepIds ? this.effectiveTime :null;
		clone.moduleId = this.moduleId;
		clone.active = this.active;
		clone.refsetId = this.refsetId;
		clone.referencedComponentId = descriptionSctId;
		clone.setLanguageDialectCode(getLanguageDialectCode());
		clone.setTypeId(getTypeId());
		clone.setValue(getValue());
		clone.setDirty(); //New components need to be written to any delta
		clone.released = this.released;
		return clone;
	}

	@Override
	public String[] toRF2() {
		return new String[] { id, 
				(effectiveTime==null?"":effectiveTime), 
				(isActiveSafely()?"1":"0"),
				moduleId, refsetId,
				referencedComponentId,
				getLanguageDialectCode(),
				getTypeId(),
				getValue()
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
				getLanguageDialectCode(),
				getTypeId(),
				getValue()
		};
	}

	public static ComponentAnnotationEntry fromRf2 (String[] lineItems) {
		ComponentAnnotationEntry a = new ComponentAnnotationEntry();
		a.setId(lineItems[COMP_ANNOT_IDX_ID]);
		a.setEffectiveTime(lineItems[COMP_ANNOT_IDX_EFFECTIVETIME]);
		a.setActive(lineItems[COMP_ANNOT_IDX_ACTIVE].equals("1"));
		a.setModuleId(lineItems[COMP_ANNOT_IDX_MODULEID]);
		a.setRefsetId(lineItems[COMP_ANNOT_IDX_REFSETID]);
		a.setReferencedComponentId(lineItems[COMP_ANNOT_IDX_REFCOMPID]);
		a.setLanguageDialectCode(lineItems[COMP_ANNOT_IDX_LANG_DIALECT_CODE]);
		a.setTypeId(lineItems[COMP_ANNOT_IDX_TYPEID]);
		a.setValue(lineItems[COMP_ANNOT_IDX_VALUE]);
		a.setClean();
		return a;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other == this) {
			return true;
		}
		if (!(other instanceof ComponentAnnotationEntry)) {
			return false;
		}
		ComponentAnnotationEntry rhs = ((ComponentAnnotationEntry) other);
		//If both sides have an SCTID, then compare that
		if (this.getId() != null && rhs.getId() != null) {
			return this.getId().equals(rhs.getId());
		}
		//TO DO Otherwise compare contents
		return false;
	}
	
	@Override
	public String toString() {
		return toString(false);
	}

	@Override
	public String toString(boolean detail) {
		try {
			String activeIndicator = isActiveSafely()?"":"*";
			String etStr = effectiveTime == null ? "N/A":effectiveTime;
			
		return "[" + activeIndicator + "AN]:" + id + " - " +
				"[ SCTID=" + getReferencedComponentId() + ", type=" +
				getTypeId() +
				", value '" + getValue() + "'" +
				(detail? ", Lang: " + getLanguageDialectCode() : "" ) +
				(detail? ", ET: '" + etStr : "'" ) +
				(detail? ", Module: " + moduleId : "" ) +
				" ]";
		} catch (Exception e) {
			return e.getMessage();
		}
	}

	@Override
	public ComponentType getComponentType() {
		return ComponentType.COMPONENT_ANNOTATION;
	}
	
	@Override
	public List<String> fieldComparison(Component other, boolean ignoreEffectiveTime) throws TermServerScriptException {
		ComponentAnnotationEntry otherL;
		if (other instanceof ComponentAnnotationEntry) {
			otherL = (ComponentAnnotationEntry)other;
		} else if (other instanceof RefsetMember) {
			try {
				otherL = ComponentAnnotationEntry.fromRf2(other.toRF2());
			} catch (Exception e) {
				throw new TermServerScriptException(e);
			}
		} else {
			throw new IllegalArgumentException("Unable to compare Component Annotation to " + other);
		}
		List<String> differences = new ArrayList<>();
		commonFieldComparison(otherL, differences);
		
		if (!this.getTypeId().equals(otherL.getTypeId())) {
			differences.add("Component Annotation is different in type : " + this.getTypeId() + " vs " + otherL.getTypeId());
		}
		//TO DO Also compare language and value
		return differences;
	}

	public static ComponentAnnotationEntry withDefaults(Component c, Component annotationType, String annotationStr) {
		ComponentAnnotationEntry entry = new ComponentAnnotationEntry();
		entry.id = UUID.randomUUID().toString();
		entry.effectiveTime = null;
		entry.active = true;
		entry.refsetId = SCTID_COMP_ANNOT_REFSET;
		entry.referencedComponentId = c.getId();
		entry.moduleId = SCTID_CORE_MODULE;
		entry.setLanguageDialectCode("en");
		entry.setTypeId(annotationType.getId());
		entry.setValue(annotationStr);
		entry.setDirty();
		return entry;
	}

	public String toStringWithModule() {
		return "[" + moduleId + "] : " + toString();
	}

}
