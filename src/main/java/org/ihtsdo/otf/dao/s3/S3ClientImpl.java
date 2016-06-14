package org.ihtsdo.otf.dao.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class S3ClientImpl extends AmazonS3Client implements S3Client {

	public S3ClientImpl(AWSCredentials awsCredentials) {
		super(awsCredentials);
	}

	public PutObjectResult putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata)
			throws AmazonClientException {
		// Memory problems with large files necessitate writing to disk before
		// uploading to AWS
		File cachedFile = cacheLocally(input, key);
		PutObjectRequest putRequest = new PutObjectRequest(bucketName, key, cachedFile);
		putRequest.setMetadata(metadata);
		try {
			return super.putObject(putRequest);
		} finally {
			cachedFile.delete();
		}
	}

	public PutObjectResult putObject(PutObjectRequest putRequest) throws AmazonClientException {
		// If we have an inputstream, modify the putRequest with a file of known size instead
		if (putRequest.getFile() == null && putRequest.getInputStream() != null) {
			File cachedFile = cacheLocally(putRequest.getInputStream(), putRequest.getKey());
			putRequest.setInputStream(null);
			putRequest.setFile(cachedFile);
			PutObjectResult result = super.putObject(putRequest);
			cachedFile.delete();
			return result;
		}
		return super.putObject(putRequest);
	}

	private File cacheLocally(InputStream inputStream, String key) throws AmazonClientException {
		try {
			File cachedFile = File.createTempFile(key, ".cached");
			FileUtils.copyInputStreamToFile(inputStream, cachedFile);
			return cachedFile; // Make sure this gets deleted after use!
		} catch (IOException e) {
			throw new AmazonClientException("Failed to cache input stream locally", e);
		}
	}

	@Override
	public String getString(String bucketName, String key) {
		try (S3Object obj = getObject(bucketName, key)) {
			InputStream is = obj.getObjectContent();
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			StringBuilder responseStrBuilder = new StringBuilder();

			String inputStr;
			while ((inputStr = streamReader.readLine()) != null) {
				responseStrBuilder.append(inputStr);
			}

			return responseStrBuilder.toString();
		} catch (IOException e) {
			throw new AmazonClientException("Failed to load resource.", e);
		}
	}

}
