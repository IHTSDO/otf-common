package org.snomed.otf.scheduler.domain;

import java.util.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class JobType {
	@Id
	@Column(length = 50)
	String name;
	
	@OneToMany(mappedBy = "type")
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
	
	@Override
	public String toString() {
		return name;
	}
}
