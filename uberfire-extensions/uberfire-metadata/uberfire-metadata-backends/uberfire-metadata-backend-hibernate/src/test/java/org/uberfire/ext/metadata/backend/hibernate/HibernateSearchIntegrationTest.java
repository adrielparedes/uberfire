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

package org.uberfire.ext.metadata.backend.hibernate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.lucene.search.Query;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.HibernateSearchIndexProvider;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.backend.hibernate.model.KPropertyImpl;
import org.uberfire.ext.metadata.backend.hibernate.model.PathIndex;
import org.uberfire.ext.metadata.preferences.HibernateSearchPreferences;

import static org.junit.Assert.*;

public abstract class HibernateSearchIntegrationTest {

    protected Logger logger = LoggerFactory.getLogger(HibernateSearchElasticSearchIntegrationTest.class);

    protected HibernateSearchIndexProvider provider;
    protected ObjectMapper mapper;
    protected HibernateSearchPreferences preferences;

    public void setUp() {

        this.mapper = new ObjectMapper();
        mapper.enable(SerializationConfig.Feature.INDENT_OUTPUT);
        preferences = new HibernateSearchPreferences();
    }

    @Test
    public void testIndexObjects() throws InterruptedException, IOException {

        String[] names = {"Hendrix", "Zappa"};

        for (int i = 0; i < names.length; i++) {
            KObjectImpl kObject = new KObjectImpl();
            kObject.setKey(names[i]);
            this.provider.index(kObject);
        }

        Thread.sleep(1000);

        List<KObjectImpl> entities = this.provider.findAll(KObjectImpl.class);
        logger.info(mapper.writeValueAsString(entities));
        assertEquals(2,
                     entities.size());
    }

    @Test
    public void testFieldWithNames() throws InterruptedException, IOException {

        PathIndex pathIndex = new PathIndex();
        pathIndex.setPath("simple/path");
        pathIndex.setPathOne("path/one");

        this.provider.index(pathIndex);

        Thread.sleep(1000);

        List<PathIndex> entities = this.provider.findAll(PathIndex.class);
        logger.info(mapper.writeValueAsString(entities));
        assertEquals("simple/path",
                     entities.get(0).getPath());
        assertEquals("path/one",
                     entities.get(0).getPathOne());
    }

    @Test
    public void testUpdateIndexedObject() throws InterruptedException {

        PathIndex pathIndex = new PathIndex();
        pathIndex.setPath("original");

        this.provider.index(pathIndex);

        Thread.sleep(1000);

        PathIndex entity = this.provider.findById(PathIndex.class,
                                                  pathIndex.getId()).get();

        entity.setPath("updated");
        this.provider.update(entity);

        Thread.sleep(1000);

        PathIndex updated = this.provider.findById(PathIndex.class,
                                                   pathIndex.getId()).get();

        Thread.sleep(1000);

        assertEquals("updated",
                     updated.getPath());
    }

    @Test
    public void testSearchById() throws InterruptedException, IOException {

        PathIndex pathIndex = new PathIndex();
        pathIndex.setPath("simple/path");
        pathIndex.setPathOne("path/one");

        PathIndex indexed = this.provider.index(pathIndex);

        Thread.sleep(1000);

        Optional<PathIndex> found = this.provider.findById(PathIndex.class,
                                                           indexed.getId());

        assertTrue(found.isPresent());
    }

    @Test
    public void testSearchByQuery() throws InterruptedException {

        PathIndex pathIndex1 = new PathIndex();
        pathIndex1.setPath("simple/path/one");
        pathIndex1.setPathOne("path/one");

        PathIndex pathIndex2 = new PathIndex();
        pathIndex2.setPath("simple/path/two");
        pathIndex2.setPathOne("path/two");

        PathIndex indexed = this.provider.index(pathIndex1);
        PathIndex indexed2 = this.provider.index(pathIndex2);

        Thread.sleep(1000);

        QueryBuilder queryBuilder = this.provider.getQueryBuilder(PathIndex.class);
        Query query = queryBuilder.keyword().onField("pathWithName").matching("path/two").createQuery();
        List<PathIndex> found = this.provider.findByQuery(PathIndex.class,
                                                          query);

        assertEquals(1,
                     found.size());
        assertEquals(indexed2.getPathOne(),
                     found.get(0).getPathOne());
    }

    @Test
    public void testRemoveIndexed() throws InterruptedException, IOException {

        PathIndex pathIndex = new PathIndex();
        pathIndex.setPath("simple/path");
        pathIndex.setPathOne("path/one");

        PathIndex indexed = this.provider.index(pathIndex);

        Thread.sleep(1000);

        this.provider.remove(PathIndex.class,
                             indexed.getId());

        Thread.sleep(1000);

        Optional<PathIndex> found = this.provider.findById(PathIndex.class,
                                                           indexed.getId());

        assertFalse(found.isPresent());
    }

    @Test
    public void testListOfObjects() throws InterruptedException, IOException {

        KPropertyImpl fieldName = new KPropertyImpl("fieldName",
                                                    "FIELD",
                                                    false);
        KPropertyImpl methodName = new KPropertyImpl("methodName",
                                                     "METHOD",
                                                     false);

        KObjectImpl kObject = new KObjectImpl();
        kObject.setKey("Key!!!");
        kObject.setProperties(Arrays.asList(fieldName,
                                            methodName));

        KObjectImpl indexed = this.provider.index(kObject);

        Thread.sleep(1000);

        Optional<KObjectImpl> found = this.provider.findById(KObjectImpl.class,
                                                             indexed.getId());

        logger.info(mapper.writeValueAsString(found.get()));

        assertTrue(found.isPresent());
    }
}
