package org.snomed.otf.script.dao;

import io.awspring.cloud.s3.InMemoryBufferingS3OutputStreamProvider;
import io.awspring.cloud.s3.PropertiesS3ObjectContentTypeResolver;
import io.awspring.cloud.s3.S3Resource;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.ClassUtils;
import org.springframework.core.io.Resource;

import software.amazon.awssdk.services.s3.S3Client;

public class SimpleStorageResourceLoader implements ResourceLoader, InitializingBean {

	private final S3Client s3Client;
	private final ResourceLoader delegate;

	/**
	 * <b>IMPORTANT:</b> If a task executor is set with an unbounded queue there will be a huge memory consumption. The
	 * reason is that each multipart of 5MB will be put in the queue to be uploaded. Therefore a bounded queue is recommended.
	 */
	private TaskExecutor taskExecutor;

	public SimpleStorageResourceLoader(S3Client s3Client, ResourceLoader delegate) {
		this.s3Client = s3Client;
		this.delegate = delegate;
	}

	public SimpleStorageResourceLoader(S3Client s3Client, ClassLoader classLoader) {
		this.s3Client = s3Client;
		this.delegate = new DefaultResourceLoader(classLoader);
	}

	public SimpleStorageResourceLoader(S3Client s3Client) {
		this(s3Client, ClassUtils.getDefaultClassLoader());
	}

	public void setTaskExecutor(TaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.taskExecutor == null) {
			this.taskExecutor = new SyncTaskExecutor();
		}
	}

	@Override
	public Resource getResource(String location) {
		if (SimpleStorageNameUtils.isSimpleStorageResource(location)) {
			return new S3Resource(location, s3Client, new InMemoryBufferingS3OutputStreamProvider(s3Client, new PropertiesS3ObjectContentTypeResolver()));
		}
		return this.delegate.getResource(location);
	}

	@Override
	public ClassLoader getClassLoader() {
		return this.delegate.getClassLoader();
	}
}
