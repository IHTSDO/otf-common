package org.snomed.otf.scheduler.domain;

import java.util.List;

public class JobSchedule {
	int id;
	String jobName;
	String author;
	String schedule;
	List<JobParameter> parameters;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
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
}
