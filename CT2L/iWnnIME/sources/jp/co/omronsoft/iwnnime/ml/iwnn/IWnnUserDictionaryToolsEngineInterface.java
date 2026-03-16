package jp.co.omronsoft.iwnnime.ml.iwnn;

import com.android.common.speech.LoggingEvents;
import java.util.ArrayList;
import jp.co.omronsoft.iwnnime.ml.IWnnIME;
import jp.co.omronsoft.iwnnime.ml.WnnWord;

public class IWnnUserDictionaryToolsEngineInterface {
    private static IWnnUserDictionaryToolsEngineInterface sSelf = null;
    private String mFilesDirPath = null;
    protected int mHashCode;
    protected int mLanguage;

    private IWnnUserDictionaryToolsEngineInterface(int language, int hashCode) {
        this.mLanguage = language;
        this.mHashCode = hashCode;
    }

    public void initializeDictionary() {
        iWnnEngine engine = getUserDictionaryEngine();
        engine.initializeUserDictionary(this.mLanguage, 10);
    }

    public boolean deleteWord(WnnWord searchWord) {
        getWords();
        iWnnEngine engine = getUserDictionaryEngine();
        boolean deleted = engine.deleteWord(searchWord);
        if (!deleted) {
            return false;
        }
        engine.writeoutDictionary(this.mLanguage, 10);
        return true;
    }

    public ArrayList<WnnWord> getWords() {
        WnnWord word;
        iWnnEngine engine = getUserDictionaryEngine();
        engine.searchWords(LoggingEvents.EXTRA_CALLING_APP_NAME);
        ArrayList<WnnWord> list = new ArrayList<>();
        for (int i = 0; i < 500 && (word = engine.getNextCandidate()) != null; i++) {
            list.add(word);
        }
        return list;
    }

    public int addWord(WnnWord addWord) {
        iWnnEngine engine = getUserDictionaryEngine();
        if (engine == null) {
            return -1;
        }
        int ret = engine.addWord(addWord);
        if (ret >= 0) {
            engine.writeoutDictionary(this.mLanguage, 10);
            return ret;
        }
        return ret;
    }

    private iWnnEngine getUserDictionaryEngine() {
        iWnnEngine engine = iWnnEngine.getEngine();
        if (IWnnIME.getCurrentIme() == null) {
            engine.init(this.mFilesDirPath);
        }
        engine.setEmojiFilter(false);
        engine.setEmailAddressFilter(false);
        engine.setDictionary(this.mLanguage, 10, this.mHashCode);
        return engine;
    }

    public int getLanguage() {
        return this.mLanguage;
    }

    public static IWnnUserDictionaryToolsEngineInterface getEngineInterface() {
        return sSelf;
    }

    public static IWnnUserDictionaryToolsEngineInterface getEngineInterface(int language, int hashCode) {
        sSelf = new IWnnUserDictionaryToolsEngineInterface(language, hashCode);
        return sSelf;
    }

    public void setDirPath(String dirPath) {
        this.mFilesDirPath = dirPath;
    }
}
