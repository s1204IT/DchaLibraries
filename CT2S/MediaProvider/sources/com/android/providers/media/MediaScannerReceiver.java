package com.android.providers.media;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.IOException;

public class MediaScannerReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Uri uri = intent.getData();
        if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
            scan(context, "internal");
            scan(context, "external");
            return;
        }
        if (uri.getScheme().equals("file")) {
            String path = uri.getPath();
            String externalStoragePath = Environment.getExternalStorageDirectory().getPath();
            String legacyPath = Environment.getLegacyExternalStorageDirectory().getPath();
            try {
                String path2 = new File(path).getCanonicalPath();
                if (path2.startsWith(legacyPath)) {
                    path2 = externalStoragePath + path2.substring(legacyPath.length());
                }
                Log.d("MediaScannerReceiver", "action: " + action + " path: " + path2);
                if ("android.intent.action.MEDIA_MOUNTED".equals(action)) {
                    scan(context, "external");
                } else if ("android.intent.action.MEDIA_SCANNER_SCAN_FILE".equals(action) && path2 != null && path2.startsWith(externalStoragePath + "/")) {
                    scanFile(context, path2);
                }
            } catch (IOException e) {
                Log.e("MediaScannerReceiver", "couldn't canonicalize " + path);
            }
        }
    }

    private void scan(Context context, String volume) {
        Bundle args = new Bundle();
        args.putString("volume", volume);
        context.startService(new Intent(context, (Class<?>) MediaScannerService.class).putExtras(args));
    }

    private void scanFile(Context context, String path) {
        Bundle args = new Bundle();
        args.putString("filepath", path);
        context.startService(new Intent(context, (Class<?>) MediaScannerService.class).putExtras(args));
    }
}
