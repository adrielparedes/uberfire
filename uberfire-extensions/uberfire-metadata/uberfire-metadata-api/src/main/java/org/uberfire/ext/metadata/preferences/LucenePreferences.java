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

package org.uberfire.ext.metadata.preferences;

public class LucenePreferences {

    public static final String DEFAULT_DIRECTORY_PROVIDER = "lucene.default.directory.provider";
    public static final String DEFAULT_INDEX_BASE = "lucene.default.index.base";

    private String defaultDirectoryProvider;

    private String defaultIndexBase;

    public LucenePreferences() {
        this.defaultDirectoryProvider = System.getProperty(DEFAULT_DIRECTORY_PROVIDER,
                                                           "filesystem");
        this.defaultIndexBase = System.getProperty(DEFAULT_INDEX_BASE,
                                                   "/tmp/lucene/index");
    }

    public String getDefaultDirectoryProvider() {
        return defaultDirectoryProvider;
    }

    public void setDefaultDirectoryProvider(String defaultDirectoryProvider) {
        this.defaultDirectoryProvider = defaultDirectoryProvider;
    }

    public String getDefaultIndexBase() {
        return defaultIndexBase;
    }

    public void setDefaultIndexBase(String defaultIndexBase) {
        this.defaultIndexBase = defaultIndexBase;
    }
}
