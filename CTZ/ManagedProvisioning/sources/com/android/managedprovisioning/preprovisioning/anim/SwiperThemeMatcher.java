package com.android.managedprovisioning.preprovisioning.anim;

import android.content.Context;
import android.graphics.Color;

public class SwiperThemeMatcher {
    private final ColorMatcher mColorMatcher;
    private final Context mContext;

    public SwiperThemeMatcher(Context context, ColorMatcher colorMatcher) {
        this.mContext = context;
        this.mColorMatcher = colorMatcher;
    }

    public int findTheme(int i) {
        int iFindClosestColor = this.mColorMatcher.findClosestColor(i);
        return this.mContext.getResources().getIdentifier(String.format("%s%02x%02x%02x", "Swiper", Integer.valueOf(Color.red(iFindClosestColor)), Integer.valueOf(Color.green(iFindClosestColor)), Integer.valueOf(Color.blue(iFindClosestColor))), "style", this.mContext.getPackageName());
    }
}
