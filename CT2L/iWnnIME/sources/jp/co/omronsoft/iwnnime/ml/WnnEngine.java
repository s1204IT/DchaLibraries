package jp.co.omronsoft.iwnnime.ml;

import android.content.SharedPreferences;
import java.util.ArrayList;

public interface WnnEngine {
    public static final int CATEGORY_RESID_DUMMY = 0;
    public static final int DICTIONARY_TYPE_LEARN = 1;
    public static final int DICTIONARY_TYPE_USER = 2;

    int addWord(WnnWord wnnWord);

    void breakSequence();

    void close();

    int convert(ComposingText composingText);

    int convertGijiStr(ComposingText composingText, int i);

    boolean deleteWord(WnnWord wnnWord);

    ArrayList<Object> getCategoryList(int i);

    WnnWord getNextCandidate();

    WnnWord[] getUserDictionaryWords();

    boolean hasCandidate();

    boolean hasHistory();

    void init(String str);

    boolean initializeDictionary(int i);

    boolean initializeDictionary(int i, int i2);

    boolean isConverting();

    boolean learn(WnnWord wnnWord);

    int makeCandidateListOf(int i);

    int predict(ComposingText composingText, int i, int i2);

    int searchWords(String str);

    void setPreferences(SharedPreferences sharedPreferences);
}
