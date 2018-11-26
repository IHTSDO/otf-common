package org.snomed.otf.scheduler.domain;

import java.util.Map;

import javax.persistence.Entity;

@Entity
public class JobScheduleParameters extends JobParameters {

	public JobScheduleParameters(Map<String, JobParameter> parameterMap) {
		super(parameterMap);
	}

}
