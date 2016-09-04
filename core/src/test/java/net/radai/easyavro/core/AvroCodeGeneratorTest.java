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

package net.radai.easyavro.core;

import net.radai.compilib.Compilib;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AvroCodeGeneratorTest {
    @Test
    public void testGen() throws Exception {
        URL recAUrl = getClass().getClassLoader().getResource("RecordA.avsc");
        Assert.assertNotNull("unable to locate test avro files", recAUrl);
        File parentFolder = new File(recAUrl.toURI()).getParentFile().getAbsoluteFile();
        Assert.assertTrue(parentFolder.isDirectory());

        Set<Path> avroFiles = new HashSet<>();
        Files.walkFileTree(parentFolder.toPath(), new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String name = file.getFileName().toString();
                if (name.endsWith(".avsc")) {
                    avroFiles.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        Path outDir = Files.createTempDirectory(null);

        AvroCodeGenerator gen = new AvroCodeGenerator();
        gen.setInputEncoding("UTF-8");
        gen.setOutputEncoding("UTF-8");
        Set<Path> sourceFiles = gen.generateSpecificClasses(avroFiles, outDir);
        Set<String> contents = sourceFiles.stream().map(path -> {
            try {
                return new String(Files.readAllBytes(path), Charset.forName("UTF-8"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
        Map<String, Class<?>> classes = Compilib.compile(contents);
    }
}
