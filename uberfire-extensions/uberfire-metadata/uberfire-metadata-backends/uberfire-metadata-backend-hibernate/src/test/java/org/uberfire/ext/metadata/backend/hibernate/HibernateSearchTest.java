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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.apache.lucene.search.Query;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.engine.spi.EntityInfo;
import org.hibernate.search.query.engine.spi.HSQuery;
import org.hibernate.search.spi.DefaultInstanceInitializer;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.spi.SearchIntegratorBuilder;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.backend.hibernate.util.AnnotationUtils;

public class HibernateSearchTest {

    private SearchIntegrator searchIntegrator;

    private Logger logger = LoggerFactory.getLogger(HibernateSearchTest.class);

    private ObjectMapper mapper = new ObjectMapper();

    @Before
    public void setUp() throws Exception {

        KieSearchConfiguration searchConfiguration = new KieSearchConfiguration();
        searchConfiguration.addClass(KObjectImpl.class);
        searchIntegrator = new SearchIntegratorBuilder()
                .configuration(searchConfiguration)
                .buildSearchIntegrator();
    }

    @Test
    public void test() throws InterruptedException, IOException {

        KObjectImpl kObject = new KObjectImpl();
        kObject.setId(Long.toHexString(Double.doubleToLongBits(Math.random())));
        kObject.setData("hendrix");

        Work work = new Work(kObject,
                             kObject.getId(),
                             WorkType.ADD,
                             false);
        KieTransactionContext tc = new KieTransactionContext();
        searchIntegrator.getWorker().performWork(work,
                                                 tc);
        tc.end();

        QueryBuilder qb = searchIntegrator.buildQueryBuilder()
                .forEntity(KObjectImpl.class)
                .get();
        Query query = qb.all().createQuery();

        HSQuery hsQuery = searchIntegrator.createHSQuery(query,
                                                         KObjectImpl.class);

        List<Field> idFields = AnnotationUtils.getFieldsNameWithAnnotation(KObjectImpl.class,
                                                                           DocumentId.class);

        List<Field> fieldFields = AnnotationUtils.getFieldsNameWithAnnotation(KObjectImpl.class,
                                                                              org.hibernate.search.annotations.Field.class);

        List<String> projectionFields = new ArrayList<>();
        projectionFields.add(idFields.get(0).getName());

        for (Field field : fieldFields) {
            String fieldName = field.getName();
            org.hibernate.search.annotations.Field annotation = field.getAnnotation(org.hibernate.search.annotations.Field.class);
            if (annotation.name() != null && !annotation.name().isEmpty()) {
                fieldName = annotation.name();
            }
            projectionFields.add(fieldName);
        }

        hsQuery.projection(projectionFields.toArray(new String[projectionFields.size()]));
        List<EntityInfo> entityInfos = hsQuery.queryEntityInfos();

        entityInfos.stream().map(entityInfo -> entityInfo.getProjection());

        for (EntityInfo entityInfo : entityInfos) {
            entityInfo.populateWithEntityInstance(new KObjectImpl());
            logger.info(mapper.writeValueAsString(entityInfo.getProjection()));
//            KObjectImpl instance = new KObjectImpl();
        }
    }

    private class KieTransactionContext implements TransactionContext {

        private boolean progress = true;
        private List<Synchronization> syncs = new ArrayList<>();

        @Override
        public boolean isTransactionInProgress() {
            return progress;
        }

        @Override
        public Object getTransactionIdentifier() {
            return this;
        }

        @Override
        public void registerSynchronization(Synchronization synchronization) {
            syncs.add(synchronization);
        }

        public void end() {
            this.progress = false;
            for (Synchronization sync : syncs) {
                sync.beforeCompletion();
            }

            for (Synchronization sync : syncs) {
                sync.afterCompletion(Status.STATUS_COMMITTED);
            }
        }
    }

    private class KieSearchConfiguration extends SearchConfigurationBase implements SearchConfiguration {

        private Map<String, Class<?>> classes;
        private Properties properties;
        private HashMap<Class<? extends Service>, Object> providedServices;
        private InstanceInitializer initializer;
        private SearchMapping programmaticMapping;
        private boolean transactionsExpected = true;
        private boolean indexMetadataComplete = true;
        private boolean deleteByTermEnforced = false;
        private boolean idProvidedImplicit = false;
        private ClassLoaderService classLoaderService;

        public KieSearchConfiguration() {
            this(DefaultInstanceInitializer.DEFAULT_INITIALIZER,
                 false);
        }

        public KieSearchConfiguration(Properties properties) {
            this(DefaultInstanceInitializer.DEFAULT_INITIALIZER,
                 properties);
        }

        public KieSearchConfiguration(InstanceInitializer init,
                                      boolean expectsJPAAnnotations) {
            this(init,
                 new Properties());
        }

        private KieSearchConfiguration(InstanceInitializer init,
                                       Properties properties) {
            this.initializer = init;
            this.classes = new HashMap<String, Class<?>>();
            this.properties = properties;
            this.providedServices = new HashMap<Class<? extends Service>, Object>();
            this.classLoaderService = new DefaultClassLoaderService();
            addProperty("hibernate.search.default.directory_provider",
                        "org.hibernate.search.store.impl.FSDirectoryProvider");
            addProperty("hibernate.search.default.indexBase",
                        "/tmp/lucene/indexes");
//            addProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().toString() );
        }

        public KieSearchConfiguration addProperty(String key,
                                                  String value) {
            properties.setProperty(key,
                                   value);
            return this;
        }

        public KieSearchConfiguration addClass(Class<?> indexed) {
            classes.put(indexed.getName(),
                        indexed);
            return this;
        }

        @Override
        public Iterator<Class<?>> getClassMappings() {
            return classes.values().iterator();
        }

        @Override
        public Class<?> getClassMapping(String name) {
            return classes.get(name);
        }

        @Override
        public String getProperty(String propertyName) {
            return properties.getProperty(propertyName);
        }

        @Override
        public Properties getProperties() {
            return properties;
        }

        @Override
        public ReflectionManager getReflectionManager() {
            return null;
        }

        @Override
        public SearchMapping getProgrammaticMapping() {
            return programmaticMapping;
        }

        @Override
        public Map<Class<? extends org.hibernate.search.engine.service.spi.Service>, Object> getProvidedServices() {
            return providedServices;
        }

        public KieSearchConfiguration setProgrammaticMapping(SearchMapping programmaticMapping) {
            this.programmaticMapping = programmaticMapping;
            return this;
        }

        public void addProvidedService(Class<? extends Service> serviceRole,
                                       Object service) {
            providedServices.put(serviceRole,
                                 service);
        }

        @Override
        public boolean isTransactionManagerExpected() {
            return this.transactionsExpected;
        }

        @Override
        public InstanceInitializer getInstanceInitializer() {
            return initializer;
        }

        @Override
        public boolean isIndexMetadataComplete() {
            return indexMetadataComplete;
        }

        @Override
        public boolean isDeleteByTermEnforced() {
            return deleteByTermEnforced;
        }

        @Override
        public boolean isIdProvidedImplicit() {
            return idProvidedImplicit;
        }

        @Override
        public ClassLoaderService getClassLoaderService() {
            return classLoaderService;
        }
    }
}
