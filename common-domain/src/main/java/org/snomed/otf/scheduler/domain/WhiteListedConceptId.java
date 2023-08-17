package org.snomed.otf.scheduler.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Embeddable;

@Embeddable
public class WhiteListedConceptId implements Serializable {
	@Serial
	private static final long serialVersionUID = -3951203459129549849L;

	private String sctId;
	
	private Long whiteListId;

	public WhiteListedConceptId() {
	}

	public String getSctId() {
		return sctId;
	}

	public void setSctId(String sctId) {
		this.sctId = sctId;
	}

	public Long getWhiteListId() {
		return whiteListId;
	}

	public void setWhiteList(WhiteList whiteList) {
		if (whiteList.getId() == null) {
			throw new IllegalArgumentException("Attempt to set WhiteList with null ID");
		}
		this.whiteListId = whiteList.getId();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof WhiteListedConceptId otherWLCI) {
			return this.hashCode() == otherWLCI.hashCode();
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(sctId, whiteListId);
	}

}

