package java.time.chrono;

import java.io.Serializable;
import java.util.Comparator;

public final class $$Lambda$AbstractChronology$j22w8kHhJoqCd56hhLQK1G0VLFw implements Comparator, Serializable {
    public static final $$Lambda$AbstractChronology$j22w8kHhJoqCd56hhLQK1G0VLFw INSTANCE = new $$Lambda$AbstractChronology$j22w8kHhJoqCd56hhLQK1G0VLFw();

    private $$Lambda$AbstractChronology$j22w8kHhJoqCd56hhLQK1G0VLFw() {
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        return Long.compare(((ChronoLocalDate) obj).toEpochDay(), ((ChronoLocalDate) obj2).toEpochDay());
    }
}
