package android.graphics;

import android.content.res.AssetManager;
import android.content.res.FontResourcesParser;
import android.graphics.fonts.FontVariationAxis;
import android.media.tv.TvContract;
import android.net.Uri;
import android.provider.FontRequest;
import android.provider.FontsContract;
import android.text.FontConfig;
import android.util.ArrayMap;
import android.util.Base64;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.LruCache;
import android.util.SparseArray;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.content.NativeLibraryHelper;
import com.android.internal.logging.nano.MetricsProto;
import com.android.internal.util.Preconditions;
import dalvik.annotation.optimization.CriticalNative;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import libcore.util.NativeAllocationRegistry;
import org.xmlpull.v1.XmlPullParserException;

public class Typeface {
    public static final int BOLD = 1;
    public static final int BOLD_ITALIC = 3;
    public static final Typeface DEFAULT;
    public static final Typeface DEFAULT_BOLD;
    private static final String DEFAULT_FAMILY = "sans-serif";
    public static final int ITALIC = 2;
    public static final int MAX_WEIGHT = 1000;
    public static final Typeface MONOSPACE;
    public static final int NORMAL = 0;
    public static final int RESOLVE_BY_FONT_TABLE = -1;
    public static final Typeface SANS_SERIF;
    public static final Typeface SERIF;
    private static final int STYLE_ITALIC = 1;
    public static final int STYLE_MASK = 3;
    private static final int STYLE_NORMAL = 0;
    static Typeface sDefaultTypeface;
    static Typeface[] sDefaults;
    static final Map<String, FontFamily[]> sSystemFallbackMap;
    static final Map<String, Typeface> sSystemFontMap;
    private int mStyle;
    private int[] mSupportedAxes;
    private int mWeight;
    public long native_instance;
    private static String TAG = "Typeface";
    private static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(Typeface.class.getClassLoader(), nativeGetReleaseFunc(), 64);

    @GuardedBy("sStyledCacheLock")
    private static final LongSparseArray<SparseArray<Typeface>> sStyledTypefaceCache = new LongSparseArray<>(3);
    private static final Object sStyledCacheLock = new Object();

    @GuardedBy("sWeightCacheLock")
    private static final LongSparseArray<SparseArray<Typeface>> sWeightTypefaceCache = new LongSparseArray<>(3);
    private static final Object sWeightCacheLock = new Object();

    @GuardedBy("sDynamicCacheLock")
    private static final LruCache<String, Typeface> sDynamicTypefaceCache = new LruCache<>(16);
    private static final Object sDynamicCacheLock = new Object();
    private static final int[] EMPTY_AXES = new int[0];

    @Retention(RetentionPolicy.SOURCE)
    public @interface Style {
    }

    private static native long nativeCreateFromArray(long[] jArr, int i, int i2);

    private static native long nativeCreateFromTypeface(long j, int i);

    private static native long nativeCreateFromTypefaceWithExactStyle(long j, int i, boolean z);

    private static native long nativeCreateFromTypefaceWithVariation(long j, List<FontVariationAxis> list);

    private static native long nativeCreateWeightAlias(long j, int i);

    @CriticalNative
    private static native long nativeGetReleaseFunc();

    @CriticalNative
    private static native int nativeGetStyle(long j);

    private static native int[] nativeGetSupportedAxes(long j);

    @CriticalNative
    private static native int nativeGetWeight(long j);

    @CriticalNative
    private static native void nativeSetDefault(long j);

    static {
        ArrayMap arrayMap = new ArrayMap();
        ArrayMap arrayMap2 = new ArrayMap();
        buildSystemFallback("/system/etc/fonts.xml", "/system/fonts/", arrayMap, arrayMap2);
        sSystemFontMap = Collections.unmodifiableMap(arrayMap);
        sSystemFallbackMap = Collections.unmodifiableMap(arrayMap2);
        setDefault(sSystemFontMap.get(DEFAULT_FAMILY));
        String str = (String) null;
        DEFAULT = create(str, 0);
        DEFAULT_BOLD = create(str, 1);
        SANS_SERIF = create(DEFAULT_FAMILY, 0);
        SERIF = create("serif", 0);
        MONOSPACE = create("monospace", 0);
        sDefaults = new Typeface[]{DEFAULT, DEFAULT_BOLD, create(str, 2), create(str, 3)};
    }

    private static void setDefault(Typeface typeface) {
        sDefaultTypeface = typeface;
        nativeSetDefault(typeface.native_instance);
    }

    public int getWeight() {
        return this.mWeight;
    }

    public int getStyle() {
        return this.mStyle;
    }

    public final boolean isBold() {
        return (this.mStyle & 1) != 0;
    }

    public final boolean isItalic() {
        return (this.mStyle & 2) != 0;
    }

    public static Typeface createFromResources(AssetManager assetManager, String str, int i) {
        synchronized (sDynamicCacheLock) {
            String strCreateAssetUid = Builder.createAssetUid(assetManager, str, 0, null, -1, -1, DEFAULT_FAMILY);
            Typeface typeface = sDynamicTypefaceCache.get(strCreateAssetUid);
            if (typeface != null) {
                return typeface;
            }
            FontFamily fontFamily = new FontFamily();
            if (!fontFamily.addFontFromAssetManager(assetManager, str, i, false, 0, -1, -1, null)) {
                return null;
            }
            if (!fontFamily.freeze()) {
                return null;
            }
            Typeface typefaceCreateFromFamiliesWithDefault = createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, DEFAULT_FAMILY, -1, -1);
            sDynamicTypefaceCache.put(strCreateAssetUid, typefaceCreateFromFamiliesWithDefault);
            return typefaceCreateFromFamiliesWithDefault;
        }
    }

    public static Typeface createFromResources(FontResourcesParser.FamilyResourceEntry familyResourceEntry, AssetManager assetManager, String str) {
        if (familyResourceEntry instanceof FontResourcesParser.ProviderResourceEntry) {
            FontResourcesParser.ProviderResourceEntry providerResourceEntry = (FontResourcesParser.ProviderResourceEntry) familyResourceEntry;
            List<List<String>> certs = providerResourceEntry.getCerts();
            ArrayList arrayList = new ArrayList();
            if (certs != null) {
                for (int i = 0; i < certs.size(); i++) {
                    List<String> list = certs.get(i);
                    ArrayList arrayList2 = new ArrayList();
                    for (int i2 = 0; i2 < list.size(); i2++) {
                        arrayList2.add(Base64.decode(list.get(i2), 0));
                    }
                    arrayList.add(arrayList2);
                }
            }
            Typeface fontSync = FontsContract.getFontSync(new FontRequest(providerResourceEntry.getAuthority(), providerResourceEntry.getPackage(), providerResourceEntry.getQuery(), arrayList));
            return fontSync == null ? DEFAULT : fontSync;
        }
        Typeface typefaceFindFromCache = findFromCache(assetManager, str);
        if (typefaceFindFromCache != null) {
            return typefaceFindFromCache;
        }
        FontFamily fontFamily = new FontFamily();
        for (FontResourcesParser.FontFileResourceEntry fontFileResourceEntry : ((FontResourcesParser.FontFamilyFilesResourceEntry) familyResourceEntry).getEntries()) {
            if (!fontFamily.addFontFromAssetManager(assetManager, fontFileResourceEntry.getFileName(), 0, false, fontFileResourceEntry.getTtcIndex(), fontFileResourceEntry.getWeight(), fontFileResourceEntry.getItalic(), FontVariationAxis.fromFontVariationSettings(fontFileResourceEntry.getVariationSettings()))) {
                return null;
            }
        }
        if (!fontFamily.freeze()) {
            return null;
        }
        Typeface typefaceCreateFromFamiliesWithDefault = createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, DEFAULT_FAMILY, -1, -1);
        synchronized (sDynamicCacheLock) {
            sDynamicTypefaceCache.put(Builder.createAssetUid(assetManager, str, 0, null, -1, -1, DEFAULT_FAMILY), typefaceCreateFromFamiliesWithDefault);
        }
        return typefaceCreateFromFamiliesWithDefault;
    }

    public static Typeface findFromCache(AssetManager assetManager, String str) {
        synchronized (sDynamicCacheLock) {
            Typeface typeface = sDynamicTypefaceCache.get(Builder.createAssetUid(assetManager, str, 0, null, -1, -1, DEFAULT_FAMILY));
            if (typeface != null) {
                return typeface;
            }
            return null;
        }
    }

    public static final class Builder {
        public static final int BOLD_WEIGHT = 700;
        public static final int NORMAL_WEIGHT = 400;
        private AssetManager mAssetManager;
        private FontVariationAxis[] mAxes;
        private String mFallbackFamilyName;
        private FileDescriptor mFd;
        private Map<Uri, ByteBuffer> mFontBuffers;
        private FontsContract.FontInfo[] mFonts;
        private String mPath;
        private int mTtcIndex;
        private int mWeight = -1;
        private int mItalic = -1;

        public Builder(File file) {
            this.mPath = file.getAbsolutePath();
        }

        public Builder(FileDescriptor fileDescriptor) {
            this.mFd = fileDescriptor;
        }

        public Builder(String str) {
            this.mPath = str;
        }

        public Builder(AssetManager assetManager, String str) {
            this.mAssetManager = (AssetManager) Preconditions.checkNotNull(assetManager);
            this.mPath = (String) Preconditions.checkStringNotEmpty(str);
        }

        public Builder(FontsContract.FontInfo[] fontInfoArr, Map<Uri, ByteBuffer> map) {
            this.mFonts = fontInfoArr;
            this.mFontBuffers = map;
        }

        public Builder setWeight(int i) {
            this.mWeight = i;
            return this;
        }

        public Builder setItalic(boolean z) {
            this.mItalic = z ? 1 : 0;
            return this;
        }

        public Builder setTtcIndex(int i) {
            if (this.mFonts != null) {
                throw new IllegalArgumentException("TTC index can not be specified for FontResult source.");
            }
            this.mTtcIndex = i;
            return this;
        }

        public Builder setFontVariationSettings(String str) {
            if (this.mFonts != null) {
                throw new IllegalArgumentException("Font variation settings can not be specified for FontResult source.");
            }
            if (this.mAxes != null) {
                throw new IllegalStateException("Font variation settings are already set.");
            }
            this.mAxes = FontVariationAxis.fromFontVariationSettings(str);
            return this;
        }

        public Builder setFontVariationSettings(FontVariationAxis[] fontVariationAxisArr) {
            if (this.mFonts != null) {
                throw new IllegalArgumentException("Font variation settings can not be specified for FontResult source.");
            }
            if (this.mAxes != null) {
                throw new IllegalStateException("Font variation settings are already set.");
            }
            this.mAxes = fontVariationAxisArr;
            return this;
        }

        public Builder setFallback(String str) {
            this.mFallbackFamilyName = str;
            return this;
        }

        private static String createAssetUid(AssetManager assetManager, String str, int i, FontVariationAxis[] fontVariationAxisArr, int i2, int i3, String str2) {
            SparseArray<String> assignedPackageIdentifiers = assetManager.getAssignedPackageIdentifiers();
            StringBuilder sb = new StringBuilder();
            int size = assignedPackageIdentifiers.size();
            for (int i4 = 0; i4 < size; i4++) {
                sb.append(assignedPackageIdentifiers.valueAt(i4));
                sb.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            }
            sb.append(str);
            sb.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            sb.append(Integer.toString(i));
            sb.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            sb.append(Integer.toString(i2));
            sb.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
            sb.append(Integer.toString(i3));
            sb.append("--");
            sb.append(str2);
            sb.append("--");
            if (fontVariationAxisArr != null) {
                for (FontVariationAxis fontVariationAxis : fontVariationAxisArr) {
                    sb.append(fontVariationAxis.getTag());
                    sb.append(NativeLibraryHelper.CLEAR_ABI_OVERRIDE);
                    sb.append(Float.toString(fontVariationAxis.getStyleValue()));
                }
            }
            return sb.toString();
        }

        private Typeface resolveFallbackTypeface() {
            if (this.mFallbackFamilyName == null) {
                return null;
            }
            Typeface typeface = Typeface.sSystemFontMap.get(this.mFallbackFamilyName);
            if (typeface == null) {
                typeface = Typeface.sDefaultTypeface;
            }
            if (this.mWeight == -1 && this.mItalic == -1) {
                return typeface;
            }
            int i = this.mWeight == -1 ? typeface.mWeight : this.mWeight;
            boolean z = false;
            if (this.mItalic != -1 ? this.mItalic == 1 : (typeface.mStyle & 2) != 0) {
                z = true;
            }
            return Typeface.createWeightStyle(typeface, i, z);
        }

        public Typeface build() {
            Throwable th = null;
            if (this.mFd != null) {
                try {
                    FileInputStream fileInputStream = new FileInputStream(this.mFd);
                    try {
                        FileChannel channel = fileInputStream.getChannel();
                        MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
                        FontFamily fontFamily = new FontFamily();
                        if (!fontFamily.addFontFromBuffer(map, this.mTtcIndex, this.mAxes, this.mWeight, this.mItalic)) {
                            fontFamily.abortCreation();
                            Typeface typefaceResolveFallbackTypeface = resolveFallbackTypeface();
                            fileInputStream.close();
                            return typefaceResolveFallbackTypeface;
                        }
                        if (fontFamily.freeze()) {
                            Typeface typefaceCreateFromFamiliesWithDefault = Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily}, this.mFallbackFamilyName, this.mWeight, this.mItalic);
                            fileInputStream.close();
                            return typefaceCreateFromFamiliesWithDefault;
                        }
                        Typeface typefaceResolveFallbackTypeface2 = resolveFallbackTypeface();
                        fileInputStream.close();
                        return typefaceResolveFallbackTypeface2;
                    } catch (Throwable th2) {
                        if (0 == 0) {
                            fileInputStream.close();
                            throw th2;
                        }
                        try {
                            fileInputStream.close();
                            throw th2;
                        } catch (Throwable th3) {
                            th.addSuppressed(th3);
                            throw th2;
                        }
                    }
                } catch (IOException e) {
                    return resolveFallbackTypeface();
                }
            }
            if (this.mAssetManager != null) {
                String strCreateAssetUid = createAssetUid(this.mAssetManager, this.mPath, this.mTtcIndex, this.mAxes, this.mWeight, this.mItalic, this.mFallbackFamilyName);
                synchronized (Typeface.sDynamicCacheLock) {
                    Typeface typeface = (Typeface) Typeface.sDynamicTypefaceCache.get(strCreateAssetUid);
                    if (typeface != null) {
                        return typeface;
                    }
                    FontFamily fontFamily2 = new FontFamily();
                    if (!fontFamily2.addFontFromAssetManager(this.mAssetManager, this.mPath, this.mTtcIndex, true, this.mTtcIndex, this.mWeight, this.mItalic, this.mAxes)) {
                        fontFamily2.abortCreation();
                        return resolveFallbackTypeface();
                    }
                    if (!fontFamily2.freeze()) {
                        return resolveFallbackTypeface();
                    }
                    Typeface typefaceCreateFromFamiliesWithDefault2 = Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily2}, this.mFallbackFamilyName, this.mWeight, this.mItalic);
                    Typeface.sDynamicTypefaceCache.put(strCreateAssetUid, typefaceCreateFromFamiliesWithDefault2);
                    return typefaceCreateFromFamiliesWithDefault2;
                }
            }
            if (this.mPath != null) {
                FontFamily fontFamily3 = new FontFamily();
                if (fontFamily3.addFont(this.mPath, this.mTtcIndex, this.mAxes, this.mWeight, this.mItalic)) {
                    return !fontFamily3.freeze() ? resolveFallbackTypeface() : Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily3}, this.mFallbackFamilyName, this.mWeight, this.mItalic);
                }
                fontFamily3.abortCreation();
                return resolveFallbackTypeface();
            }
            if (this.mFonts == null) {
                throw new IllegalArgumentException("No source was set.");
            }
            FontFamily fontFamily4 = new FontFamily();
            FontsContract.FontInfo[] fontInfoArr = this.mFonts;
            int length = fontInfoArr.length;
            boolean z = false;
            int i = 0;
            while (i < length) {
                FontsContract.FontInfo fontInfo = fontInfoArr[i];
                ByteBuffer byteBuffer = this.mFontBuffers.get(fontInfo.getUri());
                if (byteBuffer != null) {
                    if (!fontFamily4.addFontFromBuffer(byteBuffer, fontInfo.getTtcIndex(), fontInfo.getAxes(), fontInfo.getWeight(), fontInfo.isItalic() ? 1 : 0)) {
                        fontFamily4.abortCreation();
                        return null;
                    }
                    z = true;
                }
                i++;
                z = z;
            }
            if (z) {
                fontFamily4.freeze();
                return Typeface.createFromFamiliesWithDefault(new FontFamily[]{fontFamily4}, this.mFallbackFamilyName, this.mWeight, this.mItalic);
            }
            fontFamily4.abortCreation();
            return null;
        }
    }

    public static Typeface create(String str, int i) {
        return create(sSystemFontMap.get(str), i);
    }

    public static Typeface create(Typeface typeface, int i) {
        if ((i & (-4)) != 0) {
            i = 0;
        }
        if (typeface == null) {
            typeface = sDefaultTypeface;
        }
        if (typeface.mStyle == i) {
            return typeface;
        }
        long j = typeface.native_instance;
        synchronized (sStyledCacheLock) {
            SparseArray<Typeface> sparseArray = sStyledTypefaceCache.get(j);
            if (sparseArray == null) {
                sparseArray = new SparseArray<>(4);
                sStyledTypefaceCache.put(j, sparseArray);
            } else {
                Typeface typeface2 = sparseArray.get(i);
                if (typeface2 != null) {
                    return typeface2;
                }
            }
            Typeface typeface3 = new Typeface(nativeCreateFromTypeface(j, i));
            sparseArray.put(i, typeface3);
            return typeface3;
        }
    }

    public static Typeface create(Typeface typeface, int i, boolean z) {
        Preconditions.checkArgumentInRange(i, 0, 1000, TvContract.PreviewPrograms.COLUMN_WEIGHT);
        if (typeface == null) {
            typeface = sDefaultTypeface;
        }
        return createWeightStyle(typeface, i, z);
    }

    private static Typeface createWeightStyle(Typeface typeface, int i, boolean z) {
        int i2 = (i << 1) | (z ? 1 : 0);
        synchronized (sWeightCacheLock) {
            SparseArray<Typeface> sparseArray = sWeightTypefaceCache.get(typeface.native_instance);
            if (sparseArray == null) {
                sparseArray = new SparseArray<>(4);
                sWeightTypefaceCache.put(typeface.native_instance, sparseArray);
            } else {
                Typeface typeface2 = sparseArray.get(i2);
                if (typeface2 != null) {
                    return typeface2;
                }
            }
            Typeface typeface3 = new Typeface(nativeCreateFromTypefaceWithExactStyle(typeface.native_instance, i, z));
            sparseArray.put(i2, typeface3);
            return typeface3;
        }
    }

    public static Typeface createFromTypefaceWithVariation(Typeface typeface, List<FontVariationAxis> list) {
        return new Typeface(nativeCreateFromTypefaceWithVariation(typeface == null ? 0L : typeface.native_instance, list));
    }

    public static Typeface defaultFromStyle(int i) {
        return sDefaults[i];
    }

    public static Typeface createFromAsset(AssetManager assetManager, String str) {
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(assetManager);
        Typeface typefaceBuild = new Builder(assetManager, str).build();
        if (typefaceBuild != null) {
            return typefaceBuild;
        }
        try {
            InputStream inputStreamOpen = assetManager.open(str);
            if (inputStreamOpen != null) {
                inputStreamOpen.close();
            }
            return DEFAULT;
        } catch (IOException e) {
            throw new RuntimeException("Font asset not found " + str);
        }
    }

    private static String createProviderUid(String str, String str2) {
        return "provider:" + str + NativeLibraryHelper.CLEAR_ABI_OVERRIDE + str2;
    }

    public static Typeface createFromFile(File file) {
        Typeface typefaceBuild = new Builder(file).build();
        if (typefaceBuild != null) {
            return typefaceBuild;
        }
        if (!file.exists()) {
            throw new RuntimeException("Font asset not found " + file.getAbsolutePath());
        }
        return DEFAULT;
    }

    public static Typeface createFromFile(String str) {
        Preconditions.checkNotNull(str);
        return createFromFile(new File(str));
    }

    private static Typeface createFromFamilies(FontFamily[] fontFamilyArr) {
        long[] jArr = new long[fontFamilyArr.length];
        for (int i = 0; i < fontFamilyArr.length; i++) {
            jArr[i] = fontFamilyArr[i].mNativePtr;
        }
        return new Typeface(nativeCreateFromArray(jArr, -1, -1));
    }

    private static Typeface createFromFamiliesWithDefault(FontFamily[] fontFamilyArr, int i, int i2) {
        return createFromFamiliesWithDefault(fontFamilyArr, DEFAULT_FAMILY, i, i2);
    }

    private static Typeface createFromFamiliesWithDefault(FontFamily[] fontFamilyArr, String str, int i, int i2) {
        FontFamily[] fontFamilyArr2 = sSystemFallbackMap.get(str);
        if (fontFamilyArr2 == null) {
            fontFamilyArr2 = sSystemFallbackMap.get(DEFAULT_FAMILY);
        }
        long[] jArr = new long[fontFamilyArr.length + fontFamilyArr2.length];
        for (int i3 = 0; i3 < fontFamilyArr.length; i3++) {
            jArr[i3] = fontFamilyArr[i3].mNativePtr;
        }
        for (int i4 = 0; i4 < fontFamilyArr2.length; i4++) {
            jArr[fontFamilyArr.length + i4] = fontFamilyArr2[i4].mNativePtr;
        }
        return new Typeface(nativeCreateFromArray(jArr, i, i2));
    }

    private Typeface(long j) {
        this.mStyle = 0;
        this.mWeight = 0;
        if (j == 0) {
            throw new RuntimeException("native typeface cannot be made");
        }
        this.native_instance = j;
        sRegistry.registerNativeAllocation(this, this.native_instance);
        this.mStyle = nativeGetStyle(j);
        this.mWeight = nativeGetWeight(j);
    }

    private static ByteBuffer mmap(String str) throws Throwable {
        Throwable th;
        try {
            FileInputStream fileInputStream = new FileInputStream(str);
            try {
                FileChannel channel = fileInputStream.getChannel();
                MappedByteBuffer map = channel.map(FileChannel.MapMode.READ_ONLY, 0L, channel.size());
                fileInputStream.close();
                return map;
            } catch (Throwable th2) {
                th = th2;
                th = null;
                if (th != null) {
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error mapping font file " + str);
            return null;
        }
    }

    private static FontFamily createFontFamily(String str, List<FontConfig.Font> list, String[] strArr, int i, Map<String, ByteBuffer> map, String str2) throws Throwable {
        FontFamily fontFamily = new FontFamily(strArr, i);
        for (int i2 = 0; i2 < list.size(); i2++) {
            FontConfig.Font font = list.get(i2);
            String str3 = str2 + font.getFontName();
            ByteBuffer byteBufferMmap = map.get(str3);
            if (byteBufferMmap == null) {
                if (!map.containsKey(str3)) {
                    byteBufferMmap = mmap(str3);
                    map.put(str3, byteBufferMmap);
                    if (byteBufferMmap == null) {
                    }
                }
            } else if (!fontFamily.addFontFromBuffer(byteBufferMmap, font.getTtcIndex(), font.getAxes(), font.getWeight(), font.isItalic() ? 1 : 0)) {
                Log.e(TAG, "Error creating font " + str3 + "#" + font.getTtcIndex());
            }
        }
        if (!fontFamily.freeze()) {
            Log.e(TAG, "Unable to load Family: " + str + " : " + Arrays.toString(strArr));
            return null;
        }
        return fontFamily;
    }

    private static void pushFamilyToFallback(FontConfig.Family family, ArrayMap<String, ArrayList<FontFamily>> arrayMap, Map<String, ByteBuffer> map, String str) throws Throwable {
        String[] languages = family.getLanguages();
        int variant = family.getVariant();
        ArrayList arrayList = new ArrayList();
        ArrayMap arrayMap2 = new ArrayMap();
        for (FontConfig.Font font : family.getFonts()) {
            String fallbackFor = font.getFallbackFor();
            if (fallbackFor == null) {
                arrayList.add(font);
            } else {
                ArrayList arrayList2 = (ArrayList) arrayMap2.get(fallbackFor);
                if (arrayList2 == null) {
                    arrayList2 = new ArrayList();
                    arrayMap2.put(fallbackFor, arrayList2);
                }
                arrayList2.add(font);
            }
        }
        FontFamily fontFamilyCreateFontFamily = arrayList.isEmpty() ? null : createFontFamily(family.getName(), arrayList, languages, variant, map, str);
        for (int i = 0; i < arrayMap.size(); i++) {
            ArrayList arrayList3 = (ArrayList) arrayMap2.get(arrayMap.keyAt(i));
            if (arrayList3 == null) {
                if (fontFamilyCreateFontFamily != null) {
                    arrayMap.valueAt(i).add(fontFamilyCreateFontFamily);
                }
            } else {
                FontFamily fontFamilyCreateFontFamily2 = createFontFamily(family.getName(), arrayList3, languages, variant, map, str);
                if (fontFamilyCreateFontFamily2 != null) {
                    arrayMap.valueAt(i).add(fontFamilyCreateFontFamily2);
                } else if (fontFamilyCreateFontFamily != null) {
                    arrayMap.valueAt(i).add(fontFamilyCreateFontFamily);
                }
            }
        }
    }

    @VisibleForTesting
    public static void buildSystemFallback(String str, String str2, ArrayMap<String, Typeface> arrayMap, ArrayMap<String, FontFamily[]> arrayMap2) throws Throwable {
        FontFamily fontFamilyCreateFontFamily;
        try {
            FontConfig fontConfig = FontListParser.parse(new FileInputStream(str));
            HashMap map = new HashMap();
            FontConfig.Family[] families = fontConfig.getFamilies();
            ArrayMap arrayMap3 = new ArrayMap();
            for (FontConfig.Family family : families) {
                String name = family.getName();
                if (name != null && (fontFamilyCreateFontFamily = createFontFamily(family.getName(), Arrays.asList(family.getFonts()), family.getLanguages(), family.getVariant(), map, str2)) != null) {
                    ArrayList arrayList = new ArrayList();
                    arrayList.add(fontFamilyCreateFontFamily);
                    arrayMap3.put(name, arrayList);
                }
            }
            for (int i = 0; i < families.length; i++) {
                FontConfig.Family family2 = families[i];
                if (i == 0 || family2.getName() == null) {
                    pushFamilyToFallback(family2, arrayMap3, map, str2);
                }
            }
            for (int i2 = 0; i2 < arrayMap3.size(); i2++) {
                String str3 = (String) arrayMap3.keyAt(i2);
                List list = (List) arrayMap3.valueAt(i2);
                FontFamily[] fontFamilyArr = (FontFamily[]) list.toArray(new FontFamily[list.size()]);
                arrayMap2.put(str3, fontFamilyArr);
                long[] jArr = new long[fontFamilyArr.length];
                for (int i3 = 0; i3 < fontFamilyArr.length; i3++) {
                    jArr[i3] = fontFamilyArr[i3].mNativePtr;
                }
                arrayMap.put(str3, new Typeface(nativeCreateFromArray(jArr, -1, -1)));
            }
            for (FontConfig.Alias alias : fontConfig.getAliases()) {
                Typeface typeface = arrayMap.get(alias.getToName());
                int weight = alias.getWeight();
                if (weight != 400) {
                    typeface = new Typeface(nativeCreateWeightAlias(typeface.native_instance, weight));
                }
                arrayMap.put(alias.getName(), typeface);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error opening " + str, e);
        } catch (IOException e2) {
            Log.e(TAG, "Error reading " + str, e2);
        } catch (RuntimeException e3) {
            Log.w(TAG, "Didn't create default family (most likely, non-Minikin build)", e3);
        } catch (XmlPullParserException e4) {
            Log.e(TAG, "XML parse exception for " + str, e4);
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Typeface typeface = (Typeface) obj;
        if (this.mStyle == typeface.mStyle && this.native_instance == typeface.native_instance) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        return (31 * (MetricsProto.MetricsEvent.DIALOG_SUPPORT_PHONE + ((int) (this.native_instance ^ (this.native_instance >>> 32))))) + this.mStyle;
    }

    public boolean isSupportedAxes(int i) {
        if (this.mSupportedAxes == null) {
            synchronized (this) {
                if (this.mSupportedAxes == null) {
                    this.mSupportedAxes = nativeGetSupportedAxes(this.native_instance);
                    if (this.mSupportedAxes == null) {
                        this.mSupportedAxes = EMPTY_AXES;
                    }
                }
            }
        }
        return Arrays.binarySearch(this.mSupportedAxes, i) >= 0;
    }
}
