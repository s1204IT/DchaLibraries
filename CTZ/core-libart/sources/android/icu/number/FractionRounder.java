package android.icu.number;

public abstract class FractionRounder extends Rounder {
    FractionRounder() {
    }

    public Rounder withMinDigits(int i) {
        if (i > 0 && i <= 100) {
            return constructFractionSignificant(this, i, -1);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }

    public Rounder withMaxDigits(int i) {
        if (i > 0 && i <= 100) {
            return constructFractionSignificant(this, -1, i);
        }
        throw new IllegalArgumentException("Significant digits must be between 0 and 100");
    }
}
