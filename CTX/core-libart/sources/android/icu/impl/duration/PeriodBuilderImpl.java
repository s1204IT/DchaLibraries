package android.icu.impl.duration;

import android.icu.impl.duration.BasicPeriodBuilderFactory;
import java.util.TimeZone;

abstract class PeriodBuilderImpl implements PeriodBuilder {
    protected BasicPeriodBuilderFactory.Settings settings;

    protected abstract Period handleCreate(long j, long j2, boolean z);

    protected abstract PeriodBuilder withSettings(BasicPeriodBuilderFactory.Settings settings);

    @Override
    public Period create(long j) {
        return createWithReferenceDate(j, System.currentTimeMillis());
    }

    public long approximateDurationOf(TimeUnit timeUnit) {
        return BasicPeriodBuilderFactory.approximateDurationOf(timeUnit);
    }

    @Override
    public Period createWithReferenceDate(long j, long j2) {
        boolean z = j < 0;
        if (z) {
            j = -j;
        }
        long j3 = j;
        Period periodCreateLimited = this.settings.createLimited(j3, z);
        if (periodCreateLimited == null) {
            Period periodHandleCreate = handleCreate(j3, j2, z);
            if (periodHandleCreate == null) {
                return Period.lessThan(1.0f, this.settings.effectiveMinUnit()).inPast(z);
            }
            return periodHandleCreate;
        }
        return periodCreateLimited;
    }

    @Override
    public PeriodBuilder withTimeZone(TimeZone timeZone) {
        return this;
    }

    @Override
    public PeriodBuilder withLocale(String str) {
        BasicPeriodBuilderFactory.Settings locale = this.settings.setLocale(str);
        if (locale != this.settings) {
            return withSettings(locale);
        }
        return this;
    }

    protected PeriodBuilderImpl(BasicPeriodBuilderFactory.Settings settings) {
        this.settings = settings;
    }
}
