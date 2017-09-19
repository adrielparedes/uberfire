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

package org.uberfire.ext.metadata.backend.lucene.search;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.search.TotalHitCountCollector;
import org.uberfire.ext.metadata.backend.hibernate.index.QueryBuilder;
import org.uberfire.ext.metadata.backend.lucene.index.LuceneIndexManager;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.search.ClusterSegment;
import org.uberfire.ext.metadata.search.IOSearchService;
import org.uberfire.ext.metadata.search.SearchIndex;

import static java.util.Collections.emptyList;
import static org.apache.lucene.search.NumericRangeQuery.newLongRange;
import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;
import static org.uberfire.ext.metadata.backend.hibernate.util.KObjectUtil.toKObject;

/**
 *
 */
public class LuceneSearchIndex implements SearchIndex {

    private final LuceneIndexManager indexManager;
    private final QueryBuilder queryBuilder;

    public LuceneSearchIndex(final LuceneIndexManager indexManager,
                             final Analyzer analyzer) {
        this.indexManager = checkNotNull("lucene",
                                         indexManager);

        this.queryBuilder = new QueryBuilder(analyzer);
    }

    @Override
    public List<KObject> searchByAttrs(final Map<String, ?> attrs,
                                       final IOSearchService.Filter filter,
                                       final ClusterSegment... clusterSegments) {
        if (clusterSegments == null || clusterSegments.length == 0) {
            return emptyList();
        }
        if (attrs == null || attrs.size() == 0) {
            return emptyList();
        }
        final int totalNumHitsEstimate = searchByAttrsHits(attrs,
                                                           clusterSegments);
        return search(queryBuilder.buildQuery(attrs,
                                              clusterSegments),
                      totalNumHitsEstimate,
                      filter,
                      clusterSegments);
    }

    @Override
    public List<KObject> fullTextSearch(final String term,
                                        final IOSearchService.Filter filter,
                                        final ClusterSegment... clusterSegments) {
        if (clusterSegments == null || clusterSegments.length == 0) {
            return emptyList();
        }
        final int totalNumHitsEstimate = fullTextSearchHits(term,
                                                            clusterSegments);
        return search(queryBuilder.buildQuery(term,
                                              clusterSegments),
                      totalNumHitsEstimate,
                      filter,
                      clusterSegments);
    }

    @Override
    public int searchByAttrsHits(final Map<String, ?> attrs,
                                 final ClusterSegment... clusterSegments) {
        if (clusterSegments == null || clusterSegments.length == 0) {
            return 0;
        }
        if (attrs == null || attrs.size() == 0) {
            return 0;
        }
        return searchHits(queryBuilder.buildQuery(attrs,
                                                  clusterSegments),
                          clusterSegments);
    }

    @Override
    public int fullTextSearchHits(final String term,
                                  final ClusterSegment... clusterSegments) {
        if (clusterSegments == null || clusterSegments.length == 0) {
            return 0;
        }
        return searchHits(queryBuilder.buildQuery(term,
                                                  clusterSegments),
                          clusterSegments);
    }

    private int searchHits(final Query query,
                           final ClusterSegment... clusterSegments) {
        final IndexSearcher index = indexManager.getIndexSearcher(clusterSegments);
        try {
            final TotalHitCountCollector collector = new TotalHitCountCollector();
            index.search(query,
                         collector);
            return collector.getTotalHits();
        } catch (final Exception ex) {
            throw new RuntimeException("Error during Query!",
                                       ex);
        } finally {
            indexManager.release(index);
        }
    }

    private List<KObject> search(final Query query,
                                 final int totalNumHitsEstimate,
                                 final IOSearchService.Filter filter,
                                 final ClusterSegment... clusterSegments) {
        final TopScoreDocCollector collector = TopScoreDocCollector.create(totalNumHitsEstimate);
        final IndexSearcher index = indexManager.getIndexSearcher(clusterSegments);
        final List<KObject> result = new ArrayList<KObject>();
        try {
            index.search(query,
                         collector);
            final ScoreDoc[] hits = collector.topDocs(0).scoreDocs;
            for (int i = 0; i < hits.length; i++) {
                final KObject kObject = toKObject(index.doc(hits[i].doc));
                if (filter.accept(kObject)) {
                    result.add(kObject);
                }
            }
        } catch (final Exception ex) {
            throw new RuntimeException("Error during Query!",
                                       ex);
        } finally {
            indexManager.release(index);
        }

        return result;
    }
}
