package com.mediatek.nfc;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.mediatek.nfcsettingsadapter.ServiceEntry;
import java.util.Iterator;
import java.util.List;

/* loaded from: classes.dex */
public class NfcServiceStatus extends SettingsPreferenceFragment implements Preference.OnPreferenceClickListener {
    private Context mContext;
    private boolean mEditMode;
    private Menu mMenu;
    private NfcServiceHelper mNfcServiceHelper;

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        this.mContext = getActivity();
        this.mNfcServiceHelper = new NfcServiceHelper(this.mContext);
        addPreferencesFromResource(R.xml.nfc_service_status);
        setHasOptionsMenu(true);
    }

    @Override // com.android.settingslib.core.instrumentation.Instrumentable
    public int getMetricsCategory() {
        return 70;
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settings.core.InstrumentedPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onResume() {
        super.onResume();
        refreshUi(true);
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        MenuItem menuItemAdd = menu.add(0, 2, 0, (CharSequence) null);
        menuItemAdd.setShowAsAction(2);
        menuItemAdd.setIcon(R.drawable.ic_edit);
        menu.add(0, 3, 0, R.string.okay).setShowAsAction(2);
        super.onCreateOptionsMenu(menu, menuInflater);
        this.mMenu = menu;
        updateVisibilityOfMenu();
    }

    private void updateVisibilityOfMenu() {
        if (this.mMenu == null) {
            return;
        }
        MenuItem menuItemFindItem = this.mMenu.findItem(2);
        MenuItem menuItemFindItem2 = this.mMenu.findItem(3);
        if (menuItemFindItem != null && menuItemFindItem2 != null) {
            menuItemFindItem.setVisible(!this.mEditMode);
            menuItemFindItem2.setVisible(this.mEditMode);
        }
    }

    @Override // com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.app.Fragment
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        int itemId = menuItem.getItemId();
        if (itemId != 16908332) {
            switch (itemId) {
                case 2:
                    setEditMode(true);
                    refreshUi(false);
                    break;
                case 3:
                    this.mNfcServiceHelper.saveChange();
                    setEditMode(false);
                    refreshUi(false);
                    break;
            }
            return true;
        }
        if (this.mEditMode) {
            setEditMode(false);
            refreshUi(false);
            return true;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    private void setEditMode(boolean z) {
        this.mEditMode = z;
        updateVisibilityOfMenu();
        this.mNfcServiceHelper.setEditMode(z);
    }

    private void refreshUi(boolean z) {
        Log.d("NfcServiceStatus", "refreshUi, mEditMode = " + this.mEditMode + ", needRestore = " + z);
        getPreferenceScreen().removeAll();
        this.mNfcServiceHelper.initServiceList();
        this.mNfcServiceHelper.sortList();
        if (z) {
            this.mNfcServiceHelper.restoreCheckedState();
        }
        initPreferences(this.mNfcServiceHelper.getServiceList());
    }

    private void initPreferences(List<ServiceEntry> list) {
        Iterator<ServiceEntry> it = list.iterator();
        while (it.hasNext()) {
            getPreferenceScreen().addPreference(createPreference(it.next()));
        }
    }

    private NfcServicePreference createPreference(ServiceEntry serviceEntry) {
        NfcServicePreference nfcServicePreference = new NfcServicePreference(this.mContext, serviceEntry);
        if (this.mEditMode) {
            nfcServicePreference.setOnPreferenceClickListener(this);
            nfcServicePreference.setShowCheckbox(true);
        } else if (serviceEntry.getWasEnabled().booleanValue()) {
            nfcServicePreference.setEnabled(true);
            nfcServicePreference.setSummary(R.string.nfc_service_summary_enabled);
        } else {
            nfcServicePreference.setEnabled(false);
            nfcServicePreference.setSummary(R.string.nfc_service_summary_disabled);
        }
        return nfcServicePreference;
    }

    @Override // android.support.v7.preference.Preference.OnPreferenceClickListener
    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof NfcServicePreference) {
            NfcServicePreference nfcServicePreference = (NfcServicePreference) preference;
            boolean zIsChecked = nfcServicePreference.isChecked();
            Log.d("NfcServiceStatus", "onPreferenceClick, isChecked =" + zIsChecked);
            if (this.mNfcServiceHelper.setEnabled(nfcServicePreference, !zIsChecked)) {
                nfcServicePreference.setChecked(!zIsChecked);
            } else {
                Toast.makeText(this.mContext, R.string.nfc_service_overflow, 0).show();
            }
        }
        return false;
    }

    @Override // android.app.Fragment
    public void onViewStateRestored(Bundle bundle) {
        super.onViewStateRestored(bundle);
        if (bundle != null) {
            this.mEditMode = bundle.getBoolean("nfcEditMode", false);
            Log.d("NfcServiceStatus", "onViewStateRestored mEditMode = " + this.mEditMode);
            setEditMode(this.mEditMode);
            this.mNfcServiceHelper.restoreState(bundle);
        }
    }

    @Override // com.android.settings.SettingsPreferenceFragment, com.android.settingslib.core.lifecycle.ObservablePreferenceFragment, android.support.v14.preference.PreferenceFragment, android.app.Fragment
    public void onSaveInstanceState(Bundle bundle) {
        super.onSaveInstanceState(bundle);
        Log.d("NfcServiceStatus", "onSaveInstanceState, mEditMode = " + this.mEditMode);
        bundle.putBoolean("nfcEditMode", this.mEditMode);
        this.mNfcServiceHelper.saveState(bundle);
    }
}
