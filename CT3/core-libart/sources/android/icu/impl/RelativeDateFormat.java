package android.icu.impl;

import android.icu.lang.UCharacter;
import android.icu.text.BreakIterator;
import android.icu.text.DateFormat;
import android.icu.text.DisplayContext;
import android.icu.text.MessageFormat;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import android.icu.util.UResourceBundleIterator;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Comparator;
import java.util.Date;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.TreeSet;

public class RelativeDateFormat extends DateFormat {
    private static final long serialVersionUID = 1131984966440549435L;
    private MessageFormat fCombinedFormat;
    private DateFormat fDateFormat;
    private String fDatePattern;
    int fDateStyle;
    private SimpleDateFormat fDateTimeFormat;
    ULocale fLocale;
    private DateFormat fTimeFormat;
    private String fTimePattern;
    int fTimeStyle;
    private transient URelativeString[] fDates = null;
    private boolean combinedFormatHasDateAtStart = false;
    private boolean capitalizationInfoIsSet = false;
    private boolean capitalizationOfRelativeUnitsForListOrMenu = false;
    private boolean capitalizationOfRelativeUnitsForStandAlone = false;
    private transient BreakIterator capitalizationBrkIter = null;

    public static class URelativeString {
        public int offset;
        public String string;

        URelativeString(int offset, String string) {
            this.offset = offset;
            this.string = string;
        }

        URelativeString(String offset, String string) {
            this.offset = Integer.parseInt(offset);
            this.string = string;
        }
    }

    public RelativeDateFormat(int timeStyle, int dateStyle, ULocale locale, Calendar cal) {
        this.fDateTimeFormat = null;
        this.fDatePattern = null;
        this.fTimePattern = null;
        this.calendar = cal;
        this.fLocale = locale;
        this.fTimeStyle = timeStyle;
        this.fDateStyle = dateStyle;
        if (this.fDateStyle != -1) {
            int newStyle = this.fDateStyle & (-129);
            ?? dateInstance = DateFormat.getDateInstance(newStyle, locale);
            if (!(dateInstance instanceof SimpleDateFormat)) {
                throw new IllegalArgumentException("Can't create SimpleDateFormat for date style");
            }
            this.fDateTimeFormat = dateInstance;
            this.fDatePattern = this.fDateTimeFormat.toPattern();
            if (this.fTimeStyle != -1) {
                int newStyle2 = this.fTimeStyle & (-129);
                ?? timeInstance = DateFormat.getTimeInstance(newStyle2, locale);
                if (timeInstance instanceof SimpleDateFormat) {
                    this.fTimePattern = timeInstance.toPattern();
                }
            }
        } else {
            int newStyle3 = this.fTimeStyle & (-129);
            ?? timeInstance2 = DateFormat.getTimeInstance(newStyle3, locale);
            if (!(timeInstance2 instanceof SimpleDateFormat)) {
                throw new IllegalArgumentException("Can't create SimpleDateFormat for time style");
            }
            this.fDateTimeFormat = timeInstance2;
            this.fTimePattern = this.fDateTimeFormat.toPattern();
        }
        initializeCalendar(null, this.fLocale);
        loadDates();
        initializeCombinedFormat(this.calendar, this.fLocale);
    }

    @Override
    public StringBuffer format(Calendar cal, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        String relativeDayString = null;
        DisplayContext capitalizationContext = getContext(DisplayContext.Type.CAPITALIZATION);
        if (this.fDateStyle != -1) {
            int dayDiff = dayDifference(cal);
            relativeDayString = getStringForDay(dayDiff);
        }
        if (this.fDateTimeFormat != null) {
            if (relativeDayString != null && this.fDatePattern != null && (this.fTimePattern == null || this.fCombinedFormat == null || this.combinedFormatHasDateAtStart)) {
                if (relativeDayString.length() > 0 && UCharacter.isLowerCase(relativeDayString.codePointAt(0)) && (capitalizationContext == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE || ((capitalizationContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU && this.capitalizationOfRelativeUnitsForListOrMenu) || (capitalizationContext == DisplayContext.CAPITALIZATION_FOR_STANDALONE && this.capitalizationOfRelativeUnitsForStandAlone)))) {
                    if (this.capitalizationBrkIter == null) {
                        this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.fLocale);
                    }
                    relativeDayString = UCharacter.toTitleCase(this.fLocale, relativeDayString, this.capitalizationBrkIter, 768);
                }
                this.fDateTimeFormat.setContext(DisplayContext.CAPITALIZATION_NONE);
            } else {
                this.fDateTimeFormat.setContext(capitalizationContext);
            }
        }
        if (this.fDateTimeFormat != null && (this.fDatePattern != null || this.fTimePattern != null)) {
            if (this.fDatePattern == null) {
                this.fDateTimeFormat.applyPattern(this.fTimePattern);
                this.fDateTimeFormat.format(cal, toAppendTo, fieldPosition);
            } else if (this.fTimePattern == null) {
                if (relativeDayString != null) {
                    toAppendTo.append(relativeDayString);
                } else {
                    this.fDateTimeFormat.applyPattern(this.fDatePattern);
                    this.fDateTimeFormat.format(cal, toAppendTo, fieldPosition);
                }
            } else {
                String datePattern = this.fDatePattern;
                if (relativeDayString != null) {
                    datePattern = "'" + relativeDayString.replace("'", "''") + "'";
                }
                StringBuffer combinedPattern = new StringBuffer("");
                this.fCombinedFormat.format(new Object[]{this.fTimePattern, datePattern}, combinedPattern, new FieldPosition(0));
                this.fDateTimeFormat.applyPattern(combinedPattern.toString());
                this.fDateTimeFormat.format(cal, toAppendTo, fieldPosition);
            }
        } else if (this.fDateFormat != null) {
            if (relativeDayString != null) {
                toAppendTo.append(relativeDayString);
            } else {
                this.fDateFormat.format(cal, toAppendTo, fieldPosition);
            }
        }
        return toAppendTo;
    }

    @Override
    public void parse(String text, Calendar cal, ParsePosition pos) {
        throw new UnsupportedOperationException("Relative Date parse is not implemented yet");
    }

    @Override
    public void setContext(DisplayContext context) {
        super.setContext(context);
        if (!this.capitalizationInfoIsSet && (context == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU || context == DisplayContext.CAPITALIZATION_FOR_STANDALONE)) {
            initCapitalizationContextInfo(this.fLocale);
            this.capitalizationInfoIsSet = true;
        }
        if (this.capitalizationBrkIter != null) {
            return;
        }
        if (context != DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE && ((context != DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU || !this.capitalizationOfRelativeUnitsForListOrMenu) && (context != DisplayContext.CAPITALIZATION_FOR_STANDALONE || !this.capitalizationOfRelativeUnitsForStandAlone))) {
            return;
        }
        this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.fLocale);
    }

    private String getStringForDay(int day) {
        if (this.fDates == null) {
            loadDates();
        }
        for (int i = 0; i < this.fDates.length; i++) {
            if (this.fDates[i].offset == day) {
                return this.fDates[i].string;
            }
        }
        return null;
    }

    private synchronized void loadDates() {
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", this.fLocale);
        ICUResourceBundle rdb = rb.getWithFallback("fields/day/relative");
        Set<URelativeString> datesSet = new TreeSet<>(new Comparator<URelativeString>() {
            @Override
            public int compare(URelativeString r1, URelativeString r2) {
                if (r1.offset == r2.offset) {
                    return 0;
                }
                if (r1.offset < r2.offset) {
                    return -1;
                }
                return 1;
            }
        });
        UResourceBundleIterator i = rdb.getIterator();
        while (i.hasNext()) {
            UResourceBundle line = i.next();
            String k = line.getKey();
            String v = line.getString();
            URelativeString rs = new URelativeString(k, v);
            datesSet.add(rs);
        }
        this.fDates = (URelativeString[]) datesSet.toArray(new URelativeString[0]);
    }

    private void initCapitalizationContextInfo(ULocale locale) {
        ICUResourceBundle rb = (ICUResourceBundle) UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", locale);
        try {
            ICUResourceBundle rdb = rb.getWithFallback("contextTransforms/relative");
            int[] intVector = rdb.getIntVector();
            if (intVector.length < 2) {
                return;
            }
            this.capitalizationOfRelativeUnitsForListOrMenu = intVector[0] != 0;
            this.capitalizationOfRelativeUnitsForStandAlone = intVector[1] != 0;
        } catch (MissingResourceException e) {
        }
    }

    private static int dayDifference(Calendar until) {
        Calendar nowCal = (Calendar) until.clone();
        Date nowDate = new Date(System.currentTimeMillis());
        nowCal.clear();
        nowCal.setTime(nowDate);
        int dayDiff = until.get(20) - nowCal.get(20);
        return dayDiff;
    }

    private Calendar initializeCalendar(TimeZone zone, ULocale locale) {
        if (this.calendar == null) {
            if (zone == null) {
                this.calendar = Calendar.getInstance(locale);
            } else {
                this.calendar = Calendar.getInstance(zone, locale);
            }
        }
        return this.calendar;
    }

    private MessageFormat initializeCombinedFormat(Calendar cal, ULocale locale) {
        String pattern = "{1} {0}";
        try {
            CalendarData calData = new CalendarData(locale, cal.getType());
            String[] patterns = calData.getDateTimePatterns();
            if (patterns != null && patterns.length >= 9) {
                int glueIndex = 8;
                if (patterns.length >= 13) {
                    switch (this.fDateStyle) {
                        case 0:
                        case 128:
                            glueIndex = 9;
                            break;
                        case 1:
                        case 129:
                            glueIndex = 10;
                            break;
                        case 2:
                        case 130:
                            glueIndex = 11;
                            break;
                        case 3:
                        case 131:
                            glueIndex = 12;
                            break;
                    }
                }
                pattern = patterns[glueIndex];
            }
        } catch (MissingResourceException e) {
        }
        this.combinedFormatHasDateAtStart = pattern.startsWith("{1}");
        this.fCombinedFormat = new MessageFormat(pattern, locale);
        return this.fCombinedFormat;
    }
}
