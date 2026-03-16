package com.android.systemui.statusbar;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewDebug;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.R;
import java.text.NumberFormat;

public class StatusBarIconView extends AnimatedImageView {
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
        super(context);
        Resources res = context.getResources();
        this.mSlot = slot;
        this.mNumberPain = new Paint();
        this.mNumberPain.setTextAlign(Paint.Align.CENTER);
        this.mNumberPain.setColor(res.getColor(R.drawable.notification_number_text_color));
        this.mNumberPain.setAntiAlias(true);
        setNotification(notification);
        if (notification != null) {
            int outerBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
            int imageBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size);
            float scale = imageBounds / outerBounds;
            setScaleX(scale);
            setScaleY(scale);
        }
        setScaleType(ImageView.ScaleType.CENTER);
    }

    public void setNotification(Notification notification) {
        this.mNotification = notification;
        setContentDescription(notification);
    }

    public StatusBarIconView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = context.getResources();
        int outerBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
        int imageBounds = res.getDimensionPixelSize(R.dimen.status_bar_icon_drawing_size);
        float scale = imageBounds / outerBounds;
        setScaleX(scale);
        setScaleY(scale);
    }

    private static boolean streq(String a, String b) {
        if (a == b) {
            return true;
        }
        if (a == null && b != null) {
            return false;
        }
        if (a == null || b != null) {
            return a.equals(b);
        }
        return false;
    }

    public boolean set(StatusBarIcon icon) {
        boolean iconEquals = this.mIcon != null && streq(this.mIcon.iconPackage, icon.iconPackage) && this.mIcon.iconId == icon.iconId;
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
            setVisibility(icon.visible ? 0 : 8);
        }
        return true;
    }

    public void updateDrawable() {
        updateDrawable(true);
    }

    private boolean updateDrawable(boolean withClear) {
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

    public static Drawable getIcon(Context context, StatusBarIcon icon) {
        Resources r;
        if (icon.iconPackage != null) {
            try {
                int userId = icon.user.getIdentifier();
                if (userId == -1) {
                    userId = 0;
                }
                r = context.getPackageManager().getResourcesForApplicationAsUser(icon.iconPackage, userId);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e("StatusBarIconView", "Icon package not found: " + icon.iconPackage);
                return null;
            }
        } else {
            r = context.getResources();
        }
        if (icon.iconId == 0) {
            return null;
        }
        try {
            return r.getDrawable(icon.iconId);
        } catch (RuntimeException e2) {
            Log.w("StatusBarIconView", "Icon not found in " + (icon.iconPackage != null ? Integer.valueOf(icon.iconId) : "<system>") + ": " + Integer.toHexString(icon.iconId));
            return null;
        }
    }

    public StatusBarIcon getStatusBarIcon() {
        return this.mIcon;
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent event) {
        super.onInitializeAccessibilityEvent(event);
        if (this.mNotification != null) {
            event.setParcelableData(this.mNotification);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (this.mNumberBackground != null) {
            placeNumber();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.mNumberBackground != null) {
            this.mNumberBackground.draw(canvas);
            canvas.drawText(this.mNumberText, this.mNumberX, this.mNumberY, this.mNumberPain);
        }
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
        if (notification != null) {
            CharSequence tickerText = notification.tickerText;
            if (!TextUtils.isEmpty(tickerText)) {
                setContentDescription(tickerText);
            }
        }
    }

    @Override
    public String toString() {
        return "StatusBarIconView(slot=" + this.mSlot + " icon=" + this.mIcon + " notification=" + this.mNotification + ")";
    }
}
