package android.support.v7.view.menu;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.appcompat.R$dimen;
import android.support.v7.view.menu.MenuPresenter;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;
/* loaded from: classes.dex */
public class MenuPopupHelper {
    private View mAnchorView;
    private final Context mContext;
    private int mDropDownGravity;
    private boolean mForceShowIcon;
    private final PopupWindow.OnDismissListener mInternalOnDismissListener;
    private final MenuBuilder mMenu;
    private PopupWindow.OnDismissListener mOnDismissListener;
    private final boolean mOverflowOnly;
    private MenuPopup mPopup;
    private final int mPopupStyleAttr;
    private final int mPopupStyleRes;
    private MenuPresenter.Callback mPresenterCallback;

    public MenuPopupHelper(@NonNull Context context, @NonNull MenuBuilder menu, @NonNull View anchorView, boolean overflowOnly, @AttrRes int popupStyleAttr) {
        this(context, menu, anchorView, overflowOnly, popupStyleAttr, 0);
    }

    public MenuPopupHelper(@NonNull Context context, @NonNull MenuBuilder menu, @NonNull View anchorView, boolean overflowOnly, @AttrRes int popupStyleAttr, @StyleRes int popupStyleRes) {
        this.mDropDownGravity = 8388611;
        this.mInternalOnDismissListener = new PopupWindow.OnDismissListener() { // from class: android.support.v7.view.menu.MenuPopupHelper.1
            @Override // android.widget.PopupWindow.OnDismissListener
            public void onDismiss() {
                MenuPopupHelper.this.onDismiss();
            }
        };
        this.mContext = context;
        this.mMenu = menu;
        this.mAnchorView = anchorView;
        this.mOverflowOnly = overflowOnly;
        this.mPopupStyleAttr = popupStyleAttr;
        this.mPopupStyleRes = popupStyleRes;
    }

    public void setOnDismissListener(@Nullable PopupWindow.OnDismissListener listener) {
        this.mOnDismissListener = listener;
    }

    public void setAnchorView(@NonNull View anchor) {
        this.mAnchorView = anchor;
    }

    public void setForceShowIcon(boolean forceShowIcon) {
        this.mForceShowIcon = forceShowIcon;
        if (this.mPopup == null) {
            return;
        }
        this.mPopup.setForceShowIcon(forceShowIcon);
    }

    public void setGravity(int gravity) {
        this.mDropDownGravity = gravity;
    }

    public void show() {
        if (tryShow()) {
            return;
        }
        throw new IllegalStateException("MenuPopupHelper cannot be used without an anchor");
    }

    @NonNull
    public MenuPopup getPopup() {
        if (this.mPopup == null) {
            this.mPopup = createPopup();
        }
        return this.mPopup;
    }

    public boolean tryShow() {
        if (isShowing()) {
            return true;
        }
        if (this.mAnchorView == null) {
            return false;
        }
        showPopup(0, 0, false, false);
        return true;
    }

    public boolean tryShow(int x, int y) {
        if (isShowing()) {
            return true;
        }
        if (this.mAnchorView == null) {
            return false;
        }
        showPopup(x, y, true, true);
        return true;
    }

    @NonNull
    private MenuPopup createPopup() {
        MenuPopup popup;
        WindowManager windowManager = (WindowManager) this.mContext.getSystemService("window");
        Display display = windowManager.getDefaultDisplay();
        Point displaySize = new Point();
        if (Build.VERSION.SDK_INT >= 17) {
            display.getRealSize(displaySize);
        } else if (Build.VERSION.SDK_INT >= 13) {
            display.getSize(displaySize);
        } else {
            displaySize.set(display.getWidth(), display.getHeight());
        }
        int smallestWidth = Math.min(displaySize.x, displaySize.y);
        int minSmallestWidthCascading = this.mContext.getResources().getDimensionPixelSize(R$dimen.abc_cascading_menus_min_smallest_width);
        boolean enableCascadingSubmenus = smallestWidth >= minSmallestWidthCascading;
        if (enableCascadingSubmenus) {
            popup = new CascadingMenuPopup(this.mContext, this.mAnchorView, this.mPopupStyleAttr, this.mPopupStyleRes, this.mOverflowOnly);
        } else {
            popup = new StandardMenuPopup(this.mContext, this.mMenu, this.mAnchorView, this.mPopupStyleAttr, this.mPopupStyleRes, this.mOverflowOnly);
        }
        popup.addMenu(this.mMenu);
        popup.setOnDismissListener(this.mInternalOnDismissListener);
        popup.setAnchorView(this.mAnchorView);
        popup.setCallback(this.mPresenterCallback);
        popup.setForceShowIcon(this.mForceShowIcon);
        popup.setGravity(this.mDropDownGravity);
        return popup;
    }

    private void showPopup(int xOffset, int yOffset, boolean useOffsets, boolean showTitle) {
        MenuPopup popup = getPopup();
        popup.setShowTitle(showTitle);
        if (useOffsets) {
            int hgrav = GravityCompat.getAbsoluteGravity(this.mDropDownGravity, ViewCompat.getLayoutDirection(this.mAnchorView)) & 7;
            if (hgrav == 5) {
                xOffset -= this.mAnchorView.getWidth();
            }
            popup.setHorizontalOffset(xOffset);
            popup.setVerticalOffset(yOffset);
            float density = this.mContext.getResources().getDisplayMetrics().density;
            int halfSize = (int) ((48.0f * density) / 2.0f);
            Rect epicenter = new Rect(xOffset - halfSize, yOffset - halfSize, xOffset + halfSize, yOffset + halfSize);
            popup.setEpicenterBounds(epicenter);
        }
        popup.show();
    }

    public void dismiss() {
        if (!isShowing()) {
            return;
        }
        this.mPopup.dismiss();
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void onDismiss() {
        this.mPopup = null;
        if (this.mOnDismissListener == null) {
            return;
        }
        this.mOnDismissListener.onDismiss();
    }

    public boolean isShowing() {
        if (this.mPopup != null) {
            return this.mPopup.isShowing();
        }
        return false;
    }

    public void setPresenterCallback(@Nullable MenuPresenter.Callback cb) {
        this.mPresenterCallback = cb;
        if (this.mPopup == null) {
            return;
        }
        this.mPopup.setCallback(cb);
    }
}
