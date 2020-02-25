package org.snomed.otf.scheduler.domain;

import java.util.Set;

import javax.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class WhiteList {

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@JsonIgnore
	private long id;
	
	@JsonIgnore //Will be evident in JSON from structure, causes infinite recursion if included explicitly.
	String codeSystemShortname;
	
	@ElementCollection(fetch = FetchType.EAGER)
	@OneToMany(mappedBy = "whiteList", cascade = CascadeType.ALL, orphanRemoval = true)
	Set<WhiteListedConcept> concepts;
	
	WhiteList() {
	}
	
	WhiteList (String codeSystemShortname, Set<WhiteListedConcept> concepts) {
		this.codeSystemShortname = codeSystemShortname;
		this.concepts = concepts;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
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
