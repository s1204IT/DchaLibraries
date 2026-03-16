package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import com.android.common.speech.LoggingEvents;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;

public class DownloadDictionaryPreference extends PreferenceFragment implements Preference.OnPreferenceClickListener {
    private static final String CONTENT_PROVIDER_URI = "content://jp.co.omronsoft.cp.dldicprovider";
    public static final String DOWNLOAD_DICTIONARY_NAME_KEY = "download_dic_name";
    public static final int MAX_DOWNLOAD_DIC = 10;
    public static final String QUERY_PARAM_CACHE_FLAG = "CACHE_FLAG";
    public static final String QUERY_PARAM_CONVERT_BASE = "CONVERT_BASE";
    public static final String QUERY_PARAM_CONVERT_HIGH = "CONVERT_HIGH";
    public static final String QUERY_PARAM_FILE = "DICTIONARY_FILE";
    public static final String QUERY_PARAM_LIMIT = "LIMIT";
    public static final String QUERY_PARAM_MORPHO_BASE = "MORPHO_BASE";
    public static final String QUERY_PARAM_MORPHO_HIGH = "MORPHO_HIGH";
    public static final String QUERY_PARAM_NAME = "DICTIONARY_NAME";
    public static final String QUERY_PARAM_PREDICT_BASE = "PREDICT_BASE";
    public static final String QUERY_PARAM_PREDICT_HIGH = "PREDICT_HIGH";
    private static final String TAG = "iWnn";
    private ArrayList<CheckBoxPreference> mCheckBoxPreference = new ArrayList<>();
    private ArrayList<DownloadDictionary> mDownloadDictionaries;

    public static class DownloadDictionary implements Comparable<DownloadDictionary> {
        private boolean mCache;
        private int mConvertBase;
        private int mConvertHigh;
        private boolean mEnable = false;
        private String mFile;
        private int mLimit;
        private int mMorphoBase;
        private int mMorphoHigh;
        private String mName;
        private int mPredictBase;
        private int mPredictHigh;

        public DownloadDictionary(String name, String file, int convertHigh, int convertBase, int predictHigh, int predictBase, int morphoHigh, int morphoBase, boolean cache, int limit) {
            this.mName = name;
            this.mFile = file;
            this.mConvertHigh = convertHigh;
            this.mConvertBase = convertBase;
            this.mPredictHigh = predictHigh;
            this.mPredictBase = predictBase;
            this.mMorphoHigh = morphoHigh;
            this.mMorphoBase = morphoBase;
            this.mCache = cache;
            this.mLimit = limit;
        }

        public String name() {
            return this.mName;
        }

        public String file() {
            return this.mFile;
        }

        @Override
        public int compareTo(DownloadDictionary other) {
            return this.mFile.compareTo(other.mFile);
        }

        public boolean equals(Object other) {
            if (other instanceof DownloadDictionary) {
                return this.mFile.equals(((DownloadDictionary) other).mFile);
            }
            return false;
        }

        public int hashCode() {
            return this.mFile.hashCode();
        }

        public void enable() {
            this.mEnable = true;
        }

        public void disable() {
            this.mEnable = false;
        }

        public boolean isEnabled() {
            return this.mEnable;
        }

        public int convertHigh() {
            return this.mConvertHigh;
        }

        public int convertBase() {
            return this.mConvertBase;
        }

        public int predictHigh() {
            return this.mPredictHigh;
        }

        public int predictBase() {
            return this.mPredictBase;
        }

        public int morphoHigh() {
            return this.mMorphoHigh;
        }

        public int morphoBase() {
            return this.mMorphoBase;
        }

        public boolean cache() {
            return this.mCache;
        }

        public int limit() {
            return this.mLimit;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity act = getActivity();
        PreferenceScreen rootPreferenceScreen = getPreferenceManager().createPreferenceScreen(act);
        rootPreferenceScreen.setTitle(R.string.ti_preference_download_dic_title_txt);
        ArrayList<DownloadDictionary> dictionary = getDownloadDictionary(act);
        if (dictionary.size() > 0) {
            loadDownloadDictionaryPreference(rootPreferenceScreen, dictionary);
        } else {
            Toast.makeText(act, getString(R.string.ti_preference_download_dic_not_exist_txt), 0).show();
        }
        setPreferenceScreen(rootPreferenceScreen);
    }

    public static boolean isEnableDownloadDictionary(Context context) {
        ArrayList<DownloadDictionary> cp = getDictionaryFromContentProvider(context);
        ArrayList<DownloadDictionary> fp = checkDictionaryFilePath(cp);
        ArrayList<DownloadDictionary> sp = readDownloadDictionaryFromSharedPreferences(context);
        ArrayList<DownloadDictionary> dictionary = checkConsistency(fp, sp, context);
        return dictionary.size() >= 1;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        int index = preference.getOrder();
        CheckBoxPreference checkBox = this.mCheckBoxPreference.get(index);
        DownloadDictionary dic = this.mDownloadDictionaries.get(index);
        Activity act = getActivity();
        if (checkBox.isChecked()) {
            if (!dic.isEnabled()) {
                if (getEnableDicitonaries(this.mDownloadDictionaries) < 10) {
                    if (!enableDictionary(dic, this.mDownloadDictionaries)) {
                        String message = getString(R.string.ti_preference_download_dic_fail_enable_txt);
                        Toast.makeText(act, message, 0).show();
                    }
                } else {
                    String message2 = new StringBuilder(getString(R.string.ti_preference_max_selected_error_txt)).toString();
                    Toast.makeText(act, message2, 0).show();
                }
            }
        } else if (dic.isEnabled() && !disableDictionary(dic, this.mDownloadDictionaries)) {
            String message3 = getString(R.string.ti_preference_download_dic_fail_disable_txt);
            Toast.makeText(act, message3, 0).show();
        }
        checkBox.setChecked(dic.isEnabled());
        return true;
    }

    private void loadDownloadDictionaryPreference(PreferenceScreen rootPreferenceScreen, ArrayList<DownloadDictionary> dictionary) {
        rootPreferenceScreen.removeAll();
        Activity act = getActivity();
        int index = 0;
        this.mCheckBoxPreference.clear();
        for (DownloadDictionary dic : dictionary) {
            CheckBoxPreference preference = new CheckBoxPreference(act);
            preference.setTitle(dic.name());
            for (int i = 0; i < 10; i++) {
                String file = IWnnIME.getStringFromNotResetSettingsPreference(act, createSharedPrefKey(i, QUERY_PARAM_FILE), null);
                if (file != null && file.equals(dic.file())) {
                    preference.setChecked(true);
                }
            }
            preference.setOrder(index);
            preference.setOnPreferenceClickListener(this);
            rootPreferenceScreen.addPreference(preference);
            this.mCheckBoxPreference.add(preference);
            index++;
        }
    }

    private int getEnableDicitonaries(ArrayList<DownloadDictionary> dictionaries) {
        int counter = 0;
        for (int i = 0; i < dictionaries.size(); i++) {
            if (dictionaries.get(i).isEnabled()) {
                counter++;
            }
        }
        return counter;
    }

    private boolean enableDictionary(DownloadDictionary dictionary, ArrayList<DownloadDictionary> dicArray) {
        if (!new File(dictionary.file()).exists()) {
            return false;
        }
        dictionary.enable();
        boolean ret = writeDownloadDictionaryToSharedPreferences(dicArray);
        iWnnEngine.getEngine().setUpdateDownloadDictionary(true);
        iWnnEngine.getEngine().setDictionary(getActivity());
        return ret;
    }

    private boolean disableDictionary(DownloadDictionary dictionary, ArrayList<DownloadDictionary> dicArray) {
        if (!new File(dictionary.file()).exists()) {
            return false;
        }
        dictionary.disable();
        boolean ret = writeDownloadDictionaryToSharedPreferences(dicArray);
        iWnnEngine.getEngine().setUpdateDownloadDictionary(true);
        iWnnEngine.getEngine().setDictionary(getActivity());
        return ret;
    }

    public static ArrayList<DownloadDictionary> readDownloadDictionaryFromSharedPreferences(Context context) {
        ArrayList<DownloadDictionary> dicArray = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (IWnnIME.getStringFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_FILE), null) != null) {
                DownloadDictionary dictionary = new DownloadDictionary(IWnnIME.getStringFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_NAME), null), IWnnIME.getStringFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_FILE), null), IWnnIME.getIntFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_CONVERT_HIGH), 0), IWnnIME.getIntFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_CONVERT_BASE), 0), IWnnIME.getIntFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_PREDICT_HIGH), 0), IWnnIME.getIntFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_PREDICT_BASE), 0), IWnnIME.getIntFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_MORPHO_HIGH), 0), IWnnIME.getIntFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_MORPHO_BASE), 0), IWnnIME.getBooleanFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_CACHE_FLAG), false), IWnnIME.getIntFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_LIMIT), 0));
                dictionary.enable();
                dicArray.add(dictionary);
            }
        }
        return dicArray;
    }

    private static boolean writeDownloadDictionaryToSharedPreferences(ArrayList<DownloadDictionary> dicArray, Context context) {
        removeAllDownloadDictionaryFromSharedPreferences(context);
        SharedPreferences sharedPref = context.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        int i = 0;
        for (DownloadDictionary dictionary : dicArray) {
            if (dictionary.isEnabled()) {
                editor.putString(createSharedPrefKey(i, QUERY_PARAM_NAME), dictionary.name());
                editor.putString(createSharedPrefKey(i, QUERY_PARAM_FILE), dictionary.file());
                editor.putInt(createSharedPrefKey(i, QUERY_PARAM_CONVERT_HIGH), dictionary.convertHigh());
                editor.putInt(createSharedPrefKey(i, QUERY_PARAM_CONVERT_BASE), dictionary.convertBase());
                editor.putInt(createSharedPrefKey(i, QUERY_PARAM_PREDICT_HIGH), dictionary.predictHigh());
                editor.putInt(createSharedPrefKey(i, QUERY_PARAM_PREDICT_BASE), dictionary.predictBase());
                editor.putInt(createSharedPrefKey(i, QUERY_PARAM_MORPHO_HIGH), dictionary.morphoHigh());
                editor.putInt(createSharedPrefKey(i, QUERY_PARAM_MORPHO_BASE), dictionary.morphoBase());
                editor.putBoolean(createSharedPrefKey(i, QUERY_PARAM_CACHE_FLAG), dictionary.cache());
                editor.putInt(createSharedPrefKey(i, QUERY_PARAM_LIMIT), dictionary.limit());
                i++;
            }
        }
        editor.commit();
        return setDownloadDictionary(context);
    }

    private boolean writeDownloadDictionaryToSharedPreferences(ArrayList<DownloadDictionary> dicArray) {
        return writeDownloadDictionaryToSharedPreferences(dicArray, getActivity());
    }

    private static void removeAllDownloadDictionaryFromSharedPreferences(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor defaultEditor = defaultPref.edit();
        for (int i = 0; i < 10; i++) {
            if (IWnnIME.getStringFromNotResetSettingsPreference(context, createSharedPrefKey(i, QUERY_PARAM_FILE), null) != null) {
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_NAME));
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_FILE));
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_CONVERT_HIGH));
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_CONVERT_BASE));
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_PREDICT_HIGH));
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_PREDICT_BASE));
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_MORPHO_HIGH));
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_MORPHO_BASE));
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_CACHE_FLAG));
                editor.remove(createSharedPrefKey(i, QUERY_PARAM_LIMIT));
            }
            if (defaultPref.getString(createSharedPrefKey(i, QUERY_PARAM_FILE), null) != null) {
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_NAME));
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_FILE));
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_CONVERT_HIGH));
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_CONVERT_BASE));
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_PREDICT_HIGH));
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_PREDICT_BASE));
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_MORPHO_HIGH));
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_MORPHO_BASE));
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_CACHE_FLAG));
                defaultEditor.remove(createSharedPrefKey(i, QUERY_PARAM_LIMIT));
            }
        }
        editor.commit();
        setDownloadDictionary(context);
    }

    public static String createSharedPrefKey(int index, String key) {
        return DOWNLOAD_DICTIONARY_NAME_KEY + "." + index + "#" + key;
    }

    public ArrayList<DownloadDictionary> getDownloadDictionary(Context context) {
        ArrayList<DownloadDictionary> cp = getDictionaryFromContentProvider(context);
        ArrayList<DownloadDictionary> fp = checkDictionaryFilePath(cp);
        ArrayList<DownloadDictionary> sp = readDownloadDictionaryFromSharedPreferences(context);
        ArrayList<DownloadDictionary> dictionary = checkConsistency(fp, sp, context);
        this.mDownloadDictionaries = dictionary;
        return dictionary;
    }

    public static ArrayList<DownloadDictionary> getDictionaryFromContentProvider(Context context) {
        ArrayList<DownloadDictionary> dicList = new ArrayList<>();
        iWnnEngine engine = iWnnEngine.getEngine();
        Cursor cursor = context.getContentResolver().query(Uri.parse(CONTENT_PROVIDER_URI), new String[0], LoggingEvents.EXTRA_CALLING_APP_NAME, new String[0], LoggingEvents.EXTRA_CALLING_APP_NAME);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                try {
                    try {
                        String dictionaryName = cursor.getString(cursor.getColumnIndexOrThrow(QUERY_PARAM_NAME));
                        String dictionaryFile = cursor.getString(cursor.getColumnIndexOrThrow(QUERY_PARAM_FILE));
                        if (engine.checkNameLength(dictionaryName, dictionaryFile)) {
                            int convertHigh = cursor.getInt(cursor.getColumnIndexOrThrow(QUERY_PARAM_CONVERT_HIGH));
                            int convertBase = cursor.getInt(cursor.getColumnIndexOrThrow(QUERY_PARAM_CONVERT_BASE));
                            int predictHigh = cursor.getInt(cursor.getColumnIndexOrThrow(QUERY_PARAM_PREDICT_HIGH));
                            int predictBase = cursor.getInt(cursor.getColumnIndexOrThrow(QUERY_PARAM_PREDICT_BASE));
                            int morphoHigh = cursor.getInt(cursor.getColumnIndexOrThrow(QUERY_PARAM_MORPHO_HIGH));
                            int morphoBase = cursor.getInt(cursor.getColumnIndexOrThrow(QUERY_PARAM_MORPHO_BASE));
                            boolean cacheFlag = cursor.getInt(cursor.getColumnIndexOrThrow(QUERY_PARAM_CACHE_FLAG)) != 0;
                            int limit = cursor.getInt(cursor.getColumnIndexOrThrow(QUERY_PARAM_LIMIT));
                            dicList.add(new DownloadDictionary(dictionaryName, dictionaryFile, convertHigh, convertBase, predictHigh, predictBase, morphoHigh, morphoBase, cacheFlag, limit));
                        }
                    } catch (IllegalArgumentException e) {
                        Log.e(TAG, "Error occured querying Content Provider.", e);
                        if (cursor != null) {
                            cursor.close();
                        }
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                    throw th;
                }
            }
            if (cursor != null) {
                cursor.close();
            }
        } else if (cursor != null) {
        }
        return dicList;
    }

    public static ArrayList<DownloadDictionary> checkDictionaryFilePath(ArrayList<DownloadDictionary> cpDictionary) {
        ArrayList<DownloadDictionary> retDic = new ArrayList<>();
        for (DownloadDictionary dic : cpDictionary) {
            if (new File(dic.file()).exists()) {
                retDic.add(dic);
            }
        }
        return retDic;
    }

    public static ArrayList<DownloadDictionary> checkConsistency(ArrayList<DownloadDictionary> contentProv, ArrayList<DownloadDictionary> sharedPref, Context context) {
        for (DownloadDictionary prefDic : sharedPref) {
            boolean match = false;
            Iterator<DownloadDictionary> contIt = contentProv.iterator();
            while (contIt.hasNext()) {
                if (prefDic.equals(contIt.next())) {
                    match = true;
                }
            }
            if (!match) {
                prefDic.disable();
                try {
                    context.getContentResolver().delete(Uri.parse(CONTENT_PROVIDER_URI), QUERY_PARAM_FILE + "=?", new String[]{prefDic.file()});
                } catch (IllegalArgumentException e) {
                    Log.e(TAG, "Error occured deleting Content Provider.", e);
                }
                writeDownloadDictionaryToSharedPreferences(sharedPref, context);
            }
        }
        for (DownloadDictionary contDic : contentProv) {
            contDic.disable();
            Iterator<DownloadDictionary> prefIt = sharedPref.iterator();
            while (true) {
                if (!prefIt.hasNext()) {
                    break;
                }
                if (contDic.equals(prefIt.next())) {
                    contDic.enable();
                    break;
                }
            }
        }
        return contentProv;
    }

    public static boolean setDownloadDictionary(Context context) {
        ArrayList<DownloadDictionary> dicArray = readDownloadDictionaryFromSharedPreferences(context);
        int index = 0;
        for (DownloadDictionary dic : dicArray) {
            iWnnEngine.getEngine().setDownloadDictionary(index, dic.name(), dic.file(), dic.convertHigh(), dic.convertBase(), dic.predictHigh(), dic.predictBase(), dic.morphoHigh(), dic.morphoBase(), dic.cache(), dic.limit());
            index++;
        }
        for (int i = index; i < 10; i++) {
            iWnnEngine.getEngine().setDownloadDictionary(i, null, null, 0, 0, 0, 0, 0, 0, false, 0);
        }
        return iWnnEngine.getEngine().refreshConfFile();
    }
}
