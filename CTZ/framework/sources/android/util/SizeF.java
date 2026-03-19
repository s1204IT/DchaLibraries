package android.util;

import com.android.internal.util.Preconditions;

public final class SizeF {
    private final float mHeight;
    private final float mWidth;

    public SizeF(float f, float f2) {
        this.mWidth = Preconditions.checkArgumentFinite(f, "width");
        this.mHeight = Preconditions.checkArgumentFinite(f2, "height");
    }

    public float getWidth() {
        return this.mWidth;
    }

    public float getHeight() {
        return this.mHeight;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SizeF)) {
            return false;
        }
        SizeF sizeF = (SizeF) obj;
        if (this.mWidth != sizeF.mWidth || this.mHeight != sizeF.mHeight) {
            return false;
        }
        return true;
    }

    public String toString() {
        return this.mWidth + "x" + this.mHeight;
    }

    private static NumberFormatException invalidSizeF(String str) {
        throw new NumberFormatException("Invalid SizeF: \"" + str + "\"");
    }

    public static SizeF parseSizeF(String str) throws NumberFormatException {
        Preconditions.checkNotNull(str, "string must not be null");
        int iIndexOf = str.indexOf(42);
        if (iIndexOf < 0) {
            iIndexOf = str.indexOf(120);
        }
        if (iIndexOf < 0) {
            throw invalidSizeF(str);
        }
        try {
            return new SizeF(Float.parseFloat(str.substring(0, iIndexOf)), Float.parseFloat(str.substring(iIndexOf + 1)));
        } catch (NumberFormatException e) {
            throw invalidSizeF(str);
        } catch (IllegalArgumentException e2) {
            throw invalidSizeF(str);
        }
    }

    public int hashCode() {
        return Float.floatToIntBits(this.mWidth) ^ Float.floatToIntBits(this.mHeight);
    }
}
