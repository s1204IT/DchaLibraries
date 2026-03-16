package com.android.contacts.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

public class ClipboardUtils {
    public static void copyText(Context context, CharSequence label, CharSequence text, boolean showToast) {
        if (!TextUtils.isEmpty(text)) {
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService("clipboard");
            if (label == null) {
                label = "";
            }
            ClipData clipData = ClipData.newPlainText(label, text);
            clipboardManager.setPrimaryClip(clipData);
            if (showToast) {
                String toastText = context.getString(com.android.contacts.R.string.toast_text_copied);
                Toast.makeText(context, toastText, 0).show();
            }
        }
    }
}
