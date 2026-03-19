package com.android.externalstorage;

import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MountReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ContentProviderClient contentProviderClientAcquireContentProviderClient = context.getContentResolver().acquireContentProviderClient("com.android.externalstorage.documents");
        if (contentProviderClientAcquireContentProviderClient == null) {
            Log.d("MountReceiver", "MountReceiver : onReceive client is null");
            return;
        }
        try {
            ((ExternalStorageProvider) contentProviderClientAcquireContentProviderClient.getLocalContentProvider()).updateVolumes();
        } finally {
            ContentProviderClient.releaseQuietly(contentProviderClientAcquireContentProviderClient);
        }
    }
}
