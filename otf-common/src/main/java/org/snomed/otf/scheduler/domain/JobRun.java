package org.snomed.otf.scheduler.domain;

import java.util.*;

import javax.persistence.*;

import com.amazonaws.services.ec2.model.transform.TerminateInstancesRequestMarshaller;

@Entity
public class JobRun {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(columnDefinition = "BINARY(16)")
	UUID id;
	String jobName;
	
	@ElementCollection
	@CollectionTable(name = "job_run_parameters")
	@MapKeyColumn(name="param_name", length=25)
	@Column(name="value")
	Map<String, String> parameters = new HashMap<>();
	
	String terminologyServerUrl;
	Date requestTime;
	String user;
	String authToken;
	JobStatus status;
	
	@Column(length = 65535,columnDefinition="Text")
	String debugInfo;
	Date resultTime;
	Integer issuesReported;
	String resultUrl;
	
	private JobRun () {}
	
	static public JobRun create(String jobName, String user) {
		JobRun j = new JobRun();
		j.id = UUID.randomUUID();
		j.jobName = jobName;
		j.user = user;
		j.requestTime = new Date();
		return j;
	}
	
	static public JobRun create(JobSchedule jobSchedule) {
		JobRun j = new JobRun();
		j.id = UUID.randomUUID();
		j.jobName = jobSchedule.getJobName();
		j.user = jobSchedule.getUser();
		j.setParameters(new HashMap<>(jobSchedule.getParameters()));
		j.requestTime = new Date();
		return j;
	}
	public String getJobName() {
		return jobName;
	}
	public void setJobName(String jobName) {
		this.jobName = jobName;
	}
	public Map<String, String> getParameters() {
		return parameters;
	}
	public String getParameter(String key) {
		if (parameters!= null && parameters.containsKey(key)) {
			return parameters.get(key);
		}
		return null;
	}
	
	public String getMandatoryParameter(String key) {
		if (parameters!= null && parameters.containsKey(key)) {
			return parameters.get(key);
		}
		throw new IllegalArgumentException("Madatory parameter '" + key + "' was not supplied");
	}
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	public void setParameter(String key, Object value) {
		if (parameters == null) {
			parameters = new HashMap<>();
		}
		if (value != null) {
			this.parameters.put(key, value.toString());
		} else {
			this.parameters.remove(key);
		}
	}
	public String getTerminologyServerUrl() {
		return terminologyServerUrl;
	}
	public void setTerminologyServerUrl(String terminologyServerUrl) {
		this.terminologyServerUrl = terminologyServerUrl;
	}
	public Date getRequestTime() {
		return requestTime;
	}
	public void setRequestTime(Date requestTime) {
		this.requestTime = requestTime;
	}
	public String getUser() {
		return user;
	}
	public String getAuthToken() {
		return authToken;
	}
	public void setAuthToken(String authToken) {
		this.authToken = authToken;
	}
	public JobStatus getStatus() {
		return status;
	}
	public void setStatus(JobStatus status) {
		this.status = status;
	}
	public String getDebugInfo() {
		return debugInfo;
	}
	public void setDebugInfo(String debugInfo) {
		this.debugInfo = debugInfo;
	}
	public Date getResultTime() {
		return resultTime;
	}
	public void setResultTime(Date resultTime) {
		this.resultTime = resultTime;
	}
	public Integer getIssuesReported() {
		return issuesReported;
	}
	public void setIssuesReported(Integer issuesReported) {
		this.issuesReported = issuesReported;
	}
	public String getResultUrl() {
		return resultUrl;
	}
	public void setResultUrl(String resultUrl) {
		this.resultUrl = resultUrl;
	}
	public UUID getId() {
		return id;
	}
	public void setId(UUID id) {
		this.id = id;
	}
	public void setUser(String user) {
		this.user = user;
	}
	@Override
	public String toString() {
		return jobName + " (id: " + id + ")"
				+ " for user '" + user + "' in status: "
				+ status 
				+ (debugInfo == null? "" : " (Reason: " + debugInfo + ")");
	}
	
	@Override 
	public JobRun clone() {
		JobRun clone = new JobRun();
		clone.setAuthToken(getAuthToken());
		clone.setDebugInfo(getDebugInfo());
		clone.setId(getId());
		clone.setIssuesReported(getIssuesReported());
		clone.setJobName(getJobName());
		clone.setParameters(getParameters());
		clone.setRequestTime(getRequestTime());
		clone.setResultTime(getResultTime());
		clone.setResultUrl(getResultUrl());
		clone.setStatus(getStatus());
		clone.setTerminologyServerUrl(getTerminologyServerUrl());
		clone.setUser(getUser());
		return clone;
	}

}
