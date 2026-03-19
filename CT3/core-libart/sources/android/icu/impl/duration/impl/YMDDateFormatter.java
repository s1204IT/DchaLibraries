package android.icu.impl.duration.impl;

import android.icu.impl.duration.DateFormatter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class YMDDateFormatter implements DateFormatter {
    private SimpleDateFormat df;
    private String localeName;
    private String requestedFields;
    private TimeZone timeZone;

    public YMDDateFormatter(String requestedFields) {
        this(requestedFields, Locale.getDefault().toString(), TimeZone.getDefault());
    }

    public YMDDateFormatter(String requestedFields, String localeName, TimeZone timeZone) {
        this.requestedFields = requestedFields;
        this.localeName = localeName;
        this.timeZone = timeZone;
        Locale locale = Utils.localeFromString(localeName);
        this.df = new SimpleDateFormat("yyyy/mm/dd", locale);
        this.df.setTimeZone(timeZone);
    }

    @Override
    public String format(long date) {
        return format(new Date(date));
    }

    @Override
    public String format(Date date) {
        return this.df.format(date);
    }

    @Override
    public DateFormatter withLocale(String locName) {
        if (!locName.equals(this.localeName)) {
            return new YMDDateFormatter(this.requestedFields, locName, this.timeZone);
        }
        return this;
    }

    @Override
    public DateFormatter withTimeZone(TimeZone tz) {
        if (!tz.equals(this.timeZone)) {
            return new YMDDateFormatter(this.requestedFields, this.localeName, tz);
        }
        return this;
    }
}
