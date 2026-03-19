package com.android.packageinstaller.permission.ui.television;

import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.android.packageinstaller.R;

public abstract class PermissionsFrameFragment extends PreferenceFragment {
    private RecyclerView mGridView;
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

    @Override
    public void onCreatePreferences(Bundle bundle, String str) {
        if (getPreferenceScreen() == null) {
            setPreferenceScreen(getPreferenceManager().createPreferenceScreen(getActivity()));
        }
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

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        VerticalGridView verticalGridView = (VerticalGridView) layoutInflater.inflate(R.layout.leanback_preferences_list, viewGroup, false);
        verticalGridView.setWindowAlignment(3);
        verticalGridView.setFocusScrollStrategy(0);
        this.mGridView = verticalGridView;
        return this.mGridView;
    }

    @Override
    protected RecyclerView.Adapter<?> onCreateAdapter(PreferenceScreen preferenceScreen) {
        final RecyclerView.Adapter<?> adapterOnCreateAdapter = super.onCreateAdapter(preferenceScreen);
        if (adapterOnCreateAdapter != null) {
            final TextView textView = (TextView) getView().findViewById(R.id.no_permissions);
            onSetEmptyText(textView);
            final RecyclerView listView = getListView();
            adapterOnCreateAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                @Override
                public void onChanged() {
                    checkEmpty();
                }

                @Override
                public void onItemRangeInserted(int i, int i2) {
                    checkEmpty();
                }

                @Override
                public void onItemRangeRemoved(int i, int i2) {
                    checkEmpty();
                }

                private void checkEmpty() {
                    boolean zIsPreferenceListEmpty = PermissionsFrameFragment.this.isPreferenceListEmpty();
                    textView.setVisibility(zIsPreferenceListEmpty ? 0 : 8);
                    listView.setVisibility((zIsPreferenceListEmpty && adapterOnCreateAdapter.getItemCount() == 0) ? 8 : 0);
                    if (!zIsPreferenceListEmpty && PermissionsFrameFragment.this.mGridView != null) {
                        PermissionsFrameFragment.this.mGridView.requestFocus();
                    }
                }
            });
            boolean zIsPreferenceListEmpty = isPreferenceListEmpty();
            int i = 8;
            textView.setVisibility(zIsPreferenceListEmpty ? 0 : 8);
            if (!zIsPreferenceListEmpty || adapterOnCreateAdapter.getItemCount() != 0) {
                i = 0;
            }
            listView.setVisibility(i);
            if (!zIsPreferenceListEmpty && this.mGridView != null) {
                this.mGridView.requestFocus();
            }
        }
        return adapterOnCreateAdapter;
    }

    private boolean isPreferenceListEmpty() {
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        if (preferenceScreen.getPreferenceCount() != 0) {
            return preferenceScreen.getPreferenceCount() == 1 && preferenceScreen.findPreference("HeaderPreferenceKey") != null;
        }
        return true;
    }

    protected void onSetEmptyText(TextView textView) {
    }
}
