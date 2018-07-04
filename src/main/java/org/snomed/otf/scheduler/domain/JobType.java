package org.snomed.otf.scheduler.domain;

import java.util.List;

public class JobType {
	String name;
	List<JobCategory> categories;
	
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
}
