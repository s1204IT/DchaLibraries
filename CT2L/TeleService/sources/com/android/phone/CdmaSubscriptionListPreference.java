package com.android.phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.ListPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;

public class CdmaSubscriptionListPreference extends ListPreference {
    private CdmaSubscriptionButtonHandler mHandler;
    private Phone mPhone;

    public CdmaSubscriptionListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPhone = PhoneFactory.getDefaultPhone();
        this.mHandler = new CdmaSubscriptionButtonHandler();
        setCurrentCdmaSubscriptionModeValue();
    }

    private void setCurrentCdmaSubscriptionModeValue() {
        int cdmaSubscriptionMode = Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", 1);
        setValue(Integer.toString(cdmaSubscriptionMode));
    }

    public CdmaSubscriptionListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void showDialog(Bundle state) {
        setCurrentCdmaSubscriptionModeValue();
        super.showDialog(state);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        int statusCdmaSubscriptionMode;
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            int buttonCdmaSubscriptionMode = Integer.valueOf(getValue()).intValue();
            Log.d("CdmaSubscriptionListPreference", "Setting new value " + buttonCdmaSubscriptionMode);
            switch (buttonCdmaSubscriptionMode) {
                case 0:
                    statusCdmaSubscriptionMode = 0;
                    break;
                case 1:
                    statusCdmaSubscriptionMode = 1;
                    break;
                default:
                    statusCdmaSubscriptionMode = 1;
                    break;
            }
            this.mPhone.setCdmaSubscription(statusCdmaSubscriptionMode, this.mHandler.obtainMessage(0, getValue()));
        }
    }

    private class CdmaSubscriptionButtonHandler extends Handler {
        private CdmaSubscriptionButtonHandler() {
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    handleSetCdmaSubscriptionMode(msg);
                    break;
            }
        }

        private void handleSetCdmaSubscriptionMode(Message msg) {
            CdmaSubscriptionListPreference.this.mPhone = PhoneFactory.getDefaultPhone();
            AsyncResult ar = (AsyncResult) msg.obj;
            if (ar.exception == null) {
                int cdmaSubscriptionMode = Integer.valueOf((String) ar.userObj).intValue();
                Settings.Global.putInt(CdmaSubscriptionListPreference.this.mPhone.getContext().getContentResolver(), "subscription_mode", cdmaSubscriptionMode);
            } else {
                Log.e("CdmaSubscriptionListPreference", "Setting Cdma subscription source failed");
            }
        }
    }
}
