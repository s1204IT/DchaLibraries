package com.android.internal.statusbar;

import android.content.ComponentName;
import android.graphics.Rect;
import android.hardware.biometrics.IBiometricPromptReceiver;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.statusbar.IStatusBar;
import java.util.ArrayList;
import java.util.List;

public interface IStatusBarService extends IInterface {
    void addTile(ComponentName componentName) throws RemoteException;

    void clearNotificationEffects() throws RemoteException;

    void clickTile(ComponentName componentName) throws RemoteException;

    void collapsePanels() throws RemoteException;

    void disable(int i, IBinder iBinder, String str) throws RemoteException;

    void disable2(int i, IBinder iBinder, String str) throws RemoteException;

    void disable2ForUser(int i, IBinder iBinder, String str, int i2) throws RemoteException;

    void disableForUser(int i, IBinder iBinder, String str, int i2) throws RemoteException;

    void expandNotificationsPanel() throws RemoteException;

    void expandSettingsPanel(String str) throws RemoteException;

    void handleSystemKey(int i) throws RemoteException;

    void hideFingerprintDialog() throws RemoteException;

    void onClearAllNotifications(int i) throws RemoteException;

    void onFingerprintAuthenticated() throws RemoteException;

    void onFingerprintError(String str) throws RemoteException;

    void onFingerprintHelp(String str) throws RemoteException;

    void onGlobalActionsHidden() throws RemoteException;

    void onGlobalActionsShown() throws RemoteException;

    void onNotificationActionClick(String str, int i, NotificationVisibility notificationVisibility) throws RemoteException;

    void onNotificationClear(String str, String str2, int i, int i2, String str3, int i3, NotificationVisibility notificationVisibility) throws RemoteException;

    void onNotificationClick(String str, NotificationVisibility notificationVisibility) throws RemoteException;

    void onNotificationDirectReplied(String str) throws RemoteException;

    void onNotificationError(String str, String str2, int i, int i2, int i3, String str3, int i4) throws RemoteException;

    void onNotificationExpansionChanged(String str, boolean z, boolean z2) throws RemoteException;

    void onNotificationSettingsViewed(String str) throws RemoteException;

    void onNotificationSmartRepliesAdded(String str, int i) throws RemoteException;

    void onNotificationSmartReplySent(String str, int i) throws RemoteException;

    void onNotificationVisibilityChanged(NotificationVisibility[] notificationVisibilityArr, NotificationVisibility[] notificationVisibilityArr2) throws RemoteException;

    void onPanelHidden() throws RemoteException;

    void onPanelRevealed(boolean z, int i) throws RemoteException;

    void reboot(boolean z) throws RemoteException;

    void registerStatusBar(IStatusBar iStatusBar, List<String> list, List<StatusBarIcon> list2, int[] iArr, List<IBinder> list3, Rect rect, Rect rect2) throws RemoteException;

    void remTile(ComponentName componentName) throws RemoteException;

    void removeIcon(String str) throws RemoteException;

    void setIcon(String str, String str2, int i, int i2, String str3) throws RemoteException;

    void setIconVisibility(String str, boolean z) throws RemoteException;

    void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z) throws RemoteException;

    void setSystemUiVisibility(int i, int i2, String str) throws RemoteException;

    void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver iBiometricPromptReceiver) throws RemoteException;

    void showPinningEnterExitToast(boolean z) throws RemoteException;

    void showPinningEscapeToast() throws RemoteException;

    void shutdown() throws RemoteException;

    void togglePanel() throws RemoteException;

    public static abstract class Stub extends Binder implements IStatusBarService {
        private static final String DESCRIPTOR = "com.android.internal.statusbar.IStatusBarService";
        static final int TRANSACTION_addTile = 33;
        static final int TRANSACTION_clearNotificationEffects = 16;
        static final int TRANSACTION_clickTile = 35;
        static final int TRANSACTION_collapsePanels = 2;
        static final int TRANSACTION_disable = 4;
        static final int TRANSACTION_disable2 = 6;
        static final int TRANSACTION_disable2ForUser = 7;
        static final int TRANSACTION_disableForUser = 5;
        static final int TRANSACTION_expandNotificationsPanel = 1;
        static final int TRANSACTION_expandSettingsPanel = 12;
        static final int TRANSACTION_handleSystemKey = 36;
        static final int TRANSACTION_hideFingerprintDialog = 43;
        static final int TRANSACTION_onClearAllNotifications = 20;
        static final int TRANSACTION_onFingerprintAuthenticated = 40;
        static final int TRANSACTION_onFingerprintError = 42;
        static final int TRANSACTION_onFingerprintHelp = 41;
        static final int TRANSACTION_onGlobalActionsHidden = 30;
        static final int TRANSACTION_onGlobalActionsShown = 29;
        static final int TRANSACTION_onNotificationActionClick = 18;
        static final int TRANSACTION_onNotificationClear = 21;
        static final int TRANSACTION_onNotificationClick = 17;
        static final int TRANSACTION_onNotificationDirectReplied = 24;
        static final int TRANSACTION_onNotificationError = 19;
        static final int TRANSACTION_onNotificationExpansionChanged = 23;
        static final int TRANSACTION_onNotificationSettingsViewed = 27;
        static final int TRANSACTION_onNotificationSmartRepliesAdded = 25;
        static final int TRANSACTION_onNotificationSmartReplySent = 26;
        static final int TRANSACTION_onNotificationVisibilityChanged = 22;
        static final int TRANSACTION_onPanelHidden = 15;
        static final int TRANSACTION_onPanelRevealed = 14;
        static final int TRANSACTION_reboot = 32;
        static final int TRANSACTION_registerStatusBar = 13;
        static final int TRANSACTION_remTile = 34;
        static final int TRANSACTION_removeIcon = 10;
        static final int TRANSACTION_setIcon = 8;
        static final int TRANSACTION_setIconVisibility = 9;
        static final int TRANSACTION_setImeWindowStatus = 11;
        static final int TRANSACTION_setSystemUiVisibility = 28;
        static final int TRANSACTION_showFingerprintDialog = 39;
        static final int TRANSACTION_showPinningEnterExitToast = 37;
        static final int TRANSACTION_showPinningEscapeToast = 38;
        static final int TRANSACTION_shutdown = 31;
        static final int TRANSACTION_togglePanel = 3;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IStatusBarService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IStatusBarService)) {
                return (IStatusBarService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            int[] iArr;
            NotificationVisibility notificationVisibilityCreateFromParcel;
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    expandNotificationsPanel();
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    collapsePanels();
                    parcel2.writeNoException();
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    togglePanel();
                    parcel2.writeNoException();
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    disable(parcel.readInt(), parcel.readStrongBinder(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    disableForUser(parcel.readInt(), parcel.readStrongBinder(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    disable2(parcel.readInt(), parcel.readStrongBinder(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    disable2ForUser(parcel.readInt(), parcel.readStrongBinder(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    setIcon(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    setIconVisibility(parcel.readString(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    removeIcon(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    setImeWindowStatus(parcel.readStrongBinder(), parcel.readInt(), parcel.readInt(), parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 12:
                    parcel.enforceInterface(DESCRIPTOR);
                    expandSettingsPanel(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 13:
                    parcel.enforceInterface(DESCRIPTOR);
                    IStatusBar iStatusBarAsInterface = IStatusBar.Stub.asInterface(parcel.readStrongBinder());
                    ArrayList arrayList = new ArrayList();
                    ArrayList arrayList2 = new ArrayList();
                    int i3 = parcel.readInt();
                    if (i3 >= 0) {
                        iArr = new int[i3];
                    } else {
                        iArr = null;
                    }
                    ArrayList arrayList3 = new ArrayList();
                    Rect rect = new Rect();
                    Rect rect2 = new Rect();
                    registerStatusBar(iStatusBarAsInterface, arrayList, arrayList2, iArr, arrayList3, rect, rect2);
                    parcel2.writeNoException();
                    parcel2.writeStringList(arrayList);
                    parcel2.writeTypedList(arrayList2);
                    parcel2.writeIntArray(iArr);
                    parcel2.writeBinderList(arrayList3);
                    parcel2.writeInt(1);
                    rect.writeToParcel(parcel2, 1);
                    parcel2.writeInt(1);
                    rect2.writeToParcel(parcel2, 1);
                    return true;
                case 14:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPanelRevealed(parcel.readInt() != 0, parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 15:
                    parcel.enforceInterface(DESCRIPTOR);
                    onPanelHidden();
                    parcel2.writeNoException();
                    return true;
                case 16:
                    parcel.enforceInterface(DESCRIPTOR);
                    clearNotificationEffects();
                    parcel2.writeNoException();
                    return true;
                case 17:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationClick(parcel.readString(), parcel.readInt() != 0 ? NotificationVisibility.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 18:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationActionClick(parcel.readString(), parcel.readInt(), parcel.readInt() != 0 ? NotificationVisibility.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 19:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationError(parcel.readString(), parcel.readString(), parcel.readInt(), parcel.readInt(), parcel.readInt(), parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 20:
                    parcel.enforceInterface(DESCRIPTOR);
                    onClearAllNotifications(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 21:
                    parcel.enforceInterface(DESCRIPTOR);
                    String string = parcel.readString();
                    String string2 = parcel.readString();
                    int i4 = parcel.readInt();
                    int i5 = parcel.readInt();
                    String string3 = parcel.readString();
                    int i6 = parcel.readInt();
                    if (parcel.readInt() != 0) {
                        notificationVisibilityCreateFromParcel = NotificationVisibility.CREATOR.createFromParcel(parcel);
                    } else {
                        notificationVisibilityCreateFromParcel = null;
                    }
                    onNotificationClear(string, string2, i4, i5, string3, i6, notificationVisibilityCreateFromParcel);
                    parcel2.writeNoException();
                    return true;
                case 22:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationVisibilityChanged((NotificationVisibility[]) parcel.createTypedArray(NotificationVisibility.CREATOR), (NotificationVisibility[]) parcel.createTypedArray(NotificationVisibility.CREATOR));
                    parcel2.writeNoException();
                    return true;
                case 23:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationExpansionChanged(parcel.readString(), parcel.readInt() != 0, parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 24:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationDirectReplied(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 25:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationSmartRepliesAdded(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 26:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationSmartReplySent(parcel.readString(), parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 27:
                    parcel.enforceInterface(DESCRIPTOR);
                    onNotificationSettingsViewed(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 28:
                    parcel.enforceInterface(DESCRIPTOR);
                    setSystemUiVisibility(parcel.readInt(), parcel.readInt(), parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 29:
                    parcel.enforceInterface(DESCRIPTOR);
                    onGlobalActionsShown();
                    parcel2.writeNoException();
                    return true;
                case 30:
                    parcel.enforceInterface(DESCRIPTOR);
                    onGlobalActionsHidden();
                    parcel2.writeNoException();
                    return true;
                case 31:
                    parcel.enforceInterface(DESCRIPTOR);
                    shutdown();
                    parcel2.writeNoException();
                    return true;
                case 32:
                    parcel.enforceInterface(DESCRIPTOR);
                    reboot(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 33:
                    parcel.enforceInterface(DESCRIPTOR);
                    addTile(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 34:
                    parcel.enforceInterface(DESCRIPTOR);
                    remTile(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 35:
                    parcel.enforceInterface(DESCRIPTOR);
                    clickTile(parcel.readInt() != 0 ? ComponentName.CREATOR.createFromParcel(parcel) : null);
                    parcel2.writeNoException();
                    return true;
                case 36:
                    parcel.enforceInterface(DESCRIPTOR);
                    handleSystemKey(parcel.readInt());
                    parcel2.writeNoException();
                    return true;
                case 37:
                    parcel.enforceInterface(DESCRIPTOR);
                    showPinningEnterExitToast(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 38:
                    parcel.enforceInterface(DESCRIPTOR);
                    showPinningEscapeToast();
                    parcel2.writeNoException();
                    return true;
                case 39:
                    parcel.enforceInterface(DESCRIPTOR);
                    showFingerprintDialog(parcel.readInt() != 0 ? Bundle.CREATOR.createFromParcel(parcel) : null, IBiometricPromptReceiver.Stub.asInterface(parcel.readStrongBinder()));
                    parcel2.writeNoException();
                    return true;
                case 40:
                    parcel.enforceInterface(DESCRIPTOR);
                    onFingerprintAuthenticated();
                    parcel2.writeNoException();
                    return true;
                case 41:
                    parcel.enforceInterface(DESCRIPTOR);
                    onFingerprintHelp(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 42:
                    parcel.enforceInterface(DESCRIPTOR);
                    onFingerprintError(parcel.readString());
                    parcel2.writeNoException();
                    return true;
                case 43:
                    parcel.enforceInterface(DESCRIPTOR);
                    hideFingerprintDialog();
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IStatusBarService {
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
            public void expandNotificationsPanel() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(1, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void collapsePanels() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void togglePanel() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void disable(int i, IBinder iBinder, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void disableForUser(int i, IBinder iBinder, String str, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void disable2(int i, IBinder iBinder, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void disable2ForUser(int i, IBinder iBinder, String str, int i2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i2);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setIcon(String str, String str2, int i, int i2, String str3) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str3);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setIconVisibility(String str, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void removeIcon(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setImeWindowStatus(IBinder iBinder, int i, int i2, boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iBinder);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void expandSettingsPanel(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(12, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void registerStatusBar(IStatusBar iStatusBar, List<String> list, List<StatusBarIcon> list2, int[] iArr, List<IBinder> list3, Rect rect, Rect rect2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeStrongBinder(iStatusBar != null ? iStatusBar.asBinder() : null);
                    if (iArr == null) {
                        parcelObtain.writeInt(-1);
                    } else {
                        parcelObtain.writeInt(iArr.length);
                    }
                    this.mRemote.transact(13, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    parcelObtain2.readStringList(list);
                    parcelObtain2.readTypedList(list2, StatusBarIcon.CREATOR);
                    parcelObtain2.readIntArray(iArr);
                    parcelObtain2.readBinderList(list3);
                    if (parcelObtain2.readInt() != 0) {
                        rect.readFromParcel(parcelObtain2);
                    }
                    if (parcelObtain2.readInt() != 0) {
                        rect2.readFromParcel(parcelObtain2);
                    }
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPanelRevealed(boolean z, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(14, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onPanelHidden() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(15, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clearNotificationEffects() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationClick(String str, NotificationVisibility notificationVisibility) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    if (notificationVisibility != null) {
                        parcelObtain.writeInt(1);
                        notificationVisibility.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(17, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationActionClick(String str, int i, NotificationVisibility notificationVisibility) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    if (notificationVisibility != null) {
                        parcelObtain.writeInt(1);
                        notificationVisibility.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(18, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationError(String str, String str2, int i, int i2, int i3, String str3, int i4) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeInt(i3);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i4);
                    this.mRemote.transact(19, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onClearAllNotifications(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(20, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationClear(String str, String str2, int i, int i2, String str3, int i3, NotificationVisibility notificationVisibility) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeString(str2);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str3);
                    parcelObtain.writeInt(i3);
                    if (notificationVisibility != null) {
                        parcelObtain.writeInt(1);
                        notificationVisibility.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(21, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationVisibilityChanged(NotificationVisibility[] notificationVisibilityArr, NotificationVisibility[] notificationVisibilityArr2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeTypedArray(notificationVisibilityArr, 0);
                    parcelObtain.writeTypedArray(notificationVisibilityArr2, 0);
                    this.mRemote.transact(22, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationExpansionChanged(String str, boolean z, boolean z2) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(z ? 1 : 0);
                    parcelObtain.writeInt(z2 ? 1 : 0);
                    this.mRemote.transact(23, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationDirectReplied(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(24, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationSmartRepliesAdded(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(25, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationSmartReplySent(String str, int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(26, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onNotificationSettingsViewed(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(27, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void setSystemUiVisibility(int i, int i2, String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    parcelObtain.writeInt(i2);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(28, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGlobalActionsShown() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(29, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onGlobalActionsHidden() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void shutdown() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(31, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void reboot(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(32, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void addTile(ComponentName componentName) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(33, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void remTile(ComponentName componentName) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(34, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void clickTile(ComponentName componentName) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (componentName != null) {
                        parcelObtain.writeInt(1);
                        componentName.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    this.mRemote.transact(35, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void handleSystemKey(int i) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(i);
                    this.mRemote.transact(36, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showPinningEnterExitToast(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(37, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showPinningEscapeToast() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(38, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void showFingerprintDialog(Bundle bundle, IBiometricPromptReceiver iBiometricPromptReceiver) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (bundle != null) {
                        parcelObtain.writeInt(1);
                        bundle.writeToParcel(parcelObtain, 0);
                    } else {
                        parcelObtain.writeInt(0);
                    }
                    parcelObtain.writeStrongBinder(iBiometricPromptReceiver != null ? iBiometricPromptReceiver.asBinder() : null);
                    this.mRemote.transact(39, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onFingerprintAuthenticated() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(40, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onFingerprintHelp(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(41, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void onFingerprintError(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(42, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void hideFingerprintDialog() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(43, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
