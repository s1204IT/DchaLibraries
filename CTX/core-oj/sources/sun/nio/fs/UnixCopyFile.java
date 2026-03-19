package sun.nio.fs;

import com.sun.nio.file.ExtendedCopyOption;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.LinkOption;
import java.nio.file.LinkPermission;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

class UnixCopyFile {
    static native void transfer(int i, int i2, long j) throws UnixException;

    private UnixCopyFile() {
    }

    private static class Flags {
        boolean atomicMove;
        boolean copyBasicAttributes;
        boolean copyNonPosixAttributes;
        boolean copyPosixAttributes;
        boolean failIfUnableToCopyBasic;
        boolean failIfUnableToCopyNonPosix;
        boolean failIfUnableToCopyPosix;
        boolean followLinks;
        boolean interruptible;
        boolean replaceExisting;

        private Flags() {
        }

        static Flags fromCopyOptions(CopyOption... copyOptionArr) {
            Flags flags = new Flags();
            flags.followLinks = true;
            for (CopyOption copyOption : copyOptionArr) {
                if (copyOption == StandardCopyOption.REPLACE_EXISTING) {
                    flags.replaceExisting = true;
                } else if (copyOption == LinkOption.NOFOLLOW_LINKS) {
                    flags.followLinks = false;
                } else if (copyOption == StandardCopyOption.COPY_ATTRIBUTES) {
                    flags.copyBasicAttributes = true;
                    flags.copyPosixAttributes = true;
                    flags.copyNonPosixAttributes = true;
                    flags.failIfUnableToCopyBasic = true;
                } else if (copyOption == ExtendedCopyOption.INTERRUPTIBLE) {
                    flags.interruptible = true;
                } else {
                    if (copyOption == null) {
                        throw new NullPointerException();
                    }
                    throw new UnsupportedOperationException("Unsupported copy option");
                }
            }
            return flags;
        }

        static Flags fromMoveOptions(CopyOption... copyOptionArr) {
            Flags flags = new Flags();
            for (CopyOption copyOption : copyOptionArr) {
                if (copyOption == StandardCopyOption.ATOMIC_MOVE) {
                    flags.atomicMove = true;
                } else if (copyOption == StandardCopyOption.REPLACE_EXISTING) {
                    flags.replaceExisting = true;
                } else if (copyOption != LinkOption.NOFOLLOW_LINKS) {
                    if (copyOption == null) {
                        throw new NullPointerException();
                    }
                    throw new UnsupportedOperationException("Unsupported copy option");
                }
            }
            flags.copyBasicAttributes = true;
            flags.copyPosixAttributes = true;
            flags.copyNonPosixAttributes = true;
            flags.failIfUnableToCopyBasic = true;
            return flags;
        }
    }

    private static void copyDirectory(UnixPath unixPath, UnixFileAttributes unixFileAttributes, UnixPath unixPath2, Flags flags) throws IOException {
        int iOpen;
        int iOpen2;
        try {
            UnixNativeDispatcher.mkdir(unixPath2, unixFileAttributes.mode());
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath2);
        }
        if (!flags.copyBasicAttributes && !flags.copyPosixAttributes && !flags.copyNonPosixAttributes) {
            return;
        }
        try {
            iOpen = UnixNativeDispatcher.open(unixPath2, UnixConstants.O_RDONLY, 0);
        } catch (UnixException e2) {
            if (flags.copyNonPosixAttributes && flags.failIfUnableToCopyNonPosix) {
                try {
                    UnixNativeDispatcher.rmdir(unixPath2);
                } catch (UnixException e3) {
                }
                e2.rethrowAsIOException(unixPath2);
            }
            iOpen = -1;
        }
        try {
            if (flags.copyPosixAttributes) {
                try {
                    if (iOpen >= 0) {
                        UnixNativeDispatcher.fchown(iOpen, unixFileAttributes.uid(), unixFileAttributes.gid());
                        UnixNativeDispatcher.fchmod(iOpen, unixFileAttributes.mode());
                    } else {
                        UnixNativeDispatcher.chown(unixPath2, unixFileAttributes.uid(), unixFileAttributes.gid());
                        UnixNativeDispatcher.chmod(unixPath2, unixFileAttributes.mode());
                    }
                } catch (UnixException e4) {
                    if (flags.failIfUnableToCopyPosix) {
                        e4.rethrowAsIOException(unixPath2);
                    }
                }
            }
            if (flags.copyNonPosixAttributes && iOpen >= 0) {
                try {
                    iOpen2 = UnixNativeDispatcher.open(unixPath, UnixConstants.O_RDONLY, 0);
                } catch (UnixException e5) {
                    if (flags.failIfUnableToCopyNonPosix) {
                        e5.rethrowAsIOException(unixPath);
                    }
                    iOpen2 = -1;
                }
                if (iOpen2 >= 0) {
                    unixPath.getFileSystem().copyNonPosixAttributes(iOpen2, iOpen);
                    UnixNativeDispatcher.close(iOpen2);
                }
            }
            if (flags.copyBasicAttributes) {
                if (iOpen >= 0) {
                    try {
                        if (UnixNativeDispatcher.futimesSupported()) {
                            UnixNativeDispatcher.futimes(iOpen, unixFileAttributes.lastAccessTime().to(TimeUnit.MICROSECONDS), unixFileAttributes.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                        } else {
                            UnixNativeDispatcher.utimes(unixPath2, unixFileAttributes.lastAccessTime().to(TimeUnit.MICROSECONDS), unixFileAttributes.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                        }
                    } catch (UnixException e6) {
                        if (flags.failIfUnableToCopyBasic) {
                            e6.rethrowAsIOException(unixPath2);
                        }
                    }
                }
            }
            if (iOpen >= 0) {
                UnixNativeDispatcher.close(iOpen);
            }
        } catch (Throwable th) {
            if (iOpen >= 0) {
                UnixNativeDispatcher.close(iOpen);
            }
            try {
                UnixNativeDispatcher.rmdir(unixPath2);
            } catch (UnixException e7) {
            }
            throw th;
        }
    }

    private static void copyFile(UnixPath unixPath, UnixFileAttributes unixFileAttributes, UnixPath unixPath2, Flags flags, long j) throws IOException, UnixException {
        int iOpen;
        int iOpen2 = -1;
        try {
            iOpen = UnixNativeDispatcher.open(unixPath, UnixConstants.O_RDONLY, 0);
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath);
            iOpen = -1;
        }
        try {
            try {
                iOpen2 = UnixNativeDispatcher.open(unixPath2, UnixConstants.O_WRONLY | UnixConstants.O_CREAT | UnixConstants.O_EXCL, unixFileAttributes.mode());
            } catch (Throwable th) {
                UnixNativeDispatcher.close(iOpen);
                throw th;
            }
        } catch (UnixException e2) {
            e2.rethrowAsIOException(unixPath2);
        }
        try {
            try {
                transfer(iOpen2, iOpen, j);
            } catch (UnixException e3) {
                e3.rethrowAsIOException(unixPath, unixPath2);
            }
            if (flags.copyPosixAttributes) {
                try {
                    UnixNativeDispatcher.fchown(iOpen2, unixFileAttributes.uid(), unixFileAttributes.gid());
                    UnixNativeDispatcher.fchmod(iOpen2, unixFileAttributes.mode());
                } catch (UnixException e4) {
                    if (flags.failIfUnableToCopyPosix) {
                        e4.rethrowAsIOException(unixPath2);
                    }
                }
            }
            if (flags.copyNonPosixAttributes) {
                unixPath.getFileSystem().copyNonPosixAttributes(iOpen, iOpen2);
            }
            if (flags.copyBasicAttributes) {
                try {
                    if (UnixNativeDispatcher.futimesSupported()) {
                        UnixNativeDispatcher.futimes(iOpen2, unixFileAttributes.lastAccessTime().to(TimeUnit.MICROSECONDS), unixFileAttributes.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                    } else {
                        UnixNativeDispatcher.utimes(unixPath2, unixFileAttributes.lastAccessTime().to(TimeUnit.MICROSECONDS), unixFileAttributes.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                    }
                } catch (UnixException e5) {
                    if (flags.failIfUnableToCopyBasic) {
                        e5.rethrowAsIOException(unixPath2);
                    }
                }
            }
            UnixNativeDispatcher.close(iOpen2);
            UnixNativeDispatcher.close(iOpen);
        } catch (Throwable th2) {
            UnixNativeDispatcher.close(iOpen2);
            try {
                UnixNativeDispatcher.unlink(unixPath2);
            } catch (UnixException e6) {
            }
            throw th2;
        }
    }

    private static void copyLink(UnixPath unixPath, UnixFileAttributes unixFileAttributes, UnixPath unixPath2, Flags flags) throws IOException {
        byte[] bArr;
        try {
            bArr = UnixNativeDispatcher.readlink(unixPath);
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath);
            bArr = null;
        }
        try {
            UnixNativeDispatcher.symlink(bArr, unixPath2);
            if (flags.copyPosixAttributes) {
                try {
                    UnixNativeDispatcher.lchown(unixPath2, unixFileAttributes.uid(), unixFileAttributes.gid());
                } catch (UnixException e2) {
                }
            }
        } catch (UnixException e3) {
            e3.rethrowAsIOException(unixPath2);
        }
    }

    private static void copySpecial(UnixPath unixPath, UnixFileAttributes unixFileAttributes, UnixPath unixPath2, Flags flags) throws IOException {
        try {
            UnixNativeDispatcher.mknod(unixPath2, unixFileAttributes.mode(), unixFileAttributes.rdev());
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath2);
        }
        try {
            if (flags.copyPosixAttributes) {
                try {
                    UnixNativeDispatcher.chown(unixPath2, unixFileAttributes.uid(), unixFileAttributes.gid());
                    UnixNativeDispatcher.chmod(unixPath2, unixFileAttributes.mode());
                } catch (UnixException e2) {
                    if (flags.failIfUnableToCopyPosix) {
                        e2.rethrowAsIOException(unixPath2);
                    }
                }
            }
            if (flags.copyBasicAttributes) {
                try {
                    UnixNativeDispatcher.utimes(unixPath2, unixFileAttributes.lastAccessTime().to(TimeUnit.MICROSECONDS), unixFileAttributes.lastModifiedTime().to(TimeUnit.MICROSECONDS));
                } catch (UnixException e3) {
                    if (flags.failIfUnableToCopyBasic) {
                        e3.rethrowAsIOException(unixPath2);
                    }
                }
            }
        } catch (Throwable th) {
            try {
                UnixNativeDispatcher.unlink(unixPath2);
            } catch (UnixException e4) {
            }
            throw th;
        }
    }

    static void move(UnixPath unixPath, UnixPath unixPath2, CopyOption... copyOptionArr) throws IOException, UnixException {
        ?? r7;
        if (System.getSecurityManager() != null) {
            unixPath.checkWrite();
            unixPath2.checkWrite();
        }
        Flags flagsFromMoveOptions = Flags.fromMoveOptions(copyOptionArr);
        if (flagsFromMoveOptions.atomicMove) {
            try {
                UnixNativeDispatcher.rename(unixPath, unixPath2);
                return;
            } catch (UnixException e) {
                if (e.errno() == UnixConstants.EXDEV) {
                    throw new AtomicMoveNotSupportedException(unixPath.getPathForExceptionMessage(), unixPath2.getPathForExceptionMessage(), e.errorString());
                }
                e.rethrowAsIOException(unixPath, unixPath2);
                return;
            }
        }
        ?? IsDirectory = 0;
        try {
            r7 = UnixFileAttributes.get(unixPath, false);
        } catch (UnixException e2) {
            e2.rethrowAsIOException(unixPath);
            r7 = 0;
        }
        try {
            IsDirectory = UnixFileAttributes.get(unixPath2, false);
        } catch (UnixException e3) {
        }
        if (IsDirectory != 0) {
            if (r7.isSameFile(IsDirectory)) {
                return;
            }
            if (!flagsFromMoveOptions.replaceExisting) {
                throw new FileAlreadyExistsException(unixPath2.getPathForExceptionMessage());
            }
            try {
                if (IsDirectory.isDirectory()) {
                    UnixNativeDispatcher.rmdir(unixPath2);
                } else {
                    UnixNativeDispatcher.unlink(unixPath2);
                }
            } catch (UnixException e4) {
                IsDirectory = IsDirectory.isDirectory();
                if (IsDirectory != 0 && (e4.errno() == UnixConstants.EEXIST || (IsDirectory = e4.errno()) == UnixConstants.ENOTEMPTY)) {
                    throw new DirectoryNotEmptyException(unixPath2.getPathForExceptionMessage());
                }
                e4.rethrowAsIOException(unixPath2);
            }
        }
        try {
            UnixNativeDispatcher.rename(unixPath, unixPath2);
        } catch (UnixException e5) {
            if (e5.errno() != UnixConstants.EXDEV && e5.errno() != UnixConstants.EISDIR) {
                e5.rethrowAsIOException(unixPath, unixPath2);
            }
            if (r7.isDirectory()) {
                copyDirectory(unixPath, r7, unixPath2, flagsFromMoveOptions);
            } else if (r7.isSymbolicLink()) {
                copyLink(unixPath, r7, unixPath2, flagsFromMoveOptions);
            } else if (r7.isDevice()) {
                copySpecial(unixPath, r7, unixPath2, flagsFromMoveOptions);
            } else {
                copyFile(unixPath, r7, unixPath2, flagsFromMoveOptions, 0L);
            }
            try {
                if (r7.isDirectory()) {
                    UnixNativeDispatcher.rmdir(unixPath);
                } else {
                    UnixNativeDispatcher.unlink(unixPath);
                }
            } catch (UnixException e6) {
                try {
                    if (r7.isDirectory()) {
                        UnixNativeDispatcher.rmdir(unixPath2);
                    } else {
                        UnixNativeDispatcher.unlink(unixPath2);
                    }
                } catch (UnixException e7) {
                }
                if (r7.isDirectory() && (e6.errno() == UnixConstants.EEXIST || e6.errno() == UnixConstants.ENOTEMPTY)) {
                    throw new DirectoryNotEmptyException(unixPath.getPathForExceptionMessage());
                }
                e6.rethrowAsIOException(unixPath);
            }
        }
    }

    static void copy(final UnixPath unixPath, final UnixPath unixPath2, CopyOption... copyOptionArr) throws IOException, UnixException {
        final UnixFileAttributes unixFileAttributes;
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            unixPath.checkRead();
            unixPath2.checkWrite();
        }
        final Flags flagsFromCopyOptions = Flags.fromCopyOptions(copyOptionArr);
        UnixFileAttributes unixFileAttributes2 = null;
        try {
            unixFileAttributes = UnixFileAttributes.get(unixPath, flagsFromCopyOptions.followLinks);
        } catch (UnixException e) {
            e.rethrowAsIOException(unixPath);
            unixFileAttributes = null;
        }
        if (securityManager != null && unixFileAttributes.isSymbolicLink()) {
            securityManager.checkPermission(new LinkPermission("symbolic"));
        }
        try {
            unixFileAttributes2 = UnixFileAttributes.get(unixPath2, false);
        } catch (UnixException e2) {
        }
        if (unixFileAttributes2 != null) {
            if (unixFileAttributes.isSameFile(unixFileAttributes2)) {
                return;
            }
            if (!flagsFromCopyOptions.replaceExisting) {
                throw new FileAlreadyExistsException(unixPath2.getPathForExceptionMessage());
            }
            try {
                if (unixFileAttributes2.isDirectory()) {
                    UnixNativeDispatcher.rmdir(unixPath2);
                } else {
                    UnixNativeDispatcher.unlink(unixPath2);
                }
            } catch (UnixException e3) {
                if (unixFileAttributes2.isDirectory() && (e3.errno() == UnixConstants.EEXIST || e3.errno() == UnixConstants.ENOTEMPTY)) {
                    throw new DirectoryNotEmptyException(unixPath2.getPathForExceptionMessage());
                }
                e3.rethrowAsIOException(unixPath2);
            }
        }
        if (unixFileAttributes.isDirectory()) {
            copyDirectory(unixPath, unixFileAttributes, unixPath2, flagsFromCopyOptions);
            return;
        }
        if (unixFileAttributes.isSymbolicLink()) {
            copyLink(unixPath, unixFileAttributes, unixPath2, flagsFromCopyOptions);
            return;
        }
        if (!flagsFromCopyOptions.interruptible) {
            copyFile(unixPath, unixFileAttributes, unixPath2, flagsFromCopyOptions, 0L);
            return;
        }
        try {
            Cancellable.runInterruptibly(new Cancellable() {
                @Override
                public void implRun() throws IOException, UnixException {
                    UnixCopyFile.copyFile(unixPath, unixFileAttributes, unixPath2, flagsFromCopyOptions, addressToPollForCancel());
                }
            });
        } catch (ExecutionException e4) {
            Throwable cause = e4.getCause();
            if (cause instanceof IOException) {
                throw ((IOException) cause);
            }
            throw new IOException(cause);
        }
    }
}
