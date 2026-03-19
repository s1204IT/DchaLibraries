package com.android.internal.colorextraction.types;

import android.app.WallpaperColors;
import android.app.slice.Slice;
import android.content.Context;
import android.content.res.XmlResourceParser;
import android.graphics.Color;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.Log;
import android.util.MathUtils;
import android.util.Range;
import com.android.internal.R;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.colorextraction.ColorExtractor;
import com.android.internal.graphics.ColorUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class Tonal implements ExtractionType {
    private static final boolean DEBUG = true;
    private static final float FIT_WEIGHT_H = 1.0f;
    private static final float FIT_WEIGHT_L = 10.0f;
    private static final float FIT_WEIGHT_S = 1.0f;
    public static final int MAIN_COLOR_DARK = -16777216;
    public static final int MAIN_COLOR_LIGHT = -2039584;
    private static final String TAG = "Tonal";
    public static final int THRESHOLD_COLOR_DARK = -14606047;
    public static final int THRESHOLD_COLOR_LIGHT = -2039584;
    private final ArrayList<ColorRange> mBlacklistedColors;
    private final TonalPalette mGreyPalette;
    private float[] mTmpHSL = new float[3];
    private final ArrayList<TonalPalette> mTonalPalettes;

    public Tonal(Context context) {
        ConfigParser configParser = new ConfigParser(context);
        this.mTonalPalettes = configParser.getTonalPalettes();
        this.mBlacklistedColors = configParser.getBlacklistedColors();
        this.mGreyPalette = this.mTonalPalettes.get(0);
        this.mTonalPalettes.remove(0);
    }

    @Override
    public void extractInto(WallpaperColors wallpaperColors, ColorExtractor.GradientColors gradientColors, ColorExtractor.GradientColors gradientColors2, ColorExtractor.GradientColors gradientColors3) {
        if (!runTonalExtraction(wallpaperColors, gradientColors, gradientColors2, gradientColors3)) {
            applyFallback(wallpaperColors, gradientColors, gradientColors2, gradientColors3);
        }
    }

    private boolean runTonalExtraction(WallpaperColors wallpaperColors, ColorExtractor.GradientColors gradientColors, ColorExtractor.GradientColors gradientColors2, ColorExtractor.GradientColors gradientColors3) {
        Color color;
        int iMin;
        int length = 0;
        if (wallpaperColors == null) {
            return false;
        }
        List<Color> mainColors = wallpaperColors.getMainColors();
        int size = mainColors.size();
        int colorHints = wallpaperColors.getColorHints();
        boolean z = (colorHints & 1) != 0;
        boolean z2 = (colorHints & 4) != 0;
        if (size == 0) {
            return false;
        }
        float[] fArr = new float[3];
        int i = 0;
        while (true) {
            if (i < size) {
                color = mainColors.get(i);
                int argb = color.toArgb();
                ColorUtils.RGBToHSL(Color.red(argb), Color.green(argb), Color.blue(argb), fArr);
                if (!z2 || !isBlacklisted(fArr)) {
                    break;
                }
                i++;
            } else {
                color = null;
                break;
            }
        }
        if (color == null) {
            return false;
        }
        int argb2 = color.toArgb();
        ColorUtils.RGBToHSL(Color.red(argb2), Color.green(argb2), Color.blue(argb2), fArr);
        fArr[0] = fArr[0] / 360.0f;
        TonalPalette tonalPaletteFindTonalPalette = findTonalPalette(fArr[0], fArr[1]);
        if (tonalPaletteFindTonalPalette == null) {
            Log.w(TAG, "Could not find a tonal palette!");
            return false;
        }
        int iBestFit = bestFit(tonalPaletteFindTonalPalette, fArr[0], fArr[1], fArr[2]);
        if (iBestFit == -1) {
            Log.w(TAG, "Could not find best fit!");
            return false;
        }
        float[] fArrFit = fit(tonalPaletteFindTonalPalette.h, fArr[0], iBestFit, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
        float[] fArrFit2 = fit(tonalPaletteFindTonalPalette.s, fArr[1], iBestFit, 0.0f, 1.0f);
        float[] fArrFit3 = fit(tonalPaletteFindTonalPalette.l, fArr[2], iBestFit, 0.0f, 1.0f);
        StringBuilder sb = new StringBuilder("Tonal Palette - index: " + iBestFit + ". Main color: " + Integer.toHexString(getColorInt(iBestFit, fArrFit, fArrFit2, fArrFit3)) + "\nColors: ");
        for (int i2 = 0; i2 < fArrFit.length; i2++) {
            sb.append(Integer.toHexString(getColorInt(i2, fArrFit, fArrFit2, fArrFit3)));
            if (i2 < fArrFit.length - 1) {
                sb.append(", ");
            }
        }
        Log.d(TAG, sb.toString());
        int colorInt = getColorInt(iBestFit, fArrFit, fArrFit2, fArrFit3);
        ColorUtils.colorToHSL(colorInt, this.mTmpHSL);
        float f = this.mTmpHSL[2];
        ColorUtils.colorToHSL(-2039584, this.mTmpHSL);
        if (f > this.mTmpHSL[2]) {
            return false;
        }
        ColorUtils.colorToHSL(THRESHOLD_COLOR_DARK, this.mTmpHSL);
        if (f < this.mTmpHSL[2]) {
            return false;
        }
        gradientColors.setMainColor(colorInt);
        gradientColors.setSecondaryColor(colorInt);
        if (z) {
            iMin = fArrFit.length - 1;
        } else if (iBestFit >= 2) {
            iMin = Math.min(iBestFit, 3);
        } else {
            iMin = 0;
        }
        int colorInt2 = getColorInt(iMin, fArrFit, fArrFit2, fArrFit3);
        gradientColors2.setMainColor(colorInt2);
        gradientColors2.setSecondaryColor(colorInt2);
        if (z) {
            length = fArrFit.length - 1;
        } else if (iBestFit >= 2) {
            length = 2;
        }
        int colorInt3 = getColorInt(length, fArrFit, fArrFit2, fArrFit3);
        gradientColors3.setMainColor(colorInt3);
        gradientColors3.setSecondaryColor(colorInt3);
        gradientColors.setSupportsDarkText(z);
        gradientColors2.setSupportsDarkText(z);
        gradientColors3.setSupportsDarkText(z);
        Log.d(TAG, "Gradients: \n\tNormal " + gradientColors + "\n\tDark " + gradientColors2 + "\n\tExtra dark: " + gradientColors3);
        return true;
    }

    private void applyFallback(WallpaperColors wallpaperColors, ColorExtractor.GradientColors gradientColors, ColorExtractor.GradientColors gradientColors2, ColorExtractor.GradientColors gradientColors3) {
        applyFallback(wallpaperColors, gradientColors);
        applyFallback(wallpaperColors, gradientColors2);
        applyFallback(wallpaperColors, gradientColors3);
    }

    public static void applyFallback(WallpaperColors wallpaperColors, ColorExtractor.GradientColors gradientColors) {
        boolean z = (wallpaperColors == null || (wallpaperColors.getColorHints() & 1) == 0) ? false : true;
        int i = z ? -2039584 : -16777216;
        gradientColors.setMainColor(i);
        gradientColors.setSecondaryColor(i);
        gradientColors.setSupportsDarkText(z);
    }

    private int getColorInt(int i, float[] fArr, float[] fArr2, float[] fArr3) {
        this.mTmpHSL[0] = fract(fArr[i]) * 360.0f;
        this.mTmpHSL[1] = fArr2[i];
        this.mTmpHSL[2] = fArr3[i];
        return ColorUtils.HSLToColor(this.mTmpHSL);
    }

    private boolean isBlacklisted(float[] fArr) {
        for (int size = this.mBlacklistedColors.size() - 1; size >= 0; size--) {
            if (this.mBlacklistedColors.get(size).containsColor(fArr[0], fArr[1], fArr[2])) {
                return true;
            }
        }
        return false;
    }

    private static float[] fit(float[] fArr, float f, int i, float f2, float f3) {
        float[] fArr2 = new float[fArr.length];
        float f4 = f - fArr[i];
        for (int i2 = 0; i2 < fArr.length; i2++) {
            fArr2[i2] = MathUtils.constrain(fArr[i2] + f4, f2, f3);
        }
        return fArr2;
    }

    private static int bestFit(TonalPalette tonalPalette, float f, float f2, float f3) {
        int i = -1;
        float f4 = Float.POSITIVE_INFINITY;
        for (int i2 = 0; i2 < tonalPalette.h.length; i2++) {
            float fAbs = (Math.abs(f - tonalPalette.h[i2]) * 1.0f) + (1.0f * Math.abs(f2 - tonalPalette.s[i2])) + (FIT_WEIGHT_L * Math.abs(f3 - tonalPalette.l[i2]));
            if (fAbs < f4) {
                i = i2;
                f4 = fAbs;
            }
        }
        return i;
    }

    @VisibleForTesting
    public List<ColorRange> getBlacklistedColors() {
        return this.mBlacklistedColors;
    }

    private TonalPalette findTonalPalette(float f, float f2) {
        float fFract;
        if (f2 < 0.05f) {
            return this.mGreyPalette;
        }
        TonalPalette tonalPalette = null;
        float f3 = Float.POSITIVE_INFINITY;
        int size = this.mTonalPalettes.size();
        for (int i = 0; i < size; i++) {
            TonalPalette tonalPalette2 = this.mTonalPalettes.get(i);
            if ((f < tonalPalette2.minHue || f > tonalPalette2.maxHue) && ((tonalPalette2.maxHue <= 1.0f || f < 0.0f || f > fract(tonalPalette2.maxHue)) && (tonalPalette2.minHue >= 0.0f || f < fract(tonalPalette2.minHue) || f > 1.0f))) {
                if (f <= tonalPalette2.minHue && tonalPalette2.minHue - f < f3) {
                    fFract = tonalPalette2.minHue - f;
                } else if (f >= tonalPalette2.maxHue && f - tonalPalette2.maxHue < f3) {
                    fFract = f - tonalPalette2.maxHue;
                } else if (tonalPalette2.maxHue > 1.0f && f >= fract(tonalPalette2.maxHue) && f - fract(tonalPalette2.maxHue) < f3) {
                    fFract = f - fract(tonalPalette2.maxHue);
                } else if (tonalPalette2.minHue < 0.0f && f <= fract(tonalPalette2.minHue) && fract(tonalPalette2.minHue) - f < f3) {
                    fFract = fract(tonalPalette2.minHue) - f;
                }
                f3 = fFract;
                tonalPalette = tonalPalette2;
            } else {
                return tonalPalette2;
            }
        }
        return tonalPalette;
    }

    private static float fract(float f) {
        return f - ((float) Math.floor(f));
    }

    @VisibleForTesting
    public static class TonalPalette {
        public final float[] h;
        public final float[] l;
        public final float maxHue;
        public final float minHue;
        public final float[] s;

        TonalPalette(float[] fArr, float[] fArr2, float[] fArr3) {
            if (fArr.length != fArr2.length || fArr2.length != fArr3.length) {
                throw new IllegalArgumentException("All arrays should have the same size. h: " + Arrays.toString(fArr) + " s: " + Arrays.toString(fArr2) + " l: " + Arrays.toString(fArr3));
            }
            this.h = fArr;
            this.s = fArr2;
            this.l = fArr3;
            float fMin = Float.POSITIVE_INFINITY;
            float fMax = Float.NEGATIVE_INFINITY;
            for (float f : fArr) {
                fMin = Math.min(f, fMin);
                fMax = Math.max(f, fMax);
            }
            this.minHue = fMin;
            this.maxHue = fMax;
        }
    }

    @VisibleForTesting
    public static class ColorRange {
        private Range<Float> mHue;
        private Range<Float> mLightness;
        private Range<Float> mSaturation;

        public ColorRange(Range<Float> range, Range<Float> range2, Range<Float> range3) {
            this.mHue = range;
            this.mSaturation = range2;
            this.mLightness = range3;
        }

        public boolean containsColor(float f, float f2, float f3) {
            return this.mHue.contains(Float.valueOf(f)) && this.mSaturation.contains(Float.valueOf(f2)) && this.mLightness.contains(Float.valueOf(f3));
        }

        public float[] getCenter() {
            return new float[]{((Float) this.mHue.getLower()).floatValue() + ((((Float) this.mHue.getUpper()).floatValue() - ((Float) this.mHue.getLower()).floatValue()) / 2.0f), ((Float) this.mSaturation.getLower()).floatValue() + ((((Float) this.mSaturation.getUpper()).floatValue() - ((Float) this.mSaturation.getLower()).floatValue()) / 2.0f), ((Float) this.mLightness.getLower()).floatValue() + ((((Float) this.mLightness.getUpper()).floatValue() - ((Float) this.mLightness.getLower()).floatValue()) / 2.0f)};
        }

        public String toString() {
            return String.format("H: %s, S: %s, L %s", this.mHue, this.mSaturation, this.mLightness);
        }
    }

    @VisibleForTesting
    public static class ConfigParser {
        private final ArrayList<TonalPalette> mTonalPalettes = new ArrayList<>();
        private final ArrayList<ColorRange> mBlacklistedColors = new ArrayList<>();

        public ConfigParser(Context context) {
            try {
                XmlResourceParser xml = context.getResources().getXml(R.xml.color_extraction);
                for (int eventType = xml.getEventType(); eventType != 1; eventType = xml.next()) {
                    if (eventType != 0 && eventType != 3) {
                        if (eventType == 2) {
                            String name = xml.getName();
                            if (name.equals("palettes")) {
                                parsePalettes(xml);
                            } else if (name.equals("blacklist")) {
                                parseBlacklist(xml);
                            }
                        } else {
                            throw new XmlPullParserException("Invalid XML event " + eventType + " - " + xml.getName(), xml, null);
                        }
                    }
                }
            } catch (IOException | XmlPullParserException e) {
                throw new RuntimeException(e);
            }
        }

        public ArrayList<TonalPalette> getTonalPalettes() {
            return this.mTonalPalettes;
        }

        public ArrayList<ColorRange> getBlacklistedColors() {
            return this.mBlacklistedColors;
        }

        private void parseBlacklist(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            xmlPullParser.require(2, null, "blacklist");
            while (xmlPullParser.next() != 3) {
                if (xmlPullParser.getEventType() == 2) {
                    String name = xmlPullParser.getName();
                    if (name.equals(Slice.SUBTYPE_RANGE)) {
                        this.mBlacklistedColors.add(readRange(xmlPullParser));
                        xmlPullParser.next();
                    } else {
                        throw new XmlPullParserException("Invalid tag: " + name, xmlPullParser, null);
                    }
                }
            }
        }

        private ColorRange readRange(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            xmlPullParser.require(2, null, Slice.SUBTYPE_RANGE);
            float[] floatArray = readFloatArray(xmlPullParser.getAttributeValue(null, "h"));
            float[] floatArray2 = readFloatArray(xmlPullParser.getAttributeValue(null, "s"));
            float[] floatArray3 = readFloatArray(xmlPullParser.getAttributeValue(null, "l"));
            if (floatArray == null || floatArray2 == null || floatArray3 == null) {
                throw new XmlPullParserException("Incomplete range tag.", xmlPullParser, null);
            }
            return new ColorRange(new Range(Float.valueOf(floatArray[0]), Float.valueOf(floatArray[1])), new Range(Float.valueOf(floatArray2[0]), Float.valueOf(floatArray2[1])), new Range(Float.valueOf(floatArray3[0]), Float.valueOf(floatArray3[1])));
        }

        private void parsePalettes(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            xmlPullParser.require(2, null, "palettes");
            while (xmlPullParser.next() != 3) {
                if (xmlPullParser.getEventType() == 2) {
                    String name = xmlPullParser.getName();
                    if (name.equals("palette")) {
                        this.mTonalPalettes.add(readPalette(xmlPullParser));
                        xmlPullParser.next();
                    } else {
                        throw new XmlPullParserException("Invalid tag: " + name);
                    }
                }
            }
        }

        private TonalPalette readPalette(XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
            xmlPullParser.require(2, null, "palette");
            float[] floatArray = readFloatArray(xmlPullParser.getAttributeValue(null, "h"));
            float[] floatArray2 = readFloatArray(xmlPullParser.getAttributeValue(null, "s"));
            float[] floatArray3 = readFloatArray(xmlPullParser.getAttributeValue(null, "l"));
            if (floatArray == null || floatArray2 == null || floatArray3 == null) {
                throw new XmlPullParserException("Incomplete range tag.", xmlPullParser, null);
            }
            return new TonalPalette(floatArray, floatArray2, floatArray3);
        }

        private float[] readFloatArray(String str) throws XmlPullParserException, IOException {
            String[] strArrSplit = str.replaceAll(WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER, "").replaceAll("\n", "").split(",");
            float[] fArr = new float[strArrSplit.length];
            for (int i = 0; i < strArrSplit.length; i++) {
                fArr[i] = Float.parseFloat(strArrSplit[i]);
            }
            return fArr;
        }
    }
}
