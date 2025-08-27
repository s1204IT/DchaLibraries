package com.android.settingslib.inputmethod;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.icu.text.ListFormatter;
import android.provider.Settings;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.app.LocaleHelper;
import com.android.internal.inputmethod.InputMethodUtils;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/* loaded from: classes.dex */
public class InputMethodAndSubtypeUtil {
    private static final TextUtils.SimpleStringSplitter sStringInputMethodSplitter = new TextUtils.SimpleStringSplitter(':');
    private static final TextUtils.SimpleStringSplitter sStringInputMethodSubtypeSplitter = new TextUtils.SimpleStringSplitter(';');

    private static String buildInputMethodsAndSubtypesString(HashMap<String, HashSet<String>> map) {
        StringBuilder sb = new StringBuilder();
        for (String str : map.keySet()) {
            if (sb.length() > 0) {
                sb.append(':');
            }
            HashSet<String> hashSet = map.get(str);
            sb.append(str);
            Iterator<String> it = hashSet.iterator();
            while (it.hasNext()) {
                String next = it.next();
                sb.append(';');
                sb.append(next);
            }
        }
        return sb.toString();
    }

    private static String buildInputMethodsString(HashSet<String> hashSet) {
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = hashSet.iterator();
        while (it.hasNext()) {
            String next = it.next();
            if (sb.length() > 0) {
                sb.append(':');
            }
            sb.append(next);
        }
        return sb.toString();
    }

    private static int getInputMethodSubtypeSelected(ContentResolver contentResolver) {
        try {
            return Settings.Secure.getInt(contentResolver, "selected_input_method_subtype");
        } catch (Settings.SettingNotFoundException e) {
            return -1;
        }
    }

    private static boolean isInputMethodSubtypeSelected(ContentResolver contentResolver) {
        return getInputMethodSubtypeSelected(contentResolver) != -1;
    }

    private static void putSelectedInputMethodSubtype(ContentResolver contentResolver, int i) {
        Settings.Secure.putInt(contentResolver, "selected_input_method_subtype", i);
    }

    private static HashMap<String, HashSet<String>> getEnabledInputMethodsAndSubtypeList(ContentResolver contentResolver) {
        return parseInputMethodsAndSubtypesString(Settings.Secure.getString(contentResolver, "enabled_input_methods"));
    }

    private static HashMap<String, HashSet<String>> parseInputMethodsAndSubtypesString(String str) {
        HashMap<String, HashSet<String>> map = new HashMap<>();
        if (TextUtils.isEmpty(str)) {
            return map;
        }
        sStringInputMethodSplitter.setString(str);
        while (sStringInputMethodSplitter.hasNext()) {
            sStringInputMethodSubtypeSplitter.setString(sStringInputMethodSplitter.next());
            if (sStringInputMethodSubtypeSplitter.hasNext()) {
                HashSet<String> hashSet = new HashSet<>();
                String next = sStringInputMethodSubtypeSplitter.next();
                while (sStringInputMethodSubtypeSplitter.hasNext()) {
                    hashSet.add(sStringInputMethodSubtypeSplitter.next());
                }
                map.put(next, hashSet);
            }
        }
        return map;
    }

    private static HashSet<String> getDisabledSystemIMEs(ContentResolver contentResolver) {
        HashSet<String> hashSet = new HashSet<>();
        String string = Settings.Secure.getString(contentResolver, "disabled_system_input_methods");
        if (TextUtils.isEmpty(string)) {
            return hashSet;
        }
        sStringInputMethodSplitter.setString(string);
        while (sStringInputMethodSplitter.hasNext()) {
            hashSet.add(sStringInputMethodSplitter.next());
        }
        return hashSet;
    }

    public static void saveInputMethodSubtypeList(PreferenceFragment preferenceFragment, ContentResolver contentResolver, List<InputMethodInfo> list, boolean z) {
        boolean zContainsKey;
        Iterator<InputMethodInfo> it;
        String string = Settings.Secure.getString(contentResolver, "default_input_method");
        int inputMethodSubtypeSelected = getInputMethodSubtypeSelected(contentResolver);
        HashMap<String, HashSet<String>> enabledInputMethodsAndSubtypeList = getEnabledInputMethodsAndSubtypeList(contentResolver);
        HashSet<String> disabledSystemIMEs = getDisabledSystemIMEs(contentResolver);
        Iterator<InputMethodInfo> it2 = list.iterator();
        String str = string;
        boolean z2 = false;
        while (it2.hasNext()) {
            InputMethodInfo next = it2.next();
            String id = next.getId();
            Preference preferenceFindPreference = preferenceFragment.findPreference(id);
            if (preferenceFindPreference != null) {
                if (preferenceFindPreference instanceof TwoStatePreference) {
                    zContainsKey = ((TwoStatePreference) preferenceFindPreference).isChecked();
                } else {
                    zContainsKey = enabledInputMethodsAndSubtypeList.containsKey(id);
                }
                boolean zEquals = id.equals(str);
                boolean zIsSystemIme = InputMethodUtils.isSystemIme(next);
                if ((!z && InputMethodSettingValuesWrapper.getInstance(preferenceFragment.getActivity()).isAlwaysCheckedIme(next, preferenceFragment.getActivity())) || zContainsKey) {
                    if (!enabledInputMethodsAndSubtypeList.containsKey(id)) {
                        enabledInputMethodsAndSubtypeList.put(id, new HashSet<>());
                    }
                    HashSet<String> hashSet = enabledInputMethodsAndSubtypeList.get(id);
                    int subtypeCount = next.getSubtypeCount();
                    boolean z3 = z2;
                    int i = 0;
                    boolean z4 = false;
                    while (i < subtypeCount) {
                        Iterator<InputMethodInfo> it3 = it2;
                        InputMethodSubtype subtypeAt = next.getSubtypeAt(i);
                        InputMethodInfo inputMethodInfo = next;
                        String strValueOf = String.valueOf(subtypeAt.hashCode());
                        int i2 = subtypeCount;
                        TwoStatePreference twoStatePreference = (TwoStatePreference) preferenceFragment.findPreference(id + strValueOf);
                        if (twoStatePreference != null) {
                            if (!z4) {
                                hashSet.clear();
                                z4 = true;
                                z3 = true;
                            }
                            if (twoStatePreference.isEnabled() && twoStatePreference.isChecked()) {
                                hashSet.add(strValueOf);
                                if (zEquals && inputMethodSubtypeSelected == subtypeAt.hashCode()) {
                                    z3 = false;
                                }
                            } else {
                                hashSet.remove(strValueOf);
                            }
                        }
                        i++;
                        it2 = it3;
                        next = inputMethodInfo;
                        subtypeCount = i2;
                    }
                    it = it2;
                    z2 = z3;
                } else {
                    it = it2;
                    enabledInputMethodsAndSubtypeList.remove(id);
                    if (zEquals) {
                        str = null;
                    }
                }
                if (zIsSystemIme && z) {
                    if (disabledSystemIMEs.contains(id)) {
                        if (zContainsKey) {
                            disabledSystemIMEs.remove(id);
                        }
                    } else if (!zContainsKey) {
                        disabledSystemIMEs.add(id);
                    }
                }
                it2 = it;
            }
        }
        String strBuildInputMethodsAndSubtypesString = buildInputMethodsAndSubtypesString(enabledInputMethodsAndSubtypeList);
        String strBuildInputMethodsString = buildInputMethodsString(disabledSystemIMEs);
        if (z2 || !isInputMethodSubtypeSelected(contentResolver)) {
            putSelectedInputMethodSubtype(contentResolver, -1);
        }
        Settings.Secure.putString(contentResolver, "enabled_input_methods", strBuildInputMethodsAndSubtypesString);
        if (strBuildInputMethodsString.length() > 0) {
            Settings.Secure.putString(contentResolver, "disabled_system_input_methods", strBuildInputMethodsString);
        }
        if (str == null) {
            str = "";
        }
        Settings.Secure.putString(contentResolver, "default_input_method", str);
    }

    public static void loadInputMethodSubtypeList(PreferenceFragment preferenceFragment, ContentResolver contentResolver, List<InputMethodInfo> list, Map<String, List<Preference>> map) {
        HashMap<String, HashSet<String>> enabledInputMethodsAndSubtypeList = getEnabledInputMethodsAndSubtypeList(contentResolver);
        Iterator<InputMethodInfo> it = list.iterator();
        while (it.hasNext()) {
            String id = it.next().getId();
            Preference preferenceFindPreference = preferenceFragment.findPreference(id);
            if (preferenceFindPreference instanceof TwoStatePreference) {
                boolean zContainsKey = enabledInputMethodsAndSubtypeList.containsKey(id);
                ((TwoStatePreference) preferenceFindPreference).setChecked(zContainsKey);
                if (map != null) {
                    Iterator<Preference> it2 = map.get(id).iterator();
                    while (it2.hasNext()) {
                        it2.next().setEnabled(zContainsKey);
                    }
                }
                setSubtypesPreferenceEnabled(preferenceFragment, list, id, zContainsKey);
            }
        }
        updateSubtypesPreferenceChecked(preferenceFragment, list, enabledInputMethodsAndSubtypeList);
    }

    private static void setSubtypesPreferenceEnabled(PreferenceFragment preferenceFragment, List<InputMethodInfo> list, String str, boolean z) {
        PreferenceScreen preferenceScreen = preferenceFragment.getPreferenceScreen();
        for (InputMethodInfo inputMethodInfo : list) {
            if (str.equals(inputMethodInfo.getId())) {
                int subtypeCount = inputMethodInfo.getSubtypeCount();
                for (int i = 0; i < subtypeCount; i++) {
                    TwoStatePreference twoStatePreference = (TwoStatePreference) preferenceScreen.findPreference(str + inputMethodInfo.getSubtypeAt(i).hashCode());
                    if (twoStatePreference != null) {
                        twoStatePreference.setEnabled(z);
                    }
                }
            }
        }
    }

    private static void updateSubtypesPreferenceChecked(PreferenceFragment preferenceFragment, List<InputMethodInfo> list, HashMap<String, HashSet<String>> map) {
        PreferenceScreen preferenceScreen = preferenceFragment.getPreferenceScreen();
        for (InputMethodInfo inputMethodInfo : list) {
            String id = inputMethodInfo.getId();
            if (map.containsKey(id)) {
                HashSet<String> hashSet = map.get(id);
                int subtypeCount = inputMethodInfo.getSubtypeCount();
                for (int i = 0; i < subtypeCount; i++) {
                    String strValueOf = String.valueOf(inputMethodInfo.getSubtypeAt(i).hashCode());
                    TwoStatePreference twoStatePreference = (TwoStatePreference) preferenceScreen.findPreference(id + strValueOf);
                    if (twoStatePreference != null) {
                        twoStatePreference.setChecked(hashSet.contains(strValueOf));
                    }
                }
            }
        }
    }

    public static void removeUnnecessaryNonPersistentPreference(Preference preference) {
        SharedPreferences sharedPreferences;
        String key = preference.getKey();
        if (!preference.isPersistent() && key != null && (sharedPreferences = preference.getSharedPreferences()) != null && sharedPreferences.contains(key)) {
            sharedPreferences.edit().remove(key).apply();
        }
    }

    public static String getSubtypeLocaleNameAsSentence(InputMethodSubtype inputMethodSubtype, Context context, InputMethodInfo inputMethodInfo) {
        if (inputMethodSubtype == null) {
            return "";
        }
        return LocaleHelper.toSentenceCase(inputMethodSubtype.getDisplayName(context, inputMethodInfo.getPackageName(), inputMethodInfo.getServiceInfo().applicationInfo).toString(), getDisplayLocale(context));
    }

    public static String getSubtypeLocaleNameListAsSentence(List<InputMethodSubtype> list, Context context, InputMethodInfo inputMethodInfo) {
        if (list.isEmpty()) {
            return "";
        }
        Locale displayLocale = getDisplayLocale(context);
        int size = list.size();
        CharSequence[] charSequenceArr = new CharSequence[size];
        for (int i = 0; i < size; i++) {
            charSequenceArr[i] = list.get(i).getDisplayName(context, inputMethodInfo.getPackageName(), inputMethodInfo.getServiceInfo().applicationInfo);
        }
        return LocaleHelper.toSentenceCase(ListFormatter.getInstance(displayLocale).format(charSequenceArr), displayLocale);
    }

    private static Locale getDisplayLocale(Context context) {
        if (context == null) {
            return Locale.getDefault();
        }
        if (context.getResources() == null) {
            return Locale.getDefault();
        }
        Configuration configuration = context.getResources().getConfiguration();
        if (configuration == null) {
            return Locale.getDefault();
        }
        Locale locale = configuration.getLocales().get(0);
        if (locale == null) {
            return Locale.getDefault();
        }
        return locale;
    }
}
