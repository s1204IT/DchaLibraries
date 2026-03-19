package android.icu.util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class RangeDateRule implements DateRule {
    List<Range> ranges = new ArrayList(2);

    public void add(DateRule rule) {
        add(new Date(Long.MIN_VALUE), rule);
    }

    public void add(Date start, DateRule rule) {
        this.ranges.add(new Range(start, rule));
    }

    @Override
    public Date firstAfter(Date start) {
        int index = startIndex(start);
        if (index == this.ranges.size()) {
            index = 0;
        }
        Range r = rangeAt(index);
        Range e = rangeAt(index + 1);
        if (r == null || r.rule == null) {
            return null;
        }
        if (e != null) {
            Date result = r.rule.firstBetween(start, e.start);
            return result;
        }
        Date result2 = r.rule.firstAfter(start);
        return result2;
    }

    @Override
    public Date firstBetween(Date start, Date end) {
        Date e;
        if (end == null) {
            return firstAfter(start);
        }
        int index = startIndex(start);
        Date result = null;
        Range next = rangeAt(index);
        while (result == null && next != null && !next.start.after(end)) {
            Range r = next;
            next = rangeAt(index + 1);
            if (r.rule != null) {
                if (next != null && !next.start.after(end)) {
                    e = next.start;
                } else {
                    e = end;
                }
                result = r.rule.firstBetween(start, e);
            }
        }
        return result;
    }

    @Override
    public boolean isOn(Date date) {
        Range r = rangeAt(startIndex(date));
        if (r == null || r.rule == null) {
            return false;
        }
        return r.rule.isOn(date);
    }

    @Override
    public boolean isBetween(Date start, Date end) {
        return firstBetween(start, end) == null;
    }

    private int startIndex(Date start) {
        int lastIndex = this.ranges.size();
        for (int i = 0; i < this.ranges.size(); i++) {
            Range r = this.ranges.get(i);
            if (start.before(r.start)) {
                break;
            }
            lastIndex = i;
        }
        return lastIndex;
    }

    private Range rangeAt(int index) {
        if (index < this.ranges.size()) {
            return this.ranges.get(index);
        }
        return null;
    }
}
