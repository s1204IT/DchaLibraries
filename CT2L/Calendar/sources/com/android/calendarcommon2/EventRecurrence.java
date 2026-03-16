package com.android.calendarcommon2;

import android.text.TextUtils;
import android.text.format.Time;
import android.util.Log;
import java.util.HashMap;

public class EventRecurrence {
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
        sParseWeekdayMap.put("SU", 65536);
        sParseWeekdayMap.put("MO", 131072);
        sParseWeekdayMap.put("TU", 262144);
        sParseWeekdayMap.put("WE", 524288);
        sParseWeekdayMap.put("TH", 1048576);
        sParseWeekdayMap.put("FR", 2097152);
        sParseWeekdayMap.put("SA", 4194304);
    }

    public static class InvalidFormatException extends RuntimeException {
        InvalidFormatException(String s) {
            super(s);
        }
    }

    public void setStartDate(Time date) {
        this.startDate = date;
    }

    public static int calendarDay2Day(int day) {
        switch (day) {
            case 1:
                return 65536;
            case 2:
                return 131072;
            case 3:
                return 262144;
            case 4:
                return 524288;
            case 5:
                return 1048576;
            case 6:
                return 2097152;
            case 7:
                return 4194304;
            default:
                throw new RuntimeException("bad day of week: " + day);
        }
    }

    public static int timeDay2Day(int day) {
        switch (day) {
            case 0:
                return 65536;
            case 1:
                return 131072;
            case 2:
                return 262144;
            case 3:
                return 524288;
            case 4:
                return 1048576;
            case 5:
                return 2097152;
            case 6:
                return 4194304;
            default:
                throw new RuntimeException("bad day of week: " + day);
        }
    }

    public static int day2TimeDay(int day) {
        switch (day) {
            case 65536:
                return 0;
            case 131072:
                return 1;
            case 262144:
                return 2;
            case 524288:
                return 3;
            case 1048576:
                return 4;
            case 2097152:
                return 5;
            case 4194304:
                return 6;
            default:
                throw new RuntimeException("bad day of week: " + day);
        }
    }

    private static String day2String(int day) {
        switch (day) {
            case 65536:
                return "SU";
            case 131072:
                return "MO";
            case 262144:
                return "TU";
            case 524288:
                return "WE";
            case 1048576:
                return "TH";
            case 2097152:
                return "FR";
            case 4194304:
                return "SA";
            default:
                throw new IllegalArgumentException("bad day argument: " + day);
        }
    }

    private static void appendNumbers(StringBuilder s, String label, int count, int[] values) {
        if (count > 0) {
            s.append(label);
            int count2 = count - 1;
            for (int i = 0; i < count2; i++) {
                s.append(values[i]);
                s.append(",");
            }
            s.append(values[count2]);
        }
    }

    private void appendByDay(StringBuilder s, int i) {
        int n = this.bydayNum[i];
        if (n != 0) {
            s.append(n);
        }
        String str = day2String(this.byday[i]);
        s.append(str);
    }

    public String toString() {
        StringBuilder s = new StringBuilder();
        s.append("FREQ=");
        switch (this.freq) {
            case 1:
                s.append("SECONDLY");
                break;
            case 2:
                s.append("MINUTELY");
                break;
            case 3:
                s.append("HOURLY");
                break;
            case 4:
                s.append("DAILY");
                break;
            case 5:
                s.append("WEEKLY");
                break;
            case 6:
                s.append("MONTHLY");
                break;
            case 7:
                s.append("YEARLY");
                break;
        }
        if (!TextUtils.isEmpty(this.until)) {
            s.append(";UNTIL=");
            s.append(this.until);
        }
        if (this.count != 0) {
            s.append(";COUNT=");
            s.append(this.count);
        }
        if (this.interval != 0) {
            s.append(";INTERVAL=");
            s.append(this.interval);
        }
        if (this.wkst != 0) {
            s.append(";WKST=");
            s.append(day2String(this.wkst));
        }
        appendNumbers(s, ";BYSECOND=", this.bysecondCount, this.bysecond);
        appendNumbers(s, ";BYMINUTE=", this.byminuteCount, this.byminute);
        appendNumbers(s, ";BYSECOND=", this.byhourCount, this.byhour);
        int count = this.bydayCount;
        if (count > 0) {
            s.append(";BYDAY=");
            int count2 = count - 1;
            for (int i = 0; i < count2; i++) {
                appendByDay(s, i);
                s.append(",");
            }
            appendByDay(s, count2);
        }
        appendNumbers(s, ";BYMONTHDAY=", this.bymonthdayCount, this.bymonthday);
        appendNumbers(s, ";BYYEARDAY=", this.byyeardayCount, this.byyearday);
        appendNumbers(s, ";BYWEEKNO=", this.byweeknoCount, this.byweekno);
        appendNumbers(s, ";BYMONTH=", this.bymonthCount, this.bymonth);
        appendNumbers(s, ";BYSETPOS=", this.bysetposCount, this.bysetpos);
        return s.toString();
    }

    public boolean repeatsOnEveryWeekDay() {
        int count;
        if (this.freq != 5 || (count = this.bydayCount) != 5) {
            return false;
        }
        for (int i = 0; i < count; i++) {
            int day = this.byday[i];
            if (day == 65536 || day == 4194304) {
                return false;
            }
        }
        return true;
    }

    private static boolean arraysEqual(int[] array1, int count1, int[] array2, int count2) {
        if (count1 != count2) {
            return false;
        }
        for (int i = 0; i < count1; i++) {
            if (array1[i] != array2[i]) {
                return false;
            }
        }
        return true;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EventRecurrence)) {
            return false;
        }
        EventRecurrence er = (EventRecurrence) obj;
        if (this.startDate != null ? Time.compare(this.startDate, er.startDate) == 0 : er.startDate == null) {
            if (this.freq == er.freq && (this.until != null ? this.until.equals(er.until) : er.until == null) && this.count == er.count && this.interval == er.interval && this.wkst == er.wkst && arraysEqual(this.bysecond, this.bysecondCount, er.bysecond, er.bysecondCount) && arraysEqual(this.byminute, this.byminuteCount, er.byminute, er.byminuteCount) && arraysEqual(this.byhour, this.byhourCount, er.byhour, er.byhourCount) && arraysEqual(this.byday, this.bydayCount, er.byday, er.bydayCount) && arraysEqual(this.bydayNum, this.bydayCount, er.bydayNum, er.bydayCount) && arraysEqual(this.bymonthday, this.bymonthdayCount, er.bymonthday, er.bymonthdayCount) && arraysEqual(this.byyearday, this.byyeardayCount, er.byyearday, er.byyeardayCount) && arraysEqual(this.byweekno, this.byweeknoCount, er.byweekno, er.byweeknoCount) && arraysEqual(this.bymonth, this.bymonthCount, er.bymonth, er.bymonthCount) && arraysEqual(this.bysetpos, this.bysetposCount, er.bysetpos, er.bysetposCount)) {
                return true;
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

    public void parse(String recur) {
        resetFields();
        int parseFlags = 0;
        String[] parts = recur.toUpperCase().split(";");
        for (String part : parts) {
            if (!TextUtils.isEmpty(part)) {
                int equalIndex = part.indexOf(61);
                if (equalIndex <= 0) {
                    throw new InvalidFormatException("Missing LHS in " + part);
                }
                String lhs = part.substring(0, equalIndex);
                String rhs = part.substring(equalIndex + 1);
                if (rhs.length() == 0) {
                    throw new InvalidFormatException("Missing RHS in " + part);
                }
                PartParser parser = sParsePartMap.get(lhs);
                if (parser == null) {
                    if (!lhs.startsWith("X-")) {
                        throw new InvalidFormatException("Couldn't find parser for " + lhs);
                    }
                } else {
                    int flag = parser.parsePart(rhs, this);
                    if ((parseFlags & flag) != 0) {
                        throw new InvalidFormatException("Part " + lhs + " was specified twice");
                    }
                    parseFlags |= flag;
                }
            }
        }
        if ((parseFlags & 8192) == 0) {
            this.wkst = 131072;
        }
        if ((parseFlags & 1) == 0) {
            throw new InvalidFormatException("Must specify a FREQ value");
        }
        if ((parseFlags & 6) == 6) {
            Log.w(TAG, "Warning: rrule has both UNTIL and COUNT: " + recur);
        }
    }

    static abstract class PartParser {
        public abstract int parsePart(String str, EventRecurrence eventRecurrence);

        PartParser() {
        }

        public static int parseIntRange(String str, int minVal, int maxVal, boolean allowZero) {
            try {
                if (str.charAt(0) == '+') {
                    str = str.substring(1);
                }
                int val = Integer.parseInt(str);
                if (val < minVal || val > maxVal || (val == 0 && !allowZero)) {
                    throw new InvalidFormatException("Integer value out of range: " + str);
                }
                return val;
            } catch (NumberFormatException e) {
                throw new InvalidFormatException("Invalid integer value: " + str);
            }
        }

        public static int[] parseNumberList(String listStr, int minVal, int maxVal, boolean allowZero) {
            if (listStr.indexOf(",") < 0) {
                return new int[]{parseIntRange(listStr, minVal, maxVal, allowZero)};
            }
            String[] valueStrs = listStr.split(",");
            int len = valueStrs.length;
            int[] values = new int[len];
            for (int i = 0; i < len; i++) {
                values[i] = parseIntRange(valueStrs[i], minVal, maxVal, allowZero);
            }
            return values;
        }
    }

    private static class ParseFreq extends PartParser {
        private ParseFreq() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            Integer freq = (Integer) EventRecurrence.sParseFreqMap.get(value);
            if (freq == null) {
                throw new InvalidFormatException("Invalid FREQ value: " + value);
            }
            er.freq = freq.intValue();
            return 1;
        }
    }

    private static class ParseUntil extends PartParser {
        private ParseUntil() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            er.until = value;
            return 2;
        }
    }

    private static class ParseCount extends PartParser {
        private ParseCount() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            er.count = parseIntRange(value, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
            if (er.count < 0) {
                Log.d(EventRecurrence.TAG, "Invalid Count. Forcing COUNT to 1 from " + value);
                er.count = 1;
                return 4;
            }
            return 4;
        }
    }

    private static class ParseInterval extends PartParser {
        private ParseInterval() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            er.interval = parseIntRange(value, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
            if (er.interval < 1) {
                Log.d(EventRecurrence.TAG, "Invalid Interval. Forcing INTERVAL to 1 from " + value);
                er.interval = 1;
                return 8;
            }
            return 8;
        }
    }

    private static class ParseBySecond extends PartParser {
        private ParseBySecond() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            int[] bysecond = parseNumberList(value, 0, 59, true);
            er.bysecond = bysecond;
            er.bysecondCount = bysecond.length;
            return 16;
        }
    }

    private static class ParseByMinute extends PartParser {
        private ParseByMinute() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            int[] byminute = parseNumberList(value, 0, 59, true);
            er.byminute = byminute;
            er.byminuteCount = byminute.length;
            return 32;
        }
    }

    private static class ParseByHour extends PartParser {
        private ParseByHour() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            int[] byhour = parseNumberList(value, 0, 23, true);
            er.byhour = byhour;
            er.byhourCount = byhour.length;
            return 64;
        }
    }

    private static class ParseByDay extends PartParser {
        private ParseByDay() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            int bydayCount;
            int[] byday;
            int[] bydayNum;
            if (value.indexOf(",") < 0) {
                bydayCount = 1;
                byday = new int[1];
                bydayNum = new int[1];
                parseWday(value, byday, bydayNum, 0);
            } else {
                String[] wdays = value.split(",");
                int len = wdays.length;
                bydayCount = len;
                byday = new int[len];
                bydayNum = new int[len];
                for (int i = 0; i < len; i++) {
                    parseWday(wdays[i], byday, bydayNum, i);
                }
            }
            er.byday = byday;
            er.bydayNum = bydayNum;
            er.bydayCount = bydayCount;
            return 128;
        }

        private static void parseWday(String str, int[] byday, int[] bydayNum, int index) {
            String wdayStr;
            int wdayStrStart = str.length() - 2;
            if (wdayStrStart > 0) {
                String numPart = str.substring(0, wdayStrStart);
                int num = parseIntRange(numPart, -53, 53, false);
                bydayNum[index] = num;
                wdayStr = str.substring(wdayStrStart);
            } else {
                wdayStr = str;
            }
            Integer wday = (Integer) EventRecurrence.sParseWeekdayMap.get(wdayStr);
            if (wday == null) {
                throw new InvalidFormatException("Invalid BYDAY value: " + str);
            }
            byday[index] = wday.intValue();
        }
    }

    private static class ParseByMonthDay extends PartParser {
        private ParseByMonthDay() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            int[] bymonthday = parseNumberList(value, -31, 31, false);
            er.bymonthday = bymonthday;
            er.bymonthdayCount = bymonthday.length;
            return 256;
        }
    }

    private static class ParseByYearDay extends PartParser {
        private ParseByYearDay() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            int[] byyearday = parseNumberList(value, -366, 366, false);
            er.byyearday = byyearday;
            er.byyeardayCount = byyearday.length;
            return 512;
        }
    }

    private static class ParseByWeekNo extends PartParser {
        private ParseByWeekNo() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            int[] byweekno = parseNumberList(value, -53, 53, false);
            er.byweekno = byweekno;
            er.byweeknoCount = byweekno.length;
            return 1024;
        }
    }

    private static class ParseByMonth extends PartParser {
        private ParseByMonth() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            int[] bymonth = parseNumberList(value, 1, 12, false);
            er.bymonth = bymonth;
            er.bymonthCount = bymonth.length;
            return 2048;
        }
    }

    private static class ParseBySetPos extends PartParser {
        private ParseBySetPos() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            int[] bysetpos = parseNumberList(value, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
            er.bysetpos = bysetpos;
            er.bysetposCount = bysetpos.length;
            return 4096;
        }
    }

    private static class ParseWkst extends PartParser {
        private ParseWkst() {
        }

        @Override
        public int parsePart(String value, EventRecurrence er) {
            Integer wkst = (Integer) EventRecurrence.sParseWeekdayMap.get(value);
            if (wkst == null) {
                throw new InvalidFormatException("Invalid WKST value: " + value);
            }
            er.wkst = wkst.intValue();
            return 8192;
        }
    }
}
