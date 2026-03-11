package com.android.settings.applications;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.android.internal.R;
import com.android.settings.Utils;

public class LayoutPreference extends Preference {
    private View mRootView;

    public LayoutPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Preference, 0, 0);
        int layoutResource = a.getResourceId(3, 0);
        if (layoutResource == 0) {
            throw new IllegalArgumentException("LayoutPreference requires a layout to be defined");
        }
        View view = LayoutInflater.from(getContext()).inflate(layoutResource, (ViewGroup) null, false);
        setView(view);
    }

    public LayoutPreference(Context context, int resource) {
        this(context, LayoutInflater.from(context).inflate(resource, (ViewGroup) null, false));
    }

    public LayoutPreference(Context context, View view) {
        super(context);
        setView(view);
    }

    private void setView(View view) {
        setLayoutResource(com.android.settings.R.layout.layout_preference_frame);
        setSelectable(false);
        ViewGroup allDetails = (ViewGroup) view.findViewById(com.android.settings.R.id.all_details);
        if (allDetails != null) {
            Utils.forceCustomPadding(allDetails, true);
        }
        this.mRootView = view;
        setShouldDisableView(false);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        FrameLayout layout = (FrameLayout) view.itemView;
        layout.removeAllViews();
        ViewGroup parent = (ViewGroup) this.mRootView.getParent();
        if (parent != null) {
            parent.removeView(this.mRootView);
        }
        layout.addView(this.mRootView);
    }

    public View findViewById(int id) {
        return this.mRootView.findViewById(id);
    }
}
