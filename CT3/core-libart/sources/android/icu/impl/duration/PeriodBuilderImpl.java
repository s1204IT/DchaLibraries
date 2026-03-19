package android.icu.impl.duration;

import android.icu.impl.duration.BasicPeriodBuilderFactory;
import java.util.TimeZone;

abstract class PeriodBuilderImpl implements PeriodBuilder {
    protected BasicPeriodBuilderFactory.Settings settings;

    protected abstract Period handleCreate(long j, long j2, boolean z);

    protected abstract PeriodBuilder withSettings(BasicPeriodBuilderFactory.Settings settings);

    @Override
    public Period create(long duration) {
        return createWithReferenceDate(duration, System.currentTimeMillis());
    }

    public long approximateDurationOf(TimeUnit unit) {
        return BasicPeriodBuilderFactory.approximateDurationOf(unit);
    }

    @Override
    public Period createWithReferenceDate(long duration, long referenceDate) {
        boolean inPast = duration < 0;
        if (inPast) {
            duration = -duration;
        }
        Period ts = this.settings.createLimited(duration, inPast);
        if (ts == null) {
            Period ts2 = handleCreate(duration, referenceDate, inPast);
            if (ts2 == null) {
                return Period.lessThan(1.0f, this.settings.effectiveMinUnit()).inPast(inPast);
            }
            return ts2;
        }
        return ts;
    }

    @Override
    public PeriodBuilder withTimeZone(TimeZone timeZone) {
        return this;
    }

    @Override
    public PeriodBuilder withLocale(String localeName) {
        BasicPeriodBuilderFactory.Settings newSettings = this.settings.setLocale(localeName);
        if (newSettings != this.settings) {
            return withSettings(newSettings);
        }
        return this;
    }

    protected PeriodBuilderImpl(BasicPeriodBuilderFactory.Settings settings) {
        this.settings = settings;
    }
}
