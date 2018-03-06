package org.ihtsdo.otf.rest.client.snowowl.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({"id", "active", "fsn"})
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleConceptPojo {

	private String id;
	
	private boolean active;
	
	private FsnPojo fsn;	

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public FsnPojo getFsn() {
		return fsn;
	}

	public void setFsn(FsnPojo fsn) {
		this.fsn = fsn;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleConceptPojo [");
		if (id != null)
			builder.append("id=").append(id).append(", ");
		builder.append("active=").append(active).append(", ");
		if (fsn != null)
			builder.append("fsn=").append(fsn);
		builder.append("]");
		return builder.toString();
	}
}

