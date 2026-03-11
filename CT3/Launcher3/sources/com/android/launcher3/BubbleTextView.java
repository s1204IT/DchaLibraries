package com.android.launcher3;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Region;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.TextView;
import com.android.launcher3.BaseRecyclerViewFastScrollBar;
import com.android.launcher3.FastBitmapDrawable;
import com.android.launcher3.IconCache;
import com.android.launcher3.compat.PackageInstallerCompat;
import com.android.launcher3.model.PackageItemInfo;
import java.text.NumberFormat;

public class BubbleTextView extends TextView implements BaseRecyclerViewFastScrollBar.FastScrollFocusableView {

    private static final int[] f1comandroidlauncher3FastBitmapDrawable$StateSwitchesValues = null;
    private static SparseArray<Resources.Theme> sPreloaderThemes = new SparseArray<>(2);
    private final Drawable mBackground;
    private boolean mBackgroundSizeChanged;
    private final boolean mCustomShadowsEnabled;
    private final boolean mDeferShadowGenerationOnTouch;
    private boolean mDisableRelayout;
    private Drawable mIcon;
    private IconCache.IconLoadRequest mIconLoadRequest;
    private final int mIconSize;
    private boolean mIgnorePressedStateChange;
    private final Launcher mLauncher;
    private final boolean mLayoutHorizontal;
    private final CheckLongPressHelper mLongPressHelper;
    private final HolographicOutlineHelper mOutlineHelper;
    private Bitmap mPressedBackground;
    private float mSlop;
    private boolean mStayPressed;
    private final StylusEventHelper mStylusEventHelper;
    private int mTextColor;

    public interface BubbleTextShadowHandler {
        void setPressedIcon(BubbleTextView bubbleTextView, Bitmap bitmap);
    }

    private static int[] m98getcomandroidlauncher3FastBitmapDrawable$StateSwitchesValues() {
        if (f1comandroidlauncher3FastBitmapDrawable$StateSwitchesValues != null) {
            return f1comandroidlauncher3FastBitmapDrawable$StateSwitchesValues;
        }
        int[] iArr = new int[FastBitmapDrawable.State.valuesCustom().length];
        try {
            iArr[FastBitmapDrawable.State.DISABLED.ordinal()] = 3;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[FastBitmapDrawable.State.FAST_SCROLL_HIGHLIGHTED.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[FastBitmapDrawable.State.FAST_SCROLL_UNHIGHLIGHTED.ordinal()] = 4;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[FastBitmapDrawable.State.NORMAL.ordinal()] = 2;
        } catch (NoSuchFieldError e4) {
        }
        try {
            iArr[FastBitmapDrawable.State.PRESSED.ordinal()] = 5;
        } catch (NoSuchFieldError e5) {
        }
        f1comandroidlauncher3FastBitmapDrawable$StateSwitchesValues = iArr;
        return iArr;
    }

    public BubbleTextView(Context context) {
        this(context, null, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BubbleTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mDisableRelayout = false;
        this.mLauncher = (Launcher) context;
        DeviceProfile grid = this.mLauncher.getDeviceProfile();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BubbleTextView, defStyle, 0);
        this.mCustomShadowsEnabled = a.getBoolean(4, true);
        this.mLayoutHorizontal = a.getBoolean(0, false);
        this.mDeferShadowGenerationOnTouch = a.getBoolean(3, false);
        int display = a.getInteger(2, 0);
        int defaultIconSize = grid.iconSizePx;
        if (display == 0) {
            setTextSize(0, grid.iconTextSizePx);
        } else if (display == 1) {
            setTextSize(2, grid.allAppsIconTextSizeSp);
            defaultIconSize = grid.allAppsIconSizePx;
        }
        this.mIconSize = a.getDimensionPixelSize(1, defaultIconSize);
        a.recycle();
        if (this.mCustomShadowsEnabled) {
            this.mBackground = getBackground();
            setBackground(null);
        } else {
            this.mBackground = null;
        }
        this.mLongPressHelper = new CheckLongPressHelper(this);
        this.mStylusEventHelper = new StylusEventHelper(this);
        this.mOutlineHelper = HolographicOutlineHelper.obtain(getContext());
        if (this.mCustomShadowsEnabled) {
            setShadowLayer(4.0f, 0.0f, 2.0f, -587202560);
        }
        setAccessibilityDelegate(LauncherAppState.getInstance().getAccessibilityDelegate());
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache) {
        applyFromShortcutInfo(info, iconCache, false);
    }

    public void applyFromShortcutInfo(ShortcutInfo info, IconCache iconCache, boolean promiseStateChanged) {
        Bitmap b = info.getIcon(iconCache);
        FastBitmapDrawable iconDrawable = this.mLauncher.createIconDrawable(b);
        if (info.isDisabled()) {
            iconDrawable.setState(FastBitmapDrawable.State.DISABLED);
        }
        setIcon(iconDrawable, this.mIconSize);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        setText(info.title);
        setTag(info);
        if (!promiseStateChanged && !info.isPromise()) {
            return;
        }
        applyState(promiseStateChanged);
    }

    public void applyFromApplicationInfo(AppInfo info) {
        FastBitmapDrawable iconDrawable = this.mLauncher.createIconDrawable(info.iconBitmap);
        if (info.isDisabled()) {
            iconDrawable.setState(FastBitmapDrawable.State.DISABLED);
        }
        setIcon(iconDrawable, this.mIconSize);
        setText(info.title);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        super.setTag(info);
        verifyHighRes();
    }

    public void applyFromPackageItemInfo(PackageItemInfo info) {
        setIcon(this.mLauncher.createIconDrawable(info.iconBitmap), this.mIconSize);
        setText(info.title);
        if (info.contentDescription != null) {
            setContentDescription(info.contentDescription);
        }
        super.setTag(info);
        verifyHighRes();
    }

    public void applyDummyInfo() {
        ColorDrawable d = new ColorDrawable();
        setIcon(this.mLauncher.resizeIconDrawable(d), this.mIconSize);
        setText("");
    }

    public void setLongPressTimeout(int longPressTimeout) {
        this.mLongPressHelper.setLongPressTimeout(longPressTimeout);
    }

    @Override
    protected boolean setFrame(int left, int top, int right, int bottom) {
        if (getLeft() != left || getRight() != right || getTop() != top || getBottom() != bottom) {
            this.mBackgroundSizeChanged = true;
        }
        return super.setFrame(left, top, right, bottom);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        if (who != this.mBackground) {
            return super.verifyDrawable(who);
        }
        return true;
    }

    @Override
    public void setTag(Object tag) {
        if (tag != null) {
            LauncherModel.checkItemInfo((ItemInfo) tag);
        }
        super.setTag(tag);
    }

    @Override
    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        if (this.mIgnorePressedStateChange) {
            return;
        }
        updateIconState();
    }

    public Drawable getIcon() {
        return this.mIcon;
    }

    public boolean isLayoutHorizontal() {
        return this.mLayoutHorizontal;
    }

    private void updateIconState() {
        if (!(this.mIcon instanceof FastBitmapDrawable)) {
            return;
        }
        FastBitmapDrawable d = (FastBitmapDrawable) this.mIcon;
        if ((getTag() instanceof ItemInfo) && ((ItemInfo) getTag()).isDisabled()) {
            d.animateState(FastBitmapDrawable.State.DISABLED);
        } else if (isPressed() || this.mStayPressed) {
            d.animateState(FastBitmapDrawable.State.PRESSED);
        } else {
            d.animateState(FastBitmapDrawable.State.NORMAL);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean result = super.onTouchEvent(event);
        if (this.mStylusEventHelper.checkAndPerformStylusEvent(event)) {
            this.mLongPressHelper.cancelLongPress();
            result = true;
        }
        switch (event.getAction()) {
            case PackageInstallerCompat.STATUS_INSTALLED:
                if (!this.mDeferShadowGenerationOnTouch && this.mPressedBackground == null) {
                    this.mPressedBackground = this.mOutlineHelper.createMediumDropShadow(this);
                }
                if (!this.mStylusEventHelper.inStylusButtonPressed()) {
                    this.mLongPressHelper.postCheckForLongPress();
                }
                return result;
            case PackageInstallerCompat.STATUS_INSTALLING:
            case 3:
                if (!isPressed()) {
                    this.mPressedBackground = null;
                }
                this.mLongPressHelper.cancelLongPress();
                return result;
            case PackageInstallerCompat.STATUS_FAILED:
                if (!Utilities.pointInView(this, event.getX(), event.getY(), this.mSlop)) {
                    this.mLongPressHelper.cancelLongPress();
                }
                return result;
            default:
                return result;
        }
    }

    void setStayPressed(boolean stayPressed) {
        this.mStayPressed = stayPressed;
        if (!stayPressed) {
            this.mPressedBackground = null;
        } else if (this.mPressedBackground == null) {
            this.mPressedBackground = this.mOutlineHelper.createMediumDropShadow(this);
        }
        ViewParent parent = getParent();
        if (parent != null && (parent.getParent() instanceof BubbleTextShadowHandler)) {
            ((BubbleTextShadowHandler) parent.getParent()).setPressedIcon(this, this.mPressedBackground);
        }
        updateIconState();
    }

    void clearPressedBackground() {
        setPressed(false);
        setStayPressed(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (super.onKeyDown(keyCode, event)) {
            if (this.mPressedBackground == null) {
                this.mPressedBackground = this.mOutlineHelper.createMediumDropShadow(this);
                return true;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        this.mIgnorePressedStateChange = true;
        boolean result = super.onKeyUp(keyCode, event);
        this.mPressedBackground = null;
        this.mIgnorePressedStateChange = false;
        updateIconState();
        return result;
    }

    @Override
    public void draw(Canvas canvas) {
        if (!this.mCustomShadowsEnabled) {
            super.draw(canvas);
            return;
        }
        Drawable background = this.mBackground;
        if (background != null) {
            int scrollX = getScrollX();
            int scrollY = getScrollY();
            if (this.mBackgroundSizeChanged) {
                background.setBounds(0, 0, getRight() - getLeft(), getBottom() - getTop());
                this.mBackgroundSizeChanged = false;
            }
            if ((scrollX | scrollY) == 0) {
                background.draw(canvas);
            } else {
                canvas.translate(scrollX, scrollY);
                background.draw(canvas);
                canvas.translate(-scrollX, -scrollY);
            }
        }
        if (getCurrentTextColor() == getResources().getColor(android.R.color.transparent)) {
            getPaint().clearShadowLayer();
            super.draw(canvas);
            return;
        }
        getPaint().setShadowLayer(4.0f, 0.0f, 2.0f, -587202560);
        super.draw(canvas);
        canvas.save(2);
        canvas.clipRect(getScrollX(), getScrollY() + getExtendedPaddingTop(), getScrollX() + getWidth(), getScrollY() + getHeight(), Region.Op.INTERSECT);
        getPaint().setShadowLayer(1.75f, 0.0f, 0.0f, -872415232);
        super.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mBackground != null) {
            this.mBackground.setCallback(this);
        }
        if (this.mIcon instanceof PreloadIconDrawable) {
            ((PreloadIconDrawable) this.mIcon).applyPreloaderTheme(getPreloaderTheme());
        }
        this.mSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mBackground != null) {
            this.mBackground.setCallback(null);
        }
    }

    @Override
    public void setTextColor(int color) {
        this.mTextColor = color;
        super.setTextColor(color);
    }

    @Override
    public void setTextColor(ColorStateList colors) {
        this.mTextColor = colors.getDefaultColor();
        super.setTextColor(colors);
    }

    public void setTextVisibility(boolean visible) {
        Resources res = getResources();
        if (visible) {
            super.setTextColor(this.mTextColor);
        } else {
            super.setTextColor(res.getColor(android.R.color.transparent));
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();
        this.mLongPressHelper.cancelLongPress();
    }

    public void applyState(boolean promiseStateChanged) {
        int progressLevel;
        String string;
        PreloadIconDrawable preloadDrawable;
        if (!(getTag() instanceof ShortcutInfo)) {
            return;
        }
        ShortcutInfo info = (ShortcutInfo) getTag();
        boolean isPromise = info.isPromise();
        if (isPromise) {
            progressLevel = info.hasStatusFlag(4) ? info.getInstallProgress() : 0;
        } else {
            progressLevel = 100;
        }
        if (progressLevel > 0) {
            string = getContext().getString(R.string.app_downloading_title, info.title, NumberFormat.getPercentInstance().format(((double) progressLevel) * 0.01d));
        } else {
            string = getContext().getString(R.string.app_waiting_download_title, info.title);
        }
        setContentDescription(string);
        if (this.mIcon == null) {
            return;
        }
        if (this.mIcon instanceof PreloadIconDrawable) {
            preloadDrawable = (PreloadIconDrawable) this.mIcon;
        } else {
            preloadDrawable = new PreloadIconDrawable(this.mIcon, getPreloaderTheme());
            setIcon(preloadDrawable, this.mIconSize);
        }
        preloadDrawable.setLevel(progressLevel);
        if (!promiseStateChanged) {
            return;
        }
        preloadDrawable.maybePerformFinishedAnimation();
    }

    private Resources.Theme getPreloaderTheme() {
        Object tag = getTag();
        int style = (tag == null || !(tag instanceof ShortcutInfo) || ((ShortcutInfo) tag).container < 0) ? R.style.PreloadIcon : R.style.PreloadIcon_Folder;
        Resources.Theme theme = sPreloaderThemes.get(style);
        if (theme == null) {
            Resources.Theme theme2 = getResources().newTheme();
            theme2.applyStyle(style, true);
            sPreloaderThemes.put(style, theme2);
            return theme2;
        }
        return theme;
    }

    @TargetApi(17)
    private Drawable setIcon(Drawable icon, int iconSize) {
        this.mIcon = icon;
        if (iconSize != -1) {
            this.mIcon.setBounds(0, 0, iconSize, iconSize);
        }
        if (this.mLayoutHorizontal) {
            if (Utilities.ATLEAST_JB_MR1) {
                setCompoundDrawablesRelative(this.mIcon, null, null, null);
            } else {
                setCompoundDrawables(this.mIcon, null, null, null);
            }
        } else {
            setCompoundDrawables(null, this.mIcon, null, null);
        }
        return icon;
    }

    @Override
    public void requestLayout() {
        if (this.mDisableRelayout) {
            return;
        }
        super.requestLayout();
    }

    public void reapplyItemInfo(ItemInfo info) {
        View folderIcon;
        if (getTag() != info) {
            return;
        }
        FastBitmapDrawable.State prevState = FastBitmapDrawable.State.NORMAL;
        if (this.mIcon instanceof FastBitmapDrawable) {
            prevState = ((FastBitmapDrawable) this.mIcon).getCurrentState();
        }
        this.mIconLoadRequest = null;
        this.mDisableRelayout = true;
        if (info instanceof AppInfo) {
            applyFromApplicationInfo((AppInfo) info);
        } else if (info instanceof ShortcutInfo) {
            applyFromShortcutInfo((ShortcutInfo) info, LauncherAppState.getInstance().getIconCache());
            if (info.rank < 3 && info.container >= 0 && (folderIcon = this.mLauncher.getWorkspace().getHomescreenIconByItemId(info.container)) != null) {
                folderIcon.invalidate();
            }
        } else if (info instanceof PackageItemInfo) {
            applyFromPackageItemInfo((PackageItemInfo) info);
        }
        if (this.mIcon instanceof FastBitmapDrawable) {
            ((FastBitmapDrawable) this.mIcon).setState(prevState);
        }
        this.mDisableRelayout = false;
    }

    public void verifyHighRes() {
        if (this.mIconLoadRequest != null) {
            this.mIconLoadRequest.cancel();
            this.mIconLoadRequest = null;
        }
        if (getTag() instanceof AppInfo) {
            AppInfo info = (AppInfo) getTag();
            if (!info.usingLowResIcon) {
                return;
            }
            this.mIconLoadRequest = LauncherAppState.getInstance().getIconCache().updateIconInBackground(this, info);
            return;
        }
        if (getTag() instanceof ShortcutInfo) {
            ShortcutInfo info2 = (ShortcutInfo) getTag();
            if (!info2.usingLowResIcon) {
                return;
            }
            this.mIconLoadRequest = LauncherAppState.getInstance().getIconCache().updateIconInBackground(this, info2);
            return;
        }
        if (!(getTag() instanceof PackageItemInfo)) {
            return;
        }
        PackageItemInfo info3 = (PackageItemInfo) getTag();
        if (!info3.usingLowResIcon) {
            return;
        }
        this.mIconLoadRequest = LauncherAppState.getInstance().getIconCache().updateIconInBackground(this, info3);
    }

    @Override
    public void setFastScrollFocusState(FastBitmapDrawable.State focusState, boolean animated) {
        if (this.mIcon instanceof FastBitmapDrawable) {
            FastBitmapDrawable d = (FastBitmapDrawable) this.mIcon;
            if (animated) {
                FastBitmapDrawable.State prevState = d.getCurrentState();
                if (d.animateState(focusState)) {
                    animate().scaleX(focusState.viewScale).scaleY(focusState.viewScale).setStartDelay(getStartDelayForStateChange(prevState, focusState)).setDuration(FastBitmapDrawable.getDurationForStateChange(prevState, focusState)).start();
                    return;
                }
                return;
            }
            if (d.setState(focusState)) {
                animate().cancel();
                setScaleX(focusState.viewScale);
                setScaleY(focusState.viewScale);
            }
        }
    }

    private static int getStartDelayForStateChange(FastBitmapDrawable.State fromState, FastBitmapDrawable.State toState) {
        switch (m98getcomandroidlauncher3FastBitmapDrawable$StateSwitchesValues()[toState.ordinal()]) {
            case PackageInstallerCompat.STATUS_FAILED:
                switch (m98getcomandroidlauncher3FastBitmapDrawable$StateSwitchesValues()[fromState.ordinal()]) {
                    case PackageInstallerCompat.STATUS_INSTALLING:
                        return 68;
                    default:
                        return 0;
                }
            default:
                return 0;
        }
    }
}
