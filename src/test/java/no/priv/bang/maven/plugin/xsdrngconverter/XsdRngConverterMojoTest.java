package no.priv.bang.maven.plugin.xsdrngconverter;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.BeforeClass;
import org.junit.Test;

public class XsdRngConverterMojoTest {
    final static FilenameFilter rngFileFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".rng");
            }
        };
    private static Properties testProperties;

    @BeforeClass
    public static void readTestProperties() throws IOException {
        testProperties = new Properties();
        testProperties.load(XsdRngConverterMojoTest.class.getClassLoader().getResourceAsStream("test.properties"));
    }

    @Test
    public void testFindXsdFiles() {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();

        List<File> xsdFiles = mojo.findXsdFiles();
        assertEquals(1, xsdFiles.size());
    }

    @Test
    public void testConvertXsdFileNameToRngFileName() {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");
        List<File> xsdFiles = mojo.findXsdFiles();

        File rngFile = mojo.convertXsdFileNameToRngFileName(xsdFiles.get(0));

        assertThat(rngFile.getName(), endsWith(".rng"));
    }

    @Test
    public void testConvertXsdFilesToRngFiles() throws MojoExecutionException, MojoFailureException {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");

        mojo.execute();

        File[] rngFiles = new File(mojo.rngOutputDirectory).listFiles(rngFileFilter);
        assertEquals(1, rngFiles.length);
    }

}
