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

    public AlertAdapter(AlertActivity alertActivity2, int i) {
        super(alertActivity2, i, null);
        setAlertActivity(alertActivity2);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        view.findViewById(R.id.color_square).setBackgroundColor(Utils.getDisplayColorFromColor(cursor.getInt(7)));
        View viewFindViewById = view.findViewById(R.id.repeat_icon);
        if (!TextUtils.isEmpty(cursor.getString(8))) {
            viewFindViewById.setVisibility(0);
        } else {
            viewFindViewById.setVisibility(8);
        }
        updateView(context, view, cursor.getString(1), cursor.getString(2), cursor.getLong(4), cursor.getLong(5), cursor.getInt(3) != 0);
    }

    public static void updateView(Context context, View view, String str, String str2, long j, long j2, boolean z) {
        String string;
        int i;
        Resources resources = context.getResources();
        TextView textView = (TextView) view.findViewById(R.id.event_title);
        TextView textView2 = (TextView) view.findViewById(R.id.when);
        TextView textView3 = (TextView) view.findViewById(R.id.where);
        if (mFirstTime) {
            mPastEventColor = resources.getColor(R.color.alert_past_event);
            mTitleColor = resources.getColor(R.color.alert_event_title);
            mOtherColor = resources.getColor(R.color.alert_event_other);
            mFirstTime = false;
        }
        if (j2 < System.currentTimeMillis()) {
            textView.setTextColor(mPastEventColor);
            textView2.setTextColor(mPastEventColor);
            textView3.setTextColor(mPastEventColor);
        } else {
            textView.setTextColor(mTitleColor);
            textView2.setTextColor(mOtherColor);
            textView3.setTextColor(mOtherColor);
        }
        if (str == null || str.length() == 0) {
            string = resources.getString(R.string.no_title_label);
        } else {
            string = str;
        }
        textView.setText(string);
        String timeZone = Utils.getTimeZone(context, null);
        if (z) {
            i = 8210;
            timeZone = "UTC";
        } else {
            i = 17;
        }
        if (DateFormat.is24HourFormat(context)) {
            i |= 128;
        }
        int i2 = i;
        Time time = new Time(timeZone);
        time.set(j);
        boolean z2 = time.isDst != 0;
        StringBuilder sb = new StringBuilder(Utils.formatDateRange(context, j, j2, i2));
        if (!z && timeZone != Time.getCurrentTimezone()) {
            sb.append(" ");
            sb.append(TimeZone.getTimeZone(timeZone).getDisplayName(z2, 1, Locale.getDefault()));
        }
        textView2.setText(sb.toString());
        if (str2 == null || str2.length() == 0) {
            textView3.setVisibility(8);
        } else {
            textView3.setText(str2);
            textView3.setVisibility(0);
        }
    }

    @Override
    protected void onContentChanged() {
        super.onContentChanged();
        alertActivity.closeActivityIfEmpty();
    }

    private static void setAlertActivity(AlertActivity alertActivity2) {
        alertActivity = alertActivity2;
    }
}
