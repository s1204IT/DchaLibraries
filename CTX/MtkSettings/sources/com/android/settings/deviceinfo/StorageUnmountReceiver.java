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
        StorageManager storageManager = (StorageManager) context.getSystemService(StorageManager.class);
        String stringExtra = intent.getStringExtra("android.os.storage.extra.VOLUME_ID");
        VolumeInfo volumeInfoFindVolumeById = storageManager.findVolumeById(stringExtra);
        if (volumeInfoFindVolumeById != null) {
            new StorageSettings.UnmountTask(context, volumeInfoFindVolumeById).execute(new Void[0]);
            return;
        }
        Log.w("StorageSettings", "Missing volume " + stringExtra);
    }
}
