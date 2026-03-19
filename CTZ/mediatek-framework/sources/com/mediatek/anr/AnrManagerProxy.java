package com.mediatek.anr;

import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

class AnrManagerProxy implements IAnrManager {
    private IBinder mRemote;

    public AnrManagerProxy(IBinder iBinder) {
        this.mRemote = iBinder;
    }

    @Override
    public IBinder asBinder() {
        return this.mRemote;
    }

    @Override
    public void informMessageDump(String str, int i) throws RemoteException {
        Parcel parcelObtain = Parcel.obtain();
        parcelObtain.writeInterfaceToken(IAnrManager.descriptor);
        parcelObtain.writeString(str);
        parcelObtain.writeInt(i);
        this.mRemote.transact(2, parcelObtain, null, 1);
        parcelObtain.recycle();
    }
}
