package android.icu.impl.duration;

public final class Period {
    final int[] counts;
    final boolean inFuture;
    final byte timeLimit;

    public static Period at(float f, TimeUnit timeUnit) {
        checkCount(f);
        return new Period(0, false, f, timeUnit);
    }

    public static Period moreThan(float f, TimeUnit timeUnit) {
        checkCount(f);
        return new Period(2, false, f, timeUnit);
    }

    public static Period lessThan(float f, TimeUnit timeUnit) {
        checkCount(f);
        return new Period(1, false, f, timeUnit);
    }

    public Period and(float f, TimeUnit timeUnit) {
        checkCount(f);
        return setTimeUnitValue(timeUnit, f);
    }

    public Period omit(TimeUnit timeUnit) {
        return setTimeUnitInternalValue(timeUnit, 0);
    }

    public Period at() {
        return setTimeLimit((byte) 0);
    }

    public Period moreThan() {
        return setTimeLimit((byte) 2);
    }

    public Period lessThan() {
        return setTimeLimit((byte) 1);
    }

    public Period inFuture() {
        return setFuture(true);
    }

    public Period inPast() {
        return setFuture(false);
    }

    public Period inFuture(boolean z) {
        return setFuture(z);
    }

    public Period inPast(boolean z) {
        return setFuture(!z);
    }

    public boolean isSet() {
        for (int i = 0; i < this.counts.length; i++) {
            if (this.counts[i] != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isSet(TimeUnit timeUnit) {
        return this.counts[timeUnit.ordinal] > 0;
    }

    public float getCount(TimeUnit timeUnit) {
        if (this.counts[timeUnit.ordinal] == 0) {
            return 0.0f;
        }
        return (this.counts[r2] - 1) / 1000.0f;
    }

    public boolean isInFuture() {
        return this.inFuture;
    }

    public boolean isInPast() {
        return !this.inFuture;
    }

    public boolean isMoreThan() {
        return this.timeLimit == 2;
    }

    public boolean isLessThan() {
        return this.timeLimit == 1;
    }

    public boolean equals(Object obj) {
        try {
            return equals((Period) obj);
        } catch (ClassCastException e) {
            return false;
        }
    }

    public boolean equals(Period period) {
        if (period == null || this.timeLimit != period.timeLimit || this.inFuture != period.inFuture) {
            return false;
        }
        for (int i = 0; i < this.counts.length; i++) {
            if (this.counts[i] != period.counts[i]) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        int i = (this.timeLimit << 1) | (this.inFuture ? 1 : 0);
        for (int i2 = 0; i2 < this.counts.length; i2++) {
            i = (i << 2) ^ this.counts[i2];
        }
        return i;
    }

    private Period(int i, boolean z, float f, TimeUnit timeUnit) {
        this.timeLimit = (byte) i;
        this.inFuture = z;
        this.counts = new int[TimeUnit.units.length];
        this.counts[timeUnit.ordinal] = ((int) (f * 1000.0f)) + 1;
    }

    Period(int i, boolean z, int[] iArr) {
        this.timeLimit = (byte) i;
        this.inFuture = z;
        this.counts = iArr;
    }

    private Period setTimeUnitValue(TimeUnit timeUnit, float f) {
        if (f < 0.0f) {
            throw new IllegalArgumentException("value: " + f);
        }
        return setTimeUnitInternalValue(timeUnit, ((int) (f * 1000.0f)) + 1);
    }

    private Period setTimeUnitInternalValue(TimeUnit timeUnit, int i) {
        byte b = timeUnit.ordinal;
        if (this.counts[b] != i) {
            int[] iArr = new int[this.counts.length];
            for (int i2 = 0; i2 < this.counts.length; i2++) {
                iArr[i2] = this.counts[i2];
            }
            iArr[b] = i;
            return new Period(this.timeLimit, this.inFuture, iArr);
        }
        return this;
    }

    private Period setFuture(boolean z) {
        if (this.inFuture != z) {
            return new Period(this.timeLimit, z, this.counts);
        }
        return this;
    }

    private Period setTimeLimit(byte b) {
        if (this.timeLimit != b) {
            return new Period(b, this.inFuture, this.counts);
        }
        return this;
    }

    private static void checkCount(float f) {
        if (f < 0.0f) {
            throw new IllegalArgumentException("count (" + f + ") cannot be negative");
        }
    }
}
