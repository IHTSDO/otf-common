package org.snomed.otf.scheduler.domain;

import java.util.List;

public class JobCategory {
	String name;
	List<Job> jobs;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public List<Job> getJobs() {
		return jobs;
	}
	public void setJobs(List<Job> jobs) {
		this.jobs = jobs;
	}
}
