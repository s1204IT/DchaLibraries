package com.mediatek.internal.telephony;

import android.content.Context;

public class DefaultTelephonyExt implements IDefaultTelephonyExt {
    private static final String TAG = "DefaultTelephonyExt";
    protected Context mContext;

    public DefaultTelephonyExt(Context context) {
        this.mContext = context;
    }

    @Override
    public void init(Context context) {
        this.mContext = context;
    }

    @Override
    public boolean isSetLanguageBySIM() {
        return false;
    }

    @Override
    public boolean isRatMenuControlledBySIM() {
        return false;
    }

    @Override
    public String getOperatorNumericFromImpi(String str, int i) {
        return str;
    }
}
