package com.android.settings.applications.manageapplications;

import android.app.Fragment;

/* loaded from: classes.dex */
public interface FileViewHolderController {
    void onClick(Fragment fragment);

    void queryStats();

    void setupView(ApplicationViewHolder applicationViewHolder);

    boolean shouldShow();
}
