package java.lang;

import java.io.Serializable;
import java.util.Arrays;

@FindBugsSuppressWarnings({"DM_NUMBER_CTOR"})
public final class Character implements Serializable, Comparable<Character> {
    public static final byte COMBINING_SPACING_MARK = 8;
    public static final byte CONNECTOR_PUNCTUATION = 23;
    public static final byte CONTROL = 15;
    public static final byte CURRENCY_SYMBOL = 26;
    public static final byte DASH_PUNCTUATION = 20;
    public static final byte DECIMAL_DIGIT_NUMBER = 9;
    public static final byte DIRECTIONALITY_ARABIC_NUMBER = 6;
    public static final byte DIRECTIONALITY_BOUNDARY_NEUTRAL = 9;
    public static final byte DIRECTIONALITY_COMMON_NUMBER_SEPARATOR = 7;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER = 3;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_SEPARATOR = 4;
    public static final byte DIRECTIONALITY_EUROPEAN_NUMBER_TERMINATOR = 5;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT = 0;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_EMBEDDING = 14;
    public static final byte DIRECTIONALITY_LEFT_TO_RIGHT_OVERRIDE = 15;
    public static final byte DIRECTIONALITY_NONSPACING_MARK = 8;
    public static final byte DIRECTIONALITY_OTHER_NEUTRALS = 13;
    public static final byte DIRECTIONALITY_PARAGRAPH_SEPARATOR = 10;
    public static final byte DIRECTIONALITY_POP_DIRECTIONAL_FORMAT = 18;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT = 1;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC = 2;
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_EMBEDDING = 16;
    public static final byte DIRECTIONALITY_SEGMENT_SEPARATOR = 11;
    public static final byte DIRECTIONALITY_UNDEFINED = -1;
    public static final byte DIRECTIONALITY_WHITESPACE = 12;
    public static final byte ENCLOSING_MARK = 7;
    public static final byte END_PUNCTUATION = 22;
    public static final byte FINAL_QUOTE_PUNCTUATION = 30;
    public static final byte FORMAT = 16;
    public static final byte INITIAL_QUOTE_PUNCTUATION = 29;
    public static final byte LETTER_NUMBER = 10;
    public static final byte LINE_SEPARATOR = 13;
    public static final byte LOWERCASE_LETTER = 2;
    public static final byte MATH_SYMBOL = 25;
    public static final int MAX_CODE_POINT = 1114111;
    public static final char MAX_HIGH_SURROGATE = 56319;
    public static final char MAX_LOW_SURROGATE = 57343;
    public static final int MAX_RADIX = 36;
    public static final char MAX_SURROGATE = 57343;
    public static final char MAX_VALUE = 65535;
    public static final int MIN_CODE_POINT = 0;
    public static final char MIN_HIGH_SURROGATE = 55296;
    public static final char MIN_LOW_SURROGATE = 56320;
    public static final int MIN_RADIX = 2;
    public static final int MIN_SUPPLEMENTARY_CODE_POINT = 65536;
    public static final char MIN_SURROGATE = 55296;
    public static final char MIN_VALUE = 0;
    public static final byte MODIFIER_LETTER = 4;
    public static final byte MODIFIER_SYMBOL = 27;
    public static final byte NON_SPACING_MARK = 6;
    public static final byte OTHER_LETTER = 5;
    public static final byte OTHER_NUMBER = 11;
    public static final byte OTHER_PUNCTUATION = 24;
    public static final byte OTHER_SYMBOL = 28;
    public static final byte PARAGRAPH_SEPARATOR = 14;
    public static final byte PRIVATE_USE = 18;
    public static final int SIZE = 16;
    public static final byte SPACE_SEPARATOR = 12;
    public static final byte START_PUNCTUATION = 21;
    public static final byte SURROGATE = 19;
    public static final byte TITLECASE_LETTER = 3;
    public static final byte UNASSIGNED = 0;
    public static final byte UPPERCASE_LETTER = 1;
    private static final long serialVersionUID = 3786198910865385080L;
    private final char value;
    public static final Class<Character> TYPE = char[].class.getComponentType();
    public static final byte DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE = 17;
    private static final byte[] DIRECTIONALITY = {0, 1, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15, 2, 16, DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE, 18, 8, 9};
    private static final Character[] SMALL_VALUES = new Character[128];

    private static native int digitImpl(int i, int i2);

    public static native byte getIcuDirectionality(int i);

    private static native String getNameImpl(int i);

    private static native int getNumericValueImpl(int i);

    private static native int getTypeImpl(int i);

    public static native boolean isAlphabetic(int i);

    private static native boolean isDefinedImpl(int i);

    private static native boolean isDigitImpl(int i);

    private static native boolean isIdentifierIgnorableImpl(int i);

    public static native boolean isIdeographic(int i);

    private static native boolean isLetterImpl(int i);

    private static native boolean isLetterOrDigitImpl(int i);

    private static native boolean isLowerCaseImpl(int i);

    private static native boolean isMirroredImpl(int i);

    private static native boolean isSpaceCharImpl(int i);

    private static native boolean isTitleCaseImpl(int i);

    private static native boolean isUnicodeIdentifierPartImpl(int i);

    private static native boolean isUnicodeIdentifierStartImpl(int i);

    private static native boolean isUpperCaseImpl(int i);

    private static native boolean isWhitespaceImpl(int i);

    private static native int toLowerCaseImpl(int i);

    private static native int toTitleCaseImpl(int i);

    private static native int toUpperCaseImpl(int i);

    private static native int unicodeBlockForCodePoint(int i);

    private static native int unicodeBlockForName(String str);

    private static native int unicodeScriptForCodePoint(int i);

    private static native int unicodeScriptForName(String str);

    static {
        for (int i = 0; i < 128; i++) {
            SMALL_VALUES[i] = new Character((char) i);
        }
    }

    public static class Subset {
        private final String name;

        protected Subset(String name) {
            if (name == null) {
                throw new NullPointerException("name == null");
            }
            this.name = name;
        }

        public final boolean equals(Object object) {
            return object == this;
        }

        public final int hashCode() {
            return super.hashCode();
        }

        public final String toString() {
            return this.name;
        }
    }

    public static final class UnicodeBlock extends Subset {

        @Deprecated
        public static final UnicodeBlock SURROGATES_AREA = new UnicodeBlock("SURROGATES_AREA");
        public static final UnicodeBlock BASIC_LATIN = new UnicodeBlock("BASIC_LATIN");
        public static final UnicodeBlock LATIN_1_SUPPLEMENT = new UnicodeBlock("LATIN_1_SUPPLEMENT");
        public static final UnicodeBlock LATIN_EXTENDED_A = new UnicodeBlock("LATIN_EXTENDED_A");
        public static final UnicodeBlock LATIN_EXTENDED_B = new UnicodeBlock("LATIN_EXTENDED_B");
        public static final UnicodeBlock IPA_EXTENSIONS = new UnicodeBlock("IPA_EXTENSIONS");
        public static final UnicodeBlock SPACING_MODIFIER_LETTERS = new UnicodeBlock("SPACING_MODIFIER_LETTERS");
        public static final UnicodeBlock COMBINING_DIACRITICAL_MARKS = new UnicodeBlock("COMBINING_DIACRITICAL_MARKS");
        public static final UnicodeBlock GREEK = new UnicodeBlock("GREEK");
        public static final UnicodeBlock CYRILLIC = new UnicodeBlock("CYRILLIC");
        public static final UnicodeBlock CYRILLIC_SUPPLEMENTARY = new UnicodeBlock("CYRILLIC_SUPPLEMENTARY");
        public static final UnicodeBlock ARMENIAN = new UnicodeBlock("ARMENIAN");
        public static final UnicodeBlock HEBREW = new UnicodeBlock("HEBREW");
        public static final UnicodeBlock ARABIC = new UnicodeBlock("ARABIC");
        public static final UnicodeBlock SYRIAC = new UnicodeBlock("SYRIAC");
        public static final UnicodeBlock THAANA = new UnicodeBlock("THAANA");
        public static final UnicodeBlock DEVANAGARI = new UnicodeBlock("DEVANAGARI");
        public static final UnicodeBlock BENGALI = new UnicodeBlock("BENGALI");
        public static final UnicodeBlock GURMUKHI = new UnicodeBlock("GURMUKHI");
        public static final UnicodeBlock GUJARATI = new UnicodeBlock("GUJARATI");
        public static final UnicodeBlock ORIYA = new UnicodeBlock("ORIYA");
        public static final UnicodeBlock TAMIL = new UnicodeBlock("TAMIL");
        public static final UnicodeBlock TELUGU = new UnicodeBlock("TELUGU");
        public static final UnicodeBlock KANNADA = new UnicodeBlock("KANNADA");
        public static final UnicodeBlock MALAYALAM = new UnicodeBlock("MALAYALAM");
        public static final UnicodeBlock SINHALA = new UnicodeBlock("SINHALA");
        public static final UnicodeBlock THAI = new UnicodeBlock("THAI");
        public static final UnicodeBlock LAO = new UnicodeBlock("LAO");
        public static final UnicodeBlock TIBETAN = new UnicodeBlock("TIBETAN");
        public static final UnicodeBlock MYANMAR = new UnicodeBlock("MYANMAR");
        public static final UnicodeBlock GEORGIAN = new UnicodeBlock("GEORGIAN");
        public static final UnicodeBlock HANGUL_JAMO = new UnicodeBlock("HANGUL_JAMO");
        public static final UnicodeBlock ETHIOPIC = new UnicodeBlock("ETHIOPIC");
        public static final UnicodeBlock CHEROKEE = new UnicodeBlock("CHEROKEE");
        public static final UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS = new UnicodeBlock("UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS");
        public static final UnicodeBlock OGHAM = new UnicodeBlock("OGHAM");
        public static final UnicodeBlock RUNIC = new UnicodeBlock("RUNIC");
        public static final UnicodeBlock TAGALOG = new UnicodeBlock("TAGALOG");
        public static final UnicodeBlock HANUNOO = new UnicodeBlock("HANUNOO");
        public static final UnicodeBlock BUHID = new UnicodeBlock("BUHID");
        public static final UnicodeBlock TAGBANWA = new UnicodeBlock("TAGBANWA");
        public static final UnicodeBlock KHMER = new UnicodeBlock("KHMER");
        public static final UnicodeBlock MONGOLIAN = new UnicodeBlock("MONGOLIAN");
        public static final UnicodeBlock LIMBU = new UnicodeBlock("LIMBU");
        public static final UnicodeBlock TAI_LE = new UnicodeBlock("TAI_LE");
        public static final UnicodeBlock KHMER_SYMBOLS = new UnicodeBlock("KHMER_SYMBOLS");
        public static final UnicodeBlock PHONETIC_EXTENSIONS = new UnicodeBlock("PHONETIC_EXTENSIONS");
        public static final UnicodeBlock LATIN_EXTENDED_ADDITIONAL = new UnicodeBlock("LATIN_EXTENDED_ADDITIONAL");
        public static final UnicodeBlock GREEK_EXTENDED = new UnicodeBlock("GREEK_EXTENDED");
        public static final UnicodeBlock GENERAL_PUNCTUATION = new UnicodeBlock("GENERAL_PUNCTUATION");
        public static final UnicodeBlock SUPERSCRIPTS_AND_SUBSCRIPTS = new UnicodeBlock("SUPERSCRIPTS_AND_SUBSCRIPTS");
        public static final UnicodeBlock CURRENCY_SYMBOLS = new UnicodeBlock("CURRENCY_SYMBOLS");
        public static final UnicodeBlock COMBINING_MARKS_FOR_SYMBOLS = new UnicodeBlock("COMBINING_MARKS_FOR_SYMBOLS");
        public static final UnicodeBlock LETTERLIKE_SYMBOLS = new UnicodeBlock("LETTERLIKE_SYMBOLS");
        public static final UnicodeBlock NUMBER_FORMS = new UnicodeBlock("NUMBER_FORMS");
        public static final UnicodeBlock ARROWS = new UnicodeBlock("ARROWS");
        public static final UnicodeBlock MATHEMATICAL_OPERATORS = new UnicodeBlock("MATHEMATICAL_OPERATORS");
        public static final UnicodeBlock MISCELLANEOUS_TECHNICAL = new UnicodeBlock("MISCELLANEOUS_TECHNICAL");
        public static final UnicodeBlock CONTROL_PICTURES = new UnicodeBlock("CONTROL_PICTURES");
        public static final UnicodeBlock OPTICAL_CHARACTER_RECOGNITION = new UnicodeBlock("OPTICAL_CHARACTER_RECOGNITION");
        public static final UnicodeBlock ENCLOSED_ALPHANUMERICS = new UnicodeBlock("ENCLOSED_ALPHANUMERICS");
        public static final UnicodeBlock BOX_DRAWING = new UnicodeBlock("BOX_DRAWING");
        public static final UnicodeBlock BLOCK_ELEMENTS = new UnicodeBlock("BLOCK_ELEMENTS");
        public static final UnicodeBlock GEOMETRIC_SHAPES = new UnicodeBlock("GEOMETRIC_SHAPES");
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS = new UnicodeBlock("MISCELLANEOUS_SYMBOLS");
        public static final UnicodeBlock DINGBATS = new UnicodeBlock("DINGBATS");
        public static final UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A = new UnicodeBlock("MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A");
        public static final UnicodeBlock SUPPLEMENTAL_ARROWS_A = new UnicodeBlock("SUPPLEMENTAL_ARROWS_A");
        public static final UnicodeBlock BRAILLE_PATTERNS = new UnicodeBlock("BRAILLE_PATTERNS");
        public static final UnicodeBlock SUPPLEMENTAL_ARROWS_B = new UnicodeBlock("SUPPLEMENTAL_ARROWS_B");
        public static final UnicodeBlock MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B = new UnicodeBlock("MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B");
        public static final UnicodeBlock SUPPLEMENTAL_MATHEMATICAL_OPERATORS = new UnicodeBlock("SUPPLEMENTAL_MATHEMATICAL_OPERATORS");
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_ARROWS = new UnicodeBlock("MISCELLANEOUS_SYMBOLS_AND_ARROWS");
        public static final UnicodeBlock CJK_RADICALS_SUPPLEMENT = new UnicodeBlock("CJK_RADICALS_SUPPLEMENT");
        public static final UnicodeBlock KANGXI_RADICALS = new UnicodeBlock("KANGXI_RADICALS");
        public static final UnicodeBlock IDEOGRAPHIC_DESCRIPTION_CHARACTERS = new UnicodeBlock("IDEOGRAPHIC_DESCRIPTION_CHARACTERS");
        public static final UnicodeBlock CJK_SYMBOLS_AND_PUNCTUATION = new UnicodeBlock("CJK_SYMBOLS_AND_PUNCTUATION");
        public static final UnicodeBlock HIRAGANA = new UnicodeBlock("HIRAGANA");
        public static final UnicodeBlock KATAKANA = new UnicodeBlock("KATAKANA");
        public static final UnicodeBlock BOPOMOFO = new UnicodeBlock("BOPOMOFO");
        public static final UnicodeBlock HANGUL_COMPATIBILITY_JAMO = new UnicodeBlock("HANGUL_COMPATIBILITY_JAMO");
        public static final UnicodeBlock KANBUN = new UnicodeBlock("KANBUN");
        public static final UnicodeBlock BOPOMOFO_EXTENDED = new UnicodeBlock("BOPOMOFO_EXTENDED");
        public static final UnicodeBlock KATAKANA_PHONETIC_EXTENSIONS = new UnicodeBlock("KATAKANA_PHONETIC_EXTENSIONS");
        public static final UnicodeBlock ENCLOSED_CJK_LETTERS_AND_MONTHS = new UnicodeBlock("ENCLOSED_CJK_LETTERS_AND_MONTHS");
        public static final UnicodeBlock CJK_COMPATIBILITY = new UnicodeBlock("CJK_COMPATIBILITY");
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A = new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A");
        public static final UnicodeBlock YIJING_HEXAGRAM_SYMBOLS = new UnicodeBlock("YIJING_HEXAGRAM_SYMBOLS");
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS = new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS");
        public static final UnicodeBlock YI_SYLLABLES = new UnicodeBlock("YI_SYLLABLES");
        public static final UnicodeBlock YI_RADICALS = new UnicodeBlock("YI_RADICALS");
        public static final UnicodeBlock HANGUL_SYLLABLES = new UnicodeBlock("HANGUL_SYLLABLES");
        public static final UnicodeBlock HIGH_SURROGATES = new UnicodeBlock("HIGH_SURROGATES");
        public static final UnicodeBlock HIGH_PRIVATE_USE_SURROGATES = new UnicodeBlock("HIGH_PRIVATE_USE_SURROGATES");
        public static final UnicodeBlock LOW_SURROGATES = new UnicodeBlock("LOW_SURROGATES");
        public static final UnicodeBlock PRIVATE_USE_AREA = new UnicodeBlock("PRIVATE_USE_AREA");
        public static final UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS = new UnicodeBlock("CJK_COMPATIBILITY_IDEOGRAPHS");
        public static final UnicodeBlock ALPHABETIC_PRESENTATION_FORMS = new UnicodeBlock("ALPHABETIC_PRESENTATION_FORMS");
        public static final UnicodeBlock ARABIC_PRESENTATION_FORMS_A = new UnicodeBlock("ARABIC_PRESENTATION_FORMS_A");
        public static final UnicodeBlock VARIATION_SELECTORS = new UnicodeBlock("VARIATION_SELECTORS");
        public static final UnicodeBlock COMBINING_HALF_MARKS = new UnicodeBlock("COMBINING_HALF_MARKS");
        public static final UnicodeBlock CJK_COMPATIBILITY_FORMS = new UnicodeBlock("CJK_COMPATIBILITY_FORMS");
        public static final UnicodeBlock SMALL_FORM_VARIANTS = new UnicodeBlock("SMALL_FORM_VARIANTS");
        public static final UnicodeBlock ARABIC_PRESENTATION_FORMS_B = new UnicodeBlock("ARABIC_PRESENTATION_FORMS_B");
        public static final UnicodeBlock HALFWIDTH_AND_FULLWIDTH_FORMS = new UnicodeBlock("HALFWIDTH_AND_FULLWIDTH_FORMS");
        public static final UnicodeBlock SPECIALS = new UnicodeBlock("SPECIALS");
        public static final UnicodeBlock LINEAR_B_SYLLABARY = new UnicodeBlock("LINEAR_B_SYLLABARY");
        public static final UnicodeBlock LINEAR_B_IDEOGRAMS = new UnicodeBlock("LINEAR_B_IDEOGRAMS");
        public static final UnicodeBlock AEGEAN_NUMBERS = new UnicodeBlock("AEGEAN_NUMBERS");
        public static final UnicodeBlock OLD_ITALIC = new UnicodeBlock("OLD_ITALIC");
        public static final UnicodeBlock GOTHIC = new UnicodeBlock("GOTHIC");
        public static final UnicodeBlock UGARITIC = new UnicodeBlock("UGARITIC");
        public static final UnicodeBlock DESERET = new UnicodeBlock("DESERET");
        public static final UnicodeBlock SHAVIAN = new UnicodeBlock("SHAVIAN");
        public static final UnicodeBlock OSMANYA = new UnicodeBlock("OSMANYA");
        public static final UnicodeBlock CYPRIOT_SYLLABARY = new UnicodeBlock("CYPRIOT_SYLLABARY");
        public static final UnicodeBlock BYZANTINE_MUSICAL_SYMBOLS = new UnicodeBlock("BYZANTINE_MUSICAL_SYMBOLS");
        public static final UnicodeBlock MUSICAL_SYMBOLS = new UnicodeBlock("MUSICAL_SYMBOLS");
        public static final UnicodeBlock TAI_XUAN_JING_SYMBOLS = new UnicodeBlock("TAI_XUAN_JING_SYMBOLS");
        public static final UnicodeBlock MATHEMATICAL_ALPHANUMERIC_SYMBOLS = new UnicodeBlock("MATHEMATICAL_ALPHANUMERIC_SYMBOLS");
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B = new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B");
        public static final UnicodeBlock CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT = new UnicodeBlock("CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT");
        public static final UnicodeBlock TAGS = new UnicodeBlock("TAGS");
        public static final UnicodeBlock VARIATION_SELECTORS_SUPPLEMENT = new UnicodeBlock("VARIATION_SELECTORS_SUPPLEMENT");
        public static final UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_A = new UnicodeBlock("SUPPLEMENTARY_PRIVATE_USE_AREA_A");
        public static final UnicodeBlock SUPPLEMENTARY_PRIVATE_USE_AREA_B = new UnicodeBlock("SUPPLEMENTARY_PRIVATE_USE_AREA_B");
        public static final UnicodeBlock ANCIENT_GREEK_MUSICAL_NOTATION = new UnicodeBlock("ANCIENT_GREEK_MUSICAL_NOTATION");
        public static final UnicodeBlock ANCIENT_GREEK_NUMBERS = new UnicodeBlock("ANCIENT_GREEK_NUMBERS");
        public static final UnicodeBlock ARABIC_SUPPLEMENT = new UnicodeBlock("ARABIC_SUPPLEMENT");
        public static final UnicodeBlock BUGINESE = new UnicodeBlock("BUGINESE");
        public static final UnicodeBlock CJK_STROKES = new UnicodeBlock("CJK_STROKES");
        public static final UnicodeBlock COMBINING_DIACRITICAL_MARKS_SUPPLEMENT = new UnicodeBlock("COMBINING_DIACRITICAL_MARKS_SUPPLEMENT");
        public static final UnicodeBlock COPTIC = new UnicodeBlock("COPTIC");
        public static final UnicodeBlock ETHIOPIC_EXTENDED = new UnicodeBlock("ETHIOPIC_EXTENDED");
        public static final UnicodeBlock ETHIOPIC_SUPPLEMENT = new UnicodeBlock("ETHIOPIC_SUPPLEMENT");
        public static final UnicodeBlock GEORGIAN_SUPPLEMENT = new UnicodeBlock("GEORGIAN_SUPPLEMENT");
        public static final UnicodeBlock GLAGOLITIC = new UnicodeBlock("GLAGOLITIC");
        public static final UnicodeBlock KHAROSHTHI = new UnicodeBlock("KHAROSHTHI");
        public static final UnicodeBlock MODIFIER_TONE_LETTERS = new UnicodeBlock("MODIFIER_TONE_LETTERS");
        public static final UnicodeBlock NEW_TAI_LUE = new UnicodeBlock("NEW_TAI_LUE");
        public static final UnicodeBlock OLD_PERSIAN = new UnicodeBlock("OLD_PERSIAN");
        public static final UnicodeBlock PHONETIC_EXTENSIONS_SUPPLEMENT = new UnicodeBlock("PHONETIC_EXTENSIONS_SUPPLEMENT");
        public static final UnicodeBlock SUPPLEMENTAL_PUNCTUATION = new UnicodeBlock("SUPPLEMENTAL_PUNCTUATION");
        public static final UnicodeBlock SYLOTI_NAGRI = new UnicodeBlock("SYLOTI_NAGRI");
        public static final UnicodeBlock TIFINAGH = new UnicodeBlock("TIFINAGH");
        public static final UnicodeBlock VERTICAL_FORMS = new UnicodeBlock("VERTICAL_FORMS");
        public static final UnicodeBlock NKO = new UnicodeBlock("NKO");
        public static final UnicodeBlock BALINESE = new UnicodeBlock("BALINESE");
        public static final UnicodeBlock LATIN_EXTENDED_C = new UnicodeBlock("LATIN_EXTENDED_C");
        public static final UnicodeBlock LATIN_EXTENDED_D = new UnicodeBlock("LATIN_EXTENDED_D");
        public static final UnicodeBlock PHAGS_PA = new UnicodeBlock("PHAGS_PA");
        public static final UnicodeBlock PHOENICIAN = new UnicodeBlock("PHOENICIAN");
        public static final UnicodeBlock CUNEIFORM = new UnicodeBlock("CUNEIFORM");
        public static final UnicodeBlock CUNEIFORM_NUMBERS_AND_PUNCTUATION = new UnicodeBlock("CUNEIFORM_NUMBERS_AND_PUNCTUATION");
        public static final UnicodeBlock COUNTING_ROD_NUMERALS = new UnicodeBlock("COUNTING_ROD_NUMERALS");
        public static final UnicodeBlock SUNDANESE = new UnicodeBlock("SUNDANESE");
        public static final UnicodeBlock LEPCHA = new UnicodeBlock("LEPCHA");
        public static final UnicodeBlock OL_CHIKI = new UnicodeBlock("OL_CHIKI");
        public static final UnicodeBlock CYRILLIC_EXTENDED_A = new UnicodeBlock("CYRILLIC_EXTENDED_A");
        public static final UnicodeBlock VAI = new UnicodeBlock("VAI");
        public static final UnicodeBlock CYRILLIC_EXTENDED_B = new UnicodeBlock("CYRILLIC_EXTENDED_B");
        public static final UnicodeBlock SAURASHTRA = new UnicodeBlock("SAURASHTRA");
        public static final UnicodeBlock KAYAH_LI = new UnicodeBlock("KAYAH_LI");
        public static final UnicodeBlock REJANG = new UnicodeBlock("REJANG");
        public static final UnicodeBlock CHAM = new UnicodeBlock("CHAM");
        public static final UnicodeBlock ANCIENT_SYMBOLS = new UnicodeBlock("ANCIENT_SYMBOLS");
        public static final UnicodeBlock PHAISTOS_DISC = new UnicodeBlock("PHAISTOS_DISC");
        public static final UnicodeBlock LYCIAN = new UnicodeBlock("LYCIAN");
        public static final UnicodeBlock CARIAN = new UnicodeBlock("CARIAN");
        public static final UnicodeBlock LYDIAN = new UnicodeBlock("LYDIAN");
        public static final UnicodeBlock MAHJONG_TILES = new UnicodeBlock("MAHJONG_TILES");
        public static final UnicodeBlock DOMINO_TILES = new UnicodeBlock("DOMINO_TILES");
        public static final UnicodeBlock SAMARITAN = new UnicodeBlock("SAMARITAN");
        public static final UnicodeBlock UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED = new UnicodeBlock("UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED");
        public static final UnicodeBlock TAI_THAM = new UnicodeBlock("TAI_THAM");
        public static final UnicodeBlock VEDIC_EXTENSIONS = new UnicodeBlock("VEDIC_EXTENSIONS");
        public static final UnicodeBlock LISU = new UnicodeBlock("LISU");
        public static final UnicodeBlock BAMUM = new UnicodeBlock("BAMUM");
        public static final UnicodeBlock COMMON_INDIC_NUMBER_FORMS = new UnicodeBlock("COMMON_INDIC_NUMBER_FORMS");
        public static final UnicodeBlock DEVANAGARI_EXTENDED = new UnicodeBlock("DEVANAGARI_EXTENDED");
        public static final UnicodeBlock HANGUL_JAMO_EXTENDED_A = new UnicodeBlock("HANGUL_JAMO_EXTENDED_A");
        public static final UnicodeBlock JAVANESE = new UnicodeBlock("JAVANESE");
        public static final UnicodeBlock MYANMAR_EXTENDED_A = new UnicodeBlock("MYANMAR_EXTENDED_A");
        public static final UnicodeBlock TAI_VIET = new UnicodeBlock("TAI_VIET");
        public static final UnicodeBlock MEETEI_MAYEK = new UnicodeBlock("MEETEI_MAYEK");
        public static final UnicodeBlock HANGUL_JAMO_EXTENDED_B = new UnicodeBlock("HANGUL_JAMO_EXTENDED_B");
        public static final UnicodeBlock IMPERIAL_ARAMAIC = new UnicodeBlock("IMPERIAL_ARAMAIC");
        public static final UnicodeBlock OLD_SOUTH_ARABIAN = new UnicodeBlock("OLD_SOUTH_ARABIAN");
        public static final UnicodeBlock AVESTAN = new UnicodeBlock("AVESTAN");
        public static final UnicodeBlock INSCRIPTIONAL_PARTHIAN = new UnicodeBlock("INSCRIPTIONAL_PARTHIAN");
        public static final UnicodeBlock INSCRIPTIONAL_PAHLAVI = new UnicodeBlock("INSCRIPTIONAL_PAHLAVI");
        public static final UnicodeBlock OLD_TURKIC = new UnicodeBlock("OLD_TURKIC");
        public static final UnicodeBlock RUMI_NUMERAL_SYMBOLS = new UnicodeBlock("RUMI_NUMERAL_SYMBOLS");
        public static final UnicodeBlock KAITHI = new UnicodeBlock("KAITHI");
        public static final UnicodeBlock EGYPTIAN_HIEROGLYPHS = new UnicodeBlock("EGYPTIAN_HIEROGLYPHS");
        public static final UnicodeBlock ENCLOSED_ALPHANUMERIC_SUPPLEMENT = new UnicodeBlock("ENCLOSED_ALPHANUMERIC_SUPPLEMENT");
        public static final UnicodeBlock ENCLOSED_IDEOGRAPHIC_SUPPLEMENT = new UnicodeBlock("ENCLOSED_IDEOGRAPHIC_SUPPLEMENT");
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C = new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C");
        public static final UnicodeBlock MANDAIC = new UnicodeBlock("MANDAIC");
        public static final UnicodeBlock BATAK = new UnicodeBlock("BATAK");
        public static final UnicodeBlock ETHIOPIC_EXTENDED_A = new UnicodeBlock("ETHIOPIC_EXTENDED_A");
        public static final UnicodeBlock BRAHMI = new UnicodeBlock("BRAHMI");
        public static final UnicodeBlock BAMUM_SUPPLEMENT = new UnicodeBlock("BAMUM_SUPPLEMENT");
        public static final UnicodeBlock KANA_SUPPLEMENT = new UnicodeBlock("KANA_SUPPLEMENT");
        public static final UnicodeBlock PLAYING_CARDS = new UnicodeBlock("PLAYING_CARDS");
        public static final UnicodeBlock MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS = new UnicodeBlock("MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS");
        public static final UnicodeBlock EMOTICONS = new UnicodeBlock("EMOTICONS");
        public static final UnicodeBlock TRANSPORT_AND_MAP_SYMBOLS = new UnicodeBlock("TRANSPORT_AND_MAP_SYMBOLS");
        public static final UnicodeBlock ALCHEMICAL_SYMBOLS = new UnicodeBlock("ALCHEMICAL_SYMBOLS");
        public static final UnicodeBlock CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D = new UnicodeBlock("CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D");
        private static UnicodeBlock[] BLOCKS = {null, BASIC_LATIN, LATIN_1_SUPPLEMENT, LATIN_EXTENDED_A, LATIN_EXTENDED_B, IPA_EXTENSIONS, SPACING_MODIFIER_LETTERS, COMBINING_DIACRITICAL_MARKS, GREEK, CYRILLIC, ARMENIAN, HEBREW, ARABIC, SYRIAC, THAANA, DEVANAGARI, BENGALI, GURMUKHI, GUJARATI, ORIYA, TAMIL, TELUGU, KANNADA, MALAYALAM, SINHALA, THAI, LAO, TIBETAN, MYANMAR, GEORGIAN, HANGUL_JAMO, ETHIOPIC, CHEROKEE, UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS, OGHAM, RUNIC, KHMER, MONGOLIAN, LATIN_EXTENDED_ADDITIONAL, GREEK_EXTENDED, GENERAL_PUNCTUATION, SUPERSCRIPTS_AND_SUBSCRIPTS, CURRENCY_SYMBOLS, COMBINING_MARKS_FOR_SYMBOLS, LETTERLIKE_SYMBOLS, NUMBER_FORMS, ARROWS, MATHEMATICAL_OPERATORS, MISCELLANEOUS_TECHNICAL, CONTROL_PICTURES, OPTICAL_CHARACTER_RECOGNITION, ENCLOSED_ALPHANUMERICS, BOX_DRAWING, BLOCK_ELEMENTS, GEOMETRIC_SHAPES, MISCELLANEOUS_SYMBOLS, DINGBATS, BRAILLE_PATTERNS, CJK_RADICALS_SUPPLEMENT, KANGXI_RADICALS, IDEOGRAPHIC_DESCRIPTION_CHARACTERS, CJK_SYMBOLS_AND_PUNCTUATION, HIRAGANA, KATAKANA, BOPOMOFO, HANGUL_COMPATIBILITY_JAMO, KANBUN, BOPOMOFO_EXTENDED, ENCLOSED_CJK_LETTERS_AND_MONTHS, CJK_COMPATIBILITY, CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A, CJK_UNIFIED_IDEOGRAPHS, YI_SYLLABLES, YI_RADICALS, HANGUL_SYLLABLES, HIGH_SURROGATES, HIGH_PRIVATE_USE_SURROGATES, LOW_SURROGATES, PRIVATE_USE_AREA, CJK_COMPATIBILITY_IDEOGRAPHS, ALPHABETIC_PRESENTATION_FORMS, ARABIC_PRESENTATION_FORMS_A, COMBINING_HALF_MARKS, CJK_COMPATIBILITY_FORMS, SMALL_FORM_VARIANTS, ARABIC_PRESENTATION_FORMS_B, SPECIALS, HALFWIDTH_AND_FULLWIDTH_FORMS, OLD_ITALIC, GOTHIC, DESERET, BYZANTINE_MUSICAL_SYMBOLS, MUSICAL_SYMBOLS, MATHEMATICAL_ALPHANUMERIC_SYMBOLS, CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B, CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT, TAGS, CYRILLIC_SUPPLEMENTARY, TAGALOG, HANUNOO, BUHID, TAGBANWA, MISCELLANEOUS_MATHEMATICAL_SYMBOLS_A, SUPPLEMENTAL_ARROWS_A, SUPPLEMENTAL_ARROWS_B, MISCELLANEOUS_MATHEMATICAL_SYMBOLS_B, SUPPLEMENTAL_MATHEMATICAL_OPERATORS, KATAKANA_PHONETIC_EXTENSIONS, VARIATION_SELECTORS, SUPPLEMENTARY_PRIVATE_USE_AREA_A, SUPPLEMENTARY_PRIVATE_USE_AREA_B, LIMBU, TAI_LE, KHMER_SYMBOLS, PHONETIC_EXTENSIONS, MISCELLANEOUS_SYMBOLS_AND_ARROWS, YIJING_HEXAGRAM_SYMBOLS, LINEAR_B_SYLLABARY, LINEAR_B_IDEOGRAMS, AEGEAN_NUMBERS, UGARITIC, SHAVIAN, OSMANYA, CYPRIOT_SYLLABARY, TAI_XUAN_JING_SYMBOLS, VARIATION_SELECTORS_SUPPLEMENT, ANCIENT_GREEK_MUSICAL_NOTATION, ANCIENT_GREEK_NUMBERS, ARABIC_SUPPLEMENT, BUGINESE, CJK_STROKES, COMBINING_DIACRITICAL_MARKS_SUPPLEMENT, COPTIC, ETHIOPIC_EXTENDED, ETHIOPIC_SUPPLEMENT, GEORGIAN_SUPPLEMENT, GLAGOLITIC, KHAROSHTHI, MODIFIER_TONE_LETTERS, NEW_TAI_LUE, OLD_PERSIAN, PHONETIC_EXTENSIONS_SUPPLEMENT, SUPPLEMENTAL_PUNCTUATION, SYLOTI_NAGRI, TIFINAGH, VERTICAL_FORMS, NKO, BALINESE, LATIN_EXTENDED_C, LATIN_EXTENDED_D, PHAGS_PA, PHOENICIAN, CUNEIFORM, CUNEIFORM_NUMBERS_AND_PUNCTUATION, COUNTING_ROD_NUMERALS, SUNDANESE, LEPCHA, OL_CHIKI, CYRILLIC_EXTENDED_A, VAI, CYRILLIC_EXTENDED_B, SAURASHTRA, KAYAH_LI, REJANG, CHAM, ANCIENT_SYMBOLS, PHAISTOS_DISC, LYCIAN, CARIAN, LYDIAN, MAHJONG_TILES, DOMINO_TILES, SAMARITAN, UNIFIED_CANADIAN_ABORIGINAL_SYLLABICS_EXTENDED, TAI_THAM, VEDIC_EXTENSIONS, LISU, BAMUM, COMMON_INDIC_NUMBER_FORMS, DEVANAGARI_EXTENDED, HANGUL_JAMO_EXTENDED_A, JAVANESE, MYANMAR_EXTENDED_A, TAI_VIET, MEETEI_MAYEK, HANGUL_JAMO_EXTENDED_B, IMPERIAL_ARAMAIC, OLD_SOUTH_ARABIAN, AVESTAN, INSCRIPTIONAL_PARTHIAN, INSCRIPTIONAL_PAHLAVI, OLD_TURKIC, RUMI_NUMERAL_SYMBOLS, KAITHI, EGYPTIAN_HIEROGLYPHS, ENCLOSED_ALPHANUMERIC_SUPPLEMENT, ENCLOSED_IDEOGRAPHIC_SUPPLEMENT, CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C, MANDAIC, BATAK, ETHIOPIC_EXTENDED_A, BRAHMI, BAMUM_SUPPLEMENT, KANA_SUPPLEMENT, PLAYING_CARDS, MISCELLANEOUS_SYMBOLS_AND_PICTOGRAPHS, EMOTICONS, TRANSPORT_AND_MAP_SYMBOLS, ALCHEMICAL_SYMBOLS, CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D};

        public static UnicodeBlock forName(String blockName) {
            if (blockName != null) {
                int block = Character.unicodeBlockForName(blockName);
                if (block == -1) {
                    throw new IllegalArgumentException("Unknown block: " + blockName);
                }
                return BLOCKS[block];
            }
            throw new NullPointerException("blockName == null");
        }

        public static UnicodeBlock of(char c) {
            return of((int) c);
        }

        public static UnicodeBlock of(int codePoint) {
            Character.checkValidCodePoint(codePoint);
            int block = Character.unicodeBlockForCodePoint(codePoint);
            if (block == -1 || block >= BLOCKS.length) {
                return null;
            }
            return BLOCKS[block];
        }

        private UnicodeBlock(String blockName) {
            super(blockName);
        }
    }

    public Character(char value) {
        this.value = value;
    }

    public char charValue() {
        return this.value;
    }

    private static void checkValidCodePoint(int codePoint) {
        if (!isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Invalid code point: " + codePoint);
        }
    }

    @Override
    public int compareTo(Character c) {
        return compare(this.value, c.value);
    }

    public static int compare(char lhs, char rhs) {
        return lhs - rhs;
    }

    public static Character valueOf(char c) {
        return c < 128 ? SMALL_VALUES[c] : new Character(c);
    }

    public static boolean isValidCodePoint(int codePoint) {
        return codePoint >= 0 && 1114111 >= codePoint;
    }

    public static boolean isSupplementaryCodePoint(int codePoint) {
        return 65536 <= codePoint && 1114111 >= codePoint;
    }

    public static boolean isHighSurrogate(char ch) {
        return 55296 <= ch && 56319 >= ch;
    }

    public static boolean isLowSurrogate(char ch) {
        return 56320 <= ch && 57343 >= ch;
    }

    public static boolean isSurrogate(char ch) {
        return ch >= 55296 && ch <= 57343;
    }

    public static boolean isSurrogatePair(char high, char low) {
        return isHighSurrogate(high) && isLowSurrogate(low);
    }

    public static int charCount(int codePoint) {
        return codePoint >= 65536 ? 2 : 1;
    }

    public static int toCodePoint(char high, char low) {
        int h = (high & 1023) << 10;
        int l = low & 1023;
        return (h | l) + 65536;
    }

    public static int codePointAt(CharSequence seq, int index) {
        if (seq == null) {
            throw new NullPointerException("seq == null");
        }
        int len = seq.length();
        if (index < 0 || index >= len) {
            throw new IndexOutOfBoundsException();
        }
        int index2 = index + 1;
        char high = seq.charAt(index);
        if (index2 < len) {
            char low = seq.charAt(index2);
            if (isSurrogatePair(high, low)) {
                return toCodePoint(high, low);
            }
            return high;
        }
        return high;
    }

    public static int codePointAt(char[] seq, int index) {
        if (seq == null) {
            throw new NullPointerException("seq == null");
        }
        int len = seq.length;
        if (index < 0 || index >= len) {
            throw new IndexOutOfBoundsException();
        }
        int index2 = index + 1;
        char high = seq[index];
        if (index2 < len) {
            char low = seq[index2];
            if (isSurrogatePair(high, low)) {
                return toCodePoint(high, low);
            }
            return high;
        }
        return high;
    }

    public static int codePointAt(char[] seq, int index, int limit) {
        if (index < 0 || index >= limit || limit < 0 || limit > seq.length) {
            throw new IndexOutOfBoundsException();
        }
        int index2 = index + 1;
        char high = seq[index];
        if (index2 < limit) {
            char low = seq[index2];
            if (isSurrogatePair(high, low)) {
                return toCodePoint(high, low);
            }
            return high;
        }
        return high;
    }

    public static int codePointBefore(CharSequence seq, int index) {
        if (seq == null) {
            throw new NullPointerException("seq == null");
        }
        int len = seq.length();
        if (index < 1 || index > len) {
            throw new IndexOutOfBoundsException();
        }
        int index2 = index - 1;
        char low = seq.charAt(index2);
        int index3 = index2 - 1;
        if (index3 >= 0) {
            char high = seq.charAt(index3);
            if (isSurrogatePair(high, low)) {
                return toCodePoint(high, low);
            }
            return low;
        }
        return low;
    }

    public static int codePointBefore(char[] seq, int index) {
        if (seq == null) {
            throw new NullPointerException("seq == null");
        }
        int len = seq.length;
        if (index < 1 || index > len) {
            throw new IndexOutOfBoundsException();
        }
        int index2 = index - 1;
        char low = seq[index2];
        int index3 = index2 - 1;
        if (index3 >= 0) {
            char high = seq[index3];
            if (isSurrogatePair(high, low)) {
                return toCodePoint(high, low);
            }
            return low;
        }
        return low;
    }

    public static int codePointBefore(char[] seq, int index, int start) {
        if (seq == null) {
            throw new NullPointerException("seq == null");
        }
        int len = seq.length;
        if (index <= start || index > len || start < 0 || start >= len) {
            throw new IndexOutOfBoundsException();
        }
        int index2 = index - 1;
        char low = seq[index2];
        int index3 = index2 - 1;
        if (index3 >= start) {
            char high = seq[index3];
            if (isSurrogatePair(high, low)) {
                return toCodePoint(high, low);
            }
            return low;
        }
        return low;
    }

    public static int toChars(int codePoint, char[] dst, int dstIndex) {
        checkValidCodePoint(codePoint);
        if (dst == null) {
            throw new NullPointerException("dst == null");
        }
        if (dstIndex < 0 || dstIndex >= dst.length) {
            throw new IndexOutOfBoundsException();
        }
        if (isSupplementaryCodePoint(codePoint)) {
            if (dstIndex == dst.length - 1) {
                throw new IndexOutOfBoundsException();
            }
            int cpPrime = codePoint - 65536;
            int high = 55296 | ((cpPrime >> 10) & 1023);
            int low = 56320 | (cpPrime & 1023);
            dst[dstIndex] = (char) high;
            dst[dstIndex + 1] = (char) low;
            return 2;
        }
        dst[dstIndex] = (char) codePoint;
        return 1;
    }

    public static char[] toChars(int codePoint) {
        checkValidCodePoint(codePoint);
        if (!isSupplementaryCodePoint(codePoint)) {
            return new char[]{(char) codePoint};
        }
        int cpPrime = codePoint - 65536;
        int high = 55296 | ((cpPrime >> 10) & 1023);
        int low = 56320 | (cpPrime & 1023);
        return new char[]{(char) high, (char) low};
    }

    public static int codePointCount(CharSequence seq, int beginIndex, int endIndex) {
        if (seq == null) {
            throw new NullPointerException("seq == null");
        }
        int len = seq.length();
        if (beginIndex < 0 || endIndex > len || beginIndex > endIndex) {
            throw new IndexOutOfBoundsException();
        }
        int result = 0;
        int i = beginIndex;
        while (i < endIndex) {
            char c = seq.charAt(i);
            if (isHighSurrogate(c) && (i = i + 1) < endIndex) {
                char c2 = seq.charAt(i);
                if (!isLowSurrogate(c2)) {
                    result++;
                }
            }
            result++;
            i++;
        }
        return result;
    }

    public static int codePointCount(char[] seq, int offset, int count) {
        Arrays.checkOffsetAndCount(seq.length, offset, count);
        int endIndex = offset + count;
        int result = 0;
        int i = offset;
        while (i < endIndex) {
            char c = seq[i];
            if (isHighSurrogate(c) && (i = i + 1) < endIndex) {
                char c2 = seq[i];
                if (!isLowSurrogate(c2)) {
                    result++;
                }
            }
            result++;
            i++;
        }
        return result;
    }

    public static int offsetByCodePoints(CharSequence seq, int index, int codePointOffset) {
        int prev;
        int next;
        if (seq == null) {
            throw new NullPointerException("seq == null");
        }
        int len = seq.length();
        if (index < 0 || index > len) {
            throw new IndexOutOfBoundsException();
        }
        if (codePointOffset == 0) {
            return index;
        }
        if (codePointOffset > 0) {
            int codePoints = codePointOffset;
            int i = index;
            while (codePoints > 0) {
                codePoints--;
                if (i >= len) {
                    throw new IndexOutOfBoundsException();
                }
                if (isHighSurrogate(seq.charAt(i)) && (next = i + 1) < len && isLowSurrogate(seq.charAt(next))) {
                    i++;
                }
                i++;
            }
            return i;
        }
        int codePoints2 = -codePointOffset;
        int i2 = index;
        while (codePoints2 > 0) {
            codePoints2--;
            i2--;
            if (i2 < 0) {
                throw new IndexOutOfBoundsException();
            }
            if (isLowSurrogate(seq.charAt(i2)) && i2 - 1 >= 0 && isHighSurrogate(seq.charAt(prev))) {
                i2--;
            }
        }
        return i2;
    }

    public static int offsetByCodePoints(char[] seq, int start, int count, int index, int codePointOffset) {
        int prev;
        int next;
        Arrays.checkOffsetAndCount(seq.length, start, count);
        int end = start + count;
        if (index < start || index > end) {
            throw new IndexOutOfBoundsException();
        }
        if (codePointOffset == 0) {
            return index;
        }
        if (codePointOffset > 0) {
            int codePoints = codePointOffset;
            int i = index;
            while (codePoints > 0) {
                codePoints--;
                if (i >= end) {
                    throw new IndexOutOfBoundsException();
                }
                if (isHighSurrogate(seq[i]) && (next = i + 1) < end && isLowSurrogate(seq[next])) {
                    i++;
                }
                i++;
            }
            return i;
        }
        int codePoints2 = -codePointOffset;
        int i2 = index;
        while (codePoints2 > 0) {
            codePoints2--;
            i2--;
            if (i2 < start) {
                throw new IndexOutOfBoundsException();
            }
            if (isLowSurrogate(seq[i2]) && i2 - 1 >= start && isHighSurrogate(seq[prev])) {
                i2--;
            }
        }
        return i2;
    }

    public static int digit(char c, int radix) {
        return digit((int) c, radix);
    }

    public static int digit(int codePoint, int radix) {
        if (radix < 2 || radix > 36) {
            return -1;
        }
        if (codePoint < 128) {
            int result = -1;
            if (48 <= codePoint && codePoint <= 57) {
                result = codePoint - 48;
            } else if (97 <= codePoint && codePoint <= 122) {
                result = (codePoint - 97) + 10;
            } else if (65 <= codePoint && codePoint <= 90) {
                result = (codePoint - 65) + 10;
            }
            if (result >= radix) {
                return -1;
            }
            return result;
        }
        return digitImpl(codePoint, radix);
    }

    public boolean equals(Object object) {
        return (object instanceof Character) && ((Character) object).value == this.value;
    }

    public static char forDigit(int digit, int radix) {
        if (2 > radix || radix > 36 || digit < 0 || digit >= radix) {
            return (char) 0;
        }
        return (char) (digit < 10 ? digit + 48 : (digit + 97) - 10);
    }

    public static String getName(int codePoint) {
        checkValidCodePoint(codePoint);
        if (getType(codePoint) == 0) {
            return null;
        }
        String result = getNameImpl(codePoint);
        if (result == null) {
            String blockName = UnicodeBlock.of(codePoint).toString().replace('_', ' ');
            return blockName + " " + IntegralToString.intToHexString(codePoint, true, 0);
        }
        return result;
    }

    public static int getNumericValue(char c) {
        return getNumericValue((int) c);
    }

    public static int getNumericValue(int codePoint) {
        if (codePoint < 128) {
            if (codePoint >= 48 && codePoint <= 57) {
                return codePoint - 48;
            }
            if (codePoint >= 97 && codePoint <= 122) {
                return codePoint - 87;
            }
            if (codePoint >= 65 && codePoint <= 90) {
                return codePoint - 55;
            }
            return -1;
        }
        if (codePoint >= 65313 && codePoint <= 65338) {
            return codePoint - 65303;
        }
        if (codePoint >= 65345 && codePoint <= 65370) {
            return codePoint - 65335;
        }
        return getNumericValueImpl(codePoint);
    }

    public static int getType(char c) {
        return getType((int) c);
    }

    public static int getType(int codePoint) {
        int type = getTypeImpl(codePoint);
        return type <= 16 ? type : type + 1;
    }

    public static byte getDirectionality(char c) {
        return getDirectionality((int) c);
    }

    public static byte getDirectionality(int codePoint) {
        byte directionality;
        if (getType(codePoint) != 0 && (directionality = getIcuDirectionality(codePoint)) >= 0 && directionality < DIRECTIONALITY.length) {
            return DIRECTIONALITY[directionality];
        }
        return (byte) -1;
    }

    public static boolean isMirrored(char c) {
        return isMirrored((int) c);
    }

    public static boolean isMirrored(int codePoint) {
        return isMirroredImpl(codePoint);
    }

    public int hashCode() {
        return this.value;
    }

    public static char highSurrogate(int codePoint) {
        return (char) ((codePoint >> 10) + 55232);
    }

    public static char lowSurrogate(int codePoint) {
        return (char) ((codePoint & 1023) | 56320);
    }

    public static boolean isBmpCodePoint(int codePoint) {
        return codePoint >= 0 && codePoint <= 65535;
    }

    public static boolean isDefined(char c) {
        return isDefinedImpl(c);
    }

    public static boolean isDefined(int codePoint) {
        return isDefinedImpl(codePoint);
    }

    public static boolean isDigit(char c) {
        return isDigit((int) c);
    }

    public static boolean isDigit(int codePoint) {
        if (48 <= codePoint && codePoint <= 57) {
            return true;
        }
        if (codePoint < 1632) {
            return false;
        }
        return isDigitImpl(codePoint);
    }

    public static boolean isIdentifierIgnorable(char c) {
        return isIdentifierIgnorable((int) c);
    }

    public static boolean isIdentifierIgnorable(int codePoint) {
        if (codePoint < 1536) {
            return (codePoint >= 0 && codePoint <= 8) || (codePoint >= 14 && codePoint <= 27) || ((codePoint >= 127 && codePoint <= 159) || codePoint == 173);
        }
        return isIdentifierIgnorableImpl(codePoint);
    }

    public static boolean isISOControl(char c) {
        return isISOControl((int) c);
    }

    public static boolean isISOControl(int c) {
        return (c >= 0 && c <= 31) || (c >= 127 && c <= 159);
    }

    public static boolean isJavaIdentifierPart(char c) {
        return isJavaIdentifierPart((int) c);
    }

    public static boolean isJavaIdentifierPart(int codePoint) {
        if (codePoint < 64) {
            return (287948970162897407L & (1 << codePoint)) != 0;
        }
        if (codePoint < 128) {
            return ((-8646911290859585538L) & (1 << (codePoint + (-64)))) != 0;
        }
        int type = getType(codePoint);
        if ((type >= 1 && type <= 5) || type == 26 || type == 23) {
            return true;
        }
        if ((type >= 9 && type <= 10) || type == 8 || type == 6) {
            return true;
        }
        if (codePoint >= 0 && codePoint <= 8) {
            return true;
        }
        if (codePoint < 14 || codePoint > 27) {
            return (codePoint >= 127 && codePoint <= 159) || type == 16;
        }
        return true;
    }

    public static boolean isJavaIdentifierStart(char c) {
        return isJavaIdentifierStart((int) c);
    }

    public static boolean isJavaIdentifierStart(int codePoint) {
        if (codePoint < 64) {
            return codePoint == 36;
        }
        if (codePoint < 128) {
            return (576460745995190270L & (1 << (codePoint + (-64)))) != 0;
        }
        int type = getType(codePoint);
        return (type >= 1 && type <= 5) || type == 26 || type == 23 || type == 10;
    }

    @Deprecated
    public static boolean isJavaLetter(char c) {
        return isJavaIdentifierStart(c);
    }

    @Deprecated
    public static boolean isJavaLetterOrDigit(char c) {
        return isJavaIdentifierPart(c);
    }

    public static boolean isLetter(char c) {
        return isLetter((int) c);
    }

    public static boolean isLetter(int codePoint) {
        if ((65 <= codePoint && codePoint <= 90) || (97 <= codePoint && codePoint <= 122)) {
            return true;
        }
        if (codePoint < 128) {
            return false;
        }
        return isLetterImpl(codePoint);
    }

    public static boolean isLetterOrDigit(char c) {
        return isLetterOrDigit((int) c);
    }

    public static boolean isLetterOrDigit(int codePoint) {
        if (65 <= codePoint && codePoint <= 90) {
            return true;
        }
        if (97 <= codePoint && codePoint <= 122) {
            return true;
        }
        if (48 <= codePoint && codePoint <= 57) {
            return true;
        }
        if (codePoint < 128) {
            return false;
        }
        return isLetterOrDigitImpl(codePoint);
    }

    public static boolean isLowerCase(char c) {
        return isLowerCase((int) c);
    }

    public static boolean isLowerCase(int codePoint) {
        if (97 <= codePoint && codePoint <= 122) {
            return true;
        }
        if (codePoint < 128) {
            return false;
        }
        return isLowerCaseImpl(codePoint);
    }

    @Deprecated
    public static boolean isSpace(char c) {
        return c == '\n' || c == '\t' || c == '\f' || c == '\r' || c == ' ';
    }

    public static boolean isSpaceChar(char c) {
        return isSpaceChar((int) c);
    }

    public static boolean isSpaceChar(int codePoint) {
        if (codePoint == 32 || codePoint == 160) {
            return true;
        }
        if (codePoint < 4096) {
            return false;
        }
        if (codePoint == 5760 || codePoint == 6158) {
            return true;
        }
        if (codePoint < 8192) {
            return false;
        }
        if (codePoint <= 65535) {
            return codePoint <= 8202 || codePoint == 8232 || codePoint == 8233 || codePoint == 8239 || codePoint == 8287 || codePoint == 12288;
        }
        return isSpaceCharImpl(codePoint);
    }

    public static boolean isTitleCase(char c) {
        return isTitleCaseImpl(c);
    }

    public static boolean isTitleCase(int codePoint) {
        return isTitleCaseImpl(codePoint);
    }

    public static boolean isUnicodeIdentifierPart(char c) {
        return isUnicodeIdentifierPartImpl(c);
    }

    public static boolean isUnicodeIdentifierPart(int codePoint) {
        return isUnicodeIdentifierPartImpl(codePoint);
    }

    public static boolean isUnicodeIdentifierStart(char c) {
        return isUnicodeIdentifierStartImpl(c);
    }

    public static boolean isUnicodeIdentifierStart(int codePoint) {
        return isUnicodeIdentifierStartImpl(codePoint);
    }

    public static boolean isUpperCase(char c) {
        return isUpperCase((int) c);
    }

    public static boolean isUpperCase(int codePoint) {
        if (65 <= codePoint && codePoint <= 90) {
            return true;
        }
        if (codePoint < 128) {
            return false;
        }
        return isUpperCaseImpl(codePoint);
    }

    public static boolean isWhitespace(char c) {
        return isWhitespace((int) c);
    }

    public static boolean isWhitespace(int codePoint) {
        if ((codePoint >= 28 && codePoint <= 32) || (codePoint >= 9 && codePoint <= 13)) {
            return true;
        }
        if (codePoint < 4096) {
            return false;
        }
        if (codePoint == 5760 || codePoint == 6158) {
            return true;
        }
        if (codePoint < 8192 || codePoint == 8199 || codePoint == 8239) {
            return false;
        }
        if (codePoint <= 65535) {
            return codePoint <= 8202 || codePoint == 8232 || codePoint == 8233 || codePoint == 8287 || codePoint == 12288;
        }
        return isWhitespaceImpl(codePoint);
    }

    public static char reverseBytes(char c) {
        return (char) ((c << '\b') | (c >> '\b'));
    }

    public static char toLowerCase(char c) {
        return (char) toLowerCase((int) c);
    }

    public static int toLowerCase(int codePoint) {
        if (65 > codePoint || codePoint > 90) {
            return codePoint >= 192 ? toLowerCaseImpl(codePoint) : codePoint;
        }
        return (char) (codePoint + 32);
    }

    public String toString() {
        return String.valueOf(this.value);
    }

    public static String toString(char value) {
        return String.valueOf(value);
    }

    public static char toTitleCase(char c) {
        return (char) toTitleCaseImpl(c);
    }

    public static int toTitleCase(int codePoint) {
        return toTitleCaseImpl(codePoint);
    }

    public static char toUpperCase(char c) {
        return (char) toUpperCase((int) c);
    }

    public static int toUpperCase(int codePoint) {
        if (97 > codePoint || codePoint > 122) {
            return codePoint >= 181 ? toUpperCaseImpl(codePoint) : codePoint;
        }
        return (char) (codePoint - 32);
    }
}
