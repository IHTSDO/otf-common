package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.Map;
import java.util.Set;

public class Branch {

	private String name;
	private String path;
	private String state;
	private boolean deleted;
	private long baseTimestamp;
	private long headTimestamp;
	private Map<String, Object> metadata;
	private Set<String> userRoles;
	private Set<String> globalUserRoles;

	public Branch() {
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public long getBaseTimestamp() {
		return baseTimestamp;
	}

	public void setBaseTimestamp(long baseTimestamp) {
		this.baseTimestamp = baseTimestamp;
	}

	public long getHeadTimestamp() {
		return headTimestamp;
	}

	public void setHeadTimestamp(long headTimestamp) {
		this.headTimestamp = headTimestamp;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public Set <String> getUserRoles() {
		return userRoles;
	}

	public void setUserRoles(Set <String> userRoles) {
		this.userRoles = userRoles;
	}

	public Set <String> getGlobalUserRoles() {
		return globalUserRoles;
	}

	public void setGlobalUserRoles(Set <String> globalUserRoles) {
		this.globalUserRoles = globalUserRoles;
	}
}
