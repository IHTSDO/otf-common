package org.ihtsdo.otf.dao.s3;
import io.awspring.cloud.s3.ObjectMetadata;
import org.ihtsdo.otf.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;

/**
 * Offers an offline version of S3 cloud storage for testing or working offline.
 * N.B. Metadata and ACL security are not implemented.
 */
public class OfflineS3ClientImpl implements S3Client, TestS3Client {

	private final File bucketsDirectory;

	private static final boolean REPLACE_SEPARATOR = !File.pathSeparator.equals("/");
	private static final Logger LOGGER = LoggerFactory.getLogger(OfflineS3ClientImpl.class);

	public OfflineS3ClientImpl(String bucketsDirectoryPath) throws IOException {
		File bucketsDirectory;
		if (bucketsDirectoryPath != null && !bucketsDirectoryPath.isEmpty()) {
			bucketsDirectory = new File(bucketsDirectoryPath);
		} else {
			bucketsDirectory = newTempStore();
		}
		this.bucketsDirectory = bucketsDirectory;
	}

	public OfflineS3ClientImpl(File bucketsDirectory) {
		this.bucketsDirectory = bucketsDirectory;
	}

	public OfflineS3ClientImpl() throws IOException {
		this(newTempStore());
	}

	public static File newTempStore() throws IOException {
		return Files.createTempDirectory(OfflineS3ClientImpl.class.getName() + "-mock-s3").toFile();
	}

	@Override
	public void createBucket(String bucketName) {
		getBucket(bucketName);
	}

	@Override
	public ListObjectsResponse listObjects(String bucketName, String prefix) throws S3Exception {
		String searchLocation = getPlatformDependantPath(prefix);
		// Go up a directory, prefix could include partial filename
		if (searchLocation.indexOf("/") > 1) {
			searchLocation = searchLocation.substring(0, searchLocation.lastIndexOf("/"));
		} else {
			searchLocation = "";
		}
		File searchStartDir;

		File bucket = getBucket(bucketName);
		searchStartDir = new File(bucket, searchLocation);

		ListObjectsResponse.Builder responseBuilder = ListObjectsResponse.builder();
		ArrayList<S3Object> s3Objects = new ArrayList<>();
		if (searchStartDir.isDirectory()) {
			Collection<File> list = org.apache.commons.io.FileUtils.listFiles(searchStartDir, null, true); //No filter files, yes search recursively
			if (list != null) {
				for (File file : list) {
					String key = getRelativePathAsKey(bucketName, file);
					if (key.startsWith(prefix)) {
						s3Objects.add(S3Object.builder().key(key).build());
					}
				}
			}
		}
		s3Objects.sort(Comparator.comparing(S3Object::key));
		return responseBuilder.contents(s3Objects).build();
	}

	@Override
	public ListObjectsResponse listObjects(ListObjectsRequest listObjectsRequest) throws S3Exception {
		return listObjects(listObjectsRequest.bucket(), listObjectsRequest.prefix());
	}

	@Override
	public ResponseInputStream<GetObjectResponse> getObject(String bucketName, String key) {
		File file = getFile(bucketName, key);
		if (file.isFile()) {
			try {
				return new ResponseInputStream<>(GetObjectResponse.builder().contentLength(file.length()).build(), new FileInputStream(file));
			} catch (FileNotFoundException e) {
				throw S3Exception.builder().message("Object does not exist.").statusCode(404).build();
			}
		} else {
			throw S3Exception.builder().message("Object does not exist.").statusCode(404).build();
		}

	}


	@Override
	public PutObjectResponse putObject(String bucketName, String key, File file) throws S3Exception {
		return putObject(bucketName, key, getInputStream(file),file.length(), null);
	}

	@Override
	public PutObjectResponse putObject(String bucketName, String key, byte[] bytes) throws S3Exception {
		File tempFile = null;
		try {
			tempFile = File.createTempFile("file-", ".tmp", null);
			FileOutputStream fos = new FileOutputStream(tempFile);
			fos.write(bytes);
			return putObject(bucketName, key, tempFile);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			if (tempFile != null) {
				boolean deletedOK = tempFile.delete();
				if (!deletedOK) {
					throw S3Exception.builder().message("Failed to delete " + tempFile.getAbsoluteFile()).build();
				}
			}
		}
	}

	@Override
	public PutObjectResponse putObject(String bucketName, String key, InputStream inputStream, ObjectMetadata metadata, long size) throws S3Exception {
		File outFile = getFile(bucketName, key);

		// Create the target directory
		outFile.getParentFile().mkdirs();
		LOGGER.info("Offline file location {}", outFile.getAbsolutePath());
		//For ease of testing, if we're writing the final results (eg a zip file) we'll output the full path to STDOUT
		String outputFilePath = outFile.getAbsolutePath();
		if (FileUtils.isZip(outputFilePath)) {
			LOGGER.info("Writing out local results file to {}", outputFilePath);
		}

		if (inputStream != null) {
			try {
				//As per the online implementation, if the file is already there we will overwrite it.
				Files.copy(inputStream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				throw S3Exception.builder().message(String.format("Failed to store object, bucket:%s, objectKey:%s", bucketName, key)).cause(e).build();
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {
					// Just log
					LOGGER.error("Failed to close stream.", e);
				}
			}
		} else {
			throw S3Exception.builder().message("Failed to store object, no input given.").build();
		}


		// For the offline implementation we'll just copy the incoming MD5 and say we received the same thing
		PutObjectResponse.Builder responseBuilder = PutObjectResponse.builder();
		if (metadata != null) {
			responseBuilder.sseCustomerKeyMD5(metadata.getSseCustomerKeyMD5()).build();
		}
		return responseBuilder.build();
	}

	@Override
	public PutObjectResponse putObject(String bucketName, String key, InputStream input, long size) throws S3Exception {
		return putObject(bucketName, key, input, null, size);
	}

	@Override
	public PutObjectResponse putObject(String bucketName, String key, InputStream input, long size, String md5) throws S3Exception {
		return putObject(bucketName, key, input, ObjectMetadata.builder().sseCustomerKeyMD5(md5).build(), size);
	}

	@Override
	public PutObjectResponse putObject(PutObjectRequest putObjectRequest, Path path) throws S3Exception {
		return putObject(putObjectRequest.bucket(), putObjectRequest.key(), path.toFile());
	}

	@Override
	public PutObjectResponse putObject(String bucketName, String key, InputStream input) throws S3Exception {
		return putObject(bucketName, key, input, null, Long.MAX_VALUE);
	}

	private InputStream getInputStream(File inFile) {
		if (inFile != null && inFile.isFile()) {
			try {
				return new FileInputStream(inFile);
			} catch (FileNotFoundException e) {
				throw S3Exception.builder().message(String.format("File not found:%s", inFile.getAbsoluteFile())).cause(e).build();
			}
		}
		return null;
	}

	@Override
	public CopyObjectResult copyObject(String sourceBucketName, String sourceKey, String destinationBucketName, String destinationKey) throws S3Exception {
		ResponseInputStream<GetObjectResponse> sourceInput = getObject(sourceBucketName, sourceKey);
		putObject(destinationBucketName, destinationKey, sourceInput, sourceInput.response().contentLength(), null);
		CopyObjectResult.Builder builder = CopyObjectResult.builder();
		builder.lastModified(Instant.now());
		return builder.build();
	}

	@Override
	public void deleteObject(String bucketName, String key) throws S3Exception {
		File file = getFile(bucketName, key);

		//Are we deleting a file or a directory?
		if (file.isDirectory()) {
			try {
				LOGGER.warn("Deleting directory {}.", file.getAbsoluteFile());
				org.apache.commons.io.FileUtils.deleteDirectory(file);
			} catch (IOException e) {
				throw S3Exception.builder().message("Failed to delete directory: " + file.getAbsolutePath()).cause(e).build();
			}
		} else if (file.isFile()) {
			LOGGER.debug("Deleting file {}.", file.getAbsoluteFile());
			boolean deletedOK = file.delete();
			if (!deletedOK) {
				throw S3Exception.builder().message("Failed to delete " + file.getAbsoluteFile()).build();
			}
		} else {
			//Does it, in fact, not exist already? No foul if so
			if (!file.exists()) {
				throw S3Exception.builder().message("Attempted to delete entity, but it does not exist: " + file.getAbsoluteFile()).build();
			} else {
				throw S3Exception.builder().message("Encountered unexpected thing: " + file.getAbsolutePath()).build();
			}
		}
	}

	@Override
	public boolean exists(String bucketName, String key) throws S3Exception {
		return getFile(bucketName, key).exists();
	}

	@Override
    public void freshBucketStore() throws IOException {
		if (bucketsDirectory.exists() && bucketsDirectory.isDirectory()) {
			Files.walk(bucketsDirectory.toPath())
					.sorted(Comparator.reverseOrder())
					.map(Path::toFile)
					.forEach(File::delete);
		}
	}

	private File getBucket(String bucketName) {
		File bucket = new File(bucketsDirectory, bucketName);

		//Is bucket there already, or do we need to create it?
		if (!bucket.isDirectory()) {
			//Attempt to create - will fail if file already exists at that location.
			boolean success = bucket.mkdirs();
			if (!success && !bucket.exists()) {
				throw S3Exception.builder().message("Could neither find nor create Bucket at: " + bucketsDirectory + File.separator + bucketName).build();
			}
		}
		return bucket;
	}

	private File getFile(String bucketName, String key) {
		//Limitations on length of filename mean we have to use the slashed elements in the key as a directory path, unlike in the online implementation
		File bucket = getBucket(bucketName);
		key = getPlatformDependantPath(key);
		return new File(bucket, key);
	}

	/**
	 * @param file file to get the relative path of
	 * @return The path relative to the bucket directory and bucket
	 */
	private String getRelativePathAsKey(String bucketName, File file) {
		String absolutePath = file.getAbsolutePath();
		int relativeStart = bucketsDirectory.getAbsolutePath().length() + bucketName.length() + 2; //Take off the slash between bucketDirectory and final slash
		String relativePath = absolutePath.substring(relativeStart);
		relativePath = getPlatformIndependentPath(relativePath);
		return relativePath;
	}

	private String getPlatformDependantPath(String path) {
		if (REPLACE_SEPARATOR) {
			path = path.replace('/', File.separatorChar);
		}
		return path;
	}

	private String getPlatformIndependentPath(String path) {
		if (REPLACE_SEPARATOR) {
			path = path.replace(File.separatorChar, '/');
		}
		return path;
	}

	@Override
	public String getString(String bucketName, String key) {
		String result;
		try {
			File file = getFile(bucketName, key);
			result = org.apache.commons.io.FileUtils.readFileToString(file, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new RuntimeException("Failed to get string from file", e);
		}
		return result;
	}

}
