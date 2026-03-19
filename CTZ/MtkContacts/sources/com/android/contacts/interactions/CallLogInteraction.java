package com.android.contacts.interactions;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.telecom.PhoneAccountHandle;
import android.text.BidiFormatter;
import android.text.Spannable;
import android.text.TextDirectionHeuristics;
import com.android.contacts.GeoUtil;
import com.android.contacts.R;
import com.android.contacts.compat.PhoneNumberUtilsCompat;
import com.android.contacts.model.account.BaseAccountType;
import com.android.contacts.util.BitmapUtil;
import com.android.contacts.util.ContactDisplayUtils;
import com.mediatek.contacts.ExtensionManager;
import com.mediatek.contacts.quickcontact.PhoneAccountUtils;
import com.mediatek.contacts.util.Log;
import com.mediatek.provider.MtkContactsContract;

public class CallLogInteraction implements ContactInteraction {
    private static final int CALL_ARROW_ICON_RES = 2131230836;
    private static final int CALL_LOG_ICON_RES = 2131230924;
    private static final int SIM_ICON_RES = 2131230938;
    private static final String TAG = "CallLogInteraction";
    private static final String URI_TARGET_PREFIX = "tel:";
    private static BidiFormatter sBidiFormatter = BidiFormatter.getInstance();
    private PhoneAccountHandle mPhoneAccountHandle;
    private ContentValues mValues;

    public CallLogInteraction(ContentValues contentValues) {
        this.mValues = contentValues;
        initPhoneAccount();
    }

    @Override
    public Intent getIntent() {
        String number = getNumber();
        if (number == null) {
            return null;
        }
        return new Intent("android.intent.action.CALL").setData(Uri.parse(URI_TARGET_PREFIX + number));
    }

    @Override
    public String getViewHeader(Context context) {
        String asString = this.mValues.getAsString("number");
        if (asString != null) {
            return sBidiFormatter.unicodeWrap(PhoneNumberUtilsCompat.formatNumber(asString, PhoneNumberUtilsCompat.normalizeNumber(asString), GeoUtil.getCurrentCountryIso(context)), TextDirectionHeuristics.LTR);
        }
        return null;
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
        if (getCachedNumberType() == null) {
            return null;
        }
        return MtkContactsContract.CommonDataKinds.Phone.getTypeLabel(context, getCachedNumberType().intValue(), getCachedNumberLabel()).toString();
    }

    @Override
    public String getViewFooter(Context context) {
        Long date = getDate();
        if (date != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(ContactInteractionUtil.formatDateStringFromTimestamp(date.longValue(), context));
            Long duration = getDuration();
            if (duration != null) {
                sb.append("\n");
                sb.append(ContactInteractionUtil.formatDuration(duration.longValue(), context));
            }
            return sb.toString();
        }
        return null;
    }

    @Override
    public Drawable getIcon(Context context) {
        return context.getResources().getDrawable(R.drawable.quantum_ic_phone_vd_theme_24);
    }

    @Override
    public Drawable getBodyIcon(Context context) {
        return null;
    }

    @Override
    public Drawable getFooterIcon(Context context) {
        Resources resources = context.getResources();
        Integer type = getType();
        Drawable drawable = null;
        if (type == null) {
            return null;
        }
        switch (type.intValue()) {
            case 1:
                drawable = resources.getDrawable(R.drawable.ic_call_arrow);
                drawable.mutate().setColorFilter(resources.getColor(R.color.call_arrow_green), PorterDuff.Mode.MULTIPLY);
                break;
            case 2:
                drawable = BitmapUtil.getRotatedDrawable(resources, R.drawable.ic_call_arrow, 180.0f);
                drawable.setColorFilter(resources.getColor(R.color.call_arrow_green), PorterDuff.Mode.MULTIPLY);
                break;
            case 3:
                drawable = resources.getDrawable(R.drawable.ic_call_arrow);
                drawable.mutate().setColorFilter(resources.getColor(R.color.call_arrow_red), PorterDuff.Mode.MULTIPLY);
                break;
        }
        ExtensionManager.getInstance();
        return ExtensionManager.getOp01Extension().getArrowIcon(type.intValue(), drawable);
    }

    private void initPhoneAccount() {
        String asString = this.mValues.getAsString("subscription_component_name");
        String asString2 = this.mValues.getAsString("subscription_id");
        Log.d(TAG, "[initPhoneAccount] accountName: " + asString + ",accountId: " + asString2);
        this.mPhoneAccountHandle = PhoneAccountUtils.getAccount(asString, asString2);
    }

    @Override
    public Drawable getSimIcon(Context context) {
        Drawable accountIcon = PhoneAccountUtils.getAccountIcon(context, this.mPhoneAccountHandle);
        Log.d(TAG, "[getSimIcon] account icon: " + accountIcon);
        return accountIcon;
    }

    @Override
    public String getSimName(Context context) {
        String accountLabel = PhoneAccountUtils.getAccountLabel(context, this.mPhoneAccountHandle);
        Log.d(TAG, "[getSimName] accountName: " + accountLabel);
        return accountLabel;
    }

    public String getCachedName() {
        return this.mValues.getAsString("name");
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

    public Long getDuration() {
        return this.mValues.getAsLong("duration");
    }

    public Boolean getIsRead() {
        return this.mValues.getAsBoolean("is_read");
    }

    public Integer getLimitParamKey() {
        return this.mValues.getAsInteger("limit");
    }

    public Boolean getNew() {
        return this.mValues.getAsBoolean("new");
    }

    public String getNumber() {
        String asString = this.mValues.getAsString("number");
        if (asString == null) {
            return null;
        }
        return sBidiFormatter.unicodeWrap(asString, TextDirectionHeuristics.LTR);
    }

    public Integer getNumberPresentation() {
        return this.mValues.getAsInteger("presentation");
    }

    public Integer getOffsetParamKey() {
        return this.mValues.getAsInteger("offset");
    }

    public Integer getType() {
        return this.mValues.getAsInteger(BaseAccountType.Attr.TYPE);
    }

    @Override
    public Spannable getContentDescription(Context context) {
        String viewHeader = getViewHeader(context);
        return ContactDisplayUtils.getTelephoneTtsSpannable(context.getResources().getString(R.string.content_description_recent_call, getCallTypeString(context), viewHeader, getViewFooter(context)), viewHeader);
    }

    private String getCallTypeString(Context context) {
        Resources resources = context.getResources();
        Integer type = getType();
        if (type == null) {
            return "";
        }
        switch (type.intValue()) {
            case 1:
                return resources.getString(R.string.content_description_recent_call_type_incoming);
            case 2:
                return resources.getString(R.string.content_description_recent_call_type_outgoing);
            case 3:
                return resources.getString(R.string.content_description_recent_call_type_missed);
            default:
                return "";
        }
    }

    @Override
    public int getIconResourceId() {
        return R.drawable.quantum_ic_phone_vd_theme_24;
    }
}
