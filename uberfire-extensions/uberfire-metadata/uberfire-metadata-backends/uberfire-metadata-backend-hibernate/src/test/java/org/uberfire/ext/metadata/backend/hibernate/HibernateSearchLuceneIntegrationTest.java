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
 *
 */

package org.uberfire.ext.metadata.backend.hibernate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hibernate.search.spi.SearchIntegrator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.uberfire.ext.metadata.backend.hibernate.analyzer.AnalyzerProvider;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.HibernateSearchIndexProvider;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.SearchIntegratorBuilder;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.preferences.IndexManagerType;

import static org.junit.Assert.*;

public class HibernateSearchLuceneIntegrationTest extends HibernateSearchIntegrationTest {

    @Rule
    public TemporaryFolder testFolderRule = new TemporaryFolder();
    private File testFolder;

    @Before
    public void setUp() {

        super.setUp();

        testFolder = null;
        try {
            testFolder = testFolderRule.newFolder();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.preferences.setIndexManager(IndexManagerType.LUCENE.toString());
        this.preferences.getLucenePreferences().setDefaultIndexBase(new File(testFolder.getAbsolutePath(),
                                                                             ".index").getAbsolutePath());

        SearchIntegrator integrator = new SearchIntegratorBuilder()
                .withPreferences(preferences)
                .withAnalyzerProvider(AnalyzerProvider.class)
                .addClass(KObjectImpl.class)
                .build();
        this.provider = new HibernateSearchIndexProvider(integrator,
                                                         this.queryAdapter);
    }

    @After
    public void tierDown() {
        testFolderRule.delete();
    }

    @Test
    public void testLuceneShard() {
        KObjectImpl kObject1 = new KObjectImpl();
        kObject1.setClusterId("java");

        KObjectImpl kObject2 = new KObjectImpl();
        kObject2.setClusterId("mvel");

        this.provider.index(kObject1);
        this.provider.index(kObject2);

        File root = new File(this.testFolder,
                             ".index");
        boolean existsJava = Files.exists(Paths.get(root.getPath(),
                                                    KObjectImpl.class.getCanonicalName() + ".java"));
        boolean existsMvel = Files.exists(Paths.get(root.getPath(),
                                                    KObjectImpl.class.getCanonicalName() + ".mvel"));
        assertTrue(existsJava);
        assertTrue(existsMvel);
    }
}
