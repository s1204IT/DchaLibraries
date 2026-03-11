package com.android.settingslib.datetime;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.icu.text.TimeZoneFormat;
import android.icu.text.TimeZoneNames;
import android.support.v4.text.BidiFormatter;
import android.support.v4.text.TextDirectionHeuristicsCompat;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.Log;
import com.android.settingslib.R;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import libcore.util.TimeZoneFinder;
import org.xmlpull.v1.XmlPullParserException;

public class ZoneGetter {
    public static CharSequence getTimeZoneOffsetAndName(Context context, TimeZone timeZone, Date date) {
        Locale locale = context.getResources().getConfiguration().locale;
        CharSequence gmtOffsetText = getGmtOffsetText(TimeZoneFormat.getInstance(locale), locale, timeZone, date);
        String zoneLongName = getZoneLongName(TimeZoneNames.getInstance(locale), timeZone, date);
        return zoneLongName == null ? gmtOffsetText : TextUtils.concat(gmtOffsetText, " ", zoneLongName);
    }

    public static List<Map<String, Object>> getZonesList(Context context) {
        Locale locale = context.getResources().getConfiguration().locale;
        Date date = new Date();
        TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(locale);
        ZoneGetterData zoneGetterData = new ZoneGetterData(context);
        boolean zShouldUseExemplarLocationForLocalNames = shouldUseExemplarLocationForLocalNames(zoneGetterData, timeZoneNames);
        ArrayList arrayList = new ArrayList();
        for (int i = 0; i < zoneGetterData.zoneCount; i++) {
            TimeZone timeZone = zoneGetterData.timeZones[i];
            CharSequence charSequence = zoneGetterData.gmtOffsetTexts[i];
            CharSequence timeZoneDisplayName = getTimeZoneDisplayName(zoneGetterData, timeZoneNames, zShouldUseExemplarLocationForLocalNames, timeZone, zoneGetterData.olsonIdsToDisplay[i]);
            if (TextUtils.isEmpty(timeZoneDisplayName)) {
                timeZoneDisplayName = charSequence;
            }
            arrayList.add(createDisplayEntry(timeZone, charSequence, timeZoneDisplayName, timeZone.getOffset(date.getTime())));
        }
        return arrayList;
    }

    private static Map<String, Object> createDisplayEntry(TimeZone timeZone, CharSequence charSequence, CharSequence charSequence2, int i) {
        HashMap map = new HashMap();
        map.put("id", timeZone.getID());
        map.put("name", charSequence2.toString());
        map.put("display_label", charSequence2);
        map.put("gmt", charSequence.toString());
        map.put("offset_label", charSequence);
        map.put("offset", Integer.valueOf(i));
        return map;
    }

    public static List<String> readTimezonesToDisplay(Context context) {
        ArrayList arrayList = new ArrayList();
        try {
            XmlResourceParser xml = context.getResources().getXml(R.xml.timezones);
            do {
                try {
                } finally {
                }
            } while (xml.next() != 2);
            xml.next();
            while (xml.getEventType() != 3) {
                while (xml.getEventType() != 2) {
                    if (xml.getEventType() == 1) {
                        if (xml != null) {
                            xml.close();
                        }
                        return arrayList;
                    }
                    xml.next();
                }
                if (xml.getName().equals("timezone")) {
                    arrayList.add(xml.getAttributeValue(0));
                }
                while (xml.getEventType() != 3) {
                    xml.next();
                }
                xml.next();
            }
            if (xml != null) {
                xml.close();
            }
        } catch (IOException e) {
            Log.e("ZoneGetter", "Unable to read timezones.xml file");
        } catch (XmlPullParserException e2) {
            Log.e("ZoneGetter", "Ill-formatted timezones.xml file");
        }
        return arrayList;
    }

    private static boolean shouldUseExemplarLocationForLocalNames(ZoneGetterData zoneGetterData, TimeZoneNames timeZoneNames) {
        HashSet hashSet = new HashSet();
        Date date = new Date();
        for (int i = 0; i < zoneGetterData.zoneCount; i++) {
            if (zoneGetterData.localZoneIds.contains(zoneGetterData.olsonIdsToDisplay[i])) {
                CharSequence zoneLongName = getZoneLongName(timeZoneNames, zoneGetterData.timeZones[i], date);
                if (zoneLongName == null) {
                    zoneLongName = zoneGetterData.gmtOffsetTexts[i];
                }
                if (!hashSet.add(zoneLongName)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static CharSequence getTimeZoneDisplayName(ZoneGetterData zoneGetterData, TimeZoneNames timeZoneNames, boolean z, TimeZone timeZone, String str) {
        Date date = new Date();
        if (zoneGetterData.localZoneIds.contains(str) && !z) {
            return getZoneLongName(timeZoneNames, timeZone, date);
        }
        String canonicalID = android.icu.util.TimeZone.getCanonicalID(timeZone.getID());
        if (canonicalID == null) {
            canonicalID = timeZone.getID();
        }
        String exemplarLocationName = timeZoneNames.getExemplarLocationName(canonicalID);
        if (exemplarLocationName == null || exemplarLocationName.isEmpty()) {
            return getZoneLongName(timeZoneNames, timeZone, date);
        }
        return exemplarLocationName;
    }

    private static String getZoneLongName(TimeZoneNames timeZoneNames, TimeZone timeZone, Date date) {
        return timeZoneNames.getDisplayName(timeZone.getID(), timeZone.inDaylightTime(date) ? TimeZoneNames.NameType.LONG_DAYLIGHT : TimeZoneNames.NameType.LONG_STANDARD, date.getTime());
    }

    private static void appendWithTtsSpan(SpannableStringBuilder spannableStringBuilder, CharSequence charSequence, TtsSpan ttsSpan) {
        int length = spannableStringBuilder.length();
        spannableStringBuilder.append(charSequence);
        spannableStringBuilder.setSpan(ttsSpan, length, spannableStringBuilder.length(), 0);
    }

    private static String formatDigits(int i, int i2, String str) {
        int i3 = i / 10;
        int i4 = i % 10;
        StringBuilder sb = new StringBuilder(i2);
        if (i >= 10 || i2 == 2) {
            sb.append(str.charAt(i3));
        }
        sb.append(str.charAt(i4));
        return sb.toString();
    }

    public static CharSequence getGmtOffsetText(TimeZoneFormat timeZoneFormat, Locale locale, TimeZone timeZone, Date date) {
        String strSubstring;
        String str;
        TimeZoneFormat.GMTOffsetPatternType gMTOffsetPatternType;
        int i;
        String str2;
        int i2;
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();
        String gMTPattern = timeZoneFormat.getGMTPattern();
        int iIndexOf = gMTPattern.indexOf("{0}");
        if (iIndexOf == -1) {
            str = "GMT";
            strSubstring = "";
        } else {
            String strSubstring2 = gMTPattern.substring(0, iIndexOf);
            strSubstring = gMTPattern.substring(iIndexOf + 3);
            str = strSubstring2;
        }
        if (!str.isEmpty()) {
            appendWithTtsSpan(spannableStringBuilder, str, new TtsSpan.TextBuilder(str).build());
        }
        int offset = timeZone.getOffset(date.getTime());
        boolean z = true;
        if (offset < 0) {
            offset = -offset;
            gMTOffsetPatternType = TimeZoneFormat.GMTOffsetPatternType.NEGATIVE_HM;
        } else {
            gMTOffsetPatternType = TimeZoneFormat.GMTOffsetPatternType.POSITIVE_HM;
        }
        String gMTOffsetPattern = timeZoneFormat.getGMTOffsetPattern(gMTOffsetPatternType);
        String gMTOffsetDigits = timeZoneFormat.getGMTOffsetDigits();
        long j = offset;
        int i3 = (int) (j / 3600000);
        int iAbs = Math.abs((int) (j / 60000)) % 60;
        int i4 = 0;
        while (i4 < gMTOffsetPattern.length()) {
            char cCharAt = gMTOffsetPattern.charAt(i4);
            if (cCharAt == '+' || cCharAt == '-' || cCharAt == 8722) {
                String strValueOf = String.valueOf(cCharAt);
                appendWithTtsSpan(spannableStringBuilder, strValueOf, new TtsSpan.VerbatimBuilder(strValueOf).build());
            } else if (cCharAt == 'H' || cCharAt == 'm') {
                int i5 = i4 + 1;
                if (i5 < gMTOffsetPattern.length() && gMTOffsetPattern.charAt(i5) == cCharAt) {
                    i = 2;
                } else {
                    i5 = i4;
                    i = 1;
                }
                if (cCharAt == 'H') {
                    str2 = "hour";
                    i2 = i3;
                } else {
                    str2 = "minute";
                    i2 = iAbs;
                }
                appendWithTtsSpan(spannableStringBuilder, formatDigits(i2, i, gMTOffsetDigits), new TtsSpan.MeasureBuilder().setNumber(i2).setUnit(str2).build());
                i4 = i5;
            } else {
                spannableStringBuilder.append(cCharAt);
            }
            i4++;
        }
        if (!strSubstring.isEmpty()) {
            appendWithTtsSpan(spannableStringBuilder, strSubstring, new TtsSpan.TextBuilder(strSubstring).build());
        }
        SpannableString spannableString = new SpannableString(spannableStringBuilder);
        BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        if (TextUtils.getLayoutDirectionFromLocale(locale) != 1) {
            z = false;
        }
        return bidiFormatter.unicodeWrap(spannableString, z ? TextDirectionHeuristicsCompat.RTL : TextDirectionHeuristicsCompat.LTR);
    }

    public static final class ZoneGetterData {
        public final CharSequence[] gmtOffsetTexts;
        public final Set<String> localZoneIds;
        public final String[] olsonIdsToDisplay;
        public final TimeZone[] timeZones;
        public final int zoneCount;

        public ZoneGetterData(Context context) {
            Locale locale = context.getResources().getConfiguration().locale;
            TimeZoneFormat timeZoneFormat = TimeZoneFormat.getInstance(locale);
            Date date = new Date();
            List timezonesToDisplay = ZoneGetter.readTimezonesToDisplay(context);
            this.zoneCount = timezonesToDisplay.size();
            this.olsonIdsToDisplay = new String[this.zoneCount];
            this.timeZones = new TimeZone[this.zoneCount];
            this.gmtOffsetTexts = new CharSequence[this.zoneCount];
            for (int i = 0; i < this.zoneCount; i++) {
                String str = (String) timezonesToDisplay.get(i);
                this.olsonIdsToDisplay[i] = str;
                TimeZone timeZone = TimeZone.getTimeZone(str);
                this.timeZones[i] = timeZone;
                this.gmtOffsetTexts[i] = ZoneGetter.getGmtOffsetText(timeZoneFormat, locale, timeZone, date);
            }
            this.localZoneIds = new HashSet(lookupTimeZoneIdsByCountry(locale.getCountry()));
        }

        public List<String> lookupTimeZoneIdsByCountry(String str) {
            return TimeZoneFinder.getInstance().lookupTimeZoneIdsByCountry(str);
        }
    }
}
