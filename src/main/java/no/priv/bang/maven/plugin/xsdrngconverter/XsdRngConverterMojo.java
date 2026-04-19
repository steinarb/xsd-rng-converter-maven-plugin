package no.priv.bang.maven.plugin.xsdrngconverter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which reads all <a href="https://www.w3.org/XML/Schema">W3C XML schema</a> files
 * (aka. "XSD schemas") from an input directory, and writes <a href="http://relaxng.org">Relax-NG</a>
 * versions of same schemas to an output directory.
 */
@Mojo(name="convert", defaultPhase = LifecyclePhase.VALIDATE)
public class XsdRngConverterMojo extends AbstractMojo {
    private final FilenameFilter xsdFileFilter = (dir, name) -> name.endsWith(".xsd");

    /**
     * Location of directory scanned for XSD files. If the value is the filename of a single file, only that file will be converted, even if the directory the file resides in, contains multiple xsd files
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/xsd", property = "xsd.input.directory", required = true )
    String xsdInputDirectory;

    /**
     * Location of destination directory for generated RNG files (Relax-NG XML notation schema files)
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/rng", property = "rng.output.directory", required = true )
    String rngOutputDirectory;

    /**
     * Location of destination directory for generated RNC files (Relax-NG compact notation schema files)
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/rnc", property = "rnc.output.directory", required = true )
    String rncOutputDirectory;

    /**
     * If true, strip the version number from the filename of the generated RNC files. Defaults to false.
     */
    @Parameter(defaultValue = "false", property = "no.rnc.version", required = true )
    boolean noRncVersion;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        createRngOutputDirectory();
        var xsdFiles = findXsdFiles();
        convertXsdFilesToRngFiles(xsdFiles);
        var rngFiles = xsdFiles.stream().map(this::convertXsdFileNameToRngFileName).toList();
        createRncOutputDirectory();
        convertRngFilesToRncFiles(rngFiles);
    }

    private void createRngOutputDirectory() throws MojoExecutionException {
        try {
            Files.createDirectories(Paths.get(rngOutputDirectory));
        } catch (IOException e) {
            var message = String.format("Caught exception creating directory %s", rngOutputDirectory);
            throw new MojoExecutionException(message, e);
        }
    }

    private void createRncOutputDirectory() throws MojoExecutionException {
        try {
            Files.createDirectories(Paths.get(rncOutputDirectory));
        } catch (IOException e) {
            var message = String.format("Caught exception creating directory %s", rncOutputDirectory);
            throw new MojoExecutionException(message, e);
        }
    }

    private void convertXsdFilesToRngFiles(List<File> xsdFiles) throws MojoExecutionException {
        for (File xsdFile : xsdFiles) {
            var rngFile = convertXsdFileNameToRngFileName(xsdFile).getAbsolutePath();
            try {
                String[] args = { xsdFile.getAbsolutePath(), rngFile };
                com.sun.msv.writer.relaxng.Driver.main(args);
            } catch (Exception e) {
                var message = String.format("Caught exception converting %s to %s", xsdFile.toString(), rngFile);
                throw new MojoExecutionException(message, e);
            }
        }
    }

    int convertRngFilesToRncFiles(List<File> rngFiles) {
        var trangDriver = new com.thaiopensource.relaxng.translate.Driver();
        int conversionStatus = 0;
        for (File rngFile : rngFiles) {
            var rncFile = convertRngFileNameToRncFileName(rngFile).getAbsolutePath();
            String[] args = { rngFile.getAbsolutePath(), rncFile };
            conversionStatus += trangDriver.run(args);
        }

        return conversionStatus;
    }

    List<File> findXsdFiles() {
        var xsdDirectory = new File(xsdInputDirectory);
        if (Files.isDirectory(xsdDirectory.toPath())) {
            return Arrays.asList(xsdDirectory.listFiles(xsdFileFilter));
        }

        return Collections.singletonList(xsdDirectory);
    }

    File convertXsdFileNameToRngFileName(File xsdFile) {
        var outputDirectory = new File(rngOutputDirectory);
        var localFile = xsdFile.getName().replace(".xsd", ".rng");
        return new File(outputDirectory, localFile);
    }

    File convertRngFileNameToRncFileName(File rngFile) {
        var outputDirectory = new File(rncOutputDirectory);
        var localFile = maybeStripVersionNumber(rngFile.getName()).replace(".rng", ".rnc");
        return new File(outputDirectory, localFile);
    }

    String maybeStripVersionNumber(String filename) {
        if (!noRncVersion) {
            return filename;
        }

        return filename.replaceFirst("-\\d+\\.\\d+\\.", ".");
    }

}
