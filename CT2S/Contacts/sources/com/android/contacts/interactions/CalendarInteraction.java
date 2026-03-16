package com.android.contacts.interactions;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.provider.CalendarContract;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.format.Time;
import com.android.contacts.R;

public class CalendarInteraction implements ContactInteraction {
    private static final String TAG = CalendarInteraction.class.getSimpleName();
    private ContentValues mValues;

    public CalendarInteraction(ContentValues values) {
        this.mValues = values;
    }

    @Override
    public Intent getIntent() {
        return new Intent("android.intent.action.VIEW").setData(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, getEventId().intValue()));
    }

    @Override
    public long getInteractionDate() {
        return getDtstart().longValue();
    }

    @Override
    public String getViewHeader(Context context) {
        String title = getTitle();
        if (TextUtils.isEmpty(title)) {
            return context.getResources().getString(R.string.untitled_event);
        }
        return title;
    }

    @Override
    public String getViewBody(Context context) {
        return null;
    }

    @Override
    public String getViewFooter(Context context) {
        String localTimezone = Time.getCurrentTimezone();
        Long dateEnd = getDtend();
        Long dateStart = getDtstart();
        if (dateStart == null && dateEnd == null) {
            return null;
        }
        if (dateEnd == null) {
            dateEnd = dateStart;
        } else if (dateStart == null) {
            dateStart = dateEnd;
        }
        return CalendarInteractionUtils.getDisplayedDatetime(dateStart.longValue(), dateEnd.longValue(), System.currentTimeMillis(), localTimezone, getAllDay().booleanValue(), context);
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getResources().getDrawable(R.drawable.ic_event_24dp);
    }

    @Override
    public Drawable getBodyIcon(Context context) {
        return null;
    }

    @Override
    public Drawable getFooterIcon(Context context) {
        return null;
    }

    public Integer getEventId() {
        return this.mValues.getAsInteger("event_id");
    }

    public Boolean getAllDay() {
        return Boolean.valueOf(this.mValues.getAsInteger("allDay").intValue() == 1);
    }

    public Long getDtend() {
        return this.mValues.getAsLong("dtend");
    }

    public Long getDtstart() {
        return this.mValues.getAsLong("dtstart");
    }

    public String getTitle() {
        return this.mValues.getAsString("title");
    }

    @Override
    public Spannable getContentDescription(Context context) {
        return null;
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.ic_event_24dp;
    }
}
