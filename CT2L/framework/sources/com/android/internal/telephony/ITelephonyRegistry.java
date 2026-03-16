package com.android.internal.telephony;

import android.net.LinkProperties;
import android.net.NetworkCapabilities;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telephony.CellInfo;
import android.telephony.DataConnectionRealTimeInfo;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.VoLteServiceState;
import com.android.internal.telephony.IOnSubscriptionsChangedListener;
import com.android.internal.telephony.IPhoneStateListener;
import java.util.List;

public interface ITelephonyRegistry extends IInterface {
    void addOnSubscriptionsChangedListener(String str, IOnSubscriptionsChangedListener iOnSubscriptionsChangedListener) throws RemoteException;

    void listen(String str, IPhoneStateListener iPhoneStateListener, int i, boolean z) throws RemoteException;

    void listenForSubscriber(int i, String str, IPhoneStateListener iPhoneStateListener, int i2, boolean z) throws RemoteException;

    void notifyCallForwardingChanged(boolean z) throws RemoteException;

    void notifyCallForwardingChangedForSubscriber(int i, boolean z) throws RemoteException;

    void notifyCallState(int i, String str) throws RemoteException;

    void notifyCallStateForSubscriber(int i, int i2, String str) throws RemoteException;

    void notifyCellInfo(List<CellInfo> list) throws RemoteException;

    void notifyCellInfoForSubscriber(int i, List<CellInfo> list) throws RemoteException;

    void notifyCellLocation(Bundle bundle) throws RemoteException;

    void notifyCellLocationForSubscriber(int i, Bundle bundle) throws RemoteException;

    void notifyDataActivity(int i) throws RemoteException;

    void notifyDataActivityForSubscriber(int i, int i2) throws RemoteException;

    void notifyDataConnection(int i, boolean z, String str, String str2, String str3, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i2, boolean z2) throws RemoteException;

    void notifyDataConnectionFailed(String str, String str2) throws RemoteException;

    void notifyDataConnectionFailedForSubscriber(int i, String str, String str2) throws RemoteException;

    void notifyDataConnectionForSubscriber(int i, int i2, boolean z, String str, String str2, String str3, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int i3, boolean z2) throws RemoteException;

    void notifyDataConnectionRealTimeInfo(DataConnectionRealTimeInfo dataConnectionRealTimeInfo) throws RemoteException;

    void notifyDisconnectCause(int i, int i2) throws RemoteException;

    void notifyMessageWaitingChangedForPhoneId(int i, int i2, boolean z) throws RemoteException;

    void notifyOemHookRawEventForSubscriber(int i, byte[] bArr) throws RemoteException;

    void notifyOtaspChanged(int i) throws RemoteException;

    void notifyPreciseCallState(int i, int i2, int i3) throws RemoteException;

    void notifyPreciseDataConnectionFailed(String str, String str2, String str3, String str4) throws RemoteException;

    void notifyServiceStateForPhoneId(int i, int i2, ServiceState serviceState) throws RemoteException;

    void notifySignalStrength(SignalStrength signalStrength) throws RemoteException;

    void notifySignalStrengthForSubscriber(int i, SignalStrength signalStrength) throws RemoteException;

    void notifySubscriptionInfoChanged() throws RemoteException;

    void notifyVoLteServiceStateChanged(VoLteServiceState voLteServiceState) throws RemoteException;

    void removeOnSubscriptionsChangedListener(String str, IOnSubscriptionsChangedListener iOnSubscriptionsChangedListener) throws RemoteException;

    public static abstract class Stub extends Binder implements ITelephonyRegistry {
        private static final String DESCRIPTOR = "com.android.internal.telephony.ITelephonyRegistry";
        static final int TRANSACTION_addOnSubscriptionsChangedListener = 1;
        static final int TRANSACTION_listen = 3;
        static final int TRANSACTION_listenForSubscriber = 4;
        static final int TRANSACTION_notifyCallForwardingChanged = 11;
        static final int TRANSACTION_notifyCallForwardingChangedForSubscriber = 12;
        static final int TRANSACTION_notifyCallState = 5;
        static final int TRANSACTION_notifyCallStateForSubscriber = 6;
        static final int TRANSACTION_notifyCellInfo = 22;
        static final int TRANSACTION_notifyCellInfoForSubscriber = 26;
        static final int TRANSACTION_notifyCellLocation = 19;
        static final int TRANSACTION_notifyCellLocationForSubscriber = 20;
        static final int TRANSACTION_notifyDataActivity = 13;
        static final int TRANSACTION_notifyDataActivityForSubscriber = 14;
        static final int TRANSACTION_notifyDataConnection = 15;
        static final int TRANSACTION_notifyDataConnectionFailed = 17;
        static final int TRANSACTION_notifyDataConnectionFailedForSubscriber = 18;
        static final int TRANSACTION_notifyDataConnectionForSubscriber = 16;
        static final int TRANSACTION_notifyDataConnectionRealTimeInfo = 27;
        static final int TRANSACTION_notifyDisconnectCause = 24;
        static final int TRANSACTION_notifyMessageWaitingChangedForPhoneId = 10;
        static final int TRANSACTION_notifyOemHookRawEventForSubscriber = 29;
        static final int TRANSACTION_notifyOtaspChanged = 21;
        static final int TRANSACTION_notifyPreciseCallState = 23;
        static final int TRANSACTION_notifyPreciseDataConnectionFailed = 25;
        static final int TRANSACTION_notifyServiceStateForPhoneId = 7;
        static final int TRANSACTION_notifySignalStrength = 8;
        static final int TRANSACTION_notifySignalStrengthForSubscriber = 9;
        static final int TRANSACTION_notifySubscriptionInfoChanged = 30;
        static final int TRANSACTION_notifyVoLteServiceStateChanged = 28;
        static final int TRANSACTION_removeOnSubscriptionsChangedListener = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ITelephonyRegistry asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof ITelephonyRegistry)) {
                return (ITelephonyRegistry) iin;
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
            Bundle _arg1;
            Bundle _arg03;
            LinkProperties _arg6;
            NetworkCapabilities _arg7;
            LinkProperties _arg5;
            NetworkCapabilities _arg62;
            SignalStrength _arg12;
            SignalStrength _arg04;
            ServiceState _arg2;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    IOnSubscriptionsChangedListener _arg13 = IOnSubscriptionsChangedListener.Stub.asInterface(data.readStrongBinder());
                    addOnSubscriptionsChangedListener(_arg05, _arg13);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    IOnSubscriptionsChangedListener _arg14 = IOnSubscriptionsChangedListener.Stub.asInterface(data.readStrongBinder());
                    removeOnSubscriptionsChangedListener(_arg06, _arg14);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    IPhoneStateListener _arg15 = IPhoneStateListener.Stub.asInterface(data.readStrongBinder());
                    int _arg22 = data.readInt();
                    boolean _arg3 = data.readInt() != 0;
                    listen(_arg07, _arg15, _arg22, _arg3);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg08 = data.readInt();
                    String _arg16 = data.readString();
                    IPhoneStateListener _arg23 = IPhoneStateListener.Stub.asInterface(data.readStrongBinder());
                    int _arg32 = data.readInt();
                    boolean _arg4 = data.readInt() != 0;
                    listenForSubscriber(_arg08, _arg16, _arg23, _arg32, _arg4);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg09 = data.readInt();
                    String _arg17 = data.readString();
                    notifyCallState(_arg09, _arg17);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg010 = data.readInt();
                    int _arg18 = data.readInt();
                    String _arg24 = data.readString();
                    notifyCallStateForSubscriber(_arg010, _arg18, _arg24);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg011 = data.readInt();
                    int _arg19 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg2 = ServiceState.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    notifyServiceStateForPhoneId(_arg011, _arg19, _arg2);
                    reply.writeNoException();
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg04 = SignalStrength.CREATOR.createFromParcel(data);
                    } else {
                        _arg04 = null;
                    }
                    notifySignalStrength(_arg04);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg012 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg12 = SignalStrength.CREATOR.createFromParcel(data);
                    } else {
                        _arg12 = null;
                    }
                    notifySignalStrengthForSubscriber(_arg012, _arg12);
                    reply.writeNoException();
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg013 = data.readInt();
                    int _arg110 = data.readInt();
                    boolean _arg25 = data.readInt() != 0;
                    notifyMessageWaitingChangedForPhoneId(_arg013, _arg110, _arg25);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    boolean _arg014 = data.readInt() != 0;
                    notifyCallForwardingChanged(_arg014);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg015 = data.readInt();
                    boolean _arg111 = data.readInt() != 0;
                    notifyCallForwardingChangedForSubscriber(_arg015, _arg111);
                    reply.writeNoException();
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg016 = data.readInt();
                    notifyDataActivity(_arg016);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg017 = data.readInt();
                    int _arg112 = data.readInt();
                    notifyDataActivityForSubscriber(_arg017, _arg112);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg018 = data.readInt();
                    boolean _arg113 = data.readInt() != 0;
                    String _arg26 = data.readString();
                    String _arg33 = data.readString();
                    String _arg42 = data.readString();
                    if (data.readInt() != 0) {
                        _arg5 = LinkProperties.CREATOR.createFromParcel(data);
                    } else {
                        _arg5 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg62 = NetworkCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg62 = null;
                    }
                    int _arg72 = data.readInt();
                    boolean _arg8 = data.readInt() != 0;
                    notifyDataConnection(_arg018, _arg113, _arg26, _arg33, _arg42, _arg5, _arg62, _arg72, _arg8);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg019 = data.readInt();
                    int _arg114 = data.readInt();
                    boolean _arg27 = data.readInt() != 0;
                    String _arg34 = data.readString();
                    String _arg43 = data.readString();
                    String _arg52 = data.readString();
                    if (data.readInt() != 0) {
                        _arg6 = LinkProperties.CREATOR.createFromParcel(data);
                    } else {
                        _arg6 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg7 = NetworkCapabilities.CREATOR.createFromParcel(data);
                    } else {
                        _arg7 = null;
                    }
                    int _arg82 = data.readInt();
                    boolean _arg9 = data.readInt() != 0;
                    notifyDataConnectionForSubscriber(_arg019, _arg114, _arg27, _arg34, _arg43, _arg52, _arg6, _arg7, _arg82, _arg9);
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg020 = data.readString();
                    String _arg115 = data.readString();
                    notifyDataConnectionFailed(_arg020, _arg115);
                    reply.writeNoException();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg021 = data.readInt();
                    String _arg116 = data.readString();
                    String _arg28 = data.readString();
                    notifyDataConnectionFailedForSubscriber(_arg021, _arg116, _arg28);
                    reply.writeNoException();
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    notifyCellLocation(_arg03);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg022 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg1 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    notifyCellLocationForSubscriber(_arg022, _arg1);
                    reply.writeNoException();
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg023 = data.readInt();
                    notifyOtaspChanged(_arg023);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    List<CellInfo> _arg024 = data.createTypedArrayList(CellInfo.CREATOR);
                    notifyCellInfo(_arg024);
                    reply.writeNoException();
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg025 = data.readInt();
                    int _arg117 = data.readInt();
                    int _arg29 = data.readInt();
                    notifyPreciseCallState(_arg025, _arg117, _arg29);
                    reply.writeNoException();
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg026 = data.readInt();
                    int _arg118 = data.readInt();
                    notifyDisconnectCause(_arg026, _arg118);
                    reply.writeNoException();
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg027 = data.readString();
                    String _arg119 = data.readString();
                    String _arg210 = data.readString();
                    String _arg35 = data.readString();
                    notifyPreciseDataConnectionFailed(_arg027, _arg119, _arg210, _arg35);
                    reply.writeNoException();
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg028 = data.readInt();
                    List<CellInfo> _arg120 = data.createTypedArrayList(CellInfo.CREATOR);
                    notifyCellInfoForSubscriber(_arg028, _arg120);
                    reply.writeNoException();
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = DataConnectionRealTimeInfo.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    notifyDataConnectionRealTimeInfo(_arg02);
                    reply.writeNoException();
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = VoLteServiceState.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    notifyVoLteServiceStateChanged(_arg0);
                    reply.writeNoException();
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg029 = data.readInt();
                    byte[] _arg121 = data.createByteArray();
                    notifyOemHookRawEventForSubscriber(_arg029, _arg121);
                    reply.writeNoException();
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    notifySubscriptionInfoChanged();
                    reply.writeNoException();
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements ITelephonyRegistry {
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
            public void addOnSubscriptionsChangedListener(String pkg, IOnSubscriptionsChangedListener callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void removeOnSubscriptionsChangedListener(String pkg, IOnSubscriptionsChangedListener callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void listen(String pkg, IPhoneStateListener callback, int events, boolean notifyNow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(events);
                    _data.writeInt(notifyNow ? 1 : 0);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void listenForSubscriber(int subId, String pkg, IPhoneStateListener callback, int events, boolean notifyNow) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(events);
                    _data.writeInt(notifyNow ? 1 : 0);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyCallState(int state, String incomingNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeString(incomingNumber);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyCallStateForSubscriber(int subId, int state, String incomingNumber) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(state);
                    _data.writeString(incomingNumber);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyServiceStateForPhoneId(int phoneId, int subId, ServiceState state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(subId);
                    if (state != null) {
                        _data.writeInt(1);
                        state.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifySignalStrength(SignalStrength signalStrength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (signalStrength != null) {
                        _data.writeInt(1);
                        signalStrength.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifySignalStrengthForSubscriber(int subId, SignalStrength signalStrength) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    if (signalStrength != null) {
                        _data.writeInt(1);
                        signalStrength.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyMessageWaitingChangedForPhoneId(int phoneId, int subId, boolean mwi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(phoneId);
                    _data.writeInt(subId);
                    _data.writeInt(mwi ? 1 : 0);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyCallForwardingChanged(boolean cfi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(cfi ? 1 : 0);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyCallForwardingChangedForSubscriber(int subId, boolean cfi) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(cfi ? 1 : 0);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyDataActivity(int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyDataActivityForSubscriber(int subId, int state) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(state);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyDataConnection(int state, boolean isDataConnectivityPossible, String reason, String apn, String apnType, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int networkType, boolean roaming) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(state);
                    _data.writeInt(isDataConnectivityPossible ? 1 : 0);
                    _data.writeString(reason);
                    _data.writeString(apn);
                    _data.writeString(apnType);
                    if (linkProperties != null) {
                        _data.writeInt(1);
                        linkProperties.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (networkCapabilities != null) {
                        _data.writeInt(1);
                        networkCapabilities.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(networkType);
                    _data.writeInt(roaming ? 1 : 0);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyDataConnectionForSubscriber(int subId, int state, boolean isDataConnectivityPossible, String reason, String apn, String apnType, LinkProperties linkProperties, NetworkCapabilities networkCapabilities, int networkType, boolean roaming) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeInt(state);
                    _data.writeInt(isDataConnectivityPossible ? 1 : 0);
                    _data.writeString(reason);
                    _data.writeString(apn);
                    _data.writeString(apnType);
                    if (linkProperties != null) {
                        _data.writeInt(1);
                        linkProperties.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (networkCapabilities != null) {
                        _data.writeInt(1);
                        networkCapabilities.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(networkType);
                    _data.writeInt(roaming ? 1 : 0);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyDataConnectionFailed(String reason, String apnType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(reason);
                    _data.writeString(apnType);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyDataConnectionFailedForSubscriber(int subId, String reason, String apnType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeString(reason);
                    _data.writeString(apnType);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyCellLocation(Bundle cellLocation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (cellLocation != null) {
                        _data.writeInt(1);
                        cellLocation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyCellLocationForSubscriber(int subId, Bundle cellLocation) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    if (cellLocation != null) {
                        _data.writeInt(1);
                        cellLocation.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyOtaspChanged(int otaspMode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(otaspMode);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyCellInfo(List<CellInfo> cellInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(cellInfo);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyPreciseCallState(int ringingCallState, int foregroundCallState, int backgroundCallState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(ringingCallState);
                    _data.writeInt(foregroundCallState);
                    _data.writeInt(backgroundCallState);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyDisconnectCause(int disconnectCause, int preciseDisconnectCause) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(disconnectCause);
                    _data.writeInt(preciseDisconnectCause);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyPreciseDataConnectionFailed(String reason, String apnType, String apn, String failCause) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(reason);
                    _data.writeString(apnType);
                    _data.writeString(apn);
                    _data.writeString(failCause);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyCellInfoForSubscriber(int subId, List<CellInfo> cellInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeTypedList(cellInfo);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyDataConnectionRealTimeInfo(DataConnectionRealTimeInfo dcRtInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (dcRtInfo != null) {
                        _data.writeInt(1);
                        dcRtInfo.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyVoLteServiceStateChanged(VoLteServiceState lteState) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (lteState != null) {
                        _data.writeInt(1);
                        lteState.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyOemHookRawEventForSubscriber(int subId, byte[] rawData) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(subId);
                    _data.writeByteArray(rawData);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifySubscriptionInfoChanged() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
