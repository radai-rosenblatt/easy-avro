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

package org.apache.avro;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

//this exists here because Schema.Names is package private
public class MultiSchemaParser {
    private static final Logger log = LogManager.getLogger(MultiSchemaParser.class);

    public static Collection<Schema> parse(Collection<Path> avroFiles) {
        return parse(avroFiles, null);
    }

    public static Collection<Schema> parse(Collection<Path> avroFiles, Charset encodedIn) {
        Charset cs = encodedIn;
        if (cs == null) {
            cs = Charset.defaultCharset();
            log.warn("charset not provided, using system default " + cs.displayName());
        }
        Set<Path> remaining = new HashSet<>(avroFiles);
        Schema.Names names = new Schema.Names();
        while (!remaining.isEmpty()) {
            Throwable firstThisIteration = null;
            Set<Path> parsedThisIteration = new HashSet<>();
            for (Path r : remaining) {
                try (InputStream is = Files.newInputStream(r, StandardOpenOption.READ);
                     InputStreamReader reader = new InputStreamReader(is, cs)) {
                    //copy names, because a failed compilation will pollute it
                    Schema.Names namesCopy = new Schema.Names();
                    namesCopy.putAll(names);

                    JsonParser jsonParser = Schema.FACTORY.createJsonParser(reader);
                    Schema schema = Schema.parse(Schema.MAPPER.readTree(jsonParser), namesCopy);
                    log.debug("successfully parsed {}", r);
                    parsedThisIteration.add(r);
                    names.add(schema); //now that we know it was successfully compiled
                } catch (Throwable t) {
                    if (firstThisIteration == null) {
                        firstThisIteration = t;
                    }
                }
            }
            if (parsedThisIteration.isEmpty()) {
                if (firstThisIteration != null) {
                    throw new IllegalStateException("cannot make progress", firstThisIteration);
                } else {
                    throw new IllegalStateException("cannot make progress for unknown reasons");
                }
            }
            remaining.removeAll(parsedThisIteration);
        }
        return names.values();
    }
}
