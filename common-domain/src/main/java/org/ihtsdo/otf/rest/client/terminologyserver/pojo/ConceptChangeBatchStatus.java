package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.Set;

public class ConceptChangeBatchStatus {

	public enum Status {
		RUNNING, COMPLETED, FAILED
	}

	private String id;
	private String status;
	private String message;
	private Set<Long> conceptIds;
	private Long startTime;
	private Long endTime;
	private Float secondsDuration;

	public ConceptChangeBatchStatus() {
	}

	public ConceptChangeBatchStatus(Status status) {
		this.status = status.toString();
	}

	public ConceptChangeBatchStatus(Status status, String message) {
		this.status = status.toString();
		this.message = message;
	}

	public String getId() {
		return id;
	}

	public Status getStatus() {
		return status != null ? Status.valueOf(status) : null;
	}

	public String getMessage() {
		return message;
	}

	public Set<Long> getConceptIds() {
		return conceptIds;
	}

	public Long getStartTime() {
		return startTime;
	}

	public Long getEndTime() {
		return endTime;
	}

	public Float getSecondsDuration() {
		return secondsDuration;
	}
}
