package android.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.ActionProvider;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ActionMenuView;
import com.android.internal.R;
import com.android.internal.view.ActionBarPolicy;
import com.android.internal.view.menu.ActionMenuItemView;
import com.android.internal.view.menu.BaseMenuPresenter;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.view.menu.MenuItemImpl;
import com.android.internal.view.menu.MenuPopupHelper;
import com.android.internal.view.menu.MenuPresenter;
import com.android.internal.view.menu.MenuView;
import com.android.internal.view.menu.ShowableListMenu;
import com.android.internal.view.menu.SubMenuBuilder;
import java.util.ArrayList;
import java.util.List;

public class ActionMenuPresenter extends BaseMenuPresenter implements ActionProvider.SubUiVisibilityListener {
    private static final boolean ACTIONBAR_ANIMATIONS_ENABLED = false;
    private static final int ITEM_ANIMATION_DURATION = 150;
    private final SparseBooleanArray mActionButtonGroups;
    private ActionButtonSubmenu mActionButtonPopup;
    private int mActionItemWidthLimit;
    private View.OnAttachStateChangeListener mAttachStateChangeListener;
    private boolean mExpandedActionViewsExclusive;
    private ViewTreeObserver.OnPreDrawListener mItemAnimationPreDrawListener;
    private int mMaxItems;
    private boolean mMaxItemsSet;
    private int mMinCellSize;
    int mOpenSubMenuId;
    private OverflowMenuButton mOverflowButton;
    private OverflowPopup mOverflowPopup;
    private Drawable mPendingOverflowIcon;
    private boolean mPendingOverflowIconSet;
    private ActionMenuPopupCallback mPopupCallback;
    final PopupPresenterCallback mPopupPresenterCallback;
    private SparseArray<MenuItemLayoutInfo> mPostLayoutItems;
    private OpenOverflowRunnable mPostedOpenRunnable;
    private SparseArray<MenuItemLayoutInfo> mPreLayoutItems;
    private boolean mReserveOverflow;
    private boolean mReserveOverflowSet;
    private List<ItemAnimationInfo> mRunningItemAnimations;
    private boolean mStrictWidthLimit;
    private int mWidthLimit;
    private boolean mWidthLimitSet;

    public ActionMenuPresenter(Context context) {
        super(context, R.layout.action_menu_layout, R.layout.action_menu_item_layout);
        this.mActionButtonGroups = new SparseBooleanArray();
        this.mPopupPresenterCallback = new PopupPresenterCallback();
        this.mPreLayoutItems = new SparseArray<>();
        this.mPostLayoutItems = new SparseArray<>();
        this.mRunningItemAnimations = new ArrayList();
        this.mItemAnimationPreDrawListener = new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                ActionMenuPresenter.this.computeMenuItemAnimationInfo(false);
                ((View) ActionMenuPresenter.this.mMenuView).getViewTreeObserver().removeOnPreDrawListener(this);
                ActionMenuPresenter.this.runItemAnimations();
                return true;
            }
        };
        this.mAttachStateChangeListener = new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View view) {
            }

            @Override
            public void onViewDetachedFromWindow(View view) {
                ((View) ActionMenuPresenter.this.mMenuView).getViewTreeObserver().removeOnPreDrawListener(ActionMenuPresenter.this.mItemAnimationPreDrawListener);
                ActionMenuPresenter.this.mPreLayoutItems.clear();
                ActionMenuPresenter.this.mPostLayoutItems.clear();
            }
        };
    }

    @Override
    public void initForMenu(Context context, MenuBuilder menuBuilder) {
        super.initForMenu(context, menuBuilder);
        Resources resources = context.getResources();
        ActionBarPolicy actionBarPolicy = ActionBarPolicy.get(context);
        if (!this.mReserveOverflowSet) {
            this.mReserveOverflow = actionBarPolicy.showsOverflowMenuButton();
        }
        if (!this.mWidthLimitSet) {
            this.mWidthLimit = actionBarPolicy.getEmbeddedMenuWidthLimit();
        }
        if (!this.mMaxItemsSet) {
            this.mMaxItems = actionBarPolicy.getMaxActionButtons();
        }
        int measuredWidth = this.mWidthLimit;
        if (this.mReserveOverflow) {
            if (this.mOverflowButton == null) {
                this.mOverflowButton = new OverflowMenuButton(this.mSystemContext);
                if (this.mPendingOverflowIconSet) {
                    this.mOverflowButton.setImageDrawable(this.mPendingOverflowIcon);
                    this.mPendingOverflowIcon = null;
                    this.mPendingOverflowIconSet = false;
                }
                int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
                this.mOverflowButton.measure(iMakeMeasureSpec, iMakeMeasureSpec);
            }
            measuredWidth -= this.mOverflowButton.getMeasuredWidth();
        } else {
            this.mOverflowButton = null;
        }
        this.mActionItemWidthLimit = measuredWidth;
        this.mMinCellSize = (int) (56.0f * resources.getDisplayMetrics().density);
    }

    public void onConfigurationChanged(Configuration configuration) {
        if (!this.mMaxItemsSet) {
            this.mMaxItems = ActionBarPolicy.get(this.mContext).getMaxActionButtons();
        }
        if (this.mMenu != null) {
            this.mMenu.onItemsChanged(true);
        }
    }

    public void setWidthLimit(int i, boolean z) {
        this.mWidthLimit = i;
        this.mStrictWidthLimit = z;
        this.mWidthLimitSet = true;
    }

    public void setReserveOverflow(boolean z) {
        this.mReserveOverflow = z;
        this.mReserveOverflowSet = true;
    }

    public void setItemLimit(int i) {
        this.mMaxItems = i;
        this.mMaxItemsSet = true;
    }

    public void setExpandedActionViewsExclusive(boolean z) {
        this.mExpandedActionViewsExclusive = z;
    }

    public void setOverflowIcon(Drawable drawable) {
        if (this.mOverflowButton != null) {
            this.mOverflowButton.setImageDrawable(drawable);
        } else {
            this.mPendingOverflowIconSet = true;
            this.mPendingOverflowIcon = drawable;
        }
    }

    public Drawable getOverflowIcon() {
        if (this.mOverflowButton != null) {
            return this.mOverflowButton.getDrawable();
        }
        if (this.mPendingOverflowIconSet) {
            return this.mPendingOverflowIcon;
        }
        return null;
    }

    @Override
    public MenuView getMenuView(ViewGroup viewGroup) {
        Object obj = this.mMenuView;
        MenuView menuView = super.getMenuView(viewGroup);
        if (obj != menuView) {
            ((ActionMenuView) menuView).setPresenter(this);
            if (obj != null) {
                ((View) obj).removeOnAttachStateChangeListener(this.mAttachStateChangeListener);
            }
            ((View) menuView).addOnAttachStateChangeListener(this.mAttachStateChangeListener);
        }
        return menuView;
    }

    @Override
    public View getItemView(MenuItemImpl menuItemImpl, View view, ViewGroup viewGroup) {
        View actionView = menuItemImpl.getActionView();
        if (actionView == null || menuItemImpl.hasCollapsibleActionView()) {
            actionView = super.getItemView(menuItemImpl, view, viewGroup);
        }
        actionView.setVisibility(menuItemImpl.isActionViewExpanded() ? 8 : 0);
        ActionMenuView actionMenuView = (ActionMenuView) viewGroup;
        ViewGroup.LayoutParams layoutParams = actionView.getLayoutParams();
        if (!actionMenuView.checkLayoutParams(layoutParams)) {
            actionView.setLayoutParams(actionMenuView.generateLayoutParams(layoutParams));
        }
        return actionView;
    }

    @Override
    public void bindItemView(MenuItemImpl menuItemImpl, MenuView.ItemView itemView) {
        itemView.initialize(menuItemImpl, 0);
        ActionMenuItemView actionMenuItemView = (ActionMenuItemView) itemView;
        actionMenuItemView.setItemInvoker((ActionMenuView) this.mMenuView);
        if (this.mPopupCallback == null) {
            this.mPopupCallback = new ActionMenuPopupCallback();
        }
        actionMenuItemView.setPopupCallback(this.mPopupCallback);
    }

    @Override
    public boolean shouldIncludeItem(int i, MenuItemImpl menuItemImpl) {
        return menuItemImpl.isActionButton();
    }

    private void computeMenuItemAnimationInfo(boolean z) {
        ViewGroup viewGroup = (ViewGroup) this.mMenuView;
        int childCount = viewGroup.getChildCount();
        SparseArray<MenuItemLayoutInfo> sparseArray = z ? this.mPreLayoutItems : this.mPostLayoutItems;
        for (int i = 0; i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            int id = childAt.getId();
            if (id > 0 && childAt.getWidth() != 0 && childAt.getHeight() != 0) {
                sparseArray.put(id, new MenuItemLayoutInfo(childAt, z));
            }
        }
    }

    private void runItemAnimations() {
        PropertyValuesHolder propertyValuesHolderOfFloat;
        ObjectAnimator objectAnimatorOfPropertyValuesHolder;
        for (int i = 0; i < this.mPreLayoutItems.size(); i++) {
            int iKeyAt = this.mPreLayoutItems.keyAt(i);
            final MenuItemLayoutInfo menuItemLayoutInfo = this.mPreLayoutItems.get(iKeyAt);
            int iIndexOfKey = this.mPostLayoutItems.indexOfKey(iKeyAt);
            if (iIndexOfKey >= 0) {
                MenuItemLayoutInfo menuItemLayoutInfoValueAt = this.mPostLayoutItems.valueAt(iIndexOfKey);
                if (menuItemLayoutInfo.left != menuItemLayoutInfoValueAt.left) {
                    propertyValuesHolderOfFloat = PropertyValuesHolder.ofFloat(View.TRANSLATION_X, menuItemLayoutInfo.left - menuItemLayoutInfoValueAt.left, 0.0f);
                } else {
                    propertyValuesHolderOfFloat = null;
                }
                PropertyValuesHolder propertyValuesHolderOfFloat2 = menuItemLayoutInfo.top != menuItemLayoutInfoValueAt.top ? PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, menuItemLayoutInfo.top - menuItemLayoutInfoValueAt.top, 0.0f) : null;
                if (propertyValuesHolderOfFloat != null || propertyValuesHolderOfFloat2 != null) {
                    for (int i2 = 0; i2 < this.mRunningItemAnimations.size(); i2++) {
                        ItemAnimationInfo itemAnimationInfo = this.mRunningItemAnimations.get(i2);
                        if (itemAnimationInfo.id == iKeyAt && itemAnimationInfo.animType == 0) {
                            itemAnimationInfo.animator.cancel();
                        }
                    }
                    if (propertyValuesHolderOfFloat != null) {
                        if (propertyValuesHolderOfFloat2 != null) {
                            objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(menuItemLayoutInfoValueAt.view, propertyValuesHolderOfFloat, propertyValuesHolderOfFloat2);
                        } else {
                            objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(menuItemLayoutInfoValueAt.view, propertyValuesHolderOfFloat);
                        }
                    } else {
                        objectAnimatorOfPropertyValuesHolder = ObjectAnimator.ofPropertyValuesHolder(menuItemLayoutInfoValueAt.view, propertyValuesHolderOfFloat2);
                    }
                    objectAnimatorOfPropertyValuesHolder.setDuration(150L);
                    objectAnimatorOfPropertyValuesHolder.start();
                    this.mRunningItemAnimations.add(new ItemAnimationInfo(iKeyAt, menuItemLayoutInfoValueAt, objectAnimatorOfPropertyValuesHolder, 0));
                    objectAnimatorOfPropertyValuesHolder.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animator) {
                            for (int i3 = 0; i3 < ActionMenuPresenter.this.mRunningItemAnimations.size(); i3++) {
                                if (((ItemAnimationInfo) ActionMenuPresenter.this.mRunningItemAnimations.get(i3)).animator == animator) {
                                    ActionMenuPresenter.this.mRunningItemAnimations.remove(i3);
                                    return;
                                }
                            }
                        }
                    });
                }
                this.mPostLayoutItems.remove(iKeyAt);
            } else {
                float alpha = 1.0f;
                for (int i3 = 0; i3 < this.mRunningItemAnimations.size(); i3++) {
                    ItemAnimationInfo itemAnimationInfo2 = this.mRunningItemAnimations.get(i3);
                    if (itemAnimationInfo2.id == iKeyAt && itemAnimationInfo2.animType == 1) {
                        alpha = itemAnimationInfo2.menuItemLayoutInfo.view.getAlpha();
                        itemAnimationInfo2.animator.cancel();
                    }
                }
                ObjectAnimator objectAnimatorOfFloat = ObjectAnimator.ofFloat(menuItemLayoutInfo.view, View.ALPHA, alpha, 0.0f);
                ((ViewGroup) this.mMenuView).getOverlay().add(menuItemLayoutInfo.view);
                objectAnimatorOfFloat.setDuration(150L);
                objectAnimatorOfFloat.start();
                this.mRunningItemAnimations.add(new ItemAnimationInfo(iKeyAt, menuItemLayoutInfo, objectAnimatorOfFloat, 2));
                objectAnimatorOfFloat.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        int i4 = 0;
                        while (true) {
                            if (i4 >= ActionMenuPresenter.this.mRunningItemAnimations.size()) {
                                break;
                            }
                            if (((ItemAnimationInfo) ActionMenuPresenter.this.mRunningItemAnimations.get(i4)).animator == animator) {
                                ActionMenuPresenter.this.mRunningItemAnimations.remove(i4);
                                break;
                            }
                            i4++;
                        }
                        ((ViewGroup) ActionMenuPresenter.this.mMenuView).getOverlay().remove(menuItemLayoutInfo.view);
                    }
                });
            }
        }
        for (int i4 = 0; i4 < this.mPostLayoutItems.size(); i4++) {
            int iKeyAt2 = this.mPostLayoutItems.keyAt(i4);
            int iIndexOfKey2 = this.mPostLayoutItems.indexOfKey(iKeyAt2);
            if (iIndexOfKey2 >= 0) {
                MenuItemLayoutInfo menuItemLayoutInfoValueAt2 = this.mPostLayoutItems.valueAt(iIndexOfKey2);
                float alpha2 = 0.0f;
                for (int i5 = 0; i5 < this.mRunningItemAnimations.size(); i5++) {
                    ItemAnimationInfo itemAnimationInfo3 = this.mRunningItemAnimations.get(i5);
                    if (itemAnimationInfo3.id == iKeyAt2 && itemAnimationInfo3.animType == 2) {
                        alpha2 = itemAnimationInfo3.menuItemLayoutInfo.view.getAlpha();
                        itemAnimationInfo3.animator.cancel();
                    }
                }
                ObjectAnimator objectAnimatorOfFloat2 = ObjectAnimator.ofFloat(menuItemLayoutInfoValueAt2.view, View.ALPHA, alpha2, 1.0f);
                objectAnimatorOfFloat2.start();
                objectAnimatorOfFloat2.setDuration(150L);
                this.mRunningItemAnimations.add(new ItemAnimationInfo(iKeyAt2, menuItemLayoutInfoValueAt2, objectAnimatorOfFloat2, 1));
                objectAnimatorOfFloat2.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animator) {
                        for (int i6 = 0; i6 < ActionMenuPresenter.this.mRunningItemAnimations.size(); i6++) {
                            if (((ItemAnimationInfo) ActionMenuPresenter.this.mRunningItemAnimations.get(i6)).animator == animator) {
                                ActionMenuPresenter.this.mRunningItemAnimations.remove(i6);
                                return;
                            }
                        }
                    }
                });
            }
        }
        this.mPreLayoutItems.clear();
        this.mPostLayoutItems.clear();
    }

    private void setupItemAnimations() {
        computeMenuItemAnimationInfo(true);
        ((View) this.mMenuView).getViewTreeObserver().addOnPreDrawListener(this.mItemAnimationPreDrawListener);
    }

    @Override
    public void updateMenuView(boolean z) {
        super.updateMenuView(z);
        ((View) this.mMenuView).requestLayout();
        boolean z2 = false;
        if (this.mMenu != null) {
            ArrayList<MenuItemImpl> actionItems = this.mMenu.getActionItems();
            int size = actionItems.size();
            for (int i = 0; i < size; i++) {
                ActionProvider actionProvider = actionItems.get(i).getActionProvider();
                if (actionProvider != null) {
                    actionProvider.setSubUiVisibilityListener(this);
                }
            }
        }
        ArrayList<MenuItemImpl> nonActionItems = this.mMenu != null ? this.mMenu.getNonActionItems() : null;
        if (this.mReserveOverflow && nonActionItems != null) {
            int size2 = nonActionItems.size();
            if (size2 == 1) {
                z2 = !nonActionItems.get(0).isActionViewExpanded();
            } else if (size2 > 0) {
                z2 = true;
            }
        }
        if (z2) {
            if (this.mOverflowButton == null) {
                this.mOverflowButton = new OverflowMenuButton(this.mSystemContext);
            }
            ViewGroup viewGroup = (ViewGroup) this.mOverflowButton.getParent();
            if (viewGroup != this.mMenuView) {
                if (viewGroup != null) {
                    viewGroup.removeView(this.mOverflowButton);
                }
                ActionMenuView actionMenuView = (ActionMenuView) this.mMenuView;
                actionMenuView.addView(this.mOverflowButton, actionMenuView.generateOverflowButtonLayoutParams());
            }
        } else if (this.mOverflowButton != null && this.mOverflowButton.getParent() == this.mMenuView) {
            ((ViewGroup) this.mMenuView).removeView(this.mOverflowButton);
        }
        ((ActionMenuView) this.mMenuView).setOverflowReserved(this.mReserveOverflow);
    }

    @Override
    public boolean filterLeftoverView(ViewGroup viewGroup, int i) {
        if (viewGroup.getChildAt(i) == this.mOverflowButton) {
            return false;
        }
        return super.filterLeftoverView(viewGroup, i);
    }

    @Override
    public boolean onSubMenuSelected(SubMenuBuilder subMenuBuilder) {
        boolean z = false;
        if (!subMenuBuilder.hasVisibleItems()) {
            return false;
        }
        SubMenuBuilder subMenuBuilder2 = subMenuBuilder;
        while (subMenuBuilder2.getParentMenu() != this.mMenu) {
            subMenuBuilder2 = (SubMenuBuilder) subMenuBuilder2.getParentMenu();
        }
        View viewFindViewForItem = findViewForItem(subMenuBuilder2.getItem());
        if (viewFindViewForItem == null) {
            return false;
        }
        this.mOpenSubMenuId = subMenuBuilder.getItem().getItemId();
        int size = subMenuBuilder.size();
        int i = 0;
        while (true) {
            if (i >= size) {
                break;
            }
            MenuItem item = subMenuBuilder.getItem(i);
            if (!item.isVisible() || item.getIcon() == null) {
                i++;
            } else {
                z = true;
                break;
            }
        }
        this.mActionButtonPopup = new ActionButtonSubmenu(this.mContext, subMenuBuilder, viewFindViewForItem);
        this.mActionButtonPopup.setForceShowIcon(z);
        this.mActionButtonPopup.show();
        super.onSubMenuSelected(subMenuBuilder);
        return true;
    }

    private View findViewForItem(MenuItem menuItem) {
        ViewGroup viewGroup = (ViewGroup) this.mMenuView;
        if (viewGroup == null) {
            return null;
        }
        int childCount = viewGroup.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View childAt = viewGroup.getChildAt(i);
            if ((childAt instanceof MenuView.ItemView) && ((MenuView.ItemView) childAt).getItemData() == menuItem) {
                return childAt;
            }
        }
        return null;
    }

    public boolean showOverflowMenu() {
        if (this.mReserveOverflow && !isOverflowMenuShowing() && this.mMenu != null && this.mMenuView != null && this.mPostedOpenRunnable == null && !this.mMenu.getNonActionItems().isEmpty()) {
            this.mPostedOpenRunnable = new OpenOverflowRunnable(new OverflowPopup(this.mContext, this.mMenu, this.mOverflowButton, true));
            ((View) this.mMenuView).post(this.mPostedOpenRunnable);
            super.onSubMenuSelected(null);
            return true;
        }
        return false;
    }

    public boolean hideOverflowMenu() {
        if (this.mPostedOpenRunnable != null && this.mMenuView != null) {
            ((View) this.mMenuView).removeCallbacks(this.mPostedOpenRunnable);
            this.mPostedOpenRunnable = null;
            return true;
        }
        OverflowPopup overflowPopup = this.mOverflowPopup;
        if (overflowPopup != null) {
            overflowPopup.dismiss();
            return true;
        }
        return false;
    }

    public boolean dismissPopupMenus() {
        return hideOverflowMenu() | hideSubMenus();
    }

    public boolean hideSubMenus() {
        if (this.mActionButtonPopup != null) {
            this.mActionButtonPopup.dismiss();
            return true;
        }
        return false;
    }

    public boolean isOverflowMenuShowing() {
        return this.mOverflowPopup != null && this.mOverflowPopup.isShowing();
    }

    public boolean isOverflowMenuShowPending() {
        return this.mPostedOpenRunnable != null || isOverflowMenuShowing();
    }

    public boolean isOverflowReserved() {
        return this.mReserveOverflow;
    }

    @Override
    public boolean flagActionItems() {
        ArrayList<MenuItemImpl> visibleItems;
        int size;
        int iMeasureChildForCells;
        int i;
        int i2;
        int i3;
        boolean z;
        boolean z2;
        ActionMenuPresenter actionMenuPresenter = this;
        View view = null;
        int i4 = 0;
        if (actionMenuPresenter.mMenu != null) {
            visibleItems = actionMenuPresenter.mMenu.getVisibleItems();
            size = visibleItems.size();
        } else {
            visibleItems = null;
            size = 0;
        }
        int i5 = actionMenuPresenter.mMaxItems;
        int i6 = actionMenuPresenter.mActionItemWidthLimit;
        int iMakeMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, 0);
        ViewGroup viewGroup = (ViewGroup) actionMenuPresenter.mMenuView;
        int i7 = 0;
        boolean z3 = false;
        int i8 = 0;
        int i9 = i5;
        for (int i10 = 0; i10 < size; i10++) {
            MenuItemImpl menuItemImpl = visibleItems.get(i10);
            if (menuItemImpl.requiresActionButton()) {
                i7++;
            } else if (menuItemImpl.requestsActionButton()) {
                i8++;
            } else {
                z3 = true;
            }
            if (actionMenuPresenter.mExpandedActionViewsExclusive && menuItemImpl.isActionViewExpanded()) {
                i9 = 0;
            }
        }
        if (actionMenuPresenter.mReserveOverflow && (z3 || i8 + i7 > i9)) {
            i9--;
        }
        int i11 = i9 - i7;
        SparseBooleanArray sparseBooleanArray = actionMenuPresenter.mActionButtonGroups;
        sparseBooleanArray.clear();
        if (actionMenuPresenter.mStrictWidthLimit) {
            iMeasureChildForCells = i6 / actionMenuPresenter.mMinCellSize;
            i = ((i6 % actionMenuPresenter.mMinCellSize) / iMeasureChildForCells) + actionMenuPresenter.mMinCellSize;
        } else {
            iMeasureChildForCells = 0;
            i = 0;
        }
        int i12 = 0;
        int i13 = i6;
        int i14 = 0;
        while (i14 < size) {
            MenuItemImpl menuItemImpl2 = visibleItems.get(i14);
            if (menuItemImpl2.requiresActionButton()) {
                View itemView = actionMenuPresenter.getItemView(menuItemImpl2, view, viewGroup);
                if (actionMenuPresenter.mStrictWidthLimit) {
                    iMeasureChildForCells -= ActionMenuView.measureChildForCells(itemView, i, iMeasureChildForCells, iMakeMeasureSpec, i4);
                } else {
                    itemView.measure(iMakeMeasureSpec, iMakeMeasureSpec);
                }
                int measuredWidth = itemView.getMeasuredWidth();
                i13 -= measuredWidth;
                if (i12 == 0) {
                    i12 = measuredWidth;
                }
                int groupId = menuItemImpl2.getGroupId();
                if (groupId != 0) {
                    z2 = true;
                    sparseBooleanArray.put(groupId, true);
                } else {
                    z2 = true;
                }
                menuItemImpl2.setIsActionButton(z2);
                i3 = i4;
                i2 = size;
            } else if (menuItemImpl2.requestsActionButton()) {
                int groupId2 = menuItemImpl2.getGroupId();
                boolean z4 = sparseBooleanArray.get(groupId2);
                boolean z5 = (i11 > 0 || z4) && i13 > 0 && (!actionMenuPresenter.mStrictWidthLimit || iMeasureChildForCells > 0);
                if (z5) {
                    boolean z6 = z5;
                    i2 = size;
                    View itemView2 = actionMenuPresenter.getItemView(menuItemImpl2, null, viewGroup);
                    if (actionMenuPresenter.mStrictWidthLimit) {
                        int iMeasureChildForCells2 = ActionMenuView.measureChildForCells(itemView2, i, iMeasureChildForCells, iMakeMeasureSpec, 0);
                        iMeasureChildForCells -= iMeasureChildForCells2;
                        z = iMeasureChildForCells2 == 0 ? false : z6;
                    } else {
                        itemView2.measure(iMakeMeasureSpec, iMakeMeasureSpec);
                        z = z6;
                    }
                    int measuredWidth2 = itemView2.getMeasuredWidth();
                    i13 -= measuredWidth2;
                    if (i12 == 0) {
                        i12 = measuredWidth2;
                    }
                    z5 = actionMenuPresenter.mStrictWidthLimit ? z & (i13 >= 0) : z & (i13 + i12 > 0);
                } else {
                    i2 = size;
                }
                if (z5 && groupId2 != 0) {
                    sparseBooleanArray.put(groupId2, true);
                } else if (z4) {
                    sparseBooleanArray.put(groupId2, false);
                    for (int i15 = 0; i15 < i14; i15++) {
                        MenuItemImpl menuItemImpl3 = visibleItems.get(i15);
                        if (menuItemImpl3.getGroupId() == groupId2) {
                            if (menuItemImpl3.isActionButton()) {
                                i11++;
                            }
                            menuItemImpl3.setIsActionButton(false);
                        }
                    }
                }
                if (z5) {
                    i11--;
                }
                menuItemImpl2.setIsActionButton(z5);
                i3 = 0;
            } else {
                i2 = size;
                i3 = 0;
                menuItemImpl2.setIsActionButton(false);
            }
            i14++;
            i4 = i3;
            size = i2;
            actionMenuPresenter = this;
            view = null;
        }
        return true;
    }

    @Override
    public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
        dismissPopupMenus();
        super.onCloseMenu(menuBuilder, z);
    }

    @Override
    public Parcelable onSaveInstanceState() {
        SavedState savedState = new SavedState();
        savedState.openSubMenuId = this.mOpenSubMenuId;
        return savedState;
    }

    @Override
    public void onRestoreInstanceState(Parcelable parcelable) {
        MenuItem menuItemFindItem;
        SavedState savedState = (SavedState) parcelable;
        if (savedState.openSubMenuId > 0 && (menuItemFindItem = this.mMenu.findItem(savedState.openSubMenuId)) != null) {
            onSubMenuSelected((SubMenuBuilder) menuItemFindItem.getSubMenu());
        }
    }

    @Override
    public void onSubUiVisibilityChanged(boolean z) {
        if (z) {
            super.onSubMenuSelected(null);
        } else if (this.mMenu != null) {
            this.mMenu.close(false);
        }
    }

    public void setMenuView(ActionMenuView actionMenuView) {
        if (actionMenuView != this.mMenuView) {
            if (this.mMenuView != null) {
                ((View) this.mMenuView).removeOnAttachStateChangeListener(this.mAttachStateChangeListener);
            }
            this.mMenuView = actionMenuView;
            actionMenuView.addOnAttachStateChangeListener(this.mAttachStateChangeListener);
        }
        actionMenuView.initialize(this.mMenu);
    }

    private static class SavedState implements Parcelable {
        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel parcel) {
                return new SavedState(parcel);
            }

            @Override
            public SavedState[] newArray(int i) {
                return new SavedState[i];
            }
        };
        public int openSubMenuId;

        SavedState() {
        }

        SavedState(Parcel parcel) {
            this.openSubMenuId = parcel.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(this.openSubMenuId);
        }
    }

    private class OverflowMenuButton extends ImageButton implements ActionMenuView.ActionMenuChildView {
        public OverflowMenuButton(Context context) {
            super(context, null, 16843510);
            setClickable(true);
            setFocusable(true);
            setVisibility(0);
            setEnabled(true);
            setOnTouchListener(new ForwardingListener(this) {
                @Override
                public ShowableListMenu getPopup() {
                    if (ActionMenuPresenter.this.mOverflowPopup != null) {
                        return ActionMenuPresenter.this.mOverflowPopup.getPopup();
                    }
                    return null;
                }

                @Override
                public boolean onForwardingStarted() {
                    ActionMenuPresenter.this.showOverflowMenu();
                    return true;
                }

                @Override
                public boolean onForwardingStopped() {
                    if (ActionMenuPresenter.this.mPostedOpenRunnable != null) {
                        return false;
                    }
                    ActionMenuPresenter.this.hideOverflowMenu();
                    return true;
                }
            });
        }

        @Override
        public boolean performClick() {
            if (super.performClick()) {
                return true;
            }
            playSoundEffect(0);
            ActionMenuPresenter.this.showOverflowMenu();
            return true;
        }

        @Override
        public boolean needsDividerBefore() {
            return false;
        }

        @Override
        public boolean needsDividerAfter() {
            return false;
        }

        @Override
        public void onInitializeAccessibilityNodeInfoInternal(AccessibilityNodeInfo accessibilityNodeInfo) {
            super.onInitializeAccessibilityNodeInfoInternal(accessibilityNodeInfo);
            accessibilityNodeInfo.setCanOpenPopup(true);
        }

        @Override
        protected boolean setFrame(int i, int i2, int i3, int i4) {
            boolean frame = super.setFrame(i, i2, i3, i4);
            Drawable drawable = getDrawable();
            Drawable background = getBackground();
            if (drawable != null && background != null) {
                int width = getWidth();
                int height = getHeight();
                int iMax = Math.max(width, height) / 2;
                int paddingLeft = (width + (getPaddingLeft() - getPaddingRight())) / 2;
                int paddingTop = (height + (getPaddingTop() - getPaddingBottom())) / 2;
                background.setHotspotBounds(paddingLeft - iMax, paddingTop - iMax, paddingLeft + iMax, paddingTop + iMax);
            }
            return frame;
        }
    }

    private class OverflowPopup extends MenuPopupHelper {
        public OverflowPopup(Context context, MenuBuilder menuBuilder, View view, boolean z) {
            super(context, menuBuilder, view, z, 16843844);
            setGravity(Gravity.END);
            setPresenterCallback(ActionMenuPresenter.this.mPopupPresenterCallback);
        }

        @Override
        protected void onDismiss() {
            if (ActionMenuPresenter.this.mMenu != null) {
                ActionMenuPresenter.this.mMenu.close();
            }
            ActionMenuPresenter.this.mOverflowPopup = null;
            super.onDismiss();
        }
    }

    private class ActionButtonSubmenu extends MenuPopupHelper {
        public ActionButtonSubmenu(Context context, SubMenuBuilder subMenuBuilder, View view) {
            super(context, subMenuBuilder, view, false, 16843844);
            if (!((MenuItemImpl) subMenuBuilder.getItem()).isActionButton()) {
                setAnchorView(ActionMenuPresenter.this.mOverflowButton == null ? (View) ActionMenuPresenter.this.mMenuView : ActionMenuPresenter.this.mOverflowButton);
            }
            setPresenterCallback(ActionMenuPresenter.this.mPopupPresenterCallback);
        }

        @Override
        protected void onDismiss() {
            ActionMenuPresenter.this.mActionButtonPopup = null;
            ActionMenuPresenter.this.mOpenSubMenuId = 0;
            super.onDismiss();
        }
    }

    private class PopupPresenterCallback implements MenuPresenter.Callback {
        private PopupPresenterCallback() {
        }

        @Override
        public boolean onOpenSubMenu(MenuBuilder menuBuilder) {
            if (menuBuilder == null) {
                return false;
            }
            ActionMenuPresenter.this.mOpenSubMenuId = ((SubMenuBuilder) menuBuilder).getItem().getItemId();
            MenuPresenter.Callback callback = ActionMenuPresenter.this.getCallback();
            if (callback != null) {
                return callback.onOpenSubMenu(menuBuilder);
            }
            return false;
        }

        @Override
        public void onCloseMenu(MenuBuilder menuBuilder, boolean z) {
            if (menuBuilder instanceof SubMenuBuilder) {
                menuBuilder.getRootMenu().close(false);
            }
            MenuPresenter.Callback callback = ActionMenuPresenter.this.getCallback();
            if (callback != null) {
                callback.onCloseMenu(menuBuilder, z);
            }
        }
    }

    private class OpenOverflowRunnable implements Runnable {
        private OverflowPopup mPopup;

        public OpenOverflowRunnable(OverflowPopup overflowPopup) {
            this.mPopup = overflowPopup;
        }

        @Override
        public void run() {
            if (ActionMenuPresenter.this.mMenu != null) {
                ActionMenuPresenter.this.mMenu.changeMenuMode();
            }
            View view = (View) ActionMenuPresenter.this.mMenuView;
            if (view != null && view.getWindowToken() != null && this.mPopup.tryShow()) {
                ActionMenuPresenter.this.mOverflowPopup = this.mPopup;
            }
            ActionMenuPresenter.this.mPostedOpenRunnable = null;
        }
    }

    private class ActionMenuPopupCallback extends ActionMenuItemView.PopupCallback {
        private ActionMenuPopupCallback() {
        }

        @Override
        public ShowableListMenu getPopup() {
            if (ActionMenuPresenter.this.mActionButtonPopup != null) {
                return ActionMenuPresenter.this.mActionButtonPopup.getPopup();
            }
            return null;
        }
    }

    private static class MenuItemLayoutInfo {
        int left;
        int top;
        View view;

        MenuItemLayoutInfo(View view, boolean z) {
            this.left = view.getLeft();
            this.top = view.getTop();
            if (z) {
                this.left = (int) (this.left + view.getTranslationX());
                this.top = (int) (this.top + view.getTranslationY());
            }
            this.view = view;
        }
    }

    private static class ItemAnimationInfo {
        static final int FADE_IN = 1;
        static final int FADE_OUT = 2;
        static final int MOVE = 0;
        int animType;
        Animator animator;
        int id;
        MenuItemLayoutInfo menuItemLayoutInfo;

        ItemAnimationInfo(int i, MenuItemLayoutInfo menuItemLayoutInfo, Animator animator, int i2) {
            this.id = i;
            this.menuItemLayoutInfo = menuItemLayoutInfo;
            this.animator = animator;
            this.animType = i2;
        }
    }
}
