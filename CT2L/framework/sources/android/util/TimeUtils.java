package android.util;

import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.os.SystemClock;
import android.text.format.DateFormat;
import com.android.internal.R;
import com.android.internal.util.XmlUtils;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.TimeZone;
import libcore.util.ZoneInfoDB;
import org.xmlpull.v1.XmlPullParserException;

public class TimeUtils {
    private static final boolean DBG = false;
    public static final int HUNDRED_DAY_FIELD_LEN = 19;
    private static final long LARGEST_DURATION = 86399999999L;
    public static final long NANOS_PER_MS = 1000000;
    private static final int SECONDS_PER_DAY = 86400;
    private static final int SECONDS_PER_HOUR = 3600;
    private static final int SECONDS_PER_MINUTE = 60;
    private static final String TAG = "TimeUtils";
    private static final Object sLastLockObj = new Object();
    private static ArrayList<TimeZone> sLastZones = null;
    private static String sLastCountry = null;
    private static final Object sLastUniqueLockObj = new Object();
    private static ArrayList<TimeZone> sLastUniqueZoneOffsets = null;
    private static String sLastUniqueCountry = null;
    private static final Object sFormatSync = new Object();
    private static char[] sFormatStr = new char[24];

    public static TimeZone getTimeZone(int offset, boolean dst, long when, String country) {
        TimeZone best = null;
        Date d = new Date(when);
        TimeZone current = TimeZone.getDefault();
        String currentName = current.getID();
        int currentOffset = current.getOffset(when);
        boolean currentDst = current.inDaylightTime(d);
        for (TimeZone tz : getTimeZones(country)) {
            if (!tz.getID().equals(currentName) || currentOffset != offset || currentDst != dst) {
                if (best == null && tz.getOffset(when) == offset && tz.inDaylightTime(d) == dst) {
                    best = tz;
                }
            } else {
                return current;
            }
        }
        return best;
    }

    public static ArrayList<TimeZone> getTimeZonesWithUniqueOffsets(String country) {
        ArrayList<TimeZone> arrayList;
        synchronized (sLastUniqueLockObj) {
            if (country != null) {
                if (country.equals(sLastUniqueCountry)) {
                    arrayList = sLastUniqueZoneOffsets;
                } else {
                    Collection<TimeZone> zones = getTimeZones(country);
                    ArrayList<TimeZone> uniqueTimeZones = new ArrayList<>();
                    for (TimeZone zone : zones) {
                        boolean found = false;
                        int i = 0;
                        while (true) {
                            if (i >= uniqueTimeZones.size()) {
                                break;
                            }
                            if (uniqueTimeZones.get(i).getRawOffset() != zone.getRawOffset()) {
                                i++;
                            } else {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            uniqueTimeZones.add(zone);
                        }
                    }
                    synchronized (sLastUniqueLockObj) {
                        sLastUniqueZoneOffsets = uniqueTimeZones;
                        sLastUniqueCountry = country;
                        arrayList = sLastUniqueZoneOffsets;
                    }
                }
            }
        }
        return arrayList;
    }

    public static ArrayList<TimeZone> getTimeZones(String country) {
        ArrayList<TimeZone> tzs;
        synchronized (sLastLockObj) {
            if (country != null) {
                if (country.equals(sLastCountry)) {
                    tzs = sLastZones;
                } else {
                    tzs = new ArrayList<>();
                    if (country != null) {
                        Resources r = Resources.getSystem();
                        XmlResourceParser parser = r.getXml(R.xml.time_zones_by_country);
                        try {
                            XmlUtils.beginDocument(parser, "timezones");
                            while (true) {
                                XmlUtils.nextElement(parser);
                                String element = parser.getName();
                                if (element == null || !element.equals("timezone")) {
                                    break;
                                }
                                String code = parser.getAttributeValue(null, "code");
                                if (country.equals(code) && parser.next() == 4) {
                                    String zoneIdString = parser.getText();
                                    TimeZone tz = TimeZone.getTimeZone(zoneIdString);
                                    if (!tz.getID().startsWith("GMT")) {
                                        tzs.add(tz);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Got IO exception getTimeZone('" + country + "'): e=", e);
                        } catch (XmlPullParserException e2) {
                            Log.e(TAG, "Got xml parser exception getTimeZone('" + country + "'): e=", e2);
                        } finally {
                            parser.close();
                        }
                        synchronized (sLastLockObj) {
                            sLastZones = tzs;
                            sLastCountry = country;
                            tzs = sLastZones;
                        }
                    }
                }
            }
        }
        return tzs;
    }

    public static String getTimeZoneDatabaseVersion() {
        return ZoneInfoDB.getInstance().getVersion();
    }

    private static int accumField(int amt, int suffix, boolean always, int zeropad) {
        if (amt > 99 || (always && zeropad >= 3)) {
            return suffix + 3;
        }
        if (amt > 9 || (always && zeropad >= 2)) {
            return suffix + 2;
        }
        if (always || amt > 0) {
            return suffix + 1;
        }
        return 0;
    }

    private static int printField(char[] formatStr, int amt, char suffix, int pos, boolean always, int zeropad) {
        if (always || amt > 0) {
            if ((always && zeropad >= 3) || amt > 99) {
                int dig = amt / 100;
                formatStr[pos] = (char) (dig + 48);
                pos++;
                amt -= dig * 100;
            }
            if ((always && zeropad >= 2) || amt > 9 || pos != pos) {
                int dig2 = amt / 10;
                formatStr[pos] = (char) (dig2 + 48);
                pos++;
                amt -= dig2 * 10;
            }
            formatStr[pos] = (char) (amt + 48);
            int pos2 = pos + 1;
            formatStr[pos2] = suffix;
            return pos2 + 1;
        }
        return pos;
    }

    private static int formatDurationLocked(long duration, int fieldLen) {
        char prefix;
        if (sFormatStr.length < fieldLen) {
            sFormatStr = new char[fieldLen];
        }
        char[] formatStr = sFormatStr;
        if (duration == 0) {
            int pos = 0;
            int fieldLen2 = fieldLen - 1;
            while (true) {
                int pos2 = pos;
                if (pos2 < fieldLen2) {
                    pos = pos2 + 1;
                    formatStr[pos2] = ' ';
                } else {
                    formatStr[pos2] = '0';
                    return pos2 + 1;
                }
            }
        } else {
            if (duration > 0) {
                prefix = '+';
            } else {
                prefix = '-';
                duration = -duration;
            }
            if (duration > LARGEST_DURATION) {
                duration = LARGEST_DURATION;
            }
            int millis = (int) (duration % 1000);
            int seconds = (int) Math.floor(duration / 1000);
            int days = 0;
            int hours = 0;
            int minutes = 0;
            if (seconds > SECONDS_PER_DAY) {
                days = seconds / SECONDS_PER_DAY;
                seconds -= SECONDS_PER_DAY * days;
            }
            if (seconds > SECONDS_PER_HOUR) {
                hours = seconds / SECONDS_PER_HOUR;
                seconds -= hours * SECONDS_PER_HOUR;
            }
            if (seconds > 60) {
                minutes = seconds / 60;
                seconds -= minutes * 60;
            }
            int pos3 = 0;
            if (fieldLen != 0) {
                int myLen = accumField(days, 1, false, 0);
                int myLen2 = myLen + accumField(hours, 1, myLen > 0, 2);
                int myLen3 = myLen2 + accumField(minutes, 1, myLen2 > 0, 2);
                int myLen4 = myLen3 + accumField(seconds, 1, myLen3 > 0, 2);
                for (int myLen5 = myLen4 + accumField(millis, 2, true, myLen4 > 0 ? 3 : 0) + 1; myLen5 < fieldLen; myLen5++) {
                    formatStr[pos3] = ' ';
                    pos3++;
                }
            }
            formatStr[pos3] = prefix;
            int pos4 = pos3 + 1;
            boolean zeropad = fieldLen != 0;
            int pos5 = printField(formatStr, days, DateFormat.DATE, pos4, false, 0);
            int pos6 = printField(formatStr, hours, DateFormat.HOUR, pos5, pos5 != pos4, zeropad ? 2 : 0);
            int pos7 = printField(formatStr, minutes, DateFormat.MINUTE, pos6, pos6 != pos4, zeropad ? 2 : 0);
            int pos8 = printField(formatStr, seconds, 's', pos7, pos7 != pos4, zeropad ? 2 : 0);
            int pos9 = printField(formatStr, millis, DateFormat.MINUTE, pos8, true, (!zeropad || pos8 == pos4) ? 0 : 3);
            formatStr[pos9] = 's';
            return pos9 + 1;
        }
    }

    public static void formatDuration(long duration, StringBuilder builder) {
        synchronized (sFormatSync) {
            int len = formatDurationLocked(duration, 0);
            builder.append(sFormatStr, 0, len);
        }
    }

    public static void formatDuration(long duration, PrintWriter pw, int fieldLen) {
        synchronized (sFormatSync) {
            int len = formatDurationLocked(duration, fieldLen);
            pw.print(new String(sFormatStr, 0, len));
        }
    }

    public static void formatDuration(long duration, PrintWriter pw) {
        formatDuration(duration, pw, 0);
    }

    public static void formatDuration(long time, long now, PrintWriter pw) {
        if (time == 0) {
            pw.print("--");
        } else {
            formatDuration(time - now, pw, 0);
        }
    }

    public static String formatUptime(long time) {
        long diff = time - SystemClock.uptimeMillis();
        if (diff > 0) {
            return time + " (in " + diff + " ms)";
        }
        if (diff < 0) {
            return time + " (" + (-diff) + " ms ago)";
        }
        return time + " (now)";
    }

    public static String logTimeOfDay(long millis) {
        Calendar c = Calendar.getInstance();
        if (millis < 0) {
            return Long.toString(millis);
        }
        c.setTimeInMillis(millis);
        return String.format("%tm-%td %tH:%tM:%tS.%tL", c, c, c, c, c, c);
    }
}
