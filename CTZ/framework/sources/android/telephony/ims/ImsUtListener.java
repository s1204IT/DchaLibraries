package android.telephony.ims;

import android.annotation.SystemApi;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;
import com.android.ims.internal.IImsUtListener;

@SystemApi
public class ImsUtListener {
    private static final String LOG_TAG = "ImsUtListener";
    private IImsUtListener mServiceInterface;

    public void onUtConfigurationUpdated(int i) {
        try {
            this.mServiceInterface.utConfigurationUpdated(null, i);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "utConfigurationUpdated: remote exception");
        }
    }

    public void onUtConfigurationUpdateFailed(int i, ImsReasonInfo imsReasonInfo) {
        try {
            this.mServiceInterface.utConfigurationUpdateFailed(null, i, imsReasonInfo);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "utConfigurationUpdateFailed: remote exception");
        }
    }

    public void onUtConfigurationQueried(int i, Bundle bundle) {
        try {
            this.mServiceInterface.utConfigurationQueried(null, i, bundle);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "utConfigurationQueried: remote exception");
        }
    }

    public void onUtConfigurationQueryFailed(int i, ImsReasonInfo imsReasonInfo) {
        try {
            this.mServiceInterface.utConfigurationQueryFailed(null, i, imsReasonInfo);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "utConfigurationQueryFailed: remote exception");
        }
    }

    public void onUtConfigurationCallBarringQueried(int i, ImsSsInfo[] imsSsInfoArr) {
        try {
            this.mServiceInterface.utConfigurationCallBarringQueried(null, i, imsSsInfoArr);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "utConfigurationCallBarringQueried: remote exception");
        }
    }

    public void onUtConfigurationCallForwardQueried(int i, ImsCallForwardInfo[] imsCallForwardInfoArr) {
        try {
            this.mServiceInterface.utConfigurationCallForwardQueried(null, i, imsCallForwardInfoArr);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "utConfigurationCallForwardQueried: remote exception");
        }
    }

    public void onUtConfigurationCallWaitingQueried(int i, ImsSsInfo[] imsSsInfoArr) {
        try {
            this.mServiceInterface.utConfigurationCallWaitingQueried(null, i, imsSsInfoArr);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "utConfigurationCallWaitingQueried: remote exception");
        }
    }

    public void onSupplementaryServiceIndication(ImsSsData imsSsData) {
        try {
            this.mServiceInterface.onSupplementaryServiceIndication(imsSsData);
        } catch (RemoteException e) {
            Log.w(LOG_TAG, "onSupplementaryServiceIndication: remote exception");
        }
    }

    public ImsUtListener(IImsUtListener iImsUtListener) {
        this.mServiceInterface = iImsUtListener;
    }
}
