package org.ihtsdo.otf.dao.s3;

import io.awspring.cloud.s3.ObjectMetadata;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class S3ClientImpl implements S3Client {

	private final software.amazon.awssdk.services.s3.S3Client amazonS3Client;
	public S3ClientImpl(software.amazon.awssdk.services.s3.S3Client s3Client) {
		this.amazonS3Client = s3Client;
	}

	@Override
	public ListObjectsResponse listObjects(String bucketName, String prefix) throws S3Exception {
		return amazonS3Client.listObjects(lr -> lr.bucket(bucketName).prefix(prefix));
	}

	@Override
	public ListObjectsResponse listObjects(ListObjectsRequest listObjectsRequest) throws S3Exception {
		return amazonS3Client.listObjects(listObjectsRequest);
	}

	@Override
	public ResponseInputStream<GetObjectResponse> getObject(String bucketName, String key) throws S3Exception {
		return amazonS3Client.getObject(rq -> rq.bucket(bucketName).key(key));

	}

	@Override
	public PutObjectResponse putObject(String bucketName, String key, File file) throws S3Exception {
		return amazonS3Client.putObject(pr -> pr.bucket(bucketName).key(key).build(), RequestBody.fromFile(file));
	}
	@Override
	public PutObjectResponse putObject(String bucketName, String key, byte[] bytes) throws S3Exception {
		return amazonS3Client.putObject(pr -> pr.bucket(bucketName).key(key).build(), RequestBody.fromBytes(bytes));
	}

	public PutObjectResponse putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata, long size) throws S3Exception {
		PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder().bucket(bucketName).key(key);
		if (metadata != null) {
			requestBuilder.metadata(metadata.getMetadata());
		}
		return amazonS3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(input, size));
	}

	@Override
	public PutObjectResponse putObject(String bucketName, String key, InputStream input, long size, String md5) throws S3Exception, DecoderException {
		PutObjectRequest.Builder requestBuilder = PutObjectRequest.builder().bucket(bucketName).key(key);
		requestBuilder.contentLength(size);
		if (md5 != null) {
			byte[] decodedHex = Hex.decodeHex(md5.toCharArray());

			//Apparently we need the unchunked string encoding method here to match what AWS is expecting.
			String md5Base64 = Base64.encodeBase64String(decodedHex);

			requestBuilder.contentMD5(md5Base64);
		}
		return amazonS3Client.putObject(requestBuilder.build(), RequestBody.fromInputStream(input, size));
	}

	@Override
	public PutObjectResponse putObject(String bucketName, String key, InputStream input, long size) throws S3Exception, DecoderException {
		return putObject(bucketName, key, input, size, null);
	}

	@Override
	public PutObjectResponse putObject(PutObjectRequest putObjectRequest, Path path) throws S3Exception {
		return amazonS3Client.putObject(putObjectRequest, path);
	}

	@Override
	public CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws S3Exception {
		CopyObjectRequest copyRequest = CopyObjectRequest.builder()
				.sourceBucket(sourceBucketName)
				.sourceKey(sourceKey)
				.destinationBucket(destinationBucketName)
				.destinationKey(destinationKey)
				.build();
		return amazonS3Client.copyObject(copyRequest).copyObjectResult();
	}

	@Override
	public void deleteObject(String bucketName, String key) throws S3Exception {
		amazonS3Client.deleteObject(dr -> dr.bucket(bucketName).key(key));
	}

	@Override
	public boolean exists(String bucketName, String key) throws S3Exception {
		try {
			amazonS3Client.headObject(hr -> hr.bucket(bucketName).key(key));
			return true;
		} catch (NoSuchKeyException e) {
			return false;
		}
	}


	@Override
	public String getString(String bucketName, String key) {
		try (InputStream is = getObject(bucketName, key)) {
			BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
			StringBuilder responseStrBuilder = new StringBuilder();

			String inputStr;
			while ((inputStr = streamReader.readLine()) != null) {
				responseStrBuilder.append(inputStr);
			}

			return responseStrBuilder.toString();
		} catch (IOException e) {
			throw new RuntimeException("Failed to load resource.", e);
		}
	}

}
