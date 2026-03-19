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

    public AgendaAdapter(Context context, int i) {
        super(context, i, null);
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
        ViewHolder viewHolder;
        String str;
        int i;
        String displayName;
        if (view.getTag() instanceof ViewHolder) {
            viewHolder = (ViewHolder) view.getTag();
        } else {
            viewHolder = null;
        }
        if (viewHolder == null) {
            viewHolder = new ViewHolder();
            view.setTag(viewHolder);
            viewHolder.title = (TextView) view.findViewById(R.id.title);
            viewHolder.when = (TextView) view.findViewById(R.id.when);
            viewHolder.where = (TextView) view.findViewById(R.id.where);
            viewHolder.textContainer = (LinearLayout) view.findViewById(R.id.agenda_item_text_container);
            viewHolder.selectedMarker = view.findViewById(R.id.selected_marker);
            viewHolder.colorChip = (ColorChipView) view.findViewById(R.id.agenda_item_color);
        }
        viewHolder.startTimeMilli = cursor.getLong(7);
        boolean z = cursor.getInt(3) != 0;
        viewHolder.allDay = z;
        int i2 = cursor.getInt(12);
        if (i2 == 2) {
            viewHolder.title.setTextColor(this.mDeclinedColor);
            viewHolder.when.setTextColor(this.mWhereDeclinedColor);
            viewHolder.where.setTextColor(this.mWhereDeclinedColor);
            viewHolder.colorChip.setDrawStyle(2);
        } else {
            viewHolder.title.setTextColor(this.mStandardColor);
            viewHolder.when.setTextColor(this.mWhereColor);
            viewHolder.where.setTextColor(this.mWhereColor);
            if (i2 == 3) {
                viewHolder.colorChip.setDrawStyle(1);
            } else {
                viewHolder.colorChip.setDrawStyle(0);
            }
        }
        ViewGroup.LayoutParams layoutParams = viewHolder.colorChip.getLayoutParams();
        if (z) {
            layoutParams.height = this.COLOR_CHIP_ALL_DAY_HEIGHT;
        } else {
            layoutParams.height = this.COLOR_CHIP_HEIGHT;
        }
        viewHolder.colorChip.setLayoutParams(layoutParams);
        if (cursor.getInt(15) == 0 && cursor.getString(14).equals(cursor.getString(13))) {
            viewHolder.colorChip.setDrawStyle(0);
            viewHolder.title.setTextColor(this.mStandardColor);
            viewHolder.when.setTextColor(this.mStandardColor);
            viewHolder.where.setTextColor(this.mStandardColor);
        }
        TextView textView = viewHolder.title;
        TextView textView2 = viewHolder.when;
        TextView textView3 = viewHolder.where;
        viewHolder.instanceId = cursor.getLong(0);
        viewHolder.colorChip.setColor(Utils.getDisplayColorFromColor(cursor.getInt(5)));
        String string = cursor.getString(1);
        if (string == null || string.length() == 0) {
            string = this.mNoTitleLabel;
        }
        textView.setText(string);
        long j = cursor.getLong(7);
        long j2 = cursor.getLong(8);
        String string2 = cursor.getString(16);
        String timeZone = Utils.getTimeZone(context, this.mTZUpdater);
        if (z) {
            str = "UTC";
            i = 0;
        } else {
            str = timeZone;
            i = 1;
        }
        if (DateFormat.is24HourFormat(context)) {
            i |= 128;
        }
        this.mStringBuilder.setLength(0);
        String str2 = str;
        String string3 = DateUtils.formatDateRange(context, this.mFormatter, j, j2, i, str2).toString();
        if (!z && !TextUtils.equals(str2, string2)) {
            Time time = new Time(str2);
            time.set(j);
            TimeZone timeZone2 = TimeZone.getTimeZone(str2);
            if (timeZone2 != null && !timeZone2.getID().equals("GMT")) {
                displayName = timeZone2.getDisplayName(time.isDst != 0, 0);
            } else {
                displayName = str2;
            }
            string3 = string3 + " (" + displayName + ")";
        }
        textView2.setText(string3);
        String string4 = cursor.getString(2);
        if (string4 != null && string4.length() > 0) {
            textView3.setVisibility(0);
            textView3.setText(string4);
        } else {
            textView3.setVisibility(8);
        }
    }
}
