package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.*;

import org.ihtsdo.otf.exception.TermServerScriptException;

import com.google.gson.annotations.Expose;

public class Task {
	
	private static int taskSequence = 0;
	
	int uniqueTaskId;
	
	@Expose
	String key;
	
	@Expose
	String branchPath;
	
	@Expose
	String summary;

	@Expose
	String status;
	
	@Expose
	Map<String, String> assignee = new HashMap<>();

	IBatch batch;
	List<Component> components = new ArrayList<>();
	String author = null;
	String reviewer = null;
	String taskInfo;

	boolean preExistingTask = false;

	/* Call IBatch.addNewTask instead of creating a Task directly */
	public Task(IBatch batch, String author, String reviewer) {
		this.batch = batch;
		this.author = author;
		this.reviewer = reviewer;
		uniqueTaskId = ++taskSequence;
	}
	
	public String getSummary() {
		if (batch != null) {
			return batch.getTaskName(this);
		} else {
			return summary;
		}
	}
	
	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getDescriptionHTML() {
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
			return key + ": " + getSummary();
		}
		return getSummary();
	}
	public String toQuotedList() {
		StringBuilder quotedList = new StringBuilder(components.size()*10).append("[");
		boolean first = true;
		for (Component c : components) {
			if (!first) {
				quotedList.append(", ");
			}
			quotedList.append("\"").append(c.getId()).append("\"");
			first = false;
		}
		quotedList.append("]");
		return quotedList.toString();
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
		return author;
	}

	public void setAssignedAuthor(String assignedAuthor) {
		this.author = assignedAuthor;
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

	public Map<String, String> getAssignee() {
		return assignee;
	}

	public void setAssignee(Map<String, String> assignee) {
		this.assignee = assignee;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}
}
