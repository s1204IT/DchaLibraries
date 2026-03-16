package com.android.calendar.agenda;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.format.Time;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ResourceCursorAdapter;
import android.widget.TextView;
import com.android.calendar.ColorChipView;
import com.android.calendar.R;
import com.android.calendar.Utils;
import java.util.Formatter;
import java.util.Locale;
import java.util.TimeZone;

public class AgendaAdapter extends ResourceCursorAdapter {
    private int COLOR_CHIP_ALL_DAY_HEIGHT;
    private int COLOR_CHIP_HEIGHT;
    private final int mDeclinedColor;
    private final Formatter mFormatter;
    private final String mNoTitleLabel;
    private final Resources mResources;
    private float mScale;
    private final int mStandardColor;
    private final StringBuilder mStringBuilder;
    private final Runnable mTZUpdater;
    private final int mWhereColor;
    private final int mWhereDeclinedColor;

    static class ViewHolder {
        boolean allDay;
        ColorChipView colorChip;
        boolean grayed;
        long instanceId;
        int julianDay;
        View selectedMarker;
        long startTimeMilli;
        LinearLayout textContainer;
        TextView title;
        TextView when;
        TextView where;

        ViewHolder() {
        }
    }

    public AgendaAdapter(Context context, int resource) {
        super(context, resource, null);
        this.mTZUpdater = new Runnable() {
            @Override
            public void run() {
                AgendaAdapter.this.notifyDataSetChanged();
            }
        };
        this.mResources = context.getResources();
        this.mNoTitleLabel = this.mResources.getString(R.string.no_title_label);
        this.mDeclinedColor = this.mResources.getColor(R.color.agenda_item_declined_color);
        this.mStandardColor = this.mResources.getColor(R.color.agenda_item_standard_color);
        this.mWhereDeclinedColor = this.mResources.getColor(R.color.agenda_item_where_declined_text_color);
        this.mWhereColor = this.mResources.getColor(R.color.agenda_item_where_text_color);
        this.mStringBuilder = new StringBuilder(50);
        this.mFormatter = new Formatter(this.mStringBuilder, Locale.getDefault());
        this.COLOR_CHIP_ALL_DAY_HEIGHT = this.mResources.getInteger(R.integer.color_chip_all_day_height);
        this.COLOR_CHIP_HEIGHT = this.mResources.getInteger(R.integer.color_chip_height);
        if (this.mScale == 0.0f) {
            this.mScale = this.mResources.getDisplayMetrics().density;
            if (this.mScale != 1.0f) {
                this.COLOR_CHIP_ALL_DAY_HEIGHT = (int) (this.COLOR_CHIP_ALL_DAY_HEIGHT * this.mScale);
                this.COLOR_CHIP_HEIGHT = (int) (this.COLOR_CHIP_HEIGHT * this.mScale);
            }
        }
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String displayName;
        ViewHolder holder = null;
        Object tag = view.getTag();
        if (tag instanceof ViewHolder) {
            holder = (ViewHolder) view.getTag();
        }
        if (holder == null) {
            holder = new ViewHolder();
            view.setTag(holder);
            holder.title = (TextView) view.findViewById(R.id.title);
            holder.when = (TextView) view.findViewById(R.id.when);
            holder.where = (TextView) view.findViewById(R.id.where);
            holder.textContainer = (LinearLayout) view.findViewById(R.id.agenda_item_text_container);
            holder.selectedMarker = view.findViewById(R.id.selected_marker);
            holder.colorChip = (ColorChipView) view.findViewById(R.id.agenda_item_color);
        }
        holder.startTimeMilli = cursor.getLong(7);
        boolean allDay = cursor.getInt(3) != 0;
        holder.allDay = allDay;
        int selfAttendeeStatus = cursor.getInt(12);
        if (selfAttendeeStatus == 2) {
            holder.title.setTextColor(this.mDeclinedColor);
            holder.when.setTextColor(this.mWhereDeclinedColor);
            holder.where.setTextColor(this.mWhereDeclinedColor);
            holder.colorChip.setDrawStyle(2);
        } else {
            holder.title.setTextColor(this.mStandardColor);
            holder.when.setTextColor(this.mWhereColor);
            holder.where.setTextColor(this.mWhereColor);
            if (selfAttendeeStatus == 3) {
                holder.colorChip.setDrawStyle(1);
            } else {
                holder.colorChip.setDrawStyle(0);
            }
        }
        ViewGroup.LayoutParams params = holder.colorChip.getLayoutParams();
        if (allDay) {
            params.height = this.COLOR_CHIP_ALL_DAY_HEIGHT;
        } else {
            params.height = this.COLOR_CHIP_HEIGHT;
        }
        holder.colorChip.setLayoutParams(params);
        int canRespond = cursor.getInt(15);
        if (canRespond == 0) {
            String owner = cursor.getString(14);
            String organizer = cursor.getString(13);
            if (owner.equals(organizer)) {
                holder.colorChip.setDrawStyle(0);
                holder.title.setTextColor(this.mStandardColor);
                holder.when.setTextColor(this.mStandardColor);
                holder.where.setTextColor(this.mStandardColor);
            }
        }
        TextView title = holder.title;
        TextView when = holder.when;
        TextView where = holder.where;
        holder.instanceId = cursor.getLong(0);
        int color = Utils.getDisplayColorFromColor(cursor.getInt(5));
        holder.colorChip.setColor(color);
        String titleString = cursor.getString(1);
        if (titleString == null || titleString.length() == 0) {
            titleString = this.mNoTitleLabel;
        }
        title.setText(titleString);
        long begin = cursor.getLong(7);
        long end = cursor.getLong(8);
        String eventTz = cursor.getString(16);
        int flags = 0;
        String tzString = Utils.getTimeZone(context, this.mTZUpdater);
        if (allDay) {
            tzString = "UTC";
        } else {
            flags = 1;
        }
        if (DateFormat.is24HourFormat(context)) {
            flags |= 128;
        }
        this.mStringBuilder.setLength(0);
        String whenString = DateUtils.formatDateRange(context, this.mFormatter, begin, end, flags, tzString).toString();
        if (!allDay && !TextUtils.equals(tzString, eventTz)) {
            Time date = new Time(tzString);
            date.set(begin);
            TimeZone tz = TimeZone.getTimeZone(tzString);
            if (tz == null || tz.getID().equals("GMT")) {
                displayName = tzString;
            } else {
                displayName = tz.getDisplayName(date.isDst != 0, 0);
            }
            whenString = whenString + " (" + displayName + ")";
        }
        when.setText(whenString);
        String whereString = cursor.getString(2);
        if (whereString != null && whereString.length() > 0) {
            where.setVisibility(0);
            where.setText(whereString);
        } else {
            where.setVisibility(8);
        }
    }
}
