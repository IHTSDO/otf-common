package org.ihtsdo.otf.dao.s3;

import io.awspring.cloud.s3.ObjectMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StreamUtils;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

public class OfflineS3ClientImplTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OfflineS3ClientImplTest.class);

	public static final String TEST_FILE_TXT = "testFile.txt";
	private S3Client s3Client;
	private List<InputStream> streamsToClose;

	private static final String TEST_BUCKET = "test-bucket";
	private long fileSize;

	@BeforeEach
	public void setup() throws IOException {
		streamsToClose = new ArrayList<>();
		Path tempDirectory = Files.createTempDirectory(getClass().getName());
		assertTrue(new File(tempDirectory.toFile(), TEST_BUCKET).mkdirs());
		s3Client = new OfflineS3ClientImpl(tempDirectory.toFile());
		assertNotNull(getTestFileStream());
		fileSize = getTestFile().length();
	}

	@Test
	public void testListObjects() throws IOException {
		String productDir = "products/123/";
		assertEquals(0, s3Client.listObjects(TEST_BUCKET, productDir).contents().size());

		s3Client.putObject(TEST_BUCKET, productDir + "execA/file1.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "exec1/file1.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "exec1/file2.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "execZ/file1.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "exec2/file2.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "exec2/file1.txt", getTestFileStream(), fileSize);

		List<S3Object> objectSummaries = s3Client.listObjects(TEST_BUCKET, productDir).contents();
		assertEquals(6, objectSummaries.size());
		assertEquals("products/123/exec1/file1.txt", objectSummaries.get(0).key());
		assertEquals("products/123/exec1/file2.txt", objectSummaries.get(1).key());
		assertEquals("products/123/exec2/file1.txt", objectSummaries.get(2).key());
		assertEquals("products/123/exec2/file2.txt", objectSummaries.get(3).key());
		assertEquals("products/123/execA/file1.txt", objectSummaries.get(4).key());
		assertEquals("products/123/execZ/file1.txt", objectSummaries.get(5).key());
	}

	@Test
	public void testListObjectsWithPartFilenamePrefix() throws IOException {
		String prefix = "products/123/exec1/file";
		assertEquals(0, s3Client.listObjects(TEST_BUCKET, prefix).contents().size());

		s3Client.putObject(TEST_BUCKET, prefix + "1.txt", getTestFileStream(), getTestFile().length());
		s3Client.putObject(TEST_BUCKET, prefix + "2.txt", getTestFileStream(), getTestFile().length());

		List<S3Object> s3Objects = s3Client.listObjects(TEST_BUCKET, prefix).contents();
		assertEquals(2, s3Objects.size());
		assertEquals("products/123/exec1/file1.txt", s3Objects.get(0).key());
		assertEquals("products/123/exec1/file2.txt", s3Objects.get(1).key());
	}

	@Test
	public void testListObjectsUsingListObjectsRequest() throws IOException {
		String productDir = "products/123/";
		assertEquals(0, s3Client.listObjects(TEST_BUCKET, productDir).contents().size());

		s3Client.putObject(TEST_BUCKET, productDir + "execA/file1.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "exec1/file1.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "exec1/file2.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "execZ/file1.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "exec2/file2.txt", getTestFileStream(), fileSize);
		s3Client.putObject(TEST_BUCKET, productDir + "exec2/file1.txt", getTestFileStream(), fileSize);

		List<S3Object> s3Objects = s3Client.listObjects(TEST_BUCKET, productDir).contents();
		assertEquals(6, s3Objects.size());
		assertEquals("products/123/exec1/file1.txt", s3Objects.get(0).key());
		assertEquals("products/123/exec1/file2.txt", s3Objects.get(1).key());
		assertEquals("products/123/exec2/file1.txt", s3Objects.get(2).key());
		assertEquals("products/123/exec2/file2.txt", s3Objects.get(3).key());
		assertEquals("products/123/execA/file1.txt", s3Objects.get(4).key());
		assertEquals("products/123/execZ/file1.txt", s3Objects.get(5).key());
	}

	@Test
	public void testPutObjectGetObject() throws IOException {
		String productDir = "products/123/";
		String key = productDir + "execA/file1.txt";

		s3Client.putObject(TEST_BUCKET, key, getTestFileStream(), fileSize);

		List<S3Object> objectSummaries = s3Client.listObjects(TEST_BUCKET, "").contents();
		assertEquals(1, objectSummaries.size());
		assertEquals("products/123/execA/file1.txt", objectSummaries.get(0).key());

		InputStream objectContent = s3Client.getObject(TEST_BUCKET, key);
		assertNotNull(objectContent);
		assertTrue(objectContent.available() > 0);
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		assertEquals("Some content".trim(), content.trim());
	}

	@Test
	public void testPutObjectByFile() throws IOException {
		String productDir = "products/123/";
		String key = productDir + "execA/file1.txt";

		s3Client.putObject(TEST_BUCKET, key, getTestFile());

		InputStream objectContent = s3Client.getObject(TEST_BUCKET, key);
		assertNotNull(objectContent);
		assertTrue(objectContent.available() > 0);
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		assertEquals("Some content".trim(), content.trim());
	}

	@Test
	public void testCopyObject() throws IOException {
		String productDir = "products/123/";
		String key = productDir + "execA/file1.txt";

		// put first file
		s3Client.putObject(TEST_BUCKET, key, getTestFile());

		// get first file
		InputStream objectContent = s3Client.getObject(TEST_BUCKET, key);
		assertNotNull(objectContent);
		assertTrue(objectContent.available() > 0);
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		assertEquals("Some content".trim(), content.trim());

		// copy file
		String destinationKey = key + "2";
		s3Client.copyObject(TEST_BUCKET, key, TEST_BUCKET, destinationKey);

		// get copy
		InputStream object2Content = s3Client.getObject(TEST_BUCKET, key);

		// test copy
		assertNotNull(object2Content);
		assertTrue(object2Content.available() > 0);
		String content2 = StreamUtils.copyToString(object2Content, Charset.defaultCharset());
		assertEquals("Some content".trim(), content2.trim());
	}

	@Test
	public void testPutObjectByPutRequest() throws IOException {
		String productDir = "products/123/";
		String key = productDir + "execA/file1.txt";

		ObjectMetadata.Builder metadataBuilder = ObjectMetadata.builder();
		metadataBuilder.contentType("text/plain");
		s3Client.putObject(TEST_BUCKET, key, getTestFileStream(), metadataBuilder.build(), fileSize);
		InputStream objectContent = s3Client.getObject(TEST_BUCKET, key);
		assertNotNull(objectContent);
		assertTrue(objectContent.available() > 0);
		String content = StreamUtils.copyToString(objectContent, Charset.defaultCharset());
		assertEquals("Some content".trim(), content.trim());
	}

	@Test
	public void testPutObjectNoInput() {

		S3Exception thrown = assertThrows(S3Exception.class, () -> s3Client.putObject(TEST_BUCKET, "123", null, fileSize));
		assertEquals("Failed to store object, no input given.", thrown.getMessage());

	}

	@Test
	public void testDeleteNonExistentObject() {
		// now throws an AmazonServiceExecption when delete attempt fails.
		boolean exceptionDetected = false;
		try {
			s3Client.deleteObject(TEST_BUCKET, "file-does-not-exist.txt");
		} catch (S3Exception ase) {
			exceptionDetected = true;
		}
		
		assertTrue(exceptionDetected, "Expected to see exception thrown when attempting to delete non-existant object");
	}

	@Test
	public void testDeleteObject() throws IOException {
		String productDir = "products/123/";
		String key = productDir + "execA/file1.txt";
		s3Client.putObject(TEST_BUCKET, key, getTestFileStream(), fileSize);
		List<S3Object> objectSummaries = s3Client.listObjects(TEST_BUCKET, "").contents();
		assertEquals(1, objectSummaries.size());
		assertEquals("products/123/execA/file1.txt", objectSummaries.get(0).key());

		s3Client.deleteObject(TEST_BUCKET, key);

		assertEquals(0, s3Client.listObjects(TEST_BUCKET, "").contents().size());
	}

	@AfterEach
	public void tearDown() {
		for (InputStream inputStream : streamsToClose) {
			try {
				inputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private InputStream getTestFileStream() throws IOException {
		InputStream stream = getClass().getResourceAsStream(TEST_FILE_TXT);
		if (stream == null) {
			throw new IOException("Unable to open test resource " + TEST_FILE_TXT);
		}
		streamsToClose.add(stream);
		return stream;
	}

	private File getTestFile() {
		File testFile = new File(Objects.requireNonNull(getClass().getResource(TEST_FILE_TXT)).getFile());
		if (!testFile.exists()) {
			LOGGER.warn("Failed to recover test resource from: " + Objects.requireNonNull(getClass().getResource(".")).getPath());
		}
		return testFile;
	}

}
