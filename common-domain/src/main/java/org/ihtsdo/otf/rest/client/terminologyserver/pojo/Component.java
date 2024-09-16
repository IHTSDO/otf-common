package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.ihtsdo.otf.RF2Constants;
import org.ihtsdo.otf.exception.ScriptException;
import org.ihtsdo.otf.exception.TermServerScriptException;
import org.ihtsdo.otf.utils.StringUtils;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public abstract class Component implements RF2Constants {

	public enum ComponentType { CONCEPT, DESCRIPTION, STATED_RELATIONSHIP,
		INFERRED_RELATIONSHIP, LANGREFSET, ATTRIBUTE_VALUE, HISTORICAL_ASSOCIATION,
		TEXT_DEFINITION, AXIOM, ALTERNATE_IDENTIFIER, COMPONENT_ANNOTATION, REFSET_MEMBER_ANNOTATION, UNKNOWN}
	
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
	@SerializedName("memberId")

	List<ComponentAnnotationEntry> componentAnnotationEntries;
	
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
			throw new RuntimeException("Attempt to hash/equal component with no id.  Use concrete class check of values instead");
		}
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Component) {
			return this.getIdOrThrow().equals(((Component)other).getId());
		}
		return false;
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
		
		if (!ignoreEffectiveTime) {
			if (this.getEffectiveTime() != null || this.getEffectiveTime() != null) {
				if ((this.getEffectiveTime() == null && this.getEffectiveTime() != null) ||
						(this.getEffectiveTime() != null && this.getEffectiveTime() == null) ||
						!this.getEffectiveTime().equals(other.getEffectiveTime())){
					differences.add("EffectiveTime different in " + name + ": " + this.getEffectiveTime() + " vs " + other.getEffectiveTime());
				}
			}
		}
		
		if (!this.isActive() == other.isActive()) {
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
	
	public String getMutableFields() {
		return (this.isActive()?"1,":"0,") + this.getModuleId() + ",";
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
		//I know you're going to want to put this back in, but resist the temptation.
		//We should know for sure if a component has been released or not, don't 
		//fall back on the effective time, it can't be trusted.
		/*if (released == null) {
			return !(effectiveTime == null || effectiveTime.isEmpty());
		}*/
		return released;
	}

	public boolean isReleasedSafely() {
		return released != null && released;
	}

	public String toWhitelistString() {
		return (isActive()?"1":"0") + "," + moduleId + ",";
	}

	public ComponentAnnotationEntry getComponentAnnotationEntry(String refsetId) {
		for (ComponentAnnotationEntry cae : getComponentAnnotationEntries()) {
			if (cae.getRefsetId().equals(refsetId)) {
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

}
