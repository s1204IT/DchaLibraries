package android.content.res;

import android.content.res.Resources;
import android.content.res.XmlBlock;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import com.android.internal.util.XmlUtils;
import java.util.Arrays;

public class TypedArray {
    private final AssetManager mAssets;
    int[] mData;
    int[] mIndices;
    int mLength;
    private final DisplayMetrics mMetrics;
    private boolean mRecycled;
    private final Resources mResources;
    Resources.Theme mTheme;
    TypedValue mValue = new TypedValue();
    XmlBlock.Parser mXml;

    static TypedArray obtain(Resources res, int len) {
        TypedArray attrs = res.mTypedArrayPool.acquire();
        if (attrs != null) {
            attrs.mLength = len;
            attrs.mRecycled = false;
            int fullLen = len * 6;
            if (attrs.mData.length < fullLen) {
                attrs.mData = new int[fullLen];
                attrs.mIndices = new int[len + 1];
                return attrs;
            }
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
            Log.w("Resources", "Converting to string: " + v);
            return v.coerceToString();
        }
        Log.w("Resources", "getString of bad type: 0x" + Integer.toHexString(type));
        return null;
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
            Log.w("Resources", "Converting to string: " + v);
            CharSequence cs = v.coerceToString();
            if (cs != null) {
                return cs.toString();
            }
            return null;
        }
        Log.w("Resources", "getString of bad type: 0x" + Integer.toHexString(type));
        return null;
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
        if ((data[index2 + 4] & (allowedChangingConfigs ^ (-1))) != 0 || type == 0) {
            return null;
        }
        if (type == 3) {
            return loadStringValueAt(index2).toString();
        }
        TypedValue v = this.mValue;
        if (getValueAt(index2, v)) {
            Log.w("Resources", "Converting to string: " + v);
            CharSequence cs = v.coerceToString();
            if (cs != null) {
                return cs.toString();
            }
            return null;
        }
        Log.w("Resources", "getString of bad type: 0x" + Integer.toHexString(type));
        return null;
    }

    public boolean getBoolean(int index, boolean defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type != 0) {
            if (type >= 16 && type <= 31) {
                return data[index2 + 1] != 0;
            }
            TypedValue v = this.mValue;
            if (getValueAt(index2, v)) {
                Log.w("Resources", "Converting to boolean: " + v);
                return XmlUtils.convertValueToBoolean(v.coerceToString(), defValue);
            }
            Log.w("Resources", "getBoolean of bad type: 0x" + Integer.toHexString(type));
            return defValue;
        }
        return defValue;
    }

    public int getInt(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type != 0) {
            if (type >= 16 && type <= 31) {
                return data[index2 + 1];
            }
            TypedValue v = this.mValue;
            if (getValueAt(index2, v)) {
                Log.w("Resources", "Converting to int: " + v);
                return XmlUtils.convertValueToInt(v.coerceToString(), defValue);
            }
            Log.w("Resources", "getInt of bad type: 0x" + Integer.toHexString(type));
            return defValue;
        }
        return defValue;
    }

    public float getFloat(int index, float defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type != 0) {
            if (type == 4) {
                return Float.intBitsToFloat(data[index2 + 1]);
            }
            if (type >= 16 && type <= 31) {
                return data[index2 + 1];
            }
            TypedValue v = this.mValue;
            if (getValueAt(index2, v)) {
                Log.w("Resources", "Converting to float: " + v);
                CharSequence str = v.coerceToString();
                if (str != null) {
                    return Float.parseFloat(str.toString());
                }
            }
            Log.w("Resources", "getFloat of bad type: 0x" + Integer.toHexString(type));
            return defValue;
        }
        return defValue;
    }

    public int getColor(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type != 0) {
            if (type >= 16 && type <= 31) {
                return data[index2 + 1];
            }
            if (type == 3) {
                TypedValue value = this.mValue;
                if (getValueAt(index2, value)) {
                    ColorStateList csl = this.mResources.loadColorStateList(value, value.resourceId);
                    return csl.getDefaultColor();
                }
                return defValue;
            }
            if (type == 2) {
                throw new RuntimeException("Failed to resolve attribute at index " + index2);
            }
            throw new UnsupportedOperationException("Can't convert to color: type=0x" + Integer.toHexString(type));
        }
        return defValue;
    }

    public ColorStateList getColorStateList(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (!getValueAt(index * 6, value)) {
            return null;
        }
        if (value.type == 2) {
            throw new RuntimeException("Failed to resolve attribute at index " + index);
        }
        return this.mResources.loadColorStateList(value, value.resourceId);
    }

    public int getInteger(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type != 0) {
            if (type >= 16 && type <= 31) {
                int defValue2 = data[index2 + 1];
                return defValue2;
            }
            if (type == 2) {
                throw new RuntimeException("Failed to resolve attribute at index " + index2);
            }
            throw new UnsupportedOperationException("Can't convert to integer: type=0x" + Integer.toHexString(type));
        }
        return defValue;
    }

    public float getDimension(int index, float defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type != 0) {
            if (type == 5) {
                float defValue2 = TypedValue.complexToDimension(data[index2 + 1], this.mMetrics);
                return defValue2;
            }
            if (type == 2) {
                throw new RuntimeException("Failed to resolve attribute at index " + index2);
            }
            throw new UnsupportedOperationException("Can't convert to dimension: type=0x" + Integer.toHexString(type));
        }
        return defValue;
    }

    public int getDimensionPixelOffset(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type != 0) {
            if (type == 5) {
                int defValue2 = TypedValue.complexToDimensionPixelOffset(data[index2 + 1], this.mMetrics);
                return defValue2;
            }
            if (type == 2) {
                throw new RuntimeException("Failed to resolve attribute at index " + index2);
            }
            throw new UnsupportedOperationException("Can't convert to dimension: type=0x" + Integer.toHexString(type));
        }
        return defValue;
    }

    public int getDimensionPixelSize(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type != 0) {
            if (type == 5) {
                int defValue2 = TypedValue.complexToDimensionPixelSize(data[index2 + 1], this.mMetrics);
                return defValue2;
            }
            if (type == 2) {
                throw new RuntimeException("Failed to resolve attribute at index " + index2);
            }
            throw new UnsupportedOperationException("Can't convert to dimension: type=0x" + Integer.toHexString(type));
        }
        return defValue;
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
            throw new RuntimeException("Failed to resolve attribute at index " + index2);
        }
        throw new RuntimeException(getPositionDescription() + ": You must supply a " + name + " attribute.");
    }

    public int getLayoutDimension(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        int type = data[index2 + 0];
        if (type >= 16 && type <= 31) {
            int defValue2 = data[index2 + 1];
            return defValue2;
        }
        if (type == 5) {
            int defValue3 = TypedValue.complexToDimensionPixelSize(data[index2 + 1], this.mMetrics);
            return defValue3;
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
        if (type != 0) {
            if (type == 6) {
                float defValue2 = TypedValue.complexToFraction(data[index2 + 1], base, pbase);
                return defValue2;
            }
            if (type == 2) {
                throw new RuntimeException("Failed to resolve attribute at index " + index2);
            }
            throw new UnsupportedOperationException("Can't convert to fraction: type=0x" + Integer.toHexString(type));
        }
        return defValue;
    }

    public int getResourceId(int index, int defValue) {
        int resid;
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        return (data[index2 + 0] == 0 || (resid = data[index2 + 3]) == 0) ? defValue : resid;
    }

    public int getThemeAttributeId(int index, int defValue) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        int index2 = index * 6;
        int[] data = this.mData;
        if (data[index2 + 0] == 2) {
            int defValue2 = data[index2 + 1];
            return defValue2;
        }
        return defValue;
    }

    public Drawable getDrawable(int index) {
        if (this.mRecycled) {
            throw new RuntimeException("Cannot make calls to a recycled instance!");
        }
        TypedValue value = this.mValue;
        if (!getValueAt(index * 6, value)) {
            return null;
        }
        if (value.type == 2) {
            throw new RuntimeException("Failed to resolve attribute at index " + index);
        }
        return this.mResources.loadDrawable(value, value.resourceId, this.mTheme);
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
        this.mResources.mTypedArrayPool.release(this);
    }

    public int[] extractThemeAttrs() {
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
                        attrs = new int[N];
                    }
                    attrs[i] = attr;
                }
            }
        }
        return attrs;
    }

    public int getChangingConfigurations() {
        int changingConfig = 0;
        int[] data = this.mData;
        int N = length();
        for (int i = 0; i < N; i++) {
            int index = i * 6;
            int type = data[index + 0];
            if (type != 0) {
                changingConfig |= data[index + 4];
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
        outValue.changingConfigurations = data[index + 4];
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
        this.mMetrics = this.mResources.mMetrics;
        this.mAssets = this.mResources.mAssets;
        this.mData = data;
        this.mIndices = indices;
        this.mLength = len;
    }

    public String toString() {
        return Arrays.toString(this.mData);
    }
}
