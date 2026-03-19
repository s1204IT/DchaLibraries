package java.nio.file;

import java.net.URI;
import java.nio.file.spi.FileSystemProvider;

public final class Paths {
    private Paths() {
    }

    public static Path get(String str, String... strArr) {
        return FileSystems.getDefault().getPath(str, strArr);
    }

    public static Path get(URI uri) {
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new IllegalArgumentException("Missing scheme");
        }
        if (scheme.equalsIgnoreCase("file")) {
            return FileSystems.getDefault().provider().getPath(uri);
        }
        for (FileSystemProvider fileSystemProvider : FileSystemProvider.installedProviders()) {
            if (fileSystemProvider.getScheme().equalsIgnoreCase(scheme)) {
                return fileSystemProvider.getPath(uri);
            }
        }
        throw new FileSystemNotFoundException("Provider \"" + scheme + "\" not installed");
    }
}
