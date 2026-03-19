package android.icu.impl.number;

import java.math.MathContext;
import java.math.RoundingMode;

public class RoundingUtils {
    public static final int MAX_INT_FRAC_SIG = 100;
    public static final int SECTION_LOWER = 1;
    public static final int SECTION_MIDPOINT = 2;
    public static final int SECTION_UPPER = 3;
    public static final RoundingMode DEFAULT_ROUNDING_MODE = RoundingMode.HALF_EVEN;
    private static final MathContext[] MATH_CONTEXT_BY_ROUNDING_MODE_UNLIMITED = new MathContext[RoundingMode.values().length];
    private static final MathContext[] MATH_CONTEXT_BY_ROUNDING_MODE_34_DIGITS = new MathContext[RoundingMode.values().length];

    static {
        for (int i = 0; i < MATH_CONTEXT_BY_ROUNDING_MODE_34_DIGITS.length; i++) {
            MATH_CONTEXT_BY_ROUNDING_MODE_UNLIMITED[i] = new MathContext(0, RoundingMode.valueOf(i));
            MATH_CONTEXT_BY_ROUNDING_MODE_34_DIGITS[i] = new MathContext(34);
        }
    }

    public static boolean getRoundingDirection(boolean z, boolean z2, int i, int i2, Object obj) {
        switch (i2) {
            case 0:
                return false;
            case 1:
                return true;
            case 2:
                return z2;
            case 3:
                return !z2;
            case 4:
                switch (i) {
                    case 1:
                        return true;
                    case 2:
                        return false;
                    case 3:
                        return false;
                }
            case 5:
                switch (i) {
                    case 1:
                        return true;
                    case 2:
                        return true;
                    case 3:
                        return false;
                }
            case 6:
                switch (i) {
                    case 1:
                        return true;
                    case 2:
                        return z;
                    case 3:
                        return false;
                }
        }
        throw new ArithmeticException("Rounding is required on " + obj.toString());
    }

    public static boolean roundsAtMidpoint(int i) {
        switch (i) {
            case 0:
            case 1:
            case 2:
            case 3:
                return false;
            default:
                return true;
        }
    }

    public static MathContext getMathContextOrUnlimited(DecimalFormatProperties decimalFormatProperties) {
        MathContext mathContext = decimalFormatProperties.getMathContext();
        if (mathContext == null) {
            RoundingMode roundingMode = decimalFormatProperties.getRoundingMode();
            if (roundingMode == null) {
                roundingMode = RoundingMode.HALF_EVEN;
            }
            return MATH_CONTEXT_BY_ROUNDING_MODE_UNLIMITED[roundingMode.ordinal()];
        }
        return mathContext;
    }

    public static MathContext getMathContextOr34Digits(DecimalFormatProperties decimalFormatProperties) {
        MathContext mathContext = decimalFormatProperties.getMathContext();
        if (mathContext == null) {
            RoundingMode roundingMode = decimalFormatProperties.getRoundingMode();
            if (roundingMode == null) {
                roundingMode = RoundingMode.HALF_EVEN;
            }
            return MATH_CONTEXT_BY_ROUNDING_MODE_34_DIGITS[roundingMode.ordinal()];
        }
        return mathContext;
    }

    public static MathContext mathContextUnlimited(RoundingMode roundingMode) {
        return MATH_CONTEXT_BY_ROUNDING_MODE_UNLIMITED[roundingMode.ordinal()];
    }
}
