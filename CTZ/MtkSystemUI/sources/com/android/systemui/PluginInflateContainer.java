package com.android.systemui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import com.android.systemui.plugins.PluginListener;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.plugins.ViewProvider;

public class PluginInflateContainer extends AutoReinflateContainer implements PluginListener<ViewProvider> {
    private Class<?> mClass;
    private View mPluginView;

    public PluginInflateContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        String string = context.obtainStyledAttributes(attributeSet, R.styleable.PluginInflateContainer).getString(0);
        try {
            this.mClass = Class.forName(string);
        } catch (Exception e) {
            Log.d("PluginInflateContainer", "Problem getting class info " + string, e);
            this.mClass = null;
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.mClass != null) {
            ((PluginManager) Dependency.get(PluginManager.class)).addPluginListener(this, this.mClass);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (this.mClass != null) {
            ((PluginManager) Dependency.get(PluginManager.class)).removePluginListener(this);
        }
    }

    @Override
    protected void inflateLayoutImpl() {
        if (this.mPluginView != null) {
            addView(this.mPluginView);
        } else {
            super.inflateLayoutImpl();
        }
    }

    @Override
    public void onPluginConnected(ViewProvider viewProvider, Context context) {
        this.mPluginView = viewProvider.getView();
        inflateLayout();
    }

    @Override
    public void onPluginDisconnected(ViewProvider viewProvider) {
        this.mPluginView = null;
        inflateLayout();
    }
}
