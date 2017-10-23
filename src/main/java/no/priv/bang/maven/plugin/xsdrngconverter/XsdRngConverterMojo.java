package no.priv.bang.maven.plugin.xsdrngconverter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
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
    private final FilenameFilter xsdFileFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".xsd");
            }
        };

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
        //Locale.setDefault(Locale.ROOT);
        createRngOutputDirectory();
        List<File> xsdFiles = findXsdFiles();
        convertXsdFilesToRngFiles(xsdFiles);
    }

    private void createRngOutputDirectory() throws MojoExecutionException {
        try {
            Files.createDirectories(Paths.get(rngOutputDirectory));
        } catch (IOException e) {
            String message = String.format("Caught exception creating directory %s", rngOutputDirectory);
            throw new MojoExecutionException(message, e);
        }
    }

    private void convertXsdFilesToRngFiles(List<File> xsdFiles) throws MojoExecutionException {
        PrintStream origOut = System.out;
        try {
            for (File xsdFile : xsdFiles) {
                File rngFile = convertXsdFileNameToRngFileName(xsdFile);
                try(PrintStream rngStream = new PrintStream(rngFile)) {
                    String[] args = { xsdFile.getAbsolutePath() };
                    System.setOut(rngStream);
                    Driver.main(args);
                } catch (Exception e) {
                    String message = String.format("Caught exception converting %s to %s", xsdFile.toString(), rngFile.toString());
                    throw new MojoExecutionException(message, e);
                }
            }
        } finally {
            System.setOut(origOut);
        }
    }

    List<File> findXsdFiles() {
        File xsdDirectory = new File(xsdInputDirectory);
        return Arrays.asList(xsdDirectory.listFiles(xsdFileFilter));
    }

    File convertXsdFileNameToRngFileName(File xsdFile) {
        File outputDirectory = new File(rngOutputDirectory);
        String localFile = xsdFile.getName().replace(".xsd", ".rng");
        return new File(outputDirectory, localFile);
    }

}
