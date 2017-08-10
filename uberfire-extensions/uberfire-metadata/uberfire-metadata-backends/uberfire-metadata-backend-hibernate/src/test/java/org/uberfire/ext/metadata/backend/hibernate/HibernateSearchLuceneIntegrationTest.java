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

import org.hibernate.search.spi.SearchIntegrator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.HibernateSearchIndexProvider;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.SearchIntegratorBuilder;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.backend.hibernate.model.ParentIndex;
import org.uberfire.ext.metadata.backend.hibernate.model.PathIndex;
import org.uberfire.ext.metadata.preferences.IndexManagerType;

public class HibernateSearchLuceneIntegrationTest extends HibernateSearchIntegrationTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Before
    public void setUp() {

        super.setUp();

        File folder = null;
        try {
            folder = testFolder.newFolder();
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.preferences.setIndexManager(IndexManagerType.LUCENE.toString());
        this.preferences.getLucenePreferences().setDefaultIndexBase(folder.getAbsolutePath());

        SearchIntegrator integrator = new SearchIntegratorBuilder()
                .withPreferences(preferences)
                .addClass(KObjectImpl.class)
                .addClass(PathIndex.class)
                .addClass(ParentIndex.class)
                .build();
        this.provider = new HibernateSearchIndexProvider(integrator);
    }

    @After
    public void tierDown() {
        testFolder.delete();
    }
}
