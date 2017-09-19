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

package org.uberfire.ext.metadata.backend.hibernate.index;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.junit.Before;
import org.junit.Test;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;

import static org.junit.Assert.*;

public class QueryAdapterTest {

    private QueryAdapter queryAdapter;

    @Before
    public void setUp() {
        this.queryAdapter = new QueryAdapter(KObjectImpl.class,
                                             "properties");
    }

    @Test
    public void testPrependNotNecessary() {
        String isNecessary = this.queryAdapter.prependIfNeccesary("id");
        assertEquals("id",
                     isNecessary);
    }

    @Test
    public void testPrependIsNecessary() {
        String isNecessary = this.queryAdapter.prependIfNeccesary("dcore.author");
        assertEquals("properties.dcore.author",
                     isNecessary);
    }

    @Test
    public void testQueryRewrite() {
        TermQuery originalQuery = new TermQuery(new Term("dcore.author",
                                                         "adriel"));
        Query query = this.queryAdapter.fromLuceneQuery(originalQuery);
        assertEquals("properties.dcore.author:adriel",
                     query.toString());
    }

    @Test
    public void testBooleanQueryRewrite() throws ParseException {
        QueryParser queryParser = new QueryParser("",
                                                  new WhitespaceAnalyzer());

        Query originalQuery = queryParser.parse("dcore.author:hendrix AND dcore.enabled:true");

        Query query = this.queryAdapter.fromLuceneQuery(originalQuery);
        assertEquals("+properties.dcore.author:hendrix +properties.dcore.enabled:true",
                     query.toString());
    }

    @Test
    public void testWildcardQueryRewrite() throws ParseException {
        QueryParser queryParser = new QueryParser("",
                                                  new WhitespaceAnalyzer());

        Query originalQuery = queryParser.parse("dcore.author:hendr*x");

        Query query = this.queryAdapter.fromLuceneQuery(originalQuery);
        assertEquals("properties.dcore.author:hendr*x",
                     query.toString());
    }

    @Test
    public void testCombinedQueryRewrite() throws ParseException {
        QueryParser queryParser = new QueryParser("",
                                                  new WhitespaceAnalyzer());

        Query originalQuery = queryParser.parse("dcore.author:h?ndrix AND dcore.size:>10");

        Query query = this.queryAdapter.fromLuceneQuery(originalQuery);
        assertEquals("+properties.dcore.author:h?ndrix +properties.dcore.size:>10",
                     query.toString());
    }
}