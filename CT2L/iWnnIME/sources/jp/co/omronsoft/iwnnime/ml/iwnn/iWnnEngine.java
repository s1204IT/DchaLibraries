package jp.co.omronsoft.iwnnime.ml.iwnn;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import com.android.common.speech.LoggingEvents;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jp.co.omronsoft.android.decoemojimanager.interfacedata.DecoEmojiAttrInfo;
import jp.co.omronsoft.iwnnime.ml.ComposingText;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.KeyboardLanguagePackData;
import jp.co.omronsoft.iwnnime.ml.StrSegment;
import jp.co.omronsoft.iwnnime.ml.WebAPIWnnEngine;
import jp.co.omronsoft.iwnnime.ml.WnnEngine;
import jp.co.omronsoft.iwnnime.ml.WnnWord;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelPrefFragment;
import jp.co.omronsoft.iwnnime.ml.controlpanel.ControlPanelStandard;
import jp.co.omronsoft.iwnnime.ml.controlpanel.DownloadDictionaryPreference;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiOperation;
import jp.co.omronsoft.iwnnime.ml.decoemoji.DecoEmojiOperationQueue;
import jp.co.omronsoft.iwnnime.ml.standardcommon.LanguageManager;

public class iWnnEngine implements WnnEngine {
    public static final int CANDIDATE_MAX = 350;
    public static final int CONVERT_TYPE_HANKATA = 4;
    public static final int CONVERT_TYPE_HAN_EIJI_CAP = 5;
    public static final int CONVERT_TYPE_HAN_EIJI_LOWER = 9;
    public static final int CONVERT_TYPE_HAN_EIJI_UPPER = 7;
    public static final int CONVERT_TYPE_HIRAGANA = 2;
    public static final int CONVERT_TYPE_KATAKANA = 3;
    public static final int CONVERT_TYPE_NONE = 1;
    public static final int CONVERT_TYPE_ZEN_EIJI_CAP = 6;
    public static final int CONVERT_TYPE_ZEN_EIJI_LOWER = 10;
    public static final int CONVERT_TYPE_ZEN_EIJI_UPPER = 8;
    private static final boolean DEBUG = false;
    public static final String DECO_OPERATION_SEPARATOR = ",";
    public static final int DICTIONARY_DELETE_FAILURE = -1;
    private static final String DICTIONARY_VERSION = "32category";
    public static final String DICTIONARY_VERSION_KEY = "dic_version";
    private static final String DIC_DIRCTORY_NAME = "/dicset";
    private static final int DIC_IDENTIFIER_SIZE = 4;
    private static final int DIC_TYPE_SIZE = 4;
    private static final int DIC_VERSION_SIZE = 4;
    public static final String FILENAME_DECO_OPERATION_EVENT_CACHE = "decoope_event.txt";
    public static final String FILENAME_DECO_OPERATION_PROCESSED_INDEX_CACHE = "decoope_processed_index";
    private static final int INDEX_OPERATION_EVENT = 0;
    private static final int INDEX_OPERATION_ID = 1;
    public static final int IWNNIME_DIC_LANG_DEF = 100;
    public static final String KEYNAME_DECO_OPERATION_EVENT_COUNT = "event_count";
    private static final String KEYNAME_DECO_OPERATION_PROCESSED_INDEX = "processed_index";
    private static final String KEY_NORMALIZATION_USER_DIC = "normalizationUserDic";
    private static final String KEY_REMOVE_OLD_VERSION_DIC = "removeOldVersionDic";
    public static final int LEARN_DICTIONARY_DELETE = 1;
    private static final String MASTER_DIC_PATH = "/dicset/master/";
    private static final int NUM_OPERATION_CATEGORY_DATA = 2;
    private static final int OFFSET_FULL_WIDTH = 65248;
    private static final String OLD_DIC_PATH = "/data/user/0/jp.co.omronsoft.iwnnime.ml/dicset";
    public static final int SERVICE_CONFIGURATION_FILE_MAX = 3;
    public static final int SERVICE_CONNECT_MAX = 3;
    private static final String TAG = "iWnn";
    private static final String TMP_DIC_PATH = "/dicset/tmp/";
    public static final int USER_DICTIONARY_DELETE = 2;
    public static final int WNNWORD_ATTRIBUTE_CONNECTED = 8192;
    public static final int WNNWORD_ATTRIBUTE_DECOEMOJI = 16777216;
    public static final int WNNWORD_ATTRIBUTE_DELETABLE = 2;
    public static final int WNNWORD_ATTRIBUTE_HISTORY = 1;
    public static final int WNNWORD_ATTRIBUTE_JAPANESE_QWERTY_GIJI = 128;
    public static final int WNNWORD_ATTRIBUTE_LATIN_GIJI = 32;
    public static final int WNNWORD_ATTRIBUTE_MUHENKAN = 4;
    public static final int WNNWORD_ATTRIBUTE_MUHENKAN_LOWERCASE = 256;
    public static final int WNNWORD_ATTRIBUTE_NEXT_BUTTON = 67108864;
    public static final int WNNWORD_ATTRIBUTE_NO_CANDIDATE = 2048;
    public static final int WNNWORD_ATTRIBUTE_NO_DICTIONARY = 64;
    public static final int WNNWORD_ATTRIBUTE_PREV_BUTTON = 33554432;
    public static final int WNNWORD_ATTRIBUTE_SERVICE_WORD_NO_ENGINE = 4096;
    public static final int WNNWORD_ATTRIBUTE_SYMBOL = 8;
    public static final int WNNWORD_ATTRIBUTE_SYMBOLLIST = 16;
    public static final int WNNWORD_ATTRIBUTE_TARGET_LEARN = 32768;
    public static final int WNNWORD_ATTRIBUTE_WEBAPI = 512;
    public static final int WNNWORD_ATTRIBUTE_WEBAPI_GET_AGAIN = 16384;
    public static final int WNNWORD_ATTRIBUTE_WEBAPI_WORD = 1024;
    private int mCaller;
    private HashMap<String, WnnWord> mCandTable;
    private int mCaseGijiListIndex;
    private IWnnCore mCore;
    private boolean mDispWebAPIButton;
    private boolean mDispWebAPIWords;
    private boolean mHasSearchWords;
    private String mServiceConnectedName;
    private boolean mWebAPIEnable;
    private boolean mWebAPIEnableFromSettings;
    private static final String[] CONF_TABLE = {"/system/lib/lib_dic_ja_JP.conf.so", "/system/lib/lib_dic_en_USUK.conf.so"};
    private static final String[] CONF_TABLET_TABLE = {"/system/lib/lib_dic_ja_JP.conf.so", "/system/lib/lib_dic_en_tablet_USUK.conf.so"};
    private static final int[] CONVERT_TYPE_LIST = {9, 10, 7, 8, 5, 6};
    private static final int[] CONVERT_TYPE_LIST_HALF = {9, 7, 5};
    private static iWnnEngine mEngine = new iWnnEngine();
    private static ArrayList<iWnnEngine> mServiceEngine = new ArrayList<>();
    public static int OPERATION_ID_INIT = -1;
    private static final byte[] DIC_IDENTIFIER_NJEX = {78, 74, 69, 88};
    private static final byte[] DIC_IDENTIFIER_NJDC = {78, 74, 68, 67};
    private static final byte[] DIC_IDENTIFIER_NJGG = {78, 74, 71, 71};
    private static final byte[] DIC_TYPE_LEARNING = {-128, 2, 0, 0};
    private static final byte[] DIC_TYPE_UNCOMPRESSED_LEARNING = {0, 2, 0, 3};
    private String mSearchKey = null;
    private String mForecastKey = null;
    private int mOutputNum = 0;
    private int mSegment = 0;
    private int mSegmentCount = 0;
    private int mSearchCnt = 0;
    private ComposingText mSearchComposingText = null;
    private String[] mCaseGijiList = null;
    private boolean mIsRequestGiji = true;
    private Pattern mAllowDuplicationCharPattern = Pattern.compile(".*[ぁあぃいぅうぇえぉおかがきぎくぐけげこごさざしじすずせぜそぞただちぢっつづてでとどなにぬねのはばぱひびぴふぶぷへべぺほぼぽまみむめもゃやゅゆょよらりるれろゎわゐゑをん].*");
    private boolean mIsForbidDuplication = DEBUG;
    private boolean mIsConverting = DEBUG;
    private LatinFilter mLatinFilter = new LatinFilter();
    private boolean mEnableConvertedCandidate = DEBUG;
    private int mLangType = -1;
    private int mDictionarySet = -1;
    private boolean mHasBroke = true;
    private int mWebAPIOutputNum = 0;
    private boolean mEnableLearnNumber = DEBUG;
    private boolean mIsServiceDics = DEBUG;
    private boolean mHasLoadedDownloadDictionary = DEBUG;
    private boolean mUpdateDownloadDictionary = DEBUG;
    private String mPackageName = null;
    private String mPassWord = null;
    private LinkedList<DecoEmojiOperation> mOperationQueue = new LinkedList<>();
    private boolean mIsRegeneratedOperationQueue = DEBUG;
    private boolean mEnableHeadConv = DEBUG;
    private String mFilesDirPath = null;
    private boolean mIsNormalizationUserDic = DEBUG;
    private WebAPIWnnEngine mWebAPIWnnEngine = new WebAPIWnnEngine();

    public static final class AddWordDictionaryType {
        public static final int ADD_WORD_DICTINARY_TYPE_PROGRAM = 2;
        public static final int ADD_WORD_DICTIONARY_TYPE_LEARNING = 1;
        public static final int ADD_WORD_DICTIONARY_TYPE_USER = 0;
    }

    public static final class FlexibleSearchType {
        public static final int FLEXIBLE_SEARCH_OFF = 0;
        public static final int FLEXIBLE_SEARCH_ON = 1;
    }

    public static final class KeyboardType {
        public static final int KEY_TYPE_KEYPAD12 = 0;
        public static final int KEY_TYPE_NONE = 255;
        public static final int KEY_TYPE_QWERTY = 1;
    }

    public static final class SearchMethod {
        public static final int SEARCH_CONNECTION = 2;
        public static final int SEARCH_ORIGINAL_PULL_FRONT = 1;
        public static final int SEARCH_ORIGINAL_PULL_PERFECTION = 0;
        public static final int SEARCH_REVERSE_PULL_FRONT = 4;
        public static final int SEARCH_REVERSE_PULL_PERFECTION = 3;
    }

    public static final class SearchOrder {
        public static final int ORDER_FREQUENCY = 0;
        public static final int ORDER_READING = 1;
        public static final int ORDER_REGISTRATION = 2;
    }

    public static final class SetType {
        public static final int ADDITIONALDIC = 35;
        public static final int AUTOLEARNINGDIC = 45;
        public static final int DICTIONARY_TYPE_MAX = 57;
        public static final int DOWNLOADDIC = 46;
        public static final int EISUKANA = 1;
        public static final int EMAIL_ADDRESS = 5;
        public static final int JINMEI = 3;
        public static final int KAOMOJI = 2;
        public static final int LEARNDIC = 11;
        public static final int NONE = -1;
        public static final int NORMAL = 0;
        public static final int POSTAL_ADDRESS = 4;
        public static final int USERDIC = 10;
        public static final int USERDIC_EMAIL = 13;
        public static final int USERDIC_NAME = 12;
        public static final int USERDIC_PHONE = 14;
    }

    private class LatinFilter {
        private static final int CASE_HEAD_UPPER = 3;
        private static final int CASE_LOWER = 0;
        private static final int CASE_UPPER = 1;
        private HashMap<String, WnnWord> mCandEnglishTable = new HashMap<>();
        private int mCandidateCase;
        private String mInputString;

        public LatinFilter() {
        }

        private void clearLatinFilter() {
            this.mCandEnglishTable.clear();
            this.mCandidateCase = 0;
        }

        private void setSearchKey(String input) {
            this.mInputString = input;
            if (input.length() != 0) {
                if (Character.isUpperCase(input.charAt(0))) {
                    if (input.length() > 1 && Character.isUpperCase(input.charAt(1))) {
                        this.mCandidateCase = 1;
                        return;
                    } else {
                        this.mCandidateCase = 3;
                        return;
                    }
                }
                this.mCandidateCase = 0;
            }
        }

        private String candidateConversion(String candidate) {
            String str = candidate;
            if (str == null) {
                return str;
            }
            if (str.equals(iWnnEngine.this.toLowerCase(iWnnEngine.this.mSearchKey))) {
                return iWnnEngine.this.mSearchKey;
            }
            switch (this.mCandidateCase) {
                case 1:
                    if (iWnnEngine.this.toUpperCase(candidate).length() == candidate.length()) {
                        str = iWnnEngine.this.toUpperCase(candidate);
                    }
                    break;
                case 3:
                    char top = candidate.charAt(0);
                    if (Character.isLowerCase(top)) {
                        String tmp = iWnnEngine.this.toUpperCase(candidate);
                        if (tmp.length() == candidate.length()) {
                            char top2 = tmp.charAt(0);
                            str = Character.toString(top2) + candidate.substring(1);
                        }
                    }
                    break;
            }
            return str;
        }

        private boolean putCandidate(WnnWord word) {
            if (this.mInputString == null || word == null || word.candidate == null) {
                return iWnnEngine.DEBUG;
            }
            if (this.mInputString.length() <= 1) {
                if (this.mCandEnglishTable.containsKey(word.candidate)) {
                    return iWnnEngine.DEBUG;
                }
                this.mCandEnglishTable.put(word.candidate, word);
                return true;
            }
            if (this.mCandEnglishTable.containsKey(iWnnEngine.this.toLowerCase(word.candidate))) {
                return iWnnEngine.DEBUG;
            }
            this.mCandEnglishTable.put(iWnnEngine.this.toLowerCase(word.candidate), word);
            return true;
        }
    }

    private iWnnEngine() {
        this.mCore = null;
        this.mCandTable = null;
        this.mCore = new IWnnCore();
        this.mCandTable = new HashMap<>();
    }

    public static iWnnEngine getEngine() {
        return mEngine;
    }

    public static iWnnEngine getEngineForService(String serviceConnectedName) {
        for (int i = 0; i < mServiceEngine.size(); i++) {
            if (!serviceConnectedName.equals(LoggingEvents.EXTRA_CALLING_APP_NAME) && mServiceEngine.get(i).mServiceConnectedName.equals(serviceConnectedName)) {
                return mServiceEngine.get(i);
            }
        }
        if (mServiceEngine.size() >= 3) {
            return null;
        }
        iWnnEngine serviceEngine = new iWnnEngine();
        serviceEngine.mServiceConnectedName = serviceConnectedName;
        mServiceEngine.add(serviceEngine);
        return serviceEngine;
    }

    public static void clearServiceEngine() {
        for (int i = 0; i < mServiceEngine.size(); i++) {
            mServiceEngine.get(i).mCore.destroyWnnInfo();
        }
        mServiceEngine.clear();
    }

    public void setServiceConnectedName(String serviceConnectedName) {
        this.mServiceConnectedName = serviceConnectedName;
    }

    private String getEncryptedPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes(IWnnIME.CHARSET_NAME_UTF8));
            byte[] hash = md.digest();
            StringBuffer hexString = new StringBuffer();
            for (int i = 0; i < hash.length; i++) {
                if ((hash[i] & 255) < 16) {
                    hexString.append("0" + Integer.toHexString(hash[i] & 255));
                } else {
                    hexString.append(Integer.toHexString(hash[i] & 255));
                }
            }
            String encryptedPassword = hexString.toString();
            return encryptedPassword;
        } catch (Exception ex) {
            Log.e("IWnnIME", "iWnnEngine:setDictionary " + ex);
            return null;
        }
    }

    public boolean setDictionary(int language, int setType, int caller, String serviceFile, String packageName, String password) {
        normalizationUserDic();
        removeOldVersionDic();
        resetExtendedInfo();
        if (packageName != null && !packageName.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
            String encryptedPassword = getEncryptedPassword(password);
            if (encryptedPassword == null) {
                return DEBUG;
            }
            this.mPassWord = encryptedPassword;
        }
        if (this.mCaller != caller && this.mLangType != language) {
            close();
        }
        this.mCaller = caller;
        this.mPackageName = packageName;
        return setDictionary(language, setType, serviceFile);
    }

    public boolean setDictionary(int language, int setType, int caller) {
        return setDictionary(language, setType, caller, null, this.mPackageName, this.mPassWord);
    }

    public boolean setDictionary(Object caller) {
        return setDictionary(this.mLangType, this.mDictionarySet, caller.hashCode());
    }

    private boolean setDictionary(int language, int dictionary) {
        return setDictionary(language, dictionary, (String) null);
    }

    private boolean setDictionary(int language, int dictionary, String serviceFile) {
        KeyboardLanguagePackData langPack;
        String targetLangPackClassName;
        String[] confTable = getConfTable();
        if (serviceFile == null && (language <= -1 || language >= LanguageManager.getLanguageListSize())) {
            Log.e(TAG, "iWnnEngine::setDictionary() END unknown Language type error. return = false");
            return DEBUG;
        }
        if (this.mDictionarySet == dictionary && this.mLangType == language && !this.mUpdateDownloadDictionary) {
            return DEBUG;
        }
        if (57 <= dictionary || -1 >= dictionary) {
            Log.e(TAG, "iWnnEngine::setDictionary() END unknown dictionary type error. return = false");
            return DEBUG;
        }
        setDownloadDictionary();
        this.mCore.setServicePackageName(this.mPackageName, this.mPassWord);
        String confFile = null;
        if (serviceFile != null) {
            this.mIsServiceDics = true;
            confFile = serviceFile;
        } else if (language == 0 || language == 1) {
            confFile = confTable[language];
        } else {
            Context con = IWnnIME.getCurrentIme();
            if (con == null) {
                con = ControlPanelStandard.getCurrentControlPanel();
            }
            if (con != null && (targetLangPackClassName = (langPack = KeyboardLanguagePackData.getInstance()).getLangPackClassName(con, language)) != null) {
                String targetLangPackName = targetLangPackClassName.substring(0, targetLangPackClassName.lastIndexOf(46));
                confFile = langPack.getConfFile(con, targetLangPackName);
            }
        }
        boolean success = this.mCore.setDictionary(language, dictionary, confFile, language != this.mLangType ? true : DEBUG, this.mFilesDirPath);
        if (success) {
            this.mLangType = language;
            this.mDictionarySet = dictionary;
            clearCandidates();
            setUpdateDownloadDictionary(DEBUG);
            return success;
        }
        return success;
    }

    @Override
    public void breakSequence() {
        this.mHasBroke = true;
    }

    private String getSegmentString(int index) {
        String string = this.mCore.getSegmentString(index);
        if (string == null) {
            return null;
        }
        return string;
    }

    private String getSegmentStroke(int index) {
        String stroke = this.mCore.getSegmentStroke(index);
        if (stroke == null) {
            return null;
        }
        return stroke;
    }

    private WnnWord getCandidate(int index) {
        String candidate = this.mCore.getResultString(this.mSegment, index);
        String stroke = this.mCore.getResultStroke(this.mSegment, index);
        if (candidate == null || stroke == null) {
            return null;
        }
        int attribute = 0;
        boolean islearn = this.mCore.isLearnDictionary(index);
        if (islearn) {
            attribute = 2;
        }
        if (isLowercaseStrokeInLearning() && this.mCore.isGijiDic(index) && this.mSearchKey.equals(candidate)) {
            attribute = 32;
        }
        if (this.mEnableConvertedCandidate) {
            candidate = this.mLatinFilter.candidateConversion(candidate);
        }
        return new WnnWord(index, candidate, stroke, attribute);
    }

    private void clearCandidates() {
        this.mOutputNum = 0;
        this.mSearchKey = null;
        this.mForecastKey = null;
        this.mIsForbidDuplication = DEBUG;
        this.mDispWebAPIWords = DEBUG;
        this.mDispWebAPIButton = true;
        this.mCandTable.clear();
        this.mLatinFilter.clearLatinFilter();
        this.mWebAPIWnnEngine.clearCandidates();
        this.mWebAPIOutputNum = 0;
    }

    public String toUpperCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toUpperCase(LanguageManager.getChosenLocale(this.mLangType));
    }

    public String toLowerCase(String str) {
        if (str == null) {
            return null;
        }
        return str.toLowerCase(LanguageManager.getChosenLocale(this.mLangType));
    }

    @Override
    public void init(String dirPath) {
        if (dirPath != null) {
            this.mFilesDirPath = dirPath;
        }
        this.mCore.init(this.mFilesDirPath);
        clearCandidates();
        this.mIsConverting = DEBUG;
        this.mWebAPIWnnEngine.init();
    }

    @Override
    public void close() {
        this.mDictionarySet = -1;
        this.mLangType = -1;
        this.mCore.unmountDictionary();
        clearCandidates();
        this.mIsServiceDics = DEBUG;
    }

    private void removeOldVersionDic() {
        File[] files;
        if (this.mFilesDirPath != null) {
            Context context = IWnnIME.getCurrentIme();
            if (context == null) {
                context = ControlPanelStandard.getCurrentControlPanel();
            }
            if (context == null) {
                Log.e(TAG, "removeOldVersionDic() Fail to get context");
                return;
            }
            SharedPreferences pref = context.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
            if (pref != null) {
                boolean wasRemove = pref.getBoolean(KEY_REMOVE_OLD_VERSION_DIC, DEBUG);
                if (!wasRemove) {
                    String[] targets = {MASTER_DIC_PATH, TMP_DIC_PATH};
                    for (String targetPath : targets) {
                        String dicPath = this.mFilesDirPath + targetPath;
                        File dicDir = new File(dicPath);
                        if (dicDir != null && (files = dicDir.listFiles()) != null) {
                            for (File file : files) {
                                String fileName = file.getName();
                                if (fileName != null) {
                                    String[] strAry = fileName.split("_");
                                    boolean isOldFile = DEBUG;
                                    if (strAry != null && strAry.length > 2) {
                                        if (strAry[0].length() == 3 && strAry[1].length() == 3) {
                                            try {
                                                Double.parseDouble(strAry[0]);
                                                Double.parseDouble(strAry[1]);
                                                isOldFile = true;
                                            } catch (NumberFormatException e) {
                                            }
                                            if (isOldFile) {
                                            }
                                        }
                                    } else if (isOldFile) {
                                        continue;
                                    } else {
                                        boolean ret = file.delete();
                                        if (!ret) {
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    SharedPreferences.Editor editor = pref.edit();
                    if (editor != null) {
                        editor.putBoolean(KEY_REMOVE_OLD_VERSION_DIC, true);
                        editor.commit();
                    }
                }
            }
        }
    }

    @Override
    public int predict(ComposingText text, int minLen, int maxLen) {
        this.mSearchComposingText = text;
        this.mCaseGijiList = null;
        clearCandidates();
        this.mSegment = 0;
        this.mSegmentCount = 0;
        this.mIsRequestGiji = minLen == 0;
        this.mIsConverting = DEBUG;
        this.mHasSearchWords = DEBUG;
        if (text == null) {
            return 0;
        }
        if (this.mEnableConvertedCandidate) {
            this.mLatinFilter.setSearchKey(text.toString(1));
        }
        String input = text.toString(1);
        if (input == null) {
            return 0;
        }
        if (maxLen >= 0 && maxLen < input.length()) {
            input = input.substring(0, maxLen);
        }
        this.mSearchKey = input;
        Matcher matcher = this.mAllowDuplicationCharPattern.matcher(input);
        if (!matcher.matches()) {
            this.mIsForbidDuplication = true;
        }
        String input2 = stripAlphabetsIfJP(input);
        this.mForecastKey = input2;
        int ret = this.mCore.forecast(input2, minLen, maxLen, getEnableHeadConversion());
        this.mWebAPIEnable = this.mWebAPIEnableFromSettings;
        if (this.mWebAPIEnable && (input2.length() == 0 || input2.length() < minLen || this.mDictionarySet == 1 || this.mDictionarySet == 2)) {
            this.mWebAPIEnable = DEBUG;
        }
        return ret;
    }

    @Override
    public int convert(ComposingText text) {
        this.mSearchComposingText = text;
        this.mCaseGijiList = null;
        clearCandidates();
        this.mSegment = 0;
        this.mSegmentCount = 0;
        this.mIsRequestGiji = DEBUG;
        this.mIsConverting = true;
        this.mHasSearchWords = DEBUG;
        this.mWebAPIEnable = this.mWebAPIEnableFromSettings;
        if (text == null) {
            return 0;
        }
        if (this.mEnableConvertedCandidate) {
            this.mLatinFilter.setSearchKey(text.toString(1));
        }
        String input = text.toString(1);
        if (input == null) {
            return 0;
        }
        this.mSearchKey = input;
        Matcher matcher = this.mAllowDuplicationCharPattern.matcher(input);
        if (!matcher.matches()) {
            this.mIsForbidDuplication = true;
        }
        int ret = this.mCore.conv(input, text.getCursor(1));
        if (ret <= 0) {
            return 0;
        }
        StrSegment[] ss = new StrSegment[ret];
        int pos = 0;
        for (int i = 0; i < ret; i++) {
            String candidate = getSegmentString(i);
            String stroke = getSegmentStroke(i);
            if (candidate == null || stroke == null) {
                return 0;
            }
            int len = stroke.length();
            ss[i] = new StrSegment(candidate, pos, (pos + len) - 1);
            pos += len;
        }
        text.setCursor(2, text.size(2));
        text.replaceStrSegment(2, ss, text.getCursor(2));
        this.mSegmentCount = ret;
        return ret;
    }

    public int searchWords(String key, int method, int order) {
        this.mSearchCnt = 0;
        int ret = this.mCore.searchWord(method, order, key);
        if (ret < 0) {
            Log.e(TAG, "iWnnEngine::searchWord() error. ret=" + ret);
        }
        this.mHasSearchWords = true;
        return ret;
    }

    @Override
    public int searchWords(String key) {
        int method;
        int order;
        if (LoggingEvents.EXTRA_CALLING_APP_NAME.equals(key)) {
            method = 1;
            order = 1;
        } else {
            method = 0;
            order = 0;
        }
        return searchWords(key, method, order);
    }

    @Override
    public WnnWord getNextCandidate() {
        WnnWord word = getNextCandidateInternal();
        if (word != null) {
            this.mCandTable.put(word.candidate, word);
        }
        return word;
    }

    public WnnWord getNextCandidateInternal() {
        WnnWord word = null;
        if (this.mHasSearchWords) {
            word = getWord(this.mSearchCnt);
            if (word != null) {
                this.mSearchCnt++;
            }
        } else {
            if (this.mSearchKey == null) {
                return null;
            }
            if (this.mCaseGijiList != null) {
                word = createCaseGiji(null, DEBUG, DEBUG);
            } else {
                for (int cnt = 0; cnt < 350 && (word = getCandidate(this.mOutputNum)) != null; cnt++) {
                    boolean giji = this.mCore.isGijiDic(this.mOutputNum);
                    this.mOutputNum++;
                    if (this.mIsForbidDuplication || giji) {
                        if (!this.mCandTable.containsKey(word.candidate)) {
                            break;
                        }
                    } else {
                        if (!this.mEnableConvertedCandidate || this.mLatinFilter.putCandidate(word)) {
                            break;
                        }
                    }
                }
                if (word == null) {
                    if (this.mEnableConvertedCandidate) {
                        if (this.mSearchKey.length() < 1) {
                            word = null;
                        } else {
                            word = createCaseGiji(this.mSearchKey, true, DEBUG);
                        }
                    } else if (!this.mIsConverting && this.mLangType == 0) {
                        Pattern gijiPattern = Pattern.compile("^[a-z0-9０-９]+$");
                        int cursor = this.mSearchComposingText.getCursor(0);
                        String text = this.mSearchComposingText.toString(0, 0, cursor - 1);
                        if (gijiPattern.matcher(text).matches()) {
                            word = createCaseGiji(text, true, true);
                        }
                    }
                }
            }
            if (word == null && this.mLangType == 0 && this.mWebAPIEnable) {
                if (this.mDispWebAPIButton) {
                    this.mDispWebAPIButton = DEBUG;
                    word = new WnnWord(this.mOutputNum, LoggingEvents.EXTRA_CALLING_APP_NAME, LoggingEvents.EXTRA_CALLING_APP_NAME, 512);
                    this.mOutputNum++;
                } else if (this.mDispWebAPIWords) {
                    int cnt2 = 0;
                    while (true) {
                        if (cnt2 >= 500) {
                            break;
                        }
                        word = this.mWebAPIWnnEngine.getNextCandidate();
                        if (word != null) {
                            this.mOutputNum++;
                            if (!hasNonSupportCharacters(word.candidate) && !this.mCandTable.containsKey(word.candidate)) {
                                this.mWebAPIOutputNum++;
                                break;
                            }
                            cnt2++;
                        } else if (!isWebApiAllSuccessReceived()) {
                            word = new WnnWord(this.mOutputNum, LoggingEvents.EXTRA_CALLING_APP_NAME, LoggingEvents.EXTRA_CALLING_APP_NAME, 16384);
                            this.mOutputNum++;
                            this.mWebAPIOutputNum++;
                            this.mDispWebAPIWords = DEBUG;
                        } else if (this.mWebAPIOutputNum <= 0) {
                            word = new WnnWord(this.mOutputNum, LoggingEvents.EXTRA_CALLING_APP_NAME, LoggingEvents.EXTRA_CALLING_APP_NAME, 2048);
                            this.mOutputNum++;
                            this.mWebAPIOutputNum++;
                        }
                    }
                }
            }
        }
        return word;
    }

    @Override
    public boolean learn(WnnWord word) {
        int index;
        int relation;
        boolean noDictionary = DEBUG;
        boolean breakSequence = DEBUG;
        if (word != null) {
            try {
                index = word.id;
                if ((word.attribute & 64) != 0) {
                    noDictionary = true;
                }
                if (!isEnableLearnNumber()) {
                    Pattern numberPattern = Pattern.compile(".*[0-9０１２３４５６７８９].*");
                    Matcher m = numberPattern.matcher(word.candidate);
                    if (m.matches() && ((this.mLangType != 1 && this.mDictionarySet == 1) || (this.mCore.isGijiDic(word.id) && !word.candidate.equals(this.mSearchKey)))) {
                        noDictionary = true;
                    }
                }
                if (noDictionary) {
                    breakSequence = true;
                }
                if ((word.attribute & 4) != 0) {
                    if (isLowercaseStrokeInLearning()) {
                        word.attribute |= 256;
                    } else {
                        boolean success = this.mCore.noConv(word.stroke);
                        if (!success) {
                            return DEBUG;
                        }
                    }
                    index = -1;
                }
                if ((word.attribute & 128) != 0 || (word.attribute & 32) != 0 || (word.attribute & 256) != 0 || (word.attribute & 1024) != 0 || (word.attribute & 4096) != 0) {
                    if (noDictionary) {
                        this.mCore.init(this.mFilesDirPath);
                        this.mHasBroke = true;
                        return true;
                    }
                    if (this.mHasBroke) {
                        relation = 0;
                    } else {
                        relation = 1;
                    }
                    int ret = this.mCore.addWord(word.stroke, word.candidate, word.lexicalCategory, 1, relation);
                    if (ret < 0) {
                        return DEBUG;
                    }
                    if ((word.attribute & 1024) != 0) {
                        if (this.mSegment < this.mSegmentCount - 1) {
                            this.mHasBroke = breakSequence;
                            return true;
                        }
                        this.mSegment = 0;
                        this.mSegmentCount = 0;
                    }
                    int ret2 = this.mCore.forecast(word.stroke, 0, -1, getEnableHeadConversion());
                    if (ret2 == 0) {
                        return DEBUG;
                    }
                    index = 0;
                    String candidate = this.mCore.getResultString(0, 0);
                    while (candidate != null && !candidate.equals(word.candidate)) {
                        index++;
                        candidate = this.mCore.getResultString(0, index);
                    }
                    noDictionary = true;
                }
            } catch (Exception ex) {
                Log.e("IWnnIME", "iWnnEngine:learn " + ex);
                return DEBUG;
            }
        } else {
            index = -1;
        }
        boolean ret3 = this.mCore.select(this.mSegment, index, !noDictionary ? true : DEBUG, this.mHasBroke) >= 0 ? true : DEBUG;
        this.mHasBroke = breakSequence;
        return ret3;
    }

    public boolean learn(WnnWord word, boolean connected) {
        if (!connected) {
            this.mHasBroke = true;
        }
        return learn(word);
    }

    public boolean learn(boolean learn) {
        boolean ret = DEBUG;
        try {
            int select_result = this.mCore.select(this.mSegment, -1, learn, this.mHasBroke);
            if (select_result < 0) {
                Log.e(TAG, "iWnnEngine::learn(" + learn + ") = " + select_result + "failure");
                ret = DEBUG;
            } else {
                ret = true;
            }
            if (!learn) {
                this.mHasBroke = true;
            } else {
                this.mHasBroke = DEBUG;
            }
        } catch (Exception ex) {
            Log.e("IWnnIME", "iWnnEngine::learn " + ex);
        }
        return ret;
    }

    private WnnWord getWord(int index) {
        String stroke = this.mCore.getWord(index, 0);
        String candidate = this.mCore.getWord(index, 1);
        if (stroke == null || candidate == null) {
            return null;
        }
        return new WnnWord(index, candidate, stroke);
    }

    public int addWord(WnnWord word, int hinsi, int type, int relation) {
        if (word == null) {
            Log.e(TAG, "iWnnEngine::addWord() END parameter error. return = false");
            return -1;
        }
        int ret = this.mCore.addWord(word.stroke, word.candidate, hinsi, type, relation);
        if (ret < 0) {
            Log.e(TAG, "iWnnEngine::addWord() error. ret=" + ret);
            return ret;
        }
        return ret;
    }

    @Override
    public int addWord(WnnWord word) {
        int type;
        int relation;
        if (word == null) {
            Log.e(TAG, "iWnnEngine::addWord() END parameter error. return = false");
            return -1;
        }
        if ((word.attribute & 32768) != 0) {
            type = 1;
        } else {
            type = 0;
        }
        if ((word.attribute & 8192) == 0) {
            relation = 0;
        } else {
            relation = 1;
        }
        return addWord(word, 0, type, relation);
    }

    @Override
    public boolean deleteWord(WnnWord word) {
        int result;
        if (word == null) {
            Log.e(TAG, "iWnnEngine::deleteWord() END parameter error. return = false");
            return DEBUG;
        }
        if ((word.attribute & 2) != 0) {
            result = this.mCore.deleteWord(word.id);
        } else {
            result = this.mCore.deleteSearchWord(word.id);
        }
        if (result >= 0) {
            return true;
        }
        return DEBUG;
    }

    @Override
    public void setPreferences(SharedPreferences pref) {
        Set<String> className = pref.getStringSet(ControlPanelPrefFragment.WEBAPI_KEY, null);
        this.mWebAPIEnableFromSettings = (className == null || className.isEmpty()) ? DEBUG : true;
        this.mWebAPIWnnEngine.setPreferences(pref);
    }

    @Override
    public int makeCandidateListOf(int clausePosition) {
        WnnWord word;
        this.mSegment = clausePosition;
        this.mOutputNum = 0;
        this.mWebAPIOutputNum = 0;
        this.mDispWebAPIWords = DEBUG;
        this.mDispWebAPIButton = true;
        this.mIsForbidDuplication = DEBUG;
        this.mCandTable.clear();
        this.mLatinFilter.clearLatinFilter();
        if (this.mSearchKey == null || (word = getCandidate(0)) == null) {
            return 0;
        }
        Matcher matcher = this.mAllowDuplicationCharPattern.matcher(word.stroke);
        if (!matcher.matches()) {
            this.mIsForbidDuplication = true;
        }
        return 1;
    }

    public boolean writeoutDictionary(int language, int setType) {
        int dicType;
        int currentLanguage = this.mLangType;
        int currentDictionary = this.mDictionarySet;
        switch (setType) {
            case 10:
            case 12:
            case 13:
            case 14:
                dicType = 3;
                break;
            case 11:
                dicType = 2;
                break;
            default:
                return DEBUG;
        }
        setDictionary(language, 0);
        boolean ret = this.mCore.writeoutDictionary(dicType);
        if (!ret) {
            Log.e(TAG, "iWnnEngine::writeoutDictionary() END failed error. return = false");
        }
        setDictionary(currentLanguage, currentDictionary);
        return ret;
    }

    public int setFlexibleCharset(int charset, int keytype) {
        if (this.mLangType == -1) {
            return 0;
        }
        if (charset != 0 && 1 != charset) {
            return 0;
        }
        if (keytype != 0 && 1 != keytype) {
            return 0;
        }
        int ret = this.mCore.setFlexibleCharset(charset, keytype);
        this.mCore.init(this.mFilesDirPath);
        return ret;
    }

    private String convertHalftoFull(String string) {
        char[] chars = string.toCharArray();
        int length = chars.length;
        for (int i = 0; i < length; i++) {
            if ('!' <= chars[i] && chars[i] <= '~') {
                chars[i] = (char) (chars[i] + OFFSET_FULL_WIDTH);
            }
        }
        return new String(chars);
    }

    private String convertFulltoHalf(String string) {
        char[] chars = string.toCharArray();
        int length = chars.length;
        for (int i = 0; i < length; i++) {
            if (65281 <= chars[i] && chars[i] <= 65374) {
                chars[i] = (char) (chars[i] - OFFSET_FULL_WIDTH);
            }
        }
        return new String(chars);
    }

    private WnnWord createCaseGiji(String stroke, boolean init, boolean fullWidth) {
        int attribute;
        int[] listConvert;
        if (!this.mIsRequestGiji) {
            return null;
        }
        String result = null;
        if (init) {
            this.mCaseGijiList = null;
            ArrayList<String> list = new ArrayList<>();
            if (fullWidth) {
                listConvert = CONVERT_TYPE_LIST;
            } else {
                listConvert = CONVERT_TYPE_LIST_HALF;
            }
            int[] arr$ = listConvert;
            for (int cnt : arr$) {
                String resultStr = getgijistr(this.mSearchKey, stroke, cnt);
                if (!list.contains(resultStr) && !this.mCandTable.containsKey(resultStr)) {
                    list.add(resultStr);
                }
            }
            if (list.size() > 0) {
                this.mCaseGijiList = (String[]) list.toArray(new String[list.size()]);
                this.mCaseGijiListIndex = 0;
                result = this.mCaseGijiList[0];
            }
        } else {
            this.mCaseGijiListIndex++;
            if (this.mCaseGijiListIndex < this.mCaseGijiList.length) {
                result = this.mCaseGijiList[this.mCaseGijiListIndex];
            }
        }
        if (result == null) {
            this.mIsRequestGiji = DEBUG;
            return null;
        }
        if (this.mEnableConvertedCandidate) {
            attribute = 32;
        } else {
            attribute = 128;
        }
        return new WnnWord(0, result, this.mSearchKey, attribute);
    }

    public boolean initializeUserDictionary(int language, int setType) {
        if ((setType == 10 || setType == 12 || setType == 13 || setType == 14) && this.mCore.runInitialize(2, language, setType) != -1) {
            return true;
        }
        return DEBUG;
    }

    public boolean createAdditionalDictionary(int setType) {
        return setType < 35 ? DEBUG : this.mCore.createAdditionalDictionary(setType);
    }

    public boolean deleteAdditionalDictionary(int setType) {
        return setType < 35 ? DEBUG : this.mCore.deleteAdditionalDictionary(setType);
    }

    public boolean saveAdditionalDictionary(int setType) {
        return setType < 35 ? DEBUG : this.mCore.saveAdditionalDictionary(setType);
    }

    public boolean createAutoLearningDictionary(int setType) {
        return setType < 45 ? DEBUG : this.mCore.createAutoLearningDictionary(setType);
    }

    public boolean deleteAutoLearningDictionary(int setType) {
        return setType < 45 ? DEBUG : this.mCore.deleteAutoLearningDictionary(setType);
    }

    public boolean saveAutoLearningDictionary(int setType) {
        return setType < 45 ? DEBUG : this.mCore.saveAutoLearningDictionary(setType);
    }

    public boolean initializeLearnDictionary(int language) {
        if (this.mCore.runInitialize(1, language, -1) != -1) {
            return true;
        }
        return DEBUG;
    }

    private void resetExtendedInfo() {
        Context context = IWnnIME.getCurrentIme();
        if (context == null) {
            context = ControlPanelStandard.getCurrentControlPanel();
        }
        if (context == null) {
            Log.e(TAG, "resetExtendedInfo() Fail to get context");
            return;
        }
        String dicVersion = IWnnIME.getStringFromNotResetSettingsPreference(context, DICTIONARY_VERSION_KEY, LoggingEvents.EXTRA_CALLING_APP_NAME);
        if (dicVersion.equals(LoggingEvents.EXTRA_CALLING_APP_NAME)) {
            String[] confFileList = getConfTable();
            for (int i = 0; i < confFileList.length; i++) {
                File confFile = new File(confFileList[i]);
                if (confFile.exists()) {
                    resetExtendedInfo(confFileList[i]);
                }
            }
        }
        SharedPreferences.Editor editor = context.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0).edit();
        editor.putString(DICTIONARY_VERSION_KEY, DICTIONARY_VERSION);
        editor.commit();
    }

    public boolean resetExtendedInfo(String fileName) {
        if (this.mCore.resetExtendedInfo(fileName) != -1) {
            return true;
        }
        return DEBUG;
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

    public boolean undo(int count) {
        return this.mCore.undo(count);
    }

    public boolean isGijiDic(int index) {
        return this.mCore.isGijiDic(index);
    }

    public boolean setGijiFilter(int[] type) {
        return this.mCore.setGijiFilter(type);
    }

    public void setEmojiFilter(boolean enabled) {
        this.mCore.setEmojiFilter(enabled);
    }

    public boolean hasNonSupportCharacters(String str) {
        return this.mCore.hasNonSupportCharacters(str);
    }

    public void setEmailAddressFilter(boolean enabled) {
        this.mCore.setEmailAddressFilter(enabled);
    }

    public void setConvertedCandidateEnabled(boolean enabled) {
        this.mEnableConvertedCandidate = enabled;
    }

    public int getDictionary() {
        return this.mDictionarySet;
    }

    public int getLanguage() {
        return this.mLangType;
    }

    private String stripAlphabetsIfJP(String input) {
        if (this.mLangType == 0) {
            Pattern p = Pattern.compile("^[a-zA-Z]*$");
            if (!p.matcher(input).matches()) {
                Pattern p2 = Pattern.compile("[a-zA-Z]+$");
                return p2.matcher(input).replaceAll(LoggingEvents.EXTRA_CALLING_APP_NAME);
            }
            return input;
        }
        return input;
    }

    private boolean isLowercaseStrokeInLearning() {
        switch (this.mLangType) {
            case 0:
            case 2:
            case 14:
            case 15:
                return DEBUG;
            default:
                if (this.mLangType < LanguageManager.getLanguageListSize()) {
                    return true;
                }
                return DEBUG;
        }
    }

    public void startWebAPIWords() {
        this.mDispWebAPIWords = true;
        this.mDispWebAPIButton = DEBUG;
    }

    public boolean getWebAPIWordsEnabled() {
        return this.mDispWebAPIWords;
    }

    public void startWebAPI(ComposingText text) {
        this.mWebAPIWnnEngine.start(text);
    }

    public void startWebAPIGetAgain(ComposingText text) {
        this.mWebAPIWnnEngine.getAgain(text);
    }

    public void setWebApiCandidates(String yomi, String[] candidates, short[] hinshi) {
        this.mWebAPIWnnEngine.setCandidates(yomi, candidates, hinshi);
    }

    public String getSendingYomiToWebApi() {
        return this.mWebAPIWnnEngine.getSendingYomi();
    }

    public void onDoneGettingCandidates() {
        this.mWebAPIWnnEngine.onDoneGettingCandidates();
    }

    public void setDownloadDictionary() {
        if (!this.mHasLoadedDownloadDictionary && IWnnIME.getCurrentIme() != null) {
            DownloadDictionaryPreference.setDownloadDictionary(IWnnIME.getCurrentIme());
            this.mHasLoadedDownloadDictionary = true;
        }
    }

    public void setDownloadDictionary(int index, String name, String file, int convertHigh, int convertBase, int predictHigh, int predictBase, int morphoHigh, int morphoBase, boolean cache, int limit) {
        this.mCore.setDownloadDictionary(index, name, file, convertHigh, convertBase, predictHigh, predictBase, morphoHigh, morphoBase, cache, limit);
    }

    public boolean checkNameLength(String name, String file) {
        return this.mCore.checkNameLength(name, file);
    }

    public boolean refreshConfFile() {
        return this.mCore.refreshConfFile();
    }

    public void setEnableLearnNumber(boolean enableLearnNumber) {
        this.mEnableLearnNumber = enableLearnNumber;
    }

    private boolean isEnableLearnNumber() {
        if (this.mIsServiceDics) {
            return this.mEnableLearnNumber;
        }
        if (this.mLangType == 0 || this.mLangType == 1) {
            return DEBUG;
        }
        return true;
    }

    public String[] getConfTable() {
        String[] confTable = (String[]) CONF_TABLE.clone();
        if (IWnnIME.isTabletMode()) {
            String[] confTable2 = (String[]) CONF_TABLET_TABLE.clone();
            return confTable2;
        }
        return confTable;
    }

    public void initGijiList() {
        if (this.mCaseGijiList != null) {
            this.mCaseGijiList = null;
            this.mCaseGijiListIndex = 0;
            this.mIsRequestGiji = true;
        }
    }

    @Override
    public boolean isConverting() {
        return this.mIsConverting;
    }

    public void setDecoEmojiFilter(boolean enabled) {
        this.mCore.setDecoEmojiFilter(enabled);
    }

    public void controlDecoEmojiDictionary(String id, String yomi, int hinsi, int control_flag) {
        this.mCore.controlDecoEmojiDictionary(id, yomi, hinsi, control_flag);
    }

    public int checkDecoEmojiDictionary() {
        return this.mCore.checkDecoEmojiDictionary();
    }

    public int resetDecoEmojiDictionary() {
        return this.mCore.resetDecoEmojiDictionary();
    }

    public int checkDecoemojiDicset() {
        return this.mCore.checkDecoemojiDicset();
    }

    public void setWebApiResult(String packageName, boolean success) {
        this.mWebAPIWnnEngine.setWebApiResult(packageName, success);
    }

    public boolean isWebApiAllReceived() {
        return this.mWebAPIWnnEngine.isWebApiAllReceived();
    }

    public boolean isWebApiSuccessReceived() {
        return this.mWebAPIWnnEngine.isWebApiSuccessReceived();
    }

    public boolean isWebApiAllSuccessReceived() {
        return this.mWebAPIWnnEngine.isWebApiAllSuccessReceived();
    }

    @Override
    public int convertGijiStr(ComposingText text, int type) {
        if (text == null) {
            return 0;
        }
        String inputStr = text.toString(1);
        String inputKey = text.toString(0);
        if (inputStr == null || inputKey == null) {
            return 0;
        }
        String resultStr = getgijistr(inputStr, inputKey, type);
        StrSegment[] ss = {new StrSegment(resultStr, 0, inputStr.length() - 1)};
        text.setCursor(2, text.size(2));
        text.replaceStrSegment(2, ss, text.getCursor(2));
        this.mSegmentCount = 1;
        return this.mSegmentCount;
    }

    private String getgijistr(String inputStr, String inputKey, int type) {
        switch (type) {
            case 2:
            case 3:
            case 4:
                String resultStr = getGijiKanaStr(inputStr, inputKey, type);
                return resultStr;
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
                String resultStr2 = getGijiEijiStr(inputKey, type);
                return resultStr2;
            default:
                return null;
        }
    }

    @Override
    public ArrayList<Object> getCategoryList(int mode) {
        ArrayList<Object> ret = new ArrayList<>();
        ret.add(0);
        return ret;
    }

    @Override
    public boolean hasHistory() {
        return DEBUG;
    }

    private String getGijiKanaStr(String inputStr, String inputKey, int type) {
        String tempCandidate;
        StringBuilder compCandidate = new StringBuilder();
        int lenStr = inputStr.length();
        int lenKey = inputKey.length();
        int posKey = 0;
        for (int count = 0; count < lenStr; count++) {
            String tempChar = inputStr.substring(count, count + 1);
            char tc = tempChar.charAt(0);
            int ret = 0;
            if (isHiragana(tc)) {
                ret = this.mCore.getgijistr(tempChar, tempChar.length(), type);
            }
            if (ret <= 0) {
                tempCandidate = tempChar;
                if (isAlphabet(tc)) {
                    if (4 != type) {
                        tempCandidate = convertHalftoFull(tempChar);
                    }
                } else if (4 == type) {
                    while (true) {
                        if (posKey >= lenKey) {
                            break;
                        }
                        if (isAlphabet(inputKey.charAt(posKey))) {
                            posKey++;
                        } else {
                            tempCandidate = convertFulltoHalfKanaSymbol(tc);
                            if (tempCandidate == null) {
                                tempCandidate = inputKey.substring(posKey, posKey + 1);
                            }
                            posKey++;
                        }
                    }
                } else if (this.mLangType != 0) {
                    tempCandidate = convertHalftoFull(tempChar);
                }
            } else {
                tempCandidate = getSegmentString(0);
            }
            compCandidate.append(tempCandidate);
        }
        return compCandidate.toString();
    }

    private String getGijiEijiStr(String inputKey, int type) {
        String candidate;
        if (inputKey == null) {
            return null;
        }
        switch (type) {
            case 5:
                candidate = convertFulltoHalf(Character.toString(toUpperCase(inputKey).charAt(0)) + toLowerCase(inputKey).substring(1));
                break;
            case 6:
                candidate = convertHalftoFull(Character.toString(toUpperCase(inputKey).charAt(0)) + toLowerCase(inputKey).substring(1));
                break;
            case 7:
                candidate = convertFulltoHalf(toUpperCase(inputKey));
                break;
            case 8:
                candidate = convertHalftoFull(toUpperCase(inputKey));
                break;
            case 9:
                candidate = convertFulltoHalf(toLowerCase(inputKey));
                break;
            case 10:
                candidate = convertHalftoFull(toLowerCase(inputKey));
                break;
            default:
                return null;
        }
        return candidate;
    }

    private boolean isHiragana(char checkChar) {
        if (12353 > checkChar || checkChar > 12438) {
            return DEBUG;
        }
        return true;
    }

    private boolean isAlphabet(char checkChar) {
        if (('A' > checkChar || checkChar > 'Z') && ('a' > checkChar || checkChar > 'z')) {
            return DEBUG;
        }
        return true;
    }

    private String convertFulltoHalfKanaSymbol(char convChar) {
        switch (convChar) {
            case 12289:
                String retString = Character.toString((char) 65380);
                return retString;
            case 12290:
                String retString2 = Character.toString((char) 65377);
                return retString2;
            case 12300:
                String retString3 = Character.toString((char) 65378);
                return retString3;
            case 12301:
                String retString4 = Character.toString((char) 65379);
                return retString4;
            case 12539:
                String retString5 = Character.toString((char) 65381);
                return retString5;
            default:
                return null;
        }
    }

    public void setUpdateDownloadDictionary(boolean update) {
        this.mUpdateDownloadDictionary = update;
    }

    public boolean executeOperation(Context context) throws Throwable {
        boolean result = DEBUG;
        if (!this.mIsRegeneratedOperationQueue) {
            this.mOperationQueue.clear();
            regenerationOperationQueue(context);
        }
        DecoEmojiOperation operation = this.mOperationQueue.poll();
        if (operation != null) {
            switch (operation.getType()) {
                case 2:
                    DecoEmojiAttrInfo[] queueInfo = operation.getDecoEmojiAttrInfo();
                    StringBuffer strBuf = new StringBuffer();
                    for (int i = 0; i < queueInfo.length; i++) {
                        strBuf.delete(0, strBuf.length());
                        strBuf.append(String.valueOf(DecoEmojiOperationQueue.ESC_CODE));
                        if (queueInfo[i] != null) {
                            strBuf.append(String.format(DecoEmojiOperationQueue.DECO_ID_FORMAT, Integer.valueOf(queueInfo[i].getId())));
                        } else {
                            strBuf.append(String.format(DecoEmojiOperationQueue.DECO_ID_FORMAT, Integer.valueOf(OPERATION_ID_INIT)));
                        }
                        result = deleteLearnDicDecoEmojiWord(strBuf.toString());
                    }
                    break;
                case 3:
                default:
                    return true;
                case 4:
                    result = deleteLearnDicDecoEmojiWord(String.valueOf(DecoEmojiOperationQueue.ESC_CODE));
                    break;
            }
            if (result) {
                updateOperationProcessedIndexCache(context);
            } else {
                this.mOperationQueue.addFirst(operation);
            }
        }
        return result;
    }

    public boolean deleteLearnDicDecoEmojiWord(String delword) {
        int result = this.mCore.deleteLearnDicDecoEmojiWord(delword);
        if (result < 0) {
            return DEBUG;
        }
        return true;
    }

    public static int getNumEngineForService() {
        return mServiceEngine.size();
    }

    public static iWnnEngine getEngineForService(int index) {
        if (index < 0 || index >= mServiceEngine.size()) {
            return null;
        }
        return mServiceEngine.get(index);
    }

    public void enqueueOperation(DecoEmojiOperation addOperation, Context context) {
        int type = addOperation.getType();
        if (type == 2 || type == 4) {
            if (type == 4) {
                this.mOperationQueue.clear();
            }
            this.mOperationQueue.add(addOperation);
        }
    }

    private void updateOperationProcessedIndexCache(Context context) {
        if (context != null) {
            SharedPreferences pref = context.getSharedPreferences(FILENAME_DECO_OPERATION_PROCESSED_INDEX_CACHE, 0);
            SharedPreferences.Editor editor = pref.edit();
            StringBuffer key = new StringBuffer(KEYNAME_DECO_OPERATION_PROCESSED_INDEX);
            if (this.mPackageName != null) {
                key.append("_");
                key.append(this.mPackageName);
            }
            String keyStr = key.toString();
            long index = pref.getLong(keyStr, 0L);
            editor.putLong(keyStr, index + 1);
            editor.commit();
        }
    }

    private void regenerationOperationQueue(Context context) throws Throwable {
        String event;
        String id;
        if (context != null) {
            SharedPreferences pref = context.getSharedPreferences(FILENAME_DECO_OPERATION_PROCESSED_INDEX_CACHE, 0);
            StringBuffer key = new StringBuffer(KEYNAME_DECO_OPERATION_PROCESSED_INDEX);
            if (this.mPackageName != null) {
                key.append("_");
                key.append(this.mPackageName);
            }
            long eventCount = pref.getLong(KEYNAME_DECO_OPERATION_EVENT_COUNT, 0L);
            long processedIndex = pref.getLong(key.toString(), 0L);
            if (processedIndex < eventCount) {
                BufferedReader reader = null;
                try {
                    try {
                        InputStream in = context.openFileInput(FILENAME_DECO_OPERATION_EVENT_CACHE);
                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(in, IWnnIME.CHARSET_NAME_UTF8));
                        int index = 0;
                        while (true) {
                            if (index < processedIndex) {
                                try {
                                    if (reader2.readLine() == null) {
                                        Log.e(TAG, "iWnnEngine:regenerationOperationQueue() nothing event index[" + index + "] processedIndex[" + processedIndex + "]");
                                        this.mIsRegeneratedOperationQueue = true;
                                        SharedPreferences.Editor editor = pref.edit();
                                        editor.putLong(KEYNAME_DECO_OPERATION_EVENT_COUNT, index);
                                        editor.putLong(key.toString(), index);
                                        editor.commit();
                                        if (reader2 != null) {
                                            try {
                                                reader2.close();
                                            } catch (IOException e) {
                                                Log.e(TAG, "iWnnEngine:regenerationOperationQueue() Exception4[" + e + "]");
                                            }
                                        }
                                    } else {
                                        index++;
                                    }
                                } catch (FileNotFoundException e2) {
                                    e = e2;
                                    reader = reader2;
                                    Log.e(TAG, "iWnnEngine:regenerationOperationQueue() Exception1[" + e + "]");
                                    if (reader != null) {
                                        try {
                                            reader.close();
                                        } catch (IOException e3) {
                                            Log.e(TAG, "iWnnEngine:regenerationOperationQueue() Exception4[" + e3 + "]");
                                        }
                                    }
                                } catch (UnsupportedEncodingException e4) {
                                    e = e4;
                                    reader = reader2;
                                    Log.e(TAG, "iWnnEngine:regenerationOperationQueue() Exception2[" + e + "]");
                                    if (reader != null) {
                                        try {
                                            reader.close();
                                        } catch (IOException e5) {
                                            Log.e(TAG, "iWnnEngine:regenerationOperationQueue() Exception4[" + e5 + "]");
                                        }
                                    }
                                } catch (IOException e6) {
                                    e = e6;
                                    reader = reader2;
                                    Log.e(TAG, "iWnnEngine:regenerationOperationQueue() Exception3[" + e + "]");
                                    if (reader != null) {
                                        try {
                                            reader.close();
                                        } catch (IOException e7) {
                                            Log.e(TAG, "iWnnEngine:regenerationOperationQueue() Exception4[" + e7 + "]");
                                        }
                                    }
                                } catch (Throwable th) {
                                    th = th;
                                    reader = reader2;
                                    if (reader != null) {
                                        try {
                                            reader.close();
                                        } catch (IOException e8) {
                                            Log.e(TAG, "iWnnEngine:regenerationOperationQueue() Exception4[" + e8 + "]");
                                            return;
                                        }
                                    }
                                    throw th;
                                }
                            } else {
                                while (true) {
                                    String readStr = reader2.readLine();
                                    if (readStr == null) {
                                        break;
                                    }
                                    String[] splitStr = readStr.split(DECO_OPERATION_SEPARATOR);
                                    if (splitStr.length >= 2) {
                                        event = splitStr[0];
                                        id = splitStr[1];
                                    } else {
                                        Log.e(TAG, "iWnnEngine:regenerationOperationQueue() incomplete data!!");
                                        event = String.valueOf(2);
                                        id = String.valueOf(OPERATION_ID_INIT);
                                    }
                                    DecoEmojiAttrInfo addDecoemojiattrinfo = new DecoEmojiAttrInfo();
                                    addDecoemojiattrinfo.setID(Integer.valueOf(id).intValue());
                                    this.mOperationQueue.add(new DecoEmojiOperation(addDecoemojiattrinfo, Integer.valueOf(event).intValue(), true));
                                }
                                if (reader2 != null) {
                                    try {
                                        reader2.close();
                                    } catch (IOException e9) {
                                        Log.e(TAG, "iWnnEngine:regenerationOperationQueue() Exception4[" + e9 + "]");
                                    }
                                }
                            }
                        }
                    } catch (Throwable th2) {
                        th = th2;
                    }
                } catch (FileNotFoundException e10) {
                    e = e10;
                } catch (UnsupportedEncodingException e11) {
                    e = e11;
                } catch (IOException e12) {
                    e = e12;
                }
            } else {
                this.mIsRegeneratedOperationQueue = true;
            }
        }
    }

    public void clearOperationQueue() {
        this.mOperationQueue.clear();
        this.mIsRegeneratedOperationQueue = DEBUG;
    }

    public void setEnableHeadConversion(boolean set) {
        this.mEnableHeadConv = set;
    }

    private int getEnableHeadConversion() {
        if (!this.mEnableHeadConv) {
            return 0;
        }
        return 1;
    }

    private void normalizationUserDic() {
        if (!this.mIsNormalizationUserDic) {
            Context con = IWnnIME.getCurrentIme();
            if (con == null) {
                con = ControlPanelStandard.getCurrentControlPanel();
            }
            if (con == null) {
                Log.e(TAG, "normalizationUserDic() Fail to get context");
                return;
            }
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(con);
            boolean wasNormalization = pref.getBoolean(KEY_NORMALIZATION_USER_DIC, DEBUG);
            if (!wasNormalization) {
                this.mIsNormalizationUserDic = true;
                String tempDirPath = this.mFilesDirPath;
                moveFiles(OLD_DIC_PATH, tempDirPath + DIC_DIRCTORY_NAME);
                this.mIsNormalizationUserDic = DEBUG;
                SharedPreferences.Editor editor = pref.edit();
                editor.putBoolean(KEY_NORMALIZATION_USER_DIC, true);
                editor.commit();
                close();
                init(tempDirPath);
            }
        }
    }

    public void moveFiles(String from, String to) {
        File[] fromFiles;
        File fromDir = new File(from);
        if (fromDir.exists() && (fromFiles = fromDir.listFiles()) != null) {
            File toDir = new File(to);
            if (!toDir.exists()) {
                boolean isSuccess = toDir.mkdir();
                if (!isSuccess) {
                    Log.e(TAG, "iWnnEngine:moveFiles() Fail to mkdir destination directory");
                    return;
                }
            }
            for (int i = 0; i < fromFiles.length; i++) {
                if (fromFiles[i].isDirectory()) {
                    moveFiles(fromFiles[i].getPath(), to + "/" + fromFiles[i].getName());
                } else {
                    File toFile = new File(toDir.getPath() + "/" + fromFiles[i].getName());
                    if (toFile.exists()) {
                        boolean isSuccess2 = toFile.delete();
                        if (!isSuccess2) {
                            Log.e(TAG, "iWnnEngine:moveFiles() Fail to delete destination file");
                        } else {
                            boolean isSuccess3 = fromFiles[i].renameTo(toFile);
                            if (!isSuccess3) {
                                Log.e(TAG, "iWnnEngine:moveFiles() Fail to renameTo source file");
                            }
                        }
                    }
                }
            }
            boolean isSuccess4 = fromDir.delete();
            if (!isSuccess4) {
                Log.e(TAG, "iWnnEngine:moveFiles() Fail to delete source directory");
            }
        }
    }

    public void setFilesDirPath(String dirPath) {
        this.mFilesDirPath = dirPath;
    }

    public boolean deleteDictionaryFile(String file) {
        return this.mCore.deleteDictionaryFile(file);
    }

    public static boolean isClearLearningDictionary(String path) throws Throwable {
        FileInputStream fis;
        byte[] dictionaryId;
        boolean result = DEBUG;
        FileInputStream fis2 = null;
        try {
            try {
                fis = new FileInputStream(path);
            } catch (Throwable th) {
                th = th;
            }
            try {
                dictionaryId = new byte[4];
            } catch (FileNotFoundException e) {
                e = e;
                fis2 = fis;
                e.printStackTrace();
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e2) {
                        fis2 = null;
                        e2.printStackTrace();
                    }
                }
            } catch (IOException e3) {
                e = e3;
                fis2 = fis;
                e.printStackTrace();
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e4) {
                        fis2 = null;
                        e4.printStackTrace();
                    }
                }
            } catch (Throwable th2) {
                th = th2;
                fis2 = fis;
                if (fis2 != null) {
                    try {
                        fis2.close();
                    } catch (IOException e5) {
                        e5.printStackTrace();
                    }
                }
                throw th;
            }
        } catch (FileNotFoundException e6) {
            e = e6;
        } catch (IOException e7) {
            e = e7;
        }
        if (fis.read(dictionaryId) != 4) {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e8) {
                    e8.printStackTrace();
                }
            }
            return DEBUG;
        }
        if (Arrays.equals(dictionaryId, DIC_IDENTIFIER_NJDC)) {
            if (fis.skip(4L) != 4) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e9) {
                        e9.printStackTrace();
                    }
                }
                return DEBUG;
            }
            byte[] dictionaryType = new byte[4];
            if (fis.read(dictionaryType) != 4) {
                if (fis != null) {
                    try {
                        fis.close();
                    } catch (IOException e10) {
                        e10.printStackTrace();
                    }
                }
                return DEBUG;
            }
            if (Arrays.equals(dictionaryType, DIC_TYPE_LEARNING) || Arrays.equals(dictionaryType, DIC_TYPE_UNCOMPRESSED_LEARNING)) {
                result = true;
            }
        } else if (Arrays.equals(dictionaryId, DIC_IDENTIFIER_NJEX) || Arrays.equals(dictionaryId, DIC_IDENTIFIER_NJGG)) {
            result = true;
        }
        if (fis != null) {
            try {
                fis.close();
                fis2 = fis;
            } catch (IOException e11) {
                fis2 = null;
                e11.printStackTrace();
            }
        } else {
            fis2 = fis;
        }
        return result;
    }

    public String getForecastKey() {
        return this.mForecastKey;
    }

    public void setNonSupportCharactersFilter(boolean enabled) {
        this.mCore.setNonSupportCharactersFilter(enabled);
    }

    @Override
    public boolean hasCandidate() {
        if (this.mSearchKey == null) {
            return DEBUG;
        }
        WnnWord word = getCandidate(0);
        if (word != null) {
            return true;
        }
        return DEBUG;
    }
}
