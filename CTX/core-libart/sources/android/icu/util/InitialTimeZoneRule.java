package android.icu.util;

import java.util.Date;

public class InitialTimeZoneRule extends TimeZoneRule {
    private static final long serialVersionUID = 1876594993064051206L;

    public InitialTimeZoneRule(String str, int i, int i2) {
        super(str, i, i2);
    }

    @Override
    public boolean isEquivalentTo(TimeZoneRule timeZoneRule) {
        if (timeZoneRule instanceof InitialTimeZoneRule) {
            return super.isEquivalentTo(timeZoneRule);
        }
        return false;
    }

    @Override
    public Date getFinalStart(int i, int i2) {
        return null;
    }

    @Override
    public Date getFirstStart(int i, int i2) {
        return null;
    }

    @Override
    public Date getNextStart(long j, int i, int i2, boolean z) {
        return null;
    }

    @Override
    public Date getPreviousStart(long j, int i, int i2, boolean z) {
        return null;
    }

    @Override
    public boolean isTransitionRule() {
        return false;
    }
}
