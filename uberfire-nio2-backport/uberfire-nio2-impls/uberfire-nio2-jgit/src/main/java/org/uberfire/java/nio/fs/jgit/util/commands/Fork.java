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

package org.uberfire.java.nio.fs.jgit.util.commands;

import java.io.File;
import java.util.Optional;

import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.uberfire.java.nio.fs.jgit.util.exceptions.GitException;

import static org.eclipse.jgit.util.FS.DETECTED;
import static org.uberfire.commons.validation.PortablePreconditions.*;

public class Fork extends Clone {

    private File parentFolder;
    private final String source;
    private final String target;
    private CredentialsProvider credentialsProvider;

    public Fork( File parentFolder,
                 String source,
                 String target,
                 CredentialsProvider credentialsProvider ) {
        this.parentFolder = checkNotNull( "parentFolder", parentFolder );
        this.source = checkNotEmpty( "source", source );
        this.target = checkNotEmpty( "target", target );
        this.credentialsProvider = credentialsProvider;
    }

    @Override
    public Optional<Void> execute() {

        final File gitSource = this.getGitRepository( parentFolder, source );
        return this.clone( parentFolder, gitSource.getPath(), target, credentialsProvider );
    }

    private File getGitRepository( File parentFolder,
                                   String repo ) {
        final File repoFolder = new File( parentFolder, repo );
        final File resolvedRepository = RepositoryCache.FileKey.resolve( repoFolder, DETECTED );
        if ( resolvedRepository == null ) {
            String message = String.format( "Repository %s does not exists. Cannot clone.", repoFolder.getPath() );
            throw new GitException( message );
        }
        return resolvedRepository;
    }

}
