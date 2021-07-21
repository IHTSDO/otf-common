package org.snomed.otf.script.dao;

import java.io.File;

import org.ihtsdo.otf.exception.TermServerScriptException;

public interface DataUploader {

	String getUploadLocation(String s3BucketProtocol, String s3BucketDomain);

	void upload(File s3ReportFile, File locals3ReportFile) throws TermServerScriptException;

}
