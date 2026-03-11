package com.android.launcher3.model;

import android.content.pm.PackageManager;
import android.os.Process;
import android.os.UserHandle;
import com.android.launcher3.InvariantDeviceProfile;
import com.android.launcher3.LauncherAppWidgetProviderInfo;
import com.android.launcher3.Utilities;
import com.android.launcher3.compat.ShortcutConfigActivityInfo;
import com.android.launcher3.util.ComponentKey;
import java.text.Collator;

public class WidgetItem extends ComponentKey implements Comparable<WidgetItem> {
    private static Collator sCollator;
    private static UserHandle sMyUserHandle;
    public final ShortcutConfigActivityInfo activityInfo;
    public final String label;
    public final int spanX;
    public final int spanY;
    public final LauncherAppWidgetProviderInfo widgetInfo;

    public WidgetItem(LauncherAppWidgetProviderInfo launcherAppWidgetProviderInfo, PackageManager packageManager, InvariantDeviceProfile invariantDeviceProfile) {
        super(launcherAppWidgetProviderInfo.provider, launcherAppWidgetProviderInfo.getProfile());
        this.label = Utilities.trim(launcherAppWidgetProviderInfo.getLabel(packageManager));
        this.widgetInfo = launcherAppWidgetProviderInfo;
        this.activityInfo = null;
        this.spanX = Math.min(launcherAppWidgetProviderInfo.spanX, invariantDeviceProfile.numColumns);
        this.spanY = Math.min(launcherAppWidgetProviderInfo.spanY, invariantDeviceProfile.numRows);
    }

    public WidgetItem(ShortcutConfigActivityInfo shortcutConfigActivityInfo) {
        super(shortcutConfigActivityInfo.getComponent(), shortcutConfigActivityInfo.getUser());
        this.label = Utilities.trim(shortcutConfigActivityInfo.getLabel());
        this.widgetInfo = null;
        this.activityInfo = shortcutConfigActivityInfo;
        this.spanY = 1;
        this.spanX = 1;
    }

    @Override
    public int compareTo(WidgetItem widgetItem) {
        if (sMyUserHandle == null) {
            sMyUserHandle = Process.myUserHandle();
            sCollator = Collator.getInstance();
        }
        boolean z = !sMyUserHandle.equals(this.user);
        if ((!sMyUserHandle.equals(widgetItem.user)) ^ z) {
            return z ? 1 : -1;
        }
        int iCompare = sCollator.compare(this.label, widgetItem.label);
        if (iCompare != 0) {
            return iCompare;
        }
        int i = this.spanX * this.spanY;
        int i2 = widgetItem.spanX * widgetItem.spanY;
        if (i == i2) {
            return Integer.compare(this.spanY, widgetItem.spanY);
        }
        return Integer.compare(i, i2);
    }
}
