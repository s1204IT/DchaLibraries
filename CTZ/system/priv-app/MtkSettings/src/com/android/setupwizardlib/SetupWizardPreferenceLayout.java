package com.android.setupwizardlib;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.android.setupwizardlib.template.RecyclerMixin;

/* loaded from: classes.dex */
public class SetupWizardPreferenceLayout extends SetupWizardRecyclerLayout {
    public SetupWizardPreferenceLayout(Context context) {
        super(context);
    }

    public SetupWizardPreferenceLayout(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public SetupWizardPreferenceLayout(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
    }

    @Override // com.android.setupwizardlib.SetupWizardRecyclerLayout, com.android.setupwizardlib.SetupWizardLayout, com.android.setupwizardlib.TemplateLayout
    protected ViewGroup findContainer(int i) {
        if (i == 0) {
            i = R.id.suw_layout_content;
        }
        return super.findContainer(i);
    }

    @Override // com.android.setupwizardlib.SetupWizardRecyclerLayout, com.android.setupwizardlib.SetupWizardLayout, com.android.setupwizardlib.TemplateLayout
    protected View onInflateTemplate(LayoutInflater layoutInflater, int i) {
        if (i == 0) {
            i = R.layout.suw_preference_template;
        }
        return super.onInflateTemplate(layoutInflater, i);
    }

    @Override // com.android.setupwizardlib.SetupWizardRecyclerLayout, com.android.setupwizardlib.TemplateLayout
    protected void onTemplateInflated() {
        this.mRecyclerMixin = new RecyclerMixin(this, (RecyclerView) LayoutInflater.from(getContext()).inflate(R.layout.suw_preference_recycler_view, (ViewGroup) this, false));
    }
}
