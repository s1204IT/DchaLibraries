package com.android.settingslib.datetime;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.icu.text.TimeZoneNames;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.TextUtils;
import android.util.Log;
import com.android.settingslib.R$xml;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import org.xmlpull.v1.XmlPullParserException;

public class ZoneGetter {
    private ZoneGetter() {
    }

    public static String getTimeZoneOffsetAndName(TimeZone tz, Date now) {
        Locale locale = Locale.getDefault();
        String gmtString = getGmtOffsetString(locale, tz, now);
        TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(locale);
        String zoneNameString = getZoneLongName(timeZoneNames, tz, now);
        if (zoneNameString == null) {
            return gmtString;
        }
        return gmtString + " " + zoneNameString;
    }

    public static List<Map<String, Object>> getZonesList(Context context) throws Throwable {
        String displayName;
        Locale locale = Locale.getDefault();
        Date now = new Date();
        TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(locale);
        List<String> olsonIdsToDisplayList = readTimezonesToDisplay(context);
        int zoneCount = olsonIdsToDisplayList.size();
        String[] olsonIdsToDisplay = new String[zoneCount];
        TimeZone[] timeZones = new TimeZone[zoneCount];
        String[] gmtOffsetStrings = new String[zoneCount];
        for (int i = 0; i < zoneCount; i++) {
            String olsonId = olsonIdsToDisplayList.get(i);
            olsonIdsToDisplay[i] = olsonId;
            TimeZone tz = TimeZone.getTimeZone(olsonId);
            timeZones[i] = tz;
            gmtOffsetStrings[i] = getGmtOffsetString(locale, tz, now);
        }
        Set<String> localZoneIds = new HashSet<>();
        for (String olsonId2 : libcore.icu.TimeZoneNames.forLocale(locale)) {
            localZoneIds.add(olsonId2);
        }
        Set<String> localZoneNames = new HashSet<>();
        boolean useExemplarLocationForLocalNames = false;
        int i2 = 0;
        while (true) {
            if (i2 >= zoneCount) {
                break;
            }
            String olsonId3 = olsonIdsToDisplay[i2];
            if (localZoneIds.contains(olsonId3)) {
                String displayName2 = getZoneLongName(timeZoneNames, timeZones[i2], now);
                if (displayName2 == null) {
                    displayName2 = gmtOffsetStrings[i2];
                }
                boolean nameIsUnique = localZoneNames.add(displayName2);
                if (!nameIsUnique) {
                    useExemplarLocationForLocalNames = true;
                    break;
                }
            }
            i2++;
        }
        List<Map<String, Object>> zones = new ArrayList<>();
        for (int i3 = 0; i3 < zoneCount; i3++) {
            String olsonId4 = olsonIdsToDisplay[i3];
            TimeZone tz2 = timeZones[i3];
            String gmtOffsetString = gmtOffsetStrings[i3];
            boolean isLocalZoneId = localZoneIds.contains(olsonId4);
            boolean preferLongName = isLocalZoneId && !useExemplarLocationForLocalNames;
            if (preferLongName || (displayName = timeZoneNames.getExemplarLocationName(tz2.getID())) == null || displayName.isEmpty()) {
                displayName = getZoneLongName(timeZoneNames, tz2, now);
            }
            if (displayName == null || displayName.isEmpty()) {
                displayName = gmtOffsetString;
            }
            int offsetMillis = tz2.getOffset(now.getTime());
            Map<String, Object> displayEntry = createDisplayEntry(tz2, gmtOffsetString, displayName, offsetMillis);
            zones.add(displayEntry);
        }
        return zones;
    }

    private static Map<String, Object> createDisplayEntry(TimeZone tz, String gmtOffsetString, String displayName, int offsetMillis) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", tz.getID());
        map.put("name", displayName);
        map.put("gmt", gmtOffsetString);
        map.put("offset", Integer.valueOf(offsetMillis));
        return map;
    }

    private static List<String> readTimezonesToDisplay(Context context) throws Throwable {
        Throwable th;
        Throwable th2 = null;
        List<String> olsonIds = new ArrayList<>();
        XmlResourceParser xrp = null;
        try {
            try {
                xrp = context.getResources().getXml(R$xml.timezones);
                while (xrp.next() != 2) {
                }
                xrp.next();
                while (xrp.getEventType() != 3) {
                    while (xrp.getEventType() != 2) {
                        if (xrp.getEventType() == 1) {
                            if (xrp != null) {
                                try {
                                    xrp.close();
                                } catch (Throwable th3) {
                                    th2 = th3;
                                }
                            }
                            if (th2 != null) {
                                throw th2;
                            }
                            return olsonIds;
                        }
                        xrp.next();
                    }
                    if (xrp.getName().equals("timezone")) {
                        String olsonId = xrp.getAttributeValue(0);
                        olsonIds.add(olsonId);
                    }
                    while (xrp.getEventType() != 3) {
                        xrp.next();
                    }
                    xrp.next();
                }
                if (xrp != null) {
                    try {
                        xrp.close();
                    } catch (Throwable th4) {
                        th2 = th4;
                    }
                }
                if (th2 != null) {
                    throw th2;
                }
            } catch (Throwable th5) {
                try {
                    throw th5;
                } catch (Throwable th6) {
                    th2 = th5;
                    th = th6;
                    if (xrp != null) {
                        try {
                            xrp.close();
                        } catch (Throwable th7) {
                            if (th2 == null) {
                                th2 = th7;
                            } else if (th2 != th7) {
                                th2.addSuppressed(th7);
                            }
                        }
                    }
                    if (th2 == null) {
                        throw th2;
                    }
                    throw th;
                }
            }
        } catch (IOException e) {
            Log.e("ZoneGetter", "Unable to read timezones.xml file");
        } catch (XmlPullParserException e2) {
            Log.e("ZoneGetter", "Ill-formatted timezones.xml file");
        }
        return olsonIds;
    }

    private static String getZoneLongName(TimeZoneNames names, TimeZone tz, Date now) {
        TimeZoneNames.NameType nameType = tz.inDaylightTime(now) ? TimeZoneNames.NameType.LONG_DAYLIGHT : TimeZoneNames.NameType.LONG_STANDARD;
        return names.getDisplayName(tz.getID(), nameType, now.getTime());
    }

    private static String getGmtOffsetString(Locale locale, TimeZone tz, Date now) {
        SimpleDateFormat gmtFormatter = new SimpleDateFormat("ZZZZ");
        gmtFormatter.setTimeZone(tz);
        String gmtString = gmtFormatter.format(now);
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        boolean isRtl = TextUtils.getLayoutDirectionFromLocale(locale) == 1;
        return bidiFormatter.unicodeWrap(gmtString, isRtl ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR);
    }
}
