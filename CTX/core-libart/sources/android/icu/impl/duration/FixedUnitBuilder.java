package android.icu.impl.duration;

import android.icu.impl.duration.BasicPeriodBuilderFactory;

class FixedUnitBuilder extends PeriodBuilderImpl {
    private TimeUnit unit;

    public static FixedUnitBuilder get(TimeUnit timeUnit, BasicPeriodBuilderFactory.Settings settings) {
        if (settings != null && (settings.effectiveSet() & (1 << timeUnit.ordinal)) != 0) {
            return new FixedUnitBuilder(timeUnit, settings);
        }
        return null;
    }

    FixedUnitBuilder(TimeUnit timeUnit, BasicPeriodBuilderFactory.Settings settings) {
        super(settings);
        this.unit = timeUnit;
    }

    @Override
    protected PeriodBuilder withSettings(BasicPeriodBuilderFactory.Settings settings) {
        return get(this.unit, settings);
    }

    @Override
    protected Period handleCreate(long j, long j2, boolean z) {
        if (this.unit == null) {
            return null;
        }
        return Period.at((float) (j / approximateDurationOf(this.unit)), this.unit).inPast(z);
    }
}
