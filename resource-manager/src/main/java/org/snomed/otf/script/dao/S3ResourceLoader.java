package org.snomed.otf.script.dao;

import com.amazonaws.services.s3.AmazonS3;
import org.springframework.core.io.ResourceLoader;

public interface S3ResourceLoader extends ResourceLoader {

	AmazonS3 getS3Client();
}
