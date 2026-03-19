package com.android.server.wm;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.SystemClock;
import android.util.proto.ProtoOutputStream;
import android.view.SurfaceControl;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.view.animation.TranslateAnimation;
import com.android.server.wm.LocalAnimationAdapter;
import java.io.PrintWriter;
import java.util.function.Supplier;

public class WindowAnimationSpec implements LocalAnimationAdapter.AnimationSpec {
    private Animation mAnimation;
    private final boolean mCanSkipFirstFrame;
    private final boolean mIsAppAnimation;
    private final Point mPosition;
    private final Rect mStackBounds;
    private int mStackClipMode;
    private final ThreadLocal<TmpValues> mThreadLocalTmps;
    private final Rect mTmpRect;

    static TmpValues lambda$new$0() {
        return new TmpValues();
    }

    public WindowAnimationSpec(Animation animation, Point point, boolean z) {
        this(animation, point, null, z, 2, false);
    }

    public WindowAnimationSpec(Animation animation, Point point, Rect rect, boolean z, int i, boolean z2) {
        this.mPosition = new Point();
        this.mThreadLocalTmps = ThreadLocal.withInitial(new Supplier() {
            @Override
            public final Object get() {
                return WindowAnimationSpec.lambda$new$0();
            }
        });
        this.mStackBounds = new Rect();
        this.mTmpRect = new Rect();
        this.mAnimation = animation;
        if (point != null) {
            this.mPosition.set(point.x, point.y);
        }
        this.mCanSkipFirstFrame = z;
        this.mIsAppAnimation = z2;
        this.mStackClipMode = i;
        if (rect != null) {
            this.mStackBounds.set(rect);
        }
    }

    @Override
    public boolean getDetachWallpaper() {
        return this.mAnimation.getDetachWallpaper();
    }

    @Override
    public boolean getShowWallpaper() {
        return this.mAnimation.getShowWallpaper();
    }

    @Override
    public int getBackgroundColor() {
        return this.mAnimation.getBackgroundColor();
    }

    @Override
    public long getDuration() {
        return this.mAnimation.computeDurationHint();
    }

    @Override
    public void apply(SurfaceControl.Transaction transaction, SurfaceControl surfaceControl, long j) {
        TmpValues tmpValues = this.mThreadLocalTmps.get();
        tmpValues.transformation.clear();
        this.mAnimation.getTransformation(j, tmpValues.transformation);
        tmpValues.transformation.getMatrix().postTranslate(this.mPosition.x, this.mPosition.y);
        transaction.setMatrix(surfaceControl, tmpValues.transformation.getMatrix(), tmpValues.floats);
        transaction.setAlpha(surfaceControl, tmpValues.transformation.getAlpha());
        if (this.mStackClipMode == 2) {
            transaction.setWindowCrop(surfaceControl, tmpValues.transformation.getClipRect());
            return;
        }
        if (this.mStackClipMode == 0) {
            this.mTmpRect.set(this.mStackBounds);
            this.mTmpRect.offsetTo(this.mPosition.x, this.mPosition.y);
            transaction.setFinalCrop(surfaceControl, this.mTmpRect);
            transaction.setWindowCrop(surfaceControl, tmpValues.transformation.getClipRect());
            return;
        }
        this.mTmpRect.set(this.mStackBounds);
        this.mTmpRect.intersect(tmpValues.transformation.getClipRect());
        transaction.setWindowCrop(surfaceControl, this.mTmpRect);
    }

    @Override
    public long calculateStatusBarTransitionStartTime() {
        TranslateAnimation translateAnimationFindTranslateAnimation = findTranslateAnimation(this.mAnimation);
        if (translateAnimationFindTranslateAnimation != null) {
            return ((SystemClock.uptimeMillis() + translateAnimationFindTranslateAnimation.getStartOffset()) + ((long) (translateAnimationFindTranslateAnimation.getDuration() * findAlmostThereFraction(translateAnimationFindTranslateAnimation.getInterpolator())))) - 120;
        }
        return SystemClock.uptimeMillis();
    }

    @Override
    public boolean canSkipFirstFrame() {
        return this.mCanSkipFirstFrame;
    }

    @Override
    public boolean needsEarlyWakeup() {
        return this.mIsAppAnimation;
    }

    @Override
    public void dump(PrintWriter printWriter, String str) {
        printWriter.print(str);
        printWriter.println(this.mAnimation);
    }

    @Override
    public void writeToProtoInner(ProtoOutputStream protoOutputStream) {
        long jStart = protoOutputStream.start(1146756268033L);
        protoOutputStream.write(1138166333441L, this.mAnimation.toString());
        protoOutputStream.end(jStart);
    }

    private static TranslateAnimation findTranslateAnimation(Animation animation) {
        if (animation instanceof TranslateAnimation) {
            return (TranslateAnimation) animation;
        }
        if (animation instanceof AnimationSet) {
            AnimationSet animationSet = (AnimationSet) animation;
            for (int i = 0; i < animationSet.getAnimations().size(); i++) {
                Animation animation2 = animationSet.getAnimations().get(i);
                if (animation2 instanceof TranslateAnimation) {
                    return (TranslateAnimation) animation2;
                }
            }
            return null;
        }
        return null;
    }

    private static float findAlmostThereFraction(Interpolator interpolator) {
        float f = 0.5f;
        for (float f2 = 0.25f; f2 >= 0.01f; f2 /= 2.0f) {
            if (interpolator.getInterpolation(f) < 0.99f) {
                f += f2;
            } else {
                f -= f2;
            }
        }
        return f;
    }

    private static class TmpValues {
        final float[] floats;
        final Transformation transformation;

        private TmpValues() {
            this.transformation = new Transformation();
            this.floats = new float[9];
        }
    }
}
