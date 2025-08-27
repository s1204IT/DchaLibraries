package com.android.setupwizardlib;

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

/* loaded from: classes.dex */
public class SetupWizardRecyclerLayout extends SetupWizardLayout {
    protected RecyclerMixin mRecyclerMixin;

    public SetupWizardRecyclerLayout(Context context) {
        this(context, 0, 0);
    }

    public SetupWizardRecyclerLayout(Context context, int i, int i2) {
        super(context, i, i2);
        init(context, null, 0);
    }

    public SetupWizardRecyclerLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet, 0);
    }

    public SetupWizardRecyclerLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context, attributeSet, i);
    }

    private void init(Context context, AttributeSet attributeSet, int i) {
        this.mRecyclerMixin.parseAttributes(attributeSet, i);
        registerMixin(RecyclerMixin.class, this.mRecyclerMixin);
        RequireScrollMixin requireScrollMixin = (RequireScrollMixin) getMixin(RequireScrollMixin.class);
        requireScrollMixin.setScrollHandlingDelegate(new RecyclerViewScrollHandlingDelegate(requireScrollMixin, getRecyclerView()));
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mRecyclerMixin.onLayout();
    }

    public RecyclerView.Adapter<? extends RecyclerView.ViewHolder> getAdapter() {
        return this.mRecyclerMixin.getAdapter();
    }

    public void setAdapter(RecyclerView.Adapter<? extends RecyclerView.ViewHolder> adapter) {
        this.mRecyclerMixin.setAdapter(adapter);
    }

    public RecyclerView getRecyclerView() {
        return this.mRecyclerMixin.getRecyclerView();
    }

    @Override // com.android.setupwizardlib.SetupWizardLayout, com.android.setupwizardlib.TemplateLayout
    protected ViewGroup findContainer(int i) {
        if (i == 0) {
            i = R.id.suw_recycler_view;
        }
        return super.findContainer(i);
    }

    @Override // com.android.setupwizardlib.SetupWizardLayout, com.android.setupwizardlib.TemplateLayout
    protected View onInflateTemplate(LayoutInflater layoutInflater, int i) {
        if (i == 0) {
            i = R.layout.suw_recycler_template;
        }
        return super.onInflateTemplate(layoutInflater, i);
    }

    @Override // com.android.setupwizardlib.TemplateLayout
    protected void onTemplateInflated() {
        View viewFindViewById = findViewById(R.id.suw_recycler_view);
        if (viewFindViewById instanceof RecyclerView) {
            this.mRecyclerMixin = new RecyclerMixin(this, (RecyclerView) viewFindViewById);
            return;
        }
        throw new IllegalStateException("SetupWizardRecyclerLayout should use a template with recycler view");
    }

    @Override // com.android.setupwizardlib.TemplateLayout
    public <T extends View> T findManagedViewById(int i) {
        T t;
        View header = this.mRecyclerMixin.getHeader();
        if (header != null && (t = (T) header.findViewById(i)) != null) {
            return t;
        }
        return (T) super.findViewById(i);
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
