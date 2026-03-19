package android.icu.impl.duration;

import java.util.Locale;
import java.util.TimeZone;

class BasicDurationFormatterFactory implements DurationFormatterFactory {
    private PeriodBuilder builder;
    private BasicDurationFormatter f;
    private DateFormatter fallback;
    private long fallbackLimit;
    private PeriodFormatter formatter;
    private BasicPeriodFormatterService ps;
    private String localeName = Locale.getDefault().toString();
    private TimeZone timeZone = TimeZone.getDefault();

    BasicDurationFormatterFactory(BasicPeriodFormatterService basicPeriodFormatterService) {
        this.ps = basicPeriodFormatterService;
    }

    @Override
    public DurationFormatterFactory setPeriodFormatter(PeriodFormatter periodFormatter) {
        if (periodFormatter != this.formatter) {
            this.formatter = periodFormatter;
            reset();
        }
        return this;
    }

    @Override
    public DurationFormatterFactory setPeriodBuilder(PeriodBuilder periodBuilder) {
        if (periodBuilder != this.builder) {
            this.builder = periodBuilder;
            reset();
        }
        return this;
    }

    @Override
    public DurationFormatterFactory setFallback(DateFormatter dateFormatter) {
        boolean z = false;
        if (dateFormatter != null ? !dateFormatter.equals(this.fallback) : this.fallback != null) {
            z = true;
        }
        if (z) {
            this.fallback = dateFormatter;
            reset();
        }
        return this;
    }

    @Override
    public DurationFormatterFactory setFallbackLimit(long j) {
        if (j < 0) {
            j = 0;
        }
        if (j != this.fallbackLimit) {
            this.fallbackLimit = j;
            reset();
        }
        return this;
    }

    @Override
    public DurationFormatterFactory setLocale(String str) {
        if (!str.equals(this.localeName)) {
            this.localeName = str;
            if (this.builder != null) {
                this.builder = this.builder.withLocale(str);
            }
            if (this.formatter != null) {
                this.formatter = this.formatter.withLocale(str);
            }
            reset();
        }
        return this;
    }

    @Override
    public DurationFormatterFactory setTimeZone(TimeZone timeZone) {
        if (!timeZone.equals(this.timeZone)) {
            this.timeZone = timeZone;
            if (this.builder != null) {
                this.builder = this.builder.withTimeZone(timeZone);
            }
            reset();
        }
        return this;
    }

    @Override
    public DurationFormatter getFormatter() {
        if (this.f == null) {
            if (this.fallback != null) {
                this.fallback = this.fallback.withLocale(this.localeName).withTimeZone(this.timeZone);
            }
            this.formatter = getPeriodFormatter();
            this.builder = getPeriodBuilder();
            this.f = createFormatter();
        }
        return this.f;
    }

    public PeriodFormatter getPeriodFormatter() {
        if (this.formatter == null) {
            this.formatter = this.ps.newPeriodFormatterFactory().setLocale(this.localeName).getFormatter();
        }
        return this.formatter;
    }

    public PeriodBuilder getPeriodBuilder() {
        if (this.builder == null) {
            this.builder = this.ps.newPeriodBuilderFactory().setLocale(this.localeName).setTimeZone(this.timeZone).getSingleUnitBuilder();
        }
        return this.builder;
    }

    public DateFormatter getFallback() {
        return this.fallback;
    }

    public long getFallbackLimit() {
        if (this.fallback == null) {
            return 0L;
        }
        return this.fallbackLimit;
    }

    public String getLocaleName() {
        return this.localeName;
    }

    public TimeZone getTimeZone() {
        return this.timeZone;
    }

    protected BasicDurationFormatter createFormatter() {
        return new BasicDurationFormatter(this.formatter, this.builder, this.fallback, this.fallbackLimit, this.localeName, this.timeZone);
    }

    protected void reset() {
        this.f = null;
    }
}
