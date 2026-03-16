package com.android.calendar;

import android.accounts.Account;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.widget.SearchView;
import com.android.calendar.CalendarEventModel;
import com.android.calendar.CalendarUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    static int WORK_DAY_MINUTES = 840;
    static int WORK_DAY_START_MINUTES = 360;
    static int WORK_DAY_END_MINUTES = 1200;
    static int WORK_DAY_END_LENGTH = 1440 - WORK_DAY_END_MINUTES;
    static int CONFLICT_COLOR = -16777216;
    static boolean mMinutesLoaded = false;
    private static final CalendarUtils.TimeZoneUtils mTZUtils = new CalendarUtils.TimeZoneUtils("com.android.calendar_preferences");
    private static boolean mAllowWeekForDetailView = false;
    private static long mTardis = 0;
    private static String sVersion = null;
    private static final Pattern mWildcardPattern = Pattern.compile("^.*$");
    private static final Pattern COORD_PATTERN = Pattern.compile("([-+NnSs](\\s)*)?[1-9]?[0-9](°)(\\s)*([1-5]?[0-9]')?(\\s)*([1-5]?[0-9](\\.[0-9]+)?\")?((\\s)*[NnSs])?(\\s)*,(\\s)*([-+EeWw](\\s)*)?(1)?[0-9]?[0-9](°)(\\s)*([1-5]?[0-9]')?(\\s)*([1-5]?[0-9](\\.[0-9]+)?\")?((\\s)*[EeWw])?|[+-]?[1-9]?[0-9](\\.[0-9]+)(°)?(\\s)*,(\\s)*[+-]?(1)?[0-9]?[0-9](\\.[0-9]+)(°)?");

    public static class DNAStrand {
        public int[] allDays;
        public int color;
        int count;
        public float[] points;
        int position;
    }

    public static boolean isJellybeanOrLater() {
        return Build.VERSION.SDK_INT >= 16;
    }

    public static boolean isKeyLimePieOrLater() {
        return Build.VERSION.SDK_INT >= 19;
    }

    public static int getViewTypeFromIntentAndSharedPref(Activity activity) {
        Intent intent = activity.getIntent();
        Bundle extras = intent.getExtras();
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(activity);
        if (TextUtils.equals(intent.getAction(), "android.intent.action.EDIT")) {
            return 5;
        }
        if (extras != null) {
            if (extras.getBoolean("DETAIL_VIEW", false)) {
                return prefs.getInt("preferred_detailedView", 2);
            }
            if ("DAY".equals(extras.getString("VIEW"))) {
                return 2;
            }
        }
        return prefs.getInt("preferred_startView", 3);
    }

    public static String getWidgetUpdateAction(Context context) {
        return context.getPackageName() + ".APPWIDGET_UPDATE";
    }

    public static String getWidgetScheduledUpdateAction(Context context) {
        return context.getPackageName() + ".APPWIDGET_SCHEDULED_UPDATE";
    }

    public static String getSearchAuthority(Context context) {
        return context.getPackageName() + ".CalendarRecentSuggestionsProvider";
    }

    public static void setTimeZone(Context context, String timeZone) {
        mTZUtils.setTimeZone(context, timeZone);
    }

    public static String getTimeZone(Context context, Runnable callback) {
        return mTZUtils.getTimeZone(context, callback);
    }

    public static String formatDateRange(Context context, long startMillis, long endMillis, int flags) {
        return mTZUtils.formatDateRange(context, startMillis, endMillis, flags);
    }

    public static boolean getDefaultVibrate(Context context, SharedPreferences prefs) {
        boolean vibrate = false;
        if (prefs.contains("preferences_alerts_vibrateWhen")) {
            String vibrateWhen = prefs.getString("preferences_alerts_vibrateWhen", null);
            if (vibrateWhen != null && vibrateWhen.equals(context.getString(R.string.prefDefault_alerts_vibrate_true))) {
                vibrate = true;
            }
            prefs.edit().remove("preferences_alerts_vibrateWhen").commit();
            Log.d("CalUtils", "Migrating KEY_ALERTS_VIBRATE_WHEN(" + vibrateWhen + ") to KEY_ALERTS_VIBRATE = " + vibrate);
            return vibrate;
        }
        return prefs.getBoolean("preferences_alerts_vibrate", false);
    }

    public static String[] getSharedPreference(Context context, String key, String[] defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        Set<String> ss = prefs.getStringSet(key, null);
        if (ss == null) {
            return defaultValue;
        }
        String[] strings = new String[ss.size()];
        return (String[]) ss.toArray(strings);
    }

    public static String getSharedPreference(Context context, String key, String defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getString(key, defaultValue);
    }

    public static int getSharedPreference(Context context, String key, int defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getInt(key, defaultValue);
    }

    public static boolean getSharedPreference(Context context, String key, boolean defaultValue) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean(key, defaultValue);
    }

    public static void setSharedPreference(Context context, String key, String value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        prefs.edit().putString(key, value).apply();
    }

    public static void setSharedPreference(Context context, String key, String[] values) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            set.add(value);
        }
        prefs.edit().putStringSet(key, set).apply();
    }

    protected static void tardis() {
        mTardis = System.currentTimeMillis();
    }

    protected static long getTardis() {
        return mTardis;
    }

    public static void setSharedPreference(Context context, String key, boolean value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    static void setSharedPreference(Context context, String key, int value) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public static String getRingTonePreference(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("com.android.calendar_preferences_no_backup", 0);
        String ringtone = prefs.getString("preferences_alerts_ringtone", null);
        if (ringtone == null) {
            String ringtone2 = getSharedPreference(context, "preferences_alerts_ringtone", "content://settings/system/notification_sound");
            setRingTonePreference(context, ringtone2);
            return ringtone2;
        }
        return ringtone;
    }

    public static void setRingTonePreference(Context context, String value) {
        SharedPreferences prefs = context.getSharedPreferences("com.android.calendar_preferences_no_backup", 0);
        prefs.edit().putString("preferences_alerts_ringtone", value).apply();
    }

    static void setDefaultView(Context context, int viewId) {
        boolean validDetailView;
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        if (mAllowWeekForDetailView && viewId == 3) {
            validDetailView = true;
        } else {
            validDetailView = viewId == 1 || viewId == 2;
        }
        if (validDetailView) {
            editor.putInt("preferred_detailedView", viewId);
        }
        editor.putInt("preferred_startView", viewId);
        editor.apply();
    }

    public static MatrixCursor matrixCursorFromCursor(Cursor cursor) {
        if (cursor == null) {
            return null;
        }
        String[] columnNames = cursor.getColumnNames();
        if (columnNames == null) {
            columnNames = new String[0];
        }
        MatrixCursor newCursor = new MatrixCursor(columnNames);
        int numColumns = cursor.getColumnCount();
        String[] data = new String[numColumns];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                data[i] = cursor.getString(i);
            }
            newCursor.addRow(data);
        }
        return newCursor;
    }

    public static boolean compareCursors(Cursor c1, Cursor c2) {
        int numColumns;
        if (c1 == null || c2 == null || (numColumns = c1.getColumnCount()) != c2.getColumnCount() || c1.getCount() != c2.getCount()) {
            return false;
        }
        c1.moveToPosition(-1);
        c2.moveToPosition(-1);
        while (c1.moveToNext() && c2.moveToNext()) {
            for (int i = 0; i < numColumns; i++) {
                if (!TextUtils.equals(c1.getString(i), c2.getString(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static final long timeFromIntentInMillis(Intent intent) {
        Uri data = intent.getData();
        long millis = intent.getLongExtra("beginTime", -1L);
        if (millis == -1 && data != null && data.isHierarchical()) {
            List<String> path = data.getPathSegments();
            if (path.size() == 2 && path.get(0).equals("time")) {
                try {
                    millis = Long.valueOf(data.getLastPathSegment()).longValue();
                } catch (NumberFormatException e) {
                    Log.i("Calendar", "timeFromIntentInMillis: Data existed but no valid time found. Using current time.");
                }
            }
        }
        if (millis <= 0) {
            return System.currentTimeMillis();
        }
        return millis;
    }

    public static String formatMonthYear(Context context, Time time) {
        long millis = time.toMillis(true);
        return formatDateRange(context, millis, millis, 52);
    }

    public static int getWeeksSinceEpochFromJulianDay(int julianDay, int firstDayOfWeek) {
        int diff = 4 - firstDayOfWeek;
        if (diff < 0) {
            diff += 7;
        }
        int refDay = 2440588 - diff;
        return (julianDay - refDay) / 7;
    }

    public static int getJulianMondayFromWeeksSinceEpoch(int week) {
        return 2440585 + (week * 7);
    }

    public static int getFirstDayOfWeek(Context context) {
        int startDay;
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        String pref = prefs.getString("preferences_week_start_day", "-1");
        if ("-1".equals(pref)) {
            startDay = Calendar.getInstance().getFirstDayOfWeek();
        } else {
            startDay = Integer.parseInt(pref);
        }
        if (startDay == 7) {
            return 6;
        }
        if (startDay == 2) {
            return 1;
        }
        return 0;
    }

    public static int getFirstDayOfWeekAsCalendar(Context context) {
        return convertDayOfWeekFromTimeToCalendar(getFirstDayOfWeek(context));
    }

    public static int convertDayOfWeekFromTimeToCalendar(int timeDayOfWeek) {
        switch (timeDayOfWeek) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 3;
            case 3:
                return 4;
            case 4:
                return 5;
            case 5:
                return 6;
            case 6:
                return 7;
            default:
                throw new IllegalArgumentException("Argument must be between Time.SUNDAY and Time.SATURDAY");
        }
    }

    public static boolean getShowWeekNumber(Context context) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean("preferences_show_week_num", false);
    }

    public static boolean getHideDeclinedEvents(Context context) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getBoolean("preferences_hide_declined", false);
    }

    public static int getDaysPerWeek(Context context) {
        SharedPreferences prefs = GeneralPreferences.getSharedPreferences(context);
        return prefs.getInt("preferences_days_per_week", 7);
    }

    public static boolean isSaturday(int column, int firstDayOfWeek) {
        if (firstDayOfWeek == 0 && column == 6) {
            return true;
        }
        if (firstDayOfWeek == 1 && column == 5) {
            return true;
        }
        return firstDayOfWeek == 6 && column == 0;
    }

    public static boolean isSunday(int column, int firstDayOfWeek) {
        if (firstDayOfWeek == 0 && column == 0) {
            return true;
        }
        if (firstDayOfWeek == 1 && column == 6) {
            return true;
        }
        return firstDayOfWeek == 6 && column == 1;
    }

    public static long convertAlldayUtcToLocal(Time recycle, long utcTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = "UTC";
        recycle.set(utcTime);
        recycle.timezone = tz;
        return recycle.normalize(true);
    }

    public static long convertAlldayLocalToUTC(Time recycle, long localTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = tz;
        recycle.set(localTime);
        recycle.timezone = "UTC";
        return recycle.normalize(true);
    }

    public static long getNextMidnight(Time recycle, long theTime, String tz) {
        if (recycle == null) {
            recycle = new Time();
        }
        recycle.timezone = tz;
        recycle.set(theTime);
        recycle.monthDay++;
        recycle.hour = 0;
        recycle.minute = 0;
        recycle.second = 0;
        return recycle.normalize(true);
    }

    public static void checkForDuplicateNames(Map<String, Boolean> isDuplicateName, Cursor cursor, int nameIndex) {
        isDuplicateName.clear();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String displayName = cursor.getString(nameIndex);
            if (displayName != null) {
                isDuplicateName.put(displayName, Boolean.valueOf(isDuplicateName.containsKey(displayName)));
            }
        }
    }

    public static boolean equals(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    public static void setAllowWeekForDetailView(boolean allowWeekView) {
        mAllowWeekForDetailView = allowWeekView;
    }

    public static boolean getAllowWeekForDetailView() {
        return mAllowWeekForDetailView;
    }

    public static boolean getConfigBool(Context c, int key) {
        return c.getResources().getBoolean(key);
    }

    public static int getDisplayColorFromColor(int color) {
        if (isJellybeanOrLater()) {
            float[] hsv = new float[3];
            Color.colorToHSV(color, hsv);
            hsv[1] = Math.min(hsv[1] * 1.3f, 1.0f);
            hsv[2] = hsv[2] * 0.8f;
            return Color.HSVToColor(hsv);
        }
        return color;
    }

    public static int getDeclinedColorFromColor(int color) {
        int r = (((color & 16711680) * 102) - 1738080256) & (-16777216);
        int g = (((color & 65280) * 102) + 9987840) & 16711680;
        int b = (((color & 255) * 102) + 39015) & 65280;
        return (((r | g) | b) >> 8) | (-16777216);
    }

    public static void trySyncAndDisableUpgradeReceiver(Context context) {
        PackageManager pm = context.getPackageManager();
        ComponentName upgradeComponent = new ComponentName(context, (Class<?>) UpgradeReceiver.class);
        if (pm.getComponentEnabledSetting(upgradeComponent) != 2) {
            Bundle extras = new Bundle();
            extras.putBoolean("force", true);
            ContentResolver.requestSync(null, CalendarContract.Calendars.CONTENT_URI.getAuthority(), extras);
            pm.setComponentEnabledSetting(upgradeComponent, 2, 1);
        }
    }

    private static class DNASegment {
        int color;
        int day;
        int endMinute;
        int startMinute;

        private DNASegment() {
        }
    }

    public static HashMap<Integer, DNAStrand> createDNAStrands(int firstJulianDay, ArrayList<Event> events, int top, int bottom, int minPixels, int[] dayXs, Context context) {
        int i;
        if (!mMinutesLoaded) {
            if (context == null) {
                Log.wtf("CalUtils", "No context and haven't loaded parameters yet! Can't create DNA.");
            }
            Resources res = context.getResources();
            CONFLICT_COLOR = res.getColor(R.color.month_dna_conflict_time_color);
            WORK_DAY_START_MINUTES = res.getInteger(R.integer.work_start_minutes);
            WORK_DAY_END_MINUTES = res.getInteger(R.integer.work_end_minutes);
            WORK_DAY_END_LENGTH = 1440 - WORK_DAY_END_MINUTES;
            WORK_DAY_MINUTES = WORK_DAY_END_MINUTES - WORK_DAY_START_MINUTES;
            mMinutesLoaded = true;
        }
        if (events == null || events.isEmpty() || dayXs == null || dayXs.length < 1 || bottom - top < 8 || minPixels < 0) {
            Log.e("CalUtils", "Bad values for createDNAStrands! events:" + events + " dayXs:" + Arrays.toString(dayXs) + " bot-top:" + (bottom - top) + " minPixels:" + minPixels);
            return null;
        }
        LinkedList<DNASegment> segments = new LinkedList<>();
        HashMap<Integer, DNAStrand> strands = new HashMap<>();
        DNAStrand blackStrand = new DNAStrand();
        blackStrand.color = CONFLICT_COLOR;
        strands.put(Integer.valueOf(CONFLICT_COLOR), blackStrand);
        int minMinutes = ((minPixels * 4) * WORK_DAY_MINUTES) / ((bottom - top) * 3);
        int minOtherMinutes = (minMinutes * 5) / 2;
        int lastJulianDay = (dayXs.length + firstJulianDay) - 1;
        Event event = new Event();
        for (Event currEvent : events) {
            if (currEvent.endDay >= firstJulianDay && currEvent.startDay <= lastJulianDay) {
                if (currEvent.drawAsAllday()) {
                    addAllDayToStrands(currEvent, strands, firstJulianDay, dayXs.length);
                } else {
                    currEvent.copyTo(event);
                    if (event.startDay < firstJulianDay) {
                        event.startDay = firstJulianDay;
                        event.startTime = 0;
                    }
                    if (event.startTime > 1440 - minOtherMinutes) {
                        event.startTime = 1440 - minOtherMinutes;
                    }
                    if (event.endDay > lastJulianDay) {
                        event.endDay = lastJulianDay;
                        event.endTime = 1439;
                    }
                    if (event.endTime < minOtherMinutes) {
                        event.endTime = minOtherMinutes;
                    }
                    if (event.startDay == event.endDay && event.endTime - event.startTime < minOtherMinutes) {
                        if (event.startTime < WORK_DAY_START_MINUTES) {
                            event.endTime = Math.min(event.startTime + minOtherMinutes, WORK_DAY_START_MINUTES + minMinutes);
                        } else if (event.endTime > WORK_DAY_END_MINUTES) {
                            event.endTime = Math.min(event.endTime + minOtherMinutes, 1439);
                            if (event.endTime - event.startTime < minOtherMinutes) {
                                event.startTime = event.endTime - minOtherMinutes;
                            }
                        }
                    }
                    if (segments.size() == 0) {
                        addNewSegment(segments, event, strands, firstJulianDay, 0, minMinutes);
                    } else {
                        DNASegment lastSegment = segments.getLast();
                        int startMinute = ((event.startDay - firstJulianDay) * 1440) + event.startTime;
                        int endMinute = Math.max(((event.endDay - firstJulianDay) * 1440) + event.endTime, startMinute + minMinutes);
                        if (startMinute < 0) {
                            startMinute = 0;
                        }
                        if (endMinute >= 10080) {
                            endMinute = 10079;
                        }
                        if (startMinute < lastSegment.endMinute) {
                            int i2 = segments.size();
                            do {
                                i2--;
                                if (i2 < 0) {
                                    break;
                                }
                            } while (endMinute < segments.get(i2).startMinute);
                            for (int i3 = i2; i3 >= 0; i3 = i - 1) {
                                DNASegment currSegment = segments.get(i3);
                                if (startMinute > currSegment.endMinute) {
                                    break;
                                }
                                if (currSegment.color == CONFLICT_COLOR) {
                                    i = i3;
                                } else {
                                    if (endMinute < currSegment.endMinute - minMinutes) {
                                        DNASegment rhs = new DNASegment();
                                        rhs.endMinute = currSegment.endMinute;
                                        rhs.color = currSegment.color;
                                        rhs.startMinute = endMinute + 1;
                                        rhs.day = currSegment.day;
                                        currSegment.endMinute = endMinute;
                                        segments.add(i3 + 1, rhs);
                                        strands.get(Integer.valueOf(rhs.color)).count++;
                                    }
                                    if (startMinute > currSegment.startMinute + minMinutes) {
                                        DNASegment lhs = new DNASegment();
                                        lhs.startMinute = currSegment.startMinute;
                                        lhs.color = currSegment.color;
                                        lhs.endMinute = startMinute - 1;
                                        lhs.day = currSegment.day;
                                        currSegment.startMinute = startMinute;
                                        i = i3 + 1;
                                        segments.add(i3, lhs);
                                        strands.get(Integer.valueOf(lhs.color)).count++;
                                    } else {
                                        i = i3;
                                    }
                                    if (i + 1 < segments.size()) {
                                        DNASegment rhs2 = segments.get(i + 1);
                                        if (rhs2.color == CONFLICT_COLOR && currSegment.day == rhs2.day && rhs2.startMinute <= currSegment.endMinute + 1) {
                                            rhs2.startMinute = Math.min(currSegment.startMinute, rhs2.startMinute);
                                            segments.remove(currSegment);
                                            DNAStrand dNAStrand = strands.get(Integer.valueOf(currSegment.color));
                                            dNAStrand.count--;
                                            currSegment = rhs2;
                                        }
                                    }
                                    if (i - 1 >= 0) {
                                        DNASegment lhs2 = segments.get(i - 1);
                                        if (lhs2.color == CONFLICT_COLOR && currSegment.day == lhs2.day && lhs2.endMinute >= currSegment.startMinute - 1) {
                                            lhs2.endMinute = Math.max(currSegment.endMinute, lhs2.endMinute);
                                            segments.remove(currSegment);
                                            DNAStrand dNAStrand2 = strands.get(Integer.valueOf(currSegment.color));
                                            dNAStrand2.count--;
                                            currSegment = lhs2;
                                            i--;
                                        }
                                    }
                                    if (currSegment.color != CONFLICT_COLOR) {
                                        DNAStrand dNAStrand3 = strands.get(Integer.valueOf(currSegment.color));
                                        dNAStrand3.count--;
                                        currSegment.color = CONFLICT_COLOR;
                                        strands.get(Integer.valueOf(CONFLICT_COLOR)).count++;
                                    }
                                }
                            }
                        }
                        if (endMinute > lastSegment.endMinute) {
                            addNewSegment(segments, event, strands, firstJulianDay, lastSegment.endMinute, minMinutes);
                        }
                    }
                }
            }
        }
        weaveDNAStrands(segments, firstJulianDay, strands, top, bottom, dayXs);
        return strands;
    }

    private static void addAllDayToStrands(Event event, HashMap<Integer, DNAStrand> strands, int firstJulianDay, int numDays) {
        DNAStrand strand = getOrCreateStrand(strands, CONFLICT_COLOR);
        if (strand.allDays == null) {
            strand.allDays = new int[numDays];
        }
        int end = Math.min(event.endDay - firstJulianDay, numDays - 1);
        for (int i = Math.max(event.startDay - firstJulianDay, 0); i <= end; i++) {
            if (strand.allDays[i] != 0) {
                strand.allDays[i] = CONFLICT_COLOR;
            } else {
                strand.allDays[i] = event.color;
            }
        }
    }

    private static void weaveDNAStrands(LinkedList<DNASegment> segments, int firstJulianDay, HashMap<Integer, DNAStrand> strands, int top, int bottom, int[] dayXs) {
        Iterator<DNAStrand> strandIterator = strands.values().iterator();
        while (strandIterator.hasNext()) {
            DNAStrand strand = strandIterator.next();
            if (strand.count < 1 && strand.allDays == null) {
                strandIterator.remove();
            } else {
                strand.points = new float[strand.count * 4];
                strand.position = 0;
            }
        }
        for (DNASegment segment : segments) {
            DNAStrand strand2 = strands.get(Integer.valueOf(segment.color));
            int dayIndex = segment.day - firstJulianDay;
            int dayStartMinute = segment.startMinute % 1440;
            int dayEndMinute = segment.endMinute % 1440;
            int height = bottom - top;
            int workDayHeight = (height * 3) / 4;
            int remainderHeight = (height - workDayHeight) / 2;
            int x = dayXs[dayIndex];
            int y0 = top + getPixelOffsetFromMinutes(dayStartMinute, workDayHeight, remainderHeight);
            int y1 = top + getPixelOffsetFromMinutes(dayEndMinute, workDayHeight, remainderHeight);
            float[] fArr = strand2.points;
            int i = strand2.position;
            strand2.position = i + 1;
            fArr[i] = x;
            float[] fArr2 = strand2.points;
            int i2 = strand2.position;
            strand2.position = i2 + 1;
            fArr2[i2] = y0;
            float[] fArr3 = strand2.points;
            int i3 = strand2.position;
            strand2.position = i3 + 1;
            fArr3[i3] = x;
            float[] fArr4 = strand2.points;
            int i4 = strand2.position;
            strand2.position = i4 + 1;
            fArr4[i4] = y1;
        }
    }

    private static int getPixelOffsetFromMinutes(int minute, int workDayHeight, int remainderHeight) {
        if (minute < WORK_DAY_START_MINUTES) {
            int y = (minute * remainderHeight) / WORK_DAY_START_MINUTES;
            return y;
        }
        if (minute < WORK_DAY_END_MINUTES) {
            int y2 = remainderHeight + (((minute - WORK_DAY_START_MINUTES) * workDayHeight) / WORK_DAY_MINUTES);
            return y2;
        }
        int y3 = remainderHeight + workDayHeight + (((minute - WORK_DAY_END_MINUTES) * remainderHeight) / WORK_DAY_END_LENGTH);
        return y3;
    }

    private static void addNewSegment(LinkedList<DNASegment> segments, Event event, HashMap<Integer, DNAStrand> strands, int firstJulianDay, int minStart, int minMinutes) {
        if (event.startDay > event.endDay) {
            Log.wtf("CalUtils", "Event starts after it ends: " + event.toString());
        }
        if (event.startDay != event.endDay) {
            Event lhs = new Event();
            lhs.color = event.color;
            lhs.startDay = event.startDay;
            lhs.startTime = event.startTime;
            lhs.endDay = lhs.startDay;
            lhs.endTime = 1439;
            while (lhs.startDay != event.endDay) {
                addNewSegment(segments, lhs, strands, firstJulianDay, minStart, minMinutes);
                lhs.startDay++;
                lhs.endDay = lhs.startDay;
                lhs.startTime = 0;
                minStart = 0;
            }
            lhs.endTime = event.endTime;
            event = lhs;
        }
        DNASegment segment = new DNASegment();
        int dayOffset = (event.startDay - firstJulianDay) * 1440;
        int endOfDay = (dayOffset + 1440) - 1;
        segment.startMinute = Math.max(event.startTime + dayOffset, minStart);
        int minEnd = Math.min(segment.startMinute + minMinutes, endOfDay);
        segment.endMinute = Math.max(event.endTime + dayOffset, minEnd);
        if (segment.endMinute > endOfDay) {
            segment.endMinute = endOfDay;
        }
        segment.color = event.color;
        segment.day = event.startDay;
        segments.add(segment);
        DNAStrand strand = getOrCreateStrand(strands, segment.color);
        strand.count++;
    }

    private static DNAStrand getOrCreateStrand(HashMap<Integer, DNAStrand> strands, int color) {
        DNAStrand strand = strands.get(Integer.valueOf(color));
        if (strand == null) {
            DNAStrand strand2 = new DNAStrand();
            strand2.color = color;
            strand2.count = 0;
            strands.put(Integer.valueOf(strand2.color), strand2);
            return strand2;
        }
        return strand;
    }

    public static void returnToCalendarHome(Context context) {
        Intent launchIntent = new Intent(context, (Class<?>) AllInOneActivity.class);
        launchIntent.setAction("android.intent.action.VIEW");
        launchIntent.setFlags(67108864);
        launchIntent.putExtra("KEY_HOME", true);
        context.startActivity(launchIntent);
    }

    public static void setUpSearchView(SearchView view, Activity act) {
        SearchManager searchManager = (SearchManager) act.getSystemService("search");
        view.setSearchableInfo(searchManager.getSearchableInfo(act.getComponentName()));
        view.setQueryRefinementEnabled(true);
    }

    public static int getWeekNumberFromTime(long millisSinceEpoch, Context context) {
        Time weekTime = new Time(getTimeZone(context, null));
        weekTime.set(millisSinceEpoch);
        weekTime.normalize(true);
        int firstDayOfWeek = getFirstDayOfWeek(context);
        if (weekTime.weekDay == 0 && (firstDayOfWeek == 0 || firstDayOfWeek == 6)) {
            weekTime.monthDay++;
            weekTime.normalize(true);
        } else if (weekTime.weekDay == 6 && firstDayOfWeek == 6) {
            weekTime.monthDay += 2;
            weekTime.normalize(true);
        }
        return weekTime.getWeekNumber();
    }

    public static String getDayOfWeekString(int julianDay, int todayJulianDay, long millis, Context context) {
        String dayViewText;
        getTimeZone(context, null);
        if (julianDay == todayJulianDay) {
            dayViewText = context.getString(R.string.agenda_today, mTZUtils.formatDateRange(context, millis, millis, 2).toString());
        } else if (julianDay == todayJulianDay - 1) {
            dayViewText = context.getString(R.string.agenda_yesterday, mTZUtils.formatDateRange(context, millis, millis, 2).toString());
        } else if (julianDay != todayJulianDay + 1) {
            dayViewText = mTZUtils.formatDateRange(context, millis, millis, 2).toString();
        } else {
            dayViewText = context.getString(R.string.agenda_tomorrow, mTZUtils.formatDateRange(context, millis, millis, 2).toString());
        }
        return dayViewText.toUpperCase();
    }

    public static void setMidnightUpdater(Handler h, Runnable r, String timezone) {
        if (h != null && r != null && timezone != null) {
            long now = System.currentTimeMillis();
            Time time = new Time(timezone);
            time.set(now);
            long runInMillis = ((((86400 - (time.hour * 3600)) - (time.minute * 60)) - time.second) + 1) * 1000;
            h.removeCallbacks(r);
            h.postDelayed(r, runInMillis);
        }
    }

    public static void resetMidnightUpdater(Handler h, Runnable r) {
        if (h != null && r != null) {
            h.removeCallbacks(r);
        }
    }

    public static String getDisplayedDatetime(long startMillis, long endMillis, long currentMillis, String localTimezone, boolean allDay, Context context) {
        int flagsTime = 1;
        if (DateFormat.is24HourFormat(context)) {
            flagsTime = 1 | 128;
        }
        Time currentTime = new Time(localTimezone);
        currentTime.set(currentMillis);
        Resources resources = context.getResources();
        String datetimeString = null;
        if (allDay) {
            long localStartMillis = convertAlldayUtcToLocal(null, startMillis, localTimezone);
            long localEndMillis = convertAlldayUtcToLocal(null, endMillis, localTimezone);
            if (singleDayEvent(localStartMillis, localEndMillis, currentTime.gmtoff)) {
                int todayOrTomorrow = isTodayOrTomorrow(context.getResources(), localStartMillis, currentMillis, currentTime.gmtoff);
                if (1 == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.today);
                } else if (2 == todayOrTomorrow) {
                    datetimeString = resources.getString(R.string.tomorrow);
                }
            }
            if (datetimeString == null) {
                Formatter f = new Formatter(new StringBuilder(50), Locale.getDefault());
                return DateUtils.formatDateRange(context, f, startMillis, endMillis, 18, "UTC").toString();
            }
            return datetimeString;
        }
        if (singleDayEvent(startMillis, endMillis, currentTime.gmtoff)) {
            String timeString = formatDateRange(context, startMillis, endMillis, flagsTime);
            int todayOrTomorrow2 = isTodayOrTomorrow(context.getResources(), startMillis, currentMillis, currentTime.gmtoff);
            if (1 == todayOrTomorrow2) {
                return resources.getString(R.string.today_at_time_fmt, timeString);
            }
            if (2 == todayOrTomorrow2) {
                return resources.getString(R.string.tomorrow_at_time_fmt, timeString);
            }
            String dateString = formatDateRange(context, startMillis, endMillis, 18);
            return resources.getString(R.string.date_time_fmt, dateString, timeString);
        }
        int flagsDatetime = 18 | flagsTime | 65536 | 32768;
        return formatDateRange(context, startMillis, endMillis, flagsDatetime);
    }

    public static String getDisplayedTimezone(long startMillis, String localTimezone, String eventTimezone) {
        if (TextUtils.equals(localTimezone, eventTimezone)) {
            return null;
        }
        TimeZone tz = TimeZone.getTimeZone(localTimezone);
        if (tz == null || tz.getID().equals("GMT")) {
            return localTimezone;
        }
        Time startTime = new Time(localTimezone);
        startTime.set(startMillis);
        String tzDisplay = tz.getDisplayName(startTime.isDst != 0, 0);
        return tzDisplay;
    }

    private static boolean singleDayEvent(long startMillis, long endMillis, long localGmtOffset) {
        if (startMillis == endMillis) {
            return true;
        }
        int startDay = Time.getJulianDay(startMillis, localGmtOffset);
        int endDay = Time.getJulianDay(endMillis - 1, localGmtOffset);
        return startDay == endDay;
    }

    private static int isTodayOrTomorrow(Resources r, long dayMillis, long currentMillis, long localGmtOffset) {
        int startDay = Time.getJulianDay(dayMillis, localGmtOffset);
        int currentDay = Time.getJulianDay(currentMillis, localGmtOffset);
        int days = startDay - currentDay;
        if (days == 1) {
            return 2;
        }
        return days != 0 ? 0 : 1;
    }

    public static Intent createEmailAttendeesIntent(Resources resources, String eventTitle, String body, List<String> toEmails, List<String> ccEmails, String ownerAccount) {
        List<String> toList = toEmails;
        List<String> ccList = ccEmails;
        if (toEmails.size() <= 0) {
            if (ccEmails.size() <= 0) {
                throw new IllegalArgumentException("Both toEmails and ccEmails are empty.");
            }
            toList = ccEmails;
            ccList = null;
        }
        String subject = null;
        if (eventTitle != null) {
            subject = resources.getString(R.string.email_subject_prefix) + eventTitle;
        }
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme("mailto");
        if (toList.size() > 1) {
            for (int i = 1; i < toList.size(); i++) {
                uriBuilder.appendQueryParameter("to", toList.get(i));
            }
        }
        if (subject != null) {
            uriBuilder.appendQueryParameter("subject", subject);
        }
        if (body != null) {
            uriBuilder.appendQueryParameter("body", body);
        }
        if (ccList != null && ccList.size() > 0) {
            for (String email : ccList) {
                uriBuilder.appendQueryParameter("cc", email);
            }
        }
        String uri = uriBuilder.toString();
        if (uri.startsWith("mailto:")) {
            StringBuilder builder = new StringBuilder(uri);
            builder.insert(7, Uri.encode(toList.get(0)));
            uri = builder.toString();
        }
        Intent emailIntent = new Intent("android.intent.action.SENDTO", Uri.parse(uri));
        emailIntent.putExtra("fromAccountString", ownerAccount);
        if (body != null) {
            emailIntent.putExtra("android.intent.extra.TEXT", body);
        }
        return Intent.createChooser(emailIntent, resources.getString(R.string.email_picker_label));
    }

    public static boolean isValidEmail(String email) {
        return (email == null || email.endsWith("calendar.google.com")) ? false : true;
    }

    public static boolean isEmailableFrom(String email, String syncAccountName) {
        return isValidEmail(email) && !email.equals(syncAccountName);
    }

    public static void setTodayIcon(LayerDrawable icon, Context c, String timezone) {
        DayOfMonthDrawable today;
        Drawable currentDrawable = icon.findDrawableByLayerId(R.id.today_icon_day);
        if (currentDrawable != null && (currentDrawable instanceof DayOfMonthDrawable)) {
            today = (DayOfMonthDrawable) currentDrawable;
        } else {
            today = new DayOfMonthDrawable(c);
        }
        Time now = new Time(timezone);
        now.setToNow();
        now.normalize(false);
        today.setDayOfMonth(now.monthDay);
        icon.mutate();
        icon.setDrawableByLayerId(R.id.today_icon_day, today);
    }

    private static class CalendarBroadcastReceiver extends BroadcastReceiver {
        Runnable mCallBack;

        public CalendarBroadcastReceiver(Runnable callback) {
            this.mCallBack = callback;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ((intent.getAction().equals("android.intent.action.DATE_CHANGED") || intent.getAction().equals("android.intent.action.TIME_SET") || intent.getAction().equals("android.intent.action.LOCALE_CHANGED") || intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")) && this.mCallBack != null) {
                this.mCallBack.run();
            }
        }
    }

    public static BroadcastReceiver setTimeChangesReceiver(Context c, Runnable callback) {
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.TIME_SET");
        filter.addAction("android.intent.action.DATE_CHANGED");
        filter.addAction("android.intent.action.TIMEZONE_CHANGED");
        filter.addAction("android.intent.action.LOCALE_CHANGED");
        CalendarBroadcastReceiver r = new CalendarBroadcastReceiver(callback);
        c.registerReceiver(r, filter);
        return r;
    }

    public static void clearTimeChangesReceiver(Context c, BroadcastReceiver r) {
        c.unregisterReceiver(r);
    }

    public static String[] getQuickResponses(Context context) {
        String[] s = getSharedPreference(context, "preferences_quick_responses", (String[]) null);
        if (s == null) {
            return context.getResources().getStringArray(R.array.quick_response_defaults);
        }
        return s;
    }

    public static String getVersionCode(Context context) {
        if (sVersion == null) {
            try {
                sVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("CalUtils", "Error finding package " + context.getApplicationInfo().packageName);
            }
        }
        return sVersion;
    }

    public static void startCalendarMetafeedSync(Account account) {
        Bundle extras = new Bundle();
        extras.putBoolean("force", true);
        extras.putBoolean("metafeedonly", true);
        ContentResolver.requestSync(account, CalendarContract.Calendars.CONTENT_URI.getAuthority(), extras);
    }

    public static Spannable extendedLinkify(String text, boolean lastDitchGeo) {
        Spannable spanText = SpannableString.valueOf(text);
        String defaultPhoneRegion = System.getProperty("user.region", "US");
        if (!defaultPhoneRegion.equals("US")) {
            Linkify.addLinks(spanText, 15);
            URLSpan[] spans = (URLSpan[]) spanText.getSpans(0, spanText.length(), URLSpan.class);
            if (spans.length == 1) {
                int linkStart = spanText.getSpanStart(spans[0]);
                int linkEnd = spanText.getSpanEnd(spans[0]);
                if (linkStart <= indexFirstNonWhitespaceChar(spanText) && linkEnd >= indexLastNonWhitespaceChar(spanText) + 1) {
                    return spanText;
                }
            }
            Spannable spanText2 = SpannableString.valueOf(text);
            if (lastDitchGeo && !text.isEmpty()) {
                Linkify.addLinks(spanText2, mWildcardPattern, "geo:0,0?q=");
            }
            return spanText2;
        }
        boolean linkifyFoundLinks = Linkify.addLinks(spanText, 11);
        URLSpan[] existingSpans = (URLSpan[]) spanText.getSpans(0, spanText.length(), URLSpan.class);
        Matcher coordMatcher = COORD_PATTERN.matcher(spanText);
        int coordCount = 0;
        while (coordMatcher.find()) {
            int start = coordMatcher.start();
            int end = coordMatcher.end();
            if (!spanWillOverlap(spanText, existingSpans, start, end)) {
                URLSpan span = new URLSpan("geo:0,0?q=" + coordMatcher.group());
                spanText.setSpan(span, start, end, 33);
                coordCount++;
            }
        }
        URLSpan[] existingSpans2 = (URLSpan[]) spanText.getSpans(0, spanText.length(), URLSpan.class);
        int[] phoneSequences = findNanpPhoneNumbers(text);
        int phoneCount = 0;
        for (int match = 0; match < phoneSequences.length / 2; match++) {
            int start2 = phoneSequences[match * 2];
            int end2 = phoneSequences[(match * 2) + 1];
            if (!spanWillOverlap(spanText, existingSpans2, start2, end2)) {
                StringBuilder dialBuilder = new StringBuilder();
                for (int i = start2; i < end2; i++) {
                    char ch = spanText.charAt(i);
                    if (ch == '+' || Character.isDigit(ch)) {
                        dialBuilder.append(ch);
                    }
                }
                URLSpan span2 = new URLSpan("tel:" + dialBuilder.toString());
                spanText.setSpan(span2, start2, end2, 33);
                phoneCount++;
            }
        }
        if (lastDitchGeo && !text.isEmpty() && !linkifyFoundLinks && phoneCount == 0 && coordCount == 0) {
            if (Log.isLoggable("CalUtils", 2)) {
                Log.v("CalUtils", "No linkification matches, using geo default");
            }
            Linkify.addLinks(spanText, mWildcardPattern, "geo:0,0?q=");
        }
        return spanText;
    }

    private static int indexFirstNonWhitespaceChar(CharSequence str) {
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int indexLastNonWhitespaceChar(CharSequence str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    static int[] findNanpPhoneNumbers(CharSequence text) {
        ArrayList<Integer> list = new ArrayList<>();
        int startPos = 0;
        int endPos = (text.length() - 7) + 1;
        if (endPos < 0) {
            return new int[0];
        }
        while (startPos < endPos) {
            while (Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                startPos++;
            }
            if (startPos == endPos) {
                break;
            }
            int matchEnd = findNanpMatchEnd(text, startPos);
            if (matchEnd > startPos) {
                list.add(Integer.valueOf(startPos));
                list.add(Integer.valueOf(matchEnd));
                startPos = matchEnd;
            } else {
                while (!Character.isWhitespace(text.charAt(startPos)) && startPos < endPos) {
                    startPos++;
                }
            }
        }
        int[] result = new int[list.size()];
        for (int i = list.size() - 1; i >= 0; i--) {
            result[i] = list.get(i).intValue();
        }
        return result;
    }

    private static int findNanpMatchEnd(CharSequence text, int startPos) {
        char ch;
        if (text.length() > startPos + 4 && text.subSequence(startPos, startPos + 4).toString().equalsIgnoreCase("tel:")) {
            startPos += 4;
        }
        int endPos = text.length();
        int curPos = startPos;
        int foundDigits = 0;
        char firstDigit = 'x';
        boolean foundWhiteSpaceAfterAreaCode = false;
        while (curPos <= endPos) {
            if (curPos < endPos) {
                ch = text.charAt(curPos);
            } else {
                ch = 27;
            }
            if (Character.isDigit(ch)) {
                if (foundDigits == 0) {
                    firstDigit = ch;
                }
                foundDigits++;
                if (foundDigits > 11) {
                    return -1;
                }
            } else if (Character.isWhitespace(ch)) {
                if ((firstDigit == '1' && foundDigits == 4) || foundDigits == 3) {
                    foundWhiteSpaceAfterAreaCode = true;
                } else if ((firstDigit != '1' || foundDigits != 1) && (!foundWhiteSpaceAfterAreaCode || ((firstDigit != '1' || foundDigits != 7) && foundDigits != 6))) {
                    break;
                }
            } else if ("()+-*#.".indexOf(ch) == -1) {
                break;
            }
            curPos++;
        }
        if (firstDigit != '1' && (foundDigits == 7 || foundDigits == 10)) {
            return curPos;
        }
        if (firstDigit == '1' && foundDigits == 11) {
            return curPos;
        }
        return -1;
    }

    private static boolean spanWillOverlap(Spannable spanText, URLSpan[] spanList, int start, int end) {
        if (start == end) {
            return false;
        }
        for (URLSpan span : spanList) {
            int existingStart = spanText.getSpanStart(span);
            int existingEnd = spanText.getSpanEnd(span);
            if ((start >= existingStart && start < existingEnd) || (end > existingStart && end <= existingEnd)) {
                if (Log.isLoggable("CalUtils", 2)) {
                    CharSequence seq = spanText.subSequence(start, end);
                    Log.v("CalUtils", "Not linkifying " + ((Object) seq) + " as phone number due to overlap");
                }
                return true;
            }
        }
        return false;
    }

    public static ArrayList<CalendarEventModel.ReminderEntry> readRemindersFromBundle(Bundle bundle) {
        ArrayList<CalendarEventModel.ReminderEntry> reminders = null;
        ArrayList<Integer> reminderMinutes = bundle.getIntegerArrayList("key_reminder_minutes");
        ArrayList<Integer> reminderMethods = bundle.getIntegerArrayList("key_reminder_methods");
        if (reminderMinutes == null || reminderMethods == null) {
            if (reminderMinutes != null || reminderMethods != null) {
                String nullList = reminderMinutes == null ? "reminderMinutes" : "reminderMethods";
                Log.d("CalUtils", String.format("Error resolving reminders: %s was null", nullList));
            }
            return null;
        }
        int numReminders = reminderMinutes.size();
        if (numReminders == reminderMethods.size()) {
            reminders = new ArrayList<>(numReminders);
            for (int reminder_i = 0; reminder_i < numReminders; reminder_i++) {
                int minutes = reminderMinutes.get(reminder_i).intValue();
                int method = reminderMethods.get(reminder_i).intValue();
                reminders.add(CalendarEventModel.ReminderEntry.valueOf(minutes, method));
            }
        } else {
            Log.d("CalUtils", String.format("Error resolving reminders. Found %d reminderMinutes, but %d reminderMethods.", Integer.valueOf(numReminders), Integer.valueOf(reminderMethods.size())));
        }
        return reminders;
    }
}
