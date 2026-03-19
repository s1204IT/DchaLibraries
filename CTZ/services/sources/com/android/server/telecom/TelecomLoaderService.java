package com.android.server.telecom;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManagerInternal;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.provider.Settings;
import android.telecom.DefaultDialerManager;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.util.IntArray;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.telephony.SmsApplication;
import com.android.server.LocalServices;
import com.android.server.SystemService;
import com.android.server.pm.UserManagerService;

public class TelecomLoaderService extends SystemService {
    private static final String SERVICE_ACTION = "com.android.ITelecomService";
    private static final ComponentName SERVICE_COMPONENT = new ComponentName("com.android.server.telecom", "com.android.server.telecom.components.TelecomService");
    private static final String TAG = "TelecomLoaderService";
    private final Context mContext;

    @GuardedBy("mLock")
    private IntArray mDefaultDialerAppRequests;

    @GuardedBy("mLock")
    private IntArray mDefaultSimCallManagerRequests;

    @GuardedBy("mLock")
    private IntArray mDefaultSmsAppRequests;
    private final Object mLock;

    @GuardedBy("mLock")
    private TelecomServiceConnection mServiceConnection;

    private class TelecomServiceConnection implements ServiceConnection {
        private TelecomServiceConnection() {
        }

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PhoneAccountHandle simCallManager;
            String defaultDialerApplication;
            ComponentName defaultSmsApplication;
            try {
                iBinder.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        TelecomLoaderService.this.connectToTelecom();
                    }
                }, 0);
                SmsApplication.getDefaultMmsApplication(TelecomLoaderService.this.mContext, false);
                ServiceManager.addService("telecom", iBinder);
                synchronized (TelecomLoaderService.this.mLock) {
                    if (TelecomLoaderService.this.mDefaultSmsAppRequests != null || TelecomLoaderService.this.mDefaultDialerAppRequests != null || TelecomLoaderService.this.mDefaultSimCallManagerRequests != null) {
                        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
                        if (TelecomLoaderService.this.mDefaultSmsAppRequests != null && (defaultSmsApplication = SmsApplication.getDefaultSmsApplication(TelecomLoaderService.this.mContext, true)) != null) {
                            for (int size = TelecomLoaderService.this.mDefaultSmsAppRequests.size() - 1; size >= 0; size--) {
                                int i = TelecomLoaderService.this.mDefaultSmsAppRequests.get(size);
                                TelecomLoaderService.this.mDefaultSmsAppRequests.remove(size);
                                packageManagerInternal.grantDefaultPermissionsToDefaultSmsApp(defaultSmsApplication.getPackageName(), i);
                            }
                        }
                        if (TelecomLoaderService.this.mDefaultDialerAppRequests != null && (defaultDialerApplication = DefaultDialerManager.getDefaultDialerApplication(TelecomLoaderService.this.mContext)) != null) {
                            for (int size2 = TelecomLoaderService.this.mDefaultDialerAppRequests.size() - 1; size2 >= 0; size2--) {
                                int i2 = TelecomLoaderService.this.mDefaultDialerAppRequests.get(size2);
                                TelecomLoaderService.this.mDefaultDialerAppRequests.remove(size2);
                                packageManagerInternal.grantDefaultPermissionsToDefaultDialerApp(defaultDialerApplication, i2);
                            }
                        }
                        if (TelecomLoaderService.this.mDefaultSimCallManagerRequests != null && (simCallManager = ((TelecomManager) TelecomLoaderService.this.mContext.getSystemService("telecom")).getSimCallManager()) != null) {
                            int size3 = TelecomLoaderService.this.mDefaultSimCallManagerRequests.size();
                            String packageName = simCallManager.getComponentName().getPackageName();
                            for (int i3 = size3 - 1; i3 >= 0; i3--) {
                                int i4 = TelecomLoaderService.this.mDefaultSimCallManagerRequests.get(i3);
                                TelecomLoaderService.this.mDefaultSimCallManagerRequests.remove(i3);
                                packageManagerInternal.grantDefaultPermissionsToDefaultSimCallManager(packageName, i4);
                            }
                        }
                    }
                }
            } catch (RemoteException e) {
                Slog.w(TelecomLoaderService.TAG, "Failed linking to death.");
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            TelecomLoaderService.this.connectToTelecom();
        }
    }

    public TelecomLoaderService(Context context) {
        super(context);
        this.mLock = new Object();
        this.mContext = context;
        registerDefaultAppProviders();
    }

    @Override
    public void onStart() {
    }

    @Override
    public void onBootPhase(int i) {
        if (i == 550) {
            registerDefaultAppNotifier();
            registerCarrierConfigChangedReceiver();
            connectToTelecom();
        }
    }

    private void connectToTelecom() {
        synchronized (this.mLock) {
            if (this.mServiceConnection != null) {
                this.mContext.unbindService(this.mServiceConnection);
                this.mServiceConnection = null;
            }
            TelecomServiceConnection telecomServiceConnection = new TelecomServiceConnection();
            Intent intent = new Intent(SERVICE_ACTION);
            intent.setComponent(SERVICE_COMPONENT);
            if (this.mContext.bindServiceAsUser(intent, telecomServiceConnection, 67108929, UserHandle.SYSTEM)) {
                this.mServiceConnection = telecomServiceConnection;
            }
        }
    }

    private void registerDefaultAppProviders() {
        PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        packageManagerInternal.setSmsAppPackagesProvider(new PackageManagerInternal.PackagesProvider() {
            public String[] getPackages(int i) {
                synchronized (TelecomLoaderService.this.mLock) {
                    if (TelecomLoaderService.this.mServiceConnection == null) {
                        if (TelecomLoaderService.this.mDefaultSmsAppRequests == null) {
                            TelecomLoaderService.this.mDefaultSmsAppRequests = new IntArray();
                        }
                        TelecomLoaderService.this.mDefaultSmsAppRequests.add(i);
                        return null;
                    }
                    ComponentName defaultSmsApplication = SmsApplication.getDefaultSmsApplication(TelecomLoaderService.this.mContext, true);
                    if (defaultSmsApplication != null) {
                        return new String[]{defaultSmsApplication.getPackageName()};
                    }
                    return null;
                }
            }
        });
        packageManagerInternal.setDialerAppPackagesProvider(new PackageManagerInternal.PackagesProvider() {
            public String[] getPackages(int i) {
                synchronized (TelecomLoaderService.this.mLock) {
                    if (TelecomLoaderService.this.mServiceConnection == null) {
                        if (TelecomLoaderService.this.mDefaultDialerAppRequests == null) {
                            TelecomLoaderService.this.mDefaultDialerAppRequests = new IntArray();
                        }
                        TelecomLoaderService.this.mDefaultDialerAppRequests.add(i);
                        return null;
                    }
                    String defaultDialerApplication = DefaultDialerManager.getDefaultDialerApplication(TelecomLoaderService.this.mContext);
                    if (defaultDialerApplication != null) {
                        return new String[]{defaultDialerApplication};
                    }
                    return null;
                }
            }
        });
        packageManagerInternal.setSimCallManagerPackagesProvider(new PackageManagerInternal.PackagesProvider() {
            public String[] getPackages(int i) {
                synchronized (TelecomLoaderService.this.mLock) {
                    if (TelecomLoaderService.this.mServiceConnection == null) {
                        if (TelecomLoaderService.this.mDefaultSimCallManagerRequests == null) {
                            TelecomLoaderService.this.mDefaultSimCallManagerRequests = new IntArray();
                        }
                        TelecomLoaderService.this.mDefaultSimCallManagerRequests.add(i);
                        return null;
                    }
                    PhoneAccountHandle simCallManager = ((TelecomManager) TelecomLoaderService.this.mContext.getSystemService("telecom")).getSimCallManager(i);
                    if (simCallManager != null) {
                        return new String[]{simCallManager.getComponentName().getPackageName()};
                    }
                    return null;
                }
            }
        });
    }

    private void registerDefaultAppNotifier() {
        final PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        final Uri uriFor = Settings.Secure.getUriFor("sms_default_application");
        final Uri uriFor2 = Settings.Secure.getUriFor("dialer_default_application");
        ContentObserver contentObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
            @Override
            public void onChange(boolean z, Uri uri, int i) {
                if (uriFor.equals(uri)) {
                    ComponentName defaultSmsApplication = SmsApplication.getDefaultSmsApplication(TelecomLoaderService.this.mContext, true);
                    if (defaultSmsApplication != null) {
                        packageManagerInternal.grantDefaultPermissionsToDefaultSmsApp(defaultSmsApplication.getPackageName(), i);
                        return;
                    }
                    return;
                }
                if (uriFor2.equals(uri)) {
                    String defaultDialerApplication = DefaultDialerManager.getDefaultDialerApplication(TelecomLoaderService.this.mContext);
                    if (defaultDialerApplication != null) {
                        packageManagerInternal.grantDefaultPermissionsToDefaultDialerApp(defaultDialerApplication, i);
                    }
                    TelecomLoaderService.this.updateSimCallManagerPermissions(packageManagerInternal, i);
                }
            }
        };
        this.mContext.getContentResolver().registerContentObserver(uriFor, false, contentObserver, -1);
        this.mContext.getContentResolver().registerContentObserver(uriFor2, false, contentObserver, -1);
    }

    private void registerCarrierConfigChangedReceiver() {
        final PackageManagerInternal packageManagerInternal = (PackageManagerInternal) LocalServices.getService(PackageManagerInternal.class);
        this.mContext.registerReceiverAsUser(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.telephony.action.CARRIER_CONFIG_CHANGED")) {
                    for (int i : UserManagerService.getInstance().getUserIds()) {
                        TelecomLoaderService.this.updateSimCallManagerPermissions(packageManagerInternal, i);
                    }
                }
            }
        }, UserHandle.ALL, new IntentFilter("android.telephony.action.CARRIER_CONFIG_CHANGED"), null, null);
    }

    private void updateSimCallManagerPermissions(PackageManagerInternal packageManagerInternal, int i) {
        PhoneAccountHandle simCallManager = ((TelecomManager) this.mContext.getSystemService("telecom")).getSimCallManager(i);
        if (simCallManager != null) {
            Slog.i(TAG, "updating sim call manager permissions for userId:" + i);
            packageManagerInternal.grantDefaultPermissionsToDefaultSimCallManager(simCallManager.getComponentName().getPackageName(), i);
        }
    }
}
