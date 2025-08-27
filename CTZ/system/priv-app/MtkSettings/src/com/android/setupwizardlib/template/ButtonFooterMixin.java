package com.android.setupwizardlib.template;

import android.content.Context;
import android.view.ViewStub;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;

/* loaded from: classes.dex */
public class ButtonFooterMixin implements Mixin {
    private final Context mContext;
    private final ViewStub mFooterStub;

    public ButtonFooterMixin(TemplateLayout templateLayout) {
        this.mContext = templateLayout.getContext();
        this.mFooterStub = (ViewStub) templateLayout.findManagedViewById(R.id.suw_layout_footer);
    }
}
