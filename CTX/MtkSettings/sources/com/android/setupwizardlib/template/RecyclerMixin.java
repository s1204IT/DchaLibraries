package com.android.setupwizardlib.template;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import com.android.setupwizardlib.DividerItemDecoration;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.items.ItemInflater;
import com.android.setupwizardlib.items.RecyclerItemAdapter;
import com.android.setupwizardlib.util.DrawableLayoutDirectionHelper;
import com.android.setupwizardlib.view.HeaderRecyclerView;

public class RecyclerMixin implements Mixin {
    private Drawable mDefaultDivider;
    private Drawable mDivider;
    private DividerItemDecoration mDividerDecoration;
    private int mDividerInsetEnd;
    private int mDividerInsetStart;
    private View mHeader;
    private final RecyclerView mRecyclerView;
    private TemplateLayout mTemplateLayout;

    public RecyclerMixin(TemplateLayout templateLayout, RecyclerView recyclerView) {
        this.mTemplateLayout = templateLayout;
        this.mDividerDecoration = new DividerItemDecoration(this.mTemplateLayout.getContext());
        this.mRecyclerView = recyclerView;
        this.mRecyclerView.setLayoutManager(new LinearLayoutManager(this.mTemplateLayout.getContext()));
        if (recyclerView instanceof HeaderRecyclerView) {
            this.mHeader = ((HeaderRecyclerView) recyclerView).getHeader();
        }
        this.mRecyclerView.addItemDecoration(this.mDividerDecoration);
    }

    public void parseAttributes(AttributeSet attributeSet, int i) {
        Context context = this.mTemplateLayout.getContext();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.SuwRecyclerMixin, i, 0);
        int resourceId = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwRecyclerMixin_android_entries, 0);
        if (resourceId != 0) {
            RecyclerItemAdapter recyclerItemAdapter = new RecyclerItemAdapter(new ItemInflater(context).inflate(resourceId));
            recyclerItemAdapter.setHasStableIds(typedArrayObtainStyledAttributes.getBoolean(R.styleable.SuwRecyclerMixin_suwHasStableIds, false));
            setAdapter(recyclerItemAdapter);
        }
        int dimensionPixelSize = typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwRecyclerMixin_suwDividerInset, -1);
        if (dimensionPixelSize != -1) {
            setDividerInset(dimensionPixelSize);
        } else {
            setDividerInsets(typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwRecyclerMixin_suwDividerInsetStart, 0), typedArrayObtainStyledAttributes.getDimensionPixelSize(R.styleable.SuwRecyclerMixin_suwDividerInsetEnd, 0));
        }
        typedArrayObtainStyledAttributes.recycle();
    }

    public RecyclerView getRecyclerView() {
        return this.mRecyclerView;
    }

    public View getHeader() {
        return this.mHeader;
    }

    public void onLayout() {
        if (this.mDivider == null) {
            updateDivider();
        }
    }

    public RecyclerView.Adapter<? extends RecyclerView.ViewHolder> getAdapter() {
        RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter = this.mRecyclerView.getAdapter();
        if (adapter instanceof HeaderRecyclerView.HeaderAdapter) {
            return ((HeaderRecyclerView.HeaderAdapter) adapter).getWrappedAdapter();
        }
        return adapter;
    }

    public void setAdapter(RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
        this.mRecyclerView.setAdapter(adapter);
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
        boolean zIsLayoutDirectionResolved;
        if (Build.VERSION.SDK_INT >= 19) {
            zIsLayoutDirectionResolved = this.mTemplateLayout.isLayoutDirectionResolved();
        } else {
            zIsLayoutDirectionResolved = true;
        }
        if (zIsLayoutDirectionResolved) {
            if (this.mDefaultDivider == null) {
                this.mDefaultDivider = this.mDividerDecoration.getDivider();
            }
            this.mDivider = DrawableLayoutDirectionHelper.createRelativeInsetDrawable(this.mDefaultDivider, this.mDividerInsetStart, 0, this.mDividerInsetEnd, 0, this.mTemplateLayout);
            this.mDividerDecoration.setDivider(this.mDivider);
        }
    }

    public Drawable getDivider() {
        return this.mDivider;
    }

    public void setDividerItemDecoration(DividerItemDecoration dividerItemDecoration) {
        this.mRecyclerView.removeItemDecoration(this.mDividerDecoration);
        this.mDividerDecoration = dividerItemDecoration;
        this.mRecyclerView.addItemDecoration(this.mDividerDecoration);
        updateDivider();
    }
}
