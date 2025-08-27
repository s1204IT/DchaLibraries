package com.android.setupwizardlib;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import com.android.setupwizardlib.template.ListMixin;
import com.android.setupwizardlib.template.ListViewScrollHandlingDelegate;
import com.android.setupwizardlib.template.RequireScrollMixin;

/* loaded from: classes.dex */
public class SetupWizardListLayout extends SetupWizardLayout {
    private ListMixin mListMixin;

    public SetupWizardListLayout(Context context) {
        this(context, 0, 0);
    }

    public SetupWizardListLayout(Context context, int i, int i2) {
        super(context, i, i2);
        init(context, null, 0);
    }

    public SetupWizardListLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(context, attributeSet, 0);
    }

    @TargetApi(11)
    public SetupWizardListLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(context, attributeSet, i);
    }

    private void init(Context context, AttributeSet attributeSet, int i) {
        this.mListMixin = new ListMixin(this, attributeSet, i);
        registerMixin(ListMixin.class, this.mListMixin);
        RequireScrollMixin requireScrollMixin = (RequireScrollMixin) getMixin(RequireScrollMixin.class);
        requireScrollMixin.setScrollHandlingDelegate(new ListViewScrollHandlingDelegate(requireScrollMixin, getListView()));
    }

    @Override // com.android.setupwizardlib.SetupWizardLayout, com.android.setupwizardlib.TemplateLayout
    protected View onInflateTemplate(LayoutInflater layoutInflater, int i) {
        if (i == 0) {
            i = R.layout.suw_list_template;
        }
        return super.onInflateTemplate(layoutInflater, i);
    }

    @Override // com.android.setupwizardlib.SetupWizardLayout, com.android.setupwizardlib.TemplateLayout
    protected ViewGroup findContainer(int i) {
        if (i == 0) {
            i = android.R.id.list;
        }
        return super.findContainer(i);
    }

    @Override // android.widget.FrameLayout, android.view.ViewGroup, android.view.View
    protected void onLayout(boolean z, int i, int i2, int i3, int i4) {
        super.onLayout(z, i, i2, i3, i4);
        this.mListMixin.onLayout();
    }

    public ListView getListView() {
        return this.mListMixin.getListView();
    }

    public void setAdapter(ListAdapter listAdapter) {
        this.mListMixin.setAdapter(listAdapter);
    }

    public ListAdapter getAdapter() {
        return this.mListMixin.getAdapter();
    }

    @Deprecated
    public void setDividerInset(int i) {
        this.mListMixin.setDividerInset(i);
    }

    @Deprecated
    public int getDividerInset() {
        return this.mListMixin.getDividerInset();
    }

    public int getDividerInsetStart() {
        return this.mListMixin.getDividerInsetStart();
    }

    public int getDividerInsetEnd() {
        return this.mListMixin.getDividerInsetEnd();
    }

    public Drawable getDivider() {
        return this.mListMixin.getDivider();
    }
}
