package com.android.managedprovisioning;

import android.app.AlarmManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import com.android.internal.app.LocalePicker;
import com.android.managedprovisioning.DeviceOwnerProvisioningActivity;
import com.android.managedprovisioning.task.AddWifiNetworkTask;
import com.android.managedprovisioning.task.DeleteNonRequiredAppsTask;
import com.android.managedprovisioning.task.DownloadPackageTask;
import com.android.managedprovisioning.task.InstallPackageTask;
import com.android.managedprovisioning.task.SetDevicePolicyTask;
import java.util.Locale;

public class DeviceOwnerProvisioningService extends Service {
    private AddWifiNetworkTask mAddWifiNetworkTask;
    private DeleteNonRequiredAppsTask mDeleteNonRequiredAppsTask;
    private DownloadPackageTask mDownloadPackageTask;
    private BroadcastReceiver mIndirectHomeReceiver;
    private InstallPackageTask mInstallPackageTask;
    private ProvisioningParams mParams;
    private SetDevicePolicyTask mSetDevicePolicyTask;
    private boolean mProvisioningInFlight = false;
    private int mLastProgressMessage = -1;
    private int mLastErrorMessage = -1;
    private boolean mDone = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        synchronized (this) {
            if (this.mProvisioningInFlight) {
                sendProgressUpdateToActivity();
                if (this.mLastErrorMessage >= 0) {
                    sendError();
                }
                if (this.mDone) {
                    onProvisioningSuccess(this.mParams.mDeviceAdminPackageName);
                }
            } else {
                this.mProvisioningInFlight = true;
                progressUpdate(R.string.progress_data_process);
                this.mParams = (ProvisioningParams) intent.getParcelableExtra("ProvisioningParams");
                registerHomeIntentReceiver();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        DeviceOwnerProvisioningService.this.initializeProvisioningEnvironment(DeviceOwnerProvisioningService.this.mParams);
                        DeviceOwnerProvisioningService.this.startDeviceOwnerProvisioning(DeviceOwnerProvisioningService.this.mParams);
                    }
                }).start();
            }
        }
        return 2;
    }

    private void registerHomeIntentReceiver() {
        this.mIndirectHomeReceiver = new IndirectHomeReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.android.managedprovisioning.home_indirect");
        LocalBroadcastManager.getInstance(this).registerReceiver(this.mIndirectHomeReceiver, filter);
    }

    class IndirectHomeReceiver extends BroadcastReceiver {
        IndirectHomeReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (DeviceOwnerProvisioningService.this.mDone) {
                PackageManager pm = DeviceOwnerProvisioningService.this.getPackageManager();
                pm.setComponentEnabledSetting(new ComponentName(DeviceOwnerProvisioningService.this, (Class<?>) HomeReceiverActivity.class), 2, 1);
                Intent result = new Intent("android.app.action.PROFILE_PROVISIONING_COMPLETE");
                result.setPackage(DeviceOwnerProvisioningService.this.mParams.mDeviceAdminPackageName);
                result.addFlags(268435488);
                if (DeviceOwnerProvisioningService.this.mParams.mAdminExtrasBundle != null) {
                    result.putExtra("android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE", DeviceOwnerProvisioningService.this.mParams.mAdminExtrasBundle);
                }
                DeviceOwnerProvisioningService.this.sendBroadcast(result);
                DeviceOwnerProvisioningService.this.stopSelf();
            }
        }
    }

    private void startDeviceOwnerProvisioning(final ProvisioningParams params) {
        if (TextUtils.isEmpty(params.mWifiSsid)) {
            this.mAddWifiNetworkTask = null;
        } else {
            this.mAddWifiNetworkTask = new AddWifiNetworkTask(this, params.mWifiSsid, params.mWifiHidden, params.mWifiSecurityType, params.mWifiPassword, params.mWifiProxyHost, params.mWifiProxyPort, params.mWifiProxyBypassHosts, params.mWifiPacUrl, new AddWifiNetworkTask.Callback() {
                @Override
                public void onSuccess() {
                    if (!TextUtils.isEmpty(params.mDeviceAdminPackageDownloadLocation)) {
                        DeviceOwnerProvisioningService.this.progressUpdate(R.string.progress_download);
                        DeviceOwnerProvisioningService.this.mDownloadPackageTask.run();
                    } else {
                        DeviceOwnerProvisioningService.this.progressUpdate(R.string.progress_set_owner);
                        DeviceOwnerProvisioningService.this.mSetDevicePolicyTask.run();
                    }
                }

                @Override
                public void onError() {
                    DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_wifi);
                }
            });
        }
        this.mDownloadPackageTask = new DownloadPackageTask(this, params.mDeviceAdminPackageDownloadLocation, params.mDeviceAdminPackageChecksum, params.mDeviceAdminPackageDownloadCookieHeader, new DownloadPackageTask.Callback() {
            @Override
            public void onSuccess() {
                String downloadLocation = DeviceOwnerProvisioningService.this.mDownloadPackageTask.getDownloadedPackageLocation();
                DeviceOwnerProvisioningService.this.progressUpdate(R.string.progress_install);
                DeviceOwnerProvisioningService.this.mInstallPackageTask.run(downloadLocation);
            }

            @Override
            public void onError(int errorCode) {
                switch (errorCode) {
                    case 0:
                        DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_hash_mismatch);
                        break;
                    case 1:
                        DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_download_failed);
                        break;
                    default:
                        DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_general);
                        break;
                }
            }
        });
        this.mInstallPackageTask = new InstallPackageTask(this, params.mDeviceAdminPackageName, new InstallPackageTask.Callback() {
            @Override
            public void onSuccess() {
                DeviceOwnerProvisioningService.this.progressUpdate(R.string.progress_set_owner);
                DeviceOwnerProvisioningService.this.mSetDevicePolicyTask.run();
            }

            @Override
            public void onError(int errorCode) {
                switch (errorCode) {
                    case 0:
                        DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_package_invalid);
                        break;
                    case 1:
                        DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_installation_failed);
                        break;
                    default:
                        DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_general);
                        break;
                }
            }
        });
        this.mSetDevicePolicyTask = new SetDevicePolicyTask(this, params.mDeviceAdminPackageName, getResources().getString(R.string.default_owned_device_username), new SetDevicePolicyTask.Callback() {
            @Override
            public void onSuccess() {
                if (params.mLeaveAllSystemAppsEnabled) {
                    DeviceOwnerProvisioningService.this.onProvisioningSuccess(params.mDeviceAdminPackageName);
                } else {
                    DeviceOwnerProvisioningService.this.mDeleteNonRequiredAppsTask.run();
                }
            }

            @Override
            public void onError(int errorCode) {
                switch (errorCode) {
                    case 0:
                        DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_package_not_installed);
                        break;
                    case 1:
                        DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_package_invalid);
                        break;
                    default:
                        DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_general);
                        break;
                }
            }
        });
        this.mDeleteNonRequiredAppsTask = new DeleteNonRequiredAppsTask(this, params.mDeviceAdminPackageName, 0, R.array.required_apps_managed_device, R.array.vendor_required_apps_managed_device, true, false, new DeleteNonRequiredAppsTask.Callback() {
            @Override
            public void onSuccess() {
                DeviceOwnerProvisioningService.this.onProvisioningSuccess(params.mDeviceAdminPackageName);
            }

            @Override
            public void onError() {
                DeviceOwnerProvisioningService.this.error(R.string.device_owner_error_general);
            }
        });
        startFirstTask(params);
    }

    private void startFirstTask(ProvisioningParams params) {
        if (this.mAddWifiNetworkTask != null) {
            progressUpdate(R.string.progress_connect_to_wifi);
            this.mAddWifiNetworkTask.run();
        } else if (!TextUtils.isEmpty(params.mDeviceAdminPackageDownloadLocation)) {
            progressUpdate(R.string.progress_download);
            this.mDownloadPackageTask.run();
        } else {
            progressUpdate(R.string.progress_set_owner);
            this.mSetDevicePolicyTask.run();
        }
    }

    private void error(int dialogMessage) {
        this.mLastErrorMessage = dialogMessage;
        sendError();
    }

    private void sendError() {
        Intent intent = new Intent("com.android.managedprovisioning.error");
        intent.setClass(this, DeviceOwnerProvisioningActivity.ServiceMessageReceiver.class);
        intent.putExtra("UserVisibleErrorMessage-Id", this.mLastErrorMessage);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void progressUpdate(int progressMessage) {
        this.mLastProgressMessage = progressMessage;
        sendProgressUpdateToActivity();
    }

    private void sendProgressUpdateToActivity() {
        Intent intent = new Intent("com.android.managedprovisioning.progress_update");
        intent.putExtra("ProgressMessageId", this.mLastProgressMessage);
        intent.setClass(this, DeviceOwnerProvisioningActivity.ServiceMessageReceiver.class);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void onProvisioningSuccess(String deviceAdminPackage) {
        this.mDone = true;
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, (Class<?>) HomeReceiverActivity.class), 1, 1);
        Intent successIntent = new Intent("com.android.managedprovisioning.provisioning_success");
        successIntent.setClass(this, DeviceOwnerProvisioningActivity.ServiceMessageReceiver.class);
        LocalBroadcastManager.getInstance(this).sendBroadcast(successIntent);
    }

    private void initializeProvisioningEnvironment(ProvisioningParams params) {
        setTimeAndTimezone(params.mTimeZone, params.mLocalTime);
        setLocale(params.mLocale);
        Intent intent = new Intent("com.android.phone.PERFORM_CDMA_PROVISIONING");
        intent.setFlags(268435456);
        startActivity(intent);
    }

    private void setTimeAndTimezone(String timeZone, long localTime) {
        try {
            AlarmManager am = (AlarmManager) getSystemService("alarm");
            if (timeZone != null) {
                am.setTimeZone(timeZone);
            }
            if (localTime > 0) {
                am.setTime(localTime);
            }
        } catch (Exception e) {
            ProvisionLogger.loge("Alarm manager failed to set the system time/timezone.");
        }
    }

    private void setLocale(Locale locale) {
        if (locale != null && !locale.equals(Locale.getDefault())) {
            try {
                LocalePicker.updateLocale(locale);
            } catch (Exception e) {
                ProvisionLogger.loge("Failed to set the system locale.");
            }
        }
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDestroy() {
        if (this.mAddWifiNetworkTask != null) {
            this.mAddWifiNetworkTask.cleanUp();
        }
        if (this.mDownloadPackageTask != null) {
            this.mDownloadPackageTask.cleanUp();
        }
        if (this.mIndirectHomeReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(this.mIndirectHomeReceiver);
            this.mIndirectHomeReceiver = null;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
