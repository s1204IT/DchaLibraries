package com.android.launcher3.allapps;

import android.view.KeyEvent;

/* loaded from: classes.dex */
public interface SearchUiManager {
    void initialize(AllAppsContainerView allAppsContainerView);

    void preDispatchKeyEvent(KeyEvent keyEvent);

    void resetSearch();
}
