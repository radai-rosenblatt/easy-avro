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

package org.apache.avro.compiler.specific;

import org.apache.avro.Schema;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.Collection;
import java.util.regex.Matcher;

//this exists here because SpecificCompiler.OutputFile is package private
public class SpecificCompilerEx extends SpecificCompiler {
    private static final Method ENQUEUE;

    static {
        try {
            ENQUEUE = SpecificCompiler.class.getDeclaredMethod("enqueue", Schema.class);
            ENQUEUE.setAccessible(true);
        } catch (Exception e) {
            throw new IllegalStateException("unable to access SpecificCompiler.enqueue()", e);
        }
    }

    public SpecificCompilerEx(Collection<Schema> schemas) {
        this(toArray(schemas));
    }

    public SpecificCompilerEx(Schema... schemas) {
        super(schemas[0]);
        try {
            for (int i = 1; i < schemas.length; i++) {
                ENQUEUE.invoke(this, schemas[i]);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Schema[] toArray(Collection<Schema> schemas) {
        Schema[] arr = new Schema[schemas.size()];
        int i=0;
        for (Schema s : schemas) {
            arr[i++] = s;
        }
        return arr;
    }

    public void compile(Path outputRoot) throws IOException {
        compile(outputRoot.toFile());
    }

    public void compile(File outputRoot) throws IOException {
        Collection<SpecificCompiler.OutputFile> outFiles = compile();
        for (SpecificCompiler.OutputFile file : outFiles) {
            //normalize separators to be the current OS ones, just in case.
            file.writeToDestination(new File(file.path.replaceAll("[/\\\\]", Matcher.quoteReplacement(File.separator))), outputRoot);
        }
    }
}
