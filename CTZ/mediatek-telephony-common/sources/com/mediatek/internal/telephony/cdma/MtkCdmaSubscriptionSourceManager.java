package com.mediatek.internal.telephony.cdma;

import android.content.Context;
import android.telephony.Rlog;
import com.android.internal.telephony.CommandsInterface;
import com.android.internal.telephony.cdma.CdmaSubscriptionSourceManager;

public class MtkCdmaSubscriptionSourceManager extends CdmaSubscriptionSourceManager {
    static final String LOG_TAG = "MtkCdmaSSM";
    private int mActStatus;

    public MtkCdmaSubscriptionSourceManager(Context context, CommandsInterface commandsInterface) {
        super(context, commandsInterface);
        this.mActStatus = 0;
    }

    public int getActStatus() {
        log("getActStatus " + this.mActStatus);
        return this.mActStatus;
    }

    protected void setActStatus(int i) {
        this.mActStatus = i;
    }

    private void log(String str) {
        Rlog.d(LOG_TAG, str);
    }

    private void logw(String str) {
        Rlog.w(LOG_TAG, str);
    }
}
