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

package org.uberfire.ext.metadata.backend.hibernate.configuration;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.annotations.common.reflection.ReflectionManager;
import org.hibernate.search.cfg.SearchMapping;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.cfg.spi.SearchConfigurationBase;
import org.hibernate.search.engine.service.classloading.impl.DefaultClassLoaderService;
import org.hibernate.search.engine.service.classloading.spi.ClassLoaderService;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.spi.DefaultInstanceInitializer;
import org.hibernate.search.spi.InstanceInitializer;
import org.uberfire.ext.metadata.backend.hibernate.model.Indexable;

public abstract class MetadataSearchConfigurationBase extends SearchConfigurationBase implements SearchConfiguration {

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

    public MetadataSearchConfigurationBase() {
        this(DefaultInstanceInitializer.DEFAULT_INITIALIZER,
             false);
    }

    public MetadataSearchConfigurationBase(Properties properties) {
        this(DefaultInstanceInitializer.DEFAULT_INITIALIZER,
             properties);
    }

    public MetadataSearchConfigurationBase(InstanceInitializer init,
                                           boolean expectsJPAAnnotations) {
        this(init,
             new Properties());
    }

    private MetadataSearchConfigurationBase(InstanceInitializer initializer,
                                            Properties properties) {
        this.initializer = initializer;
        this.classes = new HashMap<>();
        this.properties = properties;
        this.providedServices = new HashMap<>();
        this.classLoaderService = new DefaultClassLoaderService();
    }

    public MetadataSearchConfigurationBase addProperty(String key,
                                                       String value) {
        properties.setProperty(key,
                               value);
        return this;
    }

    public MetadataSearchConfigurationBase addClass(Class<? extends Indexable> indexed) {
        classes.put(indexed.getName(),
                    indexed);
        return this;
    }

    public MetadataSearchConfigurationBase addClasses(List<Class<? extends Indexable>> indexables) {
        for (Class<? extends Indexable> indexable : indexables) {
            this.addClass(indexable);
        }
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
    public Map<Class<? extends Service>, Object> getProvidedServices() {
        return providedServices;
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
