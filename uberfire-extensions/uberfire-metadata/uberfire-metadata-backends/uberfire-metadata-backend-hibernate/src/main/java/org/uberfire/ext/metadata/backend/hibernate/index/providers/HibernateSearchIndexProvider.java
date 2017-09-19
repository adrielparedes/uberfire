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

package org.uberfire.ext.metadata.backend.hibernate.index.providers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.ext.metadata.backend.hibernate.index.QueryAdapter;
import org.uberfire.ext.metadata.backend.hibernate.model.Indexable;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.model.KCluster;

public class HibernateSearchIndexProvider implements IndexProvider {

    private final QueryAdapter queryAdapter;
    private SearchIntegrator searchIntegrator;
    private Logger logger = LoggerFactory.getLogger(HibernateSearchIndexProvider.class);

    private final Map<String, KieTransactionContext> batchMode = new ConcurrentHashMap<>();

    private HibernateSearchAnnotationProcessor annotationProcessor;

    public HibernateSearchIndexProvider(SearchIntegrator searchIntegrator,
                                        QueryAdapter queryAdapter) {
        this.searchIntegrator = searchIntegrator;
        this.annotationProcessor = new HibernateSearchAnnotationProcessor();
        this.queryAdapter = queryAdapter;

        searchIntegrator.getIndexManager("").
    }

    @Override
    public <T extends Indexable> T index(T indexable) {

        if (logger.isDebugEnabled()) {
            logger.debug("Indexing {}",
                         indexable.getClass().getCanonicalName());
        }

        Work work = new Work(indexable,
                             indexable.getId(),
                             WorkType.ADD);

        performWork(work,
                    indexable);
        return indexable;
    }

    @Override
    public <T extends Indexable> T update(T indexable) {
        Work work = new Work(indexable,
                             indexable.getId(),
                             WorkType.UPDATE);
        this.performWork(work,
                         indexable);
        return indexable;
    }

    @Override
    public void remove(Class<? extends Indexable> clazz,
                       String id) {

        if (logger.isDebugEnabled()) {
            logger.debug("Removing {} = {}",
                         clazz.getCanonicalName(),
                         id);
        }
        Optional<? extends Indexable> found = this.findById(clazz,
                                                            id);
        found.ifPresent(instance -> {
            Work work = new Work(instance,
                                 id,
                                 WorkType.DELETE);
            performWork(work,
                        instance);
        });
    }

    @Override
    public <T extends Indexable> QueryBuilder getQueryBuilder(Class<T> clazz) {
        return this.searchIntegrator.buildQueryBuilder().forEntity(clazz).get();
    }

    @Override
    public synchronized KieTransactionContext startBatch(String clusterId) {
        return batchMode.computeIfAbsent(clusterId,
                                         kCluster -> new KieTransactionContext());
    }

    @Override
    public void commit(String clusterId) {
        Optional.ofNullable(this.batchMode.get(clusterId)).ifPresent(kieTransactionContext -> {
            kieTransactionContext.end();
            batchMode.remove(clusterId);
        });
    }

    @Override
    public boolean isTransactionInProgress(KCluster cluster) {
        boolean inProgress = this.batchMode.get(cluster.getClusterId()) != null && this.batchMode.get(cluster.getClusterId()).isTransactionInProgress();
        logger.info("In Progress: {} -> {}",
                    cluster.getClusterId(),
                    inProgress);
        return inProgress;
    }

    @Override
    public <T extends Indexable> List<T> findAll(Class<T> clazz) {
        QueryBuilder qb = this.searchIntegrator.buildQueryBuilder()
                .forEntity(clazz)
                .get();
        Query query = qb.all().createQuery();
        List<T> results = this.findByQuery(clazz,
                                           query);

        if (logger.isDebugEnabled()) {
            logger.debug("Found {} results for class {}",
                         results.size(),
                         clazz.getCanonicalName());
        }
        return results;
    }

    @Override
    public <T extends Indexable> List<T> findByQuery(Class<T> clazz,
                                                     HSQuery hsQuery) {

        List<Projection> projections = annotationProcessor.getProjections(clazz);

        List<List<Projection>> projectionResult = this.executeQuery(hsQuery,
                                                                    projections);

        List<T> found = new ArrayList<>();
        for (List<Projection> rawEntity : projectionResult) {
            T instance = annotationProcessor.projectionToInstance(clazz,
                                                                  rawEntity);
            found.add(instance);
        }

        return found;
    }

    @Override
    public <T extends Indexable> List<T> findByQuery(Class<T> clazz,
                                                     Query query) {
        HSQuery hsQuery = this.searchIntegrator.createHSQuery(this.queryAdapter.fromLuceneQuery(query),
                                                              clazz);

        return this.findByQuery(clazz,
                                hsQuery);
    }

    @Override
    public <T extends Indexable> List<T> findByQuery(Class<T> clazz,
                                                     Query query,
                                                     Sort sort) {
        Query adaptedQuery = this.queryAdapter.fromLuceneQuery(query);
        HSQuery hsQuery = this.searchIntegrator.createHSQuery(adaptedQuery,
                                                              clazz);

        hsQuery.sort(this.queryAdapter.fromSort(sort));
        return this.findByQuery(clazz,
                                hsQuery);
    }

    @Override
    public <T extends Indexable> Optional<T> findById(Class<T> clazz,
                                                      String id) {
        QueryBuilder qb = this.searchIntegrator.buildQueryBuilder()
                .forEntity(clazz)
                .get();
        Query query = qb.keyword().onField("id").matching(id.trim()).createQuery();

        return this.findByQuery(clazz,
                                query)
                .stream()
                .findFirst();
    }

    private <T> void performWork(Work work,
                                 T indexable) {
        Optional<KieTransactionContext> optional = Optional.ofNullable(this.batchMode.get(((KObjectImpl) indexable).getClusterId()));

        optional.ifPresent(tc -> this.searchIntegrator.getWorker().performWork(work,
                                                                               tc));

        if (!optional.isPresent()) {
            KieTransactionContext tc = new KieTransactionContext();
            this.searchIntegrator.getWorker().performWork(work,
                                                          tc);
            tc.end();
        }
    }

    private List<List<Projection>> executeQuery(HSQuery hsQuery,
                                                List<Projection> projections) {

        String clusterId = getClusterId(projections);

        hsQuery.enableFullTextFilter("cluster").setParameter("clusterId",
                                                             clusterId);

        hsQuery.projection(projections.stream()
                                   .map(Projection::getFieldName)
                                   .toArray(i -> new String[i]));

        List<EntityInfo> entityInfos = hsQuery.queryEntityInfos();

        List<Object[]> entities = entityInfos
                .stream()
                .map(entityInfo -> entityInfo.getProjection())
                .collect(Collectors.toList());

        List<List<Projection>> result = new ArrayList<>();

        for (Object[] entityInfoProjection : entities) {
            List<Projection> entity = new ArrayList<>();
            for (int i = 0; i < entityInfoProjection.length; i++) {
                Projection baseProjection = projections.get(i);
                entity.add(new Projection(baseProjection.getField(),
                                          baseProjection.getFieldName(),
                                          entityInfoProjection[i]));
            }
            result.add(entity);
        }

        return result;
    }

    private String getClusterId(List<Projection> projections) {
        Optional<Projection> clusterIdValue = projections.stream().filter(projection -> projection.getFieldName().equals("clusterId")).findFirst();
        return clusterIdValue.map(projection -> (String) projection.getValue()).orElse("default");
    }
}
