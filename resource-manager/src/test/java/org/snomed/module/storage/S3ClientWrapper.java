package org.snomed.module.storage;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class S3ClientWrapper {
    private final S3Client s3Client;

    public S3ClientWrapper(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public boolean createBucket(String bucketName) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    public Bucket readBucket(String bucketName) {
        try {
            ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
            List<Bucket> buckets = listBucketsResponse.buckets();
            for (Bucket bucket : buckets) {
                if (bucketName.equals(bucket.name())) {
                    return bucket;
                }
            }

            return null;
        } catch (S3Exception e) {
            throw new RuntimeException("Failed to create bucket.", e);
        }
    }

    public List<Bucket> readBuckets() {
        try {
            return s3Client.listBuckets().buckets();
        } catch (S3Exception e) {
            return Collections.emptyList();
        }
    }

    public boolean deleteBucket(String bucketName) {
        try {
            Bucket existingBucket = readBucket(bucketName);
            if (existingBucket == null) {
                return false;
            }

            s3Client.deleteBucket(DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build());
            return true;
        } catch (S3Exception e) {
            return false;
        }
    }

    public List<String> readObjectKeys(String bucketName) {
        ListObjectsResponse listObjectsResponse = s3Client.listObjects(ListObjectsRequest.builder().bucket(bucketName).build());
        List<S3Object> contents = listObjectsResponse.contents();
        List<String> keys = new ArrayList<>();
        for (S3Object content : contents) {
            keys.add(content.key());
        }

        return keys;
    }

    public boolean deleteObject(String bucketName, String objectKey) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
