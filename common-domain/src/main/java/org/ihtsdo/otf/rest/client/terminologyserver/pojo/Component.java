package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.List;

public abstract class Component {
	
	public enum ComponentType { CONCEPT, DESCRIPTION, STATED_RELATIONSHIP, 
		INFERRED_RELATIONSHIP, LANGREFSET, ATTRIBUTE_VALUE, HISTORICAL_ASSOCIATION,
		TEXT_DEFINITION, AXIOM}
	
	//Generic debug string to say if concept should be highlighted for some reason, eg cause a template match to fail
	private transient String issues = "";

	public abstract String getId();
	
	public abstract String getEffectiveTime();
	
	public abstract boolean isActive();
	
	public abstract String getModuleId();
	
	public abstract String getReportedName();
	
	public abstract String getReportedType();
	
	public abstract ComponentType getComponentType();
	
	public abstract String[] toRF2() throws Exception;
	
	protected boolean isDirty = false;
	
	public void addIssue(String issue) {
		if (this.issues == null) {
			this.issues = issue;
		} else {
			if (!this.issues.isEmpty()) {
				this.issues += ", ";
			}
			this.issues += issue;
		}
	}
	
	public String getIssues() {
		return issues;
	}

	public void setIssue(String issue) {
		issues = issue;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Component) {
			return this.getId().equals(((Component)other).getId());
		}
		return false;
	}

	public abstract List<String> fieldComparison(Component other);
	
	protected void commonFieldComparison(Component other, List<String> differences) {
		String name = this.getClass().getSimpleName(); 
		
		if (!this.getId().equals(other.getId())) {
			differences.add("Id different in " + name + ": " + this.getId() + " vs " + other.getId());
		}
		
		if (this.getEffectiveTime() != null || this.getEffectiveTime() != null) {
			if ((this.getEffectiveTime() == null && this.getEffectiveTime() != null) ||
					(this.getEffectiveTime() != null && this.getEffectiveTime() == null) ||
					!this.getEffectiveTime().equals(other.getEffectiveTime())){
				differences.add("EffectiveTime different in " + name + ": " + this.getEffectiveTime() + " vs " + other.getEffectiveTime());
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
}
