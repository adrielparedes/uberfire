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

package org.uberfire.java.nio.fs.jgit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uberfire.java.nio.fs.jgit.util.JGitUtil;
import org.uberfire.java.nio.fs.jgit.util.commands.Fork;
import org.uberfire.java.nio.fs.jgit.util.exceptions.GitException;

import static org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL;
import static org.fest.assertions.api.Assertions.*;
import static org.uberfire.java.nio.fs.jgit.util.JGitUtil.*;

public class JGitForkTest extends AbstractTestInfra {

    public static final String TARGET_GIT = "target.git";
    public static final String SOURCE_GIT = "source.git";
    private static Logger logger = LoggerFactory.getLogger( JGitForkTest.class );

    @Test
    public void testToForkSuccess() throws IOException, GitAPIException {
        final File parentFolder = createTempDirectory();

        final File gitSource = new File( parentFolder, "source.git" );
        final Git origin = JGitUtil.newRepository( gitSource, true );

        commit( origin, "user_branch", "name", "name@example.com", "commit!", null, null, false, new HashMap<String, File>() {{
            put( "file2.txt", tempFile( "temp2222" ) );
        }} );
        commit( origin, "master", "name", "name@example.com", "commit", null, null, false, new HashMap<String, File>() {{
            put( "file.txt", tempFile( "temp" ) );
        }} );
        commit( origin, "master", "name", "name@example.com", "commit", null, null, false, new HashMap<String, File>() {{
            put( "file3.txt", tempFile( "temp3" ) );
        }} );

        new Fork( parentFolder, SOURCE_GIT, TARGET_GIT, CredentialsProvider.getDefault() ).execute();

        final File gitCloned = new File( parentFolder, "target.git" );
        final Git cloned = Git.open( gitCloned );

        assertThat( cloned ).isNotNull();

        assertThat( branchList( cloned, ALL ) ).hasSize( 4 );

        assertThat( branchList( cloned, ALL ).get( 0 ).getName() ).isEqualTo( "refs/heads/master" );
        assertThat( branchList( cloned, ALL ).get( 1 ).getName() ).isEqualTo( "refs/heads/user_branch" );
        assertThat( branchList( cloned, ALL ).get( 2 ).getName() ).isEqualTo( "refs/remotes/origin/master" );
        assertThat( branchList( cloned, ALL ).get( 3 ).getName() ).isEqualTo( "refs/remotes/origin/user_branch" );

        final String remotePath = cloned.remoteList().call().get( 0 ).getURIs().get( 0 ).getPath();
        assertThat( remotePath ).isEqualTo( gitSource.getPath() );

    }

    @Test
    public void testToForkWrongSource() throws IOException, GitAPIException {
        final File parentFolder = createTempDirectory();

        try {
            new Fork( parentFolder, SOURCE_GIT, TARGET_GIT, CredentialsProvider.getDefault() ).execute();
            fail( "If got here is because it could for the repository" );
        } catch ( GitException e ) {
            assertThat( e ).isNotNull();
            logger.info( e.getMessage(), e );
        }

    }

}
