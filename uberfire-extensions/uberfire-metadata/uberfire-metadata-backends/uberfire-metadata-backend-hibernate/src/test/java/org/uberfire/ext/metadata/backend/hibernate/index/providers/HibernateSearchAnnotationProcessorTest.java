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

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.uberfire.ext.metadata.backend.hibernate.model.PathIndex;

import static org.junit.Assert.*;

public class HibernateSearchAnnotationProcessorTest {

    private HibernateSearchAnnotationProcessor processor;

    @Before
    public void setUp() {
        processor = new HibernateSearchAnnotationProcessor();
    }

    @Test
    public void testProjections() {
        List<Projection> projections = processor.getProjections(PathIndex.class);
        assertEquals("id",
                     projections.get(0).getFieldName());
        assertEquals("pathWithName",
                     projections.get(1).getFieldName());
        assertEquals("path",
                     projections.get(2).getFieldName());
    }

    @Test
    public void testProjectionToObject() {
        List<Projection> projections = new ArrayList<>();
        projections.add(new Projection(FieldUtils.getField(PathIndex.class,
                                                           "id",
                                                           true),
                                       "id",
                                       "1234"));
        projections.add(new Projection(FieldUtils.getField(PathIndex.class,
                                                           "pathOne",
                                                           true),
                                       "pathWithName",
                                       "path/with/name"));
        projections.add(new Projection(FieldUtils.getField(PathIndex.class,
                                                           "path",
                                                           true),
                                       "path",
                                       "simple/path"));

        PathIndex instance = processor.projectionToInstance(PathIndex.class,
                                                            projections);
        assertEquals(projections.get(0).getValue(),
                     instance.getId());
        assertEquals(projections.get(1).getValue(),
                     instance.getPathOne());
        assertEquals(projections.get(2).getValue(),
                     instance.getPath());
    }
}