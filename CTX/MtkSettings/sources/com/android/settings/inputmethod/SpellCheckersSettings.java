package com.android.settings.inputmethod;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
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

public class SpellCheckersSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, SwitchBar.OnSwitchChangeListener {
    private static final String TAG = SpellCheckersSettings.class.getSimpleName();
    private SpellCheckerInfo mCurrentSci;
    private AlertDialog mDialog = null;
    private SpellCheckerInfo[] mEnabledScis;
    private Preference mSpellCheckerLanaguagePref;
    private SwitchBar mSwitchBar;
    private TextServicesManager mTsm;

    @Override
    public int getMetricsCategory() {
        return 59;
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        addPreferencesFromResource(R.xml.spellchecker_prefs);
        this.mSpellCheckerLanaguagePref = findPreference("spellchecker_language");
        this.mSpellCheckerLanaguagePref.setOnPreferenceClickListener(this);
        this.mTsm = (TextServicesManager) getSystemService("textservices");
        this.mCurrentSci = this.mTsm.getCurrentSpellChecker();
        this.mEnabledScis = this.mTsm.getEnabledSpellCheckers();
        populatePreferenceScreen();
    }

    private void populatePreferenceScreen() {
        SpellCheckerPreference spellCheckerPreference = new SpellCheckerPreference(getPrefContext(), this.mEnabledScis);
        spellCheckerPreference.setTitle(R.string.default_spell_checker);
        if ((this.mEnabledScis == null ? 0 : this.mEnabledScis.length) > 0) {
            spellCheckerPreference.setSummary("%s");
        } else {
            spellCheckerPreference.setSummary(R.string.spell_checker_not_selected);
        }
        spellCheckerPreference.setKey("default_spellchecker");
        spellCheckerPreference.setOnPreferenceChangeListener(this);
        getPreferenceScreen().addPreference(spellCheckerPreference);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSwitchBar = ((SettingsActivity) getActivity()).getSwitchBar();
        this.mSwitchBar.setSwitchBarText(R.string.spell_checker_master_switch_title, R.string.spell_checker_master_switch_title);
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
    public void onSwitchChanged(Switch r2, boolean z) {
        Settings.Secure.putInt(getContentResolver(), "spell_checker_enabled", z ? 1 : 0);
        updatePreferenceScreen();
    }

    private void updatePreferenceScreen() {
        SpellCheckerSubtype currentSpellCheckerSubtype;
        this.mCurrentSci = this.mTsm.getCurrentSpellChecker();
        boolean zIsSpellCheckerEnabled = this.mTsm.isSpellCheckerEnabled();
        this.mSwitchBar.setChecked(zIsSpellCheckerEnabled);
        boolean z = false;
        if (this.mCurrentSci != null) {
            currentSpellCheckerSubtype = this.mTsm.getCurrentSpellCheckerSubtype(false);
        } else {
            currentSpellCheckerSubtype = null;
        }
        this.mSpellCheckerLanaguagePref.setSummary(getSpellCheckerSubtypeLabel(this.mCurrentSci, currentSpellCheckerSubtype));
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        int preferenceCount = preferenceScreen.getPreferenceCount();
        for (int i = 0; i < preferenceCount; i++) {
            Preference preference = preferenceScreen.getPreference(i);
            preference.setEnabled(zIsSpellCheckerEnabled);
            if (preference instanceof SpellCheckerPreference) {
                ((SpellCheckerPreference) preference).setSelected(this.mCurrentSci);
            }
        }
        Preference preference2 = this.mSpellCheckerLanaguagePref;
        if (zIsSpellCheckerEnabled && this.mCurrentSci != null) {
            z = true;
        }
        preference2.setEnabled(z);
    }

    private CharSequence getSpellCheckerSubtypeLabel(SpellCheckerInfo spellCheckerInfo, SpellCheckerSubtype spellCheckerSubtype) {
        if (spellCheckerInfo == null) {
            return getString(R.string.spell_checker_not_selected);
        }
        if (spellCheckerSubtype == null) {
            return getString(R.string.use_system_language_to_select_input_method_subtypes);
        }
        return spellCheckerSubtype.getDisplayName(getActivity(), spellCheckerInfo.getPackageName(), spellCheckerInfo.getServiceInfo().applicationInfo);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == this.mSpellCheckerLanaguagePref) {
            showChooseLanguageDialog();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        SpellCheckerInfo spellCheckerInfo = (SpellCheckerInfo) obj;
        if ((spellCheckerInfo.getServiceInfo().applicationInfo.flags & 1) != 0) {
            changeCurrentSpellChecker(spellCheckerInfo);
            return true;
        }
        showSecurityWarnDialog(spellCheckerInfo);
        return false;
    }

    private static int convertSubtypeIndexToDialogItemId(int i) {
        return i + 1;
    }

    private static int convertDialogItemIdToSubtypeIndex(int i) {
        return i - 1;
    }

    private void showChooseLanguageDialog() {
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
        }
        final SpellCheckerInfo currentSpellChecker = this.mTsm.getCurrentSpellChecker();
        if (currentSpellChecker == null) {
            return;
        }
        SpellCheckerSubtype currentSpellCheckerSubtype = this.mTsm.getCurrentSpellCheckerSubtype(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.phone_language);
        int subtypeCount = currentSpellChecker.getSubtypeCount();
        CharSequence[] charSequenceArr = new CharSequence[subtypeCount + 1];
        charSequenceArr[0] = getSpellCheckerSubtypeLabel(currentSpellChecker, null);
        int i = 0;
        for (int i2 = 0; i2 < subtypeCount; i2++) {
            SpellCheckerSubtype subtypeAt = currentSpellChecker.getSubtypeAt(i2);
            int iConvertSubtypeIndexToDialogItemId = convertSubtypeIndexToDialogItemId(i2);
            charSequenceArr[iConvertSubtypeIndexToDialogItemId] = getSpellCheckerSubtypeLabel(currentSpellChecker, subtypeAt);
            if (subtypeAt.equals(currentSpellCheckerSubtype)) {
                i = iConvertSubtypeIndexToDialogItemId;
            }
        }
        builder.setSingleChoiceItems(charSequenceArr, i, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i3) {
                int iHashCode;
                if (i3 != 0) {
                    iHashCode = currentSpellChecker.getSubtypeAt(SpellCheckersSettings.convertDialogItemIdToSubtypeIndex(i3)).hashCode();
                } else {
                    iHashCode = 0;
                }
                Settings.Secure.putInt(SpellCheckersSettings.this.getContentResolver(), "selected_spell_checker_subtype", iHashCode);
                dialogInterface.dismiss();
                SpellCheckersSettings.this.updatePreferenceScreen();
            }
        });
        this.mDialog = builder.create();
        this.mDialog.show();
    }

    private void showSecurityWarnDialog(final SpellCheckerInfo spellCheckerInfo) {
        if (this.mDialog != null && this.mDialog.isShowing()) {
            this.mDialog.dismiss();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(android.R.string.dialog_alert_title);
        builder.setMessage(getString(R.string.spellchecker_security_warning, new Object[]{spellCheckerInfo.loadLabel(getPackageManager())}));
        builder.setCancelable(true);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                SpellCheckersSettings.this.changeCurrentSpellChecker(spellCheckerInfo);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        });
        this.mDialog = builder.create();
        this.mDialog.show();
    }

    private void changeCurrentSpellChecker(SpellCheckerInfo spellCheckerInfo) {
        Settings.Secure.putString(getContentResolver(), "selected_spell_checker", spellCheckerInfo.getId());
        Settings.Secure.putInt(getContentResolver(), "selected_spell_checker_subtype", 0);
        updatePreferenceScreen();
    }
}
