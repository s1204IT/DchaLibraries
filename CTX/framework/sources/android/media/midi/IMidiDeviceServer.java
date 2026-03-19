package android.media.midi;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import java.io.FileDescriptor;

public interface IMidiDeviceServer extends IInterface {
    void closeDevice() throws RemoteException;

    void closePort(IBinder iBinder) throws RemoteException;

    int connectPorts(IBinder iBinder, FileDescriptor fileDescriptor, int i) throws RemoteException;

    MidiDeviceInfo getDeviceInfo() throws RemoteException;

    FileDescriptor openInputPort(IBinder iBinder, int i) throws RemoteException;

    FileDescriptor openOutputPort(IBinder iBinder, int i) throws RemoteException;

    void setDeviceInfo(MidiDeviceInfo midiDeviceInfo) throws RemoteException;

    public static abstract class Stub extends Binder implements IMidiDeviceServer {
        private static final String DESCRIPTOR = "android.media.midi.IMidiDeviceServer";
        static final int TRANSACTION_closeDevice = 4;
        static final int TRANSACTION_closePort = 3;
        static final int TRANSACTION_connectPorts = 5;
        static final int TRANSACTION_getDeviceInfo = 6;
        static final int TRANSACTION_openInputPort = 1;
        static final int TRANSACTION_openOutputPort = 2;
        static final int TRANSACTION_setDeviceInfo = 7;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IMidiDeviceServer asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IMidiDeviceServer)) {
                return (IMidiDeviceServer) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            MidiDeviceInfo midiDeviceInfoCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    FileDescriptor fileDescriptorOpenInputPort = openInputPort(parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeRawFileDescriptor(fileDescriptorOpenInputPort);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    FileDescriptor fileDescriptorOpenOutputPort = openOutputPort(parcel.readStrongBinder(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeRawFileDescriptor(fileDescriptorOpenOutputPort);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    closePort(parcel.readStrongBinder());
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    closeDevice();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iConnectPorts = connectPorts(parcel.readStrongBinder(), parcel.readRawFileDescriptor(), parcel.readInt());
                    parcel2.writeNoException();
                    parcel2.writeInt(iConnectPorts);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    MidiDeviceInfo deviceInfo = getDeviceInfo();
                    parcel2.writeNoException();
                    if (deviceInfo != null) {
                        parcel2.writeInt(1);
                        deviceInfo.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        midiDeviceInfoCreateFromParcel = MidiDeviceInfo.CREATOR.createFromParcel(parcel);
                    } else {
                        midiDeviceInfoCreateFromParcel = null;
                    }
                    setDeviceInfo(midiDeviceInfoCreateFromParcel);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IMidiDeviceServer {
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
            public FileDescriptor openInputPort(IBinder iBinder, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readRawFileDescriptor();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public FileDescriptor openOutputPort(IBinder iBinder, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readRawFileDescriptor();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void closePort(IBinder iBinder) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void closeDevice() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public int connectPorts(IBinder iBinder, FileDescriptor fileDescriptor, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeRawFileDescriptor(fileDescriptor);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public MidiDeviceInfo getDeviceInfo() throws RemoteException {
                MidiDeviceInfo midiDeviceInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        midiDeviceInfoCreateFromParcel = MidiDeviceInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        midiDeviceInfoCreateFromParcel = null;
                    }
                    return midiDeviceInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDeviceInfo(MidiDeviceInfo midiDeviceInfo) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (midiDeviceInfo != null) {
                        parcelObtain.writeInt(1);
                        midiDeviceInfo.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
