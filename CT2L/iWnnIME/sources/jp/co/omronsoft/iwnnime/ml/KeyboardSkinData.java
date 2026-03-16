package jp.co.omronsoft.iwnnime.ml;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import java.util.HashMap;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class KeyboardSkinData {
    private static final int BIT_FLAGS_DISABLE_MODE_ALPHABET = 1;
    private static final int BIT_FLAGS_DISABLE_MODE_ORIGINAL = 8;
    private static final int BIT_FLAGS_DISABLE_MODE_ORIGINAL_ALPHABET = 9;
    public static final String DEFAULT_SKIN_NAME = "jp.co.omronsoft.wnnext.skin.standard_old.WnnKBDSkin_standard_old";
    private static final String KEY_ACTION_ENTER = "_enter";
    private static final String KEY_ACTION_ENTER_DONE = "_done";
    private static final String KEY_ACTION_ENTER_GO = "_go";
    private static final String KEY_ACTION_ENTER_NEXT = "_next";
    private static final String KEY_ACTION_ENTER_PREVIOUS = "_previous";
    private static final String KEY_ACTION_ENTER_SEARCH = "_search";
    private static final String KEY_ACTION_ENTER_SEND = "_send";
    private static final String KEY_ACTION_ENTER_UNSPECIFIED = "_ok";
    private static final String KEY_DISABLE_MODE_ALPHA = "_disable_alpha";
    private static final String KEY_DISABLE_MODE_EXCEPT_NUM = "_disable_except_num";
    private static final String KEY_DISABLE_MODE_ORIGINAL = "_disable_original";
    private static final String KEY_INPUT_MODE_ALPHA = "_alpha";
    private static final String KEY_INPUT_MODE_BOPOMOFO = "_bopomofo";
    private static final String KEY_INPUT_MODE_CYRILLIC = "_cyrillic";
    private static final String KEY_INPUT_MODE_FULL_ALPHA = "_full_alpha";
    private static final String KEY_INPUT_MODE_FULL_KATA = "_full_kata";
    private static final String KEY_INPUT_MODE_FULL_NUM = "_full_num";
    private static final String KEY_INPUT_MODE_HALF_ALPHA = "_half_alpha";
    private static final String KEY_INPUT_MODE_HALF_KATA = "_half_kata";
    private static final String KEY_INPUT_MODE_HALF_NUM = "_half_num";
    private static final String KEY_INPUT_MODE_HANGUL = "_hangul";
    private static final String KEY_INPUT_MODE_HIRA = "_hira";
    private static final String KEY_INPUT_MODE_NUM = "_num";
    private static final String KEY_INPUT_MODE_PHONE = "_phone";
    private static final String KEY_INPUT_MODE_PINYIN = "_pinyin";
    private static final String KEY_KEYBOARD_TYPE_12KEY = "_12key";
    private static final String KEY_KEYBOARD_TYPE_50KEY = "_50key";
    private static final String KEY_KEYBOARD_TYPE_QWERTY = "_qwerty";
    private static final String KEY_SETTING_FLICK_OFF = "_flickoff";
    private static final String KEY_SETTING_FLICK_ON = "_flickon";
    private static final String KEY_SHIFT_ON = "_upper";
    private static final int KEY_TYPE_MAX = 7;
    public static final String TAG_KEY_COLOR = "KeyColor";
    public static final String TAG_KEY_COLOR_2ND = "KeyColor2nd";
    private static KeyboardSkinData mKeySkin;
    public static final String STRING_KEY = "key";
    private static final String STRING_KEYCODE_ENTER = STRING_KEY + String.valueOf(DefaultSoftKeyboard.KEYCODE_ENTER);
    private static final HashMap<Integer, String> RESOURCEID_KEYSTRING_TABLE = new HashMap<Integer, String>() {
        {
            put(Integer.valueOf(R.color.candidate_hint_color), "CandidateHintColor");
            put(Integer.valueOf(R.color.candidate_index_bar_color), "CandidateNumberColor");
            put(Integer.valueOf(R.color.candidate_text), "CandidateColor");
            put(Integer.valueOf(R.color.candidate_text_symbol), "CandidateColor");
            put(Integer.valueOf(R.color.input_mode_key_main_text_color), "InputModeKeyMainColor");
            put(Integer.valueOf(R.color.input_mode_key_main_text_color_stroke), "InputModeKeyMainColorStroke");
            put(Integer.valueOf(R.color.input_mode_key_mode_text_color), "InputModeKeyModeColor");
            put(Integer.valueOf(R.color.input_mode_key_mode_text_color_stroke), "InputModeKeyModeColorStroke");
            put(Integer.valueOf(R.color.input_mode_key_mode_selected_text_color), "InputModeKeyModeSelectedColor");
            put(Integer.valueOf(R.color.input_mode_key_mode_selected_text_color_stroke), "InputModeKeyModeSelectedColorStroke");
            put(Integer.valueOf(R.color.key_background_color_enter), "KeybackgroundEnter");
            put(Integer.valueOf(R.color.key_background_color_space), "KeybackgroundSpace");
            put(Integer.valueOf(R.color.key_hint_letter), "KeyHintColor");
            put(Integer.valueOf(R.color.key_hint_letter_stroke), "KeyHintColorStroke");
            put(Integer.valueOf(R.color.key_hint_ellipsis_color), "KeyHintColor2");
            put(Integer.valueOf(R.color.key_hint_ellipsis_color_stroke), "KeyHintColor2Stroke");
            put(Integer.valueOf(R.color.key_text_color), KeyboardSkinData.TAG_KEY_COLOR);
            put(Integer.valueOf(R.color.key_text_color_stroke), "KeyColorStroke");
            put(Integer.valueOf(R.color.key_text_color_2nd), KeyboardSkinData.TAG_KEY_COLOR_2ND);
            put(Integer.valueOf(R.color.key_text_color_2nd_stroke), "KeyColor2ndStroke");
            put(Integer.valueOf(R.color.read_more_button_background_color), "CandUpDownBackgroundColor");
            put(Integer.valueOf(R.color.read_more_button_color), "CandUpDownColor");
            put(Integer.valueOf(R.color.slide_popup_background_color), "SlidePopupBackgroundNormal");
            put(Integer.valueOf(R.color.slide_popup_background_color_focused), "SlidePopupBackgroundFocused");
            put(Integer.valueOf(R.color.slide_popup_top_color), "SlidePopupNormal");
            put(Integer.valueOf(R.color.slide_popup_top_color_focused), "SlidePopupFocused");
            put(Integer.valueOf(R.color.tab_textcolor_no_select), "TabNoSelectColor");
            put(Integer.valueOf(R.color.tab_textcolor_select), "TabSelectColor");
            put(Integer.valueOf(R.color.webapi_text_candidate), "WebApiCandidateColor");
            put(Integer.valueOf(R.color.webapi_text_key), "WebApiButtonColor");
            put(Integer.valueOf(R.color.webapi_text_nocandidate), "NoCandidateColor");
            put(Integer.valueOf(R.dimen.key_label_text_size), "keyLabelTextSize");
            put(Integer.valueOf(R.dimen.key_text_size_default), "KeyTextSizeDefault");
            put(Integer.valueOf(R.drawable.cand_index_bar), "CandidateNumberBackground");
            put(Integer.valueOf(R.drawable.tab_no_select), "tab_no_select");
            put(Integer.valueOf(R.drawable.tab_press), "tab_press");
            put(Integer.valueOf(R.drawable.tab_select), "tab_select");
            put(Integer.valueOf(R.drawable.cand_down), "cand_down");
            put(Integer.valueOf(R.drawable.cand_up), "cand_up");
            put(Integer.valueOf(R.string.ti_switch_input_mode_key_mode_original_txt), "OriginalKeyModeLabel");
        }
    };
    private static final HashMap<Integer, String> RESOURCEID_KEYSTRING_TABLE_STATE_LIST = new HashMap<Integer, String>() {
        {
            put(Integer.valueOf(R.drawable.category_background), "CategoryBackground");
        }
    };
    private static final HashMap<String, String> RESOURCEID_KEYSTRING_TABLE_V22_TO_V23 = new HashMap<String, String>() {
        {
            put("sym_keyboard_language_switch_0", "key-412");
            put("sym_keyboard_language_switch_b_0", "key-412_b");
            put("key_del_0", "key-100");
            put("key_del_1", "key-234");
            put("key_del_b_0", "key-100_b");
            put("key_del_b_1", "key-234_b");
            put("key_enter_0", "key-101_enter");
            put("key_enter_b_0", "key-101_enter_b");
            put("key_enter_search_0", "key-101_search");
            put("key_enter_search_b_0", "key-101_search_b");
            put("key_mode_full_alpha_0", "key-114_full_alpha");
            put("key_mode_full_kata_0", "key-114_full_kata");
            put("key_mode_full_num_0", "key-114_full_num");
            put("key_mode_half_alpha_0", "key-114_half_alpha");
            put("key_mode_half_kata_0", "key-114_half_kata");
            put("key_mode_half_num_0", "key-114_half_num");
            put("key_mode_hira_0", "key-114_hira");
            put("key_mode_change_b_0", "key-114_ja_b");
            put("key_pict_sym_b_0", "key-106_ja_b");
            put("key_12key_pict_sym_0", "key-106_12key");
            put("key_12key_eisukana_0", "key-305");
            put("key_12key_eisukana_b_0", "key-305_b");
            put("key_12key_left_0", "key-218_12key");
            put("key_12key_left_b_0", "key-218_12key_b");
            put("key_12key_right_0", "key-217_12key");
            put("key_12key_right_b_0", "key-217_12key_b");
            put("key_12key_reverse_0", "key-219");
            put("key_12key_reverse_b_0", "key-219_b");
            put("key_12key_space_0", "key32_12key");
            put("key_12key_space_b_0", "key32_12key_b");
            put("key_12key_space_jp_0", "key32_12key_hira");
            put("key_12key_space_jp_b_0", "key32_12key_hira_b");
            put("key_12key_voice_0", "key-311_12key");
            put("key_12key_voice_b_0", "key-311_12key_b");
            put("key_12key_caps_0", "key-213_full_alpha");
            put("key_12key_caps_1", "key-213_half_alpha");
            put("key_12key_caps_b_0", "key-213_full_alpha_b");
            put("key_12key_caps_b_1", "key-213_half_alpha_b");
            put("key_12key_dakuten_0", "key-213_full_kata");
            put("key_12key_dakuten_1", "key-213_half_kata");
            put("key_12key_dakuten_2", "key-213_hira");
            put("key_12key_dakuten_b_0", "key-213_full_kata_b");
            put("key_12key_dakuten_b_1", "key-213_half_kata_b");
            put("key_12key_dakuten_b_2", "key-213_hira_b");
            put("key_12key_period_comma_0", "key-211_full_alpha");
            put("key_12key_period_comma_1", "key-211_half_alpha");
            put("key_12key_period_comma_b_0", "key-211_full_alpha_b");
            put("key_12key_period_comma_b_1", "key-211_half_alpha_b");
            put("key_12key_ten_0", "key-211_full_kata");
            put("key_12key_ten_1", "key-211_half_kata");
            put("key_12key_ten_2", "key-211_hira");
            put("key_12key_ten_b_0", "key-211_full_kata_b");
            put("key_12key_ten_b_1", "key-211_half_kata_b");
            put("key_12key_ten_b_2", "key-211_hira_b");
            put("key_12key_alpha0_0", "key-210_full_alpha");
            put("key_12key_alpha0_1", "key-210_half_alpha");
            put("key_12key_alpha1_0", "key-201_full_alpha");
            put("key_12key_alpha1_1", "key-201_half_alpha");
            put("key_12key_alpha2_0", "key-202_full_alpha");
            put("key_12key_alpha2_1", "key-202_half_alpha");
            put("key_12key_alpha3_0", "key-203_full_alpha");
            put("key_12key_alpha3_1", "key-203_half_alpha");
            put("key_12key_alpha4_0", "key-204_full_alpha");
            put("key_12key_alpha4_1", "key-204_half_alpha");
            put("key_12key_alpha5_0", "key-205_full_alpha");
            put("key_12key_alpha5_1", "key-205_half_alpha");
            put("key_12key_alpha6_0", "key-206_full_alpha");
            put("key_12key_alpha6_1", "key-206_half_alpha");
            put("key_12key_alpha7_0", "key-207_full_alpha");
            put("key_12key_alpha7_1", "key-207_half_alpha");
            put("key_12key_alpha8_0", "key-208_full_alpha");
            put("key_12key_alpha8_1", "key-208_half_alpha");
            put("key_12key_alpha9_0", "key-209_full_alpha");
            put("key_12key_alpha9_1", "key-209_half_alpha");
            put("key_12key_alpha0_b_0", "key-210_full_alpha_b");
            put("key_12key_alpha0_b_1", "key-210_half_alpha_b");
            put("key_12key_alpha1_b_0", "key-201_full_alpha_b");
            put("key_12key_alpha1_b_1", "key-201_half_alpha_b");
            put("key_12key_alpha2_b_0", "key-202_full_alpha_b");
            put("key_12key_alpha2_b_1", "key-202_half_alpha_b");
            put("key_12key_alpha3_b_0", "key-203_full_alpha_b");
            put("key_12key_alpha3_b_1", "key-203_half_alpha_b");
            put("key_12key_alpha4_b_0", "key-204_full_alpha_b");
            put("key_12key_alpha4_b_1", "key-204_half_alpha_b");
            put("key_12key_alpha5_b_0", "key-205_full_alpha_b");
            put("key_12key_alpha5_b_1", "key-205_half_alpha_b");
            put("key_12key_alpha6_b_0", "key-206_full_alpha_b");
            put("key_12key_alpha6_b_1", "key-206_half_alpha_b");
            put("key_12key_alpha7_b_0", "key-207_full_alpha_b");
            put("key_12key_alpha7_b_1", "key-207_half_alpha_b");
            put("key_12key_alpha8_b_0", "key-208_full_alpha_b");
            put("key_12key_alpha8_b_1", "key-208_half_alpha_b");
            put("key_12key_alpha9_b_0", "key-209_full_alpha_b");
            put("key_12key_alpha9_b_1", "key-209_half_alpha_b");
            put("key_12key_hiragana0_0", "key-210_hira");
            put("key_12key_hiragana1_0", "key-201_hira");
            put("key_12key_hiragana2_0", "key-202_hira");
            put("key_12key_hiragana3_0", "key-203_hira");
            put("key_12key_hiragana4_0", "key-204_hira");
            put("key_12key_hiragana5_0", "key-205_hira");
            put("key_12key_hiragana6_0", "key-206_hira");
            put("key_12key_hiragana7_0", "key-207_hira");
            put("key_12key_hiragana8_0", "key-208_hira");
            put("key_12key_hiragana9_0", "key-209_hira");
            put("key_12key_hiragana0_b_0", "key-210_hira_b");
            put("key_12key_hiragana1_b_0", "key-201_hira_b");
            put("key_12key_hiragana2_b_0", "key-202_hira_b");
            put("key_12key_hiragana3_b_0", "key-203_hira_b");
            put("key_12key_hiragana4_b_0", "key-204_hira_b");
            put("key_12key_hiragana5_b_0", "key-205_hira_b");
            put("key_12key_hiragana6_b_0", "key-206_hira_b");
            put("key_12key_hiragana7_b_0", "key-207_hira_b");
            put("key_12key_hiragana8_b_0", "key-208_hira_b");
            put("key_12key_hiragana9_b_0", "key-209_hira_b");
            put("key_12key_katakana0_b_flick_guide_0", "key-210_full_kata_flickon_b");
            put("key_12key_katakana0_b_flick_guide_1", "key-210_half_kata_flickon_b");
            put("key_12key_katakana1_b_flick_guide_0", "key-201_full_kata_flickon_b");
            put("key_12key_katakana1_b_flick_guide_1", "key-201_half_kata_flickon_b");
            put("key_12key_katakana2_b_flick_guide_0", "key-202_full_kata_flickon_b");
            put("key_12key_katakana2_b_flick_guide_1", "key-202_half_kata_flickon_b");
            put("key_12key_katakana3_b_flick_guide_0", "key-203_full_kata_flickon_b");
            put("key_12key_katakana3_b_flick_guide_1", "key-203_half_kata_flickon_b");
            put("key_12key_katakana4_b_flick_guide_0", "key-204_full_kata_flickon_b");
            put("key_12key_katakana4_b_flick_guide_1", "key-204_half_kata_flickon_b");
            put("key_12key_katakana5_b_flick_guide_0", "key-205_full_kata_flickon_b");
            put("key_12key_katakana5_b_flick_guide_1", "key-205_half_kata_flickon_b");
            put("key_12key_katakana6_b_flick_guide_0", "key-206_full_kata_flickon_b");
            put("key_12key_katakana6_b_flick_guide_1", "key-206_half_kata_flickon_b");
            put("key_12key_katakana7_b_flick_guide_0", "key-207_full_kata_flickon_b");
            put("key_12key_katakana7_b_flick_guide_1", "key-207_half_kata_flickon_b");
            put("key_12key_katakana8_b_flick_guide_0", "key-208_full_kata_flickon_b");
            put("key_12key_katakana8_b_flick_guide_1", "key-208_half_kata_flickon_b");
            put("key_12key_katakana9_b_flick_guide_0", "key-209_full_kata_flickon_b");
            put("key_12key_katakana9_b_flick_guide_1", "key-209_half_kata_flickon_b");
            put("key_12key_full_flick_off_katakana0_0", "key-210_full_kata");
            put("key_12key_full_flick_off_katakana0_1", "key-210_half_kata");
            put("key_12key_full_flick_off_katakana1_0", "key-201_full_kata");
            put("key_12key_full_flick_off_katakana1_1", "key-201_half_kata");
            put("key_12key_full_flick_off_katakana2_0", "key-202_full_kata");
            put("key_12key_full_flick_off_katakana2_1", "key-202_half_kata");
            put("key_12key_full_flick_off_katakana3_0", "key-203_full_kata");
            put("key_12key_full_flick_off_katakana3_1", "key-203_half_kata");
            put("key_12key_full_flick_off_katakana4_0", "key-204_full_kata");
            put("key_12key_full_flick_off_katakana4_1", "key-204_half_kata");
            put("key_12key_full_flick_off_katakana5_0", "key-205_full_kata");
            put("key_12key_full_flick_off_katakana5_1", "key-205_half_kata");
            put("key_12key_full_flick_off_katakana6_0", "key-206_full_kata");
            put("key_12key_full_flick_off_katakana6_1", "key-206_half_kata");
            put("key_12key_full_flick_off_katakana7_0", "key-207_full_kata");
            put("key_12key_full_flick_off_katakana7_1", "key-207_half_kata");
            put("key_12key_full_flick_off_katakana8_0", "key-208_full_kata");
            put("key_12key_full_flick_off_katakana8_1", "key-208_half_kata");
            put("key_12key_full_flick_off_katakana9_0", "key-209_full_kata");
            put("key_12key_full_flick_off_katakana9_1", "key-209_half_kata");
            put("key_12key_full_flick_off_number0_0", "key-210_full_num_flickoff");
            put("key_12key_full_flick_off_number0_1", "key-210_half_num_flickoff");
            put("key_12key_full_flick_off_number0_2", "key48_phone");
            put("key_12key_full_flick_off_number1_0", "key-201_full_num_flickoff");
            put("key_12key_full_flick_off_number1_1", "key-201_half_num_flickoff");
            put("key_12key_full_flick_off_number1_2", "key49_phone");
            put("key_12key_full_flick_off_number2_0", "key-202_full_num_flickoff");
            put("key_12key_full_flick_off_number2_1", "key-202_half_num_flickoff");
            put("key_12key_full_flick_off_number2_2", "key50_phone");
            put("key_12key_full_flick_off_number3_0", "key-203_full_num_flickoff");
            put("key_12key_full_flick_off_number3_1", "key-203_half_num_flickoff");
            put("key_12key_full_flick_off_number3_2", "key51_phone");
            put("key_12key_full_flick_off_number4_0", "key-204_full_num_flickoff");
            put("key_12key_full_flick_off_number4_1", "key-204_half_num_flickoff");
            put("key_12key_full_flick_off_number4_2", "key52_phone");
            put("key_12key_full_flick_off_number5_0", "key-205_full_num_flickoff");
            put("key_12key_full_flick_off_number5_1", "key-205_half_num_flickoff");
            put("key_12key_full_flick_off_number5_2", "key53_phone");
            put("key_12key_full_flick_off_number6_0", "key-206_full_num_flickoff");
            put("key_12key_full_flick_off_number6_1", "key-206_half_num_flickoff");
            put("key_12key_full_flick_off_number6_2", "key54_phone");
            put("key_12key_full_flick_off_number7_0", "key-207_full_num_flickoff");
            put("key_12key_full_flick_off_number7_1", "key-207_half_num_flickoff");
            put("key_12key_full_flick_off_number7_2", "key55_phone");
            put("key_12key_full_flick_off_number8_0", "key-208_full_num_flickoff");
            put("key_12key_full_flick_off_number8_1", "key-208_half_num_flickoff");
            put("key_12key_full_flick_off_number8_2", "key56_phone");
            put("key_12key_full_flick_off_number9_0", "key-209_full_num_flickoff");
            put("key_12key_full_flick_off_number9_1", "key-209_half_num_flickoff");
            put("key_12key_full_flick_off_number9_2", "key57_phone");
            put("key_12key_full_flick_off_asterisk_0", "key-213_half_num_flickoff");
            put("key_12key_full_flick_off_asterisk_1", "key42_phone");
            put("key_12key_full_flick_off_asterisk_fullwide_0", "key-213_full_num_flickoff");
            put("key_12key_full_flick_off_sharp_0", "key-211_full_num_flickoff");
            put("key_12key_full_flick_off_sharp_1", "key-211_half_num_flickoff");
            put("key_12key_full_flick_off_sharp_2", "key35_phone");
            put("key_12key_full_number0_0", "key-210_full_num_flickon");
            put("key_12key_full_number1_0", "key-201_full_num_flickon");
            put("key_12key_full_number2_0", "key-202_full_num_flickon");
            put("key_12key_full_number3_0", "key-203_full_num_flickon");
            put("key_12key_full_number4_fullwide_0", "key-204_full_num_flickon");
            put("key_12key_full_number5_0", "key-205_full_num_flickon");
            put("key_12key_full_number6_0", "key-206_full_num_flickon");
            put("key_12key_full_number7_0", "key-207_full_num_flickon");
            put("key_12key_full_number8_0", "key-208_full_num_flickon");
            put("key_12key_full_number9_0", "key-209_full_num_flickon");
            put("key_12key_full_number11_fullwide_0", "key-213_full_num_flickon");
            put("key_12key_full_number12_0", "key-211_full_num_flickon");
            put("key_12key_full_number0_b_0", "key-210_full_num_flickon_b");
            put("key_12key_full_number1_b_0", "key-201_full_num_flickon_b");
            put("key_12key_full_number2_b_0", "key-202_full_num_flickon_b");
            put("key_12key_full_number3_b_0", "key-203_full_num_flickon_b");
            put("key_12key_full_number4_b_fullwide_0", "key-204_full_num_flickon_b");
            put("key_12key_full_number5_b_0", "key-205_full_num_flickon_b");
            put("key_12key_full_number6_b_0", "key-206_full_num_flickon_b");
            put("key_12key_full_number7_b_0", "key-207_full_num_flickon_b");
            put("key_12key_full_number8_b_0", "key-208_full_num_flickon_b");
            put("key_12key_full_number9_b_0", "key-209_full_num_flickon_b");
            put("key_12key_full_number11_b_fullwide_0", "key-213_full_num_flickon_b");
            put("key_12key_full_number12_b_0", "key-211_full_num_flickon_b");
            put("key_12key_half_number0_0", "key-210_half_num_flickon");
            put("key_12key_half_number1_0", "key-201_half_num_flickon");
            put("key_12key_half_number2_0", "key-202_half_num_flickon");
            put("key_12key_half_number3_0", "key-203_half_num_flickon");
            put("key_12key_half_number4_0", "key-204_half_num_flickon");
            put("key_12key_half_number5_0", "key-205_half_num_flickon");
            put("key_12key_half_number6_0", "key-206_half_num_flickon");
            put("key_12key_half_number7_0", "key-207_half_num_flickon");
            put("key_12key_half_number8_0", "key-208_half_num_flickon");
            put("key_12key_half_number9_0", "key-209_half_num_flickon");
            put("key_12key_half_number11_0", "key-213_half_num_flickon");
            put("key_12key_half_number12_0", "key-211_half_num_flickon");
            put("key_12key_half_number0_b_0", "key-210_half_num_flickon_b");
            put("key_12key_half_number1_b_0", "key-201_half_num_flickon_b");
            put("key_12key_half_number2_b_0", "key-202_half_num_flickon_b");
            put("key_12key_half_number3_b_0", "key-203_half_num_flickon_b");
            put("key_12key_half_number4_b_0", "key-204_half_num_flickon_b");
            put("key_12key_half_number5_b_0", "key-205_half_num_flickon_b");
            put("key_12key_half_number6_b_0", "key-206_half_num_flickon_b");
            put("key_12key_half_number7_b_0", "key-207_half_num_flickon_b");
            put("key_12key_half_number8_b_0", "key-208_half_num_flickon_b");
            put("key_12key_half_number9_b_0", "key-209_half_num_flickon_b");
            put("key_12key_half_number11_b_0", "key-213_half_num_flickon_b");
            put("key_12key_half_number12_b_0", "key-211_half_num_flickon_b");
            put("key_12key_phone_minus_0", "key45_phone");
            put("key_12key_phone_n_0", "key78_phone");
            put("key_12key_phone_p_0", "key44_phone");
            put("key_12key_phone_parenthesis_left_0", "key40_phone");
            put("key_12key_phone_parenthesis_right_0", "key41_phone");
            put("key_12key_phone_period_0", "key46_phone");
            put("key_12key_phone_plus_0", "key43_phone");
            put("key_12key_phone_slash_0", "key47_phone");
            put("key_12key_phone_space_0", "key32_phone");
            put("key_12key_phone_w_0", "key59_phone");
            put("key_qwerty_pict_sym_0", "key-106_qwerty_full_alpha");
            put("key_qwerty_pict_sym_1", "key-106_qwerty_full_kata");
            put("key_qwerty_pict_sym_2", "key-106_qwerty_full_num");
            put("key_qwerty_pict_sym_3", "key-106_qwerty_half_alpha");
            put("key_qwerty_pict_sym_4", "key-106_qwerty_half_kata");
            put("key_qwerty_pict_sym_5", "key-106_qwerty_half_num");
            put("key_qwerty_pict_sym_6", "key-106_qwerty_hira");
            put("key_qwerty_shift_0", "key-1");
            put("key_qwerty_shift_b_0", "key-1_b");
            put("key_qwerty_shift_locked_0", "key-1_upper");
            put("key_qwerty_space_0", "key32_qwerty");
            put("key_qwerty_space_b_0", "key32_qwerty_b");
            put("key_qwerty_space_conv_0", "key32_qwerty_hira");
            put("key_qwerty_space_conv_b_0", "key32_qwerty_hira_b");
            put("key_qwerty_left_0", "key-218_qwerty");
            put("key_qwerty_left_b_0", "key-218_qwerty_b");
            put("key_qwerty_right_0", "key-217_qwerty");
            put("key_qwerty_right_b_0", "key-217_qwerty_b");
            put("key_qwerty_up_0", "key-235");
            put("key_qwerty_up_b_0", "key-235_b");
            put("key_qwerty_down_0", "key-236");
            put("key_qwerty_down_b_0", "key-236_b");
            put("key_qwerty_voice_0", "key-311_qwerty");
            put("key_qwerty_voice_b_0", "key-311_qwerty_b");
            put("key_qwerty_a_0", "key97");
            put("key_qwerty_a_1", "key65345");
            put("key_qwerty_at_0", "key64_half_alpha");
            put("key_qwerty_at_1", "key65312_full_alpha");
            put("key_qwerty_at_2", "key64_alpha");
            put("key_qwerty_at_3", "key64_hangul");
            put("key_qwerty_b_0", "key98");
            put("key_qwerty_b_1", "key65346");
            put("key_qwerty_c_0", "key99");
            put("key_qwerty_c_1", "key65347");
            put("key_qwerty_d_0", "key100");
            put("key_qwerty_d_1", "key65348");
            put("key_qwerty_e_0", "key101");
            put("key_qwerty_e_1", "key65349");
            put("key_qwerty_f_0", "key102");
            put("key_qwerty_f_1", "key65350");
            put("key_qwerty_g_0", "key103");
            put("key_qwerty_g_1", "key65351");
            put("key_qwerty_h_0", "key104");
            put("key_qwerty_h_1", "key65352");
            put("key_qwerty_i_0", "key105");
            put("key_qwerty_i_1", "key65353");
            put("key_qwerty_j_0", "key106");
            put("key_qwerty_j_1", "key65354");
            put("key_qwerty_k_0", "key107");
            put("key_qwerty_k_1", "key65355");
            put("key_qwerty_l_0", "key108");
            put("key_qwerty_l_1", "key65356");
            put("key_qwerty_m_0", "key109");
            put("key_qwerty_m_1", "key65357");
            put("key_qwerty_n_0", "key110");
            put("key_qwerty_n_1", "key65358");
            put("key_qwerty_o_0", "key111");
            put("key_qwerty_o_1", "key65359");
            put("key_qwerty_p_0", "key112");
            put("key_qwerty_p_1", "key65360");
            put("key_qwerty_q_0", "key113");
            put("key_qwerty_q_1", "key65361");
            put("key_qwerty_r_0", "key114");
            put("key_qwerty_r_1", "key65362");
            put("key_qwerty_s_0", "key115");
            put("key_qwerty_s_1", "key65363");
            put("key_qwerty_slash_0", "key47_half_alpha");
            put("key_qwerty_slash_1", "key65295_full_alpha");
            put("key_qwerty_slash_2", "key47_alpha");
            put("key_qwerty_slash_3", "key47_hangul");
            put("key_qwerty_t_0", "key116");
            put("key_qwerty_t_1", "key65364");
            put("key_qwerty_tyouon_0", "key12540");
            put("key_qwerty_tyouon_1", "key65392");
            put("key_qwerty_u_0", "key117");
            put("key_qwerty_u_1", "key65365");
            put("key_qwerty_v_0", "key118");
            put("key_qwerty_v_1", "key65366");
            put("key_qwerty_w_0", "key119");
            put("key_qwerty_w_1", "key65367");
            put("key_qwerty_x_0", "key120");
            put("key_qwerty_x_1", "key65368");
            put("key_qwerty_y_0", "key121");
            put("key_qwerty_y_1", "key65369");
            put("key_qwerty_z_0", "key122");
            put("key_qwerty_z_1", "key65370");
            put("key_qwerty_shift_on_a_0", "key65");
            put("key_qwerty_shift_on_a_1", "key65313");
            put("key_qwerty_shift_on_b_0", "key66");
            put("key_qwerty_shift_on_b_1", "key65314");
            put("key_qwerty_shift_on_c_0", "key67");
            put("key_qwerty_shift_on_c_1", "key65315");
            put("key_qwerty_shift_on_d_0", "key68");
            put("key_qwerty_shift_on_d_1", "key65316");
            put("key_qwerty_shift_on_e_0", "key69");
            put("key_qwerty_shift_on_e_1", "key65317");
            put("key_qwerty_shift_on_f_0", "key70");
            put("key_qwerty_shift_on_f_1", "key65318");
            put("key_qwerty_shift_on_g_0", "key71");
            put("key_qwerty_shift_on_g_1", "key65319");
            put("key_qwerty_shift_on_h_0", "key72");
            put("key_qwerty_shift_on_h_1", "key65320");
            put("key_qwerty_shift_on_i_0", "key73");
            put("key_qwerty_shift_on_i_1", "key65321");
            put("key_qwerty_shift_on_j_0", "key74");
            put("key_qwerty_shift_on_j_1", "key65322");
            put("key_qwerty_shift_on_k_0", "key75");
            put("key_qwerty_shift_on_k_1", "key65323");
            put("key_qwerty_shift_on_l_0", "key76");
            put("key_qwerty_shift_on_l_1", "key65324");
            put("key_qwerty_shift_on_m_0", "key77");
            put("key_qwerty_shift_on_m_1", "key65325");
            put("key_qwerty_shift_on_n_0", "key78");
            put("key_qwerty_shift_on_n_1", "key65326");
            put("key_qwerty_shift_on_o_0", "key79");
            put("key_qwerty_shift_on_o_1", "key65327");
            put("key_qwerty_shift_on_p_0", "key80");
            put("key_qwerty_shift_on_p_1", "key65328");
            put("key_qwerty_shift_on_q_0", "key81");
            put("key_qwerty_shift_on_q_1", "key65329");
            put("key_qwerty_shift_on_r_0", "key82");
            put("key_qwerty_shift_on_r_1", "key65330");
            put("key_qwerty_shift_on_s_0", "key83");
            put("key_qwerty_shift_on_s_1", "key65331");
            put("key_qwerty_shift_on_t_0", "key84");
            put("key_qwerty_shift_on_t_1", "key65332");
            put("key_qwerty_shift_on_u_0", "key85");
            put("key_qwerty_shift_on_u_1", "key65333");
            put("key_qwerty_shift_on_v_0", "key86");
            put("key_qwerty_shift_on_v_1", "key65334");
            put("key_qwerty_shift_on_w_0", "key87");
            put("key_qwerty_shift_on_w_1", "key65335");
            put("key_qwerty_shift_on_x_0", "key88");
            put("key_qwerty_shift_on_x_1", "key65336");
            put("key_qwerty_shift_on_y_0", "key89");
            put("key_qwerty_shift_on_y_1", "key65337");
            put("key_qwerty_shift_on_z_0", "key90");
            put("key_qwerty_shift_on_z_1", "key65338");
            put("key_qwerty_num_0_0", "key48");
            put("key_qwerty_num_0_1", "key65296");
            put("key_qwerty_num_1_0", "key49");
            put("key_qwerty_num_1_1", "key65297");
            put("key_qwerty_num_2_0", "key50");
            put("key_qwerty_num_2_1", "key65298");
            put("key_qwerty_num_3_0", "key51");
            put("key_qwerty_num_3_1", "key65299");
            put("key_qwerty_num_4_0", "key52");
            put("key_qwerty_num_4_1", "key65300");
            put("key_qwerty_num_5_0", "key53");
            put("key_qwerty_num_5_1", "key65301");
            put("key_qwerty_num_6_0", "key54");
            put("key_qwerty_num_6_1", "key65302");
            put("key_qwerty_num_7_0", "key55");
            put("key_qwerty_num_7_1", "key65303");
            put("key_qwerty_num_8_0", "key56");
            put("key_qwerty_num_8_1", "key65304");
            put("key_qwerty_num_9_0", "key57");
            put("key_qwerty_num_9_1", "key65305");
            put("key_qwerty_num_ampersand_0", "key38");
            put("key_qwerty_num_ampersand_1", "key65286");
            put("key_qwerty_num_apostrophe_0", "key39");
            put("key_qwerty_num_apostrophe_1", "key8217");
            put("key_qwerty_num_asterisk_0", "key42");
            put("key_qwerty_num_asterisk_fullwide_0", "key65290");
            put("key_qwerty_num_at_0", "key64_half_num");
            put("key_qwerty_num_at_1", "key65312_full_num");
            put("key_qwerty_num_at_2", "key64_num");
            put("key_qwerty_num_bracket_left_0", "key91");
            put("key_qwerty_num_bracket_right_0", "key93");
            put("key_qwerty_num_circumflex_0", "key94");
            put("key_qwerty_num_circumflex_1", "key65342");
            put("key_qwerty_num_colon_0", "key58");
            put("key_qwerty_num_colon_1", "key65306");
            put("key_qwerty_num_comma_0", "key44_half_num");
            put("key_qwerty_num_comma_1", "key65292_full_num");
            put("key_qwerty_num_comma_2", "key44_num");
            put("key_qwerty_num_corner_bracket_left_0", "key12300");
            put("key_qwerty_num_corner_bracket_right_0", "key12301");
            put("key_qwerty_num_curly_bracket_left_0", "key123");
            put("key_qwerty_num_curly_bracket_left_1", "key65371");
            put("key_qwerty_num_curly_bracket_right_0", "key125");
            put("key_qwerty_num_curly_bracket_right_1", "key65373");
            put("key_qwerty_num_dollar_0", "key36");
            put("key_qwerty_num_dollar_fullwide_0", "key65284");
            put("key_qwerty_num_equals_0", "key61");
            put("key_qwerty_num_equals_1", "key65309");
            put("key_qwerty_num_exclamation_0", "key33_half_num");
            put("key_qwerty_num_exclamation_1", "key65281_full_num");
            put("key_qwerty_num_exclamation_2", "key33_num");
            put("key_qwerty_num_grave_0", "key96");
            put("key_qwerty_num_grave_fullwide_0", "key8216");
            put("key_qwerty_num_greater_than_0", "key62");
            put("key_qwerty_num_greater_than_1", "key65310");
            put("key_qwerty_num_less_than_0", "key60");
            put("key_qwerty_num_less_than_1", "key65308");
            put("key_qwerty_num_lowline_0", "key95");
            put("key_qwerty_num_lowline_1", "key65343");
            put("key_qwerty_num_middledot_0", "key12539");
            put("key_qwerty_num_minus_0", "key45");
            put("key_qwerty_num_minus_1", "key65293");
            put("key_qwerty_num_parenthesis_left_0", "key40");
            put("key_qwerty_num_parenthesis_left_1", "key65288");
            put("key_qwerty_num_parenthesis_right_0", "key41");
            put("key_qwerty_num_parenthesis_right_1", "key65289");
            put("key_qwerty_num_percent_0", "key37");
            put("key_qwerty_num_percent_1", "key65285");
            put("key_qwerty_num_period_0", "key46_half_num");
            put("key_qwerty_num_period_1", "key65294_full_num");
            put("key_qwerty_num_period_2", "key46_num");
            put("key_qwerty_num_plus_0", "key43");
            put("key_qwerty_num_plus_1", "key65291");
            put("key_qwerty_num_plusminus_0", "key177");
            put("key_qwerty_num_question_0", "key63_half_num");
            put("key_qwerty_num_question_1", "key65311_full_num");
            put("key_qwerty_num_question_2", "key63_num");
            put("key_qwerty_num_quotation_0", "key34");
            put("key_qwerty_num_quotation_1", "key8221");
            put("key_qwerty_num_reverseslash_0", "key92");
            put("key_qwerty_num_reverseslash_1", "key65340");
            put("key_qwerty_num_semicolon_0", "key59");
            put("key_qwerty_num_semicolon_1", "key65307");
            put("key_qwerty_num_sharp_0", "key35");
            put("key_qwerty_num_sharp_1", "key65283");
            put("key_qwerty_num_slash_0", "key47_half_num");
            put("key_qwerty_num_slash_1", "key65295_full_num");
            put("key_qwerty_num_slash_2", "key47_num");
            put("key_qwerty_num_tilde_0", "key126");
            put("key_qwerty_num_tilde_1", "key65374");
            put("key_qwerty_num_verticalline_0", "key124");
            put("key_qwerty_num_verticalline_1", "key65372");
            put("key_qwerty_num_yen_0", "key165");
            put("key_qwerty_num_yen_1", "key65509");
            put("key_qwerty_full_exclamation_0", "key33_half_kata");
            put("key_qwerty_full_exclamation_1", "key65281_full_kata");
            put("key_qwerty_full_exclamation_2", "key65281_hira");
            put("key_qwerty_full_kuten_0", "key12290");
            put("key_qwerty_full_kuten_1", "key65377");
            put("key_qwerty_full_kuten_toggle_0", "key-413");
            put("key_qwerty_full_kuten_toggle_1", "key-414");
            put("key_qwerty_full_kuten_toggle_b_0", "key-413_b");
            put("key_qwerty_full_kuten_toggle_b_1", "key-414_b");
            put("key_qwerty_full_question_0", "key63_half_kata");
            put("key_qwerty_full_question_1", "key65311_full_kata");
            put("key_qwerty_full_question_2", "key65311_hira");
            put("key_qwerty_full_touten_0", "key12289");
            put("key_qwerty_full_touten_1", "key65380");
            put("key_qwerty_half_comma_0", "key44_half_alpha");
            put("key_qwerty_half_comma_1", "key65292_full_alpha");
            put("key_qwerty_half_comma_2", "key44_alpha");
            put("key_qwerty_half_comma_3", "key44_hangul");
            put("key_qwerty_half_exclamation_0", "key33_half_alpha");
            put("key_qwerty_half_exclamation_1", "key65281_full_alpha");
            put("key_qwerty_half_exclamation_2", "key33_alpha");
            put("key_qwerty_half_exclamation_3", "key33_hangul");
            put("key_qwerty_half_period_0", "key46_half_alpha");
            put("key_qwerty_half_period_1", "key65294_full_alpha");
            put("key_qwerty_half_period_2", "key46_alpha");
            put("key_qwerty_half_period_3", "key46_hangul");
            put("key_qwerty_half_period_toggle_0", "key-415");
            put("key_qwerty_half_period_toggle_1", "key-416");
            put("key_qwerty_half_period_toggle_2", "key-116");
            put("key_qwerty_half_period_toggle_b_0", "key-415_b");
            put("key_qwerty_half_period_toggle_b_1", "key-416_b");
            put("key_qwerty_half_period_toggle_b_2", "key-116_b");
            put("key_qwerty_half_question_0", "key63_half_alpha");
            put("key_qwerty_half_question_1", "key65311_full_alpha");
            put("key_qwerty_half_question_2", "key63_alpha");
            put("key_qwerty_half_question_3", "key63_hangul");
            put("key_qwerty_del_s_0", "key-100_ru_cyrillic");
            put("key_qwerty_del_s_b_0", "key-100_ru_cyrillic_b");
            put("key_qwerty_symbol_0", "key-106_qwerty");
            put("key_qwerty_symbol_b_0", "key-106_b");
            put("key_qwerty_mode_alpha_standard_0", "key-114_en_us_alpha");
            put("key_qwerty_mode_alpha_standard_1", "key-114_en_uk_alpha");
            put("key_qwerty_mode_num_standard_0", "key-114_en_us_num");
            put("key_qwerty_mode_num_standard_1", "key-114_en_uk_num");
            put("key_qwerty_mode_pin_0", "key-114_zh_cn_p_pinyin");
            put("key_qwerty_mode_alpha_pin_0", "key-114_zh_cn_p_alpha");
            put("key_qwerty_mode_num_pin_0", "key-114_zh_cn_p_num");
            put("key_qwerty_mode_bopo_0", "key-114_zh_tw_z_bopomofo");
            put("key_qwerty_mode_alpha_bopo_0", "key-114_zh_tw_z_alpha");
            put("key_qwerty_mode_num_bopo_0", "key-114_zh_tw_z_num");
            put("key_qwerty_mode_hangul_0", "key-114_ko_hangul");
            put("key_qwerty_mode_alpha_hangul_0", "key-114_ko_alpha");
            put("key_qwerty_mode_num_hangul_0", "key-114_ko_num");
            put("key_qwerty_mode_cyri_0", "key-114_ru_cyrillic");
            put("key_qwerty_mode_alpha_cyri_0", "key-114_ru_alpha");
            put("key_qwerty_mode_num_cyri_0", "key-114_ru_num");
            put("key_qwerty_mode_change_standard_b_0", "key-114_b");
            put("key_qwerty_num_currency_0", "key164");
            put("key_qwerty_num_degree_0", "key176");
            put("key_qwerty_num_division_0", "key247");
            put("key_qwerty_num_euro_0", "key8364");
            put("key_qwerty_num_midpoint_0", "key183");
            put("key_qwerty_num_multiplication_0", "key215");
            put("key_qwerty_num_pointing_left_0", "key171");
            put("key_qwerty_num_pointing_right_0", "key187");
            put("key_qwerty_num_pound_0", "key163");
            put("key_qwerty_num_section_0", "key167");
            put("key_qwerty_full_kuten_bopo_0", "key12290_zh_tw_z_bopomofo");
            put("key_qwerty_full_kuten_bopo_toggle_0", "key-150_zh_tw_z_bopomofo");
            put("key_qwerty_full_kuten_bopo_toggle_b_0", "key-150_zh_tw_z_bopomofo_b");
            put("key_qwerty_full_kuten_pinyin_0", "key12290_zh_cn_p_pinyin");
            put("key_qwerty_full_kuten_pinyin_toggle_0", "key-150_zh_cn_p_pinyin");
            put("key_qwerty_full_kuten_pinyin_toggle_b_0", "key-150_zh_cn_p_pinyin_b");
            put("key_qwerty_full_touten_bopo_0", "key65292_zh_tw_z_bopomofo");
            put("key_qwerty_full_touten_pinyin_0", "key65292_zh_cn_p_pinyin");
            put("key_qwerty_exclamation_pinyin_0", "key65281_zh_cn_p_pinyin");
            put("key_qwerty_question_pinyin_0", "key65311_zh_cn_p_pinyin");
        }
    };
    protected String mPackageName = LoggingEvents.EXTRA_CALLING_APP_NAME;
    protected int mResourceId = 0;
    protected HashMap<String, Integer> mResourceHash = null;
    protected PackageManager mPm = null;
    private boolean mIsSetKeyBgFlag = false;
    private boolean mEnableUndoKeyBackground = false;

    protected KeyboardSkinData() {
        mKeySkin = null;
    }

    public static synchronized KeyboardSkinData getInstance() {
        if (mKeySkin == null) {
            mKeySkin = new KeyboardSkinData();
        }
        return mKeySkin;
    }

    public void init(Context context) {
        this.mPm = context.getPackageManager();
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        setPreferences(pref);
    }

    public void setPreferences(SharedPreferences pref) {
        String packagename = DEFAULT_SKIN_NAME.substring(0, DEFAULT_SKIN_NAME.lastIndexOf(46));
        try {
            ComponentName name = new ComponentName(packagename, DEFAULT_SKIN_NAME);
            if (this.mPm != null) {
                ActivityInfo activityInfo = this.mPm.getActivityInfo(name, 128);
                if (activityInfo.metaData != null) {
                    this.mPackageName = packagename;
                    this.mResourceId = activityInfo.metaData.getInt("settingfile");
                    makeResourceHashMap();
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("IWnnIME", "KeyboardSkinData::setPreferences " + e.toString());
        }
    }

    public boolean isValid() {
        return !this.mPackageName.equals(LoggingEvents.EXTRA_CALLING_APP_NAME);
    }

    public XmlResourceParser getSettingXmlParser() {
        if (!isValid() || this.mPm == null) {
            return null;
        }
        XmlResourceParser parser = this.mPm.getXml(this.mPackageName, this.mResourceId, null);
        return parser;
    }

    protected void makeResourceHashMap() {
        this.mResourceHash = null;
        XmlResourceParser parser = getSettingXmlParser();
        if (parser != null) {
            this.mResourceHash = new HashMap<>();
            XmlResourceParser parser2 = getStartTag("Drawable", parser);
            if (parser2 == null) {
                Log.e("IWnnIME", "KeyboardSkinData::makeResourceHashMap getStartTag return Null");
                return;
            }
            while (true) {
                try {
                    int event = parser2.next();
                    if (event != 1) {
                        if (event == 2) {
                            String tag = parser2.getName();
                            Integer resid = Integer.valueOf(parser2.getAttributeResourceValue(0, 0));
                            int cnt = 0;
                            while (true) {
                                if (cnt < 7) {
                                    String tmpTag = tag + "_" + cnt;
                                    if (RESOURCEID_KEYSTRING_TABLE_V22_TO_V23.containsKey(tmpTag)) {
                                        this.mResourceHash.put(RESOURCEID_KEYSTRING_TABLE_V22_TO_V23.get(tmpTag), resid);
                                        cnt++;
                                    } else if (cnt == 0) {
                                        this.mResourceHash.put(tag, resid);
                                    }
                                }
                            }
                        } else if (event == 3 && parser2.getName().equals("Drawable")) {
                            return;
                        }
                    } else {
                        return;
                    }
                } catch (Exception e) {
                    Log.e("IWnnIME", "KeyboardSkinData::makeResourceHashMap " + e.toString());
                    return;
                }
            }
        }
    }

    public Drawable getDrawable(String key) {
        Integer resid;
        if (this.mResourceHash == null || (resid = this.mResourceHash.get(key)) == null || this.mPm == null) {
            return null;
        }
        Drawable result = this.mPm.getDrawable(this.mPackageName, resid.intValue(), null);
        return result;
    }

    public Drawable getDrawable(int resourceid) {
        String key = RESOURCEID_KEYSTRING_TABLE.get(Integer.valueOf(resourceid));
        if (key == null) {
            return null;
        }
        Drawable result = getDrawable(key);
        return result;
    }

    private Drawable getDrawable(int keycode, Keyboard keyboard, int imeAction, String suffix) {
        String key = STRING_KEY + keycode;
        if (keycode == -1 && keyboard.isShifted()) {
            Drawable drawable = getDrawableByKeycode(key + KEY_SHIFT_ON, keyboard, imeAction, suffix);
            if (drawable != null) {
                return drawable;
            }
        } else if (keycode == -222) {
            key = "key-106";
        } else if (keycode == -121) {
            key = "key32";
        }
        return getDrawableByKeycode(key, keyboard, imeAction, suffix);
    }

    private Drawable getDrawableByKeycode(String key, Keyboard keyboard, int imeAction, String suffix) {
        DefaultSoftKeyboard softKeyboard;
        boolean flickable = false;
        String inputModeAffix = LoggingEvents.EXTRA_CALLING_APP_NAME;
        String disableModeAffix = LoggingEvents.EXTRA_CALLING_APP_NAME;
        String keyboardTypeAffix = LoggingEvents.EXTRA_CALLING_APP_NAME;
        int keyboardType = -1;
        String locale = "ja";
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null) {
            locale = LanguageManager.getChosenLocaleCode(wnn.getSelectedLocale());
        }
        if (keyboard != null) {
            int inputMode = keyboard.getKeyboardMode();
            keyboardType = keyboard.getKeyboardType();
            int disableModeBitFlags = 0;
            if (wnn != null && (softKeyboard = wnn.getCurrentDefaultSoftKeyboard()) != null) {
                flickable = softKeyboard.isEnableFlick();
                if (!softKeyboard.isEnableKeyMode(3)) {
                    disableModeBitFlags = 0 | 8;
                }
                if (!softKeyboard.isEnableKeyMode(0)) {
                    disableModeBitFlags |= 1;
                }
                if (!softKeyboard.isEnableKeyMode(1)) {
                    disableModeBitFlags |= 2;
                }
            }
            inputModeAffix = getStringKeyOfInputMode(locale, inputMode);
            disableModeAffix = getStringKeyOfDisableMode(disableModeBitFlags);
            if (keyboardType == 0) {
                keyboardTypeAffix = KEY_KEYBOARD_TYPE_QWERTY;
                if (inputModeAffix.equals(KEY_INPUT_MODE_PHONE)) {
                    keyboardTypeAffix = KEY_KEYBOARD_TYPE_12KEY;
                }
            } else if (keyboardType == 1) {
                keyboardTypeAffix = KEY_KEYBOARD_TYPE_12KEY;
            } else if (keyboardType == 3) {
                keyboardTypeAffix = KEY_KEYBOARD_TYPE_50KEY;
            }
        }
        String localeAffix = "_" + locale;
        ArrayList<String> keyList = new ArrayList<>();
        keyList.add(0, key);
        keyList.add(0, key + localeAffix);
        if (keyboard != null) {
            keyList.add(0, key + keyboardTypeAffix);
            keyList.add(0, key + inputModeAffix);
            if (disableModeAffix.length() > 0) {
                keyList.add(0, key + inputModeAffix + disableModeAffix);
            }
            keyList.add(0, key + localeAffix + inputModeAffix);
            if (disableModeAffix.length() > 0) {
                keyList.add(0, key + localeAffix + inputModeAffix + disableModeAffix);
            }
            keyList.add(0, key + keyboardTypeAffix + inputModeAffix);
            if (keyboardType == 1) {
                if (flickable) {
                    keyList.add(0, key + inputModeAffix + KEY_SETTING_FLICK_ON);
                } else {
                    keyList.add(0, key + inputModeAffix + KEY_SETTING_FLICK_OFF);
                }
            }
        }
        if (key.equals(STRING_KEYCODE_ENTER)) {
            keyList.add(0, key + getStringKeyOfEnter(imeAction));
        }
        for (String eachKey : keyList) {
            Drawable drawable = getDrawable(eachKey + suffix);
            if (drawable != null) {
                return drawable;
            }
        }
        return null;
    }

    public Drawable getDrawable(int keycode, Keyboard keyboard, int imeAction) {
        return getDrawable(keycode, keyboard, imeAction, LoggingEvents.EXTRA_CALLING_APP_NAME);
    }

    public Drawable getDrawable(int keycode, Keyboard keyboard) {
        if (keycode == -101) {
            return null;
        }
        return getDrawable(keycode, keyboard, 0, LoggingEvents.EXTRA_CALLING_APP_NAME);
    }

    public Drawable getDrawablePreview(int keycode, Keyboard keyboard, int imeAction) {
        return getDrawable(keycode, keyboard, imeAction, "_b");
    }

    public Drawable getDrawablePreview(int keycode, Keyboard keyboard) {
        if (keycode == -101) {
            return null;
        }
        return getDrawable(keycode, keyboard, 0, "_b");
    }

    public Resources getResources() {
        if (this.mPm == null) {
            return null;
        }
        try {
            Resources result = this.mPm.getResourcesForApplication(this.mPackageName);
            return result;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e("IWnnIME", "KeyboardSkinData::getResources " + e.toString());
            return null;
        }
    }

    public Integer getColor(String key) {
        XmlResourceParser parser;
        XmlResourceParser parser2 = getSettingXmlParser();
        if (parser2 == null || (parser = getStartTag(key, parser2)) == null) {
            return null;
        }
        int id = parser.getAttributeResourceValue(0, 0);
        if (id == 0) {
            Integer color = Integer.valueOf(parser.getAttributeUnsignedIntValue(0, 0));
            return color;
        }
        Resources r = getResources();
        if (r == null) {
            return null;
        }
        Integer color2 = Integer.valueOf(r.getColor(id));
        return color2;
    }

    public Integer getColor(int resourceid) {
        String key = RESOURCEID_KEYSTRING_TABLE.get(Integer.valueOf(resourceid));
        if (key != null) {
            return getColor(key);
        }
        return null;
    }

    public int getDimen(String key) {
        XmlResourceParser parser;
        XmlResourceParser parser2 = getSettingXmlParser();
        if (parser2 == null || (parser = getStartTag(key, parser2)) == null) {
            return 0;
        }
        int id = parser.getAttributeResourceValue(0, 0);
        if (id == 0) {
            int result = parser.getAttributeUnsignedIntValue(0, 0);
            return result;
        }
        Resources r = getResources();
        if (r == null) {
            return 0;
        }
        int result2 = r.getDimensionPixelSize(id);
        return result2;
    }

    public int getDimen(int resourceid) {
        String key = RESOURCEID_KEYSTRING_TABLE.get(Integer.valueOf(resourceid));
        if (key == null) {
            return 0;
        }
        int result = getDimen(key);
        return result;
    }

    public float getFloat(String key) {
        XmlResourceParser parser;
        XmlResourceParser parser2 = getSettingXmlParser();
        if (parser2 == null || (parser = getStartTag(key, parser2)) == null) {
            return -1.0f;
        }
        float result = parser.getAttributeFloatValue(0, -1.0f);
        return result;
    }

    public String getStateListBaseStrings(int resourceid) {
        return RESOURCEID_KEYSTRING_TABLE_STATE_LIST.get(Integer.valueOf(resourceid));
    }

    public XmlResourceParser getStartTag(String starttag, XmlResourceParser parser) {
        int event;
        if (parser != null) {
            while (true) {
                try {
                    event = parser.next();
                    if (event == 1) {
                        break;
                    }
                    if (event == 2) {
                        String tag = parser.getName();
                        if (starttag.equals(tag)) {
                            break;
                        }
                    }
                } catch (Exception e) {
                    Log.e("IWnnIME", "KeyboardSkinData::getStartTag " + e.toString());
                    return parser;
                }
            }
            if (event == 1) {
                return null;
            }
            return parser;
        }
        return parser;
    }

    public Drawable getKeyBg(Context context, Keyboard keyboard, int keycode, boolean isSecondKey) {
        StateListDrawable keybgdrawable = new StateListDrawable();
        this.mIsSetKeyBgFlag = false;
        if (keycode == -1) {
            setKeyBg(context, keyboard, keycode, keybgdrawable, "_bg_PressShiftOn", "Keybackground2ndPressShiftOn", R.drawable.btn_keyboard_key_pressed_on_2nd, new int[]{android.R.attr.state_checkable, android.R.attr.state_checked, android.R.attr.state_pressed});
            setKeyBg(context, keyboard, keycode, keybgdrawable, "_bg_ShiftOn", "Keybackground2ndShiftOn", R.drawable.btn_keyboard_key_normal_on_2nd, new int[]{android.R.attr.state_checkable, android.R.attr.state_checked});
        }
        String key = isSecondKey ? "Keybackground2ndPress" : "KeybackgroundPress";
        setKeyBg(context, keyboard, keycode, keybgdrawable, "_bg_Press", key, R.color.key_background_color_pressed, new int[]{android.R.attr.state_pressed});
        String key2 = isSecondKey ? "Keybackground2nd" : "Keybackground";
        setKeyBg(context, keyboard, keycode, keybgdrawable, "_bg", key2, R.color.key_background_color_normal, new int[0]);
        if (!this.mIsSetKeyBgFlag) {
            return null;
        }
        return keybgdrawable;
    }

    private void setKeyBg(Context context, Keyboard keyboard, int keycode, StateListDrawable keybgdrawable, String suffix, String key, int resourceId, int[] stateSet) {
        Drawable keybg = getDrawable(keycode, keyboard, 1073741824, suffix);
        if (keybg == null) {
            keybg = getDrawable(key);
            if (keybg == null) {
                keybg = context.getResources().getDrawable(resourceId);
            }
        } else {
            this.mIsSetKeyBgFlag = true;
        }
        if (keybg != null) {
            keybgdrawable.addState(stateSet, keybg);
        }
    }

    public Drawable getKeyBg() {
        StateListDrawable keybgdrawable = new StateListDrawable();
        Drawable keybg = getDrawable("KeybackgroundPressShiftOn");
        if (keybg != null) {
            keybgdrawable.addState(new int[]{android.R.attr.state_checkable, android.R.attr.state_checked, android.R.attr.state_pressed}, keybg);
        }
        Drawable keybg2 = getDrawable("KeybackgroundShiftOn");
        if (keybg2 != null) {
            keybgdrawable.addState(new int[]{android.R.attr.state_checkable, android.R.attr.state_checked}, keybg2);
        }
        Drawable keybg3 = getDrawable("KeybackgroundPress");
        if (keybg3 != null) {
            keybgdrawable.addState(new int[]{android.R.attr.state_pressed}, keybg3);
        }
        Drawable keybg4 = getDrawable("Keybackground");
        if (keybg4 != null) {
            keybgdrawable.addState(new int[0], keybg4);
            return keybgdrawable;
        }
        return null;
    }

    public Drawable getKeyBg2nd() {
        StateListDrawable keybgdrawable = new StateListDrawable();
        Drawable keybg = getDrawable("Keybackground2ndPressShiftOn");
        if (keybg != null) {
            keybgdrawable.addState(new int[]{android.R.attr.state_checkable, android.R.attr.state_checked, android.R.attr.state_pressed}, keybg);
        }
        Drawable keybg2 = getDrawable("Keybackground2ndShiftOn");
        if (keybg2 != null) {
            keybgdrawable.addState(new int[]{android.R.attr.state_checkable, android.R.attr.state_checked}, keybg2);
        }
        Drawable keybg3 = getDrawable("Keybackground2ndPress");
        if (keybg3 != null) {
            keybgdrawable.addState(new int[]{android.R.attr.state_pressed}, keybg3);
        }
        Drawable keybg4 = getDrawable("Keybackground2nd");
        if (keybg4 != null) {
            keybgdrawable.addState(new int[0], keybg4);
            return keybgdrawable;
        }
        return null;
    }

    public Drawable getTab() {
        StateListDrawable tabdrawable = new StateListDrawable();
        Drawable tab = getDrawable("tab_press");
        if (tab != null) {
            tabdrawable.addState(new int[]{android.R.attr.state_pressed}, tab);
        }
        Drawable tab2 = getDrawable("tab_select");
        if (tab2 == null) {
            return null;
        }
        tabdrawable.addState(new int[0], tab2);
        Drawable.ConstantState constantState = tabdrawable.getConstantState();
        if (constantState != null) {
            return constantState.newDrawable(getResources());
        }
        return null;
    }

    public Drawable getTabNoSelect() {
        StateListDrawable tabdrawable = new StateListDrawable();
        Drawable tab = getDrawable("tab_press");
        if (tab != null) {
            tabdrawable.addState(new int[]{android.R.attr.state_pressed}, tab);
        }
        Drawable tab2 = getDrawable("tab_no_select");
        if (tab2 == null) {
            return null;
        }
        tabdrawable.addState(new int[0], tab2);
        Drawable.ConstantState constantState = tabdrawable.getConstantState();
        if (constantState != null) {
            return constantState.newDrawable(getResources());
        }
        return null;
    }

    public Drawable getStateListDrawable(String normal, String press, String focus, String select) {
        StateListDrawable stateDrawable = new StateListDrawable();
        Drawable drawable = getDrawable(press);
        if (drawable != null) {
            stateDrawable.addState(new int[]{android.R.attr.state_pressed}, drawable);
        }
        Drawable drawable2 = getDrawable(select);
        if (drawable2 != null) {
            stateDrawable.addState(new int[]{android.R.attr.state_selected}, drawable2);
        }
        Drawable drawable3 = getDrawable(focus);
        if (drawable3 != null) {
            stateDrawable.addState(new int[]{android.R.attr.state_focused}, drawable3);
        }
        Drawable drawable4 = getDrawable(normal);
        if (drawable4 == null) {
            return null;
        }
        stateDrawable.addState(new int[0], drawable4);
        Drawable.ConstantState constantState = stateDrawable.getConstantState();
        if (constantState != null) {
            return constantState.newDrawable(getResources());
        }
        return null;
    }

    public ColorStateList getColorStateList(String normal, String press, String select) {
        Integer color = getColor(press);
        if (color == null) {
            return null;
        }
        int pressColor = color.intValue();
        Integer color2 = getColor(select);
        if (color2 == null) {
            return null;
        }
        int selectColor = color2.intValue();
        Integer color3 = getColor(normal);
        if (color3 == null) {
            return null;
        }
        int normalColor = color3.intValue();
        int[][] states = {new int[]{android.R.attr.state_pressed}, new int[]{android.R.attr.state_selected}, new int[0]};
        int[] colors = {pressColor, selectColor, normalColor};
        return new ColorStateList(states, colors);
    }

    public String getString(String key) {
        XmlResourceParser parser;
        XmlResourceParser parser2 = getSettingXmlParser();
        if (parser2 == null || (parser = getStartTag(key, parser2)) == null) {
            return null;
        }
        int id = parser.getAttributeResourceValue(0, 0);
        if (id == 0) {
            String result = parser.getAttributeValue(0);
            return result;
        }
        Resources r = getResources();
        if (r == null) {
            return null;
        }
        String result2 = r.getString(id);
        return result2;
    }

    public String getString(int resourceid) {
        String key = RESOURCEID_KEYSTRING_TABLE.get(Integer.valueOf(resourceid));
        if (key == null) {
            return null;
        }
        String result = getString(key);
        return result;
    }

    public double getTargetVersion() {
        String version = getString("TargetVersion");
        if (version == null) {
            return 0.0d;
        }
        try {
            double result = Double.valueOf(version).doubleValue();
            return result;
        } catch (NumberFormatException e) {
            Log.e("IWnnIME", "KeyboardSkinData::getTargetVersion Exception" + e.toString());
            return 0.0d;
        }
    }

    private String getStringKeyOfEnter(int imeAction) {
        switch (imeAction) {
            case 0:
                return KEY_ACTION_ENTER_UNSPECIFIED;
            case 1:
            default:
                return KEY_ACTION_ENTER;
            case 2:
                return KEY_ACTION_ENTER_GO;
            case 3:
                return KEY_ACTION_ENTER_SEARCH;
            case 4:
                return KEY_ACTION_ENTER_SEND;
            case 5:
                return KEY_ACTION_ENTER_NEXT;
            case 6:
                return KEY_ACTION_ENTER_DONE;
            case 7:
                return KEY_ACTION_ENTER_PREVIOUS;
        }
    }

    private String getStringKeyOfInputMode(String locale, int inputMode) {
        String key = LoggingEvents.EXTRA_CALLING_APP_NAME;
        if (locale == null) {
            return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
        if (locale.equals("ja")) {
            switch (inputMode) {
                case 0:
                    key = KEY_INPUT_MODE_HALF_ALPHA;
                    break;
                case 1:
                    key = KEY_INPUT_MODE_HALF_NUM;
                    break;
                case 2:
                    key = KEY_INPUT_MODE_PHONE;
                    break;
                case 3:
                    key = KEY_INPUT_MODE_HIRA;
                    break;
                case 4:
                    key = KEY_INPUT_MODE_FULL_KATA;
                    break;
                case 5:
                    key = KEY_INPUT_MODE_HALF_KATA;
                    break;
                case 6:
                    key = KEY_INPUT_MODE_FULL_ALPHA;
                    break;
                case 7:
                    key = KEY_INPUT_MODE_FULL_NUM;
                    break;
            }
        } else {
            switch (inputMode) {
                case 0:
                    key = KEY_INPUT_MODE_ALPHA;
                    break;
                case 1:
                    key = KEY_INPUT_MODE_NUM;
                    break;
                case 2:
                    key = KEY_INPUT_MODE_PHONE;
                    break;
                case 3:
                    if (locale.equals("zh_cn_p")) {
                        key = KEY_INPUT_MODE_PINYIN;
                    } else if (locale.equals("zh_tw_z")) {
                        key = KEY_INPUT_MODE_BOPOMOFO;
                    } else if (locale.equals("ko")) {
                        key = KEY_INPUT_MODE_HANGUL;
                    } else if (locale.equals("ru")) {
                        key = KEY_INPUT_MODE_CYRILLIC;
                    }
                    break;
            }
        }
        return key;
    }

    private String getStringKeyOfDisableMode(int disableModeBitFlags) {
        switch (disableModeBitFlags) {
            case 1:
                return KEY_DISABLE_MODE_ALPHA;
            case 8:
                return KEY_DISABLE_MODE_ORIGINAL;
            case 9:
                return KEY_DISABLE_MODE_EXCEPT_NUM;
            default:
                return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
    }

    public void setEnableUndoKeyBackground(boolean enable) {
        this.mEnableUndoKeyBackground = enable;
    }

    public boolean getEnableUndoKeyBackground() {
        return this.mEnableUndoKeyBackground;
    }
}
