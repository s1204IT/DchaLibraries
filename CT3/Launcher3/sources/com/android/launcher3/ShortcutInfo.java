package com.android.launcher3;

import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import com.android.launcher3.compat.LauncherActivityInfoCompat;
import com.android.launcher3.compat.UserManagerCompat;
import java.util.Arrays;

public class ShortcutInfo extends ItemInfo {
    public boolean customIcon;
    int flags;
    public Intent.ShortcutIconResource iconResource;
    Intent intent;
    int isDisabled;
    private Bitmap mIcon;
    private int mInstallProgress;
    Intent promisedIntent;
    int status;
    boolean usingFallbackIcon;
    boolean usingLowResIcon;

    ShortcutInfo() {
        this.isDisabled = 0;
        this.flags = 0;
        this.itemType = 1;
    }

    @Override
    public Intent getIntent() {
        return this.intent;
    }

    public ShortcutInfo(AppInfo info) {
        super(info);
        this.isDisabled = 0;
        this.flags = 0;
        this.title = Utilities.trim(info.title);
        this.intent = new Intent(info.intent);
        this.customIcon = false;
        this.flags = info.flags;
        this.isDisabled = info.isDisabled;
    }

    public void setIcon(Bitmap b) {
        this.mIcon = b;
    }

    public Bitmap getIcon(IconCache iconCache) {
        if (this.mIcon == null) {
            updateIcon(iconCache);
        }
        return this.mIcon;
    }

    public void updateIcon(IconCache iconCache, boolean useLowRes) {
        if (this.itemType != 0) {
            return;
        }
        iconCache.getTitleAndIcon(this, this.promisedIntent != null ? this.promisedIntent : this.intent, this.user, useLowRes);
    }

    public void updateIcon(IconCache iconCache) {
        updateIcon(iconCache, shouldUseLowResIcon());
    }

    @Override
    void onAddToDatabase(Context context, ContentValues values) {
        String uri;
        super.onAddToDatabase(context, values);
        values.put("title", this.title != null ? this.title.toString() : null);
        if (this.promisedIntent != null) {
            uri = this.promisedIntent.toUri(0);
        } else {
            uri = this.intent != null ? this.intent.toUri(0) : null;
        }
        values.put("intent", uri);
        values.put("restored", Integer.valueOf(this.status));
        if (this.customIcon) {
            values.put("iconType", (Integer) 1);
            writeBitmap(values, this.mIcon);
            return;
        }
        if (!this.usingFallbackIcon) {
            writeBitmap(values, this.mIcon);
        }
        if (this.iconResource == null) {
            return;
        }
        values.put("iconType", (Integer) 0);
        values.put("iconPackage", this.iconResource.packageName);
        values.put("iconResource", this.iconResource.resourceName);
    }

    @Override
    public String toString() {
        return "ShortcutInfo(title=" + this.title + "intent=" + this.intent + "id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screenId + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " dropPos=" + Arrays.toString(this.dropPos) + " user=" + this.user + ")";
    }

    public ComponentName getTargetComponent() {
        return this.promisedIntent != null ? this.promisedIntent.getComponent() : this.intent.getComponent();
    }

    public boolean hasStatusFlag(int flag) {
        return (this.status & flag) != 0;
    }

    public final boolean isPromise() {
        return hasStatusFlag(3);
    }

    public int getInstallProgress() {
        return this.mInstallProgress;
    }

    public void setInstallProgress(int progress) {
        this.mInstallProgress = progress;
        this.status |= 4;
    }

    public boolean shouldUseLowResIcon() {
        return this.usingLowResIcon && this.container >= 0 && this.rank >= 3;
    }

    public static ShortcutInfo fromActivityInfo(LauncherActivityInfoCompat info, Context context) {
        ShortcutInfo shortcut = new ShortcutInfo();
        shortcut.user = info.getUser();
        shortcut.title = Utilities.trim(info.getLabel());
        shortcut.contentDescription = UserManagerCompat.getInstance(context).getBadgedLabelForUser(info.getLabel(), info.getUser());
        shortcut.customIcon = false;
        shortcut.intent = AppInfo.makeLaunchIntent(context, info, info.getUser());
        shortcut.itemType = 0;
        shortcut.flags = AppInfo.initFlags(info);
        return shortcut;
    }

    @Override
    public boolean isDisabled() {
        return this.isDisabled != 0;
    }
}
