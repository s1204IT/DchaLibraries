package android.icu.impl.coll;

public final class CollationKeys {

    static final boolean f46assertionsDisabled;
    private static final int CASE_LOWER_FIRST_COMMON_HIGH = 13;
    private static final int CASE_LOWER_FIRST_COMMON_LOW = 1;
    private static final int CASE_LOWER_FIRST_COMMON_MAX_COUNT = 7;
    private static final int CASE_LOWER_FIRST_COMMON_MIDDLE = 7;
    private static final int CASE_UPPER_FIRST_COMMON_HIGH = 15;
    private static final int CASE_UPPER_FIRST_COMMON_LOW = 3;
    private static final int CASE_UPPER_FIRST_COMMON_MAX_COUNT = 13;
    private static final int QUAT_COMMON_HIGH = 252;
    private static final int QUAT_COMMON_LOW = 28;
    private static final int QUAT_COMMON_MAX_COUNT = 113;
    private static final int QUAT_COMMON_MIDDLE = 140;
    private static final int QUAT_SHIFTED_LIMIT_BYTE = 27;
    static final int SEC_COMMON_HIGH = 69;
    private static final int SEC_COMMON_LOW = 5;
    private static final int SEC_COMMON_MAX_COUNT = 33;
    private static final int SEC_COMMON_MIDDLE = 37;
    public static final LevelCallback SIMPLE_LEVEL_FALLBACK;
    private static final int TER_LOWER_FIRST_COMMON_HIGH = 69;
    private static final int TER_LOWER_FIRST_COMMON_LOW = 5;
    private static final int TER_LOWER_FIRST_COMMON_MAX_COUNT = 33;
    private static final int TER_LOWER_FIRST_COMMON_MIDDLE = 37;
    private static final int TER_ONLY_COMMON_HIGH = 197;
    private static final int TER_ONLY_COMMON_LOW = 5;
    private static final int TER_ONLY_COMMON_MAX_COUNT = 97;
    private static final int TER_ONLY_COMMON_MIDDLE = 101;
    private static final int TER_UPPER_FIRST_COMMON_HIGH = 197;
    private static final int TER_UPPER_FIRST_COMMON_LOW = 133;
    private static final int TER_UPPER_FIRST_COMMON_MAX_COUNT = 33;
    private static final int TER_UPPER_FIRST_COMMON_MIDDLE = 165;
    private static final int[] levelMasks;

    public static abstract class SortKeyByteSink {
        private int appended_ = 0;
        protected byte[] buffer_;

        protected abstract void AppendBeyondCapacity(byte[] bArr, int i, int i2, int i3);

        protected abstract boolean Resize(int i, int i2);

        public SortKeyByteSink(byte[] dest) {
            this.buffer_ = dest;
        }

        public void setBufferAndAppended(byte[] dest, int app) {
            this.buffer_ = dest;
            this.appended_ = app;
        }

        public void Append(byte[] bytes, int n) {
            if (n <= 0 || bytes == null) {
                return;
            }
            int length = this.appended_;
            this.appended_ += n;
            int available = this.buffer_.length - length;
            if (n <= available) {
                System.arraycopy(bytes, 0, this.buffer_, length, n);
            } else {
                AppendBeyondCapacity(bytes, 0, n, length);
            }
        }

        public void Append(int b) {
            if (this.appended_ < this.buffer_.length || Resize(1, this.appended_)) {
                this.buffer_[this.appended_] = (byte) b;
            }
            this.appended_++;
        }

        public int NumberOfBytesAppended() {
            return this.appended_;
        }

        public int GetRemainingCapacity() {
            return this.buffer_.length - this.appended_;
        }

        public boolean Overflowed() {
            return this.appended_ > this.buffer_.length;
        }
    }

    public static class LevelCallback {
        boolean needToWrite(int level) {
            return true;
        }
    }

    static {
        f46assertionsDisabled = !CollationKeys.class.desiredAssertionStatus();
        SIMPLE_LEVEL_FALLBACK = new LevelCallback();
        levelMasks = new int[]{2, 6, 22, 54, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 54};
    }

    private static final class SortKeyLevel {

        static final boolean f47assertionsDisabled;
        private static final int INITIAL_CAPACITY = 40;
        byte[] buffer = new byte[40];
        int len = 0;

        static {
            f47assertionsDisabled = !SortKeyLevel.class.desiredAssertionStatus();
        }

        SortKeyLevel() {
        }

        boolean isEmpty() {
            return this.len == 0;
        }

        int length() {
            return this.len;
        }

        byte getAt(int index) {
            return this.buffer[index];
        }

        byte[] data() {
            return this.buffer;
        }

        void appendByte(int b) {
            if (this.len >= this.buffer.length && !ensureCapacity(1)) {
                return;
            }
            byte[] bArr = this.buffer;
            int i = this.len;
            this.len = i + 1;
            bArr[i] = (byte) b;
        }

        void appendWeight16(int w) {
            if (!f47assertionsDisabled) {
                if (!((65535 & w) != 0)) {
                    throw new AssertionError();
                }
            }
            byte b0 = (byte) (w >>> 8);
            byte b1 = (byte) w;
            int appendLength = b1 == 0 ? 1 : 2;
            if (this.len + appendLength > this.buffer.length && !ensureCapacity(appendLength)) {
                return;
            }
            byte[] bArr = this.buffer;
            int i = this.len;
            this.len = i + 1;
            bArr[i] = b0;
            if (b1 == 0) {
                return;
            }
            byte[] bArr2 = this.buffer;
            int i2 = this.len;
            this.len = i2 + 1;
            bArr2[i2] = b1;
        }

        void appendWeight32(long w) {
            int appendLength;
            if (!f47assertionsDisabled) {
                if (!(w != 0)) {
                    throw new AssertionError();
                }
            }
            byte[] bytes = {(byte) (w >>> 24), (byte) (w >>> 16), (byte) (w >>> 8), (byte) w};
            if (bytes[1] == 0) {
                appendLength = 1;
            } else {
                appendLength = bytes[2] == 0 ? 2 : bytes[3] == 0 ? 3 : 4;
            }
            if (this.len + appendLength > this.buffer.length && !ensureCapacity(appendLength)) {
                return;
            }
            byte[] bArr = this.buffer;
            int i = this.len;
            this.len = i + 1;
            bArr[i] = bytes[0];
            if (bytes[1] == 0) {
                return;
            }
            byte[] bArr2 = this.buffer;
            int i2 = this.len;
            this.len = i2 + 1;
            bArr2[i2] = bytes[1];
            if (bytes[2] == 0) {
                return;
            }
            byte[] bArr3 = this.buffer;
            int i3 = this.len;
            this.len = i3 + 1;
            bArr3[i3] = bytes[2];
            if (bytes[3] == 0) {
                return;
            }
            byte[] bArr4 = this.buffer;
            int i4 = this.len;
            this.len = i4 + 1;
            bArr4[i4] = bytes[3];
        }

        void appendReverseWeight16(int w) {
            if (!f47assertionsDisabled) {
                if (!((65535 & w) != 0)) {
                    throw new AssertionError();
                }
            }
            byte b0 = (byte) (w >>> 8);
            byte b1 = (byte) w;
            int appendLength = b1 == 0 ? 1 : 2;
            if (this.len + appendLength > this.buffer.length && !ensureCapacity(appendLength)) {
                return;
            }
            if (b1 == 0) {
                byte[] bArr = this.buffer;
                int i = this.len;
                this.len = i + 1;
                bArr[i] = b0;
                return;
            }
            this.buffer[this.len] = b1;
            this.buffer[this.len + 1] = b0;
            this.len += 2;
        }

        void appendTo(SortKeyByteSink sink) {
            if (!f47assertionsDisabled) {
                if (!(this.len > 0 && this.buffer[this.len + (-1)] == 1)) {
                    throw new AssertionError();
                }
            }
            sink.Append(this.buffer, this.len - 1);
        }

        private boolean ensureCapacity(int appendCapacity) {
            int newCapacity = this.buffer.length * 2;
            int altCapacity = this.len + (appendCapacity * 2);
            if (newCapacity < altCapacity) {
                newCapacity = altCapacity;
            }
            if (newCapacity < 200) {
                newCapacity = 200;
            }
            byte[] newbuf = new byte[newCapacity];
            System.arraycopy(this.buffer, 0, newbuf, 0, this.len);
            this.buffer = newbuf;
            return true;
        }
    }

    private static SortKeyLevel getSortKeyLevel(int levels, int level) {
        if ((levels & level) != 0) {
            return new SortKeyLevel();
        }
        return null;
    }

    private CollationKeys() {
    }

    public static void writeSortKeyUpToQuaternary(CollationIterator iter, boolean[] compressibleBytes, CollationSettings settings, SortKeyByteSink sink, int minLevel, LevelCallback callback, boolean preflight) {
        long variableTop;
        int q;
        int b;
        int b2;
        int b3;
        int b4;
        int b5;
        int s;
        int b6;
        int b7;
        int i;
        int options = settings.options;
        int levels = levelMasks[CollationSettings.getStrength(options)];
        if ((options & 1024) != 0) {
            levels |= 8;
        }
        int levels2 = levels & (~((1 << minLevel) - 1));
        if (levels2 == 0) {
            return;
        }
        if ((options & 12) == 0) {
            variableTop = 0;
        } else {
            variableTop = settings.variableTop + 1;
        }
        int tertiaryMask = CollationSettings.getTertiaryMask(options);
        byte[] p234 = new byte[3];
        SortKeyLevel cases = getSortKeyLevel(levels2, 8);
        SortKeyLevel secondaries = getSortKeyLevel(levels2, 4);
        SortKeyLevel tertiaries = getSortKeyLevel(levels2, 16);
        SortKeyLevel quaternaries = getSortKeyLevel(levels2, 32);
        long prevReorderedPrimary = 0;
        int commonCases = 0;
        int commonSecondaries = 0;
        int commonTertiaries = 0;
        int commonQuaternaries = 0;
        int prevSecondary = 0;
        int secSegmentStart = 0;
        while (true) {
            iter.clearCEsIfNoneRemaining();
            long ce = iter.nextCE();
            long p = ce >>> 32;
            if (p < variableTop && p > Collation.MERGE_SEPARATOR_PRIMARY) {
                if (commonQuaternaries != 0) {
                    int commonQuaternaries2 = commonQuaternaries - 1;
                    while (commonQuaternaries2 >= 113) {
                        quaternaries.appendByte(140);
                        commonQuaternaries2 -= 113;
                    }
                    quaternaries.appendByte(commonQuaternaries2 + 28);
                    commonQuaternaries = 0;
                }
                do {
                    if ((levels2 & 32) != 0) {
                        if (settings.hasReordering()) {
                            p = settings.reorder(p);
                        }
                        if ((((int) p) >>> 24) >= 27) {
                            quaternaries.appendByte(27);
                        }
                        quaternaries.appendWeight32(p);
                    }
                    do {
                        ce = iter.nextCE();
                        p = ce >>> 32;
                    } while (p == 0);
                    if (p >= variableTop) {
                        break;
                    }
                } while (p > Collation.MERGE_SEPARATOR_PRIMARY);
            }
            if (p > 1 && (levels2 & 2) != 0) {
                boolean isCompressible = compressibleBytes[((int) p) >>> 24];
                if (settings.hasReordering()) {
                    p = settings.reorder(p);
                }
                int p1 = ((int) p) >>> 24;
                if (!isCompressible || p1 != (((int) prevReorderedPrimary) >>> 24)) {
                    if (prevReorderedPrimary != 0) {
                        if (p < prevReorderedPrimary) {
                            if (p1 > 2) {
                                sink.Append(3);
                            }
                        } else {
                            sink.Append(255);
                        }
                    }
                    sink.Append(p1);
                    if (isCompressible) {
                        prevReorderedPrimary = p;
                    } else {
                        prevReorderedPrimary = 0;
                    }
                }
                byte p2 = (byte) (p >>> 16);
                if (p2 != 0) {
                    p234[0] = p2;
                    p234[1] = (byte) (p >>> 8);
                    p234[2] = (byte) p;
                    if (p234[1] == 0) {
                        i = 1;
                    } else {
                        i = p234[2] == 0 ? 2 : 3;
                    }
                    sink.Append(p234, i);
                }
                if (!preflight && sink.Overflowed()) {
                    return;
                }
            }
            int lower32 = (int) ce;
            if (lower32 != 0) {
                if ((levels2 & 4) != 0 && (s = lower32 >>> 16) != 0) {
                    if (s == 1280 && ((options & 2048) == 0 || p != Collation.MERGE_SEPARATOR_PRIMARY)) {
                        commonSecondaries++;
                    } else if ((options & 2048) == 0) {
                        if (commonSecondaries != 0) {
                            int commonSecondaries2 = commonSecondaries - 1;
                            while (commonSecondaries2 >= 33) {
                                secondaries.appendByte(37);
                                commonSecondaries2 -= 33;
                            }
                            if (s < 1280) {
                                b7 = commonSecondaries2 + 5;
                            } else {
                                b7 = 69 - commonSecondaries2;
                            }
                            secondaries.appendByte(b7);
                            commonSecondaries = 0;
                        }
                        secondaries.appendWeight16(s);
                    } else {
                        if (commonSecondaries != 0) {
                            int commonSecondaries3 = commonSecondaries - 1;
                            int remainder = commonSecondaries3 % 33;
                            if (prevSecondary < 1280) {
                                b6 = remainder + 5;
                            } else {
                                b6 = 69 - remainder;
                            }
                            secondaries.appendByte(b6);
                            commonSecondaries = commonSecondaries3 - remainder;
                            while (commonSecondaries > 0) {
                                secondaries.appendByte(37);
                                commonSecondaries -= 33;
                            }
                        }
                        if (0 < p && p <= Collation.MERGE_SEPARATOR_PRIMARY) {
                            byte[] secs = secondaries.data();
                            int last = secondaries.length() - 1;
                            while (true) {
                                int last2 = last;
                                int secSegmentStart2 = secSegmentStart;
                                if (secSegmentStart2 >= last2) {
                                    break;
                                }
                                byte b8 = secs[secSegmentStart2];
                                secSegmentStart = secSegmentStart2 + 1;
                                secs[secSegmentStart2] = secs[last2];
                                last = last2 - 1;
                                secs[last2] = b8;
                            }
                            secondaries.appendByte(p == 1 ? 1 : 2);
                            prevSecondary = 0;
                            secSegmentStart = secondaries.length();
                        } else {
                            secondaries.appendReverseWeight16(s);
                            prevSecondary = s;
                        }
                    }
                }
                if ((levels2 & 8) != 0 && (CollationSettings.getStrength(options) != 0 ? (lower32 >>> 16) != 0 : p != 0)) {
                    int c = (lower32 >>> 8) & 255;
                    if (!f46assertionsDisabled) {
                        if (!((c & 192) != 192)) {
                            throw new AssertionError();
                        }
                    }
                    if ((c & 192) == 0 && c > 1) {
                        commonCases++;
                    } else {
                        if ((options & 256) == 0) {
                            if (commonCases != 0 && (c > 1 || !cases.isEmpty())) {
                                int commonCases2 = commonCases - 1;
                                while (commonCases2 >= 7) {
                                    cases.appendByte(112);
                                    commonCases2 -= 7;
                                }
                                if (c <= 1) {
                                    b5 = commonCases2 + 1;
                                } else {
                                    b5 = 13 - commonCases2;
                                }
                                cases.appendByte(b5 << 4);
                                commonCases = 0;
                            }
                            if (c > 1) {
                                c = ((c >>> 6) + 13) << 4;
                            }
                        } else {
                            if (commonCases != 0) {
                                int commonCases3 = commonCases - 1;
                                while (commonCases3 >= 13) {
                                    cases.appendByte(48);
                                    commonCases3 -= 13;
                                }
                                cases.appendByte((commonCases3 + 3) << 4);
                                commonCases = 0;
                            }
                            if (c > 1) {
                                c = (3 - (c >>> 6)) << 4;
                            }
                        }
                        cases.appendByte(c);
                    }
                }
                if ((levels2 & 16) != 0) {
                    int t = lower32 & tertiaryMask;
                    if (!f46assertionsDisabled) {
                        if (!((49152 & lower32) != 49152)) {
                            throw new AssertionError();
                        }
                    }
                    if (t == 1280) {
                        commonTertiaries++;
                    } else if ((32768 & tertiaryMask) == 0) {
                        if (commonTertiaries != 0) {
                            int commonTertiaries2 = commonTertiaries - 1;
                            while (commonTertiaries2 >= 97) {
                                tertiaries.appendByte(101);
                                commonTertiaries2 -= 97;
                            }
                            if (t < 1280) {
                                b4 = commonTertiaries2 + 5;
                            } else {
                                b4 = 197 - commonTertiaries2;
                            }
                            tertiaries.appendByte(b4);
                            commonTertiaries = 0;
                        }
                        if (t > 1280) {
                            t += Collation.CASE_MASK;
                        }
                        tertiaries.appendWeight16(t);
                    } else if ((options & 256) == 0) {
                        if (commonTertiaries != 0) {
                            int commonTertiaries3 = commonTertiaries - 1;
                            while (commonTertiaries3 >= 33) {
                                tertiaries.appendByte(37);
                                commonTertiaries3 -= 33;
                            }
                            if (t < 1280) {
                                b3 = commonTertiaries3 + 5;
                            } else {
                                b3 = 69 - commonTertiaries3;
                            }
                            tertiaries.appendByte(b3);
                            commonTertiaries = 0;
                        }
                        if (t > 1280) {
                            t += 16384;
                        }
                        tertiaries.appendWeight16(t);
                    } else {
                        if (t > 256) {
                            if ((lower32 >>> 16) != 0) {
                                t ^= Collation.CASE_MASK;
                                if (t < 50432) {
                                    t -= 16384;
                                }
                            } else {
                                if (!f46assertionsDisabled) {
                                    if (!(34304 <= t && t <= 49151)) {
                                        throw new AssertionError();
                                    }
                                }
                                t += 16384;
                            }
                        }
                        if (commonTertiaries != 0) {
                            int commonTertiaries4 = commonTertiaries - 1;
                            while (commonTertiaries4 >= 33) {
                                tertiaries.appendByte(165);
                                commonTertiaries4 -= 33;
                            }
                            if (t < 34048) {
                                b2 = commonTertiaries4 + 133;
                            } else {
                                b2 = 197 - commonTertiaries4;
                            }
                            tertiaries.appendByte(b2);
                            commonTertiaries = 0;
                        }
                        tertiaries.appendWeight16(t);
                    }
                }
                if ((levels2 & 32) != 0) {
                    int q2 = lower32 & 65535;
                    if ((q2 & 192) == 0 && q2 > 256) {
                        commonQuaternaries++;
                    } else if (q2 == 256 && (options & 12) == 0 && quaternaries.isEmpty()) {
                        quaternaries.appendByte(1);
                    } else {
                        if (q2 == 256) {
                            q = 1;
                        } else {
                            q = ((q2 >>> 6) & 3) + 252;
                        }
                        if (commonQuaternaries != 0) {
                            int commonQuaternaries3 = commonQuaternaries - 1;
                            while (commonQuaternaries3 >= 113) {
                                quaternaries.appendByte(140);
                                commonQuaternaries3 -= 113;
                            }
                            if (q < 28) {
                                b = commonQuaternaries3 + 28;
                            } else {
                                b = 252 - commonQuaternaries3;
                            }
                            quaternaries.appendByte(b);
                            commonQuaternaries = 0;
                        }
                        quaternaries.appendByte(q);
                    }
                }
                if ((lower32 >>> 24) == 1) {
                    if ((levels2 & 4) != 0) {
                        if (!callback.needToWrite(2)) {
                            return;
                        }
                        sink.Append(1);
                        secondaries.appendTo(sink);
                    }
                    if ((levels2 & 8) != 0) {
                        if (!callback.needToWrite(3)) {
                            return;
                        }
                        sink.Append(1);
                        int length = cases.length() - 1;
                        byte b9 = 0;
                        for (int i2 = 0; i2 < length; i2++) {
                            byte c2 = cases.getAt(i2);
                            if (!f46assertionsDisabled) {
                                if (!((c2 & 15) == 0 && c2 != 0)) {
                                    throw new AssertionError();
                                }
                            }
                            if (b9 == 0) {
                                b9 = c2;
                            } else {
                                sink.Append(((c2 >> 4) & 15) | b9);
                                b9 = 0;
                            }
                        }
                        if (b9 != 0) {
                            sink.Append(b9);
                        }
                    }
                    if ((levels2 & 16) != 0) {
                        if (!callback.needToWrite(4)) {
                            return;
                        }
                        sink.Append(1);
                        tertiaries.appendTo(sink);
                    }
                    if ((levels2 & 32) == 0 || !callback.needToWrite(5)) {
                        return;
                    }
                    sink.Append(1);
                    quaternaries.appendTo(sink);
                    return;
                }
            }
        }
    }
}
