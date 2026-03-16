package jp.co.omronsoft.iwnnime.ml;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.text.Spannable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.PopupWindow;
import com.android.common.speech.LoggingEvents;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import jp.co.omronsoft.iwnnime.ml.Keyboard;
import jp.co.omronsoft.iwnnime.ml.controlpanel.KeyVibrateDialogPreference;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class WnnUtility {
    private static final int BASE_HEX = 16;
    private static final String COMMA_STRING = ",";
    public static final String PACKAGE_NAME_KEYGUARD_SERVICE = "com.android.systemui";
    private static final float PLAY_SOUND_VOLUME_DEFAULT = -1.0f;
    private static final String VERTICAL_BAR_STRING = "\\|";
    private static final AudioAttributes VIBRATION_ATTRIBUTES;
    private static AudioManager sAudioManager;
    private static Vibrator sVibrator;

    static {
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes.Builder builder = new AudioAttributes.Builder();
            VIBRATION_ATTRIBUTES = builder.setContentType(4).setUsage(13).build();
        } else {
            VIBRATION_ATTRIBUTES = null;
        }
        sVibrator = null;
        sAudioManager = null;
    }

    public static void setSpan(Spannable text, Object what, int start, int end, int flags) {
        if (text != null && what != null && start < end) {
            text.setSpan(what, start, end, flags);
        }
    }

    public static String getLabelSpec(String codesArraySpec) {
        String[] strs = codesArraySpec.split(VERTICAL_BAR_STRING, -1);
        return strs.length <= 1 ? codesArraySpec : strs[0];
    }

    public static String parseLabel(String codesArraySpec) {
        String labelSpec = getLabelSpec(codesArraySpec);
        StringBuilder sb = new StringBuilder();
        String[] arr$ = labelSpec.split(",");
        for (String codeInHex : arr$) {
            int codePoint = Integer.parseInt(codeInHex, 16);
            sb.appendCodePoint(codePoint);
        }
        return sb.toString();
    }

    public static int getMinSupportSdkVersion(String codesArraySpec) {
        String[] strs = codesArraySpec.split(VERTICAL_BAR_STRING, -1);
        if (strs.length <= 2) {
            return 0;
        }
        try {
            return Integer.parseInt(strs[2]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static int adjustValue(int value, int min, int max) {
        if (value < min) {
            value = min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    public static void showInputMethodPicker(IWnnIME wnn) {
        InputMethodManager manager;
        if (wnn != null && (manager = (InputMethodManager) wnn.getSystemService("input_method")) != null) {
            manager.showInputMethodPicker();
        }
    }

    public static InputMethodSubtype getCurrentInputMethodSubtype(Context context) {
        InputMethodManager manager;
        if (context == null || (manager = (InputMethodManager) context.getSystemService("input_method")) == null) {
            return null;
        }
        return manager.getCurrentInputMethodSubtype();
    }

    public static int getCurrentLanguageType(Context context) {
        InputMethodSubtype currentSubtype;
        String currentLocale;
        if (context == null || (currentSubtype = getCurrentInputMethodSubtype(context)) == null || (currentLocale = IWnnIME.getSubtypeLocaleDirect(currentSubtype)) == null) {
            return -1;
        }
        return LanguageManager.getChosenLanguageType(currentLocale);
    }

    public static List<ResolveInfo> getPackageInfo(Context context, String action) {
        List<ResolveInfo> resolveInfo = new ArrayList<>();
        if (context != null) {
            PackageManager pm = context.getPackageManager();
            Intent intent = new Intent(action);
            List<ResolveInfo> resolveInfo2 = pm.queryIntentActivities(intent, 0);
            Collections.sort(resolveInfo2, new ResolveInfo.DisplayNameComparator(pm));
            return resolveInfo2;
        }
        return resolveInfo;
    }

    public static void resetIme(boolean initEngine) {
        iWnnEngine engine;
        IWnnIME wnn = IWnnIME.getCurrentIme();
        if (wnn != null) {
            if (initEngine && (engine = iWnnEngine.getEngine()) != null) {
                engine.close();
                engine.init(wnn.getFilesDirPath());
            }
            IWnnImeEvent ev = new IWnnImeEvent(IWnnImeEvent.CHANGE_INPUT_CANDIDATE_VIEW);
            wnn.requestHideSelf(0);
            wnn.onEvent(ev);
        }
    }

    public static void playSoundEffect(Context context) {
        playSoundEffect(context, 5);
    }

    public static void playSoundEffect(Context context, int effectType) {
        if (context != null) {
            if (sAudioManager == null) {
                sAudioManager = (AudioManager) context.getSystemService("audio");
            }
            if (sAudioManager != null) {
                sAudioManager.playSoundEffect(effectType, PLAY_SOUND_VOLUME_DEFAULT);
            }
        }
    }

    public static void vibrate(Context context) {
        SharedPreferences pref;
        Resources res;
        if (context != null && (pref = PreferenceManager.getDefaultSharedPreferences(context)) != null && (res = context.getResources()) != null) {
            int vibrateTime = pref.getInt(KeyVibrateDialogPreference.KEY_VIBRATE_TIME_KEY, res.getInteger(R.integer.vibrate_time_default_value));
            vibrate(context, vibrateTime);
        }
    }

    public static void vibrate(Context context, long vibrateTime) {
        if (hasVibrator(context)) {
            if (Build.VERSION.SDK_INT >= 21) {
                sVibrator.vibrate(vibrateTime, VIBRATION_ATTRIBUTES);
            } else {
                sVibrator.vibrate(vibrateTime);
            }
        }
    }

    public static boolean hasVibrator(Context context) {
        if (context == null) {
            return false;
        }
        if (sVibrator == null) {
            sVibrator = (Vibrator) context.getSystemService("vibrator");
        }
        if (sVibrator == null) {
            return false;
        }
        return sVibrator.hasVibrator();
    }

    public static WnnArrayAdapter<CharSequence> makeTitleListWithIcon(Context context, CharSequence[] itemTitles, int[] itemValues, HashMap<Integer, Integer> iconTable, int checkIndex) {
        if (context == null || itemTitles == null || itemValues == null || iconTable == null) {
            CharSequence[] dummy = {LoggingEvents.EXTRA_CALLING_APP_NAME};
            return new WnnArrayAdapter<>(context, 0, dummy);
        }
        Resources res = context.getResources();
        ArrayList<Drawable> entriesImage = new ArrayList<>();
        int length = itemTitles.length < itemValues.length ? itemTitles.length : itemValues.length;
        for (int i = 0; i < length; i++) {
            Drawable iconImage = res.getDrawable(iconTable.get(Integer.valueOf(itemValues[i])).intValue());
            if (iconImage != null) {
                entriesImage.add(iconImage);
            }
        }
        WnnArrayAdapter<CharSequence> wnnArrayAdapter = new WnnArrayAdapter<>(context, 0, itemTitles);
        wnnArrayAdapter.setEntriesImage(entriesImage);
        wnnArrayAdapter.setCheckIndex(checkIndex);
        return wnnArrayAdapter;
    }

    public static ListView makeSingleChoiceListView(Context context, WnnArrayAdapter<CharSequence> adapter, int checkIndex, AdapterView.OnItemClickListener listener) {
        if (context == null || adapter == null) {
            return new ListView(context);
        }
        LayoutInflater inflater = (LayoutInflater) context.getSystemService("layout_inflater");
        if (inflater == null) {
            return new ListView(context);
        }
        ListView listView = (ListView) inflater.inflate(R.layout.wnn_listview, (ViewGroup) null);
        if (listView == null) {
            return new ListView(context);
        }
        listView.setAdapter((ListAdapter) adapter);
        listView.setChoiceMode(1);
        listView.setItemChecked(checkIndex, true);
        listView.setOnItemClickListener(listener);
        return listView;
    }

    public static boolean isThroughKeyCode(KeyEvent event) {
        return event.isSystem();
    }

    public static void addFlagsForDialog(IWnnIME wnn, Window target) {
        if (wnn != null && target != null) {
            int flags = 131072;
            EditorInfo info = wnn.getCurrentInputEditorInfo();
            if (info != null && wnn.isScreenLock() && !PACKAGE_NAME_KEYGUARD_SERVICE.equals(info.packageName)) {
                flags = 131072 | AccessibilityEventCompat.TYPE_GESTURE_DETECTION_END;
            }
            target.addFlags(flags);
        }
    }

    public static void addFlagsForPopupWindow(IWnnIME wnn, PopupWindow target) {
        EditorInfo info;
        if (wnn != null && target != null && wnn.isScreenLock() && (info = wnn.getCurrentInputEditorInfo()) != null && !PACKAGE_NAME_KEYGUARD_SERVICE.equals(info.packageName)) {
            try {
                Field fPopupView = target.getClass().getDeclaredField("mPopupView");
                Field fWindowManager = target.getClass().getDeclaredField("mWindowManager");
                fPopupView.setAccessible(true);
                fWindowManager.setAccessible(true);
                View popupView = (View) fPopupView.get(target);
                WindowManager windowManager = (WindowManager) fWindowManager.get(target);
                WindowManager.LayoutParams param = (WindowManager.LayoutParams) popupView.getLayoutParams();
                param.flags |= AccessibilityEventCompat.TYPE_GESTURE_DETECTION_END;
                windowManager.removeView(popupView);
                windowManager.addView(popupView, param);
            } catch (IllegalAccessException e) {
                Log.e("iWnn", "WnnUtility:addFlags() e[" + e + "]");
            } catch (NoSuchFieldException e2) {
                Log.e("iWnn", "WnnUtility:addFlags() e[" + e2 + "]");
            }
        }
    }

    public static boolean isFunctionKey(Keyboard.Key key) {
        if (key == null) {
            return false;
        }
        if (key.isSecondKey) {
            return true;
        }
        int keyCode = key.codes[0];
        switch (keyCode) {
            case DefaultSoftKeyboard.KEYCODE_LANGUAGE_SWITCH:
            case DefaultSoftKeyboard.KEYCODE_SPACE_JP:
            case 32:
                break;
        }
        return false;
    }
}
