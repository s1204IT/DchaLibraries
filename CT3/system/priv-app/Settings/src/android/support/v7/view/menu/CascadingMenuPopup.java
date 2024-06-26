package android.support.v7.view.menu;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R$dimen;
import android.support.v7.appcompat.R$layout;
import android.support.v7.view.menu.MenuPresenter;
import android.support.v7.widget.MenuItemHoverListener;
import android.support.v7.widget.MenuPopupWindow;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
/* loaded from: classes.dex */
final class CascadingMenuPopup extends MenuPopup implements MenuPresenter, View.OnKeyListener, PopupWindow.OnDismissListener {
    private View mAnchorView;
    private final Context mContext;
    private boolean mHasXOffset;
    private boolean mHasYOffset;
    private final int mMenuMaxWidth;
    private PopupWindow.OnDismissListener mOnDismissListener;
    private final boolean mOverflowOnly;
    private final int mPopupStyleAttr;
    private final int mPopupStyleRes;
    private MenuPresenter.Callback mPresenterCallback;
    private boolean mShouldCloseImmediately;
    private boolean mShowTitle;
    private View mShownAnchorView;
    private final Handler mSubMenuHoverHandler;
    private ViewTreeObserver mTreeObserver;
    private int mXOffset;
    private int mYOffset;
    private final List<MenuBuilder> mPendingMenus = new LinkedList();
    private final List<CascadingMenuInfo> mShowingMenus = new ArrayList();
    private final ViewTreeObserver.OnGlobalLayoutListener mGlobalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() { // from class: android.support.v7.view.menu.CascadingMenuPopup.1
        @Override // android.view.ViewTreeObserver.OnGlobalLayoutListener
        public void onGlobalLayout() {
            if (!CascadingMenuPopup.this.isShowing() || CascadingMenuPopup.this.mShowingMenus.size() <= 0 || ((CascadingMenuInfo) CascadingMenuPopup.this.mShowingMenus.get(0)).window.isModal()) {
                return;
            }
            View anchor = CascadingMenuPopup.this.mShownAnchorView;
            if (anchor == null || !anchor.isShown()) {
                CascadingMenuPopup.this.dismiss();
                return;
            }
            for (CascadingMenuInfo info : CascadingMenuPopup.this.mShowingMenus) {
                info.window.show();
            }
        }
    };
    private final MenuItemHoverListener mMenuItemHoverListener = new MenuItemHoverListener() { // from class: android.support.v7.view.menu.CascadingMenuPopup.2
        @Override // android.support.v7.widget.MenuItemHoverListener
        public void onItemHoverExit(@NonNull MenuBuilder menu, @NonNull MenuItem item) {
            CascadingMenuPopup.this.mSubMenuHoverHandler.removeCallbacksAndMessages(menu);
        }

        @Override // android.support.v7.widget.MenuItemHoverListener
        public void onItemHoverEnter(@NonNull final MenuBuilder menu, @NonNull final MenuItem item) {
            final CascadingMenuInfo cascadingMenuInfo;
            CascadingMenuPopup.this.mSubMenuHoverHandler.removeCallbacksAndMessages(null);
            int menuIndex = -1;
            int i = 0;
            int count = CascadingMenuPopup.this.mShowingMenus.size();
            while (true) {
                if (i >= count) {
                    break;
                } else if (menu != ((CascadingMenuInfo) CascadingMenuPopup.this.mShowingMenus.get(i)).menu) {
                    i++;
                } else {
                    menuIndex = i;
                    break;
                }
            }
            if (menuIndex == -1) {
                return;
            }
            int nextIndex = menuIndex + 1;
            if (nextIndex < CascadingMenuPopup.this.mShowingMenus.size()) {
                cascadingMenuInfo = (CascadingMenuInfo) CascadingMenuPopup.this.mShowingMenus.get(nextIndex);
            } else {
                cascadingMenuInfo = null;
            }
            Runnable runnable = new Runnable() { // from class: android.support.v7.view.menu.CascadingMenuPopup.2.1
                @Override // java.lang.Runnable
                public void run() {
                    if (cascadingMenuInfo != null) {
                        CascadingMenuPopup.this.mShouldCloseImmediately = true;
                        cascadingMenuInfo.menu.close(false);
                        CascadingMenuPopup.this.mShouldCloseImmediately = false;
                    }
                    if (!item.isEnabled() || !item.hasSubMenu()) {
                        return;
                    }
                    menu.performItemAction(item, 0);
                }
            };
            long uptimeMillis = SystemClock.uptimeMillis() + 200;
            CascadingMenuPopup.this.mSubMenuHoverHandler.postAtTime(runnable, menu, uptimeMillis);
        }
    };
    private int mRawDropDownGravity = 0;
    private int mDropDownGravity = 0;
    private boolean mForceShowIcon = false;
    private int mLastPosition = getInitialMenuPosition();

    @Retention(RetentionPolicy.SOURCE)
    /* loaded from: classes.dex */
    public @interface HorizPosition {
    }

    public CascadingMenuPopup(@NonNull Context context, @NonNull View anchor, @AttrRes int popupStyleAttr, @StyleRes int popupStyleRes, boolean overflowOnly) {
        this.mContext = context;
        this.mAnchorView = anchor;
        this.mPopupStyleAttr = popupStyleAttr;
        this.mPopupStyleRes = popupStyleRes;
        this.mOverflowOnly = overflowOnly;
        Resources res = context.getResources();
        this.mMenuMaxWidth = Math.max(res.getDisplayMetrics().widthPixels / 2, res.getDimensionPixelSize(R$dimen.abc_config_prefDialogWidth));
        this.mSubMenuHoverHandler = new Handler();
    }

    @Override // android.support.v7.view.menu.MenuPopup
    public void setForceShowIcon(boolean forceShow) {
        this.mForceShowIcon = forceShow;
    }

    private MenuPopupWindow createPopupWindow() {
        MenuPopupWindow popupWindow = new MenuPopupWindow(this.mContext, null, this.mPopupStyleAttr, this.mPopupStyleRes);
        popupWindow.setHoverListener(this.mMenuItemHoverListener);
        popupWindow.setOnItemClickListener(this);
        popupWindow.setOnDismissListener(this);
        popupWindow.setAnchorView(this.mAnchorView);
        popupWindow.setDropDownGravity(this.mDropDownGravity);
        popupWindow.setModal(true);
        return popupWindow;
    }

    @Override // android.support.v7.view.menu.ShowableListMenu
    public void show() {
        if (isShowing()) {
            return;
        }
        for (MenuBuilder menu : this.mPendingMenus) {
            showMenu(menu);
        }
        this.mPendingMenus.clear();
        this.mShownAnchorView = this.mAnchorView;
        if (this.mShownAnchorView == null) {
            return;
        }
        boolean addGlobalListener = this.mTreeObserver == null;
        this.mTreeObserver = this.mShownAnchorView.getViewTreeObserver();
        if (!addGlobalListener) {
            return;
        }
        this.mTreeObserver.addOnGlobalLayoutListener(this.mGlobalLayoutListener);
    }

    @Override // android.support.v7.view.menu.ShowableListMenu
    public void dismiss() {
        int length = this.mShowingMenus.size();
        if (length <= 0) {
            return;
        }
        CascadingMenuInfo[] addedMenus = (CascadingMenuInfo[]) this.mShowingMenus.toArray(new CascadingMenuInfo[length]);
        for (int i = length - 1; i >= 0; i--) {
            CascadingMenuInfo info = addedMenus[i];
            if (info.window.isShowing()) {
                info.window.dismiss();
            }
        }
    }

    @Override // android.view.View.OnKeyListener
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == 1 && keyCode == 82) {
            dismiss();
            return true;
        }
        return false;
    }

    private int getInitialMenuPosition() {
        int layoutDirection = ViewCompat.getLayoutDirection(this.mAnchorView);
        return layoutDirection == 1 ? 0 : 1;
    }

    private int getNextMenuPosition(int nextMenuWidth) {
        ListView lastListView = this.mShowingMenus.get(this.mShowingMenus.size() - 1).getListView();
        int[] screenLocation = new int[2];
        lastListView.getLocationOnScreen(screenLocation);
        Rect displayFrame = new Rect();
        this.mShownAnchorView.getWindowVisibleDisplayFrame(displayFrame);
        if (this.mLastPosition == 1) {
            int right = screenLocation[0] + lastListView.getWidth() + nextMenuWidth;
            return right > displayFrame.right ? 0 : 1;
        }
        int left = screenLocation[0] - nextMenuWidth;
        return left < 0 ? 1 : 0;
    }

    @Override // android.support.v7.view.menu.MenuPopup
    public void addMenu(MenuBuilder menu) {
        menu.addMenuPresenter(this, this.mContext);
        if (isShowing()) {
            showMenu(menu);
        } else {
            this.mPendingMenus.add(menu);
        }
    }

    private void showMenu(@NonNull MenuBuilder menu) {
        CascadingMenuInfo parentInfo;
        View view;
        int x;
        LayoutInflater inflater = LayoutInflater.from(this.mContext);
        MenuAdapter adapter = new MenuAdapter(menu, inflater, this.mOverflowOnly);
        if (!isShowing() && this.mForceShowIcon) {
            adapter.setForceShowIcon(true);
        } else if (isShowing()) {
            adapter.setForceShowIcon(MenuPopup.shouldPreserveIconSpacing(menu));
        }
        int menuWidth = measureIndividualMenuWidth(adapter, null, this.mContext, this.mMenuMaxWidth);
        MenuPopupWindow popupWindow = createPopupWindow();
        popupWindow.setAdapter(adapter);
        popupWindow.setWidth(menuWidth);
        popupWindow.setDropDownGravity(this.mDropDownGravity);
        if (this.mShowingMenus.size() > 0) {
            parentInfo = this.mShowingMenus.get(this.mShowingMenus.size() - 1);
            view = findParentViewForSubmenu(parentInfo, menu);
        } else {
            parentInfo = null;
            view = null;
        }
        if (view != null) {
            popupWindow.setTouchModal(false);
            popupWindow.setEnterTransition(null);
            int nextMenuPosition = getNextMenuPosition(menuWidth);
            boolean showOnRight = nextMenuPosition == 1;
            this.mLastPosition = nextMenuPosition;
            int[] tempLocation = new int[2];
            view.getLocationInWindow(tempLocation);
            int parentOffsetLeft = parentInfo.window.getHorizontalOffset() + tempLocation[0];
            int parentOffsetTop = parentInfo.window.getVerticalOffset() + tempLocation[1];
            if ((this.mDropDownGravity & 5) == 5) {
                if (showOnRight) {
                    x = parentOffsetLeft + menuWidth;
                } else {
                    x = parentOffsetLeft - view.getWidth();
                }
            } else if (showOnRight) {
                x = parentOffsetLeft + view.getWidth();
            } else {
                x = parentOffsetLeft - menuWidth;
            }
            popupWindow.setHorizontalOffset(x);
            popupWindow.setVerticalOffset(parentOffsetTop);
        } else {
            if (this.mHasXOffset) {
                popupWindow.setHorizontalOffset(this.mXOffset);
            }
            if (this.mHasYOffset) {
                popupWindow.setVerticalOffset(this.mYOffset);
            }
            Rect epicenterBounds = getEpicenterBounds();
            popupWindow.setEpicenterBounds(epicenterBounds);
        }
        CascadingMenuInfo menuInfo = new CascadingMenuInfo(popupWindow, menu, this.mLastPosition);
        this.mShowingMenus.add(menuInfo);
        popupWindow.show();
        if (parentInfo != null || !this.mShowTitle || menu.getHeaderTitle() == null) {
            return;
        }
        ListView listView = popupWindow.getListView();
        FrameLayout titleItemView = (FrameLayout) inflater.inflate(R$layout.abc_popup_menu_header_item_layout, (ViewGroup) listView, false);
        TextView titleView = (TextView) titleItemView.findViewById(16908310);
        titleItemView.setEnabled(false);
        titleView.setText(menu.getHeaderTitle());
        listView.addHeaderView(titleItemView, null, false);
        popupWindow.show();
    }

    private MenuItem findMenuItemForSubmenu(@NonNull MenuBuilder parent, @NonNull MenuBuilder submenu) {
        int count = parent.size();
        for (int i = 0; i < count; i++) {
            MenuItem item = parent.getItem(i);
            if (item.hasSubMenu() && submenu == item.getSubMenu()) {
                return item;
            }
        }
        return null;
    }

    @Nullable
    private View findParentViewForSubmenu(@NonNull CascadingMenuInfo parentInfo, @NonNull MenuBuilder submenu) {
        int headersCount;
        MenuAdapter menuAdapter;
        int ownerViewPosition;
        MenuItem owner = findMenuItemForSubmenu(parentInfo.menu, submenu);
        if (owner == null) {
            return null;
        }
        ListView listView = parentInfo.getListView();
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter instanceof HeaderViewListAdapter) {
            HeaderViewListAdapter headerAdapter = (HeaderViewListAdapter) listAdapter;
            headersCount = headerAdapter.getHeadersCount();
            menuAdapter = (MenuAdapter) headerAdapter.getWrappedAdapter();
        } else {
            headersCount = 0;
            menuAdapter = (MenuAdapter) listAdapter;
        }
        int ownerPosition = -1;
        int i = 0;
        int count = menuAdapter.getCount();
        while (true) {
            if (i >= count) {
                break;
            } else if (owner != menuAdapter.getItem(i)) {
                i++;
            } else {
                ownerPosition = i;
                break;
            }
        }
        if (ownerPosition != -1 && (ownerViewPosition = (ownerPosition + headersCount) - listView.getFirstVisiblePosition()) >= 0 && ownerViewPosition < listView.getChildCount()) {
            return listView.getChildAt(ownerViewPosition);
        }
        return null;
    }

    @Override // android.support.v7.view.menu.ShowableListMenu
    public boolean isShowing() {
        if (this.mShowingMenus.size() > 0) {
            return this.mShowingMenus.get(0).window.isShowing();
        }
        return false;
    }

    @Override // android.widget.PopupWindow.OnDismissListener
    public void onDismiss() {
        CascadingMenuInfo dismissedInfo = null;
        int i = 0;
        int count = this.mShowingMenus.size();
        while (true) {
            if (i >= count) {
                break;
            }
            CascadingMenuInfo info = this.mShowingMenus.get(i);
            if (info.window.isShowing()) {
                i++;
            } else {
                dismissedInfo = info;
                break;
            }
        }
        if (dismissedInfo == null) {
            return;
        }
        dismissedInfo.menu.close(false);
    }

    @Override // android.support.v7.view.menu.MenuPresenter
    public void updateMenuView(boolean cleared) {
        for (CascadingMenuInfo info : this.mShowingMenus) {
            toMenuAdapter(info.getListView().getAdapter()).notifyDataSetChanged();
        }
    }

    @Override // android.support.v7.view.menu.MenuPresenter
    public void setCallback(MenuPresenter.Callback cb) {
        this.mPresenterCallback = cb;
    }

    @Override // android.support.v7.view.menu.MenuPresenter
    public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
        for (CascadingMenuInfo info : this.mShowingMenus) {
            if (subMenu == info.menu) {
                info.getListView().requestFocus();
                return true;
            }
        }
        if (subMenu.hasVisibleItems()) {
            addMenu(subMenu);
            if (this.mPresenterCallback != null) {
                this.mPresenterCallback.onOpenSubMenu(subMenu);
            }
            return true;
        }
        return false;
    }

    private int findIndexOfAddedMenu(@NonNull MenuBuilder menu) {
        int count = this.mShowingMenus.size();
        for (int i = 0; i < count; i++) {
            CascadingMenuInfo info = this.mShowingMenus.get(i);
            if (menu == info.menu) {
                return i;
            }
        }
        return -1;
    }

    @Override // android.support.v7.view.menu.MenuPresenter
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        int menuIndex = findIndexOfAddedMenu(menu);
        if (menuIndex < 0) {
            return;
        }
        int nextMenuIndex = menuIndex + 1;
        if (nextMenuIndex < this.mShowingMenus.size()) {
            CascadingMenuInfo childInfo = this.mShowingMenus.get(nextMenuIndex);
            childInfo.menu.close(false);
        }
        CascadingMenuInfo info = this.mShowingMenus.remove(menuIndex);
        info.menu.removeMenuPresenter(this);
        if (this.mShouldCloseImmediately) {
            info.window.setExitTransition(null);
            info.window.setAnimationStyle(0);
        }
        info.window.dismiss();
        int count = this.mShowingMenus.size();
        if (count > 0) {
            this.mLastPosition = this.mShowingMenus.get(count - 1).position;
        } else {
            this.mLastPosition = getInitialMenuPosition();
        }
        if (count == 0) {
            dismiss();
            if (this.mPresenterCallback != null) {
                this.mPresenterCallback.onCloseMenu(menu, true);
            }
            if (this.mTreeObserver != null) {
                if (this.mTreeObserver.isAlive()) {
                    this.mTreeObserver.removeGlobalOnLayoutListener(this.mGlobalLayoutListener);
                }
                this.mTreeObserver = null;
            }
            this.mOnDismissListener.onDismiss();
        } else if (!allMenusAreClosing) {
        } else {
            CascadingMenuInfo rootInfo = this.mShowingMenus.get(0);
            rootInfo.menu.close(false);
        }
    }

    @Override // android.support.v7.view.menu.MenuPresenter
    public boolean flagActionItems() {
        return false;
    }

    @Override // android.support.v7.view.menu.MenuPopup
    public void setGravity(int dropDownGravity) {
        if (this.mRawDropDownGravity == dropDownGravity) {
            return;
        }
        this.mRawDropDownGravity = dropDownGravity;
        this.mDropDownGravity = GravityCompat.getAbsoluteGravity(dropDownGravity, ViewCompat.getLayoutDirection(this.mAnchorView));
    }

    @Override // android.support.v7.view.menu.MenuPopup
    public void setAnchorView(@NonNull View anchor) {
        if (this.mAnchorView == anchor) {
            return;
        }
        this.mAnchorView = anchor;
        this.mDropDownGravity = GravityCompat.getAbsoluteGravity(this.mRawDropDownGravity, ViewCompat.getLayoutDirection(this.mAnchorView));
    }

    @Override // android.support.v7.view.menu.MenuPopup
    public void setOnDismissListener(PopupWindow.OnDismissListener listener) {
        this.mOnDismissListener = listener;
    }

    @Override // android.support.v7.view.menu.ShowableListMenu
    public ListView getListView() {
        if (this.mShowingMenus.isEmpty()) {
            return null;
        }
        return this.mShowingMenus.get(this.mShowingMenus.size() - 1).getListView();
    }

    @Override // android.support.v7.view.menu.MenuPopup
    public void setHorizontalOffset(int x) {
        this.mHasXOffset = true;
        this.mXOffset = x;
    }

    @Override // android.support.v7.view.menu.MenuPopup
    public void setVerticalOffset(int y) {
        this.mHasYOffset = true;
        this.mYOffset = y;
    }

    @Override // android.support.v7.view.menu.MenuPopup
    public void setShowTitle(boolean showTitle) {
        this.mShowTitle = showTitle;
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* loaded from: classes.dex */
    public static class CascadingMenuInfo {
        public final MenuBuilder menu;
        public final int position;
        public final MenuPopupWindow window;

        public CascadingMenuInfo(@NonNull MenuPopupWindow window, @NonNull MenuBuilder menu, int position) {
            this.window = window;
            this.menu = menu;
            this.position = position;
        }

        public ListView getListView() {
            return this.window.getListView();
        }
    }
}
