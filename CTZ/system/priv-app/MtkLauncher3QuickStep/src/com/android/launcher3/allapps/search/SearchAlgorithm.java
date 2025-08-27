package com.android.launcher3.allapps.search;

import com.android.launcher3.allapps.search.AllAppsSearchBarController;

/* loaded from: classes.dex */
public interface SearchAlgorithm {
    void cancel(boolean z);

    void doSearch(String str, AllAppsSearchBarController.Callbacks callbacks);
}
