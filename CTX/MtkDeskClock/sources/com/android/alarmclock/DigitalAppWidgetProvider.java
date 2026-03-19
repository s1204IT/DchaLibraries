package com.android.alarmclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RemoteViews;
import android.widget.TextClock;
import android.widget.TextView;
import com.android.deskclock.DeskClock;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;
import com.android.deskclock.uidata.UiDataModel;
import com.android.deskclock.worldclock.CitySelectionActivity;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DigitalAppWidgetProvider extends AppWidgetProvider {
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("DigitalWidgetProvider");
    private static final String ACTION_ON_DAY_CHANGE = "com.android.deskclock.ON_DAY_CHANGE";
    private static final Intent DAY_CHANGE_INTENT = new Intent(ACTION_ON_DAY_CHANGE);

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        updateDayChangeCallback(context);
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        removeDayChangeCallback(context);
    }

    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        int[] appWidgetIds;
        LOGGER.i("onReceive: " + intent, new Object[0]);
        super.onReceive(context, intent);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager == null) {
            return;
        }
        appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, getClass()));
        switch (intent.getAction()) {
            case "android.app.action.NEXT_ALARM_CLOCK_CHANGED":
            case "android.intent.action.DATE_CHANGED":
            case "android.intent.action.LOCALE_CHANGED":
            case "android.intent.action.SCREEN_ON":
            case "android.intent.action.TIME_SET":
            case "android.intent.action.TIMEZONE_CHANGED":
            case "com.android.deskclock.ALARM_CHANGED":
            case "com.android.deskclock.ON_DAY_CHANGE":
            case "com.android.deskclock.WORLD_CITIES_CHANGED":
                for (int i : appWidgetIds) {
                    relayoutWidget(context, appWidgetManager, i, appWidgetManager.getAppWidgetOptions(i));
                }
                break;
        }
        DataModel.getDataModel().updateWidgetCount(getClass(), appWidgetIds.length, R.string.category_digital_widget);
        if (appWidgetIds.length > 0) {
            updateDayChangeCallback(context);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] iArr) {
        super.onUpdate(context, appWidgetManager, iArr);
        for (int i : iArr) {
            relayoutWidget(context, appWidgetManager, i, appWidgetManager.getAppWidgetOptions(i));
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int i, Bundle bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, i, bundle);
        relayoutWidget(context, AppWidgetManager.getInstance(context), i, bundle);
    }

    private static void relayoutWidget(Context context, AppWidgetManager appWidgetManager, int i, Bundle bundle) {
        appWidgetManager.updateAppWidget(i, new RemoteViews(relayoutWidget(context, appWidgetManager, i, bundle, false), relayoutWidget(context, appWidgetManager, i, bundle, true)));
        appWidgetManager.notifyAppWidgetViewDataChanged(i, R.id.world_city_list);
    }

    private static RemoteViews relayoutWidget(Context context, AppWidgetManager appWidgetManager, int i, Bundle bundle, boolean z) {
        Bundle appWidgetOptions;
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.digital_widget);
        if (Utils.isWidgetClickable(appWidgetManager, i)) {
            remoteViews.setOnClickPendingIntent(R.id.digital_widget, PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) DeskClock.class), 0));
        }
        String dateFormat = getDateFormat(context);
        remoteViews.setCharSequence(R.id.date, "setFormat12Hour", dateFormat);
        remoteViews.setCharSequence(R.id.date, "setFormat24Hour", dateFormat);
        String nextAlarm = Utils.getNextAlarm(context);
        if (TextUtils.isEmpty(nextAlarm)) {
            remoteViews.setViewVisibility(R.id.nextAlarm, 8);
            remoteViews.setViewVisibility(R.id.nextAlarmIcon, 8);
        } else {
            remoteViews.setTextViewText(R.id.nextAlarm, nextAlarm);
            remoteViews.setViewVisibility(R.id.nextAlarm, 0);
            remoteViews.setViewVisibility(R.id.nextAlarmIcon, 0);
        }
        if (bundle == null) {
            appWidgetOptions = appWidgetManager.getAppWidgetOptions(i);
        } else {
            appWidgetOptions = bundle;
        }
        Resources resources = context.getResources();
        float f = resources.getDisplayMetrics().density;
        int i2 = (int) (appWidgetOptions.getInt("appWidgetMinWidth") * f);
        int i3 = (int) (appWidgetOptions.getInt("appWidgetMinHeight") * f);
        int i4 = (int) (appWidgetOptions.getInt("appWidgetMaxWidth") * f);
        int i5 = (int) (f * appWidgetOptions.getInt("appWidgetMaxHeight"));
        if (!z) {
            i2 = i4;
        }
        if (z) {
            i3 = i5;
        }
        Sizes sizesOptimizeSizes = optimizeSizes(context, new Sizes(i2, i3, resources.getDimensionPixelSize(R.dimen.widget_max_clock_font_size)), nextAlarm);
        if (LOGGER.isVerboseLoggable()) {
            LOGGER.v(sizesOptimizeSizes.toString(), new Object[0]);
        }
        remoteViews.setImageViewBitmap(R.id.nextAlarmIcon, sizesOptimizeSizes.mIconBitmap);
        remoteViews.setTextViewTextSize(R.id.date, 0, sizesOptimizeSizes.mFontSizePx);
        remoteViews.setTextViewTextSize(R.id.nextAlarm, 0, sizesOptimizeSizes.mFontSizePx);
        remoteViews.setTextViewTextSize(R.id.clock, 0, sizesOptimizeSizes.mClockFontSizePx);
        if (sizesOptimizeSizes.getListHeight() <= resources.getDimensionPixelSize(R.dimen.widget_min_world_city_list_size)) {
            remoteViews.setViewVisibility(R.id.world_city_list, 8);
        } else {
            Intent intent = new Intent(context, (Class<?>) DigitalAppWidgetCityService.class);
            intent.putExtra("appWidgetId", i);
            intent.setData(Uri.parse(intent.toUri(1)));
            remoteViews.setRemoteAdapter(R.id.world_city_list, intent);
            remoteViews.setViewVisibility(R.id.world_city_list, 0);
            if (Utils.isWidgetClickable(appWidgetManager, i)) {
                remoteViews.setPendingIntentTemplate(R.id.world_city_list, PendingIntent.getActivity(context, 0, new Intent(context, (Class<?>) CitySelectionActivity.class), 0));
            }
        }
        return remoteViews;
    }

    private static Sizes optimizeSizes(Context context, Sizes sizes, String str) {
        View viewInflate = LayoutInflater.from(context).inflate(R.layout.digital_widget_sizer, (ViewGroup) null);
        String dateFormat = getDateFormat(context);
        TextClock textClock = (TextClock) viewInflate.findViewById(R.id.date);
        textClock.setFormat12Hour(dateFormat);
        textClock.setFormat24Hour(dateFormat);
        TextView textView = (TextView) viewInflate.findViewById(R.id.nextAlarmIcon);
        TextView textView2 = (TextView) viewInflate.findViewById(R.id.nextAlarm);
        if (TextUtils.isEmpty(str)) {
            textView2.setVisibility(8);
            textView.setVisibility(8);
        } else {
            textView2.setText(str);
            textView2.setVisibility(0);
            textView.setVisibility(0);
            textView.setTypeface(UiDataModel.getUiDataModel().getAlarmIconTypeface());
        }
        Sizes sizesMeasure = measure(sizes, sizes.getLargestClockFontSizePx(), viewInflate);
        if (!sizesMeasure.hasViolations()) {
            return sizesMeasure;
        }
        Sizes sizesMeasure2 = measure(sizes, sizes.getSmallestClockFontSizePx(), viewInflate);
        if (sizesMeasure2.hasViolations()) {
            return sizesMeasure2;
        }
        while (sizesMeasure2.getClockFontSizePx() != sizesMeasure.getClockFontSizePx()) {
            int clockFontSizePx = (sizesMeasure2.getClockFontSizePx() + sizesMeasure.getClockFontSizePx()) / 2;
            if (clockFontSizePx == sizesMeasure2.getClockFontSizePx()) {
                return sizesMeasure2;
            }
            Sizes sizesMeasure3 = measure(sizes, clockFontSizePx, viewInflate);
            if (sizesMeasure3.hasViolations()) {
                sizesMeasure = sizesMeasure3;
            } else {
                sizesMeasure2 = sizesMeasure3;
            }
        }
        return sizesMeasure2;
    }

    private void updateDayChangeCallback(Context context) {
        DataModel dataModel = DataModel.getDataModel();
        List<City> selectedCities = dataModel.getSelectedCities();
        boolean showHomeClock = dataModel.getShowHomeClock();
        if (selectedCities.isEmpty() && !showHomeClock) {
            removeDayChangeCallback(context);
            return;
        }
        ArraySet arraySet = new ArraySet(selectedCities.size() + 2);
        arraySet.add(TimeZone.getDefault());
        if (showHomeClock) {
            arraySet.add(dataModel.getHomeCity().getTimeZone());
        }
        Iterator<City> it = selectedCities.iterator();
        while (it.hasNext()) {
            arraySet.add(it.next().getTimeZone());
        }
        Date nextDay = Utils.getNextDay(new Date(), arraySet);
        getAlarmManager(context).setExact(1, nextDay.getTime(), PendingIntent.getBroadcast(context, 0, DAY_CHANGE_INTENT, 134217728));
    }

    private void removeDayChangeCallback(Context context) {
        PendingIntent broadcast = PendingIntent.getBroadcast(context, 0, DAY_CHANGE_INTENT, 536870912);
        if (broadcast != null) {
            getAlarmManager(context).cancel(broadcast);
            broadcast.cancel();
        }
    }

    private static AlarmManager getAlarmManager(Context context) {
        return (AlarmManager) context.getSystemService(NotificationCompat.CATEGORY_ALARM);
    }

    private static Sizes measure(Sizes sizes, int i, View view) {
        Sizes sizesNewSize = sizes.newSize();
        TextClock textClock = (TextClock) view.findViewById(R.id.date);
        TextClock textClock2 = (TextClock) view.findViewById(R.id.clock);
        TextView textView = (TextView) view.findViewById(R.id.nextAlarm);
        TextView textView2 = (TextView) view.findViewById(R.id.nextAlarmIcon);
        sizesNewSize.setClockFontSizePx(i);
        textClock2.setText(getLongestTimeString(textClock2));
        textClock2.setTextSize(0, sizesNewSize.mClockFontSizePx);
        textClock.setTextSize(0, sizesNewSize.mFontSizePx);
        textView.setTextSize(0, sizesNewSize.mFontSizePx);
        textView2.setTextSize(0, sizesNewSize.mIconFontSizePx);
        textView2.setPadding(sizesNewSize.mIconPaddingPx, 0, sizesNewSize.mIconPaddingPx, 0);
        view.measure(View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(sizesNewSize.mTargetWidthPx), 0), View.MeasureSpec.makeMeasureSpec(View.MeasureSpec.getSize(sizesNewSize.mTargetHeightPx), 0));
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        sizesNewSize.mMeasuredWidthPx = view.getMeasuredWidth();
        sizesNewSize.mMeasuredHeightPx = view.getMeasuredHeight();
        sizesNewSize.mMeasuredTextClockWidthPx = textClock2.getMeasuredWidth();
        sizesNewSize.mMeasuredTextClockHeightPx = textClock2.getMeasuredHeight();
        if (textView2.getVisibility() == 0) {
            sizesNewSize.mIconBitmap = Utils.createBitmap(textView2);
        }
        return sizesNewSize;
    }

    private static CharSequence getLongestTimeString(TextClock textClock) {
        CharSequence format12Hour;
        if (textClock.is24HourModeEnabled()) {
            format12Hour = textClock.getFormat24Hour();
        } else {
            format12Hour = textClock.getFormat12Hour();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.set(0, 0, 0, 23, 59);
        return DateFormat.format(format12Hour, calendar);
    }

    private static String getDateFormat(Context context) {
        return DateFormat.getBestDateTimePattern(Locale.getDefault(), context.getString(R.string.abbrev_wday_month_day_no_year));
    }

    private static final class Sizes {
        private int mClockFontSizePx;
        private int mFontSizePx;
        private Bitmap mIconBitmap;
        private int mIconFontSizePx;
        private int mIconPaddingPx;
        private final int mLargestClockFontSizePx;
        private int mMeasuredHeightPx;
        private int mMeasuredTextClockHeightPx;
        private int mMeasuredTextClockWidthPx;
        private int mMeasuredWidthPx;
        private final int mSmallestClockFontSizePx;
        private final int mTargetHeightPx;
        private final int mTargetWidthPx;

        private Sizes(int i, int i2, int i3) {
            this.mTargetWidthPx = i;
            this.mTargetHeightPx = i2;
            this.mLargestClockFontSizePx = i3;
            this.mSmallestClockFontSizePx = 1;
        }

        private int getLargestClockFontSizePx() {
            return this.mLargestClockFontSizePx;
        }

        private int getSmallestClockFontSizePx() {
            return this.mSmallestClockFontSizePx;
        }

        private int getClockFontSizePx() {
            return this.mClockFontSizePx;
        }

        private void setClockFontSizePx(int i) {
            this.mClockFontSizePx = i;
            this.mFontSizePx = Math.max(1, Math.round(i / 7.5f));
            this.mIconFontSizePx = (int) (this.mFontSizePx * 1.4f);
            this.mIconPaddingPx = this.mFontSizePx / 3;
        }

        private int getListHeight() {
            return this.mTargetHeightPx - this.mMeasuredHeightPx;
        }

        private boolean hasViolations() {
            return this.mMeasuredWidthPx > this.mTargetWidthPx || this.mMeasuredHeightPx > this.mTargetHeightPx;
        }

        private Sizes newSize() {
            return new Sizes(this.mTargetWidthPx, this.mTargetHeightPx, this.mLargestClockFontSizePx);
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(1000);
            sb.append("\n");
            append(sb, "Target dimensions: %dpx x %dpx\n", Integer.valueOf(this.mTargetWidthPx), Integer.valueOf(this.mTargetHeightPx));
            append(sb, "Last valid widget container measurement: %dpx x %dpx\n", Integer.valueOf(this.mMeasuredWidthPx), Integer.valueOf(this.mMeasuredHeightPx));
            append(sb, "Last text clock measurement: %dpx x %dpx\n", Integer.valueOf(this.mMeasuredTextClockWidthPx), Integer.valueOf(this.mMeasuredTextClockHeightPx));
            if (this.mMeasuredWidthPx > this.mTargetWidthPx) {
                append(sb, "Measured width %dpx exceeded widget width %dpx\n", Integer.valueOf(this.mMeasuredWidthPx), Integer.valueOf(this.mTargetWidthPx));
            }
            if (this.mMeasuredHeightPx > this.mTargetHeightPx) {
                append(sb, "Measured height %dpx exceeded widget height %dpx\n", Integer.valueOf(this.mMeasuredHeightPx), Integer.valueOf(this.mTargetHeightPx));
            }
            append(sb, "Clock font: %dpx\n", Integer.valueOf(this.mClockFontSizePx));
            return sb.toString();
        }

        private static void append(StringBuilder sb, String str, Object... objArr) {
            sb.append(String.format(Locale.ENGLISH, str, objArr));
        }
    }
}
