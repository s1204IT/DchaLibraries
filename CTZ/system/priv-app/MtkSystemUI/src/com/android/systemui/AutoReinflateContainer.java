package com.android.systemui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import com.android.systemui.statusbar.policy.ConfigurationController;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class AutoReinflateContainer extends FrameLayout implements ConfigurationController.ConfigurationListener {
    private final List<InflateListener> mInflateListeners;
    private final int mLayout;

    public interface InflateListener {
        void onInflated(View view);
    }

    public AutoReinflateContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mInflateListeners = new ArrayList();
        TypedArray typedArrayObtainStyledAttributes = context.obtainStyledAttributes(attributeSet, R.styleable.AutoReinflateContainer);
        if (!typedArrayObtainStyledAttributes.hasValue(0)) {
            throw new IllegalArgumentException("AutoReinflateContainer must contain a layout");
        }
        this.mLayout = typedArrayObtainStyledAttributes.getResourceId(0, 0);
        typedArrayObtainStyledAttributes.recycle();
        inflateLayout();
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).addCallback(this);
    }

    @Override // android.view.ViewGroup, android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ((ConfigurationController) Dependency.get(ConfigurationController.class)).removeCallback(this);
    }

    protected void inflateLayoutImpl() {
        LayoutInflater.from(getContext()).inflate(this.mLayout, this);
    }

    public void inflateLayout() {
        removeAllViews();
        inflateLayoutImpl();
        int size = this.mInflateListeners.size();
        for (int i = 0; i < size; i++) {
            this.mInflateListeners.get(i).onInflated(getChildAt(0));
        }
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onDensityOrFontScaleChanged() {
        inflateLayout();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onOverlayChanged() {
        inflateLayout();
    }

    @Override // com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
    public void onLocaleListChanged() {
        inflateLayout();
    }
}
