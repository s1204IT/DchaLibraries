package com.android.setupwizardlib.template;

import android.content.res.ColorStateList;
import android.os.Build;
import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;
import com.android.setupwizardlib.R;
import com.android.setupwizardlib.TemplateLayout;

public class ProgressBarMixin implements Mixin {
    private ColorStateList mColor;
    private TemplateLayout mTemplateLayout;

    public ProgressBarMixin(TemplateLayout templateLayout) {
        this.mTemplateLayout = templateLayout;
    }

    public boolean isShown() {
        View viewFindManagedViewById = this.mTemplateLayout.findManagedViewById(R.id.suw_layout_progress);
        return viewFindManagedViewById != null && viewFindManagedViewById.getVisibility() == 0;
    }

    public void setShown(boolean z) {
        if (z) {
            ProgressBar progressBar = getProgressBar();
            if (progressBar != null) {
                progressBar.setVisibility(0);
                return;
            }
            return;
        }
        ProgressBar progressBarPeekProgressBar = peekProgressBar();
        if (progressBarPeekProgressBar != null) {
            progressBarPeekProgressBar.setVisibility(8);
        }
    }

    private ProgressBar getProgressBar() {
        if (peekProgressBar() == null) {
            ViewStub viewStub = (ViewStub) this.mTemplateLayout.findManagedViewById(R.id.suw_layout_progress_stub);
            if (viewStub != null) {
                viewStub.inflate();
            }
            setColor(this.mColor);
        }
        return peekProgressBar();
    }

    public ProgressBar peekProgressBar() {
        return (ProgressBar) this.mTemplateLayout.findManagedViewById(R.id.suw_layout_progress);
    }

    public void setColor(ColorStateList colorStateList) {
        ProgressBar progressBarPeekProgressBar;
        this.mColor = colorStateList;
        if (Build.VERSION.SDK_INT >= 21 && (progressBarPeekProgressBar = peekProgressBar()) != null) {
            progressBarPeekProgressBar.setIndeterminateTintList(colorStateList);
            if (Build.VERSION.SDK_INT >= 23 || colorStateList != null) {
                progressBarPeekProgressBar.setProgressBackgroundTintList(colorStateList);
            }
        }
    }
}
