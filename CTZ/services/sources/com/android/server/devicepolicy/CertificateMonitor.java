package com.android.server.devicepolicy;

import android.R;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.BenesseExtension;
import android.os.Handler;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.security.Credentials;
import android.security.KeyChain;
import android.util.Log;
import com.android.internal.notification.SystemNotificationChannels;
import com.android.server.devicepolicy.DevicePolicyManagerService;
import com.android.server.pm.DumpState;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

public class CertificateMonitor {
    protected static final String LOG_TAG = "DevicePolicyManager";
    protected static final int MONITORING_CERT_NOTIFICATION_ID = 33;
    private final Handler mHandler;
    private final DevicePolicyManagerService.Injector mInjector;
    private final BroadcastReceiver mRootCaReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (StorageManager.inCryptKeeperBounce()) {
                return;
            }
            CertificateMonitor.this.updateInstalledCertificates(UserHandle.of(intent.getIntExtra("android.intent.extra.user_handle", getSendingUserId())));
        }
    };
    private final DevicePolicyManagerService mService;

    public CertificateMonitor(DevicePolicyManagerService devicePolicyManagerService, DevicePolicyManagerService.Injector injector, Handler handler) {
        this.mService = devicePolicyManagerService;
        this.mInjector = injector;
        this.mHandler = handler;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.USER_STARTED");
        intentFilter.addAction("android.intent.action.USER_UNLOCKED");
        intentFilter.addAction("android.security.action.TRUST_STORE_CHANGED");
        intentFilter.setPriority(1000);
        this.mInjector.mContext.registerReceiverAsUser(this.mRootCaReceiver, UserHandle.ALL, intentFilter, null, this.mHandler);
    }

    public String installCaCert(UserHandle userHandle, byte[] bArr) throws Exception {
        Throwable th;
        Throwable th2;
        try {
            byte[] bArrConvertToPem = Credentials.convertToPem(new Certificate[]{parseCert(bArr)});
            try {
                KeyChain.KeyChainConnection keyChainConnectionKeyChainBindAsUser = this.mInjector.keyChainBindAsUser(userHandle);
                try {
                    String strInstallCaCertificate = keyChainConnectionKeyChainBindAsUser.getService().installCaCertificate(bArrConvertToPem);
                    if (keyChainConnectionKeyChainBindAsUser != null) {
                        $closeResource(null, keyChainConnectionKeyChainBindAsUser);
                    }
                    return strInstallCaCertificate;
                } catch (Throwable th3) {
                    try {
                        throw th3;
                    } catch (Throwable th4) {
                        th = th3;
                        th2 = th4;
                        if (keyChainConnectionKeyChainBindAsUser != null) {
                            throw th2;
                        }
                        $closeResource(th, keyChainConnectionKeyChainBindAsUser);
                        throw th2;
                    }
                }
            } catch (RemoteException e) {
                Log.e(LOG_TAG, "installCaCertsToKeyChain(): ", e);
                return null;
            } catch (InterruptedException e2) {
                Log.w(LOG_TAG, "installCaCertsToKeyChain(): ", e2);
                Thread.currentThread().interrupt();
                return null;
            }
        } catch (IOException | CertificateException e3) {
            Log.e(LOG_TAG, "Problem converting cert", e3);
            return null;
        }
    }

    private static void $closeResource(Throwable th, AutoCloseable autoCloseable) throws Exception {
        if (th == null) {
            autoCloseable.close();
            return;
        }
        try {
            autoCloseable.close();
        } catch (Throwable th2) {
            th.addSuppressed(th2);
        }
    }

    public void uninstallCaCerts(UserHandle userHandle, String[] strArr) throws Exception {
        try {
            KeyChain.KeyChainConnection keyChainConnectionKeyChainBindAsUser = this.mInjector.keyChainBindAsUser(userHandle);
            Throwable th = null;
            for (String str : strArr) {
                try {
                    try {
                        keyChainConnectionKeyChainBindAsUser.getService().deleteCaCertificate(str);
                    } catch (Throwable th2) {
                        th = th2;
                        throw th;
                    }
                } finally {
                    if (keyChainConnectionKeyChainBindAsUser != null) {
                        $closeResource(th, keyChainConnectionKeyChainBindAsUser);
                    }
                }
            }
        } catch (RemoteException e) {
            Log.e(LOG_TAG, "from CaCertUninstaller: ", e);
        } catch (InterruptedException e2) {
            Log.w(LOG_TAG, "CaCertUninstaller: ", e2);
            Thread.currentThread().interrupt();
        }
    }

    public List<String> getInstalledCaCertificates(UserHandle userHandle) throws Exception {
        Throwable th;
        try {
            KeyChain.KeyChainConnection keyChainConnectionKeyChainBindAsUser = this.mInjector.keyChainBindAsUser(userHandle);
            try {
                List<String> list = keyChainConnectionKeyChainBindAsUser.getService().getUserCaAliases().getList();
                if (keyChainConnectionKeyChainBindAsUser != null) {
                    $closeResource(null, keyChainConnectionKeyChainBindAsUser);
                }
                return list;
            } catch (Throwable th2) {
                th = th2;
                th = null;
                if (keyChainConnectionKeyChainBindAsUser != null) {
                }
            }
        } catch (AssertionError e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e2) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public void onCertificateApprovalsChanged(final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public final void run() {
                this.f$0.updateInstalledCertificates(UserHandle.of(i));
            }
        });
    }

    private void updateInstalledCertificates(UserHandle userHandle) {
        if (!this.mInjector.getUserManager().isUserUnlocked(userHandle.getIdentifier())) {
            return;
        }
        try {
            List<String> installedCaCertificates = getInstalledCaCertificates(userHandle);
            this.mService.onInstalledCertificatesChanged(userHandle, installedCaCertificates);
            int size = installedCaCertificates.size() - this.mService.getAcceptedCaCertificates(userHandle).size();
            if (size == 0) {
                this.mInjector.getNotificationManager().cancelAsUser(LOG_TAG, 33, userHandle);
            } else {
                this.mInjector.getNotificationManager().notifyAsUser(LOG_TAG, 33, buildNotification(userHandle, size), userHandle);
            }
        } catch (RemoteException | RuntimeException e) {
            Log.e(LOG_TAG, "Could not retrieve certificates from KeyChain service", e);
        }
    }

    private Notification buildNotification(UserHandle userHandle, int i) {
        String string;
        String string2;
        try {
            Context contextCreateContextAsUser = this.mInjector.createContextAsUser(userHandle);
            Resources resources = this.mInjector.getResources();
            int identifier = userHandle.getIdentifier();
            ComponentName profileOwner = this.mService.getProfileOwner(userHandle.getIdentifier());
            int i2 = R.drawable.pointer_spot_hover_icon;
            if (profileOwner != null) {
                string2 = resources.getString(R.string.mediasize_iso_b10, this.mService.getProfileOwnerName(userHandle.getIdentifier()));
                identifier = this.mService.getProfileParentId(userHandle.getIdentifier());
            } else {
                if (this.mService.getDeviceOwnerUserId() == userHandle.getIdentifier()) {
                    this.mService.getDeviceOwnerName();
                    string = resources.getString(R.string.mediasize_iso_b10, this.mService.getDeviceOwnerName());
                } else {
                    string = resources.getString(R.string.mediasize_iso_b1);
                    i2 = R.drawable.stat_sys_warning;
                }
                string2 = string;
            }
            int i3 = i2;
            Intent intent = new Intent("com.android.settings.MONITORING_CERT_INFO");
            intent.setFlags(268468224);
            intent.putExtra("android.settings.extra.number_of_certificates", i);
            intent.putExtra("android.intent.extra.USER_ID", userHandle.getIdentifier());
            ActivityInfo activityInfoResolveActivityInfo = intent.resolveActivityInfo(this.mInjector.getPackageManager(), DumpState.DUMP_DEXOPT);
            if (activityInfoResolveActivityInfo != null) {
                intent.setComponent(activityInfoResolveActivityInfo.getComponentName());
            }
            PendingIntent pendingIntentPendingIntentGetActivityAsUser = this.mInjector.pendingIntentGetActivityAsUser(contextCreateContextAsUser, 0, intent, 134217728, null, UserHandle.of(identifier));
            if (BenesseExtension.getDchaState() != 0) {
                pendingIntentPendingIntentGetActivityAsUser = null;
            }
            return new Notification.Builder(contextCreateContextAsUser, SystemNotificationChannels.SECURITY).setSmallIcon(i3).setContentTitle(resources.getQuantityText(R.plurals.selected_count, i)).setContentText(string2).setContentIntent(pendingIntentPendingIntentGetActivityAsUser).setShowWhen(false).setColor(R.color.car_colorPrimary).build();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOG_TAG, "Create context as " + userHandle + " failed", e);
            return null;
        }
    }

    private static X509Certificate parseCert(byte[] bArr) throws CertificateException {
        return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(bArr));
    }
}
