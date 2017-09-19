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

import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.uberfire.ext.metadata.search.ClusterSegment;
import org.uberfire.ext.metadata.search.DateRange;

import static org.apache.lucene.search.BooleanClause.Occur.MUST;
import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;
import static org.apache.lucene.search.NumericRangeQuery.newLongRange;
import static org.uberfire.ext.metadata.engine.MetaIndexEngine.FULL_TEXT_FIELD;

public class QueryBuilder {

    private final QueryParser queryParser;

    public QueryBuilder(final Analyzer analyzer) {
        this.queryParser = new QueryParser(FULL_TEXT_FIELD,
                                           analyzer);
        this.queryParser.setAllowLeadingWildcard(true);
    }

    public Query buildQuery(final Map<String, ?> attrs,
                            final ClusterSegment... clusterSegments) {
        final BooleanQuery query = new BooleanQuery();
        for (final Map.Entry<String, ?> entry : attrs.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue() instanceof DateRange) {
                final Long from = ((DateRange) entry.getValue()).after().getTime();
                final Long to = ((DateRange) entry.getValue()).before().getTime();
                query.add(newLongRange(key,
                                       from,
                                       to,
                                       true,
                                       true),
                          MUST);
            } else if (entry.getValue() instanceof String) {
                query.add(new WildcardQuery(new Term(key,
                                                     entry.getValue().toString())),
                          MUST);
            } else if (entry.getValue() instanceof Boolean) {
                query.add(new TermQuery(new Term(key,
                                                 ((Boolean) entry.getValue()) ? "0" : "1")),
                          MUST);
            }
        }
        return composeQuery(query,
                            clusterSegments);
    }

    public Query buildQuery(final String term,
                            final ClusterSegment... clusterSegments) {

        Query fullText;
        try {
            fullText = queryParser.parse(term);
            if (fullText.toString().isEmpty()) {
                fullText = new WildcardQuery(new Term(FULL_TEXT_FIELD,
                                                      format(term) + "*"));
            }
        } catch (ParseException ex) {
            fullText = new WildcardQuery(new Term(FULL_TEXT_FIELD,
                                                  format(term)));
        }

        return composeQuery(fullText,
                            clusterSegments);
    }

    private Query composeQuery(final Query query,
                               final ClusterSegment... clusterSegments) {
        if (clusterSegments == null || clusterSegments.length == 0) {
            return query;
        }

        final BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(query,
                         MUST);

        final BooleanClause.Occur occur = (clusterSegments.length == 1 ? MUST : SHOULD);
        for (ClusterSegment clusterSegment : clusterSegments) {
            final BooleanQuery clusterSegmentQuery = new BooleanQuery();
            addClusterIdTerms(clusterSegmentQuery,
                              clusterSegment);
            addSegmentIdTerms(clusterSegmentQuery,
                              clusterSegment);
            booleanQuery.add(clusterSegmentQuery,
                             occur);
        }

        return booleanQuery;
    }

    private void addClusterIdTerms(final BooleanQuery query,
                                   final ClusterSegment clusterSegment) {
        if (clusterSegment.getClusterId() != null) {
            final Query cluster = new TermQuery(new Term("cluster.id",
                                                         clusterSegment.getClusterId()));
            query.add(cluster,
                      MUST);
        }
    }

    private void addSegmentIdTerms(final BooleanQuery query,
                                   final ClusterSegment clusterSegment) {
        if (clusterSegment.segmentIds() == null || clusterSegment.segmentIds().length == 0) {
            return;
        }

        if (clusterSegment.segmentIds().length == 1) {
            final Query segment = new TermQuery(new Term("segment.id",
                                                         clusterSegment.segmentIds()[0]));
            query.add(segment,
                      MUST);
        } else {
            final BooleanQuery segments = new BooleanQuery();
            for (final String segmentId : clusterSegment.segmentIds()) {
                final Query segment = new TermQuery(new Term("segment.id",
                                                             segmentId));
                segments.add(segment,
                             SHOULD);
            }
            query.add(segments,
                      MUST);
        }
    }

    private String format(final String term) {
        return term.toLowerCase();
    }
}
