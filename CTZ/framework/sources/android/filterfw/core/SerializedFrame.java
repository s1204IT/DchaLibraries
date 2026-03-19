package android.filterfw.core;

import android.filterfw.format.ObjectFormat;
import android.graphics.Bitmap;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class SerializedFrame extends Frame {
    private static final int INITIAL_CAPACITY = 64;
    private DirectByteOutputStream mByteOutputStream;
    private ObjectOutputStream mObjectOut;

    private class DirectByteOutputStream extends OutputStream {
        private byte[] mBuffer;
        private int mOffset = 0;
        private int mDataOffset = 0;

        public DirectByteOutputStream(int i) {
            this.mBuffer = null;
            this.mBuffer = new byte[i];
        }

        private final void ensureFit(int i) {
            if (this.mOffset + i > this.mBuffer.length) {
                byte[] bArr = this.mBuffer;
                this.mBuffer = new byte[Math.max(this.mOffset + i, this.mBuffer.length * 2)];
                System.arraycopy(bArr, 0, this.mBuffer, 0, this.mOffset);
            }
        }

        public final void markHeaderEnd() {
            this.mDataOffset = this.mOffset;
        }

        public final int getSize() {
            return this.mOffset;
        }

        public byte[] getByteArray() {
            return this.mBuffer;
        }

        @Override
        public final void write(byte[] bArr) {
            write(bArr, 0, bArr.length);
        }

        @Override
        public final void write(byte[] bArr, int i, int i2) {
            ensureFit(i2);
            System.arraycopy(bArr, i, this.mBuffer, this.mOffset, i2);
            this.mOffset += i2;
        }

        @Override
        public final void write(int i) {
            ensureFit(1);
            byte[] bArr = this.mBuffer;
            int i2 = this.mOffset;
            this.mOffset = i2 + 1;
            bArr[i2] = (byte) i;
        }

        public final void reset() {
            this.mOffset = this.mDataOffset;
        }

        public final DirectByteInputStream getInputStream() {
            return SerializedFrame.this.new DirectByteInputStream(this.mBuffer, this.mOffset);
        }
    }

    private class DirectByteInputStream extends InputStream {
        private byte[] mBuffer;
        private int mPos = 0;
        private int mSize;

        public DirectByteInputStream(byte[] bArr, int i) {
            this.mBuffer = bArr;
            this.mSize = i;
        }

        @Override
        public final int available() {
            return this.mSize - this.mPos;
        }

        @Override
        public final int read() {
            if (this.mPos >= this.mSize) {
                return -1;
            }
            byte[] bArr = this.mBuffer;
            int i = this.mPos;
            this.mPos = i + 1;
            return bArr[i] & 255;
        }

        @Override
        public final int read(byte[] bArr, int i, int i2) {
            if (this.mPos >= this.mSize) {
                return -1;
            }
            if (this.mPos + i2 > this.mSize) {
                i2 = this.mSize - this.mPos;
            }
            System.arraycopy(this.mBuffer, this.mPos, bArr, i, i2);
            this.mPos += i2;
            return i2;
        }

        @Override
        public final long skip(long j) {
            if (((long) this.mPos) + j > this.mSize) {
                j = this.mSize - this.mPos;
            }
            if (j < 0) {
                return 0L;
            }
            this.mPos = (int) (((long) this.mPos) + j);
            return j;
        }
    }

    SerializedFrame(FrameFormat frameFormat, FrameManager frameManager) {
        super(frameFormat, frameManager);
        setReusable(false);
        try {
            this.mByteOutputStream = new DirectByteOutputStream(64);
            this.mObjectOut = new ObjectOutputStream(this.mByteOutputStream);
            this.mByteOutputStream.markHeaderEnd();
        } catch (IOException e) {
            throw new RuntimeException("Could not create serialization streams for SerializedFrame!", e);
        }
    }

    static SerializedFrame wrapObject(Object obj, FrameManager frameManager) {
        SerializedFrame serializedFrame = new SerializedFrame(ObjectFormat.fromObject(obj, 1), frameManager);
        serializedFrame.setObjectValue(obj);
        return serializedFrame;
    }

    @Override
    protected boolean hasNativeAllocation() {
        return false;
    }

    @Override
    protected void releaseNativeAllocation() {
    }

    @Override
    public Object getObjectValue() {
        return deserializeObjectValue();
    }

    @Override
    public void setInts(int[] iArr) {
        assertFrameMutable();
        setGenericObjectValue(iArr);
    }

    @Override
    public int[] getInts() {
        Object objDeserializeObjectValue = deserializeObjectValue();
        if (objDeserializeObjectValue instanceof int[]) {
            return (int[]) objDeserializeObjectValue;
        }
        return null;
    }

    @Override
    public void setFloats(float[] fArr) {
        assertFrameMutable();
        setGenericObjectValue(fArr);
    }

    @Override
    public float[] getFloats() {
        Object objDeserializeObjectValue = deserializeObjectValue();
        if (objDeserializeObjectValue instanceof float[]) {
            return (float[]) objDeserializeObjectValue;
        }
        return null;
    }

    @Override
    public void setData(ByteBuffer byteBuffer, int i, int i2) {
        assertFrameMutable();
        setGenericObjectValue(ByteBuffer.wrap(byteBuffer.array(), i, i2));
    }

    @Override
    public ByteBuffer getData() {
        Object objDeserializeObjectValue = deserializeObjectValue();
        if (objDeserializeObjectValue instanceof ByteBuffer) {
            return (ByteBuffer) objDeserializeObjectValue;
        }
        return null;
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
        assertFrameMutable();
        setGenericObjectValue(bitmap);
    }

    @Override
    public Bitmap getBitmap() {
        Object objDeserializeObjectValue = deserializeObjectValue();
        if (objDeserializeObjectValue instanceof Bitmap) {
            return (Bitmap) objDeserializeObjectValue;
        }
        return null;
    }

    @Override
    protected void setGenericObjectValue(Object obj) {
        serializeObjectValue(obj);
    }

    private final void serializeObjectValue(Object obj) {
        try {
            this.mByteOutputStream.reset();
            this.mObjectOut.writeObject(obj);
            this.mObjectOut.flush();
            this.mObjectOut.close();
        } catch (IOException e) {
            throw new RuntimeException("Could not serialize object " + obj + " in " + this + "!", e);
        }
    }

    private final Object deserializeObjectValue() {
        try {
            return new ObjectInputStream(this.mByteOutputStream.getInputStream()).readObject();
        } catch (IOException e) {
            throw new RuntimeException("Could not deserialize object in " + this + "!", e);
        } catch (ClassNotFoundException e2) {
            throw new RuntimeException("Unable to deserialize object of unknown class in " + this + "!", e2);
        }
    }

    public String toString() {
        return "SerializedFrame (" + getFormat() + ")";
    }
}
