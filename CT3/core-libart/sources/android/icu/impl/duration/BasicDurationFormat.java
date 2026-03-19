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

    public static BasicDurationFormat getInstance(ULocale locale) {
        return new BasicDurationFormat(locale);
    }

    @Override
    public StringBuffer format(Object obj, StringBuffer toAppend, FieldPosition pos) {
        if (obj instanceof Long) {
            String res = formatDurationFromNow(obj.longValue());
            return toAppend.append(res);
        }
        if (obj instanceof Date) {
            String res2 = formatDurationFromNowTo(obj);
            return toAppend.append(res2);
        }
        if (obj instanceof Duration) {
            String res3 = formatDuration(obj);
            return toAppend.append(res3);
        }
        throw new IllegalArgumentException("Cannot format given Object as a Duration");
    }

    public BasicDurationFormat() {
        this.pfs = null;
        this.pfs = BasicPeriodFormatterService.getInstance();
        this.formatter = this.pfs.newDurationFormatterFactory().getFormatter();
        this.pformatter = this.pfs.newPeriodFormatterFactory().setDisplayPastFuture(false).getFormatter();
    }

    public BasicDurationFormat(ULocale locale) {
        super(locale);
        this.pfs = null;
        this.pfs = BasicPeriodFormatterService.getInstance();
        this.formatter = this.pfs.newDurationFormatterFactory().setLocale(locale.getName()).getFormatter();
        this.pformatter = this.pfs.newPeriodFormatterFactory().setDisplayPastFuture(false).setLocale(locale.getName()).getFormatter();
    }

    @Override
    public String formatDurationFrom(long duration, long referenceDate) {
        return this.formatter.formatDurationFrom(duration, referenceDate);
    }

    @Override
    public String formatDurationFromNow(long duration) {
        return this.formatter.formatDurationFromNow(duration);
    }

    @Override
    public String formatDurationFromNowTo(Date targetDate) {
        return this.formatter.formatDurationFromNowTo(targetDate);
    }

    public String formatDuration(Object obj) {
        Period p;
        DatatypeConstants.Field[] inFields = {DatatypeConstants.YEARS, DatatypeConstants.MONTHS, DatatypeConstants.DAYS, DatatypeConstants.HOURS, DatatypeConstants.MINUTES, DatatypeConstants.SECONDS};
        TimeUnit[] outFields = {TimeUnit.YEAR, TimeUnit.MONTH, TimeUnit.DAY, TimeUnit.HOUR, TimeUnit.MINUTE, TimeUnit.SECOND};
        Duration inDuration = (Duration) obj;
        Period p2 = null;
        Duration duration = inDuration;
        boolean inPast = false;
        if (inDuration.getSign() < 0) {
            duration = inDuration.negate();
            inPast = true;
        }
        boolean sawNonZero = false;
        for (int i = 0; i < inFields.length; i++) {
            if (duration.isSet(inFields[i])) {
                Number n = duration.getField(inFields[i]);
                if (n.intValue() != 0 || sawNonZero) {
                    sawNonZero = true;
                    float floatVal = n.floatValue();
                    TimeUnit alternateUnit = null;
                    float alternateVal = 0.0f;
                    if (outFields[i] == TimeUnit.SECOND) {
                        double fullSeconds = floatVal;
                        double intSeconds = Math.floor(floatVal);
                        double millis = (fullSeconds - intSeconds) * 1000.0d;
                        if (millis > 0.0d) {
                            alternateUnit = TimeUnit.MILLISECOND;
                            alternateVal = (float) millis;
                            floatVal = (float) intSeconds;
                        }
                    }
                    if (p2 == null) {
                        p2 = Period.at(floatVal, outFields[i]);
                    } else {
                        p2 = p2.and(floatVal, outFields[i]);
                    }
                    if (alternateUnit != null) {
                        p2 = p2.and(alternateVal, alternateUnit);
                    }
                }
            }
        }
        if (p2 == null) {
            return formatDurationFromNow(0L);
        }
        if (inPast) {
            p = p2.inPast();
        } else {
            p = p2.inFuture();
        }
        return this.pformatter.format(p);
    }
}
