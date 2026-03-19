package com.mediatek.internal.telephony.ims;

import android.app.PendingIntent;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.aidl.IImsSmsListener;
import android.util.Log;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsUt;
import com.android.internal.telephony.ims.ImsServiceInterfaceAdapter;
import com.mediatek.ims.internal.IMtkImsService;

public class MtkImsServiceInterfaceAdapter extends ImsServiceInterfaceAdapter {
    private static final String LOG_TAG = "MtkImsSrvAdapter";
    private IBinder mBinderMtk;
    private int mServiceId;

    public MtkImsServiceInterfaceAdapter(int i, IBinder iBinder, IBinder iBinder2) {
        super(i, iBinder);
        this.mServiceId = -1;
        this.mBinderMtk = iBinder2;
    }

    public int startSession(PendingIntent pendingIntent, IImsRegistrationListener iImsRegistrationListener) throws RemoteException {
        this.mServiceId = super.startSession(pendingIntent, iImsRegistrationListener);
        return this.mServiceId;
    }

    public void endSession(int i) throws RemoteException {
        super.endSession(i);
        this.mServiceId = -1;
    }

    public boolean isConnected(int i, int i2) throws RemoteException {
        return getInterface().isConnected(this.mServiceId, i, i2);
    }

    public boolean isOpened() throws RemoteException {
        return getInterface().isOpened(this.mServiceId);
    }

    public IImsUt getUtInterface() throws RemoteException {
        return getInterface().getUtInterface(this.mServiceId);
    }

    public IImsEcbm getEcbmInterface() throws RemoteException {
        return getInterface().getEcbmInterface(this.mServiceId);
    }

    public void setUiTTYMode(int i, Message message) throws RemoteException {
        getInterface().setUiTTYMode(this.mServiceId, i, message);
    }

    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        return getInterface().getMultiEndpointInterface(this.mServiceId);
    }

    public void setSmsListener(IImsSmsListener iImsSmsListener) {
        try {
            getMtkInterface().addImsSmsListener(this.mSlotId, iImsSmsListener);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Fail to setSmsListener " + e);
        }
    }

    public void sendSms(int i, int i2, String str, String str2, boolean z, byte[] bArr) {
        try {
            getMtkInterface().sendSms(this.mSlotId, i, i2, str, str2, z, bArr);
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "Fail to send SMS over IMS " + e);
        }
    }

    public void acknowledgeSms(int i, int i2, int i3) {
    }

    public void acknowledgeSmsReport(int i, int i2, int i3) {
    }

    public void onSmsReady() {
    }

    public String getSmsFormat() {
        return "";
    }

    private IMtkImsService getMtkInterface() throws RemoteException {
        IMtkImsService iMtkImsServiceAsInterface = IMtkImsService.Stub.asInterface(this.mBinderMtk);
        if (iMtkImsServiceAsInterface == null) {
            throw new RemoteException("Binder not Available");
        }
        return iMtkImsServiceAsInterface;
    }
}
