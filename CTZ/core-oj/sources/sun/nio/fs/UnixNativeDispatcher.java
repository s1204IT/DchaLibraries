package sun.nio.fs;

class UnixNativeDispatcher {
    private static final int SUPPORTS_BIRTHTIME = 65536;
    private static final int SUPPORTS_FUTIMES = 4;
    private static final int SUPPORTS_OPENAT = 2;
    private static final int capabilities = init();

    private static native void access0(long j, int i) throws UnixException;

    private static native void chmod0(long j, int i) throws UnixException;

    private static native void chown0(long j, int i, int i2) throws UnixException;

    static native void close(int i);

    static native void closedir(long j) throws UnixException;

    static native int dup(int i) throws UnixException;

    static native void fchmod(int i, int i2) throws UnixException;

    static native void fchown(int i, int i2, int i3) throws UnixException;

    static native void fclose(long j) throws UnixException;

    static native long fdopendir(int i) throws UnixException;

    private static native long fopen0(long j, long j2) throws UnixException;

    static native long fpathconf(int i, int i2) throws UnixException;

    static native void fstat(int i, UnixFileAttributes unixFileAttributes) throws UnixException;

    private static native void fstatat0(int i, long j, int i2, UnixFileAttributes unixFileAttributes) throws UnixException;

    static native void futimes(int i, long j, long j2) throws UnixException;

    static native byte[] getcwd();

    static native byte[] getgrgid(int i) throws UnixException;

    private static native int getgrnam0(long j) throws UnixException;

    private static native int getpwnam0(long j) throws UnixException;

    static native byte[] getpwuid(int i) throws UnixException;

    private static native int init();

    private static native void lchown0(long j, int i, int i2) throws UnixException;

    private static native void link0(long j, long j2) throws UnixException;

    private static native void lstat0(long j, UnixFileAttributes unixFileAttributes) throws UnixException;

    private static native void mkdir0(long j, int i) throws UnixException;

    private static native void mknod0(long j, int i, long j2) throws UnixException;

    private static native int open0(long j, int i, int i2) throws UnixException;

    private static native int openat0(int i, long j, int i2, int i3) throws UnixException;

    private static native long opendir0(long j) throws UnixException;

    private static native long pathconf0(long j, int i) throws UnixException;

    static native int read(int i, long j, int i2) throws UnixException;

    static native byte[] readdir(long j) throws UnixException;

    private static native byte[] readlink0(long j) throws UnixException;

    private static native byte[] realpath0(long j) throws UnixException;

    private static native void rename0(long j, long j2) throws UnixException;

    private static native void renameat0(int i, long j, int i2, long j2) throws UnixException;

    private static native void rmdir0(long j) throws UnixException;

    private static native void stat0(long j, UnixFileAttributes unixFileAttributes) throws UnixException;

    private static native void statvfs0(long j, UnixFileStoreAttributes unixFileStoreAttributes) throws UnixException;

    static native byte[] strerror(int i);

    private static native void symlink0(long j, long j2) throws UnixException;

    private static native void unlink0(long j) throws UnixException;

    private static native void unlinkat0(int i, long j, int i2) throws UnixException;

    private static native void utimes0(long j, long j2, long j3) throws UnixException;

    static native int write(int i, long j, int i2) throws UnixException;

    protected UnixNativeDispatcher() {
    }

    private static NativeBuffer copyToNativeBuffer(UnixPath unixPath) {
        byte[] byteArrayForSysCalls = unixPath.getByteArrayForSysCalls();
        int length = byteArrayForSysCalls.length + 1;
        NativeBuffer nativeBufferFromCache = NativeBuffers.getNativeBufferFromCache(length);
        if (nativeBufferFromCache == null) {
            nativeBufferFromCache = NativeBuffers.allocNativeBuffer(length);
        } else if (nativeBufferFromCache.owner() == unixPath) {
            return nativeBufferFromCache;
        }
        NativeBuffers.copyCStringToNativeBuffer(byteArrayForSysCalls, nativeBufferFromCache);
        nativeBufferFromCache.setOwner(unixPath);
        return nativeBufferFromCache;
    }

    static int open(UnixPath unixPath, int i, int i2) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            return open0(nativeBufferCopyToNativeBuffer.address(), i, i2);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static int openat(int i, byte[] bArr, int i2, int i3) throws UnixException {
        NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(bArr);
        try {
            return openat0(i, nativeBufferAsNativeBuffer.address(), i2, i3);
        } finally {
            nativeBufferAsNativeBuffer.release();
        }
    }

    static long fopen(UnixPath unixPath, String str) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(Util.toBytes(str));
        try {
            return fopen0(nativeBufferCopyToNativeBuffer.address(), nativeBufferAsNativeBuffer.address());
        } finally {
            nativeBufferAsNativeBuffer.release();
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void link(UnixPath unixPath, UnixPath unixPath2) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        NativeBuffer nativeBufferCopyToNativeBuffer2 = copyToNativeBuffer(unixPath2);
        try {
            link0(nativeBufferCopyToNativeBuffer.address(), nativeBufferCopyToNativeBuffer2.address());
        } finally {
            nativeBufferCopyToNativeBuffer2.release();
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void unlink(UnixPath unixPath) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            unlink0(nativeBufferCopyToNativeBuffer.address());
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void unlinkat(int i, byte[] bArr, int i2) throws UnixException {
        NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(bArr);
        try {
            unlinkat0(i, nativeBufferAsNativeBuffer.address(), i2);
        } finally {
            nativeBufferAsNativeBuffer.release();
        }
    }

    static void mknod(UnixPath unixPath, int i, long j) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            mknod0(nativeBufferCopyToNativeBuffer.address(), i, j);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void rename(UnixPath unixPath, UnixPath unixPath2) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        NativeBuffer nativeBufferCopyToNativeBuffer2 = copyToNativeBuffer(unixPath2);
        try {
            rename0(nativeBufferCopyToNativeBuffer.address(), nativeBufferCopyToNativeBuffer2.address());
        } finally {
            nativeBufferCopyToNativeBuffer2.release();
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void renameat(int i, byte[] bArr, int i2, byte[] bArr2) throws UnixException {
        NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(bArr);
        NativeBuffer nativeBufferAsNativeBuffer2 = NativeBuffers.asNativeBuffer(bArr2);
        try {
            renameat0(i, nativeBufferAsNativeBuffer.address(), i2, nativeBufferAsNativeBuffer2.address());
        } finally {
            nativeBufferAsNativeBuffer2.release();
            nativeBufferAsNativeBuffer.release();
        }
    }

    static void mkdir(UnixPath unixPath, int i) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            mkdir0(nativeBufferCopyToNativeBuffer.address(), i);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void rmdir(UnixPath unixPath) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            rmdir0(nativeBufferCopyToNativeBuffer.address());
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static byte[] readlink(UnixPath unixPath) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            return readlink0(nativeBufferCopyToNativeBuffer.address());
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static byte[] realpath(UnixPath unixPath) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            return realpath0(nativeBufferCopyToNativeBuffer.address());
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void symlink(byte[] bArr, UnixPath unixPath) throws UnixException {
        NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(bArr);
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            symlink0(nativeBufferAsNativeBuffer.address(), nativeBufferCopyToNativeBuffer.address());
        } finally {
            nativeBufferCopyToNativeBuffer.release();
            nativeBufferAsNativeBuffer.release();
        }
    }

    static void stat(UnixPath unixPath, UnixFileAttributes unixFileAttributes) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            stat0(nativeBufferCopyToNativeBuffer.address(), unixFileAttributes);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void lstat(UnixPath unixPath, UnixFileAttributes unixFileAttributes) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            lstat0(nativeBufferCopyToNativeBuffer.address(), unixFileAttributes);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void fstatat(int i, byte[] bArr, int i2, UnixFileAttributes unixFileAttributes) throws UnixException {
        NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(bArr);
        try {
            fstatat0(i, nativeBufferAsNativeBuffer.address(), i2, unixFileAttributes);
        } finally {
            nativeBufferAsNativeBuffer.release();
        }
    }

    static void chown(UnixPath unixPath, int i, int i2) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            chown0(nativeBufferCopyToNativeBuffer.address(), i, i2);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void lchown(UnixPath unixPath, int i, int i2) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            lchown0(nativeBufferCopyToNativeBuffer.address(), i, i2);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void chmod(UnixPath unixPath, int i) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            chmod0(nativeBufferCopyToNativeBuffer.address(), i);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void utimes(UnixPath unixPath, long j, long j2) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            utimes0(nativeBufferCopyToNativeBuffer.address(), j, j2);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static long opendir(UnixPath unixPath) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            return opendir0(nativeBufferCopyToNativeBuffer.address());
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static void access(UnixPath unixPath, int i) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            access0(nativeBufferCopyToNativeBuffer.address(), i);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static int getpwnam(String str) throws UnixException {
        NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(Util.toBytes(str));
        try {
            return getpwnam0(nativeBufferAsNativeBuffer.address());
        } finally {
            nativeBufferAsNativeBuffer.release();
        }
    }

    static int getgrnam(String str) throws UnixException {
        NativeBuffer nativeBufferAsNativeBuffer = NativeBuffers.asNativeBuffer(Util.toBytes(str));
        try {
            return getgrnam0(nativeBufferAsNativeBuffer.address());
        } finally {
            nativeBufferAsNativeBuffer.release();
        }
    }

    static void statvfs(UnixPath unixPath, UnixFileStoreAttributes unixFileStoreAttributes) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            statvfs0(nativeBufferCopyToNativeBuffer.address(), unixFileStoreAttributes);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static long pathconf(UnixPath unixPath, int i) throws UnixException {
        NativeBuffer nativeBufferCopyToNativeBuffer = copyToNativeBuffer(unixPath);
        try {
            return pathconf0(nativeBufferCopyToNativeBuffer.address(), i);
        } finally {
            nativeBufferCopyToNativeBuffer.release();
        }
    }

    static boolean openatSupported() {
        return (capabilities & 2) != 0;
    }

    static boolean futimesSupported() {
        return (capabilities & 4) != 0;
    }

    static boolean birthtimeSupported() {
        return (capabilities & 65536) != 0;
    }
}
