/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.uberfire.backend.server.cdi.workspace;

import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.server.cdi.model.Workspace;

import static org.junit.Assert.*;

public class WorkspaceManagerTest {

    private WorkspaceManager workspaceManager;

    @Before
    public void setUp() {
        this.workspaceManager = new WorkspaceManager( LoggerFactory.getLogger( WorkspaceManager.class ) );
        this.workspaceManager.initialize();
    }

    @Test(expected = NoSuchElementException.class)
    public void testWorkspaceNotFound() {
        this.workspaceManager.getWorkspace( "none" );
    }

    @Test
    public void testCreateWorkspace() {
        final Workspace workspace = this.workspaceManager.getOrCreateWorkspace( "hendrix" );
        assertEquals( "hendrix", workspace.getName() );
    }

    @Test
    public void testWorkspaceCount() {
        this.workspaceManager.getOrCreateWorkspace( "hendrix" );
        assertEquals( 1, this.workspaceManager.getWorkspaceCount() );
    }

    @Test
    public void testDeleteWorkspace() {
        final Workspace workspace = this.workspaceManager.getOrCreateWorkspace( "hendrix" );
        assertEquals( 1, this.workspaceManager.getWorkspaceCount() );
        this.workspaceManager.delete( workspace );
        assertEquals( 0, this.workspaceManager.getWorkspaceCount() );
    }

    @Test
    public void testStoreBeansWithCacheSizeEviction() {
        final Workspace workspace = this.workspaceManager.getOrCreateWorkspace( "hendrix" );

        this.workspaceManager.putBean( workspace, "a", new Object() );
        this.workspaceManager.putBean( workspace, "b", new Object() );
        this.workspaceManager.putBean( workspace, "c", new Object() );
        this.workspaceManager.putBean( workspace, "d", new Object() );
        this.workspaceManager.putBean( workspace, "e", new Object() );

        assertEquals( 3, this.workspaceManager.getBeansCount( workspace ) );
    }

}