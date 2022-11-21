package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.List;

import org.ihtsdo.otf.exception.TermServerScriptException;

public class UnknownComponent extends Component {
	
	public UnknownComponent(String id, ComponentType componentType) {
		this.id = id;
		this.componentType = componentType;
	}

	@Override
	public String getReportedName() {
		return null;
	}

	@Override
	public String getReportedType() {
		return componentType.toString();
	}

	@Override
	public String[] toRF2() throws Exception {
		return null;
	}

	@Override
	public Boolean isReleased() {
		return null;
	}

	@Override
	public List<String> fieldComparison(Component other, boolean ignoreEffectiveTime) throws TermServerScriptException {
		return null;
	}

}
