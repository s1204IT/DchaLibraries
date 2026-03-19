package android.icu.impl.coll;

public final class CollationKeys {
    static final boolean $assertionsDisabled = false;
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
    public static final LevelCallback SIMPLE_LEVEL_FALLBACK = new LevelCallback();
    private static final int[] levelMasks = {2, 6, 22, 54, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 54};

    public static abstract class SortKeyByteSink {
        private int appended_ = 0;
        protected byte[] buffer_;

        protected abstract void AppendBeyondCapacity(byte[] bArr, int i, int i2, int i3);

        protected abstract boolean Resize(int i, int i2);

        public SortKeyByteSink(byte[] bArr) {
            this.buffer_ = bArr;
        }

        public void setBufferAndAppended(byte[] bArr, int i) {
            this.buffer_ = bArr;
            this.appended_ = i;
        }

        public void Append(byte[] bArr, int i) {
            if (i <= 0 || bArr == null) {
                return;
            }
            int i2 = this.appended_;
            this.appended_ += i;
            if (i <= this.buffer_.length - i2) {
                System.arraycopy(bArr, 0, this.buffer_, i2, i);
            } else {
                AppendBeyondCapacity(bArr, 0, i, i2);
            }
        }

        public void Append(int i) {
            if (this.appended_ < this.buffer_.length || Resize(1, this.appended_)) {
                this.buffer_[this.appended_] = (byte) i;
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
        boolean needToWrite(int i) {
            return true;
        }
    }

    private static final class SortKeyLevel {
        static final boolean $assertionsDisabled = false;
        private static final int INITIAL_CAPACITY = 40;
        byte[] buffer = new byte[40];
        int len = 0;

        SortKeyLevel() {
        }

        boolean isEmpty() {
            return this.len == 0;
        }

        int length() {
            return this.len;
        }

        byte getAt(int i) {
            return this.buffer[i];
        }

        byte[] data() {
            return this.buffer;
        }

        void appendByte(int i) {
            if (this.len < this.buffer.length || ensureCapacity(1)) {
                byte[] bArr = this.buffer;
                int i2 = this.len;
                this.len = i2 + 1;
                bArr[i2] = (byte) i;
            }
        }

        void appendWeight16(int i) {
            int i2;
            byte b = (byte) (i >>> 8);
            byte b2 = (byte) i;
            if (b2 != 0) {
                i2 = 2;
            } else {
                i2 = 1;
            }
            if (this.len + i2 <= this.buffer.length || ensureCapacity(i2)) {
                byte[] bArr = this.buffer;
                int i3 = this.len;
                this.len = i3 + 1;
                bArr[i3] = b;
                if (b2 != 0) {
                    byte[] bArr2 = this.buffer;
                    int i4 = this.len;
                    this.len = i4 + 1;
                    bArr2[i4] = b2;
                }
            }
        }

        void appendWeight32(long j) {
            int i = 4;
            byte[] bArr = {(byte) (j >>> 24), (byte) (j >>> 16), (byte) (j >>> 8), (byte) j};
            if (bArr[1] == 0) {
                i = 1;
            } else if (bArr[2] == 0) {
                i = 2;
            } else if (bArr[3] == 0) {
                i = 3;
            }
            if (this.len + i <= this.buffer.length || ensureCapacity(i)) {
                byte[] bArr2 = this.buffer;
                int i2 = this.len;
                this.len = i2 + 1;
                bArr2[i2] = bArr[0];
                if (bArr[1] != 0) {
                    byte[] bArr3 = this.buffer;
                    int i3 = this.len;
                    this.len = i3 + 1;
                    bArr3[i3] = bArr[1];
                    if (bArr[2] != 0) {
                        byte[] bArr4 = this.buffer;
                        int i4 = this.len;
                        this.len = i4 + 1;
                        bArr4[i4] = bArr[2];
                        if (bArr[3] != 0) {
                            byte[] bArr5 = this.buffer;
                            int i5 = this.len;
                            this.len = i5 + 1;
                            bArr5[i5] = bArr[3];
                        }
                    }
                }
            }
        }

        void appendReverseWeight16(int i) {
            byte b = (byte) (i >>> 8);
            byte b2 = (byte) i;
            int i2 = b2 == 0 ? 1 : 2;
            if (this.len + i2 <= this.buffer.length || ensureCapacity(i2)) {
                if (b2 == 0) {
                    byte[] bArr = this.buffer;
                    int i3 = this.len;
                    this.len = i3 + 1;
                    bArr[i3] = b;
                    return;
                }
                this.buffer[this.len] = b2;
                this.buffer[this.len + 1] = b;
                this.len += 2;
            }
        }

        void appendTo(SortKeyByteSink sortKeyByteSink) {
            sortKeyByteSink.Append(this.buffer, this.len - 1);
        }

        private boolean ensureCapacity(int i) {
            int length = this.buffer.length * 2;
            int i2 = this.len + (2 * i);
            if (length >= i2) {
                i2 = length;
            }
            if (i2 < 200) {
                i2 = 200;
            }
            byte[] bArr = new byte[i2];
            System.arraycopy(this.buffer, 0, bArr, 0, this.len);
            this.buffer = bArr;
            return true;
        }
    }

    private static SortKeyLevel getSortKeyLevel(int i, int i2) {
        if ((i & i2) != 0) {
            return new SortKeyLevel();
        }
        return null;
    }

    private CollationKeys() {
    }

    public static void writeSortKeyUpToQuaternary(CollationIterator collationIterator, boolean[] zArr, CollationSettings collationSettings, SortKeyByteSink sortKeyByteSink, int i, LevelCallback levelCallback, boolean z) {
        long j;
        int i2;
        int i3;
        long j2;
        long jReorder;
        SortKeyLevel sortKeyLevel;
        int i4;
        long j3;
        int i5;
        SortKeyLevel sortKeyLevel2;
        LevelCallback levelCallback2;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        int i13;
        int i14;
        int i15;
        int i16;
        byte b;
        int i17;
        int i18;
        long jNextCE;
        CollationSettings collationSettings2 = collationSettings;
        SortKeyByteSink sortKeyByteSink2 = sortKeyByteSink;
        int i19 = collationSettings2.options;
        int i20 = levelMasks[CollationSettings.getStrength(i19)];
        if ((i19 & 1024) != 0) {
            i20 |= 8;
        }
        int i21 = i20 & (~((1 << i) - 1));
        if (i21 == 0) {
            return;
        }
        int i22 = i19 & 12;
        if (i22 != 0) {
            j = collationSettings2.variableTop + 1;
        } else {
            j = 0;
        }
        int tertiaryMask = CollationSettings.getTertiaryMask(i19);
        byte[] bArr = new byte[3];
        SortKeyLevel sortKeyLevel3 = getSortKeyLevel(i21, 8);
        SortKeyLevel sortKeyLevel4 = getSortKeyLevel(i21, 4);
        SortKeyLevel sortKeyLevel5 = getSortKeyLevel(i21, 16);
        char c = ' ';
        SortKeyLevel sortKeyLevel6 = getSortKeyLevel(i21, 32);
        int i23 = 0;
        long j4 = 0;
        int i24 = 0;
        int i25 = 0;
        int length = 0;
        int i26 = 0;
        int i27 = 0;
        while (true) {
            collationIterator.clearCEsIfNoneRemaining();
            long jNextCE2 = collationIterator.nextCE();
            long j5 = jNextCE2 >>> c;
            if (j5 >= j || j5 <= Collation.MERGE_SEPARATOR_PRIMARY) {
                i2 = i22;
                i3 = i19;
                j2 = jNextCE2;
                jReorder = j5;
            } else {
                if (i23 != 0) {
                    int i28 = i23 - 1;
                    while (i28 >= 113) {
                        sortKeyLevel6.appendByte(140);
                        i28 -= 113;
                    }
                    sortKeyLevel6.appendByte(28 + i28);
                    i2 = i22;
                    jReorder = j5;
                    i17 = 0;
                } else {
                    i2 = i22;
                    i17 = i23;
                    jReorder = j5;
                }
                while (true) {
                    if ((i21 & 32) != 0) {
                        if (collationSettings.hasReordering()) {
                            jReorder = collationSettings2.reorder(jReorder);
                        }
                        i18 = i17;
                        if ((((int) jReorder) >>> 24) >= 27) {
                            sortKeyLevel6.appendByte(27);
                        }
                        sortKeyLevel6.appendWeight32(jReorder);
                    } else {
                        i18 = i17;
                    }
                    do {
                        jNextCE = collationIterator.nextCE();
                        jReorder = jNextCE >>> 32;
                    } while (jReorder == 0);
                    if (jReorder >= j || jReorder <= Collation.MERGE_SEPARATOR_PRIMARY) {
                        break;
                    } else {
                        i17 = i18;
                    }
                }
                i3 = i19;
                j2 = jNextCE;
                i23 = i18;
            }
            long j6 = j;
            if (jReorder > 1 && (i21 & 2) != 0) {
                boolean z2 = zArr[((int) jReorder) >>> 24];
                if (collationSettings.hasReordering()) {
                    jReorder = collationSettings2.reorder(jReorder);
                }
                int i29 = (int) jReorder;
                int i30 = i29 >>> 24;
                if (!z2) {
                    sortKeyLevel = sortKeyLevel6;
                    i4 = tertiaryMask;
                    j3 = j4;
                } else {
                    sortKeyLevel = sortKeyLevel6;
                    i4 = tertiaryMask;
                    j3 = j4;
                    if (i30 != (((int) j3) >>> 24)) {
                    }
                    b = (byte) (jReorder >>> 16);
                    if (b == 0) {
                        bArr[0] = b;
                        bArr[1] = (byte) (jReorder >>> 8);
                        bArr[2] = (byte) i29;
                        sortKeyByteSink2 = sortKeyByteSink;
                        sortKeyByteSink2.Append(bArr, bArr[1] == 0 ? 1 : bArr[2] == 0 ? 2 : 3);
                    }
                    if (!z && sortKeyByteSink.Overflowed()) {
                        return;
                    }
                }
                if (j3 != 0) {
                    if (jReorder < j3) {
                        if (i30 > 2) {
                            sortKeyByteSink2.Append(3);
                        }
                    } else {
                        sortKeyByteSink2.Append(255);
                    }
                }
                sortKeyByteSink2.Append(i30);
                j3 = z2 ? jReorder : 0L;
                b = (byte) (jReorder >>> 16);
                if (b == 0) {
                }
                if (!z) {
                    return;
                }
            } else {
                sortKeyLevel = sortKeyLevel6;
                i4 = tertiaryMask;
                j3 = j4;
            }
            j4 = j3;
            int i31 = (int) j2;
            if (i31 == 0) {
                i22 = i2;
                i19 = i3;
                j = j6;
                tertiaryMask = i4;
                sortKeyLevel6 = sortKeyLevel;
                collationSettings2 = collationSettings;
            } else {
                int i32 = i21 & 4;
                if (i32 == 0 || (i14 = i31 >>> 16) == 0) {
                    i5 = i3;
                    i25 = i25;
                } else {
                    if (i14 == 1280) {
                        i5 = i3;
                        if ((i5 & 2048) == 0 || jReorder != Collation.MERGE_SEPARATOR_PRIMARY) {
                            i24++;
                        }
                    } else {
                        i5 = i3;
                    }
                    if ((i5 & 2048) == 0) {
                        if (i24 != 0) {
                            int i33 = i24 - 1;
                            while (i33 >= 33) {
                                sortKeyLevel4.appendByte(37);
                                i33 -= 33;
                            }
                            if (i14 < 1280) {
                                i16 = i33 + 5;
                            } else {
                                i16 = 69 - i33;
                            }
                            sortKeyLevel4.appendByte(i16);
                            i24 = 0;
                        }
                        sortKeyLevel4.appendWeight16(i14);
                    } else {
                        if (i24 != 0) {
                            int i34 = i24 - 1;
                            int i35 = i34 % 33;
                            if (i25 < 1280) {
                                i15 = 5 + i35;
                            } else {
                                i15 = 69 - i35;
                            }
                            sortKeyLevel4.appendByte(i15);
                            i24 = i34 - i35;
                            while (i24 > 0) {
                                sortKeyLevel4.appendByte(37);
                                i24 -= 33;
                            }
                        }
                        if (0 < jReorder && jReorder <= Collation.MERGE_SEPARATOR_PRIMARY) {
                            byte[] bArrData = sortKeyLevel4.data();
                            int length2 = sortKeyLevel4.length() - 1;
                            for (int i36 = length; i36 < length2; i36++) {
                                byte b2 = bArrData[i36];
                                bArrData[i36] = bArrData[length2];
                                bArrData[length2] = b2;
                                length2--;
                            }
                            sortKeyLevel4.appendByte(jReorder == 1 ? 1 : 2);
                            length = sortKeyLevel4.length();
                            i25 = 0;
                        } else {
                            sortKeyLevel4.appendReverseWeight16(i14);
                            i25 = i14;
                        }
                    }
                }
                int i37 = i21 & 8;
                if (i37 != 0 && (CollationSettings.getStrength(i5) != 0 ? (i31 >>> 16) != 0 : jReorder != 0)) {
                    int i38 = (i31 >>> 8) & 255;
                    if ((i38 & 192) == 0 && i38 > 1) {
                        i26++;
                    } else {
                        if ((i5 & 256) == 0) {
                            if (i26 != 0 && (i38 > 1 || !sortKeyLevel3.isEmpty())) {
                                int i39 = i26 - 1;
                                while (i39 >= 7) {
                                    sortKeyLevel3.appendByte(112);
                                    i39 -= 7;
                                }
                                if (i38 <= 1) {
                                    i13 = i39 + 1;
                                } else {
                                    i13 = 13 - i39;
                                }
                                sortKeyLevel3.appendByte(i13 << 4);
                                i26 = 0;
                            }
                            if (i38 > 1) {
                                i38 = (13 + (i38 >>> 6)) << 4;
                            }
                        } else {
                            if (i26 != 0) {
                                int i40 = i26 - 1;
                                while (i40 >= 13) {
                                    sortKeyLevel3.appendByte(48);
                                    i40 -= 13;
                                }
                                i12 = 4;
                                sortKeyLevel3.appendByte((i40 + 3) << 4);
                                i26 = 0;
                            } else {
                                i12 = 4;
                            }
                            if (i38 > 1) {
                                i38 = (3 - (i38 >>> 6)) << i12;
                            }
                        }
                        sortKeyLevel3.appendByte(i38);
                    }
                }
                int i41 = i21 & 16;
                if (i41 != 0) {
                    int i42 = i31 & i4;
                    if (i42 == 1280) {
                        i27++;
                    } else if ((i4 & 32768) == 0) {
                        if (i27 != 0) {
                            int i43 = i27 - 1;
                            while (i43 >= 97) {
                                sortKeyLevel5.appendByte(101);
                                i43 -= 97;
                            }
                            if (i42 < 1280) {
                                i11 = i43 + 5;
                            } else {
                                i11 = 197 - i43;
                            }
                            sortKeyLevel5.appendByte(i11);
                            i27 = 0;
                        }
                        if (i42 > 1280) {
                            i42 += Collation.CASE_MASK;
                        }
                        sortKeyLevel5.appendWeight16(i42);
                    } else if ((i5 & 256) == 0) {
                        if (i27 != 0) {
                            int i44 = i27 - 1;
                            while (i44 >= 33) {
                                sortKeyLevel5.appendByte(37);
                                i44 -= 33;
                            }
                            if (i42 < 1280) {
                                i10 = i44 + 5;
                            } else {
                                i10 = 69 - i44;
                            }
                            sortKeyLevel5.appendByte(i10);
                            i27 = 0;
                        }
                        if (i42 > 1280) {
                            i42 += 16384;
                        }
                        sortKeyLevel5.appendWeight16(i42);
                    } else {
                        if (i42 > 256) {
                            if ((i31 >>> 16) != 0) {
                                i42 ^= Collation.CASE_MASK;
                                if (i42 < 50432) {
                                    i42 -= 16384;
                                }
                            } else {
                                i42 += 16384;
                            }
                        }
                        if (i27 != 0) {
                            int i45 = i27 - 1;
                            while (i45 >= 33) {
                                sortKeyLevel5.appendByte(165);
                                i45 -= 33;
                            }
                            if (i42 < 34048) {
                                i9 = 133 + i45;
                            } else {
                                i9 = 197 - i45;
                            }
                            sortKeyLevel5.appendByte(i9);
                            i27 = 0;
                        }
                        sortKeyLevel5.appendWeight16(i42);
                    }
                }
                int i46 = i21 & 32;
                if (i46 != 0) {
                    int i47 = 65535 & i31;
                    if ((i47 & 192) == 0) {
                        i6 = 256;
                        if (i47 > 256) {
                            i23++;
                            sortKeyLevel2 = sortKeyLevel;
                            if ((i31 >>> 24) != 1) {
                                if (i32 != 0) {
                                    levelCallback2 = levelCallback;
                                    if (!levelCallback2.needToWrite(2)) {
                                        return;
                                    }
                                    sortKeyByteSink2.Append(1);
                                    sortKeyLevel4.appendTo(sortKeyByteSink2);
                                } else {
                                    levelCallback2 = levelCallback;
                                }
                                if (i37 != 0) {
                                    if (!levelCallback2.needToWrite(3)) {
                                        return;
                                    }
                                    sortKeyByteSink2.Append(1);
                                    int length3 = sortKeyLevel3.length() - 1;
                                    byte b3 = 0;
                                    for (int i48 = 0; i48 < length3; i48++) {
                                        byte at = sortKeyLevel3.getAt(i48);
                                        if (b3 == 0) {
                                            b3 = at;
                                        } else {
                                            sortKeyByteSink2.Append(b3 | ((at >> 4) & 15));
                                            b3 = 0;
                                        }
                                    }
                                    if (b3 != 0) {
                                        sortKeyByteSink2.Append(b3);
                                    }
                                }
                                if (i41 != 0) {
                                    if (!levelCallback2.needToWrite(4)) {
                                        return;
                                    }
                                    sortKeyByteSink2.Append(1);
                                    sortKeyLevel5.appendTo(sortKeyByteSink2);
                                }
                                if (i46 == 0 || !levelCallback2.needToWrite(5)) {
                                    return;
                                }
                                sortKeyByteSink2.Append(1);
                                sortKeyLevel2.appendTo(sortKeyByteSink2);
                                return;
                            }
                            i19 = i5;
                            sortKeyLevel6 = sortKeyLevel2;
                            i22 = i2;
                            j = j6;
                            tertiaryMask = i4;
                            collationSettings2 = collationSettings;
                        }
                    } else {
                        i6 = 256;
                    }
                    if (i47 == i6 && i2 == 0) {
                        sortKeyLevel2 = sortKeyLevel;
                        if (sortKeyLevel2.isEmpty()) {
                            sortKeyLevel2.appendByte(1);
                        }
                        if ((i31 >>> 24) != 1) {
                        }
                    } else {
                        sortKeyLevel2 = sortKeyLevel;
                    }
                    if (i47 != 256) {
                        i7 = ((i47 >>> 6) & 3) + 252;
                    } else {
                        i7 = 1;
                    }
                    if (i23 != 0) {
                        int i49 = i23 - 1;
                        while (i49 >= 113) {
                            sortKeyLevel2.appendByte(140);
                            i49 -= 113;
                        }
                        if (i7 < 28) {
                            i8 = 28 + i49;
                        } else {
                            i8 = 252 - i49;
                        }
                        sortKeyLevel2.appendByte(i8);
                        i23 = 0;
                    }
                    sortKeyLevel2.appendByte(i7);
                    if ((i31 >>> 24) != 1) {
                    }
                } else {
                    sortKeyLevel2 = sortKeyLevel;
                    if ((i31 >>> 24) != 1) {
                    }
                }
            }
            c = ' ';
        }
    }
}
