package android.hardware.camera2.params;

import android.hardware.camera2.utils.HashCodeHelpers;
import android.util.Rational;
import com.android.internal.util.Preconditions;
import java.util.Arrays;

public final class ColorSpaceTransform {
    private static final int COLUMNS = 3;
    private static final int COUNT = 9;
    private static final int COUNT_INT = 18;
    private static final int OFFSET_DENOMINATOR = 1;
    private static final int OFFSET_NUMERATOR = 0;
    private static final int RATIONAL_SIZE = 2;
    private static final int ROWS = 3;
    private final int[] mElements;

    public ColorSpaceTransform(Rational[] rationalArr) {
        Preconditions.checkNotNull(rationalArr, "elements must not be null");
        if (rationalArr.length != 9) {
            throw new IllegalArgumentException("elements must be 9 length");
        }
        this.mElements = new int[18];
        for (int i = 0; i < rationalArr.length; i++) {
            Preconditions.checkNotNull(rationalArr, "element[" + i + "] must not be null");
            int i2 = i * 2;
            this.mElements[i2 + 0] = rationalArr[i].getNumerator();
            this.mElements[i2 + 1] = rationalArr[i].getDenominator();
        }
    }

    public ColorSpaceTransform(int[] iArr) {
        Preconditions.checkNotNull(iArr, "elements must not be null");
        if (iArr.length != 18) {
            throw new IllegalArgumentException("elements must be 18 length");
        }
        for (int i = 0; i < iArr.length; i++) {
            Preconditions.checkNotNull(iArr, "element " + i + " must not be null");
        }
        this.mElements = Arrays.copyOf(iArr, iArr.length);
    }

    public Rational getElement(int i, int i2) {
        if (i < 0 || i >= 3) {
            throw new IllegalArgumentException("column out of range");
        }
        if (i2 < 0 || i2 >= 3) {
            throw new IllegalArgumentException("row out of range");
        }
        int i3 = ((i2 * 3) + i) * 2;
        return new Rational(this.mElements[i3 + 0], this.mElements[i3 + 1]);
    }

    public void copyElements(Rational[] rationalArr, int i) {
        Preconditions.checkArgumentNonnegative(i, "offset must not be negative");
        Preconditions.checkNotNull(rationalArr, "destination must not be null");
        if (rationalArr.length - i < 9) {
            throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
        }
        int i2 = 0;
        int i3 = 0;
        while (i2 < 9) {
            rationalArr[i2 + i] = new Rational(this.mElements[i3 + 0], this.mElements[i3 + 1]);
            i2++;
            i3 += 2;
        }
    }

    public void copyElements(int[] iArr, int i) {
        Preconditions.checkArgumentNonnegative(i, "offset must not be negative");
        Preconditions.checkNotNull(iArr, "destination must not be null");
        if (iArr.length - i < 18) {
            throw new ArrayIndexOutOfBoundsException("destination too small to fit elements");
        }
        for (int i2 = 0; i2 < 18; i2++) {
            iArr[i2 + i] = this.mElements[i2];
        }
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ColorSpaceTransform)) {
            return false;
        }
        ColorSpaceTransform colorSpaceTransform = (ColorSpaceTransform) obj;
        int i = 0;
        int i2 = 0;
        while (i < 9) {
            int i3 = i2 + 0;
            int i4 = i2 + 1;
            if (!new Rational(this.mElements[i3], this.mElements[i4]).equals((Object) new Rational(colorSpaceTransform.mElements[i3], colorSpaceTransform.mElements[i4]))) {
                return false;
            }
            i++;
            i2 += 2;
        }
        return true;
    }

    public int hashCode() {
        return HashCodeHelpers.hashCode(this.mElements);
    }

    public String toString() {
        return String.format("ColorSpaceTransform%s", toShortString());
    }

    private String toShortString() {
        StringBuilder sb = new StringBuilder("(");
        int i = 0;
        int i2 = 0;
        while (i < 3) {
            sb.append("[");
            int i3 = i2;
            int i4 = 0;
            while (i4 < 3) {
                int i5 = this.mElements[i3 + 0];
                int i6 = this.mElements[i3 + 1];
                sb.append(i5);
                sb.append("/");
                sb.append(i6);
                if (i4 < 2) {
                    sb.append(", ");
                }
                i4++;
                i3 += 2;
            }
            sb.append("]");
            if (i < 2) {
                sb.append(", ");
            }
            i++;
            i2 = i3;
        }
        sb.append(")");
        return sb.toString();
    }
}
