package android.emoji;

import android.graphics.Bitmap;
import java.lang.ref.WeakReference;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EmojiFactory {
    private String mName;
    private long mNativeEmojiFactory;
    private int sCacheSize = 100;
    private Map<Integer, WeakReference<Bitmap>> mCache = new CustomLinkedHashMap();

    private native void nativeDestructor(long j);

    private native int nativeGetAndroidPuaFromVendorSpecificPua(long j, int i);

    private native int nativeGetAndroidPuaFromVendorSpecificSjis(long j, char c);

    private native Bitmap nativeGetBitmapFromAndroidPua(long j, int i);

    private native int nativeGetMaximumAndroidPua(long j);

    private native int nativeGetMaximumVendorSpecificPua(long j);

    private native int nativeGetMinimumAndroidPua(long j);

    private native int nativeGetMinimumVendorSpecificPua(long j);

    private native int nativeGetVendorSpecificPuaFromAndroidPua(long j, int i);

    private native int nativeGetVendorSpecificSjisFromAndroidPua(long j, int i);

    public static native EmojiFactory newAvailableInstance();

    public static native EmojiFactory newInstance(String str);

    private class CustomLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
        public CustomLinkedHashMap() {
            super(16, 0.75f, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            return size() > EmojiFactory.this.sCacheSize;
        }
    }

    private EmojiFactory(long nativeEmojiFactory, String name) {
        this.mNativeEmojiFactory = nativeEmojiFactory;
        this.mName = name;
    }

    protected void finalize() throws Throwable {
        try {
            nativeDestructor(this.mNativeEmojiFactory);
        } finally {
            super.finalize();
        }
    }

    public String name() {
        return this.mName;
    }

    public synchronized Bitmap getBitmapFromAndroidPua(int pua) {
        Bitmap ret;
        WeakReference<Bitmap> cache = this.mCache.get(Integer.valueOf(pua));
        if (cache == null) {
            ret = nativeGetBitmapFromAndroidPua(this.mNativeEmojiFactory, pua);
            if (ret != null) {
                this.mCache.put(Integer.valueOf(pua), new WeakReference<>(ret));
            }
        } else {
            Bitmap tmp = cache.get();
            if (tmp == null) {
                ret = nativeGetBitmapFromAndroidPua(this.mNativeEmojiFactory, pua);
                this.mCache.put(Integer.valueOf(pua), new WeakReference<>(ret));
            } else {
                ret = tmp;
            }
        }
        return ret;
    }

    public synchronized Bitmap getBitmapFromVendorSpecificSjis(char sjis) {
        return getBitmapFromAndroidPua(getAndroidPuaFromVendorSpecificSjis(sjis));
    }

    public synchronized Bitmap getBitmapFromVendorSpecificPua(int vsp) {
        return getBitmapFromAndroidPua(getAndroidPuaFromVendorSpecificPua(vsp));
    }

    public int getAndroidPuaFromVendorSpecificSjis(char sjis) {
        return nativeGetAndroidPuaFromVendorSpecificSjis(this.mNativeEmojiFactory, sjis);
    }

    public int getVendorSpecificSjisFromAndroidPua(int pua) {
        return nativeGetVendorSpecificSjisFromAndroidPua(this.mNativeEmojiFactory, pua);
    }

    public int getAndroidPuaFromVendorSpecificPua(int vsp) {
        return nativeGetAndroidPuaFromVendorSpecificPua(this.mNativeEmojiFactory, vsp);
    }

    public String getAndroidPuaFromVendorSpecificPua(String vspString) {
        int newCodePoint;
        if (vspString == null) {
            return null;
        }
        int minVsp = nativeGetMinimumVendorSpecificPua(this.mNativeEmojiFactory);
        int maxVsp = nativeGetMaximumVendorSpecificPua(this.mNativeEmojiFactory);
        int len = vspString.length();
        int[] codePoints = new int[vspString.codePointCount(0, len)];
        int new_len = 0;
        int i = 0;
        while (i < len) {
            int codePoint = vspString.codePointAt(i);
            if (minVsp <= codePoint && codePoint <= maxVsp && (newCodePoint = getAndroidPuaFromVendorSpecificPua(codePoint)) > 0) {
                codePoints[new_len] = newCodePoint;
            } else {
                codePoints[new_len] = codePoint;
            }
            i = vspString.offsetByCodePoints(i, 1);
            new_len++;
        }
        return new String(codePoints, 0, new_len);
    }

    public int getVendorSpecificPuaFromAndroidPua(int pua) {
        return nativeGetVendorSpecificPuaFromAndroidPua(this.mNativeEmojiFactory, pua);
    }

    public String getVendorSpecificPuaFromAndroidPua(String puaString) {
        int newCodePoint;
        if (puaString == null) {
            return null;
        }
        int minVsp = nativeGetMinimumAndroidPua(this.mNativeEmojiFactory);
        int maxVsp = nativeGetMaximumAndroidPua(this.mNativeEmojiFactory);
        int len = puaString.length();
        int[] codePoints = new int[puaString.codePointCount(0, len)];
        int new_len = 0;
        int i = 0;
        while (i < len) {
            int codePoint = puaString.codePointAt(i);
            if (minVsp <= codePoint && codePoint <= maxVsp && (newCodePoint = getVendorSpecificPuaFromAndroidPua(codePoint)) > 0) {
                codePoints[new_len] = newCodePoint;
            } else {
                codePoints[new_len] = codePoint;
            }
            i = puaString.offsetByCodePoints(i, 1);
            new_len++;
        }
        return new String(codePoints, 0, new_len);
    }

    public int getMinimumAndroidPua() {
        return nativeGetMinimumAndroidPua(this.mNativeEmojiFactory);
    }

    public int getMaximumAndroidPua() {
        return nativeGetMaximumAndroidPua(this.mNativeEmojiFactory);
    }
}
