/*
 * Copyright 2015 JBoss, by Red Hat, Inc
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
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.java.nio.file.Path;

import static org.junit.Assert.*;
import static org.uberfire.ext.metadata.engine.MetaIndexEngine.FULL_TEXT_FIELD;

@RunWith(org.jboss.byteman.contrib.bmunit.BMUnitRunner.class)
@BMScript(value = "byteman/index.btm")
public class LuceneFullTextSearchIndexTest extends BaseIndexTest {

    @Override
    protected String[] getRepositoryNames() {
        return new String[]{this.getClass().getSimpleName()};
    }

    @Test
    public void testFullTextIndexedFile() throws IOException, InterruptedException {
        setupCountDown(2);
        final Path path1 = getBasePath(this.getClass().getSimpleName()).resolve("mydrlfile1.drl");
        ioService().write(path1,
                          "Some cheese");

        waitForCountDown(5000);

        {

            List<KObjectImpl> found = this.indexProvider.findByQuery(KObjectImpl.class,
                                                                     new WildcardQuery(new Term(FULL_TEXT_FIELD,
                                                                                                "*file*")));

            assertEquals(1,
                         found.size());
        }

        {

            List<KObjectImpl> found = this.indexProvider.findByQuery(KObjectImpl.class,
                                                                     new WildcardQuery(new Term(FULL_TEXT_FIELD,
                                                                                                "*mydrlfile1*")));

            assertEquals(1,
                         found.size());
        }

        setupCountDown(2);

        final Path path2 = getBasePath(this.getClass().getSimpleName()).resolve("s.drl");
        ioService().write(path2,
                          "Some cheese");

        waitForCountDown(5000);

        {

            List<KObjectImpl> found = this.indexProvider.findByQuery(KObjectImpl.class,
                                                                     new WildcardQuery(new Term(FULL_TEXT_FIELD,
                                                                                                "s*")));

            assertEquals(1,
                         found.size());
        }
    }
}
