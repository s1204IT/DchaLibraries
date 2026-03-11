package com.android.launcher3.popup;

import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.ImageView;
import com.android.launcher3.AbstractFloatingView;
import com.android.launcher3.BubbleTextView;
import com.android.launcher3.DragSource;
import com.android.launcher3.DropTarget;
import com.android.launcher3.ItemInfo;
import com.android.launcher3.ItemInfoWithIcon;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherModel;
import com.android.launcher3.R;
import com.android.launcher3.accessibility.LauncherAccessibilityDelegate;
import com.android.launcher3.accessibility.ShortcutMenuAccessibilityDelegate;
import com.android.launcher3.badge.BadgeInfo;
import com.android.launcher3.dragndrop.DragController;
import com.android.launcher3.dragndrop.DragLayer;
import com.android.launcher3.dragndrop.DragOptions;
import com.android.launcher3.logging.LoggerUtils;
import com.android.launcher3.notification.NotificationInfo;
import com.android.launcher3.notification.NotificationItemView;
import com.android.launcher3.notification.NotificationKeyData;
import com.android.launcher3.notification.NotificationMainView;
import com.android.launcher3.popup.SystemShortcut;
import com.android.launcher3.shortcuts.DeepShortcutManager;
import com.android.launcher3.shortcuts.DeepShortcutView;
import com.android.launcher3.shortcuts.ShortcutDragPreviewProvider;
import com.android.launcher3.touch.ItemLongClickListener;
import com.android.launcher3.userevent.nano.LauncherLogProto;
import com.android.launcher3.util.PackageUserKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@TargetApi(24)
public class PopupContainerWithArrow extends ArrowPopup implements DragSource, DragController.DragListener, View.OnLongClickListener, View.OnTouchListener {
    private final LauncherAccessibilityDelegate mAccessibilityDelegate;
    private final Point mIconLastTouchPos;
    private final PointF mInterceptTouchDown;
    private NotificationItemView mNotificationItemView;
    private int mNumNotifications;
    private BubbleTextView mOriginalIcon;
    private final List<DeepShortcutView> mShortcuts;
    private final int mStartDragThreshold;
    private ViewGroup mSystemShortcutContainer;

    public PopupContainerWithArrow(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mShortcuts = new ArrayList();
        this.mInterceptTouchDown = new PointF();
        this.mIconLastTouchPos = new Point();
        this.mStartDragThreshold = getResources().getDimensionPixelSize(R.dimen.deep_shortcuts_start_drag_threshold);
        this.mAccessibilityDelegate = new ShortcutMenuAccessibilityDelegate(this.mLauncher);
    }

    public PopupContainerWithArrow(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, 0);
    }

    public PopupContainerWithArrow(Context context) {
        this(context, null, 0);
    }

    @Override
    public LauncherAccessibilityDelegate getAccessibilityDelegate() {
        return this.mAccessibilityDelegate;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            this.mInterceptTouchDown.set(motionEvent.getX(), motionEvent.getY());
        }
        return (this.mNotificationItemView != null && this.mNotificationItemView.onInterceptTouchEvent(motionEvent)) || Math.hypot((double) (this.mInterceptTouchDown.x - motionEvent.getX()), (double) (this.mInterceptTouchDown.y - motionEvent.getY())) > ((double) ViewConfiguration.get(getContext()).getScaledTouchSlop());
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (this.mNotificationItemView != null) {
            return this.mNotificationItemView.onTouchEvent(motionEvent) || super.onTouchEvent(motionEvent);
        }
        return super.onTouchEvent(motionEvent);
    }

    @Override
    protected boolean isOfType(int i) {
        return (i & 2) != 0;
    }

    @Override
    public void logActionCommand(int i) {
        this.mLauncher.getUserEventDispatcher().logActionCommand(i, this.mOriginalIcon, 9);
    }

    @Override
    public boolean onControllerInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getAction() == 0) {
            DragLayer dragLayer = this.mLauncher.getDragLayer();
            if (!dragLayer.isEventOverView(this, motionEvent)) {
                this.mLauncher.getUserEventDispatcher().logActionTapOutside(LoggerUtils.newContainerTarget(9));
                close(true);
                return this.mOriginalIcon == null || !dragLayer.isEventOverView(this.mOriginalIcon, motionEvent);
            }
        }
        return false;
    }

    public static PopupContainerWithArrow showForIcon(BubbleTextView bubbleTextView) {
        Launcher launcher = Launcher.getLauncher(bubbleTextView.getContext());
        if (getOpen(launcher) != null) {
            bubbleTextView.clearFocus();
            return null;
        }
        ItemInfo itemInfo = (ItemInfo) bubbleTextView.getTag();
        if (!DeepShortcutManager.supportsShortcuts(itemInfo)) {
            return null;
        }
        PopupDataProvider popupDataProvider = launcher.getPopupDataProvider();
        List<String> shortcutIdsForItem = popupDataProvider.getShortcutIdsForItem(itemInfo);
        List<NotificationKeyData> notificationKeysForItem = popupDataProvider.getNotificationKeysForItem(itemInfo);
        List<SystemShortcut> enabledSystemShortcutsForItem = popupDataProvider.getEnabledSystemShortcutsForItem(itemInfo);
        PopupContainerWithArrow popupContainerWithArrow = (PopupContainerWithArrow) launcher.getLayoutInflater().inflate(R.layout.popup_container, (ViewGroup) launcher.getDragLayer(), false);
        popupContainerWithArrow.populateAndShow(bubbleTextView, shortcutIdsForItem, notificationKeysForItem, enabledSystemShortcutsForItem);
        return popupContainerWithArrow;
    }

    @Override
    protected void onInflationComplete(boolean z) {
        if (z && this.mNotificationItemView != null) {
            this.mNotificationItemView.inverseGutterMargin();
        }
        int childCount = getChildCount();
        DeepShortcutView deepShortcutView = null;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() == 0 && (childAt instanceof DeepShortcutView)) {
                if (deepShortcutView != null) {
                    deepShortcutView.setDividerVisibility(0);
                }
                DeepShortcutView deepShortcutView2 = (DeepShortcutView) childAt;
                deepShortcutView2.setDividerVisibility(4);
                deepShortcutView = deepShortcutView2;
            }
        }
    }

    @TargetApi(28)
    private void populateAndShow(BubbleTextView bubbleTextView, List<String> list, List<NotificationKeyData> list2, List<SystemShortcut> list3) {
        this.mNumNotifications = list2.size();
        this.mOriginalIcon = bubbleTextView;
        if (this.mNumNotifications > 0) {
            View.inflate(getContext(), R.layout.notification_content, this);
            this.mNotificationItemView = new NotificationItemView(this);
            if (this.mNumNotifications == 1) {
                this.mNotificationItemView.removeFooter();
            }
            updateNotificationHeader();
        }
        int childCount = getChildCount();
        this.mSystemShortcutContainer = this;
        if (!list.isEmpty()) {
            if (this.mNotificationItemView != null) {
                this.mNotificationItemView.addGutter();
            }
            for (int size = list.size(); size > 0; size--) {
                this.mShortcuts.add((DeepShortcutView) inflateAndAdd(R.layout.deep_shortcut, this));
            }
            updateHiddenShortcuts();
            if (!list3.isEmpty()) {
                this.mSystemShortcutContainer = (ViewGroup) inflateAndAdd(R.layout.system_shortcut_icons, this);
                Iterator<SystemShortcut> it = list3.iterator();
                while (it.hasNext()) {
                    initializeSystemShortcut(R.layout.system_shortcut_icon_only, this.mSystemShortcutContainer, it.next());
                }
            }
        } else if (!list3.isEmpty()) {
            if (this.mNotificationItemView != null) {
                this.mNotificationItemView.addGutter();
            }
            Iterator<SystemShortcut> it2 = list3.iterator();
            while (it2.hasNext()) {
                initializeSystemShortcut(R.layout.system_shortcut, this, it2.next());
            }
        }
        reorderAndShow(childCount);
        ItemInfo itemInfo = (ItemInfo) bubbleTextView.getTag();
        if (Build.VERSION.SDK_INT >= 28) {
            setAccessibilityPaneTitle(getTitleForAccessibility());
        }
        this.mLauncher.getDragController().addDragListener(this);
        this.mOriginalIcon.forceHideBadge(true);
        setLayoutTransition(new LayoutTransition());
        new Handler(LauncherModel.getWorkerLooper()).postAtFrontOfQueue(PopupPopulator.createUpdateRunnable(this.mLauncher, itemInfo, new Handler(Looper.getMainLooper()), this, list, this.mShortcuts, list2));
    }

    private String getTitleForAccessibility() {
        int i;
        Context context = getContext();
        if (this.mNumNotifications == 0) {
            i = R.string.action_deep_shortcut;
        } else {
            i = R.string.shortcuts_menu_with_notifications_description;
        }
        return context.getString(i);
    }

    @Override
    protected Pair<View, String> getAccessibilityTarget() {
        return Pair.create(this, "");
    }

    @Override
    protected void getTargetObjectLocation(Rect rect) {
        int height;
        this.mLauncher.getDragLayer().getDescendantRectRelativeToSelf(this.mOriginalIcon, rect);
        rect.top += this.mOriginalIcon.getPaddingTop();
        rect.left += this.mOriginalIcon.getPaddingLeft();
        rect.right -= this.mOriginalIcon.getPaddingRight();
        int i = rect.top;
        if (this.mOriginalIcon.getIcon() != null) {
            height = this.mOriginalIcon.getIcon().getBounds().height();
        } else {
            height = this.mOriginalIcon.getHeight();
        }
        rect.bottom = i + height;
    }

    public void applyNotificationInfos(List<NotificationInfo> list) {
        this.mNotificationItemView.applyNotificationInfos(list);
    }

    private void updateHiddenShortcuts() {
        int dimensionPixelSize;
        int i = this.mNotificationItemView != null ? 2 : 4;
        int dimensionPixelSize2 = getResources().getDimensionPixelSize(R.dimen.bg_popup_item_height);
        if (this.mNotificationItemView != null) {
            dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.bg_popup_item_condensed_height);
        } else {
            dimensionPixelSize = dimensionPixelSize2;
        }
        float f = dimensionPixelSize / dimensionPixelSize2;
        int size = this.mShortcuts.size();
        int i2 = 0;
        while (i2 < size) {
            DeepShortcutView deepShortcutView = this.mShortcuts.get(i2);
            deepShortcutView.setVisibility(i2 >= i ? 8 : 0);
            deepShortcutView.getLayoutParams().height = dimensionPixelSize;
            deepShortcutView.getIconView().setScaleX(f);
            deepShortcutView.getIconView().setScaleY(f);
            i2++;
        }
    }

    private void updateDividers() {
        int childCount = getChildCount();
        DeepShortcutView deepShortcutView = null;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            if (childAt.getVisibility() == 0 && (childAt instanceof DeepShortcutView)) {
                if (deepShortcutView != null) {
                    deepShortcutView.setDividerVisibility(0);
                }
                DeepShortcutView deepShortcutView2 = (DeepShortcutView) childAt;
                deepShortcutView2.setDividerVisibility(4);
                deepShortcutView = deepShortcutView2;
            }
        }
    }

    @Override
    protected void onWidgetsBound() {
        View childAt;
        ItemInfo itemInfo = (ItemInfo) this.mOriginalIcon.getTag();
        SystemShortcut.Widgets widgets = new SystemShortcut.Widgets();
        View.OnClickListener onClickListener = widgets.getOnClickListener(this.mLauncher, itemInfo);
        int childCount = this.mSystemShortcutContainer.getChildCount();
        int i = 0;
        while (true) {
            if (i < childCount) {
                childAt = this.mSystemShortcutContainer.getChildAt(i);
                if (childAt.getTag() instanceof SystemShortcut.Widgets) {
                    break;
                } else {
                    i++;
                }
            } else {
                childAt = null;
                break;
            }
        }
        if (onClickListener != null && childAt == null) {
            if (this.mSystemShortcutContainer != this) {
                initializeSystemShortcut(R.layout.system_shortcut_icon_only, this.mSystemShortcutContainer, widgets);
                return;
            } else {
                close(false);
                showForIcon(this.mOriginalIcon);
                return;
            }
        }
        if (onClickListener == null && childAt != null) {
            if (this.mSystemShortcutContainer != this) {
                this.mSystemShortcutContainer.removeView(childAt);
            } else {
                close(false);
                showForIcon(this.mOriginalIcon);
            }
        }
    }

    private void initializeSystemShortcut(int i, ViewGroup viewGroup, SystemShortcut systemShortcut) {
        View viewInflateAndAdd = inflateAndAdd(i, viewGroup);
        if (viewInflateAndAdd instanceof DeepShortcutView) {
            DeepShortcutView deepShortcutView = (DeepShortcutView) viewInflateAndAdd;
            deepShortcutView.getIconView().setBackgroundResource(systemShortcut.iconResId);
            deepShortcutView.getBubbleText().setText(systemShortcut.labelResId);
        } else if (viewInflateAndAdd instanceof ImageView) {
            ImageView imageView = (ImageView) viewInflateAndAdd;
            imageView.setImageResource(systemShortcut.iconResId);
            imageView.setContentDescription(getContext().getText(systemShortcut.labelResId));
        }
        viewInflateAndAdd.setTag(systemShortcut);
        viewInflateAndAdd.setOnClickListener(systemShortcut.getOnClickListener(this.mLauncher, (ItemInfo) this.mOriginalIcon.getTag()));
    }

    public DragOptions.PreDragCondition createPreDragCondition() {
        return new DragOptions.PreDragCondition() {
            @Override
            public boolean shouldStartDrag(double d) {
                return d > ((double) PopupContainerWithArrow.this.mStartDragThreshold);
            }

            @Override
            public void onPreDragStart(DropTarget.DragObject dragObject) {
                if (PopupContainerWithArrow.this.mIsAboveIcon) {
                    PopupContainerWithArrow.this.mOriginalIcon.setIconVisible(false);
                    PopupContainerWithArrow.this.mOriginalIcon.setVisibility(0);
                } else {
                    PopupContainerWithArrow.this.mOriginalIcon.setVisibility(4);
                }
            }

            @Override
            public void onPreDragEnd(DropTarget.DragObject dragObject, boolean z) {
                PopupContainerWithArrow.this.mOriginalIcon.setIconVisible(true);
                if (z) {
                    PopupContainerWithArrow.this.mOriginalIcon.setVisibility(4);
                    return;
                }
                PopupContainerWithArrow.this.mLauncher.getUserEventDispatcher().logDeepShortcutsOpen(PopupContainerWithArrow.this.mOriginalIcon);
                if (!PopupContainerWithArrow.this.mIsAboveIcon) {
                    PopupContainerWithArrow.this.mOriginalIcon.setVisibility(0);
                    PopupContainerWithArrow.this.mOriginalIcon.setTextVisibility(false);
                }
            }
        };
    }

    public void updateNotificationHeader(Set<PackageUserKey> set) {
        if (set.contains(PackageUserKey.fromItemInfo((ItemInfo) this.mOriginalIcon.getTag()))) {
            updateNotificationHeader();
        }
    }

    private void updateNotificationHeader() {
        ItemInfoWithIcon itemInfoWithIcon = (ItemInfoWithIcon) this.mOriginalIcon.getTag();
        BadgeInfo badgeInfoForItem = this.mLauncher.getBadgeInfoForItem(itemInfoWithIcon);
        if (this.mNotificationItemView != null && badgeInfoForItem != null) {
            this.mNotificationItemView.updateHeader(badgeInfoForItem.getNotificationCount(), itemInfoWithIcon.iconColor);
        }
    }

    public void trimNotifications(Map<PackageUserKey, BadgeInfo> map) {
        if (this.mNotificationItemView == null) {
            return;
        }
        BadgeInfo badgeInfo = map.get(PackageUserKey.fromItemInfo((ItemInfo) this.mOriginalIcon.getTag()));
        if (badgeInfo == null || badgeInfo.getNotificationKeys().size() == 0) {
            this.mNotificationItemView.removeAllViews();
            this.mNotificationItemView = null;
            updateHiddenShortcuts();
            updateDividers();
            return;
        }
        this.mNotificationItemView.trimNotifications(NotificationKeyData.extractKeysOnly(badgeInfo.getNotificationKeys()));
    }

    @Override
    public void onDropCompleted(View view, DropTarget.DragObject dragObject, boolean z) {
    }

    @Override
    public void onDragStart(DropTarget.DragObject dragObject, DragOptions dragOptions) {
        this.mDeferContainerRemoval = true;
        animateClose();
    }

    @Override
    public void onDragEnd() {
        if (!this.mIsOpen) {
            if (this.mOpenCloseAnimator != null) {
                this.mDeferContainerRemoval = false;
            } else if (this.mDeferContainerRemoval) {
                closeComplete();
            }
        }
    }

    @Override
    public void fillInLogContainerData(View view, ItemInfo itemInfo, LauncherLogProto.Target target, LauncherLogProto.Target target2) {
        if (itemInfo == NotificationMainView.NOTIFICATION_ITEM_INFO) {
            target.itemType = 8;
        } else {
            target.itemType = 5;
            target.rank = itemInfo.rank;
        }
        target2.containerType = 9;
    }

    @Override
    protected void onCreateCloseAnimation(AnimatorSet animatorSet) {
        animatorSet.play(this.mOriginalIcon.createTextAlphaAnimator(true));
        this.mOriginalIcon.forceHideBadge(false);
    }

    @Override
    protected void closeComplete() {
        super.closeComplete();
        this.mOriginalIcon.setTextVisibility(this.mOriginalIcon.shouldTextBeVisible());
        this.mOriginalIcon.forceHideBadge(false);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        int action = motionEvent.getAction();
        if (action == 0 || action == 2) {
            this.mIconLastTouchPos.set((int) motionEvent.getX(), (int) motionEvent.getY());
            return false;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View view) {
        if (!ItemLongClickListener.canStartDrag(this.mLauncher) || !(view.getParent() instanceof DeepShortcutView)) {
            return false;
        }
        DeepShortcutView deepShortcutView = (DeepShortcutView) view.getParent();
        deepShortcutView.setWillDrawIcon(false);
        Point point = new Point();
        point.x = this.mIconLastTouchPos.x - deepShortcutView.getIconCenter().x;
        point.y = this.mIconLastTouchPos.y - this.mLauncher.getDeviceProfile().iconSizePx;
        this.mLauncher.getWorkspace().beginDragShared(deepShortcutView.getIconView(), this, deepShortcutView.getFinalInfo(), new ShortcutDragPreviewProvider(deepShortcutView.getIconView(), point), new DragOptions()).animateShift(-point.x, -point.y);
        AbstractFloatingView.closeOpenContainer(this.mLauncher, 1);
        return false;
    }

    public static PopupContainerWithArrow getOpen(Launcher launcher) {
        return (PopupContainerWithArrow) getOpenView(launcher, 2);
    }
}
