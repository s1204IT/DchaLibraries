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

    public BasicDurationFormatter(PeriodFormatter periodFormatter, PeriodBuilder periodBuilder, DateFormatter dateFormatter, long j) {
        this.formatter = periodFormatter;
        this.builder = periodBuilder;
        this.fallback = dateFormatter;
        this.fallbackLimit = j >= 0 ? j : 0L;
    }

    protected BasicDurationFormatter(PeriodFormatter periodFormatter, PeriodBuilder periodBuilder, DateFormatter dateFormatter, long j, String str, TimeZone timeZone) {
        this.formatter = periodFormatter;
        this.builder = periodBuilder;
        this.fallback = dateFormatter;
        this.fallbackLimit = j;
        this.localeName = str;
        this.timeZone = timeZone;
    }

    @Override
    public String formatDurationFromNowTo(Date date) {
        long jCurrentTimeMillis = System.currentTimeMillis();
        return formatDurationFrom(date.getTime() - jCurrentTimeMillis, jCurrentTimeMillis);
    }

    @Override
    public String formatDurationFromNow(long j) {
        return formatDurationFrom(j, System.currentTimeMillis());
    }

    @Override
    public String formatDurationFrom(long j, long j2) {
        String strDoFallback = doFallback(j, j2);
        if (strDoFallback == null) {
            return doFormat(doBuild(j, j2));
        }
        return strDoFallback;
    }

    @Override
    public DurationFormatter withLocale(String str) {
        DateFormatter dateFormatterWithLocale;
        if (!str.equals(this.localeName)) {
            PeriodFormatter periodFormatterWithLocale = this.formatter.withLocale(str);
            PeriodBuilder periodBuilderWithLocale = this.builder.withLocale(str);
            if (this.fallback == null) {
                dateFormatterWithLocale = null;
            } else {
                dateFormatterWithLocale = this.fallback.withLocale(str);
            }
            return new BasicDurationFormatter(periodFormatterWithLocale, periodBuilderWithLocale, dateFormatterWithLocale, this.fallbackLimit, str, this.timeZone);
        }
        return this;
    }

    @Override
    public DurationFormatter withTimeZone(TimeZone timeZone) {
        DateFormatter dateFormatterWithTimeZone;
        if (!timeZone.equals(this.timeZone)) {
            PeriodBuilder periodBuilderWithTimeZone = this.builder.withTimeZone(timeZone);
            if (this.fallback == null) {
                dateFormatterWithTimeZone = null;
            } else {
                dateFormatterWithTimeZone = this.fallback.withTimeZone(timeZone);
            }
            return new BasicDurationFormatter(this.formatter, periodBuilderWithTimeZone, dateFormatterWithTimeZone, this.fallbackLimit, this.localeName, timeZone);
        }
        return this;
    }

    protected String doFallback(long j, long j2) {
        if (this.fallback != null && this.fallbackLimit > 0 && Math.abs(j) >= this.fallbackLimit) {
            return this.fallback.format(j2 + j);
        }
        return null;
    }

    protected Period doBuild(long j, long j2) {
        return this.builder.createWithReferenceDate(j, j2);
    }

    protected String doFormat(Period period) {
        if (!period.isSet()) {
            throw new IllegalArgumentException("period is not set");
        }
        return this.formatter.format(period);
    }
}
