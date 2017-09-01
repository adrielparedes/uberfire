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

package org.uberfire.ext.metadata.backend.hibernate.index;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.IndexProvider;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.search.ClusterSegment;
import org.uberfire.ext.metadata.search.IOSearchService;
import org.uberfire.ext.metadata.search.SearchIndex;

import static java.util.Collections.emptyList;

public class HibernateSearchSearchIndex implements SearchIndex {

    private final IndexProvider indexProvider;
    private final QueryBuilder queryBuilder;

    public HibernateSearchSearchIndex(IndexProvider indexProvider,
                                      Analyzer analyzer) {
        this.indexProvider = indexProvider;
        this.queryBuilder = new QueryBuilder(analyzer);
    }

    @Override
    public List<KObject> searchByAttrs(Map<String, ?> attrs,
                                       IOSearchService.Filter filter,
                                       ClusterSegment... clusterSegments) {

        if (clusterSegments == null || clusterSegments.length == 0) {
            return emptyList();
        }
        if (attrs == null || attrs.size() == 0) {
            return emptyList();
        }

        Query query = queryBuilder.buildQuery(attrs,
                                              clusterSegments);

        return this.search(query,
                           filter,
                           clusterSegments);
    }

    @Override
    public List<KObject> fullTextSearch(String term,
                                        IOSearchService.Filter filter,
                                        ClusterSegment... clusterSegments) {
        if (clusterSegments == null || clusterSegments.length == 0) {
            return emptyList();
        }

        Query query = queryBuilder.buildQuery(term,
                                              clusterSegments);
        return this.search(query,
                           filter,
                           clusterSegments);
    }

    private List<KObject> search(Query query,
                                 IOSearchService.Filter filter,
                                 ClusterSegment[] clusterSegments) {
        List<KObjectImpl> found = this.indexProvider.findByQuery(KObjectImpl.class,
                                                                 query);

        return found.stream().filter(kObject -> filter.accept(kObject)).collect(Collectors.toList());
    }

    @Override
    public int searchByAttrsHits(Map<String, ?> attrs,
                                 ClusterSegment... clusterSegments) {
        return 0;
    }

    @Override
    public int fullTextSearchHits(String term,
                                  ClusterSegment... clusterSegments) {
        return 0;
    }
}
