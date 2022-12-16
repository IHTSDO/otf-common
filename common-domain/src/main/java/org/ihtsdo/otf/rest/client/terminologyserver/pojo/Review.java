package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.List;

import com.google.gson.annotations.Expose;

public class Review {
	@Expose
	private String id;
	
	@Expose
	private String status;
	
	@Expose
	private List<Long> changedConcepts;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public List<Long> getChangedConcepts() {
		return changedConcepts;
	}
	public void setChangedConcepts(List<Long> changedConcepts) {
		this.changedConcepts = changedConcepts;
	}
}
