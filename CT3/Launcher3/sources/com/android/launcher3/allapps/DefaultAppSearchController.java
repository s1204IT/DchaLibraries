package com.android.launcher3.allapps;

public class DefaultAppSearchController extends AllAppsSearchBarController {
    @Override
    public DefaultAppSearchAlgorithm onInitializeSearch() {
        return new DefaultAppSearchAlgorithm(this.mApps.getApps());
    }
}
