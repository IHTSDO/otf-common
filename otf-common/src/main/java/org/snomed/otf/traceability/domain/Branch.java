package org.snomed.otf.traceability.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
public class Branch {

	@Id
	@Column(name = "id")
	@GeneratedValue(strategy=GenerationType.AUTO)
	@JsonIgnore
	private Long id;

	@Column(nullable = false)
	private String branchPath;

	public Branch() {
	}

	public Branch(String branchPath) {
		this.branchPath = branchPath;
	}

	public Long getId() {
		return id;
	}

	public String getBranchPath() {
		return branchPath;
	}

	@Override
	public String toString() {
		return "Branch{" +
				"id=" + id +
				", branchPath='" + branchPath + '\'' +
				'}';
	}
}
