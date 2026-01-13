package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import com.google.gson.annotations.Expose;

public class TaskUser {

	@Expose
	String email;

	@Expose
	String displayName;

	@Expose
	String username;

	@Expose
	boolean active;

	public TaskUser(String username) {
		this.username = username;
	}


	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}


}
