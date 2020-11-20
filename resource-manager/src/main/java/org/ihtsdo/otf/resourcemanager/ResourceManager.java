package org.ihtsdo.otf.resourcemanager;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration.Cloud;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.util.StreamUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Service to read, write, delete and move file resources from S3 or local
 * disk. Use spring-cloud-aws-autoconfigure dependency to autowire an S3 capable
 * ResourceLoader into your configuration.
 */
public class ResourceManager {

	private final ResourceConfiguration resourceConfiguration;
	private final ResourceLoader resourceLoader;
	private AmazonS3 amazonS3;

	/**
	 * Creates the {@link ResourceManager} with the corresponding <code>resourceConfiguration</code>
	 * and <code>cloudResourceLoader</code>. {@code NullPointerException} is thrown if
	 * either of the parameters are {@code null}.
	 *
	 * @param resourceConfiguration Used to get the resource configuration which details
	 *                              the local/cloud setting.
	 * @param cloudResourceLoader   The resource loader associated to the cloud
	 *                              service.
	 */
	public ResourceManager(final ResourceConfiguration resourceConfiguration,
						   final ResourceLoader cloudResourceLoader) throws NullPointerException {
		this.resourceConfiguration = Objects.requireNonNull(resourceConfiguration);
		if (resourceConfiguration.isUseCloud()) {
			this.resourceLoader = checkS3Connection(Objects.requireNonNull(cloudResourceLoader));
			this.amazonS3 = AmazonS3ClientBuilder.standard().build();
		} else {
			this.resourceLoader = new FileSystemResourceLoader();
		}
	}

	/**
	 * Checks to make sure that a GET request to the S3 bucket/path can be
	 * performed successfully. If not, it will throw an {@link
	 * com.amazonaws.services.s3.model.AmazonS3Exception}. Although, It will
	 * ignore the {@link org.apache.http.HttpStatus#SC_FORBIDDEN} status because
	 * an anonymous client may be used to access a partially public bucket.
	 *
	 * @param cloudResourceLoader Used to make sure requests can be performed
	 *                            correctly on the S3 bucket/path.
	 */
	private ResourceLoader checkS3Connection(final ResourceLoader cloudResourceLoader) {
		try {
			cloudResourceLoader.getResource(getFullPath("does-not-exist.txt")).exists();
		} catch (AmazonS3Exception e) {
			if (!e.getErrorCode().equals("403 Forbidden")) {
				throw e;
			}
		}
		return cloudResourceLoader;
	}

	/**
	 * Reads the resource from the given <code>resourcePath</code>.
	 *
	 * @param resourcePath Path to the resource.
	 * @return an {@code InputStream} which corresponds to the resource
	 * stream.
	 * @throws IOException If an error occurs while trying to load
	 *                     the resource.
	 */
	public InputStream readResourceStream(final String resourcePath) throws IOException {
		final String fullPath = getFullPath(resourcePath);
		try {
			return resourceLoader.getResource(fullPath).getInputStream();
		} catch (AmazonS3Exception e) {
			throw new IOException("Failed to load resource '" + fullPath + "'.", e);
		}
	}

	/**
	 * Reads the resource from the given <code>resourcePath</code>. It will
	 * return {@code null} if the {@code Resource} does not exist.
	 *
	 * @param resourcePath Path to the resource.
	 * @return an {@code InputStream} which corresponds to the resource stream.
	 * Otherwise, it will return {@code null} if the {@code Resource} does not
	 * exist.
	 * @throws IOException If an error occurs while trying to load the resource.
	 */
	public InputStream readResourceStreamOrNullIfNotExists(final String resourcePath) throws IOException {
		try {
			final Resource resource = resourceLoader.getResource(getFullPath(resourcePath));
			return resource.exists() ? resource.getInputStream() : null;
		} catch (AmazonS3Exception e) {
			throw new IOException("Failed to load resource '" + resourcePath + "'.", e);
		}
	}

	/**
	 * Writes the data to the given resource path location.
	 *
	 * @param resourcePath        Which points to the resource that is
	 *                            going to have the write operation performed
	 *                            on it.
	 * @param resourceInputStream Input stream which represents the converted
	 *                            object, encoded as bytes.
	 * @throws IOException If an error occurs while trying to write the
	 *                     input stream to the specified resource path.
	 */
	public void writeResource(final String resourcePath,
							  final InputStream resourceInputStream) throws IOException {
		try {
			try (final OutputStream outputStream = openWritableResourceStream(resourcePath);
				 final InputStream inputStream = resourceInputStream) {
				StreamUtils.copy(inputStream, outputStream);
			}
		} catch (AmazonS3Exception e) {
			throw new IOException("Failed to write resource '" + resourcePath + "'.", e);
		}
	}

	/**
	 * Retrieves the output stream, which will have the input stream written
	 * into it.
	 *
	 * @param resourcePath Location of the resource.
	 * @return {@code OutputStream} which is the content that can be written
	 * to.
	 * @throws IOException If an error occurs while trying to get the writable
	 *                     resource {@code OutputStream}.
	 */
	public OutputStream openWritableResourceStream(final String resourcePath) throws IOException {
		writeCheck();
		final String fullPath = getFullPath(resourcePath);
		if (!resourceConfiguration.isUseCloud()) {
			new File(fullPath).getParentFile().mkdirs();
		}
		try {
			return ((WritableResource) resourceLoader.getResource(fullPath)).getOutputStream();
		} catch (AmazonS3Exception e) {
			throw new IOException("Failed to retrieve writable resource '" + resourcePath + "'.", e);
		}
	}

	/**
	 * Deletes the specified object inside S3, given the
	 * bucket name and object key. This uses the {@code AmazonS3}
	 * client to perform this operation so a prerequisite is
	 * required that AWS credentials are provided, see {@link
	 * com.amazonaws.auth.DefaultAWSCredentialsProviderChain}.
	 * But if the configuration is pointing to local storage,
	 * it will delete the local resource from the specified
	 * resource path.
	 *
	 * @param resourcePath Which corresponds to the resource which
	 *                     is going to be deleted.
	 * @throws IOException If an error occurs while trying to delete
	 *                     the resource.
	 */
	public void deleteResource(final String resourcePath) throws IOException {
		try {
			if (resourceConfiguration.isUseCloud()) {
				amazonS3.deleteObject(resourceConfiguration.getCloud().getBucketName(),
									  getFullPath(resourcePath));
			} else {
				Files.deleteIfExists(new File(getFullPath(resourcePath)).toPath());
			}
		} catch (AmazonS3Exception e) {
			throw new IOException("Failed to delete the resource: '" + resourcePath + "'.", e);
		}
	}

	/**
	 * Moves the resource inside S3 from the source, to the
	 * destination location. After the move operation has
	 * been performed, it will delete the resource from the
	 * source location. This uses the {@code AmazonS3} client
	 * to perform this operation so a prerequisite is required
	 * that AWS credentials are provided, see {@link
	 * com.amazonaws.auth.DefaultAWSCredentialsProviderChain}.
	 * But if the configuration is pointing to local storage,
	 * it will move the resource locally from the specified resource
	 * path, to the desired resource location.
	 *
	 * @param fromResourcePath The original/current location of
	 *                         the resource.
	 * @param toResourcePath   The desired location for which the
	 *                         resource will reside.
	 * @throws IOException If an error occurs while trying to
	 *                     move the resource from the specified path, to the desired
	 *                     location.
	 */
	public void moveResource(final String fromResourcePath,
							 final String toResourcePath) throws IOException {
		try {
			if (resourceConfiguration.isUseCloud()) {
				s3MoveResource(fromResourcePath, toResourcePath);
			} else {
				localMoveResource(fromResourcePath, toResourcePath);
			}
		} catch (AmazonS3Exception e) {
			throw new IOException("Failed to move resource from '" +
										  fromResourcePath + "' to '" +
										  toResourcePath + "'.", e);
		}
	}

	/**
	 * Moves the resource locally from the specified resource
	 * path, to the desired resource location.
	 *
	 * @param fromResourcePath The original/current location of
	 *                         the resource.
	 * @param toResourcePath   The desired location for which the
	 *                         resource will reside.
	 * @throws IOException If an error occurs while trying to
	 *                     move the resource from the specified path, to the desired
	 *                     location.
	 */
	private void localMoveResource(final String fromResourcePath,
								   final String toResourcePath) throws IOException {
		Files.move(new File(getFullPath(fromResourcePath)).toPath(),
				   new File(getFullPath(toResourcePath)).toPath(),
				   StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * Moves the resource inside S3 from the source, to the
	 * destination location. After the move operation has
	 * been performed, it will delete the resource from the
	 * source location.
	 *
	 * @param fromResourcePath The original/current location of
	 *                         the resource.
	 * @param toResourcePath   The desired location for which the
	 *                         resource will reside.
	 * @throws IOException If an error occurs while trying to
	 *                     move the resource from the specified path, to the desired
	 *                     location.
	 */
	private void s3MoveResource(final String fromResourcePath,
								final String toResourcePath) throws IOException {
		final String bucketName = resourceConfiguration.getCloud().getBucketName();
		amazonS3.copyObject(bucketName, getFullPath(fromResourcePath), bucketName, getFullPath(toResourcePath));
		deleteResource(fromResourcePath);
	}

	/**
	 * Checks to make sure that the resource configuration setting
	 * <code>readonly</code> is {@code false} when performing write
	 * operations. It will throw {@code UnsupportedOperationException}
	 * if a write operation is performed when the configuration setting
	 * is set to <code>readonly == true</code>.
	 *
	 * @see ResourceConfiguration#isReadonly()
	 */
	private void writeCheck() {
		if (resourceConfiguration.isReadonly()) {
			throw new UnsupportedOperationException("Can not write resources in this read-only resource manager.");
		}
	}

	/**
	 * Returns the full specified path, which either corresponds to the
	 * local/cloud storage.
	 *
	 * @param relativePath Being examined.
	 * @return the full specified path, which either corresponds to the
	 * local/cloud storage.
	 */
	private String getFullPath(final String relativePath) {
		return resourceConfiguration.isUseCloud() ?
				getCloudPath(relativePath) :
				getPathAndRelative(resourceConfiguration.getLocal().getPath(), relativePath);
	}

	/**
	 * Builds the cloud path so that it can point to the correct bucket
	 * and object inside the cloud storage.
	 *
	 * @param relativePath Being examined.
	 * @return cloud path so that it can point to the correct bucket
	 * and object inside the cloud storage.
	 */
	private String getCloudPath(final String relativePath) {
		final Cloud cloud = resourceConfiguration.getCloud();
		return String.format("s3://%s/%s",
							 cloud.getBucketName(),
							 getPathAndRelative(cloud.getPath(), relativePath));
	}

	/**
	 * Configures the non relative/relative paths and appends them
	 * together, joint with a forward-slash in between them.
	 *
	 * @param path         Being examined.
	 * @param relativePath Being examined.
	 * @return path which is built using the non relative/relative paths.
	 */
	private String getPathAndRelative(final String path,
									  final String relativePath) {
		return configurePath(path) + configureRelativePath(relativePath);
	}

	/**
	 * Configures the relative path so that the returned path
	 * is missing the forward-slash at the beginning of the
	 * relative path. If it does not start with a forward-slash,
	 * it will just simply return the relative path.
	 *
	 * @param relativePath Which is having the leading forward-slash
	 *                     striped.
	 * @return path which has the leading forward-slash removed.
	 */
	private String configureRelativePath(final String relativePath) {
		return relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
	}

	/**
	 * Configures the path so that if it does not end with
	 * a '/', that it will append it onto the end of the
	 * path. If it ends with a forward-slash already,
	 * it will just simply return the path.
	 *
	 * @param path Being examined.
	 * @return path which contains a forward-slash at the end.
	 */
	private String configurePath(final String path) {
		return !path.isEmpty() && !path.endsWith("/") ? path + "/" : path;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ResourceManager that = (ResourceManager) o;
		return Objects.equals(resourceConfiguration, that.resourceConfiguration) &&
				Objects.equals(resourceLoader, that.resourceLoader) &&
				Objects.equals(amazonS3, that.amazonS3);
	}

	@Override
	public int hashCode() {
		return Objects.hash(resourceConfiguration, resourceLoader, amazonS3);
	}
}
