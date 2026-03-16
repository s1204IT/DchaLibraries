package com.android.launcher2;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;

class ShortcutInfo extends ItemInfo {
    boolean customIcon;
    Intent.ShortcutIconResource iconResource;
    Intent intent;
    private Bitmap mIcon;
    boolean usingFallbackIcon;

    ShortcutInfo() {
        this.itemType = 1;
    }

    public ShortcutInfo(ApplicationInfo info) {
        super(info);
        this.title = info.title.toString();
        this.intent = new Intent(info.intent);
        this.customIcon = false;
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

    public void updateIcon(IconCache iconCache) {
        this.mIcon = iconCache.getIcon(this.intent, this.user);
        this.usingFallbackIcon = iconCache.isDefaultIcon(this.mIcon);
    }

    final void setActivity(Intent intent) {
        this.intent = new Intent();
        this.intent.setFlags(270532608);
        this.intent.addCategory("android.intent.category.LAUNCHER");
        this.intent.setComponent(intent.getComponent());
        this.intent.putExtras(intent.getExtras());
        this.itemType = 0;
        updateUser(this.intent);
    }

    @Override
    void onAddToDatabase(Context context, ContentValues values) {
        super.onAddToDatabase(context, values);
        String titleStr = this.title != null ? this.title.toString() : null;
        values.put("title", titleStr);
        String uri = this.intent != null ? this.intent.toUri(0) : null;
        values.put("intent", uri);
        if (this.customIcon) {
            values.put("iconType", (Integer) 1);
            writeBitmap(values, this.mIcon);
            return;
        }
        if (!this.usingFallbackIcon) {
            writeBitmap(values, this.mIcon);
        }
        values.put("iconType", (Integer) 0);
        if (this.iconResource != null) {
            values.put("iconPackage", this.iconResource.packageName);
            values.put("iconResource", this.iconResource.resourceName);
        }
    }

    @Override
    public String toString() {
        return "ShortcutInfo(title=" + this.title.toString() + "intent=" + this.intent + "id=" + this.id + " type=" + this.itemType + " container=" + this.container + " screen=" + this.screen + " cellX=" + this.cellX + " cellY=" + this.cellY + " spanX=" + this.spanX + " spanY=" + this.spanY + " dropPos=" + this.dropPos + ")";
    }
}
