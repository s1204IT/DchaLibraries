package com.mediatek.contacts.interactions;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.mediatek.contacts.simservice.SimProcessorService;
import com.mediatek.contacts.util.Log;

public class ContactDeletionInteractionUtils {
    public static boolean doDeleteSimContact(Context context, Uri uri, Uri uri2, int i, int i2, Fragment fragment) {
        Log.i("ContactDeletionInteractionUtils", "[doDeleteSimContact]simUri: " + uri2 + ",simIndex = " + i + ",subId = " + i2);
        if (uri2 != null && fragment.isAdded()) {
            Intent intent = new Intent(context, (Class<?>) SimProcessorService.class);
            intent.setData(uri2);
            intent.putExtra("sim_index", i);
            intent.putExtra("subscription_key", i2);
            intent.putExtra("work_type", 2);
            intent.putExtra("local_contact_uri", uri);
            context.startService(intent);
            return true;
        }
        return false;
    }
}
