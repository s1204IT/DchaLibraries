package com.android.deskclock;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.AnyRes;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.os.BuildCompat;
import android.support.v4.view.AccessibilityDelegateCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.ArraySet;
import android.view.View;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.provider.AlarmInstance;
import com.android.deskclock.uidata.UiDataModel;
import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {
    public static final Uri RINGTONE_SILENT = Uri.EMPTY;

    public static void enforceMainLooper() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalAccessError("May only call from main thread.");
        }
    }

    public static void enforceNotMainLooper() {
        if (Looper.getMainLooper() == Looper.myLooper()) {
            throw new IllegalAccessError("May not call from main thread.");
        }
    }

    public static int indexOf(Object[] objArr, Object obj) {
        for (int i = 0; i < objArr.length; i++) {
            if (objArr[i].equals(obj)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isPreL() {
        return Build.VERSION.SDK_INT < 21;
    }

    public static boolean isLOrLMR1() {
        int i = Build.VERSION.SDK_INT;
        return i == 21 || i == 22;
    }

    public static boolean isLOrLater() {
        return Build.VERSION.SDK_INT >= 21;
    }

    public static boolean isLMR1OrLater() {
        return Build.VERSION.SDK_INT >= 22;
    }

    public static boolean isMOrLater() {
        return Build.VERSION.SDK_INT >= 23;
    }

    public static boolean isNOrLater() {
        return BuildCompat.isAtLeastN();
    }

    public static boolean isNMR1OrLater() {
        return BuildCompat.isAtLeastNMR1();
    }

    public static Uri getResourceUri(Context context, @AnyRes int i) {
        return new Uri.Builder().scheme("android.resource").authority(context.getPackageName()).path(String.valueOf(i)).build();
    }

    public static boolean isScrolledToTop(View view) {
        return !view.canScrollVertically(-1);
    }

    public static float calculateRadiusOffset(float f, float f2, float f3) {
        return Math.max(f, Math.max(f2, f3));
    }

    public static void setClockSecondsEnabled(TextClock textClock, AnalogClock analogClock) {
        boolean displayClockSeconds = DataModel.getDataModel().getDisplayClockSeconds();
        DataModel.ClockStyle clockStyle = DataModel.getDataModel().getClockStyle();
        switch (clockStyle) {
            case ANALOG:
                setTimeFormat(textClock, false);
                analogClock.enableSeconds(displayClockSeconds);
                return;
            case DIGITAL:
                analogClock.enableSeconds(false);
                setTimeFormat(textClock, displayClockSeconds);
                return;
            default:
                throw new IllegalStateException("unexpected clock style: " + clockStyle);
        }
    }

    public static View setClockStyle(View view, View view2) {
        DataModel.ClockStyle clockStyle = DataModel.getDataModel().getClockStyle();
        switch (clockStyle) {
            case ANALOG:
                view.setVisibility(8);
                view2.setVisibility(0);
                return view2;
            case DIGITAL:
                view.setVisibility(0);
                view2.setVisibility(8);
                return view;
            default:
                throw new IllegalStateException("unexpected clock style: " + clockStyle);
        }
    }

    public static View setScreensaverClockStyle(View view, View view2) {
        DataModel.ClockStyle screensaverClockStyle = DataModel.getDataModel().getScreensaverClockStyle();
        switch (screensaverClockStyle) {
            case ANALOG:
                view.setVisibility(8);
                view2.setVisibility(0);
                return view2;
            case DIGITAL:
                view.setVisibility(0);
                view2.setVisibility(8);
                return view;
            default:
                throw new IllegalStateException("unexpected clock style: " + screensaverClockStyle);
        }
    }

    public static void dimClockView(boolean z, View view) {
        Paint paint = new Paint();
        paint.setColor(-1);
        paint.setColorFilter(new PorterDuffColorFilter(z ? 1090519039 : -1056964609, PorterDuff.Mode.MULTIPLY));
        view.setLayerType(2, paint);
    }

    public static PendingIntent pendingServiceIntent(Context context, Intent intent) {
        return PendingIntent.getService(context, 0, intent, 134217728);
    }

    public static PendingIntent pendingActivityIntent(Context context, Intent intent) {
        return PendingIntent.getActivity(context, 0, intent, 134217728);
    }

    public static String getNextAlarm(Context context) {
        return isPreL() ? getNextAlarmPreL(context) : getNextAlarmLOrLater(context);
    }

    @TargetApi(19)
    private static String getNextAlarmPreL(Context context) {
        return Settings.System.getString(context.getContentResolver(), "next_alarm_formatted");
    }

    @TargetApi(21)
    private static String getNextAlarmLOrLater(Context context) {
        AlarmManager.AlarmClockInfo nextAlarmClock = getNextAlarmClock((AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM));
        if (nextAlarmClock != null) {
            long triggerTime = nextAlarmClock.getTriggerTime();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(triggerTime);
            return AlarmUtils.getFormattedTime(context, calendar);
        }
        return null;
    }

    @TargetApi(21)
    private static AlarmManager.AlarmClockInfo getNextAlarmClock(AlarmManager alarmManager) {
        return alarmManager.getNextAlarmClock();
    }

    @TargetApi(21)
    public static void updateNextAlarm(AlarmManager alarmManager, AlarmManager.AlarmClockInfo alarmClockInfo, PendingIntent pendingIntent) {
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
    }

    public static boolean isAlarmWithin24Hours(AlarmInstance alarmInstance) {
        return alarmInstance.getAlarmTime().getTimeInMillis() - System.currentTimeMillis() <= 86400000;
    }

    public static void refreshAlarm(Context context, View view) {
        TextView textView = (TextView) view.findViewById(R.id.nextAlarmIcon);
        TextView textView2 = (TextView) view.findViewById(R.id.nextAlarm);
        if (textView2 == null) {
            return;
        }
        String nextAlarm = getNextAlarm(context);
        if (!TextUtils.isEmpty(nextAlarm)) {
            String string = context.getString(R.string.next_alarm_description, nextAlarm);
            textView2.setText(nextAlarm);
            textView2.setContentDescription(string);
            textView2.setVisibility(0);
            textView.setVisibility(0);
            textView.setContentDescription(string);
            return;
        }
        textView2.setVisibility(8);
        textView.setVisibility(8);
    }

    public static void setClockIconTypeface(View view) {
        ((TextView) view.findViewById(R.id.nextAlarmIcon)).setTypeface(UiDataModel.getUiDataModel().getAlarmIconTypeface());
    }

    public static void updateDate(String str, String str2, View view) {
        TextView textView = (TextView) view.findViewById(R.id.date);
        if (textView == null) {
            return;
        }
        Locale locale = Locale.getDefault();
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(locale, str);
        String bestDateTimePattern2 = DateFormat.getBestDateTimePattern(locale, str2);
        Date date = new Date();
        textView.setText(new SimpleDateFormat(bestDateTimePattern, locale).format(date));
        textView.setVisibility(0);
        textView.setContentDescription(new SimpleDateFormat(bestDateTimePattern2, locale).format(date));
    }

    public static void setTimeFormat(TextClock textClock, boolean z) {
        if (textClock != null) {
            textClock.setFormat12Hour(get12ModeFormat(0.4f, z));
            textClock.setFormat24Hour(get24ModeFormat(z));
        }
    }

    public static CharSequence get12ModeFormat(float f, boolean z) {
        String bestDateTimePattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), z ? "hmsa" : "hma");
        if (f <= 0.0f) {
            bestDateTimePattern = bestDateTimePattern.replaceAll("a", com.google.android.flexbox.BuildConfig.FLAVOR).trim();
        }
        String strReplaceAll = bestDateTimePattern.replaceAll(" ", "\u200a");
        int iIndexOf = strReplaceAll.indexOf(97);
        if (iIndexOf == -1) {
            return strReplaceAll;
        }
        SpannableString spannableString = new SpannableString(strReplaceAll);
        RelativeSizeSpan relativeSizeSpan = new RelativeSizeSpan(f);
        int i = iIndexOf + 1;
        spannableString.setSpan(relativeSizeSpan, iIndexOf, i, 33);
        spannableString.setSpan(new StyleSpan(0), iIndexOf, i, 33);
        spannableString.setSpan(new TypefaceSpan("sans-serif"), iIndexOf, i, 33);
        return spannableString;
    }

    public static CharSequence get24ModeFormat(boolean z) {
        return DateFormat.getBestDateTimePattern(Locale.getDefault(), z ? "Hms" : "Hm");
    }

    public static String getGMTHourOffset(TimeZone timeZone, boolean z) {
        int rawOffset = timeZone.getRawOffset();
        long j = ((long) rawOffset) / 3600000;
        return z ? String.format(Locale.ENGLISH, "%+d", Long.valueOf(j)) : String.format(Locale.ENGLISH, "GMT %+d:%02d", Long.valueOf(j), Long.valueOf((((long) Math.abs(rawOffset)) % 3600000) / 60000));
    }

    public static Date getNextDay(Date date, Collection<TimeZone> collection) {
        Iterator<TimeZone> it = collection.iterator();
        Calendar calendar = null;
        while (it.hasNext()) {
            Calendar calendar2 = Calendar.getInstance(it.next());
            calendar2.setTime(date);
            calendar2.add(6, 1);
            calendar2.set(11, 0);
            calendar2.set(12, 0);
            calendar2.set(13, 0);
            calendar2.set(14, 0);
            if (calendar == null || calendar2.compareTo(calendar) < 0) {
                calendar = calendar2;
            }
        }
        if (calendar == null) {
            return null;
        }
        return calendar.getTime();
    }

    public static String getNumberFormattedQuantityString(Context context, int i, int i2) {
        return context.getResources().getQuantityString(i, i2, NumberFormat.getInstance().format(i2));
    }

    public static boolean isWidgetClickable(AppWidgetManager appWidgetManager, int i) {
        Bundle appWidgetOptions = appWidgetManager.getAppWidgetOptions(i);
        return (appWidgetOptions == null || appWidgetOptions.getInt("appWidgetCategory", -1) == 2) ? false : true;
    }

    public static VectorDrawableCompat getVectorDrawable(Context context, @DrawableRes int i) {
        return VectorDrawableCompat.create(context.getResources(), i, context.getTheme());
    }

    public static Bitmap createBitmap(View view) {
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmapCreateBitmap));
        return bitmapCreateBitmap;
    }

    @SuppressLint({"NewApi"})
    public static <E> ArraySet<E> newArraySet(Collection<E> collection) {
        ArraySet<E> arraySet = new ArraySet<>(collection.size());
        arraySet.addAll(collection);
        return arraySet;
    }

    public static boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == 1;
    }

    public static boolean isLandscape(Context context) {
        return context.getResources().getConfiguration().orientation == 2;
    }

    public static long now() {
        return DataModel.getDataModel().elapsedRealtime();
    }

    public static long wallClock() {
        return DataModel.getDataModel().currentTimeMillis();
    }

    public static String createHoursDifferentString(Context context, boolean z, boolean z2, int i, int i2) {
        int i3;
        if (z && i != 0) {
            String numberFormattedQuantityString = getNumberFormattedQuantityString(context, R.plurals.hours_short, Math.abs(i));
            String numberFormattedQuantityString2 = getNumberFormattedQuantityString(context, R.plurals.minutes_short, Math.abs(i2));
            if (z2) {
                i3 = R.string.world_hours_minutes_ahead;
            } else {
                i3 = R.string.world_hours_minutes_behind;
            }
            return context.getString(i3, numberFormattedQuantityString, numberFormattedQuantityString2);
        }
        String numberFormattedQuantityString3 = getNumberFormattedQuantityString(context, R.plurals.hours, Math.abs(i));
        String numberFormattedQuantityString4 = getNumberFormattedQuantityString(context, R.plurals.minutes, Math.abs(i2));
        int i4 = z2 ? R.string.world_time_ahead : R.string.world_time_behind;
        Object[] objArr = new Object[1];
        if (z) {
            numberFormattedQuantityString3 = numberFormattedQuantityString4;
        }
        objArr[0] = numberFormattedQuantityString3;
        return context.getString(i4, objArr);
    }

    static String getTimeString(Context context, int i, int i2, int i3) {
        if (i != 0) {
            return context.getString(R.string.hours_minutes_seconds, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3));
        }
        if (i2 != 0) {
            return context.getString(R.string.minutes_seconds, Integer.valueOf(i2), Integer.valueOf(i3));
        }
        return context.getString(R.string.seconds, Integer.valueOf(i3));
    }

    public static final class ClickAccessibilityDelegate extends AccessibilityDelegateCompat {
        private final boolean mIsAlwaysAccessibilityVisible;
        private final String mLabel;

        public ClickAccessibilityDelegate(String str) {
            this(str, false);
        }

        public ClickAccessibilityDelegate(String str, boolean z) {
            this.mLabel = str;
            this.mIsAlwaysAccessibilityVisible = z;
        }

        @Override
        public void onInitializeAccessibilityNodeInfo(View view, AccessibilityNodeInfoCompat accessibilityNodeInfoCompat) {
            super.onInitializeAccessibilityNodeInfo(view, accessibilityNodeInfoCompat);
            if (this.mIsAlwaysAccessibilityVisible) {
                accessibilityNodeInfoCompat.setVisibleToUser(true);
            }
            accessibilityNodeInfoCompat.addAction(new AccessibilityNodeInfoCompat.AccessibilityActionCompat(AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK.getId(), this.mLabel));
        }
    }

    public static boolean isRingtoneExisted(Context context, Uri uri) throws Throwable {
        boolean zExists;
        String string = uri.toString();
        if (string == null) {
            return false;
        }
        if (string.contains("internal") || string.contains("system") || string.contains("deskclock")) {
            return true;
        }
        String realPathFromURI = getRealPathFromURI(context, uri);
        if (!TextUtils.isEmpty(realPathFromURI)) {
            zExists = new File(realPathFromURI).exists();
        } else {
            zExists = false;
        }
        LogUtils.i("isRingtoneExisted: " + zExists + " ,ringtone: " + string + " ,Path: " + realPathFromURI, new Object[0]);
        return zExists;
    }

    public static String getRealPathFromURI(Context context, Uri uri) throws Throwable {
        Cursor cursorQuery;
        String string;
        LogUtils.i("getRealPathFromURI alarmRingtone: " + uri.toString(), new Object[0]);
        if (uri == null) {
            return null;
        }
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals(com.google.android.flexbox.BuildConfig.FLAVOR) || scheme.equals("file")) {
            return uri.getPath();
        }
        if (scheme.equals("http")) {
            return uri.toString();
        }
        try {
            if (!scheme.equals("content")) {
                LogUtils.w("Given Uri scheme is not supported", new Object[0]);
                return null;
            }
            try {
                cursorQuery = context.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                if (cursorQuery != null) {
                    try {
                        if (cursorQuery.getCount() == 0 || !cursorQuery.moveToFirst()) {
                            LogUtils.w("Given Uri could not be found in media store", new Object[0]);
                            string = null;
                        } else {
                            string = cursorQuery.getString(cursorQuery.getColumnIndexOrThrow("_data"));
                        }
                    } catch (SQLiteException e) {
                        e = e;
                        LogUtils.e("database operation error: " + e.getMessage(), new Object[0]);
                        if (cursorQuery == null) {
                            return null;
                        }
                        cursorQuery.close();
                        return null;
                    } catch (IllegalArgumentException e2) {
                        e = e2;
                        LogUtils.e("IllegalArgument error: " + e.getMessage(), new Object[0]);
                        if (cursorQuery == null) {
                            return null;
                        }
                        cursorQuery.close();
                        return null;
                    }
                }
                if (cursorQuery != null) {
                    cursorQuery.close();
                }
                return string;
            } catch (SQLiteException e3) {
                e = e3;
                cursorQuery = null;
            } catch (IllegalArgumentException e4) {
                e = e4;
                cursorQuery = null;
            } catch (Throwable th) {
                th = th;
                context = 0;
                if (context != 0) {
                    context.close();
                }
                throw th;
            }
        } catch (Throwable th2) {
            th = th2;
        }
    }
}
