package com.android.documentsui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.util.AttributeSet;
import android.widget.ImageView;

public final class DropBadgeView extends ImageView {
    private LayerDrawable mBackground;
    private int mState;
    private static final int[] STATE_REJECT_DROP = {R.attr.state_reject_drop};
    private static final int[] STATE_COPY = {R.attr.state_copy};

    public DropBadgeView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        int dimensionPixelSize = context.getResources().getDimensionPixelSize(R.dimen.drop_icon_height);
        int dimensionPixelSize2 = context.getResources().getDimensionPixelSize(R.dimen.drop_icon_width);
        int dimensionPixelSize3 = context.getResources().getDimensionPixelSize(R.dimen.root_icon_size);
        this.mBackground = new LayerDrawable(new Drawable[]{context.getResources().getDrawable(R.drawable.ic_doc_generic, null), context.getResources().getDrawable(R.drawable.drop_badge_states, null)});
        this.mBackground.setLayerGravity(1, 85);
        this.mBackground.setLayerGravity(0, 51);
        this.mBackground.setLayerSize(1, dimensionPixelSize2, dimensionPixelSize);
        this.mBackground.setLayerSize(0, dimensionPixelSize3, dimensionPixelSize3);
        setBackground(this.mBackground);
    }

    @Override
    public int[] onCreateDrawableState(int i) {
        int[] iArrOnCreateDrawableState = super.onCreateDrawableState(i + 1);
        int i2 = this.mState;
        if (i2 == 1) {
            mergeDrawableStates(iArrOnCreateDrawableState, STATE_REJECT_DROP);
        } else if (i2 == 3) {
            mergeDrawableStates(iArrOnCreateDrawableState, STATE_COPY);
        }
        return iArrOnCreateDrawableState;
    }

    void updateState(int i) {
        this.mState = i;
        refreshDrawableState();
    }

    void updateIcon(Drawable drawable) {
        this.mBackground.setDrawable(0, drawable);
    }
}
