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
import org.uberfire.backend.server.cdi.model.Workspace;
import org.uberfire.backend.server.cdi.model.WorkspaceImpl;

/**
 * Contains every workspace created in the application.
 */
@ApplicationScoped
public class  WorkspaceManager {

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

    public Workspace getOrCreateWorkspace( String name ) {
        Workspace workspace = new WorkspaceImpl( name );
        workspaces.putIfAbsent( name, workspace );
        caches.put( workspace, this.createCache() );
        return this.getWorkspace( name );
    }

    private Cache<String, Object> createCache() {
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

    public Workspace getWorkspace( String name ) {
        final Workspace workspace = this.workspaces.get( name );
        if ( workspace == null ) {
            throw new NoSuchElementException( String.format( "Workspace <<%s>> not found", name ) );
        }
        return workspace;
    }

    public <T> T getBean( Workspace workspace,
                          String beanName ) {
        return (T) this.caches.get( workspace ).getIfPresent( beanName );
    }

    public <T> void putBean( Workspace workspace,
                             String beanName,
                             T instance ) {
        try {
            this.caches.get( workspace ).get( beanName, () -> instance );
        } catch ( ExecutionException e ) {
            logger.error( "An error ocurred trying to store bean <<{}>>", instance.getClass().getSimpleName(), e );
        }
    }

    public int getWorkspaceCount() {
        return this.workspaces.size();
    }

    public void delete( final Workspace workspace ) {
        this.workspaces.remove( workspace.getName() );
    }

    public long getBeansCount( final Workspace workspace ) {
        return this.caches.get( workspace ).size();
    }
}
