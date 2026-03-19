package com.mediatek.contacts.list;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.PopupMenu;
import java.util.ArrayList;

public class DropMenu implements PopupMenu.OnMenuItemClickListener {
    private Context mContext;
    private PopupMenu.OnMenuItemClickListener mListener;
    private ArrayList<DropDownMenu> mMenus = new ArrayList<>();

    public static class DropDownMenu {
        private Button mButton;
        private boolean mIsPopupMenuShown;
        private Menu mMenu;
        private PopupMenu mPopupMenu;

        public DropDownMenu(Context context, Button button, int i, PopupMenu.OnMenuItemClickListener onMenuItemClickListener) {
            this.mButton = button;
            this.mPopupMenu = new PopupMenu(context, this.mButton);
            this.mMenu = this.mPopupMenu.getMenu();
            this.mPopupMenu.getMenuInflater().inflate(i, this.mMenu);
            this.mPopupMenu.setOnMenuItemClickListener(onMenuItemClickListener);
            this.mPopupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                @Override
                public void onDismiss(PopupMenu popupMenu) {
                    DropDownMenu.this.mIsPopupMenuShown = false;
                }
            });
            this.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DropDownMenu.this.show();
                }
            });
        }

        public MenuItem findItem(int i) {
            return this.mMenu.findItem(i);
        }

        public void show() {
            this.mIsPopupMenuShown = true;
            this.mPopupMenu.show();
        }

        public void dismiss() {
            this.mIsPopupMenuShown = false;
            this.mPopupMenu.dismiss();
        }

        public boolean isShown() {
            return this.mIsPopupMenuShown;
        }
    }

    public DropMenu(Context context) {
        this.mContext = context;
    }

    public DropDownMenu addDropDownMenu(Button button, int i) {
        DropDownMenu dropDownMenu = new DropDownMenu(this.mContext, button, i, this);
        this.mMenus.add(dropDownMenu);
        return dropDownMenu;
    }

    public void setOnMenuItemClickListener(PopupMenu.OnMenuItemClickListener onMenuItemClickListener) {
        this.mListener = onMenuItemClickListener;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (this.mListener != null) {
            return this.mListener.onMenuItemClick(menuItem);
        }
        return false;
    }
}
