package org.snomed.otf.traceability.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.*;

@Entity
public class User {

	@Id
	@GeneratedValue(strategy= GenerationType.AUTO)
	@JsonIgnore
	private Long id;

	@Column(nullable = false)
	private String username;

	public User() {
	}

	public User(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	@Override
	public String toString() {
		return "User{" +
				"id=" + id +
				", username='" + username + '\'' +
				'}';
	}
}
