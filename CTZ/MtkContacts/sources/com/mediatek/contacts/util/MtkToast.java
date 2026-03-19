package com.mediatek.contacts.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.mediatek.contacts.ContactsApplicationEx;

public final class MtkToast {
    private static Toast sToast = null;

    public static void toast(Context context, String str) {
        toast(context, str, 0);
    }

    public static void toast(Context context, int i) {
        toast(context, i, 0);
    }

    public static void toast(Context context, int i, int i2) {
        toast(context, context.getResources().getString(i), 0);
    }

    public static void toast(Context context, String str, int i) {
        getToast(context, str, i).show();
    }

    private static Toast getToast(Context context, String str, int i) {
        if (sToast == null) {
            sToast = Toast.makeText(context.getApplicationContext(), "MTKToast", i);
        }
        sToast.setText(str);
        sToast.setDuration(i);
        return sToast;
    }

    public static void toastFromNoneUiThread(final int i) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ContactsApplicationEx.getContactsApplication(), i, 0).show();
            }
        });
    }
}
