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
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.util.ContactDisplayUtils;

public class SmsInteraction implements ContactInteraction {
    private static BidiFormatter sBidiFormatter = BidiFormatter.getInstance();
    private ContentValues mValues;

    public SmsInteraction(ContentValues contentValues) {
        this.mValues = contentValues;
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
        return context.getResources().getDrawable(R.drawable.quantum_ic_message_vd_theme_24);
    }

    @Override
    public Drawable getBodyIcon(Context context) {
        return null;
    }

    @Override
    public Drawable getFooterIcon(Context context) {
        return null;
    }

    @Override
    public Drawable getSimIcon(Context context) {
        return null;
    }

    @Override
    public String getSimName(Context context) {
        return null;
    }

    public String getAddress() {
        String asString = this.mValues.getAsString("address");
        if (asString == null) {
            return null;
        }
        return sBidiFormatter.unicodeWrap(asString, sBidiFormatter.isRtlContext() ? TextDirectionHeuristics.RTL : TextDirectionHeuristics.LTR);
    }

    public String getBody() {
        return this.mValues.getAsString("body");
    }

    public Long getDate() {
        return this.mValues.getAsLong("date");
    }

    public Integer getType() {
        return this.mValues.getAsInteger(BaseAccountType.Attr.TYPE);
    }

    @Override
    public Spannable getContentDescription(Context context) {
        String viewBody = getViewBody(context);
        return ContactDisplayUtils.getTelephoneTtsSpannable(context.getResources().getString(R.string.content_description_recent_sms, getViewHeader(context), viewBody, getViewFooter(context)), viewBody);
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.quantum_ic_message_vd_theme_24;
    }
}
