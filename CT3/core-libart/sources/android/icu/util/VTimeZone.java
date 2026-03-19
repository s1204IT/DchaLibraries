package android.icu.util;

import android.icu.impl.Grego;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;

public class VTimeZone extends BasicTimeZone {

    static final boolean f119assertionsDisabled;
    private static final String COLON = ":";
    private static final String COMMA = ",";
    private static final int DEF_DSTSAVINGS = 3600000;
    private static final long DEF_TZSTARTTIME = 0;
    private static final String EQUALS_SIGN = "=";
    private static final int ERR = 3;
    private static final String ICAL_BEGIN = "BEGIN";
    private static final String ICAL_BEGIN_VTIMEZONE = "BEGIN:VTIMEZONE";
    private static final String ICAL_BYDAY = "BYDAY";
    private static final String ICAL_BYMONTH = "BYMONTH";
    private static final String ICAL_BYMONTHDAY = "BYMONTHDAY";
    private static final String ICAL_DAYLIGHT = "DAYLIGHT";
    private static final String[] ICAL_DOW_NAMES;
    private static final String ICAL_DTSTART = "DTSTART";
    private static final String ICAL_END = "END";
    private static final String ICAL_END_VTIMEZONE = "END:VTIMEZONE";
    private static final String ICAL_FREQ = "FREQ";
    private static final String ICAL_LASTMOD = "LAST-MODIFIED";
    private static final String ICAL_RDATE = "RDATE";
    private static final String ICAL_RRULE = "RRULE";
    private static final String ICAL_STANDARD = "STANDARD";
    private static final String ICAL_TZID = "TZID";
    private static final String ICAL_TZNAME = "TZNAME";
    private static final String ICAL_TZOFFSETFROM = "TZOFFSETFROM";
    private static final String ICAL_TZOFFSETTO = "TZOFFSETTO";
    private static final String ICAL_TZURL = "TZURL";
    private static final String ICAL_UNTIL = "UNTIL";
    private static final String ICAL_VTIMEZONE = "VTIMEZONE";
    private static final String ICAL_YEARLY = "YEARLY";
    private static final String ICU_TZINFO_PROP = "X-TZINFO";
    private static String ICU_TZVERSION = null;
    private static final int INI = 0;
    private static final long MAX_TIME = Long.MAX_VALUE;
    private static final long MIN_TIME = Long.MIN_VALUE;
    private static final int[] MONTHLENGTH;
    private static final String NEWLINE = "\r\n";
    private static final String SEMICOLON = ";";
    private static final int TZI = 2;
    private static final int VTZ = 1;
    private static final long serialVersionUID = -6851467294127795902L;
    private volatile transient boolean isFrozen;
    private Date lastmod;
    private String olsonzid;
    private BasicTimeZone tz;
    private String tzurl;
    private List<String> vtzlines;

    public static VTimeZone create(String tzid) {
        VTimeZone vtz = new VTimeZone(tzid);
        vtz.tz = (BasicTimeZone) TimeZone.getTimeZone(tzid, 0);
        vtz.olsonzid = vtz.tz.getID();
        return vtz;
    }

    public static VTimeZone create(Reader reader) {
        VTimeZone vtz = new VTimeZone();
        if (vtz.load(reader)) {
            return vtz;
        }
        return null;
    }

    @Override
    public int getOffset(int era, int year, int month, int day, int dayOfWeek, int milliseconds) {
        return this.tz.getOffset(era, year, month, day, dayOfWeek, milliseconds);
    }

    @Override
    public void getOffset(long date, boolean local, int[] offsets) {
        this.tz.getOffset(date, local, offsets);
    }

    @Override
    @Deprecated
    public void getOffsetFromLocal(long date, int nonExistingTimeOpt, int duplicatedTimeOpt, int[] offsets) {
        this.tz.getOffsetFromLocal(date, nonExistingTimeOpt, duplicatedTimeOpt, offsets);
    }

    @Override
    public int getRawOffset() {
        return this.tz.getRawOffset();
    }

    @Override
    public boolean inDaylightTime(Date date) {
        return this.tz.inDaylightTime(date);
    }

    @Override
    public void setRawOffset(int offsetMillis) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.tz.setRawOffset(offsetMillis);
    }

    @Override
    public boolean useDaylightTime() {
        return this.tz.useDaylightTime();
    }

    @Override
    public boolean observesDaylightTime() {
        return this.tz.observesDaylightTime();
    }

    @Override
    public boolean hasSameRules(TimeZone timeZone) {
        if (this == timeZone) {
            return true;
        }
        if (timeZone instanceof VTimeZone) {
            return this.tz.hasSameRules(timeZone.tz);
        }
        return this.tz.hasSameRules(timeZone);
    }

    public String getTZURL() {
        return this.tzurl;
    }

    public void setTZURL(String url) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.tzurl = url;
    }

    public Date getLastModified() {
        return this.lastmod;
    }

    public void setLastModified(Date date) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.lastmod = date;
    }

    public void write(Writer writer) throws IOException {
        BufferedWriter bw = new BufferedWriter(writer);
        if (this.vtzlines != null) {
            for (String line : this.vtzlines) {
                if (line.startsWith("TZURL:")) {
                    if (this.tzurl != null) {
                        bw.write(ICAL_TZURL);
                        bw.write(COLON);
                        bw.write(this.tzurl);
                        bw.write(NEWLINE);
                    }
                } else if (line.startsWith("LAST-MODIFIED:")) {
                    if (this.lastmod != null) {
                        bw.write(ICAL_LASTMOD);
                        bw.write(COLON);
                        bw.write(getUTCDateTimeString(this.lastmod.getTime()));
                        bw.write(NEWLINE);
                    }
                } else {
                    bw.write(line);
                    bw.write(NEWLINE);
                }
            }
            bw.flush();
            return;
        }
        String[] customProperties = null;
        if (this.olsonzid != null && ICU_TZVERSION != null) {
            customProperties = new String[]{"X-TZINFO:" + this.olsonzid + "[" + ICU_TZVERSION + "]"};
        }
        writeZone(writer, this.tz, customProperties);
    }

    public void write(Writer writer, long start) throws IOException {
        TimeZoneRule[] rules = this.tz.getTimeZoneRules(start);
        RuleBasedTimeZone rbtz = new RuleBasedTimeZone(this.tz.getID(), (InitialTimeZoneRule) rules[0]);
        for (int i = 1; i < rules.length; i++) {
            rbtz.addTransitionRule(rules[i]);
        }
        String[] customProperties = null;
        if (this.olsonzid != null && ICU_TZVERSION != null) {
            customProperties = new String[]{"X-TZINFO:" + this.olsonzid + "[" + ICU_TZVERSION + "/Partial@" + start + "]"};
        }
        writeZone(writer, rbtz, customProperties);
    }

    public void writeSimple(Writer writer, long time) throws IOException {
        TimeZoneRule[] rules = this.tz.getSimpleTimeZoneRulesNear(time);
        RuleBasedTimeZone rbtz = new RuleBasedTimeZone(this.tz.getID(), (InitialTimeZoneRule) rules[0]);
        for (int i = 1; i < rules.length; i++) {
            rbtz.addTransitionRule(rules[i]);
        }
        String[] customProperties = null;
        if (this.olsonzid != null && ICU_TZVERSION != null) {
            customProperties = new String[]{"X-TZINFO:" + this.olsonzid + "[" + ICU_TZVERSION + "/Simple@" + time + "]"};
        }
        writeZone(writer, rbtz, customProperties);
    }

    @Override
    public TimeZoneTransition getNextTransition(long base, boolean inclusive) {
        return this.tz.getNextTransition(base, inclusive);
    }

    @Override
    public TimeZoneTransition getPreviousTransition(long base, boolean inclusive) {
        return this.tz.getPreviousTransition(base, inclusive);
    }

    @Override
    public boolean hasEquivalentTransitions(TimeZone other, long start, long end) {
        if (this == other) {
            return true;
        }
        return this.tz.hasEquivalentTransitions(other, start, end);
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules() {
        return this.tz.getTimeZoneRules();
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules(long start) {
        return this.tz.getTimeZoneRules(start);
    }

    @Override
    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    static {
        f119assertionsDisabled = !VTimeZone.class.desiredAssertionStatus();
        ICAL_DOW_NAMES = new String[]{"SU", "MO", "TU", "WE", "TH", "FR", "SA"};
        MONTHLENGTH = new int[]{31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        try {
            ICU_TZVERSION = TimeZone.getTZDataVersion();
        } catch (MissingResourceException e) {
            ICU_TZVERSION = null;
        }
    }

    private VTimeZone() {
        this.olsonzid = null;
        this.tzurl = null;
        this.lastmod = null;
        this.isFrozen = false;
    }

    private VTimeZone(String tzid) {
        super(tzid);
        this.olsonzid = null;
        this.tzurl = null;
        this.lastmod = null;
        this.isFrozen = false;
    }

    private boolean load(Reader reader) {
        try {
            this.vtzlines = new LinkedList();
            boolean eol = false;
            boolean start = false;
            boolean success = false;
            StringBuilder line = new StringBuilder();
            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    if (start && line.toString().startsWith(ICAL_END_VTIMEZONE)) {
                        this.vtzlines.add(line.toString());
                        success = true;
                    }
                } else if (ch != 13) {
                    if (eol) {
                        if (ch != 9 && ch != 32) {
                            if (start && line.length() > 0) {
                                this.vtzlines.add(line.toString());
                            }
                            line.setLength(0);
                            if (ch != 10) {
                                line.append((char) ch);
                            }
                        }
                        eol = false;
                    } else if (ch == 10) {
                        eol = true;
                        if (start) {
                            if (line.toString().startsWith(ICAL_END_VTIMEZONE)) {
                                this.vtzlines.add(line.toString());
                                success = true;
                                break;
                            }
                        } else if (line.toString().startsWith(ICAL_BEGIN_VTIMEZONE)) {
                            this.vtzlines.add(line.toString());
                            line.setLength(0);
                            start = true;
                            eol = false;
                        }
                    } else {
                        line.append((char) ch);
                    }
                }
            }
            if (success) {
                return parse();
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean parse() {
        TimeZoneRule newRule;
        int rawOffset;
        int dstSavings;
        if (this.vtzlines == null || this.vtzlines.size() == 0) {
            return false;
        }
        String tzid = null;
        int state = 0;
        boolean dst = false;
        String from = null;
        String to = null;
        String tzname = null;
        String dtstart = null;
        boolean isRRULE = false;
        List<String> dates = null;
        List<TimeZoneRule> rules = new ArrayList<>();
        int initialRawOffset = 0;
        int initialDSTSavings = 0;
        long firstStart = MAX_TIME;
        for (String line : this.vtzlines) {
            int valueSep = line.indexOf(COLON);
            if (valueSep >= 0) {
                String name = line.substring(0, valueSep);
                String value = line.substring(valueSep + 1);
                switch (state) {
                    case 0:
                        if (name.equals(ICAL_BEGIN) && value.equals(ICAL_VTIMEZONE)) {
                            state = 1;
                        }
                        if (state != 3) {
                            this.vtzlines = null;
                            return false;
                        }
                        break;
                        break;
                    case 1:
                        if (name.equals(ICAL_TZID)) {
                            tzid = value;
                        } else if (name.equals(ICAL_TZURL)) {
                            this.tzurl = value;
                        } else if (name.equals(ICAL_LASTMOD)) {
                            this.lastmod = new Date(parseDateTimeString(value, 0));
                        } else if (name.equals(ICAL_BEGIN)) {
                            boolean isDST = value.equals(ICAL_DAYLIGHT);
                            if ((!value.equals(ICAL_STANDARD) && !isDST) || tzid == null) {
                                state = 3;
                            } else {
                                dates = null;
                                isRRULE = false;
                                from = null;
                                to = null;
                                tzname = null;
                                dst = isDST;
                                state = 2;
                            }
                        } else if (name.equals(ICAL_END)) {
                        }
                        if (state != 3) {
                        }
                        break;
                    case 2:
                        if (name.equals(ICAL_DTSTART)) {
                            dtstart = value;
                        } else if (name.equals(ICAL_TZNAME)) {
                            tzname = value;
                        } else if (name.equals(ICAL_TZOFFSETFROM)) {
                            from = value;
                        } else if (name.equals(ICAL_TZOFFSETTO)) {
                            to = value;
                        } else if (name.equals(ICAL_RDATE)) {
                            if (isRRULE) {
                                state = 3;
                            } else {
                                if (dates == null) {
                                    dates = new LinkedList<>();
                                }
                                java.util.StringTokenizer st = new java.util.StringTokenizer(value, COMMA);
                                while (st.hasMoreTokens()) {
                                    String date = st.nextToken();
                                    dates.add(date);
                                }
                            }
                        } else if (name.equals(ICAL_RRULE)) {
                            if (!isRRULE && dates != null) {
                                state = 3;
                            } else {
                                if (dates == null) {
                                    dates = new LinkedList<>();
                                }
                                isRRULE = true;
                                dates.add(value);
                            }
                        } else if (name.equals(ICAL_END)) {
                            if (dtstart == null || from == null || to == null) {
                                state = 3;
                            } else {
                                if (tzname == null) {
                                    tzname = getDefaultTZName(tzid, dst);
                                }
                                TimeZoneRule rule = null;
                                try {
                                    int fromOffset = offsetStrToMillis(from);
                                    int toOffset = offsetStrToMillis(to);
                                    if (dst) {
                                        if (toOffset - fromOffset > 0) {
                                            rawOffset = fromOffset;
                                            dstSavings = toOffset - fromOffset;
                                        } else {
                                            rawOffset = toOffset - 3600000;
                                            dstSavings = 3600000;
                                        }
                                    } else {
                                        rawOffset = toOffset;
                                        dstSavings = 0;
                                    }
                                    long start = parseDateTimeString(dtstart, fromOffset);
                                    if (isRRULE) {
                                        rule = createRuleByRRULE(tzname, rawOffset, dstSavings, start, dates, fromOffset);
                                    } else {
                                        rule = createRuleByRDATE(tzname, rawOffset, dstSavings, start, dates, fromOffset);
                                    }
                                    if (rule != null) {
                                        Date actualStart = rule.getFirstStart(fromOffset, 0);
                                        if (actualStart.getTime() < firstStart) {
                                            firstStart = actualStart.getTime();
                                            if (dstSavings <= 0 && fromOffset - toOffset == 3600000) {
                                                initialRawOffset = fromOffset - 3600000;
                                                initialDSTSavings = 3600000;
                                            } else {
                                                initialRawOffset = fromOffset;
                                                initialDSTSavings = 0;
                                            }
                                        }
                                    }
                                } catch (IllegalArgumentException e) {
                                }
                                if (rule == null) {
                                    state = 3;
                                } else {
                                    rules.add(rule);
                                    state = 1;
                                }
                            }
                        }
                        if (state != 3) {
                        }
                        break;
                    default:
                        if (state != 3) {
                        }
                        break;
                }
            }
        }
        if (rules.size() == 0) {
            return false;
        }
        InitialTimeZoneRule initialRule = new InitialTimeZoneRule(getDefaultTZName(tzid, false), initialRawOffset, initialDSTSavings);
        RuleBasedTimeZone rbtz = new RuleBasedTimeZone(tzid, initialRule);
        int finalRuleIdx = -1;
        int finalRuleCount = 0;
        for (int i = 0; i < rules.size(); i++) {
            TimeZoneRule timeZoneRule = rules.get(i);
            if ((timeZoneRule instanceof AnnualTimeZoneRule) && timeZoneRule.getEndYear() == Integer.MAX_VALUE) {
                finalRuleCount++;
                finalRuleIdx = i;
            }
        }
        if (finalRuleCount > 2) {
            return false;
        }
        if (finalRuleCount == 1) {
            if (rules.size() == 1) {
                rules.clear();
            } else {
                AnnualTimeZoneRule finalRule = (AnnualTimeZoneRule) rules.get(finalRuleIdx);
                int tmpRaw = finalRule.getRawOffset();
                int tmpDST = finalRule.getDSTSavings();
                Date finalStart = finalRule.getFirstStart(initialRawOffset, initialDSTSavings);
                Date start2 = finalStart;
                for (int i2 = 0; i2 < rules.size(); i2++) {
                    if (finalRuleIdx != i2) {
                        TimeZoneRule r = rules.get(i2);
                        Date lastStart = r.getFinalStart(tmpRaw, tmpDST);
                        if (lastStart.after(start2)) {
                            start2 = finalRule.getNextStart(lastStart.getTime(), r.getRawOffset(), r.getDSTSavings(), false);
                        }
                    }
                }
                if (start2 == finalStart) {
                    newRule = new TimeArrayTimeZoneRule(finalRule.getName(), finalRule.getRawOffset(), finalRule.getDSTSavings(), new long[]{finalStart.getTime()}, 2);
                } else {
                    int[] fields = Grego.timeToFields(start2.getTime(), null);
                    newRule = new AnnualTimeZoneRule(finalRule.getName(), finalRule.getRawOffset(), finalRule.getDSTSavings(), finalRule.getRule(), finalRule.getStartYear(), fields[0]);
                }
                rules.set(finalRuleIdx, newRule);
            }
        }
        Iterator r$iterator = rules.iterator();
        while (r$iterator.hasNext()) {
            rbtz.addTransitionRule((TimeZoneRule) r$iterator.next());
        }
        this.tz = rbtz;
        setID(tzid);
        return true;
    }

    private static String getDefaultTZName(String tzid, boolean isDST) {
        if (isDST) {
            return tzid + "(DST)";
        }
        return tzid + "(STD)";
    }

    private static TimeZoneRule createRuleByRRULE(String tzname, int rawOffset, int dstSavings, long start, List<String> dates, int fromOffset) {
        DateTimeRule adtr;
        if (dates == null || dates.size() == 0) {
            return null;
        }
        String rrule = dates.get(0);
        long[] until = new long[1];
        int[] ruleFields = parseRRULE(rrule, until);
        if (ruleFields == null) {
            return null;
        }
        int month = ruleFields[0];
        int dayOfWeek = ruleFields[1];
        int nthDayOfWeek = ruleFields[2];
        int dayOfMonth = ruleFields[3];
        if (dates.size() == 1) {
            if (ruleFields.length > 4) {
                if (ruleFields.length != 10 || month == -1 || dayOfWeek == 0) {
                    return null;
                }
                int firstDay = 31;
                int[] days = new int[7];
                for (int i = 0; i < 7; i++) {
                    days[i] = ruleFields[i + 3];
                    days[i] = days[i] > 0 ? days[i] : MONTHLENGTH[month] + days[i] + 1;
                    if (days[i] < firstDay) {
                        firstDay = days[i];
                    }
                }
                for (int i2 = 1; i2 < 7; i2++) {
                    boolean found = false;
                    int j = 0;
                    while (true) {
                        if (j >= 7) {
                            break;
                        }
                        if (days[j] != firstDay + i2) {
                            j++;
                        } else {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return null;
                    }
                }
                dayOfMonth = firstDay;
            }
        } else {
            if (month == -1 || dayOfWeek == 0 || dayOfMonth == 0 || dates.size() > 7) {
                return null;
            }
            int earliestMonth = month;
            int daysCount = ruleFields.length - 3;
            int earliestDay = 31;
            for (int i3 = 0; i3 < daysCount; i3++) {
                int dom = ruleFields[i3 + 3];
                if (dom <= 0) {
                    dom = MONTHLENGTH[month] + dom + 1;
                }
                if (dom < earliestDay) {
                    earliestDay = dom;
                }
            }
            int anotherMonth = -1;
            for (int i4 = 1; i4 < dates.size(); i4++) {
                String rrule2 = dates.get(i4);
                long[] unt = new long[1];
                int[] fields = parseRRULE(rrule2, unt);
                if (unt[0] > until[0]) {
                    until = unt;
                }
                if (fields[0] == -1 || fields[1] == 0 || fields[3] == 0) {
                    return null;
                }
                int count = fields.length - 3;
                if (daysCount + count > 7 || fields[1] != dayOfWeek) {
                    return null;
                }
                if (fields[0] != month) {
                    if (anotherMonth == -1) {
                        int diff = fields[0] - month;
                        if (diff == -11 || diff == -1) {
                            anotherMonth = fields[0];
                            earliestMonth = anotherMonth;
                            earliestDay = 31;
                        } else if (diff == 11 || diff == 1) {
                            anotherMonth = fields[0];
                        } else {
                            return null;
                        }
                    } else if (fields[0] != month && fields[0] != anotherMonth) {
                        return null;
                    }
                }
                if (fields[0] == earliestMonth) {
                    for (int j2 = 0; j2 < count; j2++) {
                        int dom2 = fields[j2 + 3];
                        if (dom2 <= 0) {
                            dom2 = MONTHLENGTH[fields[0]] + dom2 + 1;
                        }
                        if (dom2 < earliestDay) {
                            earliestDay = dom2;
                        }
                    }
                }
                daysCount += count;
            }
            if (daysCount != 7) {
                return null;
            }
            month = earliestMonth;
            dayOfMonth = earliestDay;
        }
        int[] dfields = Grego.timeToFields(((long) fromOffset) + start, null);
        int startYear = dfields[0];
        if (month == -1) {
            month = dfields[1];
        }
        if (dayOfWeek == 0 && nthDayOfWeek == 0 && dayOfMonth == 0) {
            dayOfMonth = dfields[2];
        }
        int timeInDay = dfields[5];
        int endYear = Integer.MAX_VALUE;
        if (until[0] != MIN_TIME) {
            Grego.timeToFields(until[0], dfields);
            endYear = dfields[0];
        }
        if (dayOfWeek == 0 && nthDayOfWeek == 0 && dayOfMonth != 0) {
            adtr = new DateTimeRule(month, dayOfMonth, timeInDay, 0);
        } else if (dayOfWeek != 0 && nthDayOfWeek != 0 && dayOfMonth == 0) {
            adtr = new DateTimeRule(month, nthDayOfWeek, dayOfWeek, timeInDay, 0);
        } else if (dayOfWeek != 0 && nthDayOfWeek == 0 && dayOfMonth != 0) {
            adtr = new DateTimeRule(month, dayOfMonth, dayOfWeek, true, timeInDay, 0);
        } else {
            return null;
        }
        return new AnnualTimeZoneRule(tzname, rawOffset, dstSavings, adtr, startYear, endYear);
    }

    private static int[] parseRRULE(String rrule, long[] until) {
        int[] results;
        int month = -1;
        int dayOfWeek = 0;
        int nthDayOfWeek = 0;
        int[] dayOfMonth = null;
        long untilTime = MIN_TIME;
        boolean yearly = false;
        boolean parseError = false;
        java.util.StringTokenizer st = new java.util.StringTokenizer(rrule, SEMICOLON);
        while (true) {
            if (!st.hasMoreTokens()) {
                break;
            }
            String prop = st.nextToken();
            int sep = prop.indexOf(EQUALS_SIGN);
            if (sep != -1) {
                String attr = prop.substring(0, sep);
                String value = prop.substring(sep + 1);
                if (attr.equals(ICAL_FREQ)) {
                    if (value.equals(ICAL_YEARLY)) {
                        yearly = true;
                    } else {
                        parseError = true;
                        break;
                    }
                } else if (attr.equals(ICAL_UNTIL)) {
                    try {
                        untilTime = parseDateTimeString(value, 0);
                    } catch (IllegalArgumentException e) {
                        parseError = true;
                    }
                } else if (attr.equals(ICAL_BYMONTH)) {
                    if (value.length() > 2) {
                        parseError = true;
                        break;
                    }
                    try {
                        month = Integer.parseInt(value) - 1;
                        if (month < 0 || month >= 12) {
                            break;
                        }
                    } catch (NumberFormatException e2) {
                        parseError = true;
                    }
                } else if (attr.equals(ICAL_BYDAY)) {
                    int length = value.length();
                    if (length < 2 || length > 4) {
                        break;
                    }
                    if (length > 2) {
                        int sign = 1;
                        if (value.charAt(0) == '+') {
                            sign = 1;
                        } else if (value.charAt(0) == '-') {
                            sign = -1;
                        } else if (length == 4) {
                            parseError = true;
                            break;
                        }
                        try {
                            int n = Integer.parseInt(value.substring(length - 3, length - 2));
                            if (n == 0 || n > 4) {
                                break;
                            }
                            nthDayOfWeek = n * sign;
                            value = value.substring(length - 2);
                        } catch (NumberFormatException e3) {
                            parseError = true;
                        }
                    }
                    int wday = 0;
                    while (wday < ICAL_DOW_NAMES.length) {
                        if (value.equals(ICAL_DOW_NAMES[wday])) {
                            break;
                        }
                        wday++;
                    }
                    if (wday < ICAL_DOW_NAMES.length) {
                        dayOfWeek = wday + 1;
                    } else {
                        parseError = true;
                        break;
                    }
                } else if (attr.equals(ICAL_BYMONTHDAY)) {
                    java.util.StringTokenizer days = new java.util.StringTokenizer(value, COMMA);
                    int count = days.countTokens();
                    dayOfMonth = new int[count];
                    int index = 0;
                    while (days.hasMoreTokens()) {
                        int index2 = index + 1;
                        try {
                            dayOfMonth[index] = Integer.parseInt(days.nextToken());
                            index = index2;
                        } catch (NumberFormatException e4) {
                            parseError = true;
                        }
                    }
                }
            } else {
                parseError = true;
                break;
            }
        }
        if (parseError || !yearly) {
            return null;
        }
        until[0] = untilTime;
        if (dayOfMonth == null) {
            results = new int[4];
            results[3] = 0;
        } else {
            results = new int[dayOfMonth.length + 3];
            for (int i = 0; i < dayOfMonth.length; i++) {
                results[i + 3] = dayOfMonth[i];
            }
        }
        results[0] = month;
        results[1] = dayOfWeek;
        results[2] = nthDayOfWeek;
        return results;
    }

    private static TimeZoneRule createRuleByRDATE(String tzname, int rawOffset, int dstSavings, long start, List<String> dates, int fromOffset) {
        long[] times;
        if (dates == null || dates.size() == 0) {
            times = new long[]{start};
        } else {
            times = new long[dates.size()];
            int idx = 0;
            try {
                Iterator date$iterator = dates.iterator();
                while (true) {
                    try {
                        int idx2 = idx;
                        if (!date$iterator.hasNext()) {
                            break;
                        }
                        String date = (String) date$iterator.next();
                        idx = idx2 + 1;
                        times[idx2] = parseDateTimeString(date, fromOffset);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }
            } catch (IllegalArgumentException e2) {
                return null;
            }
        }
        return new TimeArrayTimeZoneRule(tzname, rawOffset, dstSavings, times, 2);
    }

    private void writeZone(Writer w, BasicTimeZone basictz, String[] customProperties) throws IOException {
        writeHeader(w);
        if (customProperties != null && customProperties.length > 0) {
            for (int i = 0; i < customProperties.length; i++) {
                if (customProperties[i] != null) {
                    w.write(customProperties[i]);
                    w.write(NEWLINE);
                }
            }
        }
        long t = MIN_TIME;
        String dstName = null;
        int dstFromOffset = 0;
        int dstFromDSTSavings = 0;
        int dstToOffset = 0;
        int dstStartYear = 0;
        int dstMonth = 0;
        int dstDayOfWeek = 0;
        int dstWeekInMonth = 0;
        int dstMillisInDay = 0;
        long dstStartTime = DEF_TZSTARTTIME;
        long dstUntilTime = DEF_TZSTARTTIME;
        int dstCount = 0;
        AnnualTimeZoneRule finalDstRule = null;
        String stdName = null;
        int stdFromOffset = 0;
        int stdFromDSTSavings = 0;
        int stdToOffset = 0;
        int stdStartYear = 0;
        int stdMonth = 0;
        int stdDayOfWeek = 0;
        int stdWeekInMonth = 0;
        int stdMillisInDay = 0;
        long stdStartTime = DEF_TZSTARTTIME;
        long stdUntilTime = DEF_TZSTARTTIME;
        int stdCount = 0;
        AnnualTimeZoneRule finalStdRule = null;
        int[] dtfields = new int[6];
        boolean hasTransitions = false;
        while (true) {
            TimeZoneTransition tzt = basictz.getNextTransition(t, false);
            if (tzt == null) {
                break;
            }
            hasTransitions = true;
            t = tzt.getTime();
            String name = tzt.getTo().getName();
            boolean isDst = tzt.getTo().getDSTSavings() != 0;
            int fromOffset = tzt.getFrom().getRawOffset() + tzt.getFrom().getDSTSavings();
            int fromDSTSavings = tzt.getFrom().getDSTSavings();
            int toOffset = tzt.getTo().getRawOffset() + tzt.getTo().getDSTSavings();
            Grego.timeToFields(tzt.getTime() + ((long) fromOffset), dtfields);
            int weekInMonth = Grego.getDayOfWeekInMonth(dtfields[0], dtfields[1], dtfields[2]);
            int year = dtfields[0];
            boolean sameRule = false;
            if (isDst) {
                if (finalDstRule == null && (tzt.getTo() instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) tzt.getTo()).getEndYear() == Integer.MAX_VALUE) {
                    finalDstRule = (AnnualTimeZoneRule) tzt.getTo();
                }
                if (dstCount > 0) {
                    if (year == dstStartYear + dstCount && name.equals(dstName) && dstFromOffset == fromOffset && dstToOffset == toOffset) {
                        if (dstMonth == dtfields[1]) {
                            if (dstDayOfWeek == dtfields[3] && dstWeekInMonth == weekInMonth) {
                                if (dstMillisInDay == dtfields[5]) {
                                    dstUntilTime = t;
                                    dstCount++;
                                    sameRule = true;
                                }
                            }
                        }
                    }
                    if (!sameRule) {
                        if (dstCount == 1) {
                            writeZonePropsByTime(w, true, dstName, dstFromOffset, dstToOffset, dstStartTime, true);
                        } else {
                            writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dstMonth, dstWeekInMonth, dstDayOfWeek, dstStartTime, dstUntilTime);
                        }
                    }
                }
                if (!sameRule) {
                    dstName = name;
                    dstFromOffset = fromOffset;
                    dstFromDSTSavings = fromDSTSavings;
                    dstToOffset = toOffset;
                    dstStartYear = year;
                    dstMonth = dtfields[1];
                    dstDayOfWeek = dtfields[3];
                    dstWeekInMonth = weekInMonth;
                    dstMillisInDay = dtfields[5];
                    dstUntilTime = t;
                    dstStartTime = t;
                    dstCount = 1;
                }
                if (finalStdRule != null && finalDstRule != null) {
                    break;
                }
            } else {
                if (finalStdRule == null && (tzt.getTo() instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) tzt.getTo()).getEndYear() == Integer.MAX_VALUE) {
                    finalStdRule = (AnnualTimeZoneRule) tzt.getTo();
                }
                if (stdCount > 0) {
                    if (year == stdStartYear + stdCount && name.equals(stdName) && stdFromOffset == fromOffset && stdToOffset == toOffset) {
                        if (stdMonth == dtfields[1]) {
                            if (stdDayOfWeek == dtfields[3] && stdWeekInMonth == weekInMonth) {
                                if (stdMillisInDay == dtfields[5]) {
                                    stdUntilTime = t;
                                    stdCount++;
                                    sameRule = true;
                                }
                            }
                        }
                    }
                    if (!sameRule) {
                        if (stdCount == 1) {
                            writeZonePropsByTime(w, false, stdName, stdFromOffset, stdToOffset, stdStartTime, true);
                        } else {
                            writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdMonth, stdWeekInMonth, stdDayOfWeek, stdStartTime, stdUntilTime);
                        }
                    }
                }
                if (!sameRule) {
                    stdName = name;
                    stdFromOffset = fromOffset;
                    stdFromDSTSavings = fromDSTSavings;
                    stdToOffset = toOffset;
                    stdStartYear = year;
                    stdMonth = dtfields[1];
                    stdDayOfWeek = dtfields[3];
                    stdWeekInMonth = weekInMonth;
                    stdMillisInDay = dtfields[5];
                    stdUntilTime = t;
                    stdStartTime = t;
                    stdCount = 1;
                }
                if (finalStdRule != null && finalDstRule != null) {
                    break;
                }
            }
        }
        if (!hasTransitions) {
            int offset = basictz.getOffset(DEF_TZSTARTTIME);
            boolean isDst2 = offset != basictz.getRawOffset();
            writeZonePropsByTime(w, isDst2, getDefaultTZName(basictz.getID(), isDst2), offset, offset, DEF_TZSTARTTIME - ((long) offset), false);
        } else {
            if (dstCount > 0) {
                if (finalDstRule == null) {
                    if (dstCount == 1) {
                        writeZonePropsByTime(w, true, dstName, dstFromOffset, dstToOffset, dstStartTime, true);
                    } else {
                        writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dstMonth, dstWeekInMonth, dstDayOfWeek, dstStartTime, dstUntilTime);
                    }
                } else if (dstCount == 1) {
                    writeFinalRule(w, true, finalDstRule, dstFromOffset - dstFromDSTSavings, dstFromDSTSavings, dstStartTime);
                } else if (isEquivalentDateRule(dstMonth, dstWeekInMonth, dstDayOfWeek, finalDstRule.getRule())) {
                    writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dstMonth, dstWeekInMonth, dstDayOfWeek, dstStartTime, MAX_TIME);
                } else {
                    writeZonePropsByDOW(w, true, dstName, dstFromOffset, dstToOffset, dstMonth, dstWeekInMonth, dstDayOfWeek, dstStartTime, dstUntilTime);
                    Date nextStart = finalDstRule.getNextStart(dstUntilTime, dstFromOffset - dstFromDSTSavings, dstFromDSTSavings, false);
                    if (!f119assertionsDisabled) {
                        if (!(nextStart != null)) {
                            throw new AssertionError();
                        }
                    }
                    if (nextStart != null) {
                        writeFinalRule(w, true, finalDstRule, dstFromOffset - dstFromDSTSavings, dstFromDSTSavings, nextStart.getTime());
                    }
                }
            }
            if (stdCount > 0) {
                if (finalStdRule == null) {
                    if (stdCount == 1) {
                        writeZonePropsByTime(w, false, stdName, stdFromOffset, stdToOffset, stdStartTime, true);
                    } else {
                        writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdMonth, stdWeekInMonth, stdDayOfWeek, stdStartTime, stdUntilTime);
                    }
                } else if (stdCount == 1) {
                    writeFinalRule(w, false, finalStdRule, stdFromOffset - stdFromDSTSavings, stdFromDSTSavings, stdStartTime);
                } else if (isEquivalentDateRule(stdMonth, stdWeekInMonth, stdDayOfWeek, finalStdRule.getRule())) {
                    writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdMonth, stdWeekInMonth, stdDayOfWeek, stdStartTime, MAX_TIME);
                } else {
                    writeZonePropsByDOW(w, false, stdName, stdFromOffset, stdToOffset, stdMonth, stdWeekInMonth, stdDayOfWeek, stdStartTime, stdUntilTime);
                    Date nextStart2 = finalStdRule.getNextStart(stdUntilTime, stdFromOffset - stdFromDSTSavings, stdFromDSTSavings, false);
                    if (!f119assertionsDisabled) {
                        if (!(nextStart2 != null)) {
                            throw new AssertionError();
                        }
                    }
                    if (nextStart2 != null) {
                        writeFinalRule(w, false, finalStdRule, stdFromOffset - stdFromDSTSavings, stdFromDSTSavings, nextStart2.getTime());
                    }
                }
            }
        }
        writeFooter(w);
    }

    private static boolean isEquivalentDateRule(int month, int weekInMonth, int dayOfWeek, DateTimeRule dtrule) {
        if (month != dtrule.getRuleMonth() || dayOfWeek != dtrule.getRuleDayOfWeek() || dtrule.getTimeRuleType() != 0) {
            return false;
        }
        if (dtrule.getDateRuleType() == 1 && dtrule.getRuleWeekInMonth() == weekInMonth) {
            return true;
        }
        int ruleDOM = dtrule.getRuleDayOfMonth();
        if (dtrule.getDateRuleType() == 2) {
            if (ruleDOM % 7 == 1 && (ruleDOM + 6) / 7 == weekInMonth) {
                return true;
            }
            if (month != 1 && (MONTHLENGTH[month] - ruleDOM) % 7 == 6 && weekInMonth == (((MONTHLENGTH[month] - ruleDOM) + 1) / 7) * (-1)) {
                return true;
            }
        }
        if (dtrule.getDateRuleType() == 3) {
            if (ruleDOM % 7 == 0 && ruleDOM / 7 == weekInMonth) {
                return true;
            }
            if (month != 1 && (MONTHLENGTH[month] - ruleDOM) % 7 == 0 && weekInMonth == (((MONTHLENGTH[month] - ruleDOM) / 7) + 1) * (-1)) {
                return true;
            }
        }
        return false;
    }

    private static void writeZonePropsByTime(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, long time, boolean withRDATE) throws IOException {
        beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, time);
        if (withRDATE) {
            writer.write(ICAL_RDATE);
            writer.write(COLON);
            writer.write(getDateTimeString(((long) fromOffset) + time));
            writer.write(NEWLINE);
        }
        endZoneProps(writer, isDst);
    }

    private static void writeZonePropsByDOM(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int dayOfMonth, long startTime, long untilTime) throws IOException {
        beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, startTime);
        beginRRULE(writer, month);
        writer.write(ICAL_BYMONTHDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(dayOfMonth));
        if (untilTime != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(((long) fromOffset) + untilTime));
        }
        writer.write(NEWLINE);
        endZoneProps(writer, isDst);
    }

    private static void writeZonePropsByDOW(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int weekInMonth, int dayOfWeek, long startTime, long untilTime) throws IOException {
        beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, startTime);
        beginRRULE(writer, month);
        writer.write(ICAL_BYDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(weekInMonth));
        writer.write(ICAL_DOW_NAMES[dayOfWeek - 1]);
        if (untilTime != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(((long) fromOffset) + untilTime));
        }
        writer.write(NEWLINE);
        endZoneProps(writer, isDst);
    }

    private static void writeZonePropsByDOW_GEQ_DOM(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int dayOfMonth, int dayOfWeek, long startTime, long untilTime) throws IOException {
        if (dayOfMonth % 7 == 1) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, month, (dayOfMonth + 6) / 7, dayOfWeek, startTime, untilTime);
            return;
        }
        if (month != 1 && (MONTHLENGTH[month] - dayOfMonth) % 7 == 6) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, month, (((MONTHLENGTH[month] - dayOfMonth) + 1) / 7) * (-1), dayOfWeek, startTime, untilTime);
            return;
        }
        beginZoneProps(writer, isDst, tzname, fromOffset, toOffset, startTime);
        int startDay = dayOfMonth;
        int currentMonthDays = 7;
        if (dayOfMonth <= 0) {
            int prevMonthDays = 1 - dayOfMonth;
            currentMonthDays = 7 - prevMonthDays;
            int prevMonth = month + (-1) < 0 ? 11 : month - 1;
            writeZonePropsByDOW_GEQ_DOM_sub(writer, prevMonth, -prevMonthDays, dayOfWeek, prevMonthDays, MAX_TIME, fromOffset);
            startDay = 1;
        } else if (dayOfMonth + 6 > MONTHLENGTH[month]) {
            int nextMonthDays = (dayOfMonth + 6) - MONTHLENGTH[month];
            currentMonthDays = 7 - nextMonthDays;
            int nextMonth = month + 1 > 11 ? 0 : month + 1;
            writeZonePropsByDOW_GEQ_DOM_sub(writer, nextMonth, 1, dayOfWeek, nextMonthDays, MAX_TIME, fromOffset);
        }
        writeZonePropsByDOW_GEQ_DOM_sub(writer, month, startDay, dayOfWeek, currentMonthDays, untilTime, fromOffset);
        endZoneProps(writer, isDst);
    }

    private static void writeZonePropsByDOW_GEQ_DOM_sub(Writer writer, int month, int dayOfMonth, int dayOfWeek, int numDays, long untilTime, int fromOffset) throws IOException {
        int startDayNum = dayOfMonth;
        boolean isFeb = month == 1;
        if (dayOfMonth < 0 && !isFeb) {
            startDayNum = MONTHLENGTH[month] + dayOfMonth + 1;
        }
        beginRRULE(writer, month);
        writer.write(ICAL_BYDAY);
        writer.write(EQUALS_SIGN);
        writer.write(ICAL_DOW_NAMES[dayOfWeek - 1]);
        writer.write(SEMICOLON);
        writer.write(ICAL_BYMONTHDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(startDayNum));
        for (int i = 1; i < numDays; i++) {
            writer.write(COMMA);
            writer.write(Integer.toString(startDayNum + i));
        }
        if (untilTime != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(((long) fromOffset) + untilTime));
        }
        writer.write(NEWLINE);
    }

    private static void writeZonePropsByDOW_LEQ_DOM(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, int month, int dayOfMonth, int dayOfWeek, long startTime, long untilTime) throws IOException {
        if (dayOfMonth % 7 == 0) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, month, dayOfMonth / 7, dayOfWeek, startTime, untilTime);
            return;
        }
        if (month != 1 && (MONTHLENGTH[month] - dayOfMonth) % 7 == 0) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, month, (((MONTHLENGTH[month] - dayOfMonth) / 7) + 1) * (-1), dayOfWeek, startTime, untilTime);
        } else if (month == 1 && dayOfMonth == 29) {
            writeZonePropsByDOW(writer, isDst, tzname, fromOffset, toOffset, 1, -1, dayOfWeek, startTime, untilTime);
        } else {
            writeZonePropsByDOW_GEQ_DOM(writer, isDst, tzname, fromOffset, toOffset, month, dayOfMonth - 6, dayOfWeek, startTime, untilTime);
        }
    }

    private static void writeFinalRule(Writer writer, boolean isDst, AnnualTimeZoneRule rule, int fromRawOffset, int fromDSTSavings, long startTime) throws IOException {
        DateTimeRule dtrule = toWallTimeRule(rule.getRule(), fromRawOffset, fromDSTSavings);
        int timeInDay = dtrule.getRuleMillisInDay();
        if (timeInDay < 0) {
            startTime += (long) (0 - timeInDay);
        } else if (timeInDay >= 86400000) {
            startTime -= (long) (timeInDay - 86399999);
        }
        int toOffset = rule.getRawOffset() + rule.getDSTSavings();
        switch (dtrule.getDateRuleType()) {
            case 0:
                writeZonePropsByDOM(writer, isDst, rule.getName(), fromRawOffset + fromDSTSavings, toOffset, dtrule.getRuleMonth(), dtrule.getRuleDayOfMonth(), startTime, MAX_TIME);
                break;
            case 1:
                writeZonePropsByDOW(writer, isDst, rule.getName(), fromRawOffset + fromDSTSavings, toOffset, dtrule.getRuleMonth(), dtrule.getRuleWeekInMonth(), dtrule.getRuleDayOfWeek(), startTime, MAX_TIME);
                break;
            case 2:
                writeZonePropsByDOW_GEQ_DOM(writer, isDst, rule.getName(), fromRawOffset + fromDSTSavings, toOffset, dtrule.getRuleMonth(), dtrule.getRuleDayOfMonth(), dtrule.getRuleDayOfWeek(), startTime, MAX_TIME);
                break;
            case 3:
                writeZonePropsByDOW_LEQ_DOM(writer, isDst, rule.getName(), fromRawOffset + fromDSTSavings, toOffset, dtrule.getRuleMonth(), dtrule.getRuleDayOfMonth(), dtrule.getRuleDayOfWeek(), startTime, MAX_TIME);
                break;
        }
    }

    private static DateTimeRule toWallTimeRule(DateTimeRule rule, int rawOffset, int dstSavings) {
        if (rule.getTimeRuleType() == 0) {
            return rule;
        }
        int wallt = rule.getRuleMillisInDay();
        if (rule.getTimeRuleType() == 2) {
            wallt += rawOffset + dstSavings;
        } else if (rule.getTimeRuleType() == 1) {
            wallt += dstSavings;
        }
        int dshift = 0;
        if (wallt < 0) {
            dshift = -1;
            wallt += Grego.MILLIS_PER_DAY;
        } else if (wallt >= 86400000) {
            dshift = 1;
            wallt -= Grego.MILLIS_PER_DAY;
        }
        int month = rule.getRuleMonth();
        int dom = rule.getRuleDayOfMonth();
        int dow = rule.getRuleDayOfWeek();
        int dtype = rule.getDateRuleType();
        if (dshift != 0) {
            if (dtype == 1) {
                int wim = rule.getRuleWeekInMonth();
                if (wim > 0) {
                    dtype = 2;
                    dom = ((wim - 1) * 7) + 1;
                } else {
                    dtype = 3;
                    dom = MONTHLENGTH[month] + ((wim + 1) * 7);
                }
            }
            dom += dshift;
            if (dom == 0) {
                month--;
                if (month < 0) {
                    month = 11;
                }
                dom = MONTHLENGTH[month];
            } else if (dom > MONTHLENGTH[month]) {
                month++;
                if (month > 11) {
                    month = 0;
                }
                dom = 1;
            }
            if (dtype != 0) {
                dow += dshift;
                if (dow < 1) {
                    dow = 7;
                } else if (dow > 7) {
                    dow = 1;
                }
            }
        }
        if (dtype == 0) {
            DateTimeRule modifiedRule = new DateTimeRule(month, dom, wallt, 0);
            return modifiedRule;
        }
        DateTimeRule modifiedRule2 = new DateTimeRule(month, dom, dow, dtype == 2, wallt, 0);
        return modifiedRule2;
    }

    private static void beginZoneProps(Writer writer, boolean isDst, String tzname, int fromOffset, int toOffset, long startTime) throws IOException {
        writer.write(ICAL_BEGIN);
        writer.write(COLON);
        if (isDst) {
            writer.write(ICAL_DAYLIGHT);
        } else {
            writer.write(ICAL_STANDARD);
        }
        writer.write(NEWLINE);
        writer.write(ICAL_TZOFFSETTO);
        writer.write(COLON);
        writer.write(millisToOffset(toOffset));
        writer.write(NEWLINE);
        writer.write(ICAL_TZOFFSETFROM);
        writer.write(COLON);
        writer.write(millisToOffset(fromOffset));
        writer.write(NEWLINE);
        writer.write(ICAL_TZNAME);
        writer.write(COLON);
        writer.write(tzname);
        writer.write(NEWLINE);
        writer.write(ICAL_DTSTART);
        writer.write(COLON);
        writer.write(getDateTimeString(((long) fromOffset) + startTime));
        writer.write(NEWLINE);
    }

    private static void endZoneProps(Writer writer, boolean isDst) throws IOException {
        writer.write(ICAL_END);
        writer.write(COLON);
        if (isDst) {
            writer.write(ICAL_DAYLIGHT);
        } else {
            writer.write(ICAL_STANDARD);
        }
        writer.write(NEWLINE);
    }

    private static void beginRRULE(Writer writer, int month) throws IOException {
        writer.write(ICAL_RRULE);
        writer.write(COLON);
        writer.write(ICAL_FREQ);
        writer.write(EQUALS_SIGN);
        writer.write(ICAL_YEARLY);
        writer.write(SEMICOLON);
        writer.write(ICAL_BYMONTH);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(month + 1));
        writer.write(SEMICOLON);
    }

    private static void appendUNTIL(Writer writer, String until) throws IOException {
        if (until == null) {
            return;
        }
        writer.write(SEMICOLON);
        writer.write(ICAL_UNTIL);
        writer.write(EQUALS_SIGN);
        writer.write(until);
    }

    private void writeHeader(Writer writer) throws IOException {
        writer.write(ICAL_BEGIN);
        writer.write(COLON);
        writer.write(ICAL_VTIMEZONE);
        writer.write(NEWLINE);
        writer.write(ICAL_TZID);
        writer.write(COLON);
        writer.write(this.tz.getID());
        writer.write(NEWLINE);
        if (this.tzurl != null) {
            writer.write(ICAL_TZURL);
            writer.write(COLON);
            writer.write(this.tzurl);
            writer.write(NEWLINE);
        }
        if (this.lastmod == null) {
            return;
        }
        writer.write(ICAL_LASTMOD);
        writer.write(COLON);
        writer.write(getUTCDateTimeString(this.lastmod.getTime()));
        writer.write(NEWLINE);
    }

    private static void writeFooter(Writer writer) throws IOException {
        writer.write(ICAL_END);
        writer.write(COLON);
        writer.write(ICAL_VTIMEZONE);
        writer.write(NEWLINE);
    }

    private static String getDateTimeString(long time) {
        int[] fields = Grego.timeToFields(time, null);
        StringBuilder sb = new StringBuilder(15);
        sb.append(numToString(fields[0], 4));
        sb.append(numToString(fields[1] + 1, 2));
        sb.append(numToString(fields[2], 2));
        sb.append('T');
        int t = fields[5];
        int hour = t / 3600000;
        int t2 = t % 3600000;
        int min = t2 / 60000;
        int sec = (t2 % 60000) / 1000;
        sb.append(numToString(hour, 2));
        sb.append(numToString(min, 2));
        sb.append(numToString(sec, 2));
        return sb.toString();
    }

    private static String getUTCDateTimeString(long time) {
        return getDateTimeString(time) + "Z";
    }

    private static long parseDateTimeString(String str, int offset) {
        int length;
        int year = 0;
        int month = 0;
        int day = 0;
        int hour = 0;
        int min = 0;
        int sec = 0;
        boolean isUTC = false;
        boolean isValid = false;
        if (str != null && (((length = str.length()) == 15 || length == 16) && str.charAt(8) == 'T')) {
            if (length == 16) {
                if (str.charAt(15) == 'Z') {
                    isUTC = true;
                    year = Integer.parseInt(str.substring(0, 4));
                    month = Integer.parseInt(str.substring(4, 6)) - 1;
                    day = Integer.parseInt(str.substring(6, 8));
                    hour = Integer.parseInt(str.substring(9, 11));
                    min = Integer.parseInt(str.substring(11, 13));
                    sec = Integer.parseInt(str.substring(13, 15));
                    int maxDayOfMonth = Grego.monthLength(year, month);
                    if (year >= 0) {
                        isValid = true;
                    }
                }
            } else {
                try {
                    year = Integer.parseInt(str.substring(0, 4));
                    month = Integer.parseInt(str.substring(4, 6)) - 1;
                    day = Integer.parseInt(str.substring(6, 8));
                    hour = Integer.parseInt(str.substring(9, 11));
                    min = Integer.parseInt(str.substring(11, 13));
                    sec = Integer.parseInt(str.substring(13, 15));
                    int maxDayOfMonth2 = Grego.monthLength(year, month);
                    if (year >= 0 && month >= 0 && month <= 11 && day >= 1 && day <= maxDayOfMonth2 && hour >= 0 && hour < 24 && min >= 0 && min < 60 && sec >= 0 && sec < 60) {
                        isValid = true;
                    }
                } catch (NumberFormatException e) {
                }
            }
        }
        if (!isValid) {
            throw new IllegalArgumentException("Invalid date time string format");
        }
        long time = (Grego.fieldsToDay(year, month, day) * 86400000) + ((long) ((3600000 * hour) + (60000 * min) + (sec * 1000)));
        if (!isUTC) {
            return time - ((long) offset);
        }
        return time;
    }

    private static int offsetStrToMillis(String str) {
        int length;
        boolean isValid = false;
        int sign = 0;
        int hour = 0;
        int min = 0;
        if (str != null && ((length = str.length()) == 5 || length == 7)) {
            char s = str.charAt(0);
            if (s == '+') {
                sign = 1;
            } else if (s == '-') {
                sign = -1;
            }
            try {
                hour = Integer.parseInt(str.substring(1, 3));
                min = Integer.parseInt(str.substring(3, 5));
                sec = length == 7 ? Integer.parseInt(str.substring(5, 7)) : 0;
                isValid = true;
            } catch (NumberFormatException e) {
            }
        }
        if (!isValid) {
            throw new IllegalArgumentException("Bad offset string");
        }
        int millis = ((((hour * 60) + min) * 60) + sec) * sign * 1000;
        return millis;
    }

    private static String millisToOffset(int millis) {
        StringBuilder sb = new StringBuilder(7);
        if (millis >= 0) {
            sb.append('+');
        } else {
            sb.append('-');
            millis = -millis;
        }
        int t = millis / 1000;
        int sec = t % 60;
        int t2 = (t - sec) / 60;
        int min = t2 % 60;
        int hour = t2 / 60;
        sb.append(numToString(hour, 2));
        sb.append(numToString(min, 2));
        sb.append(numToString(sec, 2));
        return sb.toString();
    }

    private static String numToString(int num, int width) {
        String str = Integer.toString(num);
        int len = str.length();
        if (len >= width) {
            return str.substring(len - width, len);
        }
        StringBuilder sb = new StringBuilder(width);
        for (int i = len; i < width; i++) {
            sb.append('0');
        }
        sb.append(str);
        return sb.toString();
    }

    @Override
    public boolean isFrozen() {
        return this.isFrozen;
    }

    @Override
    public TimeZone freeze() {
        this.isFrozen = true;
        return this;
    }

    @Override
    public TimeZone cloneAsThawed() {
        VTimeZone vtz = (VTimeZone) super.cloneAsThawed();
        vtz.tz = (BasicTimeZone) this.tz.cloneAsThawed();
        vtz.isFrozen = false;
        return vtz;
    }
}
