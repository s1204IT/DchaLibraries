package com.android.phone.settings;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.preference.RingtonePreference;
import android.util.AttributeSet;
import com.android.internal.telephony.Phone;
import com.android.phone.common.util.SettingsUtil;

public class VoicemailRingtonePreference extends RingtonePreference {
    private Phone mPhone;
    private Handler mVoicemailRingtoneLookupComplete;
    private Runnable mVoicemailRingtoneLookupRunnable;

    public VoicemailRingtonePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mVoicemailRingtoneLookupComplete = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case 1:
                        VoicemailRingtonePreference.this.setSummary((CharSequence) msg.obj);
                        break;
                }
            }
        };
    }

    public void init(Phone phone) {
        this.mPhone = phone;
        VoicemailNotificationSettingsUtil.getRingtoneUri(phone);
        final String preferenceKey = VoicemailNotificationSettingsUtil.getVoicemailRingtoneSharedPrefsKey(this.mPhone);
        this.mVoicemailRingtoneLookupRunnable = new Runnable() {
            @Override
            public void run() {
                SettingsUtil.updateRingtoneName(this.getContext(), VoicemailRingtonePreference.this.mVoicemailRingtoneLookupComplete, 2, preferenceKey, 1);
            }
        };
        updateRingtoneName();
    }

    @Override
    protected Uri onRestoreRingtone() {
        return VoicemailNotificationSettingsUtil.getRingtoneUri(this.mPhone);
    }

    @Override
    protected void onSaveRingtone(Uri ringtoneUri) {
        VoicemailNotificationSettingsUtil.setRingtoneUri(this.mPhone, ringtoneUri);
        updateRingtoneName();
    }

    private void updateRingtoneName() {
        new Thread(this.mVoicemailRingtoneLookupRunnable).start();
    }
}
