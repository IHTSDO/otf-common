package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ihtsdo.otf.exception.TermServerScriptException;

import com.google.gson.annotations.Expose;
import org.ihtsdo.otf.utils.StringUtils;

public class Task implements Comparable<Task>, TermServerLocation {

	private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("^([A-Z0-9]+)-(\\d+)$");

	private static int taskSequence = 0;
	
	int uniqueTaskId;

	public enum TaskStatus {NEW, AUTO_CONFLICT, AUTO_QUEUED, PROMOTED, IN_PROGRESS, UNKNOWN, COMPLETED, AUTO_PROMOTING, AUTO_CLASSIFYING, AUTO_REBASING, IN_REVIEW, DELETED, REVIEW_COMPLETED}

	public enum TaskType { AUTHORING, CRS }

	@Expose
	String key;

	@Expose
	String projectKey;
	
	@Expose
	String branchPath;
	
	@Expose
	String summary;

	@Expose
	TaskStatus status;
	
	@Expose
	TaskUser assignee;

	@Expose
	String description;

	@Expose
	TaskType taskType;

	IBatch batch;
	List<Component> components = new ArrayList<>();
	String reviewer = null;
	String taskInfo;

	boolean preExistingTask = false;

	/* Call IBatch.addNewTask instead of creating a Task directly */
	public Task(IBatch batch, String authorUserName, String reviewer) {
		this.batch = batch;
		this.assignee = new TaskUser(authorUserName);
		this.reviewer = reviewer;
		uniqueTaskId = ++taskSequence;
	}

	@Override
	public Task clone() {
		Task clone = new Task(null, null, null);
		clone.setProjectKey(projectKey);
		clone.setBranchPath(branchPath);
		clone.setSummary(summary);
		clone.setDescription(description);
		clone.setStatus(status);
		clone.assignee = assignee;
		return clone;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSummary() {
		if (summary == null && batch != null) {
			return batch.getTaskName(this);
		} else {
			return summary;
		}
	}

	public int getUniqueTaskId() {
		return uniqueTaskId;
	}
	
	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDescription() {
		if (!StringUtils.isEmpty(description)) {
			return description;
		}
		StringBuilder html = new StringBuilder();
		if (taskInfo != null) {
			html.append("<h3>Task grouping: ").append(taskInfo).append("</h3>\n");
		}
		for (Component component : components) {
			html.append("<h5>").append(component).append("</h5>\n");
		}
		return html.toString();
	}

	public List<Component> getComponents() {
		return components;
	}

	public boolean contains(Component c) {
		//This task might not know it's components are actually concepts, so check ids
		return components.stream().anyMatch(comp -> comp.getId().equals(c.getId()));
	}

	public void setComponents(List<Component> components) {
		this.components = components;
	}
	public String getBranchPath() {
		return branchPath;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String taskKey) {
		this.key = taskKey;
	}
	public void setBranchPath(String branchPath) {
		this.branchPath = branchPath;
	}
	public String toString() {
		if (key != null) {
			return key + " [" + getStatus() + " for " + getAssignedAuthor() + "]: " + getSummary();
		}
		return getSummary();
	}

	public List<String> getIds() {
		return components.stream()
				.map(Component::getId)
				.toList();
	}
	
	public void addAll(Collection<Component> components) {
		this.components.addAll(components);
	}

	public void add(Component component) {
		this.components.add(component);
	}
	
	public void addAfter(Component c, Component after) throws TermServerScriptException {
		int insertPoint = this.components.indexOf(after) + 1;
		if (insertPoint == -1) {
			throw new TermServerScriptException("Unable to insert " + c + " after " + after + " as it's not currently known in the task");
		}
		this.components.add(insertPoint, c);
	}
	
	public void addBefore(Component c, Component before) throws TermServerScriptException {
		int insertPoint = this.components.indexOf(before);
		if (insertPoint == -1) {
			throw new TermServerScriptException("Unable to insert " + c + " before " + before + " as it's not currently known in the task");
		}
		this.components.add(insertPoint, c);
	}
	
	public int size() {
		return components.size();
	}
	
	public String getAssignedAuthor() {
		return assignee.getUsername();
	}

	public void setAssignedAuthor(String assignedAuthor) {
		this.assignee.setUsername(assignedAuthor);
	}

	public String getReviewer() {
		return reviewer;
	}

	public void setReviewer(String reviewer) {
		this.reviewer = reviewer;
	}

	public String getTaskInfo() {
		return taskInfo;
	}

	public void setTaskInfo(String taskInfo) {
		this.taskInfo = taskInfo;
	}

	public void replace(Component current, Component replacement) {
		Collections.replaceAll(components, current, replacement);
	}

	public void remove(Component removeMe) {
		components.remove(removeMe);
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Task t2) {
			return uniqueTaskId == t2.uniqueTaskId;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return uniqueTaskId;
	}

	public TaskUser getAssignee() {
		return assignee;
	}

	public void setAssignee(String username) {
		this.assignee.setUsername(username);
	}

	public boolean hasAssignee(String username) {
		return assignee.getUsername().equals(username);
	}
	
	public boolean isPreExistingTask() {
		return preExistingTask;
	}

	public void setPreExistingTask(boolean preExistingTask) {
		this.preExistingTask = preExistingTask;
	}
	
	public static int getNextTaskSequence() {
		return taskSequence + 1;
	}

	public TaskStatus getStatus() {
		return status;
	}

	public void setStatus(TaskStatus status) {
		this.status = status;
	}

	@Override
	public int compareTo(Task other) {
		if (other == null) {
			return 1;
		}

		Matcher m1 = JIRA_KEY_PATTERN.matcher(this.key);
		Matcher m2 = JIRA_KEY_PATTERN.matcher(other.key);

		if (m1.matches() && m2.matches()) {
			int prefixCompare = m1.group(1).compareTo(m2.group(1));
			if (prefixCompare != 0) {
				return prefixCompare;
			}
			return Integer.compare(
					Integer.parseInt(m1.group(2)),
					Integer.parseInt(m2.group(2))
			);
		}
		return this.key.compareTo(other.key);
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public TaskType getTaskType() {
		return taskType;
	}

	public void setTaskType(TaskType taskType) {
		this.taskType = taskType;
	}
}
