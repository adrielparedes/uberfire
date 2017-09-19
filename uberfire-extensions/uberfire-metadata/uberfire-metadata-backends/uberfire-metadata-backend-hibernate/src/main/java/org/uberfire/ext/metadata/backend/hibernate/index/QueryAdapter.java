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

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.CachingWrapperQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.WildcardQuery;
import org.hibernate.search.query.dsl.impl.RemoteMatchQuery;
import org.hibernate.search.query.dsl.impl.RemotePhraseQuery;
import org.hibernate.search.query.dsl.impl.RemoteSimpleQueryStringQuery;
import org.hibernate.search.spatial.impl.SpatialHashQuery;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.HibernateSearchAnnotationProcessor;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.Projection;
import org.uberfire.ext.metadata.backend.hibernate.model.Indexable;

import static org.uberfire.commons.validation.PortablePreconditions.checkNotEmpty;
import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

public class QueryAdapter {

    private final String propertiesFieldName;
    private final Class<? extends Indexable> indexable;
    private final List<String> blacklist;

    public QueryAdapter(Class<? extends Indexable> indexable,
                        String propertiesFieldName) {
        this(indexable,
             propertiesFieldName,
             new ArrayList<>());
    }

    public QueryAdapter(Class<? extends Indexable> indexable,
                        String propertiesFieldName,
                        List<String> blacklist) {
        this.indexable = checkNotNull("indexable",
                                      indexable);
        this.propertiesFieldName = checkNotEmpty("propertiesFieldName",
                                                 propertiesFieldName);
        this.blacklist = blacklist;
    }

    public Map<String, Analyzer> fromAnalyzers(Map<String, Analyzer> analyzers) {
        return analyzers.entrySet().stream().map(entity -> new AbstractMap.SimpleEntry<>(this.prependIfNeccesary(entity.getKey()),
                                                                                         entity.getValue())).collect(Collectors.toMap(
                e -> e.getKey(),
                e -> e.getValue()
        ));
    }

    public Sort fromSort(Sort sort) {
        SortField[] sortFields = sort.getSort();

        List<SortField> newSortFields = Arrays.stream(sortFields).map(sortField -> new SortField(prependIfNeccesary(sortField.getField()),
                                                                                                 sortField.getType())).collect(Collectors.toList());
        return new Sort(newSortFields.toArray(new SortField[newSortFields.size()]));
    }

    public Query fromLuceneQuery(Query query) {
        if (query instanceof TermQuery) {
            return convertTermQuery((TermQuery) query);
        } else if (query instanceof BooleanQuery) {
            return convertBooleanQuery((BooleanQuery) query);
        } else if (query instanceof TermRangeQuery) {
            return convertTermRangeQuery((TermRangeQuery) query);
        } else if (query instanceof NumericRangeQuery) {
            return convertNumericRangeQuery((NumericRangeQuery<?>) query);
        } else if (query instanceof WildcardQuery) {
            return convertWildcardQuery((WildcardQuery) query);
        } else if (query instanceof PrefixQuery) {
            return convertPrefixQuery((PrefixQuery) query);
        } else if (query instanceof FuzzyQuery) {
            return convertFuzzyQuery((FuzzyQuery) query);
        } else if (query instanceof RemotePhraseQuery) {
            return convertRemotePhraseQuery((RemotePhraseQuery) query);
        } else if (query instanceof RemoteMatchQuery) {
            return convertRemoteMatchQuery((RemoteMatchQuery) query);
        } else if (query instanceof RemoteSimpleQueryStringQuery) {
            return convertRemoteSimpleQueryStringQuery((RemoteSimpleQueryStringQuery) query);
        } else if (query instanceof ConstantScoreQuery) {
            return convertConstantScoreQuery((ConstantScoreQuery) query);
        } else if (query instanceof FilteredQuery) {
            return convertFilteredQuery((FilteredQuery) query);
        } else if (query instanceof QueryWrapperFilter) {
            Query result = fromLuceneQuery(((QueryWrapperFilter) query).getQuery());
            return wrapBoostIfNecessary(result,
                                        query.getBoost());
        } else if (query instanceof SpatialHashQuery) {
            return convertSpatialHashFilter((SpatialHashQuery) query);
        } else if (query instanceof PhraseQuery) {
            return convertPhraseQuery((PhraseQuery) query);
        } else if (query instanceof BoostQuery) {
            Query result = fromLuceneQuery(((BoostQuery) query).getQuery());
            return wrapBoostIfNecessary(result,
                                        query.getBoost());
        } else if (query instanceof CachingWrapperQuery) {
            Query result = fromLuceneQuery(((CachingWrapperQuery) query).getQuery());
            return wrapBoostIfNecessary(result,
                                        query.getBoost());
        } else if (query instanceof org.apache.lucene.search.CachingWrapperQuery) {
            Query result = fromLuceneQuery(((org.apache.lucene.search.CachingWrapperQuery) query).getQuery());
            return wrapBoostIfNecessary(result,
                                        query.getBoost());
        } else if (query instanceof org.apache.lucene.search.CachingWrapperFilter) {
            Query result = fromLuceneQuery(((org.apache.lucene.search.CachingWrapperFilter) query).getFilter());
            return wrapBoostIfNecessary(result,
                                        query.getBoost());
        } else {
            return query;
        }
    }

    private Query convertPhraseQuery(PhraseQuery query) {
        PhraseQuery.Builder builder = new PhraseQuery.Builder();
        Arrays.stream(query.getTerms()).forEach(term -> builder.add(this.convertTerm(term)));
        builder.setSlop(query.getSlop());
        return builder.build();
    }

    private Query convertSpatialHashFilter(SpatialHashQuery query) {
        return new SpatialHashQuery(query.getSpatialHashCellsIds(),
                                    prependIfNeccesary(query.getFieldName()));
    }

    private Query wrapBoostIfNecessary(Query result,
                                       float boost) {
        return new BoostQuery(this.fromLuceneQuery(result),
                              boost);
    }

    private Query convertFilteredQuery(FilteredQuery query) {
        return new FilteredQuery(this.fromLuceneQuery(query.getQuery()),
                                 query.getFilter(),
                                 query.getFilterStrategy());
    }

    private Query convertConstantScoreQuery(ConstantScoreQuery query) {
        return new ConstantScoreQuery(this.fromLuceneQuery(query));
    }

    private Query convertRemoteSimpleQueryStringQuery(RemoteSimpleQueryStringQuery query) {
        RemoteSimpleQueryStringQuery.Builder builder = new RemoteSimpleQueryStringQuery.Builder();
        query.getFields().forEach(field -> builder.field(prependIfNeccesary(field.getName()),
                                                         field.getBoost()));
        builder.originalRemoteAnalyzerReference(query.getOriginalRemoteAnalyzerReference());
        builder.queryRemoteAnalyzerReference(query.getQueryRemoteAnalyzerReference());
        builder.withAndAsDefaultOperator(query.isMatchAll());
        builder.query(query.getQuery());
        return builder.build();
    }

    private Query convertRemoteMatchQuery(RemoteMatchQuery query) {
        return new RemoteMatchQuery.Builder()
                .field(prependIfNeccesary(query.getField()))
                .searchTerms(query.getSearchTerms())
                .maxEditDistance(query.getMaxEditDistance())
                .analyzerReference(query.getOriginalAnalyzerReference(),
                                   query.getQueryAnalyzerReference())
                .build();
    }

    private Query convertRemotePhraseQuery(RemotePhraseQuery query) {
        return new RemotePhraseQuery(prependIfNeccesary(query.getField()),
                                     query.getSlop(),
                                     query.getPhrase(),
                                     query.getOriginalAnalyzerReference(),
                                     query.getQueryAnalyzerReference());
    }

    private Query convertFuzzyQuery(FuzzyQuery query) {
        return new FuzzyQuery(this.convertTerm(query.getTerm()),
                              query.getMaxEdits(),
                              query.getPrefixLength());
    }

    private Query convertPrefixQuery(PrefixQuery query) {
        return new PrefixQuery(this.convertTerm(query.getPrefix()));
    }

    private Query convertWildcardQuery(WildcardQuery query) {
        return new WildcardQuery(convertTerm(query.getTerm()));
    }

    private Term convertTerm(Term term) {
        return new Term(prependIfNeccesary(term.field()),
                        term.bytes());
    }

    private <T> Query convertNumericRangeQuery(NumericRangeQuery<?> query) {
        String fieldName = prependIfNeccesary(query.getField());
        if (checkInstanceOf(query,
                            Integer.class)) {
            return NumericRangeQuery.newIntRange(fieldName,
                                                 query.getPrecisionStep(),
                                                 query.getMin().intValue(),
                                                 query.getMax().intValue(),
                                                 query.includesMin(),
                                                 query.includesMax());
        } else if (checkInstanceOf(query,
                                   Double.class)) {
            return NumericRangeQuery.newDoubleRange(fieldName,
                                                    query.getPrecisionStep(),
                                                    query.getMin().doubleValue(),
                                                    query.getMax().doubleValue(),
                                                    query.includesMin(),
                                                    query.includesMax());
        } else if (checkInstanceOf(query,
                                   Float.class)) {
            return NumericRangeQuery.newFloatRange(fieldName,
                                                   query.getPrecisionStep(),
                                                   query.getMin().floatValue(),
                                                   query.getMax().floatValue(),
                                                   query.includesMin(),
                                                   query.includesMax());
        } else if (checkInstanceOf(query,
                                   Long.class)) {
            return NumericRangeQuery.newLongRange(fieldName,
                                                  query.getPrecisionStep(),
                                                  query.getMin().longValue(),
                                                  query.getMax().longValue(),
                                                  query.includesMin(),
                                                  query.includesMax());
        } else {
            return query;
        }
    }

    private boolean checkInstanceOf(NumericRangeQuery<?> query,
                                    Class<? extends Number> integerClass) {
        return integerClass.isInstance(query.getMin()) || integerClass.isInstance(query.getMax());
    }

    private Query convertTermRangeQuery(TermRangeQuery query) {
        return new TermRangeQuery(prependIfNeccesary(query.getField()),
                                  query.getLowerTerm(),
                                  query.getUpperTerm(),
                                  query.includesLower(),
                                  query.includesUpper());
    }

    private Query convertBooleanQuery(BooleanQuery query) {
        List<Query> musts = new ArrayList<>();
        List<Query> mustNots = new ArrayList<>();
        List<Query> filters = new ArrayList<>();
        List<Query> shoulds = new ArrayList<>();
        for (BooleanClause clause : query.clauses()) {
            switch (clause.getOccur()) {
                case MUST:
                    musts.add(fromLuceneQuery(clause.getQuery()));
                    break;
                case FILTER:
                    filters.add(fromLuceneQuery(clause.getQuery()));
                    break;
                case MUST_NOT:
                    mustNots.add(fromLuceneQuery(clause.getQuery()));
                    break;
                case SHOULD:
                    shoulds.add(fromLuceneQuery(clause.getQuery()));
                    break;
            }
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        musts.forEach(q -> builder.add(q,
                                       BooleanClause.Occur.MUST));
        filters.forEach(q -> builder.add(q,
                                         BooleanClause.Occur.FILTER));
        mustNots.forEach(q -> builder.add(q,
                                          BooleanClause.Occur.MUST_NOT));
        shoulds.forEach(q -> builder.add(q,
                                         BooleanClause.Occur.SHOULD));

        return builder.build();
    }

    private Query convertTermQuery(TermQuery query) {
        return new TermQuery(this.convertTerm(query.getTerm()));
    }

    protected String prependIfNeccesary(String field) {
        if (this.blacklist.contains(field) || field == null) {
            return field;
        }
        HibernateSearchAnnotationProcessor processor = new HibernateSearchAnnotationProcessor();
        List<Projection> projections = processor.getProjections(indexable);
        boolean isIncluded = projections.stream().anyMatch(projection -> projection.getFieldName().equals(field));
        if (!isIncluded) {
            return this.propertiesFieldName + "." + field;
        } else {
            return field;
        }
    }
}
