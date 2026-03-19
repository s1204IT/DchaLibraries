package java.time.chrono;

import java.time.DateTimeException;

public enum IsoEra implements Era {
    BCE,
    CE;

    public static IsoEra of(int i) {
        switch (i) {
            case 0:
                return BCE;
            case 1:
                return CE;
            default:
                throw new DateTimeException("Invalid era: " + i);
        }
    }

    @Override
    public int getValue() {
        return ordinal();
    }
}
