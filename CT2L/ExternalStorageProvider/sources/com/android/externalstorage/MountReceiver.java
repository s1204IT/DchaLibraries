package com.android.externalstorage;

import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;

public class MountReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        ContentProviderClient client = context.getContentResolver().acquireContentProviderClient("com.android.externalstorage.documents");
        try {
            ((ExternalStorageProvider) client.getLocalContentProvider()).updateVolumes();
        } finally {
            ContentProviderClient.releaseQuietly(client);
        }
    }
}
