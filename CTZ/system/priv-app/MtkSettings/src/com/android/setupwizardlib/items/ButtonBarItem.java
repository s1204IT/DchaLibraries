package com.android.setupwizardlib.items;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.items.ItemInflater;
import java.util.ArrayList;
import java.util.Iterator;

/* loaded from: classes.dex */
public class ButtonBarItem extends AbstractItem implements ItemInflater.ItemParent {
    private final ArrayList<ButtonItem> mButtons;
    private boolean mVisible;

    public ButtonBarItem() {
        this.mButtons = new ArrayList<>();
        this.mVisible = true;
    }

    public ButtonBarItem(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mButtons = new ArrayList<>();
        this.mVisible = true;
    }

    @Override // com.android.setupwizardlib.items.AbstractItem, com.android.setupwizardlib.items.ItemHierarchy
    public int getCount() {
        return isVisible() ? 1 : 0;
    }

    @Override // com.android.setupwizardlib.items.IItem
    public boolean isEnabled() {
        return false;
    }

    @Override // com.android.setupwizardlib.items.IItem
    public int getLayoutResource() {
        return R.layout.suw_items_button_bar;
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    @Override // com.android.setupwizardlib.items.AbstractItemHierarchy
    public int getViewId() {
        return getId();
    }

    @Override // com.android.setupwizardlib.items.IItem
    public void onBindView(View view) {
        LinearLayout linearLayout = (LinearLayout) view;
        linearLayout.removeAllViews();
        Iterator<ButtonItem> it = this.mButtons.iterator();
        while (it.hasNext()) {
            linearLayout.addView(it.next().createButton(linearLayout));
        }
        view.setId(getViewId());
    }

    @Override // com.android.setupwizardlib.items.ItemInflater.ItemParent
    public void addChild(ItemHierarchy itemHierarchy) {
        if (itemHierarchy instanceof ButtonItem) {
            this.mButtons.add((ButtonItem) itemHierarchy);
            return;
        }
        throw new UnsupportedOperationException("Cannot add non-button item to Button Bar");
    }
}
