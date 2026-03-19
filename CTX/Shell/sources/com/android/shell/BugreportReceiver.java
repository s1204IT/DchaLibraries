package com.android.shell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.FileUtils;
import android.util.Log;
import java.io.File;

public class BugreportReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("BugreportReceiver", "onReceive(): " + BugreportProgressService.dumpIntent(intent));
        cleanupOldFiles(this, intent, "com.android.internal.intent.action.BUGREPORT_FINISHED", 8, 604800000L);
        Intent intent2 = new Intent(context, (Class<?>) BugreportProgressService.class);
        intent2.putExtra("android.intent.extra.ORIGINAL_INTENT", intent);
        context.startService(intent2);
    }

    static void cleanupOldFiles(BroadcastReceiver broadcastReceiver, Intent intent, String str, final int i, final long j) {
        if (!str.equals(intent.getAction())) {
            return;
        }
        final File fileExtra = BugreportProgressService.getFileExtra(intent, "android.intent.extra.BUGREPORT");
        if (fileExtra == null || !fileExtra.exists()) {
            Log.e("BugreportReceiver", "Not deleting old files because file " + fileExtra + " doesn't exist");
            return;
        }
        final BroadcastReceiver.PendingResult pendingResultGoAsync = broadcastReceiver.goAsync();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voidArr) {
                try {
                    FileUtils.deleteOlderFiles(fileExtra.getParentFile(), i, j);
                } catch (RuntimeException e) {
                    Log.e("BugreportReceiver", "RuntimeException deleting old files", e);
                }
                pendingResultGoAsync.finish();
                return null;
            }
        }.execute(new Void[0]);
    }
}
