package org.snomed.otf.scheduler.domain;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class WhiteListedConcept {

	@EmbeddedId
	@JsonIgnore
	WhiteListedConceptId id;
	
	String fsn;

	public String getFsn() {
		return fsn;
	}
	
	public String getSctId() {
		return id.getSctId();
	}

	public void setSctId(String sctId) {
		if (id == null) {
			id = new WhiteListedConceptId();
		}
		id.setSctId(sctId);
	}

	public void setFsn(String fsn) {
		this.fsn = fsn;
	}
	
	public WhiteListedConceptId getId() {
		return id;
	}
	
	@Override
	public boolean equals (Object obj) {
		if (obj instanceof WhiteListedConcept) {
			return this.id.equals(((WhiteListedConcept)obj).getId());
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public String toString() {
		return id.getSctId() + "|" + fsn + "| - " + id.getWhiteListId();
	}

	public void setWhiteList(WhiteList whiteList) {
		if (id == null) {
			id = new WhiteListedConceptId();
		}
		id.setWhiteList(whiteList);
	}
	
}