package com.android.settings;

import android.app.Fragment;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

public class HighlightingFragment extends Fragment {
    private Drawable mHighlightDrawable;
    private boolean mViewHighlighted = false;
    private String mViewKey;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (icicle != null) {
            this.mViewHighlighted = icicle.getBoolean("android:view_highlighted");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("android:view_highlighted", this.mViewHighlighted);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            this.mViewKey = args.getString(":settings:fragment_args_key");
            highlightViewIfNeeded();
        }
    }

    public void highlightViewIfNeeded() {
        if (!this.mViewHighlighted && !TextUtils.isEmpty(this.mViewKey)) {
            highlightView(this.mViewKey);
        }
    }

    private Drawable getHighlightDrawable() {
        if (this.mHighlightDrawable == null) {
            this.mHighlightDrawable = getActivity().getDrawable(R.drawable.preference_highlight);
        }
        return this.mHighlightDrawable;
    }

    private void highlightView(String key) {
        final Drawable highlight = getHighlightDrawable();
        final View view = findViewForKey(getView(), key);
        if (view != null) {
            view.setBackground(highlight);
            getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    int centerX = view.getWidth() / 2;
                    int centerY = view.getHeight() / 2;
                    highlight.setHotspot(centerX, centerY);
                    view.setPressed(true);
                    view.setPressed(false);
                }
            }, 400L);
            this.mViewHighlighted = true;
        }
    }

    private View findViewForKey(View root, String key) {
        if (!checkTag(root, key)) {
            if (root instanceof ViewGroup) {
                ViewGroup group = (ViewGroup) root;
                int count = group.getChildCount();
                for (int n = 0; n < count; n++) {
                    View child = group.getChildAt(n);
                    View view = findViewForKey(child, key);
                    if (view != null) {
                        return view;
                    }
                }
            }
            return null;
        }
        return root;
    }

    private boolean checkTag(View view, String key) {
        Object tag = view.getTag(R.id.preference_highlight_key);
        if (tag == null || !(tag instanceof String)) {
            return false;
        }
        String viewKey = (String) tag;
        return !TextUtils.isEmpty(viewKey) && viewKey.equals(key);
    }
}
