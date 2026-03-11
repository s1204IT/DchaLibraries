package com.android.settingslib.inputmethod;

import android.content.Context;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import com.android.settingslib.R;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class InputMethodAndSubtypeEnablerManager implements Preference.OnPreferenceChangeListener {
    private final PreferenceFragment mFragment;
    private boolean mHaveHardKeyboard;
    private InputMethodManager mImm;
    private List<InputMethodInfo> mInputMethodInfoList;
    private final HashMap<String, List<Preference>> mInputMethodAndSubtypePrefsMap = new HashMap<>();
    private final HashMap<String, TwoStatePreference> mAutoSelectionPrefsMap = new HashMap<>();
    private final Collator mCollator = Collator.getInstance();

    public InputMethodAndSubtypeEnablerManager(PreferenceFragment preferenceFragment) {
        this.mFragment = preferenceFragment;
        this.mImm = (InputMethodManager) preferenceFragment.getContext().getSystemService(InputMethodManager.class);
        this.mInputMethodInfoList = this.mImm.getInputMethodList();
    }

    public void init(PreferenceFragment preferenceFragment, String str, PreferenceScreen preferenceScreen) {
        this.mHaveHardKeyboard = preferenceFragment.getResources().getConfiguration().keyboard == 2;
        for (InputMethodInfo inputMethodInfo : this.mInputMethodInfoList) {
            if (inputMethodInfo.getId().equals(str) || TextUtils.isEmpty(str)) {
                addInputMethodSubtypePreferences(preferenceFragment, inputMethodInfo, preferenceScreen);
            }
        }
    }

    public void refresh(Context context, PreferenceFragment preferenceFragment) {
        InputMethodSettingValuesWrapper.getInstance(context).refreshAllInputMethodAndSubtypes();
        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(preferenceFragment, context.getContentResolver(), this.mInputMethodInfoList, this.mInputMethodAndSubtypePrefsMap);
        updateAutoSelectionPreferences();
    }

    public void save(Context context, PreferenceFragment preferenceFragment) {
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(preferenceFragment, context.getContentResolver(), this.mInputMethodInfoList, this.mHaveHardKeyboard);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object obj) {
        if (!(obj instanceof Boolean)) {
            return true;
        }
        boolean zBooleanValue = ((Boolean) obj).booleanValue();
        for (String str : this.mAutoSelectionPrefsMap.keySet()) {
            if (this.mAutoSelectionPrefsMap.get(str) == preference) {
                TwoStatePreference twoStatePreference = (TwoStatePreference) preference;
                twoStatePreference.setChecked(zBooleanValue);
                setAutoSelectionSubtypesEnabled(str, twoStatePreference.isChecked());
                return false;
            }
        }
        if (!(preference instanceof InputMethodSubtypePreference)) {
            return true;
        }
        InputMethodSubtypePreference inputMethodSubtypePreference = (InputMethodSubtypePreference) preference;
        inputMethodSubtypePreference.setChecked(zBooleanValue);
        if (!inputMethodSubtypePreference.isChecked()) {
            updateAutoSelectionPreferences();
        }
        return false;
    }

    private void addInputMethodSubtypePreferences(PreferenceFragment preferenceFragment, InputMethodInfo inputMethodInfo, PreferenceScreen preferenceScreen) {
        Context context = preferenceFragment.getPreferenceManager().getContext();
        int subtypeCount = inputMethodInfo.getSubtypeCount();
        if (subtypeCount <= 1) {
            return;
        }
        String id = inputMethodInfo.getId();
        PreferenceCategory preferenceCategory = new PreferenceCategory(context);
        preferenceScreen.addPreference(preferenceCategory);
        preferenceCategory.setTitle(inputMethodInfo.loadLabel(context.getPackageManager()));
        preferenceCategory.setKey(id);
        SwitchWithNoTextPreference switchWithNoTextPreference = new SwitchWithNoTextPreference(context);
        this.mAutoSelectionPrefsMap.put(id, switchWithNoTextPreference);
        preferenceCategory.addPreference(switchWithNoTextPreference);
        switchWithNoTextPreference.setOnPreferenceChangeListener(this);
        PreferenceCategory preferenceCategory2 = new PreferenceCategory(context);
        preferenceCategory2.setTitle(R.string.active_input_method_subtypes);
        preferenceScreen.addPreference(preferenceCategory2);
        String subtypeLocaleNameAsSentence = null;
        ArrayList<Preference> arrayList = new ArrayList();
        for (int i = 0; i < subtypeCount; i++) {
            InputMethodSubtype subtypeAt = inputMethodInfo.getSubtypeAt(i);
            if (subtypeAt.overridesImplicitlyEnabledSubtype()) {
                if (subtypeLocaleNameAsSentence == null) {
                    subtypeLocaleNameAsSentence = InputMethodAndSubtypeUtil.getSubtypeLocaleNameAsSentence(subtypeAt, context, inputMethodInfo);
                }
            } else {
                arrayList.add(new InputMethodSubtypePreference(context, subtypeAt, inputMethodInfo));
            }
        }
        arrayList.sort(new Comparator() {
            @Override
            public final int compare(Object obj, Object obj2) {
                return InputMethodAndSubtypeEnablerManager.lambda$addInputMethodSubtypePreferences$0(this.f$0, (Preference) obj, (Preference) obj2);
            }
        });
        for (Preference preference : arrayList) {
            preferenceCategory2.addPreference(preference);
            preference.setOnPreferenceChangeListener(this);
            InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(preference);
        }
        this.mInputMethodAndSubtypePrefsMap.put(id, arrayList);
        if (TextUtils.isEmpty(subtypeLocaleNameAsSentence)) {
            switchWithNoTextPreference.setTitle(R.string.use_system_language_to_select_input_method_subtypes);
        } else {
            switchWithNoTextPreference.setTitle(subtypeLocaleNameAsSentence);
        }
    }

    public static int lambda$addInputMethodSubtypePreferences$0(InputMethodAndSubtypeEnablerManager inputMethodAndSubtypeEnablerManager, Preference preference, Preference preference2) {
        if (preference instanceof InputMethodSubtypePreference) {
            return ((InputMethodSubtypePreference) preference).compareTo(preference2, inputMethodAndSubtypeEnablerManager.mCollator);
        }
        return preference.compareTo(preference2);
    }

    private boolean isNoSubtypesExplicitlySelected(String str) {
        for (Preference preference : this.mInputMethodAndSubtypePrefsMap.get(str)) {
            if ((preference instanceof TwoStatePreference) && ((TwoStatePreference) preference).isChecked()) {
                return false;
            }
        }
        return true;
    }

    private void setAutoSelectionSubtypesEnabled(String str, boolean z) {
        TwoStatePreference twoStatePreference = this.mAutoSelectionPrefsMap.get(str);
        if (twoStatePreference == null) {
            return;
        }
        twoStatePreference.setChecked(z);
        for (Preference preference : this.mInputMethodAndSubtypePrefsMap.get(str)) {
            if (preference instanceof TwoStatePreference) {
                preference.setEnabled(!z);
                if (z) {
                    ((TwoStatePreference) preference).setChecked(false);
                }
            }
        }
        if (z) {
            InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this.mFragment, this.mFragment.getContext().getContentResolver(), this.mInputMethodInfoList, this.mHaveHardKeyboard);
            updateImplicitlyEnabledSubtypes(str);
        }
    }

    private void updateImplicitlyEnabledSubtypes(String str) {
        for (InputMethodInfo inputMethodInfo : this.mInputMethodInfoList) {
            String id = inputMethodInfo.getId();
            TwoStatePreference twoStatePreference = this.mAutoSelectionPrefsMap.get(id);
            if (twoStatePreference != null && twoStatePreference.isChecked() && (id.equals(str) || str == null)) {
                updateImplicitlyEnabledSubtypesOf(inputMethodInfo);
            }
        }
    }

    private void updateImplicitlyEnabledSubtypesOf(InputMethodInfo inputMethodInfo) {
        String id = inputMethodInfo.getId();
        List<Preference> list = this.mInputMethodAndSubtypePrefsMap.get(id);
        List<InputMethodSubtype> enabledInputMethodSubtypeList = this.mImm.getEnabledInputMethodSubtypeList(inputMethodInfo, true);
        if (list == null || enabledInputMethodSubtypeList == null) {
            return;
        }
        for (Preference preference : list) {
            if (preference instanceof TwoStatePreference) {
                TwoStatePreference twoStatePreference = (TwoStatePreference) preference;
                twoStatePreference.setChecked(false);
                Iterator<InputMethodSubtype> it = enabledInputMethodSubtypeList.iterator();
                while (true) {
                    if (it.hasNext()) {
                        if (twoStatePreference.getKey().equals(id + it.next().hashCode())) {
                            twoStatePreference.setChecked(true);
                            break;
                        }
                    }
                }
            }
        }
    }

    private void updateAutoSelectionPreferences() {
        for (String str : this.mInputMethodAndSubtypePrefsMap.keySet()) {
            setAutoSelectionSubtypesEnabled(str, isNoSubtypesExplicitlySelected(str));
        }
        updateImplicitlyEnabledSubtypes(null);
    }
}
