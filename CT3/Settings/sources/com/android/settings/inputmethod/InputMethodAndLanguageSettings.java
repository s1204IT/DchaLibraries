package com.android.settings.inputmethod;

import android.app.Activity;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.database.ContentObserver;
import android.hardware.input.InputDeviceIdentifier;
import android.hardware.input.InputManager;
import android.hardware.input.KeyboardLayout;
import android.os.Bundle;
import android.os.Handler;
import android.os.LocaleList;
import android.os.Parcelable;
import android.provider.Settings;
import android.speech.tts.TtsEngines;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.view.textservice.SpellCheckerInfo;
import android.view.textservice.TextServicesManager;
import com.android.internal.app.LocaleHelper;
import com.android.internal.app.LocalePicker;
import com.android.settings.R;
import com.android.settings.Settings;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.UserDictionarySettings;
import com.android.settings.Utils;
import com.android.settings.VoiceInputOutputSettings;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.inputmethod.InputMethodPreference;
import com.android.settings.inputmethod.KeyboardLayoutDialogFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import com.mediatek.settings.inputmethod.InputMethodExts;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class InputMethodAndLanguageSettings extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener, InputManager.InputDeviceListener, KeyboardLayoutDialogFragment.OnSetupKeyboardLayoutsListener, Indexable, InputMethodPreference.OnSavePreferenceListener {
    private DevicePolicyManager mDpm;
    private PreferenceCategory mGameControllerCategory;
    private Handler mHandler;
    private PreferenceCategory mHardKeyboardCategory;
    private InputManager mIm;
    private InputMethodManager mImm;
    InputMethodExts mInputMethodExts;
    private InputMethodSettingValuesWrapper mInputMethodSettingValues;
    private Intent mIntentWaitingForResult;
    private PreferenceCategory mKeyboardSettingsCategory;
    private Preference mLanguagePref;
    private SettingsObserver mSettingsObserver;
    private boolean mShowsOnlyFullImeAndKeyboardList;
    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity, SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
            String summary;
            List<SearchIndexableRaw> indexables = new ArrayList<>();
            String screenTitle = context.getString(R.string.language_keyboard_settings_title);
            if (context.getAssets().getLocales().length > 1) {
                String localeNames = InputMethodAndLanguageSettings.getLocaleNames(context);
                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.key = "phone_language";
                indexable.title = context.getString(R.string.phone_language);
                indexable.summaryOn = localeNames;
                indexable.summaryOff = localeNames;
                indexable.screenTitle = screenTitle;
                indexables.add(indexable);
            }
            SearchIndexableRaw indexable2 = new SearchIndexableRaw(context);
            indexable2.key = "spellcheckers_settings";
            indexable2.title = context.getString(R.string.spellcheckers_settings_title);
            indexable2.screenTitle = screenTitle;
            indexable2.keywords = context.getString(R.string.keywords_spell_checker);
            indexables.add(indexable2);
            if (UserDictionaryList.getUserDictionaryLocalesSet(context) != null) {
                SearchIndexableRaw indexable3 = new SearchIndexableRaw(context);
                indexable3.key = "user_dict_settings";
                indexable3.title = context.getString(R.string.user_dict_settings_title);
                indexable3.screenTitle = screenTitle;
                indexables.add(indexable3);
            }
            SearchIndexableRaw indexable4 = new SearchIndexableRaw(context);
            indexable4.key = "keyboard_settings";
            indexable4.title = context.getString(R.string.keyboard_settings_category);
            indexable4.screenTitle = screenTitle;
            indexable4.keywords = context.getString(R.string.keywords_keyboard_and_ime);
            indexables.add(indexable4);
            InputMethodSettingValuesWrapper immValues = InputMethodSettingValuesWrapper.getInstance(context);
            immValues.refreshAllInputMethodAndSubtypes();
            String currImeName = immValues.getCurrentInputMethodName(context).toString();
            SearchIndexableRaw indexable5 = new SearchIndexableRaw(context);
            indexable5.key = "current_input_method";
            indexable5.title = context.getString(R.string.current_input_method);
            indexable5.summaryOn = currImeName;
            indexable5.summaryOff = currImeName;
            indexable5.screenTitle = screenTitle;
            indexables.add(indexable5);
            InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService("input_method");
            List<InputMethodInfo> inputMethods = immValues.getInputMethodList();
            int inputMethodCount = inputMethods == null ? 0 : inputMethods.size();
            for (int i = 0; i < inputMethodCount; i++) {
                InputMethodInfo inputMethod = inputMethods.get(i);
                List<InputMethodSubtype> subtypes = inputMethodManager.getEnabledInputMethodSubtypeList(inputMethod, true);
                String summary2 = InputMethodAndSubtypeUtil.getSubtypeLocaleNameListAsSentence(subtypes, context, inputMethod);
                ServiceInfo serviceInfo = inputMethod.getServiceInfo();
                ComponentName componentName = new ComponentName(serviceInfo.packageName, serviceInfo.name);
                SearchIndexableRaw indexable6 = new SearchIndexableRaw(context);
                indexable6.key = componentName.flattenToString();
                indexable6.title = inputMethod.loadLabel(context.getPackageManager()).toString();
                indexable6.summaryOn = summary2;
                indexable6.summaryOff = summary2;
                indexable6.screenTitle = screenTitle;
                indexables.add(indexable6);
            }
            InputManager inputManager = (InputManager) context.getSystemService("input");
            boolean hasHardKeyboards = false;
            int[] devices = InputDevice.getDeviceIds();
            for (int i2 : devices) {
                InputDevice device = InputDevice.getDevice(i2);
                if (device != null && !device.isVirtual() && device.isFullKeyboard()) {
                    hasHardKeyboards = true;
                    InputDeviceIdentifier identifier = device.getIdentifier();
                    String keyboardLayoutDescriptor = inputManager.getCurrentKeyboardLayoutForInputDevice(identifier);
                    KeyboardLayout keyboardLayout = keyboardLayoutDescriptor != null ? inputManager.getKeyboardLayout(keyboardLayoutDescriptor) : null;
                    if (keyboardLayout != null) {
                        summary = keyboardLayout.toString();
                    } else {
                        summary = context.getString(R.string.keyboard_layout_default_label);
                    }
                    SearchIndexableRaw indexable7 = new SearchIndexableRaw(context);
                    indexable7.key = device.getName();
                    indexable7.title = device.getName();
                    indexable7.summaryOn = summary;
                    indexable7.summaryOff = summary;
                    indexable7.screenTitle = screenTitle;
                    indexables.add(indexable7);
                }
            }
            if (hasHardKeyboards) {
                SearchIndexableRaw indexable8 = new SearchIndexableRaw(context);
                indexable8.key = "builtin_keyboard_settings";
                indexable8.title = context.getString(R.string.builtin_keyboard_settings_title);
                indexable8.screenTitle = screenTitle;
                indexables.add(indexable8);
            }
            TtsEngines ttsEngines = new TtsEngines(context);
            if (!ttsEngines.getEngines().isEmpty()) {
                SearchIndexableRaw indexable9 = new SearchIndexableRaw(context);
                indexable9.key = "tts_settings";
                indexable9.title = context.getString(R.string.tts_settings_title);
                indexable9.screenTitle = screenTitle;
                indexable9.keywords = context.getString(R.string.keywords_text_to_speech_output);
                indexables.add(indexable9);
            }
            SearchIndexableRaw indexable10 = new SearchIndexableRaw(context);
            indexable10.key = "pointer_settings_category";
            indexable10.title = context.getString(R.string.pointer_settings_category);
            indexable10.screenTitle = screenTitle;
            indexables.add(indexable10);
            SearchIndexableRaw indexable11 = new SearchIndexableRaw(context);
            indexable11.key = "pointer_speed";
            indexable11.title = context.getString(R.string.pointer_speed);
            indexable11.screenTitle = screenTitle;
            indexables.add(indexable11);
            if (InputMethodAndLanguageSettings.haveInputDeviceWithVibrator()) {
                SearchIndexableRaw indexable12 = new SearchIndexableRaw(context);
                indexable12.key = "vibrate_input_devices";
                indexable12.title = context.getString(R.string.vibrate_input_devices);
                indexable12.summaryOn = context.getString(R.string.vibrate_input_devices_summary);
                indexable12.summaryOff = context.getString(R.string.vibrate_input_devices_summary);
                indexable12.screenTitle = screenTitle;
                indexables.add(indexable12);
            }
            return indexables;
        }
    };
    private int mDefaultInputMethodSelectorVisibility = 0;
    private final ArrayList<InputMethodPreference> mInputMethodPreferenceList = new ArrayList<>();
    private final ArrayList<PreferenceScreen> mHardKeyboardPreferenceList = new ArrayList<>();

    @Override
    protected int getMetricsCategory() {
        return 57;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.language_settings);
        Activity activity = getActivity();
        this.mImm = (InputMethodManager) getSystemService("input_method");
        this.mInputMethodSettingValues = InputMethodSettingValuesWrapper.getInstance(activity);
        try {
            this.mDefaultInputMethodSelectorVisibility = Integer.valueOf(getString(R.string.input_method_selector_visibility_default_value)).intValue();
        } catch (NumberFormatException e) {
        }
        if (activity.getAssets().getLocales().length == 1) {
            getPreferenceScreen().removePreference(findPreference("phone_language"));
        } else {
            this.mLanguagePref = findPreference("phone_language");
        }
        new VoiceInputOutputSettings(this).onCreate();
        this.mHardKeyboardCategory = (PreferenceCategory) findPreference("hard_keyboard");
        this.mKeyboardSettingsCategory = (PreferenceCategory) findPreference("keyboard_settings_category");
        this.mGameControllerCategory = (PreferenceCategory) findPreference("game_controller_settings_category");
        Intent startingIntent = activity.getIntent();
        this.mShowsOnlyFullImeAndKeyboardList = "android.settings.INPUT_METHOD_SETTINGS".equals(startingIntent.getAction());
        if (this.mShowsOnlyFullImeAndKeyboardList) {
            getPreferenceScreen().removeAll();
            if (this.mHardKeyboardCategory != null) {
                getPreferenceScreen().addPreference(this.mHardKeyboardCategory);
            }
            if (this.mKeyboardSettingsCategory != null) {
                this.mKeyboardSettingsCategory.removeAll();
                getPreferenceScreen().addPreference(this.mKeyboardSettingsCategory);
            }
        }
        this.mIm = (InputManager) activity.getSystemService("input");
        updateInputDevices();
        Preference spellChecker = findPreference("spellcheckers_settings");
        if (spellChecker != null) {
            InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(spellChecker);
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setClass(activity, SubSettings.class);
            intent.putExtra(":settings:show_fragment", SpellCheckersSettings.class.getName());
            intent.putExtra(":settings:show_fragment_title_resid", R.string.spellcheckers_settings_title);
            spellChecker.setIntent(intent);
        }
        this.mHandler = new Handler();
        this.mSettingsObserver = new SettingsObserver(this.mHandler, activity);
        this.mDpm = (DevicePolicyManager) getActivity().getSystemService("device_policy");
        InputDeviceIdentifier identifier = (InputDeviceIdentifier) startingIntent.getParcelableExtra("input_device_identifier");
        if (this.mShowsOnlyFullImeAndKeyboardList && identifier != null) {
            showKeyboardLayoutDialog(identifier);
        }
        PreferenceCategory voiceCategory = (PreferenceCategory) findPreference("voice_category");
        PreferenceCategory pointerSettingsCategory = (PreferenceCategory) findPreference("pointer_settings_category");
        this.mInputMethodExts = new InputMethodExts(activity, this.mShowsOnlyFullImeAndKeyboardList, voiceCategory, pointerSettingsCategory);
        this.mInputMethodExts.initExtendsItems();
    }

    private void updateUserDictionaryPreference(Preference userDictionaryPreference) {
        Activity activity = getActivity();
        final TreeSet<String> localeSet = UserDictionaryList.getUserDictionaryLocalesSet(activity);
        if (localeSet == null) {
            getPreferenceScreen().removePreference(userDictionaryPreference);
        } else {
            userDictionaryPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    Class<? extends Fragment> targetFragment;
                    Bundle extras = new Bundle();
                    if (localeSet.size() <= 1) {
                        if (!localeSet.isEmpty()) {
                            extras.putString("locale", (String) localeSet.first());
                        }
                        targetFragment = UserDictionarySettings.class;
                    } else {
                        targetFragment = UserDictionaryList.class;
                    }
                    InputMethodAndLanguageSettings.this.startFragment(InputMethodAndLanguageSettings.this, targetFragment.getCanonicalName(), -1, -1, extras);
                    return true;
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        this.mSettingsObserver.resume();
        this.mIm.registerInputDeviceListener(this, null);
        Preference spellChecker = findPreference("spellcheckers_settings");
        if (spellChecker != null) {
            TextServicesManager tsm = (TextServicesManager) getSystemService("textservices");
            if (!tsm.isSpellCheckerEnabled()) {
                spellChecker.setSummary(R.string.switch_off_text);
            } else {
                SpellCheckerInfo sci = tsm.getCurrentSpellChecker();
                if (sci != null) {
                    spellChecker.setSummary(sci.loadLabel(getPackageManager()));
                } else {
                    spellChecker.setSummary(R.string.spell_checker_not_selected);
                }
            }
        }
        if (!this.mShowsOnlyFullImeAndKeyboardList) {
            if (this.mLanguagePref != null) {
                String localeNames = getLocaleNames(getActivity());
                this.mLanguagePref.setSummary(localeNames);
            }
            updateUserDictionaryPreference(findPreference("key_user_dictionary_settings"));
        }
        updateInputDevices();
        this.mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        updateInputMethodPreferenceViews();
        this.mInputMethodExts.resumeExtendsItems();
    }

    @Override
    public void onPause() {
        super.onPause();
        this.mIm.unregisterInputDeviceListener(this);
        this.mSettingsObserver.pause();
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(), this.mInputMethodSettingValues.getInputMethodList(), !this.mHardKeyboardPreferenceList.isEmpty());
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        updateInputDevices();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        updateInputDevices();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        updateInputDevices();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        if (preference instanceof PreferenceScreen) {
            if (preference.getFragment() == null && "current_input_method".equals(preference.getKey())) {
                InputMethodManager imm = (InputMethodManager) getSystemService("input_method");
                imm.showInputMethodPicker(false);
            }
        } else if (preference instanceof SwitchPreference) {
            SwitchPreference pref = (SwitchPreference) preference;
            if (pref == this.mGameControllerCategory.findPreference("vibrate_input_devices")) {
                Settings.System.putInt(getContentResolver(), "vibrate_input_devices", pref.isChecked() ? 1 : 0);
                return true;
            }
        } else {
            this.mInputMethodExts.onClickExtendsItems(preference.getKey());
        }
        return super.onPreferenceTreeClick(preference);
    }

    public static String getLocaleNames(Context context) {
        LocaleList locales = LocalePicker.getLocales();
        Locale displayLocale = Locale.getDefault();
        return LocaleHelper.toSentenceCase(LocaleHelper.getDisplayLocaleList(locales, displayLocale, 2), displayLocale);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        return false;
    }

    private void updateInputMethodPreferenceViews() {
        List<InputMethodInfo> imis;
        boolean zContains;
        if (this.mKeyboardSettingsCategory == null) {
            return;
        }
        synchronized (this.mInputMethodPreferenceList) {
            Iterator pref$iterator = this.mInputMethodPreferenceList.iterator();
            while (pref$iterator.hasNext()) {
                this.mKeyboardSettingsCategory.removePreference((InputMethodPreference) pref$iterator.next());
            }
            this.mInputMethodPreferenceList.clear();
            List<String> permittedList = this.mDpm.getPermittedInputMethodsForCurrentUser();
            Context context = getPrefContext();
            if (this.mShowsOnlyFullImeAndKeyboardList) {
                imis = this.mInputMethodSettingValues.getInputMethodList();
            } else {
                imis = this.mImm.getEnabledInputMethodList();
            }
            int N = imis == null ? 0 : imis.size();
            for (int i = 0; i < N; i++) {
                InputMethodInfo imi = imis.get(i);
                if (permittedList == null) {
                    zContains = true;
                } else {
                    zContains = permittedList.contains(imi.getPackageName());
                }
                this.mInputMethodPreferenceList.add(new InputMethodPreference(context, imi, this.mShowsOnlyFullImeAndKeyboardList, zContains, this));
            }
            final Collator collator = Collator.getInstance();
            Collections.sort(this.mInputMethodPreferenceList, new Comparator<InputMethodPreference>() {
                @Override
                public int compare(InputMethodPreference lhs, InputMethodPreference rhs) {
                    return lhs.compareTo(rhs, collator);
                }
            });
            for (int i2 = 0; i2 < N; i2++) {
                InputMethodPreference pref = this.mInputMethodPreferenceList.get(i2);
                this.mKeyboardSettingsCategory.addPreference(pref);
                InputMethodAndSubtypeUtil.removeUnnecessaryNonPersistentPreference(pref);
                pref.updatePreferenceViews();
            }
        }
        updateCurrentImeName();
        InputMethodAndSubtypeUtil.loadInputMethodSubtypeList(this, getContentResolver(), this.mInputMethodSettingValues.getInputMethodList(), null);
    }

    @Override
    public void onSaveInputMethodPreference(InputMethodPreference pref) {
        InputMethodInfo imi = pref.getInputMethodInfo();
        if (!pref.isChecked()) {
            saveEnabledSubtypesOf(imi);
        }
        boolean hasHardwareKeyboard = getResources().getConfiguration().keyboard == 2;
        InputMethodAndSubtypeUtil.saveInputMethodSubtypeList(this, getContentResolver(), this.mImm.getInputMethodList(), hasHardwareKeyboard);
        this.mInputMethodSettingValues.refreshAllInputMethodAndSubtypes();
        if (pref.isChecked()) {
            restorePreviouslyEnabledSubtypesOf(imi);
        }
        for (InputMethodPreference p : this.mInputMethodPreferenceList) {
            p.updatePreferenceViews();
        }
    }

    private void saveEnabledSubtypesOf(InputMethodInfo imi) {
        HashSet<String> enabledSubtypeIdSet = new HashSet<>();
        List<InputMethodSubtype> enabledSubtypes = this.mImm.getEnabledInputMethodSubtypeList(imi, true);
        for (InputMethodSubtype subtype : enabledSubtypes) {
            String subtypeId = Integer.toString(subtype.hashCode());
            enabledSubtypeIdSet.add(subtypeId);
        }
        HashMap<String, HashSet<String>> imeToEnabledSubtypeIdsMap = loadPreviouslyEnabledSubtypeIdsMap();
        String imiId = imi.getId();
        imeToEnabledSubtypeIdsMap.put(imiId, enabledSubtypeIdSet);
        savePreviouslyEnabledSubtypeIdsMap(imeToEnabledSubtypeIdsMap);
    }

    private void restorePreviouslyEnabledSubtypesOf(InputMethodInfo imi) {
        HashMap<String, HashSet<String>> imeToEnabledSubtypeIdsMap = loadPreviouslyEnabledSubtypeIdsMap();
        String imiId = imi.getId();
        HashSet<String> enabledSubtypeIdSet = imeToEnabledSubtypeIdsMap.remove(imiId);
        if (enabledSubtypeIdSet == null) {
            return;
        }
        savePreviouslyEnabledSubtypeIdsMap(imeToEnabledSubtypeIdsMap);
        InputMethodAndSubtypeUtil.enableInputMethodSubtypesOf(getContentResolver(), imiId, enabledSubtypeIdSet);
    }

    private HashMap<String, HashSet<String>> loadPreviouslyEnabledSubtypeIdsMap() {
        Context context = getActivity();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String imesAndSubtypesString = prefs.getString("previously_enabled_subtypes", null);
        return InputMethodAndSubtypeUtil.parseInputMethodsAndSubtypesString(imesAndSubtypesString);
    }

    private void savePreviouslyEnabledSubtypeIdsMap(HashMap<String, HashSet<String>> subtypesMap) {
        Context context = getActivity();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String imesAndSubtypesString = InputMethodAndSubtypeUtil.buildInputMethodsAndSubtypesString(subtypesMap);
        prefs.edit().putString("previously_enabled_subtypes", imesAndSubtypesString).apply();
    }

    public void updateCurrentImeName() {
        Preference curPref;
        Context context = getActivity();
        if (context == null || this.mImm == null || (curPref = getPreferenceScreen().findPreference("current_input_method")) == null) {
            return;
        }
        CharSequence curIme = this.mInputMethodSettingValues.getCurrentInputMethodName(context);
        if (TextUtils.isEmpty(curIme)) {
            return;
        }
        synchronized (this) {
            curPref.setSummary(curIme);
        }
    }

    private void updateInputDevices() {
        updateHardKeyboards();
        updateGameControllers();
    }

    private void updateHardKeyboards() {
        if (this.mHardKeyboardCategory == null) {
            return;
        }
        this.mHardKeyboardPreferenceList.clear();
        int[] devices = InputDevice.getDeviceIds();
        for (int i : devices) {
            InputDevice device = InputDevice.getDevice(i);
            if (device != null && !device.isVirtual() && device.isFullKeyboard()) {
                final InputDeviceIdentifier identifier = device.getIdentifier();
                String keyboardLayoutDescriptor = this.mIm.getCurrentKeyboardLayoutForInputDevice(identifier);
                KeyboardLayout keyboardLayout = keyboardLayoutDescriptor != null ? this.mIm.getKeyboardLayout(keyboardLayoutDescriptor) : null;
                PreferenceScreen pref = new PreferenceScreen(getPrefContext(), null);
                pref.setTitle(device.getName());
                if (keyboardLayout != null) {
                    pref.setSummary(keyboardLayout.toString());
                } else {
                    pref.setSummary(R.string.keyboard_layout_default_label);
                }
                pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        InputMethodAndLanguageSettings.this.showKeyboardLayoutDialog(identifier);
                        return true;
                    }
                });
                this.mHardKeyboardPreferenceList.add(pref);
            }
        }
        if (!this.mHardKeyboardPreferenceList.isEmpty()) {
            int i2 = this.mHardKeyboardCategory.getPreferenceCount();
            while (true) {
                int i3 = i2;
                i2 = i3 - 1;
                if (i3 <= 0) {
                    break;
                }
                Preference pref2 = this.mHardKeyboardCategory.getPreference(i2);
                if (pref2.getOrder() < 1000) {
                    this.mHardKeyboardCategory.removePreference(pref2);
                }
            }
            Collections.sort(this.mHardKeyboardPreferenceList);
            int count = this.mHardKeyboardPreferenceList.size();
            for (int i4 = 0; i4 < count; i4++) {
                Preference pref3 = this.mHardKeyboardPreferenceList.get(i4);
                pref3.setOrder(i4);
                this.mHardKeyboardCategory.addPreference(pref3);
            }
            getPreferenceScreen().addPreference(this.mHardKeyboardCategory);
            return;
        }
        getPreferenceScreen().removePreference(this.mHardKeyboardCategory);
    }

    public void showKeyboardLayoutDialog(InputDeviceIdentifier inputDeviceIdentifier) {
        if (((KeyboardLayoutDialogFragment) getFragmentManager().findFragmentByTag("keyboardLayout")) != null) {
            return;
        }
        KeyboardLayoutDialogFragment fragment = new KeyboardLayoutDialogFragment(inputDeviceIdentifier);
        fragment.setTargetFragment(this, 0);
        fragment.show(getActivity().getFragmentManager(), "keyboardLayout");
    }

    @Override
    public void onSetupKeyboardLayouts(InputDeviceIdentifier inputDeviceIdentifier) {
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.setClass(getActivity(), Settings.KeyboardLayoutPickerActivity.class);
        intent.putExtra("input_device_identifier", (Parcelable) inputDeviceIdentifier);
        this.mIntentWaitingForResult = intent;
        startActivityForResult(intent, 0);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (this.mIntentWaitingForResult == null) {
            return;
        }
        InputDeviceIdentifier inputDeviceIdentifier = (InputDeviceIdentifier) this.mIntentWaitingForResult.getParcelableExtra("input_device_identifier");
        this.mIntentWaitingForResult = null;
        showKeyboardLayoutDialog(inputDeviceIdentifier);
    }

    private void updateGameControllers() {
        if (haveInputDeviceWithVibrator()) {
            getPreferenceScreen().addPreference(this.mGameControllerCategory);
            SwitchPreference pref = (SwitchPreference) this.mGameControllerCategory.findPreference("vibrate_input_devices");
            pref.setChecked(Settings.System.getInt(getContentResolver(), "vibrate_input_devices", 1) > 0);
            return;
        }
        getPreferenceScreen().removePreference(this.mGameControllerCategory);
    }

    public static boolean haveInputDeviceWithVibrator() {
        int[] devices = InputDevice.getDeviceIds();
        for (int i : devices) {
            InputDevice device = InputDevice.getDevice(i);
            if (device != null && !device.isVirtual() && device.getVibrator().hasVibrator()) {
                return true;
            }
        }
        return false;
    }

    private class SettingsObserver extends ContentObserver {
        private Context mContext;

        public SettingsObserver(Handler handler, Context context) {
            super(handler);
            this.mContext = context;
        }

        @Override
        public void onChange(boolean selfChange) {
            InputMethodAndLanguageSettings.this.updateCurrentImeName();
        }

        public void resume() {
            ContentResolver cr = this.mContext.getContentResolver();
            cr.registerContentObserver(Settings.Secure.getUriFor("default_input_method"), false, this);
            cr.registerContentObserver(Settings.Secure.getUriFor("selected_input_method_subtype"), false, this);
        }

        public void pause() {
            this.mContext.getContentResolver().unregisterContentObserver(this);
        }
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {
        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this.mContext = context;
            this.mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (!listening) {
                return;
            }
            String localeNames = InputMethodAndLanguageSettings.getLocaleNames(this.mContext);
            this.mSummaryLoader.setSummary(this, localeNames);
        }
    }
}
