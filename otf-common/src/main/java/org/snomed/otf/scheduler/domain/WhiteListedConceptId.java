package org.snomed.otf.scheduler.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Embeddable;

@Embeddable
public class WhiteListedConceptId implements Serializable {

	private static final long serialVersionUID = -3951203459129549849L;

	private String sctId;
	
	private Long jobId;

	public String getSctId() {
		return sctId;
	}

	public void setSctId(String sctId) {
		this.sctId = sctId;
	}

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof WhiteListedConceptId) {
			WhiteListedConceptId otherWLCI = (WhiteListedConceptId)other;
			if (otherWLCI.sctId.equals(sctId) && otherWLCI.jobId == jobId) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(sctId, jobId);
	}

}
