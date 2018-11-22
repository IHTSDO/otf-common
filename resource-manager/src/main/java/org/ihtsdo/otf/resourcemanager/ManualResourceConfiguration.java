package org.ihtsdo.otf.resourcemanager;

/**
 * Option to create a ResourceConfiguration manually for applications not using Spring Framework.
 */
public class ManualResourceConfiguration extends ResourceConfiguration {

	public ManualResourceConfiguration(boolean readonly, boolean useCloud, Local localConfig, Cloud cloudConfig) {
		super();
		setReadonly(readonly);
		setUseCloud(useCloud);
		setLocal(localConfig);
		setCloud(cloudConfig);
	}
}
