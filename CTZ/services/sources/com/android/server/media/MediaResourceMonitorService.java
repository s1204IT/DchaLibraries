package com.android.server.media;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.media.IMediaResourceMonitor;
import android.os.Binder;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.util.Slog;
import com.android.server.SystemService;

public class MediaResourceMonitorService extends SystemService {
    private static final String SERVICE_NAME = "media_resource_monitor";
    private final MediaResourceMonitorImpl mMediaResourceMonitorImpl;
    private static final String TAG = "MediaResourceMonitor";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);

    public MediaResourceMonitorService(Context context) {
        super(context);
        this.mMediaResourceMonitorImpl = new MediaResourceMonitorImpl();
    }

    @Override
    public void onStart() {
        publishBinderService(SERVICE_NAME, this.mMediaResourceMonitorImpl);
    }

    class MediaResourceMonitorImpl extends IMediaResourceMonitor.Stub {
        MediaResourceMonitorImpl() {
        }

        public void notifyResourceGranted(int i, int i2) throws RemoteException {
            if (MediaResourceMonitorService.DEBUG) {
                Slog.d(MediaResourceMonitorService.TAG, "notifyResourceGranted(pid=" + i + ", type=" + i2 + ")");
            }
            long jClearCallingIdentity = Binder.clearCallingIdentity();
            try {
                String[] packageNamesFromPid = getPackageNamesFromPid(i);
                if (packageNamesFromPid == null) {
                    return;
                }
                int[] enabledProfileIds = ((UserManager) MediaResourceMonitorService.this.getContext().getSystemService("user")).getEnabledProfileIds(ActivityManager.getCurrentUser());
                if (enabledProfileIds != null && enabledProfileIds.length != 0) {
                    Intent intent = new Intent("android.intent.action.MEDIA_RESOURCE_GRANTED");
                    intent.putExtra("android.intent.extra.PACKAGES", packageNamesFromPid);
                    intent.putExtra("android.intent.extra.MEDIA_RESOURCE_TYPE", i2);
                    for (int i3 : enabledProfileIds) {
                        MediaResourceMonitorService.this.getContext().sendBroadcastAsUser(intent, UserHandle.of(i3), "android.permission.RECEIVE_MEDIA_RESOURCE_USAGE");
                    }
                }
            } finally {
                Binder.restoreCallingIdentity(jClearCallingIdentity);
            }
        }

        private String[] getPackageNamesFromPid(int i) {
            try {
                for (ActivityManager.RunningAppProcessInfo runningAppProcessInfo : ActivityManager.getService().getRunningAppProcesses()) {
                    if (runningAppProcessInfo.pid == i) {
                        return runningAppProcessInfo.pkgList;
                    }
                }
                return null;
            } catch (RemoteException e) {
                Slog.w(MediaResourceMonitorService.TAG, "ActivityManager.getRunningAppProcesses() failed");
                return null;
            }
        }
    }
}
