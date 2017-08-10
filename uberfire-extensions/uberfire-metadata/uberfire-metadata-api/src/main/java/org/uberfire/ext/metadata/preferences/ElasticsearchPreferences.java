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

public class ElasticsearchPreferences {

    private static final String USERNAME = "elasticsearch.username";
    private static final String PASSWORD = "elasticsearch.password";
    private static final String REQUIRED_INDEX_STATUS = "elasticsearch.required.index.status";
    private static final String INDEX_SCHEMA_MANAGEMENT_STRATEGY = "elasticsearch.index.schema.management.strategy";
    private String username;

    private String password;

    private String requiredIndexStatus;

    private String indexSchemaManagementStrategy;

    public ElasticsearchPreferences() {

        this.setUsername(System.getProperty(USERNAME,
                                            "elastic"));
        this.setPassword(System.getProperty(PASSWORD,
                                            "changeme"));
        this.setRequiredIndexStatus(System.getProperty(REQUIRED_INDEX_STATUS,
                                                       "yellow"));
        this.setIndexSchemaManagementStrategy(System.getProperty(INDEX_SCHEMA_MANAGEMENT_STRATEGY,
                                                                 "drop-and-create"));
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRequiredIndexStatus() {
        return requiredIndexStatus;
    }

    public void setRequiredIndexStatus(String requiredIndexStatus) {
        this.requiredIndexStatus = requiredIndexStatus;
    }

    public String getIndexSchemaManagementStrategy() {
        return indexSchemaManagementStrategy;
    }

    public void setIndexSchemaManagementStrategy(String indexSchemaManagementStrategy) {
        this.indexSchemaManagementStrategy = indexSchemaManagementStrategy;
    }
}
