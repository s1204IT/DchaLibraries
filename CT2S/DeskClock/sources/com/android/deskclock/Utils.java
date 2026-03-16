package com.android.deskclock;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.deskclock.worldclock.CityObj;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

public class Utils {
    private static String sCachedVersionCode = null;
    private static String[] sShortWeekdays = null;
    public static final String[] BACKGROUND_SPECTRUM = {"#212121", "#27232e", "#2d253a", "#332847", "#382a53", "#3e2c5f", "#442e6c", "#393a7a", "#2e4687", "#235395", "#185fa2", "#0d6baf", "#0277bd", "#0d6cb1", "#1861a6", "#23569b", "#2d4a8f", "#383f84", "#433478", "#3d3169", "#382e5b", "#322b4d", "#2c273e", "#272430"};

    public static boolean isKitKatOrLater() {
        return Build.VERSION.SDK_INT > 18;
    }

    public static void prepareHelpMenuItem(Context context, MenuItem helpMenuItem) {
        String helpUrlString = context.getResources().getString(R.string.desk_clock_help_url);
        if (TextUtils.isEmpty(helpUrlString)) {
            helpMenuItem.setVisible(false);
            return;
        }
        Uri fullUri = uriWithAddedParameters(context, Uri.parse(helpUrlString));
        Intent intent = new Intent("android.intent.action.VIEW", fullUri);
        intent.setFlags(276824064);
        helpMenuItem.setIntent(intent);
        helpMenuItem.setShowAsAction(0);
        helpMenuItem.setVisible(true);
    }

    private static Uri uriWithAddedParameters(Context context, Uri baseUri) {
        Uri.Builder builder = baseUri.buildUpon();
        builder.appendQueryParameter("hl", Locale.getDefault().toString());
        if (sCachedVersionCode == null) {
            try {
                PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                sCachedVersionCode = Integer.toString(info.versionCode);
                builder.appendQueryParameter("version", sCachedVersionCode);
            } catch (PackageManager.NameNotFoundException e) {
                LogUtils.wtf("Invalid package name for context " + e, new Object[0]);
            }
        } else {
            builder.appendQueryParameter("version", sCachedVersionCode);
        }
        return builder.build();
    }

    public static long getTimeNow() {
        return SystemClock.elapsedRealtime();
    }

    public static float calculateRadiusOffset(float strokeSize, float dotStrokeSize, float markerStrokeSize) {
        return Math.max(strokeSize, Math.max(dotStrokeSize, markerStrokeSize));
    }

    public static float calculateRadiusOffset(Resources resources) {
        if (resources == null) {
            return 0.0f;
        }
        float strokeSize = resources.getDimension(R.dimen.circletimer_circle_size);
        float dotStrokeSize = resources.getDimension(R.dimen.circletimer_dot_size);
        float markerStrokeSize = resources.getDimension(R.dimen.circletimer_marker_size);
        return calculateRadiusOffset(strokeSize, dotStrokeSize, markerStrokeSize);
    }

    public static int getPressedColorId() {
        return R.color.hot_pink;
    }

    public static int getGrayColorId() {
        return R.color.clock_gray;
    }

    public static void clearSwSharedPref(SharedPreferences prefs) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove("sw_start_time");
        editor.remove("sw_accum_time");
        editor.remove("sw_state");
        int lapNum = prefs.getInt("sw_lap_num", 0);
        for (int i = 0; i < lapNum; i++) {
            String key = "sw_lap_time_" + Integer.toString(i);
            editor.remove(key);
        }
        editor.remove("sw_lap_num");
        editor.apply();
    }

    public static void showInUseNotifications(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction("notif_in_use_show");
        context.sendBroadcast(timerIntent);
    }

    public static void showTimesUpNotifications(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction("notif_times_up_show");
        context.sendBroadcast(timerIntent);
    }

    public static void cancelTimesUpNotifications(Context context) {
        Intent timerIntent = new Intent();
        timerIntent.setAction("notif_times_up_cancel");
        context.sendBroadcast(timerIntent);
    }

    public static class ScreensaverMoveSaverRunnable implements Runnable {
        private static TimeInterpolator mSlowStartWithBrakes;
        private View mContentView;
        private final Handler mHandler;
        private View mSaverView;

        public ScreensaverMoveSaverRunnable(Handler handler) {
            this.mHandler = handler;
            mSlowStartWithBrakes = new TimeInterpolator() {
                @Override
                public float getInterpolation(float x) {
                    return ((float) (Math.cos((Math.pow(x, 3.0d) + 1.0d) * 3.141592653589793d) / 2.0d)) + 0.5f;
                }
            };
        }

        public void registerViews(View contentView, View saverView) {
            this.mContentView = contentView;
            this.mSaverView = saverView;
        }

        @Override
        public void run() {
            long delay;
            if (this.mContentView == null || this.mSaverView == null) {
                this.mHandler.removeCallbacks(this);
                this.mHandler.postDelayed(this, 60000L);
                return;
            }
            float xrange = this.mContentView.getWidth() - this.mSaverView.getWidth();
            float yrange = this.mContentView.getHeight() - this.mSaverView.getHeight();
            if (xrange == 0.0f && yrange == 0.0f) {
                delay = 500;
            } else {
                int nextx = (int) (Math.random() * ((double) xrange));
                int nexty = (int) (Math.random() * ((double) yrange));
                if (this.mSaverView.getAlpha() == 0.0f) {
                    this.mSaverView.setX(nextx);
                    this.mSaverView.setY(nexty);
                    ObjectAnimator.ofFloat(this.mSaverView, "alpha", 0.0f, 1.0f).setDuration(3000L).start();
                } else {
                    AnimatorSet s = new AnimatorSet();
                    Animator xMove = ObjectAnimator.ofFloat(this.mSaverView, "x", this.mSaverView.getX(), nextx);
                    Animator yMove = ObjectAnimator.ofFloat(this.mSaverView, "y", this.mSaverView.getY(), nexty);
                    Animator xShrink = ObjectAnimator.ofFloat(this.mSaverView, "scaleX", 1.0f, 0.85f);
                    Animator xGrow = ObjectAnimator.ofFloat(this.mSaverView, "scaleX", 0.85f, 1.0f);
                    Animator yShrink = ObjectAnimator.ofFloat(this.mSaverView, "scaleY", 1.0f, 0.85f);
                    Animator yGrow = ObjectAnimator.ofFloat(this.mSaverView, "scaleY", 0.85f, 1.0f);
                    AnimatorSet shrink = new AnimatorSet();
                    shrink.play(xShrink).with(yShrink);
                    AnimatorSet grow = new AnimatorSet();
                    grow.play(xGrow).with(yGrow);
                    Animator fadeout = ObjectAnimator.ofFloat(this.mSaverView, "alpha", 1.0f, 0.0f);
                    Animator fadein = ObjectAnimator.ofFloat(this.mSaverView, "alpha", 0.0f, 1.0f);
                    AccelerateInterpolator accel = new AccelerateInterpolator();
                    DecelerateInterpolator decel = new DecelerateInterpolator();
                    shrink.setDuration(3000L).setInterpolator(accel);
                    fadeout.setDuration(3000L).setInterpolator(accel);
                    grow.setDuration(3000L).setInterpolator(decel);
                    fadein.setDuration(3000L).setInterpolator(decel);
                    s.play(shrink);
                    s.play(fadeout);
                    s.play(xMove.setDuration(0L)).after(3000L);
                    s.play(yMove.setDuration(0L)).after(3000L);
                    s.play(fadein).after(3000L);
                    s.play(grow).after(3000L);
                    s.start();
                }
                long now = System.currentTimeMillis();
                long adjust = now % 60000;
                delay = ((60000 - adjust) + 60000) - 3000;
            }
            this.mHandler.removeCallbacks(this);
            this.mHandler.postDelayed(this, delay);
        }
    }

    public static long getAlarmOnQuarterHour() {
        Calendar nextQuarter = Calendar.getInstance();
        nextQuarter.set(13, 1);
        nextQuarter.set(14, 0);
        int minute = nextQuarter.get(12);
        nextQuarter.add(12, 15 - (minute % 15));
        long alarmOnQuarterHour = nextQuarter.getTimeInMillis();
        long now = System.currentTimeMillis();
        long delta = alarmOnQuarterHour - now;
        if (0 >= delta || delta > 901000) {
            return now + 901000;
        }
        return alarmOnQuarterHour;
    }

    public static void setMidnightUpdater(Handler handler, Runnable runnable) {
        String timezone = TimeZone.getDefault().getID();
        if (handler != null && runnable != null && timezone != null) {
            long now = System.currentTimeMillis();
            Time time = new Time(timezone);
            time.set(now);
            long runInMillis = (((((24 - time.hour) * 3600) - (time.minute * 60)) - time.second) + 1) * 1000;
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, runInMillis);
        }
    }

    public static void cancelMidnightUpdater(Handler handler, Runnable runnable) {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    public static void setQuarterHourUpdater(Handler handler, Runnable runnable) {
        String timezone = TimeZone.getDefault().getID();
        if (handler != null && runnable != null && timezone != null) {
            long runInMillis = getAlarmOnQuarterHour() - System.currentTimeMillis();
            if (runInMillis < 1000) {
                runInMillis = 1000;
            }
            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, runInMillis);
        }
    }

    public static void cancelQuarterHourUpdater(Handler handler, Runnable runnable) {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    public static View setClockStyle(Context context, View digitalClock, View analogClock, String clockStyleKey) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        String defaultClockStyle = context.getResources().getString(R.string.default_clock_style);
        String style = sharedPref.getString(clockStyleKey, defaultClockStyle);
        if (style.equals("analog")) {
            digitalClock.setVisibility(8);
            analogClock.setVisibility(0);
            return analogClock;
        }
        digitalClock.setVisibility(0);
        analogClock.setVisibility(8);
        return digitalClock;
    }

    public static void dimClockView(boolean dim, View clockView) {
        Paint paint = new Paint();
        paint.setColor(-1);
        paint.setColorFilter(new PorterDuffColorFilter(dim ? 1090519039 : -1056964609, PorterDuff.Mode.MULTIPLY));
        clockView.setLayerType(2, paint);
    }

    public static String getNextAlarm(Context context) {
        AlarmManager.AlarmClockInfo info = ((AlarmManager) context.getSystemService("alarm")).getNextAlarmClock();
        if (info == null) {
            return null;
        }
        long triggerTime = info.getTriggerTime();
        Calendar alarmTime = Calendar.getInstance();
        alarmTime.setTimeInMillis(triggerTime);
        String timeString = AlarmUtils.getFormattedTime(context, alarmTime);
        return timeString;
    }

    public static void refreshAlarm(Context context, View clock) {
        String nextAlarm = getNextAlarm(context);
        TextView nextAlarmView = (TextView) clock.findViewById(R.id.nextAlarm);
        if (!TextUtils.isEmpty(nextAlarm) && nextAlarmView != null) {
            nextAlarmView.setText(context.getString(R.string.control_set_alarm_with_existing, nextAlarm));
            nextAlarmView.setContentDescription(context.getResources().getString(R.string.next_alarm_description, nextAlarm));
            nextAlarmView.setVisibility(0);
            return;
        }
        nextAlarmView.setVisibility(8);
    }

    public static void updateDate(String dateFormat, String dateFormatForAccessibility, View clock) {
        Date now = new Date();
        TextView dateDisplay = (TextView) clock.findViewById(R.id.date);
        if (dateDisplay != null) {
            Locale l = Locale.getDefault();
            String fmt = DateFormat.getBestDateTimePattern(l, dateFormat);
            SimpleDateFormat sdf = new SimpleDateFormat(fmt, l);
            dateDisplay.setText(sdf.format(now));
            dateDisplay.setVisibility(0);
            String fmt2 = DateFormat.getBestDateTimePattern(l, dateFormatForAccessibility);
            SimpleDateFormat sdf2 = new SimpleDateFormat(fmt2, l);
            dateDisplay.setContentDescription(sdf2.format(now));
        }
    }

    public static void setTimeFormat(TextClock clock, int amPmFontSize) {
        if (clock != null) {
            clock.setFormat12Hour(get12ModeFormat(amPmFontSize));
            clock.setFormat24Hour(get24ModeFormat());
        }
    }

    public static CharSequence get12ModeFormat(int amPmFontSize) {
        String pattern = DateFormat.getBestDateTimePattern(Locale.getDefault(), "hma");
        if (amPmFontSize <= 0) {
            pattern.replaceAll("a", "").trim();
        }
        String pattern2 = pattern.replaceAll(" ", "\u200a");
        int amPmPos = pattern2.indexOf(97);
        if (amPmPos != -1) {
            Spannable sp = new SpannableString(pattern2);
            sp.setSpan(new StyleSpan(0), amPmPos, amPmPos + 1, 33);
            sp.setSpan(new AbsoluteSizeSpan(amPmFontSize), amPmPos, amPmPos + 1, 33);
            sp.setSpan(new TypefaceSpan("sans-serif"), amPmPos, amPmPos + 1, 33);
            return sp;
        }
        return pattern2;
    }

    public static CharSequence get24ModeFormat() {
        return DateFormat.getBestDateTimePattern(Locale.getDefault(), "Hm");
    }

    public static CityObj[] loadCitiesFromXml(Context c) {
        Resources r = c.getResources();
        String[] cities = r.getStringArray(R.array.cities_names);
        String[] timezones = r.getStringArray(R.array.cities_tz);
        String[] ids = r.getStringArray(R.array.cities_id);
        int minLength = cities.length;
        if (cities.length != timezones.length || ids.length != cities.length) {
            minLength = Math.min(cities.length, Math.min(timezones.length, ids.length));
            LogUtils.e("City lists sizes are not the same, truncating", new Object[0]);
        }
        CityObj[] tempList = new CityObj[minLength];
        for (int i = 0; i < cities.length; i++) {
            tempList[i] = new CityObj(cities[i], timezones[i], ids[i]);
        }
        return tempList;
    }

    public static String getGMTHourOffset(TimeZone timezone, boolean showMinutes) {
        StringBuilder sb = new StringBuilder();
        sb.append("GMT  ");
        int gmtOffset = timezone.getRawOffset();
        if (gmtOffset < 0) {
            sb.append('-');
        } else {
            sb.append('+');
        }
        sb.append(((long) Math.abs(gmtOffset)) / 3600000);
        if (showMinutes) {
            int min = (Math.abs(gmtOffset) / 60000) % 60;
            sb.append(':');
            if (min < 10) {
                sb.append('0');
            }
            sb.append(min);
        }
        return sb.toString();
    }

    public static String getCityName(CityObj city, CityObj dbCity) {
        return (city.mCityId == null || dbCity == null) ? city.mCityName : dbCity.mCityName;
    }

    public static int getCurrentHourColor() {
        int hourOfDay = Calendar.getInstance().get(11);
        return Color.parseColor(BACKGROUND_SPECTRUM[hourOfDay]);
    }

    public static String[] getShortWeekdays() {
        if (sShortWeekdays == null) {
            String[] shortWeekdays = new String[7];
            SimpleDateFormat format = new SimpleDateFormat("EEEEE");
            long aSunday = new GregorianCalendar(2014, 6, 20).getTimeInMillis();
            for (int day = 0; day < 7; day++) {
                shortWeekdays[day] = format.format(new Date((((long) day) * 86400000) + aSunday));
            }
            sShortWeekdays = shortWeekdays;
        }
        return sShortWeekdays;
    }
}
