package jp.co.omronsoft.iwnnime.ml;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.android.common.speech.LoggingEvents;
import java.util.Iterator;
import java.util.Set;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class UninstallReceiver extends BroadcastReceiver {
    private static final String ADD_SYMBOL_LIST_KEY = "opt_add_symbol_list";
    private static final String KEYBOARD_IMAGE_KEY = "keyboard_skin_add";
    private static final String LANGPACK_CORE_NAME = "jp.co.omronsoft.iwnnime.languagepack";
    private static final String WEBAPI_KEY = "opt_multiwebapi";

    @Override
    public void onReceive(Context context, Intent intent) {
        String uninstallPackageName;
        IWnnIME ime;
        Uri uri = intent.getData();
        if (uri != null && (uninstallPackageName = uri.getSchemeSpecificPart()) != null) {
            boolean isReplace = intent.getBooleanExtra("android.intent.extra.REPLACING", false);
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            String classname = pref.getString("keyboard_skin_add", LoggingEvents.EXTRA_CALLING_APP_NAME);
            if (!classname.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
                String packagename = classname.substring(0, classname.lastIndexOf(46));
                if (uninstallPackageName.equals(packagename)) {
                    if (!isReplace) {
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("keyboard_skin_add", LoggingEvents.EXTRA_CALLING_APP_NAME);
                        editor.commit();
                    }
                    WnnUtility.resetIme(false);
                }
            }
            Set<String> packageNames = pref.getStringSet("opt_add_symbol_list", null);
            if (packageNames != null && packageNames.contains(uninstallPackageName)) {
                if (!isReplace) {
                    SharedPreferences.Editor editor2 = pref.edit();
                    editor2.remove("opt_add_symbol_list");
                    editor2.commit();
                    packageNames.remove(uninstallPackageName);
                    editor2.putStringSet("opt_add_symbol_list", packageNames);
                    editor2.commit();
                }
                WnnUtility.resetIme(false);
            }
            Set<String> classNames = pref.getStringSet("opt_multiwebapi", null);
            if (classNames != null) {
                Iterator<String> iterator = classNames.iterator();
                boolean isModified = false;
                while (iterator.hasNext()) {
                    String className = iterator.next();
                    String packageName = className.substring(0, className.lastIndexOf(46));
                    if (uninstallPackageName.equals(packageName)) {
                        if (!isReplace) {
                            iterator.remove();
                        }
                        isModified = true;
                    }
                }
                if (isModified) {
                    if (!isReplace) {
                        SharedPreferences.Editor editor3 = pref.edit();
                        editor3.remove("opt_multiwebapi");
                        editor3.commit();
                        editor3.putStringSet("opt_multiwebapi", classNames);
                        editor3.commit();
                    }
                    WnnUtility.resetIme(false);
                }
            }
            KeyboardLanguagePackData langPack = KeyboardLanguagePackData.getInstance();
            String uninstallPackCoreName = uninstallPackageName.substring(0, uninstallPackageName.lastIndexOf(46));
            if (uninstallPackCoreName.equals(LANGPACK_CORE_NAME)) {
                String currentLangPack = langPack.getEnableKeyboardLanguagePackDataName();
                if (!isReplace) {
                    LanguageManager.resetLanguageList();
                    langPack.setInputMethodSubtypeInstallLangPack(context);
                }
                if (uninstallPackageName.equals(currentLangPack) && (ime = IWnnIME.getCurrentIme()) != null) {
                    String currentLocale = IWnnIME.getSubtypeLocaleDirect(WnnUtility.getCurrentInputMethodSubtype(context));
                    int languageType = LanguageManager.getChosenLanguageType(currentLocale);
                    ime.setLanguage(LanguageManager.getChosenLocale(languageType), languageType, true);
                    WnnUtility.resetIme(true);
                }
            }
        }
    }
}
