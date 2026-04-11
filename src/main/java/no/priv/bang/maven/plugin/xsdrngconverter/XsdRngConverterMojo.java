package no.priv.bang.maven.plugin.xsdrngconverter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import com.sun.msv.writer.relaxng.Driver;

/**
 * Goal which reads all <a href="https://www.w3.org/XML/Schema">W3C XML schema</a> files
 * (aka. "XSD schemas") from an input directory, and writes <a href="http://relaxng.org">Relax-NG</a>
 * versions of same schemas to an output directory.
 */
public class XsdRngConverterMojo extends AbstractMojo {
    private final FilenameFilter xsdFileFilter = (dir, name) -> name.endsWith(".xsd");

    /**
     * Location of the directory scanned for XSD files.
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/xsd", property = "xsdInputDirectory", required = true )
    String xsdInputDirectory;

    /**
     * Location of the directory where the generated RNG files are put
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/rng", property = "rngOutputDirectory", required = true )
    String rngOutputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        createRngOutputDirectory();
        var xsdFiles = findXsdFiles();
        convertXsdFilesToRngFiles(xsdFiles);
    }

    private void createRngOutputDirectory() throws MojoExecutionException {
        try {
            Files.createDirectories(Paths.get(rngOutputDirectory));
        } catch (IOException e) {
            var message = String.format("Caught exception creating directory %s", rngOutputDirectory);
            throw new MojoExecutionException(message, e);
        }
    }

    private void convertXsdFilesToRngFiles(List<File> xsdFiles) throws MojoExecutionException {
        for (File xsdFile : xsdFiles) {
            var rngFile = convertXsdFileNameToRngFileName(xsdFile).getAbsolutePath();
            try {
                String[] args = { xsdFile.getAbsolutePath(), rngFile };
                Driver.main(args);
            } catch (Exception e) {
                var message = String.format("Caught exception converting %s to %s", xsdFile.toString(), rngFile);
                throw new MojoExecutionException(message, e);
            }
        }
    }

    List<File> findXsdFiles() {
        var xsdDirectory = new File(xsdInputDirectory);
        return Arrays.asList(xsdDirectory.listFiles(xsdFileFilter));
    }

    File convertXsdFileNameToRngFileName(File xsdFile) {
        var outputDirectory = new File(rngOutputDirectory);
        var localFile = xsdFile.getName().replace(".xsd", ".rng");
        return new File(outputDirectory, localFile);
    }

}
