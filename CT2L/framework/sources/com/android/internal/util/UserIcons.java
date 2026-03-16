package com.android.internal.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import com.android.internal.R;

public class UserIcons {
    private static final int[] USER_ICON_COLORS = {R.color.user_icon_1, R.color.user_icon_2, R.color.user_icon_3, R.color.user_icon_4, R.color.user_icon_5, R.color.user_icon_6, R.color.user_icon_7, R.color.user_icon_8};

    public static Bitmap convertToBitmap(Drawable icon) {
        if (icon == null) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        icon.draw(new Canvas(bitmap));
        return bitmap;
    }

    public static Drawable getDefaultUserIcon(int userId, boolean light) {
        int colorResId = light ? R.color.user_icon_default_white : R.color.user_icon_default_gray;
        if (userId != -10000) {
            colorResId = USER_ICON_COLORS[userId % USER_ICON_COLORS.length];
        }
        Drawable icon = Resources.getSystem().getDrawable(R.drawable.ic_account_circle).mutate();
        icon.setColorFilter(Resources.getSystem().getColor(colorResId), PorterDuff.Mode.SRC_IN);
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
        return icon;
    }
}
