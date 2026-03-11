package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.support.v4.util.ArrayMap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.util.NotificationColorUtil;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.NotificationEntryManager;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.StatusBarIconView;
import com.android.systemui.statusbar.notification.NotificationUtils;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;
import java.util.function.Function;

public class NotificationIconAreaController implements DarkIconDispatcher.DarkReceiver {
    private Context mContext;
    private int mIconHPadding;
    private int mIconSize;
    private final NotificationColorUtil mNotificationColorUtil;
    protected View mNotificationIconArea;
    private NotificationIconContainer mNotificationIcons;
    private NotificationStackScrollLayout mNotificationScrollLayout;
    private NotificationIconContainer mShelfIcons;
    private StatusBar mStatusBar;
    private final Runnable mUpdateStatusBarIcons = new Runnable() {
        @Override
        public final void run() {
            this.f$0.updateStatusBarIcons();
        }
    };
    private int mIconTint = -1;
    private final Rect mTintArea = new Rect();
    private final NotificationEntryManager mEntryManager = (NotificationEntryManager) Dependency.get(NotificationEntryManager.class);

    public NotificationIconAreaController(Context context, StatusBar statusBar) {
        this.mStatusBar = statusBar;
        this.mNotificationColorUtil = NotificationColorUtil.getInstance(context);
        this.mContext = context;
        initializeNotificationAreaViews(context);
    }

    protected View inflateIconArea(LayoutInflater layoutInflater) {
        return layoutInflater.inflate(R.layout.notification_icon_area, (ViewGroup) null);
    }

    protected void initializeNotificationAreaViews(Context context) {
        reloadDimens(context);
        this.mNotificationIconArea = inflateIconArea(LayoutInflater.from(context));
        this.mNotificationIcons = (NotificationIconContainer) this.mNotificationIconArea.findViewById(R.id.notificationIcons);
        this.mNotificationScrollLayout = this.mStatusBar.getNotificationScrollLayout();
    }

    public void setupShelf(NotificationShelf notificationShelf) {
        this.mShelfIcons = notificationShelf.getShelfIcons();
        notificationShelf.setCollapsedIcons(this.mNotificationIcons);
    }

    public void onDensityOrFontScaleChanged(Context context) {
        reloadDimens(context);
        FrameLayout.LayoutParams layoutParamsGenerateIconLayoutParams = generateIconLayoutParams();
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            this.mNotificationIcons.getChildAt(i).setLayoutParams(layoutParamsGenerateIconLayoutParams);
        }
        for (int i2 = 0; i2 < this.mShelfIcons.getChildCount(); i2++) {
            this.mShelfIcons.getChildAt(i2).setLayoutParams(layoutParamsGenerateIconLayoutParams);
        }
    }

    private FrameLayout.LayoutParams generateIconLayoutParams() {
        return new FrameLayout.LayoutParams(this.mIconSize + (2 * this.mIconHPadding), getHeight());
    }

    private void reloadDimens(Context context) {
        Resources resources = context.getResources();
        this.mIconSize = resources.getDimensionPixelSize(android.R.dimen.handwriting_bounds_offset_left);
        this.mIconHPadding = resources.getDimensionPixelSize(R.dimen.status_bar_icon_padding);
    }

    public View getNotificationInnerAreaView() {
        return this.mNotificationIconArea;
    }

    @Override
    public void onDarkChanged(Rect rect, float f, int i) {
        if (rect == null) {
            this.mTintArea.setEmpty();
        } else {
            this.mTintArea.set(rect);
        }
        if (this.mNotificationIconArea == null || DarkIconDispatcher.isInArea(rect, this.mNotificationIconArea)) {
            this.mIconTint = i;
        }
        applyNotificationIconsTint();
    }

    protected int getHeight() {
        return this.mStatusBar.getStatusBarHeight();
    }

    protected boolean shouldShowNotificationIcon(NotificationData.Entry entry, boolean z, boolean z2, boolean z3) {
        if ((this.mEntryManager.getNotificationData().isAmbient(entry.key) && !z) || !StatusBar.isTopLevelChild(entry) || entry.row.getVisibility() == 8) {
            return false;
        }
        if (entry.row.isDismissed() && z2) {
            return false;
        }
        if (z3 && entry.isLastMessageFromReply()) {
            return false;
        }
        return z || !this.mEntryManager.getNotificationData().shouldSuppressStatusBar(entry);
    }

    public void updateNotificationIcons() {
        updateStatusBarIcons();
        updateIconsForLayout(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((NotificationData.Entry) obj).expandedIcon;
            }
        }, this.mShelfIcons, true, false, false);
        applyNotificationIconsTint();
    }

    public void updateStatusBarIcons() {
        updateIconsForLayout(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((NotificationData.Entry) obj).icon;
            }
        }, this.mNotificationIcons, false, true, true);
    }

    private void updateIconsForLayout(Function<NotificationData.Entry, StatusBarIconView> function, NotificationIconContainer notificationIconContainer, boolean z, boolean z2, boolean z3) {
        ArrayList arrayList = new ArrayList(this.mNotificationScrollLayout.getChildCount());
        for (int i = 0; i < this.mNotificationScrollLayout.getChildCount(); i++) {
            View childAt = this.mNotificationScrollLayout.getChildAt(i);
            if (childAt instanceof ExpandableNotificationRow) {
                NotificationData.Entry entry = ((ExpandableNotificationRow) childAt).getEntry();
                if (shouldShowNotificationIcon(entry, z, z2, z3)) {
                    arrayList.add(function.apply(entry));
                }
            }
        }
        ArrayMap<String, ArrayList<StatusBarIcon>> arrayMap = new ArrayMap<>();
        ArrayList arrayList2 = new ArrayList();
        for (int i2 = 0; i2 < notificationIconContainer.getChildCount(); i2++) {
            View childAt2 = notificationIconContainer.getChildAt(i2);
            if ((childAt2 instanceof StatusBarIconView) && !arrayList.contains(childAt2)) {
                StatusBarIconView statusBarIconView = (StatusBarIconView) childAt2;
                String groupKey = statusBarIconView.getNotification().getGroupKey();
                int i3 = 0;
                boolean z4 = false;
                while (true) {
                    if (i3 >= arrayList.size()) {
                        break;
                    }
                    StatusBarIconView statusBarIconView2 = (StatusBarIconView) arrayList.get(i3);
                    if (statusBarIconView2.getSourceIcon().sameAs(statusBarIconView.getSourceIcon()) && statusBarIconView2.getNotification().getGroupKey().equals(groupKey)) {
                        if (!z4) {
                            z4 = true;
                        } else {
                            z4 = false;
                            break;
                        }
                    }
                    i3++;
                }
                if (z4) {
                    ArrayList<StatusBarIcon> arrayList3 = arrayMap.get(groupKey);
                    if (arrayList3 == null) {
                        arrayList3 = new ArrayList<>();
                        arrayMap.put(groupKey, arrayList3);
                    }
                    arrayList3.add(statusBarIconView.getStatusBarIcon());
                }
                arrayList2.add(statusBarIconView);
            }
        }
        ArrayList arrayList4 = new ArrayList();
        for (String str : arrayMap.keySet()) {
            if (arrayMap.get(str).size() != 1) {
                arrayList4.add(str);
            }
        }
        arrayMap.removeAll(arrayList4);
        notificationIconContainer.setReplacingIcons(arrayMap);
        int size = arrayList2.size();
        for (int i4 = 0; i4 < size; i4++) {
            notificationIconContainer.removeView((View) arrayList2.get(i4));
        }
        ViewGroup.LayoutParams layoutParamsGenerateIconLayoutParams = generateIconLayoutParams();
        for (int i5 = 0; i5 < arrayList.size(); i5++) {
            StatusBarIconView statusBarIconView3 = (StatusBarIconView) arrayList.get(i5);
            notificationIconContainer.removeTransientView(statusBarIconView3);
            if (statusBarIconView3.getParent() == null) {
                if (z2) {
                    statusBarIconView3.setOnDismissListener(this.mUpdateStatusBarIcons);
                }
                notificationIconContainer.addView(statusBarIconView3, i5, layoutParamsGenerateIconLayoutParams);
            }
        }
        notificationIconContainer.setChangingViewPositions(true);
        int childCount = notificationIconContainer.getChildCount();
        for (int i6 = 0; i6 < childCount; i6++) {
            View childAt3 = notificationIconContainer.getChildAt(i6);
            View view = (StatusBarIconView) arrayList.get(i6);
            if (childAt3 != view) {
                notificationIconContainer.removeView(view);
                notificationIconContainer.addView(view, i6);
            }
        }
        notificationIconContainer.setChangingViewPositions(false);
        notificationIconContainer.setReplacingIcons(null);
    }

    private void applyNotificationIconsTint() {
        for (int i = 0; i < this.mNotificationIcons.getChildCount(); i++) {
            final StatusBarIconView statusBarIconView = (StatusBarIconView) this.mNotificationIcons.getChildAt(i);
            if (statusBarIconView.getWidth() != 0) {
                updateTintForIcon(statusBarIconView);
            } else {
                statusBarIconView.executeOnLayout(new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.updateTintForIcon(statusBarIconView);
                    }
                });
            }
        }
    }

    public void updateTintForIcon(StatusBarIconView statusBarIconView) {
        boolean z;
        int tint = 0;
        if (!Boolean.TRUE.equals(statusBarIconView.getTag(R.id.icon_is_pre_L)) || NotificationUtils.isGrayscale(statusBarIconView, this.mNotificationColorUtil)) {
            z = true;
        } else {
            z = false;
        }
        if (z) {
            tint = DarkIconDispatcher.getTint(this.mTintArea, statusBarIconView, this.mIconTint);
        }
        statusBarIconView.setStaticDrawableColor(tint);
        statusBarIconView.setDecorColor(this.mIconTint);
    }

    public void showIconIsolated(StatusBarIconView statusBarIconView, boolean z) {
        this.mNotificationIcons.showIconIsolated(statusBarIconView, z);
    }

    public void setIsolatedIconLocation(Rect rect, boolean z) {
        this.mNotificationIcons.setIsolatedIconLocation(rect, z);
    }
}
