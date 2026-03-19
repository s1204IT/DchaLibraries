package android.content.res;

import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.content.res.XmlBlock;
import android.graphics.drawable.Drawable;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import com.android.internal.util.XmlUtils;
import dalvik.system.BlockGuard;
import java.util.Arrays;

public class TypedArray {
    private AssetManager mAssets;
    int[] mData;
    int[] mIndices;
    int mLength;
    private DisplayMetrics mMetrics;
    private boolean mRecycled;
    private final Resources mResources;
    Resources.Theme mTheme;
    TypedValue mValue = new TypedValue();
    XmlBlock.Parser mXml;

    static TypedArray obtain(Resources res, int len) {
        TypedArray attrs = (TypedArray) res.mTypedArrayPool.acquire();
        if (attrs != null) {
            attrs.mLength = len;
            attrs.mRecycled = false;
            attrs.mMetrics = res.getDisplayMetrics();
            attrs.mAssets = res.getAssets();
            int fullLen = len * 6;
            if (attrs.mData.length >= fullLen) {
                return attrs;
            }
            attrs.mData = new int[fullLen];
            attrs.mIndices = new int[len + 1];
            return attrs;
        }
        return new TypedArray(res, new int[len * 6], new int[len + 1], len);
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

    public int getIndex(int at) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mIndices[at + 1];
    }

    public Resources getResources() {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mResources;
    }

    public CharSequence getText(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return null;
        }
        if (type == 3) {
            return loadStringValueAt(index2);
        }
        TypedValue v = this.mValue;
        if (getValueAt(index2, v)) {
            return v.coerceToString();
        }
        throw new RuntimeException("getText of bad type: 0x" + Integer.toHexString(type));
    }

    public String getString(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return null;
        }
        if (type == 3) {
            return loadStringValueAt(index2).toString();
        }
        TypedValue v = this.mValue;
        if (getValueAt(index2, v)) {
            CharSequence cs = v.coerceToString();
            if (cs != null) {
                return cs.toString();
            }
            return null;
        }
        throw new RuntimeException("getString of bad type: 0x" + Integer.toHexString(type));
    }

    public String getNonResourceString(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 3) {
            int cookie = data[index2 + 2];
            if (cookie < 0) {
                return this.mXml.getPooledString(data[index2 + 1]).toString();
            }
            return null;
        }
        return null;
    }

    public String getNonConfigurationString(int index, int allowedChangingConfigs) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        int changingConfigs = ActivityInfo.activityInfoConfigNativeToJava(data[index2 + 4]);
        if (((~allowedChangingConfigs) & changingConfigs) != 0 || type == 0) {
            return null;
        }
        if (type == 3) {
            return loadStringValueAt(index2).toString();
        }
        TypedValue v = this.mValue;
        if (getValueAt(index2, v)) {
            CharSequence cs = v.coerceToString();
            if (cs != null) {
                return cs.toString();
            }
            return null;
        }
        throw new RuntimeException("getNonConfigurationString of bad type: 0x" + Integer.toHexString(type));
    }

    public boolean getBoolean(int index, boolean defValue) throws BlockGuard.BlockGuardPolicyException {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return defValue;
        }
        if (type >= 16 && type <= 31) {
            return data[index2 + 1] != 0;
        }
        TypedValue v = this.mValue;
        if (getValueAt(index2, v)) {
            StrictMode.noteResourceMismatch(v);
            return XmlUtils.convertValueToBoolean(v.coerceToString(), defValue);
        }
        throw new RuntimeException("getBoolean of bad type: 0x" + Integer.toHexString(type));
    }

    public int getInt(int index, int defValue) throws BlockGuard.BlockGuardPolicyException {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return defValue;
        }
        if (type >= 16 && type <= 31) {
            return data[index2 + 1];
        }
        TypedValue v = this.mValue;
        if (getValueAt(index2, v)) {
            StrictMode.noteResourceMismatch(v);
            return XmlUtils.convertValueToInt(v.coerceToString(), defValue);
        }
        throw new RuntimeException("getInt of bad type: 0x" + Integer.toHexString(type));
    }

    public float getFloat(int index, float defValue) {
        CharSequence str;
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 4) {
            return Float.intBitsToFloat(data[index2 + 1]);
        }
        if (type >= 16 && type <= 31) {
            return data[index2 + 1];
        }
        TypedValue v = this.mValue;
        if (getValueAt(index2, v) && (str = v.coerceToString()) != null) {
            StrictMode.noteResourceMismatch(v);
            return Float.parseFloat(str.toString());
        }
        throw new RuntimeException("getFloat of bad type: 0x" + Integer.toHexString(type));
    }

    public int getColor(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return defValue;
        }
        if (type >= 16 && type <= 31) {
            return data[index2 + 1];
        }
        if (type == 3) {
            TypedValue value = this.mValue;
            if (getValueAt(index2, value)) {
                ColorStateList csl = this.mResources.loadColorStateList(value, value.resourceId, this.mTheme);
                return csl.getDefaultColor();
            }
            return defValue;
        }
        if (type == 2) {
            TypedValue value2 = this.mValue;
            getValueAt(index2, value2);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value2);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + index + " to color: type=0x" + Integer.toHexString(type));
    }

    public ComplexColor getComplexColor(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (getValueAt(index * 6, value)) {
            if (value.type == 2) {
                throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
            }
            return this.mResources.loadComplexColor(value, value.resourceId, this.mTheme);
        }
        return null;
    }

    public ColorStateList getColorStateList(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (getValueAt(index * 6, value)) {
            if (value.type == 2) {
                throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
            }
            return this.mResources.loadColorStateList(value, value.resourceId, this.mTheme);
        }
        return null;
    }

    public int getInteger(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return defValue;
        }
        if (type >= 16 && type <= 31) {
            return data[index2 + 1];
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index2, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + index + " to integer: type=0x" + Integer.toHexString(type));
    }

    public float getDimension(int index, float defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 5) {
            return TypedValue.complexToDimension(data[index2 + 1], this.mMetrics);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index2, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + index + " to dimension: type=0x" + Integer.toHexString(type));
    }

    public int getDimensionPixelOffset(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 5) {
            return TypedValue.complexToDimensionPixelOffset(data[index2 + 1], this.mMetrics);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index2, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + index + " to dimension: type=0x" + Integer.toHexString(type));
    }

    public int getDimensionPixelSize(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 5) {
            return TypedValue.complexToDimensionPixelSize(data[index2 + 1], this.mMetrics);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index2, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + index + " to dimension: type=0x" + Integer.toHexString(type));
    }

    public int getLayoutDimension(int index, String name) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type >= 16 && type <= 31) {
            return data[index2 + 1];
        }
        if (type == 5) {
            return TypedValue.complexToDimensionPixelSize(data[index2 + 1], this.mMetrics);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index2, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
        }
        throw new UnsupportedOperationException(getPositionDescription() + ": You must supply a " + name + " attribute.");
    }

    public int getLayoutDimension(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type >= 16 && type <= 31) {
            return data[index2 + 1];
        }
        if (type == 5) {
            return TypedValue.complexToDimensionPixelSize(data[index2 + 1], this.mMetrics);
        }
        return defValue;
    }

    public float getFraction(int index, int base, int pbase, float defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type == 0) {
            return defValue;
        }
        if (type == 6) {
            return TypedValue.complexToFraction(data[index2 + 1], base, pbase);
        }
        if (type == 2) {
            TypedValue value = this.mValue;
            getValueAt(index2, value);
            throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
        }
        throw new UnsupportedOperationException("Can't convert value at index " + index + " to fraction: type=0x" + Integer.toHexString(type));
    }

    public int getResourceId(int index, int defValue) {
        int resid;
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        if (data[index2 + 0] != 0 && (resid = data[index2 + 3]) != 0) {
            return resid;
        }
        return defValue;
    }

    public int getThemeAttributeId(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        if (data[index2 + 0] == 2) {
            return data[index2 + 1];
        }
        return defValue;
    }

    public Drawable getDrawable(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (getValueAt(index * 6, value)) {
            if (value.type == 2) {
                throw new UnsupportedOperationException("Failed to resolve attribute at index " + index + ": " + value);
            }
            return this.mResources.loadDrawable(value, value.resourceId, this.mTheme);
        }
        return null;
    }

    public CharSequence[] getTextArray(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (getValueAt(index * 6, value)) {
            return this.mResources.getTextArray(value.resourceId);
        }
        return null;
    }

    public boolean getValue(int index, TypedValue outValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return getValueAt(index * 6, outValue);
    }

    public int getType(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        return this.mData[(index * 6) + 0];
    }

    public boolean hasValue(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int[] data = this.mData;
        int type = data[(index * 6) + 0];
        return type != 0;
    }

    public boolean hasValueOrEmpty(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        return type != 0 || data[index2 + 1] == 1;
    }

    public TypedValue peekValue(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (getValueAt(index * 6, value)) {
            return value;
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
        this.mMetrics = null;
        this.mAssets = null;
        this.mResources.mTypedArrayPool.release(this);
    }

    public int[] extractThemeAttrs() {
        return extractThemeAttrs(null);
    }

    public int[] extractThemeAttrs(int[] scrap) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int[] attrs = null;
        int[] data = this.mData;
        int N = length();
        for (int i = 0; i < N; i++) {
            int index = i * 6;
            if (data[index + 0] == 2) {
                data[index + 0] = 0;
                int attr = data[index + 1];
                if (attr != 0) {
                    if (attrs == null) {
                        if (scrap != null && scrap.length == N) {
                            attrs = scrap;
                            Arrays.fill(scrap, 0);
                        } else {
                            attrs = new int[N];
                        }
                    }
                    attrs[i] = attr;
                }
            }
        }
        return attrs;
    }

    public int getChangingConfigurations() {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int changingConfig = 0;
        int[] data = this.mData;
        int N = length();
        for (int i = 0; i < N; i++) {
            int index = i * 6;
            int type = data[index + 0];
            if (type != 0) {
                changingConfig |= ActivityInfo.activityInfoConfigNativeToJava(data[index + 4]);
            }
        }
        return changingConfig;
    }

    private boolean getValueAt(int index, TypedValue outValue) {
        int[] data = this.mData;
        int type = data[index + 0];
        if (type == 0) {
            return false;
        }
        outValue.type = type;
        outValue.data = data[index + 1];
        outValue.assetCookie = data[index + 2];
        outValue.resourceId = data[index + 3];
        outValue.changingConfigurations = ActivityInfo.activityInfoConfigNativeToJava(data[index + 4]);
        outValue.density = data[index + 5];
        outValue.string = type == 3 ? loadStringValueAt(index) : null;
        return true;
    }

    private CharSequence loadStringValueAt(int index) {
        int[] data = this.mData;
        int cookie = data[index + 2];
        if (cookie < 0) {
            if (this.mXml != null) {
                return this.mXml.getPooledString(data[index + 1]);
            }
            return null;
        }
        return this.mAssets.getPooledStringForCookie(cookie, data[index + 1]);
    }

    TypedArray(Resources resources, int[] data, int[] indices, int len) {
        this.mResources = resources;
        this.mMetrics = this.mResources.getDisplayMetrics();
        this.mAssets = this.mResources.getAssets();
        this.mData = data;
        this.mIndices = indices;
        this.mLength = len;
    }

    public String toString() {
        return Arrays.toString(this.mData);
    }
}
