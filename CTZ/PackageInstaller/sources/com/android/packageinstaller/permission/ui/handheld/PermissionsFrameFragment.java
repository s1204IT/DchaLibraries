package com.android.packageinstaller.permission.ui.handheld;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ListView;
import android.widget.TextView;
import com.android.packageinstaller.R;

public abstract class PermissionsFrameFragment extends PreferenceFragment {
    private boolean mIsLoading;
    private View mLoadingView;
    private ViewGroup mPreferencesContainer;
    private ViewGroup mPrefsView;

    protected final ViewGroup getPreferencesContainer() {
        return this.mPreferencesContainer;
    }

    @Override
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        ViewGroup viewGroup2 = (ViewGroup) layoutInflater.inflate(R.layout.permissions_frame, viewGroup, false);
        this.mPrefsView = (ViewGroup) viewGroup2.findViewById(R.id.prefs_container);
        if (this.mPrefsView == null) {
            this.mPrefsView = viewGroup2;
        }
        this.mLoadingView = viewGroup2.findViewById(R.id.loading_container);
        this.mPreferencesContainer = (ViewGroup) super.onCreateView(layoutInflater, this.mPrefsView, bundle);
        setLoading(this.mIsLoading, false, true);
        this.mPrefsView.addView(this.mPreferencesContainer);
        return viewGroup2;
    }

    protected void setLoading(boolean z, boolean z2) {
        setLoading(z, z2, false);
    }

    private void setLoading(boolean z, boolean z2, boolean z3) {
        if (this.mIsLoading != z || z3) {
            this.mIsLoading = z;
            if (getView() == null) {
                z2 = false;
            }
            if (this.mPrefsView != null) {
                setViewShown(this.mPrefsView, !z, z2);
            }
            if (this.mLoadingView != null) {
                setViewShown(this.mLoadingView, z, z2);
            }
        }
    }

    public ListView getListView() {
        ListView listView = super.getListView();
        if (listView.getEmptyView() == null) {
            listView.setEmptyView((TextView) getView().findViewById(R.id.no_permissions));
        }
        return listView;
    }

    private void setViewShown(final View view, boolean z, boolean z2) {
        if (z2) {
            Animation animationLoadAnimation = AnimationUtils.loadAnimation(getContext(), z ? android.R.anim.fade_in : android.R.anim.fade_out);
            if (z) {
                view.setVisibility(0);
            } else {
                animationLoadAnimation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(4);
                    }
                });
            }
            view.startAnimation(animationLoadAnimation);
            return;
        }
        view.clearAnimation();
        view.setVisibility(z ? 0 : 4);
    }
}
