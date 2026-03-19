package android.support.v8.renderscript;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.Type;
import android.util.Log;
import com.mediatek.camera.common.device.v2.Camera2Proxy;
import java.nio.ByteBuffer;

public class Allocation extends BaseObj {
    static BitmapFactory.Options mBitmapOptions = new BitmapFactory.Options();
    Allocation mAdaptedAllocation;
    boolean mAutoPadding;
    Bitmap mBitmap;
    ByteBuffer mByteBuffer;
    long mByteBufferStride;
    int mCurrentCount;
    int mCurrentDimX;
    int mCurrentDimY;
    int mCurrentDimZ;
    boolean mIncAllocDestroyed;
    long mIncCompatAllocation;
    boolean mReadAllowed;
    Type.CubemapFace mSelectedFace;
    int mSelectedLOD;
    int mSize;
    Type mType;
    int mUsage;
    boolean mWriteAllowed;

    public enum MipmapControl {
        MIPMAP_NONE(0),
        MIPMAP_FULL(1),
        MIPMAP_ON_SYNC_TO_TEXTURE(2);

        int mID;

        MipmapControl(int i) {
            this.mID = i;
        }
    }

    public void setIncAllocID(long j) {
        this.mIncCompatAllocation = j;
    }

    private long getIDSafe() {
        if (this.mAdaptedAllocation != null) {
            return this.mAdaptedAllocation.getID(this.mRS);
        }
        return getID(this.mRS);
    }

    private void updateCacheInfo(Type type) {
        this.mCurrentDimX = type.getX();
        this.mCurrentDimY = type.getY();
        this.mCurrentDimZ = type.getZ();
        this.mCurrentCount = this.mCurrentDimX;
        if (this.mCurrentDimY > 1) {
            this.mCurrentCount *= this.mCurrentDimY;
        }
        if (this.mCurrentDimZ > 1) {
            this.mCurrentCount *= this.mCurrentDimZ;
        }
    }

    private void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    Allocation(long j, RenderScript renderScript, Type type, int i) {
        super(j, renderScript);
        this.mByteBuffer = null;
        this.mByteBufferStride = 0L;
        this.mReadAllowed = true;
        this.mWriteAllowed = true;
        this.mAutoPadding = false;
        this.mSelectedFace = Type.CubemapFace.POSITIVE_X;
        if ((i & (-228)) != 0) {
            throw new RSIllegalArgumentException("Unknown usage specified.");
        }
        if ((i & 32) != 0) {
            this.mWriteAllowed = false;
            if ((i & (-36)) != 0) {
                throw new RSIllegalArgumentException("Invalid usage combination.");
            }
        }
        this.mType = type;
        this.mUsage = i;
        this.mIncCompatAllocation = 0L;
        this.mIncAllocDestroyed = false;
        if (type != null) {
            this.mSize = this.mType.getCount() * this.mType.getElement().getBytesSize();
            updateCacheInfo(type);
        }
        if (RenderScript.sUseGCHooks) {
            try {
                RenderScript.registerNativeAllocation.invoke(RenderScript.sRuntime, Integer.valueOf(this.mSize));
            } catch (Exception e) {
                Log.e("RenderScript_jni", "Couldn't invoke registerNativeAllocation:" + e);
                throw new RSRuntimeException("Couldn't invoke registerNativeAllocation:" + e);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        if (RenderScript.sUseGCHooks) {
            RenderScript.registerNativeFree.invoke(RenderScript.sRuntime, Integer.valueOf(this.mSize));
        }
        super.finalize();
    }

    private void validateIsInt8() {
        if (this.mType.mElement.mType == Element.DataType.SIGNED_8 || this.mType.mElement.mType == Element.DataType.UNSIGNED_8) {
            return;
        }
        throw new RSIllegalArgumentException("8 bit integer source does not match allocation type " + this.mType.mElement.mType);
    }

    public Type getType() {
        return this.mType;
    }

    private void validateBitmapFormat(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            throw new RSIllegalArgumentException("Bitmap has an unsupported format for this operation");
        }
        switch (AnonymousClass1.$SwitchMap$android$graphics$Bitmap$Config[config.ordinal()]) {
            case Camera2Proxy.TEMPLATE_PREVIEW:
                if (this.mType.getElement().mKind != Element.DataKind.PIXEL_A) {
                    throw new RSIllegalArgumentException("Allocation kind is " + this.mType.getElement().mKind + ", type " + this.mType.getElement().mType + " of " + this.mType.getElement().getBytesSize() + " bytes, passed bitmap was " + config);
                }
                return;
            case Camera2Proxy.TEMPLATE_STILL_CAPTURE:
                if (this.mType.getElement().mKind != Element.DataKind.PIXEL_RGBA || this.mType.getElement().getBytesSize() != 4) {
                    throw new RSIllegalArgumentException("Allocation kind is " + this.mType.getElement().mKind + ", type " + this.mType.getElement().mType + " of " + this.mType.getElement().getBytesSize() + " bytes, passed bitmap was " + config);
                }
                return;
            case Camera2Proxy.TEMPLATE_RECORD:
                if (this.mType.getElement().mKind != Element.DataKind.PIXEL_RGB || this.mType.getElement().getBytesSize() != 2) {
                    throw new RSIllegalArgumentException("Allocation kind is " + this.mType.getElement().mKind + ", type " + this.mType.getElement().mType + " of " + this.mType.getElement().getBytesSize() + " bytes, passed bitmap was " + config);
                }
                return;
            case Camera2Proxy.TEMPLATE_VIDEO_SNAPSHOT:
                if (this.mType.getElement().mKind != Element.DataKind.PIXEL_RGBA || this.mType.getElement().getBytesSize() != 2) {
                    throw new RSIllegalArgumentException("Allocation kind is " + this.mType.getElement().mKind + ", type " + this.mType.getElement().mType + " of " + this.mType.getElement().getBytesSize() + " bytes, passed bitmap was " + config);
                }
                return;
            default:
                return;
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$android$graphics$Bitmap$Config = new int[Bitmap.Config.values().length];

        static {
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.ALPHA_8.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.ARGB_8888.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.RGB_565.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$graphics$Bitmap$Config[Bitmap.Config.ARGB_4444.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    private void validateBitmapSize(Bitmap bitmap) {
        if (this.mCurrentDimX != bitmap.getWidth() || this.mCurrentDimY != bitmap.getHeight()) {
            throw new RSIllegalArgumentException("Cannot update allocation from bitmap, sizes mismatch");
        }
    }

    private void copyFromUnchecked(Object obj, Element.DataType dataType, int i) throws Throwable {
        this.mRS.validate();
        if (this.mCurrentDimZ > 0) {
            copy3DRangeFromUnchecked(0, 0, 0, this.mCurrentDimX, this.mCurrentDimY, this.mCurrentDimZ, obj, dataType, i);
        } else if (this.mCurrentDimY > 0) {
            copy2DRangeFromUnchecked(0, 0, this.mCurrentDimX, this.mCurrentDimY, obj, dataType, i);
        } else {
            copy1DRangeFromUnchecked(0, this.mCurrentCount, obj, dataType, i);
        }
    }

    public void copyFrom(byte[] bArr) throws Throwable {
        validateIsInt8();
        copyFromUnchecked(bArr, Element.DataType.SIGNED_8, bArr.length);
    }

    private void data1DChecks(int i, int i2, int i3, int i4, boolean z) {
        this.mRS.validate();
        if (i < 0) {
            throw new RSIllegalArgumentException("Offset must be >= 0.");
        }
        if (i2 < 1) {
            throw new RSIllegalArgumentException("Count must be >= 1.");
        }
        if (i + i2 > this.mCurrentCount) {
            throw new RSIllegalArgumentException("Overflow, Available count " + this.mCurrentCount + ", got " + i2 + " at offset " + i + ".");
        }
        if (z) {
            if (i3 < (i4 / 4) * 3) {
                throw new RSIllegalArgumentException("Array too small for allocation type.");
            }
        } else if (i3 < i4) {
            throw new RSIllegalArgumentException("Array too small for allocation type.");
        }
    }

    private void copy1DRangeFromUnchecked(int i, int i2, Object obj, Element.DataType dataType, int i3) {
        boolean z;
        int bytesSize = this.mType.mElement.getBytesSize() * i2;
        if (this.mAutoPadding && this.mType.getElement().getVectorSize() == 3) {
            z = true;
        } else {
            z = false;
        }
        boolean z2 = z;
        data1DChecks(i, i2, i3 * dataType.mSize, bytesSize, z2);
        this.mRS.nAllocationData1D(getIDSafe(), i, this.mSelectedLOD, i2, obj, bytesSize, dataType, this.mType.mElement.mType.mSize, z2);
    }

    private void validate2DRange(int i, int i2, int i3, int i4) {
        if (this.mAdaptedAllocation == null) {
            if (i < 0 || i2 < 0) {
                throw new RSIllegalArgumentException("Offset cannot be negative.");
            }
            if (i4 < 0 || i3 < 0) {
                throw new RSIllegalArgumentException("Height or width cannot be negative.");
            }
            if (i + i3 > this.mCurrentDimX || i2 + i4 > this.mCurrentDimY) {
                throw new RSIllegalArgumentException("Updated region larger than allocation.");
            }
        }
    }

    void copy2DRangeFromUnchecked(int i, int i2, int i3, int i4, Object obj, Element.DataType dataType, int i5) throws Throwable {
        boolean z;
        int i6;
        this.mRS.validate();
        validate2DRange(i, i2, i3, i4);
        int bytesSize = this.mType.mElement.getBytesSize() * i3 * i4;
        int i7 = dataType.mSize * i5;
        if (this.mAutoPadding && this.mType.getElement().getVectorSize() == 3) {
            if ((bytesSize / 4) * 3 > i7) {
                throw new RSIllegalArgumentException("Array too small for allocation type.");
            }
            i6 = bytesSize;
            z = true;
        } else {
            if (bytesSize > i7) {
                throw new RSIllegalArgumentException("Array too small for allocation type.");
            }
            z = false;
            i6 = i7;
        }
        this.mRS.nAllocationData2D(getIDSafe(), i, i2, this.mSelectedLOD, this.mSelectedFace.mID, i3, i4, obj, i6, dataType, this.mType.mElement.mType.mSize, z);
    }

    private void validate3DRange(int i, int i2, int i3, int i4, int i5, int i6) {
        if (this.mAdaptedAllocation == null) {
            if (i < 0 || i2 < 0 || i3 < 0) {
                throw new RSIllegalArgumentException("Offset cannot be negative.");
            }
            if (i5 < 0 || i4 < 0 || i6 < 0) {
                throw new RSIllegalArgumentException("Height or width cannot be negative.");
            }
            if (i + i4 > this.mCurrentDimX || i2 + i5 > this.mCurrentDimY || i3 + i6 > this.mCurrentDimZ) {
                throw new RSIllegalArgumentException("Updated region larger than allocation.");
            }
        }
    }

    private void copy3DRangeFromUnchecked(int i, int i2, int i3, int i4, int i5, int i6, Object obj, Element.DataType dataType, int i7) throws Throwable {
        boolean z;
        int i8;
        this.mRS.validate();
        validate3DRange(i, i2, i3, i4, i5, i6);
        int bytesSize = this.mType.mElement.getBytesSize() * i4 * i5 * i6;
        int i9 = dataType.mSize * i7;
        if (this.mAutoPadding && this.mType.getElement().getVectorSize() == 3) {
            if ((bytesSize / 4) * 3 > i9) {
                throw new RSIllegalArgumentException("Array too small for allocation type.");
            }
            i8 = bytesSize;
            z = true;
        } else {
            if (bytesSize > i9) {
                throw new RSIllegalArgumentException("Array too small for allocation type.");
            }
            z = false;
            i8 = i9;
        }
        this.mRS.nAllocationData3D(getIDSafe(), i, i2, i3, this.mSelectedLOD, i4, i5, i6, obj, i8, dataType, this.mType.mElement.mType.mSize, z);
    }

    public void copyTo(Bitmap bitmap) {
        this.mRS.validate();
        validateBitmapFormat(bitmap);
        validateBitmapSize(bitmap);
        this.mRS.nAllocationCopyToBitmap(getID(this.mRS), bitmap);
    }

    static {
        mBitmapOptions.inScaled = false;
    }

    public static Allocation createTyped(RenderScript renderScript, Type type, MipmapControl mipmapControl, int i) {
        renderScript.validate();
        if (type.getID(renderScript) == 0) {
            throw new RSInvalidStateException("Bad Type");
        }
        if (!renderScript.usingIO() && (i & 32) != 0) {
            throw new RSRuntimeException("USAGE_IO not supported, Allocation creation failed.");
        }
        long jNAllocationCreateTyped = renderScript.nAllocationCreateTyped(type.getID(renderScript), mipmapControl.mID, i, 0L);
        if (jNAllocationCreateTyped == 0) {
            throw new RSRuntimeException("Allocation creation failed.");
        }
        return new Allocation(jNAllocationCreateTyped, renderScript, type, i);
    }

    public static Allocation createTyped(RenderScript renderScript, Type type, int i) {
        return createTyped(renderScript, type, MipmapControl.MIPMAP_NONE, i);
    }

    static Element elementFromBitmap(RenderScript renderScript, Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == Bitmap.Config.ALPHA_8) {
            return Element.A_8(renderScript);
        }
        if (config == Bitmap.Config.ARGB_4444) {
            return Element.RGBA_4444(renderScript);
        }
        if (config == Bitmap.Config.ARGB_8888) {
            return Element.RGBA_8888(renderScript);
        }
        if (config == Bitmap.Config.RGB_565) {
            return Element.RGB_565(renderScript);
        }
        throw new RSInvalidStateException("Bad bitmap type: " + config);
    }

    static Type typeFromBitmap(RenderScript renderScript, Bitmap bitmap, MipmapControl mipmapControl) {
        Type.Builder builder = new Type.Builder(renderScript, elementFromBitmap(renderScript, bitmap));
        builder.setX(bitmap.getWidth());
        builder.setY(bitmap.getHeight());
        builder.setMipmaps(mipmapControl == MipmapControl.MIPMAP_FULL);
        return builder.create();
    }

    public static Allocation createFromBitmap(RenderScript renderScript, Bitmap bitmap, MipmapControl mipmapControl, int i) {
        renderScript.validate();
        if (bitmap.getConfig() == null) {
            if ((i & 128) != 0) {
                throw new RSIllegalArgumentException("USAGE_SHARED cannot be used with a Bitmap that has a null config.");
            }
            Bitmap bitmapCreateBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
            new Canvas(bitmapCreateBitmap).drawBitmap(bitmap, 0.0f, 0.0f, (Paint) null);
            return createFromBitmap(renderScript, bitmapCreateBitmap, mipmapControl, i);
        }
        Type typeTypeFromBitmap = typeFromBitmap(renderScript, bitmap, mipmapControl);
        if (mipmapControl == MipmapControl.MIPMAP_NONE && typeTypeFromBitmap.getElement().isCompatible(Element.RGBA_8888(renderScript)) && i == 131) {
            long jNAllocationCreateBitmapBackedAllocation = renderScript.nAllocationCreateBitmapBackedAllocation(typeTypeFromBitmap.getID(renderScript), mipmapControl.mID, bitmap, i);
            if (jNAllocationCreateBitmapBackedAllocation == 0) {
                throw new RSRuntimeException("Load failed.");
            }
            Allocation allocation = new Allocation(jNAllocationCreateBitmapBackedAllocation, renderScript, typeTypeFromBitmap, i);
            allocation.setBitmap(bitmap);
            return allocation;
        }
        long jNAllocationCreateFromBitmap = renderScript.nAllocationCreateFromBitmap(typeTypeFromBitmap.getID(renderScript), mipmapControl.mID, bitmap, i);
        if (jNAllocationCreateFromBitmap == 0) {
            throw new RSRuntimeException("Load failed.");
        }
        return new Allocation(jNAllocationCreateFromBitmap, renderScript, typeTypeFromBitmap, i);
    }

    public static Allocation createFromBitmap(RenderScript renderScript, Bitmap bitmap) {
        return createFromBitmap(renderScript, bitmap, MipmapControl.MIPMAP_NONE, 131);
    }
}
