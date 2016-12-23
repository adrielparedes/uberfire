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

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.backend.cdi.workspace.Workspace;
import org.uberfire.backend.server.cdi.model.WorkspaceImpl;

/**
 * Contains every workspace created in the application and the beans for those workspaces.
 * Beans are stored into a cache, with size and time expiration.
 */
@ApplicationScoped
public class WorkspaceManager {

    // TODO: Extract this into preferences
    private static final long CACHE_MAXIMUM_SIZE = 3;
    private static final int EXPIRATION_DURATION = 10;
    private static final TimeUnit EXPIRATION_UNIT = TimeUnit.MINUTES;

    private final Logger logger = LoggerFactory.getLogger( WorkspaceManager.class );
    private Map<String, Workspace> workspaces;
    private Map<Workspace, Cache<String, Object>> caches;

    @PostConstruct
    public void initialize() {
        this.workspaces = new HashMap<>();
        this.caches = new HashMap<>();
    }

    /**
     * Returns a workspace, but if it does not exists, it creates a new one.
     * @param name The name of the workspace.
     * @return The existent or the new workspace.
     */
    public synchronized Workspace getOrCreateWorkspace( String name ) {
        Workspace workspace = new WorkspaceImpl( name );
        workspaces.putIfAbsent( name, workspace );
        caches.put( workspace, this.createCache() );
        return this.getWorkspace( name );
    }

    protected synchronized Cache<String, Object> createCache() {
        final Cache<String, Object> cache = CacheBuilder.newBuilder()
                .maximumSize( CACHE_MAXIMUM_SIZE )
                .expireAfterAccess( EXPIRATION_DURATION, EXPIRATION_UNIT )
                .removalListener( removalNotification -> {
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "[{},{}] {}",
                                      removalNotification.getKey().toString(),
                                      removalNotification.getValue().toString(),
                                      removalNotification.getCause().toString() );
                    }
                } )
                .build();
        return cache;
    }

    /**
     * Returns a workspace. If the workspace does ont exists it throws {@link NoSuchElementException}
     * @param name Workspace name
     * @return The workspace object
     */
    public synchronized Workspace getWorkspace( String name ) {
        final Workspace workspace = this.workspaces.get( name );
        if ( workspace == null ) {
            throw new NoSuchElementException( String.format( "Workspace <<%s>> not found", name ) );
        }
        return workspace;
    }

    /**
     * Returns a bean based on a workspace and a bean name. If the bean does not exist, returns null
     * @param workspace The workspace name.
     * @param beanName The bean name for that workspace.
     * @return the bean instance
     */
    public synchronized <T> T getBean( Workspace workspace,
                                       String beanName ) {
        return (T) this.caches.get( workspace ).getIfPresent( beanName );
    }

    /**
     * Put a bean instance into a Workspace.
     * @param workspace The workspace to store beans
     * @param beanName The bean name
     * @param instance The bean instance
     */
    public synchronized <T> void putBean( Workspace workspace,
                                          String beanName,
                                          T instance ) {
        try {
            this.caches.get( workspace ).get( beanName, () -> instance );
        } catch ( ExecutionException e ) {
            logger.error( "An error ocurred trying to store bean <<{}>>", instance.getClass().getSimpleName(), e );
        }
    }

    /**
     * Deletes a workspace and its beans
     * @param workspace the workspace to delete
     */
    public synchronized void delete( final Workspace workspace ) {
        this.caches.remove( workspace );
        this.workspaces.remove( workspace.getName() );
    }

    /**
     * Returns the workspace count
     * @return the number of workspaces
     */
    public int getWorkspaceCount() {
        return this.workspaces.size();
    }

    /**
     * Return the beans count for a workspace
     * @param workspace The workspace to count beans
     * @return The number of beans for a workspace
     */
    public long getBeansCount( final Workspace workspace ) {
        return this.caches.get( workspace ).size();
    }
}