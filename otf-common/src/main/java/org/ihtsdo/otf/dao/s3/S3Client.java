package org.ihtsdo.otf.dao.s3;

import io.awspring.cloud.s3.ObjectMetadata;
import org.apache.commons.codec.DecoderException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;

public interface S3Client {

	ListObjectsResponse listObjects(String bucketName, String prefix) throws S3Exception;

	ListObjectsResponse listObjects(ListObjectsRequest listObjectsRequest) throws S3Exception;

	ResponseInputStream<GetObjectResponse> getObject(String bucketName, String key) throws S3Exception;

	PutObjectResponse putObject(String bucketName, String key, File file) throws S3Exception;

	PutObjectResponse putObject(String bucketName, String key, byte[] bytes) throws S3Exception;

	PutObjectResponse putObject(String bucketName, String key, InputStream input, ObjectMetadata metadata, long size) throws S3Exception;

	PutObjectResponse putObject(String bucketName, String key, InputStream input, long size, String md5) throws S3Exception, DecoderException;

	PutObjectResponse putObject(String bucketName, String key, InputStream input, long size) throws S3Exception;

	PutObjectResponse putObject(PutObjectRequest putObjectRequest, Path path) throws S3Exception;

	PutObjectResponse putObject(String bucketName, String key, InputStream input) throws S3Exception;

	CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws S3Exception;

	void deleteObject(String bucketName, String key) throws S3Exception;

	boolean exists(String bucketName, String key) throws S3Exception;

	String getString(String bucketName, String key);

}
