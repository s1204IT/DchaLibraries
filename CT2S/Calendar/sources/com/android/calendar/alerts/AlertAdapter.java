package com.android.calendar.alerts;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.Time;
import android.view.View;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.Locale;
import java.util.TimeZone;

public class AlertAdapter extends ResourceCursorAdapter {
    private static AlertActivity alertActivity;
    private static boolean mFirstTime = true;
    private static int mOtherColor;
    private static int mPastEventColor;
    private static int mTitleColor;

    public AlertAdapter(AlertActivity activity, int resource) {
        super(activity, resource, null);
        alertActivity = activity;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        View square = view.findViewById(R.id.color_square);
        int color = Utils.getDisplayColorFromColor(cursor.getInt(7));
        square.setBackgroundColor(color);
        View repeatContainer = view.findViewById(R.id.repeat_icon);
        String rrule = cursor.getString(8);
        if (!TextUtils.isEmpty(rrule)) {
            repeatContainer.setVisibility(0);
        } else {
            repeatContainer.setVisibility(8);
        }
        String eventName = cursor.getString(1);
        String location = cursor.getString(2);
        long startMillis = cursor.getLong(4);
        long endMillis = cursor.getLong(5);
        boolean allDay = cursor.getInt(3) != 0;
        updateView(context, view, eventName, location, startMillis, endMillis, allDay);
    }

    public static void updateView(Context context, View view, String eventName, String location, long startMillis, long endMillis, boolean allDay) {
        int flags;
        Resources res = context.getResources();
        TextView titleView = (TextView) view.findViewById(R.id.event_title);
        TextView whenView = (TextView) view.findViewById(R.id.when);
        TextView whereView = (TextView) view.findViewById(R.id.where);
        if (mFirstTime) {
            mPastEventColor = res.getColor(R.color.alert_past_event);
            mTitleColor = res.getColor(R.color.alert_event_title);
            mOtherColor = res.getColor(R.color.alert_event_other);
            mFirstTime = false;
        }
        if (endMillis < System.currentTimeMillis()) {
            titleView.setTextColor(mPastEventColor);
            whenView.setTextColor(mPastEventColor);
            whereView.setTextColor(mPastEventColor);
        } else {
            titleView.setTextColor(mTitleColor);
            whenView.setTextColor(mOtherColor);
            whereView.setTextColor(mOtherColor);
        }
        if (eventName == null || eventName.length() == 0) {
            eventName = res.getString(R.string.no_title_label);
        }
        titleView.setText(eventName);
        String tz = Utils.getTimeZone(context, null);
        if (allDay) {
            flags = 8210;
            tz = "UTC";
        } else {
            flags = 17;
        }
        if (DateFormat.is24HourFormat(context)) {
            flags |= 128;
        }
        Time time = new Time(tz);
        time.set(startMillis);
        boolean isDST = time.isDst != 0;
        StringBuilder sb = new StringBuilder(Utils.formatDateRange(context, startMillis, endMillis, flags));
        if (!allDay && tz != Time.getCurrentTimezone()) {
            sb.append(" ").append(TimeZone.getTimeZone(tz).getDisplayName(isDST, 0, Locale.getDefault()));
        }
        String when = sb.toString();
        whenView.setText(when);
        if (location == null || location.length() == 0) {
            whereView.setVisibility(8);
        } else {
            whereView.setText(location);
            whereView.setVisibility(0);
        }
    }

    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        alertActivity.closeActivityIfEmpty();
    }
}
