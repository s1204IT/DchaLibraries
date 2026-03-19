package android.webkit;

import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;

public interface IWebViewUpdateService extends IInterface {
    String changeProviderAndSetting(String str) throws RemoteException;

    void enableFallbackLogic(boolean z) throws RemoteException;

    void enableMultiProcess(boolean z) throws RemoteException;

    WebViewProviderInfo[] getAllWebViewPackages() throws RemoteException;

    PackageInfo getCurrentWebViewPackage() throws RemoteException;

    String getCurrentWebViewPackageName() throws RemoteException;

    WebViewProviderInfo[] getValidWebViewPackages() throws RemoteException;

    boolean isFallbackPackage(String str) throws RemoteException;

    boolean isMultiProcessEnabled() throws RemoteException;

    void notifyRelroCreationCompleted() throws RemoteException;

    WebViewProviderResponse waitForAndGetProvider() throws RemoteException;

    public static abstract class Stub extends Binder implements IWebViewUpdateService {
        private static final String DESCRIPTOR = "android.webkit.IWebViewUpdateService";
        static final int TRANSACTION_changeProviderAndSetting = 3;
        static final int TRANSACTION_enableFallbackLogic = 9;
        static final int TRANSACTION_enableMultiProcess = 11;
        static final int TRANSACTION_getAllWebViewPackages = 5;
        static final int TRANSACTION_getCurrentWebViewPackage = 7;
        static final int TRANSACTION_getCurrentWebViewPackageName = 6;
        static final int TRANSACTION_getValidWebViewPackages = 4;
        static final int TRANSACTION_isFallbackPackage = 8;
        static final int TRANSACTION_isMultiProcessEnabled = 10;
        static final int TRANSACTION_notifyRelroCreationCompleted = 1;
        static final int TRANSACTION_waitForAndGetProvider = 2;

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IWebViewUpdateService asInterface(IBinder iBinder) {
            if (iBinder == null) {
                return null;
            }
            IInterface iInterfaceQueryLocalInterface = iBinder.queryLocalInterface(DESCRIPTOR);
            if (iInterfaceQueryLocalInterface != null && (iInterfaceQueryLocalInterface instanceof IWebViewUpdateService)) {
                return (IWebViewUpdateService) iInterfaceQueryLocalInterface;
            }
            return new Proxy(iBinder);
        }

        @Override
        public IBinder asBinder() {
            return this;
        }

        @Override
        public boolean onTransact(int i, Parcel parcel, Parcel parcel2, int i2) throws RemoteException {
            if (i == 1598968902) {
                parcel2.writeString(DESCRIPTOR);
                return true;
            }
            switch (i) {
                case 1:
                    parcel.enforceInterface(DESCRIPTOR);
                    notifyRelroCreationCompleted();
                    parcel2.writeNoException();
                    return true;
                case 2:
                    parcel.enforceInterface(DESCRIPTOR);
                    WebViewProviderResponse webViewProviderResponseWaitForAndGetProvider = waitForAndGetProvider();
                    parcel2.writeNoException();
                    if (webViewProviderResponseWaitForAndGetProvider != null) {
                        parcel2.writeInt(1);
                        webViewProviderResponseWaitForAndGetProvider.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 3:
                    parcel.enforceInterface(DESCRIPTOR);
                    String strChangeProviderAndSetting = changeProviderAndSetting(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeString(strChangeProviderAndSetting);
                    return true;
                case 4:
                    parcel.enforceInterface(DESCRIPTOR);
                    WebViewProviderInfo[] validWebViewPackages = getValidWebViewPackages();
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(validWebViewPackages, 1);
                    return true;
                case 5:
                    parcel.enforceInterface(DESCRIPTOR);
                    WebViewProviderInfo[] allWebViewPackages = getAllWebViewPackages();
                    parcel2.writeNoException();
                    parcel2.writeTypedArray(allWebViewPackages, 1);
                    return true;
                case 6:
                    parcel.enforceInterface(DESCRIPTOR);
                    String currentWebViewPackageName = getCurrentWebViewPackageName();
                    parcel2.writeNoException();
                    parcel2.writeString(currentWebViewPackageName);
                    return true;
                case 7:
                    parcel.enforceInterface(DESCRIPTOR);
                    PackageInfo currentWebViewPackage = getCurrentWebViewPackage();
                    parcel2.writeNoException();
                    if (currentWebViewPackage != null) {
                        parcel2.writeInt(1);
                        currentWebViewPackage.writeToParcel(parcel2, 1);
                    } else {
                        parcel2.writeInt(0);
                    }
                    return true;
                case 8:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsFallbackPackage = isFallbackPackage(parcel.readString());
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsFallbackPackage ? 1 : 0);
                    return true;
                case 9:
                    parcel.enforceInterface(DESCRIPTOR);
                    enableFallbackLogic(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                case 10:
                    parcel.enforceInterface(DESCRIPTOR);
                    boolean zIsMultiProcessEnabled = isMultiProcessEnabled();
                    parcel2.writeNoException();
                    parcel2.writeInt(zIsMultiProcessEnabled ? 1 : 0);
                    return true;
                case 11:
                    parcel.enforceInterface(DESCRIPTOR);
                    enableMultiProcess(parcel.readInt() != 0);
                    parcel2.writeNoException();
                    return true;
                default:
                    return super.onTransact(i, parcel, parcel2, i2);
            }
        }

        private static class Proxy implements IWebViewUpdateService {
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
            public void notifyRelroCreationCompleted() throws RemoteException {
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
            public WebViewProviderResponse waitForAndGetProvider() throws RemoteException {
                WebViewProviderResponse webViewProviderResponseCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(2, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        webViewProviderResponseCreateFromParcel = WebViewProviderResponse.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        webViewProviderResponseCreateFromParcel = null;
                    }
                    return webViewProviderResponseCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String changeProviderAndSetting(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(3, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public WebViewProviderInfo[] getValidWebViewPackages() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(4, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (WebViewProviderInfo[]) parcelObtain2.createTypedArray(WebViewProviderInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public WebViewProviderInfo[] getAllWebViewPackages() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(5, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return (WebViewProviderInfo[]) parcelObtain2.createTypedArray(WebViewProviderInfo.CREATOR);
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public String getCurrentWebViewPackageName() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(6, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readString();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public PackageInfo getCurrentWebViewPackage() throws RemoteException {
                PackageInfo packageInfoCreateFromParcel;
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(7, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    if (parcelObtain2.readInt() != 0) {
                        packageInfoCreateFromParcel = PackageInfo.CREATOR.createFromParcel(parcelObtain2);
                    } else {
                        packageInfoCreateFromParcel = null;
                    }
                    return packageInfoCreateFromParcel;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isFallbackPackage(String str) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeString(str);
                    this.mRemote.transact(8, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enableFallbackLogic(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(9, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public boolean isMultiProcessEnabled() throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(10, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                    return parcelObtain2.readInt() != 0;
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }

            @Override
            public void enableMultiProcess(boolean z) throws RemoteException {
                Parcel parcelObtain = Parcel.obtain();
                Parcel parcelObtain2 = Parcel.obtain();
                try {
                    parcelObtain.writeInterfaceToken(Stub.DESCRIPTOR);
                    parcelObtain.writeInt(z ? 1 : 0);
                    this.mRemote.transact(11, parcelObtain, parcelObtain2, 0);
                    parcelObtain2.readException();
                } finally {
                    parcelObtain2.recycle();
                    parcelObtain.recycle();
                }
            }
        }
    }
}
