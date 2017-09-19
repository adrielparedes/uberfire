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

package org.uberfire.ext.metadata.backend.hibernate;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.arquillian.cube.CubeController;
import org.arquillian.cube.requirement.ArquillianConditionalRunner;
import org.hibernate.search.spi.SearchIntegrator;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.uberfire.ext.metadata.backend.hibernate.index.QueryAdapter;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.HibernateSearchIndexProvider;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.SearchIntegratorBuilder;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.backend.hibernate.model.PathIndex;
import org.uberfire.ext.metadata.preferences.IndexManagerType;

import static org.junit.Assert.*;

@RunWith(ArquillianConditionalRunner.class)
public class HibernateSearchElasticSearchIntegrationTest extends HibernateSearchIntegrationTest {

    private static final String CONTAINER = "elastic";

    @ArquillianResource
    private CubeController cubeController;
    private QueryAdapter properties;

    @Before
    public void setUp() {

        super.setUp();

        cubeController.create(CONTAINER);
        cubeController.start(CONTAINER);

        try {
            this.waitUntilReady(100,
                                1000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.preferences.setIndexManager(IndexManagerType.ELASTICSEARCH.toString());

        SearchIntegrator integrator = new SearchIntegratorBuilder()
                .withPreferences(preferences)
                .addClass(KObjectImpl.class)
                .addClass(PathIndex.class)
                .build();
        this.provider = new HibernateSearchIndexProvider(integrator,
                                                         queryAdapter);
    }

    @After
    public void tierDown() {
        cubeController.stop(CONTAINER);
        cubeController.destroy(CONTAINER);
    }

    @Test
    public void testElasticSearchShard() throws IOException {
        KObjectImpl kObject1 = new KObjectImpl();
        kObject1.setClusterId("java");
        kObject1.setId("1");

        KObjectImpl kObject2 = new KObjectImpl();
        kObject2.setClusterId("mvel");
        kObject2.setId("2");

        this.provider.index(kObject1);
        this.provider.index(kObject2);

        HttpClient client = createElasticClient();
        HttpHead requestJava = new HttpHead("http://localhost:9200/" + KObjectImpl.class.getCanonicalName().toLowerCase() + ".java");
        HttpHead requestMvel = new HttpHead("http://localhost:9200/" + KObjectImpl.class.getCanonicalName().toLowerCase() + ".mvel");
        HttpResponse responseJava = client.execute(requestJava);
        HttpResponse responseMvel = client.execute(requestMvel);

        assertEquals(200,
                     responseJava.getStatusLine().getStatusCode());
        assertEquals(200,
                     responseMvel.getStatusLine().getStatusCode());
    }

    private HttpClient createElasticClient() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials("elastic",
                                                  "changeme");
        provider.setCredentials(AuthScope.ANY,
                                credentials);

        return HttpClientBuilder.create().setDefaultCredentialsProvider(provider).build();
    }

    public void waitUntilReady(int repeat,
                               long wait) throws InterruptedException {

        HttpClient client = this.createElasticClient();
        HttpGet request = new HttpGet("http://localhost:9200");
        int times = repeat;
        while (times > 0) {
            try {
                HttpResponse response = client.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    times = 0;
                } else {
                    times--;
                    logger.info("Waiting for elasticsearch to be ready");
                    Thread.sleep(wait);
                }
            } catch (IOException e) {
                times--;
                logger.info("Waiting for elasticsearch to be ready");
                Thread.sleep(wait);
            }
        }
    }
}
