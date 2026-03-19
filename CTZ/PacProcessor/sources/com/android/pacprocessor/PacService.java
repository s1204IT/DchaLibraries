package com.android.pacprocessor;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import com.android.net.IProxyService;
import java.net.MalformedURLException;
import java.net.URL;

public class PacService extends Service {
    private PacNative mPacNative;
    private ProxyServiceStub mStub;

    @Override
    public void onCreate() {
        super.onCreate();
        if (this.mPacNative == null) {
            this.mPacNative = new PacNative();
            this.mStub = new ProxyServiceStub(this.mPacNative);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.mPacNative != null) {
            this.mPacNative.stopPacSupport();
            this.mPacNative = null;
            this.mStub = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        if (this.mPacNative == null) {
            this.mPacNative = new PacNative();
            this.mStub = new ProxyServiceStub(this.mPacNative);
        }
        return this.mStub;
    }

    private static class ProxyServiceStub extends IProxyService.Stub {
        private final PacNative mPacNative;

        public ProxyServiceStub(PacNative pacNative) {
            this.mPacNative = pacNative;
        }

        public String resolvePacFile(String str, String str2) throws RemoteException {
            try {
                if (str == null) {
                    throw new IllegalArgumentException("The host must not be null");
                }
                if (str2 == null) {
                    throw new IllegalArgumentException("The URL must not be null");
                }
                new URL(str2);
                for (char c : str.toCharArray()) {
                    if (!Character.isLetterOrDigit(c) && c != '.' && c != '-') {
                        throw new IllegalArgumentException("Invalid host was passed");
                    }
                }
                return this.mPacNative.makeProxyRequest(str2, str);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("Invalid URL was passed");
            }
        }

        public void setPacFile(String str) throws RemoteException {
            if (Binder.getCallingUid() != 1000) {
                Log.e("PacService", "Only system user is allowed to call setPacFile");
                throw new SecurityException();
            }
            this.mPacNative.setCurrentProxyScript(str);
        }

        public void startPacSystem() throws RemoteException {
            if (Binder.getCallingUid() != 1000) {
                Log.e("PacService", "Only system user is allowed to call startPacSystem");
                throw new SecurityException();
            }
            this.mPacNative.startPacSupport();
        }

        public void stopPacSystem() throws RemoteException {
            if (Binder.getCallingUid() != 1000) {
                Log.e("PacService", "Only system user is allowed to call stopPacSystem");
                throw new SecurityException();
            }
            this.mPacNative.stopPacSupport();
        }
    }
}
