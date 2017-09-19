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

import java.util.Optional;
import java.util.stream.StreamSupport;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Analyzer;
import org.hibernate.search.annotations.ClassBridge;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.FullTextFilterDef;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.hibernate.search.filter.ShardSensitiveOnlyFilter;
import org.uberfire.ext.metadata.backend.hibernate.analyzer.FilenameAnalyzer;
import org.uberfire.ext.metadata.engine.MetaIndexEngine;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.model.KProperty;
import org.uberfire.ext.metadata.model.schema.MetaType;

@Indexed
@FullTextFilterDef(name = "cluster", impl = ShardSensitiveOnlyFilter.class)
@ClassBridge(name = MetaIndexEngine.FULL_TEXT_FIELD, analyze = Analyze.YES, store = Store.YES, impl = FullTextClassBridge.class)
@Analyzer(impl = StandardAnalyzer.class)
public class KObjectImpl extends Indexable implements KObject {

    @Field(name = "cluster.id", analyze = Analyze.NO, store = Store.YES)
    private String clusterId;

    @Field(name = "segment.id", analyze = Analyze.NO, store = Store.YES)
    private String segmentId;

    @Field(analyze = Analyze.YES, store = Store.YES)
    private String key;

    @Field(analyze = Analyze.YES, store = Store.YES)
    @FieldBridge(impl = KPropertyBridge.class)
    private Iterable<KProperty<?>> properties;

    private boolean fullText;

    //    @Field(analyze = Analyze.NO, store = Store.YES)
    private MetaType type;

    @Override
    public String getClusterId() {
        return this.clusterId;
    }

    @Override
    public String getSegmentId() {
        return this.segmentId;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public Iterable<KProperty<?>> getProperties() {
        return this.properties;
    }

    public void setProperties(Iterable<KProperty<?>> properties) {
        this.properties = properties;
    }

    @Override
    public MetaType getType() {
        return this.type;
    }

    @Override
    public boolean fullText() {
        return this.fullText;
    }

    public void setFullText(boolean fullText) {
        this.fullText = fullText;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public void setType(MetaType type) {
        this.type = type;
    }

    public void setSegmentId(String segmentId) {
        this.segmentId = segmentId;
    }

    public Optional<KProperty<?>> getProperty(String name) {
        return StreamSupport.stream(this.getProperties().spliterator(),
                                    false)
                .filter(kProperty -> kProperty.getName().equals(name)).findAny();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("KObject{" +
                                                     ", key='" + getKey() + '\'' +
                                                     ", id='" + getId() + '\'' +
                                                     ", type=" + getType() +
                                                     ", clusterId='" + getClusterId() + '\'' +
                                                     ", segmentId='" + getSegmentId() + '\'');

        for (KProperty<?> xproperty : getProperties()) {
            sb.append(", " + xproperty.getName() + "='" + xproperty.getValue() + '\'');
        }

        sb.append('}');

        return sb.toString();
    }
}
