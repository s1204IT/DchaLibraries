package android.icu.impl;

import android.icu.impl.UResource;
import android.icu.impl.coll.CollationSettings;
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
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.MissingResourceException;

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
    private transient List<URelativeString> fDates = null;
    private boolean combinedFormatHasDateAtStart = false;
    private boolean capitalizationInfoIsSet = false;
    private boolean capitalizationOfRelativeUnitsForListOrMenu = false;
    private boolean capitalizationOfRelativeUnitsForStandAlone = false;
    private transient BreakIterator capitalizationBrkIter = null;

    public static class URelativeString {
        public int offset;
        public String string;

        URelativeString(int i, String str) {
            this.offset = i;
            this.string = str;
        }

        URelativeString(String str, String str2) {
            this.offset = Integer.parseInt(str);
            this.string = str2;
        }
    }

    public RelativeDateFormat(int i, int i2, ULocale uLocale, Calendar calendar) {
        this.fDateTimeFormat = null;
        this.fDatePattern = null;
        this.fTimePattern = null;
        this.calendar = calendar;
        this.fLocale = uLocale;
        this.fTimeStyle = i;
        this.fDateStyle = i2;
        if (this.fDateStyle != -1) {
            DateFormat dateInstance = DateFormat.getDateInstance(this.fDateStyle & (-129), uLocale);
            if (dateInstance instanceof SimpleDateFormat) {
                this.fDateTimeFormat = (SimpleDateFormat) dateInstance;
                this.fDatePattern = this.fDateTimeFormat.toPattern();
                if (this.fTimeStyle != -1) {
                    DateFormat timeInstance = DateFormat.getTimeInstance(this.fTimeStyle & (-129), uLocale);
                    if (timeInstance instanceof SimpleDateFormat) {
                        this.fTimePattern = ((SimpleDateFormat) timeInstance).toPattern();
                    }
                }
            } else {
                throw new IllegalArgumentException("Can't create SimpleDateFormat for date style");
            }
        } else {
            DateFormat timeInstance2 = DateFormat.getTimeInstance(this.fTimeStyle & (-129), uLocale);
            if (timeInstance2 instanceof SimpleDateFormat) {
                this.fDateTimeFormat = (SimpleDateFormat) timeInstance2;
                this.fTimePattern = this.fDateTimeFormat.toPattern();
            } else {
                throw new IllegalArgumentException("Can't create SimpleDateFormat for time style");
            }
        }
        initializeCalendar(null, this.fLocale);
        loadDates();
        initializeCombinedFormat(this.calendar, this.fLocale);
    }

    @Override
    public StringBuffer format(Calendar calendar, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        String titleCase;
        DisplayContext context = getContext(DisplayContext.Type.CAPITALIZATION);
        if (this.fDateStyle != -1) {
            titleCase = getStringForDay(dayDifference(calendar));
        } else {
            titleCase = null;
        }
        if (this.fDateTimeFormat != null) {
            if (titleCase != null && this.fDatePattern != null && (this.fTimePattern == null || this.fCombinedFormat == null || this.combinedFormatHasDateAtStart)) {
                if (titleCase.length() > 0 && UCharacter.isLowerCase(titleCase.codePointAt(0)) && (context == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE || ((context == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU && this.capitalizationOfRelativeUnitsForListOrMenu) || (context == DisplayContext.CAPITALIZATION_FOR_STANDALONE && this.capitalizationOfRelativeUnitsForStandAlone)))) {
                    if (this.capitalizationBrkIter == null) {
                        this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.fLocale);
                    }
                    titleCase = UCharacter.toTitleCase(this.fLocale, titleCase, this.capitalizationBrkIter, CollationSettings.CASE_FIRST_AND_UPPER_MASK);
                }
                this.fDateTimeFormat.setContext(DisplayContext.CAPITALIZATION_NONE);
            } else {
                this.fDateTimeFormat.setContext(context);
            }
        }
        if (this.fDateTimeFormat != null && (this.fDatePattern != null || this.fTimePattern != null)) {
            if (this.fDatePattern == null) {
                this.fDateTimeFormat.applyPattern(this.fTimePattern);
                this.fDateTimeFormat.format(calendar, stringBuffer, fieldPosition);
            } else if (this.fTimePattern == null) {
                if (titleCase != null) {
                    stringBuffer.append(titleCase);
                } else {
                    this.fDateTimeFormat.applyPattern(this.fDatePattern);
                    this.fDateTimeFormat.format(calendar, stringBuffer, fieldPosition);
                }
            } else {
                String str = this.fDatePattern;
                if (titleCase != null) {
                    str = "'" + titleCase.replace("'", "''") + "'";
                }
                StringBuffer stringBuffer2 = new StringBuffer("");
                this.fCombinedFormat.format(new Object[]{this.fTimePattern, str}, stringBuffer2, new FieldPosition(0));
                this.fDateTimeFormat.applyPattern(stringBuffer2.toString());
                this.fDateTimeFormat.format(calendar, stringBuffer, fieldPosition);
            }
        } else if (this.fDateFormat != null) {
            if (titleCase != null) {
                stringBuffer.append(titleCase);
            } else {
                this.fDateFormat.format(calendar, stringBuffer, fieldPosition);
            }
        }
        return stringBuffer;
    }

    @Override
    public void parse(String str, Calendar calendar, ParsePosition parsePosition) {
        throw new UnsupportedOperationException("Relative Date parse is not implemented yet");
    }

    @Override
    public void setContext(DisplayContext displayContext) {
        super.setContext(displayContext);
        if (!this.capitalizationInfoIsSet && (displayContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU || displayContext == DisplayContext.CAPITALIZATION_FOR_STANDALONE)) {
            initCapitalizationContextInfo(this.fLocale);
            this.capitalizationInfoIsSet = true;
        }
        if (this.capitalizationBrkIter == null) {
            if (displayContext == DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE || ((displayContext == DisplayContext.CAPITALIZATION_FOR_UI_LIST_OR_MENU && this.capitalizationOfRelativeUnitsForListOrMenu) || (displayContext == DisplayContext.CAPITALIZATION_FOR_STANDALONE && this.capitalizationOfRelativeUnitsForStandAlone))) {
                this.capitalizationBrkIter = BreakIterator.getSentenceInstance(this.fLocale);
            }
        }
    }

    private String getStringForDay(int i) {
        if (this.fDates == null) {
            loadDates();
        }
        for (URelativeString uRelativeString : this.fDates) {
            if (uRelativeString.offset == i) {
                return uRelativeString.string;
            }
        }
        return null;
    }

    private final class RelDateFmtDataSink extends UResource.Sink {
        private RelDateFmtDataSink() {
        }

        @Override
        public void put(UResource.Key key, UResource.Value value, boolean z) {
            if (value.getType() == 3) {
                return;
            }
            UResource.Table table = value.getTable();
            for (int i = 0; table.getKeyAndValue(i, key, value); i++) {
                try {
                    int i2 = Integer.parseInt(key.toString());
                    if (RelativeDateFormat.this.getStringForDay(i2) == null) {
                        RelativeDateFormat.this.fDates.add(new URelativeString(i2, value.getString()));
                    }
                } catch (NumberFormatException e) {
                    return;
                }
            }
        }
    }

    private synchronized void loadDates() {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, this.fLocale);
        this.fDates = new ArrayList();
        iCUResourceBundle.getAllItemsWithFallback("fields/day/relative", new RelDateFmtDataSink());
    }

    private void initCapitalizationContextInfo(ULocale uLocale) {
        try {
            int[] intVector = ((ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale)).getWithFallback("contextTransforms/relative").getIntVector();
            if (intVector.length >= 2) {
                this.capitalizationOfRelativeUnitsForListOrMenu = intVector[0] != 0;
                this.capitalizationOfRelativeUnitsForStandAlone = intVector[1] != 0;
            }
        } catch (MissingResourceException e) {
        }
    }

    private static int dayDifference(Calendar calendar) {
        Calendar calendar2 = (Calendar) calendar.clone();
        Date date = new Date(System.currentTimeMillis());
        calendar2.clear();
        calendar2.setTime(date);
        return calendar.get(20) - calendar2.get(20);
    }

    private Calendar initializeCalendar(TimeZone timeZone, ULocale uLocale) {
        if (this.calendar == null) {
            if (timeZone == null) {
                this.calendar = Calendar.getInstance(uLocale);
            } else {
                this.calendar = Calendar.getInstance(timeZone, uLocale);
            }
        }
        return this.calendar;
    }

    private MessageFormat initializeCombinedFormat(Calendar calendar, ULocale uLocale) {
        String string;
        int i;
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) UResourceBundle.getBundleInstance(ICUData.ICU_BASE_NAME, uLocale);
        ICUResourceBundle iCUResourceBundleFindWithFallback = iCUResourceBundle.findWithFallback("calendar/" + calendar.getType() + "/DateTimePatterns");
        if (iCUResourceBundleFindWithFallback == null && !calendar.getType().equals("gregorian")) {
            iCUResourceBundleFindWithFallback = iCUResourceBundle.findWithFallback("calendar/gregorian/DateTimePatterns");
        }
        if (iCUResourceBundleFindWithFallback == null || iCUResourceBundleFindWithFallback.getSize() < 9) {
            string = "{1} {0}";
        } else if (iCUResourceBundleFindWithFallback.getSize() >= 13) {
            if (this.fDateStyle >= 0 && this.fDateStyle <= 3) {
                i = this.fDateStyle + 1 + 8;
            } else if (this.fDateStyle >= 128 && this.fDateStyle <= 131) {
                i = ((this.fDateStyle + 1) - 128) + 8;
            }
            if (iCUResourceBundleFindWithFallback.get(i).getType() != 8) {
            }
        } else {
            i = 8;
            if (iCUResourceBundleFindWithFallback.get(i).getType() != 8) {
                string = iCUResourceBundleFindWithFallback.get(i).getString(0);
            } else {
                string = iCUResourceBundleFindWithFallback.getString(i);
            }
        }
        this.combinedFormatHasDateAtStart = string.startsWith("{1}");
        this.fCombinedFormat = new MessageFormat(string, uLocale);
        return this.fCombinedFormat;
    }
}
