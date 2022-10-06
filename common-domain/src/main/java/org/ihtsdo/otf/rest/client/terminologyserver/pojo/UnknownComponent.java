package org.ihtsdo.otf.rest.client.terminologyserver.pojo;

import java.util.List;

import org.ihtsdo.otf.exception.TermServerScriptException;

public class UnknownComponent extends Component {
	
	public UnknownComponent(String id, ComponentType type) {
		this.id = id;
		this.type = type;
	}

	@Override
	public String getReportedName() {
		return null;
	}

	@Override
	public String getReportedType() {
		return type.toString();
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
