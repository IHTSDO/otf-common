package org.snomed.otf.scheduler.domain;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class WhiteListedConcept {

	@Id
	String sctId;
	
	String fsn;
	
	@ManyToOne
	@JsonIgnore
	Job job;

	public String getSctId() {
		return sctId;
	}

	public void setSctId(String sctId) {
		this.sctId = sctId;
	}

	public String getFsn() {
		return fsn;
	}

	public void setFsn(String fsn) {
		this.fsn = fsn;
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}
	
	@Override
	public boolean equals(Object other) {
		return this.sctId.equals(((WhiteListedConcept)other).getSctId());
	}
	
	@Override
	public int hashCode() {
		return sctId.hashCode();
	}
	
}
