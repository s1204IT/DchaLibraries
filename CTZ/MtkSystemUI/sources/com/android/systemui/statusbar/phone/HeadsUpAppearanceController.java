package com.android.systemui.statusbar.phone;

import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;
import android.view.WindowInsets;
import com.android.internal.annotations.VisibleForTesting;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.CrossFadeHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.HeadsUpStatusBarView;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.policy.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HeadsUpAppearanceController implements DarkIconDispatcher.DarkReceiver, OnHeadsUpChangedListener {
    private final View mClockView;
    private final DarkIconDispatcher mDarkIconDispatcher;
    private float mExpandFraction;
    private float mExpandedHeight;
    private final HeadsUpManagerPhone mHeadsUpManager;
    private final HeadsUpStatusBarView mHeadsUpStatusBarView;
    private boolean mIsExpanded;
    private final NotificationIconAreaController mNotificationIconAreaController;
    private final NotificationPanelView mPanelView;
    Point mPoint;
    private final BiConsumer<Float, Float> mSetExpandedHeight;
    private final Consumer<ExpandableNotificationRow> mSetTrackingHeadsUp;
    private boolean mShown;
    private final View.OnLayoutChangeListener mStackScrollLayoutChangeListener;
    private final NotificationStackScrollLayout mStackScroller;
    private ExpandableNotificationRow mTrackedChild;
    private final Runnable mUpdatePanelTranslation;

    public HeadsUpAppearanceController(NotificationIconAreaController notificationIconAreaController, HeadsUpManagerPhone headsUpManagerPhone, View view) {
        this(notificationIconAreaController, headsUpManagerPhone, (HeadsUpStatusBarView) view.findViewById(R.id.heads_up_status_bar_view), (NotificationStackScrollLayout) view.findViewById(R.id.notification_stack_scroller), (NotificationPanelView) view.findViewById(R.id.notification_panel), view.findViewById(R.id.clock));
    }

    @VisibleForTesting
    public HeadsUpAppearanceController(NotificationIconAreaController notificationIconAreaController, HeadsUpManagerPhone headsUpManagerPhone, HeadsUpStatusBarView headsUpStatusBarView, NotificationStackScrollLayout notificationStackScrollLayout, NotificationPanelView notificationPanelView, View view) {
        this.mSetTrackingHeadsUp = new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.setTrackingHeadsUp((ExpandableNotificationRow) obj);
            }
        };
        this.mUpdatePanelTranslation = new Runnable() {
            @Override
            public final void run() {
                this.f$0.updatePanelTranslation();
            }
        };
        this.mSetExpandedHeight = new BiConsumer() {
            @Override
            public final void accept(Object obj, Object obj2) {
                this.f$0.setExpandedHeight(((Float) obj).floatValue(), ((Float) obj2).floatValue());
            }
        };
        this.mStackScrollLayoutChangeListener = new View.OnLayoutChangeListener() {
            @Override
            public final void onLayoutChange(View view2, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
                this.f$0.updatePanelTranslation();
            }
        };
        this.mNotificationIconAreaController = notificationIconAreaController;
        this.mHeadsUpManager = headsUpManagerPhone;
        this.mHeadsUpManager.addListener(this);
        this.mHeadsUpStatusBarView = headsUpStatusBarView;
        headsUpStatusBarView.setOnDrawingRectChangedListener(new Runnable() {
            @Override
            public final void run() {
                this.f$0.updateIsolatedIconLocation(true);
            }
        });
        this.mStackScroller = notificationStackScrollLayout;
        this.mPanelView = notificationPanelView;
        notificationPanelView.addTrackingHeadsUpListener(this.mSetTrackingHeadsUp);
        notificationPanelView.addVerticalTranslationListener(this.mUpdatePanelTranslation);
        notificationPanelView.setHeadsUpAppearanceController(this);
        this.mStackScroller.addOnExpandedHeightListener(this.mSetExpandedHeight);
        this.mStackScroller.addOnLayoutChangeListener(this.mStackScrollLayoutChangeListener);
        this.mStackScroller.setHeadsUpAppearanceController(this);
        this.mClockView = view;
        this.mDarkIconDispatcher = (DarkIconDispatcher) Dependency.get(DarkIconDispatcher.class);
        this.mDarkIconDispatcher.addDarkReceiver(this);
    }

    public void destroy() {
        this.mHeadsUpManager.removeListener(this);
        this.mHeadsUpStatusBarView.setOnDrawingRectChangedListener(null);
        this.mPanelView.removeTrackingHeadsUpListener(this.mSetTrackingHeadsUp);
        this.mPanelView.removeVerticalTranslationListener(this.mUpdatePanelTranslation);
        this.mPanelView.setHeadsUpAppearanceController(null);
        this.mStackScroller.removeOnExpandedHeightListener(this.mSetExpandedHeight);
        this.mStackScroller.removeOnLayoutChangeListener(this.mStackScrollLayoutChangeListener);
        this.mDarkIconDispatcher.removeDarkReceiver(this);
    }

    public void updateIsolatedIconLocation(boolean z) {
        this.mNotificationIconAreaController.setIsolatedIconLocation(this.mHeadsUpStatusBarView.getIconDrawingRect(), z);
    }

    @Override
    public void onHeadsUpPinned(ExpandableNotificationRow expandableNotificationRow) {
        updateTopEntry();
        updateHeader(expandableNotificationRow.getEntry());
    }

    private int getRtlTranslation() {
        if (this.mPoint == null) {
            this.mPoint = new Point();
        }
        int i = 0;
        if (this.mStackScroller.getDisplay() != null) {
            this.mStackScroller.getDisplay().getRealSize(this.mPoint);
            i = this.mPoint.x;
        }
        WindowInsets rootWindowInsets = this.mStackScroller.getRootWindowInsets();
        return ((rootWindowInsets.getSystemWindowInsetLeft() + this.mStackScroller.getRight()) + rootWindowInsets.getSystemWindowInsetRight()) - i;
    }

    public void updatePanelTranslation() {
        float left;
        if (this.mStackScroller.isLayoutRtl()) {
            left = getRtlTranslation();
        } else {
            left = this.mStackScroller.getLeft();
        }
        this.mHeadsUpStatusBarView.setPanelTranslation(left + this.mStackScroller.getTranslationX());
    }

    private void updateTopEntry() {
        NotificationData.Entry topEntry;
        boolean z;
        boolean z2;
        if (!this.mIsExpanded && this.mHeadsUpManager.hasPinnedHeadsUp()) {
            topEntry = this.mHeadsUpManager.getTopEntry();
        } else {
            topEntry = null;
        }
        NotificationData.Entry showingEntry = this.mHeadsUpStatusBarView.getShowingEntry();
        this.mHeadsUpStatusBarView.setEntry(topEntry);
        if (topEntry != showingEntry) {
            if (topEntry == null) {
                setShown(false);
                z2 = this.mIsExpanded;
            } else if (showingEntry == null) {
                setShown(true);
                z2 = this.mIsExpanded;
            } else {
                z = false;
                updateIsolatedIconLocation(false);
                this.mNotificationIconAreaController.showIconIsolated(topEntry != null ? topEntry.icon : null, z);
            }
            z = !z2;
            updateIsolatedIconLocation(false);
            this.mNotificationIconAreaController.showIconIsolated(topEntry != null ? topEntry.icon : null, z);
        }
    }

    private void setShown(boolean z) {
        if (this.mShown != z) {
            this.mShown = z;
            if (!z) {
                CrossFadeHelper.fadeIn(this.mClockView, 110L, 100);
                CrossFadeHelper.fadeOut(this.mHeadsUpStatusBarView, 110L, 0, new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mHeadsUpStatusBarView.setVisibility(8);
                    }
                });
            } else {
                this.mHeadsUpStatusBarView.setVisibility(0);
                CrossFadeHelper.fadeIn(this.mHeadsUpStatusBarView, 110L, 100);
                CrossFadeHelper.fadeOut(this.mClockView, 110L, 0, new Runnable() {
                    @Override
                    public final void run() {
                        this.f$0.mClockView.setVisibility(4);
                    }
                });
            }
        }
    }

    @VisibleForTesting
    public boolean isShown() {
        return this.mShown;
    }

    public boolean shouldBeVisible() {
        return !this.mIsExpanded && this.mHeadsUpManager.hasPinnedHeadsUp();
    }

    @Override
    public void onHeadsUpUnPinned(ExpandableNotificationRow expandableNotificationRow) {
        updateTopEntry();
        updateHeader(expandableNotificationRow.getEntry());
    }

    public void setExpandedHeight(float f, float f2) {
        boolean z = f != this.mExpandedHeight;
        this.mExpandedHeight = f;
        this.mExpandFraction = f2;
        boolean z2 = f > 0.0f;
        if (z) {
            updateHeadsUpHeaders();
        }
        if (z2 != this.mIsExpanded) {
            this.mIsExpanded = z2;
            updateTopEntry();
        }
    }

    public void setTrackingHeadsUp(ExpandableNotificationRow expandableNotificationRow) {
        ExpandableNotificationRow expandableNotificationRow2 = this.mTrackedChild;
        this.mTrackedChild = expandableNotificationRow;
        if (expandableNotificationRow2 != null) {
            updateHeader(expandableNotificationRow2.getEntry());
        }
    }

    private void updateHeadsUpHeaders() {
        this.mHeadsUpManager.getAllEntries().forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                this.f$0.updateHeader((NotificationData.Entry) obj);
            }
        });
    }

    public void updateHeader(NotificationData.Entry entry) {
        float f;
        ExpandableNotificationRow expandableNotificationRow = entry.row;
        if (expandableNotificationRow.isPinned() || expandableNotificationRow.isHeadsUpAnimatingAway() || expandableNotificationRow == this.mTrackedChild) {
            f = this.mExpandFraction;
        } else {
            f = 1.0f;
        }
        expandableNotificationRow.setHeaderVisibleAmount(f);
    }

    @Override
    public void onDarkChanged(Rect rect, float f, int i) {
        this.mHeadsUpStatusBarView.onDarkChanged(rect, f, i);
    }

    public void setPublicMode(boolean z) {
        this.mHeadsUpStatusBarView.setPublicMode(z);
        updateTopEntry();
    }
}
