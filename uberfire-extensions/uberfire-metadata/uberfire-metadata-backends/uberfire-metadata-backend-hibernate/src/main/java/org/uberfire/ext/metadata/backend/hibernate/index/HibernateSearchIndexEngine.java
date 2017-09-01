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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.search.Query;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.IndexProvider;
import org.uberfire.ext.metadata.backend.hibernate.model.Indexable;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.engine.MetaIndexEngine;
import org.uberfire.ext.metadata.model.KCluster;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.model.KObjectKey;

import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

public class HibernateSearchIndexEngine implements MetaIndexEngine {

    private Logger logger = LoggerFactory.getLogger(HibernateSearchIndexEngine.class);

    private final IndexProvider indexProvider;

    public HibernateSearchIndexEngine(final IndexProvider indexProvider) {
        this.indexProvider = checkNotNull("indexProvider",
                                          indexProvider);
    }

    @Override
    public boolean freshIndex(KCluster cluster) {
//
        QueryBuilder queryBuilder = this.indexProvider.getQueryBuilder(KObjectImpl.class);
        Query query = queryBuilder.keyword().onField("cluster.id").matching(cluster.getClusterId()).createQuery();
        List<KObjectImpl> found = this.indexProvider.findByQuery(KObjectImpl.class,
                                                                 query);

        final boolean isFreshIndex = found.isEmpty() &&
//                !batchMode.containsKey(cluster.getClusterId()) &&
                !this.indexProvider.isTransactionInProgress(cluster);

        logger.info("Is Fresh Index -> {}",
                    isFreshIndex);
        return isFreshIndex;
    }

    @Override
    public void startBatch(KCluster cluster) {
        this.indexProvider.startBatch(cluster.getClusterId());
    }

    @Override
    public void index(KObject object) {
        Optional<KObjectImpl> found = this.indexProvider.findById(KObjectImpl.class,
                                                                  object.getId());
        found.ifPresent(kObject -> this.indexProvider.remove(KObjectImpl.class,
                                                             kObject.getId()));
        this.indexProvider.index((Indexable) object);
    }

    @Override
    public void index(KObject... objects) {
        Arrays.stream(objects).forEach(kObject -> index(kObject));
    }

    @Override
    public void rename(KObjectKey from,
                       KObject to) {

        this.indexProvider.findById(KObjectImpl.class,
                                    from.getId());
    }

    @Override
    public void delete(KCluster cluster) {
    }

    @Override
    public void delete(KObjectKey objectKey) {
        this.indexProvider.remove(KObjectImpl.class,
                                  objectKey.getId());
    }

    @Override
    public void delete(KObjectKey... objectsKey) {
        Arrays.stream(objectsKey).forEach(kObjectKey -> delete(kObjectKey));
    }

    @Override
    public void commit(KCluster cluster) {
        this.indexProvider.commit(cluster.getClusterId());
    }

    @Override
    public void beforeDispose(Runnable callback) {

    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public void dispose() {

    }
}
