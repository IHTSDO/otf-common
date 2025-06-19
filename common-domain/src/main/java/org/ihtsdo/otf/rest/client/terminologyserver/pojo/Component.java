package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.ScriptException;
import org.ihtsdo.otf.utils.SnomedUtilsBase;
import org.ihtsdo.otf.exception.TermServerRuntimeException;
import org.ihtsdo.otf.exception.TermServerScriptException;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public abstract class Component implements RF2Constants {

	public enum ComponentType { CONCEPT, DESCRIPTION, STATED_RELATIONSHIP,
		INFERRED_RELATIONSHIP, ADDITIONAL_RELATIONSHIP, LANGREFSET, ATTRIBUTE_VALUE, HISTORICAL_ASSOCIATION,
		TEXT_DEFINITION, AXIOM, ALTERNATE_IDENTIFIER, COMPONENT_ANNOTATION, REFSET_MEMBER_ANNOTATION,
		SIMPLE_MAP, SIMPLE_REFSET_MEMBER, MDRS_REFSET_MEMBER, UNKNOWN}
	
	//The id takes a different name in most components, don't expose
	protected String id;
	protected ComponentType componentType;
	
	@SerializedName("effectiveTime")
	@Expose
	protected String effectiveTime;
	
	@SerializedName("moduleId")
	@Expose
	protected String moduleId;
	
	@SerializedName("active")
	@Expose
	protected Boolean active;
	
	@SerializedName("released")
	@Expose
	protected Boolean released;
	
	@SerializedName("releasedEffectiveTime")
	@Expose
	protected  Integer releasedEffectiveTime;

	@SerializedName("annotations")
	@Expose
	List<ComponentAnnotationEntry> componentAnnotationEntries;

	String [] previousState;
	
	//Generic debug string to say if concept should be highlighted for some reason, eg cause a template match to fail
	private List<String> issues;

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getEffectiveTime() {
		return effectiveTime;
	}
	
	public void setEffectiveTime(String effectiveTime) {
		if (this.effectiveTime != null && !this.effectiveTime.isEmpty() && effectiveTime == null) {
			//Are we resetting this component to mark a change?
			setDirty();
		}
		this.effectiveTime = effectiveTime;
	}
	
	public Boolean isActive() {
		if (active == null) {
			throw new IllegalStateException("Attempt to check active status on non-populated " + this.getClass().getSimpleName() + " component: " + getId());
		}
		return active;
	}

	public boolean isActiveSafely() {
		return active != null && active;
	}

	public Boolean getActive() {
		return active;
	}
	
	public String getModuleId() {
		return moduleId;
	}
	
	public void setModuleId(String moduleId) {
		if (this.moduleId != null && !this.moduleId.equals(moduleId)) {
			setDirty();
			this.effectiveTime = null;
		}
		this.moduleId = moduleId;
	}
	
	public abstract String getReportedName();
	
	public abstract String getReportedType();
	
	public ComponentType getComponentType() {
		return componentType;
	}
	
	public abstract String[] toRF2() throws ScriptException;
	
	protected boolean isDirty = false;
	
	public void addIssues(List<String> issues) {
		for (String issue : issues) {
			addIssue(issue);
		}
	}
	
	public void addIssue(String issue) {
		if (this.issues == null) {
			this.issues = new ArrayList<>();
		}
		this.issues.add(issue);
	}

	public void setIssues(List<String> issues) {
		clearIssues();
		if (issues != null) {
			for (String issue : issues) {
				addIssue(issue);
			}
		}
	}

	public boolean hasIssues() {
		return issues != null && !issues.isEmpty();
	}
	
	public boolean hasIssue(String targetIssue) {
		if (issues == null || issues.isEmpty()) {
			return false;
		}
		for (String issue : issues) {
			if (issue.contains(targetIssue)) {
				return true;
			}
		}
		return false;
	}

	public String getIssues() {
		return getIssues(", ");
	}

	public String getIssues(String delimiter) {
		return issues == null ? "" : issues.stream().collect(Collectors.joining(delimiter));
	}

	public List<String> getIssueList() {
		return issues == null ? new ArrayList<>() : issues;
	}

	public String[] getIssuesArray() {
		return issues.toArray(new String[] {});
	}

	public void clearIssues() {
		issues = null;
	}

	@Override
	public int hashCode() {
		return getIdOrThrow().hashCode();
	}

	private String getIdOrThrow() {
		if (getId() != null) {
			return getId();  //This might be coming from memberId if loaded directly from API
		} else {
			throw new TermServerRuntimeException("Attempt to hash/equal component with no id.  Use concrete class check of values instead");
		}
	}

	@Override
	public boolean equals(Object other) {
		return (other instanceof Component otherComponent)
		&& this.getIdOrThrow().equals(otherComponent.getId());
	}
	
	public List<String> fieldComparison(Component other) throws TermServerScriptException {
		return fieldComparison(other, false);
	}

	public abstract List<String> fieldComparison(Component other, boolean ignoreEffectiveTime) throws TermServerScriptException;
	
	protected void commonFieldComparison(Component other, List<String> differences) {
		commonFieldComparison(other, differences, false);
	}
	
	protected void commonFieldComparison(Component other, List<String> differences, boolean ignoreEffectiveTime) {
		String name = this.getClass().getSimpleName(); 
		
		if (!this.getId().equals(other.getId())) {
			differences.add("Id different in " + name + ": " + this.getId() + " vs " + other.getId());
		}
		
		if (!ignoreEffectiveTime
			&& ((this.getEffectiveTime() == null && other.getEffectiveTime() != null)
			|| 	(this.getEffectiveTime() != null && other.getEffectiveTime() == null)
			||	!this.getEffectiveTime().equals(other.getEffectiveTime()))) {
			differences.add("EffectiveTime different in " + name + ": " + this.getEffectiveTime() + " vs " + other.getEffectiveTime());
		}
		
		if (!this.isActiveSafely() == other.isActiveSafely()) {
			differences.add("Active status different in " + name + ": " + this.isActive() + " vs " + other.isActive());
		}
		
		if (!this.getModuleId().equals(other.getModuleId())) {
			differences.add("ModuleId different in " + name + ": " + this.getModuleId() + " vs " + other.getModuleId());
		}
	}

	public void setDirty() {
		this.isDirty = true;
	}
	
	public void setClean() {
		this.isDirty = false;
	}
	
	public boolean isDirty() {
		return this.isDirty;
	}

	/**
	 * @return an array of fields that can be compared to determine if this component has changed
	 * Note that the array will be sized according to the specific component type, ready for further
	 * population
	 */
	public String[] getMutableFields() {
		String[] mutableFields = new String[getMutableFieldCount()];
		mutableFields[0] = this.isActiveSafely()?"1":"0";
		mutableFields[1] = this.getModuleId();
		return mutableFields;
	}

	public int getMutableFieldCount() {
		return 2;
	}

	public String toStringWithId() {
		//Override is only needed if default implemenation does not include Id eg Descriptions or Refset Members.
		return toString();
	}
	
	public void setActive(boolean newActiveState) {
		setActive(newActiveState, false);
	}
	public void setActive(boolean newActiveState, boolean forceDirty) {
		if (forceDirty || (this.active != null && this.active != newActiveState)) {
			setDirty();
			setEffectiveTime(null);
		}
		this.active = newActiveState;
	}
	
	public void setReleased(Boolean released) {
		this.released = released;
	}

	public Integer getReleasedEffectiveTime() {
		return releasedEffectiveTime;
	}

	public void setReleasedEffectiveTime(Integer releasedEffectiveTime) {
		this.releasedEffectiveTime = releasedEffectiveTime;
	}

	public Boolean getReleased() {
		return released;
	}
	
	public Boolean isReleased() {
		//Do not be tempted to try and use effective time here, since it gets nulled out when a component is changed
		return released;
	}

	public boolean isReleasedSafely() {
		return released != null && released;
	}

	public String toWhitelistString() {
		return (isActiveSafely()?"1":"0") + "," + moduleId + ",";
	}

	public ComponentAnnotationEntry getComponentAnnotationEntry(String id) {
		for (ComponentAnnotationEntry cae : getComponentAnnotationEntries()) {
			if (cae.getId().equals(id)) {
				return cae;
			}
		}
		return null;
	}

	public void addComponentAnnotationEntry(ComponentAnnotationEntry cae) {
		getComponentAnnotationEntries().remove(cae);
		getComponentAnnotationEntries().add(cae);
	}

	public List<ComponentAnnotationEntry> getComponentAnnotationEntries() {
		if (componentAnnotationEntries == null) {
			componentAnnotationEntries = new java.util.ArrayList<>();
		}
		return componentAnnotationEntries;
	}

	public abstract boolean matchesMutableFields(Component other);

	public String[] getPreviousState() {
		return previousState;
	}

	public void setPreviousState(String[] previousState) {
		this.previousState = previousState;
	}

	public void revertToPreviousState() {
		if (previousState == null) {
			throw new TermServerRuntimeException("Attempt to revert component to previous state when no previous state recorded");
		}
		//Previous state indexes don't include id or effective time, so step back 2
		this.active = SnomedUtilsBase.translateActiveFlag(previousState[IDX_ACTIVE - 2]);
		this.moduleId = previousState[IDX_MODULEID - 2];
	}

	public boolean hasPreviousStateDataRecorded() {
		return previousState != null;
	}

	public abstract List<Component> getReferencedComponents(ComponentStore cs);
}
