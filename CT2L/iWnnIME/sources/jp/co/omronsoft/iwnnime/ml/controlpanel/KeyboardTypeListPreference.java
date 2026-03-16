package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.ListView;
import com.android.common.speech.LoggingEvents;
import java.util.HashMap;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.jajp.DefaultSoftKeyboardJAJP;

public class KeyboardTypeListPreference extends KeyboardTypeBaseListPreference {
    private static final int PREF_INPUT_MODE_FULL_ALPHABET = 1;
    private static final int PREF_INPUT_MODE_FULL_KATAKANA = 3;
    private static final int PREF_INPUT_MODE_FULL_NUMBER = 2;
    private static final int PREF_INPUT_MODE_HALF_ALPHABET = 4;
    private static final int PREF_INPUT_MODE_HALF_KATAKANA = 6;
    private static final int PREF_INPUT_MODE_HALF_NUMBER = 5;
    private static final int PREF_INPUT_MODE_HIRAGANA = 0;
    public static final String PREF_KEY_INPUT_MODE_TYPE = "input_mode_type_";
    public static final String PREF_KEY_LAST_KEYBOARD_TYPE = "last_keyboard_type_";
    private int mDisplayType;
    private SharedPreferences mKeyboardTypeCommonPref;
    private static final int[][] MODE_CONVERT_TABLE = {new int[]{0, 3}, new int[]{1, 6}, new int[]{2, 7}, new int[]{3, 4}, new int[]{4, 0}, new int[]{5, 1}, new int[]{6, 5}};
    public static final int NUMBER_SET_INPUTMODE = MODE_CONVERT_TABLE.length;
    public static final HashMap<Integer, Integer> ICON_TABLE_KEYBOARD_TYPE = new HashMap<Integer, Integer>() {
        {
            put(0, Integer.valueOf(R.drawable.keyboard_type_preview_qwerty));
            put(1, Integer.valueOf(R.drawable.keyboard_type_preview_10key));
            put(3, Integer.valueOf(R.drawable.keyboard_type_preview_50key));
        }
    };

    public KeyboardTypeListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mKeyboardTypeCommonPref = null;
        this.mDisplayType = 0;
    }

    public static Integer convertInputMode(int keyMode) {
        int[][] arr$ = MODE_CONVERT_TABLE;
        for (int[] element : arr$) {
            if (element[1] == keyMode) {
                return Integer.valueOf(element[0]);
            }
        }
        return null;
    }

    private static Integer convertKeyMode(int inputMode) {
        int[][] arr$ = MODE_CONVERT_TABLE;
        for (int[] element : arr$) {
            if (element[0] == inputMode) {
                return Integer.valueOf(element[1]);
            }
        }
        return null;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        CharSequence[] entries = getEntries();
        CharSequence[] entryValues = getEntryValues();
        this.mKeyboardTypeCommonPref = getSharedPreferences();
        if (entries == null || entryValues == null || this.mKeyboardTypeCommonPref == null) {
            throw new IllegalStateException("ListPreference requires an entries array and an entryValues array.");
        }
        String key = getKey();
        Context context = getContext();
        int length = entryValues.length;
        int[] itemValues = new int[length];
        for (int i = 0; i < length; i++) {
            itemValues[i] = Integer.parseInt(entryValues[i].toString());
        }
        Resources res = context.getResources();
        String defaultValue = new String(LoggingEvents.EXTRA_CALLING_APP_NAME);
        int mode = 0;
        if (key.equals("input_mode_type_all_portrait")) {
            this.mDisplayType = 0;
            defaultValue = res.getString(R.string.portrait_input_mode_type_all_default_value);
        } else if (key.equals("input_mode_type_all_land")) {
            this.mDisplayType = 1;
            defaultValue = res.getString(R.string.land_input_mode_type_all_default_value);
        } else {
            int sharpIndex = key.lastIndexOf("#");
            mode = Integer.valueOf(key.substring(sharpIndex + 1, key.length())).intValue();
            this.mDisplayType = Integer.valueOf(key.substring(sharpIndex - 1, sharpIndex)).intValue();
            IWnnIME.updateTabletMode(context);
        }
        this.mCurrentSelectValue = this.mKeyboardTypeCommonPref.getString(key, defaultValue);
        if (!checkKeyboardType(this.mCurrentSelectValue)) {
            this.mCurrentSelectValue = getLastKeyboardTypePref(context, mode, this.mDisplayType);
        }
        if (key.equals("input_mode_type_all_portrait") || key.equals("input_mode_type_all_land")) {
            this.mCurrentSelectValue = getInitialValue(this.mCurrentSelectValue);
        }
        int clickedDialogEntryIndex = findIndexOfValue(this.mCurrentSelectValue);
        this.mWnnArrayAdapter = WnnUtility.makeTitleListWithIcon(context, entries, itemValues, ICON_TABLE_KEYBOARD_TYPE, clickedDialogEntryIndex);
        ListView listView = WnnUtility.makeSingleChoiceListView(context, this.mWnnArrayAdapter, clickedDialogEntryIndex, this.listener);
        builder.setView(listView);
        builder.setPositiveButton(R.string.ti_dialog_button_ok_txt, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                KeyboardTypeListPreference.this.onDialogClosed(true);
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);
        if (positiveResult) {
            String key = getKey();
            if (key.equals("input_mode_type_all_portrait") || key.equals("input_mode_type_all_land")) {
                setKeyboardTypePrefAllMode(Integer.parseInt(this.mCurrentSelectValue));
            }
        }
    }

    private String getInitialValue(String retMixedValue) {
        if (this.mKeyboardTypeCommonPref != null) {
            StringBuffer setKey = new StringBuffer(PREF_KEY_INPUT_MODE_TYPE);
            setKey.append(String.valueOf(this.mDisplayType));
            setKey.append("#");
            String nowModeType = null;
            String preModeType = null;
            Context context = getContext();
            for (int mode = 0; mode < NUMBER_SET_INPUTMODE; mode++) {
                setKey.append(String.valueOf(mode));
                nowModeType = this.mKeyboardTypeCommonPref.getString(setKey.toString(), LoggingEvents.EXTRA_CALLING_APP_NAME);
                if (!checkKeyboardType(nowModeType)) {
                    nowModeType = getLastKeyboardTypePref(context, mode, this.mDisplayType);
                }
                if (preModeType == null) {
                    preModeType = nowModeType;
                } else if (!preModeType.equals(nowModeType)) {
                    return retMixedValue;
                }
                int length = setKey.length();
                int delindex = setKey.lastIndexOf("#");
                setKey.delete(delindex + 1, length);
            }
            return nowModeType;
        }
        return retMixedValue;
    }

    public static void setKeyboardTypePref(Context context, int keyboardType, int keyMode, int displayType) {
        SharedPreferences pref;
        SharedPreferences.Editor editor;
        Integer inputMode = convertInputMode(keyMode);
        if (context != null && inputMode != null && (pref = PreferenceManager.getDefaultSharedPreferences(context)) != null && (editor = pref.edit()) != null) {
            String keyName = PREF_KEY_INPUT_MODE_TYPE + String.valueOf(displayType) + "#" + String.valueOf(inputMode);
            editor.putString(keyName, String.valueOf(keyboardType));
            editor.commit();
        }
    }

    private void setKeyboardTypePrefAllMode(int keyboardType) {
        Context context = getContext();
        if (context != null) {
            for (int mode = 0; mode < NUMBER_SET_INPUTMODE; mode++) {
                Integer keymode = convertKeyMode(mode);
                if (keymode != null) {
                    setKeyboardTypePref(context, keyboardType, keymode.intValue(), this.mDisplayType);
                }
            }
        }
    }

    public static String getKeyboardTypePref(Context context, int keyMode, int displayType) {
        Integer inputMode = convertInputMode(keyMode);
        if (context == null || inputMode == null) {
            return null;
        }
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String keyName = PREF_KEY_INPUT_MODE_TYPE + String.valueOf(displayType) + "#" + String.valueOf(inputMode);
        String keyboardType = pref.getString(keyName, LoggingEvents.EXTRA_CALLING_APP_NAME);
        if (!checkKeyboardType(keyboardType)) {
            return getLastKeyboardTypePref(context, inputMode.intValue(), displayType);
        }
        return keyboardType;
    }

    public static String getLastKeyboardTypePref(Context context, int inputMode, int displayType) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String keyName = PREF_KEY_LAST_KEYBOARD_TYPE + String.valueOf(displayType) + "#" + String.valueOf(inputMode);
        String keyboardType = pref.getString(keyName, LoggingEvents.EXTRA_CALLING_APP_NAME);
        if (!checkKeyboardType(keyboardType)) {
            Integer keyMode = convertKeyMode(inputMode);
            if (keyMode == null) {
                keyboardType = Integer.toString(0);
            } else {
                keyboardType = DefaultSoftKeyboardJAJP.getDefaultKeyboardType(context, keyMode.intValue(), displayType);
            }
        }
        SharedPreferences.Editor editor = pref.edit();
        String setKey = PREF_KEY_INPUT_MODE_TYPE + String.valueOf(displayType) + "#" + String.valueOf(inputMode);
        editor.putString(setKey.toString(), keyboardType);
        editor.remove(keyName.toString());
        editor.commit();
        return keyboardType;
    }

    public static boolean checkKeyboardType(String type) {
        return Integer.toString(1).equals(type) || Integer.toString(0).equals(type) || Integer.toString(3).equals(type);
    }
}
