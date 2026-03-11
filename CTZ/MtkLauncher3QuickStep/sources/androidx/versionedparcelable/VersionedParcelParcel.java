package androidx.versionedparcelable;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RestrictTo;
import android.util.SparseIntArray;

@RestrictTo({RestrictTo.Scope.LIBRARY})
class VersionedParcelParcel extends VersionedParcel {
    private static final boolean DEBUG = false;
    private static final String TAG = "VersionedParcelParcel";
    private int mCurrentField;
    private final int mEnd;
    private int mNextRead;
    private final int mOffset;
    private final Parcel mParcel;
    private final SparseIntArray mPositionLookup;
    private final String mPrefix;

    VersionedParcelParcel(Parcel p) {
        this(p, p.dataPosition(), p.dataSize(), "");
    }

    VersionedParcelParcel(Parcel p, int offset, int end, String prefix) {
        this.mPositionLookup = new SparseIntArray();
        this.mCurrentField = -1;
        this.mNextRead = 0;
        this.mParcel = p;
        this.mOffset = offset;
        this.mEnd = end;
        this.mNextRead = this.mOffset;
        this.mPrefix = prefix;
    }

    private int readUntilField(int fieldId) {
        while (this.mNextRead < this.mEnd) {
            this.mParcel.setDataPosition(this.mNextRead);
            int size = this.mParcel.readInt();
            int fid = this.mParcel.readInt();
            this.mNextRead += size;
            if (fid == fieldId) {
                return this.mParcel.dataPosition();
            }
        }
        return -1;
    }

    @Override
    public boolean readField(int fieldId) {
        int position = readUntilField(fieldId);
        if (position == -1) {
            return false;
        }
        this.mParcel.setDataPosition(position);
        return true;
    }

    @Override
    public void setOutputField(int fieldId) {
        closeField();
        this.mCurrentField = fieldId;
        this.mPositionLookup.put(fieldId, this.mParcel.dataPosition());
        writeInt(0);
        writeInt(fieldId);
    }

    @Override
    public void closeField() {
        if (this.mCurrentField >= 0) {
            int currentFieldPosition = this.mPositionLookup.get(this.mCurrentField);
            int position = this.mParcel.dataPosition();
            int size = position - currentFieldPosition;
            this.mParcel.setDataPosition(currentFieldPosition);
            this.mParcel.writeInt(size);
            this.mParcel.setDataPosition(position);
        }
    }

    @Override
    protected VersionedParcel createSubParcel() {
        return new VersionedParcelParcel(this.mParcel, this.mParcel.dataPosition(), this.mNextRead == this.mOffset ? this.mEnd : this.mNextRead, this.mPrefix + "  ");
    }

    @Override
    public void writeByteArray(byte[] b) {
        if (b != null) {
            this.mParcel.writeInt(b.length);
            this.mParcel.writeByteArray(b);
        } else {
            this.mParcel.writeInt(-1);
        }
    }

    @Override
    public void writeByteArray(byte[] b, int offset, int len) {
        if (b != null) {
            this.mParcel.writeInt(b.length);
            this.mParcel.writeByteArray(b, offset, len);
        } else {
            this.mParcel.writeInt(-1);
        }
    }

    @Override
    public void writeInt(int val) {
        this.mParcel.writeInt(val);
    }

    @Override
    public void writeLong(long val) {
        this.mParcel.writeLong(val);
    }

    @Override
    public void writeFloat(float val) {
        this.mParcel.writeFloat(val);
    }

    @Override
    public void writeDouble(double val) {
        this.mParcel.writeDouble(val);
    }

    @Override
    public void writeString(String val) {
        this.mParcel.writeString(val);
    }

    @Override
    public void writeStrongBinder(IBinder val) {
        this.mParcel.writeStrongBinder(val);
    }

    @Override
    public void writeParcelable(Parcelable p) {
        this.mParcel.writeParcelable(p, 0);
    }

    @Override
    public void writeBoolean(boolean z) {
        this.mParcel.writeInt(z ? 1 : 0);
    }

    @Override
    public void writeStrongInterface(IInterface val) {
        this.mParcel.writeStrongInterface(val);
    }

    @Override
    public void writeBundle(Bundle val) {
        this.mParcel.writeBundle(val);
    }

    @Override
    public int readInt() {
        return this.mParcel.readInt();
    }

    @Override
    public long readLong() {
        return this.mParcel.readLong();
    }

    @Override
    public float readFloat() {
        return this.mParcel.readFloat();
    }

    @Override
    public double readDouble() {
        return this.mParcel.readDouble();
    }

    @Override
    public String readString() {
        return this.mParcel.readString();
    }

    @Override
    public IBinder readStrongBinder() {
        return this.mParcel.readStrongBinder();
    }

    @Override
    public byte[] readByteArray() {
        int len = this.mParcel.readInt();
        if (len < 0) {
            return null;
        }
        byte[] bytes = new byte[len];
        this.mParcel.readByteArray(bytes);
        return bytes;
    }

    @Override
    public <T extends Parcelable> T readParcelable() {
        return (T) this.mParcel.readParcelable(getClass().getClassLoader());
    }

    @Override
    public Bundle readBundle() {
        return this.mParcel.readBundle(getClass().getClassLoader());
    }

    @Override
    public boolean readBoolean() {
        return this.mParcel.readInt() != 0;
    }
}
