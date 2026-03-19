package android.icu.impl.duration;

import android.icu.impl.duration.BasicPeriodBuilderFactory;

class FixedUnitBuilder extends PeriodBuilderImpl {
    private TimeUnit unit;

    public static FixedUnitBuilder get(TimeUnit unit, BasicPeriodBuilderFactory.Settings settingsToUse) {
        if (settingsToUse == null || (settingsToUse.effectiveSet() & (1 << unit.ordinal)) == 0) {
            return null;
        }
        return new FixedUnitBuilder(unit, settingsToUse);
    }

    FixedUnitBuilder(TimeUnit unit, BasicPeriodBuilderFactory.Settings settings) {
        super(settings);
        this.unit = unit;
    }

    @Override
    protected PeriodBuilder withSettings(BasicPeriodBuilderFactory.Settings settingsToUse) {
        return get(this.unit, settingsToUse);
    }

    @Override
    protected Period handleCreate(long duration, long referenceDate, boolean inPast) {
        if (this.unit == null) {
            return null;
        }
        long unitDuration = approximateDurationOf(this.unit);
        return Period.at((float) (duration / unitDuration), this.unit).inPast(inPast);
    }
}
