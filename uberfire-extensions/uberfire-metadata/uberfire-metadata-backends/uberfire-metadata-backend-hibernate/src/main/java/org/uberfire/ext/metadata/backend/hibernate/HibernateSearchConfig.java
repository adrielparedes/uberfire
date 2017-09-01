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

import org.apache.lucene.analysis.Analyzer;
import org.hibernate.search.spi.SearchIntegrator;
import org.uberfire.ext.metadata.MetadataConfig;
import org.uberfire.ext.metadata.backend.hibernate.index.HibernateSearchIndexEngine;
import org.uberfire.ext.metadata.backend.hibernate.index.HibernateSearchSearchIndex;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.HibernateSearchIndexProvider;
import org.uberfire.ext.metadata.engine.IndexManager;
import org.uberfire.ext.metadata.engine.MetaIndexEngine;
import org.uberfire.ext.metadata.engine.MetaModelStore;
import org.uberfire.ext.metadata.search.SearchIndex;

public class HibernateSearchConfig implements MetadataConfig {

    private final HibernateSearchSearchIndex searchIndex;
    private final HibernateSearchIndexEngine indexEngine;
    private final HibernateSearchIndexProvider indexProvider;

    public HibernateSearchConfig(SearchIntegrator searchIntegrator,
                                 Analyzer analyzer) {

        this.indexProvider = new HibernateSearchIndexProvider(searchIntegrator);
        this.searchIndex = new HibernateSearchSearchIndex(this.indexProvider,
                                                          analyzer);
        this.indexEngine = new HibernateSearchIndexEngine(this.indexProvider);
    }

    @Override
    public SearchIndex getSearchIndex() {
        return this.searchIndex;
    }

    @Override
    public MetaIndexEngine getIndexEngine() {
        return this.indexEngine;
    }

    @Override
    public IndexManager getIndexManager() {
        return null;
    }

    @Override
    public MetaModelStore getMetaModelStore() {
        return null;
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public void dispose() {

    }
}
