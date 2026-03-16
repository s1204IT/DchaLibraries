package com.android.contacts.interactions;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.TextDirectionHeuristics;
import com.android.contacts.R;
import com.android.contacts.common.util.BitmapUtil;
import com.android.contacts.common.util.ContactDisplayUtils;

public class CallLogInteraction implements ContactInteraction {
    private static BidiFormatter sBidiFormatter = BidiFormatter.getInstance();
    private ContentValues mValues;

    public CallLogInteraction(ContentValues values) {
        this.mValues = values;
    }

    @Override
    public Intent getIntent() {
        String number = getNumber();
        if (number == null) {
            return null;
        }
        return new Intent("android.intent.action.CALL").setData(Uri.parse("tel:" + number));
    }

    @Override
    public String getViewHeader(Context context) {
        return getNumber();
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
    public String getViewBody(Context context) {
        Integer numberType = getCachedNumberType();
        if (numberType == null) {
            return null;
        }
        return ContactsContract.CommonDataKinds.Phone.getTypeLabel(context.getResources(), getCachedNumberType().intValue(), getCachedNumberLabel()).toString();
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
        return context.getResources().getDrawable(R.drawable.ic_phone_24dp);
    }

    @Override
    public Drawable getBodyIcon(Context context) {
        return null;
    }

    @Override
    public Drawable getFooterIcon(Context context) {
        Drawable callArrow = null;
        Resources res = context.getResources();
        Integer type = getType();
        if (type == null) {
            return null;
        }
        switch (type.intValue()) {
            case 1:
                callArrow = res.getDrawable(R.drawable.ic_call_arrow);
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_green), PorterDuff.Mode.MULTIPLY);
                break;
            case 2:
                callArrow = BitmapUtil.getRotatedDrawable(res, R.drawable.ic_call_arrow, 180.0f);
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_green), PorterDuff.Mode.MULTIPLY);
                break;
            case 3:
                callArrow = res.getDrawable(R.drawable.ic_call_arrow);
                callArrow.setColorFilter(res.getColor(R.color.call_arrow_red), PorterDuff.Mode.MULTIPLY);
                break;
        }
        return callArrow;
    }

    public String getCachedNumberLabel() {
        return this.mValues.getAsString("numberlabel");
    }

    public Integer getCachedNumberType() {
        return this.mValues.getAsInteger("numbertype");
    }

    public Long getDate() {
        return this.mValues.getAsLong("date");
    }

    public String getNumber() {
        String number = this.mValues.getAsString("number");
        if (number == null) {
            return null;
        }
        return sBidiFormatter.unicodeWrap(number, TextDirectionHeuristics.LTR);
    }

    public Integer getType() {
        return this.mValues.getAsInteger("type");
    }

    @Override
    public Spannable getContentDescription(Context context) {
        String phoneNumber = getViewHeader(context);
        String contentDescription = context.getResources().getString(R.string.content_description_recent_call, getCallTypeString(context), phoneNumber, getViewFooter(context));
        return ContactDisplayUtils.getTelephoneTtsSpannable(contentDescription, phoneNumber);
    }

    private String getCallTypeString(Context context) {
        String callType = "";
        Resources res = context.getResources();
        Integer type = getType();
        if (type == null) {
            return "";
        }
        switch (type.intValue()) {
            case 1:
                callType = res.getString(R.string.content_description_recent_call_type_incoming);
                break;
            case 2:
                callType = res.getString(R.string.content_description_recent_call_type_outgoing);
                break;
            case 3:
                callType = res.getString(R.string.content_description_recent_call_type_missed);
                break;
        }
        return callType;
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.ic_phone_24dp;
    }
}
