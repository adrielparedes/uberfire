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

package org.uberfire.ext.metadata.backend.hibernate.preferences;

import org.uberfire.ext.metadata.backend.hibernate.sharding.KObjectShardIdentifierProvider;
import org.uberfire.ext.metadata.preferences.IndexManagerType;
import org.uberfire.ext.metadata.preferences.LucenePreferences;

public class HibernateSearchPreferences {

    public static final String INDEX_MANAGER = "hibernate.search.index.manager";
    public static final String SHARDING_STRATEGY = "hibernate.search.default.sharding_strategy";

    private String indexManager;
    private String shardingStrategy;

    private LucenePreferences lucenePreferences;

    private ElasticsearchPreferences elasticsearchPreferences;

    public HibernateSearchPreferences() {
        this.indexManager = System.getProperty(INDEX_MANAGER,
                                               IndexManagerType.LUCENE.toString());

        this.shardingStrategy = System.getProperty(SHARDING_STRATEGY,
                                                   KObjectShardIdentifierProvider.class.getCanonicalName());

        this.lucenePreferences = new LucenePreferences();
        this.elasticsearchPreferences = new ElasticsearchPreferences();
    }

    public String getIndexManager() {
        return indexManager;
    }

    public void setIndexManager(String indexManager) {
        this.indexManager = indexManager;
    }

    public LucenePreferences getLucenePreferences() {
        return lucenePreferences;
    }

    public void setLucenePreferences(LucenePreferences lucenePreferences) {
        this.lucenePreferences = lucenePreferences;
    }

    public ElasticsearchPreferences getElasticsearchPreferences() {
        return elasticsearchPreferences;
    }

    public void setElasticsearchPreferences(ElasticsearchPreferences elasticsearchPreferences) {
        this.elasticsearchPreferences = elasticsearchPreferences;
    }

    public String getShardingStrategy() {
        return shardingStrategy;
    }

    public void setShardingStrategy(String shardingStrategy) {
        this.shardingStrategy = shardingStrategy;
    }
}
