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
import java.util.Iterator;
import java.util.List;

public class CardEmulationSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, SwitchBar.OnSwitchChangeListener {
    private static final String CATEGORY_KEY = "card_emulation_settings_category";
    private static final String DEFAULT_MODE = "SIM1";
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
            } else if ("com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED".equals(action)) {
                CardEmulationSettings.this.getPreferenceScreen().setEnabled(true);
                Log.d("@M_CardEmulationSettings", "Receive broadcast: RF field off detected");
            }
        }
    };
    private final ContentObserver mActiveCardModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z, Uri uri) {
            Log.d("@M_CardEmulationSettings", "mActiveCardModeObserver, onChange()");
            CardEmulationSettings.this.updatePreferences();
        }
    };
    private final ContentObserver mCardModeListObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z, Uri uri) {
            Log.d("@M_CardEmulationSettings", "mCardModeListObserver, onChange()");
            CardEmulationSettings.this.updatePreferences();
        }
    };
    private final ContentObserver mCardtransactionObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z, Uri uri) {
            Log.d("@M_CardEmulationSettings", "mCardtransactionObserver, onChange()");
            CardEmulationSettings.this.updatePreferences();
        }
    };
    private final ContentObserver mCardSwitchingObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z, Uri uri) {
            Log.d("@M_CardEmulationSettings", "mCardSwitchingObserver, onChange()");
            CardEmulationSettings.this.updatePreferences();
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.card_emulation_settings);
        this.mProgressCategory = (CardEmulationProgressCategory) findPreference(CATEGORY_KEY);
        getCardEmulationList();
        this.mIntentFilter = new IntentFilter();
        this.mIntentFilter.addAction("com.android.nfc_extras.action.RF_FIELD_ON_DETECTED");
        this.mIntentFilter.addAction("com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED");
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        this.mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        setEmptyView(this.mEmptyView);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("@M_CardEmulationSettings", "onCreate, mSwitchBar addOnSwitchChangeListener ");
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
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
    public void onSwitchChanged(Switch r4, boolean z) {
        Log.d("@M_CardEmulationSettings", "onCheckedChanged, desiredState " + z + " mUpdateStatusOnly " + this.mUpdateStatusOnly);
        if (this.mUpdateStatusOnly) {
            return;
        }
        if (!z) {
            Settings.Global.putString(getContentResolver(), "nfc_multise_active", this.EMULATION_OFF);
            Log.d("@M_CardEmulationSettings", "onCheckedChanged, set Settings.Global.NFC_MULTISE_ACTIVE EMULATION_OFF" + this.EMULATION_OFF);
        } else {
            String string = Settings.Global.getString(getContentResolver(), "nfc_multise_previous");
            if (string == null) {
                String[] cardEmulationList = getCardEmulationList();
                string = cardEmulationList.length > 0 ? cardEmulationList[0] : DEFAULT_MODE;
            }
            Settings.Global.putString(getContentResolver(), "nfc_multise_active", string);
            Log.d("@M_CardEmulationSettings", "onCheckedChanged, set active mode to " + string);
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

    private void updatePreferences() {
        removeAll();
        String string = Settings.Global.getString(getContentResolver(), "nfc_multise_active");
        String string2 = Settings.Global.getString(getContentResolver(), "nfc_multise_previous");
        int i = Settings.Global.getInt(getContentResolver(), "nfc_multise_in_transation", 0);
        int i2 = Settings.Global.getInt(getContentResolver(), "nfc_multise_in_switching", 0);
        Log.d("@M_CardEmulationSettings", "updatePreferences(),EMULATION_OFF " + this.EMULATION_OFF + ", active mode: " + string + ", previous mode is " + string2);
        StringBuilder sb = new StringBuilder();
        sb.append("updatePreferences, transactionStatus is ");
        sb.append(i);
        sb.append(" switchingStatus is ");
        sb.append(i2);
        Log.d("@M_CardEmulationSettings", sb.toString());
        if (this.EMULATION_OFF.equals(string)) {
            this.mUpdateStatusOnly = true;
            this.mSwitchBar.setChecked(false);
            this.mUpdateStatusOnly = false;
            if (getCardEmulationList().length == 0) {
                Log.d("@M_CardEmulationSettings", "no available security elment found and the active mode is off");
                this.mEmptyView.setText(R.string.card_emulation_settings_no_element_found);
            } else if (i2 == 0) {
                this.mEmptyView.setText(R.string.card_emulation_settings_off_text);
            } else {
                this.mEmptyView.setText(R.string.card_emulation_turning_off_text);
            }
            this.mSwitchBar.setEnabled(i == 0 && i2 == 0);
        } else {
            this.mUpdateStatusOnly = true;
            this.mSwitchBar.setChecked(true);
            this.mUpdateStatusOnly = false;
            if (i2 == 1 && this.EMULATION_OFF.equals(string2)) {
                this.mSwitchBar.setEnabled(false);
                this.mEmptyView.setText(R.string.card_emulation_turning_on_text);
            } else {
                this.mSwitchBar.setEnabled(i == 0 && i2 == 0);
                addItemPreference();
                this.mProgressCategory.getPreferenceCount();
                getPreferenceScreen().addPreference(this.mProgressCategory);
                SecurityItemPreference securityItemPreference = (SecurityItemPreference) findPreference(string);
                if (securityItemPreference != null) {
                    securityItemPreference.setChecked(true);
                    this.mActivePref = securityItemPreference;
                } else {
                    Log.d("@M_CardEmulationSettings", "Activie mode is " + string + ", can not find it on screen");
                }
                this.mProgressCategory.setProgress(i2 == 1);
                this.mProgressCategory.setEnabled(i == 0 && i2 == 0);
            }
        }
        int i3 = Settings.Global.getInt(getContentResolver(), "nfc_rf_field_active", 0);
        getPreferenceScreen().setEnabled(i3 == 0);
        Log.d("@M_CardEmulationSettings", "Read the value Global.NFC_RF_FIELD_ACTIVE : " + i3);
    }

    private void addItemPreference() {
        String[] cardEmulationList = getCardEmulationList();
        if (cardEmulationList != null) {
            for (String str : cardEmulationList) {
                SecurityItemPreference securityItemPreference = new SecurityItemPreference(getActivity());
                securityItemPreference.setTitle(str);
                securityItemPreference.setKey(str);
                securityItemPreference.setOnPreferenceChangeListener(this);
                this.mProgressCategory.addPreference(securityItemPreference);
                this.mItemPreferences.add(securityItemPreference);
                this.mItemKeys.add(str);
            }
        }
    }

    private String[] getCardEmulationList() {
        String[] strArrSplit = Settings.Global.getString(getContentResolver(), "nfc_multise_list").split("[,]");
        int length = strArrSplit.length;
        Log.d("@M_CardEmulationSettings", "getCardEmulationList, length = " + length);
        if (this.EMULATION_OFF == null) {
            this.EMULATION_OFF = strArrSplit[length - 1];
            Log.d("@M_CardEmulationSettings", "EMULATION_OFF is " + this.EMULATION_OFF);
        }
        String[] strArr = new String[length - 1];
        if (strArrSplit != null) {
            for (int i = 0; i < strArrSplit.length - 1; i++) {
                strArr[i] = strArrSplit[i];
                Log.d("@M_CardEmulationSettings", "emulation list item is " + strArr[i]);
            }
        }
        return strArr;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (preference == null || !(preference instanceof SecurityItemPreference)) {
            return false;
        }
        Log.d("@M_CardEmulationSettings", "onPreferenceChange, select " + preference.getKey() + " active");
        Settings.Global.putString(getContentResolver(), "nfc_multise_active", preference.getKey());
        this.mProgressCategory.setProgress(true);
        this.mSwitchBar.setEnabled(false);
        Iterator<SecurityItemPreference> it = this.mItemPreferences.iterator();
        while (it.hasNext()) {
            it.next().setEnabled(false);
        }
        return true;
    }

    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference != null && (preference instanceof SecurityItemPreference)) {
            Log.d("@M_CardEmulationSettings", "onPreferenceTreeClick " + preference.getKey());
            String string = Settings.Global.getString(getContentResolver(), "nfc_multise_active");
            String key = preference.getKey();
            if (key != null && !key.equals(string)) {
                Settings.Global.putString(getContentResolver(), "nfc_multise_active", preference.getKey());
                this.mProgressCategory.setProgress(true);
                this.mSwitchBar.setEnabled(false);
                Iterator<SecurityItemPreference> it = this.mItemPreferences.iterator();
                while (it.hasNext()) {
                    it.next().setEnabled(false);
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
    public int getMetricsCategory() {
        return 70;
    }
}
