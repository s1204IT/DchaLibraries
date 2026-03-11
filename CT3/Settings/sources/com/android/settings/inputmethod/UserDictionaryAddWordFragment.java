package com.android.settings.inputmethod;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import com.android.internal.app.LocalePicker;
import com.android.settings.InstrumentedFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.inputmethod.UserDictionaryAddWordContents;
import java.util.ArrayList;
import java.util.Locale;

public class UserDictionaryAddWordFragment extends InstrumentedFragment implements AdapterView.OnItemSelectedListener, LocalePicker.LocaleSelectionListener {
    private UserDictionaryAddWordContents mContents;
    private boolean mIsDeleting = false;
    private View mRootView;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        getActivity().getActionBar().setTitle(R.string.user_dict_settings_title);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
        this.mRootView = inflater.inflate(R.layout.user_dictionary_add_word_fullscreen, (ViewGroup) null);
        this.mIsDeleting = false;
        if (this.mContents == null) {
            this.mContents = new UserDictionaryAddWordContents(this.mRootView, getArguments());
        } else {
            this.mContents = new UserDictionaryAddWordContents(this.mRootView, this.mContents);
        }
        getActivity().getActionBar().setSubtitle(UserDictionarySettingsUtils.getLocaleDisplayName(getActivity(), this.mContents.getCurrentUserDictionaryLocale()));
        return this.mRootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem actionItem = menu.add(0, 1, 0, R.string.delete).setIcon(android.R.drawable.ic_menu_delete);
        actionItem.setShowAsAction(5);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            this.mContents.delete(getActivity());
            this.mIsDeleting = true;
            getActivity().onBackPressed();
            return true;
        }
        return false;
    }

    @Override
    protected int getMetricsCategory() {
        return 62;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSpinner();
    }

    private void updateSpinner() {
        ArrayList<UserDictionaryAddWordContents.LocaleRenderer> localesList = this.mContents.getLocalesList(getActivity());
        ArrayAdapter<UserDictionaryAddWordContents.LocaleRenderer> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, localesList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (this.mIsDeleting) {
            return;
        }
        this.mContents.apply(getActivity(), null);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        UserDictionaryAddWordContents.LocaleRenderer locale = (UserDictionaryAddWordContents.LocaleRenderer) parent.getItemAtPosition(pos);
        if (locale.isMoreLanguages()) {
            ((SettingsActivity) getActivity()).startPreferenceFragment(new UserDictionaryLocalePicker(this), true);
        } else {
            this.mContents.updateLocale(locale.getLocaleString());
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Bundle args = getArguments();
        this.mContents.updateLocale(args.getString("locale"));
    }

    public void onLocaleSelected(Locale locale) {
        this.mContents.updateLocale(locale.toString());
        getActivity().onBackPressed();
    }
}
