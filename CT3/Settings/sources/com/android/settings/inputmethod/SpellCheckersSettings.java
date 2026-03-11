package com.android.settings.inputmethod;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.SpellCheckerSubtype;
import android.view.textservice.TextServicesManager;
import android.widget.Switch;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;

public class SpellCheckersSettings extends SettingsPreferenceFragment implements SwitchBar.OnSwitchChangeListener, Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    private static final String TAG = SpellCheckersSettings.class.getSimpleName();
    private SpellCheckerInfo mCurrentSci;
    private AlertDialog mDialog = null;
    private SpellCheckerInfo[] mEnabledScis;
    private Preference mSpellCheckerLanaguagePref;
    private SwitchBar mSwitchBar;
    private TextServicesManager mTsm;

    @Override
    protected int getMetricsCategory() {
        return 59;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.spellchecker_prefs);
        this.mSpellCheckerLanaguagePref = findPreference("spellchecker_language");
        this.mSpellCheckerLanaguagePref.setOnPreferenceClickListener(this);
        this.mTsm = (TextServicesManager) getSystemService("textservices");
        this.mCurrentSci = this.mTsm.getCurrentSpellChecker();
        this.mEnabledScis = this.mTsm.getEnabledSpellCheckers();
        populatePreferenceScreen();
    }

    private void populatePreferenceScreen() {
        SpellCheckerPreference pref = new SpellCheckerPreference(getPrefContext(), this.mEnabledScis);
        pref.setTitle(R.string.default_spell_checker);
        int count = this.mEnabledScis != null ? this.mEnabledScis.length : 0;
        if (count > 0) {
            pref.setSummary("%s");
        } else {
            pref.setSummary(R.string.spell_checker_not_selected);
        }
        pref.setKey("default_spellchecker");
        pref.setOnPreferenceChangeListener(this);
        getPreferenceScreen().addPreference(pref);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.show();
        this.mSwitchBar.addOnSwitchChangeListener(this);
        updatePreferenceScreen();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mSwitchBar.removeOnSwitchChangeListener(this);
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        this.mTsm.setSpellCheckerEnabled(isChecked);
        updatePreferenceScreen();
    }

    public void updatePreferenceScreen() {
        SpellCheckerSubtype currentSpellCheckerSubtype;
        boolean z = false;
        this.mCurrentSci = this.mTsm.getCurrentSpellChecker();
        boolean isSpellCheckerEnabled = this.mTsm.isSpellCheckerEnabled();
        this.mSwitchBar.setChecked(isSpellCheckerEnabled);
        if (this.mCurrentSci != null) {
            currentSpellCheckerSubtype = this.mTsm.getCurrentSpellCheckerSubtype(false);
        } else {
            currentSpellCheckerSubtype = null;
        }
        this.mSpellCheckerLanaguagePref.setSummary(getSpellCheckerSubtypeLabel(this.mCurrentSci, currentSpellCheckerSubtype));
        PreferenceScreen screen = getPreferenceScreen();
        int count = screen.getPreferenceCount();
        for (int index = 0; index < count; index++) {
            Preference preference = screen.getPreference(index);
            preference.setEnabled(isSpellCheckerEnabled);
            if (preference instanceof SpellCheckerPreference) {
                SpellCheckerPreference pref = (SpellCheckerPreference) preference;
                pref.setSelected(this.mCurrentSci);
            }
        }
        Preference preference2 = this.mSpellCheckerLanaguagePref;
        if (isSpellCheckerEnabled && this.mCurrentSci != null) {
            z = true;
        }
        preference2.setEnabled(z);
    }

    private CharSequence getSpellCheckerSubtypeLabel(SpellCheckerInfo sci, SpellCheckerSubtype subtype) {
        if (sci == null) {
            return getString(R.string.spell_checker_not_selected);
        }
        if (subtype == null) {
            return getString(R.string.use_system_language_to_select_input_method_subtypes);
        }
        return subtype.getDisplayName(getActivity(), sci.getPackageName(), sci.getServiceInfo().applicationInfo);
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        if (pref == this.mSpellCheckerLanaguagePref) {
            showChooseLanguageDialog();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        SpellCheckerInfo sci = (SpellCheckerInfo) newValue;
        boolean isSystemApp = (sci.getServiceInfo().applicationInfo.flags & 1) != 0;
        if (isSystemApp) {
            changeCurrentSpellChecker(sci);
            return true;
        }
        showSecurityWarnDialog(sci);
        return false;
    }

    private static int convertSubtypeIndexToDialogItemId(int index) {
        return index + 1;
    }

    public static int convertDialogItemIdToSubtypeIndex(int item) {
        return item - 1;
    }

    private void showChooseLanguageDialog() {
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
        }
        final SpellCheckerInfo currentSci = this.mTsm.getCurrentSpellChecker();
        if (currentSci == null) {
            return;
        }
        SpellCheckerSubtype currentScs = this.mTsm.getCurrentSpellCheckerSubtype(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.phone_language);
        int subtypeCount = currentSci.getSubtypeCount();
        CharSequence[] items = new CharSequence[subtypeCount + 1];
        items[0] = getSpellCheckerSubtypeLabel(currentSci, null);
        int checkedItemId = 0;
        for (int index = 0; index < subtypeCount; index++) {
            SpellCheckerSubtype subtype = currentSci.getSubtypeAt(index);
            int itemId = convertSubtypeIndexToDialogItemId(index);
            items[itemId] = getSpellCheckerSubtypeLabel(currentSci, subtype);
            if (subtype.equals(currentScs)) {
                checkedItemId = itemId;
            }
        }
        builder.setSingleChoiceItems(items, checkedItemId, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (item == 0) {
                    SpellCheckersSettings.this.mTsm.setSpellCheckerSubtype(null);
                } else {
                    int index2 = SpellCheckersSettings.convertDialogItemIdToSubtypeIndex(item);
                    SpellCheckersSettings.this.mTsm.setSpellCheckerSubtype(currentSci.getSubtypeAt(index2));
                }
                dialog.dismiss();
                SpellCheckersSettings.this.updatePreferenceScreen();
            }
        });
        this.mDialog = builder.create();
        this.mDialog.show();
    }

    private void showSecurityWarnDialog(final SpellCheckerInfo sci) {
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(getString(R.string.spellchecker_security_warning, new Object[]{sci.loadLabel(getPackageManager())}));
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SpellCheckersSettings.this.changeCurrentSpellChecker(sci);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        this.mDialog = builder.create();
        this.mDialog.show();
    }

    public void changeCurrentSpellChecker(SpellCheckerInfo sci) {
        this.mTsm.setCurrentSpellChecker(sci);
        updatePreferenceScreen();
    }
}
