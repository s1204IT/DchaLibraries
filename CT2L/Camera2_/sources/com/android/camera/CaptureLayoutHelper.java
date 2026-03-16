package com.android.camera;

import android.graphics.Matrix;
import android.graphics.RectF;
import com.android.camera.app.CameraAppUI;
import com.android.camera.ui.PreviewStatusListener;

public class CaptureLayoutHelper implements CameraAppUI.NonDecorWindowSizeChangedListener, PreviewStatusListener.PreviewAspectRatioChangedListener {
    private final int mBottomBarMaxHeight;
    private final int mBottomBarMinHeight;
    private final int mBottomBarOptimalHeight;
    private int mWindowWidth = 0;
    private int mWindowHeight = 0;
    private float mAspectRatio = 0.0f;
    private PositionConfiguration mPositionConfiguration = null;
    private int mRotation = 0;
    private boolean mShowBottomBar = true;

    public static final class PositionConfiguration {
        public final RectF mPreviewRect = new RectF();
        public final RectF mBottomBarRect = new RectF();
        public boolean mBottomBarOverlay = false;
    }

    public CaptureLayoutHelper(int bottomBarMinHeight, int bottomBarMaxHeight, int bottomBarOptimalHeight) {
        this.mBottomBarMinHeight = bottomBarMinHeight;
        this.mBottomBarMaxHeight = bottomBarMaxHeight;
        this.mBottomBarOptimalHeight = bottomBarOptimalHeight;
    }

    @Override
    public void onPreviewAspectRatioChanged(float aspectRatio) {
        if (this.mAspectRatio != aspectRatio) {
            this.mAspectRatio = aspectRatio;
            updatePositionConfiguration();
        }
    }

    public void setShowBottomBar(boolean showBottomBar) {
        this.mShowBottomBar = showBottomBar;
    }

    private void updatePositionConfiguration() {
        if (this.mWindowWidth != 0 && this.mWindowHeight != 0) {
            this.mPositionConfiguration = getPositionConfiguration(this.mWindowWidth, this.mWindowHeight, this.mAspectRatio, this.mRotation);
        }
    }

    public RectF getBottomBarRect() {
        if (this.mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        return this.mPositionConfiguration == null ? new RectF() : new RectF(this.mPositionConfiguration.mBottomBarRect);
    }

    public RectF getPreviewRect() {
        if (this.mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        return this.mPositionConfiguration == null ? new RectF() : new RectF(this.mPositionConfiguration.mPreviewRect);
    }

    public RectF getFullscreenRect() {
        return new RectF(0.0f, 0.0f, this.mWindowWidth, this.mWindowHeight);
    }

    public RectF getUncoveredPreviewRect() {
        if (this.mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        if (this.mPositionConfiguration == null) {
            return new RectF();
        }
        if (!RectF.intersects(this.mPositionConfiguration.mBottomBarRect, this.mPositionConfiguration.mPreviewRect) || !this.mShowBottomBar) {
            return this.mPositionConfiguration.mPreviewRect;
        }
        if (this.mWindowHeight > this.mWindowWidth) {
            if (this.mRotation >= 180) {
                return new RectF(this.mPositionConfiguration.mPreviewRect.left, this.mPositionConfiguration.mBottomBarRect.bottom, this.mPositionConfiguration.mPreviewRect.right, this.mPositionConfiguration.mPreviewRect.bottom);
            }
            return new RectF(this.mPositionConfiguration.mPreviewRect.left, this.mPositionConfiguration.mPreviewRect.top, this.mPositionConfiguration.mPreviewRect.right, this.mPositionConfiguration.mBottomBarRect.top);
        }
        if (this.mRotation >= 180) {
            return new RectF(this.mPositionConfiguration.mBottomBarRect.right, this.mPositionConfiguration.mPreviewRect.top, this.mPositionConfiguration.mPreviewRect.right, this.mPositionConfiguration.mPreviewRect.bottom);
        }
        return new RectF(this.mPositionConfiguration.mPreviewRect.left, this.mPositionConfiguration.mPreviewRect.top, this.mPositionConfiguration.mBottomBarRect.left, this.mPositionConfiguration.mPreviewRect.bottom);
    }

    public boolean shouldOverlayBottomBar() {
        if (this.mPositionConfiguration == null) {
            updatePositionConfiguration();
        }
        if (this.mPositionConfiguration == null) {
            return false;
        }
        return this.mPositionConfiguration.mBottomBarOverlay;
    }

    @Override
    public void onNonDecorWindowSizeChanged(int width, int height, int rotation) {
        this.mWindowWidth = width;
        this.mWindowHeight = height;
        this.mRotation = rotation;
        updatePositionConfiguration();
    }

    private PositionConfiguration getPositionConfiguration(int width, int height, float previewAspectRatio, int rotation) {
        boolean landscape = width > height;
        PositionConfiguration config = new PositionConfiguration();
        if (previewAspectRatio == 0.0f) {
            config.mPreviewRect.set(0.0f, 0.0f, width, height);
            config.mBottomBarOverlay = true;
            if (landscape) {
                config.mBottomBarRect.set(width - this.mBottomBarOptimalHeight, 0.0f, width, height);
            } else {
                config.mBottomBarRect.set(0.0f, height - this.mBottomBarOptimalHeight, width, height);
            }
        } else {
            if (previewAspectRatio < 1.0f) {
                previewAspectRatio = 1.0f / previewAspectRatio;
            }
            int longerEdge = Math.max(width, height);
            int shorterEdge = Math.min(width, height);
            float spaceNeededAlongLongerEdge = shorterEdge * previewAspectRatio;
            float remainingSpaceAlongLongerEdge = longerEdge - spaceNeededAlongLongerEdge;
            if (remainingSpaceAlongLongerEdge <= 0.0f) {
                float previewLongerEdge = longerEdge;
                float previewShorterEdge = longerEdge / previewAspectRatio;
                float barSize = this.mBottomBarOptimalHeight;
                config.mBottomBarOverlay = true;
                if (landscape) {
                    config.mPreviewRect.set(0.0f, (height / 2) - (previewShorterEdge / 2.0f), previewLongerEdge, (height / 2) + (previewShorterEdge / 2.0f));
                    config.mBottomBarRect.set(width - barSize, (height / 2) - (previewShorterEdge / 2.0f), width, (height / 2) + (previewShorterEdge / 2.0f));
                } else {
                    config.mPreviewRect.set((width / 2) - (previewShorterEdge / 2.0f), 0.0f, (width / 2) + (previewShorterEdge / 2.0f), previewLongerEdge);
                    config.mBottomBarRect.set((width / 2) - (previewShorterEdge / 2.0f), height - barSize, (width / 2) + (previewShorterEdge / 2.0f), height);
                }
            } else if (previewAspectRatio > 1.5555556f) {
                float barSize2 = this.mBottomBarOptimalHeight;
                float previewShorterEdge2 = shorterEdge;
                float previewLongerEdge2 = shorterEdge * previewAspectRatio;
                config.mBottomBarOverlay = true;
                if (landscape) {
                    float right = width;
                    float left = right - previewLongerEdge2;
                    config.mPreviewRect.set(left, 0.0f, right, previewShorterEdge2);
                    config.mBottomBarRect.set(width - barSize2, 0.0f, width, height);
                } else {
                    float bottom = height;
                    float top = bottom - previewLongerEdge2;
                    config.mPreviewRect.set(0.0f, top, previewShorterEdge2, bottom);
                    config.mBottomBarRect.set(0.0f, height - barSize2, width, height);
                }
            } else if (remainingSpaceAlongLongerEdge <= this.mBottomBarMinHeight) {
                float previewLongerEdge3 = longerEdge - this.mBottomBarMinHeight;
                float previewShorterEdge3 = previewLongerEdge3 / previewAspectRatio;
                float barSize3 = this.mBottomBarMinHeight;
                config.mBottomBarOverlay = false;
                if (landscape) {
                    config.mPreviewRect.set(0.0f, (height / 2) - (previewShorterEdge3 / 2.0f), previewLongerEdge3, (height / 2) + (previewShorterEdge3 / 2.0f));
                    config.mBottomBarRect.set(width - barSize3, (height / 2) - (previewShorterEdge3 / 2.0f), width, (height / 2) + (previewShorterEdge3 / 2.0f));
                } else {
                    config.mPreviewRect.set((width / 2) - (previewShorterEdge3 / 2.0f), 0.0f, (width / 2) + (previewShorterEdge3 / 2.0f), previewLongerEdge3);
                    config.mBottomBarRect.set((width / 2) - (previewShorterEdge3 / 2.0f), height - barSize3, (width / 2) + (previewShorterEdge3 / 2.0f), height);
                }
            } else {
                float barSize4 = remainingSpaceAlongLongerEdge <= ((float) this.mBottomBarMaxHeight) ? remainingSpaceAlongLongerEdge : this.mBottomBarMaxHeight;
                float previewShorterEdge4 = shorterEdge;
                float previewLongerEdge4 = shorterEdge * previewAspectRatio;
                config.mBottomBarOverlay = false;
                if (landscape) {
                    float right2 = width - barSize4;
                    float left2 = right2 - previewLongerEdge4;
                    config.mPreviewRect.set(left2, 0.0f, right2, previewShorterEdge4);
                    config.mBottomBarRect.set(width - barSize4, 0.0f, width, height);
                } else {
                    float bottom2 = height - barSize4;
                    float top2 = bottom2 - previewLongerEdge4;
                    config.mPreviewRect.set(0.0f, top2, previewShorterEdge4, bottom2);
                    config.mBottomBarRect.set(0.0f, height - barSize4, width, height);
                }
            }
        }
        if (rotation >= 180) {
            Matrix rotate = new Matrix();
            rotate.setRotate(180.0f, width / 2, height / 2);
            rotate.mapRect(config.mPreviewRect);
            rotate.mapRect(config.mBottomBarRect);
        }
        round(config.mBottomBarRect);
        round(config.mPreviewRect);
        return config;
    }

    public static void round(RectF rect) {
        if (rect != null) {
            float left = Math.round(rect.left);
            float top = Math.round(rect.top);
            float right = Math.round(rect.right);
            float bottom = Math.round(rect.bottom);
            rect.set(left, top, right, bottom);
        }
    }
}
