/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.uberfire.ext.metadata.backend.hibernate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.hibernate.search.spi.SearchIntegrator;
import org.uberfire.ext.metadata.CustomAnalyzerWrapperFactory;
import org.uberfire.ext.metadata.MetadataConfig;
import org.uberfire.ext.metadata.backend.hibernate.analyzer.FilenameAnalyzer;
import org.uberfire.ext.metadata.backend.hibernate.index.QueryAdapter;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.IndexProvider;
import org.uberfire.ext.metadata.backend.hibernate.metamodel.NullMetaModelStore;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.engine.MetaModelStore;

public class HibernateSearchConfigBuilder {

    private SearchIntegrator searchIntegrator;
    private Analyzer analyzer;
    private CustomAnalyzerWrapperFactory customAnalyzerWrapperFactory;
    private Map<String, Analyzer> analyzers;
    private MetaModelStore metaModelStore;
    private QueryAdapter queryAdapter;

    public HibernateSearchConfigBuilder withSearchIntegrator(SearchIntegrator searchIntegrator) {
        this.searchIntegrator = searchIntegrator;
        queryAdapter = new QueryAdapter(KObjectImpl.class,
                                        "properties",
                                        Arrays.asList(IndexProvider.FULL_TEXT));
        return this;
    }

    public MetadataConfig build() {

        if (metaModelStore == null) {
            this.metaModelStore = new NullMetaModelStore();
        }
        if (analyzers == null) {
            withDefaultAnalyzers();
        }
        if (analyzer == null) {
            withDefaultAnalyzer();
        }

        return new HibernateSearchConfig(this.searchIntegrator,
                                         queryAdapter,
                                         this.analyzer);
    }

    public HibernateSearchConfigBuilder usingAnalyzers(Map<String, Analyzer> analyzers) {
        this.analyzers = analyzers;
        return this;
    }

    public HibernateSearchConfigBuilder usingAnalyzerWrapperFactory(CustomAnalyzerWrapperFactory factory) {
        this.customAnalyzerWrapperFactory = factory;
        return this;
    }

    public void withDefaultAnalyzers() {
        this.analyzers = new HashMap<>();
        analyzers.put(IndexProvider.CUSTOM_FIELD_FILENAME,
                      new FilenameAnalyzer());
    }

    public void withDefaultAnalyzer() {
        if (this.customAnalyzerWrapperFactory == null) {
            this.analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(CharArraySet.EMPTY_SET),
                                                        new HashMap<String, Analyzer>() {{
                                                            putAll(queryAdapter.fromAnalyzers(analyzers));
                                                        }});
        } else {
            this.analyzer = this.customAnalyzerWrapperFactory.getAnalyzerWrapper(new StandardAnalyzer(CharArraySet.EMPTY_SET),
                                                                                 queryAdapter.fromAnalyzers(analyzers));
        }
    }
}
