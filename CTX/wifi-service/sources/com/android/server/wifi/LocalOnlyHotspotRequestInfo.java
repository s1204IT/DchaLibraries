package com.android.server.wifi;

import android.net.wifi.WifiConfiguration;
import android.os.Binder;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import com.android.internal.util.Preconditions;

public class LocalOnlyHotspotRequestInfo implements IBinder.DeathRecipient {
    static final int HOTSPOT_NO_ERROR = -1;
    private final IBinder mBinder;
    private final RequestingApplicationDeathCallback mCallback;
    private final Messenger mMessenger;
    private final int mPid = Binder.getCallingPid();

    public interface RequestingApplicationDeathCallback {
        void onLocalOnlyHotspotRequestorDeath(LocalOnlyHotspotRequestInfo localOnlyHotspotRequestInfo);
    }

    LocalOnlyHotspotRequestInfo(IBinder iBinder, Messenger messenger, RequestingApplicationDeathCallback requestingApplicationDeathCallback) {
        this.mBinder = (IBinder) Preconditions.checkNotNull(iBinder);
        this.mMessenger = (Messenger) Preconditions.checkNotNull(messenger);
        this.mCallback = (RequestingApplicationDeathCallback) Preconditions.checkNotNull(requestingApplicationDeathCallback);
        try {
            this.mBinder.linkToDeath(this, 0);
        } catch (RemoteException e) {
            binderDied();
        }
    }

    public void unlinkDeathRecipient() {
        this.mBinder.unlinkToDeath(this, 0);
    }

    @Override
    public void binderDied() {
        this.mCallback.onLocalOnlyHotspotRequestorDeath(this);
    }

    public void sendHotspotFailedMessage(int i) throws RemoteException {
        Message messageObtain = Message.obtain();
        messageObtain.what = 2;
        messageObtain.arg1 = i;
        this.mMessenger.send(messageObtain);
    }

    public void sendHotspotStartedMessage(WifiConfiguration wifiConfiguration) throws RemoteException {
        Message messageObtain = Message.obtain();
        messageObtain.what = 0;
        messageObtain.obj = wifiConfiguration;
        this.mMessenger.send(messageObtain);
    }

    public void sendHotspotStoppedMessage() throws RemoteException {
        Message messageObtain = Message.obtain();
        messageObtain.what = 1;
        this.mMessenger.send(messageObtain);
    }

    public int getPid() {
        return this.mPid;
    }
}
