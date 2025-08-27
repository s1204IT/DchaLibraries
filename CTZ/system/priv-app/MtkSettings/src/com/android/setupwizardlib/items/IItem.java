package com.android.setupwizardlib.items;

import android.view.View;

/* loaded from: classes.dex */
public interface IItem {
    int getLayoutResource();

    boolean isEnabled();

    void onBindView(View view);
}
