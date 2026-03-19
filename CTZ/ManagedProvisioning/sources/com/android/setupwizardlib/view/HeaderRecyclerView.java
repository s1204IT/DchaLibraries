package com.android.setupwizardlib.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import com.android.setupwizardlib.R;

public class HeaderRecyclerView extends RecyclerView {
    private View mHeader;
    private int mHeaderRes;

    public HeaderRecyclerView(Context context) {
        super(context);
        init(null, 0);
    }

    public HeaderRecyclerView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init(attributeSet, 0);
    }

    public HeaderRecyclerView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init(attributeSet, i);
    }

    private void init(AttributeSet attributeSet, int i) {
        TypedArray typedArrayObtainStyledAttributes = getContext().obtainStyledAttributes(attributeSet, R.styleable.SuwHeaderRecyclerView, i, 0);
        this.mHeaderRes = typedArrayObtainStyledAttributes.getResourceId(R.styleable.SuwHeaderRecyclerView_suwHeader, 0);
        typedArrayObtainStyledAttributes.recycle();
    }

    @Override
    public void onInitializeAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
        super.onInitializeAccessibilityEvent(accessibilityEvent);
        int i = this.mHeader != null ? 1 : 0;
        accessibilityEvent.setItemCount(accessibilityEvent.getItemCount() - i);
        accessibilityEvent.setFromIndex(Math.max(accessibilityEvent.getFromIndex() - i, 0));
        if (Build.VERSION.SDK_INT >= 14) {
            accessibilityEvent.setToIndex(Math.max(accessibilityEvent.getToIndex() - i, 0));
        }
    }

    public View getHeader() {
        return this.mHeader;
    }

    @Override
    public void setLayoutManager(RecyclerView.LayoutManager layoutManager) {
        super.setLayoutManager(layoutManager);
        if (layoutManager != null && this.mHeader == null && this.mHeaderRes != 0) {
            this.mHeader = LayoutInflater.from(getContext()).inflate(this.mHeaderRes, (ViewGroup) this, false);
        }
    }
}
