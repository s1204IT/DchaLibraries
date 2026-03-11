package com.mediatek.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.nfc.AndroidBeam;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.widget.SwitchBar;
import com.mediatek.nfcsettingsadapter.NfcSettingsAdapter;
import com.mediatek.settings.FeatureOption;
import java.util.ArrayList;
import java.util.List;

public class NfcSettings extends SettingsPreferenceFragment implements Indexable {
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean z) {
            ArrayList arrayList = new ArrayList();
            Resources resources = context.getResources();
            if (NfcAdapter.getDefaultAdapter(context) != null && FeatureOption.MTK_NFC_ADDON_SUPPORT) {
                Log.d("@M_NfcSettings", "SearchIndexProvider add NFC");
                SearchIndexableRaw searchIndexableRaw = new SearchIndexableRaw(context);
                searchIndexableRaw.title = resources.getString(R.string.nfc_quick_toggle_title);
                searchIndexableRaw.screenTitle = resources.getString(R.string.nfc_quick_toggle_title);
                searchIndexableRaw.keywords = resources.getString(R.string.nfc_quick_toggle_title);
                arrayList.add(searchIndexableRaw);
            } else {
                Log.d("@M_NfcSettings", "NfcAdapter is null or it is default NFC");
            }
            return arrayList;
        }
    };
    private SettingsActivity mActivity;
    private Preference mAndroidBeam;
    private Preference mCardEmulationPref;
    private IntentFilter mIntentFilter;
    private NfcAdapter mNfcAdapter;
    private MtkNfcEnabler mNfcEnabler;
    private SwitchPreference mNfcP2pModePref;
    private SwitchPreference mNfcRwTagPref;
    private NfcServiceHelper mNfcServiceHelper;
    private Preference mNfcServiceStatusPref;
    private NfcSettingsAdapter mNfcSettingsAdapter;
    private SwitchPreference mNfcTapPayPref;
    private SwitchBar mSwitchBar;
    private boolean mCardEmulationExist = true;
    private boolean mNfcBeamOpen = false;
    private int mNfcState = 1;
    private QueryTask mQueryTask = null;
    private String EMULATION_OFF = null;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.nfc.action.ADAPTER_STATE_CHANGED".equals(action)) {
                Log.d("@M_NfcSettings", "Receive nfc change " + intent.getIntExtra("android.nfc.extra.ADAPTER_STATE", 1));
                if (NfcSettings.this.mNfcAdapter != null) {
                    NfcSettings.this.mQueryTask = new QueryTask();
                    NfcSettings.this.mQueryTask.execute(new Void[0]);
                    return;
                }
                return;
            }
            if ("com.android.nfc_extras.action.RF_FIELD_ON_DETECTED".equals(action)) {
                NfcSettings.this.getPreferenceScreen().setEnabled(false);
                Log.d("@M_NfcSettings", "Receive broadcast: RF field on detected");
            } else if ("com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED".equals(action)) {
                NfcSettings.this.getPreferenceScreen().setEnabled(true);
                Log.d("@M_NfcSettings", "Receive broadcast: RF field off detected");
            }
        }
    };
    private final ContentObserver mActiveCardModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean z, Uri uri) {
            Log.d("@M_NfcSettings", "mActiveCardModeObserver, onChange()");
            if (NfcSettings.this.mCardEmulationExist) {
                String string = Settings.Global.getString(NfcSettings.this.getContentResolver(), "nfc_multise_active");
                Log.d("@M_NfcSettings", "updatePreferences, active mode is " + string + " EMULATION_OFF is " + NfcSettings.this.EMULATION_OFF);
                if (NfcSettings.this.EMULATION_OFF == null || !NfcSettings.this.EMULATION_OFF.equals(string)) {
                    NfcSettings.this.mCardEmulationPref.setSummary(string);
                } else {
                    NfcSettings.this.mCardEmulationPref.setSummary(R.string.android_beam_off_summary);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.nfc_settings);
        this.mActivity = (SettingsActivity) getActivity();
        this.mNfcAdapter = NfcAdapter.getDefaultAdapter(this.mActivity);
        if (this.mNfcAdapter == null) {
            Log.d("@M_NfcSettings", "Nfc adapter is null, finish Nfc settings");
            getActivity().finish();
            return;
        }
        if (FeatureOption.MTK_NFC_ADDON_SUPPORT) {
            this.mNfcSettingsAdapter = NfcSettingsAdapter.getDefaultAdapter(this.mActivity);
        }
        this.mIntentFilter = new IntentFilter("android.nfc.action.ADAPTER_STATE_CHANGED");
        this.mIntentFilter.addAction("com.android.nfc_extras.action.RF_FIELD_ON_DETECTED");
        this.mIntentFilter.addAction("com.android.nfc_extras.action.RF_FIELD_OFF_DETECTED");
        initPreferences();
    }

    @Override
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
        Log.d("@M_NfcSettings", "onActivityCreated() ");
        this.mSwitchBar = this.mActivity.getSwitchBar();
        this.mNfcEnabler = new MtkNfcEnabler(this.mActivity, this.mSwitchBar, this.mNfcAdapter);
    }

    @Override
    public void onDestroyView() {
        Log.d("@M_NfcSettings", "onDestroyView, mSwitchBar removeOnSwitchChangeListener");
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.teardownSwitchBar();
        }
        super.onDestroyView();
    }

    private void initPreferences() {
        this.mNfcP2pModePref = (SwitchPreference) findPreference("nfc_p2p_mode");
        this.mAndroidBeam = findPreference("nfc_android_beam");
        this.mNfcRwTagPref = (SwitchPreference) findPreference("nfc_rw_tag");
        this.mCardEmulationPref = findPreference("nfc_card_emulation");
        PreferenceCategory preferenceCategory = (PreferenceCategory) findPreference("nfc_card_emulation_category");
        int i = Settings.Global.getInt(getContentResolver(), "nfc_multise_on", 0);
        Log.d("@M_NfcSettings", "NFC_MULTISE_ON is " + i);
        if (UserHandle.myUserId() != 0 || (preferenceCategory != null && i == 0)) {
            getPreferenceScreen().removePreference(preferenceCategory);
            this.mCardEmulationExist = false;
        } else {
            getEmulationOffConstant();
        }
        this.mNfcTapPayPref = (SwitchPreference) findPreference("nfc_hce_pay");
        if (this.mNfcSettingsAdapter.isShowOverflowMenu()) {
            addNfcServiceStatusPref();
        }
    }

    private void addNfcServiceStatusPref() {
        this.mNfcServiceHelper = new NfcServiceHelper(this.mActivity);
        this.mNfcServiceStatusPref = new Preference(getPreferenceScreen().getPreferenceManager().getContext());
        this.mNfcServiceStatusPref.setTitle(getString(R.string.nfc_service_status_title));
        this.mNfcServiceStatusPref.setFragment(NfcServiceStatus.class.getName());
        getPreferenceScreen().addPreference(this.mNfcServiceStatusPref);
    }

    public void updatePreferenceEnabledStatus(int i) {
        Log.d("@M_NfcSettings", "updatePreferenceEnabledStatus nfc state :" + i);
        if (i == 3) {
            this.mNfcP2pModePref.setEnabled(true);
            this.mNfcRwTagPref.setEnabled(true);
            if (!this.mNfcBeamOpen) {
                this.mAndroidBeam.setSummary(R.string.android_beam_off_summary);
            } else {
                this.mAndroidBeam.setSummary(R.string.android_beam_on_summary);
            }
            if (this.mCardEmulationExist) {
                this.mCardEmulationPref.setEnabled(true);
            }
            this.mNfcTapPayPref.setEnabled(true);
            return;
        }
        this.mNfcP2pModePref.setEnabled(false);
        this.mNfcRwTagPref.setEnabled(false);
        this.mAndroidBeam.setSummary(R.string.android_beam_off_summary);
        if (this.mCardEmulationExist) {
            this.mCardEmulationPref.setEnabled(false);
        }
        this.mNfcTapPayPref.setEnabled(false);
    }

    private void updatePreferences() {
        Log.d("@M_NfcSettings", "updatePreferences");
        updatePreferenceEnabledStatus(this.mNfcState);
        this.mNfcP2pModePref.setChecked(this.mNfcSettingsAdapter.getModeFlag(4) == 1);
        this.mNfcRwTagPref.setChecked(this.mNfcSettingsAdapter.getModeFlag(1) == 1);
        if (this.mCardEmulationExist) {
            String string = Settings.Global.getString(getContentResolver(), "nfc_multise_active");
            Log.d("@M_NfcSettings", "updatePreferences, active mode is " + string + " EMULATION_OFF is " + this.EMULATION_OFF);
            if (this.EMULATION_OFF != null && this.EMULATION_OFF.equals(string)) {
                this.mCardEmulationPref.setSummary(R.string.android_beam_off_summary);
            } else {
                this.mCardEmulationPref.setSummary(string);
            }
        }
        if (1 == Settings.Global.getInt(getContentResolver(), "nfc_hce_on", 0)) {
            this.mNfcTapPayPref.setChecked(true);
        } else {
            this.mNfcTapPayPref.setChecked(false);
        }
        int i = Settings.Global.getInt(getContentResolver(), "nfc_rf_field_active", 0);
        getPreferenceScreen().setEnabled(i == 0);
        Log.d("@M_NfcSettings", "Read the value Global.NFC_RF_FIELD_ACTIVE : " + i);
        if (this.mNfcServiceStatusPref != null) {
            this.mNfcServiceHelper.initServiceList();
            this.mNfcServiceStatusPref.setSummary(getString(R.string.nfc_service_status_summary, new Object[]{Integer.valueOf(this.mNfcServiceHelper.getSelectServiceCount()), Integer.valueOf(this.mNfcServiceHelper.getAllServiceCount())}));
        }
    }

    private void getEmulationOffConstant() {
        String string = Settings.Global.getString(getContentResolver(), "nfc_multise_list");
        if (string == null) {
            this.EMULATION_OFF = "Off";
            return;
        }
        String[] strArrSplit = string.split("[,]");
        int length = strArrSplit.length;
        if (this.EMULATION_OFF == null) {
            this.EMULATION_OFF = strArrSplit[length - 1];
            Log.d("@M_NfcSettings", "NFC_MULTISE_LIST is" + string + ", EMULATION_OFF is " + this.EMULATION_OFF);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference.equals(this.mAndroidBeam)) {
            new SubSettingLauncher(getContext()).setDestination(AndroidBeam.class.getCanonicalName()).setTitle(R.string.android_beam_settings_title).setSourceMetricsCategory(getMetricsCategory()).setArguments(null).launch();
        } else {
            if (preference.equals(this.mNfcP2pModePref)) {
                Log.d("@M_NfcSettings", "p2p mode");
                this.mNfcSettingsAdapter.setModeFlag(4, this.mNfcP2pModePref.isChecked() ? 1 : 0);
            } else if (preference.equals(this.mNfcRwTagPref)) {
                Log.d("@M_NfcSettings", "tag rw mode");
                this.mNfcSettingsAdapter.setModeFlag(1, this.mNfcRwTagPref.isChecked() ? 1 : 0);
            } else if (preference.equals(this.mCardEmulationPref)) {
                Log.d("@M_NfcSettings", "card emulation mode");
                new SubSettingLauncher(getContext()).setDestination(CardEmulationSettings.class.getCanonicalName()).setTitle(R.string.nfc_card_emulation).setSourceMetricsCategory(getMetricsCategory()).setArguments(null).launch();
            } else if (preference.equals(this.mNfcTapPayPref)) {
                boolean zIsChecked = this.mNfcTapPayPref.isChecked();
                Log.d("@M_NfcSettings", "pay tap " + zIsChecked);
                Settings.Global.putInt(getContentResolver(), "nfc_hce_on", zIsChecked ? 1 : 0);
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d("@M_NfcSettings", "onResume ");
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.resume();
        }
        if (this.mNfcAdapter != null) {
            this.mQueryTask = new QueryTask();
            this.mQueryTask.execute(new Void[0]);
        }
        getActivity().registerReceiver(this.mReceiver, this.mIntentFilter);
        getContentResolver().registerContentObserver(Settings.Global.getUriFor("nfc_multise_active"), false, this.mActiveCardModeObserver);
        updatePreferences();
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d("@M_NfcSettings", "onPause rm observer ");
        getContentResolver().unregisterContentObserver(this.mActiveCardModeObserver);
        if (this.mQueryTask != null) {
            this.mQueryTask.cancel(true);
            Log.d("@M_NfcSettings", "mQueryTask.cancel(true)");
        }
        getActivity().unregisterReceiver(this.mReceiver);
        if (this.mNfcEnabler != null) {
            this.mNfcEnabler.pause();
        }
    }

    private class QueryTask extends AsyncTask<Void, Void, Integer> {
        private QueryTask() {
        }

        @Override
        public Integer doInBackground(Void... voidArr) {
            NfcSettings.this.mNfcState = NfcSettings.this.mNfcAdapter.getAdapterState();
            NfcSettings.this.mNfcBeamOpen = NfcSettings.this.mNfcAdapter.isNdefPushEnabled();
            Log.d("@M_NfcSettings", "doInBackground  mNfcState: " + NfcSettings.this.mNfcState);
            Log.d("@M_NfcSettings", "doInBackground  mNfcBeamOpen: " + NfcSettings.this.mNfcBeamOpen);
            return Integer.valueOf(NfcSettings.this.mNfcState);
        }

        @Override
        public void onPostExecute(Integer num) {
            Log.d("@M_NfcSettings", "onPostExecute");
            NfcSettings.this.updatePreferenceEnabledStatus(num.intValue());
        }
    }

    @Override
    public int getMetricsCategory() {
        return 70;
    }
}
