package android.icu.util;

import android.icu.impl.Grego;
import java.util.Date;

public class AnnualTimeZoneRule extends TimeZoneRule {
    public static final int MAX_YEAR = Integer.MAX_VALUE;
    private static final long serialVersionUID = -8870666707791230688L;
    private final DateTimeRule dateTimeRule;
    private final int endYear;
    private final int startYear;

    public AnnualTimeZoneRule(String name, int rawOffset, int dstSavings, DateTimeRule dateTimeRule, int startYear, int endYear) {
        super(name, rawOffset, dstSavings);
        this.dateTimeRule = dateTimeRule;
        this.startYear = startYear;
        this.endYear = endYear > Integer.MAX_VALUE ? Integer.MAX_VALUE : endYear;
    }

    public DateTimeRule getRule() {
        return this.dateTimeRule;
    }

    public int getStartYear() {
        return this.startYear;
    }

    public int getEndYear() {
        return this.endYear;
    }

    public Date getStartInYear(int year, int prevRawOffset, int prevDSTSavings) {
        long ruleDay;
        long ruleDay2;
        if (year < this.startYear || year > this.endYear) {
            return null;
        }
        int type = this.dateTimeRule.getDateRuleType();
        if (type == 0) {
            ruleDay2 = Grego.fieldsToDay(year, this.dateTimeRule.getRuleMonth(), this.dateTimeRule.getRuleDayOfMonth());
        } else {
            boolean after = true;
            if (type == 1) {
                int weeks = this.dateTimeRule.getRuleWeekInMonth();
                if (weeks > 0) {
                    long ruleDay3 = Grego.fieldsToDay(year, this.dateTimeRule.getRuleMonth(), 1);
                    ruleDay = ruleDay3 + ((long) ((weeks - 1) * 7));
                } else {
                    after = false;
                    long ruleDay4 = Grego.fieldsToDay(year, this.dateTimeRule.getRuleMonth(), Grego.monthLength(year, this.dateTimeRule.getRuleMonth()));
                    ruleDay = ruleDay4 + ((long) ((weeks + 1) * 7));
                }
            } else {
                int month = this.dateTimeRule.getRuleMonth();
                int dom = this.dateTimeRule.getRuleDayOfMonth();
                if (type == 3) {
                    after = false;
                    if (month == 1 && dom == 29 && !Grego.isLeapYear(year)) {
                        dom--;
                    }
                }
                ruleDay = Grego.fieldsToDay(year, month, dom);
            }
            int dow = Grego.dayOfWeek(ruleDay);
            int delta = this.dateTimeRule.getRuleDayOfWeek() - dow;
            if (after) {
                if (delta < 0) {
                    delta += 7;
                }
            } else if (delta > 0) {
                delta -= 7;
            }
            ruleDay2 = ruleDay + ((long) delta);
        }
        long ruleTime = (86400000 * ruleDay2) + ((long) this.dateTimeRule.getRuleMillisInDay());
        if (this.dateTimeRule.getTimeRuleType() != 2) {
            ruleTime -= (long) prevRawOffset;
        }
        if (this.dateTimeRule.getTimeRuleType() == 0) {
            ruleTime -= (long) prevDSTSavings;
        }
        return new Date(ruleTime);
    }

    @Override
    public Date getFirstStart(int prevRawOffset, int prevDSTSavings) {
        return getStartInYear(this.startYear, prevRawOffset, prevDSTSavings);
    }

    @Override
    public Date getFinalStart(int prevRawOffset, int prevDSTSavings) {
        if (this.endYear == Integer.MAX_VALUE) {
            return null;
        }
        return getStartInYear(this.endYear, prevRawOffset, prevDSTSavings);
    }

    @Override
    public Date getNextStart(long base, int prevRawOffset, int prevDSTSavings, boolean inclusive) {
        int[] fields = Grego.timeToFields(base, null);
        int year = fields[0];
        if (year < this.startYear) {
            return getFirstStart(prevRawOffset, prevDSTSavings);
        }
        Date d = getStartInYear(year, prevRawOffset, prevDSTSavings);
        if (d == null) {
            return d;
        }
        if (d.getTime() < base || (!inclusive && d.getTime() == base)) {
            return getStartInYear(year + 1, prevRawOffset, prevDSTSavings);
        }
        return d;
    }

    @Override
    public Date getPreviousStart(long base, int prevRawOffset, int prevDSTSavings, boolean inclusive) {
        int[] fields = Grego.timeToFields(base, null);
        int year = fields[0];
        if (year > this.endYear) {
            return getFinalStart(prevRawOffset, prevDSTSavings);
        }
        Date d = getStartInYear(year, prevRawOffset, prevDSTSavings);
        if (d == null) {
            return d;
        }
        if (d.getTime() > base || (!inclusive && d.getTime() == base)) {
            return getStartInYear(year - 1, prevRawOffset, prevDSTSavings);
        }
        return d;
    }

    @Override
    public boolean isEquivalentTo(TimeZoneRule timeZoneRule) {
        if ((timeZoneRule instanceof AnnualTimeZoneRule) && this.startYear == timeZoneRule.startYear && this.endYear == timeZoneRule.endYear && this.dateTimeRule.equals(timeZoneRule.dateTimeRule)) {
            return super.isEquivalentTo(timeZoneRule);
        }
        return false;
    }

    @Override
    public boolean isTransitionRule() {
        return true;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(super.toString());
        buf.append(", rule={").append(this.dateTimeRule).append("}");
        buf.append(", startYear=").append(this.startYear);
        buf.append(", endYear=");
        if (this.endYear == Integer.MAX_VALUE) {
            buf.append("max");
        } else {
            buf.append(this.endYear);
        }
        return buf.toString();
    }
}
