package com.android.launcher3.notification;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.launcher3.R;
import com.android.launcher3.graphics.IconPalette;
import com.android.launcher3.notification.NotificationFooterLayout;
import com.android.launcher3.popup.PopupContainerWithArrow;
import com.android.launcher3.touch.SwipeDetector;
import com.android.launcher3.util.Themes;
import java.util.List;

/* loaded from: classes.dex */
public class NotificationItemView {
    private static final Rect sTempRect = new Rect();
    private boolean mAnimatingNextIcon;
    private final PopupContainerWithArrow mContainer;
    private final Context mContext;
    private final View mDivider;
    private final NotificationFooterLayout mFooter;
    private View mGutter;
    private final View mHeader;
    private final TextView mHeaderCount;
    private final TextView mHeaderText;
    private final View mIconView;
    private final NotificationMainView mMainView;
    private final SwipeDetector mSwipeDetector;
    private boolean mIgnoreTouch = false;
    private int mNotificationHeaderTextColor = 0;

    public NotificationItemView(PopupContainerWithArrow popupContainerWithArrow) {
        this.mContainer = popupContainerWithArrow;
        this.mContext = popupContainerWithArrow.getContext();
        this.mHeaderText = (TextView) popupContainerWithArrow.findViewById(R.id.notification_text);
        this.mHeaderCount = (TextView) popupContainerWithArrow.findViewById(R.id.notification_count);
        this.mMainView = (NotificationMainView) popupContainerWithArrow.findViewById(R.id.main_view);
        this.mFooter = (NotificationFooterLayout) popupContainerWithArrow.findViewById(R.id.footer);
        this.mIconView = popupContainerWithArrow.findViewById(R.id.popup_item_icon);
        this.mHeader = popupContainerWithArrow.findViewById(R.id.header);
        this.mDivider = popupContainerWithArrow.findViewById(R.id.divider);
        this.mSwipeDetector = new SwipeDetector(this.mContext, this.mMainView, SwipeDetector.HORIZONTAL);
        this.mSwipeDetector.setDetectableScrollConditions(3, false);
        this.mMainView.setSwipeDetector(this.mSwipeDetector);
        this.mFooter.setContainer(this);
    }

    public void addGutter() {
        if (this.mGutter == null) {
            this.mGutter = this.mContainer.inflateAndAdd(R.layout.notification_gutter, this.mContainer);
        }
    }

    public void removeFooter() {
        if (this.mContainer.indexOfChild(this.mFooter) >= 0) {
            this.mContainer.removeView(this.mFooter);
            this.mContainer.removeView(this.mDivider);
        }
    }

    public void inverseGutterMargin() {
        ViewGroup.MarginLayoutParams marginLayoutParams = (ViewGroup.MarginLayoutParams) this.mGutter.getLayoutParams();
        int i = marginLayoutParams.topMargin;
        marginLayoutParams.topMargin = marginLayoutParams.bottomMargin;
        marginLayoutParams.bottomMargin = i;
    }

    public void removeAllViews() {
        this.mContainer.removeView(this.mMainView);
        this.mContainer.removeView(this.mHeader);
        if (this.mContainer.indexOfChild(this.mFooter) >= 0) {
            this.mContainer.removeView(this.mFooter);
            this.mContainer.removeView(this.mDivider);
        }
        if (this.mGutter != null) {
            this.mContainer.removeView(this.mGutter);
        }
    }

    public void updateHeader(int i, int i2) {
        this.mHeaderCount.setText(i <= 1 ? "" : String.valueOf(i));
        if (Color.alpha(i2) > 0) {
            if (this.mNotificationHeaderTextColor == 0) {
                this.mNotificationHeaderTextColor = IconPalette.resolveContrastColor(this.mContext, i2, Themes.getAttrColor(this.mContext, R.attr.popupColorPrimary));
            }
            this.mHeaderText.setTextColor(this.mNotificationHeaderTextColor);
            this.mHeaderCount.setTextColor(this.mNotificationHeaderTextColor);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            sTempRect.set(this.mMainView.getLeft(), this.mMainView.getTop(), this.mMainView.getRight(), this.mMainView.getBottom());
            this.mIgnoreTouch = !sTempRect.contains((int) motionEvent.getX(), (int) motionEvent.getY());
            if (!this.mIgnoreTouch) {
                this.mContainer.getParent().requestDisallowInterceptTouchEvent(true);
            }
        }
        if (this.mIgnoreTouch || this.mMainView.getNotificationInfo() == null) {
            return false;
        }
        this.mSwipeDetector.onTouchEvent(motionEvent);
        return this.mSwipeDetector.isDraggingOrSettling();
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mIgnoreTouch || this.mMainView.getNotificationInfo() == null) {
            return false;
        }
        return this.mSwipeDetector.onTouchEvent(motionEvent);
    }

    public void applyNotificationInfos(List<NotificationInfo> list) {
        if (list.isEmpty()) {
            return;
        }
        this.mMainView.applyNotificationInfo(list.get(0), false);
        for (int i = 1; i < list.size(); i++) {
            this.mFooter.addNotificationInfo(list.get(i));
        }
        this.mFooter.commitNotificationInfos();
    }

    public void trimNotifications(List<String> list) {
        if ((!list.contains(this.mMainView.getNotificationInfo().notificationKey)) && !this.mAnimatingNextIcon) {
            this.mAnimatingNextIcon = true;
            this.mMainView.setContentVisibility(4);
            this.mMainView.setContentTranslation(0.0f);
            this.mIconView.getGlobalVisibleRect(sTempRect);
            this.mFooter.animateFirstNotificationTo(sTempRect, new NotificationFooterLayout.IconAnimationEndListener() { // from class: com.android.launcher3.notification.-$$Lambda$NotificationItemView$zyF4hkGDqkeABZLcostu5tMK2UM
                @Override // com.android.launcher3.notification.NotificationFooterLayout.IconAnimationEndListener
                public final void onIconAnimationEnd(NotificationInfo notificationInfo) {
                    NotificationItemView.lambda$trimNotifications$0(this.f$0, notificationInfo);
                }
            });
            return;
        }
        this.mFooter.trimNotifications(list);
    }

    public static /* synthetic */ void lambda$trimNotifications$0(NotificationItemView notificationItemView, NotificationInfo notificationInfo) {
        if (notificationInfo != null) {
            notificationItemView.mMainView.applyNotificationInfo(notificationInfo, true);
            notificationItemView.mMainView.setContentVisibility(0);
        }
        notificationItemView.mAnimatingNextIcon = false;
    }
}
