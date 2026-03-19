package sun.nio.fs;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;
import sun.security.action.GetPropertyAction;

abstract class UnixFileSystem extends FileSystem {
    private static final String GLOB_SYNTAX = "glob";
    private static final String REGEX_SYNTAX = "regex";
    private final byte[] defaultDirectory;
    private final boolean needToResolveAgainstDefaultDirectory;
    private final UnixFileSystemProvider provider;
    private final UnixPath rootDirectory;

    abstract FileStore getFileStore(UnixMountEntry unixMountEntry) throws IOException;

    abstract Iterable<UnixMountEntry> getMountEntries();

    UnixFileSystem(UnixFileSystemProvider unixFileSystemProvider, String str) {
        boolean zBooleanValue;
        this.provider = unixFileSystemProvider;
        this.defaultDirectory = Util.toBytes(UnixPath.normalizeAndCheck(str));
        boolean z = false;
        if (this.defaultDirectory[0] != 47) {
            throw new RuntimeException("default directory must be absolute");
        }
        String str2 = (String) AccessController.doPrivileged(new GetPropertyAction("sun.nio.fs.chdirAllowed", "false"));
        if (str2.length() != 0) {
            zBooleanValue = Boolean.valueOf(str2).booleanValue();
        } else {
            zBooleanValue = true;
        }
        if (zBooleanValue) {
            this.needToResolveAgainstDefaultDirectory = true;
        } else {
            byte[] bArr = UnixNativeDispatcher.getcwd();
            boolean z2 = bArr.length == this.defaultDirectory.length;
            if (z2) {
                for (int i = 0; i < bArr.length; i++) {
                    if (bArr[i] != this.defaultDirectory[i]) {
                        break;
                    }
                }
                z = z2;
                this.needToResolveAgainstDefaultDirectory = !z;
            } else {
                z = z2;
                this.needToResolveAgainstDefaultDirectory = !z;
            }
        }
        this.rootDirectory = new UnixPath(this, "/");
    }

    byte[] defaultDirectory() {
        return this.defaultDirectory;
    }

    boolean needToResolveAgainstDefaultDirectory() {
        return this.needToResolveAgainstDefaultDirectory;
    }

    UnixPath rootDirectory() {
        return this.rootDirectory;
    }

    boolean isSolaris() {
        return false;
    }

    static List<String> standardFileAttributeViews() {
        return Arrays.asList("basic", "posix", "unix", "owner");
    }

    @Override
    public final FileSystemProvider provider() {
        return this.provider;
    }

    @Override
    public final String getSeparator() {
        return "/";
    }

    @Override
    public final boolean isOpen() {
        return true;
    }

    @Override
    public final boolean isReadOnly() {
        return false;
    }

    @Override
    public final void close() throws IOException {
        throw new UnsupportedOperationException();
    }

    void copyNonPosixAttributes(int i, int i2) {
    }

    @Override
    public final Iterable<Path> getRootDirectories() {
        final List listUnmodifiableList = Collections.unmodifiableList(Arrays.asList(this.rootDirectory));
        return new Iterable<Path>() {
            @Override
            public Iterator<Path> iterator() {
                try {
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        securityManager.checkRead(UnixFileSystem.this.rootDirectory.toString());
                    }
                    return listUnmodifiableList.iterator();
                } catch (SecurityException e) {
                    return Collections.emptyList().iterator();
                }
            }
        };
    }

    private class FileStoreIterator implements Iterator<FileStore> {
        static final boolean $assertionsDisabled = false;
        private final Iterator<UnixMountEntry> entries;
        private FileStore next;

        FileStoreIterator() {
            this.entries = UnixFileSystem.this.getMountEntries().iterator();
        }

        private FileStore readNext() {
            while (this.entries.hasNext()) {
                UnixMountEntry next = this.entries.next();
                if (!next.isIgnored()) {
                    SecurityManager securityManager = System.getSecurityManager();
                    if (securityManager != null) {
                        try {
                            securityManager.checkRead(Util.toString(next.dir()));
                            try {
                                return UnixFileSystem.this.getFileStore(next);
                            } catch (IOException e) {
                            }
                        } catch (SecurityException e2) {
                        }
                    } else {
                        return UnixFileSystem.this.getFileStore(next);
                    }
                }
            }
            return null;
        }

        @Override
        public synchronized boolean hasNext() {
            if (this.next != null) {
                return true;
            }
            this.next = readNext();
            return this.next != null;
        }

        @Override
        public synchronized FileStore next() {
            FileStore fileStore;
            if (this.next == null) {
                this.next = readNext();
            }
            if (this.next == null) {
                throw new NoSuchElementException();
            }
            fileStore = this.next;
            this.next = null;
            return fileStore;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public final Iterable<FileStore> getFileStores() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            try {
                securityManager.checkPermission(new RuntimePermission("getFileStoreAttributes"));
            } catch (SecurityException e) {
                return Collections.emptyList();
            }
        }
        return new Iterable<FileStore>() {
            @Override
            public Iterator<FileStore> iterator() {
                return UnixFileSystem.this.new FileStoreIterator();
            }
        };
    }

    @Override
    public final Path getPath(String str, String... strArr) {
        if (strArr.length != 0) {
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            for (String str2 : strArr) {
                if (str2.length() > 0) {
                    if (sb.length() > 0) {
                        sb.append('/');
                    }
                    sb.append(str2);
                }
            }
            str = sb.toString();
        }
        return new UnixPath(this, str);
    }

    @Override
    public PathMatcher getPathMatcher(String str) {
        int iIndexOf = str.indexOf(58);
        if (iIndexOf <= 0 || iIndexOf == str.length()) {
            throw new IllegalArgumentException();
        }
        String strSubstring = str.substring(0, iIndexOf);
        String strSubstring2 = str.substring(iIndexOf + 1);
        if (strSubstring.equals(GLOB_SYNTAX)) {
            strSubstring2 = Globs.toUnixRegexPattern(strSubstring2);
        } else if (!strSubstring.equals(REGEX_SYNTAX)) {
            throw new UnsupportedOperationException("Syntax '" + strSubstring + "' not recognized");
        }
        final Pattern patternCompilePathMatchPattern = compilePathMatchPattern(strSubstring2);
        return new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                return patternCompilePathMatchPattern.matcher(path.toString()).matches();
            }
        };
    }

    @Override
    public final UserPrincipalLookupService getUserPrincipalLookupService() {
        return LookupService.instance;
    }

    private static class LookupService {
        static final UserPrincipalLookupService instance = new UserPrincipalLookupService() {
            @Override
            public UserPrincipal lookupPrincipalByName(String str) throws IOException {
                return UnixUserPrincipals.lookupUser(str);
            }

            @Override
            public GroupPrincipal lookupPrincipalByGroupName(String str) throws IOException {
                return UnixUserPrincipals.lookupGroup(str);
            }
        };

        private LookupService() {
        }
    }

    Pattern compilePathMatchPattern(String str) {
        return Pattern.compile(str);
    }

    char[] normalizeNativePath(char[] cArr) {
        return cArr;
    }

    String normalizeJavaPath(String str) {
        return str;
    }
}
