package java.time.chrono;

import java.time.DateTimeException;

public enum MinguoEra implements Era {
    BEFORE_ROC,
    ROC;

    public static MinguoEra of(int i) {
        switch (i) {
            case 0:
                return BEFORE_ROC;
            case 1:
                return ROC;
            default:
                throw new DateTimeException("Invalid era: " + i);
        }
    }

    @Override
    public int getValue() {
        return ordinal();
    }
}
