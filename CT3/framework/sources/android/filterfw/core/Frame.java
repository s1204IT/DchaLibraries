package android.filterfw.core;

import android.graphics.Bitmap;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.Environment;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public abstract class Frame {
    private static final int BUFSIZE = 4096;
    public static final int NO_BINDING = 0;
    private static final String TAG = "Frame";
    public static final long TIMESTAMP_NOT_SET = -2;
    public static final long TIMESTAMP_UNKNOWN = -1;
    private long mBindingId;
    private int mBindingType;
    private FrameFormat mFormat;
    private FrameManager mFrameManager;
    private boolean mReadOnly;
    private int mRefCount;
    private boolean mReusable;
    private long mTimestamp;

    public abstract Bitmap getBitmap();

    public abstract ByteBuffer getData();

    public abstract float[] getFloats();

    public abstract int[] getInts();

    public abstract Object getObjectValue();

    protected abstract boolean hasNativeAllocation();

    protected abstract void releaseNativeAllocation();

    public abstract void setBitmap(Bitmap bitmap);

    public abstract void setData(ByteBuffer byteBuffer, int i, int i2);

    public abstract void setFloats(float[] fArr);

    public abstract void setInts(int[] iArr);

    Frame(FrameFormat format, FrameManager frameManager) {
        this.mReadOnly = false;
        this.mReusable = false;
        this.mRefCount = 1;
        this.mBindingType = 0;
        this.mBindingId = 0L;
        this.mTimestamp = -2L;
        this.mFormat = format.mutableCopy();
        this.mFrameManager = frameManager;
    }

    Frame(FrameFormat format, FrameManager frameManager, int bindingType, long bindingId) {
        this.mReadOnly = false;
        this.mReusable = false;
        this.mRefCount = 1;
        this.mBindingType = 0;
        this.mBindingId = 0L;
        this.mTimestamp = -2L;
        this.mFormat = format.mutableCopy();
        this.mFrameManager = frameManager;
        this.mBindingType = bindingType;
        this.mBindingId = bindingId;
    }

    public FrameFormat getFormat() {
        return this.mFormat;
    }

    public int getCapacity() {
        return getFormat().getSize();
    }

    public boolean isReadOnly() {
        return this.mReadOnly;
    }

    public int getBindingType() {
        return this.mBindingType;
    }

    public long getBindingId() {
        return this.mBindingId;
    }

    public void setObjectValue(Object obj) {
        assertFrameMutable();
        if (obj instanceof int[]) {
            setInts(obj);
            return;
        }
        if (obj instanceof float[]) {
            setFloats(obj);
            return;
        }
        if (obj instanceof ByteBuffer) {
            setData(obj);
        } else if (obj instanceof Bitmap) {
            setBitmap(obj);
        } else {
            setGenericObjectValue(obj);
        }
    }

    public void setData(ByteBuffer buffer) {
        setData(buffer, 0, buffer.limit());
    }

    public void setData(byte[] bytes, int offset, int length) {
        setData(ByteBuffer.wrap(bytes, offset, length));
    }

    public void setTimestamp(long timestamp) {
        this.mTimestamp = timestamp;
    }

    public long getTimestamp() {
        return this.mTimestamp;
    }

    public void setDataFromFrame(Frame frame) {
        setData(frame.getData());
    }

    protected boolean requestResize(int[] newDimensions) {
        return false;
    }

    public int getRefCount() {
        return this.mRefCount;
    }

    public Frame release() {
        if (this.mFrameManager != null) {
            return this.mFrameManager.releaseFrame(this);
        }
        return this;
    }

    public Frame retain() {
        if (this.mFrameManager != null) {
            return this.mFrameManager.retainFrame(this);
        }
        return this;
    }

    public FrameManager getFrameManager() {
        return this.mFrameManager;
    }

    protected void assertFrameMutable() {
        if (!isReadOnly()) {
        } else {
            throw new RuntimeException("Attempting to modify read-only frame!");
        }
    }

    protected void setReusable(boolean reusable) {
        this.mReusable = reusable;
    }

    protected void setFormat(FrameFormat format) {
        this.mFormat = format.mutableCopy();
    }

    protected void setGenericObjectValue(Object value) {
        throw new RuntimeException("Cannot set object value of unsupported type: " + value.getClass());
    }

    protected static Bitmap convertBitmapToRGBA(Bitmap bitmap) {
        if (bitmap.getConfig() == Bitmap.Config.ARGB_8888) {
            return bitmap;
        }
        Bitmap result = bitmap.copy(Bitmap.Config.ARGB_8888, false);
        if (result == null) {
            throw new RuntimeException("Error converting bitmap to RGBA!");
        }
        if (result.getRowBytes() != result.getWidth() * 4) {
            throw new RuntimeException("Unsupported row byte count in bitmap!");
        }
        return result;
    }

    protected void reset(FrameFormat newFormat) {
        this.mFormat = newFormat.mutableCopy();
        this.mReadOnly = false;
        this.mRefCount = 1;
    }

    protected void onFrameStore() {
    }

    protected void onFrameFetch() {
    }

    final int incRefCount() {
        this.mRefCount++;
        return this.mRefCount;
    }

    final int decRefCount() {
        this.mRefCount--;
        return this.mRefCount;
    }

    final boolean isReusable() {
        return this.mReusable;
    }

    final void markReadOnly() {
        this.mReadOnly = true;
    }

    public void saveFrame(String name) throws Throwable {
        int savePixel = SystemProperties.getInt("debug.effect.save.pixel", 1);
        if (savePixel == 1) {
            savePixel(name, getData().array());
        }
        int saveImage = SystemProperties.getInt("debug.effect.save.image", 1);
        if (saveImage != 1) {
            return;
        }
        saveImage(name, getBitmap());
    }

    public void savePixel(String name, byte[] data) throws Throwable {
        DataOutputStream d;
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/debug_mca_output/" + SystemClock.uptimeMillis() + "_" + name + "_pixel.png");
        Uri uri = Uri.fromFile(file);
        Log.v(TAG, "savePixel(" + name + ") path=" + file.getPath());
        FileOutputStream f = null;
        BufferedOutputStream b = null;
        DataOutputStream d2 = null;
        try {
            try {
                FileOutputStream f2 = new FileOutputStream(file);
                try {
                    BufferedOutputStream b2 = new BufferedOutputStream(f2, 4096);
                    try {
                        d = new DataOutputStream(b2);
                    } catch (IOException e) {
                        e = e;
                        b = b2;
                        f = f2;
                    } catch (Throwable th) {
                        th = th;
                        b = b2;
                        f = f2;
                    }
                    try {
                        d.write(data);
                        d.writeUTF(uri.toString());
                        d.close();
                        closeSilently(f2);
                        closeSilently(b2);
                        closeSilently(d);
                    } catch (IOException e2) {
                        e = e2;
                        d2 = d;
                        b = b2;
                        f = f2;
                        Log.e(TAG, "Fail to store pixel. path=" + file.getPath(), e);
                        closeSilently(f);
                        closeSilently(b);
                        closeSilently(d2);
                    } catch (Throwable th2) {
                        th = th2;
                        d2 = d;
                        b = b2;
                        f = f2;
                        closeSilently(f);
                        closeSilently(b);
                        closeSilently(d2);
                        throw th;
                    }
                } catch (IOException e3) {
                    e = e3;
                    f = f2;
                } catch (Throwable th3) {
                    th = th3;
                    f = f2;
                }
            } catch (IOException e4) {
                e = e4;
            }
        } catch (Throwable th4) {
            th = th4;
        }
    }

    public void saveImage(String name, Bitmap bitmap) throws Throwable {
        FileOutputStream f;
        BufferedOutputStream b;
        DataOutputStream d;
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/debug_mca_output/" + SystemClock.uptimeMillis() + "_" + name + "_image.png");
        Uri uri = Uri.fromFile(file);
        Log.v(TAG, "saveImage(" + name + ") path=" + file.getPath());
        FileOutputStream f2 = null;
        BufferedOutputStream b2 = null;
        DataOutputStream d2 = null;
        try {
            try {
                f = new FileOutputStream(file);
                try {
                    b = new BufferedOutputStream(f, 4096);
                    try {
                        d = new DataOutputStream(b);
                    } catch (IOException e) {
                        e = e;
                        b2 = b;
                        f2 = f;
                    } catch (Throwable th) {
                        th = th;
                        b2 = b;
                        f2 = f;
                    }
                } catch (IOException e2) {
                    e = e2;
                    f2 = f;
                } catch (Throwable th2) {
                    th = th2;
                    f2 = f;
                }
            } catch (IOException e3) {
                e = e3;
            }
        } catch (Throwable th3) {
            th = th3;
        }
        try {
            d.writeUTF(uri.toString());
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, d);
            d.close();
            closeSilently(f);
            closeSilently(b);
            closeSilently(d);
        } catch (IOException e4) {
            e = e4;
            d2 = d;
            b2 = b;
            f2 = f;
            Log.e(TAG, "Fail to store image. path=" + file.getPath(), e);
            closeSilently(f2);
            closeSilently(b2);
            closeSilently(d2);
        } catch (Throwable th4) {
            th = th4;
            d2 = d;
            b2 = b;
            f2 = f;
            closeSilently(f2);
            closeSilently(b2);
            closeSilently(d2);
            throw th;
        }
    }

    public static void closeSilently(Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (IOException e) {
            Log.e(TAG, "closeSilently: Fail to close " + c, e);
        }
    }

    public static void wait3DReady() {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        GLES20.glReadPixels(0, 0, 1, 1, 6408, 5121, buffer);
    }
}
