package com.android.settings.inputmethod;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.preference.TwoStatePreference;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class InputMethodAndSubtypeEnabler extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {
    private Collator mCollator;
    private boolean mHaveHardKeyboard;
    private InputMethodManager mImm;
    private List<InputMethodInfo> mInputMethodInfoList;
    private final HashMap<String, List<Preference>> mInputMethodAndSubtypePrefsMap = new HashMap<>();
    private final HashMap<String, TwoStatePreference> mAutoSelectionPrefsMap = new HashMap<>();

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        this.mImm = (InputMethodManager) getSystemService("input_method");
        Configuration config = getResources().getConfiguration();
        this.mHaveHardKeyboard = config.keyboard == 2;
        String targetImi = getStringExtraFromIntentOrArguments("input_method_id");
        this.mInputMethodInfoList = this.mImm.getInputMethodList();
        this.mCollator = Collator.getInstance();
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(getActivity());
        int imiCount = this.mInputMethodInfoList.size();
        for (int index = 0; index < imiCount; index++) {
            InputMethodInfo imi = this.mInputMethodInfoList.get(index);
            if (imi.getId().equals(targetImi) || TextUtils.isEmpty(targetImi)) {
                addInputMethodSubtypePreferences(imi, root);
            }
        }
        setPreferenceScreen(root);
    }

    private String getStringExtraFromIntentOrArguments(String name) {
        Intent intent = getActivity().getIntent();
        String fromIntent = intent.getStringExtra(name);
        if (fromIntent == null) {
            Bundle arguments = getArguments();
            return arguments == null ? null : arguments.getString(name);
        }
        return fromIntent;
    }

    @Override
    public void onActivityCreated(Bundle icicle) {
        super.onActivityCreated(icicle);
        String title = getStringExtraFromIntentOrArguments("android.intent.extra.TITLE");
        if (!TextUtils.isEmpty(title)) {
            getActivity().setTitle(title);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        InputMethodSettingValuesWrapper.getInstance(getActivity()).refreshAllInputMethodAndSubtypes();
        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(this, getContentResolver(), this.mInputMethodInfoList, this.mInputMethodAndSubtypePrefsMap);
        updateAutoSelectionPreferences();
    }

    @Override
    public void onPause() {
        super.onPause();
        updateImplicitlyEnabledSubtypes(null, false);
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(), this.mInputMethodInfoList, this.mHaveHardKeyboard);
    }

    @Override
    public boolean onPreferenceChange(Preference pref, Object newValue) {
        if (!(newValue instanceof Boolean)) {
            return true;
        }
        boolean isChecking = ((Boolean) newValue).booleanValue();
        for (String imiId : this.mAutoSelectionPrefsMap.keySet()) {
            if (this.mAutoSelectionPrefsMap.get(imiId) == pref) {
                TwoStatePreference autoSelectionPref = (TwoStatePreference) pref;
                autoSelectionPref.setChecked(isChecking);
                setAutoSelectionSubtypesEnabled(imiId, autoSelectionPref.isChecked());
                return false;
            }
        }
        if (!(pref instanceof InputMethodSubtypePreference)) {
            return true;
        }
        InputMethodSubtypePreference subtypePref = (InputMethodSubtypePreference) pref;
        subtypePref.setChecked(isChecking);
        if (!subtypePref.isChecked()) {
            updateAutoSelectionPreferences();
        }
        return false;
    }

    private void addInputMethodSubtypePreferences(InputMethodInfo imi, PreferenceScreen preferenceScreen) {
        Context context = getActivity();
        int subtypeCount = imi.getSubtypeCount();
        if (subtypeCount > 1) {
            String imiId = imi.getId();
            PreferenceCategory keyboardSettingsCategory = new PreferenceCategory(context);
            preferenceScreen.addPreference(keyboardSettingsCategory);
            PackageManager pm = getPackageManager();
            CharSequence label = imi.loadLabel(pm);
            keyboardSettingsCategory.setTitle(label);
            keyboardSettingsCategory.setKey(imiId);
            TwoStatePreference autoSelectionPref = new SwitchWithNoTextPreference(context);
            this.mAutoSelectionPrefsMap.put(imiId, autoSelectionPref);
            keyboardSettingsCategory.addPreference(autoSelectionPref);
            autoSelectionPref.setOnPreferenceChangeListener(this);
            PreferenceCategory activeInputMethodsCategory = new PreferenceCategory(context);
            activeInputMethodsCategory.setTitle(R.string.active_input_method_subtypes);
            preferenceScreen.addPreference(activeInputMethodsCategory);
            CharSequence autoSubtypeLabel = null;
            ArrayList<Preference> subtypePreferences = new ArrayList<>();
            for (int index = 0; index < subtypeCount; index++) {
                InputMethodSubtype subtype = imi.getSubtypeAt(index);
                if (subtype.overridesImplicitlyEnabledSubtype()) {
                    if (autoSubtypeLabel == null) {
                        autoSubtypeLabel = subtype.getDisplayName(context, imi.getPackageName(), imi.getServiceInfo().applicationInfo);
                    }
                } else {
                    Preference subtypePref = new InputMethodSubtypePreference(context, subtype, imi);
                    subtypePreferences.add(subtypePref);
                }
            }
            Collections.sort(subtypePreferences, new Comparator<Preference>() {
                @Override
                public int compare(Preference lhs, Preference rhs) {
                    return lhs instanceof InputMethodSubtypePreference ? ((InputMethodSubtypePreference) lhs).compareTo(rhs, InputMethodAndSubtypeEnabler.this.mCollator) : lhs.compareTo(rhs);
                }
            });
            int prefCount = subtypePreferences.size();
            for (int index2 = 0; index2 < prefCount; index2++) {
                Preference pref = subtypePreferences.get(index2);
                activeInputMethodsCategory.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
                InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(pref);
            }
            this.mInputMethodAndSubtypePrefsMap.put(imiId, subtypePreferences);
            if (TextUtils.isEmpty(autoSubtypeLabel)) {
                autoSelectionPref.setTitle(R.string.use_system_language_to_select_input_method_subtypes);
            } else {
                autoSelectionPref.setTitle(autoSubtypeLabel);
            }
        }
    }

    private boolean isNoSubtypesExplicitlySelected(String imiId) {
        List<Preference> subtypePrefs = this.mInputMethodAndSubtypePrefsMap.get(imiId);
        for (Preference pref : subtypePrefs) {
            if ((pref instanceof TwoStatePreference) && ((TwoStatePreference) pref).isChecked()) {
                return false;
            }
        }
        return true;
    }

    private void setAutoSelectionSubtypesEnabled(String imiId, boolean autoSelectionEnabled) {
        TwoStatePreference autoSelectionPref = this.mAutoSelectionPrefsMap.get(imiId);
        if (autoSelectionPref != null) {
            autoSelectionPref.setChecked(autoSelectionEnabled);
            List<Preference> subtypePrefs = this.mInputMethodAndSubtypePrefsMap.get(imiId);
            for (Preference pref : subtypePrefs) {
                if (pref instanceof TwoStatePreference) {
                    pref.setEnabled(!autoSelectionEnabled);
                    if (autoSelectionEnabled) {
                        ((TwoStatePreference) pref).setChecked(false);
                    }
                }
            }
            if (autoSelectionEnabled) {
                InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(), this.mInputMethodInfoList, this.mHaveHardKeyboard);
                updateImplicitlyEnabledSubtypes(imiId, true);
            }
        }
    }

    private void updateImplicitlyEnabledSubtypes(String targetImiId, boolean check) {
        for (InputMethodInfo imi : this.mInputMethodInfoList) {
            String imiId = imi.getId();
            TwoStatePreference autoSelectionPref = this.mAutoSelectionPrefsMap.get(imiId);
            if (autoSelectionPref != null && autoSelectionPref.isChecked() && (imiId.equals(targetImiId) || targetImiId == null)) {
                updateImplicitlyEnabledSubtypesOf(imi, check);
            }
        }
    }

    private void updateImplicitlyEnabledSubtypesOf(InputMethodInfo imi, boolean check) {
        String imiId = imi.getId();
        List<Preference> subtypePrefs = this.mInputMethodAndSubtypePrefsMap.get(imiId);
        List<InputMethodSubtype> implicitlyEnabledSubtypes = this.mImm.getEnabledInputMethodSubtypeList(imi, true);
        if (subtypePrefs != null && implicitlyEnabledSubtypes != null) {
            for (Preference pref : subtypePrefs) {
                if (pref instanceof TwoStatePreference) {
                    TwoStatePreference subtypePref = (TwoStatePreference) pref;
                    subtypePref.setChecked(false);
                    if (check) {
                        Iterator<InputMethodSubtype> it = implicitlyEnabledSubtypes.iterator();
                        while (true) {
                            if (it.hasNext()) {
                                InputMethodSubtype subtype = it.next();
                                String implicitlyEnabledSubtypePrefKey = imiId + subtype.hashCode();
                                if (subtypePref.getKey().equals(implicitlyEnabledSubtypePrefKey)) {
                                    subtypePref.setChecked(true);
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateAutoSelectionPreferences() {
        for (String imiId : this.mInputMethodAndSubtypePrefsMap.keySet()) {
            setAutoSelectionSubtypesEnabled(imiId, isNoSubtypesExplicitlySelected(imiId));
        }
        updateImplicitlyEnabledSubtypes(null, true);
    }
}
