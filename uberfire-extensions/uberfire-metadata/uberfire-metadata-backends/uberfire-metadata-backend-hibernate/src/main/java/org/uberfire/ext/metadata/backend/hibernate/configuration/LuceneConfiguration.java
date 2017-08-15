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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.ext.metadata.backend.hibernate.preferences.HibernateSearchPreferences;
import org.uberfire.ext.metadata.preferences.LucenePreferences;

public class LuceneConfiguration extends MetadataSearchConfigurationBase {

    private Logger logger = LoggerFactory.getLogger(LuceneConfiguration.class);

    public static final String HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER = "hibernate.search.default.directory_provider";
    public static final String HIBERNATE_SEARCH_DEFAULT_INDEX_BASE = "hibernate.search.default.indexBase";

    public LuceneConfiguration(HibernateSearchPreferences preferences) {
        LucenePreferences lucenePreferences = preferences.getLucenePreferences();

        addProperty(HibernateSearchPreferences.SHARDING_STRATEGY,
                    preferences.getShardingStrategy());
        addProperty(HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER,
                    lucenePreferences.getDefaultDirectoryProvider());
        addProperty(HIBERNATE_SEARCH_DEFAULT_INDEX_BASE,
                    lucenePreferences.getDefaultIndexBase());

        if (logger.isDebugEnabled()) {
            this.getProperties().forEach((key, value) -> logger.debug("{}={}",
                                                                      key,
                                                                      value));
        }
    }
}
