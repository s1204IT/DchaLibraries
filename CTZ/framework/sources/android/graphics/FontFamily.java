package android.graphics;

import android.content.res.AssetManager;
import android.graphics.fonts.FontVariationAxis;
import android.text.TextUtils;
import android.util.Log;
import dalvik.annotation.optimization.CriticalNative;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import libcore.util.NativeAllocationRegistry;

public class FontFamily {
    private static String TAG = "FontFamily";
    private static final NativeAllocationRegistry sBuilderRegistry = new NativeAllocationRegistry(FontFamily.class.getClassLoader(), nGetBuilderReleaseFunc(), 64);
    private static final NativeAllocationRegistry sFamilyRegistry = new NativeAllocationRegistry(FontFamily.class.getClassLoader(), nGetFamilyReleaseFunc(), 64);
    private long mBuilderPtr;
    private Runnable mNativeBuilderCleaner;
    public long mNativePtr;

    @CriticalNative
    private static native void nAddAxisValue(long j, int i, float f);

    private static native boolean nAddFont(long j, ByteBuffer byteBuffer, int i, int i2, int i3);

    private static native boolean nAddFontFromAssetManager(long j, AssetManager assetManager, String str, int i, boolean z, int i2, int i3, int i4);

    private static native boolean nAddFontWeightStyle(long j, ByteBuffer byteBuffer, int i, int i2, int i3);

    @CriticalNative
    private static native long nCreateFamily(long j);

    @CriticalNative
    private static native long nGetBuilderReleaseFunc();

    @CriticalNative
    private static native long nGetFamilyReleaseFunc();

    private static native long nInitBuilder(String str, int i);

    public FontFamily() {
        this.mBuilderPtr = nInitBuilder(null, 0);
        this.mNativeBuilderCleaner = sBuilderRegistry.registerNativeAllocation(this, this.mBuilderPtr);
    }

    public FontFamily(String[] strArr, int i) {
        String strJoin;
        if (strArr == null || strArr.length == 0) {
            strJoin = null;
        } else if (strArr.length == 1) {
            strJoin = strArr[0];
        } else {
            strJoin = TextUtils.join(",", strArr);
        }
        this.mBuilderPtr = nInitBuilder(strJoin, i);
        this.mNativeBuilderCleaner = sBuilderRegistry.registerNativeAllocation(this, this.mBuilderPtr);
    }

    public boolean freeze() {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("This FontFamily is already frozen");
        }
        this.mNativePtr = nCreateFamily(this.mBuilderPtr);
        this.mNativeBuilderCleaner.run();
        this.mBuilderPtr = 0L;
        if (this.mNativePtr != 0) {
            sFamilyRegistry.registerNativeAllocation(this, this.mNativePtr);
        }
        return this.mNativePtr != 0;
    }

    public void abortCreation() {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("This FontFamily is already frozen or abandoned");
        }
        this.mNativeBuilderCleaner.run();
        this.mBuilderPtr = 0L;
    }

    public boolean addFont(String str, int i, FontVariationAxis[] fontVariationAxisArr, int i2, int i3) {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("Unable to call addFont after freezing.");
        }
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            Throwable th = null;
            try {
                try {
                    FileChannel channel = fileInputStream.getChannel();
                    MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
                    if (fontVariationAxisArr != null) {
                        for (FontVariationAxis fontVariationAxis : fontVariationAxisArr) {
                            nAddAxisValue(this.mBuilderPtr, fontVariationAxis.getOpenTypeTagValue(), fontVariationAxis.getStyleValue());
                        }
                    }
                    boolean zNAddFont = nAddFont(this.mBuilderPtr, map, i, i2, i3);
                    fileInputStream.close();
                    return zNAddFont;
                } catch (Throwable th2) {
                    th = th2;
                    throw th;
                }
            } catch (Throwable th3) {
                if (th == null) {
                    fileInputStream.close();
                    throw th3;
                }
                try {
                    fileInputStream.close();
                    throw th3;
                } catch (Throwable th4) {
                    th.addSuppressed(th4);
                    throw th3;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error mapping font file " + str);
            return false;
        }
    }

    public boolean addFontFromBuffer(ByteBuffer byteBuffer, int i, FontVariationAxis[] fontVariationAxisArr, int i2, int i3) {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("Unable to call addFontWeightStyle after freezing.");
        }
        if (fontVariationAxisArr != null) {
            for (FontVariationAxis fontVariationAxis : fontVariationAxisArr) {
                nAddAxisValue(this.mBuilderPtr, fontVariationAxis.getOpenTypeTagValue(), fontVariationAxis.getStyleValue());
            }
        }
        return nAddFontWeightStyle(this.mBuilderPtr, byteBuffer, i, i2, i3);
    }

    public boolean addFontFromAssetManager(AssetManager assetManager, String str, int i, boolean z, int i2, int i3, int i4, FontVariationAxis[] fontVariationAxisArr) {
        if (this.mBuilderPtr == 0) {
            throw new IllegalStateException("Unable to call addFontFromAsset after freezing.");
        }
        if (fontVariationAxisArr != null) {
            for (FontVariationAxis fontVariationAxis : fontVariationAxisArr) {
                nAddAxisValue(this.mBuilderPtr, fontVariationAxis.getOpenTypeTagValue(), fontVariationAxis.getStyleValue());
            }
        }
        return nAddFontFromAssetManager(this.mBuilderPtr, assetManager, str, i, z, i2, i3, i4);
    }

    private static boolean nAddFont(long j, ByteBuffer byteBuffer, int i) {
        return nAddFont(j, byteBuffer, i, -1, -1);
    }
}
