package android.os;

import android.os.Parcelable;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import dalvik.system.VMRuntime;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.nio.ByteBuffer;
import java.nio.DirectByteBuffer;
import java.nio.NioUtils;
import sun.misc.Cleaner;

public final class SharedMemory implements Parcelable, Closeable {
    private Cleaner mCleaner;
    private final FileDescriptor mFileDescriptor;
    private final MemoryRegistration mMemoryRegistration;
    private final int mSize;
    private static final int PROT_MASK = ((OsConstants.PROT_READ | OsConstants.PROT_WRITE) | OsConstants.PROT_EXEC) | OsConstants.PROT_NONE;
    public static final Parcelable.Creator<SharedMemory> CREATOR = new Parcelable.Creator<SharedMemory>() {
        @Override
        public SharedMemory createFromParcel(Parcel parcel) {
            return new SharedMemory(parcel.readRawFileDescriptor());
        }

        @Override
        public SharedMemory[] newArray(int i) {
            return new SharedMemory[i];
        }
    };

    private static native FileDescriptor nCreate(String str, int i) throws ErrnoException;

    private static native int nGetSize(FileDescriptor fileDescriptor);

    private static native int nSetProt(FileDescriptor fileDescriptor, int i);

    private SharedMemory(FileDescriptor fileDescriptor) {
        if (fileDescriptor == null) {
            throw new IllegalArgumentException("Unable to create SharedMemory from a null FileDescriptor");
        }
        if (!fileDescriptor.valid()) {
            throw new IllegalArgumentException("Unable to create SharedMemory from closed FileDescriptor");
        }
        this.mFileDescriptor = fileDescriptor;
        this.mSize = nGetSize(this.mFileDescriptor);
        if (this.mSize <= 0) {
            throw new IllegalArgumentException("FileDescriptor is not a valid ashmem fd");
        }
        this.mMemoryRegistration = new MemoryRegistration(this.mSize);
        this.mCleaner = Cleaner.create(this.mFileDescriptor, new Closer(this.mFileDescriptor, this.mMemoryRegistration));
    }

    public static SharedMemory create(String str, int i) throws ErrnoException {
        if (i <= 0) {
            throw new IllegalArgumentException("Size must be greater than zero");
        }
        return new SharedMemory(nCreate(str, i));
    }

    private void checkOpen() {
        if (!this.mFileDescriptor.valid()) {
            throw new IllegalStateException("SharedMemory is closed");
        }
    }

    private static void validateProt(int i) {
        if ((i & (~PROT_MASK)) != 0) {
            throw new IllegalArgumentException("Invalid prot value");
        }
    }

    public boolean setProtect(int i) {
        checkOpen();
        validateProt(i);
        return nSetProt(this.mFileDescriptor, i) == 0;
    }

    public FileDescriptor getFileDescriptor() {
        return this.mFileDescriptor;
    }

    public int getFd() {
        return this.mFileDescriptor.getInt$();
    }

    public int getSize() {
        checkOpen();
        return this.mSize;
    }

    public ByteBuffer mapReadWrite() throws ErrnoException {
        return map(OsConstants.PROT_READ | OsConstants.PROT_WRITE, 0, this.mSize);
    }

    public ByteBuffer mapReadOnly() throws ErrnoException {
        return map(OsConstants.PROT_READ, 0, this.mSize);
    }

    public ByteBuffer map(int i, int i2, int i3) throws ErrnoException {
        checkOpen();
        validateProt(i);
        if (i2 < 0) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }
        if (i3 <= 0) {
            throw new IllegalArgumentException("Length must be > 0");
        }
        if (i2 + i3 > this.mSize) {
            throw new IllegalArgumentException("offset + length must not exceed getSize()");
        }
        long jMmap = Os.mmap(0L, i3, i, OsConstants.MAP_SHARED, this.mFileDescriptor, i2);
        return new DirectByteBuffer(i3, jMmap, this.mFileDescriptor, new Unmapper(jMmap, i3, this.mMemoryRegistration.acquire()), (i & OsConstants.PROT_WRITE) == 0);
    }

    public static void unmap(ByteBuffer byteBuffer) {
        if (byteBuffer instanceof DirectByteBuffer) {
            NioUtils.freeDirectBuffer(byteBuffer);
            return;
        }
        throw new IllegalArgumentException("ByteBuffer wasn't created by #map(int, int, int); can't unmap");
    }

    @Override
    public void close() {
        if (this.mCleaner != null) {
            this.mCleaner.clean();
            this.mCleaner = null;
        }
    }

    @Override
    public int describeContents() {
        return 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        checkOpen();
        parcel.writeFileDescriptor(this.mFileDescriptor);
    }

    private static final class Closer implements Runnable {
        private FileDescriptor mFd;
        private MemoryRegistration mMemoryReference;

        private Closer(FileDescriptor fileDescriptor, MemoryRegistration memoryRegistration) {
            this.mFd = fileDescriptor;
            this.mMemoryReference = memoryRegistration;
        }

        @Override
        public void run() {
            try {
                Os.close(this.mFd);
            } catch (ErrnoException e) {
            }
            this.mMemoryReference.release();
            this.mMemoryReference = null;
        }
    }

    private static final class Unmapper implements Runnable {
        private long mAddress;
        private MemoryRegistration mMemoryReference;
        private int mSize;

        private Unmapper(long j, int i, MemoryRegistration memoryRegistration) {
            this.mAddress = j;
            this.mSize = i;
            this.mMemoryReference = memoryRegistration;
        }

        @Override
        public void run() {
            try {
                Os.munmap(this.mAddress, this.mSize);
            } catch (ErrnoException e) {
            }
            this.mMemoryReference.release();
            this.mMemoryReference = null;
        }
    }

    private static final class MemoryRegistration {
        private int mReferenceCount;
        private int mSize;

        private MemoryRegistration(int i) {
            this.mSize = i;
            this.mReferenceCount = 1;
            VMRuntime.getRuntime().registerNativeAllocation(this.mSize);
        }

        public synchronized MemoryRegistration acquire() {
            this.mReferenceCount++;
            return this;
        }

        public synchronized void release() {
            this.mReferenceCount--;
            if (this.mReferenceCount == 0) {
                VMRuntime.getRuntime().registerNativeFree(this.mSize);
            }
        }
    }
}
