/*
 * This file is part of EasyAvro.
 *
 * EasyAvro is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EasyAvro is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with EasyAvro.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.radai.easyavro.maven;

import net.radai.easyavro.core.AvroCodeGenerator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.io.DirectoryScanner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mojo(name = "codegen", threadSafe = true, defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodegenMojo extends AbstractMojo {

    @Parameter
    private List<String> sourcePaths = null;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/avro")
    private File outputPath;

    @Parameter(defaultValue="${project}", readonly=true, required=true)
    private MavenProject project;

    @Parameter
    private String[] includes = new String[] {"**/*.avsc", "**/*.avpr", "**/*.avdl"};

    @Parameter
    private String[] excludes = new String[0];

    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        //check parameters
        supplementDefaultConfiguration();

        //find source files
        Set<Path> files = findIncludedFiles();
        if (files.isEmpty()) {
            log.warn("found no avro files");
            return;
        }
        log.info("found " + files.size() + " avro file(s)");

        //generate java code from source files
        AvroCodeGenerator generator = new AvroCodeGenerator();
        try {
            generator.generateSpecificClasses(files, outputPath.toPath());
        } catch (IOException e) {
            throw new MojoFailureException("unable to generate java classes from avro files", e);
        }

        //add generated java code to the current maven project
        project.addCompileSourceRoot(outputPath.toString());
    }

    private void supplementDefaultConfiguration() {
        Log log = getLog();
        if (sourcePaths == null) {
            //default to ${project.basedir}/src/main/avro
            String basedir = project.getBasedir().getAbsolutePath();
            String defaultSrc = basedir + File.separator + "src" + File.separator + "main" + File.separator + "avro";
            sourcePaths = Collections.singletonList(defaultSrc);
        }
    }

    private Set<Path> findIncludedFiles() {
        Set<Path> searchRoots = new HashSet<>();
        Path basedir = project.getBasedir().getAbsoluteFile().toPath();
        for (String str : sourcePaths) {
            Path path = Paths.get(str);
            if (!path.isAbsolute()) {
                path = basedir.resolve(path); //relatives paths are relative to basedir
            }
            searchRoots.add(path);
        }

        Set<Path> selected = new HashSet<>();
        for (Path root : searchRoots) {
            if (!Files.exists(root)) {
                continue;
            }
            if (Files.isDirectory(root)) {
                DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir(root.toFile());
                scanner.setIncludes(includes);
                scanner.setExcludes(excludes);
                scanner.scan();
                for (String included : scanner.getIncludedFiles()) {
                    selected.add(root.resolve(included));
                }
            } else {
                //single-listed files included automatically
                selected.add(root);
            }
        }

        return selected;
    }
}
