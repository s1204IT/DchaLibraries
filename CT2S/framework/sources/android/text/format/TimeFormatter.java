package android.text.format;

import android.content.res.Resources;
import com.android.internal.R;
import java.nio.CharBuffer;
import java.util.Locale;
import libcore.icu.LocaleData;
import libcore.util.ZoneInfo;

class TimeFormatter {
    private static final int DAYSPERLYEAR = 366;
    private static final int DAYSPERNYEAR = 365;
    private static final int DAYSPERWEEK = 7;
    private static final int FORCE_LOWER_CASE = -1;
    private static final int HOURSPERDAY = 24;
    private static final int MINSPERHOUR = 60;
    private static final int MONSPERYEAR = 12;
    private static final int SECSPERMIN = 60;
    private static String sDateOnlyFormat;
    private static String sDateTimeFormat;
    private static Locale sLocale;
    private static LocaleData sLocaleData;
    private static String sTimeOnlyFormat;
    private final String dateOnlyFormat;
    private final String dateTimeFormat;
    private final LocaleData localeData;
    private java.util.Formatter numberFormatter;
    private StringBuilder outputBuilder;
    private final String timeOnlyFormat;

    public TimeFormatter() {
        synchronized (TimeFormatter.class) {
            Locale locale = Locale.getDefault();
            if (sLocale == null || !locale.equals(sLocale)) {
                sLocale = locale;
                sLocaleData = LocaleData.get(locale);
                Resources r = Resources.getSystem();
                sTimeOnlyFormat = r.getString(R.string.time_of_day);
                sDateOnlyFormat = r.getString(R.string.month_day_year);
                sDateTimeFormat = r.getString(R.string.date_and_time);
            }
            this.dateTimeFormat = sDateTimeFormat;
            this.timeOnlyFormat = sTimeOnlyFormat;
            this.dateOnlyFormat = sDateOnlyFormat;
            this.localeData = sLocaleData;
        }
    }

    public String format(String str, ZoneInfo.WallTime wallTime, ZoneInfo zoneInfo) {
        try {
            StringBuilder sb = new StringBuilder();
            this.outputBuilder = sb;
            this.numberFormatter = new java.util.Formatter(sb, Locale.US);
            formatInternal(str, wallTime, zoneInfo);
            String string = sb.toString();
            if (this.localeData.zeroDigit != '0') {
                string = localizeDigits(string);
            }
            return string;
        } finally {
            this.outputBuilder = null;
            this.numberFormatter = null;
        }
    }

    private String localizeDigits(String s) {
        int length = s.length();
        int offsetToLocalizedDigits = this.localeData.zeroDigit - '0';
        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char ch = s.charAt(i);
            if (ch >= '0' && ch <= '9') {
                ch = (char) (ch + offsetToLocalizedDigits);
            }
            result.append(ch);
        }
        return result.toString();
    }

    private void formatInternal(String pattern, ZoneInfo.WallTime wallTime, ZoneInfo zoneInfo) {
        CharBuffer formatBuffer = CharBuffer.wrap(pattern);
        while (formatBuffer.remaining() > 0) {
            boolean outputCurrentChar = true;
            char currentChar = formatBuffer.get(formatBuffer.position());
            if (currentChar == '%') {
                outputCurrentChar = handleToken(formatBuffer, wallTime, zoneInfo);
            }
            if (outputCurrentChar) {
                this.outputBuilder.append(formatBuffer.get(formatBuffer.position()));
            }
            formatBuffer.position(formatBuffer.position() + 1);
        }
    }

    private boolean handleToken(CharBuffer formatBuffer, ZoneInfo.WallTime wallTime, ZoneInfo zoneInfo) {
        char sign;
        int w;
        int modifier = 0;
        while (formatBuffer.remaining() > 1) {
            formatBuffer.position(formatBuffer.position() + 1);
            int currentChar = formatBuffer.get(formatBuffer.position());
            switch (currentChar) {
                case 35:
                case 45:
                case 48:
                case 94:
                case 95:
                    modifier = currentChar;
                    break;
                case 36:
                case 37:
                case 38:
                case 39:
                case 40:
                case 41:
                case 42:
                case 44:
                case 46:
                case 47:
                case 49:
                case 50:
                case 51:
                case 52:
                case 53:
                case 54:
                case 55:
                case 56:
                case 57:
                case 58:
                case 59:
                case 60:
                case 61:
                case 62:
                case 63:
                case 64:
                case 74:
                case 75:
                case 76:
                case 78:
                case 81:
                case 91:
                case 92:
                case 93:
                case 96:
                case 102:
                case 105:
                case 111:
                case 113:
                default:
                    return true;
                case 43:
                    formatInternal("%a %b %e %H:%M:%S %Z %Y", wallTime, zoneInfo);
                    return false;
                case 65:
                    modifyAndAppend((wallTime.getWeekDay() < 0 || wallTime.getWeekDay() >= 7) ? "?" : this.localeData.longWeekdayNames[wallTime.getWeekDay() + 1], modifier);
                    return false;
                case 66:
                    if (modifier == 45) {
                        modifyAndAppend((wallTime.getMonth() < 0 || wallTime.getMonth() >= 12) ? "?" : this.localeData.longStandAloneMonthNames[wallTime.getMonth()], modifier);
                    } else {
                        modifyAndAppend((wallTime.getMonth() < 0 || wallTime.getMonth() >= 12) ? "?" : this.localeData.longMonthNames[wallTime.getMonth()], modifier);
                    }
                    return false;
                case 67:
                    outputYear(wallTime.getYear(), true, false, modifier);
                    return false;
                case 68:
                    formatInternal("%m/%d/%y", wallTime, zoneInfo);
                    return false;
                case 69:
                case 79:
                    break;
                case 70:
                    formatInternal("%Y-%m-%d", wallTime, zoneInfo);
                    return false;
                case 71:
                case 86:
                case 103:
                    int year = wallTime.getYear();
                    int yday = wallTime.getYearDay();
                    int wday = wallTime.getWeekDay();
                    while (true) {
                        int len = isLeap(year) ? DAYSPERLYEAR : DAYSPERNYEAR;
                        int bot = (((yday + 11) - wday) % 7) - 3;
                        int top = bot - (len % 7);
                        if (top < -3) {
                            top += 7;
                        }
                        if (yday >= top + len) {
                            year++;
                            w = 1;
                        } else if (yday >= bot) {
                            w = ((yday - bot) / 7) + 1;
                        } else {
                            year--;
                            yday += isLeap(year) ? DAYSPERLYEAR : DAYSPERNYEAR;
                        }
                    }
                    if (currentChar == 86) {
                        this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(w));
                    } else if (currentChar == 103) {
                        outputYear(year, false, true, modifier);
                    } else {
                        outputYear(year, true, true, modifier);
                    }
                    return false;
                case 72:
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(wallTime.getHour()));
                    return false;
                case 73:
                    int hour = wallTime.getHour() % 12 != 0 ? wallTime.getHour() % 12 : 12;
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(hour));
                    return false;
                case 77:
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(wallTime.getMinute()));
                    return false;
                case 80:
                    modifyAndAppend(wallTime.getHour() >= 12 ? this.localeData.amPm[1] : this.localeData.amPm[0], -1);
                    return false;
                case 82:
                    formatInternal(DateUtils.HOUR_MINUTE_24, wallTime, zoneInfo);
                    return false;
                case 83:
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(wallTime.getSecond()));
                    return false;
                case 84:
                    formatInternal("%H:%M:%S", wallTime, zoneInfo);
                    return false;
                case 85:
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(((wallTime.getYearDay() + 7) - wallTime.getWeekDay()) / 7));
                    return false;
                case 87:
                    int n = ((wallTime.getYearDay() + 7) - (wallTime.getWeekDay() != 0 ? wallTime.getWeekDay() - 1 : 6)) / 7;
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(n));
                    return false;
                case 88:
                    formatInternal(this.timeOnlyFormat, wallTime, zoneInfo);
                    return false;
                case 89:
                    outputYear(wallTime.getYear(), true, true, modifier);
                    return false;
                case 90:
                    if (wallTime.getIsDst() < 0) {
                        return false;
                    }
                    boolean isDst = wallTime.getIsDst() != 0;
                    modifyAndAppend(zoneInfo.getDisplayName(isDst, 0), modifier);
                    return false;
                case 97:
                    modifyAndAppend((wallTime.getWeekDay() < 0 || wallTime.getWeekDay() >= 7) ? "?" : this.localeData.shortWeekdayNames[wallTime.getWeekDay() + 1], modifier);
                    return false;
                case 98:
                case 104:
                    modifyAndAppend((wallTime.getMonth() < 0 || wallTime.getMonth() >= 12) ? "?" : this.localeData.shortMonthNames[wallTime.getMonth()], modifier);
                    return false;
                case 99:
                    formatInternal(this.dateTimeFormat, wallTime, zoneInfo);
                    return false;
                case 100:
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(wallTime.getMonthDay()));
                    return false;
                case 101:
                    this.numberFormatter.format(getFormat(modifier, "%2d", "%2d", "%d", "%02d"), Integer.valueOf(wallTime.getMonthDay()));
                    return false;
                case 106:
                    int yearDay = wallTime.getYearDay() + 1;
                    this.numberFormatter.format(getFormat(modifier, "%03d", "%3d", "%d", "%03d"), Integer.valueOf(yearDay));
                    return false;
                case 107:
                    this.numberFormatter.format(getFormat(modifier, "%2d", "%2d", "%d", "%02d"), Integer.valueOf(wallTime.getHour()));
                    return false;
                case 108:
                    int n2 = wallTime.getHour() % 12 != 0 ? wallTime.getHour() % 12 : 12;
                    this.numberFormatter.format(getFormat(modifier, "%2d", "%2d", "%d", "%02d"), Integer.valueOf(n2));
                    return false;
                case 109:
                    this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(wallTime.getMonth() + 1));
                    return false;
                case 110:
                    this.outputBuilder.append('\n');
                    return false;
                case 112:
                    modifyAndAppend(wallTime.getHour() >= 12 ? this.localeData.amPm[1] : this.localeData.amPm[0], modifier);
                    return false;
                case 114:
                    formatInternal("%I:%M:%S %p", wallTime, zoneInfo);
                    return false;
                case 115:
                    int timeInSeconds = wallTime.mktime(zoneInfo);
                    this.outputBuilder.append(Integer.toString(timeInSeconds));
                    return false;
                case 116:
                    this.outputBuilder.append('\t');
                    return false;
                case 117:
                    int day = wallTime.getWeekDay() == 0 ? 7 : wallTime.getWeekDay();
                    this.numberFormatter.format("%d", Integer.valueOf(day));
                    return false;
                case 118:
                    formatInternal("%e-%b-%Y", wallTime, zoneInfo);
                    return false;
                case 119:
                    this.numberFormatter.format("%d", Integer.valueOf(wallTime.getWeekDay()));
                    return false;
                case 120:
                    formatInternal(this.dateOnlyFormat, wallTime, zoneInfo);
                    return false;
                case 121:
                    outputYear(wallTime.getYear(), false, true, modifier);
                    return false;
                case 122:
                    if (wallTime.getIsDst() < 0) {
                        return false;
                    }
                    int diff = wallTime.getGmtOffset();
                    if (diff < 0) {
                        sign = '-';
                        diff = -diff;
                    } else {
                        sign = '+';
                    }
                    this.outputBuilder.append(sign);
                    int diff2 = diff / 60;
                    this.numberFormatter.format(getFormat(modifier, "%04d", "%4d", "%d", "%04d"), Integer.valueOf(((diff2 / 60) * 100) + (diff2 % 60)));
                    return false;
            }
        }
        return true;
    }

    private void modifyAndAppend(CharSequence str, int modifier) {
        switch (modifier) {
            case -1:
                for (int i = 0; i < str.length(); i++) {
                    this.outputBuilder.append(brokenToLower(str.charAt(i)));
                }
                break;
            case 35:
                for (int i2 = 0; i2 < str.length(); i2++) {
                    char c = str.charAt(i2);
                    if (brokenIsUpper(c)) {
                        c = brokenToLower(c);
                    } else if (brokenIsLower(c)) {
                        c = brokenToUpper(c);
                    }
                    this.outputBuilder.append(c);
                }
                break;
            case 94:
                for (int i3 = 0; i3 < str.length(); i3++) {
                    this.outputBuilder.append(brokenToUpper(str.charAt(i3)));
                }
                break;
            default:
                this.outputBuilder.append(str);
                break;
        }
    }

    private void outputYear(int value, boolean outputTop, boolean outputBottom, int modifier) {
        int trail = value % 100;
        int lead = (value / 100) + (trail / 100);
        int trail2 = trail % 100;
        if (trail2 < 0 && lead > 0) {
            trail2 += 100;
            lead--;
        } else if (lead < 0 && trail2 > 0) {
            trail2 -= 100;
            lead++;
        }
        if (outputTop) {
            if (lead == 0 && trail2 < 0) {
                this.outputBuilder.append("-0");
            } else {
                this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(lead));
            }
        }
        if (outputBottom) {
            int n = trail2 < 0 ? -trail2 : trail2;
            this.numberFormatter.format(getFormat(modifier, "%02d", "%2d", "%d", "%02d"), Integer.valueOf(n));
        }
    }

    private static String getFormat(int modifier, String normal, String underscore, String dash, String zero) {
        switch (modifier) {
            case 45:
                return dash;
            case 48:
                return zero;
            case 95:
                return underscore;
            default:
                return normal;
        }
    }

    private static boolean isLeap(int year) {
        return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    private static boolean brokenIsUpper(char toCheck) {
        return toCheck >= 'A' && toCheck <= 'Z';
    }

    private static boolean brokenIsLower(char toCheck) {
        return toCheck >= 'a' && toCheck <= 'z';
    }

    private static char brokenToLower(char input) {
        if (input >= 'A' && input <= 'Z') {
            return (char) ((input - 'A') + 97);
        }
        return input;
    }

    private static char brokenToUpper(char input) {
        if (input >= 'a' && input <= 'z') {
            return (char) ((input - 'a') + 65);
        }
        return input;
    }
}
