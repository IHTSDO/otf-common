package org.snomed.module.storage;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.model.Bucket;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class S3ClientIntegrationTest extends IntegrationTest {
    @Test
    public void testCanReadBucketThatExists() {
        // given
        String bucketName = "test-bucket";

        // when
        boolean success = s3ClientWrapper.createBucket(bucketName);
        Bucket bucket = s3ClientWrapper.readBucket(bucketName);

        // then
        assertTrue(success);
        assertNotNull(bucket);
    }

    @Test
    public void testCannotReadBucketThatDoesntExist() {
        // when
        boolean success = s3ClientWrapper.createBucket("test-1");
        Bucket bucket = s3ClientWrapper.readBucket("test-2");

        // then
        assertTrue(success);
        assertNull(bucket);
    }

    @Test
    public void testCanCreateBucketTwice() {
        // given
        s3ClientWrapper.createBucket("test-1");
        s3ClientWrapper.createBucket("test-2");

        // when
        List<Bucket> buckets = s3ClientWrapper.readBuckets();

        // then
        assertEquals(2, buckets.size());
    }

    @Test
    public void testCannotCreateBucketTwice() {
        // given
        s3ClientWrapper.createBucket("test");
        s3ClientWrapper.createBucket("test");

        // when
        List<Bucket> buckets = s3ClientWrapper.readBuckets();

        // then
        assertEquals(1, buckets.size());
    }

    @Test
    public void testCanDeleteBucket() {
        // given
        s3ClientWrapper.createBucket("test");
        assertEquals(1, s3ClientWrapper.readBuckets().size());

        // when
        boolean success = s3ClientWrapper.deleteBucket("test");

        // then
        assertTrue(success);
        assertEquals(0, s3ClientWrapper.readBuckets().size());
    }

    @Test
    public void testCannotDeleteNonExistentBucket() {
        // given
        s3ClientWrapper.createBucket("test-1");
        assertEquals(1, s3ClientWrapper.readBuckets().size());

        // when
        boolean success = s3ClientWrapper.deleteBucket("test-2");

        // then
        assertFalse(success);
        assertEquals(1, s3ClientWrapper.readBuckets().size());
    }
}
