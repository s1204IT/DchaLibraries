package sun.nio.fs;

import dalvik.annotation.optimization.ReachabilitySensitive;
import dalvik.system.CloseGuard;
import java.io.IOException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.ClosedDirectoryStreamException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.LinkOption;
import java.nio.file.NotDirectoryException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.SecureDirectoryStream;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import sun.nio.fs.UnixUserPrincipals;

class UnixSecureDirectoryStream implements SecureDirectoryStream<Path> {

    @ReachabilitySensitive
    private final int dfd;
    private final UnixDirectoryStream ds;

    @ReachabilitySensitive
    private final CloseGuard guard = CloseGuard.get();

    @Override
    public SeekableByteChannel newByteChannel(Path path, Set set, FileAttribute[] fileAttributeArr) throws IOException {
        return newByteChannel2(path, (Set<? extends OpenOption>) set, (FileAttribute<?>[]) fileAttributeArr);
    }

    UnixSecureDirectoryStream(UnixPath unixPath, long j, int i, DirectoryStream.Filter<? super Path> filter) {
        this.ds = new UnixDirectoryStream(unixPath, j, filter);
        this.dfd = i;
        if (i != -1) {
            this.guard.open("close");
        }
    }

    @Override
    public void close() throws IOException {
        this.ds.writeLock().lock();
        try {
            if (this.ds.closeImpl()) {
                UnixNativeDispatcher.close(this.dfd);
            }
            this.ds.writeLock().unlock();
            this.guard.close();
        } catch (Throwable th) {
            this.ds.writeLock().unlock();
            throw th;
        }
    }

    @Override
    public Iterator<Path> iterator() {
        return this.ds.iterator(this);
    }

    private UnixPath getName(Path path) {
        if (path == null) {
            throw new NullPointerException();
        }
        if (!(path instanceof UnixPath)) {
            throw new ProviderMismatchException();
        }
        return (UnixPath) path;
    }

    @Override
    public SecureDirectoryStream<Path> newDirectoryStream(Path path, LinkOption... linkOptionArr) throws IOException, UnixException {
        int i;
        int i2;
        long jFdopendir;
        UnixPath name = getName(path);
        UnixPath unixPathResolve = this.ds.directory().resolve((Path) name);
        boolean zFollowLinks = Util.followLinks(linkOptionArr);
        if (System.getSecurityManager() != null) {
            unixPathResolve.checkRead();
        }
        this.ds.readLock().lock();
        try {
            if (!this.ds.isOpen()) {
                throw new ClosedDirectoryStreamException();
            }
            try {
                int i3 = UnixConstants.O_RDONLY;
                if (!zFollowLinks) {
                    i3 |= UnixConstants.O_NOFOLLOW;
                }
                int iOpenat = UnixNativeDispatcher.openat(this.dfd, name.asByteArray(), i3, 0);
                try {
                    int iDup = UnixNativeDispatcher.dup(iOpenat);
                    try {
                        i2 = iDup;
                        jFdopendir = UnixNativeDispatcher.fdopendir(iOpenat);
                    } catch (UnixException e) {
                        i = iOpenat;
                        e = e;
                        i2 = iDup;
                        if (i != -1) {
                            UnixNativeDispatcher.close(i);
                        }
                        if (i2 != -1) {
                            UnixNativeDispatcher.close(i2);
                        }
                        if (e.errno() == UnixConstants.ENOTDIR) {
                            throw new NotDirectoryException(name.toString());
                        }
                        e.rethrowAsIOException(name);
                        jFdopendir = 0;
                    }
                } catch (UnixException e2) {
                    i2 = -1;
                    i = iOpenat;
                    e = e2;
                }
            } catch (UnixException e3) {
                e = e3;
                i = -1;
                i2 = -1;
            }
            return new UnixSecureDirectoryStream(unixPathResolve, jFdopendir, i2, null);
        } finally {
            this.ds.readLock().unlock();
        }
    }

    public SeekableByteChannel newByteChannel2(Path path, Set<? extends OpenOption> set, FileAttribute<?>... fileAttributeArr) throws IOException {
        UnixPath name = getName(path);
        int unixMode = UnixFileModeAttribute.toUnixMode(UnixFileModeAttribute.ALL_READWRITE, fileAttributeArr);
        String pathForPermissionCheck = this.ds.directory().resolve((Path) name).getPathForPermissionCheck();
        this.ds.readLock().lock();
        try {
            if (!this.ds.isOpen()) {
                throw new ClosedDirectoryStreamException();
            }
            return UnixChannelFactory.newFileChannel(this.dfd, name, pathForPermissionCheck, set, unixMode);
        } catch (UnixException e) {
            e.rethrowAsIOException(name);
            return null;
        } finally {
            this.ds.readLock().unlock();
        }
    }

    private void implDelete(Path path, boolean z, int i) throws IOException {
        UnixFileAttributes unixFileAttributes;
        UnixPath name = getName(path);
        if (System.getSecurityManager() != null) {
            this.ds.directory().resolve((Path) name).checkDelete();
        }
        this.ds.readLock().lock();
        try {
            if (!this.ds.isOpen()) {
                throw new ClosedDirectoryStreamException();
            }
            if (!z) {
                try {
                    unixFileAttributes = UnixFileAttributes.get(this.dfd, name, false);
                } catch (UnixException e) {
                    e.rethrowAsIOException(name);
                    unixFileAttributes = null;
                }
                i = unixFileAttributes.isDirectory() ? 512 : 0;
            }
            try {
                UnixNativeDispatcher.unlinkat(this.dfd, name.asByteArray(), i);
            } catch (UnixException e2) {
                if ((i & 512) != 0 && (e2.errno() == UnixConstants.EEXIST || e2.errno() == UnixConstants.ENOTEMPTY)) {
                    throw new DirectoryNotEmptyException(null);
                }
                e2.rethrowAsIOException(name);
            }
        } finally {
            this.ds.readLock().unlock();
        }
    }

    @Override
    public void deleteFile(Path path) throws IOException {
        implDelete(path, true, 0);
    }

    @Override
    public void deleteDirectory(Path path) throws IOException {
        implDelete(path, true, 512);
    }

    @Override
    public void move(Path path, SecureDirectoryStream<Path> secureDirectoryStream, Path path2) throws IOException {
        UnixPath name = getName(path);
        UnixPath name2 = getName(path2);
        if (secureDirectoryStream == null) {
            throw new NullPointerException();
        }
        if (!(secureDirectoryStream instanceof UnixSecureDirectoryStream)) {
            throw new ProviderMismatchException();
        }
        UnixSecureDirectoryStream unixSecureDirectoryStream = (UnixSecureDirectoryStream) secureDirectoryStream;
        if (System.getSecurityManager() != null) {
            this.ds.directory().resolve((Path) name).checkWrite();
            unixSecureDirectoryStream.ds.directory().resolve((Path) name2).checkWrite();
        }
        this.ds.readLock().lock();
        try {
            unixSecureDirectoryStream.ds.readLock().lock();
            try {
                if (!this.ds.isOpen() || !unixSecureDirectoryStream.ds.isOpen()) {
                    throw new ClosedDirectoryStreamException();
                }
                try {
                    UnixNativeDispatcher.renameat(this.dfd, name.asByteArray(), unixSecureDirectoryStream.dfd, name2.asByteArray());
                } catch (UnixException e) {
                    if (e.errno() == UnixConstants.EXDEV) {
                        throw new AtomicMoveNotSupportedException(name.toString(), name2.toString(), e.errorString());
                    }
                    e.rethrowAsIOException(name, name2);
                }
            } finally {
                unixSecureDirectoryStream.ds.readLock().unlock();
            }
        } finally {
            this.ds.readLock().unlock();
        }
    }

    private <V extends FileAttributeView> V getFileAttributeViewImpl(UnixPath unixPath, Class<V> cls, boolean z) {
        if (cls == null) {
            throw new NullPointerException();
        }
        if (cls == BasicFileAttributeView.class) {
            return new BasicFileAttributeViewImpl(unixPath, z);
        }
        if (cls == PosixFileAttributeView.class || cls == FileOwnerAttributeView.class) {
            return new PosixFileAttributeViewImpl(unixPath, z);
        }
        return (V) null;
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Class<V> cls) {
        return (V) getFileAttributeViewImpl(null, cls, false);
    }

    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> cls, LinkOption... linkOptionArr) {
        return (V) getFileAttributeViewImpl(getName(path), cls, Util.followLinks(linkOptionArr));
    }

    private class BasicFileAttributeViewImpl implements BasicFileAttributeView {
        final UnixPath file;
        final boolean followLinks;

        BasicFileAttributeViewImpl(UnixPath unixPath, boolean z) {
            this.file = unixPath;
            this.followLinks = z;
        }

        int open() throws IOException {
            int i = UnixConstants.O_RDONLY;
            if (!this.followLinks) {
                i |= UnixConstants.O_NOFOLLOW;
            }
            try {
                return UnixNativeDispatcher.openat(UnixSecureDirectoryStream.this.dfd, this.file.asByteArray(), i, 0);
            } catch (UnixException e) {
                e.rethrowAsIOException(this.file);
                return -1;
            }
        }

        private void checkWriteAccess() {
            if (System.getSecurityManager() != null) {
                if (this.file == null) {
                    UnixSecureDirectoryStream.this.ds.directory().checkWrite();
                } else {
                    UnixSecureDirectoryStream.this.ds.directory().resolve((Path) this.file).checkWrite();
                }
            }
        }

        @Override
        public String name() {
            return "basic";
        }

        @Override
        public BasicFileAttributes readAttributes() throws IOException {
            UnixSecureDirectoryStream.this.ds.readLock().lock();
            try {
                if (!UnixSecureDirectoryStream.this.ds.isOpen()) {
                    throw new ClosedDirectoryStreamException();
                }
                if (System.getSecurityManager() != null) {
                    if (this.file == null) {
                        UnixSecureDirectoryStream.this.ds.directory().checkRead();
                    } else {
                        UnixSecureDirectoryStream.this.ds.directory().resolve((Path) this.file).checkRead();
                    }
                }
                return (this.file == null ? UnixFileAttributes.get(UnixSecureDirectoryStream.this.dfd) : UnixFileAttributes.get(UnixSecureDirectoryStream.this.dfd, this.file, this.followLinks)).asBasicFileAttributes();
            } catch (UnixException e) {
                e.rethrowAsIOException(this.file);
                return null;
            } finally {
                UnixSecureDirectoryStream.this.ds.readLock().unlock();
            }
        }

        @Override
        public void setTimes(FileTime fileTime, FileTime fileTime2, FileTime fileTime3) throws IOException {
            checkWriteAccess();
            UnixSecureDirectoryStream.this.ds.readLock().lock();
            try {
                if (!UnixSecureDirectoryStream.this.ds.isOpen()) {
                    throw new ClosedDirectoryStreamException();
                }
                int iOpen = this.file == null ? UnixSecureDirectoryStream.this.dfd : open();
                if (fileTime == null || fileTime2 == null) {
                    try {
                        try {
                            UnixFileAttributes unixFileAttributes = UnixFileAttributes.get(iOpen);
                            if (fileTime == null) {
                                fileTime = unixFileAttributes.lastModifiedTime();
                            }
                            if (fileTime2 == null) {
                                fileTime2 = unixFileAttributes.lastAccessTime();
                            }
                        } finally {
                            if (this.file != null) {
                                UnixNativeDispatcher.close(iOpen);
                            }
                        }
                    } catch (UnixException e) {
                        e.rethrowAsIOException(this.file);
                    }
                }
                try {
                    UnixNativeDispatcher.futimes(iOpen, fileTime2.to(TimeUnit.MICROSECONDS), fileTime.to(TimeUnit.MICROSECONDS));
                } catch (UnixException e2) {
                    e2.rethrowAsIOException(this.file);
                }
            } finally {
                UnixSecureDirectoryStream.this.ds.readLock().unlock();
            }
        }
    }

    private class PosixFileAttributeViewImpl extends BasicFileAttributeViewImpl implements PosixFileAttributeView {
        PosixFileAttributeViewImpl(UnixPath unixPath, boolean z) {
            super(unixPath, z);
        }

        private void checkWriteAndUserAccess() {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                checkWriteAccess();
                securityManager.checkPermission(new RuntimePermission("accessUserInformation"));
            }
        }

        @Override
        public String name() {
            return "posix";
        }

        @Override
        public PosixFileAttributes readAttributes() throws IOException {
            SecurityManager securityManager = System.getSecurityManager();
            if (securityManager != null) {
                if (this.file == null) {
                    UnixSecureDirectoryStream.this.ds.directory().checkRead();
                } else {
                    UnixSecureDirectoryStream.this.ds.directory().resolve((Path) this.file).checkRead();
                }
                securityManager.checkPermission(new RuntimePermission("accessUserInformation"));
            }
            UnixSecureDirectoryStream.this.ds.readLock().lock();
            try {
                if (!UnixSecureDirectoryStream.this.ds.isOpen()) {
                    throw new ClosedDirectoryStreamException();
                }
                return this.file == null ? UnixFileAttributes.get(UnixSecureDirectoryStream.this.dfd) : UnixFileAttributes.get(UnixSecureDirectoryStream.this.dfd, this.file, this.followLinks);
            } catch (UnixException e) {
                e.rethrowAsIOException(this.file);
                return null;
            } finally {
                UnixSecureDirectoryStream.this.ds.readLock().unlock();
            }
        }

        @Override
        public void setPermissions(Set<PosixFilePermission> set) throws IOException {
            checkWriteAndUserAccess();
            UnixSecureDirectoryStream.this.ds.readLock().lock();
            try {
                if (!UnixSecureDirectoryStream.this.ds.isOpen()) {
                    throw new ClosedDirectoryStreamException();
                }
                int iOpen = this.file == null ? UnixSecureDirectoryStream.this.dfd : open();
                try {
                    try {
                        UnixNativeDispatcher.fchmod(iOpen, UnixFileModeAttribute.toUnixMode(set));
                    } finally {
                        if (this.file != null && iOpen >= 0) {
                            UnixNativeDispatcher.close(iOpen);
                        }
                    }
                } catch (UnixException e) {
                    e.rethrowAsIOException(this.file);
                    if (this.file != null && iOpen >= 0) {
                    }
                }
            } finally {
                UnixSecureDirectoryStream.this.ds.readLock().unlock();
            }
        }

        private void setOwners(int i, int i2) throws IOException {
            checkWriteAndUserAccess();
            UnixSecureDirectoryStream.this.ds.readLock().lock();
            try {
                if (!UnixSecureDirectoryStream.this.ds.isOpen()) {
                    throw new ClosedDirectoryStreamException();
                }
                int iOpen = this.file == null ? UnixSecureDirectoryStream.this.dfd : open();
                try {
                    try {
                        UnixNativeDispatcher.fchown(iOpen, i, i2);
                    } finally {
                        if (this.file != null && iOpen >= 0) {
                            UnixNativeDispatcher.close(iOpen);
                        }
                    }
                } catch (UnixException e) {
                    e.rethrowAsIOException(this.file);
                    if (this.file != null && iOpen >= 0) {
                    }
                }
            } finally {
                UnixSecureDirectoryStream.this.ds.readLock().unlock();
            }
        }

        @Override
        public UserPrincipal getOwner() throws IOException {
            return readAttributes().owner();
        }

        @Override
        public void setOwner(UserPrincipal userPrincipal) throws IOException {
            if (!(userPrincipal instanceof UnixUserPrincipals.User)) {
                throw new ProviderMismatchException();
            }
            if (userPrincipal instanceof UnixUserPrincipals.Group) {
                throw new IOException("'owner' parameter can't be a group");
            }
            setOwners(((UnixUserPrincipals.User) userPrincipal).uid(), -1);
        }

        @Override
        public void setGroup(GroupPrincipal groupPrincipal) throws IOException {
            if (!(groupPrincipal instanceof UnixUserPrincipals.Group)) {
                throw new ProviderMismatchException();
            }
            setOwners(-1, ((UnixUserPrincipals.Group) groupPrincipal).gid());
        }
    }

    protected void finalize() throws IOException {
        if (this.guard != null) {
            this.guard.warnIfOpen();
        }
        close();
    }
}
