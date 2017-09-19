/*
 * Copyright 2015 JBoss, by Red Hat, Inc
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

package org.uberfire.ext.metadata.io;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.uberfire.ext.metadata.backend.hibernate.model.FieldFactory;
import org.uberfire.ext.metadata.backend.hibernate.model.KClusterImpl;
import org.uberfire.ext.metadata.backend.hibernate.model.KObjectImpl;
import org.uberfire.ext.metadata.backend.hibernate.model.KPropertyImpl;
import org.uberfire.ext.metadata.model.KCluster;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.ext.metadata.model.KObjectKey;
import org.uberfire.ext.metadata.model.KProperty;
import org.uberfire.ext.metadata.model.schema.MetaType;
import org.uberfire.java.nio.base.FileSystemId;
import org.uberfire.java.nio.base.SegmentedPath;
import org.uberfire.java.nio.file.FileSystem;
import org.uberfire.java.nio.file.Path;
import org.uberfire.java.nio.file.attribute.FileAttribute;

import static org.apache.commons.codec.binary.Base64.encodeBase64String;
import static org.apache.commons.io.FilenameUtils.getBaseName;
import static org.apache.commons.io.FilenameUtils.getExtension;

/**
 *
 */
public final class KObjectUtil {

    private static final MessageDigest DIGEST;
    private static final MetaType META_TYPE = () -> Path.class.getName();

    static {
        try {
            DIGEST = MessageDigest.getInstance("SHA1");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private KObjectUtil() {

    }

    public static KObjectKey toKObjectKey(final Path path) {
        return new KObjectKey() {
            @Override
            public String getId() {
                return sha1(getType().getName() + "|" + getKey());
            }

            @Override
            public MetaType getType() {
                return META_TYPE;
            }

            @Override
            public String getClusterId() {
                return ((FileSystemId) path.getFileSystem()).id();
            }

            @Override
            public String getSegmentId() {
                return ((SegmentedPath) path).getSegmentId();
            }

            @Override
            public String getKey() {
                return path.toUri().toString();
            }
        };
    }

    public static KObject toKObject(final Path path,
                                    final FileAttribute<?>... attrs) {

        KObjectImpl kObject = new KObjectImpl();

        kObject.setType(META_TYPE);
        kObject.setKey(path.toUri().toString());
        kObject.setClusterId(((FileSystemId) path.getFileSystem()).id());
        kObject.setSegmentId(((SegmentedPath) path).getSegmentId());
        kObject.setId(sha1(kObject.getType().getName() + "|" + kObject.getKey()));
        kObject.setFullText(true);

        List<KProperty<?>> properties = new ArrayList<>();

        Arrays.stream(attrs).forEach(attr -> {
            properties.add(new KPropertyImpl<Object>(attr.name(),
                                                     attr.value(),
                                                     true));
        });

        String fileName = Optional.ofNullable(path.getFileName()).map(path1 -> path1.toString()).orElse("/");
        properties.add(new KPropertyImpl("filename",
                                         fileName,
                                         true));

        String baseName = Optional.ofNullable(path.getFileName()).map(path1 -> getBaseName(path1.getFileName().toString().toLowerCase())).orElse("");
        properties.add(new KPropertyImpl(FieldFactory.FILE_NAME_FIELD_SORTED,
                                         baseName,
                                         false,
                                         true));

        String extension = Optional.ofNullable(path.getFileName()).map(path1 -> getExtension(path1.toString())).orElse("");
        properties.add(new KPropertyImpl("extension",
                                         extension,
                                         true));

        properties.add(new KPropertyImpl("basename",
                                         Optional.ofNullable(path.getFileName()).map(path1 -> getBaseName(path1.toString())).orElse(""),
                                         true));

        kObject.setProperties(properties);
        return kObject;
    }

    public static KCluster toKCluster(final FileSystemId fs) {
        return new KClusterImpl(fs.id());
    }

    public static KCluster toKCluster(final FileSystem fs) {
        return toKCluster((FileSystemId) fs);
    }

    private static String sha1(final String input) {
        if (input == null || input.trim().length() == 0) {
            return "--";
        }
        return encodeBase64String(DIGEST.digest(input.getBytes()));
    }
}
