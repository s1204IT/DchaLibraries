package com.mediatek.common.voiceextension;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import com.mediatek.common.voiceextension.IVoiceExtCommandListener;

public interface IVoiceExtCommandManager extends IInterface {
    int createCommandSet(String str) throws RemoteException;

    int deleteCommandSet(String str) throws RemoteException;

    String getCommandSetSelected() throws RemoteException;

    String[] getCommandSets() throws RemoteException;

    String[] getCommands() throws RemoteException;

    int isCommandSetCreated(String str) throws RemoteException;

    void pauseRecognition() throws RemoteException;

    int registerListener(IVoiceExtCommandListener iVoiceExtCommandListener) throws RemoteException;

    void resumeRecognition() throws RemoteException;

    int selectCurrentCommandSet(String str) throws RemoteException;

    void setCommandsFile(ParcelFileDescriptor parcelFileDescriptor, int i, int i2) throws RemoteException;

    void setCommandsStrArray(String[] strArr) throws RemoteException;

    void startRecognition() throws RemoteException;

    void stopRecognition() throws RemoteException;

    public static abstract class Stub extends Binder implements IVoiceExtCommandManager {
        private static final String DESCRIPTOR = "com.mediatek.common.voiceextension.IVoiceExtCommandManager";
        static final int TRANSACTION_createCommandSet = 1;
        static final int TRANSACTION_deleteCommandSet = 4;
        static final int TRANSACTION_getCommandSetSelected = 3;
        static final int TRANSACTION_getCommandSets = 10;
        static final int TRANSACTION_getCommands = 9;
        static final int TRANSACTION_isCommandSetCreated = 2;
        static final int TRANSACTION_pauseRecognition = 13;
        static final int TRANSACTION_registerListener = 8;
        static final int TRANSACTION_resumeRecognition = 14;
        static final int TRANSACTION_selectCurrentCommandSet = 5;
        static final int TRANSACTION_setCommandsFile = 7;
        static final int TRANSACTION_setCommandsStrArray = 6;
        static final int TRANSACTION_startRecognition = 11;
        static final int TRANSACTION_stopRecognition = 12;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IVoiceExtCommandManager asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IVoiceExtCommandManager)) {
                return (IVoiceExtCommandManager) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ParcelFileDescriptor parcelFileDescriptor;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iCreateCommandSet = createCommandSet(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iCreateCommandSet);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iIsCommandSetCreated = isCommandSetCreated(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iIsCommandSetCreated);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    String commandSetSelected = getCommandSetSelected();
                    parcel2.writeNoException();
                    parcel2.writeString(commandSetSelected);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iDeleteCommandSet = deleteCommandSet(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iDeleteCommandSet);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iSelectCurrentCommandSet = selectCurrentCommandSet(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(iSelectCurrentCommandSet);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    setCommandsStrArray(parcel.createStringArray());
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    if (parcel.readInt() != 0) {
                        parcelFileDescriptor = (ParcelFileDescriptor) ParcelFileDescriptor.CREATOR.createFromParcel(parcel);
                    } else {
                        parcelFileDescriptor = null;
                    }
                    setCommandsFile(parcelFileDescriptor, parcel.readInt(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    int iRegisterListener = registerListener(IVoiceExtCommandListener.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    parcel2.writeInt(iRegisterListener);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] commands = getCommands();
                    parcel2.writeNoException();
                    parcel2.writeStringArray(commands);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    String[] commandSets = getCommandSets();
                    parcel2.writeNoException();
                    parcel2.writeStringArray(commandSets);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    startRecognition();
                    parcel2.writeNoException();
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    stopRecognition();
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    pauseRecognition();
                    parcel2.writeNoException();
                    return true;
                case TRANSACTION_resumeRecognition:
                    parcel.enforceInterface(DESCRIPTOR);
                    resumeRecognition();
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IVoiceExtCommandManager {
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
            public int createCommandSet(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int isCommandSetCreated(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getCommandSetSelected() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int deleteCommandSet(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int selectCurrentCommandSet(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setCommandsStrArray(String[] strArr) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStringArray(strArr);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setCommandsFile(ParcelFileDescriptor parcelFileDescriptor, int i, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (parcelFileDescriptor != null) {
                        parcelObtain.writeInt(1);
                        parcelFileDescriptor.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public int registerListener(IVoiceExtCommandListener iVoiceExtCommandListener) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iVoiceExtCommandListener != null ? iVoiceExtCommandListener.asBinder() : null);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getCommands() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String[] getCommandSets() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.createStringArray();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void startRecognition() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void stopRecognition() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void pauseRecognition() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void resumeRecognition() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(Stub.TRANSACTION_resumeRecognition, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
