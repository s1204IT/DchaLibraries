package android.graphics;

import android.content.res.AssetManager;
import android.graphics.FontListParser;
import android.util.Log;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

public class FontFamily {
    private static String TAG = "FontFamily";
    public long mNativePtr;

    private static native boolean nAddFont(long j, ByteBuffer byteBuffer, int i);

    private static native boolean nAddFontFromAsset(long j, AssetManager assetManager, String str);

    private static native boolean nAddFontWeightStyle(long j, ByteBuffer byteBuffer, int i, List<FontListParser.Axis> list, int i2, boolean z);

    private static native long nCreateFamily(String str, int i);

    private static native void nUnrefFamily(long j);

    public FontFamily() {
        this.mNativePtr = nCreateFamily(null, 0);
        if (this.mNativePtr != 0) {
        } else {
            throw new IllegalStateException("error creating native FontFamily");
        }
    }

    public FontFamily(String lang, String variant) {
        int varEnum = 0;
        if ("compact".equals(variant)) {
            varEnum = 1;
        } else if ("elegant".equals(variant)) {
            varEnum = 2;
        }
        this.mNativePtr = nCreateFamily(lang, varEnum);
        if (this.mNativePtr != 0) {
        } else {
            throw new IllegalStateException("error creating native FontFamily");
        }
    }

    protected void finalize() throws Throwable {
        try {
            nUnrefFamily(this.mNativePtr);
        } finally {
            super.finalize();
        }
    }

    public boolean addFont(String path, int ttcIndex) throws Throwable {
        Throwable th;
        FileInputStream file;
        Throwable th2 = null;
        FileInputStream file2 = null;
        try {
            file = new FileInputStream(path);
        } catch (Throwable th3) {
            th = th3;
        }
        try {
            FileChannel fileChannel = file.getChannel();
            long fontSize = fileChannel.size();
            ByteBuffer fontBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0L, fontSize);
            boolean zNAddFont = nAddFont(this.mNativePtr, fontBuffer, ttcIndex);
            if (file != null) {
                try {
                    try {
                        file.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error mapping font file " + path);
                        return false;
                    }
                } catch (Throwable th4) {
                    th2 = th4;
                }
            }
            if (th2 != null) {
                throw th2;
            }
            return zNAddFont;
        } catch (Throwable th5) {
            th = th5;
            file2 = file;
            try {
                throw th;
            } catch (Throwable th6) {
                th = th;
                th = th6;
                if (file2 != null) {
                    try {
                        file2.close();
                    } catch (Throwable th7) {
                        if (th == null) {
                            th = th7;
                        } else if (th != th7) {
                            th.addSuppressed(th7);
                        }
                    }
                }
                if (th == null) {
                    throw th;
                }
                throw th;
            }
        }
    }

    public boolean addFontWeightStyle(ByteBuffer font, int ttcIndex, List<FontListParser.Axis> axes, int weight, boolean style) {
        return nAddFontWeightStyle(this.mNativePtr, font, ttcIndex, axes, weight, style);
    }

    public boolean addFontFromAsset(AssetManager mgr, String path) {
        return nAddFontFromAsset(this.mNativePtr, mgr, path);
    }
}
