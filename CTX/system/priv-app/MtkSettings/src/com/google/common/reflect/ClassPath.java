package com.google.common.reflect;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Logger;
/* loaded from: classes.dex */
public final class ClassPath {
    private static final Logger logger = Logger.getLogger(ClassPath.class.getName());
    private static final Predicate<ClassInfo> IS_TOP_LEVEL = new Predicate<ClassInfo>() { // from class: com.google.common.reflect.ClassPath.1
        @Override // com.google.common.base.Predicate
        public boolean apply(ClassInfo classInfo) {
            return classInfo.className.indexOf(36) == -1;
        }
    };
    private static final Splitter CLASS_PATH_ATTRIBUTE_SEPARATOR = Splitter.on(" ").omitEmptyStrings();

    /* loaded from: classes.dex */
    public static class ResourceInfo {
        final ClassLoader loader;
        private final String resourceName;

        static ResourceInfo of(String str, ClassLoader classLoader) {
            if (str.endsWith(".class")) {
                return new ClassInfo(str, classLoader);
            }
            return new ResourceInfo(str, classLoader);
        }

        ResourceInfo(String str, ClassLoader classLoader) {
            this.resourceName = (String) Preconditions.checkNotNull(str);
            this.loader = (ClassLoader) Preconditions.checkNotNull(classLoader);
        }

        public int hashCode() {
            return this.resourceName.hashCode();
        }

        public boolean equals(Object obj) {
            if (obj instanceof ResourceInfo) {
                ResourceInfo resourceInfo = (ResourceInfo) obj;
                return this.resourceName.equals(resourceInfo.resourceName) && this.loader == resourceInfo.loader;
            }
            return false;
        }

        public String toString() {
            return this.resourceName;
        }
    }

    /* loaded from: classes.dex */
    public static final class ClassInfo extends ResourceInfo {
        private final String className;

        ClassInfo(String str, ClassLoader classLoader) {
            super(str, classLoader);
            this.className = ClassPath.getClassName(str);
        }

        @Override // com.google.common.reflect.ClassPath.ResourceInfo
        public String toString() {
            return this.className;
        }
    }

    static ImmutableMap<URI, ClassLoader> getClassPathEntries(ClassLoader classLoader) {
        LinkedHashMap newLinkedHashMap = Maps.newLinkedHashMap();
        ClassLoader parent = classLoader.getParent();
        if (parent != null) {
            newLinkedHashMap.putAll(getClassPathEntries(parent));
        }
        if (classLoader instanceof URLClassLoader) {
            for (URL url : ((URLClassLoader) classLoader).getURLs()) {
                try {
                    URI uri = url.toURI();
                    if (!newLinkedHashMap.containsKey(uri)) {
                        newLinkedHashMap.put(uri, classLoader);
                    }
                } catch (URISyntaxException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
        return ImmutableMap.copyOf((Map) newLinkedHashMap);
    }

    /* loaded from: classes.dex */
    static final class Scanner {
        private final ImmutableSortedSet.Builder<ResourceInfo> resources = new ImmutableSortedSet.Builder<>(Ordering.usingToString());
        private final Set<URI> scannedUris = Sets.newHashSet();

        Scanner() {
        }

        void scan(URI uri, ClassLoader classLoader) throws IOException {
            if (uri.getScheme().equals("file") && this.scannedUris.add(uri)) {
                scanFrom(new File(uri), classLoader);
            }
        }

        void scanFrom(File file, ClassLoader classLoader) throws IOException {
            if (!file.exists()) {
                return;
            }
            if (file.isDirectory()) {
                scanDirectory(file, classLoader);
            } else {
                scanJar(file, classLoader);
            }
        }

        private void scanDirectory(File file, ClassLoader classLoader) throws IOException {
            scanDirectory(file, classLoader, "", ImmutableSet.of());
        }

        private void scanDirectory(File file, ClassLoader classLoader, String str, ImmutableSet<File> immutableSet) throws IOException {
            File canonicalFile = file.getCanonicalFile();
            if (immutableSet.contains(canonicalFile)) {
                return;
            }
            File[] listFiles = file.listFiles();
            if (listFiles == null) {
                ClassPath.logger.warning("Cannot read directory " + file);
                return;
            }
            ImmutableSet<File> build = ImmutableSet.builder().addAll((Iterable) immutableSet).add((ImmutableSet.Builder) canonicalFile).build();
            for (File file2 : listFiles) {
                String name = file2.getName();
                if (file2.isDirectory()) {
                    scanDirectory(file2, classLoader, str + name + "/", build);
                } else {
                    String str2 = str + name;
                    if (!str2.equals("META-INF/MANIFEST.MF")) {
                        this.resources.add((ImmutableSortedSet.Builder<ResourceInfo>) ResourceInfo.of(str2, classLoader));
                    }
                }
            }
        }

        private void scanJar(File file, ClassLoader classLoader) throws IOException {
            try {
                JarFile jarFile = new JarFile(file);
                try {
                    UnmodifiableIterator<URI> it = getClassPathFromManifest(file, jarFile.getManifest()).iterator();
                    while (it.hasNext()) {
                        scan(it.next(), classLoader);
                    }
                    Enumeration<JarEntry> entries = jarFile.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry nextElement = entries.nextElement();
                        if (!nextElement.isDirectory() && !nextElement.getName().equals("META-INF/MANIFEST.MF")) {
                            this.resources.add((ImmutableSortedSet.Builder<ResourceInfo>) ResourceInfo.of(nextElement.getName(), classLoader));
                        }
                    }
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                    }
                } catch (Throwable th) {
                    try {
                        jarFile.close();
                    } catch (IOException e2) {
                    }
                    throw th;
                }
            } catch (IOException e3) {
            }
        }

        static ImmutableSet<URI> getClassPathFromManifest(File file, Manifest manifest) {
            if (manifest == null) {
                return ImmutableSet.of();
            }
            ImmutableSet.Builder builder = ImmutableSet.builder();
            String value = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH.toString());
            if (value != null) {
                for (String str : ClassPath.CLASS_PATH_ATTRIBUTE_SEPARATOR.split(value)) {
                    try {
                        builder.add((ImmutableSet.Builder) getClassPathEntry(file, str));
                    } catch (URISyntaxException e) {
                        Logger logger = ClassPath.logger;
                        logger.warning("Invalid Class-Path entry: " + str);
                    }
                }
            }
            return builder.build();
        }

        static URI getClassPathEntry(File file, String str) throws URISyntaxException {
            URI uri = new URI(str);
            if (uri.isAbsolute()) {
                return uri;
            }
            return new File(file.getParentFile(), str.replace('/', File.separatorChar)).toURI();
        }
    }

    static String getClassName(String str) {
        return str.substring(0, str.length() - ".class".length()).replace('/', '.');
    }
}
