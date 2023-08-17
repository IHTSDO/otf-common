package org.ihtsdo.otf.resourcemanager;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("my.storage")
public class MyResourceConfigurationExample extends ResourceConfiguration {
}
