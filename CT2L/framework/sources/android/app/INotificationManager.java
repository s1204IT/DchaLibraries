package android.app;

import android.app.ITransientNotification;
import android.content.ComponentName;
import android.content.pm.ParceledListSlice;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.service.notification.Condition;
import android.service.notification.IConditionListener;
import android.service.notification.IConditionProvider;
import android.service.notification.INotificationListener;
import android.service.notification.StatusBarNotification;
import android.service.notification.ZenModeConfig;

public interface INotificationManager extends IInterface {
    boolean areNotificationsEnabledForPackage(String str, int i) throws RemoteException;

    void cancelAllNotifications(String str, int i) throws RemoteException;

    void cancelNotificationFromListener(INotificationListener iNotificationListener, String str, String str2, int i) throws RemoteException;

    void cancelNotificationWithTag(String str, String str2, int i, int i2) throws RemoteException;

    void cancelNotificationsFromListener(INotificationListener iNotificationListener, String[] strArr) throws RemoteException;

    void cancelToast(String str, ITransientNotification iTransientNotification) throws RemoteException;

    void enqueueNotificationWithTag(String str, String str2, String str3, int i, Notification notification, int[] iArr, int i2) throws RemoteException;

    void enqueueToast(String str, ITransientNotification iTransientNotification, int i) throws RemoteException;

    StatusBarNotification[] getActiveNotifications(String str) throws RemoteException;

    ParceledListSlice getActiveNotificationsFromListener(INotificationListener iNotificationListener, String[] strArr, int i) throws RemoteException;

    Condition[] getAutomaticZenModeConditions() throws RemoteException;

    ComponentName getEffectsSuppressor() throws RemoteException;

    int getHintsFromListener(INotificationListener iNotificationListener) throws RemoteException;

    StatusBarNotification[] getHistoricalNotifications(String str, int i) throws RemoteException;

    int getInterruptionFilterFromListener(INotificationListener iNotificationListener) throws RemoteException;

    int getPackagePriority(String str, int i) throws RemoteException;

    int getPackageVisibilityOverride(String str, int i) throws RemoteException;

    ZenModeConfig getZenModeConfig() throws RemoteException;

    boolean isSystemConditionProviderEnabled(String str) throws RemoteException;

    boolean matchesCallFilter(Bundle bundle) throws RemoteException;

    void notifyConditions(String str, IConditionProvider iConditionProvider, Condition[] conditionArr) throws RemoteException;

    void registerListener(INotificationListener iNotificationListener, ComponentName componentName, int i) throws RemoteException;

    void requestHintsFromListener(INotificationListener iNotificationListener, int i) throws RemoteException;

    void requestInterruptionFilterFromListener(INotificationListener iNotificationListener, int i) throws RemoteException;

    void requestZenModeConditions(IConditionListener iConditionListener, int i) throws RemoteException;

    void setAutomaticZenModeConditions(Uri[] uriArr) throws RemoteException;

    void setNotificationsEnabledForPackage(String str, int i, boolean z) throws RemoteException;

    void setOnNotificationPostedTrimFromListener(INotificationListener iNotificationListener, int i) throws RemoteException;

    void setPackagePriority(String str, int i, int i2) throws RemoteException;

    void setPackageVisibilityOverride(String str, int i, int i2) throws RemoteException;

    void setZenModeCondition(Condition condition) throws RemoteException;

    boolean setZenModeConfig(ZenModeConfig zenModeConfig) throws RemoteException;

    void unregisterListener(INotificationListener iNotificationListener, int i) throws RemoteException;

    public static abstract class Stub extends Binder implements INotificationManager {
        private static final String DESCRIPTOR = "android.app.INotificationManager";
        static final int TRANSACTION_areNotificationsEnabledForPackage = 7;
        static final int TRANSACTION_cancelAllNotifications = 1;
        static final int TRANSACTION_cancelNotificationFromListener = 16;
        static final int TRANSACTION_cancelNotificationWithTag = 5;
        static final int TRANSACTION_cancelNotificationsFromListener = 17;
        static final int TRANSACTION_cancelToast = 3;
        static final int TRANSACTION_enqueueNotificationWithTag = 4;
        static final int TRANSACTION_enqueueToast = 2;
        static final int TRANSACTION_getActiveNotifications = 12;
        static final int TRANSACTION_getActiveNotificationsFromListener = 18;
        static final int TRANSACTION_getAutomaticZenModeConditions = 33;
        static final int TRANSACTION_getEffectsSuppressor = 24;
        static final int TRANSACTION_getHintsFromListener = 20;
        static final int TRANSACTION_getHistoricalNotifications = 13;
        static final int TRANSACTION_getInterruptionFilterFromListener = 22;
        static final int TRANSACTION_getPackagePriority = 9;
        static final int TRANSACTION_getPackageVisibilityOverride = 11;
        static final int TRANSACTION_getZenModeConfig = 27;
        static final int TRANSACTION_isSystemConditionProviderEnabled = 26;
        static final int TRANSACTION_matchesCallFilter = 25;
        static final int TRANSACTION_notifyConditions = 29;
        static final int TRANSACTION_registerListener = 14;
        static final int TRANSACTION_requestHintsFromListener = 19;
        static final int TRANSACTION_requestInterruptionFilterFromListener = 21;
        static final int TRANSACTION_requestZenModeConditions = 30;
        static final int TRANSACTION_setAutomaticZenModeConditions = 32;
        static final int TRANSACTION_setNotificationsEnabledForPackage = 6;
        static final int TRANSACTION_setOnNotificationPostedTrimFromListener = 23;
        static final int TRANSACTION_setPackagePriority = 8;
        static final int TRANSACTION_setPackageVisibilityOverride = 10;
        static final int TRANSACTION_setZenModeCondition = 31;
        static final int TRANSACTION_setZenModeConfig = 28;
        static final int TRANSACTION_unregisterListener = 15;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static INotificationManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin != null && (iin instanceof INotificationManager)) {
                return (INotificationManager) iin;
            }
            return new Proxy(obj);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            Condition _arg0;
            ZenModeConfig _arg02;
            Bundle _arg03;
            ComponentName _arg1;
            Notification _arg4;
            switch (code) {
                case 1:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg04 = data.readString();
                    int _arg12 = data.readInt();
                    cancelAllNotifications(_arg04, _arg12);
                    reply.writeNoException();
                    return true;
                case 2:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg05 = data.readString();
                    ITransientNotification _arg13 = ITransientNotification.Stub.asInterface(data.readStrongBinder());
                    int _arg2 = data.readInt();
                    enqueueToast(_arg05, _arg13, _arg2);
                    reply.writeNoException();
                    return true;
                case 3:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg06 = data.readString();
                    ITransientNotification _arg14 = ITransientNotification.Stub.asInterface(data.readStrongBinder());
                    cancelToast(_arg06, _arg14);
                    reply.writeNoException();
                    return true;
                case 4:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg07 = data.readString();
                    String _arg15 = data.readString();
                    String _arg22 = data.readString();
                    int _arg3 = data.readInt();
                    if (data.readInt() != 0) {
                        _arg4 = Notification.CREATOR.createFromParcel(data);
                    } else {
                        _arg4 = null;
                    }
                    int[] _arg5 = data.createIntArray();
                    int _arg6 = data.readInt();
                    enqueueNotificationWithTag(_arg07, _arg15, _arg22, _arg3, _arg4, _arg5, _arg6);
                    reply.writeNoException();
                    reply.writeIntArray(_arg5);
                    return true;
                case 5:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg08 = data.readString();
                    String _arg16 = data.readString();
                    int _arg23 = data.readInt();
                    int _arg32 = data.readInt();
                    cancelNotificationWithTag(_arg08, _arg16, _arg23, _arg32);
                    reply.writeNoException();
                    return true;
                case 6:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg09 = data.readString();
                    int _arg17 = data.readInt();
                    boolean _arg24 = data.readInt() != 0;
                    setNotificationsEnabledForPackage(_arg09, _arg17, _arg24);
                    reply.writeNoException();
                    return true;
                case 7:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg010 = data.readString();
                    int _arg18 = data.readInt();
                    boolean _result = areNotificationsEnabledForPackage(_arg010, _arg18);
                    reply.writeNoException();
                    reply.writeInt(_result ? 1 : 0);
                    return true;
                case 8:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg011 = data.readString();
                    int _arg19 = data.readInt();
                    int _arg25 = data.readInt();
                    setPackagePriority(_arg011, _arg19, _arg25);
                    reply.writeNoException();
                    return true;
                case 9:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg012 = data.readString();
                    int _arg110 = data.readInt();
                    int _result2 = getPackagePriority(_arg012, _arg110);
                    reply.writeNoException();
                    reply.writeInt(_result2);
                    return true;
                case 10:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg013 = data.readString();
                    int _arg111 = data.readInt();
                    int _arg26 = data.readInt();
                    setPackageVisibilityOverride(_arg013, _arg111, _arg26);
                    reply.writeNoException();
                    return true;
                case 11:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg014 = data.readString();
                    int _arg112 = data.readInt();
                    int _result3 = getPackageVisibilityOverride(_arg014, _arg112);
                    reply.writeNoException();
                    reply.writeInt(_result3);
                    return true;
                case 12:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg015 = data.readString();
                    StatusBarNotification[] _result4 = getActiveNotifications(_arg015);
                    reply.writeNoException();
                    reply.writeTypedArray(_result4, 1);
                    return true;
                case 13:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg016 = data.readString();
                    int _arg113 = data.readInt();
                    StatusBarNotification[] _result5 = getHistoricalNotifications(_arg016, _arg113);
                    reply.writeNoException();
                    reply.writeTypedArray(_result5, 1);
                    return true;
                case 14:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg017 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    if (data.readInt() != 0) {
                        _arg1 = ComponentName.CREATOR.createFromParcel(data);
                    } else {
                        _arg1 = null;
                    }
                    int _arg27 = data.readInt();
                    registerListener(_arg017, _arg1, _arg27);
                    reply.writeNoException();
                    return true;
                case 15:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg018 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    int _arg114 = data.readInt();
                    unregisterListener(_arg018, _arg114);
                    reply.writeNoException();
                    return true;
                case 16:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg019 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    String _arg115 = data.readString();
                    String _arg28 = data.readString();
                    int _arg33 = data.readInt();
                    cancelNotificationFromListener(_arg019, _arg115, _arg28, _arg33);
                    reply.writeNoException();
                    return true;
                case 17:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg020 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    String[] _arg116 = data.createStringArray();
                    cancelNotificationsFromListener(_arg020, _arg116);
                    reply.writeNoException();
                    return true;
                case 18:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg021 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    String[] _arg117 = data.createStringArray();
                    int _arg29 = data.readInt();
                    ParceledListSlice _result6 = getActiveNotificationsFromListener(_arg021, _arg117, _arg29);
                    reply.writeNoException();
                    if (_result6 != null) {
                        reply.writeInt(1);
                        _result6.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 19:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg022 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    int _arg118 = data.readInt();
                    requestHintsFromListener(_arg022, _arg118);
                    reply.writeNoException();
                    return true;
                case 20:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg023 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    int _result7 = getHintsFromListener(_arg023);
                    reply.writeNoException();
                    reply.writeInt(_result7);
                    return true;
                case 21:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg024 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    int _arg119 = data.readInt();
                    requestInterruptionFilterFromListener(_arg024, _arg119);
                    reply.writeNoException();
                    return true;
                case 22:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg025 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    int _result8 = getInterruptionFilterFromListener(_arg025);
                    reply.writeNoException();
                    reply.writeInt(_result8);
                    return true;
                case 23:
                    data.enforceInterface(DESCRIPTOR);
                    INotificationListener _arg026 = INotificationListener.Stub.asInterface(data.readStrongBinder());
                    int _arg120 = data.readInt();
                    setOnNotificationPostedTrimFromListener(_arg026, _arg120);
                    reply.writeNoException();
                    return true;
                case 24:
                    data.enforceInterface(DESCRIPTOR);
                    ComponentName _result9 = getEffectsSuppressor();
                    reply.writeNoException();
                    if (_result9 != null) {
                        reply.writeInt(1);
                        _result9.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 25:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg03 = Bundle.CREATOR.createFromParcel(data);
                    } else {
                        _arg03 = null;
                    }
                    boolean _result10 = matchesCallFilter(_arg03);
                    reply.writeNoException();
                    reply.writeInt(_result10 ? 1 : 0);
                    return true;
                case 26:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg027 = data.readString();
                    boolean _result11 = isSystemConditionProviderEnabled(_arg027);
                    reply.writeNoException();
                    reply.writeInt(_result11 ? 1 : 0);
                    return true;
                case 27:
                    data.enforceInterface(DESCRIPTOR);
                    ZenModeConfig _result12 = getZenModeConfig();
                    reply.writeNoException();
                    if (_result12 != null) {
                        reply.writeInt(1);
                        _result12.writeToParcel(reply, 1);
                        return true;
                    }
                    reply.writeInt(0);
                    return true;
                case 28:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg02 = ZenModeConfig.CREATOR.createFromParcel(data);
                    } else {
                        _arg02 = null;
                    }
                    boolean _result13 = setZenModeConfig(_arg02);
                    reply.writeNoException();
                    reply.writeInt(_result13 ? 1 : 0);
                    return true;
                case 29:
                    data.enforceInterface(DESCRIPTOR);
                    String _arg028 = data.readString();
                    IConditionProvider _arg121 = IConditionProvider.Stub.asInterface(data.readStrongBinder());
                    Condition[] _arg210 = (Condition[]) data.createTypedArray(Condition.CREATOR);
                    notifyConditions(_arg028, _arg121, _arg210);
                    return true;
                case 30:
                    data.enforceInterface(DESCRIPTOR);
                    IConditionListener _arg029 = IConditionListener.Stub.asInterface(data.readStrongBinder());
                    int _arg122 = data.readInt();
                    requestZenModeConditions(_arg029, _arg122);
                    return true;
                case 31:
                    data.enforceInterface(DESCRIPTOR);
                    if (data.readInt() != 0) {
                        _arg0 = Condition.CREATOR.createFromParcel(data);
                    } else {
                        _arg0 = null;
                    }
                    setZenModeCondition(_arg0);
                    return true;
                case 32:
                    data.enforceInterface(DESCRIPTOR);
                    Uri[] _arg030 = (Uri[]) data.createTypedArray(Uri.CREATOR);
                    setAutomaticZenModeConditions(_arg030);
                    return true;
                case 33:
                    data.enforceInterface(DESCRIPTOR);
                    Condition[] _result14 = getAutomaticZenModeConditions();
                    reply.writeNoException();
                    reply.writeTypedArray(_result14, 1);
                    return true;
                case IBinder.INTERFACE_TRANSACTION:
                    reply.writeString(DESCRIPTOR);
                    return true;
                default:
                    return super.onTransact(code, data, reply, flags);
            }
        }

        private static class Proxy implements INotificationManager {
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
            public void cancelAllNotifications(String pkg, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(userId);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enqueueToast(String pkg, ITransientNotification callback, int duration) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(duration);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void cancelToast(String pkg, ITransientNotification callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void enqueueNotificationWithTag(String pkg, String opPkg, String tag, int id, Notification notification, int[] idReceived, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeString(opPkg);
                    _data.writeString(tag);
                    _data.writeInt(id);
                    if (notification != null) {
                        _data.writeInt(1);
                        notification.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeIntArray(idReceived);
                    _data.writeInt(userId);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                    _reply.readIntArray(idReceived);
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void cancelNotificationWithTag(String pkg, String tag, int id, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeString(tag);
                    _data.writeInt(id);
                    _data.writeInt(userId);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setNotificationsEnabledForPackage(String pkg, int uid, boolean enabled) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    _data.writeInt(enabled ? 1 : 0);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean areNotificationsEnabledForPackage(String pkg, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPackagePriority(String pkg, int uid, int priority) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    _data.writeInt(priority);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPackagePriority(String pkg, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setPackageVisibilityOverride(String pkg, int uid, int visibility) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    _data.writeInt(visibility);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getPackageVisibilityOverride(String pkg, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeInt(uid);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public StatusBarNotification[] getActiveNotifications(String callingPkg) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    StatusBarNotification[] _result = (StatusBarNotification[]) _reply.createTypedArray(StatusBarNotification.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public StatusBarNotification[] getHistoricalNotifications(String callingPkg, int count) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(callingPkg);
                    _data.writeInt(count);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                    StatusBarNotification[] _result = (StatusBarNotification[]) _reply.createTypedArray(StatusBarNotification.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void registerListener(INotificationListener listener, ComponentName component, int userid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    if (component != null) {
                        _data.writeInt(1);
                        component.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(userid);
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void unregisterListener(INotificationListener listener, int userid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeInt(userid);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void cancelNotificationFromListener(INotificationListener token, String pkg, String tag, int id) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token != null ? token.asBinder() : null);
                    _data.writeString(pkg);
                    _data.writeString(tag);
                    _data.writeInt(id);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void cancelNotificationsFromListener(INotificationListener token, String[] keys) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token != null ? token.asBinder() : null);
                    _data.writeStringArray(keys);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ParceledListSlice getActiveNotificationsFromListener(INotificationListener token, String[] keys, int trim) throws RemoteException {
                ParceledListSlice _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token != null ? token.asBinder() : null);
                    _data.writeStringArray(keys);
                    _data.writeInt(trim);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ParceledListSlice.CREATOR.createFromParcel(_reply);
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
            public void requestHintsFromListener(INotificationListener token, int hints) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token != null ? token.asBinder() : null);
                    _data.writeInt(hints);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getHintsFromListener(INotificationListener token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token != null ? token.asBinder() : null);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void requestInterruptionFilterFromListener(INotificationListener token, int interruptionFilter) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token != null ? token.asBinder() : null);
                    _data.writeInt(interruptionFilter);
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public int getInterruptionFilterFromListener(INotificationListener token) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token != null ? token.asBinder() : null);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void setOnNotificationPostedTrimFromListener(INotificationListener token, int trim) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(token != null ? token.asBinder() : null);
                    _data.writeInt(trim);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ComponentName getEffectsSuppressor() throws RemoteException {
                ComponentName _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ComponentName.CREATOR.createFromParcel(_reply);
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
            public boolean matchesCallFilter(Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public boolean isSystemConditionProviderEnabled(String path) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(path);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public ZenModeConfig getZenModeConfig() throws RemoteException {
                ZenModeConfig _result;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = ZenModeConfig.CREATOR.createFromParcel(_reply);
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
            public boolean setZenModeConfig(ZenModeConfig config) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (config != null) {
                        _data.writeInt(1);
                        config.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    boolean _result = _reply.readInt() != 0;
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            @Override
            public void notifyConditions(String pkg, IConditionProvider provider, Condition[] conditions) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(pkg);
                    _data.writeStrongBinder(provider != null ? provider.asBinder() : null);
                    _data.writeTypedArray(conditions, 0);
                    this.mRemote.transact(29, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void requestZenModeConditions(IConditionListener callback, int relevance) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeInt(relevance);
                    this.mRemote.transact(30, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setZenModeCondition(Condition condition) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (condition != null) {
                        _data.writeInt(1);
                        condition.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(31, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public void setAutomaticZenModeConditions(Uri[] conditionIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedArray(conditionIds, 0);
                    this.mRemote.transact(32, _data, null, 1);
                } finally {
                    _data.recycle();
                }
            }

            @Override
            public Condition[] getAutomaticZenModeConditions() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    Condition[] _result = (Condition[]) _reply.createTypedArray(Condition.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }
    }
}
