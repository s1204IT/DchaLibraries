package com.android.server.updates;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Telephony;

public class CarrierIdInstallReceiver extends ConfigUpdateInstallReceiver {
    public CarrierIdInstallReceiver() {
        super("/data/misc/carrierid", "carrier_list.pb", "metadata/", "version");
    }

    @Override
    protected void postInstall(Context context, Intent intent) {
        context.getContentResolver().update(Uri.withAppendedPath(Telephony.CarrierId.All.CONTENT_URI, "update_db"), new ContentValues(), null, null);
    }
}
