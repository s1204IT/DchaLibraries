package java.nio.file;

import java.io.IOException;
import java.net.URI;
import java.nio.file.spi.FileSystemProvider;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import sun.nio.fs.DefaultFileSystemProvider;

public final class FileSystems {
    private FileSystems() {
    }

    private static class DefaultFileSystemHolder {
        static final FileSystem defaultFileSystem = defaultFileSystem();

        private DefaultFileSystemHolder() {
        }

        private static FileSystem defaultFileSystem() {
            return ((FileSystemProvider) AccessController.doPrivileged(new PrivilegedAction<FileSystemProvider>() {
                @Override
                public FileSystemProvider run() {
                    return DefaultFileSystemHolder.getDefaultProvider();
                }
            })).getFileSystem(URI.create("file:///"));
        }

        private static FileSystemProvider getDefaultProvider() {
            FileSystemProvider fileSystemProviderCreate = DefaultFileSystemProvider.create();
            String property = System.getProperty("java.nio.file.spi.DefaultFileSystemProvider");
            if (property == null) {
                return fileSystemProviderCreate;
            }
            FileSystemProvider fileSystemProvider = fileSystemProviderCreate;
            for (String str : property.split(",")) {
                try {
                    fileSystemProvider = (FileSystemProvider) Class.forName(str, true, ClassLoader.getSystemClassLoader()).getDeclaredConstructor(FileSystemProvider.class).newInstance(fileSystemProvider);
                    if (!fileSystemProvider.getScheme().equals("file")) {
                        throw new Error("Default provider must use scheme 'file'");
                    }
                } catch (Exception e) {
                    throw new Error(e);
                }
            }
            return fileSystemProvider;
        }
    }

    public static FileSystem getDefault() {
        return DefaultFileSystemHolder.defaultFileSystem;
    }

    public static FileSystem getFileSystem(URI uri) {
        String scheme = uri.getScheme();
        for (FileSystemProvider fileSystemProvider : FileSystemProvider.installedProviders()) {
            if (scheme.equalsIgnoreCase(fileSystemProvider.getScheme())) {
                return fileSystemProvider.getFileSystem(uri);
            }
        }
        throw new ProviderNotFoundException("Provider \"" + scheme + "\" not found");
    }

    public static FileSystem newFileSystem(URI uri, Map<String, ?> map) throws IOException {
        return newFileSystem(uri, map, null);
    }

    public static FileSystem newFileSystem(URI uri, Map<String, ?> map, ClassLoader classLoader) throws IOException {
        String scheme = uri.getScheme();
        for (FileSystemProvider fileSystemProvider : FileSystemProvider.installedProviders()) {
            if (scheme.equalsIgnoreCase(fileSystemProvider.getScheme())) {
                return fileSystemProvider.newFileSystem(uri, map);
            }
        }
        if (classLoader != null) {
            for (FileSystemProvider fileSystemProvider2 : ServiceLoader.load(FileSystemProvider.class, classLoader)) {
                if (scheme.equalsIgnoreCase(fileSystemProvider2.getScheme())) {
                    return fileSystemProvider2.newFileSystem(uri, map);
                }
            }
        }
        throw new ProviderNotFoundException("Provider \"" + scheme + "\" not found");
    }

    public static FileSystem newFileSystem(Path path, ClassLoader classLoader) throws IOException {
        if (path == null) {
            throw new NullPointerException();
        }
        Map<String, ?> mapEmptyMap = Collections.emptyMap();
        Iterator<FileSystemProvider> it = FileSystemProvider.installedProviders().iterator();
        while (it.hasNext()) {
            try {
                return it.next().newFileSystem(path, mapEmptyMap);
            } catch (UnsupportedOperationException e) {
            }
        }
        if (classLoader != null) {
            Iterator it2 = ServiceLoader.load(FileSystemProvider.class, classLoader).iterator();
            while (it2.hasNext()) {
                try {
                    return ((FileSystemProvider) it2.next()).newFileSystem(path, mapEmptyMap);
                } catch (UnsupportedOperationException e2) {
                }
            }
        }
        throw new ProviderNotFoundException("Provider not found");
    }
}
