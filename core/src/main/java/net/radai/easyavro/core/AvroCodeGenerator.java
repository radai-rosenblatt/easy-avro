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

import org.apache.avro.MultiSchemaParser;
import org.apache.avro.Schema;
import org.apache.avro.compiler.specific.SpecificCompilerEx;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class AvroCodeGenerator {
    private String inputEncoding = "UTF-8";
    private String outputEncoding = "UTF-8";

    public void setInputEncoding(String inputEncoding) {
        this.inputEncoding = inputEncoding;
    }

    public void setOutputEncoding(String outputEncoding) {
        this.outputEncoding = outputEncoding;
    }

    public Set<Path> generateSpecificClasses(Collection<Path> avroFiles, Path outputRoot) throws IOException {
        Collection<Schema> schemata = MultiSchemaParser.parse(avroFiles, Charset.forName(inputEncoding));
        SpecificCompilerEx compiler = new SpecificCompilerEx(schemata);
        compiler.setOutputCharacterEncoding(outputEncoding);
        Set<File> filesWritten = compiler.compile(outputRoot);
        return filesWritten.stream().map(File::toPath).collect(Collectors.toSet());
    }
}
