package android.content.res;

import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.XmlBlock;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import com.android.internal.util.XmlUtils;
import dalvik.system.VMRuntime;
import java.util.Arrays;

public class TypedArray {
    static final int STYLE_ASSET_COOKIE = 2;
    static final int STYLE_CHANGING_CONFIGURATIONS = 4;
    static final int STYLE_DATA = 1;
    static final int STYLE_DENSITY = 5;
    static final int STYLE_NUM_ENTRIES = 6;
    static final int STYLE_RESOURCE_ID = 3;
    static final int STYLE_TYPE = 0;
    private AssetManager mAssets;
    int[] mData;
    long mDataAddress;
    int[] mIndices;
    long mIndicesAddress;
    int mLength;
    private DisplayMetrics mMetrics;
    private boolean mRecycled;
    private final Resources mResources;
    Resources.Theme mTheme;
    TypedValue mValue = new TypedValue();
    XmlBlock.Parser mXml;

    static TypedArray obtain(Resources resources, int i) {
        TypedArray typedArrayAcquire = resources.mTypedArrayPool.acquire();
        if (typedArrayAcquire == null) {
            typedArrayAcquire = new TypedArray(resources);
        }
        typedArrayAcquire.mRecycled = false;
        typedArrayAcquire.mAssets = resources.getAssets();
        typedArrayAcquire.mMetrics = resources.getDisplayMetrics();
        typedArrayAcquire.resize(i);
        return typedArrayAcquire;
    }

    private void resize(int i) {
        this.mLength = i;
        int i2 = i * 6;
        int i3 = i + 1;
        VMRuntime runtime = VMRuntime.getRuntime();
        if (this.mDataAddress == 0 || this.mData.length < i2) {
            this.mData = (int[]) runtime.newNonMovableArray(Integer.TYPE, i2);
            this.mDataAddress = runtime.addressOf(this.mData);
            this.mIndices = (int[]) runtime.newNonMovableArray(Integer.TYPE, i3);
            this.mIndicesAddress = runtime.addressOf(this.mIndices);
        }
    }

    public int length() {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mLength;
    }

    public int getIndexCount() {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mIndices[0];
    }

    public int getIndex(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mIndices[1 + i];
    }

    public Resources getResources() {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mResources;
    }

    public CharSequence getText(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i2 = i * 6;
        int i3 = this.mData[i2 + 0];
        if (i3 == 0) {
            return null;
        }
        if (i3 == 3) {
            return loadStringValueAt(i2);
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i2, typedValue)) {
            return typedValue.coerceToString();
        }
        throw new RuntimeException("getText of bad type: 0x" + Integer.toHexString(i3));
    }

    public String getString(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i2 = i * 6;
        int i3 = this.mData[i2 + 0];
        if (i3 == 0) {
            return null;
        }
        if (i3 == 3) {
            return loadStringValueAt(i2).toString();
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i2, typedValue)) {
            CharSequence charSequenceCoerceToString = typedValue.coerceToString();
            if (charSequenceCoerceToString != null) {
                return charSequenceCoerceToString.toString();
            }
            return null;
        }
        throw new RuntimeException("getString of bad type: 0x" + Integer.toHexString(i3));
    }

    public String getNonResourceString(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i2 = i * 6;
        int[] iArr = this.mData;
        if (iArr[i2 + 0] == 3 && iArr[i2 + 2] < 0) {
            return this.mXml.getPooledString(iArr[i2 + 1]).toString();
        }
        return null;
    }

    public String getNonConfigurationString(int i, int i2) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i3 = i * 6;
        int[] iArr = this.mData;
        int i4 = iArr[i3 + 0];
        if (((~i2) & ActivityInfo.activityInfoConfigNativeToJava(iArr[i3 + 4])) != 0 || i4 == 0) {
            return null;
        }
        if (i4 == 3) {
            return loadStringValueAt(i3).toString();
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i3, typedValue)) {
            CharSequence charSequenceCoerceToString = typedValue.coerceToString();
            if (charSequenceCoerceToString != null) {
                return charSequenceCoerceToString.toString();
            }
            return null;
        }
        throw new RuntimeException("getNonConfigurationString of bad type: 0x" + Integer.toHexString(i4));
    }

    public boolean getBoolean(int i, boolean z) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i2 = i * 6;
        int[] iArr = this.mData;
        int i3 = iArr[i2 + 0];
        if (i3 == 0) {
            return z;
        }
        if (i3 >= 16 && i3 <= 31) {
            return iArr[i2 + 1] != 0;
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i2, typedValue)) {
            StrictMode.noteResourceMismatch(typedValue);
            return XmlUtils.convertValueToBoolean(typedValue.coerceToString(), z);
        }
        throw new RuntimeException("getBoolean of bad type: 0x" + Integer.toHexString(i3));
    }

    public int getInt(int i, int i2) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i3 = i * 6;
        int[] iArr = this.mData;
        int i4 = iArr[i3 + 0];
        if (i4 == 0) {
            return i2;
        }
        if (i4 >= 16 && i4 <= 31) {
            return iArr[i3 + 1];
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i3, typedValue)) {
            StrictMode.noteResourceMismatch(typedValue);
            return XmlUtils.convertValueToInt(typedValue.coerceToString(), i2);
        }
        throw new RuntimeException("getInt of bad type: 0x" + Integer.toHexString(i4));
    }

    public float getFloat(int i, float f) {
        CharSequence charSequenceCoerceToString;
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i2 = i * 6;
        int[] iArr = this.mData;
        int i3 = iArr[i2 + 0];
        if (i3 == 0) {
            return f;
        }
        if (i3 == 4) {
            return Float.intBitsToFloat(iArr[i2 + 1]);
        }
        if (i3 >= 16 && i3 <= 31) {
            return iArr[i2 + 1];
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i2, typedValue) && (charSequenceCoerceToString = typedValue.coerceToString()) != null) {
            StrictMode.noteResourceMismatch(typedValue);
            return Float.parseFloat(charSequenceCoerceToString.toString());
        }
        throw new RuntimeException("getFloat of bad type: 0x" + Integer.toHexString(i3));
    }

    public int getColor(int i, int i2) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i3 = i * 6;
        int[] iArr = this.mData;
        int i4 = iArr[i3 + 0];
        if (i4 == 0) {
            return i2;
        }
        if (i4 >= 16 && i4 <= 31) {
            return iArr[i3 + 1];
        }
        if (i4 == 3) {
            TypedValue typedValue = this.mValue;
            if (getValueAt(i3, typedValue)) {
                return this.mResources.loadColorStateList(typedValue, typedValue.resourceId, this.mTheme).getDefaultColor();
            }
            return i2;
        }
        if (i4 == 2) {
            TypedValue typedValue2 = this.mValue;
            getValueAt(i3, typedValue2);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue2);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + i + " to color: type=0x" + Integer.toHexString(i4));
    }

    public ComplexColor getComplexColor(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i * 6, typedValue)) {
            if (typedValue.type == 2) {
                throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
            }
            return this.mResources.loadComplexColor(typedValue, typedValue.resourceId, this.mTheme);
        }
        return null;
    }

    public ColorStateList getColorStateList(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i * 6, typedValue)) {
            if (typedValue.type == 2) {
                throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
            }
            return this.mResources.loadColorStateList(typedValue, typedValue.resourceId, this.mTheme);
        }
        return null;
    }

    public int getInteger(int i, int i2) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i3 = i * 6;
        int[] iArr = this.mData;
        int i4 = iArr[i3 + 0];
        if (i4 == 0) {
            return i2;
        }
        if (i4 >= 16 && i4 <= 31) {
            return iArr[i3 + 1];
        }
        if (i4 == 2) {
            TypedValue typedValue = this.mValue;
            getValueAt(i3, typedValue);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + i + " to integer: type=0x" + Integer.toHexString(i4));
    }

    public float getDimension(int i, float f) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i2 = i * 6;
        int[] iArr = this.mData;
        int i3 = iArr[i2 + 0];
        if (i3 == 0) {
            return f;
        }
        if (i3 == 5) {
            return TypedValue.complexToDimension(iArr[i2 + 1], this.mMetrics);
        }
        if (i3 == 2) {
            TypedValue typedValue = this.mValue;
            getValueAt(i2, typedValue);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + i + " to dimension: type=0x" + Integer.toHexString(i3));
    }

    public int getDimensionPixelOffset(int i, int i2) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i3 = i * 6;
        int[] iArr = this.mData;
        int i4 = iArr[i3 + 0];
        if (i4 == 0) {
            return i2;
        }
        if (i4 == 5) {
            return TypedValue.complexToDimensionPixelOffset(iArr[i3 + 1], this.mMetrics);
        }
        if (i4 == 2) {
            TypedValue typedValue = this.mValue;
            getValueAt(i3, typedValue);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + i + " to dimension: type=0x" + Integer.toHexString(i4));
    }

    public int getDimensionPixelSize(int i, int i2) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i3 = i * 6;
        int[] iArr = this.mData;
        int i4 = iArr[i3 + 0];
        if (i4 == 0) {
            return i2;
        }
        if (i4 == 5) {
            return TypedValue.complexToDimensionPixelSize(iArr[i3 + 1], this.mMetrics);
        }
        if (i4 == 2) {
            TypedValue typedValue = this.mValue;
            getValueAt(i3, typedValue);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + i + " to dimension: type=0x" + Integer.toHexString(i4));
    }

    public int getLayoutDimension(int i, String str) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i2 = i * 6;
        int[] iArr = this.mData;
        int i3 = iArr[i2 + 0];
        if (i3 >= 16 && i3 <= 31) {
            return iArr[i2 + 1];
        }
        if (i3 == 5) {
            return TypedValue.complexToDimensionPixelSize(iArr[i2 + 1], this.mMetrics);
        }
        if (i3 == 2) {
            TypedValue typedValue = this.mValue;
            getValueAt(i2, typedValue);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
        }
        throw new UnsupportedOperationException(getPositionDescription() + ": You must supply a " + str + " attribute.");
    }

    public int getLayoutDimension(int i, int i2) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i3 = i * 6;
        int[] iArr = this.mData;
        int i4 = iArr[i3 + 0];
        if (i4 >= 16 && i4 <= 31) {
            return iArr[i3 + 1];
        }
        if (i4 == 5) {
            return TypedValue.complexToDimensionPixelSize(iArr[i3 + 1], this.mMetrics);
        }
        return i2;
    }

    public float getFraction(int i, int i2, int i3, float f) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i4 = i * 6;
        int[] iArr = this.mData;
        int i5 = iArr[i4 + 0];
        if (i5 == 0) {
            return f;
        }
        if (i5 == 6) {
            return TypedValue.complexToFraction(iArr[i4 + 1], i2, i3);
        }
        if (i5 == 2) {
            TypedValue typedValue = this.mValue;
            getValueAt(i4, typedValue);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + i + " to fraction: type=0x" + Integer.toHexString(i5));
    }

    public int getResourceId(int i, int i2) {
        int i3;
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i4 = i * 6;
        int[] iArr = this.mData;
        if (iArr[i4 + 0] != 0 && (i3 = iArr[i4 + 3]) != 0) {
            return i3;
        }
        return i2;
    }

    public int getThemeAttributeId(int i, int i2) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i3 = i * 6;
        int[] iArr = this.mData;
        if (iArr[i3 + 0] == 2) {
            return iArr[i3 + 1];
        }
        return i2;
    }

    public Drawable getDrawable(int i) {
        return getDrawableForDensity(i, 0);
    }

    public Drawable getDrawableForDensity(int i, int i2) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i * 6, typedValue)) {
            if (typedValue.type == 2) {
                throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
            }
            if (i2 > 0) {
                this.mResources.getValueForDensity(typedValue.resourceId, i2, typedValue, true);
            }
            return this.mResources.loadDrawable(typedValue, typedValue.resourceId, i2, this.mTheme);
        }
        return null;
    }

    public Typeface getFont(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i * 6, typedValue)) {
            if (typedValue.type == 2) {
                throw new UnsupportedOperationException("Failed to resolve attribute at index " + i + ": " + typedValue);
            }
            return this.mResources.getFont(typedValue, typedValue.resourceId);
        }
        return null;
    }

    public CharSequence[] getTextArray(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i * 6, typedValue)) {
            return this.mResources.getTextArray(typedValue.resourceId);
        }
        return null;
    }

    public boolean getValue(int i, TypedValue typedValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return getValueAt(i * 6, typedValue);
    }

    public int getType(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mData[(i * 6) + 0];
    }

    public boolean hasValue(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mData[(i * 6) + 0] != 0;
    }

    public boolean hasValueOrEmpty(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int i2 = i * 6;
        int[] iArr = this.mData;
        return iArr[i2 + 0] != 0 || iArr[i2 + 1] == 1;
    }

    public TypedValue peekValue(int i) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue typedValue = this.mValue;
        if (getValueAt(i * 6, typedValue)) {
            return typedValue;
        }
        return null;
    }

    public String getPositionDescription() {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mXml != null ? this.mXml.getPositionDescription() : "<internal>";
    }

    public void recycle() {
        if (this.mRecycled) {
            throw new RuntimeException(toString() + " recycled twice!");
        }
        this.mRecycled = true;
        this.mXml = null;
        this.mTheme = null;
        this.mAssets = null;
        this.mResources.mTypedArrayPool.release(this);
    }

    public int[] extractThemeAttrs() {
        return extractThemeAttrs(null);
    }

    public int[] extractThemeAttrs(int[] iArr) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int[] iArr2 = this.mData;
        int length = length();
        int[] iArr3 = null;
        for (int i = 0; i < length; i++) {
            int i2 = i * 6;
            int i3 = i2 + 0;
            if (iArr2[i3] == 2) {
                iArr2[i3] = 0;
                int i4 = iArr2[i2 + 1];
                if (i4 != 0) {
                    if (iArr3 == null) {
                        if (iArr != null && iArr.length == length) {
                            Arrays.fill(iArr, 0);
                            iArr3 = iArr;
                        } else {
                            iArr3 = new int[length];
                        }
                    }
                    iArr3[i] = i4;
                }
            }
        }
        return iArr3;
    }

    public int getChangingConfigurations() {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int[] iArr = this.mData;
        int length = length();
        int iActivityInfoConfigNativeToJava = 0;
        for (int i = 0; i < length; i++) {
            int i2 = i * 6;
            if (iArr[i2 + 0] != 0) {
                iActivityInfoConfigNativeToJava |= ActivityInfo.activityInfoConfigNativeToJava(iArr[i2 + 4]);
            }
        }
        return iActivityInfoConfigNativeToJava;
    }

    private boolean getValueAt(int i, TypedValue typedValue) {
        int[] iArr = this.mData;
        int i2 = iArr[i + 0];
        if (i2 == 0) {
            return false;
        }
        typedValue.type = i2;
        typedValue.data = iArr[i + 1];
        typedValue.assetCookie = iArr[i + 2];
        typedValue.resourceId = iArr[i + 3];
        typedValue.changingConfigurations = ActivityInfo.activityInfoConfigNativeToJava(iArr[i + 4]);
        typedValue.density = iArr[i + 5];
        typedValue.string = i2 == 3 ? loadStringValueAt(i) : null;
        return true;
    }

    private CharSequence loadStringValueAt(int i) {
        int[] iArr = this.mData;
        int i2 = iArr[i + 2];
        if (i2 < 0) {
            if (this.mXml != null) {
                return this.mXml.getPooledString(iArr[i + 1]);
            }
            return null;
        }
        return this.mAssets.getPooledStringForCookie(i2, iArr[i + 1]);
    }

    protected TypedArray(Resources resources) {
        this.mResources = resources;
        this.mMetrics = this.mResources.getDisplayMetrics();
        this.mAssets = this.mResources.getAssets();
    }

    public String toString() {
        return Arrays.toString(this.mData);
    }
}
