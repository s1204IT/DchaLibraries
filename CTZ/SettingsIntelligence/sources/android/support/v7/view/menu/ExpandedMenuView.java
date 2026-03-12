package android.support.v7.view.menu;

import android.R;
import android.content.Context;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.TintTypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

public final class ExpandedMenuView extends ListView implements MenuBuilder.ItemInvoker, MenuView, AdapterView.OnItemClickListener {
    private static final int[] TINT_ATTRS = {R.attr.background, R.attr.divider};
    private int mAnimations;
    private MenuBuilder mMenu;

    public ExpandedMenuView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.listViewStyle);
    }

    public ExpandedMenuView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
        setOnItemClickListener(this);
        TintTypedArray a = TintTypedArray.obtainStyledAttributes(context, attrs, TINT_ATTRS, defStyleAttr, 0);
        if (a.hasValue(0)) {
            setBackgroundDrawable(a.getDrawable(0));
        }
        if (a.hasValue(1)) {
            setDivider(a.getDrawable(1));
        }
        a.recycle();
    }

    @Override
    public void initialize(MenuBuilder menu) {
        this.mMenu = menu;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        setChildrenDrawingCacheEnabled(false);
    }

    @Override
    public boolean invokeItem(MenuItemImpl item) {
        return this.mMenu.performItemAction(item, 0);
    }

    @Override
    public void onItemClick(AdapterView parent, View v, int position, long id) {
        invokeItem((MenuItemImpl) getAdapter().getItem(position));
    }

    public int getWindowAnimations() {
        return this.mAnimations;
    }
}
