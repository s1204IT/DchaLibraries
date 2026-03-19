package java.math;

import android.icu.text.PluralRules;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.io.StreamCorruptedException;

public final class MathContext implements Serializable {
    public static final MathContext DECIMAL128 = new MathContext(34, RoundingMode.HALF_EVEN);
    public static final MathContext DECIMAL32 = new MathContext(7, RoundingMode.HALF_EVEN);
    public static final MathContext DECIMAL64 = new MathContext(16, RoundingMode.HALF_EVEN);
    public static final MathContext UNLIMITED = new MathContext(0, RoundingMode.HALF_UP);
    private static final long serialVersionUID = 5579720004786848255L;
    private final int precision;
    private final RoundingMode roundingMode;

    public MathContext(int i) {
        this(i, RoundingMode.HALF_UP);
    }

    public MathContext(int i, RoundingMode roundingMode) {
        this.precision = i;
        this.roundingMode = roundingMode;
        checkValid();
    }

    public MathContext(String str) {
        int iIndexOf;
        int length = "precision=".length();
        int length2 = "roundingMode=".length();
        if (!str.startsWith("precision=") || (iIndexOf = str.indexOf(32, length)) == -1) {
            throw invalidMathContext("Missing precision", str);
        }
        try {
            this.precision = Integer.parseInt(str.substring(length, iIndexOf));
            int i = iIndexOf + 1;
            if (!str.regionMatches(i, "roundingMode=", 0, length2)) {
                throw invalidMathContext("Missing rounding mode", str);
            }
            this.roundingMode = RoundingMode.valueOf(str.substring(i + length2));
            checkValid();
        } catch (NumberFormatException e) {
            throw invalidMathContext("Bad precision", str);
        }
    }

    private IllegalArgumentException invalidMathContext(String str, String str2) {
        throw new IllegalArgumentException(str + PluralRules.KEYWORD_RULE_SEPARATOR + str2);
    }

    private void checkValid() {
        if (this.precision < 0) {
            throw new IllegalArgumentException("Negative precision: " + this.precision);
        }
        if (this.roundingMode == null) {
            throw new NullPointerException("roundingMode == null");
        }
    }

    public int getPrecision() {
        return this.precision;
    }

    public RoundingMode getRoundingMode() {
        return this.roundingMode;
    }

    public boolean equals(Object obj) {
        if (obj instanceof MathContext) {
            MathContext mathContext = (MathContext) obj;
            if (mathContext.getPrecision() == this.precision && mathContext.getRoundingMode() == this.roundingMode) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return (this.precision << 3) | this.roundingMode.ordinal();
    }

    public String toString() {
        return "precision=" + this.precision + " roundingMode=" + this.roundingMode;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        try {
            checkValid();
        } catch (Exception e) {
            throw new StreamCorruptedException(e.getMessage());
        }
    }
}
