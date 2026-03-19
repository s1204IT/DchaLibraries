package com.android.internal.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import com.android.internal.R;

public class UserIcons {
    private static final int[] USER_ICON_COLORS = {R.color.user_icon_1, R.color.user_icon_2, R.color.user_icon_3, R.color.user_icon_4, R.color.user_icon_5, R.color.user_icon_6, R.color.user_icon_7, R.color.user_icon_8};

    public static Bitmap convertToBitmap(Drawable drawable) {
        if (drawable == null) {
            return null;
        }
        int intrinsicWidth = drawable.getIntrinsicWidth();
        int intrinsicHeight = drawable.getIntrinsicHeight();
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        drawable.setBounds(0, 0, intrinsicWidth, intrinsicHeight);
        drawable.draw(canvas);
        return bitmapCreateBitmap;
    }

    public static Drawable getDefaultUserIcon(Resources resources, int i, boolean z) {
        int i2 = z ? R.color.user_icon_default_white : R.color.user_icon_default_gray;
        if (i != -10000) {
            i2 = USER_ICON_COLORS[i % USER_ICON_COLORS.length];
        }
        Drawable drawableMutate = resources.getDrawable(R.drawable.ic_account_circle, null).mutate();
        drawableMutate.setColorFilter(resources.getColor(i2, null), PorterDuff.Mode.SRC_IN);
        drawableMutate.setBounds(0, 0, drawableMutate.getIntrinsicWidth(), drawableMutate.getIntrinsicHeight());
        return drawableMutate;
    }
}
