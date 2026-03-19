package android.util;

import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import java.io.Closeable;
import java.io.IOException;
import java.util.UUID;
import libcore.io.IoUtils;

public final class MemoryIntArray implements Parcelable, Closeable {
    public static final Parcelable.Creator<MemoryIntArray> CREATOR = new Parcelable.Creator<MemoryIntArray>() {
        @Override
        public MemoryIntArray createFromParcel(Parcel parcel) {
            try {
                return new MemoryIntArray(parcel);
            } catch (IOException e) {
                throw new IllegalArgumentException("Error unparceling MemoryIntArray");
            }
        }

        @Override
        public MemoryIntArray[] newArray(int i) {
            return new MemoryIntArray[i];
        }
    };
    private static final int MAX_SIZE = 1024;
    private static final String TAG = "MemoryIntArray";
    private final dalvik.system.CloseGuard mCloseGuard;
    private int mFd;
    private final boolean mIsOwner;
    private final long mMemoryAddr;

    private native void nativeClose(int i, long j, boolean z);

    private native int nativeCreate(String str, int i);

    private native int nativeGet(int i, long j, int i2);

    private native long nativeOpen(int i, boolean z);

    private native void nativeSet(int i, long j, int i2, int i3);

    private native int nativeSize(int i);

    public MemoryIntArray(int i) throws IOException {
        this.mCloseGuard = dalvik.system.CloseGuard.get();
        this.mFd = -1;
        if (i > 1024) {
            throw new IllegalArgumentException("Max size is 1024");
        }
        this.mIsOwner = true;
        this.mFd = nativeCreate(UUID.randomUUID().toString(), i);
        this.mMemoryAddr = nativeOpen(this.mFd, this.mIsOwner);
        this.mCloseGuard.open("close");
    }

    private MemoryIntArray(Parcel parcel) throws IOException {
        this.mCloseGuard = dalvik.system.CloseGuard.get();
        this.mFd = -1;
        this.mIsOwner = false;
        ParcelFileDescriptor parcelFileDescriptor = (ParcelFileDescriptor) parcel.readParcelable(null);
        if (parcelFileDescriptor == null) {
            throw new IOException("No backing file descriptor");
        }
        this.mFd = parcelFileDescriptor.detachFd();
        this.mMemoryAddr = nativeOpen(this.mFd, this.mIsOwner);
        this.mCloseGuard.open("close");
    }

    public boolean isWritable() {
        enforceNotClosed();
        return this.mIsOwner;
    }

    public int get(int i) throws IOException {
        enforceNotClosed();
        enforceValidIndex(i);
        return nativeGet(this.mFd, this.mMemoryAddr, i);
    }

    public void set(int i, int i2) throws IOException {
        enforceNotClosed();
        enforceWritable();
        enforceValidIndex(i);
        nativeSet(this.mFd, this.mMemoryAddr, i, i2);
    }

    public int size() throws IOException {
        enforceNotClosed();
        return nativeSize(this.mFd);
    }

    @Override
    public void close() throws IOException {
        if (!isClosed()) {
            nativeClose(this.mFd, this.mMemoryAddr, this.mIsOwner);
            this.mFd = -1;
            this.mCloseGuard.close();
        }
    }

    public boolean isClosed() {
        return this.mFd == -1;
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mCloseGuard != null) {
                this.mCloseGuard.warnIfOpen();
            }
            IoUtils.closeQuietly(this);
        } finally {
            super.finalize();
        }
    }

    @Override
    public int describeContents() {
        return 1;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        ParcelFileDescriptor parcelFileDescriptorAdoptFd = ParcelFileDescriptor.adoptFd(this.mFd);
        try {
            parcel.writeParcelable(parcelFileDescriptorAdoptFd, i & (-2));
        } finally {
            parcelFileDescriptorAdoptFd.detachFd();
        }
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (getClass() != obj.getClass() || this.mFd != ((MemoryIntArray) obj).mFd) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return this.mFd;
    }

    private void enforceNotClosed() {
        if (isClosed()) {
            throw new IllegalStateException("cannot interact with a closed instance");
        }
    }

    private void enforceValidIndex(int i) throws IOException {
        int size = size();
        if (i < 0 || i > size - 1) {
            StringBuilder sb = new StringBuilder();
            sb.append(i);
            sb.append(" not between 0 and ");
            sb.append(size - 1);
            throw new IndexOutOfBoundsException(sb.toString());
        }
    }

    private void enforceWritable() {
        if (!isWritable()) {
            throw new UnsupportedOperationException("array is not writable");
        }
    }

    public static int getMaxSize() {
        return 1024;
    }
}
