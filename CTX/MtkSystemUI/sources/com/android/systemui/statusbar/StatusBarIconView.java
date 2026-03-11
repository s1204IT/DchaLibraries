package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;
import android.support.v4.graphics.ColorUtils;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.util.Log;
import android.util.Property;
import android.util.TypedValue;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.statusbar.notification.NotificationIconDozeHelper;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.function.Consumer;

public class StatusBarIconView extends AnimatedImageView implements StatusIconDisplayable {
    private final int ANIMATION_DURATION_FAST;
    private boolean mAlwaysScaleIcon;
    private int mAnimationStartColor;
    private final boolean mBlocked;
    private int mCachedContrastBackgroundColor;
    private ValueAnimator mColorAnimator;
    private final ValueAnimator.AnimatorUpdateListener mColorUpdater;
    private int mContrastedDrawableColor;
    private int mCurrentSetColor;
    private float mDarkAmount;
    private int mDecorColor;
    private int mDensity;
    private boolean mDismissed;
    private ObjectAnimator mDotAnimator;
    private float mDotAppearAmount;
    private final Paint mDotPaint;
    private float mDotRadius;
    private final NotificationIconDozeHelper mDozer;
    private int mDrawableColor;
    private StatusBarIcon mIcon;
    private float mIconAppearAmount;
    private ObjectAnimator mIconAppearAnimator;
    private int mIconColor;
    private float mIconScale;
    private boolean mIsInShelf;
    private Runnable mLayoutRunnable;
    private float[] mMatrix;
    private ColorMatrixColorFilter mMatrixColorFilter;
    private StatusBarNotification mNotification;
    private Drawable mNumberBackground;
    private Paint mNumberPain;
    private String mNumberText;
    private int mNumberX;
    private int mNumberY;
    private Runnable mOnDismissListener;
    private OnVisibilityChangedListener mOnVisibilityChangedListener;

    @ViewDebug.ExportedProperty
    private String mSlot;
    private int mStaticDotRadius;
    private int mStatusBarIconDrawingSize;
    private int mStatusBarIconDrawingSizeDark;
    private int mStatusBarIconSize;
    private int mVisibleState;
    private static final Property<StatusBarIconView, Float> ICON_APPEAR_AMOUNT = new FloatProperty<StatusBarIconView>("iconAppearAmount") {
        @Override
        public void setValue(StatusBarIconView statusBarIconView, float f) {
            statusBarIconView.setIconAppearAmount(f);
        }

        @Override
        public Float get(StatusBarIconView statusBarIconView) {
            return Float.valueOf(statusBarIconView.getIconAppearAmount());
        }
    };
    private static final Property<StatusBarIconView, Float> DOT_APPEAR_AMOUNT = new FloatProperty<StatusBarIconView>("dot_appear_amount") {
        @Override
        public void setValue(StatusBarIconView statusBarIconView, float f) {
            statusBarIconView.setDotAppearAmount(f);
        }

        @Override
        public Float get(StatusBarIconView statusBarIconView) {
            return Float.valueOf(statusBarIconView.getDotAppearAmount());
        }
    };

    public interface OnVisibilityChangedListener {
        void onVisibilityChanged(int i);
    }

    public StatusBarIconView(Context context, String str, StatusBarNotification statusBarNotification) {
        this(context, str, statusBarNotification, false);
    }

    public StatusBarIconView(Context context, String str, StatusBarNotification statusBarNotification, boolean z) {
        super(context);
        this.ANIMATION_DURATION_FAST = 100;
        this.mStatusBarIconDrawingSizeDark = 1;
        this.mStatusBarIconDrawingSize = 1;
        this.mStatusBarIconSize = 1;
        this.mIconScale = 1.0f;
        this.mDotPaint = new Paint(1);
        this.mVisibleState = 0;
        this.mIconAppearAmount = 1.0f;
        this.mCurrentSetColor = 0;
        this.mAnimationStartColor = 0;
        this.mColorUpdater = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                StatusBarIconView statusBarIconView = this.f$0;
                statusBarIconView.setColorInternal(NotificationUtils.interpolateColors(statusBarIconView.mAnimationStartColor, statusBarIconView.mIconColor, valueAnimator.getAnimatedFraction()));
            }
        };
        this.mCachedContrastBackgroundColor = 0;
        this.mDozer = new NotificationIconDozeHelper(context);
        this.mBlocked = z;
        this.mSlot = str;
        this.mNumberPain = new Paint();
        this.mNumberPain.setTextAlign(Paint.Align.CENTER);
        this.mNumberPain.setColor(context.getColor(R.drawable.notification_number_text_color));
        this.mNumberPain.setAntiAlias(true);
        setNotification(statusBarNotification);
        setScaleType(ImageView.ScaleType.CENTER);
        this.mDensity = context.getResources().getDisplayMetrics().densityDpi;
        if (this.mNotification != null) {
            setDecorColor(getContext().getColor(android.R.color.accessibility_magnification_background));
        }
        reloadDimens();
        maybeUpdateIconScaleDimens();
    }

    private void maybeUpdateIconScaleDimens() {
        if (this.mNotification != null || this.mAlwaysScaleIcon) {
            updateIconScaleForNotifications();
        } else {
            updateIconScaleForSystemIcons();
        }
    }

    private void updateIconScaleForNotifications() {
        this.mIconScale = NotificationUtils.interpolate(this.mStatusBarIconDrawingSize, this.mStatusBarIconDrawingSizeDark, this.mDarkAmount) / this.mStatusBarIconSize;
        updatePivot();
    }

    private void updateIconScaleForSystemIcons() {
        this.mIconScale = 0.88235295f;
    }

    public float getIconScaleFullyDark() {
        return this.mStatusBarIconDrawingSizeDark / this.mStatusBarIconDrawingSize;
    }

    public float getIconScale() {
        return this.mIconScale;
    }

    @Override
    protected void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        int i = configuration.densityDpi;
        if (i != this.mDensity) {
            this.mDensity = i;
            reloadDimens();
            maybeUpdateIconScaleDimens();
            updateDrawable();
        }
    }

    private void reloadDimens() {
        boolean z = this.mDotRadius == ((float) this.mStaticDotRadius);
        Resources resources = getResources();
        this.mStaticDotRadius = resources.getDimensionPixelSize(R.dimen.overflow_dot_radius);
        this.mStatusBarIconSize = resources.getDimensionPixelSize(R.dimen.status_bar_icon_size);
        this.mStatusBarIconDrawingSizeDark = resources.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size_dark);
        this.mStatusBarIconDrawingSize = resources.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size);
        if (z) {
            this.mDotRadius = this.mStaticDotRadius;
        }
    }

    public void setNotification(StatusBarNotification statusBarNotification) {
        this.mNotification = statusBarNotification;
        if (statusBarNotification != null) {
            setContentDescription(statusBarNotification.getNotification());
        }
    }

    public StatusBarIconView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.ANIMATION_DURATION_FAST = 100;
        this.mStatusBarIconDrawingSizeDark = 1;
        this.mStatusBarIconDrawingSize = 1;
        this.mStatusBarIconSize = 1;
        this.mIconScale = 1.0f;
        this.mDotPaint = new Paint(1);
        this.mVisibleState = 0;
        this.mIconAppearAmount = 1.0f;
        this.mCurrentSetColor = 0;
        this.mAnimationStartColor = 0;
        this.mColorUpdater = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public final void onAnimationUpdate(ValueAnimator valueAnimator) {
                StatusBarIconView statusBarIconView = this.f$0;
                statusBarIconView.setColorInternal(NotificationUtils.interpolateColors(statusBarIconView.mAnimationStartColor, statusBarIconView.mIconColor, valueAnimator.getAnimatedFraction()));
            }
        };
        this.mCachedContrastBackgroundColor = 0;
        this.mDozer = new NotificationIconDozeHelper(context);
        this.mBlocked = false;
        this.mAlwaysScaleIcon = true;
        reloadDimens();
        updateIconScaleForNotifications();
        this.mDensity = context.getResources().getDisplayMetrics().densityDpi;
    }

    public boolean equalIcons(Icon icon, Icon icon2) {
        if (icon == icon2) {
            return true;
        }
        if (icon.getType() != icon2.getType()) {
            return false;
        }
        int type = icon.getType();
        if (type == 2) {
            return icon.getResPackage().equals(icon2.getResPackage()) && icon.getResId() == icon2.getResId();
        }
        if (type != 4) {
            return false;
        }
        return icon.getUriString().equals(icon2.getUriString());
    }

    public boolean set(StatusBarIcon statusBarIcon) {
        boolean z = this.mIcon != null && equalIcons(this.mIcon.icon, statusBarIcon.icon);
        boolean z2 = z && this.mIcon.iconLevel == statusBarIcon.iconLevel;
        boolean z3 = this.mIcon != null && this.mIcon.visible == statusBarIcon.visible;
        boolean z4 = this.mIcon != null && this.mIcon.number == statusBarIcon.number;
        this.mIcon = statusBarIcon.clone();
        setContentDescription(statusBarIcon.contentDescription);
        if (!z) {
            if (!updateDrawable(false)) {
                return false;
            }
            setTag(R.id.icon_is_grayscale, null);
        }
        if (!z2) {
            setImageLevel(statusBarIcon.iconLevel);
        }
        if (!z4) {
            if (statusBarIcon.number > 0 && getContext().getResources().getBoolean(R.bool.config_statusBarShowNumber)) {
                if (this.mNumberBackground == null) {
                    this.mNumberBackground = getContext().getResources().getDrawable(R.drawable.ic_notification_overlay);
                }
                placeNumber();
            } else {
                this.mNumberBackground = null;
                this.mNumberText = null;
            }
            invalidate();
        }
        if (!z3) {
            setVisibility((!statusBarIcon.visible || this.mBlocked) ? 8 : 0);
        }
        return true;
    }

    public void updateDrawable() {
        updateDrawable(true);
    }

    private boolean updateDrawable(boolean z) {
        if (this.mIcon == null) {
            return false;
        }
        try {
            Drawable icon = getIcon(this.mIcon);
            if (icon == null) {
                Log.w("StatusBarIconView", "No icon for slot " + this.mSlot + "; " + this.mIcon.icon);
                return false;
            }
            if (icon instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) icon;
                if (bitmapDrawable.getBitmap() != null) {
                    int byteCount = bitmapDrawable.getBitmap().getByteCount();
                    if (byteCount > 104857600) {
                        Log.w("StatusBarIconView", "Drawable is too large (" + byteCount + " bytes) " + this.mIcon);
                        return false;
                    }
                } else if (icon.getIntrinsicWidth() > 5000 || icon.getIntrinsicHeight() > 5000) {
                    Log.w("StatusBarIconView", "Drawable is too large (" + icon.getIntrinsicWidth() + "x" + icon.getIntrinsicHeight() + ") " + this.mIcon);
                    return false;
                }
            }
            if (z) {
                setImageDrawable(null);
            }
            setImageDrawable(icon);
            return true;
        } catch (OutOfMemoryError e) {
            Log.w("StatusBarIconView", "OOM while inflating " + this.mIcon.icon + " for slot " + this.mSlot);
            return false;
        }
    }

    public Icon getSourceIcon() {
        return this.mIcon.icon;
    }

    private Drawable getIcon(StatusBarIcon statusBarIcon) {
        return getIcon(getContext(), statusBarIcon);
    }

    public static Drawable getIcon(Context context, StatusBarIcon statusBarIcon) {
        int identifier = statusBarIcon.user.getIdentifier();
        if (identifier == -1) {
            identifier = 0;
        }
        Drawable drawableLoadDrawableAsUser = statusBarIcon.icon.loadDrawableAsUser(context, identifier);
        TypedValue typedValue = new TypedValue();
        context.getResources().getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float f = typedValue.getFloat();
        if (f == 1.0f) {
            return drawableLoadDrawableAsUser;
        }
        return new ScalingDrawableWrapper(drawableLoadDrawableAsUser, f);
    }

    public StatusBarIcon getStatusBarIcon() {
        return this.mIcon;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        if (this.mNotification != null) {
            accessibilityEvent.setParcelableData(this.mNotification.getNotification());
        }
    }

    @Override
    protected void onSizeChanged(int i, int i2, int i3, int i4) {
        super.onSizeChanged(i, i2, i3, i4);
        if (this.mNumberBackground != null) {
            placeNumber();
        }
    }

    @Override
    public void onRtlPropertiesChanged(int i) {
        super.onRtlPropertiesChanged(i);
        updateDrawable();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float fInterpolate;
        if (this.mIconAppearAmount > 0.0f) {
            canvas.save();
            canvas.scale(this.mIconScale * this.mIconAppearAmount, this.mIconScale * this.mIconAppearAmount, getWidth() / 2, getHeight() / 2);
            super.onDraw(canvas);
            canvas.restore();
        }
        if (this.mNumberBackground != null) {
            this.mNumberBackground.draw(canvas);
            canvas.drawText(this.mNumberText, this.mNumberX, this.mNumberY, this.mNumberPain);
        }
        if (this.mDotAppearAmount != 0.0f) {
            float fAlpha = Color.alpha(this.mDecorColor) / 255.0f;
            if (this.mDotAppearAmount <= 1.0f) {
                fInterpolate = this.mDotRadius * this.mDotAppearAmount;
            } else {
                float f = this.mDotAppearAmount - 1.0f;
                fAlpha *= 1.0f - f;
                fInterpolate = NotificationUtils.interpolate(this.mDotRadius, getWidth() / 4, f);
            }
            this.mDotPaint.setAlpha((int) (fAlpha * 255.0f));
            canvas.drawCircle(this.mStatusBarIconSize / 2, getHeight() / 2, fInterpolate, this.mDotPaint);
        }
    }

    protected void debug(int i) {
        super.debug(i);
        Log.d("View", debugIndent(i) + "slot=" + this.mSlot);
        Log.d("View", debugIndent(i) + "icon=" + this.mIcon);
    }

    void placeNumber() {
        String string;
        if (this.mIcon.number > getContext().getResources().getInteger(android.R.integer.status_bar_notification_info_maxnum)) {
            string = getContext().getResources().getString(android.R.string.status_bar_notification_info_overflow);
        } else {
            string = NumberFormat.getIntegerInstance().format(this.mIcon.number);
        }
        this.mNumberText = string;
        int width = getWidth();
        int height = getHeight();
        Rect rect = new Rect();
        this.mNumberPain.getTextBounds(string, 0, string.length(), rect);
        int i = rect.right - rect.left;
        int i2 = rect.bottom - rect.top;
        this.mNumberBackground.getPadding(rect);
        int minimumWidth = rect.left + i + rect.right;
        if (minimumWidth < this.mNumberBackground.getMinimumWidth()) {
            minimumWidth = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberX = (width - rect.right) - (((minimumWidth - rect.right) - rect.left) / 2);
        int minimumWidth2 = rect.top + i2 + rect.bottom;
        if (minimumWidth2 < this.mNumberBackground.getMinimumWidth()) {
            minimumWidth2 = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberY = (height - rect.bottom) - ((((minimumWidth2 - rect.top) - i2) - rect.bottom) / 2);
        this.mNumberBackground.setBounds(width - minimumWidth, height - minimumWidth2, width, height);
    }

    private void setContentDescription(Notification notification) {
        if (notification != null) {
            String strContentDescForNotification = contentDescForNotification(this.mContext, notification);
            if (!TextUtils.isEmpty(strContentDescForNotification)) {
                setContentDescription(strContentDescForNotification);
            }
        }
    }

    @Override
    public String toString() {
        return "StatusBarIconView(slot=" + this.mSlot + " icon=" + this.mIcon + " notification=" + this.mNotification + ")";
    }

    public StatusBarNotification getNotification() {
        return this.mNotification;
    }

    @Override
    public String getSlot() {
        return this.mSlot;
    }

    public static String contentDescForNotification(Context context, Notification notification) {
        String strValueOf = "";
        try {
            strValueOf = Notification.Builder.recoverBuilder(context, notification).loadHeaderAppName();
        } catch (RuntimeException e) {
            Log.e("StatusBarIconView", "Unable to recover builder", e);
            Parcelable parcelable = notification.extras.getParcelable("android.appInfo");
            if (parcelable instanceof ApplicationInfo) {
                strValueOf = String.valueOf(((ApplicationInfo) parcelable).loadLabel(context.getPackageManager()));
            }
        }
        CharSequence charSequence = notification.extras.getCharSequence("android.title");
        CharSequence charSequence2 = notification.extras.getCharSequence("android.text");
        CharSequence charSequence3 = notification.tickerText;
        if (TextUtils.equals(charSequence, strValueOf)) {
            charSequence = charSequence2;
        }
        if (TextUtils.isEmpty(charSequence)) {
            if (TextUtils.isEmpty(charSequence3)) {
                charSequence3 = "";
            }
        } else {
            charSequence3 = charSequence;
        }
        return context.getString(R.string.accessibility_desc_notification_icon, strValueOf, charSequence3);
    }

    @Override
    public void setDecorColor(int i) {
        this.mDecorColor = i;
        updateDecorColor();
    }

    private void updateDecorColor() {
        int iInterpolateColors = NotificationUtils.interpolateColors(this.mDecorColor, -1, this.mDarkAmount);
        if (this.mDotPaint.getColor() != iInterpolateColors) {
            this.mDotPaint.setColor(iInterpolateColors);
            if (this.mDotAppearAmount != 0.0f) {
                invalidate();
            }
        }
    }

    @Override
    public void setStaticDrawableColor(int i) {
        this.mDrawableColor = i;
        setColorInternal(i);
        updateContrastedStaticColor();
        this.mIconColor = i;
        this.mDozer.setColor(i);
    }

    public void setColorInternal(int i) {
        this.mCurrentSetColor = i;
        updateIconColor();
    }

    private void updateIconColor() {
        if (this.mCurrentSetColor != 0) {
            if (this.mMatrixColorFilter == null) {
                this.mMatrix = new float[20];
                this.mMatrixColorFilter = new ColorMatrixColorFilter(this.mMatrix);
            }
            updateTintMatrix(this.mMatrix, NotificationUtils.interpolateColors(this.mCurrentSetColor, -1, this.mDarkAmount), 0.67f * this.mDarkAmount);
            this.mMatrixColorFilter.setColorMatrixArray(this.mMatrix);
            setColorFilter(this.mMatrixColorFilter);
            invalidate();
            return;
        }
        this.mDozer.updateGrayscale(this, this.mDarkAmount);
    }

    private static void updateTintMatrix(float[] fArr, int i, float f) {
        Arrays.fill(fArr, 0.0f);
        fArr[4] = Color.red(i);
        fArr[9] = Color.green(i);
        fArr[14] = Color.blue(i);
        fArr[18] = (Color.alpha(i) / 255.0f) + f;
    }

    public void setIconColor(int i, boolean z) {
        if (this.mIconColor != i) {
            this.mIconColor = i;
            if (this.mColorAnimator != null) {
                this.mColorAnimator.cancel();
            }
            if (this.mCurrentSetColor == i) {
                return;
            }
            if (z && this.mCurrentSetColor != 0) {
                this.mAnimationStartColor = this.mCurrentSetColor;
                this.mColorAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
                this.mColorAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
                this.mColorAnimator.setDuration(100L);
                this.mColorAnimator.addUpdateListener(this.mColorUpdater);
                this.mColorAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        StatusBarIconView.this.mColorAnimator = null;
                        StatusBarIconView.this.mAnimationStartColor = 0;
                    }
                });
                this.mColorAnimator.start();
                return;
            }
            setColorInternal(i);
        }
    }

    public int getStaticDrawableColor() {
        return this.mDrawableColor;
    }

    int getContrastedStaticDrawableColor(int i) {
        if (this.mCachedContrastBackgroundColor != i) {
            this.mCachedContrastBackgroundColor = i;
            updateContrastedStaticColor();
        }
        return this.mContrastedDrawableColor;
    }

    private void updateContrastedStaticColor() {
        if (Color.alpha(this.mCachedContrastBackgroundColor) != 255) {
            this.mContrastedDrawableColor = this.mDrawableColor;
            return;
        }
        int iResolveContrastColor = this.mDrawableColor;
        if (!NotificationColorUtil.satisfiesTextContrast(this.mCachedContrastBackgroundColor, iResolveContrastColor)) {
            float[] fArr = new float[3];
            ColorUtils.colorToHSL(this.mDrawableColor, fArr);
            if (fArr[1] < 0.2f) {
                iResolveContrastColor = 0;
            }
            iResolveContrastColor = NotificationColorUtil.resolveContrastColor(this.mContext, iResolveContrastColor, this.mCachedContrastBackgroundColor);
        }
        this.mContrastedDrawableColor = iResolveContrastColor;
    }

    @Override
    public void setVisibleState(int i) {
        setVisibleState(i, true, null);
    }

    public void setVisibleState(int i, boolean z) {
        setVisibleState(i, z, null);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setVisibleState(int i, boolean z, Runnable runnable) {
        setVisibleState(i, z, runnable, 0L);
    }

    public void setVisibleState(int i, boolean z, final Runnable runnable, long j) {
        Interpolator interpolator;
        float f;
        boolean z2;
        boolean z3 = false;
        if (i != this.mVisibleState) {
            this.mVisibleState = i;
            if (this.mIconAppearAnimator != null) {
                this.mIconAppearAnimator.cancel();
            }
            if (this.mDotAnimator != null) {
                this.mDotAnimator.cancel();
            }
            float f2 = 0.0f;
            if (z) {
                Interpolator interpolator2 = Interpolators.FAST_OUT_LINEAR_IN;
                if (i != 0) {
                    interpolator = interpolator2;
                    f = 0.0f;
                } else {
                    interpolator = Interpolators.LINEAR_OUT_SLOW_IN;
                    f = 1.0f;
                }
                float iconAppearAmount = getIconAppearAmount();
                if (f != iconAppearAmount) {
                    this.mIconAppearAnimator = ObjectAnimator.ofFloat(this, ICON_APPEAR_AMOUNT, iconAppearAmount, f);
                    this.mIconAppearAnimator.setInterpolator(interpolator);
                    this.mIconAppearAnimator.setDuration(j == 0 ? 100L : j);
                    this.mIconAppearAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            StatusBarIconView.this.mIconAppearAnimator = null;
                            StatusBarIconView.this.runRunnable(runnable);
                        }
                    });
                    this.mIconAppearAnimator.start();
                    z2 = true;
                } else {
                    z2 = false;
                }
                if (i == 0) {
                    f2 = 2.0f;
                }
                Interpolator interpolator3 = Interpolators.FAST_OUT_LINEAR_IN;
                if (i == 1) {
                    interpolator3 = Interpolators.LINEAR_OUT_SLOW_IN;
                    f2 = 1.0f;
                }
                float dotAppearAmount = getDotAppearAmount();
                if (f2 != dotAppearAmount) {
                    this.mDotAnimator = ObjectAnimator.ofFloat(this, DOT_APPEAR_AMOUNT, dotAppearAmount, f2);
                    this.mDotAnimator.setInterpolator(interpolator3);
                    this.mDotAnimator.setDuration(j != 0 ? j : 100L);
                    final boolean z4 = !z2;
                    this.mDotAnimator.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            StatusBarIconView.this.mDotAnimator = null;
                            if (z4) {
                                StatusBarIconView.this.runRunnable(runnable);
                            }
                        }
                    });
                    this.mDotAnimator.start();
                    z3 = true;
                } else {
                    z3 = z2;
                }
            } else {
                setIconAppearAmount(i == 0 ? 1.0f : 0.0f);
                if (i == 1) {
                    f2 = 1.0f;
                } else if (i == 0) {
                    f2 = 2.0f;
                }
                setDotAppearAmount(f2);
            }
        }
        if (!z3) {
            runRunnable(runnable);
        }
    }

    public void runRunnable(Runnable runnable) {
        if (runnable != null) {
            runnable.run();
        }
    }

    public void setIconAppearAmount(float f) {
        if (this.mIconAppearAmount != f) {
            this.mIconAppearAmount = f;
            invalidate();
        }
    }

    public float getIconAppearAmount() {
        return this.mIconAppearAmount;
    }

    @Override
    public int getVisibleState() {
        return this.mVisibleState;
    }

    public void setDotAppearAmount(float f) {
        if (this.mDotAppearAmount != f) {
            this.mDotAppearAmount = f;
            invalidate();
        }
    }

    @Override
    public void setVisibility(int i) {
        super.setVisibility(i);
        if (this.mOnVisibilityChangedListener != null) {
            this.mOnVisibilityChangedListener.onVisibilityChanged(i);
        }
    }

    public float getDotAppearAmount() {
        return this.mDotAppearAmount;
    }

    public void setOnVisibilityChangedListener(OnVisibilityChangedListener onVisibilityChangedListener) {
        this.mOnVisibilityChangedListener = onVisibilityChangedListener;
    }

    public void setDark(boolean z, boolean z2, long j) {
        this.mDozer.setIntensityDark(new Consumer() {
            @Override
            public final void accept(Object obj) {
                StatusBarIconView.lambda$setDark$1(this.f$0, (Float) obj);
            }
        }, z, z2, j, this);
    }

    public static void lambda$setDark$1(StatusBarIconView statusBarIconView, Float f) {
        statusBarIconView.mDarkAmount = f.floatValue();
        statusBarIconView.updateIconScaleForNotifications();
        statusBarIconView.updateDecorColor();
        statusBarIconView.updateIconColor();
        statusBarIconView.updateAllowAnimation();
    }

    private void updateAllowAnimation() {
        if (this.mDarkAmount == 0.0f || this.mDarkAmount == 1.0f) {
            setAllowAnimation(this.mDarkAmount == 0.0f);
        }
    }

    @Override
    public void getDrawingRect(Rect rect) {
        super.getDrawingRect(rect);
        float translationX = getTranslationX();
        float translationY = getTranslationY();
        rect.left = (int) (rect.left + translationX);
        rect.right = (int) (rect.right + translationX);
        rect.top = (int) (rect.top + translationY);
        rect.bottom = (int) (rect.bottom + translationY);
    }

    public void setIsInShelf(boolean z) {
        this.mIsInShelf = z;
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        if (this.mLayoutRunnable != null) {
            this.mLayoutRunnable.run();
            this.mLayoutRunnable = null;
        }
        updatePivot();
    }

    private void updatePivot() {
        setPivotX(((1.0f - this.mIconScale) / 2.0f) * getWidth());
        setPivotY((getHeight() - (this.mIconScale * getWidth())) / 2.0f);
    }

    public void executeOnLayout(Runnable runnable) {
        this.mLayoutRunnable = runnable;
    }

    public void setDismissed() {
        this.mDismissed = true;
        if (this.mOnDismissListener != null) {
            this.mOnDismissListener.run();
        }
    }

    public void setOnDismissListener(Runnable runnable) {
        this.mOnDismissListener = runnable;
    }

    @Override
    public void onDarkChanged(Rect rect, float f, int i) {
        int tint = DarkIconDispatcher.getTint(rect, this, i);
        setImageTintList(ColorStateList.valueOf(tint));
        setDecorColor(tint);
    }

    @Override
    public boolean isIconVisible() {
        return this.mIcon != null && this.mIcon.visible;
    }

    @Override
    public boolean isIconBlocked() {
        return this.mBlocked;
    }
}
