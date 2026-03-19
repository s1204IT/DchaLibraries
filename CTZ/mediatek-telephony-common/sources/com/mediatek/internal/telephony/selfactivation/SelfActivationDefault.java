package com.mediatek.internal.telephony.selfactivation;

import android.content.Context;
import android.os.Bundle;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;

public class SelfActivationDefault implements ISelfActivation {
    private static final String TAG = "SelfActivationDefault";
    protected int mPhoneId;
    protected Context mContext = null;
    protected CommandsInterface mCi = null;

    public SelfActivationDefault(int i) {
        this.mPhoneId = -1;
        Rlog.d(TAG, "init");
        this.mPhoneId = i;
    }

    @Override
    public int selfActivationAction(int i, Bundle bundle) {
        return -1;
    }

    @Override
    public int getSelfActivateState() {
        return 0;
    }

    @Override
    public int getPCO520State() {
        return 0;
    }

    @Override
    public ISelfActivation setContext(Context context) {
        this.mContext = context;
        return this;
    }

    @Override
    public ISelfActivation setCommandsInterface(CommandsInterface commandsInterface) {
        this.mCi = commandsInterface;
        return this;
    }

    @Override
    public ISelfActivation buildParams() {
        return this;
    }
}
