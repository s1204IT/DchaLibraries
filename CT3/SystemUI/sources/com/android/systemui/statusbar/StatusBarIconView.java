package com.android.systemui.statusbar;

import android.app.Notification;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.R;
import java.text.NumberFormat;

public class StatusBarIconView extends AnimatedImageView {
    private boolean mAlwaysScaleIcon;
    private final boolean mBlocked;
    private int mDensity;
    private StatusBarIcon mIcon;
    private Notification mNotification;
    private Drawable mNumberBackground;
    private Paint mNumberPain;
    private String mNumberText;
    private int mNumberX;
    private int mNumberY;

    @ViewDebug.ExportedProperty
    private String mSlot;

    public StatusBarIconView(Context context, String slot, Notification notification) {
        this(context, slot, notification, false);
    }

    public StatusBarIconView(Context context, String slot, Notification notification, boolean blocked) {
        super(context);
        this.mBlocked = blocked;
        this.mSlot = slot;
        this.mNumberPain = new Paint();
        this.mNumberPain.setTextAlign(Paint.Align.CENTER);
        this.mNumberPain.setColor(context.getColor(R.drawable.notification_number_text_color));
        this.mNumberPain.setAntiAlias(true);
        setNotification(notification);
        maybeUpdateIconScale();
        setScaleType(ImageView.ScaleType.CENTER);
        this.mDensity = context.getResources().getDisplayMetrics().densityDpi;
    }

    private void maybeUpdateIconScale() {
        if (this.mNotification == null && !this.mAlwaysScaleIcon) {
            return;
        }
        updateIconScale();
    }

    private void updateIconScale() {
        Resources res = this.mContext.getResources();
        int outerBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
        int imageBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size);
        float scale = imageBounds / outerBounds;
        setScaleX(scale);
        setScaleY(scale);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        int density = newConfig.densityDpi;
        if (density == this.mDensity) {
            return;
        }
        this.mDensity = density;
        maybeUpdateIconScale();
        updateDrawable();
    }

    public void setNotification(Notification notification) {
        this.mNotification = notification;
        setContentDescription(notification);
    }

    public StatusBarIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBlocked = false;
        this.mAlwaysScaleIcon = true;
        updateIconScale();
        this.mDensity = context.getResources().getDisplayMetrics().densityDpi;
    }

    public boolean equalIcons(Icon a, Icon b) {
        if (a == b) {
            return true;
        }
        if (a.getType() != b.getType()) {
            return false;
        }
        switch (a.getType()) {
            case 2:
                return a.getResPackage().equals(b.getResPackage()) && a.getResId() == b.getResId();
            case 3:
            default:
                return false;
            case 4:
                return a.getUriString().equals(b.getUriString());
        }
    }

    public boolean set(StatusBarIcon icon) {
        boolean iconEquals = this.mIcon != null ? equalIcons(this.mIcon.icon, icon.icon) : false;
        boolean levelEquals = iconEquals && this.mIcon.iconLevel == icon.iconLevel;
        boolean visibilityEquals = this.mIcon != null && this.mIcon.visible == icon.visible;
        boolean numberEquals = this.mIcon != null && this.mIcon.number == icon.number;
        this.mIcon = icon.clone();
        setContentDescription(icon.contentDescription);
        if (!iconEquals && !updateDrawable(false)) {
            return false;
        }
        if (!levelEquals) {
            setImageLevel(icon.iconLevel);
        }
        if (!numberEquals) {
            if (icon.number > 0 && getContext().getResources().getBoolean(R.bool.config_statusBarShowNumber)) {
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
        if (!visibilityEquals) {
            setVisibility((!icon.visible || this.mBlocked) ? 8 : 0);
            return true;
        }
        return true;
    }

    public void updateDrawable() {
        updateDrawable(true);
    }

    private boolean updateDrawable(boolean withClear) {
        if (this.mIcon == null) {
            return false;
        }
        Drawable drawable = getIcon(this.mIcon);
        if (drawable == null) {
            Log.w("StatusBarIconView", "No icon for slot " + this.mSlot);
            return false;
        }
        if (withClear) {
            setImageDrawable(null);
        }
        setImageDrawable(drawable);
        return true;
    }

    private Drawable getIcon(StatusBarIcon icon) {
        return getIcon(getContext(), icon);
    }

    public static Drawable getIcon(Context context, StatusBarIcon statusBarIcon) {
        int userId = statusBarIcon.user.getIdentifier();
        if (userId == -1) {
            userId = 0;
        }
        Drawable icon = statusBarIcon.icon.loadDrawableAsUser(context, userId);
        TypedValue typedValue = new TypedValue();
        context.getResources().getValue(R.dimen.status_bar_icon_scale_factor, typedValue, true);
        float scaleFactor = typedValue.getFloat();
        if (scaleFactor == 1.0f) {
            return icon;
        }
        return new ScalingDrawableWrapper(icon, scaleFactor);
    }

    public StatusBarIcon getStatusBarIcon() {
        return this.mIcon;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (this.mNotification == null) {
            return;
        }
        event.setParcelableData(this.mNotification);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mNumberBackground == null) {
            return;
        }
        placeNumber();
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        super.onRtlPropertiesChanged(layoutDirection);
        updateDrawable();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mNumberBackground == null) {
            return;
        }
        this.mNumberBackground.draw(canvas);
        canvas.drawText(this.mNumberText, this.mNumberX, this.mNumberY, this.mNumberPain);
    }

    protected void debug(int depth) {
        super.debug(depth);
        Log.d("View", debugIndent(depth) + "slot=" + this.mSlot);
        Log.d("View", debugIndent(depth) + "icon=" + this.mIcon);
    }

    void placeNumber() {
        String str;
        int tooBig = getContext().getResources().getInteger(android.R.integer.status_bar_notification_info_maxnum);
        if (this.mIcon.number > tooBig) {
            str = getContext().getResources().getString(android.R.string.status_bar_notification_info_overflow);
        } else {
            NumberFormat f = NumberFormat.getIntegerInstance();
            str = f.format(this.mIcon.number);
        }
        this.mNumberText = str;
        int w = getWidth();
        int h = getHeight();
        Rect r = new Rect();
        this.mNumberPain.getTextBounds(str, 0, str.length(), r);
        int tw = r.right - r.left;
        int th = r.bottom - r.top;
        this.mNumberBackground.getPadding(r);
        int dw = r.left + tw + r.right;
        if (dw < this.mNumberBackground.getMinimumWidth()) {
            dw = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberX = (w - r.right) - (((dw - r.right) - r.left) / 2);
        int dh = r.top + th + r.bottom;
        if (dh < this.mNumberBackground.getMinimumWidth()) {
            dh = this.mNumberBackground.getMinimumWidth();
        }
        this.mNumberY = (h - r.bottom) - ((((dh - r.top) - th) - r.bottom) / 2);
        this.mNumberBackground.setBounds(w - dw, h - dh, w, h);
    }

    private void setContentDescription(Notification notification) {
        if (notification == null) {
            return;
        }
        String d = contentDescForNotification(this.mContext, notification);
        if (TextUtils.isEmpty(d)) {
            return;
        }
        setContentDescription(d);
    }

    @Override
    public String toString() {
        return "StatusBarIconView(slot=" + this.mSlot + " icon=" + this.mIcon + " notification=" + this.mNotification + ")";
    }

    public String getSlot() {
        return this.mSlot;
    }

    public static String contentDescForNotification(Context c, Notification n) {
        String desc;
        String appName = "";
        try {
            Notification.Builder builder = Notification.Builder.recoverBuilder(c, n);
            appName = builder.loadHeaderAppName();
        } catch (RuntimeException e) {
            Log.e("StatusBarIconView", "Unable to recover builder", e);
            Parcelable appInfo = n.extras.getParcelable("android.appInfo");
            if (appInfo instanceof ApplicationInfo) {
                appName = String.valueOf(((ApplicationInfo) appInfo).loadLabel(c.getPackageManager()));
            }
        }
        CharSequence title = n.extras.getCharSequence("android.title");
        CharSequence ticker = n.tickerText;
        if (TextUtils.isEmpty(ticker)) {
            desc = !TextUtils.isEmpty(title) ? title : "";
        } else {
            desc = ticker;
        }
        return c.getString(R.string.accessibility_desc_notification_icon, appName, desc);
    }
}
