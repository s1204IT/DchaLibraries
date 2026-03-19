package java.util.concurrent;

import libcore.icu.RelativeDateTimeFormatter;

public enum TimeUnit {
    NANOSECONDS {
        @Override
        public long toNanos(long d) {
            return d;
        }

        @Override
        public long toMicros(long d) {
            return d / 1000;
        }

        @Override
        public long toMillis(long d) {
            return d / TimeUnit.C2;
        }

        @Override
        public long toSeconds(long d) {
            return d / TimeUnit.C3;
        }

        @Override
        public long toMinutes(long d) {
            return d / TimeUnit.C4;
        }

        @Override
        public long toHours(long d) {
            return d / TimeUnit.C5;
        }

        @Override
        public long toDays(long d) {
            return d / TimeUnit.C6;
        }

        @Override
        public long convert(long d, TimeUnit u) {
            return u.toNanos(d);
        }

        @Override
        int excessNanos(long d, long m) {
            return (int) (d - (TimeUnit.C2 * m));
        }
    },
    MICROSECONDS {
        @Override
        public long toNanos(long d) {
            return x(d, 1000L, 9223372036854775L);
        }

        @Override
        public long toMicros(long d) {
            return d;
        }

        @Override
        public long toMillis(long d) {
            return d / 1000;
        }

        @Override
        public long toSeconds(long d) {
            return d / TimeUnit.C2;
        }

        @Override
        public long toMinutes(long d) {
            return d / 60000000;
        }

        @Override
        public long toHours(long d) {
            return d / 3600000000L;
        }

        @Override
        public long toDays(long d) {
            return d / 86400000000L;
        }

        @Override
        public long convert(long d, TimeUnit u) {
            return u.toMicros(d);
        }

        @Override
        int excessNanos(long d, long m) {
            return (int) ((1000 * d) - (TimeUnit.C2 * m));
        }
    },
    MILLISECONDS {
        @Override
        public long toNanos(long d) {
            return x(d, TimeUnit.C2, 9223372036854L);
        }

        @Override
        public long toMicros(long d) {
            return x(d, 1000L, 9223372036854775L);
        }

        @Override
        public long toMillis(long d) {
            return d;
        }

        @Override
        public long toSeconds(long d) {
            return d / 1000;
        }

        @Override
        public long toMinutes(long d) {
            return d / RelativeDateTimeFormatter.MINUTE_IN_MILLIS;
        }

        @Override
        public long toHours(long d) {
            return d / RelativeDateTimeFormatter.HOUR_IN_MILLIS;
        }

        @Override
        public long toDays(long d) {
            return d / 86400000;
        }

        @Override
        public long convert(long d, TimeUnit u) {
            return u.toMillis(d);
        }

        @Override
        int excessNanos(long d, long m) {
            return 0;
        }
    },
    SECONDS {
        @Override
        public long toNanos(long d) {
            return x(d, TimeUnit.C3, 9223372036L);
        }

        @Override
        public long toMicros(long d) {
            return x(d, TimeUnit.C2, 9223372036854L);
        }

        @Override
        public long toMillis(long d) {
            return x(d, 1000L, 9223372036854775L);
        }

        @Override
        public long toSeconds(long d) {
            return d;
        }

        @Override
        public long toMinutes(long d) {
            return d / 60;
        }

        @Override
        public long toHours(long d) {
            return d / 3600;
        }

        @Override
        public long toDays(long d) {
            return d / 86400;
        }

        @Override
        public long convert(long d, TimeUnit u) {
            return u.toSeconds(d);
        }

        @Override
        int excessNanos(long d, long m) {
            return 0;
        }
    },
    MINUTES {
        @Override
        public long toNanos(long d) {
            return x(d, TimeUnit.C4, 153722867L);
        }

        @Override
        public long toMicros(long d) {
            return x(d, 60000000L, 153722867280L);
        }

        @Override
        public long toMillis(long d) {
            return x(d, RelativeDateTimeFormatter.MINUTE_IN_MILLIS, 153722867280912L);
        }

        @Override
        public long toSeconds(long d) {
            return x(d, 60L, 153722867280912930L);
        }

        @Override
        public long toMinutes(long d) {
            return d;
        }

        @Override
        public long toHours(long d) {
            return d / 60;
        }

        @Override
        public long toDays(long d) {
            return d / 1440;
        }

        @Override
        public long convert(long d, TimeUnit u) {
            return u.toMinutes(d);
        }

        @Override
        int excessNanos(long d, long m) {
            return 0;
        }
    },
    HOURS {
        @Override
        public long toNanos(long d) {
            return x(d, TimeUnit.C5, 2562047L);
        }

        @Override
        public long toMicros(long d) {
            return x(d, 3600000000L, 2562047788L);
        }

        @Override
        public long toMillis(long d) {
            return x(d, RelativeDateTimeFormatter.HOUR_IN_MILLIS, 2562047788015L);
        }

        @Override
        public long toSeconds(long d) {
            return x(d, 3600L, 2562047788015215L);
        }

        @Override
        public long toMinutes(long d) {
            return x(d, 60L, 153722867280912930L);
        }

        @Override
        public long toHours(long d) {
            return d;
        }

        @Override
        public long toDays(long d) {
            return d / 24;
        }

        @Override
        public long convert(long d, TimeUnit u) {
            return u.toHours(d);
        }

        @Override
        int excessNanos(long d, long m) {
            return 0;
        }
    },
    DAYS {
        @Override
        public long toNanos(long d) {
            return x(d, TimeUnit.C6, 106751L);
        }

        @Override
        public long toMicros(long d) {
            return x(d, 86400000000L, 106751991L);
        }

        @Override
        public long toMillis(long d) {
            return x(d, 86400000L, 106751991167L);
        }

        @Override
        public long toSeconds(long d) {
            return x(d, 86400L, 106751991167300L);
        }

        @Override
        public long toMinutes(long d) {
            return x(d, 1440L, 6405119470038038L);
        }

        @Override
        public long toHours(long d) {
            return x(d, 24L, 384307168202282325L);
        }

        @Override
        public long toDays(long d) {
            return d;
        }

        @Override
        public long convert(long d, TimeUnit u) {
            return u.toDays(d);
        }

        @Override
        int excessNanos(long d, long m) {
            return 0;
        }
    };

    static final long C0 = 1;
    static final long C1 = 1000;
    static final long C2 = 1000000;
    static final long C3 = 1000000000;
    static final long C4 = 60000000000L;
    static final long C5 = 3600000000000L;
    static final long C6 = 86400000000000L;
    static final long MAX = Long.MAX_VALUE;

    TimeUnit(TimeUnit timeUnit) {
        this();
    }

    abstract int excessNanos(long j, long j2);

    public static TimeUnit[] valuesCustom() {
        return values();
    }

    static long x(long d, long m, long over) {
        if (d > over) {
            return MAX;
        }
        if (d < (-over)) {
            return Long.MIN_VALUE;
        }
        return d * m;
    }

    public long convert(long sourceDuration, TimeUnit sourceUnit) {
        throw new AbstractMethodError();
    }

    public long toNanos(long duration) {
        throw new AbstractMethodError();
    }

    public long toMicros(long duration) {
        throw new AbstractMethodError();
    }

    public long toMillis(long duration) {
        throw new AbstractMethodError();
    }

    public long toSeconds(long duration) {
        throw new AbstractMethodError();
    }

    public long toMinutes(long duration) {
        throw new AbstractMethodError();
    }

    public long toHours(long duration) {
        throw new AbstractMethodError();
    }

    public long toDays(long duration) {
        throw new AbstractMethodError();
    }

    public void timedWait(Object obj, long timeout) throws InterruptedException {
        if (timeout <= 0) {
            return;
        }
        long ms = toMillis(timeout);
        int ns = excessNanos(timeout, ms);
        obj.wait(ms, ns);
    }

    public void timedJoin(Thread thread, long timeout) throws InterruptedException {
        if (timeout <= 0) {
            return;
        }
        long ms = toMillis(timeout);
        int ns = excessNanos(timeout, ms);
        thread.join(ms, ns);
    }

    public void sleep(long timeout) throws InterruptedException {
        if (timeout <= 0) {
            return;
        }
        long ms = toMillis(timeout);
        int ns = excessNanos(timeout, ms);
        Thread.sleep(ms, ns);
    }
}
