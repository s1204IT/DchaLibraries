package com.android.contacts.interactions;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.TextDirectionHeuristics;
import com.android.contacts.R;
import com.android.contacts.common.util.ContactDisplayUtils;

public class SmsInteraction implements ContactInteraction {
    private static BidiFormatter sBidiFormatter = BidiFormatter.getInstance();
    private ContentValues mValues;

    public SmsInteraction(ContentValues values) {
        this.mValues = values;
    }

    @Override
    public Intent getIntent() {
        String address = getAddress();
        if (address == null) {
            return null;
        }
        return new Intent("android.intent.action.VIEW").setData(Uri.parse("smsto:" + address));
    }

    @Override
    public long getInteractionDate() {
        Long date = getDate();
        if (date == null) {
            return -1L;
        }
        return date.longValue();
    }

    @Override
    public String getViewHeader(Context context) {
        String body = getBody();
        return getType().intValue() == 2 ? context.getResources().getString(R.string.message_from_you_prefix, body) : body;
    }

    @Override
    public String getViewBody(Context context) {
        return getAddress();
    }

    @Override
    public String getViewFooter(Context context) {
        Long date = getDate();
        if (date == null) {
            return null;
        }
        return ContactInteractionUtil.formatDateStringFromTimestamp(date.longValue(), context);
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getResources().getDrawable(R.drawable.ic_message_24dp);
    }

    @Override
    public Drawable getBodyIcon(Context context) {
        return null;
    }

    @Override
    public Drawable getFooterIcon(Context context) {
        return null;
    }

    public String getAddress() {
        String address = this.mValues.getAsString("address");
        if (address == null) {
            return null;
        }
        return sBidiFormatter.unicodeWrap(address, TextDirectionHeuristics.LTR);
    }

    public String getBody() {
        return this.mValues.getAsString("body");
    }

    public Long getDate() {
        return this.mValues.getAsLong("date");
    }

    public Integer getType() {
        return this.mValues.getAsInteger("type");
    }

    @Override
    public Spannable getContentDescription(Context context) {
        String phoneNumber = getViewBody(context);
        String contentDescription = context.getResources().getString(R.string.content_description_recent_sms, getViewHeader(context), phoneNumber, getViewFooter(context));
        return ContactDisplayUtils.getTelephoneTtsSpannable(contentDescription, phoneNumber);
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.ic_message_24dp;
    }
}
