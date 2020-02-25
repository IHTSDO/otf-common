package org.snomed.otf.scheduler.domain;


import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class WhiteListedConcept {

	@ManyToOne
	@JsonIgnore //Will be evident in JSON from structure, causes infinite recursion if included explicitly.
	WhiteList whiteList;
	
	@Id
	String sctId;
	
	String fsn;
	
	public String getFsn() {
		return fsn;
	}
	
	public String getSctId() {
		return sctId;
	}

	public void setSctId(String sctId) {
		this.sctId = sctId;
	}

	public void setFsn(String fsn) {
		this.fsn = fsn;
	}
	
	
	@Override
	public boolean equals (Object obj) {
		if (obj instanceof WhiteListedConcept) {
			WhiteListedConcept other = (WhiteListedConcept)obj;
			return this.sctId.equals(other.sctId);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return sctId.hashCode();
	}
	
	@Override
	public String toString() {
		return getSctId() + "|" + fsn + "|";
	}

	public WhiteList getWhiteList() {
		return whiteList;
	}

	public void setWhiteList(WhiteList whiteList) {
		this.whiteList = whiteList;
	}
	
}
