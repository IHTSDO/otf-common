package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.*;

import org.ihtsdo.otf.exception.TermServerScriptException;

import com.google.gson.annotations.Expose;

public class Task {
	
	public static int TASK_SEQUENCE = 0;
	
	int uniqueTaskId;
	
	@Expose
	String key;
	
	@Expose
	String branchPath;
	
	@Expose
	String summary;
	
	IBatch batch;
	List<Component> components = new ArrayList<Component>();
	String assignedAuthor = null;
	String reviewer = null;
	String taskInfo;

	/* Call IBatch.addNewTask instead of creating a Task directly */
	public Task(IBatch batch, String[] author_reviewer) {
		this.batch = batch;
		setAuthorReviewer(author_reviewer);
		uniqueTaskId = ++TASK_SEQUENCE;
	}
	
	void setAuthorReviewer(String[] author_reviewer) {
		setAssignedAuthor(author_reviewer[0]);
		if (author_reviewer.length > 1) {
			setReviewer(author_reviewer[1]);
		}
	}

	public String getSummary() {
		if (batch != null) {
			return batch.getTaskName(this);
		} else {
			return summary;
		}
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
		return assignedAuthor;
	}

	public void setAssignedAuthor(String assignedAuthor) {
		this.assignedAuthor = assignedAuthor;
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
	
	public boolean equals(Object other) {
		if (other instanceof Task) {
			Task t2 = ((Task)other);
			return uniqueTaskId == t2.uniqueTaskId;
		}
		return false;
	}

}
