package org.ihtsdo.termserver.scripting.dao;

import org.junit.Test;
import org.snomed.otf.script.dao.ReportConfiguration;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ReportConfigurationTest {

    @Test
    public void testValidCreation() {

        ReportConfiguration reportConfiguration =
                new ReportConfiguration("S3|GOOGLE",
                        "CSV");

        Set<ReportConfiguration.ReportOutputType> expectedReportOutputTypes = new HashSet<>();
        expectedReportOutputTypes.add(ReportConfiguration.ReportOutputType.S3);
        expectedReportOutputTypes.add(ReportConfiguration.ReportOutputType.GOOGLE);

        assertEquals(reportConfiguration.getReportOutputTypes(), expectedReportOutputTypes);
        assertTrue(reportConfiguration.isValid());

        Set<ReportConfiguration.ReportFormatType> expectedReportFormatTypes = new HashSet<>();
        expectedReportFormatTypes.add(ReportConfiguration.ReportFormatType.CSV);
        assertTrue(reportConfiguration.isValid());

        assertEquals(reportConfiguration.getReportFormatTypes(), expectedReportFormatTypes);

        assertTrue(reportConfiguration.isValid());
    }

    @Test
    public void testEmptyCreation() {
        ReportConfiguration reportConfiguration =
                new ReportConfiguration(null, "CSV");
        assertNull(reportConfiguration.getReportOutputTypes());
        assertFalse(reportConfiguration.isValid());

        reportConfiguration =
                new ReportConfiguration("", "CSV");
        assertNull(reportConfiguration.getReportOutputTypes());
        assertFalse(reportConfiguration.isValid());


        reportConfiguration =
                new ReportConfiguration("S3|GOOGLE", null);
        assertNull(reportConfiguration.getReportFormatTypes());
        assertFalse(reportConfiguration.isValid());

        reportConfiguration =
                new ReportConfiguration("S3|GOOGLE", "");
        assertNull(reportConfiguration.getReportFormatTypes());
        assertFalse(reportConfiguration.isValid());

        reportConfiguration =
                new ReportConfiguration("S3|GOOGLE", "");
        assertNull(reportConfiguration.getReportFormatTypes());
        assertFalse(reportConfiguration.isValid());
    }

    @Test
    public void testInvalidEnumValue() {
        ReportConfiguration reportConfiguration =
                new ReportConfiguration("S3|GOOGLE", "CSV_INVALID");

        Set<ReportConfiguration.ReportFormatType> expectedReportFormatTypes = new HashSet<>();
        expectedReportFormatTypes.add(ReportConfiguration.ReportFormatType.CSV);
        assertFalse(reportConfiguration.isValid());
    }

    @Test
    public void testConstructionWithEnumsDirectly() {
        ReportConfiguration reportConfiguration = new ReportConfiguration(
                ReportConfiguration.ReportOutputType.GOOGLE,
                ReportConfiguration.ReportFormatType.CSV);

        assertTrue(reportConfiguration.isValid());
    }
}
