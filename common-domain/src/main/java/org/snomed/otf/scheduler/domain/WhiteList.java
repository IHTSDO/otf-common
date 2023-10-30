package org.snomed.otf.scheduler.domain;

import java.util.Set;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class WhiteList {

	@Id
	@GeneratedValue
	@JsonIgnore
	private Long id;
	
	@JsonIgnore //Will be evident in JSON from structure, causes infinite recursion if included explicitly.
	String codeSystemShortname;
	
	@ElementCollection//(fetch = FetchType.EAGER)
	@OneToMany(mappedBy = "id.whiteListId", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	Set<WhiteListedConcept> concepts;
	
	WhiteList() {
	}
	
	public WhiteList (String codeSystemShortname, Set<WhiteListedConcept> concepts) {
		this.codeSystemShortname = codeSystemShortname;
		this.concepts = concepts;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Set<WhiteListedConcept> getConcepts() {
		return concepts;
	}

	public void setConcepts(Set<WhiteListedConcept> concepts) {
		this.concepts = concepts;
	}

	public String getcodeSystemShortname() {
		return codeSystemShortname;
	}

	public void setcodeSystemShortname(String codeSystemShortname) {
		this.codeSystemShortname = codeSystemShortname;
	}
}
