package java.io;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import sun.misc.JavaIOFileDescriptorAccess;
import sun.misc.SharedSecrets;

public final class FileDescriptor {
    private int descriptor;
    public static final FileDescriptor in = dupFd(0);
    public static final FileDescriptor out = dupFd(1);
    public static final FileDescriptor err = dupFd(2);

    private static native boolean isSocket(int i);

    public native void sync() throws SyncFailedException;

    public FileDescriptor() {
        this.descriptor = -1;
    }

    private FileDescriptor(int i) {
        this.descriptor = i;
    }

    static {
        SharedSecrets.setJavaIOFileDescriptorAccess(new JavaIOFileDescriptorAccess() {
            @Override
            public void set(FileDescriptor fileDescriptor, int i) {
                fileDescriptor.descriptor = i;
            }

            @Override
            public int get(FileDescriptor fileDescriptor) {
                return fileDescriptor.descriptor;
            }

            @Override
            public void setHandle(FileDescriptor fileDescriptor, long j) {
                throw new UnsupportedOperationException();
            }

            @Override
            public long getHandle(FileDescriptor fileDescriptor) {
                throw new UnsupportedOperationException();
            }
        });
    }

    public boolean valid() {
        return this.descriptor != -1;
    }

    public final int getInt$() {
        return this.descriptor;
    }

    public final void setInt$(int i) {
        this.descriptor = i;
    }

    public boolean isSocket$() {
        return isSocket(this.descriptor);
    }

    private static FileDescriptor dupFd(int i) {
        try {
            return new FileDescriptor(Os.fcntlInt(new FileDescriptor(i), OsConstants.F_DUPFD_CLOEXEC, 0));
        } catch (ErrnoException e) {
            throw new RuntimeException(e);
        }
    }
}
