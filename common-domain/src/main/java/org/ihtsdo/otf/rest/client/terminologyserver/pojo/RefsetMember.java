package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.*;

import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.NotImplementedException;
import org.ihtsdo.otf.exception.TermServerScriptException;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RefsetMember extends Component implements RF2Constants {

	private static final Logger LOGGER = LoggerFactory.getLogger(RefsetMember.class);
	
	@SerializedName(value = "memberId", alternate = {"id"})
	@Expose
	protected String memberId;
	@SerializedName("refsetId")
	@Expose
	protected String refsetId;
	@SerializedName("referencedComponentId")
	@Expose
	protected String referencedComponentId;
	@SerializedName("additionalFields")
	@Expose
	protected Map<String, String> additionalFields = new HashMap<>();
	@SerializedName("referencedComponent")
	@Expose
	protected Object referencedComponent;
	
	protected String deletionEffectiveTime;
	
	protected boolean isDeleted = false;
	
	private static boolean firstFieldNamesWarningGiven = false;

	static void setFirstFieldNamesWarningGiven() {
		firstFieldNamesWarningGiven = true;
	}
	
	public RefsetMember() {}

	public RefsetMember(String id) {
		setId(id);
	}
	
	public String getMemberId() {
		//We get into a pickle here because the fieldname for the id changes from component to component when we're working with the API
		if (memberId == null && id != null) {
			return id;
		}
		return memberId;
	}

	@Override
	public String getId() {
		if (id == null) {
			return memberId;
		}
		return id;
	}

	public void setMemberId(String memberId) {
		this.memberId = memberId;
		this.id = memberId;
	}

	public RefsetMember(String refsetId, Component referencedCompoment) {
		this.refsetId = refsetId;
		this.referencedComponent = referencedCompoment;
	}
	
	public boolean isDeleted() {
		return isDeleted;
	}

	public String getRefsetId() {
		return refsetId;
	}

	public void setRefsetId(String refsetId) {
		this.refsetId = refsetId;
	}

	public String getReferencedComponentId() {
		return referencedComponentId;
	}

	public void setReferencedComponentId(String referencedComponentId) {
		this.referencedComponentId = referencedComponentId;
	}

	public Map<String, String> getAdditionalFields() {
		return additionalFields;
	}

	public void setAdditionalFields(Map<String, String> additionalFields) {
		this.additionalFields = additionalFields;
	}
	
	public String getField(String key) {
		return this.additionalFields.get(key);
	}
	
	public void setField(String key, String value) {
		this.additionalFields.put(key, value);
	}

	@Override
	public String getReportedName() {
		throw new NotImplementedException();
	}

	@Override
	public String getReportedType() {
		return getComponentType().toString();
	}

	@Override
	public ComponentType getComponentType() {
		if (additionalFields.containsKey("targetComponentId")) {
			return ComponentType.HISTORICAL_ASSOCIATION;
		} else if (additionalFields.containsKey("acceptabilityId")) {
			return ComponentType.LANGREFSET;
		} else if (additionalFields.containsKey(ComponentAnnotationEntry.LANGUAGE_DIALECT_CODE)) {
			return ComponentType.COMPONENT_ANNOTATION;
		} else if (additionalFields.containsKey("valueId")) {
			return ComponentType.ATTRIBUTE_VALUE;
		} else if (additionalFields.isEmpty()) {
			return ComponentType.SIMPLE_REFSET_MEMBER;
		}
		return ComponentType.UNKNOWN;
	}

	@Override
	public List<String>
	fieldComparison(Component other, boolean ignoreEffectiveTime) throws TermServerScriptException {
		//TODO Add generic field comparison based off field names
		throw new NotImplementedException();
	}
	
	public void delete (String deletionEffectiveTime) {
		this.deletionEffectiveTime = deletionEffectiveTime;
		this.isDeleted = true;
	}
	
	public String[] toRF2Deletion() {
		throw new NotImplementedException();
	}

	@Override
	public String[] toRF2() {
		return toRF2(getAdditionalFieldNames());
	}
	
	public String[] toRF2(String[] additionalFieldNames) {
		//We're OK to output to RF2 without knowing exactly what this is as long as we have < 2
		//fields to output.  Otherwise we need to know the field names
		if (additionalFields.size() != additionalFieldNames.length) {
			//If additionalFields is just 1 then the order doens't matter, so we can warn and continue
			if (additionalFields.size() == 1) {
				additionalFieldNames = additionalFields.keySet().toArray(String[]::new);
				if (!firstFieldNamesWarningGiven) {
					LOGGER.warn("toRF2() called without fieldName order being known, but only 1 field so doesn't matter");
					setFirstFieldNamesWarningGiven();
				}
			} else {
				throw new IllegalArgumentException("Additional field names supplied do not match data.");
			}
		}
		
		String[] row = new String[6 + additionalFieldNames.length];
		int col = 0;
		row[col++] = getId();
		row[col++] = effectiveTime==null?"":effectiveTime;
		row[col++] = isActiveSafely()?"1":"0";
		row[col++] = moduleId;
		row[col++] = refsetId;
		row[col++] = referencedComponentId;
		
		for (String additionalFieldName : additionalFieldNames) {
			row[col++] = getField(additionalFieldName);
		}
		return row;
	}
	
	public static void populatefromRf2(RefsetMember m, String[] lineItems, String[] additionalFieldNames) throws TermServerScriptException {
		m.setId(lineItems[REF_IDX_ID]);
		m.setMemberId(lineItems[REF_IDX_ID]);
		m.setEffectiveTime(lineItems[REF_IDX_EFFECTIVETIME]);
		m.setActive(lineItems[REF_IDX_ACTIVE].equals("1"));
		m.setModuleId(lineItems[REF_IDX_MODULEID]);
		m.setRefsetId(lineItems[REF_IDX_REFSETID]);
		m.setReferencedComponentId(lineItems[REF_IDX_REFCOMPID]);
		for (int i=0; i < additionalFieldNames.length; i++) {
			int idx = i + REF_IDX_FIRST_ADDITIONAL;
			if (lineItems.length < idx) {
				String objectName = m.getClass().getSimpleName();
				throw new TermServerScriptException(objectName + " " + m.getId() + " expected " + (idx+1) + " columns in RF2, but only contained " + lineItems.length);
			}
			m.setField(additionalFieldNames[i], lineItems[idx]);
		}
	}
	
	public String[] getAdditionalFieldsArray() {
		String[] fields = new String[getAdditionalFieldNames().length];
		int col = 0;
		for (String additionalFieldName : getAdditionalFieldNames()) {
			fields[col++] = getField(additionalFieldName);
		}
		return fields;
	}
	
	//Note that because Java does not support polymorphism of variables, only methods,
	//we need to call this method to pick up the field names of descendant types.
	public String[] getAdditionalFieldNames() {
		return new String[0];
	}
	
	@Override 
	public boolean equals(Object o) {
		if (o instanceof RefsetMember otherRefsetMember) {
			return this.getId().equals(otherRefsetMember.getId());
		}
		return false;
	}
	
	public String toString() {
		return toString(false);
	}
	
	public String toString(boolean includeModuleId) {
		StringBuilder additionalStr = new StringBuilder();
		boolean isFirst = true;
		for (Map.Entry<String, String> entry : additionalFields.entrySet()) {
			additionalStr.append(isFirst ? "" : ", ");
			additionalStr.append(entry.getKey())
					.append("=")
					.append(entry.getValue());
			isFirst = false;
		}
		String activeIndicator = isActiveSafely()?"":"*";
		String arrow = additionalStr.isEmpty() ? "" : " -> ";
		return "[" + activeIndicator + "RM]:" + getId() + " - " + refsetId + " : " + referencedComponentId + arrow + additionalStr + (includeModuleId ? " (Module: " + this.getModuleId() + ")" : "");
	}

	/**
	 * @return true if both refset members have the same refsetId, referencedComponentId and additionalFields
	 */
	public boolean duplicates(RefsetMember that) {
		if (this.getRefsetId().equals(that.getRefsetId()) &&
				this.getReferencedComponentId().equals(that.getReferencedComponentId())) {
			return matchesAdditionalFields(that);
		}
		return false;
	}

	private boolean matchesAdditionalFields(RefsetMember that) {
		return this.getAdditionalFields().equals(that.getAdditionalFields());
	}

	public boolean hasAdditionalField(String key) {
		return getAdditionalFields().containsKey(key);
	}

	public String getOnlyAdditionalFieldName() {
		if (additionalFields.size() != 1) {
			throw new IllegalArgumentException(this + " did not return only one additional field");
		}
		return additionalFields.keySet().iterator().next();
	}

	@Override
	public String[] getMutableFields() {
 		String[] mutableFields = super.getMutableFields();
		int idx = super.getMutableFieldCount();
		mutableFields[idx] = refsetId;
		mutableFields[++idx] = referencedComponentId;
		++idx;
		for (int additionalFieldIdx = 0; additionalFieldIdx < additionalFields.size(); additionalFieldIdx++) {
			mutableFields[idx + additionalFieldIdx] = getField(getAdditionalFieldNames()[additionalFieldIdx]);
		}
		return mutableFields;
	}

	@Override
	public int getMutableFieldCount() {
		return super.getMutableFieldCount() + 2 + additionalFields.size();
	}

	@Override
	public String toStringWithId() {
		return getId() + ": " + toString();
	}

	@Override
	public boolean matchesMutableFields(Component other) {
		RefsetMember otherRM = (RefsetMember)other;
		return this.getAdditionalFields().equals(otherRM.getAdditionalFields());
	}

	@Override
	public List<Component> getReferencedComponents(ComponentStore cs) {
		List<Component> referencedComponents = new ArrayList<>();
		if (this.referencedComponent instanceof Component c) {
			referencedComponents.add(c);
		} else {
			referencedComponents.add(cs.getComponent(this.referencedComponentId));
		}

		//Now work through any additional fields that may reference components
		for (String additionalFieldName : getAdditionalFieldNames()) {
			if (this.hasAdditionalField(additionalFieldName)) {
				String additionalFieldValue = this.getField(additionalFieldName);
				if (cs.isComponentId(additionalFieldValue)) {
					referencedComponents.add(cs.getComponent(additionalFieldValue));
				}
			}
		}
		return referencedComponents;
	}

	protected RefsetMember populateClone(RefsetMember clone, String newComponentSctId, boolean keepIds) {
		clone.id = keepIds ? this.getId() : UUID.randomUUID().toString();
		clone.effectiveTime = null;
		clone.moduleId = this.moduleId;
		clone.active = this.active;
		clone.refsetId = this.refsetId;
		clone.referencedComponentId = newComponentSctId;
		clone.isDirty = true; //New components need to be written to any delta
		clone.released = this.released;
		clone.setAdditionalFields(new HashMap<>(this.additionalFields));
		return clone;
	}

	public void setAdditionalValues(String[] additionalValues) {
		for (int i=0; i < additionalValues.length; i++) {
			setField(getAdditionalFieldNames()[i], additionalValues[i]);
		}
	}
}
