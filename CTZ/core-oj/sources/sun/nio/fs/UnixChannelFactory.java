package sun.nio.fs;

import java.io.FileDescriptor;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.FileChannel;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import sun.misc.JavaIOFileDescriptorAccess;
import sun.misc.SharedSecrets;
import sun.nio.ch.FileChannelImpl;
import sun.nio.ch.SimpleAsynchronousFileChannelImpl;
import sun.nio.ch.ThreadPool;

class UnixChannelFactory {
    private static final JavaIOFileDescriptorAccess fdAccess = SharedSecrets.getJavaIOFileDescriptorAccess();

    protected UnixChannelFactory() {
    }

    protected static class Flags {
        boolean append;
        boolean create;
        boolean createNew;
        boolean deleteOnClose;
        boolean dsync;
        boolean noFollowLinks;
        boolean read;
        boolean sync;
        boolean truncateExisting;
        boolean write;

        protected Flags() {
        }

        static Flags toFlags(Set<? extends OpenOption> set) {
            Flags flags = new Flags();
            for (OpenOption openOption : set) {
                if (openOption instanceof StandardOpenOption) {
                    switch ((StandardOpenOption) openOption) {
                        case READ:
                            flags.read = true;
                            break;
                        case WRITE:
                            flags.write = true;
                            break;
                        case APPEND:
                            flags.append = true;
                            break;
                        case TRUNCATE_EXISTING:
                            flags.truncateExisting = true;
                            break;
                        case CREATE:
                            flags.create = true;
                            break;
                        case CREATE_NEW:
                            flags.createNew = true;
                            break;
                        case DELETE_ON_CLOSE:
                            flags.deleteOnClose = true;
                            break;
                        case SPARSE:
                            break;
                        case SYNC:
                            flags.sync = true;
                            break;
                        case DSYNC:
                            flags.dsync = true;
                            break;
                        default:
                            throw new UnsupportedOperationException();
                    }
                } else if (openOption == LinkOption.NOFOLLOW_LINKS && UnixConstants.O_NOFOLLOW != 0) {
                    flags.noFollowLinks = true;
                } else {
                    if (openOption == null) {
                        throw new NullPointerException();
                    }
                    throw new UnsupportedOperationException(((Object) openOption) + " not supported");
                }
            }
            return flags;
        }
    }

    static FileChannel newFileChannel(int i, String str, boolean z, boolean z2) {
        FileDescriptor fileDescriptor = new FileDescriptor();
        fdAccess.set(fileDescriptor, i);
        return FileChannelImpl.open(fileDescriptor, str, z, z2, null);
    }

    static FileChannel newFileChannel(int i, UnixPath unixPath, String str, Set<? extends OpenOption> set, int i2) throws UnixException {
        Flags flags = Flags.toFlags(set);
        if (!flags.read && !flags.write) {
            if (flags.append) {
                flags.write = true;
            } else {
                flags.read = true;
            }
        }
        if (flags.read && flags.append) {
            throw new IllegalArgumentException("READ + APPEND not allowed");
        }
        if (flags.append && flags.truncateExisting) {
            throw new IllegalArgumentException("APPEND + TRUNCATE_EXISTING not allowed");
        }
        return FileChannelImpl.open(open(i, unixPath, str, flags, i2), unixPath.toString(), flags.read, flags.write, flags.append, null);
    }

    static FileChannel newFileChannel(UnixPath unixPath, Set<? extends OpenOption> set, int i) throws UnixException {
        return newFileChannel(-1, unixPath, null, set, i);
    }

    static AsynchronousFileChannel newAsynchronousFileChannel(UnixPath unixPath, Set<? extends OpenOption> set, int i, ThreadPool threadPool) throws UnixException {
        Flags flags = Flags.toFlags(set);
        if (!flags.read && !flags.write) {
            flags.read = true;
        }
        if (flags.append) {
            throw new UnsupportedOperationException("APPEND not allowed");
        }
        return SimpleAsynchronousFileChannelImpl.open(open(-1, unixPath, null, flags, i), flags.read, flags.write, threadPool);
    }

    protected static FileDescriptor open(int i, UnixPath unixPath, String str, Flags flags, int i2) throws UnixException {
        int i3;
        int iOpen;
        if (flags.read && flags.write) {
            i3 = UnixConstants.O_RDWR;
        } else {
            i3 = flags.write ? UnixConstants.O_WRONLY : UnixConstants.O_RDONLY;
        }
        boolean z = true;
        if (flags.write) {
            if (flags.truncateExisting) {
                i3 |= UnixConstants.O_TRUNC;
            }
            if (flags.append) {
                i3 |= UnixConstants.O_APPEND;
            }
            if (flags.createNew) {
                byte[] bArrAsByteArray = unixPath.asByteArray();
                if (bArrAsByteArray[bArrAsByteArray.length - 1] == 46 && (bArrAsByteArray.length == 1 || bArrAsByteArray[bArrAsByteArray.length - 2] == 47)) {
                    throw new UnixException(UnixConstants.EEXIST);
                }
                i3 |= UnixConstants.O_CREAT | UnixConstants.O_EXCL;
            } else if (flags.create) {
                i3 |= UnixConstants.O_CREAT;
            }
        }
        if (!flags.createNew && (flags.noFollowLinks || flags.deleteOnClose)) {
            if (flags.deleteOnClose && UnixConstants.O_NOFOLLOW == 0) {
                try {
                    if (UnixFileAttributes.get(unixPath, false).isSymbolicLink()) {
                        throw new UnixException("DELETE_ON_CLOSE specified and file is a symbolic link");
                    }
                } catch (UnixException e) {
                    if (!flags.create || e.errno() != UnixConstants.ENOENT) {
                        throw e;
                    }
                }
            }
            i3 |= UnixConstants.O_NOFOLLOW;
            z = false;
        }
        if (flags.dsync) {
            i3 |= UnixConstants.O_DSYNC;
        }
        if (flags.sync) {
            i3 |= UnixConstants.O_SYNC;
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            if (str == null) {
                str = unixPath.getPathForPermissionCheck();
            }
            if (flags.read) {
                securityManager.checkRead(str);
            }
            if (flags.write) {
                securityManager.checkWrite(str);
            }
            if (flags.deleteOnClose) {
                securityManager.checkDelete(str);
            }
        }
        try {
            if (i >= 0) {
                iOpen = UnixNativeDispatcher.openat(i, unixPath.asByteArray(), i3, i2);
            } else {
                iOpen = UnixNativeDispatcher.open(unixPath, i3, i2);
            }
            flags = flags.deleteOnClose;
            if (flags != 0) {
                try {
                    if (i >= 0) {
                        UnixNativeDispatcher.unlinkat(i, unixPath.asByteArray(), 0);
                    } else {
                        UnixNativeDispatcher.unlink(unixPath);
                    }
                } catch (UnixException e2) {
                }
            }
            FileDescriptor fileDescriptor = new FileDescriptor();
            fdAccess.set(fileDescriptor, iOpen);
            return fileDescriptor;
        } catch (UnixException e3) {
            if (flags.createNew && e3.errno() == UnixConstants.EISDIR) {
                e3.setError(UnixConstants.EEXIST);
            }
            if (z || e3.errno() != UnixConstants.ELOOP) {
                throw e3;
            }
            throw new UnixException(e3.getMessage() + " (NOFOLLOW_LINKS specified)");
        }
    }
}
