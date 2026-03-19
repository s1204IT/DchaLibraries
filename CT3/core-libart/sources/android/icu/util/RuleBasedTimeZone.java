package android.icu.util;

import android.icu.impl.Grego;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class RuleBasedTimeZone extends BasicTimeZone {
    private static final long serialVersionUID = 7580833058949327935L;
    private AnnualTimeZoneRule[] finalRules;
    private List<TimeZoneRule> historicRules;
    private transient List<TimeZoneTransition> historicTransitions;
    private final InitialTimeZoneRule initialRule;
    private volatile transient boolean isFrozen;
    private transient boolean upToDate;

    public RuleBasedTimeZone(String id, InitialTimeZoneRule initialRule) {
        super(id);
        this.isFrozen = false;
        this.initialRule = initialRule;
    }

    public void addTransitionRule(TimeZoneRule timeZoneRule) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen RuleBasedTimeZone instance.");
        }
        if (!timeZoneRule.isTransitionRule()) {
            throw new IllegalArgumentException("Rule must be a transition rule");
        }
        if ((timeZoneRule instanceof AnnualTimeZoneRule) && timeZoneRule.getEndYear() == Integer.MAX_VALUE) {
            if (this.finalRules == null) {
                this.finalRules = new AnnualTimeZoneRule[2];
                this.finalRules[0] = timeZoneRule;
            } else if (this.finalRules[1] == null) {
                this.finalRules[1] = timeZoneRule;
            } else {
                throw new IllegalStateException("Too many final rules");
            }
        } else {
            if (this.historicRules == null) {
                this.historicRules = new ArrayList();
            }
            this.historicRules.add(timeZoneRule);
        }
        this.upToDate = false;
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        if (era == 0) {
            year = 1 - year;
        }
        long time = (Grego.fieldsToDay(year, month, day) * 86400000) + ((long) milliseconds);
        int[] offsets = new int[2];
        getOffset(time, true, 3, 1, offsets);
        return offsets[0] + offsets[1];
    }

    @Override
    public void getOffset(long time, boolean local, int[] offsets) {
        getOffset(time, local, 4, 12, offsets);
    }

    @Override
    @Deprecated
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        getOffset(date, true, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
    }

    @Override
    public int getRawOffset() {
        long now = System.currentTimeMillis();
        int[] offsets = new int[2];
        getOffset(now, false, offsets);
        return offsets[0];
    }

    @Override
    public boolean inDaylightTime(Date date) {
        int[] offsets = new int[2];
        getOffset(date.getTime(), false, offsets);
        return offsets[1] != 0;
    }

    @Override
    public void setRawOffset(int offsetMillis) {
        throw new UnsupportedOperationException("setRawOffset in RuleBasedTimeZone is not supported.");
    }

    @Override
    public boolean useDaylightTime() {
        long now = System.currentTimeMillis();
        int[] offsets = new int[2];
        getOffset(now, false, offsets);
        if (offsets[1] != 0) {
            return true;
        }
        TimeZoneTransition tt = getNextTransition(now, false);
        return (tt == null || tt.getTo().getDSTSavings() == 0) ? false : true;
    }

    @Override
    public boolean observesDaylightTime() {
        long time = System.currentTimeMillis();
        int[] offsets = new int[2];
        getOffset(time, false, offsets);
        if (offsets[1] != 0) {
            return true;
        }
        BitSet bitSet = this.finalRules == null ? null : new BitSet(this.finalRules.length);
        while (true) {
            TimeZoneTransition tt = getNextTransition(time, false);
            if (tt == null) {
                break;
            }
            TimeZoneRule toRule = tt.getTo();
            if (toRule.getDSTSavings() != 0) {
                return true;
            }
            if (bitSet != null) {
                for (int i = 0; i < this.finalRules.length; i++) {
                    if (this.finalRules[i].equals(toRule)) {
                        bitSet.set(i);
                    }
                }
                if (bitSet.cardinality() == this.finalRules.length) {
                    break;
                }
            }
            time = tt.getTime();
        }
        return false;
    }

    @Override
    public boolean hasSameRules(TimeZone timeZone) {
        if (this == timeZone) {
            return true;
        }
        if (!(timeZone instanceof RuleBasedTimeZone) || !this.initialRule.isEquivalentTo(timeZone.initialRule)) {
            return false;
        }
        if (this.finalRules != null && timeZone.finalRules != null) {
            for (int i = 0; i < this.finalRules.length; i++) {
                if (!(this.finalRules[i] == null && timeZone.finalRules[i] == null) && (this.finalRules[i] == null || timeZone.finalRules[i] == null || !this.finalRules[i].isEquivalentTo(timeZone.finalRules[i]))) {
                    return false;
                }
            }
        } else if (this.finalRules != null || timeZone.finalRules != null) {
            return false;
        }
        if (this.historicRules != null && timeZone.historicRules != null) {
            if (this.historicRules.size() != timeZone.historicRules.size()) {
                return false;
            }
            for (TimeZoneRule rule : this.historicRules) {
                boolean foundSameRule = false;
                Iterator orule$iterator = timeZone.historicRules.iterator();
                while (true) {
                    if (!orule$iterator.hasNext()) {
                        break;
                    }
                    TimeZoneRule orule = (TimeZoneRule) orule$iterator.next();
                    if (rule.isEquivalentTo(orule)) {
                        foundSameRule = true;
                        break;
                    }
                }
                if (!foundSameRule) {
                    return false;
                }
            }
        } else if (this.historicRules != null || timeZone.historicRules != null) {
            return false;
        }
        return true;
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules() {
        int size = 1;
        if (this.historicRules != null) {
            size = this.historicRules.size() + 1;
        }
        if (this.finalRules != null) {
            if (this.finalRules[1] != null) {
                size += 2;
            } else {
                size++;
            }
        }
        TimeZoneRule[] rules = new TimeZoneRule[size];
        rules[0] = this.initialRule;
        int idx = 1;
        if (this.historicRules != null) {
            while (idx < this.historicRules.size() + 1) {
                rules[idx] = this.historicRules.get(idx - 1);
                idx++;
            }
        }
        if (this.finalRules != null) {
            int idx2 = idx + 1;
            rules[idx] = this.finalRules[0];
            if (this.finalRules[1] != null) {
                rules[idx2] = this.finalRules[1];
            }
        }
        return rules;
    }

    @Override
    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        TimeZoneTransition result;
        TimeZoneTransition tzt;
        complete();
        if (this.historicTransitions == null) {
            return null;
        }
        boolean isFinal = false;
        TimeZoneTransition tzt2 = this.historicTransitions.get(0);
        long tt = tzt2.getTime();
        if (tt > base || (inclusive && tt == base)) {
            result = tzt2;
        } else {
            int idx = this.historicTransitions.size() - 1;
            TimeZoneTransition tzt3 = this.historicTransitions.get(idx);
            long tt2 = tzt3.getTime();
            if (inclusive && tt2 == base) {
                result = tzt3;
            } else if (tt2 <= base) {
                if (this.finalRules != null) {
                    Date start0 = this.finalRules[0].getNextStart(base, this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), inclusive);
                    Date start1 = this.finalRules[1].getNextStart(base, this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), inclusive);
                    if (start1.after(start0)) {
                        tzt = new TimeZoneTransition(start0.getTime(), this.finalRules[1], this.finalRules[0]);
                    } else {
                        tzt = new TimeZoneTransition(start1.getTime(), this.finalRules[0], this.finalRules[1]);
                    }
                    result = tzt;
                    isFinal = true;
                } else {
                    return null;
                }
            } else {
                int idx2 = idx - 1;
                TimeZoneTransition prev = tzt3;
                while (idx2 > 0) {
                    TimeZoneTransition tzt4 = this.historicTransitions.get(idx2);
                    long tt3 = tzt4.getTime();
                    if (tt3 < base || (!inclusive && tt3 == base)) {
                        break;
                    }
                    idx2--;
                    prev = tzt4;
                }
                result = prev;
            }
        }
        if (result != null) {
            TimeZoneRule from = result.getFrom();
            TimeZoneRule to = result.getTo();
            if (from.getRawOffset() == to.getRawOffset() && from.getDSTSavings() == to.getDSTSavings()) {
                if (isFinal) {
                    return null;
                }
                return getNextTransition(result.getTime(), false);
            }
            return result;
        }
        return result;
    }

    @Override
    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        TimeZoneTransition result;
        complete();
        if (this.historicTransitions == null) {
            return null;
        }
        TimeZoneTransition tzt = this.historicTransitions.get(0);
        long tt = tzt.getTime();
        if (inclusive && tt == base) {
            result = tzt;
        } else {
            if (tt >= base) {
                return null;
            }
            int idx = this.historicTransitions.size() - 1;
            TimeZoneTransition tzt2 = this.historicTransitions.get(idx);
            long tt2 = tzt2.getTime();
            if (inclusive && tt2 == base) {
                result = tzt2;
            } else if (tt2 < base) {
                if (this.finalRules != null) {
                    Date start0 = this.finalRules[0].getPreviousStart(base, this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), inclusive);
                    Date start1 = this.finalRules[1].getPreviousStart(base, this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), inclusive);
                    if (start1.before(start0)) {
                        tzt2 = new TimeZoneTransition(start0.getTime(), this.finalRules[1], this.finalRules[0]);
                    } else {
                        tzt2 = new TimeZoneTransition(start1.getTime(), this.finalRules[0], this.finalRules[1]);
                    }
                }
                result = tzt2;
            } else {
                for (int idx2 = idx - 1; idx2 >= 0; idx2--) {
                    tzt2 = this.historicTransitions.get(idx2);
                    long tt3 = tzt2.getTime();
                    if (tt3 < base || (inclusive && tt3 == base)) {
                        break;
                    }
                }
                result = tzt2;
            }
        }
        if (result != null) {
            TimeZoneRule from = result.getFrom();
            TimeZoneRule to = result.getTo();
            if (from.getRawOffset() == to.getRawOffset() && from.getDSTSavings() == to.getDSTSavings()) {
                return getPreviousTransition(result.getTime(), false);
            }
            return result;
        }
        return result;
    }

    @Override
    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    private void complete() {
        Date d;
        if (this.upToDate) {
            return;
        }
        if (this.finalRules != null && this.finalRules[1] == null) {
            throw new IllegalStateException("Incomplete final rules");
        }
        if (this.historicRules != null || this.finalRules != null) {
            TimeZoneRule curRule = this.initialRule;
            long lastTransitionTime = Grego.MIN_MILLIS;
            if (this.historicRules != null) {
                BitSet done = new BitSet(this.historicRules.size());
                while (true) {
                    int curStdOffset = curRule.getRawOffset();
                    int curDstSavings = curRule.getDSTSavings();
                    long nextTransitionTime = Grego.MAX_MILLIS;
                    TimeZoneRule nextRule = null;
                    for (int i = 0; i < this.historicRules.size(); i++) {
                        if (!done.get(i)) {
                            TimeZoneRule r = this.historicRules.get(i);
                            Date d2 = r.getNextStart(lastTransitionTime, curStdOffset, curDstSavings, false);
                            if (d2 == null) {
                                done.set(i);
                            } else if (r != curRule && (!r.getName().equals(curRule.getName()) || r.getRawOffset() != curRule.getRawOffset() || r.getDSTSavings() != curRule.getDSTSavings())) {
                                long tt = d2.getTime();
                                if (tt < nextTransitionTime) {
                                    nextTransitionTime = tt;
                                    nextRule = r;
                                }
                            }
                        }
                    }
                    if (nextRule == null) {
                        boolean bDoneAll = true;
                        int j = 0;
                        while (true) {
                            if (j >= this.historicRules.size()) {
                                break;
                            }
                            if (done.get(j)) {
                                j++;
                            } else {
                                bDoneAll = false;
                                break;
                            }
                        }
                        if (bDoneAll) {
                            break;
                        }
                        if (this.finalRules != null) {
                            for (int i2 = 0; i2 < 2; i2++) {
                                if (this.finalRules[i2] != curRule && (d = this.finalRules[i2].getNextStart(lastTransitionTime, curStdOffset, curDstSavings, false)) != null) {
                                    long tt2 = d.getTime();
                                    if (tt2 < nextTransitionTime) {
                                        nextTransitionTime = tt2;
                                        nextRule = this.finalRules[i2];
                                    }
                                }
                            }
                        }
                        if (nextRule == null) {
                            break;
                        }
                        if (this.historicTransitions == null) {
                            this.historicTransitions = new ArrayList();
                        }
                        this.historicTransitions.add(new TimeZoneTransition(nextTransitionTime, curRule, nextRule));
                        lastTransitionTime = nextTransitionTime;
                        curRule = nextRule;
                    }
                }
            }
            if (this.finalRules != null) {
                if (this.historicTransitions == null) {
                    this.historicTransitions = new ArrayList();
                }
                Date d0 = this.finalRules[0].getNextStart(lastTransitionTime, curRule.getRawOffset(), curRule.getDSTSavings(), false);
                Date d1 = this.finalRules[1].getNextStart(lastTransitionTime, curRule.getRawOffset(), curRule.getDSTSavings(), false);
                if (d1.after(d0)) {
                    this.historicTransitions.add(new TimeZoneTransition(d0.getTime(), curRule, this.finalRules[0]));
                    this.historicTransitions.add(new TimeZoneTransition(this.finalRules[1].getNextStart(d0.getTime(), this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), false).getTime(), this.finalRules[0], this.finalRules[1]));
                } else {
                    this.historicTransitions.add(new TimeZoneTransition(d1.getTime(), curRule, this.finalRules[1]));
                    this.historicTransitions.add(new TimeZoneTransition(this.finalRules[0].getNextStart(d1.getTime(), this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), false).getTime(), this.finalRules[1], this.finalRules[0]));
                }
            }
        }
        this.upToDate = true;
    }

    private void getOffset(long time, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt, int[] offsets) {
        complete();
        TimeZoneRule rule = null;
        if (this.historicTransitions == null) {
            rule = this.initialRule;
        } else {
            long tstart = getTransitionTime(this.historicTransitions.get(0), local, NonExistingTimeOpt, DuplicatedTimeOpt);
            if (time < tstart) {
                rule = this.initialRule;
            } else {
                int idx = this.historicTransitions.size() - 1;
                long tend = getTransitionTime(this.historicTransitions.get(idx), local, NonExistingTimeOpt, DuplicatedTimeOpt);
                if (time > tend) {
                    if (this.finalRules != null) {
                        rule = findRuleInFinal(time, local, NonExistingTimeOpt, DuplicatedTimeOpt);
                    }
                    if (rule == null) {
                        rule = this.historicTransitions.get(idx).getTo();
                    }
                } else {
                    while (idx >= 0 && time < getTransitionTime(this.historicTransitions.get(idx), local, NonExistingTimeOpt, DuplicatedTimeOpt)) {
                        idx--;
                    }
                    rule = this.historicTransitions.get(idx).getTo();
                }
            }
        }
        offsets[0] = rule.getRawOffset();
        offsets[1] = rule.getDSTSavings();
    }

    private TimeZoneRule findRuleInFinal(long time, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt) {
        if (this.finalRules == null || this.finalRules.length != 2 || this.finalRules[0] == null || this.finalRules[1] == null) {
            return null;
        }
        long base = time;
        if (local) {
            int localDelta = getLocalDelta(this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), NonExistingTimeOpt, DuplicatedTimeOpt);
            base = time - ((long) localDelta);
        }
        Date start0 = this.finalRules[0].getPreviousStart(base, this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), true);
        long base2 = time;
        if (local) {
            int localDelta2 = getLocalDelta(this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), NonExistingTimeOpt, DuplicatedTimeOpt);
            base2 = time - ((long) localDelta2);
        }
        Date start1 = this.finalRules[1].getPreviousStart(base2, this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), true);
        if (start0 != null && start1 != null) {
            return start0.after(start1) ? this.finalRules[0] : this.finalRules[1];
        }
        if (start0 != null) {
            return this.finalRules[0];
        }
        if (start1 != null) {
            return this.finalRules[1];
        }
        return null;
    }

    private static long getTransitionTime(TimeZoneTransition tzt, boolean local, int NonExistingTimeOpt, int DuplicatedTimeOpt) {
        long time = tzt.getTime();
        if (local) {
            return time + ((long) getLocalDelta(tzt.getFrom().getRawOffset(), tzt.getFrom().getDSTSavings(), tzt.getTo().getRawOffset(), tzt.getTo().getDSTSavings(), NonExistingTimeOpt, DuplicatedTimeOpt));
        }
        return time;
    }

    private static int getLocalDelta(int rawBefore, int dstBefore, int rawAfter, int dstAfter, int NonExistingTimeOpt, int DuplicatedTimeOpt) {
        int offsetBefore = rawBefore + dstBefore;
        int offsetAfter = rawAfter + dstAfter;
        boolean dstToStd = dstBefore != 0 && dstAfter == 0;
        boolean stdToDst = dstBefore == 0 && dstAfter != 0;
        if (offsetAfter - offsetBefore >= 0) {
            if (((NonExistingTimeOpt & 3) == 1 && dstToStd) || ((NonExistingTimeOpt & 3) == 3 && stdToDst)) {
                return offsetBefore;
            }
            if (((NonExistingTimeOpt & 3) == 1 && stdToDst) || ((NonExistingTimeOpt & 3) == 3 && dstToStd)) {
                return offsetAfter;
            }
            if ((NonExistingTimeOpt & 12) == 12) {
                return offsetBefore;
            }
            return offsetAfter;
        }
        if (((DuplicatedTimeOpt & 3) == 1 && dstToStd) || ((DuplicatedTimeOpt & 3) == 3 && stdToDst)) {
            return offsetAfter;
        }
        if (((DuplicatedTimeOpt & 3) == 1 && stdToDst) || ((DuplicatedTimeOpt & 3) == 3 && dstToStd)) {
            return offsetBefore;
        }
        if ((DuplicatedTimeOpt & 12) == 4) {
            return offsetBefore;
        }
        return offsetAfter;
    }

    @Override
    public boolean isFrozen() {
        return this.isFrozen;
    }

    @Override
    public TimeZone freeze() {
        complete();
        this.isFrozen = true;
        return this;
    }

    @Override
    public TimeZone cloneAsThawed() {
        RuleBasedTimeZone tz = (RuleBasedTimeZone) super.cloneAsThawed();
        if (this.historicRules != null) {
            tz.historicRules = new ArrayList(this.historicRules);
        }
        if (this.finalRules != null) {
            tz.finalRules = (AnnualTimeZoneRule[]) this.finalRules.clone();
        }
        tz.isFrozen = false;
        return tz;
    }
}
