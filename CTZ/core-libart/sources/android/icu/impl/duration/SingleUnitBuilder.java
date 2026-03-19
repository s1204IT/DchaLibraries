package android.icu.impl.duration;

import android.icu.impl.duration.BasicPeriodBuilderFactory;

class SingleUnitBuilder extends PeriodBuilderImpl {
    SingleUnitBuilder(BasicPeriodBuilderFactory.Settings settings) {
        super(settings);
    }

    public static SingleUnitBuilder get(BasicPeriodBuilderFactory.Settings settings) {
        if (settings == null) {
            return null;
        }
        return new SingleUnitBuilder(settings);
    }

    @Override
    protected PeriodBuilder withSettings(BasicPeriodBuilderFactory.Settings settings) {
        return get(settings);
    }

    @Override
    protected Period handleCreate(long j, long j2, boolean z) {
        short sEffectiveSet = this.settings.effectiveSet();
        for (int i = 0; i < TimeUnit.units.length; i++) {
            if (((1 << i) & sEffectiveSet) != 0) {
                TimeUnit timeUnit = TimeUnit.units[i];
                long jApproximateDurationOf = approximateDurationOf(timeUnit);
                if (j >= jApproximateDurationOf) {
                    return Period.at((float) (j / jApproximateDurationOf), timeUnit).inPast(z);
                }
            }
        }
        return null;
    }
}
