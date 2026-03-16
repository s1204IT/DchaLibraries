package com.android.contacts.common;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.provider.ContactsContract;

public class ContactPresenceIconUtil {
    public static Drawable getPresenceIcon(Context context, int status) {
        switch (status) {
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return context.getResources().getDrawable(ContactsContract.StatusUpdates.getPresenceIconResourceId(status));
            default:
                return null;
        }
    }
}
