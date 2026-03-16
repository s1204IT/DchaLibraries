package libcore.util;

import dalvik.bytecode.Opcodes;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import libcore.io.BufferIterator;

public final class ZoneInfo extends TimeZone {
    private static final long MILLISECONDS_PER_400_YEARS = 12622780800000L;
    private static final long MILLISECONDS_PER_DAY = 86400000;
    private static final long UNIX_OFFSET = 62167219200000L;
    private final int mDstSavings;
    private final int mEarliestRawOffset;
    private final byte[] mIsDsts;
    private final int[] mOffsets;
    private int mRawOffset;
    private final int[] mTransitions;
    private final byte[] mTypes;
    private final boolean mUseDst;
    private static final int[] NORMAL = {0, 31, 59, 90, Opcodes.OP_INVOKE_INTERFACE_RANGE, Opcodes.OP_XOR_INT, Opcodes.OP_AND_INT_2ADDR, Opcodes.OP_REM_INT_LIT16, Opcodes.OP_IGET_WIDE_QUICK, 273, HttpURLConnection.HTTP_NOT_MODIFIED, 334};
    private static final int[] LEAP = {0, 31, 60, 91, 121, Opcodes.OP_SHL_INT, Opcodes.OP_OR_INT_2ADDR, Opcodes.OP_AND_INT_LIT16, Opcodes.OP_IGET_OBJECT_QUICK, 274, HttpURLConnection.HTTP_USE_PROXY, 335};

    public static ZoneInfo makeTimeZone(String id, BufferIterator it) {
        if (it.readInt() != 1415211366) {
            return null;
        }
        it.skip(28);
        int tzh_timecnt = it.readInt();
        int tzh_typecnt = it.readInt();
        it.skip(4);
        int[] transitions = new int[tzh_timecnt];
        it.readIntArray(transitions, 0, transitions.length);
        byte[] type = new byte[tzh_timecnt];
        it.readByteArray(type, 0, type.length);
        int[] gmtOffsets = new int[tzh_typecnt];
        byte[] isDsts = new byte[tzh_typecnt];
        for (int i = 0; i < tzh_typecnt; i++) {
            gmtOffsets[i] = it.readInt();
            isDsts[i] = it.readByte();
            it.skip(1);
        }
        return new ZoneInfo(id, transitions, type, gmtOffsets, isDsts);
    }

    private ZoneInfo(String name, int[] transitions, byte[] types, int[] gmtOffsets, byte[] isDsts) {
        this.mTransitions = transitions;
        this.mTypes = types;
        this.mIsDsts = isDsts;
        setID(name);
        int lastStd = 0;
        boolean haveStd = false;
        int lastDst = 0;
        boolean haveDst = false;
        int i = this.mTransitions.length - 1;
        while (true) {
            if ((haveStd && haveDst) || i < 0) {
                break;
            }
            int type = this.mTypes[i] & Opcodes.OP_CONST_CLASS_JUMBO;
            if (!haveStd && this.mIsDsts[type] == 0) {
                haveStd = true;
                lastStd = i;
            }
            if (!haveDst && this.mIsDsts[type] != 0) {
                haveDst = true;
                lastDst = i;
            }
            i--;
        }
        if (lastStd >= this.mTypes.length) {
            this.mRawOffset = gmtOffsets[0];
        } else {
            this.mRawOffset = gmtOffsets[this.mTypes[lastStd] & Character.DIRECTIONALITY_UNDEFINED];
        }
        if (lastDst >= this.mTypes.length) {
            this.mDstSavings = 0;
        } else {
            this.mDstSavings = Math.abs(gmtOffsets[this.mTypes[lastStd] & Character.DIRECTIONALITY_UNDEFINED] - gmtOffsets[this.mTypes[lastDst] & Character.DIRECTIONALITY_UNDEFINED]) * 1000;
        }
        int firstStd = -1;
        int i2 = 0;
        while (true) {
            if (i2 >= this.mTransitions.length) {
                break;
            }
            if (this.mIsDsts[this.mTypes[i2] & Character.DIRECTIONALITY_UNDEFINED] != 0) {
                i2++;
            } else {
                firstStd = i2;
                break;
            }
        }
        int earliestRawOffset = firstStd != -1 ? gmtOffsets[this.mTypes[firstStd] & Character.DIRECTIONALITY_UNDEFINED] : this.mRawOffset;
        this.mOffsets = gmtOffsets;
        for (int i3 = 0; i3 < this.mOffsets.length; i3++) {
            int[] iArr = this.mOffsets;
            iArr[i3] = iArr[i3] - this.mRawOffset;
        }
        boolean usesDst = false;
        int currentUnixTimeSeconds = (int) (System.currentTimeMillis() / 1000);
        int i4 = this.mTransitions.length - 1;
        while (true) {
            if (i4 < 0 || this.mTransitions[i4] < currentUnixTimeSeconds) {
                break;
            }
            if (this.mIsDsts[this.mTypes[i4]] > 0) {
                usesDst = true;
                break;
            }
            i4--;
        }
        this.mUseDst = usesDst;
        this.mRawOffset *= 1000;
        this.mEarliestRawOffset = earliestRawOffset * 1000;
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int millis) {
        long calc = ((long) (year / HttpURLConnection.HTTP_BAD_REQUEST)) * MILLISECONDS_PER_400_YEARS;
        int year2 = year % HttpURLConnection.HTTP_BAD_REQUEST;
        long calc2 = calc + (((long) year2) * 31536000000L) + (((long) ((year2 + 3) / 4)) * MILLISECONDS_PER_DAY);
        if (year2 > 0) {
            calc2 -= ((long) ((year2 - 1) / 100)) * MILLISECONDS_PER_DAY;
        }
        boolean isLeap = year2 == 0 || (year2 % 4 == 0 && year2 % 100 != 0);
        int[] mlen = isLeap ? LEAP : NORMAL;
        return getOffset(((((calc2 + (((long) mlen[month]) * MILLISECONDS_PER_DAY)) + (((long) (day - 1)) * MILLISECONDS_PER_DAY)) + ((long) millis)) - ((long) this.mRawOffset)) - UNIX_OFFSET);
    }

    @Override
    public int getOffset(long when) {
        int unix = (int) (when / 1000);
        int transition = Arrays.binarySearch(this.mTransitions, unix);
        return (transition >= 0 || (transition = (transition ^ (-1)) + (-1)) >= 0) ? this.mRawOffset + (this.mOffsets[this.mTypes[transition] & Character.DIRECTIONALITY_UNDEFINED] * 1000) : this.mEarliestRawOffset;
    }

    @Override
    public boolean inDaylightTime(Date time) {
        long when = time.getTime();
        int unix = (int) (when / 1000);
        int transition = Arrays.binarySearch(this.mTransitions, unix);
        if (transition >= 0 || (transition ^ (-1)) - 1 >= 0) {
            return this.mIsDsts[this.mTypes[transition] & Character.DIRECTIONALITY_UNDEFINED] == 1;
        }
        return false;
    }

    @Override
    public int getRawOffset() {
        return this.mRawOffset;
    }

    @Override
    public void setRawOffset(int off) {
        this.mRawOffset = off;
    }

    @Override
    public int getDSTSavings() {
        if (this.mUseDst) {
            return this.mDstSavings;
        }
        return 0;
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
        ZoneInfo other = (ZoneInfo) timeZone;
        if (this.mUseDst != other.mUseDst) {
            return false;
        }
        if (!this.mUseDst) {
            return this.mRawOffset == other.mRawOffset;
        }
        return this.mRawOffset == other.mRawOffset && Arrays.equals(this.mOffsets, other.mOffsets) && Arrays.equals(this.mIsDsts, other.mIsDsts) && Arrays.equals(this.mTypes, other.mTypes) && Arrays.equals(this.mTransitions, other.mTransitions);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ZoneInfo)) {
            return false;
        }
        ZoneInfo other = (ZoneInfo) obj;
        return getID().equals(other.getID()) && hasSameRules(other);
    }

    public int hashCode() {
        int result = getID().hashCode() + 31;
        return (((((((((((result * 31) + Arrays.hashCode(this.mOffsets)) * 31) + Arrays.hashCode(this.mIsDsts)) * 31) + this.mRawOffset) * 31) + Arrays.hashCode(this.mTransitions)) * 31) + Arrays.hashCode(this.mTypes)) * 31) + (this.mUseDst ? 1231 : 1237);
    }

    public String toString() {
        return getClass().getName() + "[id=\"" + getID() + "\",mRawOffset=" + this.mRawOffset + ",mEarliestRawOffset=" + this.mEarliestRawOffset + ",mUseDst=" + this.mUseDst + ",mDstSavings=" + this.mDstSavings + ",transitions=" + this.mTransitions.length + "]";
    }

    @Override
    public Object clone() {
        return super.clone();
    }

    public static class WallTime {
        private final GregorianCalendar calendar = createGregorianCalendar();
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

        private static GregorianCalendar createGregorianCalendar() {
            return new GregorianCalendar(false);
        }

        public void localtime(int timeSeconds, ZoneInfo zoneInfo) {
            int transitionIndex;
            byte isDst;
            try {
                int offsetSeconds = zoneInfo.mRawOffset / 1000;
                if (zoneInfo.mTransitions.length != 0 && (transitionIndex = findTransitionIndex(zoneInfo, timeSeconds)) >= 0) {
                    byte transitionType = zoneInfo.mTypes[transitionIndex];
                    offsetSeconds += zoneInfo.mOffsets[transitionType];
                    isDst = zoneInfo.mIsDsts[transitionType];
                } else {
                    isDst = 0;
                }
                int wallTimeSeconds = ZoneInfo.checkedAdd(timeSeconds, offsetSeconds);
                this.calendar.setTimeInMillis(((long) wallTimeSeconds) * 1000);
                copyFieldsFromCalendar();
                this.isDst = isDst;
                this.gmtOffsetSeconds = offsetSeconds;
            } catch (CheckedArithmeticException e) {
            }
        }

        public int mktime(ZoneInfo zoneInfo) {
            int i = 1;
            if (this.isDst > 0) {
                this.isDst = 1;
            } else if (this.isDst < 0) {
                this.isDst = -1;
                i = -1;
            } else {
                i = 0;
            }
            this.isDst = i;
            copyFieldsToCalendar();
            long longWallTimeSeconds = this.calendar.getTimeInMillis() / 1000;
            if (-2147483648L > longWallTimeSeconds || longWallTimeSeconds > 2147483647L) {
                return -1;
            }
            int wallTimeSeconds = (int) longWallTimeSeconds;
            try {
                int rawOffsetSeconds = zoneInfo.mRawOffset / 1000;
                int rawTimeSeconds = ZoneInfo.checkedSubtract(wallTimeSeconds, rawOffsetSeconds);
                if (zoneInfo.mTransitions.length == 0) {
                    if (this.isDst > 0) {
                        return -1;
                    }
                    copyFieldsFromCalendar();
                    this.isDst = 0;
                    this.gmtOffsetSeconds = rawOffsetSeconds;
                    return rawTimeSeconds;
                }
                int initialTransitionIndex = findTransitionIndex(zoneInfo, rawTimeSeconds);
                if (this.isDst < 0) {
                    Integer result = doWallTimeSearch(zoneInfo, initialTransitionIndex, wallTimeSeconds, true);
                    if (result != null) {
                        return result.intValue();
                    }
                    return -1;
                }
                Integer result2 = doWallTimeSearch(zoneInfo, initialTransitionIndex, wallTimeSeconds, true);
                if (result2 == null) {
                    result2 = doWallTimeSearch(zoneInfo, initialTransitionIndex, wallTimeSeconds, false);
                }
                if (result2 == null) {
                    result2 = -1;
                }
                return result2.intValue();
            } catch (CheckedArithmeticException e) {
                return -1;
            }
        }

        private Integer tryOffsetAdjustments(ZoneInfo zoneInfo, int oldWallTimeSeconds, OffsetInterval targetInterval, int transitionIndex, int isDstToFind) throws CheckedArithmeticException {
            int[] offsetsToTry = getOffsetsOfType(zoneInfo, transitionIndex, isDstToFind);
            for (int i : offsetsToTry) {
                int rawOffsetSeconds = zoneInfo.mRawOffset / 1000;
                int jOffsetSeconds = rawOffsetSeconds + i;
                int targetIntervalOffsetSeconds = targetInterval.getTotalOffsetSeconds();
                int adjustmentSeconds = targetIntervalOffsetSeconds - jOffsetSeconds;
                int adjustedWallTimeSeconds = ZoneInfo.checkedAdd(oldWallTimeSeconds, adjustmentSeconds);
                if (targetInterval.containsWallTime(adjustedWallTimeSeconds)) {
                    int returnValue = ZoneInfo.checkedSubtract(adjustedWallTimeSeconds, targetIntervalOffsetSeconds);
                    this.calendar.setTimeInMillis(((long) adjustedWallTimeSeconds) * 1000);
                    copyFieldsFromCalendar();
                    this.isDst = targetInterval.getIsDst();
                    this.gmtOffsetSeconds = targetIntervalOffsetSeconds;
                    return Integer.valueOf(returnValue);
                }
            }
            return null;
        }

        private static int[] getOffsetsOfType(ZoneInfo zoneInfo, int startIndex, int isDst) {
            int[] offsets = new int[zoneInfo.mOffsets.length + 1];
            boolean[] seen = new boolean[zoneInfo.mOffsets.length];
            int numFound = 0;
            int delta = 0;
            boolean clampTop = false;
            boolean clampBottom = false;
            while (true) {
                int numFound2 = numFound;
                delta *= -1;
                if (delta >= 0) {
                    delta++;
                }
                int transitionIndex = startIndex + delta;
                if (delta < 0 && transitionIndex < -1) {
                    clampBottom = true;
                    numFound = numFound2;
                } else if (delta > 0 && transitionIndex >= zoneInfo.mTypes.length) {
                    clampTop = true;
                    numFound = numFound2;
                } else if (transitionIndex != -1) {
                    byte type = zoneInfo.mTypes[transitionIndex];
                    if (!seen[type]) {
                        if (zoneInfo.mIsDsts[type] == isDst) {
                            numFound = numFound2 + 1;
                            offsets[numFound2] = zoneInfo.mOffsets[type];
                        } else {
                            numFound = numFound2;
                        }
                        seen[type] = true;
                    }
                } else if (isDst == 0) {
                    numFound = numFound2 + 1;
                    offsets[numFound2] = 0;
                } else {
                    numFound = numFound2;
                }
                if (clampTop && clampBottom) {
                    int[] toReturn = new int[numFound];
                    System.arraycopy(offsets, 0, toReturn, 0, numFound);
                    return toReturn;
                }
            }
        }

        private Integer doWallTimeSearch(ZoneInfo zoneInfo, int initialTransitionIndex, int wallTimeSeconds, boolean mustMatchDst) throws CheckedArithmeticException {
            boolean clampTop = false;
            boolean clampBottom = false;
            int loop = 0;
            while (true) {
                int transitionIndexDelta = (loop + 1) / 2;
                if (loop % 2 == 1) {
                    transitionIndexDelta *= -1;
                }
                loop++;
                if ((transitionIndexDelta <= 0 || !clampTop) && (transitionIndexDelta >= 0 || !clampBottom)) {
                    int currentTransitionIndex = initialTransitionIndex + transitionIndexDelta;
                    OffsetInterval offsetInterval = OffsetInterval.create(zoneInfo, currentTransitionIndex);
                    if (offsetInterval == null) {
                        clampTop |= transitionIndexDelta > 0;
                        clampBottom |= transitionIndexDelta < 0;
                    } else {
                        if (mustMatchDst) {
                            if (offsetInterval.containsWallTime(wallTimeSeconds) && (this.isDst == -1 || offsetInterval.getIsDst() == this.isDst)) {
                                break;
                            }
                        } else if (this.isDst != offsetInterval.getIsDst()) {
                            int isDstToFind = this.isDst;
                            Integer returnValue = tryOffsetAdjustments(zoneInfo, wallTimeSeconds, offsetInterval, currentTransitionIndex, isDstToFind);
                            if (returnValue != null) {
                                return returnValue;
                            }
                        }
                        if (transitionIndexDelta > 0) {
                            boolean endSearch = offsetInterval.getEndWallTimeSeconds() - ((long) wallTimeSeconds) > 86400;
                            if (endSearch) {
                                clampTop = true;
                            }
                        } else if (transitionIndexDelta < 0) {
                            boolean endSearch2 = ((long) wallTimeSeconds) - offsetInterval.getStartWallTimeSeconds() >= 86400;
                            if (endSearch2) {
                                clampBottom = true;
                            }
                        }
                    }
                }
                if (clampTop && clampBottom) {
                    return null;
                }
            }
        }

        public void setYear(int year) {
            this.year = year;
        }

        public void setMonth(int month) {
            this.month = month;
        }

        public void setMonthDay(int monthDay) {
            this.monthDay = monthDay;
        }

        public void setHour(int hour) {
            this.hour = hour;
        }

        public void setMinute(int minute) {
            this.minute = minute;
        }

        public void setSecond(int second) {
            this.second = second;
        }

        public void setWeekDay(int weekDay) {
            this.weekDay = weekDay;
        }

        public void setYearDay(int yearDay) {
            this.yearDay = yearDay;
        }

        public void setIsDst(int isDst) {
            this.isDst = isDst;
        }

        public void setGmtOffset(int gmtoff) {
            this.gmtOffsetSeconds = gmtoff;
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

        private static int findTransitionIndex(ZoneInfo timeZone, int timeSeconds) {
            int matchingRawTransition = Arrays.binarySearch(timeZone.mTransitions, timeSeconds);
            if (matchingRawTransition < 0) {
                return (matchingRawTransition ^ (-1)) - 1;
            }
            return matchingRawTransition;
        }
    }

    static class OffsetInterval {
        private final int endWallTimeSeconds;
        private final int isDst;
        private final int startWallTimeSeconds;
        private final int totalOffsetSeconds;

        public static OffsetInterval create(ZoneInfo timeZone, int transitionIndex) throws CheckedArithmeticException {
            int endWallTimeSeconds;
            if (transitionIndex >= -1 && transitionIndex < timeZone.mTransitions.length) {
                int rawOffsetSeconds = timeZone.mRawOffset / 1000;
                if (transitionIndex == -1) {
                    int endWallTimeSeconds2 = ZoneInfo.checkedAdd(timeZone.mTransitions[0], rawOffsetSeconds);
                    return new OffsetInterval(Integer.MIN_VALUE, endWallTimeSeconds2, 0, rawOffsetSeconds);
                }
                byte type = timeZone.mTypes[transitionIndex];
                int totalOffsetSeconds = timeZone.mOffsets[type] + rawOffsetSeconds;
                if (transitionIndex != timeZone.mTransitions.length - 1) {
                    endWallTimeSeconds = ZoneInfo.checkedAdd(timeZone.mTransitions[transitionIndex + 1], totalOffsetSeconds);
                } else {
                    endWallTimeSeconds = Integer.MAX_VALUE;
                }
                int isDst = timeZone.mIsDsts[type];
                int startWallTimeSeconds = ZoneInfo.checkedAdd(timeZone.mTransitions[transitionIndex], totalOffsetSeconds);
                return new OffsetInterval(startWallTimeSeconds, endWallTimeSeconds, isDst, totalOffsetSeconds);
            }
            return null;
        }

        private OffsetInterval(int startWallTimeSeconds, int endWallTimeSeconds, int isDst, int totalOffsetSeconds) {
            this.startWallTimeSeconds = startWallTimeSeconds;
            this.endWallTimeSeconds = endWallTimeSeconds;
            this.isDst = isDst;
            this.totalOffsetSeconds = totalOffsetSeconds;
        }

        public boolean containsWallTime(long wallTimeSeconds) {
            return wallTimeSeconds >= ((long) this.startWallTimeSeconds) && wallTimeSeconds < ((long) this.endWallTimeSeconds);
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

    private static int checkedAdd(int a, int b) throws CheckedArithmeticException {
        long result = ((long) a) + ((long) b);
        if (result != ((int) result)) {
            throw new CheckedArithmeticException();
        }
        return (int) result;
    }

    private static int checkedSubtract(int a, int b) throws CheckedArithmeticException {
        long result = ((long) a) - ((long) b);
        if (result != ((int) result)) {
            throw new CheckedArithmeticException();
        }
        return (int) result;
    }
}
