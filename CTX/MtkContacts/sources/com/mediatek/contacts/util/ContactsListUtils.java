package com.mediatek.contacts.util;

import android.content.Context;
import com.android.contacts.R;

public class ContactsListUtils {
    public static String getBlurb(Context context, String str) {
        if (str == null || str.length() <= 18) {
            return context.getString(R.string.blurbJoinContactDataWith, str);
        }
        return context.getString(R.string.blurbJoinContactDataWith, str.subSequence(0, 18).toString() + "...");
    }
}
