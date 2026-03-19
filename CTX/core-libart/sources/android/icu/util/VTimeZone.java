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
    static final boolean $assertionsDisabled = false;
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
    private static final String[] ICAL_DOW_NAMES = {"SU", "MO", "TU", "WE", "TH", "FR", "SA"};
    private static final int[] MONTHLENGTH = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};

    static {
        try {
            ICU_TZVERSION = TimeZone.getTZDataVersion();
        } catch (MissingResourceException e) {
            ICU_TZVERSION = null;
        }
    }

    public static VTimeZone create(String str) {
        BasicTimeZone frozenICUTimeZone = TimeZone.getFrozenICUTimeZone(str, true);
        if (frozenICUTimeZone == null) {
            return null;
        }
        VTimeZone vTimeZone = new VTimeZone(str);
        vTimeZone.tz = (BasicTimeZone) frozenICUTimeZone.cloneAsThawed();
        vTimeZone.olsonzid = vTimeZone.tz.getID();
        return vTimeZone;
    }

    public static VTimeZone create(Reader reader) {
        VTimeZone vTimeZone = new VTimeZone();
        if (vTimeZone.load(reader)) {
            return vTimeZone;
        }
        return null;
    }

    @Override
    public int getOffset(int i, int i2, int i3, int i4, int i5, int i6) {
        return this.tz.getOffset(i, i2, i3, i4, i5, i6);
    }

    @Override
    public void getOffset(long j, boolean z, int[] iArr) {
        this.tz.getOffset(j, z, iArr);
    }

    @Override
    @Deprecated
    public void getOffsetFromLocal(long j, int i, int i2, int[] iArr) {
        this.tz.getOffsetFromLocal(j, i, i2, iArr);
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
    public void setRawOffset(int i) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.tz.setRawOffset(i);
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
            return this.tz.hasSameRules(((VTimeZone) timeZone).tz);
        }
        return this.tz.hasSameRules(timeZone);
    }

    public String getTZURL() {
        return this.tzurl;
    }

    public void setTZURL(String str) {
        if (isFrozen()) {
            throw new UnsupportedOperationException("Attempt to modify a frozen VTimeZone instance.");
        }
        this.tzurl = str;
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
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        if (this.vtzlines != null) {
            for (String str : this.vtzlines) {
                if (str.startsWith("TZURL:")) {
                    if (this.tzurl != null) {
                        bufferedWriter.write(ICAL_TZURL);
                        bufferedWriter.write(COLON);
                        bufferedWriter.write(this.tzurl);
                        bufferedWriter.write(NEWLINE);
                    }
                } else if (str.startsWith("LAST-MODIFIED:")) {
                    if (this.lastmod != null) {
                        bufferedWriter.write(ICAL_LASTMOD);
                        bufferedWriter.write(COLON);
                        bufferedWriter.write(getUTCDateTimeString(this.lastmod.getTime()));
                        bufferedWriter.write(NEWLINE);
                    }
                } else {
                    bufferedWriter.write(str);
                    bufferedWriter.write(NEWLINE);
                }
            }
            bufferedWriter.flush();
            return;
        }
        String[] strArr = null;
        if (this.olsonzid != null && ICU_TZVERSION != null) {
            strArr = new String[]{"X-TZINFO:" + this.olsonzid + "[" + ICU_TZVERSION + "]"};
        }
        writeZone(writer, this.tz, strArr);
    }

    public void write(Writer writer, long j) throws IOException {
        TimeZoneRule[] timeZoneRules = this.tz.getTimeZoneRules(j);
        RuleBasedTimeZone ruleBasedTimeZone = new RuleBasedTimeZone(this.tz.getID(), (InitialTimeZoneRule) timeZoneRules[0]);
        for (int i = 1; i < timeZoneRules.length; i++) {
            ruleBasedTimeZone.addTransitionRule(timeZoneRules[i]);
        }
        String[] strArr = null;
        if (this.olsonzid != null && ICU_TZVERSION != null) {
            strArr = new String[]{"X-TZINFO:" + this.olsonzid + "[" + ICU_TZVERSION + "/Partial@" + j + "]"};
        }
        writeZone(writer, ruleBasedTimeZone, strArr);
    }

    public void writeSimple(Writer writer, long j) throws IOException {
        TimeZoneRule[] simpleTimeZoneRulesNear = this.tz.getSimpleTimeZoneRulesNear(j);
        RuleBasedTimeZone ruleBasedTimeZone = new RuleBasedTimeZone(this.tz.getID(), (InitialTimeZoneRule) simpleTimeZoneRulesNear[0]);
        for (int i = 1; i < simpleTimeZoneRulesNear.length; i++) {
            ruleBasedTimeZone.addTransitionRule(simpleTimeZoneRulesNear[i]);
        }
        String[] strArr = null;
        if (this.olsonzid != null && ICU_TZVERSION != null) {
            strArr = new String[]{"X-TZINFO:" + this.olsonzid + "[" + ICU_TZVERSION + "/Simple@" + j + "]"};
        }
        writeZone(writer, ruleBasedTimeZone, strArr);
    }

    @Override
    public TimeZoneTransition getNextTransition(long j, boolean z) {
        return this.tz.getNextTransition(j, z);
    }

    @Override
    public TimeZoneTransition getPreviousTransition(long j, boolean z) {
        return this.tz.getPreviousTransition(j, z);
    }

    @Override
    public boolean hasEquivalentTransitions(TimeZone timeZone, long j, long j2) {
        if (this == timeZone) {
            return true;
        }
        return this.tz.hasEquivalentTransitions(timeZone, j, j2);
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules() {
        return this.tz.getTimeZoneRules();
    }

    @Override
    public TimeZoneRule[] getTimeZoneRules(long j) {
        return this.tz.getTimeZoneRules(j);
    }

    @Override
    public Object clone() {
        if (isFrozen()) {
            return this;
        }
        return cloneAsThawed();
    }

    private VTimeZone() {
        this.olsonzid = null;
        this.tzurl = null;
        this.lastmod = null;
        this.isFrozen = false;
    }

    private VTimeZone(String str) {
        super(str);
        this.olsonzid = null;
        this.tzurl = null;
        this.lastmod = null;
        this.isFrozen = false;
    }

    private boolean load(Reader reader) {
        boolean z;
        try {
            this.vtzlines = new LinkedList();
            StringBuilder sb = new StringBuilder();
            boolean z2 = false;
            boolean z3 = false;
            while (true) {
                int i = reader.read();
                z = true;
                if (i == -1) {
                    if (z2 && sb.toString().startsWith(ICAL_END_VTIMEZONE)) {
                        this.vtzlines.add(sb.toString());
                    } else {
                        z = false;
                    }
                } else if (i != 13) {
                    if (z3) {
                        if (i != 9 && i != 32) {
                            if (z2 && sb.length() > 0) {
                                this.vtzlines.add(sb.toString());
                            }
                            sb.setLength(0);
                            if (i != 10) {
                                sb.append((char) i);
                            }
                        }
                        z3 = false;
                    } else if (i == 10) {
                        if (z2) {
                            if (sb.toString().startsWith(ICAL_END_VTIMEZONE)) {
                                this.vtzlines.add(sb.toString());
                                break;
                            }
                        } else if (sb.toString().startsWith(ICAL_BEGIN_VTIMEZONE)) {
                            this.vtzlines.add(sb.toString());
                            sb.setLength(0);
                            z3 = false;
                            z2 = true;
                        }
                        z3 = true;
                    } else {
                        sb.append((char) i);
                    }
                }
            }
            if (!z) {
                return false;
            }
            return parse();
        } catch (IOException e) {
            return false;
        }
    }

    private boolean parse() {
        boolean z;
        String str;
        String str2;
        String str3;
        TimeZoneRule timeZoneRuleCreateRuleByRDATE;
        int i;
        int i2;
        boolean z2 = false;
        if (this.vtzlines == null || this.vtzlines.size() == 0) {
            return false;
        }
        ArrayList arrayList = new ArrayList();
        Iterator<String> it = this.vtzlines.iterator();
        char c = 0;
        int i3 = 0;
        boolean z3 = false;
        boolean z4 = false;
        long j = Long.MAX_VALUE;
        String str4 = null;
        String str5 = null;
        String str6 = null;
        String str7 = null;
        String str8 = null;
        LinkedList linkedList = null;
        int i4 = 0;
        while (it.hasNext()) {
            String next = it.next();
            int iIndexOf = next.indexOf(COLON);
            if (iIndexOf >= 0) {
                Iterator<String> it2 = it;
                String strSubstring = next.substring(z2 ? 1 : 0, iIndexOf);
                String strSubstring2 = next.substring(iIndexOf + 1);
                switch (c) {
                    case 0:
                        z = z4;
                        str = str6;
                        str2 = str7;
                        str3 = str8;
                        if (strSubstring.equals(ICAL_BEGIN) && strSubstring2.equals(ICAL_VTIMEZONE)) {
                            z4 = z;
                            str6 = str;
                            str7 = str2;
                            str8 = str3;
                            c = 1;
                        } else {
                            z4 = z;
                            str6 = str;
                            str7 = str2;
                            str8 = str3;
                        }
                        break;
                    case 1:
                        z = z4;
                        str = str6;
                        str2 = str7;
                        str3 = str8;
                        if (strSubstring.equals(ICAL_TZID)) {
                            str4 = strSubstring2;
                        } else if (strSubstring.equals(ICAL_TZURL)) {
                            this.tzurl = strSubstring2;
                        } else if (strSubstring.equals(ICAL_LASTMOD)) {
                            this.lastmod = new Date(parseDateTimeString(strSubstring2, 0));
                        } else if (strSubstring.equals(ICAL_BEGIN)) {
                            boolean zEquals = strSubstring2.equals(ICAL_DAYLIGHT);
                            if ((strSubstring2.equals(ICAL_STANDARD) || zEquals) && str4 != null) {
                                z4 = zEquals;
                                str8 = str3;
                                c = 2;
                                z3 = false;
                                str5 = null;
                                str6 = null;
                                str7 = null;
                                linkedList = null;
                            }
                            z4 = z;
                            str6 = str;
                            str7 = str2;
                            str8 = str3;
                            c = 3;
                            break;
                        } else if (strSubstring.equals(ICAL_END)) {
                        }
                        z4 = z;
                        str6 = str;
                        str7 = str2;
                        str8 = str3;
                        break;
                    case 2:
                        if (strSubstring.equals(ICAL_DTSTART)) {
                            str8 = strSubstring2;
                            break;
                        } else if (strSubstring.equals(ICAL_TZNAME)) {
                            str5 = strSubstring2;
                            break;
                        } else if (strSubstring.equals(ICAL_TZOFFSETFROM)) {
                            str6 = strSubstring2;
                            break;
                        } else if (strSubstring.equals(ICAL_TZOFFSETTO)) {
                            str7 = strSubstring2;
                            break;
                        } else if (strSubstring.equals(ICAL_RDATE)) {
                            if (z3) {
                                c = 3;
                            } else {
                                LinkedList linkedList2 = linkedList == null ? new LinkedList() : linkedList;
                                java.util.StringTokenizer stringTokenizer = new java.util.StringTokenizer(strSubstring2, COMMA);
                                while (stringTokenizer.hasMoreTokens()) {
                                    linkedList2.add(stringTokenizer.nextToken());
                                }
                                linkedList = linkedList2;
                            }
                            break;
                        } else if (!strSubstring.equals(ICAL_RRULE)) {
                            if (strSubstring.equals(ICAL_END)) {
                                if (str8 == null || str6 == null || str7 == null) {
                                    z = z4;
                                    str = str6;
                                    str2 = str7;
                                    str3 = str8;
                                } else {
                                    String defaultTZName = str5 == null ? getDefaultTZName(str4, z4) : str5;
                                    try {
                                        int iOffsetStrToMillis = offsetStrToMillis(str6);
                                        int iOffsetStrToMillis2 = offsetStrToMillis(str7);
                                        if (z4) {
                                            int i5 = iOffsetStrToMillis2 - iOffsetStrToMillis;
                                            if (i5 > 0) {
                                                i = iOffsetStrToMillis;
                                                i2 = i5;
                                            } else {
                                                i2 = 3600000;
                                                i = iOffsetStrToMillis2 - 3600000;
                                            }
                                        } else {
                                            i = iOffsetStrToMillis2;
                                            i2 = 0;
                                        }
                                        long dateTimeString = parseDateTimeString(str8, iOffsetStrToMillis);
                                        if (z3) {
                                            z = z4;
                                            str = str6;
                                            str2 = str7;
                                            str3 = str8;
                                            try {
                                                timeZoneRuleCreateRuleByRDATE = createRuleByRRULE(defaultTZName, i, i2, dateTimeString, linkedList, iOffsetStrToMillis);
                                            } catch (IllegalArgumentException e) {
                                                timeZoneRuleCreateRuleByRDATE = null;
                                            }
                                        } else {
                                            z = z4;
                                            str = str6;
                                            str2 = str7;
                                            str3 = str8;
                                            timeZoneRuleCreateRuleByRDATE = createRuleByRDATE(defaultTZName, i, i2, dateTimeString, linkedList, iOffsetStrToMillis);
                                        }
                                        if (timeZoneRuleCreateRuleByRDATE != null) {
                                            try {
                                                Date firstStart = timeZoneRuleCreateRuleByRDATE.getFirstStart(iOffsetStrToMillis, 0);
                                                if (firstStart.getTime() < j) {
                                                    long time = firstStart.getTime();
                                                    if (i2 <= 0 && iOffsetStrToMillis - iOffsetStrToMillis2 == 3600000) {
                                                        i4 = iOffsetStrToMillis - 3600000;
                                                        i3 = 3600000;
                                                        j = time;
                                                    } else {
                                                        i4 = iOffsetStrToMillis;
                                                        j = time;
                                                        i3 = 0;
                                                    }
                                                }
                                            } catch (IllegalArgumentException e2) {
                                            }
                                        }
                                    } catch (IllegalArgumentException e3) {
                                        z = z4;
                                        str = str6;
                                        str2 = str7;
                                        str3 = str8;
                                    }
                                    if (timeZoneRuleCreateRuleByRDATE != null) {
                                        arrayList.add(timeZoneRuleCreateRuleByRDATE);
                                        str5 = defaultTZName;
                                        z4 = z;
                                        str6 = str;
                                        str7 = str2;
                                        str8 = str3;
                                        c = 1;
                                    } else {
                                        str5 = defaultTZName;
                                    }
                                }
                                z4 = z;
                                str6 = str;
                                str7 = str2;
                                str8 = str3;
                                c = 3;
                            }
                            break;
                        } else if (z3 || linkedList == null) {
                            LinkedList linkedList3 = linkedList == null ? new LinkedList() : linkedList;
                            linkedList3.add(strSubstring2);
                            linkedList = linkedList3;
                            z3 = true;
                            break;
                        }
                    default:
                        z = z4;
                        str = str6;
                        str2 = str7;
                        str3 = str8;
                        z4 = z;
                        str6 = str;
                        str7 = str2;
                        str8 = str3;
                        break;
                }
                if (c == 3) {
                    this.vtzlines = null;
                    return false;
                }
                z2 = false;
                it = it2;
            }
        }
        if (arrayList.size() == 0) {
            return z2;
        }
        RuleBasedTimeZone ruleBasedTimeZone = new RuleBasedTimeZone(str4, new InitialTimeZoneRule(getDefaultTZName(str4, z2), i4, i3));
        int i6 = -1;
        int i7 = 0;
        for (int i8 = 0; i8 < arrayList.size(); i8++) {
            TimeZoneRule timeZoneRule = (TimeZoneRule) arrayList.get(i8);
            if ((timeZoneRule instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) timeZoneRule).getEndYear() == Integer.MAX_VALUE) {
                i7++;
                i6 = i8;
            }
        }
        if (i7 > 2) {
            return false;
        }
        if (i7 == 1) {
            if (arrayList.size() == 1) {
                arrayList.clear();
            } else {
                AnnualTimeZoneRule annualTimeZoneRule = (AnnualTimeZoneRule) arrayList.get(i6);
                int rawOffset = annualTimeZoneRule.getRawOffset();
                int dSTSavings = annualTimeZoneRule.getDSTSavings();
                Date firstStart2 = annualTimeZoneRule.getFirstStart(i4, i3);
                Date nextStart = firstStart2;
                for (int i9 = 0; i9 < arrayList.size(); i9++) {
                    if (i6 != i9) {
                        TimeZoneRule timeZoneRule2 = (TimeZoneRule) arrayList.get(i9);
                        Date finalStart = timeZoneRule2.getFinalStart(rawOffset, dSTSavings);
                        if (finalStart.after(nextStart)) {
                            nextStart = annualTimeZoneRule.getNextStart(finalStart.getTime(), timeZoneRule2.getRawOffset(), timeZoneRule2.getDSTSavings(), false);
                        }
                    }
                }
                arrayList.set(i6, nextStart == firstStart2 ? new TimeArrayTimeZoneRule(annualTimeZoneRule.getName(), annualTimeZoneRule.getRawOffset(), annualTimeZoneRule.getDSTSavings(), new long[]{firstStart2.getTime()}, 2) : new AnnualTimeZoneRule(annualTimeZoneRule.getName(), annualTimeZoneRule.getRawOffset(), annualTimeZoneRule.getDSTSavings(), annualTimeZoneRule.getRule(), annualTimeZoneRule.getStartYear(), Grego.timeToFields(nextStart.getTime(), null)[0]));
            }
        }
        Iterator it3 = arrayList.iterator();
        while (it3.hasNext()) {
            ruleBasedTimeZone.addTransitionRule((TimeZoneRule) it3.next());
        }
        this.tz = ruleBasedTimeZone;
        setID(str4);
        return true;
    }

    private static String getDefaultTZName(String str, boolean z) {
        if (z) {
            return str + "(DST)";
        }
        return str + "(STD)";
    }

    private static TimeZoneRule createRuleByRRULE(String str, int i, int i2, long j, List<String> list, int i3) {
        int i4;
        int[] iArr;
        int length;
        int i5;
        DateTimeRule dateTimeRule;
        boolean z;
        if (list == null || list.size() == 0) {
            return null;
        }
        long[] jArr = new long[1];
        int[] rrule = parseRRULE(list.get(0), jArr);
        if (rrule == null) {
            return null;
        }
        int i6 = rrule[0];
        int i7 = rrule[1];
        int i8 = rrule[2];
        int i9 = 3;
        int i10 = rrule[3];
        int i11 = -1;
        if (list.size() == 1) {
            if (rrule.length > 4) {
                if (rrule.length != 10 || i6 == -1 || i7 == 0) {
                    return null;
                }
                int[] iArr2 = new int[7];
                i4 = 31;
                for (int i12 = 0; i12 < 7; i12++) {
                    iArr2[i12] = rrule[3 + i12];
                    iArr2[i12] = iArr2[i12] > 0 ? iArr2[i12] : MONTHLENGTH[i6] + iArr2[i12] + 1;
                    if (iArr2[i12] < i4) {
                        i4 = iArr2[i12];
                    }
                }
                for (int i13 = 1; i13 < 7; i13++) {
                    int i14 = 0;
                    while (true) {
                        if (i14 < 7) {
                            if (iArr2[i14] != i4 + i13) {
                                i14++;
                            } else {
                                z = true;
                                break;
                            }
                        } else {
                            z = false;
                            break;
                        }
                    }
                    if (!z) {
                        return null;
                    }
                }
                iArr = null;
            } else {
                iArr = null;
                i4 = i10;
            }
        } else {
            if (i6 == -1 || i7 == 0 || i10 == 0) {
                return null;
            }
            if (list.size() > 7) {
                return null;
            }
            int length2 = rrule.length - 3;
            i4 = 31;
            for (int i15 = 0; i15 < length2; i15++) {
                int i16 = rrule[3 + i15];
                if (i16 <= 0) {
                    i16 = MONTHLENGTH[i6] + i16 + 1;
                }
                int i17 = i16;
                if (i17 < i4) {
                    i4 = i17;
                }
            }
            int i18 = 1;
            int i19 = i6;
            int i20 = -1;
            while (i18 < list.size()) {
                long[] jArr2 = new long[1];
                int[] rrule2 = parseRRULE(list.get(i18), jArr2);
                if (jArr2[0] > jArr[0]) {
                    jArr = jArr2;
                }
                if (rrule2[0] == i11 || rrule2[1] == 0 || rrule2[i9] == 0 || (length2 = length2 + (length = rrule2.length - i9)) > 7 || rrule2[1] != i7) {
                    return null;
                }
                if (rrule2[0] != i6) {
                    if (i20 == -1) {
                        int i21 = rrule2[0] - i6;
                        if (i21 == -11 || i21 == -1) {
                            i5 = rrule2[0];
                            i19 = i5;
                            i4 = 31;
                        } else if (i21 == 11 || i21 == 1) {
                            i5 = rrule2[0];
                        } else {
                            return null;
                        }
                        i20 = i5;
                    } else if (rrule2[0] != i6 && rrule2[0] != i20) {
                        return null;
                    }
                }
                if (rrule2[0] == i19) {
                    for (int i22 = 0; i22 < length; i22++) {
                        int i23 = rrule2[3 + i22];
                        if (i23 <= 0) {
                            i23 = MONTHLENGTH[rrule2[0]] + i23 + 1;
                        }
                        int i24 = i23;
                        if (i24 < i4) {
                            i4 = i24;
                        }
                    }
                }
                i18++;
                i9 = 3;
                i11 = -1;
            }
            iArr = null;
            if (length2 != 7) {
                return null;
            }
            i6 = i19;
        }
        int[] iArrTimeToFields = Grego.timeToFields(j + ((long) i3), iArr);
        int i25 = iArrTimeToFields[0];
        int i26 = i6 == -1 ? iArrTimeToFields[1] : i6;
        if (i7 == 0 && i8 == 0 && i4 == 0) {
            i4 = iArrTimeToFields[2];
        }
        int i27 = iArrTimeToFields[5];
        int i28 = Integer.MAX_VALUE;
        if (jArr[0] != MIN_TIME) {
            Grego.timeToFields(jArr[0], iArrTimeToFields);
            i28 = iArrTimeToFields[0];
        }
        int i29 = i28;
        if (i7 == 0 && i8 == 0 && i4 != 0) {
            dateTimeRule = new DateTimeRule(i26, i4, i27, 0);
        } else if (i7 != 0 && i8 != 0 && i4 == 0) {
            dateTimeRule = new DateTimeRule(i26, i8, i7, i27, 0);
        } else if (i7 != 0 && i8 == 0 && i4 != 0) {
            dateTimeRule = new DateTimeRule(i26, i4, i7, true, i27, 0);
        } else {
            return null;
        }
        return new AnnualTimeZoneRule(str, i, i2, dateTimeRule, i25, i29);
    }

    private static int[] parseRRULE(String str, long[] jArr) {
        int[] iArr;
        int i;
        int i2;
        int i3;
        int i4;
        java.util.StringTokenizer stringTokenizer = new java.util.StringTokenizer(str, SEMICOLON);
        int i5 = -1;
        boolean z = false;
        int i6 = 0;
        int i7 = 0;
        boolean z2 = false;
        long dateTimeString = Long.MIN_VALUE;
        int[] iArr2 = null;
        int i8 = -1;
        while (stringTokenizer.hasMoreTokens()) {
            String strNextToken = stringTokenizer.nextToken();
            int iIndexOf = strNextToken.indexOf(EQUALS_SIGN);
            if (iIndexOf != i5) {
                String strSubstring = strNextToken.substring(0, iIndexOf);
                String strSubstring2 = strNextToken.substring(iIndexOf + 1);
                if (strSubstring.equals(ICAL_FREQ)) {
                    if (strSubstring2.equals(ICAL_YEARLY)) {
                        z2 = true;
                        i5 = -1;
                    }
                } else if (strSubstring.equals(ICAL_UNTIL)) {
                    try {
                        dateTimeString = parseDateTimeString(strSubstring2, 0);
                        i5 = -1;
                    } catch (IllegalArgumentException e) {
                    }
                } else if (!strSubstring.equals(ICAL_BYMONTH)) {
                    if (strSubstring.equals(ICAL_BYDAY)) {
                        int length = strSubstring2.length();
                        if (length >= 2 && length <= 4) {
                            if (length <= 2) {
                                i = 0;
                                while (i < ICAL_DOW_NAMES.length && !strSubstring2.equals(ICAL_DOW_NAMES[i])) {
                                    i++;
                                }
                                if (i >= ICAL_DOW_NAMES.length) {
                                    i6 = i + 1;
                                }
                            } else if (strSubstring2.charAt(0) == '+') {
                                i2 = 1;
                                int i9 = length - 3;
                                i3 = length - 2;
                                i4 = Integer.parseInt(strSubstring2.substring(i9, i3));
                                if (i4 != 0) {
                                }
                            } else if (strSubstring2.charAt(0) == '-') {
                                i2 = -1;
                                int i92 = length - 3;
                                i3 = length - 2;
                                try {
                                    i4 = Integer.parseInt(strSubstring2.substring(i92, i3));
                                    if (i4 != 0 && i4 <= 4) {
                                        strSubstring2 = strSubstring2.substring(i3);
                                        i7 = i4 * i2;
                                        i = 0;
                                        while (i < ICAL_DOW_NAMES.length) {
                                            i++;
                                        }
                                        if (i >= ICAL_DOW_NAMES.length) {
                                        }
                                    }
                                } catch (NumberFormatException e2) {
                                }
                            } else {
                                if (length == 4) {
                                }
                                i2 = 1;
                                int i922 = length - 3;
                                i3 = length - 2;
                                i4 = Integer.parseInt(strSubstring2.substring(i922, i3));
                                if (i4 != 0) {
                                }
                            }
                        }
                    } else if (strSubstring.equals(ICAL_BYMONTHDAY)) {
                        java.util.StringTokenizer stringTokenizer2 = new java.util.StringTokenizer(strSubstring2, COMMA);
                        int[] iArr3 = new int[stringTokenizer2.countTokens()];
                        int i10 = 0;
                        while (stringTokenizer2.hasMoreTokens()) {
                            int i11 = i10 + 1;
                            try {
                                iArr3[i10] = Integer.parseInt(stringTokenizer2.nextToken());
                                i10 = i11;
                            } catch (NumberFormatException e3) {
                                iArr2 = iArr3;
                                z = true;
                            }
                        }
                        iArr2 = iArr3;
                    }
                    i5 = -1;
                } else if (strSubstring2.length() <= 2) {
                    try {
                        i8 = Integer.parseInt(strSubstring2) - 1;
                        if (i8 >= 0 && i8 < 12) {
                            i5 = -1;
                        }
                    } catch (NumberFormatException e4) {
                    }
                }
            }
            z = true;
        }
        if (z || !z2) {
            return null;
        }
        jArr[0] = dateTimeString;
        if (iArr2 == null) {
            iArr = new int[4];
            iArr[3] = 0;
        } else {
            iArr = new int[iArr2.length + 3];
            for (int i12 = 0; i12 < iArr2.length; i12++) {
                iArr[3 + i12] = iArr2[i12];
            }
        }
        iArr[0] = i8;
        iArr[1] = i6;
        iArr[2] = i7;
        return iArr;
    }

    private static TimeZoneRule createRuleByRDATE(String str, int i, int i2, long j, List<String> list, int i3) {
        long[] jArr;
        int i4 = 0;
        if (list == null || list.size() == 0) {
            jArr = new long[]{j};
        } else {
            long[] jArr2 = new long[list.size()];
            try {
                Iterator<String> it = list.iterator();
                while (it.hasNext()) {
                    int i5 = i4 + 1;
                    jArr2[i4] = parseDateTimeString(it.next(), i3);
                    i4 = i5;
                }
                jArr = jArr2;
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return new TimeArrayTimeZoneRule(str, i, i2, jArr, 2);
    }

    private void writeZone(Writer writer, BasicTimeZone basicTimeZone, String[] strArr) throws IOException {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        String str;
        boolean z;
        AnnualTimeZoneRule annualTimeZoneRule;
        AnnualTimeZoneRule annualTimeZoneRule2;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        long j;
        int i11;
        int i12;
        long j2;
        int i13;
        int i14;
        int i15;
        int i16;
        int i17;
        int i18;
        int i19;
        int i20;
        int i21;
        int i22;
        int i23;
        int i24;
        int i25;
        String str2;
        int i26;
        int i27;
        boolean z2;
        BasicTimeZone basicTimeZone2;
        int i28;
        int i29;
        int i30;
        String str3;
        int i31;
        int i32;
        int i33;
        int i34;
        int i35;
        int i36;
        String str4;
        int i37;
        int i38;
        int i39;
        int i40;
        String str5;
        int i41;
        int i42;
        int i43;
        int i44;
        int i45;
        String str6;
        int i46;
        int i47;
        boolean z3;
        int i48;
        int i49;
        int i50;
        int i51;
        int i52;
        int i53;
        int i54;
        Writer writer2 = writer;
        BasicTimeZone basicTimeZone3 = basicTimeZone;
        writeHeader(writer);
        boolean z4 = false;
        if (strArr != null && strArr.length > 0) {
            for (int i55 = 0; i55 < strArr.length; i55++) {
                if (strArr[i55] != null) {
                    writer2.write(strArr[i55]);
                    writer2.write(NEWLINE);
                }
            }
        }
        long j3 = MIN_TIME;
        int[] iArr = new int[6];
        String str7 = null;
        AnnualTimeZoneRule annualTimeZoneRule3 = null;
        AnnualTimeZoneRule annualTimeZoneRule4 = null;
        String str8 = null;
        int i56 = 0;
        int i57 = 0;
        int i58 = 0;
        int i59 = 0;
        int i60 = 0;
        int i61 = 0;
        int i62 = 0;
        int i63 = 0;
        int i64 = 0;
        int i65 = 0;
        int i66 = 0;
        int i67 = 0;
        int i68 = 0;
        int i69 = 0;
        int i70 = 0;
        int i71 = 0;
        int i72 = 0;
        int i73 = 0;
        int i74 = 0;
        long j4 = DEF_TZSTARTTIME;
        long j5 = DEF_TZSTARTTIME;
        long j6 = DEF_TZSTARTTIME;
        long j7 = DEF_TZSTARTTIME;
        while (true) {
            TimeZoneTransition nextTransition = basicTimeZone3.getNextTransition(j3, z4);
            if (nextTransition != null) {
                long time = nextTransition.getTime();
                String name = nextTransition.getTo().getName();
                boolean z5 = nextTransition.getTo().getDSTSavings() != 0 ? true : z4;
                int rawOffset = nextTransition.getFrom().getRawOffset() + nextTransition.getFrom().getDSTSavings();
                int dSTSavings = nextTransition.getFrom().getDSTSavings();
                int dSTSavings2 = nextTransition.getTo().getDSTSavings() + nextTransition.getTo().getRawOffset();
                int i75 = i56;
                int i76 = i57;
                Grego.timeToFields(nextTransition.getTime() + ((long) rawOffset), iArr);
                z = false;
                int dayOfWeekInMonth = Grego.getDayOfWeekInMonth(iArr[0], iArr[1], iArr[2]);
                int i77 = iArr[0];
                if (!z5) {
                    int i78 = i77;
                    i18 = i58;
                    int i79 = i60;
                    String str9 = str8;
                    int i80 = i71;
                    int i81 = i72;
                    i3 = i59;
                    int i82 = i61;
                    String str10 = str7;
                    int i83 = i73;
                    if (annualTimeZoneRule4 == null && (nextTransition.getTo() instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) nextTransition.getTo()).getEndYear() == Integer.MAX_VALUE) {
                        annualTimeZoneRule4 = (AnnualTimeZoneRule) nextTransition.getTo();
                    }
                    if (i64 <= 0) {
                        i19 = rawOffset;
                        i20 = i67;
                        i21 = i74;
                        i22 = i75;
                        i23 = i79;
                        i24 = dayOfWeekInMonth;
                        i13 = 1;
                        i25 = i68;
                        str2 = name;
                        i26 = i76;
                        i27 = i64;
                        z2 = false;
                    } else {
                        if (i78 != i65 + i64) {
                            i28 = i67;
                            i29 = i68;
                            i30 = i74;
                            str3 = name;
                        } else {
                            str3 = name;
                            if (str3.equals(str9)) {
                                i33 = i79;
                                if (i33 == rawOffset) {
                                    i32 = i76;
                                    if (i32 == dSTSavings2) {
                                        i31 = i75;
                                        if (i31 == iArr[1]) {
                                            i28 = i67;
                                            if (i28 == iArr[3]) {
                                                i29 = i68;
                                                i34 = dayOfWeekInMonth;
                                                if (i29 == i34) {
                                                    i30 = i74;
                                                    if (i30 == iArr[5]) {
                                                        i35 = i64 + 1;
                                                        j7 = time;
                                                        z2 = true;
                                                    }
                                                    if (z2) {
                                                        if (i35 == 1) {
                                                            i36 = i35;
                                                            i21 = i30;
                                                            i25 = i29;
                                                            i24 = i34;
                                                            i22 = i31;
                                                            i20 = i28;
                                                            i26 = i32;
                                                            writeZonePropsByTime(writer, false, str9, i33, i32, j6, true);
                                                        } else {
                                                            i36 = i35;
                                                            i21 = i30;
                                                            i25 = i29;
                                                            i24 = i34;
                                                            i22 = i31;
                                                            i20 = i28;
                                                            i26 = i32;
                                                            str2 = str3;
                                                            i23 = i33;
                                                            i78 = i78;
                                                            i19 = rawOffset;
                                                            i13 = 1;
                                                            writeZonePropsByDOW(writer, false, str9, i33, i26, i31, i25, i20, j6, j7);
                                                            i27 = i36;
                                                        }
                                                    } else {
                                                        i36 = i35;
                                                        i21 = i30;
                                                        i25 = i29;
                                                        i24 = i34;
                                                        i22 = i31;
                                                        i20 = i28;
                                                        i26 = i32;
                                                    }
                                                    str2 = str3;
                                                    i23 = i33;
                                                    i78 = i78;
                                                    i19 = rawOffset;
                                                    i13 = 1;
                                                    i27 = i36;
                                                } else {
                                                    i30 = i74;
                                                }
                                                i35 = i64;
                                                z2 = false;
                                                if (z2) {
                                                }
                                                str2 = str3;
                                                i23 = i33;
                                                i78 = i78;
                                                i19 = rawOffset;
                                                i13 = 1;
                                                i27 = i36;
                                            }
                                        } else {
                                            i28 = i67;
                                        }
                                        i29 = i68;
                                        i30 = i74;
                                    } else {
                                        i28 = i67;
                                        i29 = i68;
                                        i30 = i74;
                                        i31 = i75;
                                    }
                                } else {
                                    i28 = i67;
                                    i29 = i68;
                                    i30 = i74;
                                    i31 = i75;
                                    i32 = i76;
                                }
                                i34 = dayOfWeekInMonth;
                                i35 = i64;
                                z2 = false;
                                if (z2) {
                                }
                                str2 = str3;
                                i23 = i33;
                                i78 = i78;
                                i19 = rawOffset;
                                i13 = 1;
                                i27 = i36;
                            } else {
                                i28 = i67;
                                i29 = i68;
                                i30 = i74;
                            }
                        }
                        i31 = i75;
                        i32 = i76;
                        i33 = i79;
                        i34 = dayOfWeekInMonth;
                        i35 = i64;
                        z2 = false;
                        if (z2) {
                        }
                        str2 = str3;
                        i23 = i33;
                        i78 = i78;
                        i19 = rawOffset;
                        i13 = 1;
                        i27 = i36;
                    }
                    if (z2) {
                        i64 = i27;
                        i68 = i25;
                        i56 = i22;
                        i57 = i26;
                        i60 = i23;
                        i74 = i21;
                        i67 = i20;
                    } else {
                        int i84 = iArr[i13];
                        int i85 = iArr[3];
                        i74 = iArr[5];
                        i56 = i84;
                        i67 = i85;
                        i64 = i13;
                        i57 = dSTSavings2;
                        str9 = str2;
                        i66 = dSTSavings;
                        j6 = time;
                        j7 = j6;
                        i68 = i24;
                        i65 = i78;
                        i60 = i19;
                    }
                    if (annualTimeZoneRule4 == null || annualTimeZoneRule3 == null) {
                        basicTimeZone2 = basicTimeZone;
                        i61 = i82;
                        str7 = str10;
                        i73 = i83;
                        str8 = str9;
                        i72 = i81;
                        i71 = i80;
                        basicTimeZone3 = basicTimeZone2;
                        i69 = i13;
                        j3 = time;
                        i58 = i18;
                        z4 = false;
                        i59 = i3;
                        writer2 = writer;
                    } else {
                        i = i56;
                        i2 = i57;
                        i4 = i60;
                        i10 = i13;
                        annualTimeZoneRule2 = annualTimeZoneRule4;
                        i6 = i62;
                        i7 = i64;
                        i8 = i67;
                        i9 = i68;
                        j = j4;
                        i5 = i82;
                        i12 = i81;
                        str = str10;
                        i11 = i80;
                        i58 = i18;
                        str8 = str9;
                        annualTimeZoneRule = annualTimeZoneRule3;
                        j2 = j7;
                        break;
                    }
                } else {
                    if (annualTimeZoneRule3 == null && (nextTransition.getTo() instanceof AnnualTimeZoneRule) && ((AnnualTimeZoneRule) nextTransition.getTo()).getEndYear() == Integer.MAX_VALUE) {
                        annualTimeZoneRule3 = (AnnualTimeZoneRule) nextTransition.getTo();
                    }
                    if (i62 <= 0) {
                        str4 = name;
                        i37 = i77;
                        i38 = i58;
                        i4 = i60;
                        i39 = dayOfWeekInMonth;
                        i40 = rawOffset;
                        str5 = str8;
                        i41 = i71;
                        i42 = i72;
                        i2 = i76;
                        i43 = i75;
                        i44 = i59;
                        i45 = i61;
                        str6 = str7;
                        i46 = i73;
                        i47 = i62;
                        z3 = false;
                    } else {
                        if (i77 == i63 + i62 && name.equals(str7) && i59 == rawOffset && i58 == dSTSavings2 && i61 == iArr[1]) {
                            i50 = i71;
                            if (i50 == iArr[3]) {
                                i51 = i72;
                                if (i51 == dayOfWeekInMonth) {
                                    i49 = i61;
                                    i52 = i73;
                                    if (i52 != iArr[5]) {
                                        i53 = i62;
                                        z3 = false;
                                    } else {
                                        i53 = i62 + 1;
                                        j5 = time;
                                        z3 = true;
                                    }
                                    if (!z3) {
                                        int i86 = i52;
                                        if (i53 == 1) {
                                            i54 = i53;
                                            str4 = name;
                                            i42 = i51;
                                            i43 = i75;
                                            i37 = i77;
                                            i2 = i76;
                                            i38 = i58;
                                            i44 = i59;
                                            i4 = i60;
                                            str5 = str8;
                                            i45 = i49;
                                            i46 = i86;
                                            writeZonePropsByTime(writer2, true, str7, i59, i58, j4, true);
                                            i39 = dayOfWeekInMonth;
                                            str6 = str7;
                                            i41 = i50;
                                            i40 = rawOffset;
                                        } else {
                                            i54 = i53;
                                            str4 = name;
                                            i42 = i51;
                                            i37 = i77;
                                            i38 = i58;
                                            i4 = i60;
                                            str5 = str8;
                                            i2 = i76;
                                            i43 = i75;
                                            i45 = i49;
                                            i46 = i86;
                                            i44 = i59;
                                            i39 = dayOfWeekInMonth;
                                            str6 = str7;
                                            i41 = i50;
                                            i40 = rawOffset;
                                            writeZonePropsByDOW(writer2, true, str7, i44, i58, i45, i42, i50, j4, j5);
                                        }
                                    } else {
                                        i54 = i53;
                                        str4 = name;
                                        i42 = i51;
                                        i37 = i77;
                                        i38 = i58;
                                        i4 = i60;
                                        i39 = dayOfWeekInMonth;
                                        str6 = str7;
                                        i41 = i50;
                                        i40 = rawOffset;
                                        str5 = str8;
                                        i2 = i76;
                                        i43 = i75;
                                        i45 = i49;
                                        i44 = i59;
                                        i46 = i52;
                                    }
                                    i47 = i54;
                                } else {
                                    i49 = i61;
                                    i52 = i73;
                                    i53 = i62;
                                    z3 = false;
                                    if (!z3) {
                                    }
                                    i47 = i54;
                                }
                            } else {
                                i49 = i61;
                            }
                        } else {
                            i49 = i61;
                            i50 = i71;
                        }
                        i51 = i72;
                        i52 = i73;
                        i53 = i62;
                        z3 = false;
                        if (!z3) {
                        }
                        i47 = i54;
                    }
                    if (z3) {
                        i10 = 1;
                        i62 = i47;
                        i61 = i45;
                        str7 = str6;
                        i58 = i38;
                        i48 = i44;
                        i72 = i42;
                        i71 = i41;
                    } else {
                        i10 = 1;
                        i61 = iArr[1];
                        i71 = iArr[3];
                        i62 = 1;
                        i48 = i40;
                        i58 = dSTSavings2;
                        i70 = dSTSavings;
                        i46 = iArr[5];
                        j4 = time;
                        j5 = j4;
                        str7 = str4;
                        i63 = i37;
                        i72 = i39;
                    }
                    if (annualTimeZoneRule4 == null || annualTimeZoneRule3 == null) {
                        i18 = i58;
                        i3 = i48;
                        i13 = i10;
                        str8 = str5;
                        i73 = i46;
                        i56 = i43;
                        i57 = i2;
                        i60 = i4;
                        basicTimeZone2 = basicTimeZone;
                        basicTimeZone3 = basicTimeZone2;
                        i69 = i13;
                        j3 = time;
                        i58 = i18;
                        z4 = false;
                        i59 = i3;
                        writer2 = writer;
                    } else {
                        i3 = i48;
                        i5 = i61;
                        str = str7;
                        i13 = i10;
                        str8 = str5;
                        annualTimeZoneRule = annualTimeZoneRule3;
                        annualTimeZoneRule2 = annualTimeZoneRule4;
                        i6 = i62;
                        i7 = i64;
                        i8 = i67;
                        i9 = i68;
                        j = j4;
                        i11 = i71;
                        i12 = i72;
                        j2 = j7;
                        i = i43;
                        break;
                    }
                }
            } else {
                i = i56;
                i2 = i57;
                i3 = i59;
                i4 = i60;
                i5 = i61;
                str = str7;
                z = z4;
                annualTimeZoneRule = annualTimeZoneRule3;
                annualTimeZoneRule2 = annualTimeZoneRule4;
                i6 = i62;
                i7 = i64;
                i8 = i67;
                i9 = i68;
                i10 = i69;
                j = j4;
                i11 = i71;
                i12 = i72;
                j2 = j7;
                i13 = 1;
                break;
            }
        }
        if (i10 == 0) {
            int offset = basicTimeZone.getOffset(DEF_TZSTARTTIME);
            boolean z6 = offset != basicTimeZone.getRawOffset() ? i13 : z;
            writeZonePropsByTime(writer, z6, getDefaultTZName(basicTimeZone.getID(), z6), offset, offset, DEF_TZSTARTTIME - ((long) offset), false);
        } else {
            if (i6 > 0) {
                if (annualTimeZoneRule == null) {
                    if (i6 == i13) {
                        i17 = i9;
                        writeZonePropsByTime(writer, true, str, i3, i58, j, true);
                        i15 = i8;
                        i16 = i;
                    } else {
                        i17 = i9;
                        i15 = i8;
                        i16 = i;
                        writeZonePropsByDOW(writer, true, str, i3, i58, i5, i12, i11, j, j5);
                    }
                } else {
                    int i87 = i9;
                    i15 = i8;
                    i16 = i;
                    if (i6 == i13) {
                        writeFinalRule(writer, true, annualTimeZoneRule, i3 - i70, i70, j);
                        i14 = i87;
                    } else if (isEquivalentDateRule(i5, i12, i11, annualTimeZoneRule.getRule())) {
                        i17 = i87;
                        writeZonePropsByDOW(writer, true, str, i3, i58, i5, i12, i11, j, MAX_TIME);
                    } else {
                        i14 = i87;
                        writeZonePropsByDOW(writer, true, str, i3, i58, i5, i12, i11, j, j5);
                        int i88 = i3 - i70;
                        Date nextStart = annualTimeZoneRule.getNextStart(j5, i88, i70, false);
                        if (nextStart != null) {
                            writeFinalRule(writer, true, annualTimeZoneRule, i88, i70, nextStart.getTime());
                        }
                    }
                }
                i14 = i17;
            } else {
                i14 = i9;
                i15 = i8;
                i16 = i;
            }
            if (i7 > 0) {
                if (annualTimeZoneRule2 == null) {
                    if (i7 == i13) {
                        writeZonePropsByTime(writer, false, str8, i4, i2, j6, true);
                    } else {
                        writeZonePropsByDOW(writer, false, str8, i4, i2, i16, i14, i15, j6, j2);
                    }
                } else if (i7 == i13) {
                    writeFinalRule(writer, false, annualTimeZoneRule2, i4 - i66, i66, j6);
                } else {
                    int i89 = i16;
                    int i90 = i15;
                    int i91 = i14;
                    if (isEquivalentDateRule(i89, i91, i90, annualTimeZoneRule2.getRule())) {
                        writeZonePropsByDOW(writer, false, str8, i4, i2, i89, i91, i90, j6, MAX_TIME);
                    } else {
                        writeZonePropsByDOW(writer, false, str8, i4, i2, i89, i91, i90, j6, j2);
                        int i92 = i4 - i66;
                        AnnualTimeZoneRule annualTimeZoneRule5 = annualTimeZoneRule2;
                        Date nextStart2 = annualTimeZoneRule2.getNextStart(j2, i92, i66, false);
                        if (nextStart2 != null) {
                            writeFinalRule(writer, false, annualTimeZoneRule5, i92, i66, nextStart2.getTime());
                        }
                    }
                }
            }
        }
        writeFooter(writer);
    }

    private static boolean isEquivalentDateRule(int i, int i2, int i3, DateTimeRule dateTimeRule) {
        if (i != dateTimeRule.getRuleMonth() || i3 != dateTimeRule.getRuleDayOfWeek() || dateTimeRule.getTimeRuleType() != 0) {
            return false;
        }
        if (dateTimeRule.getDateRuleType() == 1 && dateTimeRule.getRuleWeekInMonth() == i2) {
            return true;
        }
        int ruleDayOfMonth = dateTimeRule.getRuleDayOfMonth();
        if (dateTimeRule.getDateRuleType() == 2) {
            if (ruleDayOfMonth % 7 == 1 && (ruleDayOfMonth + 6) / 7 == i2) {
                return true;
            }
            if (i != 1 && (MONTHLENGTH[i] - ruleDayOfMonth) % 7 == 6 && i2 == (((MONTHLENGTH[i] - ruleDayOfMonth) + 1) / 7) * (-1)) {
                return true;
            }
        }
        if (dateTimeRule.getDateRuleType() == 3) {
            if (ruleDayOfMonth % 7 == 0 && ruleDayOfMonth / 7 == i2) {
                return true;
            }
            if (i != 1 && (MONTHLENGTH[i] - ruleDayOfMonth) % 7 == 0 && i2 == (-1) * (((MONTHLENGTH[i] - ruleDayOfMonth) / 7) + 1)) {
                return true;
            }
        }
        return false;
    }

    private static void writeZonePropsByTime(Writer writer, boolean z, String str, int i, int i2, long j, boolean z2) throws IOException {
        beginZoneProps(writer, z, str, i, i2, j);
        if (z2) {
            writer.write(ICAL_RDATE);
            writer.write(COLON);
            writer.write(getDateTimeString(j + ((long) i)));
            writer.write(NEWLINE);
        }
        endZoneProps(writer, z);
    }

    private static void writeZonePropsByDOM(Writer writer, boolean z, String str, int i, int i2, int i3, int i4, long j, long j2) throws IOException {
        beginZoneProps(writer, z, str, i, i2, j);
        beginRRULE(writer, i3);
        writer.write(ICAL_BYMONTHDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(i4));
        if (j2 != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(j2 + ((long) i)));
        }
        writer.write(NEWLINE);
        endZoneProps(writer, z);
    }

    private static void writeZonePropsByDOW(Writer writer, boolean z, String str, int i, int i2, int i3, int i4, int i5, long j, long j2) throws IOException {
        beginZoneProps(writer, z, str, i, i2, j);
        beginRRULE(writer, i3);
        writer.write(ICAL_BYDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(i4));
        writer.write(ICAL_DOW_NAMES[i5 - 1]);
        if (j2 != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(j2 + ((long) i)));
        }
        writer.write(NEWLINE);
        endZoneProps(writer, z);
    }

    private static void writeZonePropsByDOW_GEQ_DOM(Writer writer, boolean z, String str, int i, int i2, int i3, int i4, int i5, long j, long j2) throws IOException {
        int i6;
        int i7 = 7;
        if (i4 % 7 == 1) {
            writeZonePropsByDOW(writer, z, str, i, i2, i3, (i4 + 6) / 7, i5, j, j2);
            return;
        }
        if (i3 != 1 && (MONTHLENGTH[i3] - i4) % 7 == 6) {
            writeZonePropsByDOW(writer, z, str, i, i2, i3, (-1) * (((MONTHLENGTH[i3] - i4) + 1) / 7), i5, j, j2);
            return;
        }
        beginZoneProps(writer, z, str, i, i2, j);
        if (i4 <= 0) {
            int i8 = 1 - i4;
            i7 = 7 - i8;
            int i9 = i3 - 1;
            writeZonePropsByDOW_GEQ_DOM_sub(writer, i9 < 0 ? 11 : i9, -i8, i5, i8, MAX_TIME, i);
            i6 = 1;
        } else {
            int i10 = i4 + 6;
            if (i10 > MONTHLENGTH[i3]) {
                int i11 = i10 - MONTHLENGTH[i3];
                i7 = 7 - i11;
                int i12 = i3 + 1;
                writeZonePropsByDOW_GEQ_DOM_sub(writer, i12 > 11 ? 0 : i12, 1, i5, i11, MAX_TIME, i);
            }
            i6 = i4;
        }
        writeZonePropsByDOW_GEQ_DOM_sub(writer, i3, i6, i5, i7, j2, i);
        endZoneProps(writer, z);
    }

    private static void writeZonePropsByDOW_GEQ_DOM_sub(Writer writer, int i, int i2, int i3, int i4, long j, int i5) throws IOException {
        boolean z;
        if (i != 1) {
            z = false;
        } else {
            z = true;
        }
        if (i2 < 0 && !z) {
            i2 = MONTHLENGTH[i] + i2 + 1;
        }
        beginRRULE(writer, i);
        writer.write(ICAL_BYDAY);
        writer.write(EQUALS_SIGN);
        writer.write(ICAL_DOW_NAMES[i3 - 1]);
        writer.write(SEMICOLON);
        writer.write(ICAL_BYMONTHDAY);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(i2));
        for (int i6 = 1; i6 < i4; i6++) {
            writer.write(COMMA);
            writer.write(Integer.toString(i2 + i6));
        }
        if (j != MAX_TIME) {
            appendUNTIL(writer, getDateTimeString(j + ((long) i5)));
        }
        writer.write(NEWLINE);
    }

    private static void writeZonePropsByDOW_LEQ_DOM(Writer writer, boolean z, String str, int i, int i2, int i3, int i4, int i5, long j, long j2) throws IOException {
        if (i4 % 7 == 0) {
            writeZonePropsByDOW(writer, z, str, i, i2, i3, i4 / 7, i5, j, j2);
            return;
        }
        if (i3 != 1 && (MONTHLENGTH[i3] - i4) % 7 == 0) {
            writeZonePropsByDOW(writer, z, str, i, i2, i3, (-1) * (((MONTHLENGTH[i3] - i4) / 7) + 1), i5, j, j2);
        } else if (i3 == 1 && i4 == 29) {
            writeZonePropsByDOW(writer, z, str, i, i2, 1, -1, i5, j, j2);
        } else {
            writeZonePropsByDOW_GEQ_DOM(writer, z, str, i, i2, i3, i4 - 6, i5, j, j2);
        }
    }

    private static void writeFinalRule(Writer writer, boolean z, AnnualTimeZoneRule annualTimeZoneRule, int i, int i2, long j) throws IOException {
        long j2;
        DateTimeRule wallTimeRule = toWallTimeRule(annualTimeZoneRule.getRule(), i, i2);
        int ruleMillisInDay = wallTimeRule.getRuleMillisInDay();
        if (ruleMillisInDay < 0) {
            j2 = j + ((long) (0 - ruleMillisInDay));
        } else if (ruleMillisInDay >= 86400000) {
            j2 = j - ((long) (ruleMillisInDay - 86399999));
        } else {
            j2 = j;
        }
        int rawOffset = annualTimeZoneRule.getRawOffset() + annualTimeZoneRule.getDSTSavings();
        switch (wallTimeRule.getDateRuleType()) {
            case 0:
                writeZonePropsByDOM(writer, z, annualTimeZoneRule.getName(), i + i2, rawOffset, wallTimeRule.getRuleMonth(), wallTimeRule.getRuleDayOfMonth(), j2, MAX_TIME);
                break;
            case 1:
                writeZonePropsByDOW(writer, z, annualTimeZoneRule.getName(), i + i2, rawOffset, wallTimeRule.getRuleMonth(), wallTimeRule.getRuleWeekInMonth(), wallTimeRule.getRuleDayOfWeek(), j2, MAX_TIME);
                break;
            case 2:
                writeZonePropsByDOW_GEQ_DOM(writer, z, annualTimeZoneRule.getName(), i + i2, rawOffset, wallTimeRule.getRuleMonth(), wallTimeRule.getRuleDayOfMonth(), wallTimeRule.getRuleDayOfWeek(), j2, MAX_TIME);
                break;
            case 3:
                writeZonePropsByDOW_LEQ_DOM(writer, z, annualTimeZoneRule.getName(), i + i2, rawOffset, wallTimeRule.getRuleMonth(), wallTimeRule.getRuleDayOfMonth(), wallTimeRule.getRuleDayOfWeek(), j2, MAX_TIME);
                break;
        }
    }

    private static DateTimeRule toWallTimeRule(DateTimeRule dateTimeRule, int i, int i2) {
        int i3;
        int i4;
        int dateRuleType;
        int i5;
        int i6;
        if (dateTimeRule.getTimeRuleType() == 0) {
            return dateTimeRule;
        }
        int ruleMillisInDay = dateTimeRule.getRuleMillisInDay();
        if (dateTimeRule.getTimeRuleType() == 2) {
            ruleMillisInDay += i + i2;
        } else if (dateTimeRule.getTimeRuleType() == 1) {
            ruleMillisInDay += i2;
        }
        if (ruleMillisInDay < 0) {
            ruleMillisInDay += Grego.MILLIS_PER_DAY;
            i3 = -1;
        } else if (ruleMillisInDay >= 86400000) {
            i4 = ruleMillisInDay - Grego.MILLIS_PER_DAY;
            i3 = 1;
            int ruleMonth = dateTimeRule.getRuleMonth();
            int ruleDayOfMonth = dateTimeRule.getRuleDayOfMonth();
            int ruleDayOfWeek = dateTimeRule.getRuleDayOfWeek();
            dateRuleType = dateTimeRule.getDateRuleType();
            if (i3 == 0) {
                if (dateRuleType == 1) {
                    int ruleWeekInMonth = dateTimeRule.getRuleWeekInMonth();
                    if (ruleWeekInMonth > 0) {
                        ruleDayOfMonth = ((ruleWeekInMonth - 1) * 7) + 1;
                        dateRuleType = 2;
                    } else {
                        dateRuleType = 3;
                        ruleDayOfMonth = ((ruleWeekInMonth + 1) * 7) + MONTHLENGTH[ruleMonth];
                    }
                }
                int i7 = ruleDayOfMonth + i3;
                if (i7 == 0) {
                    ruleMonth--;
                    if (ruleMonth < 0) {
                        ruleMonth = 11;
                    }
                    i7 = MONTHLENGTH[ruleMonth];
                } else if (i7 > MONTHLENGTH[ruleMonth]) {
                    int i8 = ruleMonth + 1;
                    ruleMonth = i8 > 11 ? 0 : i8;
                    i7 = 1;
                }
                if (dateRuleType != 0) {
                    int i9 = i3 + ruleDayOfWeek;
                    if (i9 >= 1) {
                        if (i9 > 7) {
                            i5 = i7;
                            i6 = 1;
                        } else {
                            i5 = i7;
                            i6 = i9;
                        }
                    } else {
                        i6 = 7;
                        i5 = i7;
                    }
                    if (dateRuleType == 0) {
                        return new DateTimeRule(ruleMonth, i5, i4, 0);
                    }
                    return new DateTimeRule(ruleMonth, i5, i6, dateRuleType == 2, i4, 0);
                }
                i5 = i7;
            } else {
                i5 = ruleDayOfMonth;
            }
            i6 = ruleDayOfWeek;
            if (dateRuleType == 0) {
            }
        } else {
            i3 = 0;
        }
        i4 = ruleMillisInDay;
        int ruleMonth2 = dateTimeRule.getRuleMonth();
        int ruleDayOfMonth2 = dateTimeRule.getRuleDayOfMonth();
        int ruleDayOfWeek2 = dateTimeRule.getRuleDayOfWeek();
        dateRuleType = dateTimeRule.getDateRuleType();
        if (i3 == 0) {
        }
        i6 = ruleDayOfWeek2;
        if (dateRuleType == 0) {
        }
    }

    private static void beginZoneProps(Writer writer, boolean z, String str, int i, int i2, long j) throws IOException {
        writer.write(ICAL_BEGIN);
        writer.write(COLON);
        if (z) {
            writer.write(ICAL_DAYLIGHT);
        } else {
            writer.write(ICAL_STANDARD);
        }
        writer.write(NEWLINE);
        writer.write(ICAL_TZOFFSETTO);
        writer.write(COLON);
        writer.write(millisToOffset(i2));
        writer.write(NEWLINE);
        writer.write(ICAL_TZOFFSETFROM);
        writer.write(COLON);
        writer.write(millisToOffset(i));
        writer.write(NEWLINE);
        writer.write(ICAL_TZNAME);
        writer.write(COLON);
        writer.write(str);
        writer.write(NEWLINE);
        writer.write(ICAL_DTSTART);
        writer.write(COLON);
        writer.write(getDateTimeString(j + ((long) i)));
        writer.write(NEWLINE);
    }

    private static void endZoneProps(Writer writer, boolean z) throws IOException {
        writer.write(ICAL_END);
        writer.write(COLON);
        if (z) {
            writer.write(ICAL_DAYLIGHT);
        } else {
            writer.write(ICAL_STANDARD);
        }
        writer.write(NEWLINE);
    }

    private static void beginRRULE(Writer writer, int i) throws IOException {
        writer.write(ICAL_RRULE);
        writer.write(COLON);
        writer.write(ICAL_FREQ);
        writer.write(EQUALS_SIGN);
        writer.write(ICAL_YEARLY);
        writer.write(SEMICOLON);
        writer.write(ICAL_BYMONTH);
        writer.write(EQUALS_SIGN);
        writer.write(Integer.toString(i + 1));
        writer.write(SEMICOLON);
    }

    private static void appendUNTIL(Writer writer, String str) throws IOException {
        if (str != null) {
            writer.write(SEMICOLON);
            writer.write(ICAL_UNTIL);
            writer.write(EQUALS_SIGN);
            writer.write(str);
        }
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
        if (this.lastmod != null) {
            writer.write(ICAL_LASTMOD);
            writer.write(COLON);
            writer.write(getUTCDateTimeString(this.lastmod.getTime()));
            writer.write(NEWLINE);
        }
    }

    private static void writeFooter(Writer writer) throws IOException {
        writer.write(ICAL_END);
        writer.write(COLON);
        writer.write(ICAL_VTIMEZONE);
        writer.write(NEWLINE);
    }

    private static String getDateTimeString(long j) {
        int[] iArrTimeToFields = Grego.timeToFields(j, null);
        StringBuilder sb = new StringBuilder(15);
        sb.append(numToString(iArrTimeToFields[0], 4));
        sb.append(numToString(iArrTimeToFields[1] + 1, 2));
        sb.append(numToString(iArrTimeToFields[2], 2));
        sb.append('T');
        int i = iArrTimeToFields[5];
        int i2 = i / 3600000;
        int i3 = i % 3600000;
        sb.append(numToString(i2, 2));
        sb.append(numToString(i3 / 60000, 2));
        sb.append(numToString((i3 % 60000) / 1000, 2));
        return sb.toString();
    }

    private static String getUTCDateTimeString(long j) {
        return getDateTimeString(j) + "Z";
    }

    private static long parseDateTimeString(String str, int i) {
        int i2;
        boolean z;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int length;
        boolean z2 = false;
        if (str != null && (((length = str.length()) == 15 || length == 16) && str.charAt(8) == 'T')) {
            if (length == 16) {
                if (str.charAt(15) == 'Z') {
                    z = true;
                }
                i2 = 0;
                z = false;
                i3 = 0;
                i4 = 0;
                i5 = 0;
                i6 = 0;
                i7 = 0;
            } else {
                z = false;
            }
            try {
                i5 = Integer.parseInt(str.substring(0, 4));
                try {
                    i3 = Integer.parseInt(str.substring(4, 6)) - 1;
                } catch (NumberFormatException e) {
                    i3 = 0;
                    i4 = 0;
                }
            } catch (NumberFormatException e2) {
                i3 = 0;
                i4 = 0;
                i5 = 0;
                i6 = 0;
            }
            try {
                i4 = Integer.parseInt(str.substring(6, 8));
                try {
                    i6 = Integer.parseInt(str.substring(9, 11));
                    try {
                        i7 = Integer.parseInt(str.substring(11, 13));
                    } catch (NumberFormatException e3) {
                        i7 = 0;
                    }
                    try {
                        i2 = Integer.parseInt(str.substring(13, 15));
                        int iMonthLength = Grego.monthLength(i5, i3);
                        if (i5 >= 0 && i3 >= 0 && i3 <= 11 && i4 >= 1 && i4 <= iMonthLength && i6 >= 0 && i6 < 24 && i7 >= 0 && i7 < 60 && i2 >= 0 && i2 < 60) {
                            z2 = true;
                        }
                    } catch (NumberFormatException e4) {
                        i2 = 0;
                    }
                } catch (NumberFormatException e5) {
                    i6 = 0;
                    i7 = i6;
                    i2 = 0;
                    if (!z2) {
                    }
                }
            } catch (NumberFormatException e6) {
                i4 = 0;
                i6 = i4;
                i7 = i6;
                i2 = 0;
                if (!z2) {
                }
            }
        } else {
            i2 = 0;
            z = false;
            i3 = 0;
            i4 = 0;
            i5 = 0;
            i6 = 0;
            i7 = 0;
        }
        if (!z2) {
            throw new IllegalArgumentException("Invalid date time string format");
        }
        long jFieldsToDay = (Grego.fieldsToDay(i5, i3, i4) * 86400000) + ((long) ((i6 * 3600000) + (i7 * 60000) + (i2 * 1000)));
        if (!z) {
            return jFieldsToDay - ((long) i);
        }
        return jFieldsToDay;
    }

    private static int offsetStrToMillis(String str) {
        int i;
        int i2;
        int i3;
        int i4;
        int length;
        int i5 = 0;
        if (str != null && ((length = str.length()) == 5 || length == 7)) {
            char cCharAt = str.charAt(0);
            if (cCharAt != '+') {
                if (cCharAt == '-') {
                    i2 = -1;
                }
                i = 0;
                i2 = 0;
                i3 = 0;
                i4 = 0;
            } else {
                i2 = 1;
            }
            try {
                i4 = Integer.parseInt(str.substring(1, 3));
                try {
                    i3 = Integer.parseInt(str.substring(3, 5));
                    if (length == 7) {
                        try {
                            i5 = Integer.parseInt(str.substring(5, 7));
                        } catch (NumberFormatException e) {
                            i = 0;
                        }
                    }
                    i = i5;
                    i5 = 1;
                } catch (NumberFormatException e2) {
                    i3 = 0;
                }
            } catch (NumberFormatException e3) {
                i3 = 0;
                i4 = 0;
            }
        } else {
            i = 0;
            i2 = 0;
            i3 = 0;
            i4 = 0;
        }
        if (i5 == 0) {
            throw new IllegalArgumentException("Bad offset string");
        }
        return i2 * ((((i4 * 60) + i3) * 60) + i) * 1000;
    }

    private static String millisToOffset(int i) {
        StringBuilder sb = new StringBuilder(7);
        if (i >= 0) {
            sb.append('+');
        } else {
            sb.append('-');
            i = -i;
        }
        int i2 = i / 1000;
        int i3 = i2 % 60;
        int i4 = (i2 - i3) / 60;
        sb.append(numToString(i4 / 60, 2));
        sb.append(numToString(i4 % 60, 2));
        sb.append(numToString(i3, 2));
        return sb.toString();
    }

    private static String numToString(int i, int i2) {
        String string = Integer.toString(i);
        int length = string.length();
        if (length >= i2) {
            return string.substring(length - i2, length);
        }
        StringBuilder sb = new StringBuilder(i2);
        while (length < i2) {
            sb.append('0');
            length++;
        }
        sb.append(string);
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
        VTimeZone vTimeZone = (VTimeZone) super.cloneAsThawed();
        vTimeZone.tz = (BasicTimeZone) this.tz.cloneAsThawed();
        vTimeZone.isFrozen = false;
        return vTimeZone;
    }
}
