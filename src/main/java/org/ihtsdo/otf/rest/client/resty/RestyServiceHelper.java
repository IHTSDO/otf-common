package org.ihtsdo.otf.rest.client.resty;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.otf.rest.exception.EntityAlreadyExistsException;
import org.ihtsdo.otf.rest.exception.ResourceNotFoundException;

import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.JSONResource;

public class RestyServiceHelper {

	public static final String HTTP_STATUS = "HTTPStatus";
	public static final String ERROR_MESSAGE = "errorMessage";

	private static final int[] HTTP_SUCCESSFULL_ARRAY = { 200, 201, 202, 203, 204 };

	/**
	 * Throws an appropriate Business Exception if the JSONResponse object is not acceptable (ie 200 - 210)
	 * 
	 * @throws BusinessServiceException
	 * @throws IOException
	 * @throws JSONException
	 */
	public static void ensureSuccessfull(JSONResource response) throws JSONException, IOException, BusinessServiceException {
		boolean httpSuccessfull = false;
		// Not easy to recover the status as header keys are inconsistent.
		// Check against all known acceptable statuses instead.
		for (int thisStatus : HTTP_SUCCESSFULL_ARRAY) {
			if (response.status(thisStatus)) {
				httpSuccessfull = true;
			}
		}

		if (!httpSuccessfull) {
			String body = "";
			String httpStatus = "Status Unrecoverable";

			// First try and parse out an appropriate exception to throw
			throwAppropriateException(response);
			try {
				// Otherwise, return the whole body for information
				httpStatus = response.getHTTPStatus().toString();
				body = response.object().toString(2);
			} catch (Exception e) {
				body = "Unable to parse body: ";
				body += e.getMessage();
			}
			throw new IOException("Call to " + response.location() + " returned unacceptable status [" + httpStatus + "] with body " + body);
		}
	}

	private static void throwAppropriateException(JSONResource response) throws BusinessServiceException{
		// If we can, work out what happened in the response and throw something appropriate
		String exceptionMessage = "Uable to obtain error message from response";
		JSONObject jsonObj = null;
		
		try {
			jsonObj = response.object();
			exceptionMessage = jsonObj.toString(2);
		} catch (Exception e) {
			exceptionMessage += " due to " + e.getMessage();
		}

		// Can we get the http status directly from the object?
		Integer httpStatus = response.getHTTPStatus();

		// Othewise try and parse one out of the response body
		if (jsonObj != null && httpStatus == null && jsonObj.has(HTTP_STATUS)) {
			try{
				httpStatus = Integer.parseInt(jsonObj.getString(HTTP_STATUS));
			} catch (JSONException e) {}
		}

		if (httpStatus != null) {
			// Do we also have an error message ? Use that as the exception message if so, otherwise use body
			if (jsonObj != null && jsonObj.has(ERROR_MESSAGE)) {
				try{
					exceptionMessage = jsonObj.getString(ERROR_MESSAGE);
				} catch (JSONException e) {}
			}
			switch (httpStatus.intValue()) {
			case HttpStatus.SC_NOT_FOUND:
				throw new ResourceNotFoundException(exceptionMessage);
			case HttpStatus.SC_CONFLICT:
				throw new EntityAlreadyExistsException(exceptionMessage);
			}
		}

	}

}
