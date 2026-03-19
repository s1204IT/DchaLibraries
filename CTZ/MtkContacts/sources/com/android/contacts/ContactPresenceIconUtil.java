package com.android.contacts;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;
import com.android.contacts.compat.CompatUtils;

public class ContactPresenceIconUtil {
    public static Drawable getPresenceIcon(Context context, int i) {
        switch (i) {
            case 1:
            case 2:
            case 3:
            case CompatUtils.TYPE_ASSERT:
            case 5:
                return context.getResources().getDrawable(ContactsContract.StatusUpdates.getPresenceIconResourceId(i));
            default:
                return null;
        }
    }
}
