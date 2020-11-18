package org.ihtsdo.otf.resourcemanager;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
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
            this.amazonS3 = buildAmazonS3Client(resourceConfiguration);
        } else {
            this.resourceLoader = new FileSystemResourceLoader();
        }
    }

    /**
     * Builds the {@link AmazonS3} instance so that delete and move operations can occur on
     * objects inside S3.
     *
     * @param resourceConfiguration Which contains the configuration settings for the access
     *                              and secret key.
     * @return {@link AmazonS3} instance so that delete and movement operations can occur on
     * objects inside S3.
     */
    private AmazonS3 buildAmazonS3Client(final ResourceConfiguration resourceConfiguration) {
        final Cloud cloud = resourceConfiguration.getCloud();
        return AmazonS3ClientBuilder.standard()
                                    .withRegion(cloud.getRegion())
                                    .withCredentials(
                                            new AWSStaticCredentialsProvider(
                                                    new BasicAWSCredentials(cloud.getAccessKey(),
                                                                            cloud.getSecretKey())))
                                    .build();
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
            try (final OutputStream outputStream = writeResourceStream(resourcePath);
                 final InputStream inputStream = resourceInputStream) {
                StreamUtils.copy(inputStream, outputStream);
            }
        } catch (AmazonS3Exception e) {
            throw new IOException("Failed to write resource '" + resourcePath + "'.", e);
        }
    }

    /**
     * Writes the data to the given resource path location and returns the
     * {@code OutputStream}.
     *
     * @param resourcePath Which points to the resource that is going
     *                     to have the write operation performed on it.
     * @return an {@code OutputStream} which is the result after getting
     * the resource from the given path.
     * @throws IOException If an error occurs while trying to get the
     *                     {@code OutputStream}.
     */
    public OutputStream writeResourceStream(final String resourcePath) throws IOException {
        writeCheck();
        final String fullPath = getFullPath(resourcePath);
        if (!resourceConfiguration.isUseCloud()) new File(fullPath).getParentFile().mkdirs();
        try {
            return ((WritableResource) resourceLoader.getResource(fullPath)).getOutputStream();
        } catch (AmazonS3Exception e) {
            throw new IOException("Failed to write resource '" + resourcePath + "'.", e);
        }
    }

    /**
     * Deletes the specified object inside S3, given the
     * bucket name and object key.
     *
     * @param bucketName Where the object resides.
     * @param objectKey Key which is associated to the object
     *                  inside S3.
     * @throws IOException If an error occurs while trying to
     * delete the resource from S3.
     */
    public void deleteResource(final String bucketName,
                               final String objectKey) throws IOException {
        deleteResource(new DeleteObjectRequest(bucketName,
                                               objectKey));
    }

    /**
     * Deletes the specified object inside S3, given the
     * bucket name and object key.
     *
     * @param deleteObjectRequest Request which contains the
     *                            bucket name and object key.
     * @throws IOException If an error occurs while trying to
     * delete the resource from S3.
     */
    public void deleteResource(final DeleteObjectRequest deleteObjectRequest) throws IOException {
        try {
            checkCloudServiceIsConfigured();
            amazonS3.deleteObject(deleteObjectRequest);
        } catch (AmazonS3Exception e) {
            throw new IOException("Failed to delete the S3 object: '"
                                          + deleteObjectRequest.getBucketName() + "/"
                                          + deleteObjectRequest.getKey() + "'.", e);
        }
    }

    /**
     * Deletes the local resource from the specified resource path.
     *
     * @param resourcePath Which points to the resource that is going
     *                     to be deleted.
     * @throws IOException If an error occurs while trying to delete the
     *                     resource.
     */
    public boolean deleteResource(final String resourcePath) throws IOException {
        try {
            return Files.deleteIfExists(new File(getFullPath(resourcePath)).toPath());
        } catch (IOException e) {
            throw new IOException("Failed to delete resource '" + resourcePath + "'.", e);
        }
    }

    /**
     * Moves the resource inside S3 from the source, to the
     * destination location. After the move operation has
     * been performed, it will delete the resource from the
     * source location.
     *
     * @param sourceBucketName The name of the source bucket.
     * @param sourceKey The key of the source object.
     * @param destinationBucketName The name of the destination bucket.
     * @param destinationKey The key of the destination object.
     * @throws IOException If an error occurs while trying to move the
     * resource inside S3 from the source, to the destination location.
     */
    public void moveResource(final String sourceBucketName,
                             final String sourceKey,
                             final String destinationBucketName,
                             final String destinationKey) throws IOException {
        moveResource(new CopyObjectRequest(sourceBucketName, sourceKey, destinationBucketName, destinationKey));
    }

    /**
     * Moves the resource inside S3 from the source, to the
     * destination location. After the move operation has
     * been performed, it will delete the resource from the
     * source location.
     *
     * @param copyObjectRequest Request which contains the source
     *                          bucket name, source object key,
     *                          destination bucket name and destination
     *                          object key.
     * @throws IOException If an error occurs while trying to move the
     * resource inside S3 from the source, to the destination location.
     */
    public void moveResource(final CopyObjectRequest copyObjectRequest) throws IOException {
        try {
            checkCloudServiceIsConfigured();
            amazonS3.copyObject(copyObjectRequest);
            deleteResource(copyObjectRequest.getSourceBucketName(), copyObjectRequest.getSourceKey());
        } catch (AmazonS3Exception e) {
            throw new IOException("Failed to move resource from '" + copyObjectRequest.getSourceBucketName() +
                                          "' to '" + copyObjectRequest.getDestinationBucketName() + "'.", e);
        }
    }

    /**
     * Moves the resource locally from the specified resource path, to the desired
     * resource location.
     *
     * @param fromResourcePath    Path to the current/old resource location.
     * @param toResourcePath      Path to the new resource location.
     * @throws IOException If an error occurs while trying to move the resource
     *                     from the specified path, to the desired location.
     */
    public void moveResource(final String fromResourcePath,
                             final String toResourcePath) throws IOException {
        try {
            Files.move(new File(getFullPath(fromResourcePath)).toPath(),
                       new File(getFullPath(toResourcePath)).toPath(),
                       StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IOException("Failed to move resource from '" + fromResourcePath + "' to '" + toResourcePath + "'.", e);
        }
    }

    /**
     * Checks to make sure the resource configuration is pointing
     * to cloud: <code>useCloud == true</code>. This check occurs
     * before deletion/movement operation occur on S3 objects.
     *
     */
    private void checkCloudServiceIsConfigured() {
        if (!resourceConfiguration.isUseCloud()) {
            throw new UnsupportedOperationException("A valid AmazonS3 client must be created to perform this operation.");
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
