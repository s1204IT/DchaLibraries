package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.R;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import java.util.ArrayList;

public class NotificationIconAreaController {
    private int mIconHPadding;
    private int mIconSize;
    private ImageView mMoreIcon;
    private final NotificationColorUtil mNotificationColorUtil;
    protected View mNotificationIconArea;
    private IconMerger mNotificationIcons;
    private PhoneStatusBar mPhoneStatusBar;
    private int mIconTint = -1;
    private final Rect mTintArea = new Rect();

    public NotificationIconAreaController(Context context, PhoneStatusBar phoneStatusBar) {
        this.mPhoneStatusBar = phoneStatusBar;
        this.mNotificationColorUtil = NotificationColorUtil.getInstance(context);
        initializeNotificationAreaViews(context);
    }

    protected View inflateIconArea(LayoutInflater inflater) {
        return inflater.inflate(R.layout.notification_icon_area, (ViewGroup) null);
    }

    protected void initializeNotificationAreaViews(Context context) {
        reloadDimens(context);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        this.mNotificationIconArea = inflateIconArea(layoutInflater);
        this.mNotificationIcons = (IconMerger) this.mNotificationIconArea.findViewById(R.id.notificationIcons);
        this.mMoreIcon = (ImageView) this.mNotificationIconArea.findViewById(R.id.moreIcon);
        if (this.mMoreIcon == null) {
            return;
        }
        this.mMoreIcon.setImageTintList(ColorStateList.valueOf(this.mIconTint));
        this.mNotificationIcons.setOverflowIndicator(this.mMoreIcon);
    }

    public void onDensityOrFontScaleChanged(Context context) {
        reloadDimens(context);
        LinearLayout.LayoutParams params = generateIconLayoutParams();
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            View child = this.mNotificationIcons.getChildAt(i);
            child.setLayoutParams(params);
        }
    }

    @NonNull
    private LinearLayout.LayoutParams generateIconLayoutParams() {
        return new LinearLayout.LayoutParams(this.mIconSize + (this.mIconHPadding * 2), getHeight());
    }

    private void reloadDimens(Context context) {
        Resources res = context.getResources();
        this.mIconSize = res.getDimensionPixelSize(android.R.dimen.action_bar_default_height_material);
        this.mIconHPadding = res.getDimensionPixelSize(R.dimen.status_bar_icon_padding);
    }

    public View getNotificationInnerAreaView() {
        return this.mNotificationIconArea;
    }

    public void setTintArea(Rect tintArea) {
        if (tintArea == null) {
            this.mTintArea.setEmpty();
        } else {
            this.mTintArea.set(tintArea);
        }
        applyNotificationIconsTint();
    }

    public void setIconTint(int iconTint) {
        this.mIconTint = iconTint;
        if (this.mMoreIcon != null) {
            this.mMoreIcon.setImageTintList(ColorStateList.valueOf(this.mIconTint));
        }
        applyNotificationIconsTint();
    }

    protected int getHeight() {
        return this.mPhoneStatusBar.getStatusBarHeight();
    }

    protected boolean shouldShowNotification(NotificationData.Entry entry, NotificationData notificationData) {
        return (!notificationData.isAmbient(entry.key) || NotificationData.showNotificationEvenIfUnprovisioned(entry.notification)) && PhoneStatusBar.isTopLevelChild(entry) && entry.row.getVisibility() != 8;
    }

    public void updateNotificationIcons(NotificationData notificationData) {
        LinearLayout.LayoutParams params = generateIconLayoutParams();
        ArrayList<NotificationData.Entry> activeNotifications = notificationData.getActiveNotifications();
        int size = activeNotifications.size();
        ArrayList<StatusBarIconView> toShow = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            NotificationData.Entry ent = activeNotifications.get(i);
            if (shouldShowNotification(ent, notificationData)) {
                toShow.add(ent.icon);
            }
        }
        ArrayList<View> toRemove = new ArrayList<>();
        for (int i2 = 0; i2 < this.mNotificationIcons.getChildCount(); i2++) {
            View child = this.mNotificationIcons.getChildAt(i2);
            if (!toShow.contains(child)) {
                toRemove.add(child);
            }
        }
        int toRemoveCount = toRemove.size();
        for (int i3 = 0; i3 < toRemoveCount; i3++) {
            this.mNotificationIcons.removeView(toRemove.get(i3));
        }
        for (int i4 = 0; i4 < toShow.size(); i4++) {
            StatusBarIconView v = toShow.get(i4);
            if (v.getParent() == null) {
                this.mNotificationIcons.addView(v, i4, params);
            }
        }
        int childCount = this.mNotificationIcons.getChildCount();
        for (int i5 = 0; i5 < childCount; i5++) {
            View actual = this.mNotificationIcons.getChildAt(i5);
            StatusBarIconView expected = toShow.get(i5);
            if (actual != expected) {
                this.mNotificationIcons.removeView(expected);
                this.mNotificationIcons.addView(expected, i5);
            }
        }
        applyNotificationIconsTint();
    }

    private void applyNotificationIconsTint() {
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            StatusBarIconView v = (StatusBarIconView) this.mNotificationIcons.getChildAt(i);
            boolean isPreL = Boolean.TRUE.equals(v.getTag(R.id.icon_is_pre_L));
            boolean colorize = isPreL ? NotificationUtils.isGrayscale(v, this.mNotificationColorUtil) : true;
            if (colorize) {
                v.setImageTintList(ColorStateList.valueOf(StatusBarIconController.getTint(this.mTintArea, v, this.mIconTint)));
            }
        }
    }
}
