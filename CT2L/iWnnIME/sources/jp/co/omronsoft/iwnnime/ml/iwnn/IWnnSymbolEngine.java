package jp.co.omronsoft.iwnnime.ml.iwnn;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.common.speech.LoggingEvents;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiCategoryInfo;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiContract;
import jp.co.omronsoft.android.emoji.EmojiAssist;
import jp.co.omronsoft.iwnnime.ml.AdditionalSymbolList;
import jp.co.omronsoft.iwnnime.ml.ComposingText;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.R;
import jp.co.omronsoft.iwnnime.ml.WnnEngine;
import jp.co.omronsoft.iwnnime.ml.WnnUtility;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.candidate.CandidatesManager;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiList;

public class IWnnSymbolEngine implements WnnEngine {
    private static final int DATABASE_VERSION = 6;
    public static final int DB_MAXHISTORY = 50;
    private static final boolean DEBUG = false;
    private static final String DUMMY_STRING = "dummy";
    private static final String KEY_NAME_EMOJI_HISTORY = "emoji_history";
    private static final String KEY_NAME_KAO_MOJI_HISTORY = "kaomoji_history";
    private static final String KEY_NAME_SYMBOL_HISTORY = "symbol_history";
    public static final String LAST_SYMBOLLIST = "last_symbollist";
    public static final String LAST_SYMBOLLIST_ADD_SYMBOL = "last_symbollist_add_symbol";
    public static final int MAX_ITEM_IN_PAGE = 1000;
    public static final int MODE_ADD_SYMBOL = 7;
    public static final int MODE_DECOEMOJI = 6;
    public static final int MODE_EMOJI = 3;
    public static final int MODE_KAO_MOJI = 2;
    public static final int MODE_NONE = -1;
    public static final int MODE_SYMBOL = 1;
    private static final String TABLE_NAME = "SymbolEngine";
    private static final String TAG = "iWnn";
    private static final String TRANSFER_DATABASE_KEY = "transfer_database";
    private int mAdditionalSymbolIndex;
    private AdditionalSymbolList mAdditionalSymbolList;
    private String[] mAdditionalSymbolPackageList;
    private String[] mAdditionalSymbolTabList;
    private DecoEmojiList mDecoEmojiList;
    private iWnnEngine mEngine;
    private Context mLocalContext;
    private static final String DB_NAME_DOCOMO_EMOJI = "db_select_emoji";
    private static final String DB_NAME_EMOJI = "db_select_emoji_uni6";
    private static final String DB_NAME_HARFSIZESYMBOL = "db_select_harfwidth_symbol";
    private static final String DB_NAME_KAOMOJI = "db_select_kaomoji";
    private static final String[] TRANSFER_DB = {DB_NAME_DOCOMO_EMOJI, DB_NAME_EMOJI, DB_NAME_HARFSIZESYMBOL, DB_NAME_KAOMOJI};
    private static final String ROWID = "rowid";
    private static final String HISTORY_DATA = "history_data";
    private static final String[] QUERY_COLUMNS = {ROWID, HISTORY_DATA};
    private static final int[] SYM_TOGGLE_TABLE_JP = {1, 2, 6, 7, 3};
    private ArrayList<String> mEmojiHistoriesInfo = null;
    private ArrayList<String> mSymbolHistoriesInfo = null;
    private ArrayList<String> mKaomojiHistoriesInfo = null;
    private HashMap<String, ArrayList<EmojiAssist.DecoEmojiTextInfo>> mAddSymbolHistoriesInfoMap = new HashMap<>();
    private HashMap<String, EmojiAssist.DecoEmojiTextInfo> mAdditionalDecoEmojiInfoMap = new HashMap<>();
    private int mAddSymbolType = -1;
    private int mMode = -1;
    private int mLanguage = -1;
    private int mDictionary = -1;
    String[] mSymbolList = null;
    ArrayList<EmojiAssist.DecoEmojiTextInfo> mDecoList = null;
    private boolean mEnableEmoji = true;
    private boolean mEnableDecoEmoji = DEBUG;
    private boolean mEnableEmoticon = true;

    private static class DatabaseHelper extends SQLiteOpenHelper {
        DatabaseHelper(Context context, String str) {
            super(context, str, (SQLiteDatabase.CursorFactory) null, 6);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            if (oldVersion <= 3 && getDatabaseName().equals(IWnnSymbolEngine.DB_NAME_DOCOMO_EMOJI)) {
                try {
                    db.delete(IWnnSymbolEngine.TABLE_NAME, null, null);
                } catch (SQLException e) {
                    db.execSQL("DROP TABLE IF EXISTS SymbolEngine");
                }
            }
        }
    }

    public IWnnSymbolEngine(Context context, String locale) {
        this.mLocalContext = null;
        this.mEngine = null;
        this.mDecoEmojiList = null;
        this.mEngine = iWnnEngine.getEngine();
        this.mLocalContext = context;
        this.mDecoEmojiList = new DecoEmojiList(this.mLocalContext);
        this.mAdditionalSymbolList = new AdditionalSymbolList(this.mLocalContext);
        transferHistories();
    }

    public void resetHistories() {
        SharedPreferences pref = this.mLocalContext.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
        SharedPreferences.Editor editor = pref.edit();
        if (pref.contains(KEY_NAME_EMOJI_HISTORY)) {
            editor.remove(KEY_NAME_EMOJI_HISTORY);
        }
        if (pref.contains(KEY_NAME_SYMBOL_HISTORY)) {
            editor.remove(KEY_NAME_SYMBOL_HISTORY);
        }
        if (pref.contains(KEY_NAME_KAO_MOJI_HISTORY)) {
            editor.remove(KEY_NAME_KAO_MOJI_HISTORY);
        }
        this.mEmojiHistoriesInfo = null;
        this.mSymbolHistoriesInfo = null;
        this.mKaomojiHistoriesInfo = null;
        this.mAdditionalSymbolList.deleteHistories();
        editor.commit();
    }

    public boolean setDictionary(int language, int dictionary) {
        boolean success = this.mEngine.setDictionary(language, dictionary, this.mLocalContext.hashCode());
        if (success) {
            init(IWnnIME.getFilesDirPath(this.mLocalContext));
        } else {
            Log.e(TAG, "failed setDictionary()");
        }
        return success;
    }

    public void initializeMode() {
        this.mLanguage = -1;
        this.mDictionary = -1;
        this.mMode = -1;
    }

    public void setSymToggle() {
        boolean isInAddSymbol = DEBUG;
        if (this.mMode == 7 && this.mAdditionalSymbolPackageList != null) {
            this.mAdditionalSymbolIndex++;
            if (this.mAdditionalSymbolIndex < this.mAdditionalSymbolPackageList.length) {
                isInAddSymbol = true;
            }
        }
        if (isInAddSymbol) {
            setMode(7);
            return;
        }
        int[] table = SYM_TOGGLE_TABLE_JP;
        int length = table.length;
        int next = 0;
        if (this.mMode != -1) {
            int i = 0;
            while (true) {
                if (i >= length) {
                    break;
                }
                if (this.mMode != table[i]) {
                    i++;
                } else {
                    i++;
                    while (i < length && ((!this.mEnableEmoji && table[i] == 3) || ((!this.mEnableDecoEmoji && table[i] == 6) || ((!this.mEnableEmoticon && table[i] == 2) || (this.mAdditionalSymbolPackageList == null && table[i] == 7))))) {
                        i++;
                    }
                }
            }
            next = i % length;
        }
        setMode(table[next]);
    }

    public void setLastSymbollist(int mode) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mLocalContext);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt(LAST_SYMBOLLIST, mode);
        editor.commit();
        if (mode == 7) {
            String currentPackageName = getCurrentPackageName();
            editor.putString(LAST_SYMBOLLIST_ADD_SYMBOL, currentPackageName);
            editor.commit();
        }
    }

    public boolean setPriorityAddSymbollist() {
        updateAdditionalSymbolInfo();
        int index = -1;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this.mLocalContext);
        String lastPackageName = pref.getString(LAST_SYMBOLLIST_ADD_SYMBOL, null);
        if (lastPackageName != null && this.mAdditionalSymbolPackageList != null) {
            int mIndex = 0;
            while (true) {
                if (mIndex >= this.mAdditionalSymbolPackageList.length) {
                    break;
                }
                if (!lastPackageName.equals(this.mAdditionalSymbolPackageList[mIndex])) {
                    mIndex++;
                } else {
                    index = mIndex;
                    break;
                }
            }
        }
        if (index >= 0) {
            setAdditionalSymbolIndex(index);
            return true;
        }
        return DEBUG;
    }

    public void setMode(int mode) {
        this.mMode = mode;
        if (this.mLanguage != -1 && this.mDictionary != -1) {
            setDictionary(this.mLanguage, this.mDictionary);
        }
        this.mDictionary = -1;
        this.mLanguage = -1;
        if (mode != 7) {
            this.mAdditionalSymbolIndex = 0;
        }
        loadHistories();
        if (mode == 6) {
            this.mDecoEmojiList.initializeList();
        }
    }

    public int getMode() {
        return this.mMode;
    }

    private void transferHistories() {
        SharedPreferences pref = this.mLocalContext.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
        if (!pref.getBoolean(TRANSFER_DATABASE_KEY, DEBUG)) {
            SQLiteDatabase db = null;
            Cursor cursor = null;
            SharedPreferences.Editor editor = pref.edit();
            String dbDirPath = this.mLocalContext.getDatabasePath(DUMMY_STRING).getPath();
            int pos = dbDirPath.indexOf(DUMMY_STRING);
            File dbDir = new File(dbDirPath.substring(0, pos));
            if (dbDir != null && dbDir.exists()) {
                File[] fileList = dbDir.listFiles();
                if (fileList != null) {
                    for (File file : fileList) {
                        try {
                            try {
                                String file2 = file.getName();
                                if (Arrays.asList(TRANSFER_DB).contains(file2)) {
                                    DatabaseHelper dbHelper = new DatabaseHelper(this.mLocalContext, file2);
                                    db = dbHelper.getWritableDatabase();
                                    if (db != null) {
                                        cursor = db.query(TABLE_NAME, QUERY_COLUMNS, null, null, null, null, null);
                                        if (cursor != null) {
                                            int rowcount = cursor.getCount();
                                            if (rowcount > 0) {
                                                cursor.moveToLast();
                                                ArrayList<String> historiesInfo = new ArrayList<>();
                                                for (int j = 0; j < rowcount; j++) {
                                                    String info = cursor.getString(1);
                                                    historiesInfo.add(info);
                                                    cursor.moveToPrevious();
                                                }
                                                int mode = -1;
                                                if (file2.equals(DB_NAME_DOCOMO_EMOJI) || file2.equals(DB_NAME_EMOJI)) {
                                                    mode = 3;
                                                } else if (file2.equals(DB_NAME_KAOMOJI)) {
                                                    mode = 2;
                                                } else if (file2.equals(DB_NAME_HARFSIZESYMBOL)) {
                                                    mode = 1;
                                                }
                                                setHistories(historiesInfo, mode);
                                            }
                                            cursor.close();
                                        }
                                        db.close();
                                    }
                                }
                            } catch (SQLException e) {
                                Log.e(TAG, "IWnnSymbolEngine::openHistories " + e.toString());
                                if (db != null) {
                                    db.close();
                                }
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        } finally {
                            if (db != null) {
                                db.close();
                            }
                            if (cursor != null) {
                                cursor.close();
                            }
                        }
                    }
                    closeHistories();
                    for (File dbFile : fileList) {
                        this.mLocalContext.deleteDatabase(dbFile.getName());
                    }
                }
                boolean isSuccess = dbDir.delete();
                if (!isSuccess) {
                    Log.e(TAG, "IWnnSymbolEngine:transferHistories() Fail to delete database dir");
                }
            }
            editor.putBoolean(TRANSFER_DATABASE_KEY, true);
            editor.commit();
        }
    }

    private String getHistoryKey() {
        switch (this.mMode) {
            case 1:
                return KEY_NAME_SYMBOL_HISTORY;
            case 2:
                return KEY_NAME_KAO_MOJI_HISTORY;
            case 3:
                return KEY_NAME_EMOJI_HISTORY;
            default:
                return LoggingEvents.EXTRA_CALLING_APP_NAME;
        }
    }

    public void closeHistories() {
        SharedPreferences pref = this.mLocalContext.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
        SharedPreferences.Editor editor = pref.edit();
        if (this.mEmojiHistoriesInfo != null && this.mEmojiHistoriesInfo.size() > 0) {
            editor.putString(KEY_NAME_EMOJI_HISTORY, createHistoryString(this.mEmojiHistoriesInfo));
        }
        if (this.mSymbolHistoriesInfo != null && this.mSymbolHistoriesInfo.size() > 0) {
            editor.putString(KEY_NAME_SYMBOL_HISTORY, createHistoryString(this.mSymbolHistoriesInfo));
        }
        if (this.mKaomojiHistoriesInfo != null && this.mKaomojiHistoriesInfo.size() > 0) {
            editor.putString(KEY_NAME_KAO_MOJI_HISTORY, createHistoryString(this.mKaomojiHistoriesInfo));
        }
        updateAdditionalSymbolListHistories();
        if (!pref.getBoolean(TRANSFER_DATABASE_KEY, DEBUG)) {
            editor.putBoolean(TRANSFER_DATABASE_KEY, true);
        }
        editor.commit();
    }

    private void updateAdditionalSymbolListHistories() {
        Set<String> keySet = this.mAddSymbolHistoriesInfoMap.keySet();
        if (keySet != null) {
            String[] keyValues = new String[this.mAddSymbolHistoriesInfoMap.size()];
            keySet.toArray(keyValues);
            for (String key : keyValues) {
                ArrayList<EmojiAssist.DecoEmojiTextInfo> historiesInfo = this.mAddSymbolHistoriesInfoMap.get(key);
                if (historiesInfo != null) {
                    String[] tmpHistories = new String[50];
                    for (int j = 0; j < historiesInfo.size() && 50 > j; j++) {
                        EmojiAssist.DecoEmojiTextInfo decoEmojiTextInfo = historiesInfo.get(j);
                        String path = decoEmojiTextInfo.getUri();
                        StringBuilder strb = new StringBuilder(path);
                        if (this.mAddSymbolType == 2) {
                            String width = String.valueOf(decoEmojiTextInfo.getWidth());
                            String height = String.valueOf(decoEmojiTextInfo.getHeight());
                            String kind = String.valueOf(decoEmojiTextInfo.getKind());
                            strb.append(EmojiAssist.SPLIT_KEY);
                            strb.append(width);
                            strb.append(EmojiAssist.SPLIT_KEY);
                            strb.append(height);
                            strb.append(EmojiAssist.SPLIT_KEY);
                            strb.append(kind);
                        }
                        tmpHistories[j] = strb.toString();
                    }
                    this.mAdditionalSymbolList.updateHistories(key, tmpHistories);
                }
            }
            this.mAddSymbolHistoriesInfoMap.clear();
        }
    }

    private String createHistoryString(ArrayList<String> historiesInfo) {
        StringBuilder histories = new StringBuilder();
        for (String info : historiesInfo) {
            histories.append(info);
            histories.append(EmojiAssist.SPLIT_KEY);
        }
        return histories.toString();
    }

    private void loadHistories() {
        if (this.mMode == 7) {
            loadAdditionalSymbolHistories();
        } else {
            loadStandardHistories();
        }
    }

    private void loadStandardHistories() {
        if (getHistories() == null) {
            ArrayList<String> historiesInfo = new ArrayList<>();
            SharedPreferences pref = this.mLocalContext.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
            String history = pref.getString(getHistoryKey(), null);
            if (history != null) {
                String[] historyData = history.split(EmojiAssist.SPLIT_KEY);
                for (String info : historyData) {
                    historiesInfo.add(info);
                }
            } else {
                historiesInfo.clear();
            }
            setHistories(historiesInfo, this.mMode);
        }
    }

    private void loadAdditionalSymbolHistories() {
        EmojiAssist.DecoEmojiTextInfo info;
        if (getDecoEmojiTextInfoHistories() == null) {
            ArrayList<EmojiAssist.DecoEmojiTextInfo> historiesInfo = new ArrayList<>();
            String packageName = getCurrentPackageName();
            if (packageName != null) {
                String[] histories = loadAdditionalSymbolItem(0);
                if (histories != null && histories.length > 0) {
                    HashMap<String, EmojiAssist.DecoEmojiTextInfo> tempMap = (HashMap) this.mAdditionalDecoEmojiInfoMap.clone();
                    for (int historyCount = 0; historyCount < 50 && historyCount < histories.length; historyCount++) {
                        if (this.mAddSymbolType == 2) {
                            EmojiAssist.DecoEmojiTextInfo info2 = tempMap.get(histories[historyCount]);
                            info = info2;
                        } else {
                            info = new EmojiAssist.DecoEmojiTextInfo();
                        }
                        info.setUri(histories[historyCount]);
                        historiesInfo.add(info);
                    }
                }
                setDecoEmojiTextInfoHistories(historiesInfo);
            }
        }
    }

    private ArrayList<String> getHistories() {
        switch (this.mMode) {
            case 1:
                ArrayList<String> historiesInfo = this.mSymbolHistoriesInfo;
                return historiesInfo;
            case 2:
                ArrayList<String> historiesInfo2 = this.mKaomojiHistoriesInfo;
                return historiesInfo2;
            case 3:
                ArrayList<String> historiesInfo3 = this.mEmojiHistoriesInfo;
                return historiesInfo3;
            default:
                return null;
        }
    }

    private ArrayList<EmojiAssist.DecoEmojiTextInfo> getDecoEmojiTextInfoHistories() {
        switch (this.mMode) {
            case 7:
                String packageName = getCurrentPackageName();
                if (packageName != null && (tmpHistoriesInfo = this.mAddSymbolHistoriesInfoMap.get(packageName)) != null) {
                    break;
                }
                break;
        }
        return null;
    }

    private void setHistories(ArrayList<String> historiesInfo, int mode) {
        switch (mode) {
            case 1:
                this.mSymbolHistoriesInfo = historiesInfo;
                break;
            case 2:
                this.mKaomojiHistoriesInfo = historiesInfo;
                break;
            case 3:
                this.mEmojiHistoriesInfo = historiesInfo;
                break;
        }
    }

    private void setDecoEmojiTextInfoHistories(ArrayList<EmojiAssist.DecoEmojiTextInfo> historiesInfo) {
        switch (this.mMode) {
            case 7:
                String packageName = getCurrentPackageName();
                if (packageName != null) {
                    this.mAddSymbolHistoriesInfoMap.put(packageName, historiesInfo);
                }
                break;
        }
    }

    private void updateHistory(WnnWord word) {
        if (this.mMode != 7 && this.mMode != 6) {
            ArrayList<String> historiesInfo = getHistories();
            if (historiesInfo == null) {
                historiesInfo = new ArrayList<>();
            }
            String info = new String(word.candidate);
            historiesInfo.remove(info);
            historiesInfo.add(0, info);
            int length = historiesInfo.size();
            if (length > 50) {
                historiesInfo.remove(50);
            }
            setHistories(historiesInfo, this.mMode);
            return;
        }
        if (this.mMode == 7) {
            ArrayList<EmojiAssist.DecoEmojiTextInfo> historiesInfo2 = getDecoEmojiTextInfoHistories();
            if (historiesInfo2 == null) {
                historiesInfo2 = new ArrayList<>();
            }
            EmojiAssist.DecoEmojiTextInfo info2 = new EmojiAssist.DecoEmojiTextInfo();
            info2.setUri(word.candidate);
            for (int i = 0; i < historiesInfo2.size(); i++) {
                String str = historiesInfo2.get(i).getUri();
                if (str.equals(info2.getUri())) {
                    historiesInfo2.remove(i);
                }
            }
            historiesInfo2.add(0, info2);
            int length2 = historiesInfo2.size();
            if (length2 > 50) {
                historiesInfo2.remove(50);
            }
            setDecoEmojiTextInfoHistories(historiesInfo2);
        }
    }

    @Override
    public void init(String dirPath) {
        this.mEngine.init(dirPath);
    }

    @Override
    public void close() {
        closeHistories();
    }

    @Override
    public int predict(ComposingText text, int minLen, int maxLen) {
        return 1;
    }

    @Override
    public int convert(ComposingText text) {
        return 0;
    }

    @Override
    public int searchWords(String key) {
        return 0;
    }

    @Override
    public WnnWord getNextCandidate() {
        return null;
    }

    @Override
    public boolean learn(WnnWord word) {
        if (word == null) {
            return DEBUG;
        }
        if (!word.candidate.equals("\n")) {
            int symbolModeType = word.getSymbolMode();
            boolean setOtherMode = DEBUG;
            int currentMode = getMode();
            if (symbolModeType != -1 && symbolModeType != currentMode) {
                setOtherMode = true;
                setMode(symbolModeType);
            }
            updateHistory(word);
            if (setOtherMode) {
                setMode(currentMode);
            }
        }
        if (this.mMode == 6) {
            this.mEngine.breakSequence();
            return true;
        }
        try {
            String tmp = word.candidate;
            WnnWord learnWord = new WnnWord(0, tmp, tmp, word.attribute | 4);
            this.mEngine.learn(learnWord);
            return true;
        } catch (Exception ex) {
            Log.d("IWnnIME", "IWnnSymbolEngine:learn " + ex);
            return DEBUG;
        }
    }

    @Override
    public int addWord(WnnWord word) {
        return 0;
    }

    @Override
    public boolean deleteWord(WnnWord word) {
        return true;
    }

    @Override
    public void setPreferences(SharedPreferences pref) {
    }

    @Override
    public void breakSequence() {
        this.mEngine.breakSequence();
    }

    @Override
    public int makeCandidateListOf(int clausePosition) {
        return 1;
    }

    @Override
    public boolean initializeDictionary(int dictionary, int type) {
        return DEBUG;
    }

    @Override
    public boolean initializeDictionary(int dictionary) {
        return DEBUG;
    }

    @Override
    public WnnWord[] getUserDictionaryWords() {
        return null;
    }

    @Override
    public boolean isConverting() {
        return DEBUG;
    }

    public void updateAdditionalSymbolInfo() {
        this.mAdditionalSymbolPackageList = AdditionalSymbolList.getSelectAdditionalSymbolList(this.mLocalContext, this.mEnableEmoji, this.mEnableDecoEmoji);
        this.mAdditionalSymbolTabList = null;
        if (this.mAdditionalSymbolPackageList != null) {
            this.mAdditionalSymbolTabList = new String[this.mAdditionalSymbolPackageList.length];
            for (int i = 0; i < this.mAdditionalSymbolTabList.length; i++) {
                String packageName = this.mAdditionalSymbolPackageList[i];
                this.mAdditionalSymbolTabList[i] = this.mAdditionalSymbolList.getTabName(packageName);
            }
        }
    }

    public String[] getAdditionalSymbolTabNames() {
        if (this.mAdditionalSymbolTabList == null) {
            return null;
        }
        return (String[]) this.mAdditionalSymbolTabList.clone();
    }

    public int getAdditionalSymbolIndex() {
        return this.mAdditionalSymbolIndex;
    }

    public void setAdditionalSymbolIndex(int index) {
        this.mAdditionalSymbolIndex = index;
    }

    private String getCurrentPackageName() {
        if (this.mAdditionalSymbolPackageList == null) {
            return null;
        }
        String packageName = this.mAdditionalSymbolPackageList[this.mAdditionalSymbolIndex];
        return packageName;
    }

    @Override
    public int convertGijiStr(ComposingText text, int type) {
        return 0;
    }

    public boolean startLongPressActionAdditionalSymbol(WnnWord clickWord) {
        String packageName;
        if (this.mMode != 7 || clickWord == null || (packageName = getCurrentPackageName()) == null) {
            return DEBUG;
        }
        boolean ret = this.mAdditionalSymbolList.startLongPressActionAdditionalSymbol(packageName, clickWord.candidate);
        return ret;
    }

    @Override
    public ArrayList<Object> getCategoryList(int mode) {
        TypedArray typeArray = null;
        ArrayList<Object> ret = new ArrayList<>();
        Resources res = this.mLocalContext.getResources();
        switch (mode) {
            case 1:
                typeArray = res.obtainTypedArray(R.array.category_list_symbol);
                break;
            case 2:
                typeArray = res.obtainTypedArray(R.array.category_list_kaomoji);
                break;
            case 3:
                typeArray = res.obtainTypedArray(R.array.category_list_emoji);
                break;
            case 6:
                ret = getDecoEmojiCategoryList();
                break;
            case 7:
                ret = getAdditionalSymbolCategoryList();
                break;
        }
        if (typeArray != null) {
            int length = typeArray.length();
            for (int index = 0; index < length; index++) {
                int resId = typeArray.getResourceId(index, 0);
                ret.add(Integer.valueOf(resId));
            }
            typeArray.recycle();
        }
        return ret;
    }

    @Override
    public boolean hasHistory() {
        int historySize = 0;
        if (this.mMode != 7 && this.mMode != 6) {
            ArrayList<String> historiesInfo = getHistories();
            if (historiesInfo != null) {
                historySize = historiesInfo.size();
            }
        } else {
            ArrayList<EmojiAssist.DecoEmojiTextInfo> historiesInfo2 = getDecoEmojiTextInfoHistories();
            if (historiesInfo2 != null) {
                historySize = historiesInfo2.size();
            }
        }
        if (historySize > 0) {
            return true;
        }
        return DEBUG;
    }

    public ArrayList<Object> getAdditionalSymbolCategoryList() {
        String[] tempList;
        ArrayList<Object> ret = new ArrayList<>();
        String packageName = getCurrentPackageName();
        ret.add(Integer.valueOf(R.drawable.ic_emoji_recent_light));
        if (packageName != null && (tempList = this.mAdditionalSymbolList.getCategoryList(packageName)) != null) {
            for (String str : tempList) {
                ret.add(str);
            }
        }
        return ret;
    }

    public ArrayList<Object> getDecoEmojiCategoryList() {
        String categoryText;
        ArrayList<Object> ret = new ArrayList<>();
        ArrayList<DecoEmojiCategoryInfo> tempList = this.mDecoEmojiList.getCategoryList();
        boolean isJapanese = Locale.getDefault().getLanguage().equals(Locale.JAPANESE.toString());
        ret.add(Integer.valueOf(R.drawable.ic_emoji_recent_light));
        for (DecoEmojiCategoryInfo info : tempList) {
            if (isJapanese) {
                categoryText = info.getCategoryName_jpn();
            } else {
                categoryText = info.getCategoryName_eng();
            }
            ret.add(categoryText);
        }
        return ret;
    }

    public ArrayList<DecoEmojiCategoryInfo> getDecoEmojiCategoryInfoList() {
        return this.mDecoEmojiList.getCategoryList();
    }

    private String[] getHistoryItemList() {
        ArrayList<String> history = new ArrayList<>();
        this.mAdditionalDecoEmojiInfoMap.clear();
        if (this.mMode != 7 && this.mMode != 6) {
            ArrayList<String> historiesInfo = getHistories();
            if (historiesInfo != null) {
                Iterator<String> it = historiesInfo.iterator();
                while (it.hasNext()) {
                    history.add(it.next());
                }
            }
        } else {
            ArrayList<EmojiAssist.DecoEmojiTextInfo> historiesInfo2 = getDecoEmojiTextInfoHistories();
            if (historiesInfo2 != null) {
                for (EmojiAssist.DecoEmojiTextInfo info : historiesInfo2) {
                    history.add(info.getUri());
                    if (this.mAddSymbolType == 2) {
                        this.mAdditionalDecoEmojiInfoMap.put(info.getUri(), info);
                    }
                }
            }
        }
        String[] ret = (String[]) history.toArray(new String[history.size()]);
        return ret;
    }

    public void loadSymbolItem(int category) {
        this.mSymbolList = null;
        this.mDecoList = null;
        if (category == 0) {
            this.mSymbolList = getHistoryItemList();
        } else {
            switch (this.mMode) {
                case 1:
                    this.mSymbolList = loadSymbolItemFromXml(R.array.list_id_symbol, category, true);
                    break;
                case 2:
                    this.mSymbolList = loadSymbolItemFromXml(R.array.list_id_kaomoji, category, DEBUG);
                    break;
                case 3:
                    this.mSymbolList = loadSymbolItemFromXml(R.array.list_id_emoji_symbol, category, true);
                    break;
                case 6:
                    this.mDecoList = this.mDecoEmojiList.getDecoEmojiItemList(category);
                    this.mSymbolList = loadDecoEmojiItem(this.mDecoList);
                    break;
                case 7:
                    this.mSymbolList = loadAdditionalSymbolItem(category);
                    break;
            }
        }
        if (this.mSymbolList != null && this.mMode == 7) {
            ArrayList<String> tmpList = new ArrayList<>();
            for (int i = 0; i < this.mSymbolList.length; i++) {
                if (!iWnnEngine.getEngine().hasNonSupportCharacters(this.mSymbolList[i])) {
                    tmpList.add(this.mSymbolList[i]);
                }
            }
            this.mSymbolList = (String[]) tmpList.toArray(new String[tmpList.size()]);
        }
    }

    public String[] loadSymbolItemFromXml(int resourceId, int category, boolean parseLabel) {
        String[] ret = {null};
        try {
            Resources res = this.mLocalContext.getResources();
            String[] targetNameList = res.getStringArray(resourceId);
            int loadIndex = category - 1;
            if (loadIndex >= 0 && loadIndex < targetNameList.length) {
                int id = res.getIdentifier(targetNameList[loadIndex], "array", "jp.co.omronsoft.iwnnime.ml");
                String[] array = res.getStringArray(id);
                if (parseLabel) {
                    ArrayList<String> retList = new ArrayList<>();
                    for (String conv : array) {
                        int supportedMinSdkVersion = WnnUtility.getMinSupportSdkVersion(conv);
                        if (Build.VERSION.SDK_INT >= supportedMinSdkVersion) {
                            retList.add(WnnUtility.parseLabel(conv));
                        }
                    }
                    return (String[]) retList.toArray(new String[retList.size()]);
                }
                return array;
            }
            return ret;
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "IWnnSymbolEngine::loadSymbolItemFromXml " + e.toString());
            return ret;
        }
    }

    private String[] loadDecoEmojiItem(ArrayList<EmojiAssist.DecoEmojiTextInfo> list) {
        ArrayList<String> retList = new ArrayList<>();
        for (EmojiAssist.DecoEmojiTextInfo info : list) {
            retList.add(info.getUri());
        }
        String[] ret = (String[]) retList.toArray(new String[retList.size()]);
        return ret;
    }

    private String[] loadAdditionalSymbolItem(int categoryId) {
        String category;
        this.mAddSymbolType = -1;
        String packageName = getCurrentPackageName();
        String[] ret = {null};
        if (packageName != null) {
            if (categoryId == 0) {
                category = CandidatesManager.CATEGORY_NAME_HISTORY;
            } else {
                category = String.valueOf(categoryId - 1);
            }
            AdditionalSymbolList additionalSymbolList = this.mAdditionalSymbolList;
            this.mAddSymbolType = AdditionalSymbolList.getSymbolType(this.mLocalContext, packageName);
            if (this.mAddSymbolType == 2) {
                List<EmojiAssist.DecoEmojiTextInfo> list = this.mAdditionalSymbolList.getDecoEmojiCandidates(packageName, category);
                this.mAdditionalDecoEmojiInfoMap.clear();
                ArrayList<String> candidates = new ArrayList<>();
                if (list != null) {
                    int count = 0;
                    for (int i = 0; i < list.size(); i++) {
                        this.mAdditionalDecoEmojiInfoMap.put(list.get(i).getUri(), list.get(i));
                        int emojiKind = list.get(i).getKind();
                        int emojiType = IWnnIME.getEmojiType();
                        if (emojiType <= 0) {
                            emojiType = 1;
                        }
                        int convEmojiType = DecoEmojiContract.convertEmojiType(emojiKind);
                        if ((convEmojiType & emojiType) != 0 || convEmojiType == 0) {
                            if (convEmojiType == 0) {
                                if (count == 0 && candidates.size() > 0) {
                                    candidates.remove(candidates.size() - 1);
                                }
                                count = 0;
                            } else {
                                count++;
                            }
                            candidates.add(list.get(i).getUri());
                        }
                    }
                }
                return (String[]) candidates.toArray(new String[candidates.size()]);
            }
            return this.mAdditionalSymbolList.getCandidates(packageName, category);
        }
        return ret;
    }

    public String[] getCandidatesList() {
        return this.mSymbolList;
    }

    public ArrayList<EmojiAssist.DecoEmojiTextInfo> getDecoEmojiTextInfoList() {
        return this.mDecoList != null ? (ArrayList) this.mDecoList.clone() : this.mDecoList;
    }

    public int getAdditionalSymbolType() {
        return this.mAddSymbolType;
    }

    public HashMap<String, EmojiAssist.DecoEmojiTextInfo> getAdditionalDecoEmojiInfoMap() {
        return this.mAdditionalDecoEmojiInfoMap != null ? (HashMap) this.mAdditionalDecoEmojiInfoMap.clone() : this.mAdditionalDecoEmojiInfoMap;
    }

    @Override
    public boolean hasCandidate() {
        return true;
    }

    public void setEnableEmoji(boolean enableEmoji) {
        this.mEnableEmoji = enableEmoji;
    }

    public void setEnableDecoEmoji(boolean enableDecoEmoji) {
        this.mEnableDecoEmoji = enableDecoEmoji;
    }

    public void setEnableEmoticon(boolean enableEmoticon) {
        this.mEnableEmoticon = enableEmoticon;
    }

    public boolean isEnableEmoji() {
        return this.mEnableEmoji;
    }

    public boolean isEnableDecoEmoji() {
        return this.mEnableDecoEmoji;
    }

    public boolean isEnableEmoticon() {
        return this.mEnableEmoticon;
    }
}
