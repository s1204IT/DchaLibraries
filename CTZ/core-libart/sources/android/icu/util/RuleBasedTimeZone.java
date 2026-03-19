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

    public RuleBasedTimeZone(String str, InitialTimeZoneRule initialTimeZoneRule) {
        super(str);
        this.isFrozen = false;
        this.initialRule = initialTimeZoneRule;
    }

    public void addTransitionRule(TimeZoneRule timeZoneRule) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen RuleBasedTimeZone instance.");
        }
        if (!timeZoneRule.isTransitionRule()) {
            throw new IllegalArgumentException("Rule must be a transition rule");
        }
        if (timeZoneRule instanceof AnnualTimeZoneRule) {
            AnnualTimeZoneRule annualTimeZoneRule = (AnnualTimeZoneRule) timeZoneRule;
            if (annualTimeZoneRule.getEndYear() == Integer.MAX_VALUE) {
                if (this.finalRules == null) {
                    this.finalRules = new AnnualTimeZoneRule[2];
                    this.finalRules[0] = annualTimeZoneRule;
                } else if (this.finalRules[1] == null) {
                    this.finalRules[1] = annualTimeZoneRule;
                } else {
                    throw new IllegalStateException("Too many final rules");
                }
            } else {
                if (this.historicRules == null) {
                    this.historicRules = new ArrayList();
                }
                this.historicRules.add(timeZoneRule);
            }
        }
        this.upToDate = false;
    }

    @Override
    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6) {
        if (i == 0) {
            i2 = 1 - i2;
        }
        int[] iArr = new int[2];
        getOffset((Grego.fieldsToDay(i2, i3, i4) * 86400000) + ((long) i6), true, 3, 1, iArr);
        return iArr[0] + iArr[1];
    }

    @Override
    public void getOffset(long j, boolean z, int[] iArr) {
        getOffset(j, z, 4, 12, iArr);
    }

    @Override
    @Deprecated
    public void getOffsetFromLocal(long j, int i, int i2, int[] iArr) {
        getOffset(j, true, i, i2, iArr);
    }

    @Override
    public int getRawOffset() {
        int[] iArr = new int[2];
        getOffset(System.currentTimeMillis(), false, iArr);
        return iArr[0];
    }

    @Override
    public boolean inDaylightTime(Date date) {
        int[] iArr = new int[2];
        getOffset(date.getTime(), false, iArr);
        return iArr[1] != 0;
    }

    @Override
    public void setRawOffset(int i) {
        throw new UnsupportedOperationException("setRawOffset in RuleBasedTimeZone is not supported.");
    }

    @Override
    public boolean useDaylightTime() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        int[] iArr = new int[2];
        getOffset(jCurrentTimeMillis, false, iArr);
        if (iArr[1] != 0) {
            return true;
        }
        TimeZoneTransition nextTransition = getNextTransition(jCurrentTimeMillis, false);
        if (nextTransition == null || nextTransition.getTo().getDSTSavings() == 0) {
            return false;
        }
        return true;
    }

    @Override
    public boolean observesDaylightTime() {
        long jCurrentTimeMillis = System.currentTimeMillis();
        int[] iArr = new int[2];
        getOffset(jCurrentTimeMillis, false, iArr);
        if (iArr[1] != 0) {
            return true;
        }
        BitSet bitSet = this.finalRules == null ? null : new BitSet(this.finalRules.length);
        while (true) {
            TimeZoneTransition nextTransition = getNextTransition(jCurrentTimeMillis, false);
            if (nextTransition == null) {
                break;
            }
            TimeZoneRule to = nextTransition.getTo();
            if (to.getDSTSavings() != 0) {
                return true;
            }
            if (bitSet != null) {
                for (int i = 0; i < this.finalRules.length; i++) {
                    if (this.finalRules[i].equals(to)) {
                        bitSet.set(i);
                    }
                }
                if (bitSet.cardinality() == this.finalRules.length) {
                    break;
                }
            }
            jCurrentTimeMillis = nextTransition.getTime();
        }
        return false;
    }

    @Override
    public boolean hasSameRules(TimeZone timeZone) {
        boolean z;
        if (this == timeZone) {
            return true;
        }
        if (!(timeZone instanceof RuleBasedTimeZone)) {
            return false;
        }
        RuleBasedTimeZone ruleBasedTimeZone = (RuleBasedTimeZone) timeZone;
        if (!this.initialRule.isEquivalentTo(ruleBasedTimeZone.initialRule)) {
            return false;
        }
        if (this.finalRules != null && ruleBasedTimeZone.finalRules != null) {
            for (int i = 0; i < this.finalRules.length; i++) {
                if (!(this.finalRules[i] == null && ruleBasedTimeZone.finalRules[i] == null) && (this.finalRules[i] == null || ruleBasedTimeZone.finalRules[i] == null || !this.finalRules[i].isEquivalentTo(ruleBasedTimeZone.finalRules[i]))) {
                    return false;
                }
            }
        } else if (this.finalRules != null || ruleBasedTimeZone.finalRules != null) {
            return false;
        }
        if (this.historicRules != null && ruleBasedTimeZone.historicRules != null) {
            if (this.historicRules.size() != ruleBasedTimeZone.historicRules.size()) {
                return false;
            }
            for (TimeZoneRule timeZoneRule : this.historicRules) {
                Iterator<TimeZoneRule> it = ruleBasedTimeZone.historicRules.iterator();
                while (true) {
                    if (it.hasNext()) {
                        if (timeZoneRule.isEquivalentTo(it.next())) {
                            z = true;
                            break;
                        }
                    } else {
                        z = false;
                        break;
                    }
                }
                if (!z) {
                    return false;
                }
            }
        } else if (this.historicRules != null || ruleBasedTimeZone.historicRules != null) {
            return false;
        }
        return true;
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules() {
        int size;
        int i;
        if (this.historicRules != null) {
            size = this.historicRules.size() + 1;
        } else {
            size = 1;
        }
        if (this.finalRules != null) {
            if (this.finalRules[1] != null) {
                size += 2;
            } else {
                size++;
            }
        }
        TimeZoneRule[] timeZoneRuleArr = new TimeZoneRule[size];
        timeZoneRuleArr[0] = this.initialRule;
        if (this.historicRules != null) {
            i = 1;
            while (i < this.historicRules.size() + 1) {
                timeZoneRuleArr[i] = this.historicRules.get(i - 1);
                i++;
            }
        } else {
            i = 1;
        }
        if (this.finalRules != null) {
            int i2 = i + 1;
            timeZoneRuleArr[i] = this.finalRules[0];
            if (this.finalRules[1] != null) {
                timeZoneRuleArr[i2] = this.finalRules[1];
            }
        }
        return timeZoneRuleArr;
    }

    @Override
    public TimeZoneTransition getNextTransition(long j, boolean z) {
        complete();
        if (this.historicTransitions == null) {
            return null;
        }
        TimeZoneTransition timeZoneTransition = this.historicTransitions.get(0);
        long time = timeZoneTransition.getTime();
        boolean z2 = true;
        if (time <= j && (!z || time != j)) {
            int size = this.historicTransitions.size() - 1;
            TimeZoneTransition timeZoneTransition2 = this.historicTransitions.get(size);
            long time2 = timeZoneTransition2.getTime();
            if (z && time2 == j) {
                z2 = false;
                timeZoneTransition = timeZoneTransition2;
            } else if (time2 <= j) {
                if (this.finalRules == null) {
                    return null;
                }
                Date nextStart = this.finalRules[0].getNextStart(j, this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), z);
                Date nextStart2 = this.finalRules[1].getNextStart(j, this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), z);
                if (nextStart2.after(nextStart)) {
                    timeZoneTransition = new TimeZoneTransition(nextStart.getTime(), this.finalRules[1], this.finalRules[0]);
                } else {
                    timeZoneTransition = new TimeZoneTransition(nextStart2.getTime(), this.finalRules[0], this.finalRules[1]);
                }
            } else {
                int i = size - 1;
                while (i > 0) {
                    TimeZoneTransition timeZoneTransition3 = this.historicTransitions.get(i);
                    long time3 = timeZoneTransition3.getTime();
                    if (time3 < j || (!z && time3 == j)) {
                        break;
                    }
                    i--;
                    timeZoneTransition2 = timeZoneTransition3;
                }
                z2 = false;
                timeZoneTransition = timeZoneTransition2;
            }
        } else {
            z2 = false;
        }
        TimeZoneRule from = timeZoneTransition.getFrom();
        TimeZoneRule to = timeZoneTransition.getTo();
        if (from.getRawOffset() == to.getRawOffset() && from.getDSTSavings() == to.getDSTSavings()) {
            if (z2) {
                return null;
            }
            return getNextTransition(timeZoneTransition.getTime(), false);
        }
        return timeZoneTransition;
    }

    @Override
    public TimeZoneTransition getPreviousTransition(long j, boolean z) {
        complete();
        if (this.historicTransitions == null) {
            return null;
        }
        TimeZoneTransition timeZoneTransition = this.historicTransitions.get(0);
        long time = timeZoneTransition.getTime();
        if (!z || time != j) {
            if (time >= j) {
                return null;
            }
            int size = this.historicTransitions.size() - 1;
            TimeZoneTransition timeZoneTransition2 = this.historicTransitions.get(size);
            long time2 = timeZoneTransition2.getTime();
            if (z && time2 == j) {
                timeZoneTransition = timeZoneTransition2;
            } else if (time2 < j) {
                if (this.finalRules != null) {
                    Date previousStart = this.finalRules[0].getPreviousStart(j, this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), z);
                    Date previousStart2 = this.finalRules[1].getPreviousStart(j, this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), z);
                    if (previousStart2.before(previousStart)) {
                        timeZoneTransition = new TimeZoneTransition(previousStart.getTime(), this.finalRules[1], this.finalRules[0]);
                    } else {
                        timeZoneTransition = new TimeZoneTransition(previousStart2.getTime(), this.finalRules[0], this.finalRules[1]);
                    }
                } else {
                    timeZoneTransition = timeZoneTransition2;
                }
            } else {
                for (int i = size - 1; i >= 0; i--) {
                    timeZoneTransition2 = this.historicTransitions.get(i);
                    long time3 = timeZoneTransition2.getTime();
                    if (time3 < j || (z && time3 == j)) {
                        break;
                    }
                }
                timeZoneTransition = timeZoneTransition2;
            }
        }
        TimeZoneRule from = timeZoneTransition.getFrom();
        TimeZoneRule to = timeZoneTransition.getTo();
        if (from.getRawOffset() == to.getRawOffset() && from.getDSTSavings() == to.getDSTSavings()) {
            return getPreviousTransition(timeZoneTransition.getTime(), false);
        }
        return timeZoneTransition;
    }

    @Override
    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    private void complete() {
        boolean z;
        Date nextStart;
        boolean z2;
        if (this.upToDate) {
            return;
        }
        if (this.finalRules != null && this.finalRules[1] == null) {
            throw new IllegalStateException("Incomplete final rules");
        }
        if (this.historicRules != null || this.finalRules != null) {
            TimeZoneRule timeZoneRule = this.initialRule;
            long j = Grego.MIN_MILLIS;
            if (this.historicRules != null) {
                BitSet bitSet = new BitSet(this.historicRules.size());
                while (true) {
                    int rawOffset = timeZoneRule.getRawOffset();
                    int dSTSavings = timeZoneRule.getDSTSavings();
                    long j2 = 183882168921600000L;
                    TimeZoneRule timeZoneRule2 = null;
                    for (int i = 0; i < this.historicRules.size(); i++) {
                        if (!bitSet.get(i)) {
                            TimeZoneRule timeZoneRule3 = this.historicRules.get(i);
                            Date nextStart2 = timeZoneRule3.getNextStart(j, rawOffset, dSTSavings, false);
                            if (nextStart2 == null) {
                                bitSet.set(i);
                            } else if (timeZoneRule3 != timeZoneRule && (!timeZoneRule3.getName().equals(timeZoneRule.getName()) || timeZoneRule3.getRawOffset() != timeZoneRule.getRawOffset() || timeZoneRule3.getDSTSavings() != timeZoneRule.getDSTSavings())) {
                                long time = nextStart2.getTime();
                                if (time < j2) {
                                    timeZoneRule2 = timeZoneRule3;
                                    j2 = time;
                                }
                            }
                        }
                    }
                    if (timeZoneRule2 == null) {
                        int i2 = 0;
                        while (true) {
                            if (i2 < this.historicRules.size()) {
                                if (bitSet.get(i2)) {
                                    i2++;
                                } else {
                                    z2 = false;
                                    break;
                                }
                            } else {
                                z2 = true;
                                break;
                            }
                        }
                        if (z2) {
                            break;
                        }
                        if (this.finalRules != null) {
                            for (int i3 = 0; i3 < 2; i3++) {
                                if (this.finalRules[i3] != timeZoneRule && (nextStart = this.finalRules[i3].getNextStart(j, rawOffset, dSTSavings, false)) != null) {
                                    long time2 = nextStart.getTime();
                                    if (time2 < j2) {
                                        j2 = time2;
                                        timeZoneRule2 = this.finalRules[i3];
                                    }
                                }
                            }
                        }
                        TimeZoneRule timeZoneRule4 = timeZoneRule2;
                        long j3 = j2;
                        if (timeZoneRule4 == null) {
                            break;
                        }
                        if (this.historicTransitions == null) {
                            this.historicTransitions = new ArrayList();
                        }
                        this.historicTransitions.add(new TimeZoneTransition(j3, timeZoneRule, timeZoneRule4));
                        timeZoneRule = timeZoneRule4;
                        j = j3;
                    }
                }
            }
            if (this.finalRules != null) {
                if (this.historicTransitions == null) {
                    this.historicTransitions = new ArrayList();
                }
                long j4 = j;
                Date nextStart3 = this.finalRules[0].getNextStart(j4, timeZoneRule.getRawOffset(), timeZoneRule.getDSTSavings(), false);
                Date nextStart4 = this.finalRules[1].getNextStart(j4, timeZoneRule.getRawOffset(), timeZoneRule.getDSTSavings(), false);
                if (nextStart4.after(nextStart3)) {
                    this.historicTransitions.add(new TimeZoneTransition(nextStart3.getTime(), timeZoneRule, this.finalRules[0]));
                    z = true;
                    this.historicTransitions.add(new TimeZoneTransition(this.finalRules[1].getNextStart(nextStart3.getTime(), this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), false).getTime(), this.finalRules[0], this.finalRules[1]));
                } else {
                    this.historicTransitions.add(new TimeZoneTransition(nextStart4.getTime(), timeZoneRule, this.finalRules[1]));
                    z = true;
                    this.historicTransitions.add(new TimeZoneTransition(this.finalRules[0].getNextStart(nextStart4.getTime(), this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), false).getTime(), this.finalRules[1], this.finalRules[0]));
                }
            } else {
                z = true;
            }
        } else {
            z = true;
        }
        this.upToDate = z;
    }

    private void getOffset(long j, boolean z, int i, int i2, int[] iArr) {
        TimeZoneRule to;
        complete();
        if (this.historicTransitions == null || j < getTransitionTime(this.historicTransitions.get(0), z, i, i2)) {
            to = this.initialRule;
        } else {
            int size = this.historicTransitions.size() - 1;
            if (j > getTransitionTime(this.historicTransitions.get(size), z, i, i2)) {
                if (this.finalRules != null) {
                    to = findRuleInFinal(j, z, i, i2);
                } else {
                    to = null;
                }
                if (to == null) {
                    to = this.historicTransitions.get(size).getTo();
                }
            } else {
                while (size >= 0 && j < getTransitionTime(this.historicTransitions.get(size), z, i, i2)) {
                    size--;
                }
                to = this.historicTransitions.get(size).getTo();
            }
        }
        iArr[0] = to.getRawOffset();
        iArr[1] = to.getDSTSavings();
    }

    private TimeZoneRule findRuleInFinal(long j, boolean z, int i, int i2) {
        if (this.finalRules == null || this.finalRules.length != 2 || this.finalRules[0] == null || this.finalRules[1] == null) {
            return null;
        }
        Date previousStart = this.finalRules[0].getPreviousStart(z ? j - ((long) getLocalDelta(this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), i, i2)) : j, this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), true);
        Date previousStart2 = this.finalRules[1].getPreviousStart(z ? j - ((long) getLocalDelta(this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), this.finalRules[1].getRawOffset(), this.finalRules[1].getDSTSavings(), i, i2)) : j, this.finalRules[0].getRawOffset(), this.finalRules[0].getDSTSavings(), true);
        if (previousStart != null && previousStart2 != null) {
            return previousStart.after(previousStart2) ? this.finalRules[0] : this.finalRules[1];
        }
        if (previousStart != null) {
            return this.finalRules[0];
        }
        if (previousStart2 != null) {
            return this.finalRules[1];
        }
        return null;
    }

    private static long getTransitionTime(TimeZoneTransition timeZoneTransition, boolean z, int i, int i2) {
        long time = timeZoneTransition.getTime();
        if (z) {
            return time + ((long) getLocalDelta(timeZoneTransition.getFrom().getRawOffset(), timeZoneTransition.getFrom().getDSTSavings(), timeZoneTransition.getTo().getRawOffset(), timeZoneTransition.getTo().getDSTSavings(), i, i2));
        }
        return time;
    }

    private static int getLocalDelta(int i, int i2, int i3, int i4, int i5, int i6) {
        int i7 = i + i2;
        int i8 = i3 + i4;
        boolean z = false;
        boolean z2 = i2 != 0 && i4 == 0;
        if (i2 == 0 && i4 != 0) {
            z = true;
        }
        if (i8 - i7 >= 0) {
            int i9 = i5 & 3;
            if (i9 == 1 && z2) {
                return i7;
            }
            if (i9 == 3 && z) {
                return i7;
            }
            if ((i9 != 1 || !z) && ((i9 != 3 || !z2) && (i5 & 12) == 12)) {
                return i7;
            }
        } else {
            int i10 = i6 & 3;
            if ((i10 != 1 || !z2) && (i10 != 3 || !z)) {
                if (i10 == 1 && z) {
                    return i7;
                }
                if ((i10 == 3 && z2) || (i6 & 12) == 4) {
                    return i7;
                }
            }
        }
        return i8;
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
        RuleBasedTimeZone ruleBasedTimeZone = (RuleBasedTimeZone) super.cloneAsThawed();
        if (this.historicRules != null) {
            ruleBasedTimeZone.historicRules = new ArrayList(this.historicRules);
        }
        if (this.finalRules != null) {
            ruleBasedTimeZone.finalRules = (AnnualTimeZoneRule[]) this.finalRules.clone();
        }
        ruleBasedTimeZone.isFrozen = false;
        return ruleBasedTimeZone;
    }
}
