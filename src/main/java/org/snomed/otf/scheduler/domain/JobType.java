package org.snomed.otf.scheduler.domain;

import java.util.*;

public class JobType {
	String name;
	List<JobCategory> categories = new ArrayList<>();
	
	public JobType(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<JobCategory> getCategories() {
		return categories;
	}
	public void setCategories(List<JobCategory> categories) {
		this.categories = categories;
	}
	public JobType addCategory(JobCategory category) {
		this.categories.add(category);
		return this;
	}
	@Override
	public boolean equals (Object other) {
		if (other instanceof JobType) {
			JobType otherType = (JobType)other;
			return name.equals(otherType.getName());
		}
		return false;
	}
}
