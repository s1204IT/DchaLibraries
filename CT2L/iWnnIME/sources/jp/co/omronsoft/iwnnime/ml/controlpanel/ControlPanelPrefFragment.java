package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.util.Log;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import jp.co.omronsoft.iwnnime.ml.AdditionalSymbolList;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.KeyboardLanguagePackData;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelStandard;
import jp.co.omronsoft.iwnnime.ml.controlpanel.DownloadDictionaryPreference;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class ControlPanelPrefFragment extends PreferenceFragment implements DialogInterface.OnClickListener, ControlPanelStandard.OnRestartConpaneListener {
    public static final String ADDITIONAL_DICTIONARY_KEY = "additional_dictionary";
    public static final String ADDITIONAL_SYMBOL_LIST_KEY = "opt_add_symbol_list";
    public static final String AUTO_CAPS_KEY = "auto_caps";
    public static final String AUTO_CURSOR_MOVEMENT_KEY = "opt_auto_cursor_movement";
    public static final String AUTO_SPACE_KEY = "opt_auto_space";
    public static final String CANDIDATE_LINES_LANDSCAPE_KEY = "setting_landscape";
    public static final String CANDIDATE_LINES_PORTRAIT_KEY = "setting_portrait";
    public static final String CATEGORY_ASSIST_KEY = "category_assist";
    public static final String CATEGORY_DESIGN_KEY = "category_design";
    public static final String CATEGORY_DICTIONARY_KEY = "category_dictionary";
    public static final String CATEGORY_HARDWAREKEYBOARD_KEY = "category_hardware_keyboard";
    public static final String CATEGORY_KEY_SHOWING_KEY = "category_key_showing";
    public static final String CATEGORY_ROOT_KEY = "iwnnime_pref";
    public static final String CATEGORY_USER_DICTIONARY_KEY = "category_user_dictionary";
    public static final String CHANGE_OTHER_IME_KEY = "opt_change_otherime";
    protected static final boolean DEBUG = false;
    public static final String DEFAULT_CHOOSE_LANGUAGE = "ja";
    private static final String DIALOG_FRAGMENT_TAG = "conpane_fragment_dialog";
    private static final String DIALOG_ID_KEY = "id";
    public static final String DISPLAY_LANGUAGE_SWITCH_KEY = "opt_display_language_switch_key";
    public static final String DISPLAY_LEFT_RIGHT_KEY = "opt_display_left_right_key";
    public static final String DISPLAY_NUMBER_KEY = "opt_display_number_key";
    public static final String DISPLAY_UNDO_KEY = "opt_display_undo_key";
    public static final String DOWNLOAD_DICTIONARY_KEY = "download_dictionary";
    public static final String ENABLE_HALF_ALPHABET_MODE_KEY = "opt_enable_half_alphabet";
    public static final String FLICK_INPUT_KEY = "flick_input";
    public static final String FLICK_SENSITIVITY_KEY = "flick_sensitivity_relative";
    public static final String FLICK_TOGGLE_INPUT_KEY = "flick_toggle_input";
    public static final String FULLSCREEN_MODE_KEY = "fullscreen_mode";
    public static final String HALF_SPACE_INPUT_JA_KEY = "opt_half_space_input_ja";
    public static final String KANA_ROMAN_INPUT_KEY = "kana_roman_input";
    public static final String KEYBOARD_IMAGE_KEY = "keyboard_skin_add";
    public static final String KEY_SOUND_KEY = "key_sound";
    public static final String MUSHROOM_KEY = "opt_mushroom";
    private static final int NUMBER_SET_DISPLAYMODE = 2;
    public static final String OPT_ENABLE_LEARNING_EN_KEY = "opt_enable_learning_en";
    public static final String OPT_ENABLE_LEARNING_JA_KEY = "opt_enable_learning_ja";
    public static final String OPT_FUNFUN_EN_KEY = "opt_funfun_en";
    public static final String OPT_FUNFUN_JA_KEY = "opt_funfun_ja";
    public static final String OPT_HEAD_CONV_KEY = "opt_head_conversion";
    public static final String OPT_PREDICTION_EN_KEY = "opt_prediction_en";
    public static final String OPT_PREDICTION_JA_KEY = "opt_prediction_ja";
    public static final String OPT_SPELL_CORRECTION_EN_KEY = "opt_spell_correction_en";
    public static final String OPT_SPELL_CORRECTION_JA_KEY = "opt_spell_correction_ja";
    public static final String PARENTHESIS_CURSOR_KEY = "parenthesis_cursor_movement";
    public static final String POPUP_PREVIEW_KEY = "popup_preview";
    public static final String SHOW_CANDIDATE_AREA_ALWAYS_KEY = "show_candidate_area_always";
    protected static final String TAG = "iWnn";
    public static final String USER_DICTIONARY_DE_KEY = "user_dictionary_edit_words_de";
    public static final String USER_DICTIONARY_KO_KEY = "user_dictionary_edit_words_ko";
    public static final String USER_DICTIONARY_RU_KEY = "user_dictionary_edit_words_ru";
    public static final String USER_DICTIONARY_ZHCN_KEY = "user_dictionary_edit_words_zhcn";
    public static final String USER_DICTIONARY_ZHTW_KEY = "user_dictionary_edit_words_zhtw";
    public static final String VIBRATION_KEY = "key_vibration";
    public static final String VIBRATION_TIME_KEY = "key_vibration_time";
    private static final int VOICE_INPUT_CONFIRM_DIALOG = -1;
    public static final String VOICE_SETTINGS_KEY = "voice_input";
    public static final String WEBAPI_KEY = "opt_multiwebapi";
    private PreferenceShutterManager mShutterManager;
    private boolean mOkClicked = DEBUG;
    protected SharedPreferences.OnSharedPreferenceChangeListener mChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            ControlPanelPrefFragment.this.onPreferenceChanged(sharedPreferences, key);
        }
    };
    private Preference.OnPreferenceChangeListener mPrefChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            return ControlPanelPrefFragment.this.onPreferenceChange(preference, newValue);
        }
    };

    public static class ControlPanelPrefFragmentDialog extends DialogFragment {
        public static ControlPanelPrefFragmentDialog newInstance(int id) {
            ControlPanelPrefFragmentDialog frag = new ControlPanelPrefFragmentDialog();
            Bundle args = new Bundle();
            args.putInt(ControlPanelPrefFragment.DIALOG_ID_KEY, id);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog dialog = null;
            Activity act = getActivity();
            int id = getArguments().getInt(ControlPanelPrefFragment.DIALOG_ID_KEY);
            DialogInterface.OnClickListener listener = (DialogInterface.OnClickListener) getTargetFragment();
            switch (id) {
                case -1:
                    AlertDialog.Builder builder = new AlertDialog.Builder(act).setTitle(R.string.ti_voice_warning_title_txt).setPositiveButton(R.string.ti_dialog_button_ok_txt, listener).setNegativeButton(R.string.ti_dialog_button_cancel_txt, listener);
                    String message = getString(R.string.ti_voice_warning_may_not_understand_txt) + "\n\n" + getString(R.string.ti_voice_hint_dialog_message_txt);
                    builder.setMessage(message);
                    dialog = builder.create();
                    break;
                default:
                    Log.e(ControlPanelPrefFragment.TAG, "unknown dialog " + id);
                    break;
            }
            if (act instanceof ControlPanelStandard) {
                ((ControlPanelStandard) act).setOnRestartConpaneListener((ControlPanelStandard.OnRestartConpaneListener) getTargetFragment());
            }
            return dialog;
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);
            ((ControlPanelPrefFragment) getTargetFragment()).onDismissDialog();
            Activity act = getActivity();
            if (act instanceof ControlPanelStandard) {
                ((ControlPanelStandard) act).setOnRestartConpaneListener(null);
            }
        }
    }

    public void onDismissDialog() {
        if (this.mOkClicked) {
            ((SwitchPreference) findPreference(VOICE_SETTINGS_KEY)).setChecked(true);
        }
    }

    @Override
    public void OnRestartConpane() {
        onDismissDialog();
    }

    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
        if (whichButton == -1) {
            this.mOkClicked = true;
        }
    }

    private static class PreferenceShutterManager {
        private static final String[][] REMOVABLE_LIST = {new String[]{ControlPanelPrefFragment.CATEGORY_USER_DICTIONARY_KEY, ControlPanelPrefFragment.USER_DICTIONARY_KO_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_USER_DICTIONARY_KEY, ControlPanelPrefFragment.USER_DICTIONARY_DE_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_USER_DICTIONARY_KEY, ControlPanelPrefFragment.USER_DICTIONARY_RU_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_USER_DICTIONARY_KEY, ControlPanelPrefFragment.USER_DICTIONARY_ZHCN_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_USER_DICTIONARY_KEY, ControlPanelPrefFragment.USER_DICTIONARY_ZHTW_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_DESIGN_KEY, ControlPanelPrefFragment.KEYBOARD_IMAGE_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_ASSIST_KEY, ControlPanelPrefFragment.VIBRATION_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_ASSIST_KEY, ControlPanelPrefFragment.VIBRATION_TIME_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_ROOT_KEY, ControlPanelPrefFragment.WEBAPI_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_ROOT_KEY, ControlPanelPrefFragment.CATEGORY_DICTIONARY_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_DICTIONARY_KEY, ControlPanelPrefFragment.ADDITIONAL_DICTIONARY_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_DICTIONARY_KEY, ControlPanelPrefFragment.DOWNLOAD_DICTIONARY_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_ROOT_KEY, ControlPanelPrefFragment.CATEGORY_HARDWAREKEYBOARD_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_HARDWAREKEYBOARD_KEY, ControlPanelPrefFragment.KANA_ROMAN_INPUT_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_ROOT_KEY, ControlPanelPrefFragment.ADDITIONAL_SYMBOL_LIST_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_KEY_SHOWING_KEY, ControlPanelPrefFragment.DISPLAY_LANGUAGE_SWITCH_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_KEY_SHOWING_KEY, ControlPanelPrefFragment.CHANGE_OTHER_IME_KEY}, new String[]{ControlPanelPrefFragment.CATEGORY_KEY_SHOWING_KEY, ControlPanelPrefFragment.DISPLAY_LEFT_RIGHT_KEY}};
        private HashMap<String, PreferenceShutter> mMap;

        private PreferenceShutterManager(PreferenceScreen screen) {
            this.mMap = new HashMap<>();
            int size = REMOVABLE_LIST.length;
            for (int i = 0; i < size; i++) {
                String groupKey = REMOVABLE_LIST[i][0];
                String key = REMOVABLE_LIST[i][1];
                PreferenceShutter ps = new PreferenceShutter(screen, groupKey, key);
                this.mMap.put(key, ps);
            }
        }

        private void showPreferenceByKey(String key, boolean show) {
            PreferenceShutter shutter = this.mMap.get(key);
            if (shutter != null) {
                shutter.showPreference(show);
            }
        }

        private void changeShowingByWebApi(Context context) {
            boolean enable = WebApiListPreference.isEnableWebApi(context);
            showPreferenceByKey(ControlPanelPrefFragment.WEBAPI_KEY, enable);
        }

        private void changeShowingByAdditionalSymbolList(Context context) {
            List<ResolveInfo> resolveInfo = AdditionalSymbolList.getAdditionalSymbolListInfo(context);
            boolean enable = resolveInfo.size() > 0 ? true : ControlPanelPrefFragment.DEBUG;
            showPreferenceByKey(ControlPanelPrefFragment.ADDITIONAL_SYMBOL_LIST_KEY, enable);
        }

        private void changeShowingByVibration(Context context) {
            boolean enable = WnnUtility.hasVibrator(context);
            showPreferenceByKey(ControlPanelPrefFragment.VIBRATION_KEY, enable);
            showPreferenceByKey(ControlPanelPrefFragment.VIBRATION_TIME_KEY, enable);
        }

        private void changeShowingByLanguage(Context context) {
            if (Build.VERSION.SDK_INT < 16) {
                showPreferenceByKey(ControlPanelPrefFragment.CHANGE_OTHER_IME_KEY, ControlPanelPrefFragment.DEBUG);
            } else {
                showPreferenceByKey(ControlPanelPrefFragment.CHANGE_OTHER_IME_KEY, true);
            }
        }

        private void changeShowingByLanguageSwitch(Context context) {
            if (Build.VERSION.SDK_INT < 16) {
                showPreferenceByKey(ControlPanelPrefFragment.DISPLAY_LANGUAGE_SWITCH_KEY, ControlPanelPrefFragment.DEBUG);
                showPreferenceByKey(ControlPanelPrefFragment.CHANGE_OTHER_IME_KEY, ControlPanelPrefFragment.DEBUG);
            }
        }

        private void changeShowingByKeyboardSkinList(Context context) {
            List<ResolveInfo> resolveInfo = WnnUtility.getPackageInfo(context, KeyBoardSkinAddListPreference.KEYBOARDSKINADD_ACTION);
            boolean enable = resolveInfo.size() > 0 ? true : ControlPanelPrefFragment.DEBUG;
            showPreferenceByKey(ControlPanelPrefFragment.KEYBOARD_IMAGE_KEY, enable);
        }

        private void changeShowingByLeftRightKey(Context context) {
            boolean enable = true;
            if (IWnnIME.isTabletMode()) {
                enable = ControlPanelPrefFragment.DEBUG;
            }
            showPreferenceByKey(ControlPanelPrefFragment.DISPLAY_LEFT_RIGHT_KEY, enable);
        }

        private void changeShowingByCategoryDictionary(Context context) {
            boolean enableAdditional = AdditionalDictionaryPreference.isEnableAdditionalDictionary(context);
            boolean enableDownload = DownloadDictionaryPreference.isEnableDownloadDictionary(context);
            if (!enableAdditional && !enableDownload) {
                showPreferenceByKey(ControlPanelPrefFragment.CATEGORY_DICTIONARY_KEY, ControlPanelPrefFragment.DEBUG);
            } else {
                showPreferenceByKey(ControlPanelPrefFragment.CATEGORY_DICTIONARY_KEY, true);
            }
            showPreferenceByKey(ControlPanelPrefFragment.ADDITIONAL_DICTIONARY_KEY, enableAdditional);
            showPreferenceByKey(ControlPanelPrefFragment.DOWNLOAD_DICTIONARY_KEY, enableDownload);
        }
    }

    protected static class PreferenceShutter {
        private boolean mIsShow = true;
        private PreferenceGroup mParent;
        private Preference mTarget;

        public PreferenceShutter(PreferenceScreen screen, String groupKey, String key) {
            this.mParent = (PreferenceGroup) screen.findPreference(groupKey);
            if (this.mParent != null) {
                this.mTarget = this.mParent.findPreference(key);
            }
        }

        public void showPreference(boolean show) {
            if (show != this.mIsShow && this.mParent != null && this.mTarget != null) {
                this.mIsShow = show;
                if (show) {
                    this.mParent.addPreference(this.mTarget);
                } else {
                    this.mParent.removePreference(this.mTarget);
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity act = getActivity();
        String xml = getArguments().getString("fragment");
        int id = getResources().getIdentifier(xml, "xml", act.getPackageName());
        if (id != 0) {
            addPreferencesFromResource(id);
        }
        getPreferenceScreen().setKey(CATEGORY_ROOT_KEY);
        this.mShutterManager = new PreferenceShutterManager(getPreferenceScreen());
        changeShowingByLanguage();
        changeShowingByAutoCursorMovement();
        this.mShutterManager.changeShowingByVibration(act);
        this.mShutterManager.changeShowingByLanguageSwitch(act);
        this.mShutterManager.changeShowingByLeftRightKey(act);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(act);
        sharedPref.registerOnSharedPreferenceChangeListener(this.mChangeListener);
        Preference voicePref = findPreference(VOICE_SETTINGS_KEY);
        if (voicePref != null) {
            voicePref.setOnPreferenceChangeListener(this.mPrefChangeListener);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Activity act = getActivity();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(act);
        sharedPref.unregisterOnSharedPreferenceChangeListener(this.mChangeListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity act = getActivity();
        this.mShutterManager.changeShowingByCategoryDictionary(act);
        changeShowingByKana_Roman_Input();
        this.mShutterManager.changeShowingByWebApi(act);
        this.mShutterManager.changeShowingByAdditionalSymbolList(act);
        if (!KeyBoardSkinAddListPreference.GOOGLE_PLAY_LINK_BUTTON && !KeyBoardSkinAddListPreference.KEYBOARD_IMAGE_LINK_BUTTON) {
            this.mShutterManager.changeShowingByKeyboardSkinList(act);
        }
        this.mShutterManager.changeShowingByLanguage(act);
        changeShowingByLanguage();
        setSummaryOfKeyboardImage();
        setSummaryOfCandidateLines();
        setSummaryOfKeyboardType();
        setSummaryOf50KeyType();
        setSummaryOfAutoCursorMovement();
        setSummaryOfKanaRomanInput();
        setSummaryOfMushroom();
        setSummaryOfWebApi();
        setSummaryOfAdditionalSymbolList();
        setSummaryOfAdditionalDictionary();
        setSummaryOfDownloadDictionary();
        setSummaryOfKeyVibrationTime();
    }

    protected void onPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (FLICK_INPUT_KEY.equals(key) || FLICK_TOGGLE_INPUT_KEY.equals(key)) {
            changeShowingByAutoCursorMovement();
            return;
        }
        if (KEYBOARD_IMAGE_KEY.equals(key)) {
            setSummaryOfKeyboardImage();
            return;
        }
        if (AUTO_CURSOR_MOVEMENT_KEY.equals(key)) {
            setSummaryOfAutoCursorMovement();
            return;
        }
        if (KANA_ROMAN_INPUT_KEY.equals(key)) {
            setSummaryOfKanaRomanInput();
            return;
        }
        if (MUSHROOM_KEY.equals(key)) {
            setSummaryOfMushroom();
            return;
        }
        if (WEBAPI_KEY.equals(key)) {
            setSummaryOfWebApi();
            return;
        }
        if (ADDITIONAL_SYMBOL_LIST_KEY.equals(key)) {
            setSummaryOfAdditionalSymbolList();
            return;
        }
        if (CANDIDATE_LINES_PORTRAIT_KEY.equals(key) || CANDIDATE_LINES_LANDSCAPE_KEY.equals(key)) {
            setSummaryOfCandidateLines();
            return;
        }
        if (key.startsWith(KeyboardTypeListPreference.PREF_KEY_INPUT_MODE_TYPE)) {
            setSummaryOfKeyboardType();
            return;
        }
        if (Keyboard50KeyTypeListPreference.PREF_50KEY_TYPE.equals(key)) {
            setSummaryOf50KeyType();
        } else if (key.equals(KeyVibrateDialogPreference.KEY_VIBRATE_TIME_KEY)) {
            setSummaryOfKeyVibrationTime();
        } else if (ENABLE_HALF_ALPHABET_MODE_KEY.equals(key)) {
            setDependencyParamsForInputMode();
        }
    }

    private boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (!VOICE_SETTINGS_KEY.equals(key) || !(newValue instanceof Boolean) || !((Boolean) newValue).booleanValue()) {
            return true;
        }
        this.mOkClicked = DEBUG;
        ControlPanelPrefFragmentDialog dialog = ControlPanelPrefFragmentDialog.newInstance(-1);
        dialog.setTargetFragment(this, 0);
        dialog.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
        return DEBUG;
    }

    private void changeShowingByLanguage() {
        this.mShutterManager.showPreferenceByKey(USER_DICTIONARY_KO_KEY, isSetSubtype("ko"));
        this.mShutterManager.showPreferenceByKey(USER_DICTIONARY_DE_KEY, isSetSubtype("de"));
        this.mShutterManager.showPreferenceByKey(USER_DICTIONARY_RU_KEY, isSetSubtype("ru"));
        this.mShutterManager.showPreferenceByKey(USER_DICTIONARY_ZHCN_KEY, isSetSubtype("zh_cn_p"));
        this.mShutterManager.showPreferenceByKey(USER_DICTIONARY_ZHTW_KEY, isSetSubtype("zh_tw_z"));
    }

    protected void setSummary(String key, String summary) {
        Preference pref = findPreference(key);
        if (pref != null) {
            pref.setSummary(summary);
        }
    }

    private boolean preferenceExists(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            return true;
        }
        return DEBUG;
    }

    private void setSummaryOfKeyVibrationTime() {
        if (preferenceExists(VIBRATION_TIME_KEY)) {
            Activity act = getActivity();
            Resources res = getResources();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(act);
            int vibrateTime = sharedPref.getInt(KeyVibrateDialogPreference.KEY_VIBRATE_TIME_KEY, res.getInteger(R.integer.vibrate_time_default_value));
            String summary = res.getQuantityString(R.plurals.ti_preference_key_vibration_time_summary_txt, vibrateTime, Integer.valueOf(vibrateTime));
            setSummary(VIBRATION_TIME_KEY, summary);
        }
    }

    private void setSummaryOfKeyboardImage() {
        ActivityInfo activityInfo;
        CharSequence actLabel;
        if (preferenceExists(KEYBOARD_IMAGE_KEY)) {
            Activity act = getActivity();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(act);
            String className = sharedPref.getString(KEYBOARD_IMAGE_KEY, LoggingEvents.EXTRA_CALLING_APP_NAME);
            String summary = getResources().getString(R.string.ti_preference_keyboard_add_default_txt);
            if (!className.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
                PackageManager pm = act.getPackageManager();
                String packagename = className.substring(0, className.lastIndexOf(46));
                summary = className;
                try {
                    ComponentName name = new ComponentName(packagename, className);
                    if (pm != null && (activityInfo = pm.getActivityInfo(name, 0)) != null && (actLabel = activityInfo.loadLabel(pm)) != null) {
                        summary = actLabel.toString();
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "ControlPanelPrefFragmentDisplay::setSummaryOfKeyboardImage()" + e.toString());
                }
            }
            setSummary(KEYBOARD_IMAGE_KEY, summary);
        }
    }

    private void setSummaryOfCandidateLines() {
        if (preferenceExists(CANDIDATE_LINES_PORTRAIT_KEY) || preferenceExists(CANDIDATE_LINES_LANDSCAPE_KEY)) {
            Activity act = getActivity();
            Resources res = getResources();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(act);
            String portraitLines = sharedPref.getString(CANDIDATE_LINES_PORTRAIT_KEY, res.getString(R.string.setting_portrait_default_value));
            String landscapeLines = sharedPref.getString(CANDIDATE_LINES_LANDSCAPE_KEY, res.getString(R.string.setting_landscape_default_value));
            String portraitLines2 = res.getQuantityString(R.plurals.ti_preference_candidate_lines_summary_txt, Integer.valueOf(portraitLines).intValue(), Integer.valueOf(portraitLines));
            String landscapeLines2 = res.getQuantityString(R.plurals.ti_preference_candidate_lines_summary_txt, Integer.valueOf(landscapeLines).intValue(), Integer.valueOf(landscapeLines));
            setSummary(CANDIDATE_LINES_PORTRAIT_KEY, portraitLines2);
            setSummary(CANDIDATE_LINES_LANDSCAPE_KEY, landscapeLines2);
        }
    }

    public void setSummaryOfKeyboardType() {
        if (preferenceExists("input_mode_type_0#0") || preferenceExists("input_mode_type_1#0")) {
            StringBuffer setKey = new StringBuffer(KeyboardTypeListPreference.PREF_KEY_INPUT_MODE_TYPE);
            Activity act = getActivity();
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(act);
            PreferenceScreen screen = getPreferenceScreen();
            Resources res = getResources();
            String tenkeySummary = res.getString(R.string.ti_input_mode_type_entry_54_txt);
            String qwertykeySummary = res.getString(R.string.ti_input_mode_type_entry_qwerty_txt);
            String type50keySummary = res.getString(R.string.ti_input_mode_type_entry_50key_txt);
            String summary = tenkeySummary;
            for (int displaymode = 0; displaymode < 2; displaymode++) {
                setKey.append(String.valueOf(displaymode));
                setKey.append("#");
                for (int mode = 0; mode < KeyboardTypeListPreference.NUMBER_SET_INPUTMODE; mode++) {
                    setKey.append(String.valueOf(mode));
                    String modeType = pref.getString(setKey.toString(), LoggingEvents.EXTRA_CALLING_APP_NAME);
                    if (!KeyboardTypeListPreference.checkKeyboardType(modeType)) {
                        modeType = KeyboardTypeListPreference.getLastKeyboardTypePref(act, mode, displaymode);
                    }
                    Preference preferenceScreen = screen.findPreference(setKey.toString());
                    if (modeType.equals(String.valueOf(0))) {
                        summary = qwertykeySummary;
                    } else if (modeType.equals(String.valueOf(3))) {
                        summary = type50keySummary;
                    }
                    if (preferenceScreen != null) {
                        preferenceScreen.setSummary(summary);
                    }
                    summary = tenkeySummary;
                    int length = setKey.length();
                    int delindex = setKey.lastIndexOf("#");
                    setKey.delete(delindex + 1, length);
                }
                int length2 = setKey.length();
                int delindex2 = KeyboardTypeListPreference.PREF_KEY_INPUT_MODE_TYPE.length();
                setKey.delete(delindex2, length2);
            }
        }
    }

    public void setSummaryOf50KeyType() {
        if (preferenceExists(Keyboard50KeyTypeListPreference.PREF_50KEY_TYPE)) {
            PreferenceScreen screen = getPreferenceScreen();
            Preference preferenceScreen = screen.findPreference(Keyboard50KeyTypeListPreference.PREF_50KEY_TYPE);
            if (preferenceScreen != null) {
                Resources res = getResources();
                String type50keyVRSummary = res.getString(R.string.ti_50key_type_entry_vertical_right_txt);
                String type50keyVLSummary = res.getString(R.string.ti_50key_type_entry_vertical_left_txt);
                String type50keyHSummary = res.getString(R.string.ti_50key_type_entry_horizontal_txt);
                Activity act = getActivity();
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(act);
                String defaultValue = res.getString(R.string.keyboard_50key_type_list_item_vertical_right);
                int keyboard50KeyType = Integer.parseInt(pref.getString(Keyboard50KeyTypeListPreference.PREF_50KEY_TYPE, defaultValue));
                String summary = type50keyVRSummary;
                if (keyboard50KeyType == 1) {
                    summary = type50keyVLSummary;
                } else if (keyboard50KeyType == 2) {
                    summary = type50keyHSummary;
                }
                preferenceScreen.setSummary(summary);
            }
        }
    }

    private void setSummaryOfAutoCursorMovement() {
        if (preferenceExists(AUTO_CURSOR_MOVEMENT_KEY)) {
            Resources res = getResources();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String time = sharedPref.getString(AUTO_CURSOR_MOVEMENT_KEY, res.getString(R.string.auto_cursor_movement_id_default));
            Float fTime = Float.valueOf(Float.valueOf(time).floatValue() / 1000.0f);
            String summary = res.getString(R.string.ti_preference_auto_cursor_movement_summary_off_txt);
            if (!time.equals(res.getString(R.string.auto_cursor_movement_list_item_off))) {
                summary = res.getString(R.string.ti_preference_auto_cursor_movement_summary_on_txt, fTime);
            }
            setSummary(AUTO_CURSOR_MOVEMENT_KEY, summary);
        }
    }

    private void setSummaryOfKanaRomanInput() {
        if (preferenceExists(KANA_ROMAN_INPUT_KEY)) {
            Resources res = getResources();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String mode = sharedPref.getString(KANA_ROMAN_INPUT_KEY, res.getString(R.string.kana_roman_input_default_value));
            String summary = res.getString(R.string.ti_preference_roman_letter_txt);
            if (mode.equals(res.getString(R.string.kana_roman_input_mode_list_item_kana))) {
                summary = res.getString(R.string.ti_preference_kana_letter_txt);
            }
            setSummary(KANA_ROMAN_INPUT_KEY, summary);
        }
    }

    private void setSummaryOfMushroom() {
        if (preferenceExists(MUSHROOM_KEY)) {
            Resources res = getResources();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String mode = sharedPref.getString(MUSHROOM_KEY, res.getString(R.string.mushroom_id_default));
            String summary = res.getString(R.string.ti_preference_mushroom_summary_off_txt);
            if (mode.equals(res.getString(R.string.mushroom_list_item_use))) {
                summary = res.getString(R.string.ti_preference_mushroom_summary_on_txt);
            }
            setSummary(MUSHROOM_KEY, summary);
        }
    }

    private void setSummaryOfWebApi() {
        if (preferenceExists(WEBAPI_KEY)) {
            Resources res = getResources();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Set<String> apiList = sharedPref.getStringSet(WEBAPI_KEY, null);
            int settingCnt = 0;
            if (apiList != null) {
                settingCnt = apiList.size();
            }
            String summary = res.getString(R.string.ti_preference_multiple_selection_summary_txt, Integer.valueOf(settingCnt), 5);
            setSummary(WEBAPI_KEY, summary);
        }
    }

    private void setSummaryOfAdditionalSymbolList() {
        if (preferenceExists(ADDITIONAL_SYMBOL_LIST_KEY)) {
            Resources res = getResources();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
            Set<String> apiList = sharedPref.getStringSet(ADDITIONAL_SYMBOL_LIST_KEY, null);
            int settingCnt = 0;
            if (apiList != null) {
                settingCnt = apiList.size();
            }
            String summary = res.getString(R.string.ti_preference_multiple_selection_summary_txt, Integer.valueOf(settingCnt), 5);
            setSummary(ADDITIONAL_SYMBOL_LIST_KEY, summary);
        }
    }

    private void setSummaryOfAdditionalDictionary() {
        if (preferenceExists(ADDITIONAL_DICTIONARY_KEY)) {
            Resources res = getResources();
            int languageType = LanguageManager.getChosenLanguageType("ja");
            int settingCnt = 0;
            for (int cnt = 1; cnt <= 10; cnt++) {
                String key = AdditionalDictionaryPreference.createAdditionalDictionaryKey(languageType, cnt);
                String dictionaryName = IWnnIME.getStringFromNotResetSettingsPreference(getActivity(), key, null);
                if (dictionaryName != null) {
                    settingCnt++;
                }
            }
            String summary = res.getString(R.string.ti_preference_multiple_selection_summary_txt, Integer.valueOf(settingCnt), 10);
            setSummary(ADDITIONAL_DICTIONARY_KEY, summary);
        }
    }

    private void setSummaryOfDownloadDictionary() {
        if (preferenceExists(DOWNLOAD_DICTIONARY_KEY)) {
            Activity act = getActivity();
            Resources res = getResources();
            ArrayList<DownloadDictionaryPreference.DownloadDictionary> cp = DownloadDictionaryPreference.getDictionaryFromContentProvider(act);
            ArrayList<DownloadDictionaryPreference.DownloadDictionary> fp = DownloadDictionaryPreference.checkDictionaryFilePath(cp);
            ArrayList<DownloadDictionaryPreference.DownloadDictionary> sp = DownloadDictionaryPreference.readDownloadDictionaryFromSharedPreferences(act);
            ArrayList<DownloadDictionaryPreference.DownloadDictionary> dictionary = DownloadDictionaryPreference.checkConsistency(fp, sp, act);
            int settingCnt = 0;
            for (DownloadDictionaryPreference.DownloadDictionary dic : dictionary) {
                for (int cnt = 0; cnt < 10; cnt++) {
                    String file = IWnnIME.getStringFromNotResetSettingsPreference(act, DownloadDictionaryPreference.createSharedPrefKey(cnt, DownloadDictionaryPreference.QUERY_PARAM_FILE), null);
                    if (file != null && file.equals(dic.file())) {
                        settingCnt++;
                    }
                }
            }
            String summary = res.getString(R.string.ti_preference_multiple_selection_summary_txt, Integer.valueOf(settingCnt), 10);
            setSummary(DOWNLOAD_DICTIONARY_KEY, summary);
        }
    }

    private void setDependencyParamsForInputMode() {
        if (preferenceExists(ENABLE_HALF_ALPHABET_MODE_KEY)) {
            Activity act = getActivity();
            Resources res = getResources();
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(act);
            boolean alphabetMode = sharedPref.getBoolean(ENABLE_HALF_ALPHABET_MODE_KEY, res.getBoolean(R.bool.half_alphabet_mode_default_value));
            if (!alphabetMode) {
                Preference pref = findPreference(DISPLAY_LANGUAGE_SWITCH_KEY);
                if (pref instanceof SwitchPreference) {
                    ((SwitchPreference) pref).setChecked(true);
                }
                Preference pref2 = findPreference(CHANGE_OTHER_IME_KEY);
                if (pref2 instanceof SwitchPreference) {
                    ((SwitchPreference) pref2).setChecked(true);
                }
            }
        }
    }

    private void changeShowingByAutoCursorMovement() {
        boolean enable;
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Resources res = getResources();
        if (preference.getBoolean(FLICK_INPUT_KEY, res.getBoolean(R.bool.flick_input_default_value)) && !preference.getBoolean(FLICK_TOGGLE_INPUT_KEY, res.getBoolean(R.bool.flick_toggle_input_default_value))) {
            enable = DEBUG;
        } else {
            enable = true;
        }
        Preference preferenceScreen = getPreferenceScreen().findPreference(AUTO_CURSOR_MOVEMENT_KEY);
        if (preferenceScreen != null) {
            preferenceScreen.setEnabled(enable);
        }
    }

    public void changeShowingByKana_Roman_Input() {
        int hiddenState = getResources().getConfiguration().hardKeyboardHidden;
        if (hiddenState == 1) {
            this.mShutterManager.showPreferenceByKey(CATEGORY_HARDWAREKEYBOARD_KEY, true);
            this.mShutterManager.showPreferenceByKey(KANA_ROMAN_INPUT_KEY, true);
        } else {
            this.mShutterManager.showPreferenceByKey(CATEGORY_HARDWAREKEYBOARD_KEY, DEBUG);
            this.mShutterManager.showPreferenceByKey(KANA_ROMAN_INPUT_KEY, DEBUG);
        }
    }

    private boolean isSetSubtype(String lang) {
        KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
        InputMethodInfo imi = langPack.getMyselfInputMethodInfo(getActivity());
        boolean ret = DEBUG;
        if (imi == null) {
            return DEBUG;
        }
        int cnt = 0;
        while (true) {
            if (cnt >= imi.getSubtypeCount()) {
                break;
            }
            InputMethodSubtype subtype = imi.getSubtypeAt(cnt);
            if (subtype != null) {
                String locale = IWnnIME.getSubtypeLocaleDirect(subtype);
                if (locale.equals(lang)) {
                    ret = true;
                    break;
                }
            }
            cnt++;
        }
        return ret;
    }
}
