package com.android.launcher3.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.IconCache;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.LauncherAppWidgetInfo;
import com.android.launcher3.R;
import com.android.launcher3.graphics.DrawableFactory;
import com.android.launcher3.model.PackageItemInfo;
import com.android.launcher3.touch.ItemClickHandler;
import com.android.launcher3.util.Themes;

public class PendingAppWidgetHostView extends LauncherAppWidgetHostView implements View.OnClickListener, IconCache.ItemInfoUpdateReceiver {
    private static final float MIN_SATUNATION = 0.7f;
    private static final float SETUP_ICON_SIZE_FACTOR = 0.4f;
    private Drawable mCenterDrawable;
    private View.OnClickListener mClickListener;
    private View mDefaultView;
    private final boolean mDisabledForSafeMode;
    private boolean mDrawableSizeChanged;
    private final LauncherAppWidgetInfo mInfo;
    private final TextPaint mPaint;
    private final Rect mRect;
    private Drawable mSettingIconDrawable;
    private Layout mSetupTextLayout;
    private final int mStartState;

    public PendingAppWidgetHostView(Context context, LauncherAppWidgetInfo launcherAppWidgetInfo, IconCache iconCache, boolean z) {
        super(new ContextThemeWrapper(context, R.style.WidgetContainerTheme));
        this.mRect = new Rect();
        this.mInfo = launcherAppWidgetInfo;
        this.mStartState = launcherAppWidgetInfo.restoreStatus;
        this.mDisabledForSafeMode = z;
        this.mPaint = new TextPaint();
        this.mPaint.setColor(Themes.getAttrColor(getContext(), android.R.attr.textColorPrimary));
        this.mPaint.setTextSize(TypedValue.applyDimension(0, this.mLauncher.getDeviceProfile().iconTextSizePx, getResources().getDisplayMetrics()));
        setBackgroundResource(R.drawable.pending_widget_bg);
        setWillNotDraw(false);
        setElevation(getResources().getDimension(R.dimen.pending_widget_elevation));
        updateAppWidget(null);
        setOnClickListener(ItemClickHandler.INSTANCE);
        if (launcherAppWidgetInfo.pendingItemInfo == null) {
            launcherAppWidgetInfo.pendingItemInfo = new PackageItemInfo(launcherAppWidgetInfo.providerName.getPackageName());
            launcherAppWidgetInfo.pendingItemInfo.user = launcherAppWidgetInfo.user;
            iconCache.updateIconInBackground(this, launcherAppWidgetInfo.pendingItemInfo);
            return;
        }
        reapplyItemInfo(launcherAppWidgetInfo.pendingItemInfo);
    }

    @Override
    public void updateAppWidgetSize(Bundle bundle, int i, int i2, int i3, int i4) {
    }

    @Override
    protected View getDefaultView() {
        if (this.mDefaultView == null) {
            this.mDefaultView = this.mInflater.inflate(R.layout.appwidget_not_ready, (ViewGroup) this, false);
            this.mDefaultView.setOnClickListener(this);
            applyState();
        }
        return this.mDefaultView;
    }

    @Override
    public void setOnClickListener(View.OnClickListener onClickListener) {
        this.mClickListener = onClickListener;
    }

    public boolean isReinflateIfNeeded() {
        return this.mStartState != this.mInfo.restoreStatus;
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        this.mDrawableSizeChanged = true;
    }

    @Override
    public void reapplyItemInfo(ItemInfoWithIcon itemInfoWithIcon) {
        if (this.mCenterDrawable != null) {
            this.mCenterDrawable.setCallback(null);
            this.mCenterDrawable = null;
        }
        if (itemInfoWithIcon.iconBitmap != null) {
            DrawableFactory drawableFactory = DrawableFactory.get(getContext());
            if (this.mDisabledForSafeMode) {
                FastBitmapDrawable fastBitmapDrawableNewIcon = drawableFactory.newIcon(itemInfoWithIcon);
                fastBitmapDrawableNewIcon.setIsDisabled(true);
                this.mCenterDrawable = fastBitmapDrawableNewIcon;
                this.mSettingIconDrawable = null;
            } else if (isReadyForClickSetup()) {
                this.mCenterDrawable = drawableFactory.newIcon(itemInfoWithIcon);
                this.mSettingIconDrawable = getResources().getDrawable(R.drawable.ic_setting).mutate();
                updateSettingColor(itemInfoWithIcon.iconColor);
            } else {
                this.mCenterDrawable = DrawableFactory.get(getContext()).newPendingIcon(itemInfoWithIcon, getContext());
                this.mSettingIconDrawable = null;
                applyState();
            }
            this.mCenterDrawable.setCallback(this);
            this.mDrawableSizeChanged = true;
        }
        invalidate();
    }

    private void updateSettingColor(int i) {
        float[] fArr = new float[3];
        Color.colorToHSV(i, fArr);
        fArr[1] = Math.min(fArr[1], MIN_SATUNATION);
        fArr[2] = 1.0f;
        this.mSettingIconDrawable.setColorFilter(Color.HSVToColor(fArr), PorterDuff.Mode.SRC_IN);
    }

    @Override
    protected boolean verifyDrawable(Drawable drawable) {
        return drawable == this.mCenterDrawable || super.verifyDrawable(drawable);
    }

    public void applyState() {
        if (this.mCenterDrawable != null) {
            this.mCenterDrawable.setLevel(Math.max(this.mInfo.installProgress, 0));
        }
    }

    @Override
    public void onClick(View view) {
        if (this.mClickListener != null) {
            this.mClickListener.onClick(this);
        }
    }

    public boolean isReadyForClickSetup() {
        return !this.mInfo.hasRestoreFlag(2) && (this.mInfo.hasRestoreFlag(4) || this.mInfo.hasRestoreFlag(1));
    }

    private void updateDrawableBounds() {
        DeviceProfile deviceProfile = this.mLauncher.getDeviceProfile();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.pending_widget_min_padding);
        int i = 2 * dimensionPixelSize;
        int width = ((getWidth() - paddingLeft) - paddingRight) - i;
        int height = ((getHeight() - paddingTop) - paddingBottom) - i;
        if (this.mSettingIconDrawable == null) {
            int iMin = Math.min(deviceProfile.iconSizePx, Math.min(width, height));
            this.mRect.set(0, 0, iMin, iMin);
            this.mRect.offsetTo((getWidth() - this.mRect.width()) / 2, (getHeight() - this.mRect.height()) / 2);
            this.mCenterDrawable.setBounds(this.mRect);
            return;
        }
        float fMax = Math.max(0, Math.min(width, height));
        float f = fMax * 1.8f;
        float fMax2 = Math.max(width, height);
        if (f > fMax2) {
            fMax = fMax2 / 1.8f;
        }
        int iMin2 = (int) Math.min(fMax, deviceProfile.iconSizePx);
        int height2 = (getHeight() - iMin2) / 2;
        this.mSetupTextLayout = null;
        if (width > 0) {
            this.mSetupTextLayout = new StaticLayout(getResources().getText(R.string.gadget_setup_text), this.mPaint, width, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, true);
            int height3 = this.mSetupTextLayout.getHeight();
            if (height3 + (iMin2 * 1.8f) + deviceProfile.iconDrawablePaddingPx >= height) {
                this.mSetupTextLayout = null;
            } else {
                height2 = (((getHeight() - height3) - deviceProfile.iconDrawablePaddingPx) - iMin2) / 2;
            }
        }
        this.mRect.set(0, 0, iMin2, iMin2);
        this.mRect.offset((getWidth() - iMin2) / 2, height2);
        this.mCenterDrawable.setBounds(this.mRect);
        int i2 = paddingLeft + dimensionPixelSize;
        this.mRect.left = i2;
        int i3 = (int) (0.4f * iMin2);
        this.mRect.right = this.mRect.left + i3;
        this.mRect.top = paddingTop + dimensionPixelSize;
        this.mRect.bottom = this.mRect.top + i3;
        this.mSettingIconDrawable.setBounds(this.mRect);
        if (this.mSetupTextLayout != null) {
            this.mRect.left = i2;
            this.mRect.top = this.mCenterDrawable.getBounds().bottom + deviceProfile.iconDrawablePaddingPx;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.mCenterDrawable == null) {
            return;
        }
        if (this.mDrawableSizeChanged) {
            updateDrawableBounds();
            this.mDrawableSizeChanged = false;
        }
        this.mCenterDrawable.draw(canvas);
        if (this.mSettingIconDrawable != null) {
            this.mSettingIconDrawable.draw(canvas);
        }
        if (this.mSetupTextLayout != null) {
            canvas.save();
            canvas.translate(this.mRect.left, this.mRect.top);
            this.mSetupTextLayout.draw(canvas);
            canvas.restore();
        }
    }
}
