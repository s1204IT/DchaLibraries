package android.icu.impl.duration;

import android.icu.impl.duration.BasicPeriodBuilderFactory;

class MultiUnitBuilder extends PeriodBuilderImpl {
    private int nPeriods;

    MultiUnitBuilder(int i, BasicPeriodBuilderFactory.Settings settings) {
        super(settings);
        this.nPeriods = i;
    }

    public static MultiUnitBuilder get(int i, BasicPeriodBuilderFactory.Settings settings) {
        if (i > 0 && settings != null) {
            return new MultiUnitBuilder(i, settings);
        }
        return null;
    }

    @Override
    protected PeriodBuilder withSettings(BasicPeriodBuilderFactory.Settings settings) {
        return get(this.nPeriods, settings);
    }

    @Override
    protected Period handleCreate(long j, long j2, boolean z) {
        Period periodAnd;
        short sEffectiveSet = this.settings.effectiveSet();
        Period period = null;
        long j3 = j;
        int i = 0;
        for (int i2 = 0; i2 < TimeUnit.units.length; i2++) {
            if (((1 << i2) & sEffectiveSet) != 0) {
                TimeUnit timeUnit = TimeUnit.units[i2];
                if (i == this.nPeriods) {
                    break;
                }
                long jApproximateDurationOf = approximateDurationOf(timeUnit);
                if (j3 >= jApproximateDurationOf || i > 0) {
                    i++;
                    double d = jApproximateDurationOf;
                    double dFloor = j3 / d;
                    if (i < this.nPeriods) {
                        dFloor = Math.floor(dFloor);
                        j3 -= (long) (d * dFloor);
                    }
                    if (period == null) {
                        periodAnd = Period.at((float) dFloor, timeUnit).inPast(z);
                    } else {
                        periodAnd = period.and((float) dFloor, timeUnit);
                    }
                    period = periodAnd;
                }
            }
        }
        return period;
    }
}
