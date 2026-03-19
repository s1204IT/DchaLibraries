package android.content.res;

import android.os.Bundle;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.Parcelable;
import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AssetFileDescriptor implements Parcelable, Closeable {
    public static final Parcelable.Creator<AssetFileDescriptor> CREATOR = new Parcelable.Creator<AssetFileDescriptor>() {
        @Override
        public AssetFileDescriptor createFromParcel(Parcel parcel) {
            return new AssetFileDescriptor(parcel);
        }

        @Override
        public AssetFileDescriptor[] newArray(int i) {
            return new AssetFileDescriptor[i];
        }
    };
    public static final long UNKNOWN_LENGTH = -1;
    private final Bundle mExtras;
    private final ParcelFileDescriptor mFd;
    private final long mLength;
    private final long mStartOffset;

    public AssetFileDescriptor(ParcelFileDescriptor parcelFileDescriptor, long j, long j2) {
        this(parcelFileDescriptor, j, j2, null);
    }

    public AssetFileDescriptor(ParcelFileDescriptor parcelFileDescriptor, long j, long j2, Bundle bundle) {
        if (parcelFileDescriptor == null) {
            throw new IllegalArgumentException("fd must not be null");
        }
        if (j2 < 0 && j != 0) {
            throw new IllegalArgumentException("startOffset must be 0 when using UNKNOWN_LENGTH");
        }
        this.mFd = parcelFileDescriptor;
        this.mStartOffset = j;
        this.mLength = j2;
        this.mExtras = bundle;
    }

    public ParcelFileDescriptor getParcelFileDescriptor() {
        return this.mFd;
    }

    public FileDescriptor getFileDescriptor() {
        return this.mFd.getFileDescriptor();
    }

    public long getStartOffset() {
        return this.mStartOffset;
    }

    public Bundle getExtras() {
        return this.mExtras;
    }

    public long getLength() {
        if (this.mLength >= 0) {
            return this.mLength;
        }
        long statSize = this.mFd.getStatSize();
        if (statSize >= 0) {
            return statSize;
        }
        return -1L;
    }

    public long getDeclaredLength() {
        return this.mLength;
    }

    @Override
    public void close() throws IOException {
        this.mFd.close();
    }

    public FileInputStream createInputStream() throws IOException {
        if (this.mLength < 0) {
            return new ParcelFileDescriptor.AutoCloseInputStream(this.mFd);
        }
        return new AutoCloseInputStream(this);
    }

    public FileOutputStream createOutputStream() throws IOException {
        if (this.mLength < 0) {
            return new ParcelFileDescriptor.AutoCloseOutputStream(this.mFd);
        }
        return new AutoCloseOutputStream(this);
    }

    public String toString() {
        return "{AssetFileDescriptor: " + this.mFd + " start=" + this.mStartOffset + " len=" + this.mLength + "}";
    }

    public static class AutoCloseInputStream extends ParcelFileDescriptor.AutoCloseInputStream {
        private long mRemaining;

        public AutoCloseInputStream(AssetFileDescriptor assetFileDescriptor) throws IOException {
            super(assetFileDescriptor.getParcelFileDescriptor());
            super.skip(assetFileDescriptor.getStartOffset());
            this.mRemaining = (int) assetFileDescriptor.getLength();
        }

        @Override
        public int available() throws IOException {
            if (this.mRemaining < 0) {
                return super.available();
            }
            if (this.mRemaining < 2147483647L) {
                return (int) this.mRemaining;
            }
            return Integer.MAX_VALUE;
        }

        @Override
        public int read() throws IOException {
            byte[] bArr = new byte[1];
            if (read(bArr, 0, 1) != -1) {
                return bArr[0] & 255;
            }
            return -1;
        }

        @Override
        public int read(byte[] bArr, int i, int i2) throws IOException {
            if (this.mRemaining < 0) {
                return super.read(bArr, i, i2);
            }
            if (this.mRemaining == 0) {
                return -1;
            }
            if (i2 > this.mRemaining) {
                i2 = (int) this.mRemaining;
            }
            int i3 = super.read(bArr, i, i2);
            if (i3 >= 0) {
                this.mRemaining -= (long) i3;
            }
            return i3;
        }

        @Override
        public int read(byte[] bArr) throws IOException {
            return read(bArr, 0, bArr.length);
        }

        @Override
        public long skip(long j) throws IOException {
            if (this.mRemaining < 0) {
                return super.skip(j);
            }
            if (this.mRemaining == 0) {
                return -1L;
            }
            if (j > this.mRemaining) {
                j = this.mRemaining;
            }
            long jSkip = super.skip(j);
            if (jSkip >= 0) {
                this.mRemaining -= jSkip;
            }
            return jSkip;
        }

        @Override
        public void mark(int i) {
            if (this.mRemaining >= 0) {
                return;
            }
            super.mark(i);
        }

        @Override
        public boolean markSupported() {
            if (this.mRemaining >= 0) {
                return false;
            }
            return super.markSupported();
        }

        @Override
        public synchronized void reset() throws IOException {
            if (this.mRemaining >= 0) {
                return;
            }
            super.reset();
        }
    }

    public static class AutoCloseOutputStream extends ParcelFileDescriptor.AutoCloseOutputStream {
        private long mRemaining;

        public AutoCloseOutputStream(AssetFileDescriptor assetFileDescriptor) throws IOException {
            super(assetFileDescriptor.getParcelFileDescriptor());
            if (assetFileDescriptor.getParcelFileDescriptor().seekTo(assetFileDescriptor.getStartOffset()) < 0) {
                throw new IOException("Unable to seek");
            }
            this.mRemaining = (int) assetFileDescriptor.getLength();
        }

        @Override
        public void write(byte[] bArr, int i, int i2) throws IOException {
            if (this.mRemaining < 0) {
                super.write(bArr, i, i2);
            } else {
                if (this.mRemaining == 0) {
                    return;
                }
                if (i2 > this.mRemaining) {
                    i2 = (int) this.mRemaining;
                }
                super.write(bArr, i, i2);
                this.mRemaining -= (long) i2;
            }
        }

        @Override
        public void write(byte[] bArr) throws IOException {
            if (this.mRemaining < 0) {
                super.write(bArr);
                return;
            }
            if (this.mRemaining == 0) {
                return;
            }
            int length = bArr.length;
            if (length > this.mRemaining) {
                length = (int) this.mRemaining;
            }
            super.write(bArr);
            this.mRemaining -= (long) length;
        }

        @Override
        public void write(int i) throws IOException {
            if (this.mRemaining < 0) {
                super.write(i);
            } else {
                if (this.mRemaining == 0) {
                    return;
                }
                super.write(i);
                this.mRemaining--;
            }
        }
    }

    @Override
    public int describeContents() {
        return this.mFd.describeContents();
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        this.mFd.writeToParcel(parcel, i);
        parcel.writeLong(this.mStartOffset);
        parcel.writeLong(this.mLength);
        if (this.mExtras != null) {
            parcel.writeInt(1);
            parcel.writeBundle(this.mExtras);
        } else {
            parcel.writeInt(0);
        }
    }

    AssetFileDescriptor(Parcel parcel) {
        this.mFd = ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
        this.mStartOffset = parcel.readLong();
        this.mLength = parcel.readLong();
        if (parcel.readInt() != 0) {
            this.mExtras = parcel.readBundle();
        } else {
            this.mExtras = null;
        }
    }
}
