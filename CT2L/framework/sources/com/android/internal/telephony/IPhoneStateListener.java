package com.android.internal.telephony;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.PreciseCallState;
import android.telephony.PreciseDataConnectionState;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.VoLteServiceState;
import java.util.List;

public interface IPhoneStateListener extends IInterface {
    void onCallForwardingIndicatorChanged(boolean z) throws RemoteException;

    void onCallStateChanged(int i, String str) throws RemoteException;

    void onCellInfoChanged(List<CellInfo> list) throws RemoteException;

    void onCellLocationChanged(Bundle bundle) throws RemoteException;

    void onDataActivity(int i) throws RemoteException;

    void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dataConnectionRealTimeInfo) throws RemoteException;

    void onDataConnectionStateChanged(int i, int i2) throws RemoteException;

    void onMessageWaitingIndicatorChanged(boolean z) throws RemoteException;

    void onOemHookRawEvent(byte[] bArr) throws RemoteException;

    void onOtaspChanged(int i) throws RemoteException;

    void onPreciseCallStateChanged(PreciseCallState preciseCallState) throws RemoteException;

    void onPreciseDataConnectionStateChanged(PreciseDataConnectionState preciseDataConnectionState) throws RemoteException;

    void onServiceStateChanged(ServiceState serviceState) throws RemoteException;

    void onSignalStrengthChanged(int i) throws RemoteException;

    void onSignalStrengthsChanged(SignalStrength signalStrength) throws RemoteException;

    void onVoLteServiceStateChanged(VoLteServiceState voLteServiceState) throws RemoteException;

    public static abstract class Stub extends Binder implements IPhoneStateListener {
        private static final String DESCRIPTOR = "com.android.internal.telephony.IPhoneStateListener";
        static final int TRANSACTION_onCallForwardingIndicatorChanged = 4;
        static final int TRANSACTION_onCallStateChanged = 6;
        static final int TRANSACTION_onCellInfoChanged = 11;
        static final int TRANSACTION_onCellLocationChanged = 5;
        static final int TRANSACTION_onDataActivity = 8;
        static final int TRANSACTION_onDataConnectionRealTimeInfoChanged = 14;
        static final int TRANSACTION_onDataConnectionStateChanged = 7;
        static final int TRANSACTION_onMessageWaitingIndicatorChanged = 3;
        static final int TRANSACTION_onOemHookRawEvent = 16;
        static final int TRANSACTION_onOtaspChanged = 10;
        static final int TRANSACTION_onPreciseCallStateChanged = 12;
        static final int TRANSACTION_onPreciseDataConnectionStateChanged = 13;
        static final int TRANSACTION_onServiceStateChanged = 1;
        static final int TRANSACTION_onSignalStrengthChanged = 2;
        static final int TRANSACTION_onSignalStrengthsChanged = 9;
        static final int TRANSACTION_onVoLteServiceStateChanged = 15;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IPhoneStateListener asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IPhoneStateListener)) {
                return (IPhoneStateListener) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            VoLteServiceState _arg0;
            DataConnectionRealTimeInfo _arg02;
            PreciseDataConnectionState _arg03;
            PreciseCallState _arg04;
            SignalStrength _arg05;
            Bundle _arg06;
            boolean _arg07;
            ServiceState _arg08;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg08 = ServiceState.CREATOR.createFromParcel(data);
                    } else {
                        _arg08 = null;
                    }
                    onServiceStateChanged(_arg08);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    onSignalStrengthChanged(data.readInt());
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    _arg07 = data.readInt() != 0;
                    onMessageWaitingIndicatorChanged(_arg07);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    _arg07 = data.readInt() != 0;
                    onCallForwardingIndicatorChanged(_arg07);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg06 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg06 = null;
                    }
                    onCellLocationChanged(_arg06);
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    String _arg1 = data.readString();
                    onCallStateChanged(_arg09, _arg1);
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    int _arg12 = data.readInt();
                    onDataConnectionStateChanged(_arg010, _arg12);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    onDataActivity(data.readInt());
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg05 = SignalStrength.CREATOR.createFromParcel(data);
                    } else {
                        _arg05 = null;
                    }
                    onSignalStrengthsChanged(_arg05);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    onOtaspChanged(data.readInt());
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    onCellInfoChanged(data.createTypedArrayList(CellInfo.CREATOR));
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg04 = PreciseCallState.CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    onPreciseCallStateChanged(_arg04);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = PreciseDataConnectionState.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    onPreciseDataConnectionStateChanged(_arg03);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = DataConnectionRealTimeInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    onDataConnectionRealTimeInfoChanged(_arg02);
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = VoLteServiceState.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    onVoLteServiceStateChanged(_arg0);
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    onOemHookRawEvent(data.createByteArray());
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IPhoneStateListener {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            @Override
            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            @Override
            public void onServiceStateChanged(ServiceState serviceState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (serviceState != null) {
                        _data.writeInt(1);
                        serviceState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(1, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onSignalStrengthChanged(int asu) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(asu);
                    this.mRemote.transact(2, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onMessageWaitingIndicatorChanged(boolean mwi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(mwi ? 1 : 0);
                    this.mRemote.transact(3, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCallForwardingIndicatorChanged(boolean cfi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cfi ? 1 : 0);
                    this.mRemote.transact(4, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCellLocationChanged(Bundle location) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (location != null) {
                        _data.writeInt(1);
                        location.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(5, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCallStateChanged(int state, String incomingNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeString(incomingNumber);
                    this.mRemote.transact(6, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDataConnectionStateChanged(int state, int networkType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(networkType);
                    this.mRemote.transact(7, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDataActivity(int direction) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(direction);
                    this.mRemote.transact(8, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onSignalStrengthsChanged(SignalStrength signalStrength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (signalStrength != null) {
                        _data.writeInt(1);
                        signalStrength.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onOtaspChanged(int otaspMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(otaspMode);
                    this.mRemote.transact(10, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onCellInfoChanged(List<CellInfo> cellInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(cellInfo);
                    this.mRemote.transact(11, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPreciseCallStateChanged(PreciseCallState callState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (callState != null) {
                        _data.writeInt(1);
                        callState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(12, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onPreciseDataConnectionStateChanged(PreciseDataConnectionState dataConnectionState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (dataConnectionState != null) {
                        _data.writeInt(1);
                        dataConnectionState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onDataConnectionRealTimeInfoChanged(DataConnectionRealTimeInfo dcRtInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (dcRtInfo != null) {
                        _data.writeInt(1);
                        dcRtInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onVoLteServiceStateChanged(VoLteServiceState lteState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lteState != null) {
                        _data.writeInt(1);
                        lteState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(15, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void onOemHookRawEvent(byte[] rawData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeByteArray(rawData);
                    this.mRemote.transact(16, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }
        }
    }
}
