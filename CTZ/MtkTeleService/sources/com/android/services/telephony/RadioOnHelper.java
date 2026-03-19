package com.android.services.telephony;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneFactory;
import com.android.services.telephony.RadioOnStateListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RadioOnHelper implements RadioOnStateListener.Callback {
    private RadioOnStateListener.Callback mCallback;
    private final Context mContext;
    private boolean mDialByNormal = false;
    private int mEccPhoneId = -1;
    private List<RadioOnStateListener> mInProgressListeners = new ArrayList(TelephonyManager.getDefault().getPhoneCount());
    private boolean mIsRadioOnCallingEnabled;
    private List<RadioOnStateListener> mListeners;

    public RadioOnHelper(Context context) {
        this.mContext = context;
    }

    private void setupListeners() {
        if (this.mListeners != null) {
            return;
        }
        this.mListeners = new ArrayList(TelephonyManager.getDefault().getPhoneCount());
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            this.mListeners.add(new RadioOnStateListener());
        }
    }

    public void triggerRadioOnAndListen(RadioOnStateListener.Callback callback) {
        setupListeners();
        this.mCallback = callback;
        this.mInProgressListeners.clear();
        this.mIsRadioOnCallingEnabled = false;
        for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
            Phone phone = PhoneFactory.getPhone(i);
            if (phone != null) {
                this.mInProgressListeners.add(this.mListeners.get(i));
                if (this.mDialByNormal && phone.getPhoneId() == this.mEccPhoneId) {
                    this.mListeners.get(i).setWaitForInService(true);
                    Log.d(this, "waitForInService set true (i=" + i + ", mDialByNormal=" + this.mDialByNormal + ", phone.getPhoneId()=" + phone.getPhoneId() + ", mEccPhoneId=" + this.mEccPhoneId + ")", new Object[0]);
                }
                this.mListeners.get(i).setWaitForRadioOffFirst(Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) > 0 && phone.isRadioOn());
                this.mListeners.get(i).waitForRadioOn(phone, this);
            }
        }
        powerOnRadio();
    }

    private void powerOnRadio() {
        Log.d(this, "powerOnRadio().", new Object[0]);
        if (Settings.Global.getInt(this.mContext.getContentResolver(), "airplane_mode_on", 0) > 0) {
            Log.d(this, "==> Turning off airplane mode.", new Object[0]);
            Settings.Global.putInt(this.mContext.getContentResolver(), "airplane_mode_on", 0);
            Intent intent = new Intent("android.intent.action.AIRPLANE_MODE");
            intent.putExtra("state", false);
            this.mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
        }
    }

    @Override
    public void onComplete(RadioOnStateListener radioOnStateListener, boolean z) {
        this.mIsRadioOnCallingEnabled = z | this.mIsRadioOnCallingEnabled;
        this.mInProgressListeners.remove(radioOnStateListener);
        if (this.mCallback != null && this.mInProgressListeners.isEmpty()) {
            this.mCallback.onComplete(null, this.mIsRadioOnCallingEnabled);
        }
    }

    @Override
    public boolean isOkToCall(Phone phone, int i) {
        if (this.mCallback == null) {
            return false;
        }
        return this.mCallback.isOkToCall(phone, i);
    }

    public void cleanup() {
        Log.d(this, "cleanup()", new Object[0]);
        Iterator it = new ArrayList(this.mInProgressListeners).iterator();
        while (it.hasNext()) {
            ((RadioOnStateListener) it.next()).cleanup();
        }
    }

    public void setEccByNormalPhoneId(int i) {
        this.mDialByNormal = true;
        this.mEccPhoneId = i;
    }

    public void resetEccByNormalPhoneId() {
        this.mDialByNormal = false;
        this.mEccPhoneId = -1;
    }
}
