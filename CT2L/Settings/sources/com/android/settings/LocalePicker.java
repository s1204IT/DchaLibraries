package com.android.settings;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.android.internal.app.LocalePicker;
import com.android.settings.SettingsPreferenceFragment;
import java.util.Locale;

public class LocalePicker extends com.android.internal.app.LocalePicker implements LocalePicker.LocaleSelectionListener, DialogCreatable {
    private SettingsPreferenceFragment.SettingsDialogFragment mDialogFragment;
    private Locale mTargetLocale;

    public LocalePicker() {
        setLocaleSelectionListener(this);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null && savedInstanceState.containsKey("locale")) {
            this.mTargetLocale = new Locale(savedInstanceState.getString("locale"));
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        ListView list = (ListView) view.findViewById(android.R.id.list);
        Utils.forcePrepareCustomPreferencesList(container, view, list, false);
        return view;
    }

    public void onLocaleSelected(Locale locale) {
        if (Utils.hasMultipleUsers(getActivity())) {
            this.mTargetLocale = locale;
            showDialog(1);
        } else {
            getActivity().onBackPressed();
            updateLocale(locale);
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.mTargetLocale != null) {
            outState.putString("locale", this.mTargetLocale.toString());
        }
    }

    protected void showDialog(int dialogId) {
        if (this.mDialogFragment != null) {
            Log.e("LocalePicker", "Old dialog fragment not null!");
        }
        this.mDialogFragment = new SettingsPreferenceFragment.SettingsDialogFragment(this, dialogId);
        this.mDialogFragment.show(getActivity().getFragmentManager(), Integer.toString(dialogId));
    }

    @Override
    public Dialog onCreateDialog(final int dialogId) {
        return Utils.buildGlobalChangeWarningDialog(getActivity(), R.string.global_locale_change_title, new Runnable() {
            @Override
            public void run() {
                LocalePicker.this.removeDialog(dialogId);
                LocalePicker.this.getActivity().onBackPressed();
                LocalePicker.updateLocale(LocalePicker.this.mTargetLocale);
            }
        });
    }

    protected void removeDialog(int dialogId) {
        if (this.mDialogFragment != null && this.mDialogFragment.getDialogId() == dialogId) {
            this.mDialogFragment.dismiss();
        }
        this.mDialogFragment = null;
    }
}
