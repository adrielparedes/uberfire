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

package org.uberfire.ext.metadata.backend.hibernate.sharding;

import java.io.Serializable;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.store.ShardIdentifierProviderTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;

public class KObjectShardIdentifierProvider extends ShardIdentifierProviderTemplate {

    private Logger logger = LoggerFactory.getLogger(KObjectShardIdentifierProvider.class);

    @Override
    protected Set<String> loadInitialShardNames(Properties properties,
                                                BuildContext buildContext) {

        return Collections.emptySet();
    }

    @Override
    public String getShardIdentifier(Class<?> entityType,
                                     Serializable serializable,
                                     String s,
                                     Document document) {

        if (!entityType.equals(KObjectImpl.class)) {
            throw new RuntimeException("");
        }

        String identifier = Optional.ofNullable(document.getField("clusterId")).orElse(new StringField("clusterId",
                                                                                                       "default",
                                                                                                       Field.Store.YES)).stringValue();
        addShard(identifier);

        if (logger.isDebugEnabled()) {
            logger.debug("Getting shard identifier = {}",
                         identifier);
        }

        return identifier;
    }
}
