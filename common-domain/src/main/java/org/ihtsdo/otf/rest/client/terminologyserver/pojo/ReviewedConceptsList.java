package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.List;

import com.google.gson.annotations.Expose;

public class ReviewedConceptsList {
	@Expose
	private List<String> conceptIds;

	@Expose
	private String approvalDate;

	public List<String> getConceptIds() {
		return conceptIds;
	}
	public void setConceptIds(List<String> conceptIds) {
		this.conceptIds = conceptIds;
	}
	public String getApprovalDate() {
		return approvalDate;
	}
	public void setApprovalDate(String approvalDate) {
		this.approvalDate = approvalDate;
	}
}
