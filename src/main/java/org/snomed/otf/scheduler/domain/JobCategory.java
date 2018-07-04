package org.snomed.otf.scheduler.domain;

import java.util.*;

public class JobCategory {
	String name;
	JobType type;
	List<Job> jobs = new ArrayList<>();
	
	public JobCategory(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public JobType getType() {
		return type;
	}
	public void setType(JobType type) {
		this.type = type;
	}
	public List<Job> getJobs() {
		return jobs;
	}
	public void setJobs(List<Job> jobs) {
		this.jobs = jobs;
	}
	public JobCategory addJob (Job job) {
		jobs.add(job);
		return this;
	}
	@Override
	public boolean equals (Object other) {
		if (other instanceof JobCategory) {
			JobCategory otherCat = (JobCategory)other;
			if (type.equals(otherCat.getType())) {
				return name.equals(otherCat.getName());
			}
		}
		return false;
	}
}
