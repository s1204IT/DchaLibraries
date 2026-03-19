package com.mediatek.ims.internal;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcel;
import android.os.RemoteException;
import com.mediatek.ims.internal.IMtkImsUtListener;

public interface IMtkImsUt extends IInterface {
    String getUtIMPUFromNetwork() throws RemoteException;

    String getXcapConflictErrorMessage() throws RemoteException;

    boolean isSupportCFT() throws RemoteException;

    void processECT(Message message, Messenger messenger) throws RemoteException;

    int queryCallForwardInTimeSlot(int i) throws RemoteException;

    void setListener(IMtkImsUtListener iMtkImsUtListener) throws RemoteException;

    void setupXcapUserAgentString(String str) throws RemoteException;

    int updateCallBarringForServiceClass(String str, int i, int i2, String[] strArr, int i3) throws RemoteException;

    int updateCallForwardInTimeSlot(int i, int i2, String str, int i3, long[] jArr) throws RemoteException;

    public static abstract class Stub extends Binder implements IMtkImsUt {
        private static final String DESCRIPTOR = "com.mediatek.ims.internal.IMtkImsUt";
        static final int TRANSACTION_getUtIMPUFromNetwork = 2;
        static final int TRANSACTION_getXcapConflictErrorMessage = 9;
        static final int TRANSACTION_isSupportCFT = 7;
        static final int TRANSACTION_processECT = 6;
        static final int TRANSACTION_queryCallForwardInTimeSlot = 3;
        static final int TRANSACTION_setListener = 1;
        static final int TRANSACTION_setupXcapUserAgentString = 8;
        static final int TRANSACTION_updateCallBarringForServiceClass = 5;
        static final int TRANSACTION_updateCallForwardInTimeSlot = 4;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMtkImsUt asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMtkImsUt)) {
                return (IMtkImsUt) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            Message message;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    setListener(IMtkImsUtListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    String utIMPUFromNetwork = getUtIMPUFromNetwork();
                    parcel2.writeNoException();
                    parcel2.writeString(utIMPUFromNetwork);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iQueryCallForwardInTimeSlot = queryCallForwardInTimeSlot(parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iQueryCallForwardInTimeSlot);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iUpdateCallForwardInTimeSlot = updateCallForwardInTimeSlot(parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt(), parcel.createLongArray());
                    parcel2.writeNoException();
                    parcel2.writeInt(iUpdateCallForwardInTimeSlot);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iUpdateCallBarringForServiceClass = updateCallBarringForServiceClass(parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.createStringArray(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iUpdateCallBarringForServiceClass);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        message = (Message) Message.CREATOR.createFromParcel(parcel);
                    } else {
                        message = null;
                    }
                    processECT(message, parcel.readInt() != 0 ? (Messenger) Messenger.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsSupportCFT = isSupportCFT();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsSupportCFT ? 1 : 0);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    setupXcapUserAgentString(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    String xcapConflictErrorMessage = getXcapConflictErrorMessage();
                    parcel2.writeNoException();
                    parcel2.writeString(xcapConflictErrorMessage);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMtkImsUt {
            private IBinder mRemote;

            Proxy(IBinder iBinder) {
                this.mRemote = iBinder;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void setListener(IMtkImsUtListener iMtkImsUtListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iMtkImsUtListener != null ? iMtkImsUtListener.asBinder() : null);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getUtIMPUFromNetwork() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int queryCallForwardInTimeSlot(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int updateCallForwardInTimeSlot(int i, int i2, String str, int i3, long[] jArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeLongArray(jArr);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int updateCallBarringForServiceClass(String str, int i, int i2, String[] strArr, int i3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeStringArray(strArr);
                    parcelObtain.writeInt(i3);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void processECT(Message message, Messenger messenger) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (message != null) {
                        parcelObtain.writeInt(1);
                        message.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (messenger != null) {
                        parcelObtain.writeInt(1);
                        messenger.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isSupportCFT() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setupXcapUserAgentString(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getXcapConflictErrorMessage() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
