package org.snomed.otf.scheduler.domain;

import java.util.*;

public class JobSchedule {
	UUID id;
	String jobName;
	String user;
	String schedule;
	List<JobParameter> parameters;
	

	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public String getSchedule() {
		return schedule;
	}
	public void setSchedule(String schedule) {
		this.schedule = schedule;
	}
	public List<JobParameter> getParameters() {
		return parameters;
	}
	public void setParameters(List<JobParameter> parameters) {
		this.parameters = parameters;
	}
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
}
