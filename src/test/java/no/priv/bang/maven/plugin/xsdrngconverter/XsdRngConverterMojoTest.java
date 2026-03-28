package no.priv.bang.maven.plugin.xsdrngconverter;


import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class XsdRngConverterMojoTest {
    final static FilenameFilter rngFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".rng");
        }
    };
    private static Properties testProperties;

    @BeforeAll
    public static void readTestProperties() throws IOException {
        testProperties = new Properties();
        testProperties.load(XsdRngConverterMojoTest.class.getClassLoader().getResourceAsStream("test.properties"));
    }

    @Test
    public void testFindXsdFiles() {
        var mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();

        var xsdFiles = mojo.findXsdFiles();
        assertThat(xsdFiles).hasSize(1);
    }

    @Test
    public void testConvertXsdFileNameToRngFileName() {
        var mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");
        var xsdFiles = mojo.findXsdFiles();

        var rngFile = mojo.convertXsdFileNameToRngFileName(xsdFiles.get(0));
        assertThat(rngFile.getName()).endsWith(".rng");
    }

    @Test
    public void testConvertXsdFilesToRngFiles() throws MojoExecutionException, MojoFailureException {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");

        mojo.execute();

        var rngFiles = new File(mojo.rngOutputDirectory).listFiles(rngFileFilter);
        assertThat(rngFiles).hasSize(1);
    }

}
