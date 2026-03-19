package com.mediatek.contacts.aas;

import android.content.Context;
import android.widget.Toast;

public class ToastHelper {
    private Context mContext;
    private Toast mToast = null;

    public ToastHelper(Context context) {
        this.mContext = null;
        if (context == null) {
            throw new IllegalArgumentException();
        }
        this.mContext = context;
    }

    public void showToast(String str) {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this.mContext, str, 0);
        } else {
            this.mToast.setText(str);
        }
        this.mToast.show();
    }

    public void showToast(int i) {
        if (this.mToast == null) {
            this.mToast = Toast.makeText(this.mContext, i, 0);
        } else {
            this.mToast.setText(i);
        }
        this.mToast.show();
    }
}
