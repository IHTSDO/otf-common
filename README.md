# OTF-Common Library

Java library for use across SNOMED International Open Tooling Framework applications.

## Resource Manager
The resource manager provides a file storage abstraction layer allowing the storage mechanism to change via configuration.

### Example configuration
In the following example Spring Boot resource manager configuration `xx` can be anything but usually includes the application name and what the storage is for. For example `srs.build.storage`.
```
# If cloud storage (AWS S3) or local disk should be used.
xx.useCloud=false

# If the resource store should be accessed as read-only or read/write.
xx.readonly=false

# If local disk is used this is the path to the root of the store.
xx.local.path=store/builds

# If cloud storage is used this is the AWS S3 bucket name.
xx.cloud.bucketName=

# If cloud storage is used this is path to the root of the store within the S3 bucket.
xx.cloud.path=
```
