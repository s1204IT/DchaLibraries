package android.content.res;

import android.content.res.Resources;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.MathUtils;
import android.util.SparseArray;
import android.util.StateSet;
import android.util.Xml;
import com.android.ims.ImsConfig;
import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.GrowingArrayUtils;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class ColorStateList extends ComplexColor implements Parcelable {
    private static final int DEFAULT_COLOR = -65536;
    private static final String TAG = "ColorStateList";
    private int mChangingConfigurations;
    private int[] mColors;
    private int mDefaultColor;
    private ColorStateListFactory mFactory;
    private boolean mIsOpaque;
    private int[][] mStateSpecs;
    private int[][] mThemeAttrs;
    private static final int[][] EMPTY = {new int[0]};
    private static final SparseArray<WeakReference<ColorStateList>> sCache = new SparseArray<>();
    public static final Parcelable.Creator<ColorStateList> CREATOR = new Parcelable.Creator<ColorStateList>() {
        @Override
        public ColorStateList[] newArray(int i) {
            return new ColorStateList[i];
        }

        @Override
        public ColorStateList createFromParcel(Parcel parcel) {
            int i = parcel.readInt();
            int[][] iArr = new int[i][];
            for (int i2 = 0; i2 < i; i2++) {
                iArr[i2] = parcel.createIntArray();
            }
            return new ColorStateList(iArr, parcel.createIntArray());
        }
    };

    private ColorStateList() {
    }

    public ColorStateList(int[][] iArr, int[] iArr2) {
        this.mStateSpecs = iArr;
        this.mColors = iArr2;
        onColorsChanged();
    }

    public static ColorStateList valueOf(int i) {
        synchronized (sCache) {
            int iIndexOfKey = sCache.indexOfKey(i);
            if (iIndexOfKey >= 0) {
                ColorStateList colorStateList = sCache.valueAt(iIndexOfKey).get();
                if (colorStateList != null) {
                    return colorStateList;
                }
                sCache.removeAt(iIndexOfKey);
            }
            for (int size = sCache.size() - 1; size >= 0; size--) {
                if (sCache.valueAt(size).get() == null) {
                    sCache.removeAt(size);
                }
            }
            ColorStateList colorStateList2 = new ColorStateList(EMPTY, new int[]{i});
            sCache.put(i, new WeakReference<>(colorStateList2));
            return colorStateList2;
        }
    }

    private ColorStateList(ColorStateList colorStateList) {
        if (colorStateList != null) {
            this.mChangingConfigurations = colorStateList.mChangingConfigurations;
            this.mStateSpecs = colorStateList.mStateSpecs;
            this.mDefaultColor = colorStateList.mDefaultColor;
            this.mIsOpaque = colorStateList.mIsOpaque;
            this.mThemeAttrs = (int[][]) colorStateList.mThemeAttrs.clone();
            this.mColors = (int[]) colorStateList.mColors.clone();
        }
    }

    @Deprecated
    public static ColorStateList createFromXml(Resources resources, XmlPullParser xmlPullParser) throws XmlPullParserException, IOException {
        return createFromXml(resources, xmlPullParser, null);
    }

    public static ColorStateList createFromXml(Resources resources, XmlPullParser xmlPullParser, Resources.Theme theme) throws XmlPullParserException, IOException {
        int next;
        AttributeSet attributeSetAsAttributeSet = Xml.asAttributeSet(xmlPullParser);
        do {
            next = xmlPullParser.next();
            if (next == 2) {
                break;
            }
        } while (next != 1);
        if (next != 2) {
            throw new XmlPullParserException("No start tag found");
        }
        return createFromXmlInner(resources, xmlPullParser, attributeSetAsAttributeSet, theme);
    }

    static ColorStateList createFromXmlInner(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        String name = xmlPullParser.getName();
        if (!name.equals("selector")) {
            throw new XmlPullParserException(xmlPullParser.getPositionDescription() + ": invalid color state list tag " + name);
        }
        ColorStateList colorStateList = new ColorStateList();
        colorStateList.inflate(resources, xmlPullParser, attributeSet, theme);
        return colorStateList;
    }

    public ColorStateList withAlpha(int i) {
        int[] iArr = new int[this.mColors.length];
        int length = iArr.length;
        for (int i2 = 0; i2 < length; i2++) {
            iArr[i2] = (this.mColors[i2] & 16777215) | (i << 24);
        }
        return new ColorStateList(this.mStateSpecs, iArr);
    }

    private void inflate(Resources resources, XmlPullParser xmlPullParser, AttributeSet attributeSet, Resources.Theme theme) throws XmlPullParserException, IOException {
        int depth;
        int i = 1;
        int depth2 = xmlPullParser.getDepth() + 1;
        int[][] iArr = (int[][]) ArrayUtils.newUnpaddedArray(int[].class, 20);
        int i2 = 0;
        int i3 = -65536;
        int[][] iArr2 = iArr;
        int[][] iArr3 = new int[iArr.length][];
        int[] iArrAppend = new int[iArr.length];
        int i4 = 0;
        boolean z = false;
        int i5 = 0;
        while (true) {
            int next = xmlPullParser.next();
            if (next == i || ((depth = xmlPullParser.getDepth()) < depth2 && next == 3)) {
                break;
            }
            if (next == 2 && depth <= depth2 && xmlPullParser.getName().equals(ImsConfig.EXTRA_CHANGED_ITEM)) {
                TypedArray typedArrayObtainAttributes = Resources.obtainAttributes(resources, theme, attributeSet, R.styleable.ColorStateListItem);
                int[] iArrExtractThemeAttrs = typedArrayObtainAttributes.extractThemeAttrs();
                int color = typedArrayObtainAttributes.getColor(i2, Color.MAGENTA);
                int i6 = depth2;
                float f = typedArrayObtainAttributes.getFloat(1, 1.0f);
                int changingConfigurations = i4 | typedArrayObtainAttributes.getChangingConfigurations();
                typedArrayObtainAttributes.recycle();
                int attributeCount = attributeSet.getAttributeCount();
                int[] iArr4 = new int[attributeCount];
                int i7 = 0;
                int i8 = 0;
                while (i7 < attributeCount) {
                    int i9 = attributeCount;
                    int attributeNameResource = attributeSet.getAttributeNameResource(i7);
                    if (attributeNameResource != 16843173 && attributeNameResource != 16843551) {
                        int i10 = i8 + 1;
                        if (!attributeSet.getAttributeBooleanValue(i7, false)) {
                            attributeNameResource = -attributeNameResource;
                        }
                        iArr4[i8] = attributeNameResource;
                        i8 = i10;
                    }
                    i7++;
                    attributeCount = i9;
                }
                int[] iArrTrimStateSet = StateSet.trimStateSet(iArr4, i8);
                int iModulateColorAlpha = modulateColorAlpha(color, f);
                if (i5 == 0 || iArrTrimStateSet.length == 0) {
                    i3 = iModulateColorAlpha;
                }
                if (iArrExtractThemeAttrs != null) {
                    z = true;
                }
                iArrAppend = GrowingArrayUtils.append(iArrAppend, i5, iModulateColorAlpha);
                iArr3 = (int[][]) GrowingArrayUtils.append(iArr3, i5, iArrExtractThemeAttrs);
                iArr2 = (int[][]) GrowingArrayUtils.append(iArr2, i5, iArrTrimStateSet);
                i5++;
                depth2 = i6;
                i4 = changingConfigurations;
            } else {
                depth2 = depth2;
            }
            i = 1;
            i2 = 0;
        }
        this.mChangingConfigurations = i4;
        this.mDefaultColor = i3;
        if (z) {
            this.mThemeAttrs = new int[i5][];
            System.arraycopy(iArr3, 0, this.mThemeAttrs, 0, i5);
        } else {
            this.mThemeAttrs = null;
        }
        this.mColors = new int[i5];
        this.mStateSpecs = new int[i5][];
        System.arraycopy(iArrAppend, 0, this.mColors, 0, i5);
        System.arraycopy(iArr2, 0, this.mStateSpecs, 0, i5);
        onColorsChanged();
    }

    @Override
    public boolean canApplyTheme() {
        return this.mThemeAttrs != null;
    }

    private void applyTheme(Resources.Theme theme) {
        float fAlpha;
        if (this.mThemeAttrs == null) {
            return;
        }
        int[][] iArr = this.mThemeAttrs;
        int length = iArr.length;
        boolean z = false;
        for (int i = 0; i < length; i++) {
            if (iArr[i] != null) {
                TypedArray typedArrayResolveAttributes = theme.resolveAttributes(iArr[i], R.styleable.ColorStateListItem);
                if (iArr[i][0] != 0) {
                    fAlpha = Color.alpha(this.mColors[i]) / 255.0f;
                } else {
                    fAlpha = 1.0f;
                }
                iArr[i] = typedArrayResolveAttributes.extractThemeAttrs(iArr[i]);
                if (iArr[i] != null) {
                    z = true;
                }
                this.mColors[i] = modulateColorAlpha(typedArrayResolveAttributes.getColor(0, this.mColors[i]), typedArrayResolveAttributes.getFloat(1, fAlpha));
                this.mChangingConfigurations |= typedArrayResolveAttributes.getChangingConfigurations();
                typedArrayResolveAttributes.recycle();
            }
        }
        if (!z) {
            this.mThemeAttrs = null;
        }
        onColorsChanged();
    }

    @Override
    public ColorStateList obtainForTheme(Resources.Theme theme) {
        if (theme == null || !canApplyTheme()) {
            return this;
        }
        ColorStateList colorStateList = new ColorStateList(this);
        colorStateList.applyTheme(theme);
        return colorStateList;
    }

    @Override
    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mChangingConfigurations;
    }

    private int modulateColorAlpha(int i, float f) {
        if (f == 1.0f) {
            return i;
        }
        return (i & 16777215) | (MathUtils.constrain((int) ((Color.alpha(i) * f) + 0.5f), 0, 255) << 24);
    }

    @Override
    public boolean isStateful() {
        return this.mStateSpecs.length >= 1 && this.mStateSpecs[0].length > 0;
    }

    public boolean hasFocusStateSpecified() {
        return StateSet.containsAttribute(this.mStateSpecs, 16842908);
    }

    public boolean isOpaque() {
        return this.mIsOpaque;
    }

    public int getColorForState(int[] iArr, int i) {
        int length = this.mStateSpecs.length;
        for (int i2 = 0; i2 < length; i2++) {
            if (StateSet.stateSetMatches(this.mStateSpecs[i2], iArr)) {
                return this.mColors[i2];
            }
        }
        return i;
    }

    @Override
    public int getDefaultColor() {
        return this.mDefaultColor;
    }

    public int[][] getStates() {
        return this.mStateSpecs;
    }

    public int[] getColors() {
        return this.mColors;
    }

    public boolean hasState(int i) {
        for (int[] iArr : this.mStateSpecs) {
            int length = iArr.length;
            for (int i2 = 0; i2 < length; i2++) {
                if (iArr[i2] == i || iArr[i2] == (~i)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String toString() {
        return "ColorStateList{mThemeAttrs=" + Arrays.deepToString(this.mThemeAttrs) + "mChangingConfigurations=" + this.mChangingConfigurations + "mStateSpecs=" + Arrays.deepToString(this.mStateSpecs) + "mColors=" + Arrays.toString(this.mColors) + "mDefaultColor=" + this.mDefaultColor + '}';
    }

    private void onColorsChanged() {
        int i;
        int[][] iArr = this.mStateSpecs;
        int[] iArr2 = this.mColors;
        int length = iArr.length;
        boolean z = true;
        if (length > 0) {
            i = iArr2[0];
            int i2 = length - 1;
            while (true) {
                if (i2 <= 0) {
                    break;
                }
                if (iArr[i2].length != 0) {
                    i2--;
                } else {
                    i = iArr2[i2];
                    break;
                }
            }
            int i3 = 0;
            while (true) {
                if (i3 >= length) {
                    break;
                }
                if (Color.alpha(iArr2[i3]) == 255) {
                    i3++;
                } else {
                    z = false;
                    break;
                }
            }
        } else {
            i = -65536;
        }
        this.mDefaultColor = i;
        this.mIsOpaque = z;
    }

    @Override
    public ConstantState<ComplexColor> getConstantState() {
        if (this.mFactory == null) {
            this.mFactory = new ColorStateListFactory(this);
        }
        return this.mFactory;
    }

    private static class ColorStateListFactory extends ConstantState<ComplexColor> {
        private final ColorStateList mSrc;

        public ColorStateListFactory(ColorStateList colorStateList) {
            this.mSrc = colorStateList;
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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        if (canApplyTheme()) {
            Log.w(TAG, "Wrote partially-resolved ColorStateList to parcel!");
        }
        int length = this.mStateSpecs.length;
        parcel.writeInt(length);
        for (int i2 = 0; i2 < length; i2++) {
            parcel.writeIntArray(this.mStateSpecs[i2]);
        }
        parcel.writeIntArray(this.mColors);
    }
}
