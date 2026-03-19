package com.android.alarmclock;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.android.deskclock.LogUtils;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.data.City;
import com.android.deskclock.data.DataModel;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class DigitalAppWidgetCityViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private static final LogUtils.Logger LOGGER = new LogUtils.Logger("DigWidgetViewsFactory");
    private final float m12HourFontSize;
    private final float m24HourFontSize;
    private final Context mContext;
    private City mHomeCity;
    private boolean mShowHomeClock;
    private final int mWidgetId;
    private final Intent mFillInIntent = new Intent();
    private float mFontScale = 1.0f;
    private List<City> mCities = Collections.emptyList();

    public DigitalAppWidgetCityViewsFactory(Context context, Intent intent) {
        this.mContext = context;
        this.mWidgetId = intent.getIntExtra("appWidgetId", 0);
        Resources resources = context.getResources();
        this.m12HourFontSize = resources.getDimension(R.dimen.digital_widget_city_12_medium_font_size);
        this.m24HourFontSize = resources.getDimension(R.dimen.digital_widget_city_24_medium_font_size);
    }

    @Override
    public void onCreate() {
        LOGGER.i("DigitalAppWidgetCityViewsFactory onCreate " + this.mWidgetId, new Object[0]);
    }

    @Override
    public void onDestroy() {
        LOGGER.i("DigitalAppWidgetCityViewsFactory onDestroy " + this.mWidgetId, new Object[0]);
    }

    @Override
    public synchronized int getCount() {
        boolean z;
        z = this.mShowHomeClock;
        return (int) Math.ceil(((double) ((z ? 1 : 0) + this.mCities.size())) / 2.0d);
    }

    @Override
    public synchronized RemoteViews getViewAt(int i) {
        City city;
        City city2;
        City city3;
        RemoteViews remoteViews;
        int i2 = (i * 2) + (this.mShowHomeClock ? -1 : 0);
        int i3 = i2 + 1;
        if (i2 == -1) {
            city2 = this.mHomeCity;
        } else if (i2 < this.mCities.size()) {
            city2 = this.mCities.get(i2);
        } else {
            city = null;
            city3 = i3 < this.mCities.size() ? this.mCities.get(i3) : null;
            remoteViews = new RemoteViews(this.mContext.getPackageName(), R.layout.world_clock_remote_list_item);
            if (city == null) {
                update(remoteViews, city, R.id.left_clock, R.id.city_name_left, R.id.city_day_left);
            } else {
                hide(remoteViews, R.id.left_clock, R.id.city_name_left, R.id.city_day_left);
            }
            if (city3 == null) {
                update(remoteViews, city3, R.id.right_clock, R.id.city_name_right, R.id.city_day_right);
            } else {
                hide(remoteViews, R.id.right_clock, R.id.city_name_right, R.id.city_day_right);
            }
            boolean z = true;
            if (i == getCount() - 1) {
                z = false;
            }
            remoteViews.setViewVisibility(R.id.city_spacer, z ? 8 : 0);
            remoteViews.setOnClickFillInIntent(R.id.widget_item, this.mFillInIntent);
        }
        city = city2;
        city3 = i3 < this.mCities.size() ? this.mCities.get(i3) : null;
        remoteViews = new RemoteViews(this.mContext.getPackageName(), R.layout.world_clock_remote_list_item);
        if (city == null) {
        }
        if (city3 == null) {
        }
        boolean z2 = true;
        if (i == getCount() - 1) {
        }
        remoteViews.setViewVisibility(R.id.city_spacer, z2 ? 8 : 0);
        remoteViews.setOnClickFillInIntent(R.id.widget_item, this.mFillInIntent);
        return remoteViews;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public synchronized void onDataSetChanged() {
        RefreshRunnable refreshRunnable = new RefreshRunnable();
        DataModel.getDataModel().run(refreshRunnable);
        this.mHomeCity = refreshRunnable.mHomeCity;
        this.mCities = refreshRunnable.mCities;
        this.mShowHomeClock = refreshRunnable.mShowHomeClock;
        this.mFontScale = WidgetUtils.getScaleRatio(this.mContext, null, this.mWidgetId, this.mCities.size());
    }

    private void update(RemoteViews remoteViews, City city, int i, int i2, int i3) {
        remoteViews.setCharSequence(i, "setFormat12Hour", Utils.get12ModeFormat(0.4f, false));
        remoteViews.setCharSequence(i, "setFormat24Hour", Utils.get24ModeFormat(false));
        remoteViews.setTextViewTextSize(i, 0, (DateFormat.is24HourFormat(this.mContext) ? this.m24HourFontSize : this.m12HourFontSize) * this.mFontScale);
        remoteViews.setString(i, "setTimeZone", city.getTimeZone().getID());
        remoteViews.setTextViewText(i2, city.getName());
        Calendar calendar = Calendar.getInstance(TimeZone.getDefault());
        Calendar calendar2 = Calendar.getInstance(city.getTimeZone());
        boolean z = calendar.get(7) != calendar2.get(7);
        if (z) {
            remoteViews.setTextViewText(i3, this.mContext.getString(R.string.world_day_of_week_label, calendar2.getDisplayName(7, 1, Locale.getDefault())));
        }
        remoteViews.setViewVisibility(i3, z ? 0 : 8);
        remoteViews.setViewVisibility(i, 0);
        remoteViews.setViewVisibility(i2, 0);
    }

    private void hide(RemoteViews remoteViews, int i, int i2, int i3) {
        remoteViews.setViewVisibility(i3, 4);
        remoteViews.setViewVisibility(i, 4);
        remoteViews.setViewVisibility(i2, 4);
    }

    private static final class RefreshRunnable implements Runnable {
        private List<City> mCities;
        private City mHomeCity;
        private boolean mShowHomeClock;

        private RefreshRunnable() {
        }

        @Override
        public void run() {
            this.mHomeCity = DataModel.getDataModel().getHomeCity();
            this.mCities = new ArrayList(DataModel.getDataModel().getSelectedCities());
            this.mShowHomeClock = DataModel.getDataModel().getShowHomeClock();
        }
    }
}
