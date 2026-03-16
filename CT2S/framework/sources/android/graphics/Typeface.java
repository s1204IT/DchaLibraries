package android.graphics;

import android.content.res.AssetManager;
import android.graphics.FontListParser;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.xmlpull.v1.XmlPullParserException;

public class Typeface {
    public static final int BOLD = 1;
    public static final int BOLD_ITALIC = 3;
    public static final Typeface DEFAULT;
    public static final Typeface DEFAULT_BOLD;
    static final String FONTS_CONFIG = "fonts.xml";
    public static final int ITALIC = 2;
    public static final Typeface MONOSPACE;
    public static final int NORMAL = 0;
    public static final Typeface SANS_SERIF;
    public static final Typeface SERIF;
    static Typeface sDefaultTypeface;
    static Typeface[] sDefaults;
    static FontFamily[] sFallbackFonts;
    static Map<String, Typeface> sSystemFontMap;
    private int mStyle;
    public long native_instance;
    private static String TAG = "Typeface";
    private static final LongSparseArray<SparseArray<Typeface>> sTypefaceCache = new LongSparseArray<>(3);

    private static native long nativeCreateFromArray(long[] jArr);

    private static native long nativeCreateFromTypeface(long j, int i);

    private static native long nativeCreateWeightAlias(long j, int i);

    private static native int nativeGetStyle(long j);

    private static native void nativeSetDefault(long j);

    private static native void nativeUnref(long j);

    static {
        init();
        DEFAULT = create((String) null, 0);
        DEFAULT_BOLD = create((String) null, 1);
        SANS_SERIF = create("sans-serif", 0);
        SERIF = create("serif", 0);
        MONOSPACE = create("monospace", 0);
        sDefaults = new Typeface[]{DEFAULT, DEFAULT_BOLD, create((String) null, 2), create((String) null, 3)};
    }

    private static void setDefault(Typeface t) {
        sDefaultTypeface = t;
        nativeSetDefault(t.native_instance);
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

    public static Typeface create(String familyName, int style) {
        if (sSystemFontMap != null) {
            return create(sSystemFontMap.get(familyName), style);
        }
        return null;
    }

    public static Typeface create(Typeface family, int style) {
        Typeface typeface;
        if (style < 0 || style > 3) {
            style = 0;
        }
        long ni = 0;
        if (family != null) {
            if (family.mStyle == style) {
                return family;
            }
            ni = family.native_instance;
        }
        SparseArray<Typeface> styles = sTypefaceCache.get(ni);
        if (styles == null || (typeface = styles.get(style)) == null) {
            Typeface typeface2 = new Typeface(nativeCreateFromTypeface(ni, style));
            if (styles == null) {
                styles = new SparseArray<>(4);
                sTypefaceCache.put(ni, styles);
            }
            styles.put(style, typeface2);
            return typeface2;
        }
        return typeface;
    }

    public static Typeface defaultFromStyle(int style) {
        return sDefaults[style];
    }

    public static Typeface createFromAsset(AssetManager mgr, String path) {
        if (sFallbackFonts != null) {
            FontFamily fontFamily = new FontFamily();
            if (fontFamily.addFontFromAsset(mgr, path)) {
                FontFamily[] families = {fontFamily};
                return createFromFamiliesWithDefault(families);
            }
        }
        throw new RuntimeException("Font asset not found " + path);
    }

    public static Typeface createFromFile(File path) {
        return createFromFile(path.getAbsolutePath());
    }

    public static Typeface createFromFile(String path) {
        if (sFallbackFonts != null) {
            FontFamily fontFamily = new FontFamily();
            if (fontFamily.addFont(path)) {
                FontFamily[] families = {fontFamily};
                return createFromFamiliesWithDefault(families);
            }
        }
        throw new RuntimeException("Font not found " + path);
    }

    public static Typeface createFromFamilies(FontFamily[] families) {
        long[] ptrArray = new long[families.length];
        for (int i = 0; i < families.length; i++) {
            ptrArray[i] = families[i].mNativePtr;
        }
        return new Typeface(nativeCreateFromArray(ptrArray));
    }

    public static Typeface createFromFamiliesWithDefault(FontFamily[] families) {
        long[] ptrArray = new long[families.length + sFallbackFonts.length];
        for (int i = 0; i < families.length; i++) {
            ptrArray[i] = families[i].mNativePtr;
        }
        for (int i2 = 0; i2 < sFallbackFonts.length; i2++) {
            ptrArray[families.length + i2] = sFallbackFonts[i2].mNativePtr;
        }
        return new Typeface(nativeCreateFromArray(ptrArray));
    }

    private Typeface(long ni) {
        this.mStyle = 0;
        if (ni == 0) {
            throw new RuntimeException("native typeface cannot be made");
        }
        this.native_instance = ni;
        this.mStyle = nativeGetStyle(ni);
    }

    private static FontFamily makeFamilyFromParsed(FontListParser.Family family) {
        FontFamily fontFamily = new FontFamily(family.lang, family.variant);
        for (FontListParser.Font font : family.fonts) {
            fontFamily.addFontWeightStyle(font.fontName, font.weight, font.isItalic);
        }
        return fontFamily;
    }

    private static void init() {
        Typeface typeface;
        File systemFontConfigLocation = getSystemFontConfigLocation();
        File configFilename = new File(systemFontConfigLocation, FONTS_CONFIG);
        try {
            FileInputStream fontsIn = new FileInputStream(configFilename);
            FontListParser.Config fontConfig = FontListParser.parse(fontsIn);
            List<FontFamily> familyList = new ArrayList<>();
            for (int i = 0; i < fontConfig.families.size(); i++) {
                FontListParser.Family f = fontConfig.families.get(i);
                if (i == 0 || f.name == null) {
                    familyList.add(makeFamilyFromParsed(f));
                }
            }
            sFallbackFonts = (FontFamily[]) familyList.toArray(new FontFamily[familyList.size()]);
            setDefault(createFromFamilies(sFallbackFonts));
            Map<String, Typeface> systemFonts = new HashMap<>();
            for (int i2 = 0; i2 < fontConfig.families.size(); i2++) {
                FontListParser.Family f2 = fontConfig.families.get(i2);
                if (f2.name != null) {
                    if (i2 == 0) {
                        typeface = sDefaultTypeface;
                    } else {
                        FontFamily fontFamily = makeFamilyFromParsed(f2);
                        FontFamily[] families = {fontFamily};
                        typeface = createFromFamiliesWithDefault(families);
                    }
                    systemFonts.put(f2.name, typeface);
                }
            }
            for (FontListParser.Alias alias : fontConfig.aliases) {
                Typeface base = systemFonts.get(alias.toName);
                Typeface newFace = base;
                int weight = alias.weight;
                if (weight != 400) {
                    newFace = new Typeface(nativeCreateWeightAlias(base.native_instance, weight));
                }
                systemFonts.put(alias.name, newFace);
            }
            sSystemFontMap = systemFonts;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Error opening " + configFilename);
        } catch (IOException e2) {
            Log.e(TAG, "Error reading " + configFilename);
        } catch (RuntimeException e3) {
            Log.w(TAG, "Didn't create default family (most likely, non-Minikin build)", e3);
        } catch (XmlPullParserException e4) {
            Log.e(TAG, "XML parse exception for " + configFilename);
        }
    }

    private static File getSystemFontConfigLocation() {
        return new File("/system/etc/");
    }

    protected void finalize() throws Throwable {
        try {
            nativeUnref(this.native_instance);
        } finally {
            super.finalize();
        }
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Typeface typeface = (Typeface) o;
        return this.mStyle == typeface.mStyle && this.native_instance == typeface.native_instance;
    }

    public int hashCode() {
        int result = ((int) (this.native_instance ^ (this.native_instance >>> 32))) + 527;
        return (result * 31) + this.mStyle;
    }
}
