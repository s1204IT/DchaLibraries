package com.android.internal.telephony.ims;

import android.app.PendingIntent;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.telephony.ims.ImsCallProfile;
import com.android.ims.internal.IImsCallSession;
import com.android.ims.internal.IImsCallSessionListener;
import com.android.ims.internal.IImsConfig;
import com.android.ims.internal.IImsEcbm;
import com.android.ims.internal.IImsMultiEndpoint;
import com.android.ims.internal.IImsRegistrationListener;
import com.android.ims.internal.IImsService;
import com.android.ims.internal.IImsUt;

public class ImsServiceInterfaceAdapter extends MmTelInterfaceAdapter {
    private static final int SERVICE_ID = 1;

    public ImsServiceInterfaceAdapter(int i, IBinder iBinder) {
        super(i, iBinder);
    }

    @Override
    public int startSession(PendingIntent pendingIntent, IImsRegistrationListener iImsRegistrationListener) throws RemoteException {
        return getInterface().open(this.mSlotId, 1, pendingIntent, iImsRegistrationListener);
    }

    @Override
    public void endSession(int i) throws RemoteException {
        getInterface().close(i);
    }

    @Override
    public boolean isConnected(int i, int i2) throws RemoteException {
        return getInterface().isConnected(1, i, i2);
    }

    @Override
    public boolean isOpened() throws RemoteException {
        return getInterface().isOpened(1);
    }

    @Override
    public int getFeatureState() throws RemoteException {
        return 2;
    }

    @Override
    public void addRegistrationListener(IImsRegistrationListener iImsRegistrationListener) throws RemoteException {
        getInterface().addRegistrationListener(this.mSlotId, 1, iImsRegistrationListener);
    }

    @Override
    public void removeRegistrationListener(IImsRegistrationListener iImsRegistrationListener) throws RemoteException {
    }

    @Override
    public ImsCallProfile createCallProfile(int i, int i2, int i3) throws RemoteException {
        return getInterface().createCallProfile(i, i2, i3);
    }

    @Override
    public IImsCallSession createCallSession(int i, ImsCallProfile imsCallProfile) throws RemoteException {
        return getInterface().createCallSession(i, imsCallProfile, (IImsCallSessionListener) null);
    }

    @Override
    public IImsCallSession getPendingCallSession(int i, String str) throws RemoteException {
        return getInterface().getPendingCallSession(i, str);
    }

    @Override
    public IImsUt getUtInterface() throws RemoteException {
        return getInterface().getUtInterface(1);
    }

    @Override
    public IImsConfig getConfigInterface() throws RemoteException {
        return getInterface().getConfigInterface(this.mSlotId);
    }

    @Override
    public void turnOnIms() throws RemoteException {
        getInterface().turnOnIms(this.mSlotId);
    }

    @Override
    public void turnOffIms() throws RemoteException {
        getInterface().turnOffIms(this.mSlotId);
    }

    @Override
    public IImsEcbm getEcbmInterface() throws RemoteException {
        return getInterface().getEcbmInterface(1);
    }

    @Override
    public void setUiTTYMode(int i, Message message) throws RemoteException {
        getInterface().setUiTTYMode(1, i, message);
    }

    @Override
    public IImsMultiEndpoint getMultiEndpointInterface() throws RemoteException {
        return getInterface().getMultiEndpointInterface(1);
    }

    protected IImsService getInterface() throws RemoteException {
        IImsService iImsServiceAsInterface = IImsService.Stub.asInterface(this.mBinder);
        if (iImsServiceAsInterface == null) {
            throw new RemoteException("Binder not Available");
        }
        return iImsServiceAsInterface;
    }
}
