package no.priv.bang.maven.plugin.xsdrngconverter;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.maven.plugin.MojoExecutionException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.rng.CompactSchemaReader;
import com.thaiopensource.xml.sax.ErrorHandlerImpl;

class XsdRngConverterMojoTest {
    static final FilenameFilter rngFileFilter = (dir, name) -> name.endsWith(".rng");
    static final FilenameFilter rncFileFilter = (dir, name) -> name.endsWith(".rnc");
    private static Properties testProperties;

    @BeforeAll
    static void readTestProperties() throws Exception {
        testProperties = new Properties();
        testProperties.load(XsdRngConverterMojoTest.class.getClassLoader().getResourceAsStream("test.properties"));
    }

    @BeforeEach
    void cleanOutputDirectoryBetweenTests() {
        var outputDirectory = new File(testProperties.getProperty("testOutputDirectory"));
        var filesToDelete = outputDirectory.listFiles();
        if (filesToDelete != null) {
            for (var file: filesToDelete) {
                file.delete();
            }
        }

        var rncOutputDirectory = new File(testProperties.getProperty("testRncOutputDirectory"));
        var rncFilesToDelete = rncOutputDirectory.listFiles();
        if (rncFilesToDelete != null) {
            for (var file: rncFilesToDelete) {
                file.delete();
            }
        }
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
    void testConvertRngFileNameToRncFileName() {
        var mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");
        mojo.rncOutputDirectory = testProperties.getProperty("testRncOutputDirectory");
        var xsdFiles = mojo.findXsdFiles();
        var rngFiles = xsdFiles.stream().map(f -> mojo.convertXsdFileNameToRngFileName(f)).toList();

        var rncFile = mojo.convertRngFileNameToRncFileName(rngFiles.get(0));
        assertThat(rncFile.getName()).endsWith(".rnc");
    }

    @Test
    void testConvertXsdFilesToRngAndRncFiles() throws Exception {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");
        mojo.rncOutputDirectory = testProperties.getProperty("testRncOutputDirectory");

        mojo.execute();

        var rngFiles = new File(mojo.rngOutputDirectory).listFiles(rngFileFilter);
        assertThat(rngFiles).hasSize(1);
        assertDoesNotThrow(() -> parseXmlFile(rngFiles[0])); // Verify generated file is XML file

        var rncFiles = new File(mojo.rncOutputDirectory).listFiles(rncFileFilter);
        assertThat(rncFiles).hasSize(1);
        //assertDoesNotThrow(() -> parseRncFile(rncFiles[0])); // Verify generated file is actual Relax-NG compact notation schema
        parseRncFile(rncFiles[0]);
    }

    private Document parseXmlFile(File file) throws Exception {
        var factory = DocumentBuilderFactory.newInstance();
        var builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    private boolean parseRncFile(File rncFile) throws Exception {
        var errorhandler = new ErrorHandlerImpl();
        var propertymapBuilder = new PropertyMapBuilder();
        propertymapBuilder.put(ValidateProperty.ERROR_HANDLER, errorhandler);
        var schemaReader = CompactSchemaReader.getInstance();
        var validationDriver = new ValidationDriver(propertymapBuilder.toPropertyMap(), schemaReader);
        var inputSource = new InputSource(new FileInputStream(rncFile));
        return validationDriver.loadSchema(inputSource);
    }

    @Test
    void testMojoWithErrorOnTargetDirectoryCreate() {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.rngOutputDirectory = "/not/found/dummy/directory";

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void testMojoWithErrorOnRncTargetDirectoryCreate() {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("xsd/xhtml1-strict.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");
        mojo.rncOutputDirectory = "/not/found/dummy/directory";

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
        void testConvertXsdFilesToRngFilesOnFileNotActuallyXsd() {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.xsdInputDirectory = new File(getClass().getClassLoader().getResource("not_xsd/not_actually.xsd").getFile()).getParent();
        mojo.rngOutputDirectory = testProperties.getProperty("testOutputDirectory");

        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test
    void testConvertRngFilesToRncFilesOnFileNotRng() throws MojoExecutionException {
        XsdRngConverterMojo mojo = new XsdRngConverterMojo();
        mojo.rncOutputDirectory = testProperties.getProperty("testRncOutputDirectory");
        var mockRngDirectory = new File(getClass().getClassLoader().getResource("not_rng/not_actually.rng").getFile()).getParent();
        var rngFiles = new File(mockRngDirectory).listFiles(rngFileFilter);

        // Status code 0 signifies a successful conversion
        assertThat(mojo.convertRngFilesToRncFiles(List.of(rngFiles))).isNotEqualTo(0);
    }

}
