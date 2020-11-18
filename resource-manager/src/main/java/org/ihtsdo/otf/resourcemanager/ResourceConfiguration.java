package org.ihtsdo.otf.resourcemanager;

/**
 * Use this configuration class by extending it.
 * Create a class which extends ResourceConfiguration and add the Spring 'Configuration' and 'ConfigurationProperties' annotations.
 * The ConfigurationProperties annotation should include a prefix for your resource configuration.
 * For example a TasksResourceConfiguration with a prefix of 'tasks.storage' would load the following properties from your configuration:
 * - tasks.storage.readonly
 * - tasks.storage.useCloud
 * - tasks.storage.local.path
 * - tasks.storage.cloud.bucketName
 * - tasks.storage.cloud.path
 * - tasks.storage.cloud.accessKey
 * - tasks.storage.cloud.secretKey
 * - tasks.storage.cloud.region
 *
 * TasksResourceConfiguration would be autowired into your spring configuration and then passed to the constructor of ResourceManager.
 */
public abstract class ResourceConfiguration {

	private boolean readonly;
	private boolean useCloud;

	private Local local;
	private Cloud cloud;

	public boolean isReadonly() {
		return readonly;
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}

	public boolean isUseCloud() {
		return useCloud;
	}

	public void setUseCloud(boolean useCloud) {
		this.useCloud = useCloud;
	}

	public Local getLocal() {
		return local;
	}

	public void setLocal(Local local) {
		this.local = local;
	}

	public Cloud getCloud() {
		return cloud;
	}

	public void setCloud(Cloud cloud) {
		this.cloud = cloud;
	}

	@Override
	public String toString() {
		return "ResourceConfiguration{" +
				"readonly=" + readonly +
				", useCloud=" + useCloud +
				", local=" + local +
				", cloud=" + cloud +
				'}';
	}

	public static class Local {

		private String path;

		public Local() {
		}

		public Local(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = normalisePath(path);
		}

		@Override
		public String toString() {
			return "Local{" +
					"path='" + path + '\'' +
					'}';
		}
	}

	public static class Cloud {

		private String bucketName;
		private String path;
		private String accessKey;
		private String secretKey;
		private String region;

		public Cloud() {
		}

		public Cloud(final String bucketName,
					 final String path,
					 final String accessKey,
					 final String secretKey,
					 final String region) {
			this.bucketName = bucketName;
			this.path = path;
			this.accessKey = accessKey;
			this.secretKey = secretKey;
			this.region = region;
		}

		public String getBucketName() {
			return bucketName;
		}

		public void setBucketName(String bucketName) {
			this.bucketName = bucketName;
		}

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = normalisePath(path);
		}

		public String getAccessKey() {
			return accessKey;
		}

		public void setAccessKey(String accessKey) {
			this.accessKey = accessKey;
		}

		public String getSecretKey() {
			return secretKey;
		}

		public void setSecretKey(String secretKey) {
			this.secretKey = secretKey;
		}

		public String getRegion() {
			return region;
		}

		public void setRegion(String region) {
			this.region = region;
		}

		@Override
		public String toString() {
			return "Cloud{" +
					"bucketName='" + bucketName + '\'' +
					", path='" + path + '\'' +
					", accessKey='" + accessKey + '\'' +
					", secretKey='" + secretKey + '\'' +
					", region='" + region + '\'' +
					'}';
		}
	}

	static String normalisePath(String path) {
		if (path == null || path.isEmpty()) {
			return "";
		}
		if (path.charAt(0) == '/') {
			path = path.substring(1);
		}
		if (!path.isEmpty() && path.lastIndexOf("/") != path.length() - 1) {
			path += "/";
		}
		return path;
	}
}
