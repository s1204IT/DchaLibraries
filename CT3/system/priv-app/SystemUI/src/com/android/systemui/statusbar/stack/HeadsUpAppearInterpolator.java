package com.android.systemui.statusbar.stack;

import android.graphics.Path;
import android.view.animation.PathInterpolator;
/* loaded from: a.zip:com/android/systemui/statusbar/stack/HeadsUpAppearInterpolator.class */
public class HeadsUpAppearInterpolator extends PathInterpolator {
    public HeadsUpAppearInterpolator() {
        super(getAppearPath());
    }

    private static Path getAppearPath() {
        Path path = new Path();
        path.moveTo(0.0f, 0.0f);
        float f = 400.0f + 100.0f;
        path.cubicTo(225.0f / f, 0.0f, 200.0f / f, 1.125f, 250.0f / f, 1.125f);
        path.cubicTo((60.0f + 250.0f) / f, 1.125f, (30.0f + 250.0f) / f, 0.975f, 400.0f / f, 0.975f);
        path.cubicTo((40.0f + 400.0f) / f, 0.975f, (20.0f + 400.0f) / f, 1.0f, 1.0f, 1.0f);
        return path;
    }
}
