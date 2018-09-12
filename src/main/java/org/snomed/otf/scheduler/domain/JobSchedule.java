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
	
	@ElementCollection
	@CollectionTable(name = "job_schedule_parameters")
	@MapKeyColumn(name="key")
	@Column(name="value")
	Map<String, String> parameters;

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
	public Map<String, String> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, String> parameters) {
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
