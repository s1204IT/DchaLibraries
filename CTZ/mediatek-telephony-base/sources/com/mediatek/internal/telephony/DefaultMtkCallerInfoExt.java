package com.mediatek.internal.telephony;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

public class DefaultMtkCallerInfoExt implements IMtkCallerInfoExt {
    @Override
    public CharSequence getTypeLabel(Context context, int i, CharSequence charSequence, Cursor cursor) {
        return ContactsContract.CommonDataKinds.Phone.getDisplayLabel(context, i, charSequence);
    }

    @Override
    public CharSequence getTypeLabel(Context context, int i, CharSequence charSequence, Cursor cursor, int i2) {
        return ContactsContract.CommonDataKinds.Phone.getDisplayLabel(context, i, charSequence);
    }
}
