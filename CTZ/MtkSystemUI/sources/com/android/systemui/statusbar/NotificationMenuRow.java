package com.android.systemui.statusbar;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.systemui.Interpolators;
import com.android.systemui.R;
import com.android.systemui.plugins.statusbar.NotificationMenuRowPlugin;
import com.android.systemui.plugins.statusbar.NotificationSwipeActionHelper;
import com.android.systemui.statusbar.ExpandableNotificationRow;
import com.android.systemui.statusbar.NotificationGuts;
import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;
import java.util.ArrayList;

public class NotificationMenuRow implements View.OnClickListener, NotificationMenuRowPlugin, ExpandableNotificationRow.LayoutListener {
    private boolean mAnimating;
    private NotificationMenuRowPlugin.MenuItem mAppOpsItem;
    private CheckForDrag mCheckForDrag;
    private Context mContext;
    private boolean mDismissing;
    private ValueAnimator mFadeAnimator;
    private boolean mIconsPlaced;
    private NotificationMenuRowPlugin.MenuItem mInfoItem;
    private boolean mIsUserTouching;
    private FrameLayout mMenuContainer;
    private boolean mMenuFadedIn;
    private NotificationMenuRowPlugin.OnMenuEventListener mMenuListener;
    private boolean mMenuSnappedOnLeft;
    private boolean mMenuSnappedTo;
    private boolean mOnLeft;
    private ExpandableNotificationRow mParent;
    private float mPrevX;
    private boolean mShouldShowMenu;
    private boolean mSnapping;
    private NotificationMenuRowPlugin.MenuItem mSnoozeItem;
    private NotificationSwipeActionHelper mSwipeHelper;
    private float mTranslation;
    private int[] mIconLocation = new int[2];
    private int[] mParentLocation = new int[2];
    private float mHorizSpaceForIcon = -1.0f;
    private int mVertSpaceForIcons = -1;
    private int mIconPadding = -1;
    private float mAlpha = 0.0f;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ArrayList<NotificationMenuRowPlugin.MenuItem> mMenuItems = new ArrayList<>();

    public NotificationMenuRow(Context context) {
        this.mContext = context;
        this.mShouldShowMenu = context.getResources().getBoolean(R.bool.config_showNotificationGear);
    }

    @Override
    public ArrayList<NotificationMenuRowPlugin.MenuItem> getMenuItems(Context context) {
        return this.mMenuItems;
    }

    @Override
    public NotificationMenuRowPlugin.MenuItem getLongpressMenuItem(Context context) {
        return this.mInfoItem;
    }

    @Override
    public NotificationMenuRowPlugin.MenuItem getAppOpsMenuItem(Context context) {
        return this.mAppOpsItem;
    }

    @Override
    public NotificationMenuRowPlugin.MenuItem getSnoozeMenuItem(Context context) {
        return this.mSnoozeItem;
    }

    @Override
    public void setSwipeActionHelper(NotificationSwipeActionHelper notificationSwipeActionHelper) {
        this.mSwipeHelper = notificationSwipeActionHelper;
    }

    @Override
    public void setMenuClickListener(NotificationMenuRowPlugin.OnMenuEventListener onMenuEventListener) {
        this.mMenuListener = onMenuEventListener;
    }

    @Override
    public void createMenu(ViewGroup viewGroup, StatusBarNotification statusBarNotification) {
        this.mParent = (ExpandableNotificationRow) viewGroup;
        createMenuViews(true);
    }

    @Override
    public boolean isMenuVisible() {
        return this.mAlpha > 0.0f;
    }

    @Override
    public View getMenuView() {
        return this.mMenuContainer;
    }

    @Override
    public void resetMenu() {
        resetState(true);
    }

    @Override
    public void onNotificationUpdated(StatusBarNotification statusBarNotification) {
        if (this.mMenuContainer == null) {
            return;
        }
        createMenuViews(!isMenuVisible());
    }

    @Override
    public void onConfigurationChanged() {
        this.mParent.setLayoutListener(this);
    }

    @Override
    public void onLayout() {
        this.mIconsPlaced = false;
        setMenuLocation();
        this.mParent.removeListener();
    }

    private void createMenuViews(boolean z) {
        Resources resources = this.mContext.getResources();
        this.mHorizSpaceForIcon = resources.getDimensionPixelSize(R.dimen.notification_menu_icon_size);
        this.mVertSpaceForIcons = resources.getDimensionPixelSize(R.dimen.notification_min_height);
        this.mMenuItems.clear();
        if (this.mParent != null && this.mParent.getStatusBarNotification() != null) {
            if (!((this.mParent.getStatusBarNotification().getNotification().flags & 64) != 0)) {
                this.mSnoozeItem = createSnoozeItem(this.mContext);
                this.mMenuItems.add(this.mSnoozeItem);
            }
        }
        this.mInfoItem = createInfoItem(this.mContext);
        this.mMenuItems.add(this.mInfoItem);
        this.mAppOpsItem = createAppOpsItem(this.mContext);
        this.mMenuItems.add(this.mAppOpsItem);
        if (this.mMenuContainer != null) {
            this.mMenuContainer.removeAllViews();
        } else {
            this.mMenuContainer = new FrameLayout(this.mContext);
        }
        for (int i = 0; i < this.mMenuItems.size(); i++) {
            addMenuView(this.mMenuItems.get(i), this.mMenuContainer);
        }
        if (z) {
            resetState(false);
            return;
        }
        this.mIconsPlaced = false;
        setMenuLocation();
        if (!this.mIsUserTouching) {
            showMenu(this.mParent, this.mOnLeft ? getSpaceForMenu() : -getSpaceForMenu(), 0.0f);
        }
    }

    private void resetState(boolean z) {
        setMenuAlpha(0.0f);
        this.mIconsPlaced = false;
        this.mMenuFadedIn = false;
        this.mAnimating = false;
        this.mSnapping = false;
        this.mDismissing = false;
        this.mMenuSnappedTo = false;
        setMenuLocation();
        if (this.mMenuListener != null && z) {
            this.mMenuListener.onMenuReset(this.mParent);
        }
    }

    @Override
    public boolean onTouchEvent(View view, MotionEvent motionEvent, float f) {
        switch (motionEvent.getActionMasked()) {
            case 0:
                this.mSnapping = false;
                if (this.mFadeAnimator != null) {
                    this.mFadeAnimator.cancel();
                }
                this.mHandler.removeCallbacks(this.mCheckForDrag);
                this.mCheckForDrag = null;
                this.mPrevX = motionEvent.getRawX();
                this.mIsUserTouching = true;
                return false;
            case 1:
                this.mIsUserTouching = false;
                return handleUpEvent(motionEvent, view, f);
            case 2:
                this.mSnapping = false;
                float rawX = motionEvent.getRawX() - this.mPrevX;
                this.mPrevX = motionEvent.getRawX();
                if (!isTowardsMenu(rawX) && isMenuLocationChange()) {
                    this.mMenuSnappedTo = false;
                    if (!this.mHandler.hasCallbacks(this.mCheckForDrag)) {
                        this.mCheckForDrag = null;
                    } else {
                        setMenuAlpha(0.0f);
                        setMenuLocation();
                    }
                }
                if (this.mShouldShowMenu && !NotificationStackScrollLayout.isPinnedHeadsUp(view) && !this.mParent.areGutsExposed() && !this.mParent.isDark() && (this.mCheckForDrag == null || !this.mHandler.hasCallbacks(this.mCheckForDrag))) {
                    this.mCheckForDrag = new CheckForDrag();
                    this.mHandler.postDelayed(this.mCheckForDrag, 60L);
                }
                return false;
            case 3:
                this.mIsUserTouching = false;
                cancelDrag();
                return false;
            default:
                return false;
        }
    }

    private boolean handleUpEvent(MotionEvent motionEvent, View view, float f) {
        if (!this.mShouldShowMenu) {
            if (this.mSwipeHelper.isDismissGesture(motionEvent)) {
                dismiss(view, f);
            } else {
                snapBack(view, f);
            }
            return true;
        }
        boolean zIsTowardsMenu = isTowardsMenu(f);
        boolean z = false;
        boolean z2 = this.mSwipeHelper.getMinDismissVelocity() <= Math.abs(f);
        this.mSwipeHelper.swipedFarEnough(this.mTranslation, this.mParent.getWidth());
        boolean z3 = !this.mParent.canViewBeDismissed() && ((double) (motionEvent.getEventTime() - motionEvent.getDownTime())) >= 200.0d;
        float spaceForMenu = this.mOnLeft ? getSpaceForMenu() : -getSpaceForMenu();
        if (this.mMenuSnappedTo && isMenuVisible() && this.mMenuSnappedOnLeft == this.mOnLeft) {
            float spaceForMenu2 = getSpaceForMenu() - (this.mHorizSpaceForIcon * 0.2f);
            float width = this.mParent.getWidth() * 0.6f;
            boolean z4 = !this.mOnLeft ? this.mTranslation >= (-spaceForMenu2) || this.mTranslation <= (-width) : this.mTranslation <= spaceForMenu2 || this.mTranslation >= width;
            if (!this.mOnLeft ? this.mTranslation > (-spaceForMenu2) : this.mTranslation < spaceForMenu2) {
                z = true;
            }
            if (z4 && !this.mSwipeHelper.isDismissGesture(motionEvent)) {
                showMenu(view, spaceForMenu, f);
            } else if (!this.mSwipeHelper.isDismissGesture(motionEvent) || z) {
                snapBack(view, f);
            } else {
                dismiss(view, f);
            }
        } else if ((!this.mSwipeHelper.isFalseGesture(motionEvent) && swipedEnoughToShowMenu() && (!z2 || z3)) || (zIsTowardsMenu && !this.mSwipeHelper.isDismissGesture(motionEvent))) {
            showMenu(view, spaceForMenu, f);
        } else if (!this.mSwipeHelper.isDismissGesture(motionEvent) || zIsTowardsMenu) {
            snapBack(view, f);
        } else {
            dismiss(view, f);
        }
        return true;
    }

    private void showMenu(View view, float f, float f2) {
        this.mMenuSnappedTo = true;
        this.mMenuSnappedOnLeft = this.mOnLeft;
        this.mMenuListener.onMenuShown(view);
        this.mSwipeHelper.snap(view, f, f2);
    }

    private void snapBack(View view, float f) {
        cancelDrag();
        this.mMenuSnappedTo = false;
        this.mSnapping = true;
        this.mSwipeHelper.snap(view, 0.0f, f);
    }

    private void dismiss(View view, float f) {
        cancelDrag();
        this.mMenuSnappedTo = false;
        this.mDismissing = true;
        this.mSwipeHelper.dismiss(view, f);
    }

    private void cancelDrag() {
        if (this.mFadeAnimator != null) {
            this.mFadeAnimator.cancel();
        }
        this.mHandler.removeCallbacks(this.mCheckForDrag);
    }

    private boolean swipedEnoughToShowMenu() {
        float f;
        if (this.mParent.canViewBeDismissed()) {
            f = 0.25f;
        } else {
            f = 0.15f;
        }
        float f2 = this.mHorizSpaceForIcon * f;
        return !this.mSwipeHelper.swipedFarEnough(0.0f, 0.0f) && isMenuVisible() && (!this.mOnLeft ? this.mTranslation >= (-f2) : this.mTranslation <= f2);
    }

    private boolean isTowardsMenu(float f) {
        return isMenuVisible() && ((this.mOnLeft && f <= 0.0f) || (!this.mOnLeft && f >= 0.0f));
    }

    @Override
    public void setAppName(String str) {
        if (str == null) {
            return;
        }
        Resources resources = this.mContext.getResources();
        int size = this.mMenuItems.size();
        for (int i = 0; i < size; i++) {
            NotificationMenuRowPlugin.MenuItem menuItem = this.mMenuItems.get(i);
            String str2 = String.format(resources.getString(R.string.notification_menu_accessibility), str, menuItem.getContentDescription());
            View menuView = menuItem.getMenuView();
            if (menuView != null) {
                menuView.setContentDescription(str2);
            }
        }
    }

    @Override
    public void onHeightUpdate() {
        float f;
        if (this.mParent == null || this.mMenuItems.size() == 0 || this.mMenuContainer == null) {
            return;
        }
        if (this.mParent.getActualHeight() < this.mVertSpaceForIcons) {
            f = (r0 / 2) - (this.mHorizSpaceForIcon / 2.0f);
        } else {
            f = (this.mVertSpaceForIcons - this.mHorizSpaceForIcon) / 2.0f;
        }
        this.mMenuContainer.setTranslationY(f);
    }

    @Override
    public void onTranslationUpdate(float f) {
        this.mTranslation = f;
        if (this.mAnimating || !this.mMenuFadedIn) {
            return;
        }
        float width = this.mParent.getWidth() * 0.3f;
        float fAbs = Math.abs(f);
        float width2 = 0.0f;
        if (fAbs != 0.0f) {
            if (fAbs > width) {
                width2 = 1.0f - ((fAbs - width) / (this.mParent.getWidth() - width));
            } else {
                width2 = 1.0f;
            }
        }
        setMenuAlpha(width2);
    }

    @Override
    public void onClick(View view) {
        if (this.mMenuListener == null) {
            return;
        }
        view.getLocationOnScreen(this.mIconLocation);
        this.mParent.getLocationOnScreen(this.mParentLocation);
        int i = (int) (this.mHorizSpaceForIcon / 2.0f);
        int height = view.getHeight() / 2;
        this.mMenuListener.onMenuClicked(this.mParent, (this.mIconLocation[0] - this.mParentLocation[0]) + i, (this.mIconLocation[1] - this.mParentLocation[1]) + height, this.mMenuItems.get(this.mMenuContainer.indexOfChild(view)));
    }

    private boolean isMenuLocationChange() {
        return (this.mOnLeft && ((this.mTranslation > ((float) (-this.mIconPadding)) ? 1 : (this.mTranslation == ((float) (-this.mIconPadding)) ? 0 : -1)) < 0)) || (!this.mOnLeft && ((this.mTranslation > ((float) this.mIconPadding) ? 1 : (this.mTranslation == ((float) this.mIconPadding) ? 0 : -1)) > 0));
    }

    private void setMenuLocation() {
        int i = 0;
        boolean z = this.mTranslation > 0.0f;
        if ((this.mIconsPlaced && z == this.mOnLeft) || this.mSnapping || this.mMenuContainer == null || !this.mMenuContainer.isAttachedToWindow()) {
            return;
        }
        int childCount = this.mMenuContainer.getChildCount();
        while (i < childCount) {
            View childAt = this.mMenuContainer.getChildAt(i);
            float f = i * this.mHorizSpaceForIcon;
            i++;
            float width = this.mParent.getWidth() - (this.mHorizSpaceForIcon * i);
            if (!z) {
                f = width;
            }
            childAt.setX(f);
        }
        this.mOnLeft = z;
        this.mIconsPlaced = true;
    }

    private void setMenuAlpha(float f) {
        this.mAlpha = f;
        if (this.mMenuContainer == null) {
            return;
        }
        if (f == 0.0f) {
            this.mMenuFadedIn = false;
            this.mMenuContainer.setVisibility(4);
        } else {
            this.mMenuContainer.setVisibility(0);
        }
        int childCount = this.mMenuContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            this.mMenuContainer.getChildAt(i).setAlpha(this.mAlpha);
        }
    }

    private float getSpaceForMenu() {
        return this.mHorizSpaceForIcon * this.mMenuContainer.getChildCount();
    }

    private final class CheckForDrag implements Runnable {
        private CheckForDrag() {
        }

        @Override
        public void run() {
            float fAbs = Math.abs(NotificationMenuRow.this.mTranslation);
            float spaceForMenu = NotificationMenuRow.this.getSpaceForMenu();
            float width = NotificationMenuRow.this.mParent.getWidth() * 0.4f;
            if ((!NotificationMenuRow.this.isMenuVisible() || NotificationMenuRow.this.isMenuLocationChange()) && fAbs >= ((double) spaceForMenu) * 0.4d && fAbs < width) {
                NotificationMenuRow.this.fadeInMenu(width);
            }
        }
    }

    private void fadeInMenu(final float f) {
        if (this.mDismissing || this.mAnimating) {
            return;
        }
        if (isMenuLocationChange()) {
            setMenuAlpha(0.0f);
        }
        final float f2 = this.mTranslation;
        final boolean z = this.mTranslation > 0.0f;
        setMenuLocation();
        this.mFadeAnimator = ValueAnimator.ofFloat(this.mAlpha, 1.0f);
        this.mFadeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                if (((z && f2 <= f) || (!z && Math.abs(f2) <= f)) && !NotificationMenuRow.this.mMenuFadedIn) {
                    NotificationMenuRow.this.setMenuAlpha(((Float) valueAnimator.getAnimatedValue()).floatValue());
                }
            }
        });
        this.mFadeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animator) {
                NotificationMenuRow.this.mAnimating = true;
            }

            @Override
            public void onAnimationCancel(Animator animator) {
                NotificationMenuRow.this.setMenuAlpha(0.0f);
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                NotificationMenuRow.this.mAnimating = false;
                NotificationMenuRow.this.mMenuFadedIn = NotificationMenuRow.this.mAlpha == 1.0f;
            }
        });
        this.mFadeAnimator.setInterpolator(Interpolators.ALPHA_IN);
        this.mFadeAnimator.setDuration(200L);
        this.mFadeAnimator.start();
    }

    @Override
    public void setMenuItems(ArrayList<NotificationMenuRowPlugin.MenuItem> arrayList) {
    }

    public static NotificationMenuRowPlugin.MenuItem createSnoozeItem(Context context) {
        Resources resources = context.getResources();
        return new NotificationMenuItem(context, resources.getString(R.string.notification_menu_snooze_description), (NotificationSnooze) LayoutInflater.from(context).inflate(R.layout.notification_snooze, (ViewGroup) null, false), R.drawable.ic_snooze);
    }

    public static NotificationMenuRowPlugin.MenuItem createInfoItem(Context context) {
        return new NotificationMenuItem(context, context.getResources().getString(R.string.notification_menu_gear_description), (NotificationInfo) LayoutInflater.from(context).inflate(R.layout.notification_info, (ViewGroup) null, false), R.drawable.ic_settings);
    }

    public static NotificationMenuRowPlugin.MenuItem createAppOpsItem(Context context) {
        return new NotificationMenuItem(context, null, (AppOpsInfo) LayoutInflater.from(context).inflate(R.layout.app_ops_info, (ViewGroup) null, false), -1);
    }

    private void addMenuView(NotificationMenuRowPlugin.MenuItem menuItem, ViewGroup viewGroup) {
        View menuView = menuItem.getMenuView();
        if (menuView != null) {
            viewGroup.addView(menuView);
            menuView.setOnClickListener(this);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) menuView.getLayoutParams();
            layoutParams.width = (int) this.mHorizSpaceForIcon;
            layoutParams.height = (int) this.mHorizSpaceForIcon;
            menuView.setLayoutParams(layoutParams);
        }
    }

    public static class NotificationMenuItem implements NotificationMenuRowPlugin.MenuItem {
        String mContentDescription;
        NotificationGuts.GutsContent mGutsContent;
        View mMenuView;

        public NotificationMenuItem(Context context, String str, NotificationGuts.GutsContent gutsContent, int i) {
            Resources resources = context.getResources();
            int dimensionPixelSize = resources.getDimensionPixelSize(R.dimen.notification_menu_icon_padding);
            int color = resources.getColor(R.color.notification_gear_color);
            if (i >= 0) {
                AlphaOptimizedImageView alphaOptimizedImageView = new AlphaOptimizedImageView(context);
                alphaOptimizedImageView.setPadding(dimensionPixelSize, dimensionPixelSize, dimensionPixelSize, dimensionPixelSize);
                alphaOptimizedImageView.setImageDrawable(context.getResources().getDrawable(i));
                alphaOptimizedImageView.setColorFilter(color);
                alphaOptimizedImageView.setAlpha(1.0f);
                this.mMenuView = alphaOptimizedImageView;
            }
            this.mContentDescription = str;
            this.mGutsContent = gutsContent;
        }

        @Override
        public View getMenuView() {
            return this.mMenuView;
        }

        @Override
        public View getGutsView() {
            return this.mGutsContent.getContentView();
        }

        @Override
        public String getContentDescription() {
            return this.mContentDescription;
        }
    }
}
