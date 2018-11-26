package org.snomed.otf.scheduler.domain;

import java.util.*;

import javax.persistence.*;

@Entity
public class JobSchedule {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	UUID id;
	
	String jobName;
	String user;
	String schedule;
	
	@OneToOne(cascade = CascadeType.ALL)
	JobScheduleParameters parameters;

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
	public JobParameters getParameters() {
		return parameters;
	}
	public void setParameters(JobScheduleParameters parameters) {
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
