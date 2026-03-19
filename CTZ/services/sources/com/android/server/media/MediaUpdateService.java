package com.android.server.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.media.IMediaExtractorUpdateService;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Slog;
import com.android.server.SystemService;
import com.android.server.backup.BackupManagerConstants;
import com.android.server.pm.DumpState;
import com.android.server.pm.Settings;

public class MediaUpdateService extends SystemService {
    private static final String EXTRACTOR_UPDATE_SERVICE_NAME = "media.extractor.update";
    final Handler mHandler;
    private IMediaExtractorUpdateService mMediaExtractorUpdateService;
    private static final String TAG = "MediaUpdateService";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final String MEDIA_UPDATE_PACKAGE_NAME = SystemProperties.get("ro.mediacomponents.package");

    public MediaUpdateService(Context context) {
        super(context);
        this.mHandler = new Handler();
    }

    @Override
    public void onStart() {
        if (("userdebug".equals(Build.TYPE) || "eng".equals(Build.TYPE)) && !TextUtils.isEmpty(MEDIA_UPDATE_PACKAGE_NAME)) {
            connect();
            registerBroadcastReceiver();
        }
    }

    private void connect() {
        IBinder service = ServiceManager.getService(EXTRACTOR_UPDATE_SERVICE_NAME);
        if (service != null) {
            try {
                service.linkToDeath(new IBinder.DeathRecipient() {
                    @Override
                    public void binderDied() {
                        Slog.w(MediaUpdateService.TAG, "mediaextractor died; reconnecting");
                        MediaUpdateService.this.mMediaExtractorUpdateService = null;
                        MediaUpdateService.this.connect();
                    }
                }, 0);
            } catch (Exception e) {
                service = null;
            }
        }
        if (service != null) {
            this.mMediaExtractorUpdateService = IMediaExtractorUpdateService.Stub.asInterface(service);
            this.mHandler.post(new Runnable() {
                @Override
                public void run() {
                    MediaUpdateService.this.packageStateChanged();
                }
            });
        } else {
            Slog.w(TAG, "media.extractor.update not found.");
        }
    }

    private void registerBroadcastReceiver() {
        BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                byte b = 0;
                if (intent.getIntExtra("android.intent.extra.user_handle", 0) != 0) {
                }
                String action = intent.getAction();
                int iHashCode = action.hashCode();
                if (iHashCode != 172491798) {
                    if (iHashCode != 525384130) {
                        b = (iHashCode == 1544582882 && action.equals("android.intent.action.PACKAGE_ADDED")) ? (byte) 2 : (byte) -1;
                    } else if (!action.equals("android.intent.action.PACKAGE_REMOVED")) {
                    }
                } else if (action.equals("android.intent.action.PACKAGE_CHANGED")) {
                    b = 1;
                }
                switch (b) {
                    case 0:
                        if (!intent.getExtras().getBoolean("android.intent.extra.REPLACING")) {
                            MediaUpdateService.this.packageStateChanged();
                            break;
                        }
                        break;
                    case 1:
                        MediaUpdateService.this.packageStateChanged();
                        break;
                    case 2:
                        MediaUpdateService.this.packageStateChanged();
                        break;
                }
            }
        };
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.PACKAGE_ADDED");
        intentFilter.addAction("android.intent.action.PACKAGE_REMOVED");
        intentFilter.addAction("android.intent.action.PACKAGE_CHANGED");
        intentFilter.addDataScheme(Settings.ATTR_PACKAGE);
        intentFilter.addDataSchemeSpecificPart(MEDIA_UPDATE_PACKAGE_NAME, 0);
        getContext().registerReceiverAsUser(broadcastReceiver, UserHandle.ALL, intentFilter, null, null);
    }

    private void packageStateChanged() {
        ApplicationInfo applicationInfo;
        boolean z;
        boolean z2 = false;
        try {
            applicationInfo = getContext().getPackageManager().getApplicationInfo(MEDIA_UPDATE_PACKAGE_NAME, DumpState.DUMP_DEXOPT);
        } catch (Exception e) {
            applicationInfo = null;
        }
        try {
            z = applicationInfo.enabled;
        } catch (Exception e2) {
            Slog.v(TAG, "package '" + MEDIA_UPDATE_PACKAGE_NAME + "' not installed");
            z = false;
        }
        if (applicationInfo != null && Build.VERSION.SDK_INT != applicationInfo.targetSdkVersion) {
            Slog.w(TAG, "This update package is not for this platform version. Ignoring. platform:" + Build.VERSION.SDK_INT + " targetSdk:" + applicationInfo.targetSdkVersion);
        } else {
            z2 = z;
        }
        loadExtractorPlugins((applicationInfo == null || !z2) ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : applicationInfo.sourceDir);
    }

    private void loadExtractorPlugins(String str) {
        try {
            if (this.mMediaExtractorUpdateService != null) {
                this.mMediaExtractorUpdateService.loadPlugins(str);
            }
        } catch (Exception e) {
            Slog.w(TAG, "Error in loadPlugins", e);
        }
    }
}
