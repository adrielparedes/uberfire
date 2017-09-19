/*
 * Copyright 2015 JBoss, by Red Hat, Inc
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
 */

package org.uberfire.ext.metadata.io;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.spi.SearchIntegrator;
import org.junit.Test;
import org.uberfire.commons.async.DescriptiveThreadFactory;
import org.uberfire.ext.metadata.backend.hibernate.HibernateSearchConfig;
import org.uberfire.ext.metadata.backend.hibernate.HibernateSearchConfigBuilder;
import org.uberfire.ext.metadata.backend.hibernate.index.providers.SearchIntegratorBuilder;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.backend.hibernate.preferences.HibernateSearchPreferences;
import org.uberfire.ext.metadata.engine.Observer;
import org.uberfire.io.IOService;
import org.uberfire.io.attribute.DublinCoreView;
import org.uberfire.io.impl.IOServiceDotFileImpl;
import org.uberfire.java.nio.file.OpenOption;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.attribute.FileAttribute;

import static org.junit.Assert.*;

public class BatchIndexTest extends BaseIndexTest {

    @Override
    protected String[] getRepositoryNames() {
        return new String[]{"temp-repo-test"};
    }

    @Override
    protected IOService ioService() {
        if (ioService == null) {
            SearchIntegrator searchIntegrator = new SearchIntegratorBuilder()
                    .withPreferences(new HibernateSearchPreferences())
                    .addClass(KObjectImpl.class)
                    .build();

            this.config = new HibernateSearchConfigBuilder()
                    .withSearchIntegrator(searchIntegrator)
                    .build();
            ioService = new IOServiceDotFileImpl();
        }
        return ioService;
    }

    @Test
    public void testIndex() throws IOException, InterruptedException {
        {
            final Path file = ioService().get("git://temp-repo-test/path/to/file.txt");
            ioService().write(file,
                              "some content here",
                              Collections.emptySet(),
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.author";
                                  }

                                  @Override
                                  public Object value() {
                                      return "My User Name Here";
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.lastModification";
                                  }

                                  @Override
                                  public Object value() {
                                      return new Date();
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.comment";
                                  }

                                  @Override
                                  public Object value() {
                                      return "initial document version, should be revised later.";
                                  }
                              }
            );
        }
        {
            final Path file = ioService().get("git://temp-repo-test/path/to/some/complex/file.txt");
            ioService().write(file,
                              "some other content here",
                              Collections.<OpenOption>emptySet(),
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.author";
                                  }

                                  @Override
                                  public Object value() {
                                      return "My Second User Name";
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.lastModification";
                                  }

                                  @Override
                                  public Object value() {
                                      return new Date();
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.comment";
                                  }

                                  @Override
                                  public Object value() {
                                      return "important document, should be used right now.";
                                  }
                              }
            );
        }
        {
            final Path file = ioService().get("git://temp-repo-test/simple.doc");
            ioService().write(file,
                              "some doc content here",
                              Collections.<OpenOption>emptySet(),
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.author";
                                  }

                                  @Override
                                  public Object value() {
                                      return "My Original User";
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.lastModification";
                                  }

                                  @Override
                                  public Object value() {
                                      return new Date();
                                  }
                              },
                              new FileAttribute<Object>() {
                                  @Override
                                  public String name() {
                                      return "dcore.comment";
                                  }

                                  @Override
                                  public Object value() {
                                      return "unlock document updated, should be checked by boss.";
                                  }
                              }
            );
        }

        {
            final Path file = ioService().get("git://temp-repo-test/xxx/simple.xls");
            ioService().write(file,
                              "plans!?");
        }

        new BatchIndex(config.getIndexEngine(),
                       ioService(),
                       new Observer() {
                           @Override
                           public void information(final String message) {

                           }

                           @Override
                           public void warning(final String message) {

                           }

                           @Override
                           public void error(final String message) {

                           }
                       },
                       Executors.newCachedThreadPool(new DescriptiveThreadFactory()),
                       DublinCoreView.class).run(ioService().get("git://temp-repo-test/"),
                                                 () -> {
                                                     try {
                                                         {

                                                             List<KObjectImpl> found = ((HibernateSearchConfig) config).getIndexProvider().findAll(KObjectImpl.class);

                                                             assertEquals(4,
                                                                          found.size());
                                                         }

                                                         {

                                                             List<KObjectImpl> found = ((HibernateSearchConfig) config).getIndexProvider().findByQuery(KObjectImpl.class,
                                                                                                                                                       new TermQuery(new Term("dcore.author",
                                                                                                                                                                              "Name")));

                                                             assertEquals(2,
                                                                          found.size());
                                                         }

                                                         {

                                                             List<KObjectImpl> found = ((HibernateSearchConfig) config).getIndexProvider().findByQuery(KObjectImpl.class,
                                                                                                                                                       new TermQuery(new Term("dcore.author",
                                                                                                                                                                              "Second")));

                                                             assertEquals(1,
                                                                          found.size());
                                                         }
                                                     } catch (Exception ex) {
                                                         ex.printStackTrace();
                                                         fail();
                                                     }
                                                 });
    }
}
