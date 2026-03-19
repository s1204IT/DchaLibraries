package android.icu.util;

import android.icu.impl.number.Padder;
import java.io.Serializable;

public final class DateInterval implements Serializable {
    private static final long serialVersionUID = 1;
    private final long fromDate;
    private final long toDate;

    public DateInterval(long j, long j2) {
        this.fromDate = j;
        this.toDate = j2;
    }

    public long getFromDate() {
        return this.fromDate;
    }

    public long getToDate() {
        return this.toDate;
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof DateInterval)) {
            return false;
        }
        DateInterval dateInterval = (DateInterval) obj;
        return this.fromDate == dateInterval.fromDate && this.toDate == dateInterval.toDate;
    }

    public int hashCode() {
        return (int) (this.fromDate + this.toDate);
    }

    public String toString() {
        return String.valueOf(this.fromDate) + Padder.FALLBACK_PADDING_STRING + String.valueOf(this.toDate);
    }
}
