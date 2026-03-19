package android.icu.util;

import android.icu.impl.Grego;
import android.icu.lang.UCharacterEnums;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;

public class SimpleTimeZone extends BasicTimeZone {
    static final boolean $assertionsDisabled = false;
    private static final int DOM_MODE = 1;
    private static final int DOW_GE_DOM_MODE = 3;
    private static final int DOW_IN_MONTH_MODE = 2;
    private static final int DOW_LE_DOM_MODE = 4;
    public static final int STANDARD_TIME = 1;
    public static final int UTC_TIME = 2;
    public static final int WALL_TIME = 0;
    private static final long serialVersionUID = -7034676239311322769L;
    private static final byte[] staticMonthLength = {31, 29, 31, UCharacterEnums.ECharacterCategory.CHAR_CATEGORY_COUNT, 31, UCharacterEnums.ECharacterCategory.CHAR_CATEGORY_COUNT, 31, 31, UCharacterEnums.ECharacterCategory.CHAR_CATEGORY_COUNT, 31, UCharacterEnums.ECharacterCategory.CHAR_CATEGORY_COUNT, 31};
    private int dst;
    private transient AnnualTimeZoneRule dstRule;
    private int endDay;
    private int endDayOfWeek;
    private int endMode;
    private int endMonth;
    private int endTime;
    private int endTimeMode;
    private transient TimeZoneTransition firstTransition;
    private transient InitialTimeZoneRule initialRule;
    private volatile transient boolean isFrozen;
    private int raw;
    private int startDay;
    private int startDayOfWeek;
    private int startMode;
    private int startMonth;
    private int startTime;
    private int startTimeMode;
    private int startYear;
    private transient AnnualTimeZoneRule stdRule;
    private transient boolean transitionRulesInitialized;
    private boolean useDaylight;
    private STZInfo xinfo;

    public SimpleTimeZone(int i, String str) {
        super(str);
        this.dst = 3600000;
        this.xinfo = null;
        this.isFrozen = false;
        construct(i, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 3600000);
    }

    public SimpleTimeZone(int i, String str, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9) {
        super(str);
        this.dst = 3600000;
        this.xinfo = null;
        this.isFrozen = false;
        construct(i, i2, i3, i4, i5, 0, i6, i7, i8, i9, 0, 3600000);
    }

    public SimpleTimeZone(int i, String str, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12) {
        super(str);
        this.dst = 3600000;
        this.xinfo = null;
        this.isFrozen = false;
        construct(i, i2, i3, i4, i5, i6, i7, i8, i9, i10, i11, i12);
    }

    public SimpleTimeZone(int i, String str, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10) {
        super(str);
        this.dst = 3600000;
        this.xinfo = null;
        this.isFrozen = false;
        construct(i, i2, i3, i4, i5, 0, i6, i7, i8, i9, 0, i10);
    }

    @Override
    public void setID(String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        super.setID(str);
        this.transitionRulesInitialized = false;
    }

    @Override
    public void setRawOffset(int i) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        this.raw = i;
        this.transitionRulesInitialized = false;
    }

    @Override
    public int getRawOffset() {
        return this.raw;
    }

    public void setStartYear(int i) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().sy = i;
        this.startYear = i;
        this.transitionRulesInitialized = false;
    }

    public void setStartRule(int i, int i2, int i3, int i4) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setStart(i, i2, i3, i4, -1, false);
        setStartRule(i, i2, i3, i4, 0);
    }

    private void setStartRule(int i, int i2, int i3, int i4, int i5) {
        this.startMonth = i;
        this.startDay = i2;
        this.startDayOfWeek = i3;
        this.startTime = i4;
        this.startTimeMode = i5;
        decodeStartRule();
        this.transitionRulesInitialized = false;
    }

    public void setStartRule(int i, int i2, int i3) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setStart(i, -1, -1, i3, i2, false);
        setStartRule(i, i2, 0, i3, 0);
    }

    public void setStartRule(int i, int i2, int i3, int i4, boolean z) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setStart(i, -1, i3, i4, i2, z);
        if (!z) {
            i2 = -i2;
        }
        setStartRule(i, i2, -i3, i4, 0);
    }

    public void setEndRule(int i, int i2, int i3, int i4) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setEnd(i, i2, i3, i4, -1, false);
        setEndRule(i, i2, i3, i4, 0);
    }

    public void setEndRule(int i, int i2, int i3) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setEnd(i, -1, -1, i3, i2, false);
        setEndRule(i, i2, 0, i3);
    }

    public void setEndRule(int i, int i2, int i3, int i4, boolean z) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        getSTZInfo().setEnd(i, -1, i3, i4, i2, z);
        setEndRule(i, i2, i3, i4, 0, z);
    }

    private void setEndRule(int i, int i2, int i3, int i4, int i5, boolean z) {
        if (!z) {
            i2 = -i2;
        }
        setEndRule(i, i2, -i3, i4, i5);
    }

    private void setEndRule(int i, int i2, int i3, int i4, int i5) {
        this.endMonth = i;
        this.endDay = i2;
        this.endDayOfWeek = i3;
        this.endTime = i4;
        this.endTimeMode = i5;
        decodeEndRule();
        this.transitionRulesInitialized = false;
    }

    public void setDSTSavings(int i) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen SimpleTimeZone instance.");
        }
        if (i <= 0) {
            throw new IllegalArgumentException();
        }
        this.dst = i;
        this.transitionRulesInitialized = false;
    }

    @Override
    public int getDSTSavings() {
        return this.dst;
    }

    private void readObject(ObjectInputStream objectInputStream) throws ClassNotFoundException, IOException {
        objectInputStream.defaultReadObject();
        if (this.xinfo != null) {
            this.xinfo.applyTo(this);
        }
    }

    public String toString() {
        return "SimpleTimeZone: " + getID();
    }

    private STZInfo getSTZInfo() {
        if (this.xinfo == null) {
            this.xinfo = new STZInfo();
        }
        return this.xinfo;
    }

    @Override
    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6) {
        if (i3 < 0 || i3 > 11) {
            throw new IllegalArgumentException();
        }
        return getOffset(i, i2, i3, i4, i5, i6, Grego.monthLength(i2, i3));
    }

    @Deprecated
    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6, int i7) {
        if (i3 < 0 || i3 > 11) {
            throw new IllegalArgumentException();
        }
        return getOffset(i, i2, i3, i4, i5, i6, Grego.monthLength(i2, i3), Grego.previousMonthLength(i2, i3));
    }

    private int getOffset(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        int i9;
        int i10;
        if ((i != 1 && i != 0) || i3 < 0 || i3 > 11 || i4 < 1 || i4 > i7 || i5 < 1 || i5 > 7 || i6 < 0 || i6 >= 86400000 || i7 < 28 || i7 > 31 || i8 < 28 || i8 > 31) {
            throw new IllegalArgumentException();
        }
        int i11 = this.raw;
        if (!this.useDaylight || i2 < this.startYear || i != 1) {
            return i11;
        }
        int iCompareToRule = 0;
        boolean z = this.startMonth > this.endMonth;
        int iCompareToRule2 = compareToRule(i3, i7, i8, i4, i5, i6, this.startTimeMode == 2 ? -this.raw : 0, this.startMode, this.startMonth, this.startDayOfWeek, this.startDay, this.startTime);
        if (z != (iCompareToRule2 >= 0)) {
            if (this.endTimeMode == 0) {
                i10 = this.dst;
            } else if (this.endTimeMode == 2) {
                i10 = -this.raw;
            } else {
                i9 = 0;
                iCompareToRule = compareToRule(i3, i7, i8, i4, i5, i6, i9, this.endMode, this.endMonth, this.endDayOfWeek, this.endDay, this.endTime);
            }
            i9 = i10;
            iCompareToRule = compareToRule(i3, i7, i8, i4, i5, i6, i9, this.endMode, this.endMonth, this.endDayOfWeek, this.endDay, this.endTime);
        }
        if ((!z && iCompareToRule2 >= 0 && iCompareToRule < 0) || (z && (iCompareToRule2 >= 0 || iCompareToRule < 0))) {
            return i11 + this.dst;
        }
        return i11;
    }

    @Override
    @Deprecated
    public void getOffsetFromLocal(long j, int i, int i2, int[] iArr) {
        long dSTSavings;
        long j2;
        boolean z;
        iArr[0] = getRawOffset();
        int[] iArr2 = new int[6];
        Grego.timeToFields(j, iArr2);
        iArr[1] = getOffset(1, iArr2[0], iArr2[1], iArr2[2], iArr2[3], iArr2[5]) - iArr[0];
        if (iArr[1] > 0) {
            int i3 = i & 3;
            if (i3 == 1 || (i3 != 3 && (i & 12) != 12)) {
                dSTSavings = j - ((long) getDSTSavings());
                j2 = dSTSavings;
                z = true;
            }
            j2 = j;
            z = false;
        } else {
            int i4 = i2 & 3;
            if (i4 == 3 || (i4 != 1 && (i2 & 12) == 4)) {
                dSTSavings = j - ((long) getDSTSavings());
                j2 = dSTSavings;
                z = true;
            }
            j2 = j;
            z = false;
        }
        if (z) {
            Grego.timeToFields(j2, iArr2);
            iArr[1] = getOffset(1, iArr2[0], iArr2[1], iArr2[2], iArr2[3], iArr2[5]) - iArr[0];
        }
    }

    private int compareToRule(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12) {
        int i13 = i6 + i7;
        while (i13 >= 86400000) {
            i13 -= Grego.MILLIS_PER_DAY;
            i4++;
            i5 = (i5 % 7) + 1;
            if (i4 > i2) {
                i++;
                i4 = 1;
            }
        }
        while (i13 < 0) {
            i4--;
            i5 = ((i5 + 5) % 7) + 1;
            if (i4 < 1) {
                i--;
                i4 = i3;
            }
            i13 += Grego.MILLIS_PER_DAY;
        }
        if (i < i9) {
            return -1;
        }
        if (i > i9) {
            return 1;
        }
        if (i11 > i2) {
            i11 = i2;
        }
        switch (i8) {
            case 1:
                break;
            case 2:
                i11 = i11 > 0 ? ((i11 - 1) * 7) + 1 + (((i10 + 7) - ((i5 - i4) + 1)) % 7) : (((i11 + 1) * 7) + i2) - (((((i5 + i2) - i4) + 7) - i10) % 7);
                break;
            case 3:
                i11 += ((((49 + i10) - i11) - i5) + i4) % 7;
                break;
            case 4:
                i11 -= ((((49 - i10) + i11) + i5) - i4) % 7;
                break;
            default:
                i11 = 0;
                break;
        }
        if (i4 < i11) {
            return -1;
        }
        if (i4 > i11) {
            return 1;
        }
        if (i13 < i12) {
            return -1;
        }
        return i13 > i12 ? 1 : 0;
    }

    @Override
    public boolean useDaylightTime() {
        return this.useDaylight;
    }

    @Override
    public boolean observesDaylightTime() {
        return this.useDaylight;
    }

    @Override
    public boolean inDaylightTime(Date date) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(this);
        gregorianCalendar.setTime(date);
        return gregorianCalendar.inDaylightTime();
    }

    private void construct(int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11, int i12) {
        this.raw = i;
        this.startMonth = i2;
        this.startDay = i3;
        this.startDayOfWeek = i4;
        this.startTime = i5;
        this.startTimeMode = i6;
        this.endMonth = i7;
        this.endDay = i8;
        this.endDayOfWeek = i9;
        this.endTime = i10;
        this.endTimeMode = i11;
        this.dst = i12;
        this.startYear = 0;
        this.startMode = 1;
        this.endMode = 1;
        decodeRules();
        if (i12 <= 0) {
            throw new IllegalArgumentException();
        }
    }

    private void decodeRules() {
        decodeStartRule();
        decodeEndRule();
    }

    private void decodeStartRule() {
        this.useDaylight = (this.startDay == 0 || this.endDay == 0) ? false : true;
        if (this.useDaylight && this.dst == 0) {
            this.dst = Grego.MILLIS_PER_DAY;
        }
        if (this.startDay != 0) {
            if (this.startMonth < 0 || this.startMonth > 11) {
                throw new IllegalArgumentException();
            }
            if (this.startTime < 0 || this.startTime > 86400000 || this.startTimeMode < 0 || this.startTimeMode > 2) {
                throw new IllegalArgumentException();
            }
            if (this.startDayOfWeek == 0) {
                this.startMode = 1;
            } else {
                if (this.startDayOfWeek > 0) {
                    this.startMode = 2;
                } else {
                    this.startDayOfWeek = -this.startDayOfWeek;
                    if (this.startDay > 0) {
                        this.startMode = 3;
                    } else {
                        this.startDay = -this.startDay;
                        this.startMode = 4;
                    }
                }
                if (this.startDayOfWeek > 7) {
                    throw new IllegalArgumentException();
                }
            }
            if (this.startMode == 2) {
                if (this.startDay < -5 || this.startDay > 5) {
                    throw new IllegalArgumentException();
                }
            } else if (this.startDay < 1 || this.startDay > staticMonthLength[this.startMonth]) {
                throw new IllegalArgumentException();
            }
        }
    }

    private void decodeEndRule() {
        this.useDaylight = (this.startDay == 0 || this.endDay == 0) ? false : true;
        if (this.useDaylight && this.dst == 0) {
            this.dst = Grego.MILLIS_PER_DAY;
        }
        if (this.endDay != 0) {
            if (this.endMonth < 0 || this.endMonth > 11) {
                throw new IllegalArgumentException();
            }
            if (this.endTime < 0 || this.endTime > 86400000 || this.endTimeMode < 0 || this.endTimeMode > 2) {
                throw new IllegalArgumentException();
            }
            if (this.endDayOfWeek == 0) {
                this.endMode = 1;
            } else {
                if (this.endDayOfWeek > 0) {
                    this.endMode = 2;
                } else {
                    this.endDayOfWeek = -this.endDayOfWeek;
                    if (this.endDay > 0) {
                        this.endMode = 3;
                    } else {
                        this.endDay = -this.endDay;
                        this.endMode = 4;
                    }
                }
                if (this.endDayOfWeek > 7) {
                    throw new IllegalArgumentException();
                }
            }
            if (this.endMode == 2) {
                if (this.endDay < -5 || this.endDay > 5) {
                    throw new IllegalArgumentException();
                }
            } else if (this.endDay < 1 || this.endDay > staticMonthLength[this.endMonth]) {
                throw new IllegalArgumentException();
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        SimpleTimeZone simpleTimeZone = (SimpleTimeZone) obj;
        if (this.raw == simpleTimeZone.raw && this.useDaylight == simpleTimeZone.useDaylight && idEquals(getID(), simpleTimeZone.getID())) {
            if (!this.useDaylight) {
                return true;
            }
            if (this.dst == simpleTimeZone.dst && this.startMode == simpleTimeZone.startMode && this.startMonth == simpleTimeZone.startMonth && this.startDay == simpleTimeZone.startDay && this.startDayOfWeek == simpleTimeZone.startDayOfWeek && this.startTime == simpleTimeZone.startTime && this.startTimeMode == simpleTimeZone.startTimeMode && this.endMode == simpleTimeZone.endMode && this.endMonth == simpleTimeZone.endMonth && this.endDay == simpleTimeZone.endDay && this.endDayOfWeek == simpleTimeZone.endDayOfWeek && this.endTime == simpleTimeZone.endTime && this.endTimeMode == simpleTimeZone.endTimeMode && this.startYear == simpleTimeZone.startYear) {
                return true;
            }
        }
        return false;
    }

    private boolean idEquals(String str, String str2) {
        if (str == null && str2 == null) {
            return true;
        }
        if (str != null && str2 != null) {
            return str.equals(str2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int iHashCode = (super.hashCode() + this.raw) ^ ((this.raw >>> 8) + (!this.useDaylight ? 1 : 0));
        if (!this.useDaylight) {
            return iHashCode + ((((((((((((((this.dst ^ ((this.dst >>> 10) + this.startMode)) ^ ((this.startMode >>> 11) + this.startMonth)) ^ ((this.startMonth >>> 12) + this.startDay)) ^ ((this.startDay >>> 13) + this.startDayOfWeek)) ^ ((this.startDayOfWeek >>> 14) + this.startTime)) ^ ((this.startTime >>> 15) + this.startTimeMode)) ^ ((this.startTimeMode >>> 16) + this.endMode)) ^ ((this.endMode >>> 17) + this.endMonth)) ^ ((this.endMonth >>> 18) + this.endDay)) ^ ((this.endDay >>> 19) + this.endDayOfWeek)) ^ ((this.endDayOfWeek >>> 20) + this.endTime)) ^ ((this.endTime >>> 21) + this.endTimeMode)) ^ ((this.endTimeMode >>> 22) + this.startYear)) ^ (this.startYear >>> 23));
        }
        return iHashCode;
    }

    @Override
    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    @Override
    public boolean hasSameRules(TimeZone timeZone) {
        if (this == timeZone) {
            return true;
        }
        if (!(timeZone instanceof SimpleTimeZone)) {
            return false;
        }
        SimpleTimeZone simpleTimeZone = (SimpleTimeZone) timeZone;
        if (simpleTimeZone != null && this.raw == simpleTimeZone.raw && this.useDaylight == simpleTimeZone.useDaylight) {
            if (!this.useDaylight) {
                return true;
            }
            if (this.dst == simpleTimeZone.dst && this.startMode == simpleTimeZone.startMode && this.startMonth == simpleTimeZone.startMonth && this.startDay == simpleTimeZone.startDay && this.startDayOfWeek == simpleTimeZone.startDayOfWeek && this.startTime == simpleTimeZone.startTime && this.startTimeMode == simpleTimeZone.startTimeMode && this.endMode == simpleTimeZone.endMode && this.endMonth == simpleTimeZone.endMonth && this.endDay == simpleTimeZone.endDay && this.endDayOfWeek == simpleTimeZone.endDayOfWeek && this.endTime == simpleTimeZone.endTime && this.endTimeMode == simpleTimeZone.endTimeMode && this.startYear == simpleTimeZone.startYear) {
                return true;
            }
        }
        return false;
    }

    @Override
    public TimeZoneTransition getNextTransition(long j, boolean z) {
        if (!this.useDaylight) {
            return null;
        }
        initTransitionRules();
        long time = this.firstTransition.getTime();
        if (j < time || (z && j == time)) {
            return this.firstTransition;
        }
        Date nextStart = this.stdRule.getNextStart(j, this.dstRule.getRawOffset(), this.dstRule.getDSTSavings(), z);
        Date nextStart2 = this.dstRule.getNextStart(j, this.stdRule.getRawOffset(), this.stdRule.getDSTSavings(), z);
        if (nextStart != null && (nextStart2 == null || nextStart.before(nextStart2))) {
            return new TimeZoneTransition(nextStart.getTime(), this.dstRule, this.stdRule);
        }
        if (nextStart2 == null || !(nextStart == null || nextStart2.before(nextStart))) {
            return null;
        }
        return new TimeZoneTransition(nextStart2.getTime(), this.stdRule, this.dstRule);
    }

    @Override
    public TimeZoneTransition getPreviousTransition(long j, boolean z) {
        if (!this.useDaylight) {
            return null;
        }
        initTransitionRules();
        long time = this.firstTransition.getTime();
        if (j < time || (!z && j == time)) {
            return null;
        }
        Date previousStart = this.stdRule.getPreviousStart(j, this.dstRule.getRawOffset(), this.dstRule.getDSTSavings(), z);
        Date previousStart2 = this.dstRule.getPreviousStart(j, this.stdRule.getRawOffset(), this.stdRule.getDSTSavings(), z);
        if (previousStart != null && (previousStart2 == null || previousStart.after(previousStart2))) {
            return new TimeZoneTransition(previousStart.getTime(), this.dstRule, this.stdRule);
        }
        if (previousStart2 == null || !(previousStart == null || previousStart2.after(previousStart))) {
            return null;
        }
        return new TimeZoneTransition(previousStart2.getTime(), this.stdRule, this.dstRule);
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules() {
        initTransitionRules();
        TimeZoneRule[] timeZoneRuleArr = new TimeZoneRule[this.useDaylight ? 3 : 1];
        timeZoneRuleArr[0] = this.initialRule;
        if (this.useDaylight) {
            timeZoneRuleArr[1] = this.stdRule;
            timeZoneRuleArr[2] = this.dstRule;
        }
        return timeZoneRuleArr;
    }

    private synchronized void initTransitionRules() {
        if (this.transitionRulesInitialized) {
            return;
        }
        if (this.useDaylight) {
            DateTimeRule dateTimeRule = null;
            int i = 2;
            int i2 = this.startTimeMode == 1 ? 1 : this.startTimeMode == 2 ? 2 : 0;
            switch (this.startMode) {
                case 1:
                    dateTimeRule = new DateTimeRule(this.startMonth, this.startDay, this.startTime, i2);
                    break;
                case 2:
                    dateTimeRule = new DateTimeRule(this.startMonth, this.startDay, this.startDayOfWeek, this.startTime, i2);
                    break;
                case 3:
                    dateTimeRule = new DateTimeRule(this.startMonth, this.startDay, this.startDayOfWeek, true, this.startTime, i2);
                    break;
                case 4:
                    dateTimeRule = new DateTimeRule(this.startMonth, this.startDay, this.startDayOfWeek, false, this.startTime, i2);
                    break;
            }
            this.dstRule = new AnnualTimeZoneRule(getID() + "(DST)", getRawOffset(), getDSTSavings(), dateTimeRule, this.startYear, Integer.MAX_VALUE);
            long time = this.dstRule.getFirstStart(getRawOffset(), 0).getTime();
            if (this.endTimeMode != 1) {
                if (this.endTimeMode != 2) {
                    i = 0;
                }
            } else {
                i = 1;
            }
            switch (this.endMode) {
                case 1:
                    dateTimeRule = new DateTimeRule(this.endMonth, this.endDay, this.endTime, i);
                    break;
                case 2:
                    dateTimeRule = new DateTimeRule(this.endMonth, this.endDay, this.endDayOfWeek, this.endTime, i);
                    break;
                case 3:
                    dateTimeRule = new DateTimeRule(this.endMonth, this.endDay, this.endDayOfWeek, true, this.endTime, i);
                    break;
                case 4:
                    dateTimeRule = new DateTimeRule(this.endMonth, this.endDay, this.endDayOfWeek, false, this.endTime, i);
                    break;
            }
            this.stdRule = new AnnualTimeZoneRule(getID() + "(STD)", getRawOffset(), 0, dateTimeRule, this.startYear, Integer.MAX_VALUE);
            long time2 = this.stdRule.getFirstStart(getRawOffset(), this.dstRule.getDSTSavings()).getTime();
            if (time2 < time) {
                this.initialRule = new InitialTimeZoneRule(getID() + "(DST)", getRawOffset(), this.dstRule.getDSTSavings());
                this.firstTransition = new TimeZoneTransition(time2, this.initialRule, this.stdRule);
            } else {
                this.initialRule = new InitialTimeZoneRule(getID() + "(STD)", getRawOffset(), 0);
                this.firstTransition = new TimeZoneTransition(time, this.initialRule, this.dstRule);
            }
        } else {
            this.initialRule = new InitialTimeZoneRule(getID(), getRawOffset(), 0);
        }
        this.transitionRulesInitialized = true;
    }

    @Override
    public boolean isFrozen() {
        return this.isFrozen;
    }

    @Override
    public TimeZone freeze() {
        this.isFrozen = true;
        return this;
    }

    @Override
    public TimeZone cloneAsThawed() {
        SimpleTimeZone simpleTimeZone = (SimpleTimeZone) super.cloneAsThawed();
        simpleTimeZone.isFrozen = false;
        return simpleTimeZone;
    }
}
