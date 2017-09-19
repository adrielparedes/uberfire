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
import java.net.URI;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.hibernate.search.spi.SearchIntegrator;
import org.jboss.byteman.contrib.bmunit.BMScript;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.uberfire.commons.async.DescriptiveThreadFactory;
import org.uberfire.ext.metadata.backend.hibernate.analyzer.FilenameAnalyzer;
import org.uberfire.ext.metadata.backend.hibernate.index.HibernateSearchIndexEngine;
import org.uberfire.ext.metadata.backend.hibernate.index.HibernateSearchSearchIndex;
import org.uberfire.ext.metadata.backend.hibernate.index.QueryAdapter;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.HibernateSearchIndexProvider;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.IndexProvider;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.SearchIntegratorBuilder;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.backend.hibernate.preferences.HibernateSearchPreferences;
import org.uberfire.ext.metadata.engine.MetaIndexEngine;
import org.uberfire.ext.metadata.model.KCluster;
import org.uberfire.io.IOService;
import org.uberfire.io.attribute.DublinCoreView;
import org.uberfire.java.nio.base.version.VersionAttributeView;
import org.uberfire.java.nio.file.FileSystem;
import org.uberfire.java.nio.file.Path;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(org.jboss.byteman.contrib.bmunit.BMUnitRunner.class)
@BMScript(value = "byteman/index.btm")
public class BatchIndexConcurrencyTest extends BaseIndexTest {

    private MetaIndexEngine metaIndexEngine;

    @Override
    protected String[] getRepositoryNames() {
        return new String[]{this.getClass().getSimpleName()};
    }

    @Override
    protected IOService ioService() {
        if (ioService == null) {

            HashMap<String, Analyzer> analyzers = new HashMap<>();
            analyzers.put(IndexProvider.CUSTOM_FIELD_FILENAME,
                          new FilenameAnalyzer());

            PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(CharArraySet.EMPTY_SET),
                                                                           analyzers);

            SearchIntegrator searchIntegrator = new SearchIntegratorBuilder()
                    .addClass(KObjectImpl.class)
                    .withPreferences(new HibernateSearchPreferences())
                    .build();

            queryAdapter = new QueryAdapter(KObjectImpl.class,
                                            "properties");

            indexProvider = new HibernateSearchIndexProvider(searchIntegrator,
                                                             new QueryAdapter(KObjectImpl.class,
                                                                              "properties"));

            searchIndex = new HibernateSearchSearchIndex(indexProvider,
                                                         analyzer);
            this.metaIndexEngine = spy(new HibernateSearchIndexEngine(indexProvider));

            IOService service = new IOServiceIndexedImpl(this.metaIndexEngine,
                                                         Executors.newCachedThreadPool(new DescriptiveThreadFactory()),
                                                         DublinCoreView.class,
                                                         VersionAttributeView.class) {
                @Override
                protected void setupWatchService(FileSystem fs) {

                }
            };
            this.ioService = service;
        }
        return ioService;
    }

    @Test
    //See https://bugzilla.redhat.com/show_bug.cgi?id=1288132
    public void testSingleConcurrentBatchIndexExecution() throws IOException, InterruptedException {
        //Write a file to ensure the FileSystem has a Root Directory
        final Path path1 = getBasePath(this.getClass().getSimpleName()).resolve("xxx");
        ioService().write(path1,
                          "xxx!");

        setupCountDown(1);

        final URI fsURI = URI.create("git://" + this.getClass().getSimpleName() + "/file1");

        //Make multiple requests for the FileSystem. We should only have one batch index operation
        final CountDownLatch startSignal = new CountDownLatch(1);
        for (int i = 0; i < 3; i++) {
            Runnable r = () -> {
                try {
                    startSignal.await();
                    ioService().getFileSystem(fsURI);
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                }
            };
            new Thread(r).start();
        }
        startSignal.countDown();

        waitForCountDown(5000);

        assertEquals(1,
                     getStartBatchCount());
        verify(this.metaIndexEngine,
               times(3)).freshIndex(any(KCluster.class));
    }
}