package jp.co.omronsoft.iwnnime.ml.iwnn;

class IWnnNative {
    public static final native int WriteOutDictionary(int i, int i2);

    public static final native int addWord(int i, String str, String str2, int i2, int i3, int i4);

    public static final native int checkDecoEmojiDictionary(int i);

    public static final native int checkDecoemojiDicset(int i);

    public static final native int checkNameLength(String str, String str2);

    public static final native void controlDecoEmojiDictionary(int i, String str, String str2, int i2, int i3);

    public static final native int conv(int i, int i2);

    public static final native int createAdditionalDictionary(int i, int i2);

    public static final native int createAutoLearningDictionary(int i, int i2);

    public static final native void decoemojiFilter(int i, int i2);

    public static final native int deleteAdditionalDictionary(int i, int i2);

    public static final native int deleteAutoLearningDictionary(int i, int i2);

    public static final native int deleteDictionary(int i, int i2, int i3, int i4);

    public static final native int deleteDictionaryFile(String str);

    public static final native int deleteLearnDicDecoEmojiWord(int i);

    public static final native int deleteSearchWord(int i, int i2);

    public static final native int deleteWord(int i, int i2);

    public static final native void destroy(int i);

    public static final native void emailAddressFilter(int i, int i2);

    public static final native void emojiFilter(int i, int i2);

    public static final native int forecast(int i, int i2, int i3, int i4);

    public static final native int getInfo();

    public static final native String getInput(int i);

    public static final native short getMorphemeHinsi(int i, int i2);

    public static final native void getMorphemeWord(int i, int i2, String[] strArr);

    public static final native void getMorphemeYomi(int i, int i2, String[] strArr);

    public static final native String getSegmentString(int i, int i2);

    public static final native String getSegmentStroke(int i, int i2);

    public static final native int getState(int i);

    public static final native String getWord(int i, int i2, int i3);

    public static final native String getWordString(int i, int i2, int i3);

    public static final native String getWordStroke(int i, int i2, int i3);

    public static final native int getgijistr(int i, int i2, int i3);

    public static final native int hasNonSupportCharacters(int i, String str);

    public static final native int init(int i, String str);

    public static final native int isGijiResult(int i, int i2);

    public static final native int isLearnDictionary(int i, int i2);

    public static final native int noconv(int i);

    public static final native void nonSupportCharactersFilter(int i, int i2);

    public static final native int refreshConfFile(int i);

    public static final native int resetDecoEmojiDictionary(int i);

    public static final native int resetExtendedInfo(int i, String str);

    public static final native int saveAdditionalDictionary(int i, int i2);

    public static final native int saveAutoLearningDictionary(int i, int i2);

    public static final native int searchWord(int i, int i2, int i3);

    public static final native int select(int i, int i2, int i3, int i4);

    public static final native int setActiveLang(int i, int i2);

    public static final native int setBookshelf(int i, int i2);

    public static final native void setDownloadDictionary(int i, int i2, String str, String str2, int i3, int i4, int i5, int i6, int i7, int i8, boolean z, int i9);

    public static final native int setFlexibleCharset(int i, int i2, int i3);

    public static final native int setGijiFilter(int i, int[] iArr);

    public static final native int setInput(int i, String str);

    public static final native int setServicePackageName(int i, String str, String str2);

    public static final native int setState(int i);

    public static final native void setStateSystem(int i, int i2, int i3);

    public static final native int setdicByConf(int i, String str, int i2);

    public static final native void splitWord(int i, String str, int[] iArr);

    public static final native int undo(int i, int i2);

    public static final native int unmountDics(int i);
}
