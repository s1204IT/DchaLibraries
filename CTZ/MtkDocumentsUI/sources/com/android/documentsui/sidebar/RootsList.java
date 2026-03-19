package com.android.documentsui.sidebar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.ListView;
import com.android.documentsui.base.Features;

public class RootsList extends ListView {
    private final Features mFeatures;

    public RootsList(Context context) {
        super(context);
        this.mFeatures = Features.create(getContext());
    }

    public RootsList(Context context, AttributeSet attributeSet, int i, int i2) {
        super(context, attributeSet, i, i2);
        this.mFeatures = Features.create(getContext());
    }

    public RootsList(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        this.mFeatures = Features.create(getContext());
    }

    public RootsList(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mFeatures = Features.create(getContext());
    }

    @Override
    public boolean onKeyDown(int i, KeyEvent keyEvent) {
        if (i == 61) {
            return this.mFeatures.isSystemKeyboardNavigationEnabled() && super.onKeyDown(i, keyEvent);
        }
        switch (i) {
            case 21:
            case 22:
                return true;
            default:
                return super.onKeyDown(i, keyEvent);
        }
    }
}
