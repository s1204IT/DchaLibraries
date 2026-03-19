package android.icu.util;

import android.icu.util.ULocale;
import java.util.Date;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

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

    public static Holiday[] getHolidays(ULocale locale) {
        Holiday[] result = noHolidays;
        try {
            ResourceBundle bundle = UResourceBundle.getBundleInstance("android.icu.impl.data.HolidayBundle", locale);
            Holiday[] result2 = (Holiday[]) bundle.getObject("holidays");
            return result2;
        } catch (MissingResourceException e) {
            return result;
        }
    }

    @Override
    public Date firstAfter(Date start) {
        return this.rule.firstAfter(start);
    }

    @Override
    public Date firstBetween(Date start, Date end) {
        return this.rule.firstBetween(start, end);
    }

    @Override
    public boolean isOn(Date date) {
        return this.rule.isOn(date);
    }

    @Override
    public boolean isBetween(Date start, Date end) {
        return this.rule.isBetween(start, end);
    }

    protected Holiday(String name, DateRule rule) {
        this.name = name;
        this.rule = rule;
    }

    public String getDisplayName() {
        return getDisplayName(ULocale.getDefault(ULocale.Category.DISPLAY));
    }

    public String getDisplayName(Locale locale) {
        return getDisplayName(ULocale.forLocale(locale));
    }

    public String getDisplayName(ULocale locale) {
        String dispName = this.name;
        try {
            ResourceBundle bundle = UResourceBundle.getBundleInstance("android.icu.impl.data.HolidayBundle", locale);
            String dispName2 = bundle.getString(this.name);
            return dispName2;
        } catch (MissingResourceException e) {
            return dispName;
        }
    }

    public DateRule getRule() {
        return this.rule;
    }

    public void setRule(DateRule rule) {
        this.rule = rule;
    }
}
