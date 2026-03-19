package android.icu.text;

import android.icu.impl.ICUResourceBundle;
import android.icu.util.Calendar;
import android.icu.util.ChineseCalendar;
import android.icu.util.ULocale;
import java.util.Locale;

@Deprecated
public class ChineseDateFormatSymbols extends DateFormatSymbols {
    static final long serialVersionUID = 6827816119783952890L;
    String[] isLeapMonth;

    @Deprecated
    public ChineseDateFormatSymbols() {
        this(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    @Deprecated
    public ChineseDateFormatSymbols(Locale locale) {
        super((Class<? extends Calendar>) ChineseCalendar.class, ULocale.forLocale(locale));
    }

    @Deprecated
    public ChineseDateFormatSymbols(ULocale uLocale) {
        super((Class<? extends Calendar>) ChineseCalendar.class, uLocale);
    }

    @Deprecated
    public ChineseDateFormatSymbols(Calendar calendar, Locale locale) {
        super((Class<? extends Calendar>) calendar.getClass(), locale);
    }

    @Deprecated
    public ChineseDateFormatSymbols(Calendar calendar, ULocale uLocale) {
        super((Class<? extends Calendar>) calendar.getClass(), uLocale);
    }

    @Deprecated
    public String getLeapMonth(int i) {
        return this.isLeapMonth[i];
    }

    @Override
    @Deprecated
    protected void initializeData(ULocale uLocale, ICUResourceBundle iCUResourceBundle, String str) {
        super.initializeData(uLocale, iCUResourceBundle, str);
        initializeIsLeapMonth();
    }

    @Override
    void initializeData(DateFormatSymbols dateFormatSymbols) {
        super.initializeData(dateFormatSymbols);
        if (dateFormatSymbols instanceof ChineseDateFormatSymbols) {
            this.isLeapMonth = ((ChineseDateFormatSymbols) dateFormatSymbols).isLeapMonth;
        } else {
            initializeIsLeapMonth();
        }
    }

    private void initializeIsLeapMonth() {
        this.isLeapMonth = new String[2];
        this.isLeapMonth[0] = "";
        this.isLeapMonth[1] = this.leapMonthPatterns != null ? this.leapMonthPatterns[0].replace("{0}", "") : "";
    }
}
