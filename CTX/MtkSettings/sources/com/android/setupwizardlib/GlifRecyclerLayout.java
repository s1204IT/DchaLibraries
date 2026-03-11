package com.android.setupwizardlib;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.setupwizardlib.template.RecyclerMixin;
import com.android.setupwizardlib.template.RecyclerViewScrollHandlingDelegate;
import com.android.setupwizardlib.template.RequireScrollMixin;

public class GlifRecyclerLayout extends GlifLayout {
    protected RecyclerMixin mRecyclerMixin;

    public GlifRecyclerLayout(Context context) {
        this(context, 0, 0);
    }

    public GlifRecyclerLayout(Context context, int i) {
        this(context, i, 0);
    }

    public GlifRecyclerLayout(Context context, int i, int i2) {
        super(context, i, i2);
        init(context, null, 0);
    }

    public GlifRecyclerLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet, 0);
    }

    @TargetApi(11)
    public GlifRecyclerLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context, attributeSet, i);
    }

    private void init(Context context, AttributeSet attributeSet, int i) {
        this.mRecyclerMixin.parseAttributes(attributeSet, i);
        registerMixin(RecyclerMixin.class, this.mRecyclerMixin);
        RequireScrollMixin requireScrollMixin = (RequireScrollMixin) getMixin(RequireScrollMixin.class);
        requireScrollMixin.setScrollHandlingDelegate(new RecyclerViewScrollHandlingDelegate(requireScrollMixin, getRecyclerView()));
    }

    @Override
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mRecyclerMixin.onLayout();
    }

    @Override
    protected View onInflateTemplate(LayoutInflater layoutInflater, int i) {
        if (i == 0) {
            i = R.layout.suw_glif_recycler_template;
        }
        return super.onInflateTemplate(layoutInflater, i);
    }

    @Override
    protected void onTemplateInflated() {
        View viewFindViewById = findViewById(R.id.suw_recycler_view);
        if (viewFindViewById instanceof RecyclerView) {
            this.mRecyclerMixin = new RecyclerMixin(this, (RecyclerView) viewFindViewById);
            return;
        }
        throw new IllegalStateException("GlifRecyclerLayout should use a template with recycler view");
    }

    @Override
    protected ViewGroup findContainer(int i) {
        if (i == 0) {
            i = R.id.suw_recycler_view;
        }
        return super.findContainer(i);
    }

    @Override
    public <T extends View> T findManagedViewById(int i) {
        T t;
        View header = this.mRecyclerMixin.getHeader();
        if (header != null && (t = (T) header.findViewById(i)) != null) {
            return t;
        }
        return (T) super.findViewById(i);
    }

    public void setDividerItemDecoration(DividerItemDecoration dividerItemDecoration) {
        this.mRecyclerMixin.setDividerItemDecoration(dividerItemDecoration);
    }

    public RecyclerView getRecyclerView() {
        return this.mRecyclerMixin.getRecyclerView();
    }

    public void setAdapter(RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
        this.mRecyclerMixin.setAdapter(adapter);
    }

    public RecyclerView.Adapter<? extends RecyclerView.ViewHolder> getAdapter() {
        return this.mRecyclerMixin.getAdapter();
    }

    @Deprecated
    public void setDividerInset(int i) {
        this.mRecyclerMixin.setDividerInset(i);
    }

    @Deprecated
    public int getDividerInset() {
        return this.mRecyclerMixin.getDividerInset();
    }

    public int getDividerInsetStart() {
        return this.mRecyclerMixin.getDividerInsetStart();
    }

    public int getDividerInsetEnd() {
        return this.mRecyclerMixin.getDividerInsetEnd();
    }

    public Drawable getDivider() {
        return this.mRecyclerMixin.getDivider();
    }
}
