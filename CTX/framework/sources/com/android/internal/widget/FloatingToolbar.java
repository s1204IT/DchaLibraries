package com.android.internal.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Size;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import com.android.internal.R;
import com.android.internal.util.Preconditions;
import com.android.internal.widget.FloatingToolbar;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public final class FloatingToolbar {
    public static final String FLOATING_TOOLBAR_TAG = "floating_toolbar";
    private static final MenuItem.OnMenuItemClickListener NO_OP_MENUITEM_CLICK_LISTENER = new MenuItem.OnMenuItemClickListener() {
        @Override
        public final boolean onMenuItemClick(MenuItem menuItem) {
            return FloatingToolbar.lambda$static$0(menuItem);
        }
    };
    private final Context mContext;
    private Menu mMenu;
    private final FloatingToolbarPopup mPopup;
    private int mSuggestedWidth;
    private final Window mWindow;
    private final Rect mContentRect = new Rect();
    private final Rect mPreviousContentRect = new Rect();
    private List<MenuItem> mShowingMenuItems = new ArrayList();
    private MenuItem.OnMenuItemClickListener mMenuItemClickListener = NO_OP_MENUITEM_CLICK_LISTENER;
    private boolean mWidthChanged = true;
    private final View.OnLayoutChangeListener mOrientationChangeHandler = new View.OnLayoutChangeListener() {
        private final Rect mNewRect = new Rect();
        private final Rect mOldRect = new Rect();

        @Override
        public void onLayoutChange(View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
            this.mNewRect.set(i, i2, i3, i4);
            this.mOldRect.set(i5, i6, i7, i8);
            if (FloatingToolbar.this.mPopup.isShowing() && !this.mNewRect.equals(this.mOldRect)) {
                FloatingToolbar.this.mWidthChanged = true;
                FloatingToolbar.this.updateLayout();
            }
        }
    };
    private final Comparator<MenuItem> mMenuItemComparator = new Comparator() {
        @Override
        public final int compare(Object obj, Object obj2) {
            return FloatingToolbar.lambda$new$1((MenuItem) obj, (MenuItem) obj2);
        }
    };

    static boolean lambda$static$0(MenuItem menuItem) {
        return false;
    }

    static int lambda$new$1(MenuItem menuItem, MenuItem menuItem2) {
        if (menuItem.getItemId() == 16908353) {
            return menuItem2.getItemId() == 16908353 ? 0 : -1;
        }
        if (menuItem2.getItemId() == 16908353) {
            return 1;
        }
        if (menuItem.requiresActionButton()) {
            return menuItem2.requiresActionButton() ? 0 : -1;
        }
        if (menuItem2.requiresActionButton()) {
            return 1;
        }
        if (menuItem.requiresOverflow()) {
            return !menuItem2.requiresOverflow() ? 1 : 0;
        }
        if (menuItem2.requiresOverflow()) {
            return -1;
        }
        return menuItem.getOrder() - menuItem2.getOrder();
    }

    public FloatingToolbar(Window window) {
        this.mContext = applyDefaultTheme(window.getContext());
        this.mWindow = (Window) Preconditions.checkNotNull(window);
        this.mPopup = new FloatingToolbarPopup(this.mContext, window.getDecorView());
    }

    public FloatingToolbar setMenu(Menu menu) {
        this.mMenu = (Menu) Preconditions.checkNotNull(menu);
        return this;
    }

    public FloatingToolbar setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener onMenuItemClickListener) {
        if (onMenuItemClickListener != null) {
            this.mMenuItemClickListener = onMenuItemClickListener;
        } else {
            this.mMenuItemClickListener = NO_OP_MENUITEM_CLICK_LISTENER;
        }
        return this;
    }

    public FloatingToolbar setContentRect(Rect rect) {
        this.mContentRect.set((Rect) Preconditions.checkNotNull(rect));
        return this;
    }

    public FloatingToolbar setSuggestedWidth(int i) {
        this.mWidthChanged = ((double) Math.abs(i - this.mSuggestedWidth)) > ((double) this.mSuggestedWidth) * 0.2d;
        this.mSuggestedWidth = i;
        return this;
    }

    public FloatingToolbar show() {
        registerOrientationHandler();
        doShow();
        return this;
    }

    public FloatingToolbar updateLayout() {
        if (this.mPopup.isShowing()) {
            doShow();
        }
        return this;
    }

    public void dismiss() {
        unregisterOrientationHandler();
        this.mPopup.dismiss();
    }

    public void hide() {
        this.mPopup.hide();
    }

    public boolean isShowing() {
        return this.mPopup.isShowing();
    }

    public boolean isHidden() {
        return this.mPopup.isHidden();
    }

    public void setOutsideTouchable(boolean z, PopupWindow.OnDismissListener onDismissListener) {
        if (this.mPopup.setOutsideTouchable(z, onDismissListener) && isShowing()) {
            dismiss();
            doShow();
        }
    }

    private void doShow() {
        List<MenuItem> visibleAndEnabledMenuItems = getVisibleAndEnabledMenuItems(this.mMenu);
        visibleAndEnabledMenuItems.sort(this.mMenuItemComparator);
        if (!isCurrentlyShowing(visibleAndEnabledMenuItems) || this.mWidthChanged) {
            this.mPopup.dismiss();
            this.mPopup.layoutMenuItems(visibleAndEnabledMenuItems, this.mMenuItemClickListener, this.mSuggestedWidth);
            this.mShowingMenuItems = visibleAndEnabledMenuItems;
        }
        if (!this.mPopup.isShowing()) {
            this.mPopup.show(this.mContentRect);
        } else if (!this.mPreviousContentRect.equals(this.mContentRect)) {
            this.mPopup.updateCoordinates(this.mContentRect);
        }
        this.mWidthChanged = false;
        this.mPreviousContentRect.set(this.mContentRect);
    }

    private boolean isCurrentlyShowing(List<MenuItem> list) {
        if (this.mShowingMenuItems == null || list.size() != this.mShowingMenuItems.size()) {
            return false;
        }
        int size = list.size();
        for (int i = 0; i < size; i++) {
            MenuItem menuItem = list.get(i);
            MenuItem menuItem2 = this.mShowingMenuItems.get(i);
            if (menuItem.getItemId() != menuItem2.getItemId() || !TextUtils.equals(menuItem.getTitle(), menuItem2.getTitle()) || !Objects.equals(menuItem.getIcon(), menuItem2.getIcon()) || menuItem.getGroupId() != menuItem2.getGroupId()) {
                return false;
            }
        }
        return true;
    }

    private List<MenuItem> getVisibleAndEnabledMenuItems(Menu menu) {
        ArrayList arrayList = new ArrayList();
        for (int i = 0; menu != null && i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item.isVisible() && item.isEnabled()) {
                SubMenu subMenu = item.getSubMenu();
                if (subMenu != null) {
                    arrayList.addAll(getVisibleAndEnabledMenuItems(subMenu));
                } else {
                    arrayList.add(item);
                }
            }
        }
        return arrayList;
    }

    private void registerOrientationHandler() {
        unregisterOrientationHandler();
        this.mWindow.getDecorView().addOnLayoutChangeListener(this.mOrientationChangeHandler);
    }

    private void unregisterOrientationHandler() {
        this.mWindow.getDecorView().removeOnLayoutChangeListener(this.mOrientationChangeHandler);
    }

    private static final class FloatingToolbarPopup {
        private static final int MAX_OVERFLOW_SIZE = 4;
        private static final int MIN_OVERFLOW_SIZE = 2;
        private final Drawable mArrow;
        private final AnimationSet mCloseOverflowAnimation;
        private final ViewGroup mContentContainer;
        private final Context mContext;
        private final AnimatorSet mDismissAnimation;
        private final Interpolator mFastOutLinearInInterpolator;
        private final Interpolator mFastOutSlowInInterpolator;
        private boolean mHidden;
        private final AnimatorSet mHideAnimation;
        private final int mIconTextSpacing;
        private boolean mIsOverflowOpen;
        private final int mLineHeight;
        private final Interpolator mLinearOutSlowInInterpolator;
        private final ViewGroup mMainPanel;
        private Size mMainPanelSize;
        private final int mMarginHorizontal;
        private final int mMarginVertical;
        private MenuItem.OnMenuItemClickListener mOnMenuItemClickListener;
        private final AnimationSet mOpenOverflowAnimation;
        private boolean mOpenOverflowUpwards;
        private final Drawable mOverflow;
        private final Animation.AnimationListener mOverflowAnimationListener;
        private final ImageButton mOverflowButton;
        private final Size mOverflowButtonSize;
        private final OverflowPanel mOverflowPanel;
        private Size mOverflowPanelSize;
        private final OverflowPanelViewHelper mOverflowPanelViewHelper;
        private final View mParent;
        private final PopupWindow mPopupWindow;
        private final AnimatorSet mShowAnimation;
        private final AnimatedVectorDrawable mToArrow;
        private final AnimatedVectorDrawable mToOverflow;
        private int mTransitionDurationScale;
        private final Rect mViewPortOnScreen = new Rect();
        private final Point mCoordsOnWindow = new Point();
        private final int[] mTmpCoords = new int[2];
        private final Region mTouchableRegion = new Region();
        private final ViewTreeObserver.OnComputeInternalInsetsListener mInsetsComputer = new ViewTreeObserver.OnComputeInternalInsetsListener() {
            @Override
            public final void onComputeInternalInsets(ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
                FloatingToolbar.FloatingToolbarPopup.lambda$new$0(this.f$0, internalInsetsInfo);
            }
        };
        private final Runnable mPreparePopupContentRTLHelper = new Runnable() {
            @Override
            public void run() {
                FloatingToolbarPopup.this.setPanelsStatesAtRestingPosition();
                FloatingToolbarPopup.this.setContentAreaAsTouchableSurface();
                FloatingToolbarPopup.this.mContentContainer.setAlpha(1.0f);
            }
        };
        private boolean mDismissed = true;
        private final View.OnClickListener mMenuItemButtonOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((view.getTag() instanceof MenuItem) && FloatingToolbarPopup.this.mOnMenuItemClickListener != null) {
                    FloatingToolbarPopup.this.mOnMenuItemClickListener.onMenuItemClick((MenuItem) view.getTag());
                }
            }
        };
        private final Interpolator mLogAccelerateInterpolator = new LogAccelerateInterpolator();

        public static void lambda$new$0(FloatingToolbarPopup floatingToolbarPopup, ViewTreeObserver.InternalInsetsInfo internalInsetsInfo) {
            internalInsetsInfo.contentInsets.setEmpty();
            internalInsetsInfo.visibleInsets.setEmpty();
            internalInsetsInfo.touchableRegion.set(floatingToolbarPopup.mTouchableRegion);
            internalInsetsInfo.setTouchableInsets(3);
        }

        public FloatingToolbarPopup(Context context, View view) {
            this.mParent = (View) Preconditions.checkNotNull(view);
            this.mContext = (Context) Preconditions.checkNotNull(context);
            this.mContentContainer = FloatingToolbar.createContentContainer(context);
            this.mPopupWindow = FloatingToolbar.createPopupWindow(this.mContentContainer);
            this.mMarginHorizontal = view.getResources().getDimensionPixelSize(R.dimen.floating_toolbar_horizontal_margin);
            this.mMarginVertical = view.getResources().getDimensionPixelSize(R.dimen.floating_toolbar_vertical_margin);
            this.mLineHeight = context.getResources().getDimensionPixelSize(R.dimen.floating_toolbar_height);
            this.mIconTextSpacing = context.getResources().getDimensionPixelSize(R.dimen.floating_toolbar_icon_text_spacing);
            this.mFastOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563661);
            this.mLinearOutSlowInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563662);
            this.mFastOutLinearInInterpolator = AnimationUtils.loadInterpolator(this.mContext, 17563663);
            this.mArrow = this.mContext.getResources().getDrawable(R.drawable.ft_avd_tooverflow, this.mContext.getTheme());
            this.mArrow.setAutoMirrored(true);
            this.mOverflow = this.mContext.getResources().getDrawable(R.drawable.ft_avd_toarrow, this.mContext.getTheme());
            this.mOverflow.setAutoMirrored(true);
            this.mToArrow = (AnimatedVectorDrawable) this.mContext.getResources().getDrawable(R.drawable.ft_avd_toarrow_animation, this.mContext.getTheme());
            this.mToArrow.setAutoMirrored(true);
            this.mToOverflow = (AnimatedVectorDrawable) this.mContext.getResources().getDrawable(R.drawable.ft_avd_tooverflow_animation, this.mContext.getTheme());
            this.mToOverflow.setAutoMirrored(true);
            this.mOverflowButton = createOverflowButton();
            this.mOverflowButtonSize = measure(this.mOverflowButton);
            this.mMainPanel = createMainPanel();
            this.mOverflowPanelViewHelper = new OverflowPanelViewHelper(this.mContext, this.mIconTextSpacing);
            this.mOverflowPanel = createOverflowPanel();
            this.mOverflowAnimationListener = createOverflowAnimationListener();
            this.mOpenOverflowAnimation = new AnimationSet(true);
            this.mOpenOverflowAnimation.setAnimationListener(this.mOverflowAnimationListener);
            this.mCloseOverflowAnimation = new AnimationSet(true);
            this.mCloseOverflowAnimation.setAnimationListener(this.mOverflowAnimationListener);
            this.mShowAnimation = FloatingToolbar.createEnterAnimation(this.mContentContainer);
            this.mDismissAnimation = FloatingToolbar.createExitAnimation(this.mContentContainer, 150, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    FloatingToolbarPopup.this.mPopupWindow.dismiss();
                    FloatingToolbarPopup.this.mContentContainer.removeAllViews();
                }
            });
            this.mHideAnimation = FloatingToolbar.createExitAnimation(this.mContentContainer, 0, new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animator) {
                    FloatingToolbarPopup.this.mPopupWindow.dismiss();
                }
            });
        }

        public boolean setOutsideTouchable(boolean z, PopupWindow.OnDismissListener onDismissListener) {
            boolean z2 = true;
            if (this.mPopupWindow.isOutsideTouchable() ^ z) {
                this.mPopupWindow.setOutsideTouchable(z);
                this.mPopupWindow.setFocusable(!z);
            } else {
                z2 = false;
            }
            this.mPopupWindow.setOnDismissListener(onDismissListener);
            return z2;
        }

        public void layoutMenuItems(List<MenuItem> list, MenuItem.OnMenuItemClickListener onMenuItemClickListener, int i) {
            this.mOnMenuItemClickListener = onMenuItemClickListener;
            cancelOverflowAnimations();
            clearPanels();
            List<MenuItem> listLayoutMainPanelItems = layoutMainPanelItems(list, getAdjustedToolbarWidth(i));
            if (!listLayoutMainPanelItems.isEmpty()) {
                layoutOverflowPanelItems(listLayoutMainPanelItems);
            }
            updatePopupSize();
        }

        public void show(Rect rect) {
            Preconditions.checkNotNull(rect);
            if (isShowing()) {
                return;
            }
            this.mHidden = false;
            this.mDismissed = false;
            cancelDismissAndHideAnimations();
            cancelOverflowAnimations();
            refreshCoordinatesAndOverflowDirection(rect);
            preparePopupContent();
            this.mPopupWindow.showAtLocation(this.mParent, 0, this.mCoordsOnWindow.x, this.mCoordsOnWindow.y);
            setTouchableSurfaceInsetsComputer();
            runShowAnimation();
        }

        public void dismiss() {
            if (this.mDismissed) {
                return;
            }
            this.mHidden = false;
            this.mDismissed = true;
            this.mHideAnimation.cancel();
            runDismissAnimation();
            setZeroTouchableSurface();
        }

        public void hide() {
            if (!isShowing()) {
                return;
            }
            this.mHidden = true;
            runHideAnimation();
            setZeroTouchableSurface();
        }

        public boolean isShowing() {
            return (this.mDismissed || this.mHidden) ? false : true;
        }

        public boolean isHidden() {
            return this.mHidden;
        }

        public void updateCoordinates(Rect rect) {
            Preconditions.checkNotNull(rect);
            if (!isShowing() || !this.mPopupWindow.isShowing()) {
                return;
            }
            cancelOverflowAnimations();
            refreshCoordinatesAndOverflowDirection(rect);
            preparePopupContent();
            this.mPopupWindow.update(this.mCoordsOnWindow.x, this.mCoordsOnWindow.y, this.mPopupWindow.getWidth(), this.mPopupWindow.getHeight());
        }

        private void refreshCoordinatesAndOverflowDirection(Rect rect) {
            int height;
            refreshViewPort();
            int iMin = Math.min(rect.centerX() - (this.mPopupWindow.getWidth() / 2), this.mViewPortOnScreen.right - this.mPopupWindow.getWidth());
            int i = rect.top - this.mViewPortOnScreen.top;
            int i2 = this.mViewPortOnScreen.bottom - rect.bottom;
            int i3 = this.mMarginVertical * 2;
            int i4 = this.mLineHeight + i3;
            if (!hasOverflow()) {
                if (i >= i4) {
                    height = rect.top - i4;
                } else if (i2 >= i4) {
                    height = rect.bottom;
                } else if (i2 >= this.mLineHeight) {
                    height = rect.bottom - this.mMarginVertical;
                } else {
                    height = Math.max(this.mViewPortOnScreen.top, rect.top - i4);
                }
            } else {
                int iCalculateOverflowHeight = calculateOverflowHeight(2) + i3;
                int i5 = (this.mViewPortOnScreen.bottom - rect.top) + i4;
                int i6 = (rect.bottom - this.mViewPortOnScreen.top) + i4;
                if (i >= iCalculateOverflowHeight) {
                    updateOverflowHeight(i - i3);
                    height = rect.top - this.mPopupWindow.getHeight();
                    this.mOpenOverflowUpwards = true;
                } else if (i >= i4 && i5 >= iCalculateOverflowHeight) {
                    updateOverflowHeight(i5 - i3);
                    height = rect.top - i4;
                    this.mOpenOverflowUpwards = false;
                } else if (i2 >= iCalculateOverflowHeight) {
                    updateOverflowHeight(i2 - i3);
                    height = rect.bottom;
                    this.mOpenOverflowUpwards = false;
                } else if (i2 >= i4 && this.mViewPortOnScreen.height() >= iCalculateOverflowHeight) {
                    updateOverflowHeight(i6 - i3);
                    height = (rect.bottom + i4) - this.mPopupWindow.getHeight();
                    this.mOpenOverflowUpwards = true;
                } else {
                    updateOverflowHeight(this.mViewPortOnScreen.height() - i3);
                    height = this.mViewPortOnScreen.top;
                    this.mOpenOverflowUpwards = false;
                }
            }
            this.mParent.getRootView().getLocationOnScreen(this.mTmpCoords);
            int i7 = this.mTmpCoords[0];
            int i8 = this.mTmpCoords[1];
            this.mParent.getRootView().getLocationInWindow(this.mTmpCoords);
            this.mCoordsOnWindow.set(Math.max(0, iMin - (i7 - this.mTmpCoords[0])), Math.max(0, height - (i8 - this.mTmpCoords[1])));
        }

        private void runShowAnimation() {
            this.mShowAnimation.start();
        }

        private void runDismissAnimation() {
            this.mDismissAnimation.start();
        }

        private void runHideAnimation() {
            this.mHideAnimation.start();
        }

        private void cancelDismissAndHideAnimations() {
            this.mDismissAnimation.cancel();
            this.mHideAnimation.cancel();
        }

        private void cancelOverflowAnimations() {
            this.mContentContainer.clearAnimation();
            this.mMainPanel.animate().cancel();
            this.mOverflowPanel.animate().cancel();
            this.mToArrow.stop();
            this.mToOverflow.stop();
        }

        private void openOverflow() {
            final float width;
            final int width2 = this.mOverflowPanelSize.getWidth();
            final int height = this.mOverflowPanelSize.getHeight();
            final int width3 = this.mContentContainer.getWidth();
            final int height2 = this.mContentContainer.getHeight();
            final float y = this.mContentContainer.getY();
            final float x = this.mContentContainer.getX();
            final float width4 = x + this.mContentContainer.getWidth();
            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float f, Transformation transformation) {
                    FloatingToolbarPopup.setWidth(FloatingToolbarPopup.this.mContentContainer, width3 + ((int) (f * (width2 - width3))));
                    if (FloatingToolbarPopup.this.isInRTLMode()) {
                        FloatingToolbarPopup.this.mContentContainer.setX(x);
                        FloatingToolbarPopup.this.mMainPanel.setX(0.0f);
                        FloatingToolbarPopup.this.mOverflowPanel.setX(0.0f);
                    } else {
                        FloatingToolbarPopup.this.mContentContainer.setX(width4 - FloatingToolbarPopup.this.mContentContainer.getWidth());
                        FloatingToolbarPopup.this.mMainPanel.setX(FloatingToolbarPopup.this.mContentContainer.getWidth() - width3);
                        FloatingToolbarPopup.this.mOverflowPanel.setX(FloatingToolbarPopup.this.mContentContainer.getWidth() - width2);
                    }
                }
            };
            Animation animation2 = new Animation() {
                @Override
                protected void applyTransformation(float f, Transformation transformation) {
                    FloatingToolbarPopup.setHeight(FloatingToolbarPopup.this.mContentContainer, height2 + ((int) (f * (height - height2))));
                    if (FloatingToolbarPopup.this.mOpenOverflowUpwards) {
                        FloatingToolbarPopup.this.mContentContainer.setY(y - (FloatingToolbarPopup.this.mContentContainer.getHeight() - height2));
                        FloatingToolbarPopup.this.positionContentYCoordinatesIfOpeningOverflowUpwards();
                    }
                }
            };
            final float x2 = this.mOverflowButton.getX();
            if (isInRTLMode()) {
                width = (width2 + x2) - this.mOverflowButton.getWidth();
            } else {
                width = (x2 - width2) + this.mOverflowButton.getWidth();
            }
            Animation animation3 = new Animation() {
                @Override
                protected void applyTransformation(float f, Transformation transformation) {
                    float width5;
                    float f2 = x2 + (f * (width - x2));
                    if (!FloatingToolbarPopup.this.isInRTLMode()) {
                        width5 = FloatingToolbarPopup.this.mContentContainer.getWidth() - width3;
                    } else {
                        width5 = 0.0f;
                    }
                    FloatingToolbarPopup.this.mOverflowButton.setX(f2 + width5);
                }
            };
            animation.setInterpolator(this.mLogAccelerateInterpolator);
            animation.setDuration(getAdjustedDuration(250));
            animation2.setInterpolator(this.mFastOutSlowInInterpolator);
            animation2.setDuration(getAdjustedDuration(250));
            animation3.setInterpolator(this.mFastOutSlowInInterpolator);
            animation3.setDuration(getAdjustedDuration(250));
            this.mOpenOverflowAnimation.getAnimations().clear();
            this.mOpenOverflowAnimation.getAnimations().clear();
            this.mOpenOverflowAnimation.addAnimation(animation);
            this.mOpenOverflowAnimation.addAnimation(animation2);
            this.mOpenOverflowAnimation.addAnimation(animation3);
            this.mContentContainer.startAnimation(this.mOpenOverflowAnimation);
            this.mIsOverflowOpen = true;
            this.mMainPanel.animate().alpha(0.0f).withLayer().setInterpolator(this.mLinearOutSlowInInterpolator).setDuration(250L).start();
            this.mOverflowPanel.setAlpha(1.0f);
        }

        private void closeOverflow() {
            final float width;
            final int width2 = this.mMainPanelSize.getWidth();
            final int width3 = this.mContentContainer.getWidth();
            final float x = this.mContentContainer.getX();
            final float width4 = x + this.mContentContainer.getWidth();
            Animation animation = new Animation() {
                @Override
                protected void applyTransformation(float f, Transformation transformation) {
                    FloatingToolbarPopup.setWidth(FloatingToolbarPopup.this.mContentContainer, width3 + ((int) (f * (width2 - width3))));
                    if (FloatingToolbarPopup.this.isInRTLMode()) {
                        FloatingToolbarPopup.this.mContentContainer.setX(x);
                        FloatingToolbarPopup.this.mMainPanel.setX(0.0f);
                        FloatingToolbarPopup.this.mOverflowPanel.setX(0.0f);
                    } else {
                        FloatingToolbarPopup.this.mContentContainer.setX(width4 - FloatingToolbarPopup.this.mContentContainer.getWidth());
                        FloatingToolbarPopup.this.mMainPanel.setX(FloatingToolbarPopup.this.mContentContainer.getWidth() - width2);
                        FloatingToolbarPopup.this.mOverflowPanel.setX(FloatingToolbarPopup.this.mContentContainer.getWidth() - width3);
                    }
                }
            };
            final int height = this.mMainPanelSize.getHeight();
            final int height2 = this.mContentContainer.getHeight();
            final float y = this.mContentContainer.getY() + this.mContentContainer.getHeight();
            Animation animation2 = new Animation() {
                @Override
                protected void applyTransformation(float f, Transformation transformation) {
                    FloatingToolbarPopup.setHeight(FloatingToolbarPopup.this.mContentContainer, height2 + ((int) (f * (height - height2))));
                    if (FloatingToolbarPopup.this.mOpenOverflowUpwards) {
                        FloatingToolbarPopup.this.mContentContainer.setY(y - FloatingToolbarPopup.this.mContentContainer.getHeight());
                        FloatingToolbarPopup.this.positionContentYCoordinatesIfOpeningOverflowUpwards();
                    }
                }
            };
            final float x2 = this.mOverflowButton.getX();
            if (isInRTLMode()) {
                width = (x2 - width3) + this.mOverflowButton.getWidth();
            } else {
                width = (width3 + x2) - this.mOverflowButton.getWidth();
            }
            Animation animation3 = new Animation() {
                @Override
                protected void applyTransformation(float f, Transformation transformation) {
                    float width5;
                    float f2 = x2 + (f * (width - x2));
                    if (!FloatingToolbarPopup.this.isInRTLMode()) {
                        width5 = FloatingToolbarPopup.this.mContentContainer.getWidth() - width3;
                    } else {
                        width5 = 0.0f;
                    }
                    FloatingToolbarPopup.this.mOverflowButton.setX(f2 + width5);
                }
            };
            animation.setInterpolator(this.mFastOutSlowInInterpolator);
            animation.setDuration(getAdjustedDuration(250));
            animation2.setInterpolator(this.mLogAccelerateInterpolator);
            animation2.setDuration(getAdjustedDuration(250));
            animation3.setInterpolator(this.mFastOutSlowInInterpolator);
            animation3.setDuration(getAdjustedDuration(250));
            this.mCloseOverflowAnimation.getAnimations().clear();
            this.mCloseOverflowAnimation.addAnimation(animation);
            this.mCloseOverflowAnimation.addAnimation(animation2);
            this.mCloseOverflowAnimation.addAnimation(animation3);
            this.mContentContainer.startAnimation(this.mCloseOverflowAnimation);
            this.mIsOverflowOpen = false;
            this.mMainPanel.animate().alpha(1.0f).withLayer().setInterpolator(this.mFastOutLinearInInterpolator).setDuration(100L).start();
            this.mOverflowPanel.animate().alpha(0.0f).withLayer().setInterpolator(this.mLinearOutSlowInInterpolator).setDuration(150L).start();
        }

        private void setPanelsStatesAtRestingPosition() {
            this.mOverflowButton.setEnabled(true);
            this.mOverflowPanel.awakenScrollBars();
            if (this.mIsOverflowOpen) {
                setSize(this.mContentContainer, this.mOverflowPanelSize);
                this.mMainPanel.setAlpha(0.0f);
                this.mMainPanel.setVisibility(4);
                this.mOverflowPanel.setAlpha(1.0f);
                this.mOverflowPanel.setVisibility(0);
                this.mOverflowButton.setImageDrawable(this.mArrow);
                this.mOverflowButton.setContentDescription(this.mContext.getString(R.string.floating_toolbar_close_overflow_description));
                if (isInRTLMode()) {
                    this.mContentContainer.setX(this.mMarginHorizontal);
                    this.mMainPanel.setX(0.0f);
                    this.mOverflowButton.setX(r0.getWidth() - this.mOverflowButtonSize.getWidth());
                    this.mOverflowPanel.setX(0.0f);
                } else {
                    this.mContentContainer.setX((this.mPopupWindow.getWidth() - r0.getWidth()) - this.mMarginHorizontal);
                    this.mMainPanel.setX(-this.mContentContainer.getX());
                    this.mOverflowButton.setX(0.0f);
                    this.mOverflowPanel.setX(0.0f);
                }
                if (this.mOpenOverflowUpwards) {
                    this.mContentContainer.setY(this.mMarginVertical);
                    this.mMainPanel.setY(r0.getHeight() - this.mContentContainer.getHeight());
                    this.mOverflowButton.setY(r0.getHeight() - this.mOverflowButtonSize.getHeight());
                    this.mOverflowPanel.setY(0.0f);
                    return;
                }
                this.mContentContainer.setY(this.mMarginVertical);
                this.mMainPanel.setY(0.0f);
                this.mOverflowButton.setY(0.0f);
                this.mOverflowPanel.setY(this.mOverflowButtonSize.getHeight());
                return;
            }
            setSize(this.mContentContainer, this.mMainPanelSize);
            this.mMainPanel.setAlpha(1.0f);
            this.mMainPanel.setVisibility(0);
            this.mOverflowPanel.setAlpha(0.0f);
            this.mOverflowPanel.setVisibility(4);
            this.mOverflowButton.setImageDrawable(this.mOverflow);
            this.mOverflowButton.setContentDescription(this.mContext.getString(R.string.floating_toolbar_open_overflow_description));
            if (hasOverflow()) {
                if (isInRTLMode()) {
                    this.mContentContainer.setX(this.mMarginHorizontal);
                    this.mMainPanel.setX(0.0f);
                    this.mOverflowButton.setX(0.0f);
                    this.mOverflowPanel.setX(0.0f);
                } else {
                    this.mContentContainer.setX((this.mPopupWindow.getWidth() - r0.getWidth()) - this.mMarginHorizontal);
                    this.mMainPanel.setX(0.0f);
                    this.mOverflowButton.setX(r0.getWidth() - this.mOverflowButtonSize.getWidth());
                    this.mOverflowPanel.setX(r0.getWidth() - this.mOverflowPanelSize.getWidth());
                }
                if (this.mOpenOverflowUpwards) {
                    this.mContentContainer.setY((this.mMarginVertical + this.mOverflowPanelSize.getHeight()) - r0.getHeight());
                    this.mMainPanel.setY(0.0f);
                    this.mOverflowButton.setY(0.0f);
                    this.mOverflowPanel.setY(r0.getHeight() - this.mOverflowPanelSize.getHeight());
                    return;
                }
                this.mContentContainer.setY(this.mMarginVertical);
                this.mMainPanel.setY(0.0f);
                this.mOverflowButton.setY(0.0f);
                this.mOverflowPanel.setY(this.mOverflowButtonSize.getHeight());
                return;
            }
            this.mContentContainer.setX(this.mMarginHorizontal);
            this.mContentContainer.setY(this.mMarginVertical);
            this.mMainPanel.setX(0.0f);
            this.mMainPanel.setY(0.0f);
        }

        private void updateOverflowHeight(int i) {
            if (hasOverflow()) {
                int iCalculateOverflowHeight = calculateOverflowHeight((i - this.mOverflowButtonSize.getHeight()) / this.mLineHeight);
                if (this.mOverflowPanelSize.getHeight() != iCalculateOverflowHeight) {
                    this.mOverflowPanelSize = new Size(this.mOverflowPanelSize.getWidth(), iCalculateOverflowHeight);
                }
                setSize(this.mOverflowPanel, this.mOverflowPanelSize);
                if (this.mIsOverflowOpen) {
                    setSize(this.mContentContainer, this.mOverflowPanelSize);
                    if (this.mOpenOverflowUpwards) {
                        float height = this.mOverflowPanelSize.getHeight() - iCalculateOverflowHeight;
                        this.mContentContainer.setY(this.mContentContainer.getY() + height);
                        this.mOverflowButton.setY(this.mOverflowButton.getY() - height);
                    }
                } else {
                    setSize(this.mContentContainer, this.mMainPanelSize);
                }
                updatePopupSize();
            }
        }

        private void updatePopupSize() {
            int iMax;
            int iMax2 = 0;
            if (this.mMainPanelSize != null) {
                iMax = Math.max(0, this.mMainPanelSize.getWidth());
                iMax2 = Math.max(0, this.mMainPanelSize.getHeight());
            } else {
                iMax = 0;
            }
            if (this.mOverflowPanelSize != null) {
                iMax = Math.max(iMax, this.mOverflowPanelSize.getWidth());
                iMax2 = Math.max(iMax2, this.mOverflowPanelSize.getHeight());
            }
            this.mPopupWindow.setWidth(iMax + (this.mMarginHorizontal * 2));
            this.mPopupWindow.setHeight(iMax2 + (this.mMarginVertical * 2));
            maybeComputeTransitionDurationScale();
        }

        private void refreshViewPort() {
            this.mParent.getWindowVisibleDisplayFrame(this.mViewPortOnScreen);
        }

        private int getAdjustedToolbarWidth(int i) {
            refreshViewPort();
            int iWidth = this.mViewPortOnScreen.width() - (2 * this.mParent.getResources().getDimensionPixelSize(R.dimen.floating_toolbar_horizontal_margin));
            if (i <= 0) {
                i = this.mParent.getResources().getDimensionPixelSize(R.dimen.floating_toolbar_preferred_width);
            }
            return Math.min(i, iWidth);
        }

        private void setZeroTouchableSurface() {
            this.mTouchableRegion.setEmpty();
        }

        private void setContentAreaAsTouchableSurface() {
            int width;
            int height;
            Preconditions.checkNotNull(this.mMainPanelSize);
            if (this.mIsOverflowOpen) {
                Preconditions.checkNotNull(this.mOverflowPanelSize);
                width = this.mOverflowPanelSize.getWidth();
                height = this.mOverflowPanelSize.getHeight();
            } else {
                width = this.mMainPanelSize.getWidth();
                height = this.mMainPanelSize.getHeight();
            }
            this.mTouchableRegion.set((int) this.mContentContainer.getX(), (int) this.mContentContainer.getY(), ((int) this.mContentContainer.getX()) + width, ((int) this.mContentContainer.getY()) + height);
        }

        private void setTouchableSurfaceInsetsComputer() {
            ViewTreeObserver viewTreeObserver = this.mPopupWindow.getContentView().getRootView().getViewTreeObserver();
            viewTreeObserver.removeOnComputeInternalInsetsListener(this.mInsetsComputer);
            viewTreeObserver.addOnComputeInternalInsetsListener(this.mInsetsComputer);
        }

        private boolean isInRTLMode() {
            return this.mContext.getApplicationInfo().hasRtlSupport() && this.mContext.getResources().getConfiguration().getLayoutDirection() == 1;
        }

        private boolean hasOverflow() {
            return this.mOverflowPanelSize != null;
        }

        public List<MenuItem> layoutMainPanelItems(List<MenuItem> list, int i) {
            Preconditions.checkNotNull(list);
            LinkedList linkedList = new LinkedList();
            LinkedList linkedList2 = new LinkedList();
            for (MenuItem menuItem : list) {
                if (menuItem.getItemId() != 16908353 && menuItem.requiresOverflow()) {
                    linkedList2.add(menuItem);
                } else {
                    linkedList.add(menuItem);
                }
            }
            linkedList.addAll(linkedList2);
            this.mMainPanel.removeAllViews();
            this.mMainPanel.setPaddingRelative(0, 0, 0, 0);
            boolean z = true;
            int i2 = i;
            while (!linkedList.isEmpty()) {
                MenuItem menuItem2 = (MenuItem) linkedList.peek();
                if (!z && menuItem2.requiresOverflow()) {
                    break;
                }
                boolean z2 = z && menuItem2.getItemId() == 16908353;
                View viewCreateMenuItemButton = FloatingToolbar.createMenuItemButton(this.mContext, menuItem2, this.mIconTextSpacing, z2);
                if (!z2 && (viewCreateMenuItemButton instanceof LinearLayout)) {
                    ((LinearLayout) viewCreateMenuItemButton).setGravity(17);
                }
                if (z) {
                    viewCreateMenuItemButton.setPaddingRelative((int) (((double) viewCreateMenuItemButton.getPaddingStart()) * 1.5d), viewCreateMenuItemButton.getPaddingTop(), viewCreateMenuItemButton.getPaddingEnd(), viewCreateMenuItemButton.getPaddingBottom());
                }
                boolean z3 = linkedList.size() == 1;
                if (z3) {
                    viewCreateMenuItemButton.setPaddingRelative(viewCreateMenuItemButton.getPaddingStart(), viewCreateMenuItemButton.getPaddingTop(), (int) (1.5d * ((double) viewCreateMenuItemButton.getPaddingEnd())), viewCreateMenuItemButton.getPaddingBottom());
                }
                viewCreateMenuItemButton.measure(0, 0);
                int iMin = Math.min(viewCreateMenuItemButton.getMeasuredWidth(), i);
                boolean z4 = iMin <= i2 - this.mOverflowButtonSize.getWidth();
                boolean z5 = z3 && iMin <= i2;
                if (!z4 && !z5) {
                    break;
                }
                setButtonTagAndClickListener(viewCreateMenuItemButton, menuItem2);
                viewCreateMenuItemButton.setTooltipText(menuItem2.getTooltipText());
                this.mMainPanel.addView(viewCreateMenuItemButton);
                ViewGroup.LayoutParams layoutParams = viewCreateMenuItemButton.getLayoutParams();
                layoutParams.width = iMin;
                viewCreateMenuItemButton.setLayoutParams(layoutParams);
                i2 -= iMin;
                linkedList.pop();
                menuItem2.getGroupId();
                z = false;
            }
            if (!linkedList.isEmpty()) {
                this.mMainPanel.setPaddingRelative(0, 0, this.mOverflowButtonSize.getWidth(), 0);
            }
            this.mMainPanelSize = measure(this.mMainPanel);
            return linkedList;
        }

        private void layoutOverflowPanelItems(List<MenuItem> list) {
            ArrayAdapter arrayAdapter = (ArrayAdapter) this.mOverflowPanel.getAdapter();
            arrayAdapter.clear();
            int size = list.size();
            for (int i = 0; i < size; i++) {
                arrayAdapter.add(list.get(i));
            }
            this.mOverflowPanel.setAdapter((ListAdapter) arrayAdapter);
            if (this.mOpenOverflowUpwards) {
                this.mOverflowPanel.setY(0.0f);
            } else {
                this.mOverflowPanel.setY(this.mOverflowButtonSize.getHeight());
            }
            this.mOverflowPanelSize = new Size(Math.max(getOverflowWidth(), this.mOverflowButtonSize.getWidth()), calculateOverflowHeight(4));
            setSize(this.mOverflowPanel, this.mOverflowPanelSize);
        }

        private void preparePopupContent() {
            this.mContentContainer.removeAllViews();
            if (hasOverflow()) {
                this.mContentContainer.addView(this.mOverflowPanel);
            }
            this.mContentContainer.addView(this.mMainPanel);
            if (hasOverflow()) {
                this.mContentContainer.addView(this.mOverflowButton);
            }
            setPanelsStatesAtRestingPosition();
            setContentAreaAsTouchableSurface();
            if (isInRTLMode()) {
                this.mContentContainer.setAlpha(0.0f);
                this.mContentContainer.post(this.mPreparePopupContentRTLHelper);
            }
        }

        private void clearPanels() {
            this.mOverflowPanelSize = null;
            this.mMainPanelSize = null;
            this.mIsOverflowOpen = false;
            this.mMainPanel.removeAllViews();
            ArrayAdapter arrayAdapter = (ArrayAdapter) this.mOverflowPanel.getAdapter();
            arrayAdapter.clear();
            this.mOverflowPanel.setAdapter((ListAdapter) arrayAdapter);
            this.mContentContainer.removeAllViews();
        }

        private void positionContentYCoordinatesIfOpeningOverflowUpwards() {
            if (this.mOpenOverflowUpwards) {
                this.mMainPanel.setY(this.mContentContainer.getHeight() - this.mMainPanelSize.getHeight());
                this.mOverflowButton.setY(this.mContentContainer.getHeight() - this.mOverflowButton.getHeight());
                this.mOverflowPanel.setY(this.mContentContainer.getHeight() - this.mOverflowPanelSize.getHeight());
            }
        }

        private int getOverflowWidth() {
            int count = this.mOverflowPanel.getAdapter().getCount();
            int iMax = 0;
            for (int i = 0; i < count; i++) {
                iMax = Math.max(this.mOverflowPanelViewHelper.calculateWidth((MenuItem) this.mOverflowPanel.getAdapter().getItem(i)), iMax);
            }
            return iMax;
        }

        private int calculateOverflowHeight(int i) {
            int i2;
            int iMin = Math.min(4, Math.min(Math.max(2, i), this.mOverflowPanel.getCount()));
            if (iMin < this.mOverflowPanel.getCount()) {
                i2 = (int) (this.mLineHeight * 0.5f);
            } else {
                i2 = 0;
            }
            return (iMin * this.mLineHeight) + this.mOverflowButtonSize.getHeight() + i2;
        }

        private void setButtonTagAndClickListener(View view, MenuItem menuItem) {
            view.setTag(menuItem);
            view.setOnClickListener(this.mMenuItemButtonOnClickListener);
        }

        private int getAdjustedDuration(int i) {
            if (this.mTransitionDurationScale < 150) {
                return Math.max(i - 50, 0);
            }
            if (this.mTransitionDurationScale > 300) {
                return i + 50;
            }
            return (int) (i * ValueAnimator.getDurationScale());
        }

        private void maybeComputeTransitionDurationScale() {
            if (this.mMainPanelSize != null && this.mOverflowPanelSize != null) {
                int width = this.mMainPanelSize.getWidth() - this.mOverflowPanelSize.getWidth();
                int height = this.mOverflowPanelSize.getHeight() - this.mMainPanelSize.getHeight();
                this.mTransitionDurationScale = (int) (Math.sqrt((width * width) + (height * height)) / ((double) this.mContentContainer.getContext().getResources().getDisplayMetrics().density));
            }
        }

        private ViewGroup createMainPanel() {
            return new LinearLayout(this.mContext) {
                @Override
                protected void onMeasure(int i, int i2) {
                    if (FloatingToolbarPopup.this.isOverflowAnimating()) {
                        i = View.MeasureSpec.makeMeasureSpec(FloatingToolbarPopup.this.mMainPanelSize.getWidth(), 1073741824);
                    }
                    super.onMeasure(i, i2);
                }

                @Override
                public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
                    return FloatingToolbarPopup.this.isOverflowAnimating();
                }
            };
        }

        private ImageButton createOverflowButton() {
            final ImageButton imageButton = (ImageButton) LayoutInflater.from(this.mContext).inflate(R.layout.floating_popup_overflow_button, (ViewGroup) null);
            imageButton.setImageDrawable(this.mOverflow);
            imageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public final void onClick(View view) {
                    FloatingToolbar.FloatingToolbarPopup.lambda$createOverflowButton$1(this.f$0, imageButton, view);
                }
            });
            return imageButton;
        }

        public static void lambda$createOverflowButton$1(FloatingToolbarPopup floatingToolbarPopup, ImageButton imageButton, View view) {
            if (floatingToolbarPopup.mIsOverflowOpen) {
                imageButton.setImageDrawable(floatingToolbarPopup.mToOverflow);
                floatingToolbarPopup.mToOverflow.start();
                floatingToolbarPopup.closeOverflow();
            } else {
                imageButton.setImageDrawable(floatingToolbarPopup.mToArrow);
                floatingToolbarPopup.mToArrow.start();
                floatingToolbarPopup.openOverflow();
            }
        }

        private OverflowPanel createOverflowPanel() {
            final OverflowPanel overflowPanel = new OverflowPanel(this);
            overflowPanel.setLayoutParams(new ViewGroup.LayoutParams(-1, -1));
            overflowPanel.setDivider(null);
            overflowPanel.setDividerHeight(0);
            overflowPanel.setAdapter((ListAdapter) new ArrayAdapter<MenuItem>(this.mContext, 0) {
                @Override
                public View getView(int i, View view, ViewGroup viewGroup) {
                    return FloatingToolbarPopup.this.mOverflowPanelViewHelper.getView(getItem(i), FloatingToolbarPopup.this.mOverflowPanelSize.getWidth(), view);
                }
            });
            overflowPanel.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                    FloatingToolbar.FloatingToolbarPopup.lambda$createOverflowPanel$2(this.f$0, overflowPanel, adapterView, view, i, j);
                }
            });
            return overflowPanel;
        }

        public static void lambda$createOverflowPanel$2(FloatingToolbarPopup floatingToolbarPopup, OverflowPanel overflowPanel, AdapterView adapterView, View view, int i, long j) {
            MenuItem menuItem = (MenuItem) overflowPanel.getAdapter().getItem(i);
            if (floatingToolbarPopup.mOnMenuItemClickListener != null) {
                floatingToolbarPopup.mOnMenuItemClickListener.onMenuItemClick(menuItem);
            }
        }

        private boolean isOverflowAnimating() {
            return (this.mOpenOverflowAnimation.hasStarted() && !this.mOpenOverflowAnimation.hasEnded()) || (this.mCloseOverflowAnimation.hasStarted() && !this.mCloseOverflowAnimation.hasEnded());
        }

        class AnonymousClass13 implements Animation.AnimationListener {
            AnonymousClass13() {
            }

            @Override
            public void onAnimationStart(Animation animation) {
                FloatingToolbarPopup.this.mOverflowButton.setEnabled(false);
                FloatingToolbarPopup.this.mMainPanel.setVisibility(0);
                FloatingToolbarPopup.this.mOverflowPanel.setVisibility(0);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                FloatingToolbarPopup.this.mContentContainer.post(new Runnable() {
                    @Override
                    public final void run() {
                        FloatingToolbar.FloatingToolbarPopup.AnonymousClass13.lambda$onAnimationEnd$0(this.f$0);
                    }
                });
            }

            public static void lambda$onAnimationEnd$0(AnonymousClass13 anonymousClass13) {
                FloatingToolbarPopup.this.setPanelsStatesAtRestingPosition();
                FloatingToolbarPopup.this.setContentAreaAsTouchableSurface();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        }

        private Animation.AnimationListener createOverflowAnimationListener() {
            return new AnonymousClass13();
        }

        private static Size measure(View view) {
            Preconditions.checkState(view.getParent() == null);
            view.measure(0, 0);
            return new Size(view.getMeasuredWidth(), view.getMeasuredHeight());
        }

        private static void setSize(View view, int i, int i2) {
            view.setMinimumWidth(i);
            view.setMinimumHeight(i2);
            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new ViewGroup.LayoutParams(0, 0);
            }
            layoutParams.width = i;
            layoutParams.height = i2;
            view.setLayoutParams(layoutParams);
        }

        private static void setSize(View view, Size size) {
            setSize(view, size.getWidth(), size.getHeight());
        }

        private static void setWidth(View view, int i) {
            setSize(view, i, view.getLayoutParams().height);
        }

        private static void setHeight(View view, int i) {
            setSize(view, view.getLayoutParams().width, i);
        }

        private static final class OverflowPanel extends ListView {
            private final FloatingToolbarPopup mPopup;

            OverflowPanel(FloatingToolbarPopup floatingToolbarPopup) {
                super(((FloatingToolbarPopup) Preconditions.checkNotNull(floatingToolbarPopup)).mContext);
                this.mPopup = floatingToolbarPopup;
                setScrollBarDefaultDelayBeforeFade(ViewConfiguration.getScrollDefaultDelay() * 3);
                setScrollIndicators(3);
            }

            @Override
            protected void onMeasure(int i, int i2) {
                super.onMeasure(i, View.MeasureSpec.makeMeasureSpec(this.mPopup.mOverflowPanelSize.getHeight() - this.mPopup.mOverflowButtonSize.getHeight(), 1073741824));
            }

            @Override
            public boolean dispatchTouchEvent(MotionEvent motionEvent) {
                if (this.mPopup.isOverflowAnimating()) {
                    return true;
                }
                return super.dispatchTouchEvent(motionEvent);
            }

            @Override
            protected boolean awakenScrollBars() {
                return super.awakenScrollBars();
            }
        }

        private static final class LogAccelerateInterpolator implements Interpolator {
            private static final int BASE = 100;
            private static final float LOGS_SCALE = 1.0f / computeLog(1.0f, 100);

            private LogAccelerateInterpolator() {
            }

            private static float computeLog(float f, int i) {
                return (float) (1.0d - Math.pow(i, -f));
            }

            @Override
            public float getInterpolation(float f) {
                return 1.0f - (computeLog(1.0f - f, 100) * LOGS_SCALE);
            }
        }

        private static final class OverflowPanelViewHelper {
            private final View mCalculator = createMenuButton(null);
            private final Context mContext;
            private final int mIconTextSpacing;
            private final int mSidePadding;

            public OverflowPanelViewHelper(Context context, int i) {
                this.mContext = (Context) Preconditions.checkNotNull(context);
                this.mIconTextSpacing = i;
                this.mSidePadding = context.getResources().getDimensionPixelSize(R.dimen.floating_toolbar_overflow_side_padding);
            }

            public View getView(MenuItem menuItem, int i, View view) {
                Preconditions.checkNotNull(menuItem);
                if (view != null) {
                    FloatingToolbar.updateMenuItemButton(view, menuItem, this.mIconTextSpacing, shouldShowIcon(menuItem));
                } else {
                    view = createMenuButton(menuItem);
                }
                view.setMinimumWidth(i);
                return view;
            }

            public int calculateWidth(MenuItem menuItem) {
                FloatingToolbar.updateMenuItemButton(this.mCalculator, menuItem, this.mIconTextSpacing, shouldShowIcon(menuItem));
                this.mCalculator.measure(0, 0);
                return this.mCalculator.getMeasuredWidth();
            }

            private View createMenuButton(MenuItem menuItem) {
                View viewCreateMenuItemButton = FloatingToolbar.createMenuItemButton(this.mContext, menuItem, this.mIconTextSpacing, shouldShowIcon(menuItem));
                viewCreateMenuItemButton.setPadding(this.mSidePadding, 0, this.mSidePadding, 0);
                return viewCreateMenuItemButton;
            }

            private boolean shouldShowIcon(MenuItem menuItem) {
                return menuItem != null && menuItem.getGroupId() == 16908353;
            }
        }
    }

    private static View createMenuItemButton(Context context, MenuItem menuItem, int i, boolean z) {
        View viewInflate = LayoutInflater.from(context).inflate(R.layout.floating_popup_menu_button, (ViewGroup) null);
        if (menuItem != null) {
            updateMenuItemButton(viewInflate, menuItem, i, z);
        }
        return viewInflate;
    }

    private static void updateMenuItemButton(View view, MenuItem menuItem, int i, boolean z) {
        TextView textView = (TextView) view.findViewById(R.id.floating_toolbar_menu_item_text);
        textView.setEllipsize(null);
        if (TextUtils.isEmpty(menuItem.getTitle())) {
            textView.setVisibility(8);
        } else {
            textView.setVisibility(0);
            textView.setText(menuItem.getTitle());
        }
        ImageView imageView = (ImageView) view.findViewById(R.id.floating_toolbar_menu_item_image);
        if (menuItem.getIcon() == null || !z) {
            imageView.setVisibility(8);
            if (textView != null) {
                textView.setPaddingRelative(0, 0, 0, 0);
            }
        } else {
            imageView.setVisibility(0);
            imageView.setImageDrawable(menuItem.getIcon());
            if (textView != null) {
                textView.setPaddingRelative(i, 0, 0, 0);
            }
        }
        CharSequence contentDescription = menuItem.getContentDescription();
        if (TextUtils.isEmpty(contentDescription)) {
            view.setContentDescription(menuItem.getTitle());
        } else {
            view.setContentDescription(contentDescription);
        }
    }

    private static ViewGroup createContentContainer(Context context) {
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.floating_popup_container, (ViewGroup) null);
        viewGroup.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
        viewGroup.setTag(FLOATING_TOOLBAR_TAG);
        viewGroup.setClipToOutline(true);
        return viewGroup;
    }

    private static PopupWindow createPopupWindow(ViewGroup viewGroup) {
        LinearLayout linearLayout = new LinearLayout(viewGroup.getContext());
        PopupWindow popupWindow = new PopupWindow(linearLayout);
        popupWindow.setClippingEnabled(false);
        popupWindow.setWindowLayoutType(1005);
        popupWindow.setAnimationStyle(0);
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));
        viewGroup.setLayoutParams(new ViewGroup.LayoutParams(-2, -2));
        linearLayout.addView(viewGroup);
        return popupWindow;
    }

    private static AnimatorSet createEnterAnimation(View view) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(view, View.ALPHA, 0.0f, 1.0f).setDuration(150L));
        return animatorSet;
    }

    private static AnimatorSet createExitAnimation(View view, int i, Animator.AnimatorListener animatorListener) {
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(ObjectAnimator.ofFloat(view, View.ALPHA, 1.0f, 0.0f).setDuration(100L));
        animatorSet.setStartDelay(i);
        animatorSet.addListener(animatorListener);
        return animatorSet;
    }

    private static Context applyDefaultTheme(Context context) {
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(new int[]{R.attr.isLightTheme});
        int i = typedArrayObtainStyledAttributes.getBoolean(0, true) ? 16974123 : 16974120;
        typedArrayObtainStyledAttributes.recycle();
        return new ContextThemeWrapper(context, i);
    }
}
