package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import java.io.File;
import java.io.IOException;

public class MediaScannerReceiver extends BroadcastReceiver {
    private static Handler sHandler;
    private static boolean sIsBootComplete = false;
    static boolean sIsShutdown = !"def_value".equals(SystemProperties.get("sys.shutdown.requested", "def_value"));

    @Override
    public void onReceive(Context context, Intent intent) {
        Uri data;
        String action = intent.getAction();
        Log.v("MediaScannerReceiver", "onReceive action = " + action);
        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            Log.v("MediaScannerReceiver", "onReceive BOOT_COMPLETED,begin to scan internal and external storage.");
            scan(context, "internal");
            scanUntilAllStorageMounted(context);
            sIsBootComplete = true;
            SharedPreferences.Editor editorEdit = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editorEdit.putBoolean("dev.mount_before_boot", false);
            editorEdit.commit();
            sIsShutdown = false;
            return;
        }
        if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
            scanTranslatable(context);
            return;
        }
        if (action.equals("android.intent.action.ACTION_SHUTDOWN") || action.equals("mediatek.intent.action.ACTION_SHUTDOWN_IPO")) {
            sIsShutdown = true;
            SharedPreferences.Editor editorEdit2 = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editorEdit2.putBoolean("dev.mount_before_boot", true);
            editorEdit2.commit();
            return;
        }
        if (action.equals("mediatek.intent.action.OVERTIME_REMOVAL")) {
            context.getContentResolver().call(MediaStore.Files.getContentUri("external"), "action_remove_overtime", (String) null, (Bundle) null);
            return;
        }
        if (!sIsShutdown && (data = intent.getData()) != null && data.getScheme().equals("file")) {
            String path = data.getPath();
            String path2 = Environment.getExternalStorageDirectory().getPath();
            String path3 = Environment.getLegacyExternalStorageDirectory().getPath();
            try {
                String canonicalPath = new File(path).getCanonicalPath();
                if (canonicalPath.startsWith(path3)) {
                    canonicalPath = path2 + canonicalPath.substring(path3.length());
                }
                Log.d("MediaScannerReceiver", "action: " + action + " path: " + canonicalPath);
                if ("android.intent.action.MEDIA_MOUNTED".equals(action)) {
                    if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean("dev.mount_before_boot", true) && !sIsBootComplete) {
                        Log.v("MediaScannerReceiver", "Mounted before boot completed with path: " + canonicalPath);
                        return;
                    }
                    scanUntilAllStorageMounted(context);
                    return;
                }
                if ("android.intent.action.MEDIA_SCANNER_SCAN_FILE".equals(action) && isInScanDirectory(context, canonicalPath)) {
                    scanFile(context, canonicalPath);
                }
            } catch (IOException e) {
                Log.e("MediaScannerReceiver", "couldn't canonicalize " + path);
            }
        }
    }

    private void scan(Context context, String str) {
        Bundle bundle = new Bundle();
        bundle.putString("volume", str);
        context.startService(new Intent(context, (Class<?>) MediaScannerService.class).putExtras(bundle));
    }

    private void scanFile(Context context, String str) {
        Bundle bundle = new Bundle();
        bundle.putString("filepath", str);
        context.startService(new Intent(context, (Class<?>) MediaScannerService.class).putExtras(bundle));
    }

    private void scanTranslatable(Context context) {
        Bundle bundle = new Bundle();
        bundle.putBoolean("update_titles", true);
        context.startService(new Intent(context, (Class<?>) MediaScannerService.class).putExtras(bundle));
    }

    private void scanUntilAllStorageMounted(Context context) {
        Handler handler = getHandler();
        handler.removeCallbacksAndMessages(null);
        handler.obtainMessage(10, 0, 0, context).sendToTarget();
    }

    private boolean isInScanDirectory(Context context, String str) {
        if (str == null) {
            Log.w("MediaScannerReceiver", "scan path is null");
            return false;
        }
        String[] volumePaths = ((StorageManager) context.getSystemService("storage")).getVolumePaths();
        if (volumePaths == null || volumePaths.length == 0) {
            Log.w("MediaScannerReceiver", "there are no valid directores");
            return false;
        }
        for (String str2 : volumePaths) {
            if (str.startsWith(str2)) {
                return true;
            }
        }
        Log.w("MediaScannerReceiver", "invalid scan path " + str + ", not in any directories");
        return false;
    }

    private boolean isAllStorageMounted(Context context) {
        for (StorageVolume storageVolume : ((StorageManager) context.getSystemService("storage")).getVolumeList()) {
            String path = storageVolume.getPath();
            String state = storageVolume.getState();
            Log.v("MediaScannerReceiver", "isAllStorageMounted: path = " + path + ", state = " + state);
            if ("unmounted".equals(state) || "checking".equals(state)) {
                return false;
            }
        }
        return true;
    }

    private Handler getHandler() {
        if (sHandler == null) {
            sHandler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    Context context = (Context) message.obj;
                    int i = message.arg1;
                    Log.v("MediaScannerReceiver", "Check whether all storage mounted,have waited " + i + "ms");
                    if (10 == message.what) {
                        if (i > 5000 || MediaScannerReceiver.this.isAllStorageMounted(context)) {
                            Log.v("MediaScannerReceiver", "All storages have mountedor check time out, begin to scan.");
                            MediaScannerReceiver.this.scan(context, "external");
                            removeCallbacksAndMessages(null);
                            Handler unused = MediaScannerReceiver.sHandler = null;
                            return;
                        }
                        Log.v("MediaScannerReceiver", "Some storage has not been mounted, wait it mounted until time out.");
                        sendMessageDelayed(obtainMessage(message.what, i + 1000, -1, message.obj), 1000L);
                    }
                }
            };
        }
        return sHandler;
    }
}
