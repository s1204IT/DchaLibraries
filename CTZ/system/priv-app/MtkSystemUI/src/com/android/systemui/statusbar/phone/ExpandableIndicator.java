package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import com.android.systemui.R;

/* loaded from: classes.dex */
public class ExpandableIndicator extends ImageView {
    private boolean mExpanded;
    private boolean mIsDefaultDirection;

    public ExpandableIndicator(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mIsDefaultDirection = true;
    }

    @Override // android.view.View
    protected void onFinishInflate() {
        super.onFinishInflate();
        updateIndicatorDrawable();
        setContentDescription(getContentDescription(this.mExpanded));
    }

    public void setExpanded(boolean z) {
        if (z == this.mExpanded) {
            return;
        }
        this.mExpanded = z;
        AnimatedVectorDrawable animatedVectorDrawable = (AnimatedVectorDrawable) getContext().getDrawable(getDrawableResourceId(!this.mExpanded)).getConstantState().newDrawable();
        setImageDrawable(animatedVectorDrawable);
        animatedVectorDrawable.forceAnimationOnUI();
        animatedVectorDrawable.start();
        setContentDescription(getContentDescription(z));
    }

    private int getDrawableResourceId(boolean z) {
        return this.mIsDefaultDirection ? z ? R.drawable.ic_volume_collapse_animation : R.drawable.ic_volume_expand_animation : z ? R.drawable.ic_volume_expand_animation : R.drawable.ic_volume_collapse_animation;
    }

    private String getContentDescription(boolean z) {
        return z ? this.mContext.getString(R.string.accessibility_quick_settings_collapse) : this.mContext.getString(R.string.accessibility_quick_settings_expand);
    }

    private void updateIndicatorDrawable() {
        setImageResource(getDrawableResourceId(this.mExpanded));
    }
}
