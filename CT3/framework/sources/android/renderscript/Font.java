package android.renderscript;

import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Font extends BaseObj {

    private static final int[] f28androidrenderscriptFont$StyleSwitchesValues = null;
    private static Map<String, FontFamily> sFontFamilyMap;
    private static final String[] sSansNames = {"sans-serif", "arial", "helvetica", "tahoma", "verdana"};
    private static final String[] sSerifNames = {"serif", "times", "times new roman", "palatino", "georgia", "baskerville", "goudy", "fantasy", "cursive", "ITC Stone Serif"};
    private static final String[] sMonoNames = {"monospace", "courier", "courier new", "monaco"};

    private static int[] m1920getandroidrenderscriptFont$StyleSwitchesValues() {
        if (f28androidrenderscriptFont$StyleSwitchesValues != null) {
            return f28androidrenderscriptFont$StyleSwitchesValues;
        }
        int[] iArr = new int[Style.valuesCustom().length];
        try {
            iArr[Style.BOLD.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[Style.BOLD_ITALIC.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[Style.ITALIC.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[Style.NORMAL.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f28androidrenderscriptFont$StyleSwitchesValues = iArr;
        return iArr;
    }

    static {
        initFontFamilyMap();
    }

    private static class FontFamily {
        String mBoldFileName;
        String mBoldItalicFileName;
        String mItalicFileName;
        String[] mNames;
        String mNormalFileName;

        FontFamily(FontFamily fontFamily) {
            this();
        }

        private FontFamily() {
        }
    }

    public enum Style {
        NORMAL,
        BOLD,
        ITALIC,
        BOLD_ITALIC;

        public static Style[] valuesCustom() {
            return values();
        }
    }

    private static void addFamilyToMap(FontFamily family) {
        for (int i = 0; i < family.mNames.length; i++) {
            sFontFamilyMap.put(family.mNames[i], family);
        }
    }

    private static void initFontFamilyMap() {
        FontFamily fontFamily = null;
        sFontFamilyMap = new HashMap();
        FontFamily sansFamily = new FontFamily(fontFamily);
        sansFamily.mNames = sSansNames;
        sansFamily.mNormalFileName = "Roboto-Regular.ttf";
        sansFamily.mBoldFileName = "Roboto-Bold.ttf";
        sansFamily.mItalicFileName = "Roboto-Italic.ttf";
        sansFamily.mBoldItalicFileName = "Roboto-BoldItalic.ttf";
        addFamilyToMap(sansFamily);
        FontFamily serifFamily = new FontFamily(fontFamily);
        serifFamily.mNames = sSerifNames;
        serifFamily.mNormalFileName = "NotoSerif-Regular.ttf";
        serifFamily.mBoldFileName = "NotoSerif-Bold.ttf";
        serifFamily.mItalicFileName = "NotoSerif-Italic.ttf";
        serifFamily.mBoldItalicFileName = "NotoSerif-BoldItalic.ttf";
        addFamilyToMap(serifFamily);
        FontFamily monoFamily = new FontFamily(fontFamily);
        monoFamily.mNames = sMonoNames;
        monoFamily.mNormalFileName = "DroidSansMono.ttf";
        monoFamily.mBoldFileName = "DroidSansMono.ttf";
        monoFamily.mItalicFileName = "DroidSansMono.ttf";
        monoFamily.mBoldItalicFileName = "DroidSansMono.ttf";
        addFamilyToMap(monoFamily);
    }

    static String getFontFileName(String familyName, Style style) {
        FontFamily family = sFontFamilyMap.get(familyName);
        if (family != null) {
            switch (m1920getandroidrenderscriptFont$StyleSwitchesValues()[style.ordinal()]) {
                case 1:
                    return family.mBoldFileName;
                case 2:
                    return family.mBoldItalicFileName;
                case 3:
                    return family.mItalicFileName;
                case 4:
                    return family.mNormalFileName;
                default:
                    return "DroidSans.ttf";
            }
        }
        return "DroidSans.ttf";
    }

    Font(long id, RenderScript rs) {
        super(id, rs);
        this.guard.open("destroy");
    }

    public static Font createFromFile(RenderScript rs, Resources res, String path, float pointSize) {
        rs.validate();
        int dpi = res.getDisplayMetrics().densityDpi;
        long fontId = rs.nFontCreateFromFile(path, pointSize, dpi);
        if (fontId == 0) {
            throw new RSRuntimeException("Unable to create font from file " + path);
        }
        Font rsFont = new Font(fontId, rs);
        return rsFont;
    }

    public static Font createFromFile(RenderScript rs, Resources res, File path, float pointSize) {
        return createFromFile(rs, res, path.getAbsolutePath(), pointSize);
    }

    public static Font createFromAsset(RenderScript rs, Resources res, String path, float pointSize) {
        rs.validate();
        AssetManager mgr = res.getAssets();
        int dpi = res.getDisplayMetrics().densityDpi;
        long fontId = rs.nFontCreateFromAsset(mgr, path, pointSize, dpi);
        if (fontId == 0) {
            throw new RSRuntimeException("Unable to create font from asset " + path);
        }
        Font rsFont = new Font(fontId, rs);
        return rsFont;
    }

    public static Font createFromResource(RenderScript rs, Resources res, int id, float pointSize) {
        String name = "R." + Integer.toString(id);
        rs.validate();
        try {
            ?? OpenRawResource = res.openRawResource(id);
            int dpi = res.getDisplayMetrics().densityDpi;
            if (OpenRawResource instanceof AssetManager.AssetInputStream) {
                long asset = OpenRawResource.getNativeAsset();
                long fontId = rs.nFontCreateFromAssetStream(name, pointSize, dpi, asset);
                if (fontId == 0) {
                    throw new RSRuntimeException("Unable to create font from resource " + id);
                }
                Font rsFont = new Font(fontId, rs);
                return rsFont;
            }
            throw new RSRuntimeException("Unsupported asset stream created");
        } catch (Exception e) {
            throw new RSRuntimeException("Unable to open resource " + id);
        }
    }

    public static Font create(RenderScript rs, Resources res, String familyName, Style fontStyle, float pointSize) {
        String fileName = getFontFileName(familyName, fontStyle);
        String fontPath = Environment.getRootDirectory().getAbsolutePath();
        return createFromFile(rs, res, fontPath + "/fonts/" + fileName, pointSize);
    }
}
