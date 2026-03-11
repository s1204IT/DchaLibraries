package com.android.setupwizardlib.template;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.items.ItemAdapter;
import com.android.setupwizardlib.items.ItemGroup;
import com.android.setupwizardlib.items.ItemInflater;
import com.android.setupwizardlib.util.DrawableLayoutDirectionHelper;

public class ListMixin implements Mixin {
    private Drawable mDefaultDivider;
    private Drawable mDivider;
    private int mDividerInsetEnd;
    private int mDividerInsetStart;
    private ListView mListView;
    private TemplateLayout mTemplateLayout;

    public ListMixin(TemplateLayout templateLayout, AttributeSet attributeSet, int i) {
        this.mTemplateLayout = templateLayout;
        Context context = templateLayout.getContext();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwListMixin, i, 0);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwListMixin_android_entries, 0);
        if (resourceId != 0) {
            setAdapter(new ItemAdapter((ItemGroup) new ItemInflater(context).inflate(resourceId)));
        }
        int dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwListMixin_suwDividerInset, -1);
        if (dimensionPixelSize != -1) {
            setDividerInset(dimensionPixelSize);
        } else {
            setDividerInsets(typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwListMixin_suwDividerInsetStart, 0), typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwListMixin_suwDividerInsetEnd, 0));
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    public ListView getListView() {
        return getListViewInternal();
    }

    private ListView getListViewInternal() {
        if (this.mListView == null) {
            View viewFindManagedViewById = this.mTemplateLayout.findManagedViewById(android.R.id.list);
            if (viewFindManagedViewById instanceof ListView) {
                this.mListView = (ListView) viewFindManagedViewById;
            }
        }
        return this.mListView;
    }

    public void onLayout() {
        if (this.mDivider == null) {
            updateDivider();
        }
    }

    public ListAdapter getAdapter() {
        ListView listViewInternal = getListViewInternal();
        if (listViewInternal != null) {
            ListAdapter adapter = listViewInternal.getAdapter();
            if (adapter instanceof HeaderViewListAdapter) {
                return ((HeaderViewListAdapter) adapter).getWrappedAdapter();
            }
            return adapter;
        }
        return null;
    }

    public void setAdapter(ListAdapter listAdapter) {
        ListView listViewInternal = getListViewInternal();
        if (listViewInternal != null) {
            listViewInternal.setAdapter(listAdapter);
        }
    }

    @Deprecated
    public void setDividerInset(int i) {
        setDividerInsets(i, 0);
    }

    public void setDividerInsets(int i, int i2) {
        this.mDividerInsetStart = i;
        this.mDividerInsetEnd = i2;
        updateDivider();
    }

    @Deprecated
    public int getDividerInset() {
        return getDividerInsetStart();
    }

    public int getDividerInsetStart() {
        return this.mDividerInsetStart;
    }

    public int getDividerInsetEnd() {
        return this.mDividerInsetEnd;
    }

    private void updateDivider() {
        ListView listViewInternal = getListViewInternal();
        if (listViewInternal == null) {
            return;
        }
        boolean zIsLayoutDirectionResolved = true;
        if (Build.VERSION.SDK_INT >= 19) {
            zIsLayoutDirectionResolved = this.mTemplateLayout.isLayoutDirectionResolved();
        }
        if (zIsLayoutDirectionResolved) {
            if (this.mDefaultDivider == null) {
                this.mDefaultDivider = listViewInternal.getDivider();
            }
            this.mDivider = DrawableLayoutDirectionHelper.createRelativeInsetDrawable(this.mDefaultDivider, this.mDividerInsetStart, 0, this.mDividerInsetEnd, 0, this.mTemplateLayout);
            listViewInternal.setDivider(this.mDivider);
        }
    }

    public Drawable getDivider() {
        return this.mDivider;
    }
}
