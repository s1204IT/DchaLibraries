package com.android.internal.telephony.dataconnection;

import android.telephony.Rlog;
import java.util.ArrayList;
import java.util.List;

public class TransportManager {
    private static final String TAG = TransportManager.class.getSimpleName();
    private List<Integer> mAvailableTransports = new ArrayList();

    public TransportManager() {
        this.mAvailableTransports.add(1);
    }

    public List<Integer> getAvailableTransports() {
        return new ArrayList(this.mAvailableTransports);
    }

    private void log(String str) {
        Rlog.d(TAG, str);
    }

    private void loge(String str) {
        Rlog.e(TAG, str);
    }
}
