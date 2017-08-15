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

import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.spi.SearchIntegrator;
import org.uberfire.ext.metadata.backend.hibernate.configuration.ConfigurationManager;
import org.uberfire.ext.metadata.backend.hibernate.configuration.MetadataSearchConfigurationBase;
import org.uberfire.ext.metadata.backend.hibernate.model.Indexable;
import org.uberfire.ext.metadata.backend.hibernate.preferences.HibernateSearchPreferences;

public class SearchIntegratorBuilder {

    private SearchConfiguration searchConfiguration;
    private HibernateSearchPreferences preferences;
    private List<Class<? extends Indexable>> classes;

    public SearchIntegratorBuilder() {

        this.preferences = new HibernateSearchPreferences();
        this.classes = new ArrayList<>();
    }

    public SearchIntegratorBuilder withPreferences(HibernateSearchPreferences preferences) {
        this.preferences = preferences;
        return this;
    }

    public SearchIntegratorBuilder addClass(Class<? extends Indexable> indexable) {
        this.classes.add(indexable);
        return this;
    }

    public SearchIntegrator build() {
        MetadataSearchConfigurationBase searchConfiguration = new ConfigurationManager(preferences).getSearchConfiguration();
        searchConfiguration.addClasses(classes);
        return new org.hibernate.search.spi.SearchIntegratorBuilder()
                .configuration(searchConfiguration)
                .buildSearchIntegrator();
    }
}
