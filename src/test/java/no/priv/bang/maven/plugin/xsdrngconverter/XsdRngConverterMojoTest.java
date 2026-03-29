package no.priv.bang.maven.plugin.xsdrngconverter;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class XsdRngConverterMojoTest {
    final static FilenameFilter rngFileFilter = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.endsWith(".rng");
        }
    };
    private static Properties testProperties;

    @BeforeAll
    static void readTestProperties() throws Exception {
        testProperties = new Properties();
        testProperties.load(XsdRngConverterMojoTest.class.getClassLoader().getResourceAsStream("test.properties"));
    }

    @Test
    void testFindXsdFiles() {
        var mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();

        var xsdFiles = mojo.findXsdFiles();
        assertThat(xsdFiles).hasSize(1);
    }

    @Test
    void testConvertXsdFileNameToRngFileName() {
        var mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");
        var xsdFiles = mojo.findXsdFiles();

        var rngFile = mojo.convertXsdFileNameToRngFileName(xsdFiles.get(0));
        assertThat(rngFile.getName()).endsWith(".rng");
    }

    @Test
    void testConvertXsdFilesToRngFiles() throws Exception {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");

        mojo.execute();

        var rngFiles = new File(mojo.rngOutputDirectory).listFiles(rngFileFilter);
        assertThat(rngFiles).hasSize(1);
    }

    @Test
    void testMojoWithErrorOnTargetDirectoryCreate() throws Exception {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.rngOutputDirectory = "/not/found/dummy/directory";

        assertThrows(MojoExecutionException.class, () -> mojo.execute());
    }

}
