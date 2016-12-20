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

package org.uberfire.backend.server.cdi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;

import org.jboss.errai.security.shared.api.identity.UserImpl;
import org.jglue.cdiunit.AdditionalClasses;
import org.jglue.cdiunit.CdiRunner;
import org.jglue.cdiunit.ContextController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.server.cdi.model.WorkspaceImpl;
import org.uberfire.backend.server.cdi.workspace.WorkspaceManager;
import org.uberfire.backend.server.cdi.workspace.WorkspaceScopedExtension;
import org.uberfire.rpc.SessionInfo;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(CdiRunner.class)
@AdditionalClasses(WorkspaceScopedExtension.class)
public class WorkspaceBuilderServiceTest {

    @Produces
    @Mock
    private SessionInfo sessionInfo;

    @Inject
    private ContextController contextController;

    @Inject
    private WorkspaceManager workspaceManager;

    @Inject
    private SessionBasedBean service;

    @Produces
    public Logger produceLogger( InjectionPoint injectionPoint ) {
        return LoggerFactory.getLogger( WorkspaceBuilderServiceTest.class );
    }

    @Test
    public void testWorkspaceScoped() {

        when( sessionInfo.getIdentity() ).thenReturn( new UserImpl( "hendrix" ) );

        contextController.openRequest();
        service.build( "a:b:c" );
        contextController.closeRequest();

        when( sessionInfo.getIdentity() ).thenReturn( new UserImpl( "ray vaughan" ) );

        contextController.openRequest();
        service.build( "d:e:f" );
        contextController.closeRequest();

        final WorkspaceImpl workspace1 = (WorkspaceImpl) workspaceManager.getWorkspace( "hendrix" );
        assertEquals( 1, workspaceManager.getBeansCount( workspace1 ) );

        final WorkspaceImpl workspace2 = (WorkspaceImpl) workspaceManager.getWorkspace( "ray vaughan" );
        assertEquals( 1, workspaceManager.getBeansCount( workspace2 ) );

        assertEquals( 2, workspaceManager.getWorkspaceCount() );

    }

    @Test
    public void testConcurrentWorkspaceScoped() {

        CountDownLatch latch = new CountDownLatch( 2 );

        Thread thread1 = new Thread( () -> {
            contextController.openRequest();
            when( sessionInfo.getIdentity() ).thenReturn( new UserImpl( "hendrix" ) );
            service.build( "a:b:c" );
            contextController.closeRequest();
            latch.countDown();
        } );

        Thread thread2 = new Thread( () -> {
            contextController.openRequest();
            when( sessionInfo.getIdentity() ).thenReturn( new UserImpl( "ray vaughan" ) );
            service.build( "d:e:f" );
            contextController.closeRequest();
            latch.countDown();
        } );

        thread1.start();
        thread2.start();

        try {
            latch.await( 10, TimeUnit.SECONDS );

            final WorkspaceImpl workspace1 = (WorkspaceImpl) workspaceManager.getWorkspace( "hendrix" );
            assertEquals( 1, workspaceManager.getBeansCount( workspace1 ) );

            final WorkspaceImpl workspace2 = (WorkspaceImpl) workspaceManager.getWorkspace( "ray vaughan" );
            assertEquals( 1, workspaceManager.getBeansCount( workspace2 ) );

            assertEquals( 2, workspaceManager.getWorkspaceCount() );

        } catch ( InterruptedException e ) {
            fail();
        }

    }

}