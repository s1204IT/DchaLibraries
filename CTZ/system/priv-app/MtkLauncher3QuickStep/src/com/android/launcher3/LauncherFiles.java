package com.android.launcher3;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/* loaded from: classes.dex */
public class LauncherFiles {
    public static final String DEVICE_PREFERENCES_KEY = "com.android.launcher3.device.prefs";
    public static final String MANAGED_USER_PREFERENCES_KEY = "com.android.launcher3.managedusers.prefs";
    public static final String SHARED_PREFERENCES_KEY = "com.android.launcher3.prefs";
    private static final String XML = ".xml";
    public static final String LAUNCHER_DB = "launcher.db";
    public static final String WIDGET_PREVIEWS_DB = "widgetpreviews.db";
    public static final String APP_ICONS_DB = "app_icons.db";
    public static final List<String> ALL_FILES = Collections.unmodifiableList(Arrays.asList(LAUNCHER_DB, "com.android.launcher3.prefs.xml", WIDGET_PREVIEWS_DB, "com.android.launcher3.managedusers.prefs.xml", "com.android.launcher3.device.prefs.xml", APP_ICONS_DB));
}
