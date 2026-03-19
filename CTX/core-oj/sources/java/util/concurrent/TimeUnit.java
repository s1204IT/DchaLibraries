package java.util.concurrent;

public enum TimeUnit {
    NANOSECONDS {
        @Override
        public long toNanos(long j) {
            return j;
        }

        @Override
        public long toMicros(long j) {
            return j / TimeUnit.C1;
        }

        @Override
        public long toMillis(long j) {
            return j / TimeUnit.C2;
        }

        @Override
        public long toSeconds(long j) {
            return j / TimeUnit.C3;
        }

        @Override
        public long toMinutes(long j) {
            return j / TimeUnit.C4;
        }

        @Override
        public long toHours(long j) {
            return j / TimeUnit.C5;
        }

        @Override
        public long toDays(long j) {
            return j / TimeUnit.C6;
        }

        @Override
        public long convert(long j, TimeUnit timeUnit) {
            return timeUnit.toNanos(j);
        }

        @Override
        int excessNanos(long j, long j2) {
            return (int) (j - (j2 * TimeUnit.C2));
        }
    },
    MICROSECONDS {
        @Override
        public long toNanos(long j) {
            return x(j, TimeUnit.C1, 9223372036854775L);
        }

        @Override
        public long toMicros(long j) {
            return j;
        }

        @Override
        public long toMillis(long j) {
            return j / TimeUnit.C1;
        }

        @Override
        public long toSeconds(long j) {
            return j / TimeUnit.C2;
        }

        @Override
        public long toMinutes(long j) {
            return j / 60000000;
        }

        @Override
        public long toHours(long j) {
            return j / 3600000000L;
        }

        @Override
        public long toDays(long j) {
            return j / 86400000000L;
        }

        @Override
        public long convert(long j, TimeUnit timeUnit) {
            return timeUnit.toMicros(j);
        }

        @Override
        int excessNanos(long j, long j2) {
            return (int) ((j * TimeUnit.C1) - (j2 * TimeUnit.C2));
        }
    },
    MILLISECONDS {
        @Override
        public long toNanos(long j) {
            return x(j, TimeUnit.C2, 9223372036854L);
        }

        @Override
        public long toMicros(long j) {
            return x(j, TimeUnit.C1, 9223372036854775L);
        }

        @Override
        public long toMillis(long j) {
            return j;
        }

        @Override
        public long toSeconds(long j) {
            return j / TimeUnit.C1;
        }

        @Override
        public long toMinutes(long j) {
            return j / 60000;
        }

        @Override
        public long toHours(long j) {
            return j / 3600000;
        }

        @Override
        public long toDays(long j) {
            return j / 86400000;
        }

        @Override
        public long convert(long j, TimeUnit timeUnit) {
            return timeUnit.toMillis(j);
        }

        @Override
        int excessNanos(long j, long j2) {
            return 0;
        }
    },
    SECONDS {
        @Override
        public long toNanos(long j) {
            return x(j, TimeUnit.C3, 9223372036L);
        }

        @Override
        public long toMicros(long j) {
            return x(j, TimeUnit.C2, 9223372036854L);
        }

        @Override
        public long toMillis(long j) {
            return x(j, TimeUnit.C1, 9223372036854775L);
        }

        @Override
        public long toSeconds(long j) {
            return j;
        }

        @Override
        public long toMinutes(long j) {
            return j / 60;
        }

        @Override
        public long toHours(long j) {
            return j / 3600;
        }

        @Override
        public long toDays(long j) {
            return j / 86400;
        }

        @Override
        public long convert(long j, TimeUnit timeUnit) {
            return timeUnit.toSeconds(j);
        }

        @Override
        int excessNanos(long j, long j2) {
            return 0;
        }
    },
    MINUTES {
        @Override
        public long toNanos(long j) {
            return x(j, TimeUnit.C4, 153722867L);
        }

        @Override
        public long toMicros(long j) {
            return x(j, 60000000L, 153722867280L);
        }

        @Override
        public long toMillis(long j) {
            return x(j, 60000L, 153722867280912L);
        }

        @Override
        public long toSeconds(long j) {
            return x(j, 60L, 153722867280912930L);
        }

        @Override
        public long toMinutes(long j) {
            return j;
        }

        @Override
        public long toHours(long j) {
            return j / 60;
        }

        @Override
        public long toDays(long j) {
            return j / 1440;
        }

        @Override
        public long convert(long j, TimeUnit timeUnit) {
            return timeUnit.toMinutes(j);
        }

        @Override
        int excessNanos(long j, long j2) {
            return 0;
        }
    },
    HOURS {
        @Override
        public long toNanos(long j) {
            return x(j, TimeUnit.C5, 2562047L);
        }

        @Override
        public long toMicros(long j) {
            return x(j, 3600000000L, 2562047788L);
        }

        @Override
        public long toMillis(long j) {
            return x(j, 3600000L, 2562047788015L);
        }

        @Override
        public long toSeconds(long j) {
            return x(j, 3600L, 2562047788015215L);
        }

        @Override
        public long toMinutes(long j) {
            return x(j, 60L, 153722867280912930L);
        }

        @Override
        public long toHours(long j) {
            return j;
        }

        @Override
        public long toDays(long j) {
            return j / 24;
        }

        @Override
        public long convert(long j, TimeUnit timeUnit) {
            return timeUnit.toHours(j);
        }

        @Override
        int excessNanos(long j, long j2) {
            return 0;
        }
    },
    DAYS {
        @Override
        public long toNanos(long j) {
            return x(j, TimeUnit.C6, 106751L);
        }

        @Override
        public long toMicros(long j) {
            return x(j, 86400000000L, 106751991L);
        }

        @Override
        public long toMillis(long j) {
            return x(j, 86400000L, 106751991167L);
        }

        @Override
        public long toSeconds(long j) {
            return x(j, 86400L, 106751991167300L);
        }

        @Override
        public long toMinutes(long j) {
            return x(j, 1440L, 6405119470038038L);
        }

        @Override
        public long toHours(long j) {
            return x(j, 24L, 384307168202282325L);
        }

        @Override
        public long toDays(long j) {
            return j;
        }

        @Override
        public long convert(long j, TimeUnit timeUnit) {
            return timeUnit.toDays(j);
        }

        @Override
        int excessNanos(long j, long j2) {
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

    abstract int excessNanos(long j, long j2);

    static long x(long j, long j2, long j3) {
        if (j > j3) {
            return Long.MAX_VALUE;
        }
        if (j < (-j3)) {
            return Long.MIN_VALUE;
        }
        return j * j2;
    }

    public long convert(long j, TimeUnit timeUnit) {
        throw new AbstractMethodError();
    }

    public long toNanos(long j) {
        throw new AbstractMethodError();
    }

    public long toMicros(long j) {
        throw new AbstractMethodError();
    }

    public long toMillis(long j) {
        throw new AbstractMethodError();
    }

    public long toSeconds(long j) {
        throw new AbstractMethodError();
    }

    public long toMinutes(long j) {
        throw new AbstractMethodError();
    }

    public long toHours(long j) {
        throw new AbstractMethodError();
    }

    public long toDays(long j) {
        throw new AbstractMethodError();
    }

    public void timedWait(Object obj, long j) throws InterruptedException {
        if (j > 0) {
            long millis = toMillis(j);
            obj.wait(millis, excessNanos(j, millis));
        }
    }

    public void timedJoin(Thread thread, long j) throws InterruptedException {
        if (j > 0) {
            long millis = toMillis(j);
            thread.join(millis, excessNanos(j, millis));
        }
    }

    public void sleep(long j) throws InterruptedException {
        if (j > 0) {
            long millis = toMillis(j);
            Thread.sleep(millis, excessNanos(j, millis));
        }
    }
}
