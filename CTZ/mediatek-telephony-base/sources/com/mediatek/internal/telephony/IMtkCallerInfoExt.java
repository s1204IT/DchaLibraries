package com.mediatek.internal.telephony;

import android.content.Context;
import android.database.Cursor;

public interface IMtkCallerInfoExt {
    CharSequence getTypeLabel(Context context, int i, CharSequence charSequence, Cursor cursor);

    CharSequence getTypeLabel(Context context, int i, CharSequence charSequence, Cursor cursor, int i2);
}
