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

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.reftree.RefTreeDatabase;
import org.uberfire.java.nio.fs.jgit.util.GitImpl;

/**
 * TODO: update me
 */
public class GarbageCollector {

    private final GitImpl git;

    public GarbageCollector(final GitImpl git) {
        this.git = git;
    }

    public void execute() {
        try {
            if (!(git.getRepository().getRefDatabase() instanceof RefTreeDatabase)) {
                git._gc().call();
            }
        } catch (GitAPIException e) {
            throw new RuntimeException(e);
        }
    }
}
