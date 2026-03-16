package com.android.alarmclock;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.text.format.DateFormat;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import com.android.deskclock.R;
import com.android.deskclock.Utils;
import com.android.deskclock.worldclock.CityObj;
import com.android.deskclock.worldclock.WorldClockAdapter;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class DigitalWidgetViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private RemoteWorldClockAdapter mAdapter;
    private Context mContext;
    private float mFontScale;
    private int mId;
    private Resources mResources;

    private class RemoteWorldClockAdapter extends WorldClockAdapter {
        private final float mFont24Size;
        private final float mFontSize;

        public RemoteWorldClockAdapter(Context context) {
            super(context);
            this.mClocksPerRow = context.getResources().getInteger(R.integer.appwidget_world_clocks_per_row);
            this.mFontSize = context.getResources().getDimension(R.dimen.widget_medium_font_size);
            this.mFont24Size = context.getResources().getDimension(R.dimen.widget_24_medium_font_size);
        }

        public RemoteViews getViewAt(int position) {
            int index = position * 2;
            if (index < 0 || index >= this.mCitiesList.length) {
                return null;
            }
            RemoteViews views = new RemoteViews(DigitalWidgetViewsFactory.this.mContext.getPackageName(), R.layout.world_clock_remote_list_item);
            updateView(views, (CityObj) this.mCitiesList[index], R.id.left_clock, R.id.city_name_left, R.id.city_day_left);
            if (index + 1 < this.mCitiesList.length) {
                updateView(views, (CityObj) this.mCitiesList[index + 1], R.id.right_clock, R.id.city_name_right, R.id.city_day_right);
            } else {
                hideView(views, R.id.right_clock, R.id.city_name_right, R.id.city_day_right);
            }
            int lastRow = ((this.mCitiesList.length + 1) / 2) - 1;
            if (position == lastRow) {
                views.setViewVisibility(R.id.city_spacer, 8);
                return views;
            }
            views.setViewVisibility(R.id.city_spacer, 0);
            return views;
        }

        private void updateView(RemoteViews clock, CityObj cityObj, int clockId, int labelId, int dayId) {
            Calendar now = Calendar.getInstance();
            now.setTimeInMillis(System.currentTimeMillis());
            int myDayOfWeek = now.get(7);
            CityObj cityInDb = this.mCitiesDb.get(cityObj.mCityId);
            String cityTZ = cityInDb != null ? cityInDb.mTimeZone : cityObj.mTimeZone;
            now.setTimeZone(TimeZone.getTimeZone(cityTZ));
            int cityDayOfWeek = now.get(7);
            WidgetUtils.setTimeFormat(clock, (int) DigitalWidgetViewsFactory.this.mResources.getDimension(R.dimen.widget_label_font_size), clockId);
            float fontSize = DigitalWidgetViewsFactory.this.mFontScale * (DateFormat.is24HourFormat(DigitalWidgetViewsFactory.this.mContext) ? this.mFont24Size : this.mFontSize);
            clock.setTextViewTextSize(clockId, 0, DigitalWidgetViewsFactory.this.mFontScale * fontSize);
            clock.setString(clockId, "setTimeZone", cityObj.mTimeZone);
            clock.setTextViewText(labelId, Utils.getCityName(cityObj, cityInDb));
            if (myDayOfWeek != cityDayOfWeek) {
                clock.setTextViewText(dayId, DigitalWidgetViewsFactory.this.mContext.getString(R.string.world_day_of_week_label, now.getDisplayName(7, 1, Locale.getDefault())));
                clock.setViewVisibility(dayId, 0);
            } else {
                clock.setViewVisibility(dayId, 8);
            }
            clock.setViewVisibility(clockId, 0);
            clock.setViewVisibility(labelId, 0);
        }

        private void hideView(RemoteViews clock, int clockId, int labelId, int dayId) {
            clock.setViewVisibility(clockId, 4);
            clock.setViewVisibility(labelId, 4);
            clock.setViewVisibility(dayId, 4);
        }
    }

    public DigitalWidgetViewsFactory(Context context, Intent intent) {
        this.mId = 0;
        this.mFontScale = 1.0f;
        this.mContext = context;
        this.mResources = this.mContext.getResources();
        this.mId = intent.getIntExtra("appWidgetId", 0);
        this.mAdapter = new RemoteWorldClockAdapter(context);
    }

    public DigitalWidgetViewsFactory() {
        this.mId = 0;
        this.mFontScale = 1.0f;
    }

    @Override
    public int getCount() {
        if (WidgetUtils.showList(this.mContext, this.mId, this.mFontScale)) {
            return this.mAdapter.getCount();
        }
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews v = this.mAdapter.getViewAt(position);
        if (v != null) {
            Intent fillInIntent = new Intent();
            v.setOnClickFillInIntent(R.id.widget_item, fillInIntent);
        }
        return v;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        this.mAdapter.loadData(this.mContext);
        this.mAdapter.loadCitiesDb(this.mContext);
        this.mAdapter.updateHomeLabel(this.mContext);
        this.mFontScale = WidgetUtils.getScaleRatio(this.mContext, null, this.mId);
    }

    @Override
    public void onDestroy() {
    }
}
