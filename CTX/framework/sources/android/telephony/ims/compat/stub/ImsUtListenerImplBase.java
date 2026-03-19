package android.telephony.ims.compat.stub;

import android.os.Bundle;
import android.os.RemoteException;
import android.telephony.ims.ImsCallForwardInfo;
import android.telephony.ims.ImsReasonInfo;
import android.telephony.ims.ImsSsData;
import android.telephony.ims.ImsSsInfo;
import com.android.ims.internal.IImsUt;
import com.android.ims.internal.IImsUtListener;

public class ImsUtListenerImplBase extends IImsUtListener.Stub {
    @Override
    public void utConfigurationUpdated(IImsUt iImsUt, int i) throws RemoteException {
    }

    @Override
    public void utConfigurationUpdateFailed(IImsUt iImsUt, int i, ImsReasonInfo imsReasonInfo) throws RemoteException {
    }

    @Override
    public void utConfigurationQueried(IImsUt iImsUt, int i, Bundle bundle) throws RemoteException {
    }

    @Override
    public void utConfigurationQueryFailed(IImsUt iImsUt, int i, ImsReasonInfo imsReasonInfo) throws RemoteException {
    }

    @Override
    public void utConfigurationCallBarringQueried(IImsUt iImsUt, int i, ImsSsInfo[] imsSsInfoArr) throws RemoteException {
    }

    @Override
    public void utConfigurationCallForwardQueried(IImsUt iImsUt, int i, ImsCallForwardInfo[] imsCallForwardInfoArr) throws RemoteException {
    }

    @Override
    public void utConfigurationCallWaitingQueried(IImsUt iImsUt, int i, ImsSsInfo[] imsSsInfoArr) throws RemoteException {
    }

    @Override
    public void onSupplementaryServiceIndication(ImsSsData imsSsData) {
    }
}
