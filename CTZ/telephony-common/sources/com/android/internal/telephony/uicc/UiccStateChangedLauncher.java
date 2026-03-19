package com.android.internal.telephony.uicc;

import android.R;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.telephony.TelephonyManager;
import android.util.Log;
import com.android.internal.telephony.uicc.IccCardStatus;

public class UiccStateChangedLauncher extends Handler {
    private static final int EVENT_ICC_CHANGED = 1;
    private static final String TAG = UiccStateChangedLauncher.class.getName();
    private static String sDeviceProvisioningPackage = null;
    private Context mContext;
    private boolean[] mIsRestricted = null;
    private UiccController mUiccController;

    public UiccStateChangedLauncher(Context context, UiccController uiccController) {
        sDeviceProvisioningPackage = context.getResources().getString(R.string.activity_resolver_use_once);
        if (sDeviceProvisioningPackage != null && !sDeviceProvisioningPackage.isEmpty()) {
            this.mContext = context;
            this.mUiccController = uiccController;
            this.mUiccController.registerForIccChanged(this, 1, null);
        }
    }

    @Override
    public void handleMessage(Message message) {
        boolean z;
        if (message.what == 1) {
            if (this.mIsRestricted == null) {
                this.mIsRestricted = new boolean[TelephonyManager.getDefault().getPhoneCount()];
                z = true;
            } else {
                z = false;
            }
            boolean z2 = z;
            for (int i = 0; i < this.mIsRestricted.length; i++) {
                UiccCard uiccCardForPhone = this.mUiccController.getUiccCardForPhone(i);
                if ((uiccCardForPhone == null || uiccCardForPhone.getCardState() != IccCardStatus.CardState.CARDSTATE_RESTRICTED) != this.mIsRestricted[i]) {
                    this.mIsRestricted[i] = !this.mIsRestricted[i];
                    z2 = true;
                }
            }
            if (z2) {
                notifyStateChanged();
                return;
            }
            return;
        }
        throw new RuntimeException("unexpected event not handled");
    }

    private void notifyStateChanged() {
        Intent intent = new Intent("android.intent.action.SIM_STATE_CHANGED");
        intent.setPackage(sDeviceProvisioningPackage);
        try {
            this.mContext.sendBroadcast(intent);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }
}
