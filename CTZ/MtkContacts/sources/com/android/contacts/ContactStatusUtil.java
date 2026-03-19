package com.android.contacts;

import android.content.Context;
import android.content.res.Resources;
import com.android.contacts.compat.CompatUtils;

public class ContactStatusUtil {
    public static String getStatusString(Context context, int i) {
        Resources resources = context.getResources();
        switch (i) {
            case 2:
            case 3:
                return resources.getString(R.string.status_away);
            case CompatUtils.TYPE_ASSERT:
                return resources.getString(R.string.status_busy);
            case 5:
                return resources.getString(R.string.status_available);
            default:
                return null;
        }
    }
}
