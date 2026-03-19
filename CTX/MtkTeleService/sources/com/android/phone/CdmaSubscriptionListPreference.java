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
    private static final int CDMA_SUBSCRIPTION_NV = 1;
    private static final int CDMA_SUBSCRIPTION_RUIM_SIM = 0;
    private static final String LOG_TAG = "CdmaSubscriptionListPreference";
    static final int preferredSubscriptionMode = 0;
    private CdmaSubscriptionButtonHandler mHandler;
    private Phone mPhone;

    public CdmaSubscriptionListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mPhone = PhoneFactory.getDefaultPhone();
        this.mHandler = new CdmaSubscriptionButtonHandler();
        setCurrentCdmaSubscriptionModeValue();
    }

    private void setCurrentCdmaSubscriptionModeValue() {
        setValue(Integer.toString(Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "subscription_mode", 0)));
    }

    public CdmaSubscriptionListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void showDialog(Bundle bundle) {
        setCurrentCdmaSubscriptionModeValue();
        super.showDialog(bundle);
    }

    @Override
    protected void onDialogClosed(boolean z) {
        int i;
        super.onDialogClosed(z);
        if (!z) {
            return;
        }
        int i2 = Integer.parseInt(getValue());
        Log.d(LOG_TAG, "Setting new value " + i2);
        switch (i2) {
            case 0:
            default:
                i = 0;
                break;
            case 1:
                i = 1;
                break;
        }
        this.mPhone.setCdmaSubscription(i, this.mHandler.obtainMessage(0, getValue()));
    }

    private class CdmaSubscriptionButtonHandler extends Handler {
        static final int MESSAGE_SET_CDMA_SUBSCRIPTION = 0;

        private CdmaSubscriptionButtonHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            if (message.what == 0) {
                handleSetCdmaSubscriptionMode(message);
            }
        }

        private void handleSetCdmaSubscriptionMode(Message message) {
            CdmaSubscriptionListPreference.this.mPhone = PhoneFactory.getDefaultPhone();
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception == null) {
                Settings.Global.putInt(CdmaSubscriptionListPreference.this.mPhone.getContext().getContentResolver(), "subscription_mode", Integer.parseInt((String) asyncResult.userObj));
            } else {
                Log.e(CdmaSubscriptionListPreference.LOG_TAG, "Setting Cdma subscription source failed");
            }
        }
    }

    public void setPhone(Phone phone) {
        if (phone != null) {
            Log.d(LOG_TAG, "set phone to " + phone.getSubId());
            this.mPhone = phone;
        }
    }
}
