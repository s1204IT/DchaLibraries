package android.graphics;

import android.graphics.fonts.FontVariationAxis;
import android.os.LocaleList;
import android.text.GraphicsOperations;
import android.text.SpannableString;
import android.text.SpannedString;
import android.text.TextUtils;
import com.android.internal.annotations.GuardedBy;
import dalvik.annotation.optimization.CriticalNative;
import dalvik.annotation.optimization.FastNative;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import libcore.util.NativeAllocationRegistry;

public class Paint {
    public static final int ANTI_ALIAS_FLAG = 1;
    public static final int AUTO_HINTING_TEXT_FLAG = 2048;
    public static final int BIDI_DEFAULT_LTR = 2;
    public static final int BIDI_DEFAULT_RTL = 3;
    private static final int BIDI_FLAG_MASK = 7;
    public static final int BIDI_FORCE_LTR = 4;
    public static final int BIDI_FORCE_RTL = 5;
    public static final int BIDI_LTR = 0;
    private static final int BIDI_MAX_FLAG_VALUE = 5;
    public static final int BIDI_RTL = 1;
    public static final int CURSOR_AFTER = 0;
    public static final int CURSOR_AT = 4;
    public static final int CURSOR_AT_OR_AFTER = 1;
    public static final int CURSOR_AT_OR_BEFORE = 3;
    public static final int CURSOR_BEFORE = 2;
    private static final int CURSOR_OPT_MAX_VALUE = 4;
    public static final int DEV_KERN_TEXT_FLAG = 256;
    public static final int DIRECTION_LTR = 0;
    public static final int DIRECTION_RTL = 1;
    public static final int DITHER_FLAG = 4;
    public static final int EMBEDDED_BITMAP_TEXT_FLAG = 1024;
    public static final int FAKE_BOLD_TEXT_FLAG = 32;
    public static final int FILTER_BITMAP_FLAG = 2;
    static final int HIDDEN_DEFAULT_PAINT_FLAGS = 1280;
    public static final int HINTING_OFF = 0;
    public static final int HINTING_ON = 1;
    public static final int HYPHENEDIT_MASK_END_OF_LINE = 7;
    public static final int HYPHENEDIT_MASK_START_OF_LINE = 24;
    public static final int LCD_RENDER_TEXT_FLAG = 512;
    public static final int LINEAR_TEXT_FLAG = 64;
    private static final long NATIVE_PAINT_SIZE = 98;
    public static final int STRIKE_THRU_TEXT_FLAG = 16;
    public static final int SUBPIXEL_TEXT_FLAG = 128;
    public static final int UNDERLINE_TEXT_FLAG = 8;
    public static final int VERTICAL_TEXT_FLAG = 4096;
    public int mBidiFlags;
    private ColorFilter mColorFilter;
    private float mCompatScaling;
    private String mFontFeatureSettings;
    private String mFontVariationSettings;
    private boolean mHasCompatScaling;
    private float mInvCompatScaling;
    private LocaleList mLocales;
    private MaskFilter mMaskFilter;
    private long mNativeColorFilter;
    private long mNativePaint;
    private long mNativeShader;
    private PathEffect mPathEffect;
    private Shader mShader;
    private int mShadowLayerColor;
    private float mShadowLayerDx;
    private float mShadowLayerDy;
    private float mShadowLayerRadius;
    private Typeface mTypeface;
    private Xfermode mXfermode;
    private static final Object sCacheLock = new Object();

    @GuardedBy("sCacheLock")
    private static final HashMap<String, Integer> sMinikinLocaleListIdCache = new HashMap<>();
    static final Style[] sStyleArray = {Style.FILL, Style.STROKE, Style.FILL_AND_STROKE};
    static final Cap[] sCapArray = {Cap.BUTT, Cap.ROUND, Cap.SQUARE};
    static final Join[] sJoinArray = {Join.MITER, Join.ROUND, Join.BEVEL};
    static final Align[] sAlignArray = {Align.LEFT, Align.CENTER, Align.RIGHT};

    public static class FontMetrics {
        public float ascent;
        public float bottom;
        public float descent;
        public float leading;
        public float top;
    }

    @CriticalNative
    private static native float nAscent(long j);

    private static native int nBreakText(long j, String str, boolean z, float f, int i, float[] fArr);

    private static native int nBreakText(long j, char[] cArr, int i, int i2, float f, int i3, float[] fArr);

    @CriticalNative
    private static native float nDescent(long j);

    @CriticalNative
    private static native boolean nEqualsForTextMeasurement(long j, long j2);

    @CriticalNative
    private static native int nGetAlpha(long j);

    private static native void nGetCharArrayBounds(long j, char[] cArr, int i, int i2, int i3, Rect rect);

    @CriticalNative
    private static native int nGetColor(long j);

    @CriticalNative
    private static native boolean nGetFillPath(long j, long j2, long j3);

    @CriticalNative
    private static native int nGetFlags(long j);

    @FastNative
    private static native float nGetFontMetrics(long j, FontMetrics fontMetrics);

    @FastNative
    private static native int nGetFontMetricsInt(long j, FontMetricsInt fontMetricsInt);

    @CriticalNative
    private static native int nGetHinting(long j);

    @CriticalNative
    private static native int nGetHyphenEdit(long j);

    @CriticalNative
    private static native float nGetLetterSpacing(long j);

    private static native long nGetNativeFinalizer();

    private static native int nGetOffsetForAdvance(long j, char[] cArr, int i, int i2, int i3, int i4, boolean z, float f);

    private static native float nGetRunAdvance(long j, char[] cArr, int i, int i2, int i3, int i4, boolean z, int i5);

    @CriticalNative
    private static native float nGetStrikeThruPosition(long j);

    @CriticalNative
    private static native float nGetStrikeThruThickness(long j);

    private static native void nGetStringBounds(long j, String str, int i, int i2, int i3, Rect rect);

    @CriticalNative
    private static native int nGetStrokeCap(long j);

    @CriticalNative
    private static native int nGetStrokeJoin(long j);

    @CriticalNative
    private static native float nGetStrokeMiter(long j);

    @CriticalNative
    private static native float nGetStrokeWidth(long j);

    @CriticalNative
    private static native int nGetStyle(long j);

    private static native float nGetTextAdvances(long j, String str, int i, int i2, int i3, int i4, int i5, float[] fArr, int i6);

    private static native float nGetTextAdvances(long j, char[] cArr, int i, int i2, int i3, int i4, int i5, float[] fArr, int i6);

    @CriticalNative
    private static native int nGetTextAlign(long j);

    private static native void nGetTextPath(long j, int i, String str, int i2, int i3, float f, float f2, long j2);

    private static native void nGetTextPath(long j, int i, char[] cArr, int i2, int i3, float f, float f2, long j2);

    private native int nGetTextRunCursor(long j, String str, int i, int i2, int i3, int i4, int i5);

    private native int nGetTextRunCursor(long j, char[] cArr, int i, int i2, int i3, int i4, int i5);

    @CriticalNative
    private static native float nGetTextScaleX(long j);

    @CriticalNative
    private static native float nGetTextSize(long j);

    @CriticalNative
    private static native float nGetTextSkewX(long j);

    @CriticalNative
    private static native float nGetUnderlinePosition(long j);

    @CriticalNative
    private static native float nGetUnderlineThickness(long j);

    @CriticalNative
    private static native float nGetWordSpacing(long j);

    private static native boolean nHasGlyph(long j, int i, String str);

    @CriticalNative
    private static native boolean nHasShadowLayer(long j);

    private static native long nInit();

    private static native long nInitWithPaint(long j);

    @CriticalNative
    private static native boolean nIsElegantTextHeight(long j);

    @CriticalNative
    private static native void nReset(long j);

    @CriticalNative
    private static native void nSet(long j, long j2);

    @CriticalNative
    private static native void nSetAlpha(long j, int i);

    @CriticalNative
    private static native void nSetAntiAlias(long j, boolean z);

    @CriticalNative
    private static native void nSetColor(long j, int i);

    @CriticalNative
    private static native long nSetColorFilter(long j, long j2);

    @CriticalNative
    private static native void nSetDither(long j, boolean z);

    @CriticalNative
    private static native void nSetElegantTextHeight(long j, boolean z);

    @CriticalNative
    private static native void nSetFakeBoldText(long j, boolean z);

    @CriticalNative
    private static native void nSetFilterBitmap(long j, boolean z);

    @CriticalNative
    private static native void nSetFlags(long j, int i);

    @FastNative
    private static native void nSetFontFeatureSettings(long j, String str);

    @CriticalNative
    private static native void nSetHinting(long j, int i);

    @CriticalNative
    private static native void nSetHyphenEdit(long j, int i);

    @CriticalNative
    private static native void nSetLetterSpacing(long j, float f);

    @CriticalNative
    private static native void nSetLinearText(long j, boolean z);

    @CriticalNative
    private static native long nSetMaskFilter(long j, long j2);

    @CriticalNative
    private static native long nSetPathEffect(long j, long j2);

    @CriticalNative
    private static native long nSetShader(long j, long j2);

    @CriticalNative
    private static native void nSetShadowLayer(long j, float f, float f2, float f3, int i);

    @CriticalNative
    private static native void nSetStrikeThruText(long j, boolean z);

    @CriticalNative
    private static native void nSetStrokeCap(long j, int i);

    @CriticalNative
    private static native void nSetStrokeJoin(long j, int i);

    @CriticalNative
    private static native void nSetStrokeMiter(long j, float f);

    @CriticalNative
    private static native void nSetStrokeWidth(long j, float f);

    @CriticalNative
    private static native void nSetStyle(long j, int i);

    @CriticalNative
    private static native void nSetSubpixelText(long j, boolean z);

    @CriticalNative
    private static native void nSetTextAlign(long j, int i);

    @FastNative
    private static native int nSetTextLocales(long j, String str);

    @CriticalNative
    private static native void nSetTextLocalesByMinikinLocaleListId(long j, int i);

    @CriticalNative
    private static native void nSetTextScaleX(long j, float f);

    @CriticalNative
    private static native void nSetTextSize(long j, float f);

    @CriticalNative
    private static native void nSetTextSkewX(long j, float f);

    @CriticalNative
    private static native void nSetTypeface(long j, long j2);

    @CriticalNative
    private static native void nSetUnderlineText(long j, boolean z);

    @CriticalNative
    private static native void nSetWordSpacing(long j, float f);

    @CriticalNative
    private static native void nSetXfermode(long j, int i);

    private static class NoImagePreloadHolder {
        public static final NativeAllocationRegistry sRegistry = new NativeAllocationRegistry(Paint.class.getClassLoader(), Paint.nGetNativeFinalizer(), Paint.NATIVE_PAINT_SIZE);

        private NoImagePreloadHolder() {
        }
    }

    public enum Style {
        FILL(0),
        STROKE(1),
        FILL_AND_STROKE(2);

        final int nativeInt;

        Style(int i) {
            this.nativeInt = i;
        }
    }

    public enum Cap {
        BUTT(0),
        ROUND(1),
        SQUARE(2);

        final int nativeInt;

        Cap(int i) {
            this.nativeInt = i;
        }
    }

    public enum Join {
        MITER(0),
        ROUND(1),
        BEVEL(2);

        final int nativeInt;

        Join(int i) {
            this.nativeInt = i;
        }
    }

    public enum Align {
        LEFT(0),
        CENTER(1),
        RIGHT(2);

        final int nativeInt;

        Align(int i) {
            this.nativeInt = i;
        }
    }

    public Paint() {
        this(0);
    }

    public Paint(int i) {
        this.mBidiFlags = 2;
        this.mNativePaint = nInit();
        NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mNativePaint);
        setFlags(i | 1280);
        this.mInvCompatScaling = 1.0f;
        this.mCompatScaling = 1.0f;
        setTextLocales(LocaleList.getAdjustedDefault());
    }

    public Paint(Paint paint) {
        this.mBidiFlags = 2;
        this.mNativePaint = nInitWithPaint(paint.getNativeInstance());
        NoImagePreloadHolder.sRegistry.registerNativeAllocation(this, this.mNativePaint);
        setClassVariablesFrom(paint);
    }

    public void reset() {
        nReset(this.mNativePaint);
        setFlags(1280);
        this.mColorFilter = null;
        this.mMaskFilter = null;
        this.mPathEffect = null;
        this.mShader = null;
        this.mNativeShader = 0L;
        this.mTypeface = null;
        this.mXfermode = null;
        this.mHasCompatScaling = false;
        this.mCompatScaling = 1.0f;
        this.mInvCompatScaling = 1.0f;
        this.mBidiFlags = 2;
        setTextLocales(LocaleList.getAdjustedDefault());
        setElegantTextHeight(false);
        this.mFontFeatureSettings = null;
        this.mFontVariationSettings = null;
        this.mShadowLayerRadius = 0.0f;
        this.mShadowLayerDx = 0.0f;
        this.mShadowLayerDy = 0.0f;
        this.mShadowLayerColor = 0;
    }

    public void set(Paint paint) {
        if (this != paint) {
            nSet(this.mNativePaint, paint.mNativePaint);
            setClassVariablesFrom(paint);
        }
    }

    private void setClassVariablesFrom(Paint paint) {
        this.mColorFilter = paint.mColorFilter;
        this.mMaskFilter = paint.mMaskFilter;
        this.mPathEffect = paint.mPathEffect;
        this.mShader = paint.mShader;
        this.mNativeShader = paint.mNativeShader;
        this.mTypeface = paint.mTypeface;
        this.mXfermode = paint.mXfermode;
        this.mHasCompatScaling = paint.mHasCompatScaling;
        this.mCompatScaling = paint.mCompatScaling;
        this.mInvCompatScaling = paint.mInvCompatScaling;
        this.mBidiFlags = paint.mBidiFlags;
        this.mLocales = paint.mLocales;
        this.mFontFeatureSettings = paint.mFontFeatureSettings;
        this.mFontVariationSettings = paint.mFontVariationSettings;
        this.mShadowLayerRadius = paint.mShadowLayerRadius;
        this.mShadowLayerDx = paint.mShadowLayerDx;
        this.mShadowLayerDy = paint.mShadowLayerDy;
        this.mShadowLayerColor = paint.mShadowLayerColor;
    }

    public boolean hasEqualAttributes(Paint paint) {
        return this.mColorFilter == paint.mColorFilter && this.mMaskFilter == paint.mMaskFilter && this.mPathEffect == paint.mPathEffect && this.mShader == paint.mShader && this.mTypeface == paint.mTypeface && this.mXfermode == paint.mXfermode && this.mHasCompatScaling == paint.mHasCompatScaling && this.mCompatScaling == paint.mCompatScaling && this.mInvCompatScaling == paint.mInvCompatScaling && this.mBidiFlags == paint.mBidiFlags && this.mLocales.equals(paint.mLocales) && TextUtils.equals(this.mFontFeatureSettings, paint.mFontFeatureSettings) && TextUtils.equals(this.mFontVariationSettings, paint.mFontVariationSettings) && this.mShadowLayerRadius == paint.mShadowLayerRadius && this.mShadowLayerDx == paint.mShadowLayerDx && this.mShadowLayerDy == paint.mShadowLayerDy && this.mShadowLayerColor == paint.mShadowLayerColor && getFlags() == paint.getFlags() && getHinting() == paint.getHinting() && getStyle() == paint.getStyle() && getColor() == paint.getColor() && getStrokeWidth() == paint.getStrokeWidth() && getStrokeMiter() == paint.getStrokeMiter() && getStrokeCap() == paint.getStrokeCap() && getStrokeJoin() == paint.getStrokeJoin() && getTextAlign() == paint.getTextAlign() && isElegantTextHeight() == paint.isElegantTextHeight() && getTextSize() == paint.getTextSize() && getTextScaleX() == paint.getTextScaleX() && getTextSkewX() == paint.getTextSkewX() && getLetterSpacing() == paint.getLetterSpacing() && getWordSpacing() == paint.getWordSpacing() && getHyphenEdit() == paint.getHyphenEdit();
    }

    public void setCompatibilityScaling(float f) {
        if (f == 1.0d) {
            this.mHasCompatScaling = false;
            this.mInvCompatScaling = 1.0f;
            this.mCompatScaling = 1.0f;
        } else {
            this.mHasCompatScaling = true;
            this.mCompatScaling = f;
            this.mInvCompatScaling = 1.0f / f;
        }
    }

    public long getNativeInstance() {
        long nativeInstance;
        if (this.mShader != null) {
            nativeInstance = this.mShader.getNativeInstance();
        } else {
            nativeInstance = 0;
        }
        if (nativeInstance != this.mNativeShader) {
            this.mNativeShader = nativeInstance;
            nSetShader(this.mNativePaint, this.mNativeShader);
        }
        long nativeInstance2 = this.mColorFilter != null ? this.mColorFilter.getNativeInstance() : 0L;
        if (nativeInstance2 != this.mNativeColorFilter) {
            this.mNativeColorFilter = nativeInstance2;
            nSetColorFilter(this.mNativePaint, this.mNativeColorFilter);
        }
        return this.mNativePaint;
    }

    public int getBidiFlags() {
        return this.mBidiFlags;
    }

    public void setBidiFlags(int i) {
        int i2 = i & 7;
        if (i2 > 5) {
            throw new IllegalArgumentException("unknown bidi flag: " + i2);
        }
        this.mBidiFlags = i2;
    }

    public int getFlags() {
        return nGetFlags(this.mNativePaint);
    }

    public void setFlags(int i) {
        nSetFlags(this.mNativePaint, i);
    }

    public int getHinting() {
        return nGetHinting(this.mNativePaint);
    }

    public void setHinting(int i) {
        nSetHinting(this.mNativePaint, i);
    }

    public final boolean isAntiAlias() {
        return (getFlags() & 1) != 0;
    }

    public void setAntiAlias(boolean z) {
        nSetAntiAlias(this.mNativePaint, z);
    }

    public final boolean isDither() {
        return (getFlags() & 4) != 0;
    }

    public void setDither(boolean z) {
        nSetDither(this.mNativePaint, z);
    }

    public final boolean isLinearText() {
        return (getFlags() & 64) != 0;
    }

    public void setLinearText(boolean z) {
        nSetLinearText(this.mNativePaint, z);
    }

    public final boolean isSubpixelText() {
        return (getFlags() & 128) != 0;
    }

    public void setSubpixelText(boolean z) {
        nSetSubpixelText(this.mNativePaint, z);
    }

    public final boolean isUnderlineText() {
        return (getFlags() & 8) != 0;
    }

    public float getUnderlinePosition() {
        return nGetUnderlinePosition(this.mNativePaint);
    }

    public float getUnderlineThickness() {
        return nGetUnderlineThickness(this.mNativePaint);
    }

    public void setUnderlineText(boolean z) {
        nSetUnderlineText(this.mNativePaint, z);
    }

    public final boolean isStrikeThruText() {
        return (getFlags() & 16) != 0;
    }

    public float getStrikeThruPosition() {
        return nGetStrikeThruPosition(this.mNativePaint);
    }

    public float getStrikeThruThickness() {
        return nGetStrikeThruThickness(this.mNativePaint);
    }

    public void setStrikeThruText(boolean z) {
        nSetStrikeThruText(this.mNativePaint, z);
    }

    public final boolean isFakeBoldText() {
        return (getFlags() & 32) != 0;
    }

    public void setFakeBoldText(boolean z) {
        nSetFakeBoldText(this.mNativePaint, z);
    }

    public final boolean isFilterBitmap() {
        return (getFlags() & 2) != 0;
    }

    public void setFilterBitmap(boolean z) {
        nSetFilterBitmap(this.mNativePaint, z);
    }

    public Style getStyle() {
        return sStyleArray[nGetStyle(this.mNativePaint)];
    }

    public void setStyle(Style style) {
        nSetStyle(this.mNativePaint, style.nativeInt);
    }

    public int getColor() {
        return nGetColor(this.mNativePaint);
    }

    public void setColor(int i) {
        nSetColor(this.mNativePaint, i);
    }

    public int getAlpha() {
        return nGetAlpha(this.mNativePaint);
    }

    public void setAlpha(int i) {
        nSetAlpha(this.mNativePaint, i);
    }

    public void setARGB(int i, int i2, int i3, int i4) {
        setColor((i << 24) | (i2 << 16) | (i3 << 8) | i4);
    }

    public float getStrokeWidth() {
        return nGetStrokeWidth(this.mNativePaint);
    }

    public void setStrokeWidth(float f) {
        nSetStrokeWidth(this.mNativePaint, f);
    }

    public float getStrokeMiter() {
        return nGetStrokeMiter(this.mNativePaint);
    }

    public void setStrokeMiter(float f) {
        nSetStrokeMiter(this.mNativePaint, f);
    }

    public Cap getStrokeCap() {
        return sCapArray[nGetStrokeCap(this.mNativePaint)];
    }

    public void setStrokeCap(Cap cap) {
        nSetStrokeCap(this.mNativePaint, cap.nativeInt);
    }

    public Join getStrokeJoin() {
        return sJoinArray[nGetStrokeJoin(this.mNativePaint)];
    }

    public void setStrokeJoin(Join join) {
        nSetStrokeJoin(this.mNativePaint, join.nativeInt);
    }

    public boolean getFillPath(Path path, Path path2) {
        return nGetFillPath(this.mNativePaint, path.readOnlyNI(), path2.mutateNI());
    }

    public Shader getShader() {
        return this.mShader;
    }

    public Shader setShader(Shader shader) {
        if (this.mShader != shader) {
            this.mNativeShader = -1L;
            nSetShader(this.mNativePaint, 0L);
        }
        this.mShader = shader;
        return shader;
    }

    public ColorFilter getColorFilter() {
        return this.mColorFilter;
    }

    public ColorFilter setColorFilter(ColorFilter colorFilter) {
        if (this.mColorFilter != colorFilter) {
            this.mNativeColorFilter = -1L;
        }
        this.mColorFilter = colorFilter;
        return colorFilter;
    }

    public Xfermode getXfermode() {
        return this.mXfermode;
    }

    public Xfermode setXfermode(Xfermode xfermode) {
        int i = xfermode != null ? xfermode.porterDuffMode : Xfermode.DEFAULT;
        if (i != (this.mXfermode != null ? this.mXfermode.porterDuffMode : Xfermode.DEFAULT)) {
            nSetXfermode(this.mNativePaint, i);
        }
        this.mXfermode = xfermode;
        return xfermode;
    }

    public PathEffect getPathEffect() {
        return this.mPathEffect;
    }

    public PathEffect setPathEffect(PathEffect pathEffect) {
        long j;
        if (pathEffect != null) {
            j = pathEffect.native_instance;
        } else {
            j = 0;
        }
        nSetPathEffect(this.mNativePaint, j);
        this.mPathEffect = pathEffect;
        return pathEffect;
    }

    public MaskFilter getMaskFilter() {
        return this.mMaskFilter;
    }

    public MaskFilter setMaskFilter(MaskFilter maskFilter) {
        long j;
        if (maskFilter != null) {
            j = maskFilter.native_instance;
        } else {
            j = 0;
        }
        nSetMaskFilter(this.mNativePaint, j);
        this.mMaskFilter = maskFilter;
        return maskFilter;
    }

    public Typeface getTypeface() {
        return this.mTypeface;
    }

    public Typeface setTypeface(Typeface typeface) {
        nSetTypeface(this.mNativePaint, typeface == null ? 0L : typeface.native_instance);
        this.mTypeface = typeface;
        return typeface;
    }

    @Deprecated
    public Rasterizer getRasterizer() {
        return null;
    }

    @Deprecated
    public Rasterizer setRasterizer(Rasterizer rasterizer) {
        return rasterizer;
    }

    public void setShadowLayer(float f, float f2, float f3, int i) {
        this.mShadowLayerRadius = f;
        this.mShadowLayerDx = f2;
        this.mShadowLayerDy = f3;
        this.mShadowLayerColor = i;
        nSetShadowLayer(this.mNativePaint, f, f2, f3, i);
    }

    public void clearShadowLayer() {
        setShadowLayer(0.0f, 0.0f, 0.0f, 0);
    }

    public boolean hasShadowLayer() {
        return nHasShadowLayer(this.mNativePaint);
    }

    public Align getTextAlign() {
        return sAlignArray[nGetTextAlign(this.mNativePaint)];
    }

    public void setTextAlign(Align align) {
        nSetTextAlign(this.mNativePaint, align.nativeInt);
    }

    public Locale getTextLocale() {
        return this.mLocales.get(0);
    }

    public LocaleList getTextLocales() {
        return this.mLocales;
    }

    public void setTextLocale(Locale locale) {
        if (locale == null) {
            throw new IllegalArgumentException("locale cannot be null");
        }
        if (this.mLocales != null && this.mLocales.size() == 1 && locale.equals(this.mLocales.get(0))) {
            return;
        }
        this.mLocales = new LocaleList(locale);
        syncTextLocalesWithMinikin();
    }

    public void setTextLocales(LocaleList localeList) {
        if (localeList == null || localeList.isEmpty()) {
            throw new IllegalArgumentException("locales cannot be null or empty");
        }
        if (localeList.equals(this.mLocales)) {
            return;
        }
        this.mLocales = localeList;
        syncTextLocalesWithMinikin();
    }

    private void syncTextLocalesWithMinikin() {
        String languageTags = this.mLocales.toLanguageTags();
        synchronized (sCacheLock) {
            Integer num = sMinikinLocaleListIdCache.get(languageTags);
            if (num == null) {
                sMinikinLocaleListIdCache.put(languageTags, Integer.valueOf(nSetTextLocales(this.mNativePaint, languageTags)));
            } else {
                nSetTextLocalesByMinikinLocaleListId(this.mNativePaint, num.intValue());
            }
        }
    }

    public boolean isElegantTextHeight() {
        return nIsElegantTextHeight(this.mNativePaint);
    }

    public void setElegantTextHeight(boolean z) {
        nSetElegantTextHeight(this.mNativePaint, z);
    }

    public float getTextSize() {
        return nGetTextSize(this.mNativePaint);
    }

    public void setTextSize(float f) {
        nSetTextSize(this.mNativePaint, f);
    }

    public float getTextScaleX() {
        return nGetTextScaleX(this.mNativePaint);
    }

    public void setTextScaleX(float f) {
        nSetTextScaleX(this.mNativePaint, f);
    }

    public float getTextSkewX() {
        return nGetTextSkewX(this.mNativePaint);
    }

    public void setTextSkewX(float f) {
        nSetTextSkewX(this.mNativePaint, f);
    }

    public float getLetterSpacing() {
        return nGetLetterSpacing(this.mNativePaint);
    }

    public void setLetterSpacing(float f) {
        nSetLetterSpacing(this.mNativePaint, f);
    }

    public float getWordSpacing() {
        return nGetWordSpacing(this.mNativePaint);
    }

    public void setWordSpacing(float f) {
        nSetWordSpacing(this.mNativePaint, f);
    }

    public String getFontFeatureSettings() {
        return this.mFontFeatureSettings;
    }

    public void setFontFeatureSettings(String str) {
        if (str != null && str.equals("")) {
            str = null;
        }
        if (str != null || this.mFontFeatureSettings != null) {
            if (str != null && str.equals(this.mFontFeatureSettings)) {
                return;
            }
            this.mFontFeatureSettings = str;
            nSetFontFeatureSettings(this.mNativePaint, str);
        }
    }

    public String getFontVariationSettings() {
        return this.mFontVariationSettings;
    }

    public boolean setFontVariationSettings(String str) {
        String strNullIfEmpty = TextUtils.nullIfEmpty(str);
        if (strNullIfEmpty == this.mFontVariationSettings || (strNullIfEmpty != null && strNullIfEmpty.equals(this.mFontVariationSettings))) {
            return true;
        }
        if (strNullIfEmpty == null || strNullIfEmpty.length() == 0) {
            this.mFontVariationSettings = null;
            setTypeface(Typeface.createFromTypefaceWithVariation(this.mTypeface, Collections.emptyList()));
            return true;
        }
        Typeface typeface = this.mTypeface == null ? Typeface.DEFAULT : this.mTypeface;
        FontVariationAxis[] fontVariationAxisArrFromFontVariationSettings = FontVariationAxis.fromFontVariationSettings(strNullIfEmpty);
        ArrayList arrayList = new ArrayList();
        for (FontVariationAxis fontVariationAxis : fontVariationAxisArrFromFontVariationSettings) {
            if (typeface.isSupportedAxes(fontVariationAxis.getOpenTypeTagValue())) {
                arrayList.add(fontVariationAxis);
            }
        }
        if (arrayList.isEmpty()) {
            return false;
        }
        this.mFontVariationSettings = strNullIfEmpty;
        setTypeface(Typeface.createFromTypefaceWithVariation(typeface, arrayList));
        return true;
    }

    public int getHyphenEdit() {
        return nGetHyphenEdit(this.mNativePaint);
    }

    public void setHyphenEdit(int i) {
        nSetHyphenEdit(this.mNativePaint, i);
    }

    public float ascent() {
        return nAscent(this.mNativePaint);
    }

    public float descent() {
        return nDescent(this.mNativePaint);
    }

    public float getFontMetrics(FontMetrics fontMetrics) {
        return nGetFontMetrics(this.mNativePaint, fontMetrics);
    }

    public FontMetrics getFontMetrics() {
        FontMetrics fontMetrics = new FontMetrics();
        getFontMetrics(fontMetrics);
        return fontMetrics;
    }

    public static class FontMetricsInt {
        public int ascent;
        public int bottom;
        public int descent;
        public int leading;
        public int top;

        public String toString() {
            return "FontMetricsInt: top=" + this.top + " ascent=" + this.ascent + " descent=" + this.descent + " bottom=" + this.bottom + " leading=" + this.leading;
        }
    }

    public int getFontMetricsInt(FontMetricsInt fontMetricsInt) {
        return nGetFontMetricsInt(this.mNativePaint, fontMetricsInt);
    }

    public FontMetricsInt getFontMetricsInt() {
        FontMetricsInt fontMetricsInt = new FontMetricsInt();
        getFontMetricsInt(fontMetricsInt);
        return fontMetricsInt;
    }

    public float getFontSpacing() {
        return getFontMetrics(null);
    }

    public float measureText(char[] cArr, int i, int i2) {
        if (cArr == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if ((i | i2) < 0 || i + i2 > cArr.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (cArr.length == 0 || i2 == 0) {
            return 0.0f;
        }
        if (!this.mHasCompatScaling) {
            return (float) Math.ceil(nGetTextAdvances(this.mNativePaint, cArr, i, i2, i, i2, this.mBidiFlags, (float[]) null, 0));
        }
        float textSize = getTextSize();
        setTextSize(this.mCompatScaling * textSize);
        float fNGetTextAdvances = nGetTextAdvances(this.mNativePaint, cArr, i, i2, i, i2, this.mBidiFlags, (float[]) null, 0);
        setTextSize(textSize);
        return (float) Math.ceil(fNGetTextAdvances * this.mInvCompatScaling);
    }

    public float measureText(String str, int i, int i2) {
        if (str == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if ((i | i2 | (i2 - i) | (str.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (str.length() == 0 || i == i2) {
            return 0.0f;
        }
        if (!this.mHasCompatScaling) {
            return (float) Math.ceil(nGetTextAdvances(this.mNativePaint, str, i, i2, i, i2, this.mBidiFlags, (float[]) null, 0));
        }
        float textSize = getTextSize();
        setTextSize(this.mCompatScaling * textSize);
        float fNGetTextAdvances = nGetTextAdvances(this.mNativePaint, str, i, i2, i, i2, this.mBidiFlags, (float[]) null, 0);
        setTextSize(textSize);
        return (float) Math.ceil(fNGetTextAdvances * this.mInvCompatScaling);
    }

    public float measureText(String str) {
        if (str == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        return measureText(str, 0, str.length());
    }

    public float measureText(CharSequence charSequence, int i, int i2) {
        if (charSequence == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        int i3 = i2 - i;
        if ((i | i2 | i3 | (charSequence.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (charSequence.length() == 0 || i == i2) {
            return 0.0f;
        }
        if (charSequence instanceof String) {
            return measureText((String) charSequence, i, i2);
        }
        if ((charSequence instanceof SpannedString) || (charSequence instanceof SpannableString)) {
            return measureText(charSequence.toString(), i, i2);
        }
        if (charSequence instanceof GraphicsOperations) {
            return ((GraphicsOperations) charSequence).measureText(i, i2, this);
        }
        char[] cArrObtain = TemporaryBuffer.obtain(i3);
        TextUtils.getChars(charSequence, i, i2, cArrObtain, 0);
        float fMeasureText = measureText(cArrObtain, 0, i3);
        TemporaryBuffer.recycle(cArrObtain);
        return fMeasureText;
    }

    public int breakText(char[] cArr, int i, int i2, float f, float[] fArr) {
        if (cArr == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if (i < 0 || cArr.length - i < Math.abs(i2)) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (cArr.length == 0 || i2 == 0) {
            return 0;
        }
        if (!this.mHasCompatScaling) {
            return nBreakText(this.mNativePaint, cArr, i, i2, f, this.mBidiFlags, fArr);
        }
        float textSize = getTextSize();
        setTextSize(this.mCompatScaling * textSize);
        int iNBreakText = nBreakText(this.mNativePaint, cArr, i, i2, this.mCompatScaling * f, this.mBidiFlags, fArr);
        setTextSize(textSize);
        if (fArr != null) {
            fArr[0] = fArr[0] * this.mInvCompatScaling;
        }
        return iNBreakText;
    }

    public int breakText(CharSequence charSequence, int i, int i2, boolean z, float f, float[] fArr) {
        int iBreakText;
        if (charSequence == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        int i3 = i2 - i;
        if ((i | i2 | i3 | (charSequence.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (charSequence.length() == 0 || i == i2) {
            return 0;
        }
        if (i == 0 && (charSequence instanceof String) && i2 == charSequence.length()) {
            return breakText((String) charSequence, z, f, fArr);
        }
        char[] cArrObtain = TemporaryBuffer.obtain(i3);
        TextUtils.getChars(charSequence, i, i2, cArrObtain, 0);
        if (z) {
            iBreakText = breakText(cArrObtain, 0, i3, f, fArr);
        } else {
            iBreakText = breakText(cArrObtain, 0, -i3, f, fArr);
        }
        TemporaryBuffer.recycle(cArrObtain);
        return iBreakText;
    }

    public int breakText(String str, boolean z, float f, float[] fArr) {
        if (str == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if (str.length() == 0) {
            return 0;
        }
        if (!this.mHasCompatScaling) {
            return nBreakText(this.mNativePaint, str, z, f, this.mBidiFlags, fArr);
        }
        float textSize = getTextSize();
        setTextSize(this.mCompatScaling * textSize);
        int iNBreakText = nBreakText(this.mNativePaint, str, z, f * this.mCompatScaling, this.mBidiFlags, fArr);
        setTextSize(textSize);
        if (fArr != null) {
            fArr[0] = fArr[0] * this.mInvCompatScaling;
        }
        return iNBreakText;
    }

    public int getTextWidths(char[] cArr, int i, int i2, float[] fArr) {
        if (cArr == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if ((i | i2) < 0 || i + i2 > cArr.length || i2 > fArr.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (cArr.length == 0 || i2 == 0) {
            return 0;
        }
        if (!this.mHasCompatScaling) {
            nGetTextAdvances(this.mNativePaint, cArr, i, i2, i, i2, this.mBidiFlags, fArr, 0);
            return i2;
        }
        float textSize = getTextSize();
        setTextSize(this.mCompatScaling * textSize);
        nGetTextAdvances(this.mNativePaint, cArr, i, i2, i, i2, this.mBidiFlags, fArr, 0);
        setTextSize(textSize);
        for (int i3 = 0; i3 < i2; i3++) {
            fArr[i3] = fArr[i3] * this.mInvCompatScaling;
        }
        return i2;
    }

    public int getTextWidths(CharSequence charSequence, int i, int i2, float[] fArr) {
        if (charSequence == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        int i3 = i2 - i;
        if ((i | i2 | i3 | (charSequence.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i3 > fArr.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (charSequence.length() == 0 || i == i2) {
            return 0;
        }
        if (charSequence instanceof String) {
            return getTextWidths((String) charSequence, i, i2, fArr);
        }
        if ((charSequence instanceof SpannedString) || (charSequence instanceof SpannableString)) {
            return getTextWidths(charSequence.toString(), i, i2, fArr);
        }
        if (charSequence instanceof GraphicsOperations) {
            return ((GraphicsOperations) charSequence).getTextWidths(i, i2, fArr, this);
        }
        char[] cArrObtain = TemporaryBuffer.obtain(i3);
        TextUtils.getChars(charSequence, i, i2, cArrObtain, 0);
        int textWidths = getTextWidths(cArrObtain, 0, i3, fArr);
        TemporaryBuffer.recycle(cArrObtain);
        return textWidths;
    }

    public int getTextWidths(String str, int i, int i2, float[] fArr) {
        if (str == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        int i3 = i2 - i;
        if ((i | i2 | i3 | (str.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i3 > fArr.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (str.length() == 0 || i == i2) {
            return 0;
        }
        if (!this.mHasCompatScaling) {
            nGetTextAdvances(this.mNativePaint, str, i, i2, i, i2, this.mBidiFlags, fArr, 0);
            return i3;
        }
        float textSize = getTextSize();
        setTextSize(this.mCompatScaling * textSize);
        nGetTextAdvances(this.mNativePaint, str, i, i2, i, i2, this.mBidiFlags, fArr, 0);
        setTextSize(textSize);
        for (int i4 = 0; i4 < i3; i4++) {
            fArr[i4] = fArr[i4] * this.mInvCompatScaling;
        }
        return i3;
    }

    public int getTextWidths(String str, float[] fArr) {
        return getTextWidths(str, 0, str.length(), fArr);
    }

    public float getTextRunAdvances(char[] cArr, int i, int i2, int i3, int i4, boolean z, float[] fArr, int i5) {
        if (cArr == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        int i6 = i3 + i4;
        if ((i | i2 | i3 | i4 | i5 | (i - i3) | (i4 - i2) | (i6 - (i + i2)) | (cArr.length - i6) | (fArr == null ? 0 : fArr.length - (i5 + i2))) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (cArr.length == 0 || i2 == 0) {
            return 0.0f;
        }
        if (!this.mHasCompatScaling) {
            return nGetTextAdvances(this.mNativePaint, cArr, i, i2, i3, i4, z ? 5 : 4, fArr, i5);
        }
        float textSize = getTextSize();
        setTextSize(this.mCompatScaling * textSize);
        float fNGetTextAdvances = nGetTextAdvances(this.mNativePaint, cArr, i, i2, i3, i4, z ? 5 : 4, fArr, i5);
        setTextSize(textSize);
        if (fArr != null) {
            int i7 = i5 + i2;
            for (int i8 = i5; i8 < i7; i8++) {
                fArr[i8] = fArr[i8] * this.mInvCompatScaling;
            }
        }
        return fNGetTextAdvances * this.mInvCompatScaling;
    }

    public float getTextRunAdvances(CharSequence charSequence, int i, int i2, int i3, int i4, boolean z, float[] fArr, int i5) {
        if (charSequence == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        int i6 = i2 - i;
        int i7 = i - i3;
        if ((i | i2 | i3 | i4 | i5 | i6 | i7 | (i4 - i2) | (charSequence.length() - i4) | (fArr == null ? 0 : (fArr.length - i5) - i6)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (charSequence instanceof String) {
            return getTextRunAdvances((String) charSequence, i, i2, i3, i4, z, fArr, i5);
        }
        if ((charSequence instanceof SpannedString) || (charSequence instanceof SpannableString)) {
            return getTextRunAdvances(charSequence.toString(), i, i2, i3, i4, z, fArr, i5);
        }
        if (charSequence instanceof GraphicsOperations) {
            return ((GraphicsOperations) charSequence).getTextRunAdvances(i, i2, i3, i4, z, fArr, i5, this);
        }
        if (charSequence.length() == 0 || i2 == i) {
            return 0.0f;
        }
        int i8 = i4 - i3;
        char[] cArrObtain = TemporaryBuffer.obtain(i8);
        TextUtils.getChars(charSequence, i3, i4, cArrObtain, 0);
        float textRunAdvances = getTextRunAdvances(cArrObtain, i7, i6, 0, i8, z, fArr, i5);
        TemporaryBuffer.recycle(cArrObtain);
        return textRunAdvances;
    }

    public float getTextRunAdvances(String str, int i, int i2, int i3, int i4, boolean z, float[] fArr, int i5) {
        if (str == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        int i6 = i2 - i;
        if ((i | i2 | i3 | i4 | i5 | i6 | (i - i3) | (i4 - i2) | (str.length() - i4) | (fArr == null ? 0 : (fArr.length - i5) - i6)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (str.length() == 0 || i == i2) {
            return 0.0f;
        }
        if (!this.mHasCompatScaling) {
            return nGetTextAdvances(this.mNativePaint, str, i, i2, i3, i4, z ? 5 : 4, fArr, i5);
        }
        float textSize = getTextSize();
        setTextSize(this.mCompatScaling * textSize);
        float fNGetTextAdvances = nGetTextAdvances(this.mNativePaint, str, i, i2, i3, i4, z ? 5 : 4, fArr, i5);
        setTextSize(textSize);
        if (fArr != null) {
            int i7 = i5 + i6;
            for (int i8 = i5; i8 < i7; i8++) {
                fArr[i8] = fArr[i8] * this.mInvCompatScaling;
            }
        }
        return fNGetTextAdvances * this.mInvCompatScaling;
    }

    public int getTextRunCursor(char[] cArr, int i, int i2, int i3, int i4, int i5) {
        int i6 = i + i2;
        if ((i | i6 | i4 | (i6 - i) | (i4 - i) | (i6 - i4) | (cArr.length - i6) | i5) < 0 || i5 > 4) {
            throw new IndexOutOfBoundsException();
        }
        return nGetTextRunCursor(this.mNativePaint, cArr, i, i2, i3, i4, i5);
    }

    public int getTextRunCursor(CharSequence charSequence, int i, int i2, int i3, int i4, int i5) {
        if ((charSequence instanceof String) || (charSequence instanceof SpannedString) || (charSequence instanceof SpannableString)) {
            return getTextRunCursor(charSequence.toString(), i, i2, i3, i4, i5);
        }
        if (charSequence instanceof GraphicsOperations) {
            return ((GraphicsOperations) charSequence).getTextRunCursor(i, i2, i3, i4, i5, this);
        }
        int i6 = i2 - i;
        char[] cArrObtain = TemporaryBuffer.obtain(i6);
        TextUtils.getChars(charSequence, i, i2, cArrObtain, 0);
        int textRunCursor = getTextRunCursor(cArrObtain, 0, i6, i3, i4 - i, i5);
        TemporaryBuffer.recycle(cArrObtain);
        if (textRunCursor == -1) {
            return -1;
        }
        return textRunCursor + i;
    }

    public int getTextRunCursor(String str, int i, int i2, int i3, int i4, int i5) {
        if ((i | i2 | i4 | (i2 - i) | (i4 - i) | (i2 - i4) | (str.length() - i2) | i5) < 0 || i5 > 4) {
            throw new IndexOutOfBoundsException();
        }
        return nGetTextRunCursor(this.mNativePaint, str, i, i2, i3, i4, i5);
    }

    public void getTextPath(char[] cArr, int i, int i2, float f, float f2, Path path) {
        if ((i | i2) < 0 || i + i2 > cArr.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        nGetTextPath(this.mNativePaint, this.mBidiFlags, cArr, i, i2, f, f2, path.mutateNI());
    }

    public void getTextPath(String str, int i, int i2, float f, float f2, Path path) {
        if ((i | i2 | (i2 - i) | (str.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        nGetTextPath(this.mNativePaint, this.mBidiFlags, str, i, i2, f, f2, path.mutateNI());
    }

    public void getTextBounds(String str, int i, int i2, Rect rect) {
        if ((i | i2 | (i2 - i) | (str.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (rect == null) {
            throw new NullPointerException("need bounds Rect");
        }
        nGetStringBounds(this.mNativePaint, str, i, i2, this.mBidiFlags, rect);
    }

    public void getTextBounds(CharSequence charSequence, int i, int i2, Rect rect) {
        int i3 = i2 - i;
        if ((i | i2 | i3 | (charSequence.length() - i2)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (rect == null) {
            throw new NullPointerException("need bounds Rect");
        }
        char[] cArrObtain = TemporaryBuffer.obtain(i3);
        TextUtils.getChars(charSequence, i, i2, cArrObtain, 0);
        getTextBounds(cArrObtain, 0, i3, rect);
        TemporaryBuffer.recycle(cArrObtain);
    }

    public void getTextBounds(char[] cArr, int i, int i2, Rect rect) {
        if ((i | i2) < 0 || i + i2 > cArr.length) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (rect == null) {
            throw new NullPointerException("need bounds Rect");
        }
        nGetCharArrayBounds(this.mNativePaint, cArr, i, i2, this.mBidiFlags, rect);
    }

    public boolean hasGlyph(String str) {
        return nHasGlyph(this.mNativePaint, this.mBidiFlags, str);
    }

    public float getRunAdvance(char[] cArr, int i, int i2, int i3, int i4, boolean z, int i5) {
        if (cArr == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if ((i3 | i | i5 | i2 | i4 | (i - i3) | (i5 - i) | (i2 - i5) | (i4 - i2) | (cArr.length - i4)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == i) {
            return 0.0f;
        }
        return nGetRunAdvance(this.mNativePaint, cArr, i, i2, i3, i4, z, i5);
    }

    public float getRunAdvance(CharSequence charSequence, int i, int i2, int i3, int i4, boolean z, int i5) {
        if (charSequence == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        int i6 = i - i3;
        if ((i3 | i | i5 | i2 | i4 | i6 | (i5 - i) | (i2 - i5) | (i4 - i2) | (charSequence.length() - i4)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        if (i2 == i) {
            return 0.0f;
        }
        int i7 = i4 - i3;
        char[] cArrObtain = TemporaryBuffer.obtain(i7);
        TextUtils.getChars(charSequence, i3, i4, cArrObtain, 0);
        float runAdvance = getRunAdvance(cArrObtain, i6, i2 - i3, 0, i7, z, i5 - i3);
        TemporaryBuffer.recycle(cArrObtain);
        return runAdvance;
    }

    public int getOffsetForAdvance(char[] cArr, int i, int i2, int i3, int i4, boolean z, float f) {
        if (cArr == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        if ((i3 | i | i2 | i4 | (i - i3) | (i2 - i) | (i4 - i2) | (cArr.length - i4)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        return nGetOffsetForAdvance(this.mNativePaint, cArr, i, i2, i3, i4, z, f);
    }

    public int getOffsetForAdvance(CharSequence charSequence, int i, int i2, int i3, int i4, boolean z, float f) {
        if (charSequence == null) {
            throw new IllegalArgumentException("text cannot be null");
        }
        int i5 = i - i3;
        if (((i2 - i) | i3 | i | i2 | i4 | i5 | (i4 - i2) | (charSequence.length() - i4)) < 0) {
            throw new IndexOutOfBoundsException();
        }
        int i6 = i4 - i3;
        char[] cArrObtain = TemporaryBuffer.obtain(i6);
        TextUtils.getChars(charSequence, i3, i4, cArrObtain, 0);
        int offsetForAdvance = getOffsetForAdvance(cArrObtain, i5, i2 - i3, 0, i6, z, f) + i3;
        TemporaryBuffer.recycle(cArrObtain);
        return offsetForAdvance;
    }

    public boolean equalsForTextMeasurement(Paint paint) {
        return nEqualsForTextMeasurement(this.mNativePaint, paint.mNativePaint);
    }
}
