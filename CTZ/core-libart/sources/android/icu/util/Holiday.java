package android.icu.util;

import android.icu.util.ULocale;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;

public abstract class Holiday implements DateRule {
    private static Holiday[] noHolidays = new Holiday[0];
    private String name;
    private DateRule rule;

    public static Holiday[] getHolidays() {
        return getHolidays(ULocale.getDefault(ULocale.Category.FORMAT));
    }

    public static Holiday[] getHolidays(Locale locale) {
        return getHolidays(ULocale.forLocale(locale));
    }

    public static Holiday[] getHolidays(ULocale uLocale) {
        try {
            return (Holiday[]) UResourceBundle.getBundleInstance("android.icu.impl.data.HolidayBundle", uLocale).getObject("holidays");
        } catch (MissingResourceException e) {
            return noHolidays;
        }
    }

    @Override
    public Date firstAfter(Date date) {
        return this.rule.firstAfter(date);
    }

    @Override
    public Date firstBetween(Date date, Date date2) {
        return this.rule.firstBetween(date, date2);
    }

    @Override
    public boolean isOn(Date date) {
        return this.rule.isOn(date);
    }

    @Override
    public boolean isBetween(Date date, Date date2) {
        return this.rule.isBetween(date, date2);
    }

    protected Holiday(String str, DateRule dateRule) {
        this.name = str;
        this.rule = dateRule;
    }

    public String getDisplayName() {
        return getDisplayName(ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String getDisplayName(Locale locale) {
        return getDisplayName(ULocale.forLocale(locale));
    }

    public String getDisplayName(ULocale uLocale) {
        try {
            return UResourceBundle.getBundleInstance("android.icu.impl.data.HolidayBundle", uLocale).getString(this.name);
        } catch (MissingResourceException e) {
            return this.name;
        }
    }

    public DateRule getRule() {
        return this.rule;
    }

    public void setRule(DateRule dateRule) {
        this.rule = dateRule;
    }
}
