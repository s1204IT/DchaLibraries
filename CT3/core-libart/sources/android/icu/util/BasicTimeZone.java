package android.icu.util;

import android.icu.impl.Grego;
import java.util.BitSet;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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

    public boolean hasEquivalentTransitions(TimeZone tz, long start, long end) {
        return hasEquivalentTransitions(tz, start, end, false);
    }

    public boolean hasEquivalentTransitions(TimeZone tz, long start, long end, boolean ignoreDstAmount) {
        if (this == tz) {
            return true;
        }
        if (!(tz instanceof BasicTimeZone)) {
            return false;
        }
        int[] offsets1 = new int[2];
        int[] offsets2 = new int[2];
        getOffset(start, false, offsets1);
        tz.getOffset(start, false, offsets2);
        if (ignoreDstAmount) {
            if (offsets1[0] + offsets1[1] == offsets2[0] + offsets2[1]) {
                if (offsets1[1] == 0 || offsets2[1] != 0) {
                    if (offsets1[1] == 0 && offsets2[1] != 0) {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (offsets1[0] != offsets2[0] || offsets1[1] != offsets2[1]) {
            return false;
        }
        long time = start;
        while (true) {
            TimeZoneTransition tr1 = getNextTransition(time, false);
            TimeZoneTransition tr2 = ((BasicTimeZone) tz).getNextTransition(time, false);
            if (ignoreDstAmount) {
                while (tr1 != null && tr1.getTime() <= end && tr1.getFrom().getRawOffset() + tr1.getFrom().getDSTSavings() == tr1.getTo().getRawOffset() + tr1.getTo().getDSTSavings() && tr1.getFrom().getDSTSavings() != 0 && tr1.getTo().getDSTSavings() != 0) {
                    tr1 = getNextTransition(tr1.getTime(), false);
                }
                while (tr2 != null && tr2.getTime() <= end && tr2.getFrom().getRawOffset() + tr2.getFrom().getDSTSavings() == tr2.getTo().getRawOffset() + tr2.getTo().getDSTSavings() && tr2.getFrom().getDSTSavings() != 0 && tr2.getTo().getDSTSavings() != 0) {
                    tr2 = ((BasicTimeZone) tz).getNextTransition(tr2.getTime(), false);
                }
            }
            boolean inRange1 = false;
            boolean inRange2 = false;
            if (tr1 != null && tr1.getTime() <= end) {
                inRange1 = true;
            }
            if (tr2 != null && tr2.getTime() <= end) {
                inRange2 = true;
            }
            if (inRange1 || inRange2) {
                if (!inRange1 || !inRange2 || tr1.getTime() != tr2.getTime()) {
                    return false;
                }
                if (ignoreDstAmount) {
                    if (tr1.getTo().getRawOffset() + tr1.getTo().getDSTSavings() == tr2.getTo().getRawOffset() + tr2.getTo().getDSTSavings()) {
                        if (tr1.getTo().getDSTSavings() == 0 || tr2.getTo().getDSTSavings() != 0) {
                            if (tr1.getTo().getDSTSavings() == 0 && tr2.getTo().getDSTSavings() != 0) {
                                return false;
                            }
                        } else {
                            return false;
                        }
                    } else {
                        return false;
                    }
                } else if (tr1.getTo().getRawOffset() != tr2.getTo().getRawOffset() || tr1.getTo().getDSTSavings() != tr2.getTo().getDSTSavings()) {
                    return false;
                }
                time = tr1.getTime();
            } else {
                return true;
            }
        }
    }

    public TimeZoneRule[] getTimeZoneRules(long start) {
        TimeZoneTransition tzt;
        TimeZoneRule[] all = getTimeZoneRules();
        TimeZoneTransition tzt2 = getPreviousTransition(start, true);
        if (tzt2 == null) {
            return all;
        }
        BitSet isProcessed = new BitSet(all.length);
        List<TimeZoneRule> filteredRules = new LinkedList<>();
        TimeZoneRule initial = new InitialTimeZoneRule(tzt2.getTo().getName(), tzt2.getTo().getRawOffset(), tzt2.getTo().getDSTSavings());
        filteredRules.add(initial);
        isProcessed.set(0);
        for (int i = 1; i < all.length; i++) {
            Date d = all[i].getNextStart(start, initial.getRawOffset(), initial.getDSTSavings(), false);
            if (d == null) {
                isProcessed.set(i);
            }
        }
        long time = start;
        boolean bFinalStd = false;
        boolean bFinalDst = false;
        while (true) {
            if (bFinalStd && bFinalDst) {
                break;
            }
            TimeZoneTransition tzt3 = getNextTransition(time, false);
            if (tzt3 == null) {
                break;
            }
            time = tzt3.getTime();
            TimeZoneRule toRule = tzt3.getTo();
            int ruleIdx = 1;
            while (ruleIdx < all.length && !all[ruleIdx].equals(toRule)) {
                ruleIdx++;
            }
            if (ruleIdx >= all.length) {
                throw new IllegalStateException("The rule was not found");
            }
            if (!isProcessed.get(ruleIdx)) {
                if (toRule instanceof TimeArrayTimeZoneRule) {
                    TimeArrayTimeZoneRule tar = (TimeArrayTimeZoneRule) toRule;
                    long t = start;
                    while (true) {
                        tzt = getNextTransition(t, false);
                        if (tzt == null || tzt.getTo().equals(tar)) {
                            break;
                        }
                        t = tzt.getTime();
                    }
                    if (tzt != null) {
                        Date firstStart = tar.getFirstStart(tzt.getFrom().getRawOffset(), tzt.getFrom().getDSTSavings());
                        if (firstStart.getTime() > start) {
                            filteredRules.add(tar);
                        } else {
                            long[] times = tar.getStartTimes();
                            int timeType = tar.getTimeType();
                            int idx = 0;
                            while (idx < times.length) {
                                long t2 = times[idx];
                                if (timeType == 1) {
                                    t2 -= (long) tzt.getFrom().getRawOffset();
                                }
                                if (timeType == 0) {
                                    t2 -= (long) tzt.getFrom().getDSTSavings();
                                }
                                if (t2 > start) {
                                    break;
                                }
                                idx++;
                            }
                            int asize = times.length - idx;
                            if (asize > 0) {
                                long[] newtimes = new long[asize];
                                System.arraycopy(times, idx, newtimes, 0, asize);
                                TimeArrayTimeZoneRule newtar = new TimeArrayTimeZoneRule(tar.getName(), tar.getRawOffset(), tar.getDSTSavings(), newtimes, tar.getTimeType());
                                filteredRules.add(newtar);
                            }
                        }
                    }
                } else if (toRule instanceof AnnualTimeZoneRule) {
                    AnnualTimeZoneRule ar = (AnnualTimeZoneRule) toRule;
                    Date firstStart2 = ar.getFirstStart(tzt3.getFrom().getRawOffset(), tzt3.getFrom().getDSTSavings());
                    if (firstStart2.getTime() == tzt3.getTime()) {
                        filteredRules.add(ar);
                    } else {
                        int[] dfields = new int[6];
                        Grego.timeToFields(tzt3.getTime(), dfields);
                        AnnualTimeZoneRule newar = new AnnualTimeZoneRule(ar.getName(), ar.getRawOffset(), ar.getDSTSavings(), ar.getRule(), dfields[0], ar.getEndYear());
                        filteredRules.add(newar);
                    }
                    if (ar.getEndYear() == Integer.MAX_VALUE) {
                        if (ar.getDSTSavings() == 0) {
                            bFinalStd = true;
                        } else {
                            bFinalDst = true;
                        }
                    }
                }
                isProcessed.set(ruleIdx);
            }
        }
    }

    public TimeZoneRule[] getSimpleTimeZoneRulesNear(long date) {
        TimeZoneRule initialRule;
        TimeZoneTransition tr;
        TimeZoneTransition tr2;
        AnnualTimeZoneRule[] annualRules = null;
        TimeZoneTransition tr3 = getNextTransition(date, false);
        if (tr3 != null) {
            String initialName = tr3.getFrom().getName();
            int initialRaw = tr3.getFrom().getRawOffset();
            int initialDst = tr3.getFrom().getDSTSavings();
            long nextTransitionTime = tr3.getTime();
            if (((tr3.getFrom().getDSTSavings() == 0 && tr3.getTo().getDSTSavings() != 0) || (tr3.getFrom().getDSTSavings() != 0 && tr3.getTo().getDSTSavings() == 0)) && MILLIS_PER_YEAR + date > nextTransitionTime) {
                annualRules = new AnnualTimeZoneRule[2];
                int[] dtfields = Grego.timeToFields(((long) tr3.getFrom().getRawOffset()) + nextTransitionTime + ((long) tr3.getFrom().getDSTSavings()), null);
                int weekInMonth = Grego.getDayOfWeekInMonth(dtfields[0], dtfields[1], dtfields[2]);
                DateTimeRule dtr = new DateTimeRule(dtfields[1], weekInMonth, dtfields[3], dtfields[5], 0);
                annualRules[0] = new AnnualTimeZoneRule(tr3.getTo().getName(), initialRaw, tr3.getTo().getDSTSavings(), dtr, dtfields[0], Integer.MAX_VALUE);
                if (tr3.getTo().getRawOffset() == initialRaw && (tr2 = getNextTransition(nextTransitionTime, false)) != null) {
                    if (((tr2.getFrom().getDSTSavings() == 0 && tr2.getTo().getDSTSavings() != 0) || (tr2.getFrom().getDSTSavings() != 0 && tr2.getTo().getDSTSavings() == 0)) && MILLIS_PER_YEAR + nextTransitionTime > tr2.getTime()) {
                        dtfields = Grego.timeToFields(tr2.getTime() + ((long) tr2.getFrom().getRawOffset()) + ((long) tr2.getFrom().getDSTSavings()), dtfields);
                        int weekInMonth2 = Grego.getDayOfWeekInMonth(dtfields[0], dtfields[1], dtfields[2]);
                        DateTimeRule dtr2 = new DateTimeRule(dtfields[1], weekInMonth2, dtfields[3], dtfields[5], 0);
                        AnnualTimeZoneRule secondRule = new AnnualTimeZoneRule(tr2.getTo().getName(), tr2.getTo().getRawOffset(), tr2.getTo().getDSTSavings(), dtr2, dtfields[0] - 1, Integer.MAX_VALUE);
                        Date d = secondRule.getPreviousStart(date, tr2.getFrom().getRawOffset(), tr2.getFrom().getDSTSavings(), true);
                        if (d != null && d.getTime() <= date && initialRaw == tr2.getTo().getRawOffset() && initialDst == tr2.getTo().getDSTSavings()) {
                            annualRules[1] = secondRule;
                        }
                    }
                }
                if (annualRules[1] == null && (tr = getPreviousTransition(date, true)) != null && ((tr.getFrom().getDSTSavings() == 0 && tr.getTo().getDSTSavings() != 0) || (tr.getFrom().getDSTSavings() != 0 && tr.getTo().getDSTSavings() == 0))) {
                    int[] dtfields2 = Grego.timeToFields(tr.getTime() + ((long) tr.getFrom().getRawOffset()) + ((long) tr.getFrom().getDSTSavings()), dtfields);
                    int weekInMonth3 = Grego.getDayOfWeekInMonth(dtfields2[0], dtfields2[1], dtfields2[2]);
                    DateTimeRule dtr3 = new DateTimeRule(dtfields2[1], weekInMonth3, dtfields2[3], dtfields2[5], 0);
                    AnnualTimeZoneRule secondRule2 = new AnnualTimeZoneRule(tr.getTo().getName(), initialRaw, initialDst, dtr3, annualRules[0].getStartYear() - 1, Integer.MAX_VALUE);
                    if (secondRule2.getNextStart(date, tr.getFrom().getRawOffset(), tr.getFrom().getDSTSavings(), false).getTime() > nextTransitionTime) {
                        annualRules[1] = secondRule2;
                    }
                }
                if (annualRules[1] == null) {
                    annualRules = null;
                } else {
                    initialName = annualRules[0].getName();
                    initialRaw = annualRules[0].getRawOffset();
                    initialDst = annualRules[0].getDSTSavings();
                }
            }
            initialRule = new InitialTimeZoneRule(initialName, initialRaw, initialDst);
        } else {
            TimeZoneTransition tr4 = getPreviousTransition(date, true);
            if (tr4 != null) {
                initialRule = new InitialTimeZoneRule(tr4.getTo().getName(), tr4.getTo().getRawOffset(), tr4.getTo().getDSTSavings());
            } else {
                int[] offsets = new int[2];
                getOffset(date, false, offsets);
                initialRule = new InitialTimeZoneRule(getID(), offsets[0], offsets[1]);
            }
        }
        if (annualRules == null) {
            TimeZoneRule[] result = {initialRule};
            return result;
        }
        TimeZoneRule[] result2 = {initialRule, annualRules[0], annualRules[1]};
        return result2;
    }

    @Deprecated
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        throw new IllegalStateException("Not implemented");
    }

    protected BasicTimeZone() {
    }

    @Deprecated
    protected BasicTimeZone(String ID) {
        super(ID);
    }
}
