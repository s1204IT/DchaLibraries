package com.android.systemui;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.LocaleList;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import java.util.ArrayList;
import java.util.List;

public class AutoReinflateContainer extends FrameLayout {
    private int mDensity;
    private final List<InflateListener> mInflateListeners;
    private final int mLayout;
    private LocaleList mLocaleList;

    public interface InflateListener {
        void onInflated(View view);
    }

    public AutoReinflateContainer(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mInflateListeners = new ArrayList();
        this.mDensity = context.getResources().getConfiguration().densityDpi;
        this.mLocaleList = context.getResources().getConfiguration().getLocales();
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AutoReinflateContainer);
        if (!a.hasValue(0)) {
            throw new IllegalArgumentException("AutoReinflateContainer must contain a layout");
        }
        this.mLayout = a.getResourceId(0, 0);
        inflateLayout();
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean shouldInflateLayout = false;
        int density = newConfig.densityDpi;
        if (density != this.mDensity) {
            this.mDensity = density;
            shouldInflateLayout = true;
        }
        LocaleList localeList = newConfig.getLocales();
        if (localeList != this.mLocaleList) {
            this.mLocaleList = localeList;
            shouldInflateLayout = true;
        }
        if (!shouldInflateLayout) {
            return;
        }
        inflateLayout();
    }

    private void inflateLayout() {
        removeAllViews();
        LayoutInflater.from(getContext()).inflate(this.mLayout, this);
        int N = this.mInflateListeners.size();
        for (int i = 0; i < N; i++) {
            this.mInflateListeners.get(i).onInflated(getChildAt(0));
        }
    }

    public void addInflateListener(InflateListener listener) {
        this.mInflateListeners.add(listener);
        listener.onInflated(getChildAt(0));
    }
}
