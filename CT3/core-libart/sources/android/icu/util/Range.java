package android.icu.util;

import java.util.Date;

class Range {
    public DateRule rule;
    public Date start;

    public Range(Date start, DateRule rule) {
        this.start = start;
        this.rule = rule;
    }
}
