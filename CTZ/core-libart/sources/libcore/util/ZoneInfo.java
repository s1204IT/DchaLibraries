package libcore.util;

import android.icu.lang.UCharacter;
import android.icu.lang.UCharacterEnums;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import libcore.io.BufferIterator;

public final class ZoneInfo extends TimeZone {
    private static final long MILLISECONDS_PER_400_YEARS = 12622780800000L;
    private static final long MILLISECONDS_PER_DAY = 86400000;
    private static final long UNIX_OFFSET = 62167219200000L;
    static final long serialVersionUID = -4598738130123921552L;
    private int mDstSavings;
    private final int mEarliestRawOffset;
    private final byte[] mIsDsts;
    private final int[] mOffsets;
    private int mRawOffset;
    private final long[] mTransitions;
    private final byte[] mTypes;
    private final boolean mUseDst;
    private static final int[] NORMAL = {0, 31, 59, 90, 120, 151, 181, 212, 243, UCharacter.UnicodeBlock.TANGUT_COMPONENTS_ID, 304, 334};
    private static final int[] LEAP = {0, 31, 60, 91, 121, 152, 182, 213, 244, UCharacter.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F_ID, 305, 335};

    public static ZoneInfo readTimeZone(String str, BufferIterator bufferIterator, long j) throws IOException {
        int i = bufferIterator.readInt();
        if (i != 1415211366) {
            throw new IOException("Timezone id=" + str + " has an invalid header=" + i);
        }
        bufferIterator.skip(28);
        int i2 = bufferIterator.readInt();
        if (i2 < 0 || i2 > 2000) {
            throw new IOException("Timezone id=" + str + " has an invalid number of transitions=" + i2);
        }
        int i3 = bufferIterator.readInt();
        if (i3 < 1) {
            throw new IOException("ZoneInfo requires at least one type to be provided for each timezone but could not find one for '" + str + "'");
        }
        if (i3 > 256) {
            throw new IOException("Timezone with id " + str + " has too many types=" + i3);
        }
        bufferIterator.skip(4);
        int[] iArr = new int[i2];
        bufferIterator.readIntArray(iArr, 0, iArr.length);
        long[] jArr = new long[i2];
        for (int i4 = 0; i4 < i2; i4++) {
            jArr[i4] = iArr[i4];
            if (i4 > 0) {
                int i5 = i4 - 1;
                if (jArr[i4] <= jArr[i5]) {
                    throw new IOException(str + " transition at " + i4 + " is not sorted correctly, is " + jArr[i4] + ", previous is " + jArr[i5]);
                }
            }
        }
        byte[] bArr = new byte[i2];
        bufferIterator.readByteArray(bArr, 0, bArr.length);
        for (int i6 = 0; i6 < bArr.length; i6++) {
            int i7 = bArr[i6] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            if (i7 >= i3) {
                throw new IOException(str + " type at " + i6 + " is not < " + i3 + ", is " + i7);
            }
        }
        int[] iArr2 = new int[i3];
        byte[] bArr2 = new byte[i3];
        for (int i8 = 0; i8 < i3; i8++) {
            iArr2[i8] = bufferIterator.readInt();
            byte b = bufferIterator.readByte();
            if (b != 0 && b != 1) {
                throw new IOException(str + " dst at " + i8 + " is not 0 or 1, is " + ((int) b));
            }
            bArr2[i8] = b;
            bufferIterator.skip(1);
        }
        return new ZoneInfo(str, jArr, bArr, iArr2, bArr2, j);
    }

    private ZoneInfo(String str, long[] jArr, byte[] bArr, int[] iArr, byte[] bArr2, long j) {
        if (iArr.length == 0) {
            throw new IllegalArgumentException("ZoneInfo requires at least one offset to be provided for each timezone but could not find one for '" + str + "'");
        }
        this.mTransitions = jArr;
        this.mTypes = bArr;
        this.mIsDsts = bArr2;
        setID(str);
        int length = this.mTransitions.length - 1;
        int i = -1;
        int i2 = -1;
        while (true) {
            if ((i != -1 && i2 != -1) || length < 0) {
                break;
            }
            int i3 = this.mTypes[length] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            if (i == -1 && this.mIsDsts[i3] == 0) {
                i = length;
            }
            if (i2 == -1 && this.mIsDsts[i3] != 0) {
                i2 = length;
            }
            length--;
        }
        if (this.mTransitions.length == 0) {
            this.mRawOffset = iArr[0];
        } else {
            if (i == -1) {
                throw new IllegalStateException("ZoneInfo requires at least one non-DST transition to be provided for each timezone that has at least one transition but could not find one for '" + str + "'");
            }
            this.mRawOffset = iArr[this.mTypes[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED];
        }
        if (i2 != -1 && this.mTransitions[i2] < roundUpMillisToSeconds(j)) {
            i2 = -1;
        }
        if (i2 == -1) {
            this.mDstSavings = 0;
            this.mUseDst = false;
        } else {
            this.mDstSavings = (iArr[this.mTypes[i2] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED] - iArr[this.mTypes[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED]) * 1000;
            this.mUseDst = true;
        }
        int i4 = 0;
        while (true) {
            if (i4 < this.mTransitions.length) {
                if (this.mIsDsts[this.mTypes[i4] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED] == 0) {
                    break;
                } else {
                    i4++;
                }
            } else {
                i4 = -1;
                break;
            }
        }
        int i5 = i4 != -1 ? iArr[this.mTypes[i4] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED] : this.mRawOffset;
        this.mOffsets = iArr;
        for (int i6 = 0; i6 < this.mOffsets.length; i6++) {
            int[] iArr2 = this.mOffsets;
            iArr2[i6] = iArr2[i6] - this.mRawOffset;
        }
        this.mRawOffset *= 1000;
        this.mEarliestRawOffset = i5 * 1000;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (!this.mUseDst && this.mDstSavings != 0) {
            this.mDstSavings = 0;
        }
    }

    @Override
    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6) {
        long j = ((long) (i2 / 400)) * MILLISECONDS_PER_400_YEARS;
        int i7 = i2 % 400;
        long j2 = j + (((long) i7) * 31536000000L) + (((long) ((i7 + 3) / 4)) * 86400000);
        if (i7 > 0) {
            j2 -= ((long) ((i7 - 1) / 100)) * 86400000;
        }
        return getOffset(((((j2 + (((long) (i7 == 0 || (i7 % 4 == 0 && i7 % 100 != 0) ? LEAP : NORMAL)[i3]) * 86400000)) + (((long) (i4 - 1)) * 86400000)) + ((long) i6)) - ((long) this.mRawOffset)) - UNIX_OFFSET);
    }

    public int findTransitionIndex(long j) {
        int iBinarySearch = Arrays.binarySearch(this.mTransitions, j);
        if (iBinarySearch < 0 && (~iBinarySearch) - 1 < 0) {
            return -1;
        }
        return iBinarySearch;
    }

    int findOffsetIndexForTimeInSeconds(long j) {
        int iFindTransitionIndex = findTransitionIndex(j);
        if (iFindTransitionIndex < 0) {
            return -1;
        }
        return this.mTypes[iFindTransitionIndex] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
    }

    int findOffsetIndexForTimeInMilliseconds(long j) {
        return findOffsetIndexForTimeInSeconds(roundDownMillisToSeconds(j));
    }

    static long roundDownMillisToSeconds(long j) {
        if (j < 0) {
            return (j - 999) / 1000;
        }
        return j / 1000;
    }

    static long roundUpMillisToSeconds(long j) {
        if (j > 0) {
            return (j + 999) / 1000;
        }
        return j / 1000;
    }

    public int getOffsetsByUtcTime(long j, int[] iArr) {
        int i;
        int i2;
        int i3;
        int iFindTransitionIndex = findTransitionIndex(roundDownMillisToSeconds(j));
        if (iFindTransitionIndex == -1) {
            i2 = this.mEarliestRawOffset;
            i = i2;
            i3 = 0;
        } else {
            int i4 = this.mTypes[iFindTransitionIndex] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
            i = this.mRawOffset + (this.mOffsets[i4] * 1000);
            if (this.mIsDsts[i4] != 0) {
                int i5 = iFindTransitionIndex - 1;
                while (true) {
                    if (i5 >= 0) {
                        int i6 = this.mTypes[i5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                        if (this.mIsDsts[i6] != 0) {
                            i5--;
                        } else {
                            i2 = this.mRawOffset + (this.mOffsets[i6] * 1000);
                            break;
                        }
                    } else {
                        i2 = -1;
                        break;
                    }
                }
                if (i2 == -1) {
                    i2 = this.mEarliestRawOffset;
                }
                i3 = i - i2;
            } else {
                i3 = 0;
                i2 = i;
            }
        }
        iArr[0] = i2;
        iArr[1] = i3;
        return i;
    }

    @Override
    public int getOffset(long j) {
        int iFindOffsetIndexForTimeInMilliseconds = findOffsetIndexForTimeInMilliseconds(j);
        if (iFindOffsetIndexForTimeInMilliseconds == -1) {
            return this.mEarliestRawOffset;
        }
        return this.mRawOffset + (this.mOffsets[iFindOffsetIndexForTimeInMilliseconds] * 1000);
    }

    @Override
    public boolean inDaylightTime(Date date) {
        int iFindOffsetIndexForTimeInMilliseconds = findOffsetIndexForTimeInMilliseconds(date.getTime());
        return iFindOffsetIndexForTimeInMilliseconds != -1 && this.mIsDsts[iFindOffsetIndexForTimeInMilliseconds] == 1;
    }

    @Override
    public int getRawOffset() {
        return this.mRawOffset;
    }

    @Override
    public void setRawOffset(int i) {
        this.mRawOffset = i;
    }

    @Override
    public int getDSTSavings() {
        return this.mDstSavings;
    }

    @Override
    public boolean useDaylightTime() {
        return this.mUseDst;
    }

    @Override
    public boolean hasSameRules(TimeZone timeZone) {
        if (!(timeZone instanceof ZoneInfo)) {
            return false;
        }
        ZoneInfo zoneInfo = (ZoneInfo) timeZone;
        if (this.mUseDst != zoneInfo.mUseDst) {
            return false;
        }
        return !this.mUseDst ? this.mRawOffset == zoneInfo.mRawOffset : this.mRawOffset == zoneInfo.mRawOffset && Arrays.equals(this.mOffsets, zoneInfo.mOffsets) && Arrays.equals(this.mIsDsts, zoneInfo.mIsDsts) && Arrays.equals(this.mTypes, zoneInfo.mTypes) && Arrays.equals(this.mTransitions, zoneInfo.mTransitions);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ZoneInfo)) {
            return false;
        }
        ZoneInfo zoneInfo = (ZoneInfo) obj;
        return getID().equals(zoneInfo.getID()) && hasSameRules(zoneInfo);
    }

    public int hashCode() {
        return (31 * (((((((((((getID().hashCode() + 31) * 31) + Arrays.hashCode(this.mOffsets)) * 31) + Arrays.hashCode(this.mIsDsts)) * 31) + this.mRawOffset) * 31) + Arrays.hashCode(this.mTransitions)) * 31) + Arrays.hashCode(this.mTypes))) + (this.mUseDst ? 1231 : 1237);
    }

    public String toString() {
        return getClass().getName() + "[id=\"" + getID() + "\",mRawOffset=" + this.mRawOffset + ",mEarliestRawOffset=" + this.mEarliestRawOffset + ",mUseDst=" + this.mUseDst + ",mDstSavings=" + this.mDstSavings + ",transitions=" + this.mTransitions.length + "]";
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    public static class WallTime {
        private final GregorianCalendar calendar = new GregorianCalendar(0, 0, 0, 0, 0, 0);
        private int gmtOffsetSeconds;
        private int hour;
        private int isDst;
        private int minute;
        private int month;
        private int monthDay;
        private int second;
        private int weekDay;
        private int year;
        private int yearDay;

        public WallTime() {
            this.calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
        }

        public void localtime(int i, ZoneInfo zoneInfo) {
            int iFindOffsetIndexForTimeInSeconds;
            try {
                int i2 = zoneInfo.mRawOffset / 1000;
                byte b = 0;
                if (zoneInfo.mTransitions.length != 0 && (iFindOffsetIndexForTimeInSeconds = zoneInfo.findOffsetIndexForTimeInSeconds(i)) != -1) {
                    i2 += zoneInfo.mOffsets[iFindOffsetIndexForTimeInSeconds];
                    b = zoneInfo.mIsDsts[iFindOffsetIndexForTimeInSeconds];
                }
                this.calendar.setTimeInMillis(((long) ZoneInfo.checkedAdd(i, i2)) * 1000);
                copyFieldsFromCalendar();
                this.isDst = b;
                this.gmtOffsetSeconds = i2;
            } catch (CheckedArithmeticException e) {
            }
        }

        public int mktime(ZoneInfo zoneInfo) {
            int i;
            if (this.isDst > 0) {
                this.isDst = 1;
                i = 1;
            } else if (this.isDst < 0) {
                this.isDst = -1;
                i = -1;
            } else {
                i = 0;
            }
            this.isDst = i;
            copyFieldsToCalendar();
            long timeInMillis = this.calendar.getTimeInMillis() / 1000;
            if (-2147483648L > timeInMillis || timeInMillis > 2147483647L) {
                return -1;
            }
            int i2 = (int) timeInMillis;
            try {
                int i3 = zoneInfo.mRawOffset / 1000;
                int iCheckedSubtract = ZoneInfo.checkedSubtract(i2, i3);
                if (zoneInfo.mTransitions.length == 0) {
                    if (this.isDst > 0) {
                        return -1;
                    }
                    copyFieldsFromCalendar();
                    this.isDst = 0;
                    this.gmtOffsetSeconds = i3;
                    return iCheckedSubtract;
                }
                int iFindTransitionIndex = zoneInfo.findTransitionIndex(iCheckedSubtract);
                if (this.isDst < 0) {
                    Integer numDoWallTimeSearch = doWallTimeSearch(zoneInfo, iFindTransitionIndex, i2, true);
                    if (numDoWallTimeSearch == null) {
                        return -1;
                    }
                    return numDoWallTimeSearch.intValue();
                }
                Integer numDoWallTimeSearch2 = doWallTimeSearch(zoneInfo, iFindTransitionIndex, i2, true);
                if (numDoWallTimeSearch2 == null) {
                    numDoWallTimeSearch2 = doWallTimeSearch(zoneInfo, iFindTransitionIndex, i2, false);
                }
                if (numDoWallTimeSearch2 == null) {
                    numDoWallTimeSearch2 = -1;
                }
                return numDoWallTimeSearch2.intValue();
            } catch (CheckedArithmeticException e) {
                return -1;
            }
        }

        private Integer tryOffsetAdjustments(ZoneInfo zoneInfo, int i, OffsetInterval offsetInterval, int i2, int i3) throws CheckedArithmeticException {
            int[] offsetsOfType = getOffsetsOfType(zoneInfo, i2, i3);
            for (int i4 : offsetsOfType) {
                int i5 = (zoneInfo.mRawOffset / 1000) + i4;
                int totalOffsetSeconds = offsetInterval.getTotalOffsetSeconds();
                int iCheckedAdd = ZoneInfo.checkedAdd(i, totalOffsetSeconds - i5);
                long j = iCheckedAdd;
                if (offsetInterval.containsWallTime(j)) {
                    int iCheckedSubtract = ZoneInfo.checkedSubtract(iCheckedAdd, totalOffsetSeconds);
                    this.calendar.setTimeInMillis(j * 1000);
                    copyFieldsFromCalendar();
                    this.isDst = offsetInterval.getIsDst();
                    this.gmtOffsetSeconds = totalOffsetSeconds;
                    return Integer.valueOf(iCheckedSubtract);
                }
            }
            return null;
        }

        private static int[] getOffsetsOfType(ZoneInfo zoneInfo, int i, int i2) {
            int[] iArr = new int[zoneInfo.mOffsets.length + 1];
            boolean[] zArr = new boolean[zoneInfo.mOffsets.length];
            int i3 = 0;
            int i4 = 0;
            boolean z = false;
            boolean z2 = false;
            while (true) {
                i3 *= -1;
                if (i3 >= 0) {
                    i3++;
                }
                int i5 = i + i3;
                if (i3 < 0 && i5 < -1) {
                    z2 = true;
                } else if (i3 > 0 && i5 >= zoneInfo.mTypes.length) {
                    z = true;
                } else if (i5 != -1) {
                    int i6 = zoneInfo.mTypes[i5] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    if (!zArr[i6]) {
                        if (zoneInfo.mIsDsts[i6] == i2) {
                            iArr[i4] = zoneInfo.mOffsets[i6];
                            i4++;
                        }
                        zArr[i6] = true;
                    }
                } else if (i2 == 0) {
                    iArr[i4] = 0;
                    i4++;
                }
                if (z && z2) {
                    int[] iArr2 = new int[i4];
                    System.arraycopy(iArr, 0, iArr2, 0, i4);
                    return iArr2;
                }
            }
        }

        private Integer doWallTimeSearch(ZoneInfo zoneInfo, int i, int i2, boolean z) throws CheckedArithmeticException {
            OffsetInterval offsetInterval;
            int i3 = 0;
            boolean z2 = false;
            boolean z3 = false;
            while (true) {
                int i4 = i3 + 1;
                int i5 = i4 / 2;
                if (i3 % 2 == 1) {
                    i5 *= -1;
                }
                int i6 = i5;
                if ((i6 <= 0 || !z2) && (i6 >= 0 || !z3)) {
                    int i7 = i + i6;
                    OffsetInterval offsetIntervalCreate = OffsetInterval.create(zoneInfo, i7);
                    if (offsetIntervalCreate == null) {
                        z2 = (i6 > 0) | z2;
                        z3 |= i6 < 0;
                    } else {
                        if (z) {
                            if (offsetIntervalCreate.containsWallTime(i2) && (this.isDst == -1 || offsetIntervalCreate.getIsDst() == this.isDst)) {
                                break;
                            }
                        } else {
                            if (this.isDst != offsetIntervalCreate.getIsDst()) {
                                offsetInterval = offsetIntervalCreate;
                                Integer numTryOffsetAdjustments = tryOffsetAdjustments(zoneInfo, i2, offsetIntervalCreate, i7, this.isDst);
                                if (numTryOffsetAdjustments != null) {
                                    return numTryOffsetAdjustments;
                                }
                            }
                            if (i6 <= 0) {
                                if (offsetInterval.getEndWallTimeSeconds() - ((long) i2) > 86400) {
                                    z2 = true;
                                }
                            } else if (i6 < 0) {
                                if (((long) i2) - offsetInterval.getStartWallTimeSeconds() >= 86400) {
                                    z3 = true;
                                }
                            }
                        }
                        offsetInterval = offsetIntervalCreate;
                        if (i6 <= 0) {
                        }
                    }
                }
                if (!z2 || !z3) {
                    i3 = i4;
                } else {
                    return null;
                }
            }
        }

        public void setYear(int i) {
            this.year = i;
        }

        public void setMonth(int i) {
            this.month = i;
        }

        public void setMonthDay(int i) {
            this.monthDay = i;
        }

        public void setHour(int i) {
            this.hour = i;
        }

        public void setMinute(int i) {
            this.minute = i;
        }

        public void setSecond(int i) {
            this.second = i;
        }

        public void setWeekDay(int i) {
            this.weekDay = i;
        }

        public void setYearDay(int i) {
            this.yearDay = i;
        }

        public void setIsDst(int i) {
            this.isDst = i;
        }

        public void setGmtOffset(int i) {
            this.gmtOffsetSeconds = i;
        }

        public int getYear() {
            return this.year;
        }

        public int getMonth() {
            return this.month;
        }

        public int getMonthDay() {
            return this.monthDay;
        }

        public int getHour() {
            return this.hour;
        }

        public int getMinute() {
            return this.minute;
        }

        public int getSecond() {
            return this.second;
        }

        public int getWeekDay() {
            return this.weekDay;
        }

        public int getYearDay() {
            return this.yearDay;
        }

        public int getGmtOffset() {
            return this.gmtOffsetSeconds;
        }

        public int getIsDst() {
            return this.isDst;
        }

        private void copyFieldsToCalendar() {
            this.calendar.set(1, this.year);
            this.calendar.set(2, this.month);
            this.calendar.set(5, this.monthDay);
            this.calendar.set(11, this.hour);
            this.calendar.set(12, this.minute);
            this.calendar.set(13, this.second);
            this.calendar.set(14, 0);
        }

        private void copyFieldsFromCalendar() {
            this.year = this.calendar.get(1);
            this.month = this.calendar.get(2);
            this.monthDay = this.calendar.get(5);
            this.hour = this.calendar.get(11);
            this.minute = this.calendar.get(12);
            this.second = this.calendar.get(13);
            this.weekDay = this.calendar.get(7) - 1;
            this.yearDay = this.calendar.get(6) - 1;
        }
    }

    static class OffsetInterval {
        private final int endWallTimeSeconds;
        private final int isDst;
        private final int startWallTimeSeconds;
        private final int totalOffsetSeconds;

        public static OffsetInterval create(ZoneInfo zoneInfo, int i) throws CheckedArithmeticException {
            int iCheckedAdd;
            if (i >= -1 && i < zoneInfo.mTransitions.length) {
                int i2 = zoneInfo.mRawOffset / 1000;
                if (i != -1) {
                    int i3 = zoneInfo.mTypes[i] & UCharacterEnums.ECharacterDirection.DIRECTIONALITY_UNDEFINED;
                    int i4 = zoneInfo.mOffsets[i3] + i2;
                    if (i != zoneInfo.mTransitions.length - 1) {
                        iCheckedAdd = ZoneInfo.checkedAdd(zoneInfo.mTransitions[i + 1], i4);
                    } else {
                        iCheckedAdd = Integer.MAX_VALUE;
                    }
                    return new OffsetInterval(ZoneInfo.checkedAdd(zoneInfo.mTransitions[i], i4), iCheckedAdd, zoneInfo.mIsDsts[i3], i4);
                }
                return new OffsetInterval(Integer.MIN_VALUE, ZoneInfo.checkedAdd(zoneInfo.mTransitions[0], i2), 0, i2);
            }
            return null;
        }

        private OffsetInterval(int i, int i2, int i3, int i4) {
            this.startWallTimeSeconds = i;
            this.endWallTimeSeconds = i2;
            this.isDst = i3;
            this.totalOffsetSeconds = i4;
        }

        public boolean containsWallTime(long j) {
            return j >= ((long) this.startWallTimeSeconds) && j < ((long) this.endWallTimeSeconds);
        }

        public int getIsDst() {
            return this.isDst;
        }

        public int getTotalOffsetSeconds() {
            return this.totalOffsetSeconds;
        }

        public long getEndWallTimeSeconds() {
            return this.endWallTimeSeconds;
        }

        public long getStartWallTimeSeconds() {
            return this.startWallTimeSeconds;
        }
    }

    private static class CheckedArithmeticException extends Exception {
        private CheckedArithmeticException() {
        }
    }

    private static int checkedAdd(long j, int i) throws CheckedArithmeticException {
        long j2 = j + ((long) i);
        int i2 = (int) j2;
        if (j2 != i2) {
            throw new CheckedArithmeticException();
        }
        return i2;
    }

    private static int checkedSubtract(int i, int i2) throws CheckedArithmeticException {
        long j = ((long) i) - ((long) i2);
        int i3 = (int) j;
        if (j != i3) {
            throw new CheckedArithmeticException();
        }
        return i3;
    }
}
