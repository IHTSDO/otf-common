package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

public interface ComponentStore {

	Component getComponent(String id);

	boolean isComponentId(String id);
}
