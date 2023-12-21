package org.ihtsdo.otf.dao.s3.helper;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.io.IOUtils;
import org.ihtsdo.otf.dao.s3.S3Client;
import org.ihtsdo.otf.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FileHelper {

	private final S3Client s3Client;

	private final String bucketName;

	private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

	public FileHelper(String bucketName, @Autowired S3Client s3Client) {
		this.bucketName = bucketName;
		this.s3Client = s3Client;
	}

	public void putFile(InputStream fileStream, long fileSize, String targetFilePath) throws S3Exception {
		LOGGER.debug("Putting file to {}/{}", bucketName, targetFilePath);
		s3Client.putObject(bucketName, targetFilePath, fileStream, fileSize);
	}
	public void putFile(InputStream fileStream, String targetFilePath) {
		LOGGER.debug("Putting file to {}/{}", bucketName, targetFilePath);
		s3Client.putObject(bucketName, targetFilePath, fileStream);
	}

	public String putFile(File file, String targetFilePath) throws NoSuchAlgorithmException, IOException, DecoderException {
		return putFile(file, targetFilePath, false);
	}


	public String putFile(File file, String targetFilePath, boolean calcMD5) throws NoSuchAlgorithmException, IOException, DecoderException {

		InputStream is = new FileInputStream(file);
		String md5Received = "MD5 not received";
		try {
			String localMd5 = null;
			if (calcMD5) {
				localMd5 = FileUtils.calculateMD5(file);
			}
			PutObjectResponse response = s3Client.putObject(bucketName, targetFilePath, is, file.length(), localMd5);
			md5Received = (response == null ? null : response.sseCustomerKeyMD5());
			LOGGER.debug("S3Client put request returned MD5: " + md5Received);

			if (calcMD5) {
				// Also upload the hex encoded (ie normal) md5 digest in a file
				String md5TargetPath = targetFilePath + ".md5";
				File md5File = FileUtils.createMD5File(file, localMd5);
				InputStream isMD5 = new FileInputStream(md5File);
				s3Client.putObject(bucketName, md5TargetPath, isMD5, md5File.length(), null);
			}
		} finally {
			IOUtils.closeQuietly(is);
		}
		return md5Received;
	}

	public InputStream getFileStream(String filePath) {
		try {
			return s3Client.getObject(bucketName, filePath);
		} catch (S3Exception e) {
			if (404 != e.statusCode()) {
				throw e;
			}
		}
		return null;
	}

	public List<String> listFiles(String directoryPath) {
		List<String> files = new ArrayList<>();
		try {
			ListObjectsResponse objectListing = s3Client.listObjects(bucketName, directoryPath);
			for (S3Object s3Object : objectListing.contents()) {
				files.add(s3Object.key().substring(directoryPath.length()));
			}
		} catch (S3Exception e) {
			//Trying to list files in a directory that doesn't exist isn't a problem, we'll just return an empty array
			LOGGER.info("Probable attempt to get listing on non-existent directory: {} error {}", directoryPath, e.getLocalizedMessage());
		}
		return files;
	}

	// TODO: User logging against file actions?
	public void deleteFile(String filePath) {
		s3Client.deleteObject(bucketName, filePath);
	}

	/**
	 * Copies a file from one S3 location to another
	 * @param sourcePath source path
	 * @param targetPath target path
	 */
	public void copyFile(String sourcePath, String targetPath) {
		LOGGER.debug("Copy file '{}' to '{}'", sourcePath, targetPath);
		s3Client.copyObject(bucketName, sourcePath, bucketName, targetPath);
	}


	/**
	 * Copies a file from one S3 location to another
	 * @param sourcePath   source path
	 * @param targetBucket target bucket name
	 * @param targetPath   target path
	 */
	public void copyFile(String sourcePath, String targetBucket, String targetPath) {
		LOGGER.debug("Copy file '{}' to  bucket '{}' as file name'{}'", sourcePath, targetBucket, targetPath);
		s3Client.copyObject(bucketName, sourcePath, targetBucket, targetPath);
	}

	/**
	 * @param targetFilePath the path to check
	 * @return true if the target file actually exists in the fileStore (online or offline)
	 */
	public boolean exists(String targetFilePath) {
		return s3Client.exists(bucketName, targetFilePath);
	}

}
