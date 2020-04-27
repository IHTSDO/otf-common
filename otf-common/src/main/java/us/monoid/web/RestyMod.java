package us.monoid.web;

import java.net.URLConnection;

/**
 * Please replace use of Resty and RestyMod with the Spring Framework RestTemplate.
 */
@Deprecated
public class RestyMod extends Resty {

	public RestyMod(Option... someOptions) {
		super(someOptions);
	}

	@Override
	// Don't add the resource's Accept header
	protected <T extends AbstractResource> void addStandardHeaders(URLConnection con, T resource) {
		con.setRequestProperty("User-Agent", userAgent);
	}
}

