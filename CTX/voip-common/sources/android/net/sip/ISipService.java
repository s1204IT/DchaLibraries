package android.net.sip;

import android.app.PendingIntent;
import android.net.sip.ISipSession;
import android.net.sip.ISipSessionListener;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface ISipService extends IInterface {
    void close(String str, String str2) throws RemoteException;

    ISipSession createSession(SipProfile sipProfile, ISipSessionListener iSipSessionListener, String str) throws RemoteException;

    SipProfile[] getListOfProfiles(String str) throws RemoteException;

    ISipSession getPendingSession(String str, String str2) throws RemoteException;

    boolean isOpened(String str, String str2) throws RemoteException;

    boolean isRegistered(String str, String str2) throws RemoteException;

    void open(SipProfile sipProfile, String str) throws RemoteException;

    void open3(SipProfile sipProfile, PendingIntent pendingIntent, ISipSessionListener iSipSessionListener, String str) throws RemoteException;

    void setRegistrationListener(String str, ISipSessionListener iSipSessionListener, String str2) throws RemoteException;

    public static abstract class Stub extends Binder implements ISipService {
        private static final String DESCRIPTOR = "android.net.sip.ISipService";
        static final int TRANSACTION_close = 3;
        static final int TRANSACTION_createSession = 7;
        static final int TRANSACTION_getListOfProfiles = 9;
        static final int TRANSACTION_getPendingSession = 8;
        static final int TRANSACTION_isOpened = 4;
        static final int TRANSACTION_isRegistered = 5;
        static final int TRANSACTION_open = 1;
        static final int TRANSACTION_open3 = 2;
        static final int TRANSACTION_setRegistrationListener = 6;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ISipService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof ISipService)) {
                return (ISipService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            SipProfile sipProfileCreateFromParcel;
            SipProfile sipProfileCreateFromParcel2;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    open(parcel.readInt() != 0 ? SipProfile.CREATOR.createFromParcel(parcel) : null, parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        sipProfileCreateFromParcel = SipProfile.CREATOR.createFromParcel(parcel);
                    } else {
                        sipProfileCreateFromParcel = null;
                    }
                    open3(sipProfileCreateFromParcel, parcel.readInt() != 0 ? (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel) : null, ISipSessionListener.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    close(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsOpened = isOpened(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsOpened ? 1 : 0);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsRegistered = isRegistered(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsRegistered ? 1 : 0);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    setRegistrationListener(parcel.readString(), ISipSessionListener.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        sipProfileCreateFromParcel2 = SipProfile.CREATOR.createFromParcel(parcel);
                    } else {
                        sipProfileCreateFromParcel2 = null;
                    }
                    ISipSession iSipSessionCreateSession = createSession(sipProfileCreateFromParcel2, ISipSessionListener.Stub.asInterface(parcel.readStrongBinder()), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(iSipSessionCreateSession != null ? iSipSessionCreateSession.asBinder() : null);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    ISipSession pendingSession = getPendingSession(parcel.readString(), parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeStrongBinder(pendingSession != null ? pendingSession.asBinder() : null);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    SipProfile[] listOfProfiles = getListOfProfiles(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(listOfProfiles, 1);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements ISipService {
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
            public void open(SipProfile sipProfile, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sipProfile != null) {
                        parcelObtain.writeInt(1);
                        sipProfile.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void open3(SipProfile sipProfile, PendingIntent pendingIntent, ISipSessionListener iSipSessionListener, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sipProfile != null) {
                        parcelObtain.writeInt(1);
                        sipProfile.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (pendingIntent != null) {
                        parcelObtain.writeInt(1);
                        pendingIntent.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iSipSessionListener != null ? iSipSessionListener.asBinder() : null);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void close(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isOpened(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isRegistered(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setRegistrationListener(String str, ISipSessionListener iSipSessionListener, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iSipSessionListener != null ? iSipSessionListener.asBinder() : null);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ISipSession createSession(SipProfile sipProfile, ISipSessionListener iSipSessionListener, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (sipProfile != null) {
                        parcelObtain.writeInt(1);
                        sipProfile.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iSipSessionListener != null ? iSipSessionListener.asBinder() : null);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return ISipSession.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public ISipSession getPendingSession(String str, String str2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return ISipSession.Stub.asInterface(parcelObtain2.readStrongBinder());
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public SipProfile[] getListOfProfiles(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (SipProfile[]) parcelObtain2.createTypedArray(SipProfile.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
