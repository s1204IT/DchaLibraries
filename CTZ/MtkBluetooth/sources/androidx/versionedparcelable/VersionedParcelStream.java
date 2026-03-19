package androidx.versionedparcelable;

import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcelable;
import android.support.annotation.RestrictTo;
import android.util.SparseArray;
import androidx.versionedparcelable.VersionedParcel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.Set;

@RestrictTo({RestrictTo.Scope.LIBRARY})
class VersionedParcelStream extends VersionedParcel {
    private static final int TYPE_BOOLEAN = 5;
    private static final int TYPE_BOOLEAN_ARRAY = 6;
    private static final int TYPE_DOUBLE = 7;
    private static final int TYPE_DOUBLE_ARRAY = 8;
    private static final int TYPE_FLOAT = 13;
    private static final int TYPE_FLOAT_ARRAY = 14;
    private static final int TYPE_INT = 9;
    private static final int TYPE_INT_ARRAY = 10;
    private static final int TYPE_LONG = 11;
    private static final int TYPE_LONG_ARRAY = 12;
    private static final int TYPE_NULL = 0;
    private static final int TYPE_STRING = 3;
    private static final int TYPE_STRING_ARRAY = 4;
    private static final int TYPE_SUB_BUNDLE = 1;
    private static final int TYPE_SUB_PERSISTABLE_BUNDLE = 2;
    private static final Charset UTF_16 = Charset.forName("UTF-16");
    private final SparseArray<InputBuffer> mCachedFields = new SparseArray<>();
    private DataInputStream mCurrentInput;
    private DataOutputStream mCurrentOutput;
    private FieldBuffer mFieldBuffer;
    private boolean mIgnoreParcelables;
    private final DataInputStream mMasterInput;
    private final DataOutputStream mMasterOutput;

    public VersionedParcelStream(InputStream input, OutputStream output) {
        this.mMasterInput = input != null ? new DataInputStream(input) : null;
        this.mMasterOutput = output != null ? new DataOutputStream(output) : null;
        this.mCurrentInput = this.mMasterInput;
        this.mCurrentOutput = this.mMasterOutput;
    }

    @Override
    public boolean isStream() {
        return true;
    }

    @Override
    public void setSerializationFlags(boolean allowSerialization, boolean ignoreParcelables) {
        if (!allowSerialization) {
            throw new RuntimeException("Serialization of this object is not allowed");
        }
        this.mIgnoreParcelables = ignoreParcelables;
    }

    @Override
    public void closeField() {
        if (this.mFieldBuffer == null) {
            return;
        }
        try {
            if (this.mFieldBuffer.mOutput.size() != 0) {
                this.mFieldBuffer.flushField();
            }
            this.mFieldBuffer = null;
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    protected VersionedParcel createSubParcel() {
        return new VersionedParcelStream(this.mCurrentInput, this.mCurrentOutput);
    }

    @Override
    public boolean readField(int fieldId) {
        InputBuffer buffer = this.mCachedFields.get(fieldId);
        if (buffer != null) {
            this.mCachedFields.remove(fieldId);
            this.mCurrentInput = buffer.mInputStream;
            return true;
        }
        while (true) {
            try {
                int fieldInfo = this.mMasterInput.readInt();
                int size = fieldInfo & 65535;
                if (size == 65535) {
                    size = this.mMasterInput.readInt();
                }
                InputBuffer buffer2 = new InputBuffer(65535 & (fieldInfo >> 16), size, this.mMasterInput);
                if (buffer2.mFieldId == fieldId) {
                    this.mCurrentInput = buffer2.mInputStream;
                    return true;
                }
                this.mCachedFields.put(buffer2.mFieldId, buffer2);
            } catch (IOException e) {
                return false;
            }
        }
    }

    @Override
    public void setOutputField(int fieldId) {
        closeField();
        this.mFieldBuffer = new FieldBuffer(fieldId, this.mMasterOutput);
        this.mCurrentOutput = this.mFieldBuffer.mDataStream;
    }

    @Override
    public void writeByteArray(byte[] b) {
        try {
            if (b != null) {
                this.mCurrentOutput.writeInt(b.length);
                this.mCurrentOutput.write(b);
            } else {
                this.mCurrentOutput.writeInt(-1);
            }
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public void writeByteArray(byte[] b, int offset, int len) {
        try {
            if (b != null) {
                this.mCurrentOutput.writeInt(len);
                this.mCurrentOutput.write(b, offset, len);
            } else {
                this.mCurrentOutput.writeInt(-1);
            }
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public void writeInt(int val) {
        try {
            this.mCurrentOutput.writeInt(val);
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public void writeLong(long val) {
        try {
            this.mCurrentOutput.writeLong(val);
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public void writeFloat(float val) {
        try {
            this.mCurrentOutput.writeFloat(val);
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public void writeDouble(double val) {
        try {
            this.mCurrentOutput.writeDouble(val);
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public void writeString(String val) {
        try {
            if (val != null) {
                byte[] bytes = val.getBytes(UTF_16);
                this.mCurrentOutput.writeInt(bytes.length);
                this.mCurrentOutput.write(bytes);
            } else {
                this.mCurrentOutput.writeInt(-1);
            }
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public void writeBoolean(boolean val) {
        try {
            this.mCurrentOutput.writeBoolean(val);
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public void writeStrongBinder(IBinder val) {
        if (!this.mIgnoreParcelables) {
            throw new RuntimeException("Binders cannot be written to an OutputStream");
        }
    }

    @Override
    public void writeParcelable(Parcelable p) {
        if (!this.mIgnoreParcelables) {
            throw new RuntimeException("Parcelables cannot be written to an OutputStream");
        }
    }

    @Override
    public void writeStrongInterface(IInterface val) {
        if (!this.mIgnoreParcelables) {
            throw new RuntimeException("Binders cannot be written to an OutputStream");
        }
    }

    @Override
    public IBinder readStrongBinder() {
        return null;
    }

    @Override
    public <T extends Parcelable> T readParcelable() {
        return null;
    }

    @Override
    public int readInt() {
        try {
            return this.mCurrentInput.readInt();
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public long readLong() {
        try {
            return this.mCurrentInput.readLong();
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public float readFloat() {
        try {
            return this.mCurrentInput.readFloat();
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public double readDouble() {
        try {
            return this.mCurrentInput.readDouble();
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public String readString() {
        try {
            int len = this.mCurrentInput.readInt();
            if (len > 0) {
                byte[] bytes = new byte[len];
                this.mCurrentInput.readFully(bytes);
                return new String(bytes, UTF_16);
            }
            return null;
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public byte[] readByteArray() {
        try {
            int len = this.mCurrentInput.readInt();
            if (len > 0) {
                byte[] bytes = new byte[len];
                this.mCurrentInput.readFully(bytes);
                return bytes;
            }
            return null;
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public boolean readBoolean() {
        try {
            return this.mCurrentInput.readBoolean();
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public void writeBundle(Bundle val) {
        try {
            if (val != null) {
                Set<String> keys = val.keySet();
                this.mCurrentOutput.writeInt(keys.size());
                for (String key : keys) {
                    writeString(key);
                    Object o = val.get(key);
                    writeObject(o);
                }
                return;
            }
            this.mCurrentOutput.writeInt(-1);
        } catch (IOException e) {
            throw new VersionedParcel.ParcelException(e);
        }
    }

    @Override
    public Bundle readBundle() {
        int size = readInt();
        if (size < 0) {
            return null;
        }
        Bundle b = new Bundle();
        for (int i = 0; i < size; i++) {
            String key = readString();
            readObject(readInt(), key, b);
        }
        return b;
    }

    private void writeObject(Object obj) {
        if (obj == 0) {
            writeInt(0);
            return;
        }
        if (obj instanceof Bundle) {
            writeInt(1);
            writeBundle(obj);
            return;
        }
        if (obj instanceof String) {
            writeInt(3);
            writeString(obj);
            return;
        }
        if (obj instanceof String[]) {
            writeInt(4);
            writeArray(obj);
            return;
        }
        if (obj instanceof Boolean) {
            writeInt(5);
            writeBoolean(obj.booleanValue());
            return;
        }
        if (obj instanceof boolean[]) {
            writeInt(6);
            writeBooleanArray(obj);
            return;
        }
        if (obj instanceof Double) {
            writeInt(7);
            writeDouble(obj.doubleValue());
            return;
        }
        if (obj instanceof double[]) {
            writeInt(8);
            writeDoubleArray(obj);
            return;
        }
        if (obj instanceof Integer) {
            writeInt(9);
            writeInt(obj.intValue());
            return;
        }
        if (obj instanceof int[]) {
            writeInt(10);
            writeIntArray(obj);
            return;
        }
        if (obj instanceof Long) {
            writeInt(11);
            writeLong(obj.longValue());
            return;
        }
        if (obj instanceof long[]) {
            writeInt(12);
            writeLongArray(obj);
            return;
        }
        if (obj instanceof Float) {
            writeInt(13);
            writeFloat(obj.floatValue());
        } else if (obj instanceof float[]) {
            writeInt(14);
            writeFloatArray(obj);
        } else {
            throw new IllegalArgumentException("Unsupported type " + obj.getClass());
        }
    }

    private void readObject(int type, String key, Bundle b) {
        switch (type) {
            case 0:
                b.putParcelable(key, null);
                return;
            case 1:
                b.putBundle(key, readBundle());
                return;
            case 2:
                b.putBundle(key, readBundle());
                return;
            case 3:
                b.putString(key, readString());
                return;
            case 4:
                b.putStringArray(key, (String[]) readArray(new String[0]));
                return;
            case 5:
                b.putBoolean(key, readBoolean());
                return;
            case 6:
                b.putBooleanArray(key, readBooleanArray());
                return;
            case 7:
                b.putDouble(key, readDouble());
                return;
            case 8:
                b.putDoubleArray(key, readDoubleArray());
                return;
            case 9:
                b.putInt(key, readInt());
                return;
            case 10:
                b.putIntArray(key, readIntArray());
                return;
            case 11:
                b.putLong(key, readLong());
                return;
            case 12:
                b.putLongArray(key, readLongArray());
                return;
            case 13:
                b.putFloat(key, readFloat());
                return;
            case 14:
                b.putFloatArray(key, readFloatArray());
                return;
            default:
                throw new RuntimeException("Unknown type " + type);
        }
    }

    private static class FieldBuffer {
        private final int mFieldId;
        private final DataOutputStream mTarget;
        private final ByteArrayOutputStream mOutput = new ByteArrayOutputStream();
        private final DataOutputStream mDataStream = new DataOutputStream(this.mOutput);

        FieldBuffer(int fieldId, DataOutputStream target) {
            this.mFieldId = fieldId;
            this.mTarget = target;
        }

        void flushField() throws IOException {
            this.mDataStream.flush();
            int size = this.mOutput.size();
            int fieldInfo = (this.mFieldId << 16) | (size >= 65535 ? 65535 : size);
            this.mTarget.writeInt(fieldInfo);
            if (size >= 65535) {
                this.mTarget.writeInt(size);
            }
            this.mOutput.writeTo(this.mTarget);
        }
    }

    private static class InputBuffer {
        private final int mFieldId;
        private final DataInputStream mInputStream;
        private final int mSize;

        InputBuffer(int fieldId, int size, DataInputStream inputStream) throws IOException {
            this.mSize = size;
            this.mFieldId = fieldId;
            byte[] data = new byte[this.mSize];
            inputStream.readFully(data);
            this.mInputStream = new DataInputStream(new ByteArrayInputStream(data));
        }
    }
}
