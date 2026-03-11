package com.android.settings.deviceinfo.imei;

import android.content.Context;
import android.content.res.Resources;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.util.Log;
import com.android.settings.R;

public class ImeiInfoDialogController {
    static final int ID_CDMA_SETTINGS = 2131361956;
    static final int ID_GSM_SETTINGS = 2131362164;
    static final int ID_IMEI_SV_VALUE = 2131362203;
    static final int ID_IMEI_VALUE = 2131362204;
    static final int ID_MEID_NUMBER_VALUE = 2131362306;
    static final int ID_MIN_NUMBER_VALUE = 2131362319;
    static final int ID_PRL_VERSION_VALUE = 2131362422;
    private final ImeiInfoDialogFragment mDialog;
    private final int mSlotId;
    private final SubscriptionInfo mSubscriptionInfo;
    private final TelephonyManager mTelephonyManager;

    private static CharSequence getTextAsDigits(CharSequence charSequence) {
        if (charSequence == null) {
            return "";
        }
        if (!TextUtils.isDigitsOnly(charSequence)) {
            return charSequence;
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(charSequence);
        spannableStringBuilder.setSpan(new TtsSpan.DigitsBuilder(charSequence.toString()).build(), 0, spannableStringBuilder.length(), 33);
        return spannableStringBuilder;
    }

    public ImeiInfoDialogController(ImeiInfoDialogFragment imeiInfoDialogFragment, int i) {
        this.mDialog = imeiInfoDialogFragment;
        this.mSlotId = i;
        Context context = imeiInfoDialogFragment.getContext();
        this.mSubscriptionInfo = getSubscriptionInfo(context, i);
        if (this.mSubscriptionInfo != null) {
            this.mTelephonyManager = TelephonyManager.from(context).createForSubscriptionId(this.mSubscriptionInfo.getSubscriptionId());
        } else {
            this.mTelephonyManager = (TelephonyManager) context.getSystemService("phone");
        }
    }

    public void populateImeiInfo() {
        if (this.mTelephonyManager.getCurrentPhoneTypeForSlot(this.mSlotId) == 2) {
            updateDialogForCdmaPhone();
        } else {
            updateDialogForGsmPhone();
        }
    }

    private void updateDialogForCdmaPhone() {
        String cdmaMin;
        Resources resources = this.mDialog.getContext().getResources();
        this.mDialog.setText(R.id.meid_number_value, getMeid());
        ImeiInfoDialogFragment imeiInfoDialogFragment = this.mDialog;
        if (this.mSubscriptionInfo != null) {
            cdmaMin = this.mTelephonyManager.getCdmaMin(this.mSubscriptionInfo.getSubscriptionId());
        } else {
            cdmaMin = "";
        }
        imeiInfoDialogFragment.setText(R.id.min_number_value, cdmaMin);
        if (resources.getBoolean(R.bool.config_msid_enable)) {
            this.mDialog.setText(R.id.min_number_label, resources.getString(R.string.status_msid_number));
        }
        this.mDialog.setText(R.id.prl_version_value, getCdmaPrlVersion());
        if (this.mSubscriptionInfo != null && isCdmaLteEnabled()) {
            this.mDialog.setText(R.id.imei_value, getTextAsDigits(this.mTelephonyManager.getImei(this.mSlotId)));
            this.mDialog.setText(R.id.imei_sv_value, getTextAsDigits(this.mTelephonyManager.getDeviceSoftwareVersion(this.mSlotId)));
        } else {
            this.mDialog.removeViewFromScreen(R.id.gsm_settings);
        }
    }

    private void updateDialogForGsmPhone() {
        this.mDialog.setText(R.id.imei_value, getTextAsDigits(this.mTelephonyManager.getImei(this.mSlotId)));
        this.mDialog.setText(R.id.imei_sv_value, getTextAsDigits(this.mTelephonyManager.getDeviceSoftwareVersion(this.mSlotId)));
        this.mDialog.removeViewFromScreen(R.id.cdma_settings);
    }

    private SubscriptionInfo getSubscriptionInfo(Context context, int i) {
        SubscriptionInfo activeSubscriptionInfoForSimSlotIndex = SubscriptionManager.from(context).getActiveSubscriptionInfoForSimSlotIndex(i);
        StringBuilder sb = new StringBuilder();
        sb.append("getSubscriptionInfo, slotId=");
        sb.append(i);
        sb.append(", subInfo=");
        sb.append(activeSubscriptionInfoForSimSlotIndex == null ? "null" : activeSubscriptionInfoForSimSlotIndex);
        Log.d("ImeiInfoDialogController", sb.toString());
        return activeSubscriptionInfoForSimSlotIndex;
    }

    String getCdmaPrlVersion() {
        return this.mTelephonyManager.getCdmaPrlVersion();
    }

    boolean isCdmaLteEnabled() {
        return this.mTelephonyManager.getLteOnCdmaMode(this.mSubscriptionInfo.getSubscriptionId()) == 1;
    }

    String getMeid() {
        return this.mTelephonyManager.getMeid(this.mSlotId);
    }
}
