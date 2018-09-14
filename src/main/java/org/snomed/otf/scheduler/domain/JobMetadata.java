package org.snomed.otf.scheduler.domain;

import java.util.ArrayList;
import java.util.List;

public class JobMetadata {
	List<Job> jobs = new ArrayList<>();

	public List<Job> getJobs() {
		return jobs;
	}

	public void setJobs(List<Job> jobs) {
		this.jobs = jobs;
	}
	
}
