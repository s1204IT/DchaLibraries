package sun.nio.fs;

import java.io.FilePermission;
import java.io.IOException;
import java.lang.reflect.GenericDeclaration;
import java.net.URI;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.LinkPermission;
import java.nio.file.NotDirectoryException;
import java.nio.file.NotLinkException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.spi.FileTypeDetector;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import sun.nio.ch.ThreadPool;
import sun.security.util.SecurityConstants;

public abstract class UnixFileSystemProvider extends AbstractFileSystemProvider {
    private static final String USER_DIR = "user.dir";
    private final UnixFileSystem theFileSystem = newFileSystem(System.getProperty(USER_DIR));

    abstract FileStore getFileStore(UnixPath unixPath) throws IOException;

    abstract UnixFileSystem newFileSystem(String str);

    @Override
    public final String getScheme() {
        return "file";
    }

    private void checkUri(URI uri) {
        if (!uri.getScheme().equalsIgnoreCase(getScheme())) {
            throw new IllegalArgumentException("URI does not match this provider");
        }
        if (uri.getAuthority() != null) {
            throw new IllegalArgumentException("Authority component present");
        }
        if (uri.getPath() == null) {
            throw new IllegalArgumentException("Path component is undefined");
        }
        if (!uri.getPath().equals("/")) {
            throw new IllegalArgumentException("Path component should be '/'");
        }
        if (uri.getQuery() != null) {
            throw new IllegalArgumentException("Query component present");
        }
        if (uri.getFragment() != null) {
            throw new IllegalArgumentException("Fragment component present");
        }
    }

    @Override
    public final FileSystem newFileSystem(URI uri, Map<String, ?> map) {
        checkUri(uri);
        throw new FileSystemAlreadyExistsException();
    }

    @Override
    public final FileSystem getFileSystem(URI uri) {
        checkUri(uri);
        return this.theFileSystem;
    }

    @Override
    public Path getPath(URI uri) {
        return UnixUriUtils.fromUri(this.theFileSystem, uri);
    }

    UnixPath checkPath(Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        if (!(path instanceof UnixPath)) {
            throw new ProviderMismatchException();
        }
        return (UnixPath) path;
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> cls, LinkOption... linkOptionArr) {
        UnixPath unixPath = UnixPath.toUnixPath(path);
        boolean zFollowLinks = Util.followLinks(linkOptionArr);
        if (cls == BasicFileAttributeView.class) {
            return UnixFileAttributeViews.createBasicView(unixPath, zFollowLinks);
        }
        if (cls == PosixFileAttributeView.class) {
            return UnixFileAttributeViews.createPosixView(unixPath, zFollowLinks);
        }
        if (cls == FileOwnerAttributeView.class) {
            return UnixFileAttributeViews.createOwnerView(unixPath, zFollowLinks);
        }
        if (cls == null) {
            throw new NullPointerException();
        }
        return (V) null;
    }

    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> cls, LinkOption... linkOptionArr) throws IOException {
        GenericDeclaration genericDeclaration;
        if (cls == BasicFileAttributes.class) {
            genericDeclaration = BasicFileAttributeView.class;
        } else if (cls == PosixFileAttributes.class) {
            genericDeclaration = PosixFileAttributeView.class;
        } else {
            if (cls == null) {
                throw new NullPointerException();
            }
            throw new UnsupportedOperationException();
        }
        return (A) ((BasicFileAttributeView) getFileAttributeView(path, (Class) genericDeclaration, linkOptionArr)).readAttributes();
    }

    @Override
    protected DynamicFileAttributeView getFileAttributeView(Path path, String str, LinkOption... linkOptionArr) {
        UnixPath unixPath = UnixPath.toUnixPath(path);
        boolean zFollowLinks = Util.followLinks(linkOptionArr);
        if (str.equals("basic")) {
            return UnixFileAttributeViews.createBasicView(unixPath, zFollowLinks);
        }
        if (str.equals("posix")) {
            return UnixFileAttributeViews.createPosixView(unixPath, zFollowLinks);
        }
        if (str.equals("unix")) {
            return UnixFileAttributeViews.createUnixView(unixPath, zFollowLinks);
        }
        if (str.equals("owner")) {
            return UnixFileAttributeViews.createOwnerView(unixPath, zFollowLinks);
        }
        return null;
    }

    @Override
    public FileChannel newFileChannel(Path path, Set<? extends OpenOption> set, FileAttribute<?>... fileAttributeArr) throws IOException {
        UnixPath unixPathCheckPath = checkPath(path);
        try {
            return UnixChannelFactory.newFileChannel(unixPathCheckPath, set, UnixFileModeAttribute.toUnixMode(UnixFileModeAttribute.ALL_READWRITE, fileAttributeArr));
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPathCheckPath);
            return null;
        }
    }

    @Override
    public AsynchronousFileChannel newAsynchronousFileChannel(Path path, Set<? extends OpenOption> set, ExecutorService executorService, FileAttribute<?>... fileAttributeArr) throws IOException {
        ThreadPool threadPoolWrap;
        UnixPath unixPathCheckPath = checkPath(path);
        int unixMode = UnixFileModeAttribute.toUnixMode(UnixFileModeAttribute.ALL_READWRITE, fileAttributeArr);
        if (executorService != null) {
            threadPoolWrap = ThreadPool.wrap(executorService, 0);
        } else {
            threadPoolWrap = null;
        }
        try {
            return UnixChannelFactory.newAsynchronousFileChannel(unixPathCheckPath, set, unixMode, threadPoolWrap);
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPathCheckPath);
            return null;
        }
    }

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> set, FileAttribute<?>... fileAttributeArr) throws IOException {
        UnixPath unixPath = UnixPath.toUnixPath(path);
        try {
            return UnixChannelFactory.newFileChannel(unixPath, set, UnixFileModeAttribute.toUnixMode(UnixFileModeAttribute.ALL_READWRITE, fileAttributeArr));
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath);
            return null;
        }
    }

    @Override
    boolean implDelete(Path path, boolean z) throws IOException {
        UnixFileAttributes unixFileAttributes;
        UnixPath unixPath = UnixPath.toUnixPath(path);
        unixPath.checkDelete();
        try {
            unixFileAttributes = UnixFileAttributes.get(unixPath, false);
            try {
                if (unixFileAttributes.isDirectory()) {
                    UnixNativeDispatcher.rmdir(unixPath);
                    return true;
                }
                UnixNativeDispatcher.unlink(unixPath);
                return true;
            } catch (UnixException e) {
                e = e;
                if (!z && e.errno() == UnixConstants.ENOENT) {
                    return false;
                }
                if (unixFileAttributes != null && unixFileAttributes.isDirectory() && (e.errno() == UnixConstants.EEXIST || e.errno() == UnixConstants.ENOTEMPTY)) {
                    throw new DirectoryNotEmptyException(unixPath.getPathForExceptionMessage());
                }
                e.rethrowAsIOException(unixPath);
                return false;
            }
        } catch (UnixException e2) {
            e = e2;
            unixFileAttributes = null;
        }
    }

    @Override
    public void copy(Path path, Path path2, CopyOption... copyOptionArr) throws IOException, UnixException {
        UnixCopyFile.copy(UnixPath.toUnixPath(path), UnixPath.toUnixPath(path2), copyOptionArr);
    }

    @Override
    public void move(Path path, Path path2, CopyOption... copyOptionArr) throws IOException, UnixException {
        UnixCopyFile.move(UnixPath.toUnixPath(path), UnixPath.toUnixPath(path2), copyOptionArr);
    }

    @Override
    public void checkAccess(Path path, AccessMode... accessModeArr) throws IOException {
        boolean z;
        boolean z2;
        boolean z3;
        UnixPath unixPath = UnixPath.toUnixPath(path);
        boolean z4 = true;
        int i = 0;
        if (accessModeArr.length == 0) {
            z = false;
            z2 = false;
            z3 = false;
        } else {
            int length = accessModeArr.length;
            z = false;
            z2 = false;
            z3 = false;
            for (int i2 = 0; i2 < length; i2++) {
                switch (accessModeArr[i2]) {
                    case READ:
                        z = true;
                        break;
                    case WRITE:
                        z2 = true;
                        break;
                    case EXECUTE:
                        z3 = true;
                        break;
                    default:
                        throw new AssertionError((Object) "Should not get here");
                }
            }
            z4 = false;
        }
        if (z4 || z) {
            unixPath.checkRead();
            i = 0 | (z ? UnixConstants.R_OK : UnixConstants.F_OK);
        }
        if (z2) {
            unixPath.checkWrite();
            i |= UnixConstants.W_OK;
        }
        if (z3) {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                securityManager.checkExec(unixPath.getPathForPermissionCheck());
            }
            i |= UnixConstants.X_OK;
        }
        try {
            UnixNativeDispatcher.access(unixPath, i);
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath);
        }
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        UnixPath unixPath = UnixPath.toUnixPath(path);
        if (unixPath.equals(path2)) {
            return true;
        }
        if (path2 == null) {
            throw new NullPointerException();
        }
        if (!(path2 instanceof UnixPath)) {
            return false;
        }
        UnixPath unixPath2 = (UnixPath) path2;
        unixPath.checkRead();
        unixPath2.checkRead();
        try {
            try {
                return UnixFileAttributes.get(unixPath, true).isSameFile(UnixFileAttributes.get(unixPath2, true));
            } catch (UnixException e) {
                e.rethrowAsIOException(unixPath2);
                return false;
            }
        } catch (UnixException e2) {
            e2.rethrowAsIOException(unixPath);
            return false;
        }
    }

    @Override
    public boolean isHidden(Path path) {
        UnixPath unixPath = UnixPath.toUnixPath(path);
        unixPath.checkRead();
        UnixPath fileName = unixPath.getFileName();
        return fileName != null && fileName.asByteArray()[0] == 46;
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        throw new SecurityException("getFileStore");
    }

    @Override
    public void createDirectory(Path path, FileAttribute<?>... fileAttributeArr) throws IOException {
        UnixPath unixPath = UnixPath.toUnixPath(path);
        unixPath.checkWrite();
        try {
            UnixNativeDispatcher.mkdir(unixPath, UnixFileModeAttribute.toUnixMode(UnixFileModeAttribute.ALL_PERMISSIONS, fileAttributeArr));
        } catch (UnixException e) {
            if (e.errno() == UnixConstants.EISDIR) {
                throw new FileAlreadyExistsException(unixPath.toString());
            }
            e.rethrowAsIOException(unixPath);
        }
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path path, DirectoryStream.Filter<? super Path> filter) throws IOException, UnixException {
        int iOpen;
        int iDup;
        UnixPath unixPath = UnixPath.toUnixPath(path);
        unixPath.checkRead();
        if (filter == null) {
            throw new NullPointerException();
        }
        if (!UnixNativeDispatcher.openatSupported() || UnixConstants.O_NOFOLLOW == 0) {
            try {
                return new UnixDirectoryStream(unixPath, UnixNativeDispatcher.opendir(unixPath), filter);
            } catch (UnixException e) {
                if (e.errno() == UnixConstants.ENOTDIR) {
                    throw new NotDirectoryException(unixPath.getPathForExceptionMessage());
                }
                e.rethrowAsIOException(unixPath);
            }
        }
        long jFdopendir = 0;
        try {
            iOpen = UnixNativeDispatcher.open(unixPath, UnixConstants.O_RDONLY, 0);
            try {
                iDup = UnixNativeDispatcher.dup(iOpen);
                try {
                    jFdopendir = UnixNativeDispatcher.fdopendir(iOpen);
                } catch (UnixException e2) {
                    e = e2;
                    if (iOpen != -1) {
                        UnixNativeDispatcher.close(iOpen);
                    }
                    if (iDup != -1) {
                        UnixNativeDispatcher.close(iDup);
                    }
                    if (e.errno() == UnixConstants.ENOTDIR) {
                        throw new NotDirectoryException(unixPath.getPathForExceptionMessage());
                    }
                    e.rethrowAsIOException(unixPath);
                }
            } catch (UnixException e3) {
                e = e3;
                iDup = -1;
            }
        } catch (UnixException e4) {
            e = e4;
            iOpen = -1;
            iDup = -1;
        }
        return new UnixSecureDirectoryStream(unixPath, jFdopendir, iDup, filter);
    }

    @Override
    public void createSymbolicLink(Path path, Path path2, FileAttribute<?>... fileAttributeArr) throws IOException {
        UnixPath unixPath = UnixPath.toUnixPath(path);
        UnixPath unixPath2 = UnixPath.toUnixPath(path2);
        if (fileAttributeArr.length > 0) {
            UnixFileModeAttribute.toUnixMode(0, fileAttributeArr);
            throw new UnsupportedOperationException("Initial file attributesnot supported when creating symbolic link");
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new LinkPermission("symbolic"));
            unixPath.checkWrite();
        }
        try {
            UnixNativeDispatcher.symlink(unixPath2.asByteArray(), unixPath);
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath);
        }
    }

    @Override
    public void createLink(Path path, Path path2) throws IOException {
        UnixPath unixPath = UnixPath.toUnixPath(path);
        UnixPath unixPath2 = UnixPath.toUnixPath(path2);
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new LinkPermission("hard"));
            unixPath.checkWrite();
            unixPath2.checkWrite();
        }
        try {
            UnixNativeDispatcher.link(unixPath2, unixPath);
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath, unixPath2);
        }
    }

    @Override
    public Path readSymbolicLink(Path path) throws IOException {
        UnixPath unixPath = UnixPath.toUnixPath(path);
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new FilePermission(unixPath.getPathForPermissionCheck(), SecurityConstants.FILE_READLINK_ACTION));
        }
        try {
            return new UnixPath(unixPath.getFileSystem(), UnixNativeDispatcher.readlink(unixPath));
        } catch (UnixException e) {
            if (e.errno() == UnixConstants.EINVAL) {
                throw new NotLinkException(unixPath.getPathForExceptionMessage());
            }
            e.rethrowAsIOException(unixPath);
            return null;
        }
    }

    FileTypeDetector getFileTypeDetector() {
        return new AbstractFileTypeDetector() {
            @Override
            public String implProbeContentType(Path path) {
                return null;
            }
        };
    }

    final FileTypeDetector chain(final AbstractFileTypeDetector... abstractFileTypeDetectorArr) {
        return new AbstractFileTypeDetector() {
            @Override
            protected String implProbeContentType(Path path) throws IOException {
                for (AbstractFileTypeDetector abstractFileTypeDetector : abstractFileTypeDetectorArr) {
                    String strImplProbeContentType = abstractFileTypeDetector.implProbeContentType(path);
                    if (strImplProbeContentType != null && !strImplProbeContentType.isEmpty()) {
                        return strImplProbeContentType;
                    }
                }
                return null;
            }
        };
    }
}
