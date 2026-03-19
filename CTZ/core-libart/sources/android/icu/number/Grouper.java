package android.icu.number;

import android.icu.impl.number.DecimalQuantity;
import android.icu.impl.number.PatternStringParser;

@Deprecated
public class Grouper {
    static final boolean $assertionsDisabled = false;
    private static final byte B2 = 2;
    private static final byte B3 = 3;
    private static final byte N1 = -1;
    private final byte grouping1;
    private final byte grouping2;
    private final boolean min2;
    private static final byte N2 = -2;
    private static final Grouper DEFAULTS = new Grouper(N2, N2, false);
    private static final Grouper MIN2 = new Grouper(N2, N2, true);
    private static final Grouper NONE = new Grouper((byte) -1, (byte) -1, false);
    private static final Grouper GROUPING_3 = new Grouper((byte) 3, (byte) 3, false);
    private static final Grouper GROUPING_3_2 = new Grouper((byte) 3, (byte) 2, false);
    private static final Grouper GROUPING_3_MIN2 = new Grouper((byte) 3, (byte) 3, true);
    private static final Grouper GROUPING_3_2_MIN2 = new Grouper((byte) 3, (byte) 2, true);

    private Grouper(byte b, byte b2, boolean z) {
        this.grouping1 = b;
        this.grouping2 = b2;
        this.min2 = z;
    }

    @Deprecated
    public static Grouper defaults() {
        return DEFAULTS;
    }

    @Deprecated
    public static Grouper minTwoDigits() {
        return MIN2;
    }

    @Deprecated
    public static Grouper none() {
        return NONE;
    }

    static Grouper getInstance(byte b, byte b2, boolean z) {
        if (b == -1) {
            return NONE;
        }
        if (z || b != 3 || b2 != 3) {
            if (!z && b == 3 && b2 == 2) {
                return GROUPING_3_2;
            }
            if (z && b == 3 && b2 == 3) {
                return GROUPING_3_MIN2;
            }
            if (z && b == 3 && b2 == 2) {
                return GROUPING_3_2_MIN2;
            }
            return new Grouper(b, b2, z);
        }
        return GROUPING_3;
    }

    Grouper withLocaleData(PatternStringParser.ParsedPatternInfo parsedPatternInfo) {
        if (this.grouping1 != -2) {
            return this;
        }
        byte b = (byte) (parsedPatternInfo.positive.groupingSizes & 65535);
        byte b2 = (byte) ((parsedPatternInfo.positive.groupingSizes >>> 16) & 65535);
        byte b3 = (byte) (65535 & (parsedPatternInfo.positive.groupingSizes >>> 32));
        if (b2 == -1) {
            b = -1;
        }
        if (b3 == -1) {
            b2 = b;
        }
        return getInstance(b, b2, this.min2);
    }

    boolean groupAtPosition(int i, DecimalQuantity decimalQuantity) {
        if (this.grouping1 == -1 || this.grouping1 == 0) {
            return false;
        }
        int i2 = i - this.grouping1;
        if (i2 >= 0 && i2 % this.grouping2 == 0) {
            if ((decimalQuantity.getUpperDisplayMagnitude() - this.grouping1) + 1 >= (this.min2 ? 2 : 1)) {
                return true;
            }
        }
        return false;
    }
}
