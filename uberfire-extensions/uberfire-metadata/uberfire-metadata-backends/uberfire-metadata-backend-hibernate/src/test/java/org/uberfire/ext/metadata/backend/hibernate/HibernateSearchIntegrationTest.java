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
import org.uberfire.ext.metadata.backend.hibernate.preferences.HibernateSearchPreferences;

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
    public void testUpdateIndexedObject() throws InterruptedException {

        KObjectImpl pathIndex = new KObjectImpl();
        pathIndex.setKey("original");

        this.provider.index(pathIndex);

        Thread.sleep(1000);

        KObjectImpl entity = this.provider.findById(KObjectImpl.class,
                                                    pathIndex.getId()).get();

        entity.setKey("updated");
        this.provider.update(entity);

        Thread.sleep(1000);

        KObjectImpl updated = this.provider.findById(KObjectImpl.class,
                                                     pathIndex.getId()).get();

        Thread.sleep(1000);

        assertEquals("updated",
                     updated.getKey());
    }

    @Test
    public void testSearchById() throws InterruptedException, IOException {

        KPropertyImpl kProperty = new KPropertyImpl("type",
                                                    "java",
                                                    true);

        KObjectImpl kObject = new KObjectImpl();
        kObject.setProperties(Arrays.asList(kProperty));

        KObjectImpl indexed = this.provider.index(kObject);

        Thread.sleep(1000);

        Optional<KObjectImpl> found = this.provider.findById(KObjectImpl.class,
                                                             indexed.getId());

        assertTrue(found.isPresent());
    }

    @Test
    public void testSearchByQuery() throws InterruptedException {

        KPropertyImpl kProperty = new KPropertyImpl("type",
                                                    "java",
                                                    true);

        KObjectImpl kObject = new KObjectImpl();
        kObject.setClusterId("java");
        kObject.setProperties(Arrays.asList(kProperty));

        KPropertyImpl kProperty2 = new KPropertyImpl("type",
                                                     "mvel",
                                                     true);

        KObjectImpl kObject2 = new KObjectImpl();
        kObject2.setClusterId("java");
        kObject2.setProperties(Arrays.asList(kProperty2));

        KObjectImpl indexed = this.provider.index(kObject);
        KObjectImpl indexed2 = this.provider.index(kObject2);

        Thread.sleep(1000);

        QueryBuilder queryBuilder = this.provider.getQueryBuilder(KObjectImpl.class);

        Query query = queryBuilder.keyword()
                .onField("properties.type")
                .ignoreFieldBridge()
                .ignoreAnalyzer()
                .matching("java")
                .createQuery();

        List<KObjectImpl> found = this.provider.findByQuery(KObjectImpl.class,
                                                            query);

        assertEquals(1,
                     found.size());
        assertEquals(indexed.getId(),
                     found.get(0).getId());
    }

    @Test
    public void testRemoveIndexed() throws InterruptedException, IOException {

        KObjectImpl pathIndex = new KObjectImpl();
        pathIndex.setKey("key");

        KObjectImpl indexed = this.provider.index(pathIndex);

        Thread.sleep(1000);

        this.provider.remove(KObjectImpl.class,
                             indexed.getId());

        Thread.sleep(1000);

        Optional<KObjectImpl> found = this.provider.findById(KObjectImpl.class,
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
        kObject.setClusterId("java");
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
