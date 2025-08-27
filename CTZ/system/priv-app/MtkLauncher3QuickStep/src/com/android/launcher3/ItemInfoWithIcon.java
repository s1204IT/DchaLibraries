package com.android.launcher3;

import android.graphics.Bitmap;

/* loaded from: classes.dex */
public abstract class ItemInfoWithIcon extends ItemInfo {
    public static final int FLAG_ADAPTIVE_ICON = 256;
    public static final int FLAG_DISABLED_BY_PUBLISHER = 16;
    public static final int FLAG_DISABLED_LOCKED_USER = 32;
    public static final int FLAG_DISABLED_MASK = 63;
    public static final int FLAG_DISABLED_NOT_AVAILABLE = 2;
    public static final int FLAG_DISABLED_QUIET_USER = 8;
    public static final int FLAG_DISABLED_SAFEMODE = 1;
    public static final int FLAG_DISABLED_SUSPENDED = 4;
    public static final int FLAG_ICON_BADGED = 512;
    public static final int FLAG_SYSTEM_MASK = 192;
    public static final int FLAG_SYSTEM_NO = 128;
    public static final int FLAG_SYSTEM_YES = 64;
    public Bitmap iconBitmap;
    public int iconColor;
    public int runtimeStatusFlags;
    public boolean usingLowResIcon;

    protected ItemInfoWithIcon() {
        this.runtimeStatusFlags = 0;
    }

    protected ItemInfoWithIcon(ItemInfoWithIcon itemInfoWithIcon) {
        super(itemInfoWithIcon);
        this.runtimeStatusFlags = 0;
        this.iconBitmap = itemInfoWithIcon.iconBitmap;
        this.iconColor = itemInfoWithIcon.iconColor;
        this.usingLowResIcon = itemInfoWithIcon.usingLowResIcon;
        this.runtimeStatusFlags = itemInfoWithIcon.runtimeStatusFlags;
    }

    @Override // com.android.launcher3.ItemInfo
    public boolean isDisabled() {
        return (this.runtimeStatusFlags & 63) != 0;
    }
}
