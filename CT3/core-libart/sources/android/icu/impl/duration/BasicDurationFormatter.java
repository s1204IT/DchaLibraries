package android.icu.impl.duration;

import java.util.Date;
import java.util.TimeZone;

class BasicDurationFormatter implements DurationFormatter {
    private PeriodBuilder builder;
    private DateFormatter fallback;
    private long fallbackLimit;
    private PeriodFormatter formatter;
    private String localeName;
    private TimeZone timeZone;

    public BasicDurationFormatter(PeriodFormatter formatter, PeriodBuilder builder, DateFormatter fallback, long fallbackLimit) {
        this.formatter = formatter;
        this.builder = builder;
        this.fallback = fallback;
        this.fallbackLimit = fallbackLimit < 0 ? 0L : fallbackLimit;
    }

    protected BasicDurationFormatter(PeriodFormatter formatter, PeriodBuilder builder, DateFormatter fallback, long fallbackLimit, String localeName, TimeZone timeZone) {
        this.formatter = formatter;
        this.builder = builder;
        this.fallback = fallback;
        this.fallbackLimit = fallbackLimit;
        this.localeName = localeName;
        this.timeZone = timeZone;
    }

    @Override
    public String formatDurationFromNowTo(Date targetDate) {
        long now = System.currentTimeMillis();
        long duration = targetDate.getTime() - now;
        return formatDurationFrom(duration, now);
    }

    @Override
    public String formatDurationFromNow(long duration) {
        return formatDurationFrom(duration, System.currentTimeMillis());
    }

    @Override
    public String formatDurationFrom(long duration, long referenceDate) {
        String s = doFallback(duration, referenceDate);
        if (s == null) {
            Period p = doBuild(duration, referenceDate);
            return doFormat(p);
        }
        return s;
    }

    @Override
    public DurationFormatter withLocale(String locName) {
        DateFormatter dateFormatterWithLocale;
        if (!locName.equals(this.localeName)) {
            PeriodFormatter newFormatter = this.formatter.withLocale(locName);
            PeriodBuilder newBuilder = this.builder.withLocale(locName);
            if (this.fallback == null) {
                dateFormatterWithLocale = null;
            } else {
                dateFormatterWithLocale = this.fallback.withLocale(locName);
            }
            return new BasicDurationFormatter(newFormatter, newBuilder, dateFormatterWithLocale, this.fallbackLimit, locName, this.timeZone);
        }
        return this;
    }

    @Override
    public DurationFormatter withTimeZone(TimeZone tz) {
        DateFormatter dateFormatterWithTimeZone;
        if (!tz.equals(this.timeZone)) {
            PeriodBuilder newBuilder = this.builder.withTimeZone(tz);
            if (this.fallback == null) {
                dateFormatterWithTimeZone = null;
            } else {
                dateFormatterWithTimeZone = this.fallback.withTimeZone(tz);
            }
            return new BasicDurationFormatter(this.formatter, newBuilder, dateFormatterWithTimeZone, this.fallbackLimit, this.localeName, tz);
        }
        return this;
    }

    protected String doFallback(long duration, long referenceDate) {
        if (this.fallback == null || this.fallbackLimit <= 0 || Math.abs(duration) < this.fallbackLimit) {
            return null;
        }
        return this.fallback.format(referenceDate + duration);
    }

    protected Period doBuild(long duration, long referenceDate) {
        return this.builder.createWithReferenceDate(duration, referenceDate);
    }

    protected String doFormat(Period period) {
        if (!period.isSet()) {
            throw new IllegalArgumentException("period is not set");
        }
        return this.formatter.format(period);
    }
}
