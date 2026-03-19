package com.android.server.wm.animation;

import android.animation.KeyframeSet;
import android.animation.PathKeyframes;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public class CurvedTranslateAnimation extends Animation {
    private final PathKeyframes mKeyframes;

    public CurvedTranslateAnimation(Path path) {
        this.mKeyframes = KeyframeSet.ofPath(path);
    }

    @Override
    protected void applyTransformation(float f, Transformation transformation) {
        PointF pointF = (PointF) this.mKeyframes.getValue(f);
        transformation.getMatrix().setTranslate(pointF.x, pointF.y);
    }
}
