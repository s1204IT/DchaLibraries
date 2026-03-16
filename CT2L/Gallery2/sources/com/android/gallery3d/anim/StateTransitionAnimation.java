package com.android.gallery3d.anim;

import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.gallery3d.glrenderer.GLCanvas;
import com.android.gallery3d.glrenderer.RawTexture;
import com.android.gallery3d.ui.GLView;
import com.android.gallery3d.ui.TiledScreenNail;

public class StateTransitionAnimation extends Animation {
    private float mCurrentBackgroundAlpha;
    private float mCurrentBackgroundScale;
    private float mCurrentContentAlpha;
    private float mCurrentContentScale;
    private float mCurrentOverlayAlpha;
    private float mCurrentOverlayScale;
    private RawTexture mOldScreenTexture;
    private final Spec mTransitionSpec;

    public enum Transition {
        None,
        Outgoing,
        Incoming,
        PhotoIncoming
    }

    public static class Spec {
        public static final Spec INCOMING;
        public static final Spec PHOTO_INCOMING;
        private static final Interpolator DEFAULT_INTERPOLATOR = new DecelerateInterpolator();
        public static final Spec OUTGOING = new Spec();
        public int duration = 330;
        public float backgroundAlphaFrom = 0.0f;
        public float backgroundAlphaTo = 0.0f;
        public float backgroundScaleFrom = 0.0f;
        public float backgroundScaleTo = 0.0f;
        public float contentAlphaFrom = 1.0f;
        public float contentAlphaTo = 1.0f;
        public float contentScaleFrom = 1.0f;
        public float contentScaleTo = 1.0f;
        public float overlayAlphaFrom = 0.0f;
        public float overlayAlphaTo = 0.0f;
        public float overlayScaleFrom = 0.0f;
        public float overlayScaleTo = 0.0f;
        public Interpolator interpolator = DEFAULT_INTERPOLATOR;

        static {
            OUTGOING.backgroundAlphaFrom = 0.5f;
            OUTGOING.backgroundAlphaTo = 0.0f;
            OUTGOING.backgroundScaleFrom = 1.0f;
            OUTGOING.backgroundScaleTo = 0.0f;
            OUTGOING.contentAlphaFrom = 0.5f;
            OUTGOING.contentAlphaTo = 1.0f;
            OUTGOING.contentScaleFrom = 3.0f;
            OUTGOING.contentScaleTo = 1.0f;
            INCOMING = new Spec();
            INCOMING.overlayAlphaFrom = 1.0f;
            INCOMING.overlayAlphaTo = 0.0f;
            INCOMING.overlayScaleFrom = 1.0f;
            INCOMING.overlayScaleTo = 3.0f;
            INCOMING.contentAlphaFrom = 0.0f;
            INCOMING.contentAlphaTo = 1.0f;
            INCOMING.contentScaleFrom = 0.25f;
            INCOMING.contentScaleTo = 1.0f;
            PHOTO_INCOMING = INCOMING;
        }

        private static Spec specForTransition(Transition t) {
            switch (t) {
                case Outgoing:
                    return OUTGOING;
                case Incoming:
                    return INCOMING;
                case PhotoIncoming:
                    return PHOTO_INCOMING;
                default:
                    return null;
            }
        }
    }

    public StateTransitionAnimation(Transition t, RawTexture oldScreen) {
        this(Spec.specForTransition(t), oldScreen);
    }

    public StateTransitionAnimation(Spec spec, RawTexture oldScreen) {
        this.mTransitionSpec = spec == null ? Spec.OUTGOING : spec;
        setDuration(this.mTransitionSpec.duration);
        setInterpolator(this.mTransitionSpec.interpolator);
        this.mOldScreenTexture = oldScreen;
        TiledScreenNail.disableDrawPlaceholder();
    }

    @Override
    public boolean calculate(long currentTimeMillis) {
        boolean retval = super.calculate(currentTimeMillis);
        if (!isActive()) {
            if (this.mOldScreenTexture != null) {
                this.mOldScreenTexture.recycle();
                this.mOldScreenTexture = null;
            }
            TiledScreenNail.enableDrawPlaceholder();
        }
        return retval;
    }

    @Override
    protected void onCalculate(float progress) {
        this.mCurrentContentScale = this.mTransitionSpec.contentScaleFrom + ((this.mTransitionSpec.contentScaleTo - this.mTransitionSpec.contentScaleFrom) * progress);
        this.mCurrentContentAlpha = this.mTransitionSpec.contentAlphaFrom + ((this.mTransitionSpec.contentAlphaTo - this.mTransitionSpec.contentAlphaFrom) * progress);
        this.mCurrentBackgroundAlpha = this.mTransitionSpec.backgroundAlphaFrom + ((this.mTransitionSpec.backgroundAlphaTo - this.mTransitionSpec.backgroundAlphaFrom) * progress);
        this.mCurrentBackgroundScale = this.mTransitionSpec.backgroundScaleFrom + ((this.mTransitionSpec.backgroundScaleTo - this.mTransitionSpec.backgroundScaleFrom) * progress);
        this.mCurrentOverlayScale = this.mTransitionSpec.overlayScaleFrom + ((this.mTransitionSpec.overlayScaleTo - this.mTransitionSpec.overlayScaleFrom) * progress);
        this.mCurrentOverlayAlpha = this.mTransitionSpec.overlayAlphaFrom + ((this.mTransitionSpec.overlayAlphaTo - this.mTransitionSpec.overlayAlphaFrom) * progress);
    }

    private void applyOldTexture(GLView view, GLCanvas canvas, float alpha, float scale, boolean clear) {
        if (this.mOldScreenTexture != null) {
            if (clear) {
                canvas.clearBuffer(view.getBackgroundColor());
            }
            canvas.save();
            canvas.setAlpha(alpha);
            int xOffset = view.getWidth() / 2;
            int yOffset = view.getHeight() / 2;
            canvas.translate(xOffset, yOffset);
            canvas.scale(scale, scale, 1.0f);
            this.mOldScreenTexture.draw(canvas, -xOffset, -yOffset);
            canvas.restore();
        }
    }

    public void applyBackground(GLView view, GLCanvas canvas) {
        if (this.mCurrentBackgroundAlpha > 0.0f) {
            applyOldTexture(view, canvas, this.mCurrentBackgroundAlpha, this.mCurrentBackgroundScale, true);
        }
    }

    public void applyContentTransform(GLView view, GLCanvas canvas) {
        int xOffset = view.getWidth() / 2;
        int yOffset = view.getHeight() / 2;
        canvas.translate(xOffset, yOffset);
        canvas.scale(this.mCurrentContentScale, this.mCurrentContentScale, 1.0f);
        canvas.translate(-xOffset, -yOffset);
        canvas.setAlpha(this.mCurrentContentAlpha);
    }

    public void applyOverlay(GLView view, GLCanvas canvas) {
        if (this.mCurrentOverlayAlpha > 0.0f) {
            applyOldTexture(view, canvas, this.mCurrentOverlayAlpha, this.mCurrentOverlayScale, false);
        }
    }
}
