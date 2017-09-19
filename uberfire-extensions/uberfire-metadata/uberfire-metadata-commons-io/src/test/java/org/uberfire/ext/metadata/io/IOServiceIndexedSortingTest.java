/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.ext.metadata.io;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.WildcardQuery;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.uberfire.ext.metadata.backend.hibernate.model.FieldFactory;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.java.nio.file.OpenOption;
import org.uberfire.java.nio.file.Path;

import static org.junit.Assert.*;

@RunWith(org.jboss.byteman.contrib.bmunit.BMUnitRunner.class)
@BMScript(value = "byteman/index.btm")
public class IOServiceIndexedSortingTest extends BaseIndexTest {

    @Override
    protected String[] getRepositoryNames() {
        return new String[]{this.getClass().getSimpleName()};
    }

    @Test
    public void testSortedFiles() throws IOException, InterruptedException {

        setupCountDown(4);

        //Write files in reverse order so natural Lucene order would be c, b, a
        final Path base = writeFile("cFile1.txt");
        writeFile("CFile2.txt");
        writeFile("bFile.txt");
        writeFile("aFile.txt");

        waitForCountDown(5000);

        {
            final Sort sort = new Sort(new SortField(FieldFactory.FILE_NAME_FIELD_SORTED,
                                                     SortField.Type.STRING));
            final Query query = new WildcardQuery(new Term("filename",
                                                           "*txt"));

            List<KObjectImpl> hits = this.indexProvider.findByQuery(KObjectImpl.class,
                                                                    query,
                                                                    sort);

            assertEquals(4,
                         hits.size());
            assertEquals("aFile.txt",
                         hits.get(0).getProperty("filename").get().getValue());
            assertEquals("bFile.txt",
                         hits.get(1).getProperty("filename").get().getValue());
            assertEquals("cFile1.txt",
                         hits.get(2).getProperty("filename").get().getValue());
            assertEquals("CFile2.txt",
                         hits.get(3).getProperty("filename").get().getValue());
        }
    }

    private Path writeFile(final String fileName) {
        final Path path = getBasePath(this.getClass().getSimpleName()).resolve(fileName);
        ioService().write(path,
                          "content",
                          Collections.<OpenOption>emptySet());
        return path;
    }
}
