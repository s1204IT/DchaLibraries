package android.util;

import android.net.TrafficStats;

public enum DataUnit {
    KILOBYTES {
        @Override
        public long toBytes(long j) {
            return j * 1000;
        }
    },
    MEGABYTES {
        @Override
        public long toBytes(long j) {
            return j * TimeUtils.NANOS_PER_MS;
        }
    },
    GIGABYTES {
        @Override
        public long toBytes(long j) {
            return j * 1000000000;
        }
    },
    KIBIBYTES {
        @Override
        public long toBytes(long j) {
            return j * 1024;
        }
    },
    MEBIBYTES {
        @Override
        public long toBytes(long j) {
            return j * 1048576;
        }
    },
    GIBIBYTES {
        @Override
        public long toBytes(long j) {
            return j * TrafficStats.GB_IN_BYTES;
        }
    };

    public long toBytes(long j) {
        throw new AbstractMethodError();
    }
}
