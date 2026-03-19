package com.android.server.updates;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;

public class ApnDbInstallReceiver extends ConfigUpdateInstallReceiver {
    private static final Uri UPDATE_APN_DB = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "update_db");

    public ApnDbInstallReceiver() {
        super("/data/misc/", "apns-conf.xml", "metadata/", "version");
    }

    @Override
    protected void postInstall(Context context, Intent intent) {
        ContentResolver resolver = context.getContentResolver();
        resolver.delete(UPDATE_APN_DB, null, null);
    }
}
