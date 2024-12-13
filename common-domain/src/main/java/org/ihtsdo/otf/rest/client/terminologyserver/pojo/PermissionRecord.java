package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.Set;

public class PermissionRecord {

    private String role;

    private String path;

    private Set<String> userGroups;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Set<String> getUserGroups() {
        return userGroups;
    }

    public void setUserGroups(Set<String> userGroups) {
        this.userGroups = userGroups;
    }
}
