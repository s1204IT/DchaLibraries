package com.android.settings.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.util.Log;
import com.android.settings.deviceinfo.StorageSettings;

public class StorageUnmountReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        StorageManager storage = (StorageManager) context.getSystemService(StorageManager.class);
        String volId = intent.getStringExtra("android.os.storage.extra.VOLUME_ID");
        VolumeInfo vol = storage.findVolumeById(volId);
        if (vol != null) {
            new StorageSettings.UnmountTask(context, vol).execute(new Void[0]);
        } else {
            Log.w("StorageSettings", "Missing volume " + volId);
        }
    }
}
