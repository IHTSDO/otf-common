package org.ihtsdo.otf.dao.resources;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.springframework.cloud.aws.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Service to read and write file resources from S3 or local disk.
 * Use spring-cloud-aws-autoconfigure dependency autowire an S3 capable ResourceLoader into your configuration.
 */
public class ResourceManager {

	private final ResourceConfiguration resourceConfiguration;
	private final ResourceLoader resourceLoader;

	public ResourceManager(ResourceConfiguration resourceConfiguration, ResourceLoader cloudResourceLoader) {
		this.resourceConfiguration = resourceConfiguration;
		if (resourceConfiguration.isUseCloud()) {
			// Check we can perform a get request to our S3 bucket and path
			String s3Path = getFullPath("does-not-exist.txt");
			cloudResourceLoader.getResource(s3Path).exists();
			resourceLoader = cloudResourceLoader;
		} else {
			resourceLoader = new FileSystemResourceLoader();
		}
	}

	public InputStream readResourceStream(String resourcePath) throws IOException {
		try {
			String fullPath = getFullPath(resourcePath);
			Resource resource = resourceLoader.getResource(fullPath);
			return resource.getInputStream();
		} catch (AmazonS3Exception e) {
			throw new IOException("Failed to load resource '" + resourcePath + "'.", e);
		}
	}

	public void writeResource(String resourcePath, InputStream resourceInputStream) throws IOException {
		try {
			try (OutputStream outputStream = writeResourceStream(resourcePath);
				 InputStream inputStream = resourceInputStream) {
				StreamUtils.copy(inputStream, outputStream);
			}
		} catch (AmazonS3Exception e) {
			throw new IOException("Failed to write resource '" + resourcePath + "'.", e);
		}
	}

	public OutputStream writeResourceStream(String resourcePath) throws IOException {
		writeCheck();
		String fullPath = getFullPath(resourcePath);
		if (!resourceConfiguration.isUseCloud()) {
			new java.io.File(fullPath).getParentFile().mkdirs();
		}
		try {
			Resource resource = resourceLoader.getResource(fullPath);
			WritableResource writableResource = (WritableResource) resource;
			return writableResource.getOutputStream();
		} catch (AmazonS3Exception e) {
			throw new IOException("Failed to write resource '" + resourcePath + "'.", e);
		}
	}

	private void writeCheck() {
		if (resourceConfiguration.isReadonly()) {
			throw new UnsupportedOperationException("Can not write resources in this read-only resource manager.");
		}
	}

	private String getFullPath(String relativePath) {
		if (resourceConfiguration.isUseCloud()) {
			ResourceConfiguration.Cloud cloud = resourceConfiguration.getCloud();
			return "s3://" + cloud.getBucketName() + "/" + cloud.getPath() + relativePath;
		}
		return resourceConfiguration.getLocal().getPath() + relativePath;
	}
}
