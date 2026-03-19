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

        private static Spec specForTransition(Transition transition) {
            switch (AnonymousClass1.$SwitchMap$com$android$gallery3d$anim$StateTransitionAnimation$Transition[transition.ordinal()]) {
                case 1:
                    return OUTGOING;
                case 2:
                    return INCOMING;
                case 3:
                    return PHOTO_INCOMING;
                default:
                    return null;
            }
        }
    }

    static class AnonymousClass1 {
        static final int[] $SwitchMap$com$android$gallery3d$anim$StateTransitionAnimation$Transition = new int[Transition.values().length];

        static {
            try {
                $SwitchMap$com$android$gallery3d$anim$StateTransitionAnimation$Transition[Transition.Outgoing.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$android$gallery3d$anim$StateTransitionAnimation$Transition[Transition.Incoming.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$android$gallery3d$anim$StateTransitionAnimation$Transition[Transition.PhotoIncoming.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$android$gallery3d$anim$StateTransitionAnimation$Transition[Transition.None.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public StateTransitionAnimation(Transition transition, RawTexture rawTexture) {
        this(Spec.specForTransition(transition), rawTexture);
    }

    public StateTransitionAnimation(Spec spec, RawTexture rawTexture) {
        this.mTransitionSpec = spec == null ? Spec.OUTGOING : spec;
        setDuration(this.mTransitionSpec.duration);
        setInterpolator(this.mTransitionSpec.interpolator);
        this.mOldScreenTexture = rawTexture;
        TiledScreenNail.disableDrawPlaceholder();
    }

    @Override
    public boolean calculate(long j) {
        boolean zCalculate = super.calculate(j);
        if (!isActive()) {
            if (this.mOldScreenTexture != null) {
                this.mOldScreenTexture.recycle();
                this.mOldScreenTexture = null;
            }
            TiledScreenNail.enableDrawPlaceholder();
        }
        return zCalculate;
    }

    @Override
    protected void onCalculate(float f) {
        this.mCurrentContentScale = this.mTransitionSpec.contentScaleFrom + ((this.mTransitionSpec.contentScaleTo - this.mTransitionSpec.contentScaleFrom) * f);
        this.mCurrentContentAlpha = this.mTransitionSpec.contentAlphaFrom + ((this.mTransitionSpec.contentAlphaTo - this.mTransitionSpec.contentAlphaFrom) * f);
        this.mCurrentBackgroundAlpha = this.mTransitionSpec.backgroundAlphaFrom + ((this.mTransitionSpec.backgroundAlphaTo - this.mTransitionSpec.backgroundAlphaFrom) * f);
        this.mCurrentBackgroundScale = this.mTransitionSpec.backgroundScaleFrom + ((this.mTransitionSpec.backgroundScaleTo - this.mTransitionSpec.backgroundScaleFrom) * f);
        this.mCurrentOverlayScale = this.mTransitionSpec.overlayScaleFrom + ((this.mTransitionSpec.overlayScaleTo - this.mTransitionSpec.overlayScaleFrom) * f);
        this.mCurrentOverlayAlpha = this.mTransitionSpec.overlayAlphaFrom + ((this.mTransitionSpec.overlayAlphaTo - this.mTransitionSpec.overlayAlphaFrom) * f);
    }

    private void applyOldTexture(GLView gLView, GLCanvas gLCanvas, float f, float f2, boolean z) {
        if (this.mOldScreenTexture == null) {
            return;
        }
        if (z) {
            gLCanvas.clearBuffer(gLView.getBackgroundColor());
        }
        gLCanvas.save();
        gLCanvas.setAlpha(f);
        int width = gLView.getWidth() / 2;
        int height = gLView.getHeight() / 2;
        gLCanvas.translate(width, height);
        gLCanvas.scale(f2, f2, 1.0f);
        this.mOldScreenTexture.draw(gLCanvas, -width, -height);
        gLCanvas.restore();
    }

    public void applyBackground(GLView gLView, GLCanvas gLCanvas) {
        if (this.mCurrentBackgroundAlpha > 0.0f) {
            applyOldTexture(gLView, gLCanvas, this.mCurrentBackgroundAlpha, this.mCurrentBackgroundScale, true);
        }
    }

    public void applyContentTransform(GLView gLView, GLCanvas gLCanvas) {
        gLCanvas.translate(gLView.getWidth() / 2, gLView.getHeight() / 2);
        gLCanvas.scale(this.mCurrentContentScale, this.mCurrentContentScale, 1.0f);
        gLCanvas.translate(-r0, -r5);
        gLCanvas.setAlpha(this.mCurrentContentAlpha);
    }

    public void applyOverlay(GLView gLView, GLCanvas gLCanvas) {
        if (this.mCurrentOverlayAlpha > 0.0f) {
            applyOldTexture(gLView, gLCanvas, this.mCurrentOverlayAlpha, this.mCurrentOverlayScale, false);
        }
    }
}
