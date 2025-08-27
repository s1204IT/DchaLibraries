package com.android.systemui;

import android.content.Context;
import android.widget.Toast;

/* loaded from: classes.dex */
public class SysUIToast {
    public static Toast makeText(Context context, int i, int i2) {
        return makeText(context, context.getString(i), i2);
    }

    public static Toast makeText(Context context, CharSequence charSequence, int i) {
        Toast toastMakeText = Toast.makeText(context, charSequence, i);
        toastMakeText.getWindowParams().privateFlags |= 16;
        return toastMakeText;
    }
}
