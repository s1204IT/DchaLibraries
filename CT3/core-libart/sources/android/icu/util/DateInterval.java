package android.icu.util;

import java.io.Serializable;

public final class DateInterval implements Serializable {
    private static final long serialVersionUID = 1;
    private final long fromDate;
    private final long toDate;

    public DateInterval(long from, long to) {
        this.fromDate = from;
        this.toDate = to;
    }

    public long getFromDate() {
        return this.fromDate;
    }

    public long getToDate() {
        return this.toDate;
    }

    public boolean equals(Object obj) {
        return (obj instanceof DateInterval) && this.fromDate == obj.fromDate && this.toDate == obj.toDate;
    }

    public int hashCode() {
        return (int) (this.fromDate + this.toDate);
    }

    public String toString() {
        return String.valueOf(this.fromDate) + " " + String.valueOf(this.toDate);
    }
}
