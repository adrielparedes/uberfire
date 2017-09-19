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

package org.uberfire.ext.metadata.backend.hibernate.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.FieldMetadataBuilder;
import org.hibernate.search.bridge.spi.FieldType;
import org.hibernate.search.elasticsearch.bridge.spi.Elasticsearch;
import org.hibernate.search.elasticsearch.cfg.DynamicType;
import org.uberfire.ext.metadata.backend.hibernate.index.SimpleFieldFactory;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.IndexProvider;
import org.uberfire.ext.metadata.model.KProperty;

public class KPropertyBridge implements TwoWayFieldBridge,
                                        MetadataProvidingFieldBridge {

    @Override
    public Object get(String name,
                      Document document) {
        List<KPropertyImpl> children = document.getFields().stream()
                .filter(indexableField -> indexableField.name().contains(name))
                .map(indexableField -> new KPropertyImpl<Object>(indexableField.name().replace(name + ".",
                                                                                               ""),
                                                                 indexableField.stringValue(),
                                                                 false)).collect(Collectors.toList());
        return children;
    }

    @Override
    public String objectToString(Object object) {
        return object.toString();
    }

    @Override
    public void set(String name,
                    Object value,
                    Document document,
                    LuceneOptions luceneOptions) {
        SimpleFieldFactory fieldFactory = new SimpleFieldFactory(name);
        Collection<KProperty<?>> list = (Collection<KProperty<?>>) value;
        if (list != null) {

            list.forEach(kProperty -> {
                IndexableField[] fields = fieldFactory.build(kProperty);
                Arrays.stream(fields).forEach(indexableField -> document.add(indexableField));
            });
        }
    }

    @Override
    public void configureFieldMetadata(String name,
                                       FieldMetadataBuilder builder) {
        builder.field(name,
                      FieldType.OBJECT)
                .mappedOn(Elasticsearch.class)
                .dynamic(DynamicType.TRUE)
                .field(name + "." + IndexProvider.CUSTOM_FIELD_FILENAME_SORTED,
                       FieldType.STRING).sortable(true);
    }
}
