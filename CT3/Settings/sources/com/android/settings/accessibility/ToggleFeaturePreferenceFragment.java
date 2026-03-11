package com.android.settings.accessibility;

import android.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.TextView;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;

public abstract class ToggleFeaturePreferenceFragment extends SettingsPreferenceFragment {
    protected String mPreferenceKey;
    protected Intent mSettingsIntent;
    protected CharSequence mSettingsTitle;
    protected Preference mSummaryPreference;
    protected SwitchBar mSwitchBar;
    protected ToggleSwitch mToggleSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getActivity());
        setPreferenceScreen(preferenceScreen);
        this.mSummaryPreference = new Preference(getPrefContext()) {
            @Override
            public void onBindViewHolder(PreferenceViewHolder view) {
                super.onBindViewHolder(view);
                view.setDividerAllowedAbove(false);
                view.setDividerAllowedBelow(false);
                TextView summaryView = (TextView) view.findViewById(R.id.summary);
                summaryView.setText(getSummary());
                sendAccessibilityEvent(summaryView);
            }

            private void sendAccessibilityEvent(View view) {
                AccessibilityManager accessibilityManager = AccessibilityManager.getInstance(ToggleFeaturePreferenceFragment.this.getActivity());
                if (!accessibilityManager.isEnabled()) {
                    return;
                }
                AccessibilityEvent event = AccessibilityEvent.obtain();
                event.setEventType(8);
                view.onInitializeAccessibilityEvent(event);
                view.dispatchPopulateAccessibilityEvent(event);
                accessibilityManager.sendAccessibilityEvent(event);
            }
        };
        this.mSummaryPreference.setSelectable(false);
        this.mSummaryPreference.setPersistent(false);
        this.mSummaryPreference.setLayoutResource(com.android.settings.R.layout.text_description_preference);
        preferenceScreen.addPreference(this.mSummaryPreference);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        this.mToggleSwitch = this.mSwitchBar.getSwitch();
        onProcessArguments(getArguments());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        installActionBarToggleSwitch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        removeActionBarToggleSwitch();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (this.mSettingsTitle == null || this.mSettingsIntent == null) {
            return;
        }
        MenuItem menuItem = menu.add(this.mSettingsTitle);
        menuItem.setShowAsAction(1);
        menuItem.setIntent(this.mSettingsIntent);
    }

    protected void onInstallSwitchBarToggleSwitch() {
    }

    protected void onRemoveSwitchBarToggleSwitch() {
    }

    private void installActionBarToggleSwitch() {
        this.mSwitchBar.show();
        onInstallSwitchBarToggleSwitch();
    }

    private void removeActionBarToggleSwitch() {
        this.mToggleSwitch.setOnBeforeCheckedChangeListener(null);
        onRemoveSwitchBarToggleSwitch();
        this.mSwitchBar.hide();
    }

    public void setTitle(String title) {
        getActivity().setTitle(title);
    }

    protected void onProcessArguments(Bundle arguments) {
        if (arguments == null) {
            getPreferenceScreen().removePreference(this.mSummaryPreference);
            return;
        }
        this.mPreferenceKey = arguments.getString("preference_key");
        if (arguments.containsKey("checked")) {
            boolean enabled = arguments.getBoolean("checked");
            this.mSwitchBar.setCheckedInternal(enabled);
        }
        if (arguments.containsKey("title")) {
            setTitle(arguments.getString("title"));
        }
        if (arguments.containsKey("summary")) {
            CharSequence summary = arguments.getCharSequence("summary");
            this.mSummaryPreference.setSummary(summary);
        } else {
            getPreferenceScreen().removePreference(this.mSummaryPreference);
        }
    }
}
