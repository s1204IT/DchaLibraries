package com.android.contacts.common;

import android.content.Context;
import android.content.res.Resources;

public class ContactStatusUtil {
    public static String getStatusString(Context context, int presence) {
        Resources resources = context.getResources();
        switch (presence) {
            case 2:
            case 3:
                return resources.getString(com.android.contacts.R.string.status_away);
            case 4:
                return resources.getString(com.android.contacts.R.string.status_busy);
            case 5:
                return resources.getString(com.android.contacts.R.string.status_available);
            default:
                return null;
        }
    }
}
