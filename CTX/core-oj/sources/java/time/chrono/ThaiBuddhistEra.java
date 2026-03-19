package java.time.chrono;

import java.time.DateTimeException;

public enum ThaiBuddhistEra implements Era {
    BEFORE_BE,
    BE;

    public static ThaiBuddhistEra of(int i) {
        switch (i) {
            case 0:
                return BEFORE_BE;
            case 1:
                return BE;
            default:
                throw new DateTimeException("Invalid era: " + i);
        }
    }

    @Override
    public int getValue() {
        return ordinal();
    }
}
