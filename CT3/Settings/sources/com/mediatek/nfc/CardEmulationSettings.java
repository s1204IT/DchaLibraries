package com.mediatek.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import java.util.ArrayList;
import java.util.List;

public class CardEmulationSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener {
    private static final String CATEGORY_KEY = "card_emulation_settings_category";
    private static final String TAG = "CardEmulationSettings";
    private SecurityItemPreference mActivePref;
    private TextView mEmptyView;
    private IntentFilter mIntentFilter;
    private CardEmulationProgressCategory mProgressCategory;
    private SwitchBar mSwitchBar;
    private String EMULATION_OFF = null;
    private final List<SecurityItemPreference> mItemPreferences = new ArrayList();
    private final List<String> mItemKeys = new ArrayList();
    private boolean mUpdateStatusOnly = false;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.android.nfc_extras.action.RF_FIELD_ON_DETECTED".equals(action)) {
                CardEmulationSettings.this.getPreferenceScreen().setEnabled(false);
                Log.d("@M_CardEmulationSettings", "Receive broadcast: RF field on detected");
            } else {
                if (!"com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED".equals(action)) {
                    return;
                }
                CardEmulationSettings.this.getPreferenceScreen().setEnabled(true);
                Log.d("@M_CardEmulationSettings", "Receive broadcast: RF field off detected");
            }
        }
    };
    private final ContentObserver mActiveCardModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d("@M_CardEmulationSettings", "mActiveCardModeObserver, onChange()");
            CardEmulationSettings.this.updatePreferences();
        }
    };
    private final ContentObserver mCardModeListObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d("@M_CardEmulationSettings", "mCardModeListObserver, onChange()");
            CardEmulationSettings.this.updatePreferences();
        }
    };
    private final ContentObserver mCardtransactionObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d("@M_CardEmulationSettings", "mCardtransactionObserver, onChange()");
            CardEmulationSettings.this.updatePreferences();
        }
    };
    private final ContentObserver mCardSwitchingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            Log.d("@M_CardEmulationSettings", "mCardSwitchingObserver, onChange()");
            CardEmulationSettings.this.updatePreferences();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.card_emulation_settings);
        this.mProgressCategory = (CardEmulationProgressCategory) findPreference(CATEGORY_KEY);
        getCardEmulationList();
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("com.android.nfc_extras.action.RF_FIELD_ON_DETECTED");
        this.mIntentFilter.addAction("com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED");
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        setEmptyView(this.mEmptyView);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("@M_CardEmulationSettings", "onCreate, mSwitchBar addOnSwitchChangeListener ");
        SettingsActivity activity = (SettingsActivity) getActivity();
        this.mSwitchBar = activity.getSwitchBar();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        this.mSwitchBar.show();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d("@M_CardEmulationSettings", "onStop, mSwitchBar removeOnSwitchChangeListener ");
        this.mSwitchBar.removeOnSwitchChangeListener(this);
        this.mSwitchBar.hide();
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean desiredState) {
        Log.d("@M_CardEmulationSettings", "onCheckedChanged, desiredState " + desiredState + " mUpdateStatusOnly " + this.mUpdateStatusOnly);
        if (this.mUpdateStatusOnly) {
            return;
        }
        if (!desiredState) {
            Settings.Global.putString(getContentResolver(), "nfc_multise_active", this.EMULATION_OFF);
            Log.d("@M_CardEmulationSettings", "onCheckedChanged, set Settings.Global.NFC_MULTISE_ACTIVE EMULATION_OFF" + this.EMULATION_OFF);
        } else {
            String previousMode = Settings.Global.getString(getContentResolver(), "nfc_multise_previous");
            Settings.Global.putString(getContentResolver(), "nfc_multise_active", previousMode);
            Log.d("@M_CardEmulationSettings", "onCheckedChanged, set active mode to " + previousMode);
        }
        this.mSwitchBar.setEnabled(false);
    }

    private void removeAll() {
        this.mProgressCategory.removeAll();
        getPreferenceScreen().removeAll();
        this.mProgressCategory.setProgress(false);
        this.mItemPreferences.clear();
        this.mItemKeys.clear();
    }

    public void updatePreferences() {
        removeAll();
        String activeMode = Settings.Global.getString(getContentResolver(), "nfc_multise_active");
        String previousMode = Settings.Global.getString(getContentResolver(), "nfc_multise_previous");
        int transactionStatus = Settings.Global.getInt(getContentResolver(), "nfc_multise_in_transation", 0);
        int switchingStatus = Settings.Global.getInt(getContentResolver(), "nfc_multise_in_switching", 0);
        Log.d("@M_CardEmulationSettings", "updatePreferences(),EMULATION_OFF " + this.EMULATION_OFF + ", active mode: " + activeMode + ", previous mode is " + previousMode);
        Log.d("@M_CardEmulationSettings", "updatePreferences, transactionStatus is " + transactionStatus + " switchingStatus is " + switchingStatus);
        if (this.EMULATION_OFF.equals(activeMode)) {
            this.mUpdateStatusOnly = true;
            this.mSwitchBar.setChecked(false);
            this.mUpdateStatusOnly = false;
            if (getCardEmulationList().length == 0) {
                Log.d("@M_CardEmulationSettings", "no available security elment found and the active mode is off");
                this.mEmptyView.setText(R.string.card_emulation_settings_no_element_found);
            } else if (switchingStatus == 0) {
                this.mEmptyView.setText(R.string.card_emulation_settings_off_text);
            } else {
                this.mEmptyView.setText(R.string.card_emulation_turning_off_text);
            }
            this.mSwitchBar.setEnabled(transactionStatus == 0 && switchingStatus == 0);
        } else {
            this.mUpdateStatusOnly = true;
            this.mSwitchBar.setChecked(true);
            this.mUpdateStatusOnly = false;
            if (switchingStatus == 1 && this.EMULATION_OFF.equals(previousMode)) {
                this.mSwitchBar.setEnabled(false);
                this.mEmptyView.setText(R.string.card_emulation_turning_on_text);
            } else {
                this.mSwitchBar.setEnabled(transactionStatus == 0 && switchingStatus == 0);
                addItemPreference();
                this.mProgressCategory.getPreferenceCount();
                getPreferenceScreen().addPreference(this.mProgressCategory);
                SecurityItemPreference itemPref = (SecurityItemPreference) findPreference(activeMode);
                if (itemPref != null) {
                    itemPref.setChecked(true);
                    this.mActivePref = itemPref;
                } else {
                    Log.d("@M_CardEmulationSettings", "Activie mode is " + activeMode + ", can not find it on screen");
                }
                this.mProgressCategory.setProgress(switchingStatus == 1);
                this.mProgressCategory.setEnabled(transactionStatus == 0 && switchingStatus == 0);
            }
        }
        int fieldActive = Settings.Global.getInt(getContentResolver(), "nfc_rf_field_active", 0);
        getPreferenceScreen().setEnabled(fieldActive == 0);
        Log.d("@M_CardEmulationSettings", "Read the value Global.NFC_RF_FIELD_ACTIVE : " + fieldActive);
    }

    private void addItemPreference() {
        String[] list = getCardEmulationList();
        if (list == null) {
            return;
        }
        for (String key : list) {
            SecurityItemPreference pref = new SecurityItemPreference(getActivity());
            pref.setTitle(key);
            pref.setKey(key);
            pref.setOnPreferenceChangeListener(this);
            this.mProgressCategory.addPreference(pref);
            this.mItemPreferences.add(pref);
            this.mItemKeys.add(key);
        }
    }

    private String[] getCardEmulationList() {
        String list = Settings.Global.getString(getContentResolver(), "nfc_multise_list");
        String[] tokens = list.split("[,]");
        int length = tokens.length;
        if (this.EMULATION_OFF == null) {
            this.EMULATION_OFF = tokens[length - 1];
            Log.d("@M_CardEmulationSettings", "EMULATION_OFF is " + this.EMULATION_OFF);
        }
        String[] emulationList = new String[length - 1];
        if (tokens != null) {
            for (int i = 0; i < tokens.length - 1; i++) {
                emulationList[i] = tokens[i];
                Log.d("@M_CardEmulationSettings", "emulation list item is " + emulationList[i]);
            }
        }
        return emulationList;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == null || !(preference instanceof SecurityItemPreference)) {
            return false;
        }
        Log.d("@M_CardEmulationSettings", "onPreferenceChange, select " + preference.getKey() + " active");
        Settings.Global.putString(getContentResolver(), "nfc_multise_active", preference.getKey());
        this.mProgressCategory.setProgress(true);
        this.mSwitchBar.setEnabled(false);
        for (SecurityItemPreference pref : this.mItemPreferences) {
            pref.setEnabled(false);
        }
        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference != null && (preference instanceof SecurityItemPreference)) {
            Log.d("@M_CardEmulationSettings", "onPreferenceTreeClick " + preference.getKey());
            String activeMode = Settings.Global.getString(getContentResolver(), "nfc_multise_active");
            String prefKey = preference.getKey();
            if (prefKey != null && !prefKey.equals(activeMode)) {
                Settings.Global.putString(getContentResolver(), "nfc_multise_active", preference.getKey());
                this.mProgressCategory.setProgress(true);
                this.mSwitchBar.setEnabled(false);
                for (SecurityItemPreference pref : this.mItemPreferences) {
                    pref.setEnabled(false);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("nfc_multise_active"), false, this.mActiveCardModeObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("nfc_multise_list"), false, this.mCardModeListObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("nfc_multise_in_transation"), false, this.mCardtransactionObserver);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("nfc_multise_in_switching"), false, this.mCardSwitchingObserver);
        getActivity().registerReceiver(this.mReceiver, this.mIntentFilter);
        updatePreferences();
    }

    @Override
    public void onPause() {
        super.onPause();
        getContentResolver().unregisterContentObserver(this.mActiveCardModeObserver);
        getContentResolver().unregisterContentObserver(this.mCardModeListObserver);
        getContentResolver().unregisterContentObserver(this.mCardtransactionObserver);
        getContentResolver().unregisterContentObserver(this.mCardSwitchingObserver);
        getActivity().unregisterReceiver(this.mReceiver);
    }

    @Override
    protected int getMetricsCategory() {
        return 100004;
    }
}
