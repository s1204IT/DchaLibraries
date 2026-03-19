package android.icu.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RangeDateRule implements DateRule {
    List<Range> ranges = new ArrayList(2);

    public void add(DateRule dateRule) {
        add(new Date(Long.MIN_VALUE), dateRule);
    }

    public void add(Date date, DateRule dateRule) {
        this.ranges.add(new Range(date, dateRule));
    }

    @Override
    public Date firstAfter(Date date) {
        int iStartIndex = startIndex(date);
        if (iStartIndex == this.ranges.size()) {
            iStartIndex = 0;
        }
        Range rangeRangeAt = rangeAt(iStartIndex);
        Range rangeRangeAt2 = rangeAt(iStartIndex + 1);
        if (rangeRangeAt == null || rangeRangeAt.rule == null) {
            return null;
        }
        if (rangeRangeAt2 != null) {
            return rangeRangeAt.rule.firstBetween(date, rangeRangeAt2.start);
        }
        return rangeRangeAt.rule.firstAfter(date);
    }

    @Override
    public Date firstBetween(Date date, Date date2) {
        if (date2 == null) {
            return firstAfter(date);
        }
        int iStartIndex = startIndex(date);
        Date dateFirstBetween = null;
        Range rangeRangeAt = rangeAt(iStartIndex);
        while (dateFirstBetween == null && rangeRangeAt != null && !rangeRangeAt.start.after(date2)) {
            Range rangeRangeAt2 = rangeAt(iStartIndex + 1);
            if (rangeRangeAt.rule != null) {
                dateFirstBetween = rangeRangeAt.rule.firstBetween(date, (rangeRangeAt2 == null || rangeRangeAt2.start.after(date2)) ? date2 : rangeRangeAt2.start);
            }
            rangeRangeAt = rangeRangeAt2;
        }
        return dateFirstBetween;
    }

    @Override
    public boolean isOn(Date date) {
        Range rangeRangeAt = rangeAt(startIndex(date));
        return (rangeRangeAt == null || rangeRangeAt.rule == null || !rangeRangeAt.rule.isOn(date)) ? false : true;
    }

    @Override
    public boolean isBetween(Date date, Date date2) {
        return firstBetween(date, date2) == null;
    }

    private int startIndex(Date date) {
        int i;
        int size = this.ranges.size();
        int i2 = 0;
        while (true) {
            int i3 = i2;
            i = size;
            size = i3;
            if (size >= this.ranges.size() || date.before(this.ranges.get(size).start)) {
                break;
            }
            i2 = size + 1;
        }
        return i;
    }

    private Range rangeAt(int i) {
        if (i < this.ranges.size()) {
            return this.ranges.get(i);
        }
        return null;
    }
}
