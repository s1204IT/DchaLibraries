package jp.co.omronsoft.iwnnime.ml.controlpanel;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.iwnn.iWnnEngine;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class AdditionalDictionaryPreference extends PreferenceFragment implements DialogInterface.OnClickListener {
    private static final String ADDITIONAL_DICTIONARY_NAME_KEY = "additional_dic_name";
    private static final String ALLOW_EXTENSION = ".d01";
    private static final String DIALOG_DICTIONAY_NAME_KEY = "titlelist";
    private static final String DIALOG_FRAGMENT_TAG = "additional_dic_dialog";
    public static final int MAX_ADDITIONAL_DIC = 10;
    private static final int MAX_DICTIONARY_NAME_LENGTH = 50;
    private static final int MAX_DICTIONARY_WORD_LENGTH = 50;
    private static final int MAX_DICTIONARY_WORD_LIST = 500;
    private static final String SD_ADDITIONAL_DIC_PATH = "/iwnn/additional_dic";
    private static final String TAG = "iWnn";
    private int mLanguage = -1;
    private Preference mClickPreference = null;

    private static class AdditionalDictionary implements Comparable<AdditionalDictionary> {
        private String mPath;
        private String mTitle;

        public AdditionalDictionary(String title, String path) {
            this.mTitle = title;
            this.mPath = path;
        }

        public String title() {
            return this.mTitle;
        }

        public String path() {
            return this.mPath;
        }

        @Override
        public int compareTo(AdditionalDictionary other) {
            return this.mTitle.compareTo(other.mTitle);
        }

        public boolean equals(Object other) {
            if (other instanceof AdditionalDictionary) {
                return this.mTitle.equals(((AdditionalDictionary) other).mTitle);
            }
            return false;
        }

        public int hashCode() {
            return this.mTitle.hashCode();
        }
    }

    public static class AdditionalDictionaryDialog extends DialogFragment {
        public static AdditionalDictionaryDialog newInstance(String[] titlelist) {
            AdditionalDictionaryDialog frag = new AdditionalDictionaryDialog();
            Bundle args = new Bundle();
            args.putStringArray(AdditionalDictionaryPreference.DIALOG_DICTIONAY_NAME_KEY, titlelist);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            String[] titlelist = getArguments().getStringArray(AdditionalDictionaryPreference.DIALOG_DICTIONAY_NAME_KEY);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setCancelable(true);
            builder.setNegativeButton(R.string.ti_dialog_button_cancel_txt, (DialogInterface.OnClickListener) null);
            builder.setTitle(getString(R.string.ti_preference_additional_dic_select_list_title_txt));
            builder.setItems(titlelist, (DialogInterface.OnClickListener) getTargetFragment());
            Dialog dialog = builder.create();
            return dialog;
        }
    }

    @Override
    public void onClick(DialogInterface di, int position) throws Throwable {
        if (position > 0) {
            AdditionalDictionary[] list = createDictionaryList();
            saveLocalDictionary(this.mClickPreference, list[position - 1]);
        } else {
            initializeLocalDictionary(this.mClickPreference);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Activity act = getActivity();
        PreferenceScreen rootPreferenceScreen = getPreferenceManager().createPreferenceScreen(act);
        rootPreferenceScreen.setTitle(R.string.ti_preference_additional_dic_title_txt);
        String listTitle = getString(R.string.ti_preference_additional_dic_list_title_txt);
        String noneText = getString(R.string.ti_preference_additional_dic_list_none_txt);
        Preference.OnPreferenceClickListener listener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference pref) {
                AdditionalDictionaryPreference.this.mClickPreference = pref;
                AdditionalDictionaryDialog dialog = AdditionalDictionaryDialog.newInstance(AdditionalDictionaryPreference.this.createTitleList(AdditionalDictionaryPreference.createDictionaryList()));
                dialog.setTargetFragment(AdditionalDictionaryPreference.this, 0);
                dialog.show(AdditionalDictionaryPreference.this.getFragmentManager(), AdditionalDictionaryPreference.DIALOG_FRAGMENT_TAG);
                return true;
            }
        };
        for (int i = 1; i <= 10; i++) {
            Preference preference = new Preference(act);
            preference.setTitle(listTitle + i);
            String key = getAdditionalDictionaryKey(i);
            String dictionaryName = IWnnIME.getStringFromNotResetSettingsPreference(act, key, noneText);
            setPreferenceSummary(preference, dictionaryName);
            preference.setOrder(i);
            preference.setOnPreferenceClickListener(listener);
            rootPreferenceScreen.addPreference(preference);
        }
        setPreferenceScreen(rootPreferenceScreen);
    }

    private String getAdditionalDictionaryKey(int index) {
        if (this.mLanguage == -1) {
            this.mLanguage = getCurrentLanguage(getActivity());
        }
        return createAdditionalDictionaryKey(this.mLanguage, index);
    }

    private static int getCurrentLanguage(Context context) {
        return LanguageManager.getChosenLanguageType("ja");
    }

    public static String createAdditionalDictionaryKey(int language, int index) {
        return ADDITIONAL_DICTIONARY_NAME_KEY + language + "_" + index;
    }

    private static void setPreferenceSummary(Preference pref, String summary) {
        pref.setSummary("[ " + summary + " ]");
    }

    private void saveLocalDictionary(Preference pref, AdditionalDictionary dictionary) throws Throwable {
        iWnnEngine engine = iWnnEngine.getEngine();
        if (prepareLocalDictionary(pref, false)) {
            engine.createAdditionalDictionary(getIwnnEngineDicType(pref));
        }
        if (prepareLocalDictionary(pref, true)) {
            engine.createAdditionalDictionary(getIwnnEngineDicType(pref));
        }
        boolean added = addWordsFromFile(pref, dictionary.path());
        SharedPreferences sharedPref = getActivity().getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        if (added) {
            setPreferenceSummary(pref, dictionary.title());
            editor.putString(getAdditionalDictionaryKey(pref.getOrder()), dictionary.title());
        } else {
            setPreferenceSummary(pref, getString(R.string.ti_preference_additional_dic_list_none_txt));
            String key = getAdditionalDictionaryKey(pref.getOrder());
            editor.remove(key);
            removeKeyForDefaultSharedPreferences(key);
            int type = getIwnnEngineDicType(pref);
            if (prepareLocalDictionary(pref, false)) {
                engine.deleteAdditionalDictionary(type);
            }
            if (prepareLocalDictionary(pref, true)) {
                engine.deleteAdditionalDictionary(type);
            }
        }
        editor.commit();
    }

    private boolean addWordsFromFile(Preference pref, String path) throws Throwable {
        BufferedReader buf;
        BufferedReader buf2 = null;
        Activity act = getActivity();
        try {
            try {
                buf = new BufferedReader(new InputStreamReader(new FileInputStream(path), IWnnIME.CHARSET_NAME_UTF8));
            } catch (Throwable th) {
                th = th;
            }
        } catch (FileNotFoundException e) {
            e = e;
        } catch (IOException e2) {
            e = e2;
        }
        try {
            buf.readLine();
            int count = 0;
            while (true) {
                String line = buf.readLine();
                if (line == null || 500 <= count) {
                    break;
                }
                int tab = line.indexOf(EmojiAssist.SPLIT_KEY);
                if (tab > 0 && 50 >= tab) {
                    int last = line.length();
                    int candidateLength = (last - tab) - 1;
                    if (50 >= candidateLength) {
                        String stroke = line.substring(0, tab);
                        String candidate = line.substring(tab + 1, last);
                        boolean failed = false;
                        if (prepareLocalDictionary(pref, false) && !addWordToLocalDictionary(stroke, candidate)) {
                            failed = true;
                            Log.e(TAG, "addWordsFromFile() : Fail to add word first");
                        }
                        if (!failed && prepareLocalDictionary(pref, true) && !addWordToLocalDictionary(stroke, candidate)) {
                            Log.e(TAG, "addWordsFromFile() : Fail to add word second");
                        }
                        if (!failed) {
                            count++;
                        }
                    }
                }
            }
            if (count > 0) {
                boolean failed2 = false;
                iWnnEngine engine = iWnnEngine.getEngine();
                if (prepareLocalDictionary(pref, false) && !engine.saveAdditionalDictionary(getIwnnEngineDicType(pref))) {
                    failed2 = true;
                }
                if (!failed2 && prepareLocalDictionary(pref, true) && !engine.saveAdditionalDictionary(getIwnnEngineDicType(pref))) {
                    failed2 = true;
                }
                if (!failed2) {
                    String message = String.valueOf(count) + " " + getString(R.string.ti_preference_additional_dic_load_comp_message_txt);
                    Toast.makeText(act, message, 0).show();
                    if (buf != null) {
                        try {
                            buf.close();
                        } catch (IOException e3) {
                            Log.e(TAG, "Fail to close BufferedReader", e3);
                        }
                    }
                    return true;
                }
            }
            if (buf != null) {
                try {
                    buf.close();
                    buf2 = buf;
                } catch (IOException e4) {
                    Log.e(TAG, "Fail to close BufferedReader", e4);
                    buf2 = buf;
                }
            } else {
                buf2 = buf;
            }
        } catch (FileNotFoundException e5) {
            e = e5;
            buf2 = buf;
            Log.e(TAG, "Not found a dictionary file", e);
            if (buf2 != null) {
                try {
                    buf2.close();
                } catch (IOException e6) {
                    Log.e(TAG, "Fail to close BufferedReader", e6);
                }
            }
        } catch (IOException e7) {
            e = e7;
            buf2 = buf;
            Log.e(TAG, "Fail to read line", e);
            if (buf2 != null) {
                try {
                    buf2.close();
                } catch (IOException e8) {
                    Log.e(TAG, "Fail to close BufferedReader", e8);
                }
            }
        } catch (Throwable th2) {
            th = th2;
            buf2 = buf;
            if (buf2 != null) {
                try {
                    buf2.close();
                } catch (IOException e9) {
                    Log.e(TAG, "Fail to close BufferedReader", e9);
                }
            }
            throw th;
        }
        Toast.makeText(act, getString(R.string.ti_preference_additional_dic_load_fail_message_txt), 0).show();
        return false;
    }

    private boolean addWordToLocalDictionary(String stroke, String candidate) {
        iWnnEngine engine = iWnnEngine.getEngine();
        return engine.addWord(new WnnWord(candidate, stroke)) == 0;
    }

    private boolean prepareLocalDictionary(Preference pref, boolean secondLang) {
        iWnnEngine engine = iWnnEngine.getEngine();
        int language = this.mLanguage;
        if (language == 0) {
            if (secondLang) {
                language = 1;
            }
        } else if (secondLang) {
            return false;
        }
        int type = getIwnnEngineDicType(pref);
        engine.setDictionary(language, type, getActivity().hashCode());
        return true;
    }

    private int getIwnnEngineDicType(Preference pref) {
        return (pref.getOrder() + 35) - 1;
    }

    private void initializeLocalDictionary(Preference pref) {
        iWnnEngine engine = iWnnEngine.getEngine();
        if (prepareLocalDictionary(pref, false)) {
            engine.deleteAdditionalDictionary(getIwnnEngineDicType(pref));
        }
        if (prepareLocalDictionary(pref, true)) {
            engine.deleteAdditionalDictionary(getIwnnEngineDicType(pref));
        }
        setPreferenceSummary(pref, getString(R.string.ti_preference_additional_dic_list_none_txt));
        Activity act = getActivity();
        SharedPreferences sharedPref = act.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
        SharedPreferences.Editor editor = sharedPref.edit();
        String key = getAdditionalDictionaryKey(pref.getOrder());
        editor.remove(key);
        editor.commit();
        removeKeyForDefaultSharedPreferences(key);
        Toast.makeText(act, R.string.ti_preference_additional_dic_intialize_message_txt, 0).show();
    }

    private String[] createTitleList(AdditionalDictionary[] list) {
        int count = list.length;
        String[] titleList = new String[count + 1];
        titleList[0] = getString(R.string.ti_preference_additional_dic_clear_txt);
        for (int i = 0; i < count; i++) {
            titleList[i + 1] = list[i].title();
        }
        return titleList;
    }

    public static boolean isEnableAdditionalDictionary(Context context) {
        String[] files = getDictionaryFilePaths();
        if (files != null && files.length != 0) {
            return true;
        }
        for (int i = 1; i <= 10; i++) {
            int language = getCurrentLanguage(context);
            String key = createAdditionalDictionaryKey(language, i);
            String dictionaryName = IWnnIME.getStringFromNotResetSettingsPreference(context, key, null);
            if (dictionaryName != null) {
                return true;
            }
        }
        return false;
    }

    private static String[] getDictionaryFilePaths() {
        String[] paths;
        File file = Environment.getExternalStorageDirectory();
        File dicDir = new File(file.getPath() + SD_ADDITIONAL_DIC_PATH);
        try {
            if (dicDir.isDirectory() && (paths = dicDir.list()) != null) {
                int size = paths.length;
                String base = dicDir.getPath() + "/";
                ArrayList<String> list = new ArrayList<>();
                int extensionLength = ALLOW_EXTENSION.length();
                for (int i = 0; i < size; i++) {
                    String path = paths[i];
                    int pathLength = path.length();
                    if (extensionLength < pathLength) {
                        String extension = path.substring(pathLength - extensionLength, pathLength);
                        if (extension.equals(ALLOW_EXTENSION)) {
                            list.add(base + paths[i]);
                        }
                    }
                }
                return (String[]) list.toArray(new String[list.size()]);
            }
            return null;
        } catch (SecurityException e) {
            return null;
        }
    }

    private static AdditionalDictionary[] createDictionaryList() throws Throwable {
        BufferedReader buf;
        int length;
        String[] paths = getDictionaryFilePaths();
        if (paths == null) {
            return new AdditionalDictionary[0];
        }
        ArrayList<AdditionalDictionary> list = new ArrayList<>();
        for (String path : paths) {
            BufferedReader buf2 = null;
            try {
                try {
                    buf = new BufferedReader(new InputStreamReader(new FileInputStream(path), IWnnIME.CHARSET_NAME_UTF8));
                } catch (Throwable th) {
                    th = th;
                }
            } catch (FileNotFoundException e) {
                e = e;
            } catch (IOException e2) {
                e = e2;
            }
            try {
                String title = buf.readLine();
                if (title != null && (length = title.length()) > 0 && length <= 50) {
                    list.add(new AdditionalDictionary(title, path));
                }
                if (buf != null) {
                    try {
                        buf.close();
                    } catch (IOException e3) {
                        Log.e(TAG, "Fail to close BufferedReader", e3);
                    }
                }
            } catch (FileNotFoundException e4) {
                e = e4;
                buf2 = buf;
                Log.e(TAG, "Not found a dictionary file", e);
                if (buf2 != null) {
                    try {
                        buf2.close();
                    } catch (IOException e5) {
                        Log.e(TAG, "Fail to close BufferedReader", e5);
                    }
                }
            } catch (IOException e6) {
                e = e6;
                buf2 = buf;
                Log.e(TAG, "Fail to read title", e);
                if (buf2 != null) {
                    try {
                        buf2.close();
                    } catch (IOException e7) {
                        Log.e(TAG, "Fail to close BufferedReader", e7);
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                buf2 = buf;
                if (buf2 != null) {
                    try {
                        buf2.close();
                    } catch (IOException e8) {
                        Log.e(TAG, "Fail to close BufferedReader", e8);
                    }
                }
                throw th;
            }
        }
        AdditionalDictionary[] ret = (AdditionalDictionary[]) list.toArray(new AdditionalDictionary[list.size()]);
        Arrays.sort(ret);
        return ret;
    }

    private void removeKeyForDefaultSharedPreferences(String key) {
        SharedPreferences defaultPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
        if (defaultPref.contains(key)) {
            SharedPreferences.Editor defaultEditor = defaultPref.edit();
            defaultEditor.remove(key);
            defaultEditor.commit();
        }
    }
}
