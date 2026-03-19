package android.content.res;

import android.content.res.Resources;
import android.graphics.LinearGradient;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.net.wifi.WifiEnterpriseConfig;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Xml;
import com.android.ims.ImsConfig;
import com.android.internal.R;
import com.android.internal.util.GrowingArrayUtils;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class GradientColor extends ComplexColor {
    private static final boolean DBG_GRADIENT = false;
    private static final String TAG = "GradientColor";
    private static final int TILE_MODE_CLAMP = 0;
    private static final int TILE_MODE_MIRROR = 2;
    private static final int TILE_MODE_REPEAT = 1;
    private int mCenterColor;
    private float mCenterX;
    private float mCenterY;
    private int mChangingConfigurations;
    private int mDefaultColor;
    private int mEndColor;
    private float mEndX;
    private float mEndY;
    private GradientColorFactory mFactory;
    private float mGradientRadius;
    private int mGradientType;
    private boolean mHasCenterColor;
    private int[] mItemColors;
    private float[] mItemOffsets;
    private int[][] mItemsThemeAttrs;
    private Shader mShader;
    private int mStartColor;
    private float mStartX;
    private float mStartY;
    private int[] mThemeAttrs;
    private int mTileMode;

    @Retention(RetentionPolicy.SOURCE)
    private @interface GradientTileMode {
    }

    private GradientColor() {
        this.mShader = null;
        this.mGradientType = 0;
        this.mCenterX = 0.0f;
        this.mCenterY = 0.0f;
        this.mStartX = 0.0f;
        this.mStartY = 0.0f;
        this.mEndX = 0.0f;
        this.mEndY = 0.0f;
        this.mStartColor = 0;
        this.mCenterColor = 0;
        this.mEndColor = 0;
        this.mHasCenterColor = false;
        this.mTileMode = 0;
        this.mGradientRadius = 0.0f;
    }

    private GradientColor(GradientColor gradientColor) {
        this.mShader = null;
        this.mGradientType = 0;
        this.mCenterX = 0.0f;
        this.mCenterY = 0.0f;
        this.mStartX = 0.0f;
        this.mStartY = 0.0f;
        this.mEndX = 0.0f;
        this.mEndY = 0.0f;
        this.mStartColor = 0;
        this.mCenterColor = 0;
        this.mEndColor = 0;
        this.mHasCenterColor = false;
        this.mTileMode = 0;
        this.mGradientRadius = 0.0f;
        if (gradientColor != null) {
            this.mChangingConfigurations = gradientColor.mChangingConfigurations;
            this.mDefaultColor = gradientColor.mDefaultColor;
            this.mShader = gradientColor.mShader;
            this.mGradientType = gradientColor.mGradientType;
            this.mCenterX = gradientColor.mCenterX;
            this.mCenterY = gradientColor.mCenterY;
            this.mStartX = gradientColor.mStartX;
            this.mStartY = gradientColor.mStartY;
            this.mEndX = gradientColor.mEndX;
            this.mEndY = gradientColor.mEndY;
            this.mStartColor = gradientColor.mStartColor;
            this.mCenterColor = gradientColor.mCenterColor;
            this.mEndColor = gradientColor.mEndColor;
            this.mHasCenterColor = gradientColor.mHasCenterColor;
            this.mGradientRadius = gradientColor.mGradientRadius;
            this.mTileMode = gradientColor.mTileMode;
            if (gradientColor.mItemColors != null) {
                this.mItemColors = (int[]) gradientColor.mItemColors.clone();
            }
            if (gradientColor.mItemOffsets != null) {
                this.mItemOffsets = (float[]) gradientColor.mItemOffsets.clone();
            }
            if (gradientColor.mThemeAttrs != null) {
                this.mThemeAttrs = (int[]) gradientColor.mThemeAttrs.clone();
            }
            if (gradientColor.mItemsThemeAttrs != null) {
                this.mItemsThemeAttrs = (int[][]) gradientColor.mItemsThemeAttrs.clone();
            }
        }
    }

    private static Shader.TileMode parseTileMode(int i) {
        switch (i) {
            case 0:
                return Shader.TileMode.CLAMP;
            case 1:
                return Shader.TileMode.REPEAT;
            case 2:
                return Shader.TileMode.MIRROR;
            default:
                return Shader.TileMode.CLAMP;
        }
    }

    private void updateRootElementState(TypedArray typedArray) {
        this.mThemeAttrs = typedArray.extractThemeAttrs();
        this.mStartX = typedArray.getFloat(8, this.mStartX);
        this.mStartY = typedArray.getFloat(9, this.mStartY);
        this.mEndX = typedArray.getFloat(10, this.mEndX);
        this.mEndY = typedArray.getFloat(11, this.mEndY);
        this.mCenterX = typedArray.getFloat(3, this.mCenterX);
        this.mCenterY = typedArray.getFloat(4, this.mCenterY);
        this.mGradientType = typedArray.getInt(2, this.mGradientType);
        this.mStartColor = typedArray.getColor(0, this.mStartColor);
        this.mHasCenterColor |= typedArray.hasValue(7);
        this.mCenterColor = typedArray.getColor(7, this.mCenterColor);
        this.mEndColor = typedArray.getColor(1, this.mEndColor);
        this.mTileMode = typedArray.getInt(6, this.mTileMode);
        this.mGradientRadius = typedArray.getFloat(5, this.mGradientRadius);
    }

    private void validateXmlContent() throws XmlPullParserException {
        if (this.mGradientRadius <= 0.0f && this.mGradientType == 1) {
            throw new XmlPullParserException("<gradient> tag requires 'gradientRadius' attribute with radial type");
        }
    }

    public Shader getShader() {
        return this.mShader;
    }

    public static GradientColor createFromXml(Resources resources, XmlResourceParser xmlResourceParser, Resources.Theme theme) throws XmlPullParserException, IOException {
        int next;
        AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlResourceParser);
        do {
            next = xmlResourceParser.next();
            if (next == 2) {
                break;
            }
        } while (next != 1);
        if (next != 2) {
            throw new XmlPullParserException("No start tag found");
        }
        return createFromXmlInner(resources, xmlResourceParser, attributeSetAsAttributeSet, theme);
    }

    static GradientColor createFromXmlInner(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        String name = xmlPullParser.getName();
        if (!name.equals("gradient")) {
            throw new XmlPullParserException(xmlPullParser.getPositionDescription() + ": invalid gradient color tag " + name);
        }
        GradientColor gradientColor = new GradientColor();
        gradientColor.inflate(resources, xmlPullParser, attributeSet, theme);
        return gradientColor;
    }

    private void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        TypedArray typedArrayObtainAttributes = Resources.obtainAttributes(resources, theme, attributeSet, R.styleable.GradientColor);
        updateRootElementState(typedArrayObtainAttributes);
        this.mChangingConfigurations |= typedArrayObtainAttributes.getChangingConfigurations();
        typedArrayObtainAttributes.recycle();
        validateXmlContent();
        inflateChildElements(resources, xmlPullParser, attributeSet, theme);
        onColorsChange();
    }

    private void inflateChildElements(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        int depth;
        int i = 1;
        int depth2 = xmlPullParser.getDepth() + 1;
        float[] fArr = new float[20];
        int[] iArr = new int[fArr.length];
        int[][] iArr2 = new int[fArr.length][];
        float[] fArrAppend = fArr;
        int[] iArrAppend = iArr;
        int i2 = 0;
        boolean z = false;
        while (true) {
            int next = xmlPullParser.next();
            if (next == i || ((depth = xmlPullParser.getDepth()) < depth2 && next == 3)) {
                break;
            }
            if (next == 2 && depth <= depth2 && xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                TypedArray typedArrayObtainAttributes = Resources.obtainAttributes(resources, theme, attributeSet, R.styleable.GradientColorItem);
                boolean zHasValue = typedArrayObtainAttributes.hasValue(0);
                boolean zHasValue2 = typedArrayObtainAttributes.hasValue(i);
                if (!zHasValue || !zHasValue2) {
                    break;
                }
                int[] iArrExtractThemeAttrs = typedArrayObtainAttributes.extractThemeAttrs();
                int color = typedArrayObtainAttributes.getColor(0, 0);
                float f = typedArrayObtainAttributes.getFloat(i, 0.0f);
                this.mChangingConfigurations |= typedArrayObtainAttributes.getChangingConfigurations();
                typedArrayObtainAttributes.recycle();
                if (iArrExtractThemeAttrs != null) {
                    z = true;
                }
                iArrAppend = GrowingArrayUtils.append(iArrAppend, i2, color);
                fArrAppend = GrowingArrayUtils.append(fArrAppend, i2, f);
                iArr2 = (int[][]) GrowingArrayUtils.append(iArr2, i2, iArrExtractThemeAttrs);
                i2++;
            }
            i = 1;
        }
        throw new XmlPullParserException(xmlPullParser.getPositionDescription() + ": <item> tag requires a 'color' attribute and a 'offset' attribute!");
    }

    private void applyItemsAttrsTheme(Resources.Theme theme) {
        if (this.mItemsThemeAttrs == null) {
            return;
        }
        int[][] iArr = this.mItemsThemeAttrs;
        int length = iArr.length;
        boolean z = false;
        for (int i = 0; i < length; i++) {
            if (iArr[i] != null) {
                TypedArray typedArrayResolveAttributes = theme.resolveAttributes(iArr[i], R.styleable.GradientColorItem);
                iArr[i] = typedArrayResolveAttributes.extractThemeAttrs(iArr[i]);
                if (iArr[i] != null) {
                    z = true;
                }
                this.mItemColors[i] = typedArrayResolveAttributes.getColor(0, this.mItemColors[i]);
                this.mItemOffsets[i] = typedArrayResolveAttributes.getFloat(1, this.mItemOffsets[i]);
                this.mChangingConfigurations |= typedArrayResolveAttributes.getChangingConfigurations();
                typedArrayResolveAttributes.recycle();
            }
        }
        if (!z) {
            this.mItemsThemeAttrs = null;
        }
    }

    private void onColorsChange() {
        int[] iArr;
        float[] fArr;
        if (this.mItemColors != null) {
            int length = this.mItemColors.length;
            iArr = new int[length];
            fArr = new float[length];
            for (int i = 0; i < length; i++) {
                iArr[i] = this.mItemColors[i];
                fArr[i] = this.mItemOffsets[i];
            }
        } else if (this.mHasCenterColor) {
            iArr = new int[]{this.mStartColor, this.mCenterColor, this.mEndColor};
            fArr = new float[]{0.0f, 0.5f, 1.0f};
        } else {
            iArr = new int[]{this.mStartColor, this.mEndColor};
            fArr = null;
        }
        if (iArr.length < 2) {
            Log.w(TAG, "<gradient> tag requires 2 color values specified!" + iArr.length + WifiEnterpriseConfig.CA_CERT_ALIAS_DELIMITER + iArr);
        }
        if (this.mGradientType == 0) {
            this.mShader = new LinearGradient(this.mStartX, this.mStartY, this.mEndX, this.mEndY, iArr, fArr, parseTileMode(this.mTileMode));
        } else if (this.mGradientType == 1) {
            this.mShader = new RadialGradient(this.mCenterX, this.mCenterY, this.mGradientRadius, iArr, fArr, parseTileMode(this.mTileMode));
        } else {
            this.mShader = new SweepGradient(this.mCenterX, this.mCenterY, iArr, fArr);
        }
        this.mDefaultColor = iArr[0];
    }

    @Override
    public int getDefaultColor() {
        return this.mDefaultColor;
    }

    @Override
    public ConstantState<ComplexColor> getConstantState() {
        if (this.mFactory == null) {
            this.mFactory = new GradientColorFactory(this);
        }
        return this.mFactory;
    }

    private static class GradientColorFactory extends ConstantState<ComplexColor> {
        private final GradientColor mSrc;

        public GradientColorFactory(GradientColor gradientColor) {
            this.mSrc = gradientColor;
        }

        @Override
        public int getChangingConfigurations() {
            return this.mSrc.mChangingConfigurations;
        }

        @Override
        public ComplexColor newInstance2() {
            return this.mSrc;
        }

        @Override
        public ComplexColor newInstance2(Resources resources, Resources.Theme theme) {
            return this.mSrc.obtainForTheme(theme);
        }
    }

    @Override
    public GradientColor obtainForTheme(Resources.Theme theme) {
        if (theme == null || !canApplyTheme()) {
            return this;
        }
        GradientColor gradientColor = new GradientColor(this);
        gradientColor.applyTheme(theme);
        return gradientColor;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mChangingConfigurations;
    }

    private void applyTheme(Resources.Theme theme) {
        if (this.mThemeAttrs != null) {
            applyRootAttrsTheme(theme);
        }
        if (this.mItemsThemeAttrs != null) {
            applyItemsAttrsTheme(theme);
        }
        onColorsChange();
    }

    private void applyRootAttrsTheme(Resources.Theme theme) {
        TypedArray typedArrayResolveAttributes = theme.resolveAttributes(this.mThemeAttrs, R.styleable.GradientColor);
        this.mThemeAttrs = typedArrayResolveAttributes.extractThemeAttrs(this.mThemeAttrs);
        updateRootElementState(typedArrayResolveAttributes);
        this.mChangingConfigurations |= typedArrayResolveAttributes.getChangingConfigurations();
        typedArrayResolveAttributes.recycle();
    }

    @Override
    public boolean canApplyTheme() {
        return (this.mThemeAttrs == null && this.mItemsThemeAttrs == null) ? false : true;
    }
}
