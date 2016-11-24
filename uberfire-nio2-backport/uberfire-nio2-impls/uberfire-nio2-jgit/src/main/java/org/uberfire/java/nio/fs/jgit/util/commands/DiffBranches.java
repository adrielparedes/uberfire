/*
 * 2016 Red Hat, Inc. and/or its affiliates.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.uberfire.java.nio.base.FileDiff;
import org.uberfire.java.nio.fs.jgit.util.JGitUtil;
import org.uberfire.java.nio.fs.jgit.util.exceptions.GitException;

import static org.uberfire.commons.validation.PortablePreconditions.*;

/**
 * Implements the Git Diff command between branches for bare repositories.
 * It needs the repository, and the two branches from that repository you want
 * to diff.
 * It returns a list of DiffFile with differences between branches.
 */
public class DiffBranches extends GitCommand {

    private final Repository repository;
    private final String branchA;
    private final String branchB;

    public DiffBranches( Repository repository,
                         String branchA,
                         String branchB ) {
        this.repository = checkNotNull( "repository", repository );
        this.branchA = checkNotEmpty( "branchA", branchA );
        this.branchB = checkNotEmpty( "branchB", branchB );
    }

    @Override
    public Optional<List<FileDiff>> execute() {
        List<FileDiff> diffs = new ArrayList<>();

        final List<DiffEntry> result = JGitUtil.getDiff( repository,
                                                         JGitUtil.getTreeRefObjectId( repository, this.branchA ).toObjectId(),
                                                         JGitUtil.getTreeRefObjectId( repository, this.branchB ).toObjectId() );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        DiffFormatter formatter = new DiffFormatter( outputStream );
        formatter.setRepository( repository );

        result.forEach( elem -> {
            FileHeader header = getFileHeader( formatter, elem );
            header.toEditList().forEach( edit -> diffs.add( createFileDiff( outputStream, formatter, elem, header, edit ) ) );
        } );

        return Optional.of( diffs );

    }

    private FileHeader getFileHeader( final DiffFormatter formatter,
                                      final DiffEntry elem ) {
        try {
            return formatter.toFileHeader( elem );
        } catch ( IOException e ) {
            throw new GitException( "A problem occurred when trying to obtain diffs between files", e );
        }
    }

    private FileDiff createFileDiff( final ByteArrayOutputStream outputStream,
                                     final DiffFormatter formatter,
                                     final DiffEntry elem,
                                     final FileHeader header,
                                     final Edit edit ) {
        final String changeType = header.getChangeType().toString();
        final int startA = edit.getBeginA();
        final int endA = edit.getEndA();
        final int startB = edit.getBeginB();
        final int endB = edit.getEndB();

        String pathA = header.getOldPath();
        String pathB = header.getNewPath();

//            final List<String> linesA = getLines( elem.getOldId().toObjectId(), startA, endA );
//            final List<String> linesB = getLines( elem.getNewId().toObjectId(), startB, endB );

        List<String> lines = this.getLines( outputStream, formatter, elem );

        FileDiff diff = new FileDiff( pathA, pathB, startA, endA, startB, endB, changeType, lines );
        return diff;

    }

    private List<String> getLines( final ByteArrayOutputStream out,
                                   final DiffFormatter df,
                                   final DiffEntry diff ) {
        try {

            List<String> lines = new ArrayList<>();
            df.format( diff );
            diff.getOldId();
            String diffText = out.toString( "UTF-8" );
            lines.addAll( Arrays.asList( diffText.split( System.getProperty( "line.separator" ) ) ) );
            out.reset();
            return lines;
        } catch ( IOException e ) {
            throw new GitException( "A problem occurred when trying to obtain diffs between files", e );
        }
    }

//    private List<String> getLines( final ObjectId id,
//                                   final int fromStart,
//                                   final int fromEnd ) throws IOException {
//        List<String> lines = new ArrayList<>();
//        if ( !id.equals( ObjectId.zeroId() ) ) {
//            ByteArrayOutputStream stream = new ByteArrayOutputStream();
//            final ObjectLoader loader = repository.open( id );
//            loader.copyTo( stream );
//            final String content = stream.toString();
//            List<String> filteredLines = Arrays.asList( content.split( "\n" ) );
//            lines = filteredLines.subList( fromStart, fromEnd );
//        }
//        return lines;
//    }

}
