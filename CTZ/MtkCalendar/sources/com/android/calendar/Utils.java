package com.android.calendar;

import android.accounts.Account;
import android.app.Activity;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageItemInfo;
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
import com.mediatek.calendar.LogUtil;
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
        SharedPreferences sharedPreferences = GeneralPreferences.getSharedPreferences(activity);
        if (TextUtils.equals(intent.getAction(), "android.intent.action.EDIT")) {
            return 5;
        }
        if (extras != null) {
            if (extras.getBoolean("DETAIL_VIEW", false)) {
                return sharedPreferences.getInt("preferred_detailedView", 2);
            }
            if ("DAY".equals(extras.getString("VIEW"))) {
                return 2;
            }
        }
        return sharedPreferences.getInt("preferred_startView", 3);
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

    public static void setTimeZone(Context context, String str) {
        mTZUtils.setTimeZone(context, str);
    }

    public static String getTimeZone(Context context, Runnable runnable) {
        return mTZUtils.getTimeZone(context, runnable);
    }

    public static String formatDateRange(Context context, long j, long j2, int i) {
        return mTZUtils.formatDateRange(context, j, j2, i);
    }

    public static boolean getDefaultVibrate(Context context, SharedPreferences sharedPreferences) {
        boolean z = false;
        if (sharedPreferences.contains("preferences_alerts_vibrateWhen")) {
            String string = sharedPreferences.getString("preferences_alerts_vibrateWhen", null);
            if (string != null && string.equals(context.getString(R.string.prefDefault_alerts_vibrate_true))) {
                z = true;
            }
            sharedPreferences.edit().remove("preferences_alerts_vibrateWhen").commit();
            Log.d("CalUtils", "Migrating KEY_ALERTS_VIBRATE_WHEN(" + string + ") to KEY_ALERTS_VIBRATE = " + z);
            return z;
        }
        return sharedPreferences.getBoolean("preferences_alerts_vibrate", false);
    }

    public static String[] getSharedPreference(Context context, String str, String[] strArr) {
        Set<String> stringSet = GeneralPreferences.getSharedPreferences(context).getStringSet(str, null);
        if (stringSet != null) {
            return (String[]) stringSet.toArray(new String[stringSet.size()]);
        }
        return strArr;
    }

    public static String getSharedPreference(Context context, String str, String str2) {
        return GeneralPreferences.getSharedPreferences(context).getString(str, str2);
    }

    public static int getSharedPreference(Context context, String str, int i) {
        return GeneralPreferences.getSharedPreferences(context).getInt(str, i);
    }

    public static boolean getSharedPreference(Context context, String str, boolean z) {
        return GeneralPreferences.getSharedPreferences(context).getBoolean(str, z);
    }

    public static void setSharedPreference(Context context, String str, String str2) {
        GeneralPreferences.getSharedPreferences(context).edit().putString(str, str2).apply();
    }

    public static void setSharedPreference(Context context, String str, String[] strArr) {
        SharedPreferences sharedPreferences = GeneralPreferences.getSharedPreferences(context);
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        for (String str2 : strArr) {
            linkedHashSet.add(str2);
        }
        sharedPreferences.edit().putStringSet(str, linkedHashSet).apply();
    }

    protected static void tardis() {
        mTardis = System.currentTimeMillis();
    }

    protected static long getTardis() {
        return mTardis;
    }

    public static void setSharedPreference(Context context, String str, boolean z) {
        SharedPreferences.Editor editorEdit = GeneralPreferences.getSharedPreferences(context).edit();
        editorEdit.putBoolean(str, z);
        editorEdit.apply();
    }

    public static void setSharedPreference(Context context, String str, int i) {
        SharedPreferences.Editor editorEdit = GeneralPreferences.getSharedPreferences(context).edit();
        editorEdit.putInt(str, i);
        editorEdit.apply();
    }

    public static String getRingTonePreference(Context context) {
        String string = context.getSharedPreferences("com.android.calendar_preferences_no_backup", 0).getString("preferences_alerts_ringtone", null);
        if (string != null && string.equals("")) {
            return "";
        }
        if (string == null) {
            String sharedPreference = getSharedPreference(context, "preferences_alerts_ringtone", "content://settings/system/notification_sound");
            setRingTonePreference(context, sharedPreference);
            return sharedPreference;
        }
        return string;
    }

    public static void setRingTonePreference(Context context, String str) {
        context.getSharedPreferences("com.android.calendar_preferences_no_backup", 0).edit().putString("preferences_alerts_ringtone", str).apply();
    }

    static void setDefaultView(Context context, int i) {
        SharedPreferences.Editor editorEdit = GeneralPreferences.getSharedPreferences(context).edit();
        boolean z = true;
        if ((!mAllowWeekForDetailView || i != 3) && i != 1 && i != 2) {
            z = false;
        }
        if (z) {
            editorEdit.putInt("preferred_detailedView", i);
        }
        editorEdit.putInt("preferred_startView", i);
        editorEdit.apply();
    }

    public static MatrixCursor matrixCursorFromCursor(Cursor cursor) {
        return matrixCursorFromCursor(cursor, false);
    }

    public static MatrixCursor matrixCursorFromCursor(Cursor cursor, boolean z) {
        if (cursor == null) {
            return null;
        }
        String[] columnNames = cursor.getColumnNames();
        if (columnNames == null) {
            columnNames = new String[0];
        }
        MatrixCursor matrixCursor = new MatrixCursor(columnNames);
        int columnCount = cursor.getColumnCount();
        String[] strArr = new String[columnCount];
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            for (int i = 0; i < columnCount; i++) {
                strArr[i] = cursor.getString(i);
            }
            matrixCursor.addRow(strArr);
        }
        if (z) {
            matrixCursor.moveToFirst();
        }
        return matrixCursor;
    }

    public static boolean compareCursors(Cursor cursor, Cursor cursor2) {
        int columnCount;
        if (cursor == null || cursor2 == null || (columnCount = cursor.getColumnCount()) != cursor2.getColumnCount() || cursor.getCount() != cursor2.getCount()) {
            return false;
        }
        cursor.moveToPosition(-1);
        cursor2.moveToPosition(-1);
        while (cursor.moveToNext() && cursor2.moveToNext()) {
            for (int i = 0; i < columnCount; i++) {
                if (!TextUtils.equals(cursor.getString(i), cursor2.getString(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static final long timeFromIntentInMillis(Intent intent) {
        long jLongValue;
        Uri data = intent.getData();
        long longExtra = intent.getLongExtra("beginTime", -1L);
        if (longExtra == -1 && data != null && data.isHierarchical()) {
            List<String> pathSegments = data.getPathSegments();
            if (pathSegments.size() == 2 && pathSegments.get(0).equals("time")) {
                try {
                    jLongValue = Long.valueOf(data.getLastPathSegment()).longValue();
                } catch (NumberFormatException e) {
                    Log.i("Calendar", "timeFromIntentInMillis: Data existed but no valid time found. Using current time.");
                    jLongValue = longExtra;
                }
            }
        } else {
            jLongValue = longExtra;
        }
        if (jLongValue <= 0) {
            return System.currentTimeMillis();
        }
        return jLongValue;
    }

    public static String formatMonthYear(Context context, Time time) {
        long millis = time.toMillis(true);
        return formatDateRange(context, millis, millis, 52);
    }

    public static int getWeeksSinceEpochFromJulianDay(int i, int i2) {
        int i3 = 4 - i2;
        if (i3 < 0) {
            i3 += 7;
        }
        return (i - (2440588 - i3)) / 7;
    }

    public static int getJulianMondayFromWeeksSinceEpoch(int i) {
        return 2440585 + (i * 7);
    }

    public static int getFirstDayOfWeek(Context context) {
        int firstDayOfWeek;
        String string = GeneralPreferences.getSharedPreferences(context).getString("preferences_week_start_day", "-1");
        if ("-1".equals(string)) {
            firstDayOfWeek = Calendar.getInstance().getFirstDayOfWeek();
        } else {
            firstDayOfWeek = Integer.parseInt(string);
        }
        if (firstDayOfWeek == 7) {
            return 6;
        }
        if (firstDayOfWeek == 2) {
            return 1;
        }
        return 0;
    }

    public static int getFirstDayOfWeekAsCalendar(Context context) {
        return convertDayOfWeekFromTimeToCalendar(getFirstDayOfWeek(context));
    }

    public static int convertDayOfWeekFromTimeToCalendar(int i) {
        switch (i) {
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
        return GeneralPreferences.getSharedPreferences(context).getBoolean("preferences_show_week_num", false);
    }

    public static boolean getHideDeclinedEvents(Context context) {
        return GeneralPreferences.getSharedPreferences(context).getBoolean("preferences_hide_declined", false);
    }

    public static int getDaysPerWeek(Context context) {
        return GeneralPreferences.getSharedPreferences(context).getInt("preferences_days_per_week", 7);
    }

    public static boolean isSaturday(int i, int i2) {
        if (i2 != 0 || i != 6) {
            if (i2 == 1 && i == 5) {
                return true;
            }
            return i2 == 6 && i == 0;
        }
        return true;
    }

    public static boolean isSunday(int i, int i2) {
        if (i2 != 0 || i != 0) {
            if (i2 == 1 && i == 6) {
                return true;
            }
            return i2 == 6 && i == 1;
        }
        return true;
    }

    public static long convertAlldayUtcToLocal(Time time, long j, String str) {
        if (time == null) {
            time = new Time();
        }
        time.timezone = "UTC";
        time.set(j);
        time.timezone = str;
        return time.normalize(true);
    }

    public static long convertAlldayLocalToUTC(Time time, long j, String str) {
        if (time == null) {
            time = new Time();
        }
        time.timezone = str;
        time.set(j);
        time.timezone = "UTC";
        return time.normalize(true);
    }

    public static long getNextMidnight(Time time, long j, String str) {
        if (time == null) {
            time = new Time();
        }
        time.timezone = str;
        time.set(j);
        time.monthDay++;
        time.hour = 0;
        time.minute = 0;
        time.second = 0;
        return time.normalize(true);
    }

    public static void checkForDuplicateNames(Map<String, Boolean> map, Cursor cursor, int i) {
        map.clear();
        cursor.moveToPosition(-1);
        while (cursor.moveToNext()) {
            String string = cursor.getString(i);
            if (string != null) {
                map.put(string, Boolean.valueOf(map.containsKey(string)));
            }
        }
    }

    public static boolean equals(Object obj, Object obj2) {
        return obj == null ? obj2 == null : obj.equals(obj2);
    }

    public static void setAllowWeekForDetailView(boolean z) {
        mAllowWeekForDetailView = z;
    }

    public static boolean getAllowWeekForDetailView() {
        return mAllowWeekForDetailView;
    }

    public static boolean getConfigBool(Context context, int i) {
        return context.getResources().getBoolean(i);
    }

    public static int getDisplayColorFromColor(int i) {
        if (!isJellybeanOrLater()) {
            return i;
        }
        float[] fArr = new float[3];
        Color.colorToHSV(i, fArr);
        fArr[1] = Math.min(fArr[1] * 1.3f, 1.0f);
        fArr[2] = fArr[2] * 0.8f;
        return Color.HSVToColor(fArr);
    }

    public static int getDeclinedColorFromColor(int i, int i2, int i3) {
        int i4 = 255 - i2;
        return ((((((i & 255) * i2) + ((i3 & 255) * i4)) & 65280) | (((((i & 16711680) * i2) + ((i3 & 16711680) * i4)) & (-16777216)) | (16711680 & (((i & 65280) * i2) + ((i3 & 65280) * i4))))) >> 8) | (-16777216);
    }

    public static int getDeclinedColorFromColor(int i) {
        return getDeclinedColorFromColor(i, 102, -1);
    }

    public static void trySyncAndDisableUpgradeReceiver(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, (Class<?>) UpgradeReceiver.class);
        if (packageManager.getComponentEnabledSetting(componentName) == 2) {
            return;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean("force", true);
        ContentResolver.requestSync(null, CalendarContract.Calendars.CONTENT_URI.getAuthority(), bundle);
        packageManager.setComponentEnabledSetting(componentName, 2, 1);
    }

    private static class DNASegment {
        int color;
        int day;
        int endMinute;
        int startMinute;

        private DNASegment() {
        }
    }

    public static HashMap<Integer, DNAStrand> createDNAStrands(int i, ArrayList<Event> arrayList, int i2, int i3, int i4, int[] iArr, Context context) {
        int i5;
        int i6;
        int i7;
        DNASegment dNASegment;
        int i8;
        boolean z = true;
        if (!mMinutesLoaded) {
            if (context == null) {
                Log.wtf("CalUtils", "No context and haven't loaded parameters yet! Can't create DNA.");
            }
            Resources resources = context.getResources();
            CONFLICT_COLOR = resources.getColor(R.color.month_dna_conflict_time_color);
            WORK_DAY_START_MINUTES = resources.getInteger(R.integer.work_start_minutes);
            WORK_DAY_END_MINUTES = resources.getInteger(R.integer.work_end_minutes);
            WORK_DAY_END_LENGTH = 1440 - WORK_DAY_END_MINUTES;
            WORK_DAY_MINUTES = WORK_DAY_END_MINUTES - WORK_DAY_START_MINUTES;
            mMinutesLoaded = true;
        }
        if (arrayList == null || arrayList.isEmpty() || iArr == null || iArr.length < 1 || (i5 = i3 - i2) < 8 || i4 < 0) {
            Log.d("CalUtils", "Bad values for createDNAStrands! events:" + arrayList + " dayXs:" + Arrays.toString(iArr) + " bot-top:" + (i3 - i2) + " minPixels:" + i4);
            return null;
        }
        LinkedList linkedList = new LinkedList();
        HashMap<Integer, DNAStrand> map = new HashMap<>();
        DNAStrand dNAStrand = new DNAStrand();
        dNAStrand.color = CONFLICT_COLOR;
        map.put(Integer.valueOf(CONFLICT_COLOR), dNAStrand);
        int i9 = ((i4 * 4) * WORK_DAY_MINUTES) / (3 * i5);
        int i10 = (i9 * 5) / 2;
        int length = (iArr.length + i) - 1;
        Event event = new Event();
        Iterator<Event> it = arrayList.iterator();
        while (it.hasNext()) {
            Event next = it.next();
            if (next.endDay >= i && next.startDay <= length) {
                if (next.drawAsAllday()) {
                    addAllDayToStrands(next, map, i, iArr.length);
                } else {
                    next.copyTo(event);
                    if (event.startDay < i) {
                        event.startDay = i;
                        event.startTime = 0;
                    }
                    int i11 = 1440 - i10;
                    if (event.startTime > i11) {
                        event.startTime = i11;
                    }
                    if (event.endDay > length) {
                        event.endDay = length;
                        event.endTime = 1439;
                    }
                    if (event.endTime < i10) {
                        event.endTime = i10;
                    }
                    if (event.startDay == event.endDay && event.endTime - event.startTime < i10) {
                        if (event.startTime < WORK_DAY_START_MINUTES) {
                            event.endTime = Math.min(event.startTime + i10, WORK_DAY_START_MINUTES + i9);
                        } else if (event.endTime > WORK_DAY_END_MINUTES) {
                            event.endTime = Math.min(event.endTime + i10, 1439);
                            if (event.endTime - event.startTime < i10) {
                                event.startTime = event.endTime - i10;
                            }
                        }
                    }
                    if (linkedList.size() == 0) {
                        addNewSegment(linkedList, event, map, i, 0, i9);
                        event = event;
                        it = it;
                        length = length;
                        z = true;
                    } else {
                        Iterator<Event> it2 = it;
                        Event event2 = event;
                        int i12 = length;
                        DNASegment dNASegment2 = (DNASegment) linkedList.getLast();
                        int i13 = ((event2.startDay - i) * 1440) + event2.startTime;
                        int iMax = Math.max(((event2.endDay - i) * 1440) + event2.endTime, i13 + i9);
                        if (i13 < 0) {
                            i13 = 0;
                        }
                        if (iMax >= 10080) {
                            iMax = 10079;
                        }
                        if (i13 < dNASegment2.endMinute) {
                            int size = linkedList.size();
                            do {
                                size--;
                                if (size < 0) {
                                    break;
                                }
                            } while (iMax < ((DNASegment) linkedList.get(size)).startMinute);
                            while (size >= 0) {
                                DNASegment dNASegment3 = (DNASegment) linkedList.get(size);
                                if (i13 > dNASegment3.endMinute) {
                                    break;
                                }
                                if (dNASegment3.color == CONFLICT_COLOR) {
                                    i7 = i13;
                                } else {
                                    if (iMax < dNASegment3.endMinute - i9) {
                                        DNASegment dNASegment4 = new DNASegment();
                                        dNASegment4.endMinute = dNASegment3.endMinute;
                                        dNASegment4.color = dNASegment3.color;
                                        dNASegment4.startMinute = iMax + 1;
                                        dNASegment4.day = dNASegment3.day;
                                        dNASegment3.endMinute = iMax;
                                        linkedList.add(size + 1, dNASegment4);
                                        map.get(Integer.valueOf(dNASegment4.color)).count++;
                                    }
                                    if (i13 > dNASegment3.startMinute + i9) {
                                        DNASegment dNASegment5 = new DNASegment();
                                        dNASegment5.startMinute = dNASegment3.startMinute;
                                        dNASegment5.color = dNASegment3.color;
                                        dNASegment5.endMinute = i13 - 1;
                                        dNASegment5.day = dNASegment3.day;
                                        dNASegment3.startMinute = i13;
                                        i6 = size + 1;
                                        linkedList.add(size, dNASegment5);
                                        map.get(Integer.valueOf(dNASegment5.color)).count++;
                                    } else {
                                        i6 = size;
                                    }
                                    int i14 = i6 + 1;
                                    if (i14 < linkedList.size()) {
                                        dNASegment = (DNASegment) linkedList.get(i14);
                                        i7 = i13;
                                        if (dNASegment.color == CONFLICT_COLOR && dNASegment3.day == dNASegment.day && dNASegment.startMinute <= dNASegment3.endMinute + 1) {
                                            dNASegment.startMinute = Math.min(dNASegment3.startMinute, dNASegment.startMinute);
                                            linkedList.remove(dNASegment3);
                                            map.get(Integer.valueOf(dNASegment3.color)).count--;
                                        }
                                        i8 = i6 - 1;
                                        if (i8 < 0) {
                                            DNASegment dNASegment6 = (DNASegment) linkedList.get(i8);
                                            if (dNASegment6.color == CONFLICT_COLOR && dNASegment.day == dNASegment6.day && dNASegment6.endMinute >= dNASegment.startMinute - 1) {
                                                dNASegment6.endMinute = Math.max(dNASegment.endMinute, dNASegment6.endMinute);
                                                linkedList.remove(dNASegment);
                                                map.get(Integer.valueOf(dNASegment.color)).count--;
                                                i6--;
                                            } else {
                                                dNASegment6 = dNASegment;
                                            }
                                            size = i6;
                                            if (dNASegment6.color != CONFLICT_COLOR) {
                                                map.get(Integer.valueOf(dNASegment6.color)).count--;
                                                dNASegment6.color = CONFLICT_COLOR;
                                                map.get(Integer.valueOf(CONFLICT_COLOR)).count++;
                                            }
                                        }
                                        size--;
                                        i13 = i7;
                                    } else {
                                        i7 = i13;
                                    }
                                    dNASegment = dNASegment3;
                                    i8 = i6 - 1;
                                    if (i8 < 0) {
                                    }
                                    size--;
                                    i13 = i7;
                                }
                                size--;
                                i13 = i7;
                            }
                        }
                        if (iMax > dNASegment2.endMinute) {
                            addNewSegment(linkedList, event2, map, i, dNASegment2.endMinute, i9);
                        }
                        event = event2;
                        z = true;
                        it = it2;
                        length = i12;
                    }
                }
            }
        }
        weaveDNAStrands(linkedList, i, map, i2, i3, iArr);
        return map;
    }

    private static void addAllDayToStrands(Event event, HashMap<Integer, DNAStrand> map, int i, int i2) {
        DNAStrand orCreateStrand = getOrCreateStrand(map, CONFLICT_COLOR);
        if (orCreateStrand.allDays == null) {
            orCreateStrand.allDays = new int[i2];
        }
        int iMin = Math.min(event.endDay - i, i2 - 1);
        for (int iMax = Math.max(event.startDay - i, 0); iMax <= iMin; iMax++) {
            if (orCreateStrand.allDays[iMax] != 0) {
                orCreateStrand.allDays[iMax] = CONFLICT_COLOR;
            } else {
                orCreateStrand.allDays[iMax] = event.color;
            }
        }
    }

    private static void weaveDNAStrands(LinkedList<DNASegment> linkedList, int i, HashMap<Integer, DNAStrand> map, int i2, int i3, int[] iArr) {
        Iterator<DNAStrand> it = map.values().iterator();
        while (it.hasNext()) {
            DNAStrand next = it.next();
            if (next.count < 1 && next.allDays == null) {
                it.remove();
            } else {
                next.points = new float[next.count * 4];
                next.position = 0;
            }
        }
        for (DNASegment dNASegment : linkedList) {
            DNAStrand dNAStrand = map.get(Integer.valueOf(dNASegment.color));
            int i4 = dNASegment.day - i;
            int i5 = dNASegment.startMinute % 1440;
            int i6 = dNASegment.endMinute % 1440;
            int i7 = i3 - i2;
            int i8 = (i7 * 3) / 4;
            int i9 = (i7 - i8) / 2;
            int i10 = iArr[i4];
            int pixelOffsetFromMinutes = getPixelOffsetFromMinutes(i5, i8, i9) + i2;
            int pixelOffsetFromMinutes2 = getPixelOffsetFromMinutes(i6, i8, i9) + i2;
            float[] fArr = dNAStrand.points;
            int i11 = dNAStrand.position;
            dNAStrand.position = i11 + 1;
            float f = i10;
            fArr[i11] = f;
            float[] fArr2 = dNAStrand.points;
            int i12 = dNAStrand.position;
            dNAStrand.position = i12 + 1;
            fArr2[i12] = pixelOffsetFromMinutes;
            float[] fArr3 = dNAStrand.points;
            int i13 = dNAStrand.position;
            dNAStrand.position = i13 + 1;
            fArr3[i13] = f;
            float[] fArr4 = dNAStrand.points;
            int i14 = dNAStrand.position;
            dNAStrand.position = i14 + 1;
            fArr4[i14] = pixelOffsetFromMinutes2;
        }
    }

    private static int getPixelOffsetFromMinutes(int i, int i2, int i3) {
        if (i < WORK_DAY_START_MINUTES) {
            return (i * i3) / WORK_DAY_START_MINUTES;
        }
        if (i < WORK_DAY_END_MINUTES) {
            return (((i - WORK_DAY_START_MINUTES) * i2) / WORK_DAY_MINUTES) + i3;
        }
        return (((i - WORK_DAY_END_MINUTES) * i3) / WORK_DAY_END_LENGTH) + i2 + i3;
    }

    private static void addNewSegment(LinkedList<DNASegment> linkedList, Event event, HashMap<Integer, DNAStrand> map, int i, int i2, int i3) {
        int i4 = event.startDay;
        int i5 = event.endDay;
        if (event.startDay != event.endDay) {
            Event event2 = new Event();
            event2.color = event.color;
            event2.startDay = event.startDay;
            event2.startTime = event.startTime;
            event2.endDay = event2.startDay;
            event2.endTime = 1439;
            int i6 = i2;
            while (event2.startDay != event.endDay) {
                addNewSegment(linkedList, event2, map, i, i6, i3);
                event2.startDay++;
                event2.endDay = event2.startDay;
                event2.startTime = 0;
                i6 = 0;
            }
            event2.endTime = event.endTime;
            event = event2;
            i2 = i6;
        }
        DNASegment dNASegment = new DNASegment();
        int i7 = (event.startDay - i) * 1440;
        int i8 = (i7 + 1440) - 1;
        dNASegment.startMinute = Math.max(event.startTime + i7, i2);
        dNASegment.endMinute = Math.max(i7 + event.endTime, Math.min(dNASegment.startMinute + i3, i8));
        if (dNASegment.endMinute > i8) {
            dNASegment.endMinute = i8;
        }
        dNASegment.color = event.color;
        dNASegment.day = event.startDay;
        linkedList.add(dNASegment);
        getOrCreateStrand(map, dNASegment.color).count++;
    }

    private static DNAStrand getOrCreateStrand(HashMap<Integer, DNAStrand> map, int i) {
        DNAStrand dNAStrand = map.get(Integer.valueOf(i));
        if (dNAStrand == null) {
            DNAStrand dNAStrand2 = new DNAStrand();
            dNAStrand2.color = i;
            dNAStrand2.count = 0;
            map.put(Integer.valueOf(dNAStrand2.color), dNAStrand2);
            return dNAStrand2;
        }
        return dNAStrand;
    }

    public static void returnToCalendarHome(Context context) {
        Intent intent = new Intent(context, (Class<?>) AllInOneActivity.class);
        intent.setAction("android.intent.action.VIEW");
        intent.setFlags(67108864);
        intent.putExtra("KEY_HOME", true);
        context.startActivity(intent);
    }

    public static void setUpSearchView(SearchView searchView, Activity activity) {
        searchView.setSearchableInfo(((SearchManager) activity.getSystemService("search")).getSearchableInfo(activity.getComponentName()));
        searchView.setQueryRefinementEnabled(true);
    }

    public static int getWeekNumberFromTime(long j, Context context) {
        Time time = new Time(getTimeZone(context, null));
        time.set(j);
        time.normalize(true);
        int firstDayOfWeek = getFirstDayOfWeek(context);
        if (time.weekDay == 0 && (firstDayOfWeek == 0 || firstDayOfWeek == 6)) {
            time.monthDay++;
            time.normalize(true);
        } else if (time.weekDay == 6 && firstDayOfWeek == 6) {
            time.monthDay += 2;
            time.normalize(true);
        }
        return time.getWeekNumber();
    }

    public static String getDayOfWeekString(int i, int i2, long j, Context context) {
        String string;
        getTimeZone(context, null);
        if (i == i2) {
            string = context.getString(R.string.agenda_today, mTZUtils.formatDateRange(context, j, j, 2).toString());
        } else if (i == i2 - 1) {
            string = context.getString(R.string.agenda_yesterday, mTZUtils.formatDateRange(context, j, j, 2).toString());
        } else if (i == i2 + 1) {
            string = context.getString(R.string.agenda_tomorrow, mTZUtils.formatDateRange(context, j, j, 2).toString());
        } else {
            string = mTZUtils.formatDateRange(context, j, j, 2).toString();
        }
        return string.toUpperCase();
    }

    public static void setMidnightUpdater(Handler handler, Runnable runnable, String str) {
        if (handler == null || runnable == null || str == null) {
            return;
        }
        long jCurrentTimeMillis = System.currentTimeMillis();
        Time time = new Time(str);
        time.set(jCurrentTimeMillis);
        long j = ((((86400 - (time.hour * 3600)) - (time.minute * 60)) - time.second) + 1) * 1000;
        handler.removeCallbacks(runnable);
        handler.postDelayed(runnable, j);
    }

    public static void resetMidnightUpdater(Handler handler, Runnable runnable) {
        if (handler == null || runnable == null) {
            return;
        }
        handler.removeCallbacks(runnable);
    }

    public static String getDisplayedDatetime(long j, long j2, long j3, String str, boolean z, Context context) {
        String string;
        int i = DateFormat.is24HourFormat(context) ? 129 : 1;
        Time time = new Time(str);
        time.set(j3);
        Resources resources = context.getResources();
        if (z) {
            String string2 = null;
            long jConvertAlldayUtcToLocal = convertAlldayUtcToLocal(null, j, str);
            if (singleDayEvent(jConvertAlldayUtcToLocal, convertAlldayUtcToLocal(null, j2, str), time.gmtoff)) {
                int iIsTodayOrTomorrow = isTodayOrTomorrow(context.getResources(), jConvertAlldayUtcToLocal, j3, time.gmtoff);
                if (1 == iIsTodayOrTomorrow) {
                    string2 = resources.getString(R.string.today);
                } else if (2 == iIsTodayOrTomorrow) {
                    string2 = resources.getString(R.string.tomorrow);
                }
            }
            if (string2 == null) {
                return DateUtils.formatDateRange(context, new Formatter(new StringBuilder(50), Locale.getDefault()), j, j2, 18, "UTC").toString();
            }
            return string2;
        }
        if (singleDayEvent(j, j2, time.gmtoff)) {
            String dateRange = formatDateRange(context, j, j2, i);
            int iIsTodayOrTomorrow2 = isTodayOrTomorrow(context.getResources(), j, j3, time.gmtoff);
            if (1 == iIsTodayOrTomorrow2) {
                string = resources.getString(R.string.today_at_time_fmt, dateRange);
            } else if (2 == iIsTodayOrTomorrow2) {
                string = resources.getString(R.string.tomorrow_at_time_fmt, dateRange);
            } else {
                string = resources.getString(R.string.date_time_fmt, formatDateRange(context, j, j2, 18), dateRange);
            }
            return string;
        }
        return formatDateRange(context, j, j2, 18 | i | 65536 | 32768);
    }

    public static String getDisplayedTimezone(long j, String str, String str2) {
        if (!TextUtils.equals(str, str2)) {
            TimeZone timeZone = TimeZone.getTimeZone(str);
            if (timeZone == null || timeZone.getID().equals("GMT")) {
                return str;
            }
            Time time = new Time(str);
            time.set(j);
            return timeZone.getDisplayName(time.isDst != 0, 0);
        }
        return null;
    }

    private static boolean singleDayEvent(long j, long j2, long j3) {
        return j == j2 || Time.getJulianDay(j, j3) == Time.getJulianDay(j2 - 1, j3);
    }

    private static int isTodayOrTomorrow(Resources resources, long j, long j2, long j3) {
        int julianDay = Time.getJulianDay(j, j3) - Time.getJulianDay(j2, j3);
        if (julianDay == 1) {
            return 2;
        }
        if (julianDay == 0) {
            return 1;
        }
        return 0;
    }

    public static Intent createEmailAttendeesIntent(Resources resources, String str, String str2, List<String> list, List<String> list2, String str3) {
        if (list.size() <= 0) {
            if (list2.size() <= 0) {
                throw new IllegalArgumentException("Both toEmails and ccEmails are empty.");
            }
            list = list2;
            list2 = null;
        }
        String str4 = str != null ? resources.getString(R.string.email_subject_prefix) + str : null;
        Uri.Builder builder = new Uri.Builder();
        builder.scheme("mailto");
        if (list.size() > 1) {
            for (int i = 1; i < list.size(); i++) {
                builder.appendQueryParameter("to", list.get(i));
            }
        }
        if (str4 != null) {
            builder.appendQueryParameter("subject", str4);
        }
        if (str2 != null) {
            builder.appendQueryParameter("body", str2);
        }
        if (list2 != null && list2.size() > 0) {
            Iterator<String> it = list2.iterator();
            while (it.hasNext()) {
                builder.appendQueryParameter("cc", it.next());
            }
        }
        String string = builder.toString();
        if (string.startsWith("mailto:")) {
            StringBuilder sb = new StringBuilder(string);
            sb.insert(7, Uri.encode(list.get(0)));
            string = sb.toString();
        }
        Intent intent = new Intent("android.intent.action.SENDTO", Uri.parse(string));
        intent.putExtra("fromAccountString", str3);
        if (str2 != null) {
            intent.putExtra("android.intent.extra.TEXT", str2);
        }
        return Intent.createChooser(intent, resources.getString(R.string.email_picker_label));
    }

    public static boolean isValidEmail(String str) {
        return (str == null || str.endsWith("calendar.google.com")) ? false : true;
    }

    public static boolean isEmailableFrom(String str, String str2) {
        return isValidEmail(str) && !str.equals(str2);
    }

    public static void setTodayIcon(LayerDrawable layerDrawable, Context context, String str) {
        DayOfMonthDrawable dayOfMonthDrawable;
        Drawable drawableFindDrawableByLayerId = layerDrawable.findDrawableByLayerId(R.id.today_icon_day);
        if (drawableFindDrawableByLayerId != null) {
            boolean z = drawableFindDrawableByLayerId instanceof DayOfMonthDrawable;
            dayOfMonthDrawable = drawableFindDrawableByLayerId;
            if (!z) {
                dayOfMonthDrawable = new DayOfMonthDrawable(context);
            }
        }
        Time time = new Time(str);
        time.setToNow();
        time.normalize(false);
        dayOfMonthDrawable.setDayOfMonth(time.monthDay);
        layerDrawable.mutate();
        layerDrawable.setDrawableByLayerId(R.id.today_icon_day, dayOfMonthDrawable);
    }

    private static class CalendarBroadcastReceiver extends BroadcastReceiver {
        Runnable mCallBack;

        public CalendarBroadcastReceiver(Runnable runnable) {
            this.mCallBack = runnable;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ((intent.getAction().equals("android.intent.action.DATE_CHANGED") || intent.getAction().equals("android.intent.action.TIME_SET") || intent.getAction().equals("android.intent.action.LOCALE_CHANGED") || intent.getAction().equals("android.intent.action.TIMEZONE_CHANGED")) && this.mCallBack != null) {
                this.mCallBack.run();
            }
        }
    }

    public static BroadcastReceiver setTimeChangesReceiver(Context context, Runnable runnable) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.TIME_SET");
        intentFilter.addAction("android.intent.action.DATE_CHANGED");
        intentFilter.addAction("android.intent.action.TIMEZONE_CHANGED");
        intentFilter.addAction("android.intent.action.LOCALE_CHANGED");
        CalendarBroadcastReceiver calendarBroadcastReceiver = new CalendarBroadcastReceiver(runnable);
        context.registerReceiver(calendarBroadcastReceiver, intentFilter);
        return calendarBroadcastReceiver;
    }

    public static void clearTimeChangesReceiver(Context context, BroadcastReceiver broadcastReceiver) {
        context.unregisterReceiver(broadcastReceiver);
    }

    public static String[] getQuickResponses(Context context) {
        String[] sharedPreference = getSharedPreference(context, "preferences_quick_responses", (String[]) null);
        if (sharedPreference == null) {
            return context.getResources().getStringArray(R.array.quick_response_defaults);
        }
        return sharedPreference;
    }

    public static String getVersionCode(Context context) {
        if (sVersion == null) {
            try {
                sVersion = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("CalUtils", "Error finding package " + ((PackageItemInfo) context.getApplicationInfo()).packageName);
            }
        }
        return sVersion;
    }

    public static void startCalendarMetafeedSync(Account account) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("force", true);
        bundle.putBoolean("metafeedonly", true);
        ContentResolver.requestSync(account, CalendarContract.Calendars.CONTENT_URI.getAuthority(), bundle);
    }

    public static Spannable extendedLinkify(String str, boolean z) {
        SpannableString spannableStringValueOf = SpannableString.valueOf(str);
        if (!System.getProperty("user.region", "US").equals("US")) {
            Linkify.addLinks(spannableStringValueOf, 15);
            URLSpan[] uRLSpanArr = (URLSpan[]) spannableStringValueOf.getSpans(0, spannableStringValueOf.length(), URLSpan.class);
            if (uRLSpanArr.length == 1) {
                int spanStart = spannableStringValueOf.getSpanStart(uRLSpanArr[0]);
                int spanEnd = spannableStringValueOf.getSpanEnd(uRLSpanArr[0]);
                if (spanStart <= indexFirstNonWhitespaceChar(spannableStringValueOf) && spanEnd >= indexLastNonWhitespaceChar(spannableStringValueOf) + 1) {
                    return spannableStringValueOf;
                }
            }
            SpannableString spannableStringValueOf2 = SpannableString.valueOf(str);
            if (z && !str.isEmpty()) {
                Linkify.addLinks(spannableStringValueOf2, mWildcardPattern, "geo:0,0?q=");
            }
            return spannableStringValueOf2;
        }
        boolean zAddLinks = Linkify.addLinks(spannableStringValueOf, 11);
        URLSpan[] uRLSpanArr2 = (URLSpan[]) spannableStringValueOf.getSpans(0, spannableStringValueOf.length(), URLSpan.class);
        Matcher matcher = COORD_PATTERN.matcher(spannableStringValueOf);
        int i = 0;
        while (matcher.find()) {
            int iStart = matcher.start();
            int iEnd = matcher.end();
            if (!spanWillOverlap(spannableStringValueOf, uRLSpanArr2, iStart, iEnd)) {
                spannableStringValueOf.setSpan(new URLSpan("geo:0,0?q=" + matcher.group()), iStart, iEnd, 33);
                i++;
            }
        }
        URLSpan[] uRLSpanArr3 = (URLSpan[]) spannableStringValueOf.getSpans(0, spannableStringValueOf.length(), URLSpan.class);
        int[] iArrFindNanpPhoneNumbers = findNanpPhoneNumbers(str);
        int i2 = 0;
        for (int i3 = 0; i3 < iArrFindNanpPhoneNumbers.length / 2; i3++) {
            int i4 = i3 * 2;
            int i5 = iArrFindNanpPhoneNumbers[i4];
            int i6 = iArrFindNanpPhoneNumbers[i4 + 1];
            if (!spanWillOverlap(spannableStringValueOf, uRLSpanArr3, i5, i6)) {
                StringBuilder sb = new StringBuilder();
                for (int i7 = i5; i7 < i6; i7++) {
                    char cCharAt = spannableStringValueOf.charAt(i7);
                    if (cCharAt == '+' || Character.isDigit(cCharAt)) {
                        sb.append(cCharAt);
                    }
                }
                spannableStringValueOf.setSpan(new URLSpan("tel:" + sb.toString()), i5, i6, 33);
                i2++;
            }
        }
        if (z && !str.isEmpty() && !zAddLinks && i2 == 0 && i == 0) {
            if (Log.isLoggable("CalUtils", 2)) {
                Log.v("CalUtils", "No linkification matches, using geo default");
            }
            Linkify.addLinks(spannableStringValueOf, mWildcardPattern, "geo:0,0?q=");
        }
        return spannableStringValueOf;
    }

    private static int indexFirstNonWhitespaceChar(CharSequence charSequence) {
        for (int i = 0; i < charSequence.length(); i++) {
            if (!Character.isWhitespace(charSequence.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int indexLastNonWhitespaceChar(CharSequence charSequence) {
        for (int length = charSequence.length() - 1; length >= 0; length--) {
            if (!Character.isWhitespace(charSequence.charAt(length))) {
                return length;
            }
        }
        return -1;
    }

    static int[] findNanpPhoneNumbers(CharSequence charSequence) {
        ArrayList arrayList = new ArrayList();
        int length = (charSequence.length() - 7) + 1;
        int i = 0;
        if (length < 0) {
            return new int[0];
        }
        while (i < length) {
            while (Character.isWhitespace(charSequence.charAt(i)) && i < length) {
                i++;
            }
            if (i == length) {
                break;
            }
            int iFindNanpMatchEnd = findNanpMatchEnd(charSequence, i);
            if (iFindNanpMatchEnd > i) {
                arrayList.add(Integer.valueOf(i));
                arrayList.add(Integer.valueOf(iFindNanpMatchEnd));
                i = iFindNanpMatchEnd;
            } else {
                while (!Character.isWhitespace(charSequence.charAt(i)) && i < length) {
                    i++;
                }
            }
        }
        int[] iArr = new int[arrayList.size()];
        for (int size = arrayList.size() - 1; size >= 0; size--) {
            iArr[size] = ((Integer) arrayList.get(size)).intValue();
        }
        return iArr;
    }

    private static int findNanpMatchEnd(CharSequence charSequence, int i) {
        char cCharAt;
        int i2 = i + 4;
        if (charSequence.length() > i2 && charSequence.subSequence(i, i2).toString().equalsIgnoreCase("tel:")) {
            i = i2;
        }
        int length = charSequence.length();
        char c = 'x';
        int i3 = 0;
        boolean z = false;
        while (i <= length) {
            if (i < length) {
                cCharAt = charSequence.charAt(i);
            } else {
                cCharAt = 27;
            }
            if (Character.isDigit(cCharAt)) {
                if (i3 == 0) {
                    c = cCharAt;
                }
                i3++;
                if (i3 > 11) {
                    return -1;
                }
            } else if (Character.isWhitespace(cCharAt)) {
                if ((c != '1' || i3 != 4) && i3 != 3) {
                    if ((c != '1' || i3 != 1) && (!z || ((c != '1' || i3 != 7) && i3 != 6))) {
                        break;
                    }
                } else {
                    z = true;
                }
            } else if ("()+-*#.".indexOf(cCharAt) == -1) {
                break;
            }
            i++;
        }
        if ((c == '1' || !(i3 == 7 || i3 == 10)) && !(c == '1' && i3 == 11)) {
            return -1;
        }
        return i;
    }

    private static boolean spanWillOverlap(Spannable spannable, URLSpan[] uRLSpanArr, int i, int i2) {
        if (i == i2) {
            return false;
        }
        for (URLSpan uRLSpan : uRLSpanArr) {
            int spanStart = spannable.getSpanStart(uRLSpan);
            int spanEnd = spannable.getSpanEnd(uRLSpan);
            if ((i >= spanStart && i < spanEnd) || (i2 > spanStart && i2 <= spanEnd)) {
                if (Log.isLoggable("CalUtils", 2)) {
                    Log.v("CalUtils", "Not linkifying " + ((Object) spannable.subSequence(i, i2)) + " as phone number due to overlap");
                    return true;
                }
                return true;
            }
        }
        return false;
    }

    public static ArrayList<CalendarEventModel.ReminderEntry> readRemindersFromBundle(Bundle bundle) {
        ArrayList<Integer> integerArrayList = bundle.getIntegerArrayList("key_reminder_minutes");
        ArrayList<Integer> integerArrayList2 = bundle.getIntegerArrayList("key_reminder_methods");
        ArrayList<CalendarEventModel.ReminderEntry> arrayList = null;
        if (integerArrayList == null || integerArrayList2 == null) {
            if (integerArrayList != null || integerArrayList2 != null) {
                Log.d("CalUtils", String.format("Error resolving reminders: %s was null", integerArrayList == null ? "reminderMinutes" : "reminderMethods"));
            }
            return null;
        }
        int size = integerArrayList.size();
        if (size == integerArrayList2.size()) {
            arrayList = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                arrayList.add(CalendarEventModel.ReminderEntry.valueOf(integerArrayList.get(i).intValue(), integerArrayList2.get(i).intValue()));
            }
        } else {
            Log.d("CalUtils", String.format("Error resolving reminders. Found %d reminderMinutes, but %d reminderMethods.", Integer.valueOf(size), Integer.valueOf(integerArrayList2.size())));
        }
        return arrayList;
    }

    public static boolean canUseProviderByUri(ContentResolver contentResolver, Uri uri) throws Throwable {
        ContentProviderClient contentProviderClient = null;
        try {
            try {
                ContentProviderClient contentProviderClientAcquireContentProviderClient = contentResolver.acquireContentProviderClient(uri);
                if (contentProviderClientAcquireContentProviderClient != null) {
                    if (contentProviderClientAcquireContentProviderClient == null) {
                        return true;
                    }
                    contentProviderClientAcquireContentProviderClient.release();
                    return true;
                }
                try {
                    Log.w("CalUtils", "failed to find calendar provider.");
                    if (contentProviderClientAcquireContentProviderClient != null) {
                        contentProviderClientAcquireContentProviderClient.release();
                    }
                    return false;
                } catch (Exception e) {
                    e = e;
                    contentProviderClient = contentProviderClientAcquireContentProviderClient;
                    Log.e("CalUtils", "failed to acquire calendar's ContentProvider.");
                    e.printStackTrace();
                    if (contentProviderClient != null) {
                        contentProviderClient.release();
                    }
                    return false;
                } catch (Throwable th) {
                    th = th;
                    contentProviderClient = contentProviderClientAcquireContentProviderClient;
                }
            } catch (Throwable th2) {
                th = th2;
            }
            if (contentProviderClient != null) {
                contentProviderClient.release();
            }
            throw th;
        } catch (Exception e2) {
            e = e2;
        }
    }

    public static Time getFirstDisplayTimeInCalendar(Context context) {
        Time time = new Time(getTimeZone(context, null));
        int firstDayOfWeek = getFirstDayOfWeek(context);
        int i = 0;
        int julianMondayFromWeeksSinceEpoch = Time.getJulianMondayFromWeeksSinceEpoch(0);
        switch (firstDayOfWeek) {
            case 0:
                i = julianMondayFromWeeksSinceEpoch - 1;
                break;
            case 1:
                i = julianMondayFromWeeksSinceEpoch;
                break;
            case 2:
                i = julianMondayFromWeeksSinceEpoch - 6;
                break;
            case 3:
                i = julianMondayFromWeeksSinceEpoch - 5;
                break;
            case 4:
                i = julianMondayFromWeeksSinceEpoch - 4;
                break;
            case 5:
                i = julianMondayFromWeeksSinceEpoch - 3;
                break;
            case 6:
                i = julianMondayFromWeeksSinceEpoch - 2;
                break;
        }
        time.setJulianDay(i);
        return time;
    }

    public static Time getLastDisplayTimeInCalendar(Context context) {
        Time time = new Time(getTimeZone(context, null));
        int firstDayOfWeek = getFirstDayOfWeek(context);
        int i = 2465059;
        int julianMondayFromWeeksSinceEpoch = Time.getJulianMondayFromWeeksSinceEpoch(Time.getWeeksSinceEpochFromJulianDay(2465059, firstDayOfWeek));
        switch (firstDayOfWeek) {
            case 0:
                i = julianMondayFromWeeksSinceEpoch + 5;
                break;
            case 1:
                i = julianMondayFromWeeksSinceEpoch + 6;
                break;
            case 2:
                i = julianMondayFromWeeksSinceEpoch;
                break;
            case 3:
                i = julianMondayFromWeeksSinceEpoch + 1;
                break;
            case 4:
                i = julianMondayFromWeeksSinceEpoch + 2;
                break;
            case 5:
                i = julianMondayFromWeeksSinceEpoch + 3;
                break;
            case 6:
                i = julianMondayFromWeeksSinceEpoch + 4;
                break;
        }
        time.setJulianDay(i);
        return time;
    }

    public static Time getValidTimeInCalendar(Context context, Time time) {
        if (time == null) {
            return null;
        }
        long millis = time.toMillis(false);
        long millis2 = getFirstDisplayTimeInCalendar(context).toMillis(false);
        long millis3 = getLastDisplayTimeInCalendar(context).toMillis(false);
        if (millis < millis2 || millis > millis3) {
            time.setToNow();
        }
        return time;
    }

    public static Time getValidTimeInCalendar(Context context, long j) {
        Time time = new Time(getTimeZone(context, null));
        time.set(j);
        long millis = getFirstDisplayTimeInCalendar(context).toMillis(false);
        long millis2 = getLastDisplayTimeInCalendar(context).toMillis(false);
        if (j < millis || j > millis2) {
            time.setToNow();
        }
        return time;
    }

    public static long setJulianDayInGeneral(Time time, int i) {
        int i2 = i - 2440588;
        Time time2 = new Time(time.timezone);
        if (i2 <= 0) {
            LogUtil.d("CalUtils", "Julian day before epoch day, adjust by epoch day");
            time2.monthDay += i2;
            time.set(time2);
        } else {
            time.setJulianDay(i);
        }
        time.normalize(true);
        return time.toMillis(true);
    }

    public static int getJulianDayInGeneral(Time time, boolean z) {
        Time time2 = new Time(time);
        time2.hour = 0;
        time2.minute = 0;
        time2.second = 0;
        time2.normalize(z);
        long millis = time2.toMillis(z);
        long millis2 = millis - new Time(time.timezone).toMillis(z);
        if (millis2 < 0) {
            LogUtil.d("CalUtils", "Julian day before epoch day, adjust by epoch day");
            return 2440588 + ((int) (millis2 / 86400000));
        }
        return Time.getJulianDay(millis, time.gmtoff);
    }
}
