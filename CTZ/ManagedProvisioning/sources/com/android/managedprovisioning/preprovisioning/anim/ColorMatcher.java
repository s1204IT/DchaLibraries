package com.android.managedprovisioning.preprovisioning.anim;

import android.graphics.Color;

public class ColorMatcher {
    public int findClosestColor(int i) {
        return Color.argb(255, bucketize(Color.red(i)), bucketize(Color.green(i)), bucketize(Color.blue(i)));
    }

    private int bucketize(int i) {
        return Math.min(255, ((int) Math.round(((double) i) / 32.0d)) * 32);
    }
}
