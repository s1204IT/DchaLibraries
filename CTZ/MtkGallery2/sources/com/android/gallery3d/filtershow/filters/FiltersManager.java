package com.android.gallery3d.filtershow.filters;

import android.content.res.Resources;

public class FiltersManager extends BaseFiltersManager {
    private static FiltersManager sInstance = null;
    private static FiltersManager sPreviewInstance = null;
    private static FiltersManager sHighresInstance = null;

    public FiltersManager() {
        init();
    }

    public static FiltersManager getPreviewManager() {
        if (sPreviewInstance == null) {
            sPreviewInstance = new FiltersManager();
        }
        return sPreviewInstance;
    }

    public static FiltersManager getManager() {
        if (sInstance == null) {
            sInstance = new FiltersManager();
        }
        return sInstance;
    }

    public static FiltersManager getHighresManager() {
        if (sHighresInstance == null) {
            sHighresInstance = new FiltersManager();
        }
        return sHighresInstance;
    }

    public static void reset() {
        sInstance = null;
        sPreviewInstance = null;
        sHighresInstance = null;
    }

    public static void setResources(Resources resources) {
        getManager().setFilterResources(resources);
        getPreviewManager().setFilterResources(resources);
        getHighresManager().setFilterResources(resources);
    }
}
