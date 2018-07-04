package org.snomed.otf.scheduler.domain;

import java.net.URL;
import java.util.List;

public class Job {
	String name;
	String description;
	List<String> parameterNames;
	URL jobRun;
	List<JobSchedule> schedules;
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public List<String> getParameterNames() {
		return parameterNames;
	}
	public void setParameterNames(List<String> parameterNames) {
		this.parameterNames = parameterNames;
	}
	public URL getJobRun() {
		return jobRun;
	}
	public void setJobRun(URL jobRun) {
		this.jobRun = jobRun;
	}
	public List<JobSchedule> getSchedules() {
		return schedules;
	}
	public void setSchedules(List<JobSchedule> schedules) {
		this.schedules = schedules;
	}
}
