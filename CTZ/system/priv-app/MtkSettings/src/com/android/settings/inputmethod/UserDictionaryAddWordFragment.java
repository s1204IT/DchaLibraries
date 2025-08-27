package com.android.settings.inputmethod;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;

/* loaded from: classes.dex */
public class UserDictionaryAddWordFragment extends InstrumentedFragment {
    private UserDictionaryAddWordContents mContents;
    private boolean mIsDeleting = false;
    private View mRootView;

    @Override // android.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    @Override // android.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        this.mRootView = layoutInflater.inflate(R.layout.user_dictionary_add_word_fullscreen, (ViewGroup) null);
        this.mIsDeleting = false;
        if (this.mContents == null) {
            this.mContents = new UserDictionaryAddWordContents(this.mRootView, getArguments());
        } else {
            this.mContents = new UserDictionaryAddWordContents(this.mRootView, this.mContents);
        }
        getActivity().getActionBar().setSubtitle(UserDictionarySettingsUtils.getLocaleDisplayName(getActivity(), this.mContents.getCurrentUserDictionaryLocale()));
        return this.mRootView;
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        menu.add(0, 1, 0, R.string.delete).setIcon(R.drawable.ic_delete).setShowAsAction(5);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == 1) {
            this.mContents.delete(getActivity());
            this.mIsDeleting = true;
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 62;
    }

    @Override // com.android.settings.core.InstrumentedFragment, com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        updateSpinner();
    }

    private void updateSpinner() {
        new ArrayAdapter(getActivity(), android.R.layout.simple_spinner_item, this.mContents.getLocalesList(getActivity())).setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservableFragment, android.app.Fragment
    public void onPause() {
        super.onPause();
        if (!this.mIsDeleting) {
            this.mContents.apply(getActivity(), null);
        }
    }
}
