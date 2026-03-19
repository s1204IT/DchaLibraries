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

public class CdmaSystemSelectListPreference extends ListPreference {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "CdmaRoamingListPreference";
    private MyHandler mHandler;
    private Phone mPhone;

    public CdmaSystemSelectListPreference(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mHandler = new MyHandler();
        this.mPhone = PhoneGlobals.getPhone();
        this.mHandler = new MyHandler();
        this.mPhone.queryCdmaRoamingPreference(this.mHandler.obtainMessage(0));
    }

    public CdmaSystemSelectListPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void showDialog(Bundle bundle) {
        if (!this.mPhone.isInEcm()) {
            super.showDialog(bundle);
        }
    }

    @Override
    protected void onDialogClosed(boolean z) {
        super.onDialogClosed(z);
        if (z && getValue() != null) {
            int i = Integer.parseInt(getValue());
            if (i != Settings.Global.getInt(this.mPhone.getContext().getContentResolver(), "roaming_settings", 0)) {
                int i2 = i != 2 ? 0 : 2;
                Settings.Global.putInt(this.mPhone.getContext().getContentResolver(), "roaming_settings", i);
                this.mPhone.setCdmaRoamingPreference(i2, this.mHandler.obtainMessage(1));
                return;
            }
            return;
        }
        Log.d(LOG_TAG, String.format("onDialogClosed: positiveResult=%b value=%s -- do nothing", Boolean.valueOf(z), getValue()));
    }

    private class MyHandler extends Handler {
        static final int MESSAGE_GET_ROAMING_PREFERENCE = 0;
        static final int MESSAGE_SET_ROAMING_PREFERENCE = 1;

        private MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleQueryCdmaRoamingPreference(message);
                    break;
                case 1:
                    handleSetCdmaRoamingPreference(message);
                    break;
            }
        }

        private void handleQueryCdmaRoamingPreference(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception == null) {
                int i = ((int[]) asyncResult.result)[0];
                int i2 = Settings.Global.getInt(CdmaSystemSelectListPreference.this.mPhone.getContext().getContentResolver(), "roaming_settings", 0);
                if (i == 0 || i == 2) {
                    if (i != i2) {
                        Settings.Global.putInt(CdmaSystemSelectListPreference.this.mPhone.getContext().getContentResolver(), "roaming_settings", i);
                    }
                    CdmaSystemSelectListPreference.this.setValue(Integer.toString(i));
                    return;
                }
                resetCdmaRoamingModeToDefault();
            }
        }

        private void handleSetCdmaRoamingPreference(Message message) {
            if (((AsyncResult) message.obj).exception != null || CdmaSystemSelectListPreference.this.getValue() == null) {
                CdmaSystemSelectListPreference.this.mPhone.queryCdmaRoamingPreference(obtainMessage(0));
            } else {
                Settings.Global.putInt(CdmaSystemSelectListPreference.this.mPhone.getContext().getContentResolver(), "roaming_settings", Integer.parseInt(CdmaSystemSelectListPreference.this.getValue()));
            }
        }

        private void resetCdmaRoamingModeToDefault() {
            CdmaSystemSelectListPreference.this.setValue(Integer.toString(2));
            Settings.Global.putInt(CdmaSystemSelectListPreference.this.mPhone.getContext().getContentResolver(), "roaming_settings", 2);
            CdmaSystemSelectListPreference.this.mPhone.setCdmaRoamingPreference(2, obtainMessage(1));
        }
    }

    public void setPhone(Phone phone) {
        if (phone != null && this.mPhone != phone) {
            Log.d(LOG_TAG, "set phone to " + phone.getSubId());
            this.mPhone = phone;
            this.mPhone.queryCdmaRoamingPreference(this.mHandler.obtainMessage(0));
        }
    }
}
