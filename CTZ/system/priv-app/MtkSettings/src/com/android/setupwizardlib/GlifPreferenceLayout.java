package com.android.setupwizardlib;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.setupwizardlib.template.RecyclerMixin;

/* loaded from: classes.dex */
public class GlifPreferenceLayout extends GlifRecyclerLayout {
    public GlifPreferenceLayout(Context context) {
        super(context);
    }

    public GlifPreferenceLayout(Context context, int i, int i2) {
        super(context, i, i2);
    }

    public GlifPreferenceLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public GlifPreferenceLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // com.android.setupwizardlib.GlifRecyclerLayout, com.android.setupwizardlib.GlifLayout, com.android.setupwizardlib.TemplateLayout
    protected ViewGroup findContainer(int i) {
        if (i == 0) {
            i = R.id.suw_layout_content;
        }
        return super.findContainer(i);
    }

    public RecyclerView onCreateRecyclerView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        return this.mRecyclerMixin.getRecyclerView();
    }

    @Override // com.android.setupwizardlib.GlifRecyclerLayout, com.android.setupwizardlib.GlifLayout, com.android.setupwizardlib.TemplateLayout
    protected View onInflateTemplate(LayoutInflater layoutInflater, int i) {
        if (i == 0) {
            i = R.layout.suw_glif_preference_template;
        }
        return super.onInflateTemplate(layoutInflater, i);
    }

    @Override // com.android.setupwizardlib.GlifRecyclerLayout, com.android.setupwizardlib.TemplateLayout
    protected void onTemplateInflated() {
        this.mRecyclerMixin = new RecyclerMixin(this, (RecyclerView) LayoutInflater.from(getContext()).inflate(R.layout.suw_glif_preference_recycler_view, (ViewGroup) this, false));
    }
}
