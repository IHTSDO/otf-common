package org.snomed.otf.scheduler.domain;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import jakarta.persistence.*;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
public class JobParameter implements Serializable {

	public enum Type {	BOOLEAN, STRING, CONCEPT, 
						CONCEPT_LIST, TEMPLATE_NAME, TEMPLATE,
						HIDDEN, ECL, PROJECT, DROPDOWN, CHECKBOXES, RELEASE_ARCHIVE, BUILD_ARCHIVE }
	
	@Id
	@JsonIgnore
	@GeneratedValue(strategy = GenerationType.AUTO)
	Long id;
	
	@JsonIgnore
	String paramKey;
	
	@JsonIgnore
	@ManyToOne
	JobParameters parentParams;
	
	Type type;
	
	@Column(columnDefinition="TEXT")
	String value;
	
	@ElementCollection(targetClass=String.class)
	@Column(name="parameter_values")
	private List<String> values;
	
	@ElementCollection(targetClass=String.class)
	private List<String> options;
	
	@Column(columnDefinition="TEXT")
	String description;
	
	String validationRule;
	
	Boolean mandatory;
	
	Integer displayOrder;

	public JobParameter () {}
	
	/* Parameters must always know their owning object, otherwise database
	 * saves will fail.
	 */
	protected JobParameter (JobParameters parentParams, String key, int displayOrder) {
		this.parentParams = parentParams;
		this.displayOrder = displayOrder;
		this.paramKey = key;
	}

	//This function is for chaining and adding a sibling
	//The JobParameter returned is a new one, not "this"
	public JobParameter add(String paramKey) {
		return parentParams.add(paramKey);
	}
	
	public JobParameter withType (Type type) {
		this.type = type;
		return this;
	}
	
	public JobParameter withDescription (String description) {
		this.description = description;
		return this;
	}
	
	public JobParameter withValues(String... values) {
		if (values == null) {
			setValues(null);
		} else {
			setValues(Arrays.asList(values));
		}
		return this;
	}
	
	public JobParameter withOptions(String... options) {
		if (options == null) {
			setOptions(null);
		} else {
			setOptions(Arrays.asList(options));
		}
		return this;
	}
	
	public JobParameter withValue(Object value) {
		return setValue(value);
	}

	public JobParameter setValue(Object value) {
		if (value == null) {
			this.value = null;
		} else {
			this.value = value.toString();
		}
		return this;
	}

	public String getValue() {
		return value;
	}
	
	public JobParameter withDefaultValue(Object defaultValue) {
		if (defaultValue == null) {
			this.value = null;
		} else {
			this.value = defaultValue.toString();
		}
		return this;
	}
	
	public JobParameter withDefaultValues(String... defaultValues) {
		if (defaultValues == null) {
			this.values = null;
		} else {
			this.values = Arrays.asList(defaultValues);
		}
		return this;
	}

	public JobParameters build() {
		return parentParams;
	}

	public String getParamKey() {
		return paramKey;
	}

	public void setParamKey(String key) {
		this.paramKey = key;
	}

	public boolean getMandatory() {
		return mandatory == null ? Boolean.FALSE : mandatory ;
	}

	public void setMandatory(Boolean mandatory) {
		this.mandatory = mandatory;
	}

	public JobParameters getParentParams() {
		return parentParams;
	}

	public void setParentParams(JobParameters parentParams) {
		this.parentParams = parentParams;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getValidationRule() {
		return validationRule;
	}

	public void setValidationRule(String validationRule) {
		this.validationRule = validationRule;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public JobParameter withMandatory() {
		this.mandatory = true;
		return this;
	}
	
	public String toString() {
		String str = "'" + (value == null? "" : value) + "'";
		if (getMandatory()) {
			str = "* " + str;
		}
		return str;
	}

	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}
	
	public List<String> getValues() {
		return values;
	}

	public void setValues(List<String> values) {
		this.values = values;
	}
	
	public List<String> getOptions() {
		return options;
	}

	public void setOptions(List<String> options) {
		this.options = options;
	}
	
	public int hashCode() {
		return paramKey.hashCode();
	}
	
	public boolean equals(Object other) {
		if (other instanceof JobParameter otherJobParameter) {
			return paramKey.equals(otherJobParameter.getParamKey());
		}
		return false;
	}

}
