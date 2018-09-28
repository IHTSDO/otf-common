package org.snomed.otf.scheduler.domain;

import java.util.ArrayList;
import java.util.List;

public class JobMetadata {
	List<JobType> jobTypes = new ArrayList<>();

	public List<JobType> getJobTypes() {
		return jobTypes;
	}

	public void setJobTypes(List<JobType> jobTypes) {
		this.jobTypes = jobTypes;
	}
	
}
