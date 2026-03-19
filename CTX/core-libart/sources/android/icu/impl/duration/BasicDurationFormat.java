package android.icu.impl.duration;

import android.icu.text.DurationFormat;
import android.icu.util.ULocale;
import java.text.FieldPosition;
import java.util.Date;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;

public class BasicDurationFormat extends DurationFormat {
    private static final long serialVersionUID = -3146984141909457700L;
    transient DurationFormatter formatter;
    transient PeriodFormatter pformatter;
    transient PeriodFormatterService pfs;

    public static BasicDurationFormat getInstance(ULocale uLocale) {
        return new BasicDurationFormat(uLocale);
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        if (obj instanceof Long) {
            stringBuffer.append(formatDurationFromNow(((Long) obj).longValue()));
            return stringBuffer;
        }
        if (obj instanceof Date) {
            stringBuffer.append(formatDurationFromNowTo((Date) obj));
            return stringBuffer;
        }
        if (obj instanceof Duration) {
            stringBuffer.append(formatDuration(obj));
            return stringBuffer;
        }
        throw new IllegalArgumentException("Cannot format given Object as a Duration");
    }

    public BasicDurationFormat() {
        this.pfs = null;
        this.pfs = BasicPeriodFormatterService.getInstance();
        this.formatter = this.pfs.newDurationFormatterFactory().getFormatter();
        this.pformatter = this.pfs.newPeriodFormatterFactory().setDisplayPastFuture(false).getFormatter();
    }

    public BasicDurationFormat(ULocale uLocale) {
        super(uLocale);
        this.pfs = null;
        this.pfs = BasicPeriodFormatterService.getInstance();
        this.formatter = this.pfs.newDurationFormatterFactory().setLocale(uLocale.getName()).getFormatter();
        this.pformatter = this.pfs.newPeriodFormatterFactory().setDisplayPastFuture(false).setLocale(uLocale.getName()).getFormatter();
    }

    @Override
    public String formatDurationFrom(long j, long j2) {
        return this.formatter.formatDurationFrom(j, j2);
    }

    @Override
    public String formatDurationFromNow(long j) {
        return this.formatter.formatDurationFromNow(j);
    }

    @Override
    public String formatDurationFromNowTo(Date date) {
        return this.formatter.formatDurationFromNowTo(date);
    }

    public String formatDuration(Object obj) {
        boolean z;
        Period periodInFuture;
        float f;
        TimeUnit timeUnit;
        DatatypeConstants.Field[] fieldArr = {DatatypeConstants.YEARS, DatatypeConstants.MONTHS, DatatypeConstants.DAYS, DatatypeConstants.HOURS, DatatypeConstants.MINUTES, DatatypeConstants.SECONDS};
        TimeUnit[] timeUnitArr = {TimeUnit.YEAR, TimeUnit.MONTH, TimeUnit.DAY, TimeUnit.HOUR, TimeUnit.MINUTE, TimeUnit.SECOND};
        Duration durationNegate = (Duration) obj;
        if (durationNegate.getSign() < 0) {
            durationNegate = durationNegate.negate();
            z = true;
        } else {
            z = false;
        }
        boolean z2 = false;
        Period periodAnd = null;
        for (int i = 0; i < fieldArr.length; i++) {
            if (durationNegate.isSet(fieldArr[i])) {
                Number field = durationNegate.getField(fieldArr[i]);
                if (field.intValue() != 0 || z2) {
                    float fFloatValue = field.floatValue();
                    if (timeUnitArr[i] == TimeUnit.SECOND) {
                        double d = fFloatValue;
                        double dFloor = Math.floor(d);
                        double d2 = (d - dFloor) * 1000.0d;
                        if (d2 <= 0.0d) {
                            f = 0.0f;
                            timeUnit = null;
                        } else {
                            timeUnit = TimeUnit.MILLISECOND;
                            fFloatValue = (float) dFloor;
                            f = (float) d2;
                        }
                        if (periodAnd == null) {
                            periodAnd = Period.at(fFloatValue, timeUnitArr[i]);
                        } else {
                            periodAnd = periodAnd.and(fFloatValue, timeUnitArr[i]);
                        }
                        if (timeUnit != null) {
                            periodAnd = periodAnd.and(f, timeUnit);
                        }
                        z2 = true;
                    }
                }
            }
        }
        if (periodAnd == null) {
            return formatDurationFromNow(0L);
        }
        if (z) {
            periodInFuture = periodAnd.inPast();
        } else {
            periodInFuture = periodAnd.inFuture();
        }
        return this.pformatter.format(periodInFuture);
    }
}
