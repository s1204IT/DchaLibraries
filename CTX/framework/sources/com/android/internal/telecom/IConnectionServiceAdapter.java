package com.android.internal.telecom;

import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.telecom.ConnectionRequest;
import android.telecom.DisconnectCause;
import android.telecom.Logging.Session;
import android.telecom.ParcelableConference;
import android.telecom.ParcelableConnection;
import android.telecom.PhoneAccountHandle;
import android.telecom.StatusHints;
import com.android.internal.telecom.IVideoProvider;
import com.android.internal.telecom.RemoteServiceCallback;
import java.util.List;

public interface IConnectionServiceAdapter extends IInterface {
    void addConferenceCall(String str, ParcelableConference parcelableConference, Session.Info info) throws RemoteException;

    void addExistingConnection(String str, ParcelableConnection parcelableConnection, Session.Info info) throws RemoteException;

    void handleCreateConnectionComplete(String str, ConnectionRequest connectionRequest, ParcelableConnection parcelableConnection, Session.Info info) throws RemoteException;

    void onConnectionEvent(String str, String str2, Bundle bundle, Session.Info info) throws RemoteException;

    void onConnectionServiceFocusReleased(Session.Info info) throws RemoteException;

    void onPhoneAccountChanged(String str, PhoneAccountHandle phoneAccountHandle, Session.Info info) throws RemoteException;

    void onPostDialChar(String str, char c, Session.Info info) throws RemoteException;

    void onPostDialWait(String str, String str2, Session.Info info) throws RemoteException;

    void onRemoteRttRequest(String str, Session.Info info) throws RemoteException;

    void onRttInitiationFailure(String str, int i, Session.Info info) throws RemoteException;

    void onRttInitiationSuccess(String str, Session.Info info) throws RemoteException;

    void onRttSessionRemotelyTerminated(String str, Session.Info info) throws RemoteException;

    void putExtras(String str, Bundle bundle, Session.Info info) throws RemoteException;

    void queryRemoteConnectionServices(RemoteServiceCallback remoteServiceCallback, Session.Info info) throws RemoteException;

    void removeCall(String str, Session.Info info) throws RemoteException;

    void removeExtras(String str, List<String> list, Session.Info info) throws RemoteException;

    void setActive(String str, Session.Info info) throws RemoteException;

    void setAddress(String str, Uri uri, int i, Session.Info info) throws RemoteException;

    void setAudioRoute(String str, int i, String str2, Session.Info info) throws RemoteException;

    void setCallerDisplayName(String str, String str2, int i, Session.Info info) throws RemoteException;

    void setConferenceMergeFailed(String str, Session.Info info) throws RemoteException;

    void setConferenceableConnections(String str, List<String> list, Session.Info info) throws RemoteException;

    void setConnectionCapabilities(String str, int i, Session.Info info) throws RemoteException;

    void setConnectionProperties(String str, int i, Session.Info info) throws RemoteException;

    void setDialing(String str, Session.Info info) throws RemoteException;

    void setDisconnected(String str, DisconnectCause disconnectCause, Session.Info info) throws RemoteException;

    void setIsConferenced(String str, String str2, Session.Info info) throws RemoteException;

    void setIsVoipAudioMode(String str, boolean z, Session.Info info) throws RemoteException;

    void setOnHold(String str, Session.Info info) throws RemoteException;

    void setPulling(String str, Session.Info info) throws RemoteException;

    void setRingbackRequested(String str, boolean z, Session.Info info) throws RemoteException;

    void setRinging(String str, Session.Info info) throws RemoteException;

    void setStatusHints(String str, StatusHints statusHints, Session.Info info) throws RemoteException;

    void setVideoProvider(String str, IVideoProvider iVideoProvider, Session.Info info) throws RemoteException;

    void setVideoState(String str, int i, Session.Info info) throws RemoteException;

    public static abstract class Stub extends Binder implements IConnectionServiceAdapter {
        private static final String DESCRIPTOR = "com.android.internal.telecom.IConnectionServiceAdapter";
        static final int TRANSACTION_addConferenceCall = 13;
        static final int TRANSACTION_addExistingConnection = 25;
        static final int TRANSACTION_handleCreateConnectionComplete = 1;
        static final int TRANSACTION_onConnectionEvent = 29;
        static final int TRANSACTION_onConnectionServiceFocusReleased = 35;
        static final int TRANSACTION_onPhoneAccountChanged = 34;
        static final int TRANSACTION_onPostDialChar = 16;
        static final int TRANSACTION_onPostDialWait = 15;
        static final int TRANSACTION_onRemoteRttRequest = 33;
        static final int TRANSACTION_onRttInitiationFailure = 31;
        static final int TRANSACTION_onRttInitiationSuccess = 30;
        static final int TRANSACTION_onRttSessionRemotelyTerminated = 32;
        static final int TRANSACTION_putExtras = 26;
        static final int TRANSACTION_queryRemoteConnectionServices = 17;
        static final int TRANSACTION_removeCall = 14;
        static final int TRANSACTION_removeExtras = 27;
        static final int TRANSACTION_setActive = 2;
        static final int TRANSACTION_setAddress = 22;
        static final int TRANSACTION_setAudioRoute = 28;
        static final int TRANSACTION_setCallerDisplayName = 23;
        static final int TRANSACTION_setConferenceMergeFailed = 12;
        static final int TRANSACTION_setConferenceableConnections = 24;
        static final int TRANSACTION_setConnectionCapabilities = 9;
        static final int TRANSACTION_setConnectionProperties = 10;
        static final int TRANSACTION_setDialing = 4;
        static final int TRANSACTION_setDisconnected = 6;
        static final int TRANSACTION_setIsConferenced = 11;
        static final int TRANSACTION_setIsVoipAudioMode = 20;
        static final int TRANSACTION_setOnHold = 7;
        static final int TRANSACTION_setPulling = 5;
        static final int TRANSACTION_setRingbackRequested = 8;
        static final int TRANSACTION_setRinging = 3;
        static final int TRANSACTION_setStatusHints = 21;
        static final int TRANSACTION_setVideoProvider = 18;
        static final int TRANSACTION_setVideoState = 19;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IConnectionServiceAdapter asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IConnectionServiceAdapter)) {
                return (IConnectionServiceAdapter) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            ConnectionRequest connectionRequestCreateFromParcel;
            ParcelableConnection parcelableConnectionCreateFromParcel;
            DisconnectCause disconnectCauseCreateFromParcel;
            ParcelableConference parcelableConferenceCreateFromParcel;
            StatusHints statusHintsCreateFromParcel;
            Uri uriCreateFromParcel;
            ParcelableConnection parcelableConnectionCreateFromParcel2;
            Bundle bundleCreateFromParcel;
            Bundle bundleCreateFromParcel2;
            PhoneAccountHandle phoneAccountHandleCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    if (parcel.readInt() != 0) {
                        connectionRequestCreateFromParcel = ConnectionRequest.CREATOR.createFromParcel(parcel);
                    } else {
                        connectionRequestCreateFromParcel = null;
                    }
                    if (parcel.readInt() != 0) {
                        parcelableConnectionCreateFromParcel = ParcelableConnection.CREATOR.createFromParcel(parcel);
                    } else {
                        parcelableConnectionCreateFromParcel = null;
                    }
                    handleCreateConnectionComplete(string, connectionRequestCreateFromParcel, parcelableConnectionCreateFromParcel, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    setActive(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    setRinging(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    setDialing(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    setPulling(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string2 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        disconnectCauseCreateFromParcel = DisconnectCause.CREATOR.createFromParcel(parcel);
                    } else {
                        disconnectCauseCreateFromParcel = null;
                    }
                    setDisconnected(string2, disconnectCauseCreateFromParcel, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    setOnHold(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    setRingbackRequested(parcel.readString(), parcel.readInt() != 0, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    setConnectionCapabilities(parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    setConnectionProperties(parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    setIsConferenced(parcel.readString(), parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    setConferenceMergeFailed(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string3 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        parcelableConferenceCreateFromParcel = ParcelableConference.CREATOR.createFromParcel(parcel);
                    } else {
                        parcelableConferenceCreateFromParcel = null;
                    }
                    addConferenceCall(string3, parcelableConferenceCreateFromParcel, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeCall(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPostDialWait(parcel.readString(), parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPostDialChar(parcel.readString(), (char) parcel.readInt(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    queryRemoteConnectionServices(RemoteServiceCallback.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    setVideoProvider(parcel.readString(), IVideoProvider.Stub.asInterface(parcel.readStrongBinder()), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    setVideoState(parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    setIsVoipAudioMode(parcel.readString(), parcel.readInt() != 0, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string4 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        statusHintsCreateFromParcel = StatusHints.CREATOR.createFromParcel(parcel);
                    } else {
                        statusHintsCreateFromParcel = null;
                    }
                    setStatusHints(string4, statusHintsCreateFromParcel, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string5 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        uriCreateFromParcel = Uri.CREATOR.createFromParcel(parcel);
                    } else {
                        uriCreateFromParcel = null;
                    }
                    setAddress(string5, uriCreateFromParcel, parcel.readInt(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    setCallerDisplayName(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    setConferenceableConnections(parcel.readString(), parcel.createStringArrayList(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string6 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        parcelableConnectionCreateFromParcel2 = ParcelableConnection.CREATOR.createFromParcel(parcel);
                    } else {
                        parcelableConnectionCreateFromParcel2 = null;
                    }
                    addExistingConnection(string6, parcelableConnectionCreateFromParcel2, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string7 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel = null;
                    }
                    putExtras(string7, bundleCreateFromParcel, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeExtras(parcel.readString(), parcel.createStringArrayList(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    setAudioRoute(parcel.readString(), parcel.readInt(), parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string8 = parcel.readString();
                    String string9 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        bundleCreateFromParcel2 = Bundle.CREATOR.createFromParcel(parcel);
                    } else {
                        bundleCreateFromParcel2 = null;
                    }
                    onConnectionEvent(string8, string9, bundleCreateFromParcel2, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    onRttInitiationSuccess(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    onRttInitiationFailure(parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    onRttSessionRemotelyTerminated(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    onRemoteRttRequest(parcel.readString(), parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string10 = parcel.readString();
                    if (parcel.readInt() != 0) {
                        phoneAccountHandleCreateFromParcel = PhoneAccountHandle.CREATOR.createFromParcel(parcel);
                    } else {
                        phoneAccountHandleCreateFromParcel = null;
                    }
                    onPhoneAccountChanged(string10, phoneAccountHandleCreateFromParcel, parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    onConnectionServiceFocusReleased(parcel.readInt() != 0 ? Session.Info.CREATOR.createFromParcel(parcel) : null);
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IConnectionServiceAdapter {
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
            public void handleCreateConnectionComplete(String str, ConnectionRequest connectionRequest, ParcelableConnection parcelableConnection, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (connectionRequest != null) {
                        parcelObtain.writeInt(1);
                        connectionRequest.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (parcelableConnection != null) {
                        parcelObtain.writeInt(1);
                        parcelableConnection.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(1, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setActive(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(2, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setRinging(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(3, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDialing(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(4, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setPulling(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(5, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setDisconnected(String str, DisconnectCause disconnectCause, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (disconnectCause != null) {
                        parcelObtain.writeInt(1);
                        disconnectCause.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(6, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setOnHold(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(7, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setRingbackRequested(String str, boolean z, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(8, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setConnectionCapabilities(String str, int i, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(9, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setConnectionProperties(String str, int i, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(10, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setIsConferenced(String str, String str2, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(11, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setConferenceMergeFailed(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(12, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addConferenceCall(String str, ParcelableConference parcelableConference, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (parcelableConference != null) {
                        parcelObtain.writeInt(1);
                        parcelableConference.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(13, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeCall(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(14, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPostDialWait(String str, String str2, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(15, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPostDialChar(String str, char c, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(c);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(16, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void queryRemoteConnectionServices(RemoteServiceCallback remoteServiceCallback, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(remoteServiceCallback != null ? remoteServiceCallback.asBinder() : null);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(17, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setVideoProvider(String str, IVideoProvider iVideoProvider, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStrongBinder(iVideoProvider != null ? iVideoProvider.asBinder() : null);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(18, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setVideoState(String str, int i, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(19, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setIsVoipAudioMode(String str, boolean z, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(20, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setStatusHints(String str, StatusHints statusHints, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (statusHints != null) {
                        parcelObtain.writeInt(1);
                        statusHints.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(21, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAddress(String str, Uri uri, int i, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (uri != null) {
                        parcelObtain.writeInt(1);
                        uri.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeInt(i);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(22, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setCallerDisplayName(String str, String str2, int i, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(23, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setConferenceableConnections(String str, List<String> list, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStringList(list);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(24, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addExistingConnection(String str, ParcelableConnection parcelableConnection, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (parcelableConnection != null) {
                        parcelObtain.writeInt(1);
                        parcelableConnection.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(25, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void putExtras(String str, Bundle bundle, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(26, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeExtras(String str, List<String> list, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeStringList(list);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(27, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setAudioRoute(String str, int i, String str2, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeString(str2);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(28, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onConnectionEvent(String str, String str2, Bundle bundle, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(29, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onRttInitiationSuccess(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(30, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onRttInitiationFailure(String str, int i, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(31, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onRttSessionRemotelyTerminated(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(32, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onRemoteRttRequest(String str, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(33, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPhoneAccountChanged(String str, PhoneAccountHandle phoneAccountHandle, Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (phoneAccountHandle != null) {
                        parcelObtain.writeInt(1);
                        phoneAccountHandle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(34, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onConnectionServiceFocusReleased(Session.Info info) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (info != null) {
                        parcelObtain.writeInt(1);
                        info.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(35, parcelObtain, null, 1);
                } finally {
                    parcelObtain.recycle();
                }
            }
        }
    }
}
