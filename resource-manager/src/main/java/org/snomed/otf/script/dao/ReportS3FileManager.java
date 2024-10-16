package org.snomed.otf.script.dao;

import org.ihtsdo.otf.exception.TermServerScriptException;
import org.ihtsdo.otf.rest.client.terminologyserver.pojo.Project;
import org.ihtsdo.otf.utils.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snomed.otf.script.dao.transformer.DataTransformer;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReportS3FileManager extends ReportFileManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReportS3FileManager.class);

    // This is static for all environments
    public static final String S3_BUCKET_PROTOCOL = "https://";
    public static final String S3_BUCKET_DOMAIN = ".s3.amazonaws.com/";
    public static final String S3_DIRECTORY = "jobs/@REPORT_NAME@/runs/@BRANCH_PATH@/latest/";
    public static final String S3_SHEET = "sheet";
    public static final String BRANCH_PATH_KEY = "@BRANCH_PATH@";
    public static final String REPORT_NAME_KEY = "@REPORT_NAME@";

    private final DataBroker reportDataUploader;
    private final DataTransformer dataTransformer;

    // Do not delete as the worker is reset every time is starts.
    // So might as well keep the files
    private boolean deleteTempFiles = true;
    private boolean deleteLocalReports = false;

    private Map<File, File> localReportsToTransformedReports;
    private Map<File, File> transformedReportsToS3Reports;

    public ReportS3FileManager(ReportManager owner,
                               DataBroker dataUploader,
                               DataTransformer dataTransformer) {
        super(owner);
        this.reportDataUploader = dataUploader;
        this.dataTransformer = dataTransformer;
    }

    protected void postProcess() throws TermServerScriptException {
        localReportsToTransformedReports = new LinkedHashMap<>();
        transformedReportsToS3Reports = new LinkedHashMap<>();

        // the report has finished so do post processing
        try {
            // create the file mapping for the transformation/s
            mapReportsFilesToS3();

            // transform the report/s
            LOGGER.info("Transforming reports...");
            transformReports();

            // upload the file/s to s3
            LOGGER.info("Uploading reports to S3...");
            uploadReports();

            // delete the local reports
            deleteReports();
        } catch (Exception e) {
            String msg = "Failed to complete transform and upload the report" + ExceptionUtils.getExceptionCause("", e);
            throw new TermServerScriptException(msg);
        }
    }

    private void mapReportsFilesToS3() {
        String branchPath = getBranchPath();
        String reportName = getReportName();

        String baseSheetName = S3_DIRECTORY
                .replaceAll(REPORT_NAME_KEY, reportName)
                .replaceAll(BRANCH_PATH_KEY, branchPath);

        for (int index = 0; index < reportFiles.length; index++) {
            File localReportFile = reportFiles[index];
            File transformedFile = new File (localReportFile.getPath() + dataTransformer.getFileExtension());
            File s3File = new File(baseSheetName + S3_SHEET + (index+1) + dataTransformer.getFileExtension());

            localReportsToTransformedReports.put(localReportFile, transformedFile);
            transformedReportsToS3Reports.put(transformedFile, s3File);
        }
    }

    protected void transformReports() throws TermServerScriptException {
        // Process the report
        File localReportFile = null;
        File locals3ReportFile = null;
        for (Map.Entry<File, File> entry : localReportsToTransformedReports.entrySet()) {
            try {
                // transform form the reports
                localReportFile = entry.getKey();
                locals3ReportFile = entry.getValue();
                dataTransformer.transform(localReportFile, locals3ReportFile);
            } catch (Exception e) {
                String msg = "Failed to complete transform " +
                        (localReportFile != null ? localReportFile.getPath() : "")
                        + " to " +
                        (locals3ReportFile != null ? locals3ReportFile.getPath() : "");
                throw new TermServerScriptException(msg, e);
            }
        }
    }

    protected void uploadReports() throws TermServerScriptException {
        File locals3ReportFile = null;
        File s3ReportFile = null;
        for (Map.Entry<File, File> entry : transformedReportsToS3Reports.entrySet()) {
            try {
                // transform form the reports
                locals3ReportFile = entry.getKey();
                s3ReportFile = entry.getValue();
                String filePath = File.separator + s3ReportFile.getPath();
                String targetS3Path = reportDataUploader.getUploadLocation(S3_BUCKET_PROTOCOL, S3_BUCKET_DOMAIN) + filePath;
                LOGGER.info("Outputting Report to {}", cleanFileName(targetS3Path));
                reportDataUploader.upload(s3ReportFile, locals3ReportFile);
            } catch (Exception e) {
                String msg = "Failed to upload " +
                        (locals3ReportFile != null ? locals3ReportFile.getPath() : "") +
                        " to " + (s3ReportFile != null ? s3ReportFile.getPath() : "");
                throw new TermServerScriptException(msg, e);
            }
        }
    }

    private void deleteReports() {
        if (deleteTempFiles) {
            LOGGER.info("Deleting local report file...");
            deleteFiles(localReportsToTransformedReports.keySet().toArray(File[]::new));
        }
        if (deleteLocalReports) {
            LOGGER.info("Deleting local transformed report file...");
            deleteFiles(localReportsToTransformedReports.values().toArray(File[]::new));
        }
    }

    private String getBranchPath() {
        String branchPath = "";
        Project project = owner.getScript().getProject();
        if (project.getBranchPath() == null) {
            // we do not have branch so use whatever complex name the report says
            branchPath = owner.getScript().getReportComplexName();
        } else {
            branchPath = project.getBranchPath().replace("/", "|");
        }
        return branchPath;
    }

    protected void deleteFiles(File[] files) {
        if (files != null) {
            Arrays.stream(files).forEach(file -> {
                if (file != null && file.exists()) {
                    file.delete();
                }
            });
        }
    }

    // we clean the filenames up as depending on config and
    // different modes of operation sometimes you end up with multiple slashes
    private String cleanFileName(String filename) {
        if (filename != null) {
            File file = new File(filename); // this will remove the double separators and make consistent
            return file.getPath().replaceAll(":/", "://");
        }
        return "";
    }

    public String getURL() {
    	if (transformedReportsToS3Reports == null || transformedReportsToS3Reports.values() == null) {
    		return null;
    	}
        // get the path for all the reports (this is the way it's done for csv files)
        return cleanFileName(reportDataUploader.getUploadLocation(S3_BUCKET_PROTOCOL, S3_BUCKET_DOMAIN) +
                File.separator +
                transformedReportsToS3Reports.values().stream().findFirst().orElseThrow().getParentFile()) + File.separator;
    }

    public boolean isDeleteLocalReports() {
        return deleteLocalReports;
    }

    public void setDeleteLocalReports(boolean deleteLocalReports) {
        this.deleteLocalReports = deleteLocalReports;
    }

    public boolean isDeleteTempFiles() {
        return deleteTempFiles;
    }

    public void setDeleteTempFiles(boolean deleteTempFiles) {
        this.deleteTempFiles = deleteTempFiles;
    }

    public void deleteAllLocalFiles() {
        setDeleteLocalReports(true);
        setDeleteTempFiles(true);
    }
}
