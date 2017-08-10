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

import java.util.List;
import java.util.Optional;

import org.apache.lucene.search.Query;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.uberfire.ext.metadata.backend.hibernate.model.Indexable;

public interface IndexProvider {

    <T extends Indexable> T index(T indexable);

    <T extends Indexable> T update(T indexable);

    <T extends Indexable> List<T> findAll(Class<T> clazz);

    <T extends Indexable> List<T> findByQuery(Class<T> clazz,
                                              Query query);

    <T extends Indexable> Optional<T> findById(Class<T> clazz,
                                               String id);

    void remove(Class<? extends Indexable> clazz,
                String id);

    <T extends Indexable> QueryBuilder getQueryBuilder(Class<T> clazz);
}
