package libcore.icu;

import android.icu.text.DateFormat;
import android.icu.text.DateTimePatternGenerator;
import android.icu.text.DisplayContext;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.ULocale;
import libcore.util.BasicLruCache;

public class DateTimeFormat {
    private static final FormatterCache CACHED_FORMATTERS = new FormatterCache();

    static class FormatterCache extends BasicLruCache<String, DateFormat> {
        FormatterCache() {
            super(8);
        }
    }

    private DateTimeFormat() {
    }

    public static String format(ULocale uLocale, Calendar calendar, int i, DisplayContext displayContext) {
        String str;
        String skeleton = DateUtilsBridge.toSkeleton(calendar, i);
        String str2 = skeleton + "\t" + uLocale + "\t" + calendar.getTimeZone();
        synchronized (CACHED_FORMATTERS) {
            DateFormat dateFormat = CACHED_FORMATTERS.get(str2);
            if (dateFormat == null) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DateTimePatternGenerator.getInstance(uLocale).getBestPattern(skeleton), uLocale);
                CACHED_FORMATTERS.put(str2, simpleDateFormat);
                dateFormat = simpleDateFormat;
            }
            dateFormat.setContext(displayContext);
            str = dateFormat.format(calendar);
        }
        return str;
    }
}
