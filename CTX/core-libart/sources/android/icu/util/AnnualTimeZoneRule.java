package android.icu.util;

import android.icu.impl.Grego;
import java.util.Date;

public class AnnualTimeZoneRule extends TimeZoneRule {
    public static final int MAX_YEAR = Integer.MAX_VALUE;
    private static final long serialVersionUID = -8870666707791230688L;
    private final DateTimeRule dateTimeRule;
    private final int endYear;
    private final int startYear;

    public AnnualTimeZoneRule(String str, int i, int i2, DateTimeRule dateTimeRule, int i3, int i4) {
        super(str, i, i2);
        this.dateTimeRule = dateTimeRule;
        this.startYear = i3;
        this.endYear = i4;
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

    public Date getStartInYear(int i, int i2, int i3) {
        long jFieldsToDay;
        long jFieldsToDay2;
        if (i < this.startYear || i > this.endYear) {
            return null;
        }
        int dateRuleType = this.dateTimeRule.getDateRuleType();
        if (dateRuleType == 0) {
            jFieldsToDay2 = Grego.fieldsToDay(i, this.dateTimeRule.getRuleMonth(), this.dateTimeRule.getRuleDayOfMonth());
        } else {
            boolean z = false;
            if (dateRuleType == 1) {
                int ruleWeekInMonth = this.dateTimeRule.getRuleWeekInMonth();
                if (ruleWeekInMonth > 0) {
                    jFieldsToDay = Grego.fieldsToDay(i, this.dateTimeRule.getRuleMonth(), 1) + ((long) (7 * (ruleWeekInMonth - 1)));
                    z = true;
                } else {
                    jFieldsToDay = Grego.fieldsToDay(i, this.dateTimeRule.getRuleMonth(), Grego.monthLength(i, this.dateTimeRule.getRuleMonth())) + ((long) (7 * (ruleWeekInMonth + 1)));
                }
            } else {
                int ruleMonth = this.dateTimeRule.getRuleMonth();
                int ruleDayOfMonth = this.dateTimeRule.getRuleDayOfMonth();
                if (dateRuleType == 3) {
                    if (ruleMonth == 1 && ruleDayOfMonth == 29 && !Grego.isLeapYear(i)) {
                        ruleDayOfMonth--;
                    }
                } else {
                    z = true;
                }
                jFieldsToDay = Grego.fieldsToDay(i, ruleMonth, ruleDayOfMonth);
            }
            int ruleDayOfWeek = this.dateTimeRule.getRuleDayOfWeek() - Grego.dayOfWeek(jFieldsToDay);
            if (z) {
                if (ruleDayOfWeek < 0) {
                    ruleDayOfWeek += 7;
                }
            } else if (ruleDayOfWeek > 0) {
                ruleDayOfWeek -= 7;
            }
            jFieldsToDay2 = ((long) ruleDayOfWeek) + jFieldsToDay;
        }
        long ruleMillisInDay = (jFieldsToDay2 * 86400000) + ((long) this.dateTimeRule.getRuleMillisInDay());
        if (this.dateTimeRule.getTimeRuleType() != 2) {
            ruleMillisInDay -= (long) i2;
        }
        if (this.dateTimeRule.getTimeRuleType() == 0) {
            ruleMillisInDay -= (long) i3;
        }
        return new Date(ruleMillisInDay);
    }

    @Override
    public Date getFirstStart(int i, int i2) {
        return getStartInYear(this.startYear, i, i2);
    }

    @Override
    public Date getFinalStart(int i, int i2) {
        if (this.endYear == Integer.MAX_VALUE) {
            return null;
        }
        return getStartInYear(this.endYear, i, i2);
    }

    @Override
    public Date getNextStart(long j, int i, int i2, boolean z) {
        int i3 = Grego.timeToFields(j, null)[0];
        if (i3 < this.startYear) {
            return getFirstStart(i, i2);
        }
        Date startInYear = getStartInYear(i3, i, i2);
        if (startInYear == null) {
            return startInYear;
        }
        if (startInYear.getTime() < j || (!z && startInYear.getTime() == j)) {
            return getStartInYear(i3 + 1, i, i2);
        }
        return startInYear;
    }

    @Override
    public Date getPreviousStart(long j, int i, int i2, boolean z) {
        int i3 = Grego.timeToFields(j, null)[0];
        if (i3 > this.endYear) {
            return getFinalStart(i, i2);
        }
        Date startInYear = getStartInYear(i3, i, i2);
        if (startInYear == null) {
            return startInYear;
        }
        if (startInYear.getTime() > j || (!z && startInYear.getTime() == j)) {
            return getStartInYear(i3 - 1, i, i2);
        }
        return startInYear;
    }

    @Override
    public boolean isEquivalentTo(TimeZoneRule timeZoneRule) {
        if (!(timeZoneRule instanceof AnnualTimeZoneRule)) {
            return false;
        }
        AnnualTimeZoneRule annualTimeZoneRule = (AnnualTimeZoneRule) timeZoneRule;
        if (this.startYear == annualTimeZoneRule.startYear && this.endYear == annualTimeZoneRule.endYear && this.dateTimeRule.equals(annualTimeZoneRule.dateTimeRule)) {
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
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append(", rule={" + this.dateTimeRule + "}");
        StringBuilder sb2 = new StringBuilder();
        sb2.append(", startYear=");
        sb2.append(this.startYear);
        sb.append(sb2.toString());
        sb.append(", endYear=");
        if (this.endYear == Integer.MAX_VALUE) {
            sb.append("max");
        } else {
            sb.append(this.endYear);
        }
        return sb.toString();
    }
}
