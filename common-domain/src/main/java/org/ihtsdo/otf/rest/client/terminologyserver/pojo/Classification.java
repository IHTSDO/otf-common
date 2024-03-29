package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.google.gson.annotations.Expose;

public class Classification {
	
	@Expose
	private String id;
	
	@Expose
	private String status;

	@Expose
	private String errorMessage;
	
	@Expose
	private ClassificationResults results;
	
	public Classification (ClassificationResults results) {
		this.results = results;
	}
	
	public Classification(String errorMsg) {
		ClassificationResults runObj = new ClassificationResults();
		runObj.setStatus(ClassificationStatus.FAILED);
		results = runObj;
		errorMessage = errorMsg;
	}

	public String getStatus() {
		return status;

	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public String getId() {
		return id;
	}

	public ClassificationResults getResults() {
		return results;
	}

	public void setResults(ClassificationResults results) {
		this.results = results;
	}
	
	@Override
	public String toString() {
		String str = "";
		if (id != null ) {
			str += "Classification id: " + id + "\n";
		}
		if (status != null) {
			str += "status : " + status;
		} else {
			str += "No classification data received";
		}
		return str;
	}
	
}
