package com.android.setupwizardlib.template;

import android.view.View;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;
import com.android.setupwizardlib.view.NavigationBar;

public class NavigationBarMixin implements Mixin {
    private TemplateLayout mTemplateLayout;

    public NavigationBarMixin(TemplateLayout templateLayout) {
        this.mTemplateLayout = templateLayout;
    }

    public NavigationBar getNavigationBar() {
        View viewFindManagedViewById = this.mTemplateLayout.findManagedViewById(R.id.suw_layout_navigation_bar);
        if (viewFindManagedViewById instanceof NavigationBar) {
            return (NavigationBar) viewFindManagedViewById;
        }
        return null;
    }
}
