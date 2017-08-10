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

import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.uberfire.ext.metadata.backend.hibernate.exceptions.ProjectionException;
import org.uberfire.ext.metadata.backend.hibernate.model.Indexable;

public class HibernateSearchAnnotationProcessor {

    public List<Projection> getHibernateFieldName(Class<org.hibernate.search.annotations.Field> fieldClass,
                                                  List<Field> projectionFields) {
        return this.getFieldNames(projectionFields,
                                  field -> {
                                      org.hibernate.search.annotations.Field annotation = field.getAnnotation(fieldClass);
                                      if (annotation.name() != null && !annotation.name().isEmpty()) {
                                          return Optional.of(new Projection(field,
                                                                            annotation.name(),
                                                                            null));
                                      } else {
                                          return Optional.empty();
                                      }
                                  });
    }

    public List<Projection> getDocumentIdFieldNames(Class<DocumentId> documentIdClass,
                                                    List<Field> documentIds) {
        return this.getFieldNames(documentIds,
                                  field -> {
                                      DocumentId annotation = field.getAnnotation(documentIdClass);
                                      if (annotation.name() != null && !annotation.name().isEmpty()) {
                                          return Optional.of(new Projection(field,
                                                                            annotation.name(),
                                                                            null));
                                      } else {
                                          return Optional.empty();
                                      }
                                  });
    }

    protected List<Projection> getFieldNames(List<Field> fields,
                                             Function<Field, Optional<Projection>> annotationToName) {

        Function<Field, Projection> mapFieldToName = field -> {
            String fieldName = field.getName();
            Optional<Projection> annotationName = annotationToName.apply(field);

            return annotationName.orElse(new Projection(field,
                                                        fieldName,
                                                        null));
        };
        return fields.stream().map(mapFieldToName).collect(Collectors.toList());
    }

    public <T extends Indexable> List<Projection> getProjections(Class<T> clazz) {

        List<Projection> projections = new ArrayList<>();

        List<Field> documentIds = FieldUtils.getFieldsListWithAnnotation(clazz,
                                                                         DocumentId.class);
        List<Field> projectionFields = FieldUtils.getFieldsListWithAnnotation(clazz,
                                                                              org.hibernate.search.annotations.Field.class);

        List<Field> indexedEmbeddedFields = FieldUtils.getFieldsListWithAnnotation(clazz,
                                                                                   IndexedEmbedded.class);

        projections.addAll(this.getDocumentIdFieldNames(DocumentId.class,
                                                        documentIds));
        projections.addAll(this.getHibernateFieldName(org.hibernate.search.annotations.Field.class,
                                                      projectionFields));
        return projections;
    }

    public <T extends Indexable> T projectionToInstance(Class<T> clazz,
                                                        List<Projection> rawEntity) {
        try {
            T instance = clazz.newInstance();
            for (Projection field : rawEntity) {
                BeanUtils.setProperty(instance,
                                      field.getField().getName(),
                                      field.getValue());
            }
            return instance;
        } catch (Exception e) {
            throw new ProjectionException(MessageFormat.format("A problem occurred trying to create a {0} instance",
                                                               clazz.getCanonicalName()),
                                          e);
        }
    }
}
