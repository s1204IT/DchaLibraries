package android.icu.number;

public class IntegerWidth {
    static final IntegerWidth DEFAULT = new IntegerWidth(1, -1);
    final int maxInt;
    final int minInt;

    private IntegerWidth(int i, int i2) {
        this.minInt = i;
        this.maxInt = i2;
    }

    public static IntegerWidth zeroFillTo(int i) {
        if (i == 1) {
            return DEFAULT;
        }
        if (i >= 0 && i < 100) {
            return new IntegerWidth(i, -1);
        }
        throw new IllegalArgumentException("Integer digits must be between 0 and 100");
    }

    public IntegerWidth truncateAt(int i) {
        if (i == this.maxInt) {
            return this;
        }
        if (i >= 0 && i < 100) {
            return new IntegerWidth(this.minInt, i);
        }
        if (i == -1) {
            return new IntegerWidth(this.minInt, i);
        }
        throw new IllegalArgumentException("Integer digits must be between 0 and 100");
    }
}
