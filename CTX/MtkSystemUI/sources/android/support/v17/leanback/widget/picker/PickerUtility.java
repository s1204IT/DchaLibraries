package android.support.v17.leanback.widget.picker;

import android.content.res.Resources;
import android.os.Build;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;

class PickerUtility {
    static final boolean SUPPORTS_BEST_DATE_TIME_PATTERN;

    static {
        SUPPORTS_BEST_DATE_TIME_PATTERN = Build.VERSION.SDK_INT >= 18;
    }

    public static class DateConstant {
        public final String[] days;
        public final Locale locale;
        public final String[] months;

        private DateConstant(Locale locale, Resources resources) {
            this.locale = locale;
            DateFormatSymbols symbols = DateFormatSymbols.getInstance(locale);
            this.months = symbols.getShortMonths();
            Calendar calendar = Calendar.getInstance(locale);
            this.days = PickerUtility.createStringIntArrays(calendar.getMinimum(5), calendar.getMaximum(5), "%02d");
        }
    }

    public static class TimeConstant {
        public final String[] ampm;
        public final String[] hours12;
        public final String[] hours24;
        public final Locale locale;
        public final String[] minutes;

        private TimeConstant(Locale locale, Resources resources) {
            this.locale = locale;
            DateFormatSymbols symbols = DateFormatSymbols.getInstance(locale);
            this.hours12 = PickerUtility.createStringIntArrays(1, 12, "%02d");
            this.hours24 = PickerUtility.createStringIntArrays(0, 23, "%02d");
            this.minutes = PickerUtility.createStringIntArrays(0, 59, "%02d");
            this.ampm = symbols.getAmPmStrings();
        }
    }

    public static DateConstant getDateConstantInstance(Locale locale, Resources resources) {
        return new DateConstant(locale, resources);
    }

    public static TimeConstant getTimeConstantInstance(Locale locale, Resources resources) {
        return new TimeConstant(locale, resources);
    }

    public static String[] createStringIntArrays(int firstNumber, int lastNumber, String format) {
        String[] array = new String[(lastNumber - firstNumber) + 1];
        for (int i = firstNumber; i <= lastNumber; i++) {
            if (format != null) {
                array[i - firstNumber] = String.format(format, Integer.valueOf(i));
            } else {
                array[i - firstNumber] = String.valueOf(i);
            }
        }
        return array;
    }

    public static Calendar getCalendarForLocale(Calendar oldCalendar, Locale locale) {
        if (oldCalendar == null) {
            return Calendar.getInstance(locale);
        }
        long currentTimeMillis = oldCalendar.getTimeInMillis();
        Calendar newCalendar = Calendar.getInstance(locale);
        newCalendar.setTimeInMillis(currentTimeMillis);
        return newCalendar;
    }
}
