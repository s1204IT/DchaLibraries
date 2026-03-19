package android.icu.util;

import android.icu.impl.Grego;
import java.util.BitSet;
import java.util.Date;
import java.util.LinkedList;

public abstract class BasicTimeZone extends TimeZone {

    @Deprecated
    protected static final int FORMER_LATTER_MASK = 12;

    @Deprecated
    public static final int LOCAL_DST = 3;

    @Deprecated
    public static final int LOCAL_FORMER = 4;

    @Deprecated
    public static final int LOCAL_LATTER = 12;

    @Deprecated
    public static final int LOCAL_STD = 1;
    private static final long MILLIS_PER_YEAR = 31536000000L;

    @Deprecated
    protected static final int STD_DST_MASK = 3;
    private static final long serialVersionUID = -3204278532246180932L;

    public abstract TimeZoneTransition getNextTransition(long j, boolean z);

    public abstract TimeZoneTransition getPreviousTransition(long j, boolean z);

    public abstract TimeZoneRule[] getTimeZoneRules();

    public boolean hasEquivalentTransitions(TimeZone timeZone, long j, long j2) {
        return hasEquivalentTransitions(timeZone, j, j2, false);
    }

    public boolean hasEquivalentTransitions(TimeZone timeZone, long j, long j2, boolean z) {
        if (this == timeZone) {
            return true;
        }
        if (!(timeZone instanceof BasicTimeZone)) {
            return false;
        }
        int[] iArr = new int[2];
        int[] iArr2 = new int[2];
        getOffset(j, false, iArr);
        timeZone.getOffset(j, false, iArr2);
        if (z) {
            if (iArr[0] + iArr[1] != iArr2[0] + iArr2[1] || ((iArr[1] != 0 && iArr2[1] == 0) || (iArr[1] == 0 && iArr2[1] != 0))) {
                return false;
            }
        } else if (iArr[0] != iArr2[0] || iArr[1] != iArr2[1]) {
            return false;
        }
        while (true) {
            TimeZoneTransition nextTransition = getNextTransition(j, false);
            BasicTimeZone basicTimeZone = (BasicTimeZone) timeZone;
            TimeZoneTransition nextTransition2 = basicTimeZone.getNextTransition(j, false);
            if (z) {
                while (nextTransition != null && nextTransition.getTime() <= j2 && nextTransition.getFrom().getRawOffset() + nextTransition.getFrom().getDSTSavings() == nextTransition.getTo().getRawOffset() + nextTransition.getTo().getDSTSavings() && nextTransition.getFrom().getDSTSavings() != 0 && nextTransition.getTo().getDSTSavings() != 0) {
                    nextTransition = getNextTransition(nextTransition.getTime(), false);
                }
                while (nextTransition2 != null && nextTransition2.getTime() <= j2 && nextTransition2.getFrom().getRawOffset() + nextTransition2.getFrom().getDSTSavings() == nextTransition2.getTo().getRawOffset() + nextTransition2.getTo().getDSTSavings() && nextTransition2.getFrom().getDSTSavings() != 0 && nextTransition2.getTo().getDSTSavings() != 0) {
                    nextTransition2 = basicTimeZone.getNextTransition(nextTransition2.getTime(), false);
                }
            }
            boolean z2 = nextTransition != null && nextTransition.getTime() <= j2;
            boolean z3 = nextTransition2 != null && nextTransition2.getTime() <= j2;
            if (!z2 && !z3) {
                return true;
            }
            if (!z2 || !z3 || nextTransition.getTime() != nextTransition2.getTime()) {
                return false;
            }
            if (z) {
                if (nextTransition.getTo().getRawOffset() + nextTransition.getTo().getDSTSavings() != nextTransition2.getTo().getRawOffset() + nextTransition2.getTo().getDSTSavings() || ((nextTransition.getTo().getDSTSavings() != 0 && nextTransition2.getTo().getDSTSavings() == 0) || (nextTransition.getTo().getDSTSavings() == 0 && nextTransition2.getTo().getDSTSavings() != 0))) {
                    break;
                }
            } else if (nextTransition.getTo().getRawOffset() != nextTransition2.getTo().getRawOffset() || nextTransition.getTo().getDSTSavings() != nextTransition2.getTo().getDSTSavings()) {
                break;
            }
            j = nextTransition.getTime();
        }
        return false;
    }

    public TimeZoneRule[] getTimeZoneRules(long j) {
        boolean z;
        BitSet bitSet;
        boolean z2;
        TimeZoneTransition nextTransition;
        BasicTimeZone basicTimeZone = this;
        TimeZoneRule[] timeZoneRules = getTimeZoneRules();
        int i = 1;
        TimeZoneTransition previousTransition = basicTimeZone.getPreviousTransition(j, true);
        if (previousTransition == null) {
            return timeZoneRules;
        }
        BitSet bitSet2 = new BitSet(timeZoneRules.length);
        LinkedList linkedList = new LinkedList();
        InitialTimeZoneRule initialTimeZoneRule = new InitialTimeZoneRule(previousTransition.getTo().getName(), previousTransition.getTo().getRawOffset(), previousTransition.getTo().getDSTSavings());
        linkedList.add(initialTimeZoneRule);
        boolean z3 = false;
        bitSet2.set(0);
        for (int i2 = 1; i2 < timeZoneRules.length; i2++) {
            if (timeZoneRules[i2].getNextStart(j, initialTimeZoneRule.getRawOffset(), initialTimeZoneRule.getDSTSavings(), false) == null) {
                bitSet2.set(i2);
            }
        }
        long j2 = j;
        boolean z4 = false;
        boolean z5 = false;
        while (true) {
            if (z4 && z5) {
                break;
            }
            TimeZoneTransition nextTransition2 = basicTimeZone.getNextTransition(j2, z3);
            if (nextTransition2 == null) {
                break;
            }
            long time = nextTransition2.getTime();
            TimeZoneRule to = nextTransition2.getTo();
            int i3 = i;
            while (i3 < timeZoneRules.length && !timeZoneRules[i3].equals(to)) {
                i3++;
            }
            if (i3 >= timeZoneRules.length) {
                throw new IllegalStateException("The rule was not found");
            }
            if (bitSet2.get(i3)) {
                j2 = time;
            } else {
                if (to instanceof TimeArrayTimeZoneRule) {
                    TimeArrayTimeZoneRule timeArrayTimeZoneRule = (TimeArrayTimeZoneRule) to;
                    bitSet = bitSet2;
                    long time2 = j;
                    while (true) {
                        nextTransition = basicTimeZone.getNextTransition(time2, z3);
                        if (nextTransition == null || nextTransition.getTo().equals(timeArrayTimeZoneRule)) {
                            break;
                        }
                        time2 = nextTransition.getTime();
                        basicTimeZone = this;
                        z3 = false;
                    }
                    if (nextTransition == null) {
                        z = z4;
                    } else if (timeArrayTimeZoneRule.getFirstStart(nextTransition.getFrom().getRawOffset(), nextTransition.getFrom().getDSTSavings()).getTime() > j) {
                        linkedList.add(timeArrayTimeZoneRule);
                        z = z4;
                    } else {
                        long[] startTimes = timeArrayTimeZoneRule.getStartTimes();
                        int timeType = timeArrayTimeZoneRule.getTimeType();
                        ?? r11 = z3;
                        while (true) {
                            if (r11 < startTimes.length) {
                                long dSTSavings = startTimes[r11];
                                if (timeType == 1) {
                                    z = z4;
                                    dSTSavings -= (long) nextTransition.getFrom().getRawOffset();
                                } else {
                                    z = z4;
                                }
                                if (timeType == 0) {
                                    dSTSavings -= (long) nextTransition.getFrom().getDSTSavings();
                                }
                                if (dSTSavings > j) {
                                    break;
                                }
                                z4 = z;
                                r11++;
                            } else {
                                z = z4;
                                break;
                            }
                        }
                        int length = startTimes.length - r11;
                        if (length > 0) {
                            long[] jArr = new long[length];
                            System.arraycopy(startTimes, r11, jArr, 0, length);
                            linkedList.add(new TimeArrayTimeZoneRule(timeArrayTimeZoneRule.getName(), timeArrayTimeZoneRule.getRawOffset(), timeArrayTimeZoneRule.getDSTSavings(), jArr, timeArrayTimeZoneRule.getTimeType()));
                        }
                    }
                } else {
                    z = z4;
                    bitSet = bitSet2;
                    if (to instanceof AnnualTimeZoneRule) {
                        AnnualTimeZoneRule annualTimeZoneRule = (AnnualTimeZoneRule) to;
                        if (annualTimeZoneRule.getFirstStart(nextTransition2.getFrom().getRawOffset(), nextTransition2.getFrom().getDSTSavings()).getTime() == nextTransition2.getTime()) {
                            linkedList.add(annualTimeZoneRule);
                            z2 = false;
                        } else {
                            int[] iArr = new int[6];
                            Grego.timeToFields(nextTransition2.getTime(), iArr);
                            z2 = false;
                            linkedList.add(new AnnualTimeZoneRule(annualTimeZoneRule.getName(), annualTimeZoneRule.getRawOffset(), annualTimeZoneRule.getDSTSavings(), annualTimeZoneRule.getRule(), iArr[0], annualTimeZoneRule.getEndYear()));
                        }
                        if (annualTimeZoneRule.getEndYear() == Integer.MAX_VALUE) {
                            if (annualTimeZoneRule.getDSTSavings() == 0) {
                                z4 = true;
                            } else {
                                z4 = z;
                                z5 = true;
                            }
                        }
                        BitSet bitSet3 = bitSet;
                        bitSet3.set(i3);
                        bitSet2 = bitSet3;
                        z3 = z2;
                        j2 = time;
                        basicTimeZone = this;
                        i = 1;
                    }
                    z4 = z;
                    BitSet bitSet32 = bitSet;
                    bitSet32.set(i3);
                    bitSet2 = bitSet32;
                    z3 = z2;
                    j2 = time;
                    basicTimeZone = this;
                    i = 1;
                }
                z2 = false;
                z4 = z;
                BitSet bitSet322 = bitSet;
                bitSet322.set(i3);
                bitSet2 = bitSet322;
                z3 = z2;
                j2 = time;
                basicTimeZone = this;
                i = 1;
            }
        }
    }

    public TimeZoneRule[] getSimpleTimeZoneRulesNear(long j) {
        int i;
        InitialTimeZoneRule initialTimeZoneRule;
        AnnualTimeZoneRule[] annualTimeZoneRuleArr;
        int i2;
        String str;
        int i3;
        int dSTSavings;
        String name;
        long j2;
        AnnualTimeZoneRule[] annualTimeZoneRuleArr2;
        int i4;
        char c;
        TimeZoneTransition nextTransition;
        TimeZoneTransition nextTransition2 = getNextTransition(j, false);
        if (nextTransition2 == null) {
            TimeZoneTransition previousTransition = getPreviousTransition(j, true);
            if (previousTransition != null) {
                initialTimeZoneRule = new InitialTimeZoneRule(previousTransition.getTo().getName(), previousTransition.getTo().getRawOffset(), previousTransition.getTo().getDSTSavings());
                i = 1;
            } else {
                int[] iArr = new int[2];
                getOffset(j, false, iArr);
                i = 1;
                initialTimeZoneRule = new InitialTimeZoneRule(getID(), iArr[0], iArr[1]);
            }
            annualTimeZoneRuleArr = null;
        } else {
            String name2 = nextTransition2.getFrom().getName();
            int rawOffset = nextTransition2.getFrom().getRawOffset();
            int dSTSavings2 = nextTransition2.getFrom().getDSTSavings();
            long time = nextTransition2.getTime();
            if (((nextTransition2.getFrom().getDSTSavings() != 0 || nextTransition2.getTo().getDSTSavings() == 0) && (nextTransition2.getFrom().getDSTSavings() == 0 || nextTransition2.getTo().getDSTSavings() != 0)) || j + MILLIS_PER_YEAR <= time) {
                i2 = rawOffset;
                str = name2;
                i3 = dSTSavings2;
            } else {
                AnnualTimeZoneRule[] annualTimeZoneRuleArr3 = new AnnualTimeZoneRule[2];
                str = name2;
                int[] iArrTimeToFields = Grego.timeToFields(((long) nextTransition2.getFrom().getRawOffset()) + time + ((long) nextTransition2.getFrom().getDSTSavings()), null);
                annualTimeZoneRuleArr3[0] = new AnnualTimeZoneRule(nextTransition2.getTo().getName(), rawOffset, nextTransition2.getTo().getDSTSavings(), new DateTimeRule(iArrTimeToFields[1], Grego.getDayOfWeekInMonth(iArrTimeToFields[0], iArrTimeToFields[1], iArrTimeToFields[2]), iArrTimeToFields[3], iArrTimeToFields[5], 0), iArrTimeToFields[0], Integer.MAX_VALUE);
                if (nextTransition2.getTo().getRawOffset() == rawOffset && (nextTransition = getNextTransition(time, false)) != null && (((nextTransition.getFrom().getDSTSavings() == 0 && nextTransition.getTo().getDSTSavings() != 0) || (nextTransition.getFrom().getDSTSavings() != 0 && nextTransition.getTo().getDSTSavings() == 0)) && time + MILLIS_PER_YEAR > nextTransition.getTime())) {
                    iArrTimeToFields = Grego.timeToFields(nextTransition.getTime() + ((long) nextTransition.getFrom().getRawOffset()) + ((long) nextTransition.getFrom().getDSTSavings()), iArrTimeToFields);
                    AnnualTimeZoneRule annualTimeZoneRule = new AnnualTimeZoneRule(nextTransition.getTo().getName(), nextTransition.getTo().getRawOffset(), nextTransition.getTo().getDSTSavings(), new DateTimeRule(iArrTimeToFields[1], Grego.getDayOfWeekInMonth(iArrTimeToFields[0], iArrTimeToFields[1], iArrTimeToFields[2]), iArrTimeToFields[3], iArrTimeToFields[5], 0), iArrTimeToFields[0] - 1, Integer.MAX_VALUE);
                    j2 = time;
                    annualTimeZoneRuleArr2 = annualTimeZoneRuleArr3;
                    i3 = dSTSavings2;
                    i4 = rawOffset;
                    Date previousStart = annualTimeZoneRule.getPreviousStart(j, nextTransition.getFrom().getRawOffset(), nextTransition.getFrom().getDSTSavings(), true);
                    if (previousStart != null && previousStart.getTime() <= j && i4 == nextTransition.getTo().getRawOffset() && i3 == nextTransition.getTo().getDSTSavings()) {
                        annualTimeZoneRuleArr2[1] = annualTimeZoneRule;
                    }
                } else {
                    j2 = time;
                    annualTimeZoneRuleArr2 = annualTimeZoneRuleArr3;
                    i3 = dSTSavings2;
                    i4 = rawOffset;
                }
                if (annualTimeZoneRuleArr2[1] == null) {
                    TimeZoneTransition previousTransition2 = getPreviousTransition(j, true);
                    if (previousTransition2 != null && ((previousTransition2.getFrom().getDSTSavings() == 0 && previousTransition2.getTo().getDSTSavings() != 0) || (previousTransition2.getFrom().getDSTSavings() != 0 && previousTransition2.getTo().getDSTSavings() == 0))) {
                        int[] iArrTimeToFields2 = Grego.timeToFields(previousTransition2.getTime() + ((long) previousTransition2.getFrom().getRawOffset()) + ((long) previousTransition2.getFrom().getDSTSavings()), iArrTimeToFields);
                        i2 = i4;
                        AnnualTimeZoneRule annualTimeZoneRule2 = new AnnualTimeZoneRule(previousTransition2.getTo().getName(), i2, i3, new DateTimeRule(iArrTimeToFields2[1], Grego.getDayOfWeekInMonth(iArrTimeToFields2[0], iArrTimeToFields2[1], iArrTimeToFields2[2]), iArrTimeToFields2[3], iArrTimeToFields2[5], 0), annualTimeZoneRuleArr2[0].getStartYear() - 1, Integer.MAX_VALUE);
                        if (annualTimeZoneRule2.getNextStart(j, previousTransition2.getFrom().getRawOffset(), previousTransition2.getFrom().getDSTSavings(), false).getTime() > j2) {
                            c = 1;
                            annualTimeZoneRuleArr2[1] = annualTimeZoneRule2;
                        }
                    } else {
                        i2 = i4;
                    }
                    c = 1;
                } else {
                    c = 1;
                    i2 = i4;
                }
                if (annualTimeZoneRuleArr2[c] != null) {
                    name = annualTimeZoneRuleArr2[0].getName();
                    int rawOffset2 = annualTimeZoneRuleArr2[0].getRawOffset();
                    dSTSavings = annualTimeZoneRuleArr2[0].getDSTSavings();
                    i2 = rawOffset2;
                    annualTimeZoneRuleArr = annualTimeZoneRuleArr2;
                }
                initialTimeZoneRule = new InitialTimeZoneRule(name, i2, dSTSavings);
                i = 1;
            }
            dSTSavings = i3;
            name = str;
            annualTimeZoneRuleArr = null;
            initialTimeZoneRule = new InitialTimeZoneRule(name, i2, dSTSavings);
            i = 1;
        }
        if (annualTimeZoneRuleArr == null) {
            TimeZoneRule[] timeZoneRuleArr = new TimeZoneRule[i];
            timeZoneRuleArr[0] = initialTimeZoneRule;
            return timeZoneRuleArr;
        }
        TimeZoneRule[] timeZoneRuleArr2 = new TimeZoneRule[3];
        timeZoneRuleArr2[0] = initialTimeZoneRule;
        timeZoneRuleArr2[i] = annualTimeZoneRuleArr[0];
        timeZoneRuleArr2[2] = annualTimeZoneRuleArr[i];
        return timeZoneRuleArr2;
    }

    @Deprecated
    public void getOffsetFromLocal(long j, int i, int i2, int[] iArr) {
        throw new IllegalStateException("Not implemented");
    }

    protected BasicTimeZone() {
    }

    @Deprecated
    protected BasicTimeZone(String str) {
        super(str);
    }
}
