package com.android.services.telephony;

import android.os.AsyncResult;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.os.SomeArgs;
import com.android.internal.telephony.Phone;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.PhoneFactory;
import com.android.internal.telephony.TelephonyDevController;
import com.mediatek.internal.telephony.IRadioPower;
import com.mediatek.internal.telephony.RadioCapabilitySwitchUtil;
import com.mediatek.internal.telephony.RadioManager;
import com.mediatek.services.telephony.MtkTelephonyConnectionServiceUtil;
import com.mediatek.telephony.MtkTelephonyManagerEx;

public class RadioOnStateListener {
    public static final int MSG_RADIO_OFF_OR_NOT_AVAILABLE = 4;

    @VisibleForTesting
    public static final int MSG_RETRY_TIMEOUT = 3;

    @VisibleForTesting
    public static final int MSG_SERVICE_STATE_CHANGED = 2;

    @VisibleForTesting
    public static final int MSG_START_SEQUENCE = 1;
    private Callback mCallback;
    private int mNumRetriesSoFar;
    private Phone mPhone;
    private RadioPowerInterface mRadioPowerIf;
    private TelephonyManager mTm;
    private static int MAX_NUM_RETRIES = 5;
    private static long TIME_BETWEEN_RETRIES_MILLIS = 5000;
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    SomeArgs someArgs = (SomeArgs) message.obj;
                    try {
                        RadioOnStateListener.this.startSequenceInternal((Phone) someArgs.arg1, (Callback) someArgs.arg2);
                        RadioOnStateListener.this.mWaitForInService = ((Boolean) someArgs.arg3).booleanValue();
                        return;
                    } finally {
                        someArgs.recycle();
                    }
                case 2:
                    RadioOnStateListener.this.onServiceStateChanged((ServiceState) ((AsyncResult) message.obj).result);
                    return;
                case 3:
                    RadioOnStateListener.this.onRetryTimeout();
                    return;
                case 4:
                    Log.d(this, "MSG_RADIO_OFF_OR_NOT_AVAILABLE", new Object[0]);
                    RadioOnStateListener.this.mWaitForRadioOffFirst = false;
                    return;
                default:
                    Log.wtf(this, "handleMessage: unexpected message: %d.", Integer.valueOf(message.what));
                    return;
            }
        }
    };
    TelephonyDevController mTelDevController = TelephonyDevController.getInstance();
    private boolean mWaitForInService = false;
    private boolean mWaitForRadioOffFirst = false;

    interface Callback {
        boolean isOkToCall(Phone phone, int i);

        void onComplete(RadioOnStateListener radioOnStateListener, boolean z);
    }

    private boolean hasC2kOverImsModem() {
        return (this.mTelDevController == null || this.mTelDevController.getModem(0) == null || !this.mTelDevController.getModem(0).hasC2kOverImsModem()) ? false : true;
    }

    class RadioPowerInterface implements IRadioPower {
        RadioPowerInterface() {
        }

        public void notifyRadioPowerChange(boolean z, int i) {
            Log.d(this, "notifyRadioPowerChange, power:" + z + " phoneId:" + i, new Object[0]);
            if (RadioOnStateListener.this.mPhone != null) {
                if (RadioOnStateListener.this.mPhone.getPhoneId() == i && z && TelephonyManager.getDefault().getPhoneCount() <= 1) {
                    MtkTelephonyConnectionServiceUtil.getInstance().enterEmergencyMode(RadioOnStateListener.this.mPhone, 1);
                    return;
                }
                return;
            }
            Log.d(this, "notifyRadioPowerChange, return since mPhone is null", new Object[0]);
        }
    }

    public void waitForRadioOn(Phone phone, Callback callback) {
        Log.i(this, "waitForRadioOn: Phone " + phone.getPhoneId(), new Object[0]);
        if (this.mPhone != null) {
            return;
        }
        this.mTm = TelephonyManager.getDefault();
        this.mPhone = phone;
        this.mRadioPowerIf = new RadioPowerInterface();
        RadioManager.registerForRadioPowerChange("EmergencyCallHelper", this.mRadioPowerIf);
        SomeArgs someArgsObtain = SomeArgs.obtain();
        someArgsObtain.arg1 = phone;
        someArgsObtain.arg2 = callback;
        someArgsObtain.arg3 = Boolean.valueOf(this.mWaitForInService);
        this.mHandler.obtainMessage(1, someArgsObtain).sendToTarget();
        if (this.mWaitForRadioOffFirst) {
            this.mPhone.registerForRadioOffOrNotAvailable(this.mHandler, 4, (Object) null);
        }
    }

    private void startSequenceInternal(Phone phone, Callback callback) {
        Log.d(this, "startSequenceInternal: Phone " + phone.getPhoneId(), new Object[0]);
        cleanup();
        this.mPhone = phone;
        this.mCallback = callback;
        registerForServiceStateChanged();
        startRetryTimer();
    }

    private void onServiceStateChanged(ServiceState serviceState) {
        Log.i(this, "onServiceStateChanged(), new state = %s, Phone = %s", serviceState, Integer.valueOf(this.mPhone.getPhoneId()));
        Log.i(this, "onServiceStateChanged(), phoneId=" + this.mPhone.getPhoneId() + ", isEmergencyOnly=" + serviceState.isEmergencyOnly() + ", phoneType=" + this.mPhone.getPhoneType() + ", hasCard=" + this.mTm.hasIccCard(this.mPhone.getPhoneId()) + ", mWaitForRadioOffFirst= " + this.mWaitForRadioOffFirst, new Object[0]);
        if (this.mWaitForRadioOffFirst) {
            Log.i(this, "onServiceStateChanged: need to wait for radio off first.", new Object[0]);
            return;
        }
        if (isOkToCall(serviceState.getState()) || ((!this.mWaitForInService || !this.mTm.hasIccCard(this.mPhone.getPhoneId())) && serviceState.isEmergencyOnly())) {
            Log.i(this, "onServiceStateChanged: ok to call! (mWaitForInService=" + this.mWaitForInService + ")", new Object[0]);
            onComplete(true);
            cleanup();
            return;
        }
        Log.i(this, "onServiceStateChanged: not ready to call yet, keep waiting.", new Object[0]);
    }

    private boolean isOkToCall(int i) {
        return MtkTelephonyManagerEx.getDefault().useVzwLogic() ? this.mPhone.getState() == PhoneConstants.State.OFFHOOK || i != 3 || MtkTelephonyManagerEx.getDefault().isWifiCallingEnabled(this.mPhone.getSubId()) : this.mPhone.getState() == PhoneConstants.State.OFFHOOK || i == 0 || (!(this.mWaitForInService && this.mTm.hasIccCard(this.mPhone.getPhoneId())) && i == 2) || ((this.mNumRetriesSoFar == MAX_NUM_RETRIES && i == 1) || ((isAllCdmaCard() && i == 1 && this.mPhone.getPhoneId() != RadioCapabilitySwitchUtil.getMainCapabilityPhoneId() && hasInServicePhone()) || MtkTelephonyManagerEx.getDefault().isWifiCallingEnabled(this.mPhone.getSubId())));
    }

    private boolean isAllCdmaCard() {
        MtkTelephonyManagerEx mtkTelephonyManagerEx = MtkTelephonyManagerEx.getDefault();
        for (int i = 0; i < this.mTm.getPhoneCount(); i++) {
            int iccAppFamily = mtkTelephonyManagerEx.getIccAppFamily(i);
            if (iccAppFamily == 0 || iccAppFamily == 1) {
                return false;
            }
        }
        return true;
    }

    private boolean hasInServicePhone() {
        for (int i = 0; i < this.mTm.getPhoneCount(); i++) {
            if (PhoneFactory.getPhone(i).getServiceState().getState() == 0) {
                return true;
            }
        }
        return false;
    }

    private void onRetryTimeout() {
        int state = this.mPhone.getServiceState().getState();
        Log.i(this, "onRetryTimeout():  phone state = %s, service state = %d, retries = %d.", this.mPhone.getState(), Integer.valueOf(state), Integer.valueOf(this.mNumRetriesSoFar));
        Log.i(this, "onRetryTimeout(), phoneId=" + this.mPhone.getPhoneId() + ", emergencyOnly=" + this.mPhone.getServiceState().isEmergencyOnly() + ", phonetype=" + this.mPhone.getPhoneType() + ", hasCard=" + this.mTm.hasIccCard(this.mPhone.getPhoneId()) + ", mWaitForRadioOffFirst=" + this.mWaitForRadioOffFirst, new Object[0]);
        if (this.mWaitForRadioOffFirst) {
            Log.i(this, "onServiceStateChanged: need to wait for radio off first.", new Object[0]);
            startRetryTimer();
            return;
        }
        if (isOkToCall(state) || ((!this.mWaitForInService || !this.mTm.hasIccCard(this.mPhone.getPhoneId())) && this.mPhone.getServiceState().isEmergencyOnly())) {
            Log.i(this, "onRetryTimeout: Radio is on. Cleaning up. (mWaitForInService=" + this.mWaitForInService + ")", new Object[0]);
            onComplete(true);
            cleanup();
            return;
        }
        this.mNumRetriesSoFar++;
        Log.i(this, "mNumRetriesSoFar is now " + this.mNumRetriesSoFar, new Object[0]);
        if (this.mNumRetriesSoFar > MAX_NUM_RETRIES) {
            Log.w(this, "Hit MAX_NUM_RETRIES; giving up.", new Object[0]);
            cleanup();
            return;
        }
        Log.i(this, "Trying (again) to turn on the radio.", new Object[0]);
        if (RadioManager.isMSimModeSupport()) {
            Log.i(this, "isMSimModeSupport true, use RadioManager forceSetRadioPower", new Object[0]);
            RadioManager.getInstance().forceSetRadioPower(true, this.mPhone.getPhoneId());
        } else {
            Log.i(this, "isMSimModeSupport false, use default setRadioPower", new Object[0]);
            this.mPhone.setRadioPower(true);
        }
        startRetryTimer();
    }

    public void cleanup() {
        String str;
        StringBuilder sb = new StringBuilder();
        sb.append("cleanup(), ");
        if (this.mPhone != null) {
            str = "phoneId=" + this.mPhone.getPhoneId();
        } else {
            str = "(mPhone null)";
        }
        sb.append(str);
        Log.d(this, sb.toString(), new Object[0]);
        onComplete(false);
        unregisterForServiceStateChanged();
        cancelRetryTimer();
        this.mHandler.removeMessages(1);
        this.mPhone = null;
        this.mNumRetriesSoFar = 0;
        this.mWaitForInService = false;
    }

    private void startRetryTimer() {
        cancelRetryTimer();
        this.mHandler.sendEmptyMessageDelayed(3, TIME_BETWEEN_RETRIES_MILLIS);
    }

    private void cancelRetryTimer() {
        this.mHandler.removeMessages(3);
    }

    private void registerForServiceStateChanged() {
        unregisterForServiceStateChanged();
        this.mPhone.registerForServiceStateChanged(this.mHandler, 2, (Object) null);
    }

    private void unregisterForServiceStateChanged() {
        if (this.mPhone != null) {
            this.mPhone.unregisterForServiceStateChanged(this.mHandler);
        }
        this.mHandler.removeMessages(2);
    }

    private void onComplete(boolean z) {
        if (this.mCallback != null) {
            Callback callback = this.mCallback;
            this.mCallback = null;
            callback.onComplete(this, z);
            RadioManager.unregisterForRadioPowerChange(this.mRadioPowerIf);
        }
    }

    @VisibleForTesting
    public Handler getHandler() {
        return this.mHandler;
    }

    @VisibleForTesting
    public void setMaxNumRetries(int i) {
        MAX_NUM_RETRIES = i;
    }

    @VisibleForTesting
    public void setTimeBetweenRetriesMillis(long j) {
        TIME_BETWEEN_RETRIES_MILLIS = j;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !getClass().equals(obj.getClass())) {
            return false;
        }
        RadioOnStateListener radioOnStateListener = (RadioOnStateListener) obj;
        if (this.mNumRetriesSoFar != radioOnStateListener.mNumRetriesSoFar) {
            return false;
        }
        if (this.mCallback == null ? radioOnStateListener.mCallback != null : !this.mCallback.equals(radioOnStateListener.mCallback)) {
            return false;
        }
        if (this.mPhone != null) {
            return this.mPhone.equals(radioOnStateListener.mPhone);
        }
        if (radioOnStateListener.mPhone == null) {
            return true;
        }
        return false;
    }

    public void setWaitForInService(boolean z) {
        this.mWaitForInService = z;
    }

    void setWaitForRadioOffFirst(boolean z) {
        this.mWaitForRadioOffFirst = z;
    }
}
