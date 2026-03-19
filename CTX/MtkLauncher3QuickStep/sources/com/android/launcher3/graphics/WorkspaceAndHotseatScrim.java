package com.android.launcher3.graphics;

import android.animation.ObjectAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;
import android.support.v4.view.ViewCompat;
import android.util.DisplayMetrics;
import android.util.Property;
import android.view.View;
import com.android.launcher3.CellLayout;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.Workspace;
import com.android.launcher3.uioverrides.WallpaperColorInfo;
import com.android.launcher3.util.Themes;

public class WorkspaceAndHotseatScrim implements View.OnAttachStateChangeListener, WallpaperColorInfo.OnChangeListener {
    private static final int ALPHA_MASK_BITMAP_DP = 200;
    private static final int ALPHA_MASK_HEIGHT_DP = 500;
    private static final int ALPHA_MASK_WIDTH_DP = 2;
    private static final int DARK_SCRIM_COLOR = 1426063360;
    private static final int MAX_HOTSEAT_SCRIM_ALPHA = 100;
    private final Bitmap mBottomMask;
    private boolean mDrawBottomScrim;
    private boolean mDrawTopScrim;
    private int mFullScrimColor;
    private final boolean mHasSysUiScrim;
    private boolean mHideSysUiScrim;
    private final Launcher mLauncher;
    private final int mMaskHeight;
    private final View mRoot;
    private float mScrimProgress;
    private final Drawable mTopScrim;
    private final WallpaperColorInfo mWallpaperColorInfo;
    private Workspace mWorkspace;
    public static Property<WorkspaceAndHotseatScrim, Float> SCRIM_PROGRESS = new Property<WorkspaceAndHotseatScrim, Float>(Float.TYPE, "scrimProgress") {
        @Override
        public Float get(WorkspaceAndHotseatScrim workspaceAndHotseatScrim) {
            return Float.valueOf(workspaceAndHotseatScrim.mScrimProgress);
        }

        @Override
        public void set(WorkspaceAndHotseatScrim workspaceAndHotseatScrim, Float f) {
            workspaceAndHotseatScrim.setScrimProgress(f.floatValue());
        }
    };
    public static Property<WorkspaceAndHotseatScrim, Float> SYSUI_PROGRESS = new Property<WorkspaceAndHotseatScrim, Float>(Float.TYPE, "sysUiProgress") {
        @Override
        public Float get(WorkspaceAndHotseatScrim workspaceAndHotseatScrim) {
            return Float.valueOf(workspaceAndHotseatScrim.mSysUiProgress);
        }

        @Override
        public void set(WorkspaceAndHotseatScrim workspaceAndHotseatScrim, Float f) {
            workspaceAndHotseatScrim.setSysUiProgress(f.floatValue());
        }
    };
    private static Property<WorkspaceAndHotseatScrim, Float> SYSUI_ANIM_MULTIPLIER = new Property<WorkspaceAndHotseatScrim, Float>(Float.TYPE, "sysUiAnimMultiplier") {
        @Override
        public Float get(WorkspaceAndHotseatScrim workspaceAndHotseatScrim) {
            return Float.valueOf(workspaceAndHotseatScrim.mSysUiAnimMultiplier);
        }

        @Override
        public void set(WorkspaceAndHotseatScrim workspaceAndHotseatScrim, Float f) {
            workspaceAndHotseatScrim.mSysUiAnimMultiplier = f.floatValue();
            workspaceAndHotseatScrim.reapplySysUiAlpha();
        }
    };
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.SCREEN_OFF".equals(action)) {
                WorkspaceAndHotseatScrim.this.mAnimateScrimOnNextDraw = true;
            } else if ("android.intent.action.USER_PRESENT".equals(action)) {
                WorkspaceAndHotseatScrim.this.mAnimateScrimOnNextDraw = false;
            }
        }
    };
    private final Rect mHighlightRect = new Rect();
    private final RectF mFinalMaskRect = new RectF();
    private final Paint mBottomMaskPaint = new Paint(2);
    private int mScrimAlpha = 0;
    private float mSysUiProgress = 1.0f;
    private boolean mAnimateScrimOnNextDraw = false;
    private float mSysUiAnimMultiplier = 1.0f;

    public WorkspaceAndHotseatScrim(View view) {
        this.mRoot = view;
        this.mLauncher = Launcher.getLauncher(view.getContext());
        this.mWallpaperColorInfo = WallpaperColorInfo.getInstance(this.mLauncher);
        this.mMaskHeight = Utilities.pxFromDp(200.0f, view.getResources().getDisplayMetrics());
        this.mHasSysUiScrim = !this.mWallpaperColorInfo.supportsDarkText();
        if (this.mHasSysUiScrim) {
            this.mTopScrim = Themes.getAttrDrawable(view.getContext(), R.attr.workspaceStatusBarScrim);
            this.mBottomMask = createDitheredAlphaMask();
        } else {
            this.mTopScrim = null;
            this.mBottomMask = null;
        }
        view.addOnAttachStateChangeListener(this);
        onExtractedColorsChanged(this.mWallpaperColorInfo);
    }

    public void setWorkspace(Workspace workspace) {
        this.mWorkspace = workspace;
    }

    public void draw(Canvas canvas) {
        if (this.mScrimAlpha > 0) {
            this.mWorkspace.computeScrollWithoutInvalidation();
            CellLayout currentDragOverlappingLayout = this.mWorkspace.getCurrentDragOverlappingLayout();
            canvas.save();
            if (currentDragOverlappingLayout != null && currentDragOverlappingLayout != this.mLauncher.getHotseat().getLayout()) {
                this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(currentDragOverlappingLayout, this.mHighlightRect);
                canvas.clipRect(this.mHighlightRect, Region.Op.DIFFERENCE);
            }
            canvas.drawColor(ColorUtils.setAlphaComponent(this.mFullScrimColor, this.mScrimAlpha));
            canvas.restore();
        }
        if (this.mHideSysUiScrim || !this.mHasSysUiScrim) {
            return;
        }
        if (this.mSysUiProgress <= 0.0f) {
            this.mAnimateScrimOnNextDraw = false;
            return;
        }
        if (this.mAnimateScrimOnNextDraw) {
            this.mSysUiAnimMultiplier = 0.0f;
            reapplySysUiAlphaNoInvalidate();
            ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(this, SYSUI_ANIM_MULTIPLIER, 1.0f);
            objectAnimatorOfFloat.setAutoCancel(true);
            objectAnimatorOfFloat.setDuration(600L);
            objectAnimatorOfFloat.setStartDelay(this.mLauncher.getWindow().getTransitionBackgroundFadeDuration());
            objectAnimatorOfFloat.start();
            this.mAnimateScrimOnNextDraw = false;
        }
        if (this.mDrawTopScrim) {
            this.mTopScrim.draw(canvas);
        }
        if (this.mDrawBottomScrim) {
            canvas.drawBitmap(this.mBottomMask, (Rect) null, this.mFinalMaskRect, this.mBottomMaskPaint);
        }
    }

    public void onInsetsChanged(Rect rect) {
        this.mDrawTopScrim = rect.top > 0;
        this.mDrawBottomScrim = !this.mLauncher.getDeviceProfile().isVerticalBarLayout();
    }

    private void setScrimProgress(float f) {
        if (this.mScrimProgress != f) {
            this.mScrimProgress = f;
            this.mScrimAlpha = Math.round(255.0f * this.mScrimProgress);
            invalidate();
        }
    }

    @Override
    public void onViewAttachedToWindow(View view) {
        this.mWallpaperColorInfo.addOnChangeListener(this);
        onExtractedColorsChanged(this.mWallpaperColorInfo);
        if (this.mHasSysUiScrim) {
            IntentFilter intentFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
            intentFilter.addAction("android.intent.action.USER_PRESENT");
            this.mRoot.getContext().registerReceiver(this.mReceiver, intentFilter);
        }
    }

    @Override
    public void onViewDetachedFromWindow(View view) {
        this.mWallpaperColorInfo.removeOnChangeListener(this);
        if (this.mHasSysUiScrim) {
            this.mRoot.getContext().unregisterReceiver(this.mReceiver);
        }
    }

    @Override
    public void onExtractedColorsChanged(WallpaperColorInfo wallpaperColorInfo) {
        this.mBottomMaskPaint.setColor(ColorUtils.compositeColors(DARK_SCRIM_COLOR, wallpaperColorInfo.getMainColor()));
        reapplySysUiAlpha();
        this.mFullScrimColor = wallpaperColorInfo.getMainColor();
        if (this.mScrimAlpha > 0) {
            invalidate();
        }
    }

    public void setSize(int i, int i2) {
        if (this.mHasSysUiScrim) {
            this.mTopScrim.setBounds(0, 0, i, i2);
            this.mFinalMaskRect.set(0.0f, i2 - this.mMaskHeight, i, i2);
        }
    }

    public void hideSysUiScrim(boolean z) {
        this.mHideSysUiScrim = z;
        if (!z) {
            this.mAnimateScrimOnNextDraw = true;
        }
        invalidate();
    }

    private void setSysUiProgress(float f) {
        if (f != this.mSysUiProgress) {
            this.mSysUiProgress = f;
            reapplySysUiAlpha();
        }
    }

    private void reapplySysUiAlpha() {
        if (this.mHasSysUiScrim) {
            reapplySysUiAlphaNoInvalidate();
            if (!this.mHideSysUiScrim) {
                invalidate();
            }
        }
    }

    private void reapplySysUiAlphaNoInvalidate() {
        float f = this.mSysUiProgress * this.mSysUiAnimMultiplier;
        this.mBottomMaskPaint.setAlpha(Math.round(100.0f * f));
        this.mTopScrim.setAlpha(Math.round(255.0f * f));
    }

    public void invalidate() {
        this.mRoot.invalidate();
    }

    public Bitmap createDitheredAlphaMask() {
        DisplayMetrics displayMetrics = this.mLauncher.getResources().getDisplayMetrics();
        int iPxFromDp = Utilities.pxFromDp(2.0f, displayMetrics);
        int iPxFromDp2 = Utilities.pxFromDp(500.0f, displayMetrics);
        Bitmap bitmapCreateBitmap = Bitmap.createBitmap(iPxFromDp, this.mMaskHeight, Bitmap.Config.ALPHA_8);
        Canvas canvas = new Canvas(bitmapCreateBitmap);
        Paint paint = new Paint(4);
        float f = iPxFromDp2;
        paint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, f, new int[]{ViewCompat.MEASURED_SIZE_MASK, ColorUtils.setAlphaComponent(-1, 242), -1}, new float[]{0.0f, 0.8f, 1.0f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0.0f, 0.0f, iPxFromDp, f, paint);
        return bitmapCreateBitmap;
    }
}
