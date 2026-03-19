package com.android.phone;

import android.content.Context;
import android.os.AsyncResult;
import android.os.Handler;
import android.os.Message;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import com.android.internal.telephony.Phone;

public class CdmaVoicePrivacySwitchPreference extends SwitchPreference {
    private static final String LOG_TAG = "CdmaVoicePrivacySwitchPreference";
    private final boolean DBG;
    private MyHandler mHandler;
    Phone phone;

    public CdmaVoicePrivacySwitchPreference(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.DBG = true;
        this.mHandler = new MyHandler();
        this.phone = PhoneGlobals.getPhone();
        this.phone.getEnhancedVoicePrivacy(this.mHandler.obtainMessage(0));
    }

    public CdmaVoicePrivacySwitchPreference(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.switchPreferenceStyle);
    }

    public CdmaVoicePrivacySwitchPreference(Context context) {
        this(context, null);
    }

    @Override
    protected void onClick() {
        super.onClick();
        this.phone.enableEnhancedVoicePrivacy(isChecked(), this.mHandler.obtainMessage(1));
    }

    private class MyHandler extends Handler {
        static final int MESSAGE_GET_VP = 0;
        static final int MESSAGE_SET_VP = 1;

        private MyHandler() {
        }

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 0:
                    handleGetVPResponse(message);
                    break;
                case 1:
                    handleSetVPResponse(message);
                    break;
            }
        }

        private void handleGetVPResponse(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                Log.d(CdmaVoicePrivacySwitchPreference.LOG_TAG, "handleGetVPResponse: ar.exception=" + asyncResult.exception);
                CdmaVoicePrivacySwitchPreference.this.setEnabled(false);
                return;
            }
            Log.d(CdmaVoicePrivacySwitchPreference.LOG_TAG, "handleGetVPResponse: VP state successfully queried.");
            int i = ((int[]) asyncResult.result)[0];
            Log.d(CdmaVoicePrivacySwitchPreference.LOG_TAG, "Voice privacy value returned:" + i);
            CdmaVoicePrivacySwitchPreference.this.setChecked(i != 0);
            Settings.Secure.putInt(CdmaVoicePrivacySwitchPreference.this.getContext().getContentResolver(), "enhanced_voice_privacy_enabled", i);
        }

        private void handleSetVPResponse(Message message) {
            AsyncResult asyncResult = (AsyncResult) message.obj;
            if (asyncResult.exception != null) {
                Log.d(CdmaVoicePrivacySwitchPreference.LOG_TAG, "handleSetVPResponse: ar.exception=" + asyncResult.exception);
                CdmaVoicePrivacySwitchPreference.this.setChecked(CdmaVoicePrivacySwitchPreference.this.isChecked() ^ true);
            }
            Log.d(CdmaVoicePrivacySwitchPreference.LOG_TAG, "handleSetVPResponse: re get");
            CdmaVoicePrivacySwitchPreference.this.phone.getEnhancedVoicePrivacy(obtainMessage(0));
        }
    }

    public void setPhone(Phone phone) {
        this.phone = phone;
        this.phone.getEnhancedVoicePrivacy(this.mHandler.obtainMessage(0));
    }
}
