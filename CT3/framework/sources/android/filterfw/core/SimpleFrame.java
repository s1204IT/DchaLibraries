package android.filterfw.core;

import android.filterfw.format.ObjectFormat;
import android.graphics.Bitmap;
import java.nio.ByteBuffer;

public class SimpleFrame extends Frame {
    private Object mObject;

    SimpleFrame(FrameFormat format, FrameManager frameManager) {
        super(format, frameManager);
        initWithFormat(format);
        setReusable(false);
    }

    static SimpleFrame wrapObject(Object object, FrameManager frameManager) {
        FrameFormat format = ObjectFormat.fromObject(object, 1);
        SimpleFrame result = new SimpleFrame(format, frameManager);
        result.setObjectValue(object);
        return result;
    }

    private void initWithFormat(FrameFormat format) {
        int count = format.getLength();
        int baseType = format.getBaseType();
        switch (baseType) {
            case 2:
                this.mObject = new byte[count];
                break;
            case 3:
                this.mObject = new short[count];
                break;
            case 4:
                this.mObject = new int[count];
                break;
            case 5:
                this.mObject = new float[count];
                break;
            case 6:
                this.mObject = new double[count];
                break;
            default:
                this.mObject = null;
                break;
        }
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
        return this.mObject;
    }

    @Override
    public void setInts(int[] ints) {
        assertFrameMutable();
        setGenericObjectValue(ints);
    }

    @Override
    public int[] getInts() {
        if (this.mObject instanceof int[]) {
            return (int[]) this.mObject;
        }
        return null;
    }

    @Override
    public void setFloats(float[] floats) {
        assertFrameMutable();
        setGenericObjectValue(floats);
    }

    @Override
    public float[] getFloats() {
        if (this.mObject instanceof float[]) {
            return (float[]) this.mObject;
        }
        return null;
    }

    @Override
    public void setData(ByteBuffer buffer, int offset, int length) {
        assertFrameMutable();
        setGenericObjectValue(ByteBuffer.wrap(buffer.array(), offset, length));
    }

    @Override
    public ByteBuffer getData() {
        if (this.mObject instanceof ByteBuffer) {
            return (ByteBuffer) this.mObject;
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
        if (this.mObject instanceof Bitmap) {
            return (Bitmap) this.mObject;
        }
        return null;
    }

    private void setFormatObjectClass(Class objectClass) {
        MutableFrameFormat format = getFormat().mutableCopy();
        format.setObjectClass(objectClass);
        setFormat(format);
    }

    @Override
    protected void setGenericObjectValue(Object object) {
        FrameFormat format = getFormat();
        if (format.getObjectClass() == null) {
            setFormatObjectClass(object.getClass());
        } else if (!format.getObjectClass().isAssignableFrom(object.getClass())) {
            throw new RuntimeException("Attempting to set object value of type '" + object.getClass() + "' on SimpleFrame of type '" + format.getObjectClass() + "'!");
        }
        this.mObject = object;
    }

    public String toString() {
        return "SimpleFrame (" + getFormat() + ")";
    }
}
