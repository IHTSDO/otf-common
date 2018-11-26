package org.snomed.otf.scheduler.domain;

import java.util.Map;

import javax.persistence.Entity;

@Entity
public class JobRunParameters extends JobParameters {

	public JobRunParameters(Map<String, JobParameter> parameterMap) {
		super(parameterMap);
	}

}
