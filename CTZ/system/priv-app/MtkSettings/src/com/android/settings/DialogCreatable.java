package com.android.settings;

import android.app.Dialog;

/* loaded from: classes.dex */
public interface DialogCreatable {
    int getDialogMetricsCategory(int i);

    Dialog onCreateDialog(int i);
}
