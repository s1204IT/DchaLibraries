package jp.co.omronsoft.iwnnime.ml.iwnn;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import jp.co.omronsoft.iwnnime.ml.DecoEmojiListener;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;

public class IWnnCore {
    private static final boolean DEBUG = false;
    public static final int HEAD_CONVERSION_OFF = 0;
    public static final int HEAD_CONVERSION_ON = 1;
    private static final String IWNN_LIBRARY_DATAPATH = "/system/lib/";
    public static final int LEARN_CONNECT = 128;
    public static final int LEARN_ENABLE = 1;
    public static final int RELATIONAL_LEARNING_OFF = 0;
    public static final int RELATIONAL_LEARNING_ON = 1;
    private static final String TAG = "iWnn";
    private static boolean sHasLibrary;
    private int mIwnnInfo;
    private IWnnSituationManager mSituationManager;

    public static final class DictionaryType {
        public static final int DICTIONARY_TYPE_LEARNING = 2;
        public static final int DICTIONARY_TYPE_USER = 3;
    }

    public static final class Hinshi {
        public static final int CHIMEI = 2;
        public static final int JINMEI = 1;
        public static final int KIGOU = 3;
        public static final int MEISI = 0;
        public static final int MEISI_NO_CONJ = 2;
    }

    static {
        sHasLibrary = DEBUG;
        sHasLibrary = true;
        try {
            System.load("/system/lib/libiwnn.so");
        } catch (UnsatisfiedLinkError e) {
            try {
                System.loadLibrary("iwnn");
            } catch (UnsatisfiedLinkError e2) {
                sHasLibrary = DEBUG;
            }
        }
    }

    public IWnnCore() {
        this.mSituationManager = null;
        try {
            this.mIwnnInfo = IWnnNative.getInfo();
            this.mSituationManager = new IWnnSituationManager(this);
        } catch (Exception ex) {
            Log.e(TAG, "WARNING: " + ex.toString());
        } catch (UnsatisfiedLinkError e) {
            this.mIwnnInfo = 0;
        }
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (this.mIwnnInfo != 0) {
            IWnnNative.destroy(this.mIwnnInfo);
            this.mIwnnInfo = 0;
        }
    }

    public void destroyWnnInfo() {
        if (this.mIwnnInfo != 0) {
            IWnnNative.destroy(this.mIwnnInfo);
            this.mIwnnInfo = 0;
        }
    }

    public boolean setDictionary(int language, int dictionary, String confFilePath, boolean change, String dirPath) {
        if (change) {
            int result = IWnnNative.setdicByConf(this.mIwnnInfo, confFilePath, language);
            if (result <= 0) {
                Context context = IWnnIME.getContext();
                if (context != null) {
                    SharedPreferences pref = context.getSharedPreferences(IWnnIME.FILENAME_NOT_RESET_SETTINGS_PREFERENCE, 0);
                    SharedPreferences.Editor editor = pref.edit();
                    editor.putInt(DecoEmojiListener.PREF_KEY, -1);
                    editor.commit();
                }
                if (result == 0) {
                    return DEBUG;
                }
            }
            if (IWnnNative.setActiveLang(this.mIwnnInfo, language) == 0) {
                return DEBUG;
            }
        }
        if (IWnnNative.setBookshelf(this.mIwnnInfo, dictionary) <= 0) {
            return DEBUG;
        }
        if (change) {
            init(dirPath);
        }
        return true;
    }

    public int unmountDictionary() {
        return IWnnNative.unmountDics(this.mIwnnInfo);
    }

    public int setLanguage(int lang) {
        return IWnnNative.setActiveLang(this.mIwnnInfo, lang);
    }

    public int setDictionary(String conf_file, int lang) {
        return IWnnNative.setdicByConf(this.mIwnnInfo, conf_file, lang);
    }

    public int init(String dirPath) {
        int result = IWnnNative.init(this.mIwnnInfo, dirPath);
        this.mSituationManager.updateState();
        return result;
    }

    public boolean pullSituationState() {
        int result = IWnnNative.getState(this.mIwnnInfo);
        if (result < 0) {
            return DEBUG;
        }
        return true;
    }

    public boolean pushSituationState() {
        int result = IWnnNative.setState(this.mIwnnInfo);
        if (result < 0) {
            return DEBUG;
        }
        return true;
    }

    public void setSituationBiasValue(int situation, int bias) {
        IWnnNative.setStateSystem(this.mIwnnInfo, situation, bias);
    }

    public int forecast(String input) {
        if (IWnnNative.setInput(this.mIwnnInfo, input) < 0) {
            return 0;
        }
        return IWnnNative.forecast(this.mIwnnInfo, 0, -1, 1);
    }

    public int forecast(String input, int minLen, int maxLen, int headConv) {
        if (IWnnNative.setInput(this.mIwnnInfo, input) < 0) {
            return 0;
        }
        return IWnnNative.forecast(this.mIwnnInfo, minLen, maxLen, headConv);
    }

    public int conv(String input, int divide_pos) {
        if (IWnnNative.setInput(this.mIwnnInfo, input) < 0) {
            return 0;
        }
        return IWnnNative.conv(this.mIwnnInfo, divide_pos);
    }

    public boolean noConv(String input) {
        if (IWnnNative.setInput(this.mIwnnInfo, input) >= 0 && IWnnNative.noconv(this.mIwnnInfo) != 0) {
            return true;
        }
        return DEBUG;
    }

    public int select(int segment, int cand, boolean learn, boolean connected) {
        int ret = IWnnNative.select(this.mIwnnInfo, segment, cand, (connected ? 0 : 128) | (learn ? 1 : 0));
        return ret;
    }

    public int searchWord(int method, int order, String input) {
        if (IWnnNative.setInput(this.mIwnnInfo, input) < 0) {
            return 0;
        }
        return IWnnNative.searchWord(this.mIwnnInfo, method, order);
    }

    public String getWord(int index, int type) {
        return IWnnNative.getWord(this.mIwnnInfo, index, type);
    }

    public int addWord(String yomi, String repr, int group, int dtype, int con) {
        return IWnnNative.addWord(this.mIwnnInfo, yomi, repr, group, dtype, con);
    }

    public int deleteSearchWord(int index) {
        return IWnnNative.deleteSearchWord(this.mIwnnInfo, index);
    }

    public int deleteWord(int index) {
        return IWnnNative.deleteWord(this.mIwnnInfo, index);
    }

    public String getResultString(int segment, int cand) {
        String ret = IWnnNative.getWordString(this.mIwnnInfo, segment, cand);
        return ret;
    }

    public String getResultStroke(int segment, int cand) {
        String ret = IWnnNative.getWordStroke(this.mIwnnInfo, segment, cand);
        return ret;
    }

    public String getSegmentStroke(int segment) {
        String ret = IWnnNative.getSegmentStroke(this.mIwnnInfo, segment);
        return ret;
    }

    public String getSegmentString(int segment) {
        String ret = IWnnNative.getSegmentString(this.mIwnnInfo, segment);
        return ret;
    }

    public boolean writeoutDictionary(int type) {
        if (type != 2 && type != 3) {
            return DEBUG;
        }
        int result = IWnnNative.WriteOutDictionary(this.mIwnnInfo, type);
        if (result >= 0) {
            return true;
        }
        return DEBUG;
    }

    public int runInitialize(int type, int language, int dictionary) {
        if (type == 1 || type == 2) {
            return IWnnNative.deleteDictionary(this.mIwnnInfo, type, language, dictionary);
        }
        return -1;
    }

    public int resetExtendedInfo(String fileName) {
        return IWnnNative.resetExtendedInfo(this.mIwnnInfo, fileName);
    }

    public int setFlexibleCharset(int charset, int keytype) {
        return IWnnNative.setFlexibleCharset(this.mIwnnInfo, charset, keytype);
    }

    public boolean isLearnDictionary(int index) {
        int result = IWnnNative.isLearnDictionary(this.mIwnnInfo, index);
        if (result == 0) {
            return DEBUG;
        }
        return true;
    }

    public boolean isGijiDic(int index) {
        int result = IWnnNative.isGijiResult(this.mIwnnInfo, index);
        if (result <= 0) {
            return DEBUG;
        }
        return true;
    }

    public boolean undo(int count) {
        int result = IWnnNative.undo(this.mIwnnInfo, count);
        if (result < 0) {
            return DEBUG;
        }
        return true;
    }

    public boolean hasNonSupportCharacters(String str) {
        int ret = IWnnNative.hasNonSupportCharacters(this.mIwnnInfo, str);
        if (ret == 1) {
            return DEBUG;
        }
        return true;
    }

    public void setEmojiFilter(boolean enabled) {
        int filter;
        if (enabled) {
            filter = 1;
        } else {
            filter = 0;
        }
        IWnnNative.emojiFilter(this.mIwnnInfo, filter);
    }

    public void setEmailAddressFilter(boolean enabled) {
        int filter;
        if (enabled) {
            filter = 1;
        } else {
            filter = 0;
        }
        IWnnNative.emailAddressFilter(this.mIwnnInfo, filter);
    }

    public void splitWord(String input, int[] result) {
        if (result.length >= 2) {
            result[0] = 0;
            result[1] = 0;
            IWnnNative.splitWord(this.mIwnnInfo, input, result);
        }
    }

    public void getMorphemeText(int index, String[][] getText) {
        if (index >= 0 && getText.length >= 1 && getText[0].length == 2) {
            getText[0][0] = null;
            getText[0][1] = null;
            IWnnNative.getMorphemeWord(this.mIwnnInfo, index, getText[0]);
            int yomiIndex = index;
            for (int i = 1; i < getText.length; i++) {
                getText[i][0] = null;
                getText[i][1] = null;
                IWnnNative.getMorphemeYomi(this.mIwnnInfo, yomiIndex, getText[i]);
                yomiIndex = -1;
            }
        }
    }

    public short getMorphemePartOfSpeech(int index) {
        return IWnnNative.getMorphemeHinsi(this.mIwnnInfo, index);
    }

    public boolean deleteAdditionalDictionary(int setType) {
        if (IWnnNative.deleteAdditionalDictionary(this.mIwnnInfo, setType) == 0) {
            return true;
        }
        return DEBUG;
    }

    public boolean createAdditionalDictionary(int setType) {
        if (IWnnNative.createAdditionalDictionary(this.mIwnnInfo, setType) == 0) {
            return true;
        }
        return DEBUG;
    }

    public boolean saveAdditionalDictionary(int setType) {
        if (IWnnNative.saveAdditionalDictionary(this.mIwnnInfo, setType) == 0) {
            return true;
        }
        return DEBUG;
    }

    public boolean deleteAutoLearningDictionary(int setType) {
        if (IWnnNative.deleteAutoLearningDictionary(this.mIwnnInfo, setType) == 0) {
            return true;
        }
        return DEBUG;
    }

    public boolean createAutoLearningDictionary(int setType) {
        if (IWnnNative.createAutoLearningDictionary(this.mIwnnInfo, setType) == 0) {
            return true;
        }
        return DEBUG;
    }

    public boolean saveAutoLearningDictionary(int setType) {
        if (IWnnNative.saveAutoLearningDictionary(this.mIwnnInfo, setType) == 0) {
            return true;
        }
        return DEBUG;
    }

    public void setDownloadDictionary(int index, String name, String file, int convertHigh, int convertBase, int predictHigh, int predictBase, int morphoHigh, int morphoBase, boolean cache, int limit) {
        IWnnNative.setDownloadDictionary(this.mIwnnInfo, index, name, file, convertHigh, convertBase, predictHigh, predictBase, morphoHigh, morphoBase, cache, limit);
    }

    public boolean checkNameLength(String name, String file) {
        if (IWnnNative.checkNameLength(name, file) == 0) {
            return true;
        }
        return DEBUG;
    }

    public boolean refreshConfFile() {
        if (IWnnNative.refreshConfFile(this.mIwnnInfo) == 0) {
            return true;
        }
        return DEBUG;
    }

    public void setServicePackageName(String packageName, String password) {
        IWnnNative.setServicePackageName(this.mIwnnInfo, packageName, password);
    }

    public void setDecoEmojiFilter(boolean enabled) {
        int filter;
        if (enabled) {
            filter = 1;
        } else {
            filter = 0;
        }
        IWnnNative.decoemojiFilter(this.mIwnnInfo, filter);
    }

    public void controlDecoEmojiDictionary(String id, String yomi, int hinsi, int control_flag) {
        IWnnNative.controlDecoEmojiDictionary(this.mIwnnInfo, id, yomi, hinsi, control_flag);
    }

    public int checkDecoEmojiDictionary() {
        return IWnnNative.checkDecoEmojiDictionary(this.mIwnnInfo);
    }

    public int resetDecoEmojiDictionary() {
        return IWnnNative.resetDecoEmojiDictionary(this.mIwnnInfo);
    }

    public int checkDecoemojiDicset() {
        return IWnnNative.checkDecoemojiDicset(this.mIwnnInfo);
    }

    public int getgijistr(String input, int divide_pos, int type) {
        if (IWnnNative.setInput(this.mIwnnInfo, input) < 0) {
            return 0;
        }
        return IWnnNative.getgijistr(this.mIwnnInfo, divide_pos, type);
    }

    public int deleteLearnDicDecoEmojiWord(String delword) {
        if (IWnnNative.setInput(this.mIwnnInfo, delword) < 0) {
            return -1;
        }
        return IWnnNative.deleteLearnDicDecoEmojiWord(this.mIwnnInfo);
    }

    public boolean setGijiFilter(int[] type) {
        int result = IWnnNative.setGijiFilter(this.mIwnnInfo, type);
        if (result < 0) {
            return DEBUG;
        }
        return true;
    }

    public boolean deleteDictionaryFile(String file) {
        if (IWnnNative.deleteDictionaryFile(file) == 1) {
            return true;
        }
        return DEBUG;
    }

    public void setNonSupportCharactersFilter(boolean enabled) {
        int filter;
        if (enabled) {
            filter = 1;
        } else {
            filter = 0;
        }
        IWnnNative.nonSupportCharactersFilter(this.mIwnnInfo, filter);
    }

    public static boolean hasLibrary() {
        return sHasLibrary;
    }

    public static boolean hasLibraryForService() {
        return sHasLibrary;
    }
}
