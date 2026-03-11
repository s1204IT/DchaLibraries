package com.android.browser;

import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

public class OpenDownloadReceiver extends BroadcastReceiver {
    private static Handler sAsyncHandler;

    static {
        HandlerThread thr = new HandlerThread("Open browser download async");
        thr.start();
        sAsyncHandler = new Handler(thr.getLooper());
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (!"android.intent.action.DOWNLOAD_NOTIFICATION_CLICKED".equals(action)) {
            openDownloadsPage(context);
            return;
        }
        long[] ids = intent.getLongArrayExtra("extra_click_download_ids");
        if (ids == null || ids.length == 0) {
            openDownloadsPage(context);
            return;
        }
        final long id = ids[0];
        final BroadcastReceiver.PendingResult result = goAsync();
        Runnable worker = new Runnable() {
            @Override
            public void run() {
                OpenDownloadReceiver.this.onReceiveAsync(context, id);
                result.finish();
            }
        };
        sAsyncHandler.post(worker);
    }

    public void onReceiveAsync(Context context, long id) {
        DownloadManager manager = (DownloadManager) context.getSystemService("download");
        Uri uri = manager.getUriForDownloadedFile(id);
        if (uri == null) {
            openDownloadsPage(context);
            return;
        }
        Intent launchIntent = new Intent("android.intent.action.VIEW");
        launchIntent.setDataAndType(uri, manager.getMimeTypeForDownloadedFile(id));
        launchIntent.setFlags(268435456);
        try {
            context.startActivity(launchIntent);
        } catch (ActivityNotFoundException e) {
            openDownloadsPage(context);
        }
    }

    private void openDownloadsPage(Context context) {
        Intent pageView = new Intent("android.intent.action.VIEW_DOWNLOADS");
        pageView.setFlags(268435456);
        try {
            context.startActivity(pageView);
        } catch (ActivityNotFoundException ex) {
            ex.printStackTrace();
        }
    }
}
