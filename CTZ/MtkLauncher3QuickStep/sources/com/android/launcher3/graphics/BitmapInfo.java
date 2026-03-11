package com.android.launcher3.graphics;

import android.graphics.Bitmap;
import com.android.launcher3.ItemInfoWithIcon;

public class BitmapInfo {
    public int color;
    public Bitmap icon;

    public void applyTo(ItemInfoWithIcon itemInfoWithIcon) {
        itemInfoWithIcon.iconBitmap = this.icon;
        itemInfoWithIcon.iconColor = this.color;
    }

    public void applyTo(BitmapInfo bitmapInfo) {
        bitmapInfo.icon = this.icon;
        bitmapInfo.color = this.color;
    }

    public static BitmapInfo fromBitmap(Bitmap bitmap) {
        BitmapInfo bitmapInfo = new BitmapInfo();
        bitmapInfo.icon = bitmap;
        bitmapInfo.color = ColorExtractor.findDominantColorByHue(bitmap);
        return bitmapInfo;
    }
}
