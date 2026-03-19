package com.mediatek.services.telephony;

import android.os.SystemProperties;
import android.telecom.ConnectionRequest;
import android.telecom.PhoneAccountHandle;
import android.telephony.TelephonyManager;
import com.android.phone.PhoneUtils;
import com.android.phone.settings.SettingsConstants;
import com.android.services.telephony.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EmergencyRetryHandler {
    private static final int MAX_NUM_RETRIES;
    private static final boolean MTK_CT_VOLTE_SUPPORT = SettingsConstants.DUA_VAL_ON.equals(SystemProperties.get("persist.vendor.mtk_ct_volte_support", SettingsConstants.DUAL_VAL_OFF));
    private Iterator<PhoneAccountHandle> mAttemptRecordIterator;
    private int mNumRetriesSoFar;
    private ConnectionRequest mRequest;
    private String mCallId = null;
    private List<PhoneAccountHandle> mAttemptRecords = new ArrayList();

    static {
        int phoneCount = 1;
        if (TelephonyManager.getDefault().getPhoneCount() > 1) {
            phoneCount = TelephonyManager.getDefault().getPhoneCount() - 1;
        } else if (!MTK_CT_VOLTE_SUPPORT) {
            phoneCount = 0;
        }
        MAX_NUM_RETRIES = phoneCount;
    }

    public EmergencyRetryHandler(ConnectionRequest connectionRequest, int i) {
        this.mRequest = null;
        this.mNumRetriesSoFar = 0;
        this.mRequest = connectionRequest;
        this.mNumRetriesSoFar = 0;
        int i2 = 0;
        while (i2 < MAX_NUM_RETRIES) {
            int i3 = i2;
            for (int i4 = 0; i4 < TelephonyManager.getDefault().getPhoneCount(); i4++) {
                if (i != i4) {
                    PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle = PhoneUtils.makePstnPhoneAccountHandle(Integer.toString(i4));
                    this.mAttemptRecords.add(phoneAccountHandleMakePstnPhoneAccountHandle);
                    i3++;
                    log("Add #" + i3 + " to ECC retry list, " + phoneAccountHandleMakePstnPhoneAccountHandle);
                }
            }
            PhoneAccountHandle phoneAccountHandleMakePstnPhoneAccountHandle2 = PhoneUtils.makePstnPhoneAccountHandle(Integer.toString(i));
            this.mAttemptRecords.add(phoneAccountHandleMakePstnPhoneAccountHandle2);
            int i5 = i3 + 1;
            log("Add #" + i5 + " to ECC Retry list, " + phoneAccountHandleMakePstnPhoneAccountHandle2);
            i2 = i5;
        }
        this.mAttemptRecordIterator = this.mAttemptRecords.iterator();
    }

    public void setCallId(String str) {
        log("setCallId, id=" + str);
        this.mCallId = str;
    }

    public String getCallId() {
        log("getCallId, id=" + this.mCallId);
        return this.mCallId;
    }

    public boolean isTimeout() {
        boolean z = this.mNumRetriesSoFar >= MAX_NUM_RETRIES;
        log("isTimeout, timeout=" + z + ", mNumRetriesSoFar=" + this.mNumRetriesSoFar);
        return z;
    }

    public ConnectionRequest getRequest() {
        log("getRequest, request=" + this.mRequest);
        return this.mRequest;
    }

    public PhoneAccountHandle getNextAccountHandle() {
        if (this.mAttemptRecordIterator.hasNext()) {
            this.mNumRetriesSoFar++;
            log("getNextAccountHandle, next account handle exists");
            return this.mAttemptRecordIterator.next();
        }
        log("getNextAccountHandle, next account handle is null");
        return null;
    }

    private void log(String str) {
        Log.d("ECCRetryHandler", str, new Object[0]);
    }
}
