package org.ihtsdo.otf.resourcemanager;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.ihtsdo.otf.resourcemanager.ResourceConfiguration.Cloud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.utils.FileUtils;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.regions.providers.DefaultAwsRegionProviderChain;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service to read, write, delete and move file resources from S3 or local
 * disk. Use spring-cloud-aws-autoconfigure dependency to autowire an S3 capable
 * ResourceLoader into your configuration.
 */
public class ResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceManager.class);

	private final ResourceConfiguration resourceConfiguration;
	private final ResourceLoader resourceLoader;

	private S3Client s3Client;

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
			s3Client = S3Client.builder()
					.region(DefaultAwsRegionProviderChain.builder().build().getRegion())
					.build();

		} else {
			this.resourceLoader = new FileSystemResourceLoader();
		}
	}

	public ResourceManager(final ResourceConfiguration resourceConfiguration,
						   final ResourceLoader cloudResourceLoader, final S3Client s3Client) throws NullPointerException {
		this.resourceConfiguration = Objects.requireNonNull(resourceConfiguration);
		if (resourceConfiguration.isUseCloud()) {
			this.resourceLoader = checkS3Connection(Objects.requireNonNull(cloudResourceLoader));
			this.s3Client = s3Client;
		} else {
			this.resourceLoader = new FileSystemResourceLoader();
		}
	}

	/**
	 * Return concatenated bucketName and path.
	 *
	 * @return concatenated bucketName and path.
	 */
	public Optional<String> getBucketNamePath() {
		if (resourceConfiguration != null) {
			Cloud cloud = resourceConfiguration.getCloud();
			if (cloud != null) {
				String bucketName = cloud.getBucketName();
				String path = cloud.getPath();
				if (bucketName != null && path != null) {
					return Optional.of(bucketName + "/" + path);
				}
			}
		}
		return Optional.empty();
	}

	/**
	 * Checks to make sure that a GET request to the S3 bucket/path can be
	 * performed successfully. If not, it will throw an {@link
	 * software.amazon.awssdk.services.s3.model.S3Exception}. Although, It will
	 * ignore the {@link org.apache.http.HttpStatus#SC_FORBIDDEN} status because
	 * an anonymous client may be used to access a partially public bucket.
	 *
	 * @param cloudResourceLoader Used to make sure requests can be performed
	 *                            correctly on the S3 bucket/path.
	 */
	private ResourceLoader checkS3Connection(final ResourceLoader cloudResourceLoader) {
		try {
			cloudResourceLoader.getResource(getFullPath("does-not-exist.txt")).exists();
		} catch (S3Exception e) {
			if (e.statusCode() != 403) {
				throw e;
			}
		} catch (Exception e) {
			LOGGER.error("Failed to check S3 connection.");
			e.printStackTrace();
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
		} catch (FileNotFoundException e) {
			//We'll just allow a file not found exception to bubble up.  Anything else we'll annotate further.
			throw e;
		} catch (S3Exception | IOException e) {
			throw new IOException("Failed to load resource '" + fullPath + "'.", e);
		}
	}

	public File doReadResourceFile(String resourcePath) throws IOException {
		File tmpFile = FileUtils.doCreateTempFile(UUID.randomUUID() + ".zip");
		try (InputStream inputStream = this.readResourceStream(resourcePath)) {
			FileUtils.copyInputStreamToFile(inputStream, tmpFile);
			return tmpFile;
		} catch (IOException e) {
			return tmpFile;
		}
	}

	public Set<String> listCachedFilenames(String prefix) throws IOException {
		return listFilenames(prefix, true);
	}

	public String getCachePath() {
		return resourceConfiguration.getLocal().getPath();
	}

	/**
	 * Return a list of filenames for given resource
	 * @param prefix File prefix
	 * @return Set of all relevant filenames
	 * @throws IOException If an error occurs while trying to load the resource.
	 */
	public Set<String> listFilenames(String prefix) throws IOException {
		return listFilenames(prefix, false);
	}

	public Set<String> listFilenames() throws IOException {
		return listFilenames(null, false);
	}

	public Set<String> listFilenamesBySuffix(String suffix) throws IOException {
		Set<String> filenames = listFilenames(null, false);
		filenames.removeIf(f -> !f.endsWith(suffix));
		return filenames;
	}

	private Set<String> listFilenames(String prefix, boolean forceLocal) throws IOException {
		Set<String> fileNames = new HashSet<>();
		if (resourceConfiguration.isUseCloud() && !forceLocal) {
			try {
				Cloud cloud = resourceConfiguration.getCloud();
				//In case we're running on a PC we need to convert backslashes to forward
				String configPath = cloud.getPath().replaceAll("\\\\", "/");
				final String s3Path = configurePath(configPath);
				final String prefixPath = prefix == null || prefix.isEmpty() ? s3Path : s3Path + prefix;
				LOGGER.info("Listing file names in bucket {} with prefix {}", cloud.getBucketName(), prefixPath);
				ListObjectsRequest listObjectsRequest = ListObjectsRequest.builder().bucket(cloud.getBucketName()).prefix(prefixPath).maxKeys(10000).build();
				boolean done = false;
				while (!done) {
					ListObjectsResponse listObjectsResponse = s3Client.listObjects(listObjectsRequest);
					for (S3Object object : listObjectsResponse.contents()) {
						fileNames.add(object.key().substring(s3Path.length()));
					}
					if (Boolean.TRUE.equals(listObjectsResponse.isTruncated())) {
						String nextMarker = listObjectsResponse.contents().get(listObjectsResponse.contents().size() - 1).key();
						listObjectsRequest = ListObjectsRequest.builder().bucket(cloud.getBucketName()).prefix(prefixPath).maxKeys(10000).marker(nextMarker).build();
					} else {
						done = true;
					}
				}
			} catch (S3Exception e) {
				throw new IOException("Failed to determine existence of '" + prefix + "'.", e);
			}
		} else {
			String localPath = resourceConfiguration.getLocal().getPath();
			if (localPath.startsWith(ResourceLoader.CLASSPATH_URL_PREFIX)) {
				ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
				PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(classLoader);
				Resource[] resources;
				if (prefix == null || prefix.isEmpty()) {
					resources = resolver.getResources(configurePath(localPath) + "*.*");
				} else {
					resources = resolver.getResources(configurePath(localPath) + prefix + "*.*");
				}

				fileNames.addAll(Arrays.stream(resources)
						.map(Resource::getFilename)
						.filter(Objects::nonNull)
						.collect(Collectors.toSet()));
			} else {
				File[] files;
				if (prefix == null) {
					files = ResourceUtils.getFile(localPath).listFiles();
				} else {
					files = ResourceUtils.getFile(localPath).listFiles((FileFilter) new PrefixFileFilter(prefix, IOCase.INSENSITIVE));
				}
				Arrays.stream(files).forEach(file -> fileNames.add(file.getName()));
			}
		}

		return fileNames;
	}

	public Set<String> doListFilenames(String prefix, String suffix) {
		Set<String> fileNames = doListFilenames(prefix);
		fileNames.removeIf(p -> !p.endsWith(suffix));

		return fileNames;
	}

	public Set<String> doListFilenames(String prefix) {
		try {
			Set<String> packages = this.listFilenames(prefix);

			return packages;
		} catch (IOException e) {
			return Collections.emptySet();
		}
	}

	/**
	 * Determine existence of resource from the given <code>resourcePath</code>.
	 *
	 * @return boolean true if the resource exists
	 * @throws IOException If an error occurs while trying to examine
	 *                     the resource.
	 */
	public boolean doesObjectExist(final File resource) throws IOException {
		try {
			if (resourceConfiguration.isUseCloud()) {
				return resourceLoader.getResource(getFullPath(resource.getPath())).exists();
			} else {
				return Files.isReadable(resource.toPath());
			}
		} catch (S3Exception e) {
			throw new IOException("Failed to determine existence of '" + resource + "'.", e);
		}
	}

	public boolean doesObjectExist(String resourcePath) {
		try {
			return listFilenames().contains(resourcePath);
		} catch (IOException e) {
			return false;
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
		} catch (S3Exception e) {
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
		} catch (S3Exception e) {
			throw new IOException("Failed to write resource '" + resourcePath + "'.", e);
		}
	}

	public void doWriteResource(String resourcePath, InputStream resourceInputStream) throws IOException {
		try {
			this.writeResource(resourcePath, resourceInputStream);
		} catch (IOException e) {
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
			final Resource resource = resourceLoader.getResource(fullPath);
			final WritableResource writableResource = (WritableResource) resource;
			return writableResource.getOutputStream();
		} catch (S3Exception e) {
			throw new IOException("Failed to retrieve writable resource '" + resourcePath + "'.", e);
		}
	}

	/**
	 * Deletes the specified object inside S3, given the
	 * bucket name and object key. This uses the {@code AmazonS3}
	 * client to perform this operation so a prerequisite is
	 * required that AWS credentials are provided.
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
				final String path = resourceConfiguration.getCloud().getPath();
				s3Client.deleteObject(delete -> delete.bucket(resourceConfiguration.getCloud().getBucketName()).key((path != null ? path + "/" : "") + resourcePath));
			} else {
				Files.deleteIfExists(new File(getFullPath(resourcePath)).toPath());
			}
		} catch (S3Exception e) {
			throw new IOException("Failed to delete the resource: '" + resourcePath + "'.", e);
		}
	}

	public boolean doDeleteResource(String resourcePath) {
		try {
			this.deleteResource(resourcePath);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Moves the resource inside S3 from the source, to the
	 * destination location. After the move operation has
	 * been performed, it will delete the resource from the
	 * source location. This uses the {@code AmazonS3} client
	 * to perform this operation so a prerequisite is required
	 * that AWS credentials are provided.
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
				s3MoveResource(fromResourcePath, toResourcePath, true);
			} else {
				localMoveResource(fromResourcePath, toResourcePath);
			}
		} catch (S3Exception e) {
			throw new IOException("Failed to move resource from '" +
					fromResourcePath + "' to '" +
					toResourcePath + "'.", e);
		}
	}

	public void copyResource(final String fromResourcePath,
							 final String toResourcePath) throws IOException {
		try {
			if (resourceConfiguration.isUseCloud()) {
				s3MoveResource(fromResourcePath, toResourcePath, false);
			} else {
				localMoveResource(fromResourcePath, toResourcePath);
			}
		} catch (S3Exception e) {
			throw new IOException("Failed to move resource from '" +
					fromResourcePath + "' to '" +
					toResourcePath + "'.", e);
		}
	}

	public boolean doCopyResource(final String fromResourcePath,
								  final String toResourcePath) {
		try {
			this.copyResource(fromResourcePath, toResourcePath);
			return true;
		} catch (Exception e) {
			return false;
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
								final String toResourcePath,
								final boolean deleteResource) throws IOException {
		final String bucketName = resourceConfiguration.getCloud().getBucketName();
		final String path = resourceConfiguration.getCloud().getPath();
		s3Client.copyObject(copy -> copy.sourceBucket(bucketName)
				.sourceKey((path != null ? path + "/" : "") + fromResourcePath)
				.destinationBucket(bucketName)
				.destinationKey((path != null ? path + "/" : "") + toResourcePath));
		if (deleteResource) {
			deleteResource(fromResourcePath);
		}
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
		String cloudPath = String.format("s3://%s/%s",
				cloud.getBucketName(),
				getPathAndRelative(cloud.getPath(), relativePath));
		//In case we're running on a PC we need to convert backslashes to forward
		return cloudPath.replaceAll("\\\\", "/");
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
		if (path == null || path.isBlank()) {
			return "";
		}

		return !path.isEmpty() && !path.endsWith("/") ? path + "/" : path;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ResourceManager that = (ResourceManager) o;
		return Objects.equals(resourceConfiguration, that.resourceConfiguration) &&
				Objects.equals(resourceLoader, that.resourceLoader) &&
				Objects.equals(s3Client, that.s3Client);
	}

	@Override
	public int hashCode() {
		return Objects.hash(resourceConfiguration, resourceLoader, s3Client);
	}
}
