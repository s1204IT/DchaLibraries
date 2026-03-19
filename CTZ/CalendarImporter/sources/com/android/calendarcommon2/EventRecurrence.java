package com.android.calendarcommon2;

import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import java.util.HashMap;

public class EventRecurrence {
    private static final boolean ALLOW_LOWER_CASE = true;
    public static final int DAILY = 4;
    public static final int FR = 2097152;
    public static final int HOURLY = 3;
    public static final int MINUTELY = 2;
    public static final int MO = 131072;
    public static final int MONTHLY = 6;
    private static final boolean ONLY_ONE_UNTIL_COUNT = false;
    private static final int PARSED_BYDAY = 128;
    private static final int PARSED_BYHOUR = 64;
    private static final int PARSED_BYMINUTE = 32;
    private static final int PARSED_BYMONTH = 2048;
    private static final int PARSED_BYMONTHDAY = 256;
    private static final int PARSED_BYSECOND = 16;
    private static final int PARSED_BYSETPOS = 4096;
    private static final int PARSED_BYWEEKNO = 1024;
    private static final int PARSED_BYYEARDAY = 512;
    private static final int PARSED_COUNT = 4;
    private static final int PARSED_FREQ = 1;
    private static final int PARSED_INTERVAL = 8;
    private static final int PARSED_UNTIL = 2;
    private static final int PARSED_WKST = 8192;
    public static final int SA = 4194304;
    public static final int SECONDLY = 1;
    public static final int SU = 65536;
    public static final int TH = 1048576;
    public static final int TU = 262144;
    private static final boolean VALIDATE_UNTIL = false;
    public static final int WE = 524288;
    public static final int WEEKLY = 5;
    public static final int YEARLY = 7;
    private static final HashMap<String, Integer> sParseFreqMap;
    private static final HashMap<String, Integer> sParseWeekdayMap;
    public int[] byday;
    public int bydayCount;
    public int[] bydayNum;
    public int[] byhour;
    public int byhourCount;
    public int[] byminute;
    public int byminuteCount;
    public int[] bymonth;
    public int bymonthCount;
    public int[] bymonthday;
    public int bymonthdayCount;
    public int[] bysecond;
    public int bysecondCount;
    public int[] bysetpos;
    public int bysetposCount;
    public int[] byweekno;
    public int byweeknoCount;
    public int[] byyearday;
    public int byyeardayCount;
    public int count;
    public int freq;
    public int interval;
    public Time startDate;
    public String until;
    public int wkst;
    private static String TAG = "EventRecur";
    private static HashMap<String, PartParser> sParsePartMap = new HashMap<>();

    static {
        sParsePartMap.put("FREQ", new ParseFreq());
        sParsePartMap.put("UNTIL", new ParseUntil());
        sParsePartMap.put("COUNT", new ParseCount());
        sParsePartMap.put("INTERVAL", new ParseInterval());
        sParsePartMap.put("BYSECOND", new ParseBySecond());
        sParsePartMap.put("BYMINUTE", new ParseByMinute());
        sParsePartMap.put("BYHOUR", new ParseByHour());
        sParsePartMap.put("BYDAY", new ParseByDay());
        sParsePartMap.put("BYMONTHDAY", new ParseByMonthDay());
        sParsePartMap.put("BYYEARDAY", new ParseByYearDay());
        sParsePartMap.put("BYWEEKNO", new ParseByWeekNo());
        sParsePartMap.put("BYMONTH", new ParseByMonth());
        sParsePartMap.put("BYSETPOS", new ParseBySetPos());
        sParsePartMap.put("WKST", new ParseWkst());
        sParseFreqMap = new HashMap<>();
        sParseFreqMap.put("SECONDLY", 1);
        sParseFreqMap.put("MINUTELY", 2);
        sParseFreqMap.put("HOURLY", 3);
        sParseFreqMap.put("DAILY", 4);
        sParseFreqMap.put("WEEKLY", 5);
        sParseFreqMap.put("MONTHLY", 6);
        sParseFreqMap.put("YEARLY", 7);
        sParseWeekdayMap = new HashMap<>();
        sParseWeekdayMap.put("SU", Integer.valueOf(SU));
        sParseWeekdayMap.put("MO", Integer.valueOf(MO));
        sParseWeekdayMap.put("TU", Integer.valueOf(TU));
        sParseWeekdayMap.put("WE", Integer.valueOf(WE));
        sParseWeekdayMap.put("TH", Integer.valueOf(TH));
        sParseWeekdayMap.put("FR", Integer.valueOf(FR));
        sParseWeekdayMap.put("SA", Integer.valueOf(SA));
    }

    public static class InvalidFormatException extends RuntimeException {
        InvalidFormatException(String str) {
            super(str);
        }
    }

    public void setStartDate(Time time) {
        this.startDate = time;
    }

    public static int calendarDay2Day(int i) {
        switch (i) {
            case 1:
                return SU;
            case 2:
                return MO;
            case 3:
                return TU;
            case 4:
                return WE;
            case 5:
                return TH;
            case 6:
                return FR;
            case 7:
                return SA;
            default:
                throw new RuntimeException("bad day of week: " + i);
        }
    }

    public static int timeDay2Day(int i) {
        switch (i) {
            case 0:
                return SU;
            case 1:
                return MO;
            case 2:
                return TU;
            case 3:
                return WE;
            case 4:
                return TH;
            case 5:
                return FR;
            case 6:
                return SA;
            default:
                throw new RuntimeException("bad day of week: " + i);
        }
    }

    public static int day2TimeDay(int i) {
        if (i == 65536) {
            return 0;
        }
        if (i == 131072) {
            return 1;
        }
        if (i == 262144) {
            return 2;
        }
        if (i == 524288) {
            return 3;
        }
        if (i == 1048576) {
            return 4;
        }
        if (i == 2097152) {
            return 5;
        }
        if (i == 4194304) {
            return 6;
        }
        throw new RuntimeException("bad day of week: " + i);
    }

    public static int day2CalendarDay(int i) {
        if (i == 65536) {
            return 1;
        }
        if (i == 131072) {
            return 2;
        }
        if (i == 262144) {
            return 3;
        }
        if (i == 524288) {
            return 4;
        }
        if (i == 1048576) {
            return 5;
        }
        if (i == 2097152) {
            return 6;
        }
        if (i == 4194304) {
            return 7;
        }
        throw new RuntimeException("bad day of week: " + i);
    }

    private static String day2String(int i) {
        if (i == 65536) {
            return "SU";
        }
        if (i == 131072) {
            return "MO";
        }
        if (i == 262144) {
            return "TU";
        }
        if (i == 524288) {
            return "WE";
        }
        if (i == 1048576) {
            return "TH";
        }
        if (i == 2097152) {
            return "FR";
        }
        if (i == 4194304) {
            return "SA";
        }
        throw new IllegalArgumentException("bad day argument: " + i);
    }

    private static void appendNumbers(StringBuilder sb, String str, int i, int[] iArr) {
        if (i > 0) {
            sb.append(str);
            int i2 = i - 1;
            for (int i3 = 0; i3 < i2; i3++) {
                sb.append(iArr[i3]);
                sb.append(",");
            }
            sb.append(iArr[i2]);
        }
    }

    private void appendByDay(StringBuilder sb, int i) {
        int i2 = this.bydayNum[i];
        if (i2 != 0) {
            sb.append(i2);
        }
        sb.append(day2String(this.byday[i]));
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FREQ=");
        switch (this.freq) {
            case 1:
                sb.append("SECONDLY");
                break;
            case 2:
                sb.append("MINUTELY");
                break;
            case 3:
                sb.append("HOURLY");
                break;
            case 4:
                sb.append("DAILY");
                break;
            case 5:
                sb.append("WEEKLY");
                break;
            case 6:
                sb.append("MONTHLY");
                break;
            case 7:
                sb.append("YEARLY");
                break;
        }
        if (!TextUtils.isEmpty(this.until)) {
            sb.append(";UNTIL=");
            sb.append(this.until);
        }
        if (this.count != 0) {
            sb.append(";COUNT=");
            sb.append(this.count);
        }
        if (this.interval != 0) {
            sb.append(";INTERVAL=");
            sb.append(this.interval);
        }
        if (this.wkst != 0) {
            sb.append(";WKST=");
            sb.append(day2String(this.wkst));
        }
        appendNumbers(sb, ";BYSECOND=", this.bysecondCount, this.bysecond);
        appendNumbers(sb, ";BYMINUTE=", this.byminuteCount, this.byminute);
        appendNumbers(sb, ";BYSECOND=", this.byhourCount, this.byhour);
        int i = this.bydayCount;
        if (i > 0) {
            sb.append(";BYDAY=");
            int i2 = i - 1;
            for (int i3 = 0; i3 < i2; i3++) {
                appendByDay(sb, i3);
                sb.append(",");
            }
            appendByDay(sb, i2);
        }
        appendNumbers(sb, ";BYMONTHDAY=", this.bymonthdayCount, this.bymonthday);
        appendNumbers(sb, ";BYYEARDAY=", this.byyeardayCount, this.byyearday);
        appendNumbers(sb, ";BYWEEKNO=", this.byweeknoCount, this.byweekno);
        appendNumbers(sb, ";BYMONTH=", this.bymonthCount, this.bymonth);
        appendNumbers(sb, ";BYSETPOS=", this.bysetposCount, this.bysetpos);
        return sb.toString();
    }

    public boolean repeatsOnEveryWeekDay() {
        int i;
        if (this.freq != 5 || (i = this.bydayCount) != 5) {
            return false;
        }
        for (int i2 = 0; i2 < i; i2++) {
            int i3 = this.byday[i2];
            if (i3 == 65536 || i3 == 4194304) {
                return false;
            }
        }
        return ALLOW_LOWER_CASE;
    }

    public boolean repeatsMonthlyOnDayCount() {
        if (this.freq == 6 && this.bydayCount == 1 && this.bymonthdayCount == 0 && this.bydayNum[0] > 0) {
            return ALLOW_LOWER_CASE;
        }
        return false;
    }

    private static boolean arraysEqual(int[] iArr, int i, int[] iArr2, int i2) {
        if (i != i2) {
            return false;
        }
        for (int i3 = 0; i3 < i; i3++) {
            if (iArr[i3] != iArr2[i3]) {
                return false;
            }
        }
        return ALLOW_LOWER_CASE;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return ALLOW_LOWER_CASE;
        }
        if (!(obj instanceof EventRecurrence)) {
            return false;
        }
        EventRecurrence eventRecurrence = (EventRecurrence) obj;
        if (this.startDate != null ? Time.compare(this.startDate, eventRecurrence.startDate) == 0 : eventRecurrence.startDate == null) {
            if (this.freq == eventRecurrence.freq && (this.until != null ? this.until.equals(eventRecurrence.until) : eventRecurrence.until == null) && this.count == eventRecurrence.count && this.interval == eventRecurrence.interval && this.wkst == eventRecurrence.wkst && arraysEqual(this.bysecond, this.bysecondCount, eventRecurrence.bysecond, eventRecurrence.bysecondCount) && arraysEqual(this.byminute, this.byminuteCount, eventRecurrence.byminute, eventRecurrence.byminuteCount) && arraysEqual(this.byhour, this.byhourCount, eventRecurrence.byhour, eventRecurrence.byhourCount) && arraysEqual(this.byday, this.bydayCount, eventRecurrence.byday, eventRecurrence.bydayCount) && arraysEqual(this.bydayNum, this.bydayCount, eventRecurrence.bydayNum, eventRecurrence.bydayCount) && arraysEqual(this.bymonthday, this.bymonthdayCount, eventRecurrence.bymonthday, eventRecurrence.bymonthdayCount) && arraysEqual(this.byyearday, this.byyeardayCount, eventRecurrence.byyearday, eventRecurrence.byyeardayCount) && arraysEqual(this.byweekno, this.byweeknoCount, eventRecurrence.byweekno, eventRecurrence.byweeknoCount) && arraysEqual(this.bymonth, this.bymonthCount, eventRecurrence.bymonth, eventRecurrence.bymonthCount) && arraysEqual(this.bysetpos, this.bysetposCount, eventRecurrence.bysetpos, eventRecurrence.bysetposCount)) {
                return ALLOW_LOWER_CASE;
            }
        }
        return false;
    }

    public int hashCode() {
        throw new UnsupportedOperationException();
    }

    private void resetFields() {
        this.until = null;
        this.bysetposCount = 0;
        this.bymonthCount = 0;
        this.byweeknoCount = 0;
        this.byyeardayCount = 0;
        this.bymonthdayCount = 0;
        this.bydayCount = 0;
        this.byhourCount = 0;
        this.byminuteCount = 0;
        this.bysecondCount = 0;
        this.interval = 0;
        this.count = 0;
        this.freq = 0;
    }

    public void parse(String str) {
        resetFields();
        int i = 0;
        for (String str2 : str.toUpperCase().split(";")) {
            if (!TextUtils.isEmpty(str2)) {
                int iIndexOf = str2.indexOf(61);
                if (iIndexOf <= 0) {
                    throw new InvalidFormatException("Missing LHS in " + str2);
                }
                String strSubstring = str2.substring(0, iIndexOf);
                String strSubstring2 = str2.substring(iIndexOf + 1);
                if (strSubstring2.length() == 0) {
                    throw new InvalidFormatException("Missing RHS in " + str2);
                }
                PartParser partParser = sParsePartMap.get(strSubstring);
                if (partParser == null) {
                    if (!strSubstring.startsWith("X-")) {
                        throw new InvalidFormatException("Couldn't find parser for " + strSubstring);
                    }
                } else {
                    int part = partParser.parsePart(strSubstring2, this);
                    if ((i & part) != 0) {
                        throw new InvalidFormatException("Part " + strSubstring + " was specified twice");
                    }
                    i |= part;
                }
            }
        }
        if ((i & PARSED_WKST) == 0) {
            this.wkst = MO;
        }
        if ((i & 1) == 0) {
            throw new InvalidFormatException("Must specify a FREQ value");
        }
        if ((i & 6) == 6) {
            Log.w(TAG, "Warning: rrule has both UNTIL and COUNT: " + str);
        }
    }

    static abstract class PartParser {
        public abstract int parsePart(String str, EventRecurrence eventRecurrence);

        PartParser() {
        }

        public static int parseIntRange(String str, int i, int i2, boolean z) {
            try {
                if (str.charAt(0) == '+') {
                    str = str.substring(1);
                }
                int i3 = Integer.parseInt(str);
                if (i3 < i || i3 > i2 || (i3 == 0 && !z)) {
                    throw new InvalidFormatException("Integer value out of range: " + str);
                }
                return i3;
            } catch (NumberFormatException e) {
                throw new InvalidFormatException("Invalid integer value: " + str);
            }
        }

        public static int[] parseNumberList(String str, int i, int i2, boolean z) {
            if (str.indexOf(",") < 0) {
                return new int[]{parseIntRange(str, i, i2, z)};
            }
            String[] strArrSplit = str.split(",");
            int length = strArrSplit.length;
            int[] iArr = new int[length];
            for (int i3 = 0; i3 < length; i3++) {
                iArr[i3] = parseIntRange(strArrSplit[i3], i, i2, z);
            }
            return iArr;
        }
    }

    private static class ParseFreq extends PartParser {
        private ParseFreq() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            Integer num = (Integer) EventRecurrence.sParseFreqMap.get(str);
            if (num == null) {
                throw new InvalidFormatException("Invalid FREQ value: " + str);
            }
            eventRecurrence.freq = num.intValue();
            return 1;
        }
    }

    private static class ParseUntil extends PartParser {
        private ParseUntil() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            eventRecurrence.until = str;
            return 2;
        }
    }

    private static class ParseCount extends PartParser {
        private ParseCount() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            eventRecurrence.count = parseIntRange(str, Integer.MIN_VALUE, Integer.MAX_VALUE, EventRecurrence.ALLOW_LOWER_CASE);
            if (eventRecurrence.count < 0) {
                Log.d(EventRecurrence.TAG, "Invalid Count. Forcing COUNT to 1 from " + str);
                eventRecurrence.count = 1;
                return 4;
            }
            return 4;
        }
    }

    private static class ParseInterval extends PartParser {
        private ParseInterval() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            eventRecurrence.interval = parseIntRange(str, Integer.MIN_VALUE, Integer.MAX_VALUE, EventRecurrence.ALLOW_LOWER_CASE);
            if (eventRecurrence.interval < 1) {
                Log.d(EventRecurrence.TAG, "Invalid Interval. Forcing INTERVAL to 1 from " + str);
                eventRecurrence.interval = 1;
                return 8;
            }
            return 8;
        }
    }

    private static class ParseBySecond extends PartParser {
        private ParseBySecond() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            int[] numberList = parseNumberList(str, 0, 59, EventRecurrence.ALLOW_LOWER_CASE);
            eventRecurrence.bysecond = numberList;
            eventRecurrence.bysecondCount = numberList.length;
            return 16;
        }
    }

    private static class ParseByMinute extends PartParser {
        private ParseByMinute() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            int[] numberList = parseNumberList(str, 0, 59, EventRecurrence.ALLOW_LOWER_CASE);
            eventRecurrence.byminute = numberList;
            eventRecurrence.byminuteCount = numberList.length;
            return EventRecurrence.PARSED_BYMINUTE;
        }
    }

    private static class ParseByHour extends PartParser {
        private ParseByHour() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            int[] numberList = parseNumberList(str, 0, 23, EventRecurrence.ALLOW_LOWER_CASE);
            eventRecurrence.byhour = numberList;
            eventRecurrence.byhourCount = numberList.length;
            return EventRecurrence.PARSED_BYHOUR;
        }
    }

    private static class ParseByDay extends PartParser {
        private ParseByDay() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            int[] iArr;
            int[] iArr2;
            int length = 1;
            if (str.indexOf(",") < 0) {
                iArr = new int[1];
                iArr2 = new int[1];
                parseWday(str, iArr, iArr2, 0);
            } else {
                String[] strArrSplit = str.split(",");
                length = strArrSplit.length;
                iArr = new int[length];
                iArr2 = new int[length];
                for (int i = 0; i < length; i++) {
                    parseWday(strArrSplit[i], iArr, iArr2, i);
                }
            }
            eventRecurrence.byday = iArr;
            eventRecurrence.bydayNum = iArr2;
            eventRecurrence.bydayCount = length;
            return EventRecurrence.PARSED_BYDAY;
        }

        private static void parseWday(String str, int[] iArr, int[] iArr2, int i) {
            String strSubstring;
            int length = str.length() - 2;
            if (length > 0) {
                iArr2[i] = parseIntRange(str.substring(0, length), -53, 53, false);
                strSubstring = str.substring(length);
            } else {
                strSubstring = str;
            }
            Integer num = (Integer) EventRecurrence.sParseWeekdayMap.get(strSubstring);
            if (num == null) {
                throw new InvalidFormatException("Invalid BYDAY value: " + str);
            }
            iArr[i] = num.intValue();
        }
    }

    private static class ParseByMonthDay extends PartParser {
        private ParseByMonthDay() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            int[] numberList = parseNumberList(str, -31, 31, false);
            eventRecurrence.bymonthday = numberList;
            eventRecurrence.bymonthdayCount = numberList.length;
            return EventRecurrence.PARSED_BYMONTHDAY;
        }
    }

    private static class ParseByYearDay extends PartParser {
        private ParseByYearDay() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            int[] numberList = parseNumberList(str, -366, 366, false);
            eventRecurrence.byyearday = numberList;
            eventRecurrence.byyeardayCount = numberList.length;
            return EventRecurrence.PARSED_BYYEARDAY;
        }
    }

    private static class ParseByWeekNo extends PartParser {
        private ParseByWeekNo() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            int[] numberList = parseNumberList(str, -53, 53, false);
            eventRecurrence.byweekno = numberList;
            eventRecurrence.byweeknoCount = numberList.length;
            return EventRecurrence.PARSED_BYWEEKNO;
        }
    }

    private static class ParseByMonth extends PartParser {
        private ParseByMonth() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            int[] numberList = parseNumberList(str, 1, 12, false);
            eventRecurrence.bymonth = numberList;
            eventRecurrence.bymonthCount = numberList.length;
            return EventRecurrence.PARSED_BYMONTH;
        }
    }

    private static class ParseBySetPos extends PartParser {
        private ParseBySetPos() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            int[] numberList = parseNumberList(str, Integer.MIN_VALUE, Integer.MAX_VALUE, EventRecurrence.ALLOW_LOWER_CASE);
            eventRecurrence.bysetpos = numberList;
            eventRecurrence.bysetposCount = numberList.length;
            return EventRecurrence.PARSED_BYSETPOS;
        }
    }

    private static class ParseWkst extends PartParser {
        private ParseWkst() {
        }

        @Override
        public int parsePart(String str, EventRecurrence eventRecurrence) {
            Integer num = (Integer) EventRecurrence.sParseWeekdayMap.get(str);
            if (num == null) {
                throw new InvalidFormatException("Invalid WKST value: " + str);
            }
            eventRecurrence.wkst = num.intValue();
            return EventRecurrence.PARSED_WKST;
        }
    }
}
