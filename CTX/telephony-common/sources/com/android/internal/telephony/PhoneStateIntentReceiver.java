package com.android.internal.telephony;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.telephony.Rlog;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import com.android.internal.telephony.PhoneConstants;

@Deprecated
public final class PhoneStateIntentReceiver extends BroadcastReceiver {
    private static final boolean DBG = false;
    private static final String LOG_TAG = "PhoneStatIntentReceiver";
    private static final int NOTIF_PHONE = 1;
    private static final int NOTIF_SERVICE = 2;
    private static final int NOTIF_SIGNAL = 4;
    private int mAsuEventWhat;
    private Context mContext;
    private IntentFilter mFilter;
    PhoneConstants.State mPhoneState;
    private int mPhoneStateEventWhat;
    ServiceState mServiceState;
    private int mServiceStateEventWhat;
    SignalStrength mSignalStrength;
    private Handler mTarget;
    private int mWants;

    public PhoneStateIntentReceiver() {
        this.mPhoneState = PhoneConstants.State.IDLE;
        this.mServiceState = new ServiceState();
        this.mSignalStrength = new SignalStrength();
        this.mFilter = new IntentFilter();
    }

    public PhoneStateIntentReceiver(Context context, Handler handler) {
        this();
        setContext(context);
        setTarget(handler);
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public void setTarget(Handler handler) {
        this.mTarget = handler;
    }

    public PhoneConstants.State getPhoneState() {
        if ((this.mWants & 1) == 0) {
            throw new RuntimeException("client must call notifyPhoneCallState(int)");
        }
        return this.mPhoneState;
    }

    public ServiceState getServiceState() {
        if ((this.mWants & 2) == 0) {
            throw new RuntimeException("client must call notifyServiceState(int)");
        }
        return this.mServiceState;
    }

    public int getSignalStrengthLevelAsu() {
        if ((this.mWants & 4) == 0) {
            throw new RuntimeException("client must call notifySignalStrength(int)");
        }
        return this.mSignalStrength.getAsuLevel();
    }

    public int getSignalStrengthDbm() {
        if ((this.mWants & 4) == 0) {
            throw new RuntimeException("client must call notifySignalStrength(int)");
        }
        return this.mSignalStrength.getDbm();
    }

    public void notifyPhoneCallState(int i) {
        this.mWants |= 1;
        this.mPhoneStateEventWhat = i;
        this.mFilter.addAction("android.intent.action.PHONE_STATE");
    }

    public boolean getNotifyPhoneCallState() {
        return (this.mWants & 1) != 0;
    }

    public void notifyServiceState(int i) {
        this.mWants |= 2;
        this.mServiceStateEventWhat = i;
        this.mFilter.addAction("android.intent.action.SERVICE_STATE");
    }

    public boolean getNotifyServiceState() {
        return (this.mWants & 2) != 0;
    }

    public void notifySignalStrength(int i) {
        this.mWants |= 4;
        this.mAsuEventWhat = i;
        this.mFilter.addAction("android.intent.action.SIG_STR");
    }

    public boolean getNotifySignalStrength() {
        return (this.mWants & 4) != 0;
    }

    public void registerIntent() {
        this.mContext.registerReceiver(this, this.mFilter);
    }

    public void unregisterIntent() {
        this.mContext.unregisterReceiver(this);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        try {
            if ("android.intent.action.SIG_STR".equals(action)) {
                this.mSignalStrength = SignalStrength.newFromBundle(intent.getExtras());
                if (this.mTarget != null && getNotifySignalStrength()) {
                    this.mTarget.sendMessage(Message.obtain(this.mTarget, this.mAsuEventWhat));
                    return;
                }
                return;
            }
            if ("android.intent.action.PHONE_STATE".equals(action)) {
                this.mPhoneState = Enum.valueOf(PhoneConstants.State.class, intent.getStringExtra("state"));
                if (this.mTarget != null && getNotifyPhoneCallState()) {
                    this.mTarget.sendMessage(Message.obtain(this.mTarget, this.mPhoneStateEventWhat));
                }
                return;
            }
            if ("android.intent.action.SERVICE_STATE".equals(action)) {
                this.mServiceState = ServiceState.newFromBundle(intent.getExtras());
                if (this.mTarget != null && getNotifyServiceState()) {
                    this.mTarget.sendMessage(Message.obtain(this.mTarget, this.mServiceStateEventWhat));
                }
            }
        } catch (Exception e) {
            Rlog.e(LOG_TAG, "[PhoneStateIntentRecv] caught " + e);
            e.printStackTrace();
        }
    }
}
