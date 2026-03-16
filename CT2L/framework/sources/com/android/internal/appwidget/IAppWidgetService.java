package com.android.internal.appwidget;

import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.widget.RemoteViews;
import com.android.internal.appwidget.IAppWidgetHost;
import java.util.ArrayList;
import java.util.List;

public interface IAppWidgetService extends IInterface {
    int allocateAppWidgetId(String str, int i) throws RemoteException;

    boolean bindAppWidgetId(String str, int i, int i2, ComponentName componentName, Bundle bundle) throws RemoteException;

    void bindRemoteViewsService(String str, int i, Intent intent, IBinder iBinder) throws RemoteException;

    IntentSender createAppWidgetConfigIntentSender(String str, int i) throws RemoteException;

    void deleteAllHosts() throws RemoteException;

    void deleteAppWidgetId(String str, int i) throws RemoteException;

    void deleteHost(String str, int i) throws RemoteException;

    int[] getAppWidgetIds(ComponentName componentName) throws RemoteException;

    int[] getAppWidgetIdsForHost(String str, int i) throws RemoteException;

    AppWidgetProviderInfo getAppWidgetInfo(String str, int i) throws RemoteException;

    Bundle getAppWidgetOptions(String str, int i) throws RemoteException;

    RemoteViews getAppWidgetViews(String str, int i) throws RemoteException;

    List<AppWidgetProviderInfo> getInstalledProvidersForProfile(int i, int i2) throws RemoteException;

    boolean hasBindAppWidgetPermission(String str, int i) throws RemoteException;

    void notifyAppWidgetViewDataChanged(String str, int[] iArr, int i) throws RemoteException;

    void partiallyUpdateAppWidgetIds(String str, int[] iArr, RemoteViews remoteViews) throws RemoteException;

    void setBindAppWidgetPermission(String str, int i, boolean z) throws RemoteException;

    int[] startListening(IAppWidgetHost iAppWidgetHost, String str, int i, List<RemoteViews> list) throws RemoteException;

    void stopListening(String str, int i) throws RemoteException;

    void unbindRemoteViewsService(String str, int i, Intent intent) throws RemoteException;

    void updateAppWidgetIds(String str, int[] iArr, RemoteViews remoteViews) throws RemoteException;

    void updateAppWidgetOptions(String str, int i, Bundle bundle) throws RemoteException;

    void updateAppWidgetProvider(ComponentName componentName, RemoteViews remoteViews) throws RemoteException;

    public static abstract class Stub extends Binder implements IAppWidgetService {
        private static final String DESCRIPTOR = "com.android.internal.appwidget.IAppWidgetService";
        static final int TRANSACTION_allocateAppWidgetId = 3;
        static final int TRANSACTION_bindAppWidgetId = 20;
        static final int TRANSACTION_bindRemoteViewsService = 21;
        static final int TRANSACTION_createAppWidgetConfigIntentSender = 9;
        static final int TRANSACTION_deleteAllHosts = 6;
        static final int TRANSACTION_deleteAppWidgetId = 4;
        static final int TRANSACTION_deleteHost = 5;
        static final int TRANSACTION_getAppWidgetIds = 23;
        static final int TRANSACTION_getAppWidgetIdsForHost = 8;
        static final int TRANSACTION_getAppWidgetInfo = 17;
        static final int TRANSACTION_getAppWidgetOptions = 12;
        static final int TRANSACTION_getAppWidgetViews = 7;
        static final int TRANSACTION_getInstalledProvidersForProfile = 16;
        static final int TRANSACTION_hasBindAppWidgetPermission = 18;
        static final int TRANSACTION_notifyAppWidgetViewDataChanged = 15;
        static final int TRANSACTION_partiallyUpdateAppWidgetIds = 13;
        static final int TRANSACTION_setBindAppWidgetPermission = 19;
        static final int TRANSACTION_startListening = 1;
        static final int TRANSACTION_stopListening = 2;
        static final int TRANSACTION_unbindRemoteViewsService = 22;
        static final int TRANSACTION_updateAppWidgetIds = 10;
        static final int TRANSACTION_updateAppWidgetOptions = 11;
        static final int TRANSACTION_updateAppWidgetProvider = 14;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IAppWidgetService asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof IAppWidgetService)) {
                return (IAppWidgetService) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            ComponentName _arg0;
            Intent _arg2;
            Intent _arg22;
            ComponentName _arg3;
            Bundle _arg4;
            ComponentName _arg02;
            RemoteViews _arg1;
            RemoteViews _arg23;
            Bundle _arg24;
            RemoteViews _arg25;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    IAppWidgetHost _arg03 = IAppWidgetHost.Stub.asInterface(data.readStrongBinder());
                    String _arg12 = data.readString();
                    int _arg26 = data.readInt();
                    ArrayList arrayList = new ArrayList();
                    int[] _result = startListening(_arg03, _arg12, _arg26, arrayList);
                    reply.writeNoException();
                    reply.writeIntArray(_result);
                    reply.writeTypedList(arrayList);
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    int _arg13 = data.readInt();
                    stopListening(_arg04, _arg13);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    int _arg14 = data.readInt();
                    int _result2 = allocateAppWidgetId(_arg05, _arg14);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    int _arg15 = data.readInt();
                    deleteAppWidgetId(_arg06, _arg15);
                    reply.writeNoException();
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    int _arg16 = data.readInt();
                    deleteHost(_arg07, _arg16);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    deleteAllHosts();
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    int _arg17 = data.readInt();
                    RemoteViews _result3 = getAppWidgetViews(_arg08, _arg17);
                    reply.writeNoException();
                    if (_result3 != null) {
                        reply.writeInt(1);
                        _result3.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    int _arg18 = data.readInt();
                    int[] _result4 = getAppWidgetIdsForHost(_arg09, _arg18);
                    reply.writeNoException();
                    reply.writeIntArray(_result4);
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    int _arg19 = data.readInt();
                    IntentSender _result5 = createAppWidgetConfigIntentSender(_arg010, _arg19);
                    reply.writeNoException();
                    if (_result5 != null) {
                        reply.writeInt(1);
                        _result5.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    int[] _arg110 = data.createIntArray();
                    if (data.readInt() != 0) {
                        _arg25 = RemoteViews.CREATOR.createFromParcel(data);
                    } else {
                        _arg25 = null;
                    }
                    updateAppWidgetIds(_arg011, _arg110, _arg25);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    int _arg111 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg24 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg24 = null;
                    }
                    updateAppWidgetOptions(_arg012, _arg111, _arg24);
                    reply.writeNoException();
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    int _arg112 = data.readInt();
                    Bundle _result6 = getAppWidgetOptions(_arg013, _arg112);
                    reply.writeNoException();
                    if (_result6 != null) {
                        reply.writeInt(1);
                        _result6.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    int[] _arg113 = data.createIntArray();
                    if (data.readInt() != 0) {
                        _arg23 = RemoteViews.CREATOR.createFromParcel(data);
                    } else {
                        _arg23 = null;
                    }
                    partiallyUpdateAppWidgetIds(_arg014, _arg113, _arg23);
                    reply.writeNoException();
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg1 = RemoteViews.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    updateAppWidgetProvider(_arg02, _arg1);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg015 = data.readString();
                    int[] _arg114 = data.createIntArray();
                    int _arg27 = data.readInt();
                    notifyAppWidgetViewDataChanged(_arg015, _arg114, _arg27);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    int _arg016 = data.readInt();
                    int _arg115 = data.readInt();
                    List<AppWidgetProviderInfo> _result7 = getInstalledProvidersForProfile(_arg016, _arg115);
                    reply.writeNoException();
                    reply.writeTypedList(_result7);
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg017 = data.readString();
                    int _arg116 = data.readInt();
                    AppWidgetProviderInfo _result8 = getAppWidgetInfo(_arg017, _arg116);
                    reply.writeNoException();
                    if (_result8 != null) {
                        reply.writeInt(1);
                        _result8.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg018 = data.readString();
                    int _arg117 = data.readInt();
                    boolean _result9 = hasBindAppWidgetPermission(_arg018, _arg117);
                    reply.writeNoException();
                    reply.writeInt(_result9 ? 1 : 0);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg019 = data.readString();
                    int _arg118 = data.readInt();
                    boolean _arg28 = data.readInt() != 0;
                    setBindAppWidgetPermission(_arg019, _arg118, _arg28);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg020 = data.readString();
                    int _arg119 = data.readInt();
                    int _arg29 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg3 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg3 = null;
                    }
                    if (data.readInt() != 0) {
                        _arg4 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg4 = null;
                    }
                    boolean _result10 = bindAppWidgetId(_arg020, _arg119, _arg29, _arg3, _arg4);
                    reply.writeNoException();
                    reply.writeInt(_result10 ? 1 : 0);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg021 = data.readString();
                    int _arg120 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg22 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        _arg22 = null;
                    }
                    IBinder _arg32 = data.readStrongBinder();
                    bindRemoteViewsService(_arg021, _arg120, _arg22, _arg32);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg022 = data.readString();
                    int _arg121 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg2 = Intent.CREATOR.createFromParcel(data);
                    } else {
                        _arg2 = null;
                    }
                    unbindRemoteViewsService(_arg022, _arg121, _arg2);
                    reply.writeNoException();
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    int[] _result11 = getAppWidgetIds(_arg0);
                    reply.writeNoException();
                    reply.writeIntArray(_result11);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements IAppWidgetService {
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
            public int[] startListening(IAppWidgetHost host, String callingPackage, int hostId, List<RemoteViews> updatedViews) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(host != null ? host.asBinder() : null);
                    _data.writeString(callingPackage);
                    _data.writeInt(hostId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    _reply.readTypedList(updatedViews, RemoteViews.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void stopListening(String callingPackage, int hostId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(hostId);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int allocateAppWidgetId(String callingPackage, int hostId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(hostId);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void deleteAppWidgetId(String callingPackage, int appWidgetId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void deleteHost(String packageName, int hostId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(hostId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void deleteAllHosts() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public RemoteViews getAppWidgetViews(String callingPackage, int appWidgetId) throws RemoteException {
                RemoteViews _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = RemoteViews.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getAppWidgetIdsForHost(String callingPackage, int hostId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(hostId);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public IntentSender createAppWidgetConfigIntentSender(String callingPackage, int appWidgetId) throws RemoteException {
                IntentSender _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = IntentSender.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void updateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeIntArray(appWidgetIds);
                    if (views != null) {
                        _data.writeInt(1);
                        views.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void updateAppWidgetOptions(String callingPackage, int appWidgetId, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public Bundle getAppWidgetOptions(String callingPackage, int appWidgetId) throws RemoteException {
                Bundle _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = Bundle.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void partiallyUpdateAppWidgetIds(String callingPackage, int[] appWidgetIds, RemoteViews views) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeIntArray(appWidgetIds);
                    if (views != null) {
                        _data.writeInt(1);
                        views.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void updateAppWidgetProvider(ComponentName provider, RemoteViews views) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (provider != null) {
                        _data.writeInt(1);
                        provider.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (views != null) {
                        _data.writeInt(1);
                        views.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyAppWidgetViewDataChanged(String packageName, int[] appWidgetIds, int viewId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeIntArray(appWidgetIds);
                    _data.writeInt(viewId);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public List<AppWidgetProviderInfo> getInstalledProvidersForProfile(int categoryFilter, int profileId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(categoryFilter);
                    _data.writeInt(profileId);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    List<AppWidgetProviderInfo> _result = _reply.createTypedArrayList(AppWidgetProviderInfo.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public AppWidgetProviderInfo getAppWidgetInfo(String callingPackage, int appWidgetId) throws RemoteException {
                AppWidgetProviderInfo _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = AppWidgetProviderInfo.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean hasBindAppWidgetPermission(String packageName, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setBindAppWidgetPermission(String packageName, int userId, boolean permission) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeInt(permission ? 1 : 0);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean bindAppWidgetId(String callingPackage, int appWidgetId, int providerProfileId, ComponentName providerComponent, Bundle options) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    _data.writeInt(providerProfileId);
                    if (providerComponent != null) {
                        _data.writeInt(1);
                        providerComponent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (options != null) {
                        _data.writeInt(1);
                        options.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void bindRemoteViewsService(String callingPackage, int appWidgetId, Intent intent, IBinder connection) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(connection);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unbindRemoteViewsService(String callingPackage, int appWidgetId, Intent intent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPackage);
                    _data.writeInt(appWidgetId);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int[] getAppWidgetIds(ComponentName providerComponent) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (providerComponent != null) {
                        _data.writeInt(1);
                        providerComponent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    int[] _result = _reply.createIntArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
