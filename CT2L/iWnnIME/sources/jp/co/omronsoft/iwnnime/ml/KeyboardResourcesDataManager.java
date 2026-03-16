package jp.co.omronsoft.iwnnime.ml;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import java.util.HashMap;

public class KeyboardResourcesDataManager {
    private static final HashMap<Integer, Integer> RESOURCEID_TEXTCOLOR_STROKECOLOR_TABLE = new HashMap<Integer, Integer>() {
        {
            put(Integer.valueOf(R.color.key_text_color), Integer.valueOf(R.color.key_text_color_stroke));
            put(Integer.valueOf(R.color.key_text_color_2nd), Integer.valueOf(R.color.key_text_color_2nd_stroke));
            put(Integer.valueOf(R.color.key_hint_letter), Integer.valueOf(R.color.key_hint_letter_stroke));
            put(Integer.valueOf(R.color.key_hint_ellipsis_color), Integer.valueOf(R.color.key_hint_ellipsis_color_stroke));
            put(Integer.valueOf(R.color.input_mode_key_main_text_color), Integer.valueOf(R.color.input_mode_key_main_text_color_stroke));
            put(Integer.valueOf(R.color.input_mode_key_mode_text_color), Integer.valueOf(R.color.input_mode_key_mode_text_color_stroke));
            put(Integer.valueOf(R.color.input_mode_key_mode_selected_text_color), Integer.valueOf(R.color.input_mode_key_mode_selected_text_color_stroke));
            put(Integer.valueOf(R.color.key_text_color_close), Integer.valueOf(R.color.key_text_color_close_stroke));
        }
    };
    private static KeyboardResourcesDataManager mManager;
    private Drawable mKeymore = null;
    private Drawable mKeyprev = null;

    private KeyboardResourcesDataManager() {
        mManager = null;
    }

    public static synchronized KeyboardResourcesDataManager getInstance() {
        if (mManager == null) {
            mManager = new KeyboardResourcesDataManager();
        }
        return mManager;
    }

    public Drawable getDrawable(Context context, int resourceId, String resourceKey) {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getDrawable(resourceKey);
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            Drawable result2 = langpack.getDrawable(resourceKey);
            if (result2 == null && context != null) {
                return context.getResources().getDrawable(resourceId);
            }
            return result2;
        }
        return result;
    }

    public Drawable getDrawable(Context context, int resourceId) {
        Resources res;
        Drawable result = KeyboardSkinData.getInstance().getDrawable(resourceId);
        if (result == null) {
            Drawable result2 = KeyboardLanguagePackData.getInstance().getDrawable(resourceId);
            if (result2 == null && context != null && (res = context.getResources()) != null) {
                return res.getDrawable(resourceId);
            }
            return result2;
        }
        return result;
    }

    public Drawable getDrawable(String skinKey, String langpackKey) {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getDrawable(skinKey);
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getDrawable(langpackKey);
        }
        return result;
    }

    public Drawable getDrawable(String key) {
        return getDrawable(key, key);
    }

    public Drawable getDrawable(int keycode, Keyboard keyboard) {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getDrawable(keycode, keyboard);
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getDrawable(keycode, keyboard);
        }
        return result;
    }

    public Drawable getKeyPreviewDrawable(Context context, int resourceId) {
        this.mKeymore = null;
        this.mKeyprev = null;
        switch (resourceId) {
            case R.drawable.keyboard_key_feedback_background:
                this.mKeymore = getDrawable("KeyPreviewNoMoreBackground");
                this.mKeyprev = getDrawable("KeyPreviewBackground");
                break;
            case R.drawable.keyboard_key_feedback_more_background:
                this.mKeymore = getDrawable("KeyPreviewMoreBackground");
                this.mKeyprev = getDrawable("KeyPreviewBackground");
                break;
        }
        if (this.mKeymore != null) {
            Drawable result = this.mKeymore;
            return result;
        }
        if (this.mKeyprev != null) {
            Drawable result2 = this.mKeyprev;
            return result2;
        }
        if (context == null) {
            return null;
        }
        Drawable result3 = context.getResources().getDrawable(resourceId);
        return result3;
    }

    public int getColor(Context context, int resourceId) {
        Resources res;
        int result = 0;
        Integer skinColor = KeyboardSkinData.getInstance().getColor(resourceId);
        if (skinColor != null) {
            return skinColor.intValue();
        }
        Integer lpColor = KeyboardLanguagePackData.getInstance().getColor(resourceId);
        if (lpColor != null) {
            return lpColor.intValue();
        }
        if (context != null && (res = context.getResources()) != null) {
            result = res.getColor(resourceId);
        }
        return result;
    }

    public Integer getColor(String key) {
        Integer color = KeyboardSkinData.getInstance().getColor(key);
        if (color == null) {
            return KeyboardLanguagePackData.getInstance().getColor(key);
        }
        return color;
    }

    public int[] getTextColor(Context context, int resourceId) {
        int[] textColor = {0, getColor(context, strokeId), getColor(context, resourceId)};
        int strokeId = RESOURCEID_TEXTCOLOR_STROKECOLOR_TABLE.get(Integer.valueOf(resourceId)).intValue();
        return textColor;
    }

    private Drawable getStateListDrawable(String normal, String press, String select) {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getStateListDrawable(normal, press, null, select);
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getStateListDrawable(normal, press, null, select);
        }
        return result;
    }

    public Drawable getStateListDrawable(int resId) {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        String key = keyskin.getStateListBaseStrings(resId);
        if (key == null) {
            return null;
        }
        Drawable result = getStateListDrawable(key + "Normal", key + "Pressed", key + "Selected");
        return result;
    }

    public ColorStateList getColorStateList(String normal, String press, String select) {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        ColorStateList result = keyskin.getColorStateList(normal, press, select);
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getColorStateList(normal, press, select);
        }
        return result;
    }

    public float getFloat(String key) {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        float result = keyskin.getFloat(key);
        if (result == -1.0f) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getFloat(key);
        }
        return result;
    }

    public Drawable getKeyBg(Context context, Keyboard keyboard, int keycode, boolean isSecondKey) {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getKeyBg(context, keyboard, keycode, isSecondKey);
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            if (isSecondKey) {
                return langpack.getKeyBg2nd();
            }
            return langpack.getKeyBg();
        }
        return result;
    }

    public Drawable getKeyBg() {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getKeyBg();
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getKeyBg();
        }
        return result;
    }

    public Drawable getKeyBg2nd() {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getKeyBg2nd();
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getKeyBg2nd();
        }
        return result;
    }

    public Drawable getTab() {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getTab();
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getTab();
        }
        return result;
    }

    public Drawable getTabNoSelect() {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getTabNoSelect();
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getTabNoSelect();
        }
        return result;
    }

    private Drawable getCandidateBackgroundDrawable(String normal, String press, String focus) {
        KeyboardSkinData keyskin = KeyboardSkinData.getInstance();
        Drawable result = keyskin.getStateListDrawable(normal, press, focus, null);
        if (result == null) {
            KeyboardLanguagePackData langpack = KeyboardLanguagePackData.getInstance();
            return langpack.getStateListDrawable(normal, press, focus, null);
        }
        return result;
    }

    public boolean isEnableColorFilter(int keyCode, boolean isIconSkin, boolean isSecondKey, int colorIndex) {
        if (isIconSkin) {
            return false;
        }
        switch (keyCode) {
            case DefaultSoftKeyboard.KEYCODE_LANGUAGE_SWITCH:
            case DefaultSoftKeyboard.KEYCODE_TOGGLE_MODE:
            case DefaultSoftKeyboard.KEYCODE_ENTER:
            case -1:
                KeyboardSkinData skin = KeyboardSkinData.getInstance();
                String tag = isSecondKey ? KeyboardSkinData.TAG_KEY_COLOR_2ND : KeyboardSkinData.TAG_KEY_COLOR;
                if (skin.getColor(tag) != null || colorIndex != 2) {
                }
                break;
        }
        return true;
    }

    public Drawable getKeyboardBg() {
        return getDrawable("Keyboardbackground");
    }

    public Drawable getKeyboardBg1Line() {
        return getDrawable("Keyboardbackground1Line");
    }

    public Drawable getCandidateBack() {
        return getDrawable("CandBack");
    }

    public Integer getKeyPreviewColor() {
        return getColor("KeyPreviewColor");
    }

    public Drawable getCandidateBackground() {
        return getCandidateBackgroundDrawable("CandidateBackgroundNormal", "CandidateBackgroundPressed", "CandidateBackgroundFocused");
    }

    public Drawable getCandidateFocusBackground() {
        return getCandidateBackgroundDrawable("CandidateBackgroundFocused", "CandidateBackgroundPressed", null);
    }

    public Drawable getCandidateBackgroundOneLine() {
        return getCandidateBackgroundDrawable("CandidateBackgroundNormalOneLine", "CandidateBackgroundPressedOneLine", "CandidateBackgroundFocusedOneLine");
    }

    public Drawable getCandidateFocusBackgroundOneLine() {
        return getCandidateBackgroundDrawable("CandidateBackgroundFocusedOneLine", "CandidateBackgroundPressedOneLine", null);
    }

    public Drawable getCandidateBackgroundWebApi() {
        return getCandidateBackgroundDrawable("CandidateBackgroundNormalWebApi", "CandidateBackgroundPressedWebApi", "CandidateBackgroundFocusedWebApi");
    }

    public Drawable getCandidateFocusBackgroundWebApi() {
        return getCandidateBackgroundDrawable("CandidateBackgroundFocusedWebApi", "CandidateBackgroundPressedWebApi", null);
    }

    public ColorStateList getCategoryColorStateList() {
        return getColorStateList("CategoryColorNormal", "CategoryColorPressed", "CategoryColorSelected");
    }
}
