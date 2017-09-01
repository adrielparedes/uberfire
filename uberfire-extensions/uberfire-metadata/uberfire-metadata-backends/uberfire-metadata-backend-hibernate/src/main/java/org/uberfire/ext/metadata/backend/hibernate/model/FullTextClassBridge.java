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

package org.uberfire.ext.metadata.backend.hibernate.model;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.LuceneOptions;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.uberfire.ext.metadata.model.KProperty;

public class FullTextClassBridge implements TwoWayFieldBridge {

    @Override
    public void set(String s,
                    Object o,
                    Document document,
                    LuceneOptions luceneOptions) {

        KObjectImpl kObject = (KObjectImpl) o;
        if (kObject.fullText()) {

            StringBuilder allText = new StringBuilder();

            allText.append(kObject.getKey()).append('\n');

            for (final KProperty<?> property : kObject.getProperties()) {
                if (property.getValue() instanceof String) {
                    allText.append(property.getValue()).append('\n');
                }
            }

            document.add(new TextField(s,
                                       allText.toString().toLowerCase(),
                                       Field.Store.YES));
        }
    }

    @Override
    public Object get(String s,
                      Document document) {
        return document.get(s);
    }

    @Override
    public String objectToString(Object o) {
        return o.toString();
    }
}
