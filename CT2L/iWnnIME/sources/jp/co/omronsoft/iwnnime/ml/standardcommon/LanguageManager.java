package jp.co.omronsoft.iwnnime.ml.standardcommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import jp.co.omronsoft.iwnnime.ml.R;

public class LanguageManager {
    private static final int ENTRY_NAME_ID = 4;
    public static final int LANGUAGE_CATEGORY_HANGUL = 3;
    public static final int LANGUAGE_CATEGORY_JAJP = 0;
    public static final int LANGUAGE_CATEGORY_LATIN = 1;
    public static final int LANGUAGE_CATEGORY_NONE = -1;
    public static final int LANGUAGE_CATEGORY_ZH = 2;
    private static final int LANGUAGE_TYPE = 0;
    private static final int LOCALE = 1;
    private static final int LOCALE_CODE = 3;
    private static final int LOCALE_STRING = 2;
    private static LanguageManager sSelf;
    private static final Locale LOCALE_JA = Locale.JAPANESE;
    private static final Locale LOCALE_EN = Locale.ENGLISH;
    private static final Locale LOCALE_DE = Locale.GERMAN;
    private static final Locale LOCALE_EN_US = Locale.US;
    private static final Locale LOCALE_EN_GB = Locale.UK;
    private static final Locale LOCALE_IT = Locale.ITALIAN;
    private static final Locale LOCALE_FR = Locale.FRENCH;
    private static final Locale LOCALE_ES_ES = new Locale("es", "ES");
    private static final Locale LOCALE_NL_NL = new Locale("nl", "NL");
    private static final Locale LOCALE_PL_PL = new Locale("pl", "PL");
    private static final Locale LOCALE_RU_RU = new Locale("ru", "RU");
    private static final Locale LOCALE_SV_SE = new Locale("sv", "SE");
    private static final Locale LOCALE_NB_NO = new Locale("nb", "NO");
    private static final Locale LOCALE_CS_CZ = new Locale("cs", "CZ");
    private static final Locale LOCALE_ZH_CN = Locale.SIMPLIFIED_CHINESE;
    private static final Locale LOCALE_ZH_TW = Locale.TRADITIONAL_CHINESE;
    private static final Locale LOCALE_PT_PT = new Locale("pt", "PT");
    private static final Locale LOCALE_FR_CA = Locale.CANADA_FRENCH;
    private static final Locale LOCALE_KO_KR = Locale.KOREA;
    private static final Object[][] LANGUAGE_TABLE = {new Object[]{0, LOCALE_JA, LOCALE_JA.toString(), "ja", Integer.valueOf(R.string.ti_choose_language_entry_ja_txt)}, new Object[]{1, LOCALE_EN, LOCALE_EN.toString(), "en", 0}, new Object[]{2, LOCALE_DE, LOCALE_DE.toString(), "de", Integer.valueOf(R.string.ti_choose_language_entry_de_txt)}, new Object[]{3, LOCALE_EN_US, LOCALE_EN_US.toString(), "en_us", Integer.valueOf(R.string.ti_choose_language_entry_en_us_txt)}, new Object[]{4, LOCALE_EN_GB, LOCALE_EN_GB.toString(), "en_uk", Integer.valueOf(R.string.ti_choose_language_entry_en_uk_txt)}, new Object[]{5, LOCALE_IT, LOCALE_IT.toString(), "it", Integer.valueOf(R.string.ti_choose_language_entry_it_txt)}, new Object[]{6, LOCALE_FR, LOCALE_FR.toString(), "fr", Integer.valueOf(R.string.ti_choose_language_entry_fr_txt)}, new Object[]{7, LOCALE_ES_ES, LOCALE_ES_ES.toString(), "es", Integer.valueOf(R.string.ti_choose_language_entry_es_txt)}, new Object[]{8, LOCALE_NL_NL, LOCALE_NL_NL.toString(), "nl", Integer.valueOf(R.string.ti_choose_language_entry_nl_txt)}, new Object[]{9, LOCALE_PL_PL, LOCALE_PL_PL.toString(), "pl", Integer.valueOf(R.string.ti_choose_language_entry_pl_txt)}, new Object[]{10, LOCALE_RU_RU, LOCALE_RU_RU.toString(), "ru", Integer.valueOf(R.string.ti_choose_language_entry_ru_txt)}, new Object[]{11, LOCALE_SV_SE, LOCALE_SV_SE.toString(), "sv", Integer.valueOf(R.string.ti_choose_language_entry_sv_txt)}, new Object[]{12, LOCALE_NB_NO, LOCALE_NB_NO.toString(), "nb", Integer.valueOf(R.string.ti_choose_language_entry_nb_txt)}, new Object[]{13, LOCALE_CS_CZ, LOCALE_CS_CZ.toString(), "cs", Integer.valueOf(R.string.ti_choose_language_entry_cs_txt)}, new Object[]{14, LOCALE_ZH_CN, LOCALE_ZH_CN.toString(), "zh_cn_p", Integer.valueOf(R.string.ti_choose_language_entry_zh_cn_p_txt)}, new Object[]{15, LOCALE_ZH_TW, LOCALE_ZH_TW.toString(), "zh_tw_z", Integer.valueOf(R.string.ti_choose_language_entry_zh_tw_z_txt)}, new Object[]{16, LOCALE_PT_PT, LOCALE_PT_PT.toString(), "pt", Integer.valueOf(R.string.ti_choose_language_entry_pt_txt)}, new Object[]{17, LOCALE_FR_CA, LOCALE_FR_CA.toString(), "fr_ca", Integer.valueOf(R.string.ti_choose_language_entry_fr_ca_txt)}, new Object[]{18, LOCALE_KO_KR, LOCALE_KO_KR.toString(), "ko", Integer.valueOf(R.string.ti_choose_language_entry_ko_txt)}};
    private static final HashMap<Integer, Integer> USER_DICTIONARY_TITLE_MAP = new HashMap<Integer, Integer>() {
        {
            put(0, Integer.valueOf(R.string.ti_preference_dictionary_menu_ja_txt));
            put(2, Integer.valueOf(R.string.ti_preference_dictionary_menu_de_txt));
            put(10, Integer.valueOf(R.string.ti_preference_dictionary_menu_ru_txt));
            put(14, Integer.valueOf(R.string.ti_preference_dictionary_menu_zhcn_txt));
            put(15, Integer.valueOf(R.string.ti_preference_dictionary_menu_zhtw_txt));
            put(18, Integer.valueOf(R.string.ti_preference_dictionary_menu_ko_txt));
        }
    };
    private static ArrayList<Language> mLanguageList = new ArrayList<>();

    public static final class LanguageType {
        public static final int CANADA_FRENCH = 17;
        public static final int COUNT_OF_LANGUAGETYPE = 19;
        public static final int CZECH = 13;
        public static final int DUTCH = 8;
        public static final int ENGLISH = 1;
        public static final int ENGLISH_UK = 4;
        public static final int ENGLISH_US = 3;
        public static final int FRENCH = 6;
        public static final int GERMAN = 2;
        public static final int ITALIAN = 5;
        public static final int JAPANESE = 0;
        public static final int KOREAN = 18;
        public static final int NONE = -1;
        public static final int NORWEGIAN_BOKMAL = 12;
        public static final int POLISH = 9;
        public static final int PORTUGUESE = 16;
        public static final int RUSSIAN = 10;
        public static final int SIMPLIFIED_CHINESE = 14;
        public static final int SPANISH = 7;
        public static final int SWEDISH = 11;
        public static final int TRADITIONAL_CHINESE = 15;
    }

    private static class Language {
        Locale locale;
        String localeCode;
        String localeString;
        int nameId;
        int type;

        private Language(int type, Locale locale, String localeString, String localeCode, int nameId) {
            this.type = type;
            this.locale = locale;
            this.localeString = localeString;
            this.localeCode = localeCode;
            this.nameId = nameId;
        }

        public int type() {
            return this.type;
        }

        public Locale locale() {
            return this.locale;
        }

        public String localeString() {
            return this.localeString;
        }

        public String localeCode() {
            return this.localeCode;
        }

        public int nameId() {
            return this.nameId;
        }
    }

    static {
        resetLanguageList();
        sSelf = new LanguageManager();
    }

    public static LanguageManager getInstance() {
        return sSelf;
    }

    public static int getChosenLanguageType(String localeCode) {
        for (int i = 0; i < mLanguageList.size(); i++) {
            Language language = mLanguageList.get(i);
            if (localeCode.equals(language.localeCode())) {
                return language.type();
            }
        }
        return -1;
    }

    public static int getChosenLanguageType(Locale locale) {
        for (int i = 0; i < mLanguageList.size(); i++) {
            Language language = mLanguageList.get(i);
            if (locale.equals(language.locale())) {
                return language.type();
            }
        }
        return -1;
    }

    public static String getChosenLocaleCode(Locale locale) {
        for (int i = 0; i < mLanguageList.size(); i++) {
            Language language = mLanguageList.get(i);
            if (locale.equals(language.locale())) {
                return language.localeCode();
            }
        }
        return null;
    }

    public static String getChosenLocaleCode(String localeString) {
        for (int i = 0; i < mLanguageList.size(); i++) {
            Language language = mLanguageList.get(i);
            if (localeString.equals(language.localeString())) {
                return language.localeCode();
            }
        }
        return null;
    }

    public static Locale getChosenLocale(int languageType) {
        return (languageType <= -1 || languageType >= mLanguageList.size()) ? Locale.getDefault() : mLanguageList.get(languageType).locale();
    }

    public static boolean isNoStroke(int languageType) {
        switch (languageType) {
            case 0:
            case 1:
            case 14:
            case 15:
            case 18:
                return false;
            default:
                return true;
        }
    }

    public static boolean isChineseLanguage(int languageType) {
        switch (languageType) {
            case 14:
            case 15:
                return true;
            default:
                return false;
        }
    }

    public static boolean isSimplifiedChineseLanguage(int languageType) {
        switch (languageType) {
            case 14:
                return true;
            default:
                return false;
        }
    }

    public static boolean isKoreanLanguage(int languageType) {
        switch (languageType) {
            case 18:
                return true;
            default:
                return false;
        }
    }

    public static boolean is3TypekeyboardLanguage(int languageType) {
        switch (languageType) {
            case 10:
            case 14:
            case 15:
            case 18:
                return true;
            case 11:
            case 12:
            case 13:
            case 16:
            case 17:
            default:
                return false;
        }
    }

    public static int getUserDictionaryTitle(int languageType) {
        Integer nameId = USER_DICTIONARY_TITLE_MAP.get(Integer.valueOf(languageType));
        if (nameId == null) {
            nameId = Integer.valueOf(R.string.ti_preference_dictionary_menu_en_txt);
        }
        return nameId.intValue();
    }

    public static int getNameId(String localeString) {
        int nameId = -1;
        for (int i = 0; i < mLanguageList.size(); i++) {
            Language language = mLanguageList.get(i);
            if (localeString.equals(language.localeString())) {
                nameId = language.nameId();
            }
        }
        return nameId;
    }

    public static void resetLanguageList() {
        mLanguageList.clear();
        for (int i = 0; i < LANGUAGE_TABLE.length; i++) {
            mLanguageList.add(new Language(((Integer) LANGUAGE_TABLE[i][0]).intValue(), (Locale) LANGUAGE_TABLE[i][1], (String) LANGUAGE_TABLE[i][2], (String) LANGUAGE_TABLE[i][3], ((Integer) LANGUAGE_TABLE[i][4]).intValue()));
        }
    }

    public static void addLanguageList(String localeString) {
        int i = 0;
        Locale locale = Locale.getDefault();
        String[] array = localeString.split("_");
        int length = array.length;
        switch (length) {
            case 1:
                locale = new Locale(array[0]);
                break;
            case 2:
                locale = new Locale(array[0], array[1]);
                break;
            case 3:
                locale = new Locale(array[0], array[1], array[2]);
                break;
        }
        int type = mLanguageList.size();
        mLanguageList.add(new Language(type, locale, locale.toString(), locale.toString().toLowerCase(), i));
    }

    public static int getLanguageListSize() {
        return mLanguageList.size();
    }
}
