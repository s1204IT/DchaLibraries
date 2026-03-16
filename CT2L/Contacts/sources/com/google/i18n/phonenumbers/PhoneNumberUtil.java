package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.Phonemetadata;
import com.google.i18n.phonenumbers.Phonenumber;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneNumberUtil {
    private static final Map<Character, Character> ALL_PLUS_NUMBER_GROUPING_SYMBOLS;
    private static final Map<Character, Character> ALPHA_MAPPINGS;
    private static final Map<Character, Character> ALPHA_PHONE_MAPPINGS;
    private static final Pattern CAPTURING_DIGIT_PATTERN;
    private static final Pattern CC_PATTERN;
    private static final Map<Character, Character> DIALLABLE_CHAR_MAPPINGS;
    private static final Pattern EXTN_PATTERN;
    static final String EXTN_PATTERNS_FOR_MATCHING;
    private static final String EXTN_PATTERNS_FOR_PARSING;
    private static final Pattern FG_PATTERN;
    private static final Pattern FIRST_GROUP_ONLY_PREFIX_PATTERN;
    private static final Pattern FIRST_GROUP_PATTERN;
    private static final Map<Integer, String> MOBILE_TOKEN_MAPPINGS;
    static final Pattern NON_DIGITS_PATTERN;
    private static final Pattern NP_PATTERN;
    static final Pattern PLUS_CHARS_PATTERN;
    static final Pattern SECOND_NUMBER_START_PATTERN;
    private static final Pattern SEPARATOR_PATTERN;
    private static final Pattern UNIQUE_INTERNATIONAL_PREFIX;
    static final Pattern UNWANTED_END_CHAR_PATTERN;
    private static final String VALID_ALPHA;
    private static final Pattern VALID_ALPHA_PHONE_PATTERN;
    private static final String VALID_PHONE_NUMBER;
    private static final Pattern VALID_PHONE_NUMBER_PATTERN;
    private static final Pattern VALID_START_CHAR_PATTERN;
    private static PhoneNumberUtil instance;
    private final Map<Integer, List<String>> countryCallingCodeToRegionCodeMap;
    private final String currentFilePrefix;
    private final MetadataLoader metadataLoader;
    static final MetadataLoader DEFAULT_METADATA_LOADER = new MetadataLoader() {
        @Override
        public InputStream loadMetadata(String metadataFileName) {
            return PhoneNumberUtil.class.getResourceAsStream(metadataFileName);
        }
    };
    private static final Logger logger = Logger.getLogger(PhoneNumberUtil.class.getName());
    private final Set<String> nanpaRegions = new HashSet(35);
    private final Map<String, Phonemetadata.PhoneMetadata> regionToMetadataMap = Collections.synchronizedMap(new HashMap());
    private final Map<Integer, Phonemetadata.PhoneMetadata> countryCodeToNonGeographicalMetadataMap = Collections.synchronizedMap(new HashMap());
    private final RegexCache regexCache = new RegexCache(100);
    private final Set<String> supportedRegions = new HashSet(320);
    private final Set<Integer> countryCodesForNonGeographicalRegion = new HashSet();

    public enum MatchType {
        NOT_A_NUMBER,
        NO_MATCH,
        SHORT_NSN_MATCH,
        NSN_MATCH,
        EXACT_MATCH
    }

    public enum PhoneNumberType {
        FIXED_LINE,
        MOBILE,
        FIXED_LINE_OR_MOBILE,
        TOLL_FREE,
        PREMIUM_RATE,
        SHARED_COST,
        VOIP,
        PERSONAL_NUMBER,
        PAGER,
        UAN,
        VOICEMAIL,
        UNKNOWN
    }

    public enum ValidationResult {
        IS_POSSIBLE,
        INVALID_COUNTRY_CODE,
        TOO_SHORT,
        TOO_LONG
    }

    static {
        HashMap<Integer, String> mobileTokenMap = new HashMap<>();
        mobileTokenMap.put(52, "1");
        mobileTokenMap.put(54, "9");
        MOBILE_TOKEN_MAPPINGS = Collections.unmodifiableMap(mobileTokenMap);
        HashMap<Character, Character> asciiDigitMappings = new HashMap<>();
        asciiDigitMappings.put('0', '0');
        asciiDigitMappings.put('1', '1');
        asciiDigitMappings.put('2', '2');
        asciiDigitMappings.put('3', '3');
        asciiDigitMappings.put('4', '4');
        asciiDigitMappings.put('5', '5');
        asciiDigitMappings.put('6', '6');
        asciiDigitMappings.put('7', '7');
        asciiDigitMappings.put('8', '8');
        asciiDigitMappings.put('9', '9');
        HashMap<Character, Character> alphaMap = new HashMap<>(40);
        alphaMap.put('A', '2');
        alphaMap.put('B', '2');
        alphaMap.put('C', '2');
        alphaMap.put('D', '3');
        alphaMap.put('E', '3');
        alphaMap.put('F', '3');
        alphaMap.put('G', '4');
        alphaMap.put('H', '4');
        alphaMap.put('I', '4');
        alphaMap.put('J', '5');
        alphaMap.put('K', '5');
        alphaMap.put('L', '5');
        alphaMap.put('M', '6');
        alphaMap.put('N', '6');
        alphaMap.put('O', '6');
        alphaMap.put('P', '7');
        alphaMap.put('Q', '7');
        alphaMap.put('R', '7');
        alphaMap.put('S', '7');
        alphaMap.put('T', '8');
        alphaMap.put('U', '8');
        alphaMap.put('V', '8');
        alphaMap.put('W', '9');
        alphaMap.put('X', '9');
        alphaMap.put('Y', '9');
        alphaMap.put('Z', '9');
        ALPHA_MAPPINGS = Collections.unmodifiableMap(alphaMap);
        HashMap<Character, Character> combinedMap = new HashMap<>(100);
        combinedMap.putAll(ALPHA_MAPPINGS);
        combinedMap.putAll(asciiDigitMappings);
        ALPHA_PHONE_MAPPINGS = Collections.unmodifiableMap(combinedMap);
        HashMap<Character, Character> diallableCharMap = new HashMap<>();
        diallableCharMap.putAll(asciiDigitMappings);
        diallableCharMap.put('+', '+');
        diallableCharMap.put('*', '*');
        DIALLABLE_CHAR_MAPPINGS = Collections.unmodifiableMap(diallableCharMap);
        HashMap<Character, Character> allPlusNumberGroupings = new HashMap<>();
        Iterator<Character> it = ALPHA_MAPPINGS.keySet().iterator();
        while (it.hasNext()) {
            char c = it.next().charValue();
            allPlusNumberGroupings.put(Character.valueOf(Character.toLowerCase(c)), Character.valueOf(c));
            allPlusNumberGroupings.put(Character.valueOf(c), Character.valueOf(c));
        }
        allPlusNumberGroupings.putAll(asciiDigitMappings);
        allPlusNumberGroupings.put('-', '-');
        allPlusNumberGroupings.put((char) 65293, '-');
        allPlusNumberGroupings.put((char) 8208, '-');
        allPlusNumberGroupings.put((char) 8209, '-');
        allPlusNumberGroupings.put((char) 8210, '-');
        allPlusNumberGroupings.put((char) 8211, '-');
        allPlusNumberGroupings.put((char) 8212, '-');
        allPlusNumberGroupings.put((char) 8213, '-');
        allPlusNumberGroupings.put((char) 8722, '-');
        allPlusNumberGroupings.put('/', '/');
        allPlusNumberGroupings.put((char) 65295, '/');
        allPlusNumberGroupings.put(' ', ' ');
        allPlusNumberGroupings.put((char) 12288, ' ');
        allPlusNumberGroupings.put((char) 8288, ' ');
        allPlusNumberGroupings.put('.', '.');
        allPlusNumberGroupings.put((char) 65294, '.');
        ALL_PLUS_NUMBER_GROUPING_SYMBOLS = Collections.unmodifiableMap(allPlusNumberGroupings);
        UNIQUE_INTERNATIONAL_PREFIX = Pattern.compile("[\\d]+(?:[~⁓∼～][\\d]+)?");
        String strValueOf = String.valueOf(Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).replaceAll("[, \\[\\]]", ""));
        String strValueOf2 = String.valueOf(Arrays.toString(ALPHA_MAPPINGS.keySet().toArray()).toLowerCase().replaceAll("[, \\[\\]]", ""));
        VALID_ALPHA = strValueOf2.length() != 0 ? strValueOf.concat(strValueOf2) : new String(strValueOf);
        PLUS_CHARS_PATTERN = Pattern.compile("[+＋]+");
        SEPARATOR_PATTERN = Pattern.compile("[-x‐-―−ー－-／  \u00ad\u200b\u2060\u3000()（）［］.\\[\\]/~⁓∼～]+");
        CAPTURING_DIGIT_PATTERN = Pattern.compile("(\\p{Nd})");
        VALID_START_CHAR_PATTERN = Pattern.compile("[+＋\\p{Nd}]");
        SECOND_NUMBER_START_PATTERN = Pattern.compile("[\\\\/] *x");
        UNWANTED_END_CHAR_PATTERN = Pattern.compile("[[\\P{N}&&\\P{L}]&&[^#]]+$");
        VALID_ALPHA_PHONE_PATTERN = Pattern.compile("(?:.*?[A-Za-z]){3}.*");
        String strValueOf3 = String.valueOf(String.valueOf("\\p{Nd}{2}|[+＋]*+(?:[-x‐-―−ー－-／  \u00ad\u200b\u2060\u3000()（）［］.\\[\\]/~⁓∼～*]*\\p{Nd}){3,}[-x‐-―−ー－-／  \u00ad\u200b\u2060\u3000()（）［］.\\[\\]/~⁓∼～*"));
        String strValueOf4 = String.valueOf(String.valueOf(VALID_ALPHA));
        String strValueOf5 = String.valueOf(String.valueOf("\\p{Nd}"));
        VALID_PHONE_NUMBER = new StringBuilder(strValueOf3.length() + 2 + strValueOf4.length() + strValueOf5.length()).append(strValueOf3).append(strValueOf4).append(strValueOf5).append("]*").toString();
        String strValueOf6 = String.valueOf("xｘ#＃~～");
        String singleExtnSymbolsForParsing = strValueOf6.length() != 0 ? ",".concat(strValueOf6) : new String(",");
        EXTN_PATTERNS_FOR_PARSING = createExtnPattern(singleExtnSymbolsForParsing);
        EXTN_PATTERNS_FOR_MATCHING = createExtnPattern("xｘ#＃~～");
        String strValueOf7 = String.valueOf(String.valueOf(EXTN_PATTERNS_FOR_PARSING));
        EXTN_PATTERN = Pattern.compile(new StringBuilder(strValueOf7.length() + 5).append("(?:").append(strValueOf7).append(")$").toString(), 66);
        String strValueOf8 = String.valueOf(String.valueOf(VALID_PHONE_NUMBER));
        String strValueOf9 = String.valueOf(String.valueOf(EXTN_PATTERNS_FOR_PARSING));
        VALID_PHONE_NUMBER_PATTERN = Pattern.compile(new StringBuilder(strValueOf8.length() + 5 + strValueOf9.length()).append(strValueOf8).append("(?:").append(strValueOf9).append(")?").toString(), 66);
        NON_DIGITS_PATTERN = Pattern.compile("(\\D+)");
        FIRST_GROUP_PATTERN = Pattern.compile("(\\$\\d)");
        NP_PATTERN = Pattern.compile("\\$NP");
        FG_PATTERN = Pattern.compile("\\$FG");
        CC_PATTERN = Pattern.compile("\\$CC");
        FIRST_GROUP_ONLY_PREFIX_PATTERN = Pattern.compile("\\(?\\$1\\)?");
        instance = null;
    }

    private static String createExtnPattern(String singleExtnSymbols) {
        String strValueOf = String.valueOf(String.valueOf(";ext=(\\p{Nd}{1,7})|[  \\t,]*(?:e?xt(?:ensi(?:ó?|ó))?n?|ｅ?ｘｔｎ?|["));
        String strValueOf2 = String.valueOf(String.valueOf(singleExtnSymbols));
        String strValueOf3 = String.valueOf(String.valueOf("(\\p{Nd}{1,7})"));
        String strValueOf4 = String.valueOf(String.valueOf("\\p{Nd}"));
        return new StringBuilder(strValueOf.length() + 48 + strValueOf2.length() + strValueOf3.length() + strValueOf4.length()).append(strValueOf).append(strValueOf2).append("]|int|anexo|ｉｎｔ)").append("[:\\.．]?[  \\t,-]*").append(strValueOf3).append("#?|").append("[- ]+(").append(strValueOf4).append("{1,5})#").toString();
    }

    PhoneNumberUtil(String filePrefix, MetadataLoader metadataLoader, Map<Integer, List<String>> countryCallingCodeToRegionCodeMap) {
        this.currentFilePrefix = filePrefix;
        this.metadataLoader = metadataLoader;
        this.countryCallingCodeToRegionCodeMap = countryCallingCodeToRegionCodeMap;
        for (Map.Entry<Integer, List<String>> entry : countryCallingCodeToRegionCodeMap.entrySet()) {
            List<String> regionCodes = entry.getValue();
            if (regionCodes.size() == 1 && "001".equals(regionCodes.get(0))) {
                this.countryCodesForNonGeographicalRegion.add(entry.getKey());
            } else {
                this.supportedRegions.addAll(regionCodes);
            }
        }
        if (this.supportedRegions.remove("001")) {
            logger.log(Level.WARNING, "invalid metadata (country calling code was mapped to the non-geo entity as well as specific region(s))");
        }
        this.nanpaRegions.addAll(countryCallingCodeToRegionCodeMap.get(1));
    }

    void loadMetadataFromFile(String filePrefix, String regionCode, int countryCallingCode, MetadataLoader metadataLoader) {
        boolean isNonGeoRegion = "001".equals(regionCode);
        String strValueOf = String.valueOf(String.valueOf(filePrefix));
        String strValueOf2 = String.valueOf(String.valueOf(isNonGeoRegion ? String.valueOf(countryCallingCode) : regionCode));
        String fileName = new StringBuilder(strValueOf.length() + 1 + strValueOf2.length()).append(strValueOf).append("_").append(strValueOf2).toString();
        InputStream source = metadataLoader.loadMetadata(fileName);
        if (source == null) {
            Logger logger2 = logger;
            Level level = Level.SEVERE;
            String strValueOf3 = String.valueOf(fileName);
            logger2.log(level, strValueOf3.length() != 0 ? "missing metadata: ".concat(strValueOf3) : new String("missing metadata: "));
            String strValueOf4 = String.valueOf(fileName);
            throw new IllegalStateException(strValueOf4.length() != 0 ? "missing metadata: ".concat(strValueOf4) : new String("missing metadata: "));
        }
        try {
            ObjectInputStream in = new ObjectInputStream(source);
            try {
                Phonemetadata.PhoneMetadataCollection metadataCollection = loadMetadataAndCloseInput(in);
                List<Phonemetadata.PhoneMetadata> metadataList = metadataCollection.getMetadataList();
                if (metadataList.isEmpty()) {
                    Logger logger3 = logger;
                    Level level2 = Level.SEVERE;
                    String strValueOf5 = String.valueOf(fileName);
                    logger3.log(level2, strValueOf5.length() != 0 ? "empty metadata: ".concat(strValueOf5) : new String("empty metadata: "));
                    String strValueOf6 = String.valueOf(fileName);
                    throw new IllegalStateException(strValueOf6.length() != 0 ? "empty metadata: ".concat(strValueOf6) : new String("empty metadata: "));
                }
                if (metadataList.size() > 1) {
                    Logger logger4 = logger;
                    Level level3 = Level.WARNING;
                    String strValueOf7 = String.valueOf(fileName);
                    logger4.log(level3, strValueOf7.length() != 0 ? "invalid metadata (too many entries): ".concat(strValueOf7) : new String("invalid metadata (too many entries): "));
                }
                Phonemetadata.PhoneMetadata metadata = metadataList.get(0);
                if (isNonGeoRegion) {
                    this.countryCodeToNonGeographicalMetadataMap.put(Integer.valueOf(countryCallingCode), metadata);
                } else {
                    this.regionToMetadataMap.put(regionCode, metadata);
                }
            } catch (IOException e) {
                e = e;
                Logger logger5 = logger;
                Level level4 = Level.SEVERE;
                String strValueOf8 = String.valueOf(fileName);
                logger5.log(level4, strValueOf8.length() != 0 ? "cannot load/parse metadata: ".concat(strValueOf8) : new String("cannot load/parse metadata: "), (Throwable) e);
                String strValueOf9 = String.valueOf(fileName);
                throw new RuntimeException(strValueOf9.length() != 0 ? "cannot load/parse metadata: ".concat(strValueOf9) : new String("cannot load/parse metadata: "), e);
            }
        } catch (IOException e2) {
            e = e2;
        }
    }

    private static Phonemetadata.PhoneMetadataCollection loadMetadataAndCloseInput(ObjectInputStream source) {
        Phonemetadata.PhoneMetadataCollection metadataCollection = new Phonemetadata.PhoneMetadataCollection();
        try {
            try {
                try {
                    metadataCollection.readExternal(source);
                    try {
                        source.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "error closing input stream (ignored)", (Throwable) e);
                    }
                } catch (IOException e2) {
                    logger.log(Level.WARNING, "error reading input (ignored)", (Throwable) e2);
                    try {
                        try {
                            source.close();
                        } catch (IOException e3) {
                            logger.log(Level.WARNING, "error closing input stream (ignored)", (Throwable) e3);
                        }
                    } catch (Throwable th) {
                    }
                }
            } catch (Throwable th2) {
                try {
                    try {
                        source.close();
                    } catch (IOException e4) {
                        logger.log(Level.WARNING, "error closing input stream (ignored)", (Throwable) e4);
                    }
                } catch (Throwable th3) {
                }
            }
        } catch (Throwable th4) {
        }
        return metadataCollection;
    }

    static String extractPossibleNumber(String number) {
        Matcher m = VALID_START_CHAR_PATTERN.matcher(number);
        if (m.find()) {
            String number2 = number.substring(m.start());
            Matcher trailingCharsMatcher = UNWANTED_END_CHAR_PATTERN.matcher(number2);
            if (trailingCharsMatcher.find()) {
                number2 = number2.substring(0, trailingCharsMatcher.start());
                Logger logger2 = logger;
                Level level = Level.FINER;
                String strValueOf = String.valueOf(number2);
                logger2.log(level, strValueOf.length() != 0 ? "Stripped trailing characters: ".concat(strValueOf) : new String("Stripped trailing characters: "));
            }
            Matcher secondNumber = SECOND_NUMBER_START_PATTERN.matcher(number2);
            if (secondNumber.find()) {
                return number2.substring(0, secondNumber.start());
            }
            return number2;
        }
        return "";
    }

    static boolean isViablePhoneNumber(String number) {
        if (number.length() < 2) {
            return false;
        }
        Matcher m = VALID_PHONE_NUMBER_PATTERN.matcher(number);
        return m.matches();
    }

    static String normalize(String number) {
        Matcher m = VALID_ALPHA_PHONE_PATTERN.matcher(number);
        return m.matches() ? normalizeHelper(number, ALPHA_PHONE_MAPPINGS, true) : normalizeDigitsOnly(number);
    }

    static void normalize(StringBuilder number) {
        String normalizedNumber = normalize(number.toString());
        number.replace(0, number.length(), normalizedNumber);
    }

    public static String normalizeDigitsOnly(String number) {
        return normalizeDigits(number, false).toString();
    }

    static StringBuilder normalizeDigits(String number, boolean keepNonDigits) {
        StringBuilder normalizedDigits = new StringBuilder(number.length());
        char[] arr$ = number.toCharArray();
        for (char c : arr$) {
            int digit = Character.digit(c, 10);
            if (digit != -1) {
                normalizedDigits.append(digit);
            } else if (keepNonDigits) {
                normalizedDigits.append(c);
            }
        }
        return normalizedDigits;
    }

    private static String normalizeHelper(String number, Map<Character, Character> normalizationReplacements, boolean removeNonMatches) {
        StringBuilder normalizedNumber = new StringBuilder(number.length());
        for (int i = 0; i < number.length(); i++) {
            char character = number.charAt(i);
            Character newDigit = normalizationReplacements.get(Character.valueOf(Character.toUpperCase(character)));
            if (newDigit != null) {
                normalizedNumber.append(newDigit);
            } else if (!removeNonMatches) {
                normalizedNumber.append(character);
            }
        }
        return normalizedNumber.toString();
    }

    static synchronized void setInstance(PhoneNumberUtil util) {
        instance = util;
    }

    public static synchronized PhoneNumberUtil getInstance() {
        if (instance == null) {
            setInstance(createInstance(DEFAULT_METADATA_LOADER));
        }
        return instance;
    }

    public static PhoneNumberUtil createInstance(MetadataLoader metadataLoader) {
        if (metadataLoader == null) {
            throw new IllegalArgumentException("metadataLoader could not be null.");
        }
        return new PhoneNumberUtil("/com/google/i18n/phonenumbers/data/PhoneNumberMetadataProto", metadataLoader, CountryCodeToRegionCodeMap.getCountryCodeToRegionCodeMap());
    }

    private boolean isValidRegionCode(String regionCode) {
        return regionCode != null && this.supportedRegions.contains(regionCode);
    }

    private Phonemetadata.PhoneMetadata getMetadataForRegionOrCallingCode(int countryCallingCode, String regionCode) {
        return "001".equals(regionCode) ? getMetadataForNonGeographicalRegion(countryCallingCode) : getMetadataForRegion(regionCode);
    }

    public String getNationalSignificantNumber(Phonenumber.PhoneNumber number) {
        StringBuilder nationalNumber = new StringBuilder();
        if (number.isItalianLeadingZero()) {
            char[] zeros = new char[number.getNumberOfLeadingZeros()];
            Arrays.fill(zeros, '0');
            nationalNumber.append(new String(zeros));
        }
        nationalNumber.append(number.getNationalNumber());
        return nationalNumber.toString();
    }

    public PhoneNumberType getNumberType(Phonenumber.PhoneNumber number) {
        String regionCode = getRegionCodeForNumber(number);
        Phonemetadata.PhoneMetadata metadata = getMetadataForRegionOrCallingCode(number.getCountryCode(), regionCode);
        if (metadata == null) {
            return PhoneNumberType.UNKNOWN;
        }
        String nationalSignificantNumber = getNationalSignificantNumber(number);
        return getNumberTypeHelper(nationalSignificantNumber, metadata);
    }

    private PhoneNumberType getNumberTypeHelper(String nationalNumber, Phonemetadata.PhoneMetadata metadata) {
        Phonemetadata.PhoneNumberDesc generalNumberDesc = metadata.getGeneralDesc();
        if (!generalNumberDesc.hasNationalNumberPattern() || !isNumberMatchingDesc(nationalNumber, generalNumberDesc)) {
            return PhoneNumberType.UNKNOWN;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getPremiumRate())) {
            return PhoneNumberType.PREMIUM_RATE;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getTollFree())) {
            return PhoneNumberType.TOLL_FREE;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getSharedCost())) {
            return PhoneNumberType.SHARED_COST;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getVoip())) {
            return PhoneNumberType.VOIP;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getPersonalNumber())) {
            return PhoneNumberType.PERSONAL_NUMBER;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getPager())) {
            return PhoneNumberType.PAGER;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getUan())) {
            return PhoneNumberType.UAN;
        }
        if (isNumberMatchingDesc(nationalNumber, metadata.getVoicemail())) {
            return PhoneNumberType.VOICEMAIL;
        }
        boolean isFixedLine = isNumberMatchingDesc(nationalNumber, metadata.getFixedLine());
        if (isFixedLine) {
            if (metadata.isSameMobileAndFixedLinePattern()) {
                return PhoneNumberType.FIXED_LINE_OR_MOBILE;
            }
            if (isNumberMatchingDesc(nationalNumber, metadata.getMobile())) {
                return PhoneNumberType.FIXED_LINE_OR_MOBILE;
            }
            return PhoneNumberType.FIXED_LINE;
        }
        if (!metadata.isSameMobileAndFixedLinePattern() && isNumberMatchingDesc(nationalNumber, metadata.getMobile())) {
            return PhoneNumberType.MOBILE;
        }
        return PhoneNumberType.UNKNOWN;
    }

    Phonemetadata.PhoneMetadata getMetadataForRegion(String regionCode) {
        if (!isValidRegionCode(regionCode)) {
            return null;
        }
        synchronized (this.regionToMetadataMap) {
            if (!this.regionToMetadataMap.containsKey(regionCode)) {
                loadMetadataFromFile(this.currentFilePrefix, regionCode, 0, this.metadataLoader);
            }
        }
        return this.regionToMetadataMap.get(regionCode);
    }

    Phonemetadata.PhoneMetadata getMetadataForNonGeographicalRegion(int countryCallingCode) {
        synchronized (this.countryCodeToNonGeographicalMetadataMap) {
            if (!this.countryCallingCodeToRegionCodeMap.containsKey(Integer.valueOf(countryCallingCode))) {
                return null;
            }
            if (!this.countryCodeToNonGeographicalMetadataMap.containsKey(Integer.valueOf(countryCallingCode))) {
                loadMetadataFromFile(this.currentFilePrefix, "001", countryCallingCode, this.metadataLoader);
            }
            return this.countryCodeToNonGeographicalMetadataMap.get(Integer.valueOf(countryCallingCode));
        }
    }

    boolean isNumberPossibleForDesc(String nationalNumber, Phonemetadata.PhoneNumberDesc numberDesc) {
        Matcher possibleNumberPatternMatcher = this.regexCache.getPatternForRegex(numberDesc.getPossibleNumberPattern()).matcher(nationalNumber);
        return possibleNumberPatternMatcher.matches();
    }

    boolean isNumberMatchingDesc(String nationalNumber, Phonemetadata.PhoneNumberDesc numberDesc) {
        Matcher nationalNumberPatternMatcher = this.regexCache.getPatternForRegex(numberDesc.getNationalNumberPattern()).matcher(nationalNumber);
        return isNumberPossibleForDesc(nationalNumber, numberDesc) && nationalNumberPatternMatcher.matches();
    }

    public String getRegionCodeForNumber(Phonenumber.PhoneNumber number) {
        int countryCode = number.getCountryCode();
        List<String> regions = this.countryCallingCodeToRegionCodeMap.get(Integer.valueOf(countryCode));
        if (regions == null) {
            String numberString = getNationalSignificantNumber(number);
            Logger logger2 = logger;
            Level level = Level.WARNING;
            String strValueOf = String.valueOf(String.valueOf(numberString));
            logger2.log(level, new StringBuilder(strValueOf.length() + 54).append("Missing/invalid country_code (").append(countryCode).append(") for number ").append(strValueOf).toString());
            return null;
        }
        if (regions.size() == 1) {
            return regions.get(0);
        }
        return getRegionCodeForNumberFromRegionList(number, regions);
    }

    private String getRegionCodeForNumberFromRegionList(Phonenumber.PhoneNumber number, List<String> regionCodes) {
        String nationalNumber = getNationalSignificantNumber(number);
        for (String regionCode : regionCodes) {
            Phonemetadata.PhoneMetadata metadata = getMetadataForRegion(regionCode);
            if (metadata.hasLeadingDigits()) {
                if (this.regexCache.getPatternForRegex(metadata.getLeadingDigits()).matcher(nationalNumber).lookingAt()) {
                    return regionCode;
                }
            } else if (getNumberTypeHelper(nationalNumber, metadata) != PhoneNumberType.UNKNOWN) {
                return regionCode;
            }
        }
        return null;
    }

    public String getRegionCodeForCountryCode(int countryCallingCode) {
        List<String> regionCodes = this.countryCallingCodeToRegionCodeMap.get(Integer.valueOf(countryCallingCode));
        return regionCodes == null ? "ZZ" : regionCodes.get(0);
    }

    private ValidationResult testNumberLengthAgainstPattern(Pattern numberPattern, String number) {
        Matcher numberMatcher = numberPattern.matcher(number);
        if (numberMatcher.matches()) {
            return ValidationResult.IS_POSSIBLE;
        }
        if (numberMatcher.lookingAt()) {
            return ValidationResult.TOO_LONG;
        }
        return ValidationResult.TOO_SHORT;
    }

    private boolean isShorterThanPossibleNormalNumber(Phonemetadata.PhoneMetadata regionMetadata, String number) {
        Pattern possibleNumberPattern = this.regexCache.getPatternForRegex(regionMetadata.getGeneralDesc().getPossibleNumberPattern());
        return testNumberLengthAgainstPattern(possibleNumberPattern, number) == ValidationResult.TOO_SHORT;
    }

    int extractCountryCode(StringBuilder fullNumber, StringBuilder nationalNumber) {
        if (fullNumber.length() == 0 || fullNumber.charAt(0) == '0') {
            return 0;
        }
        int numberLength = fullNumber.length();
        for (int i = 1; i <= 3 && i <= numberLength; i++) {
            int potentialCountryCode = Integer.parseInt(fullNumber.substring(0, i));
            if (this.countryCallingCodeToRegionCodeMap.containsKey(Integer.valueOf(potentialCountryCode))) {
                nationalNumber.append(fullNumber.substring(i));
                return potentialCountryCode;
            }
        }
        return 0;
    }

    int maybeExtractCountryCode(String number, Phonemetadata.PhoneMetadata defaultRegionMetadata, StringBuilder nationalNumber, boolean keepRawInput, Phonenumber.PhoneNumber phoneNumber) throws NumberParseException {
        if (number.length() == 0) {
            return 0;
        }
        StringBuilder fullNumber = new StringBuilder(number);
        String possibleCountryIddPrefix = "NonMatch";
        if (defaultRegionMetadata != null) {
            possibleCountryIddPrefix = defaultRegionMetadata.getInternationalPrefix();
        }
        Phonenumber.PhoneNumber.CountryCodeSource countryCodeSource = maybeStripInternationalPrefixAndNormalize(fullNumber, possibleCountryIddPrefix);
        if (keepRawInput) {
            phoneNumber.setCountryCodeSource(countryCodeSource);
        }
        if (countryCodeSource != Phonenumber.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY) {
            if (fullNumber.length() <= 2) {
                throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_AFTER_IDD, "Phone number had an IDD, but after this was not long enough to be a viable phone number.");
            }
            int potentialCountryCode = extractCountryCode(fullNumber, nationalNumber);
            if (potentialCountryCode != 0) {
                phoneNumber.setCountryCode(potentialCountryCode);
                return potentialCountryCode;
            }
            throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE, "Country calling code supplied was not recognised.");
        }
        if (defaultRegionMetadata != null) {
            int defaultCountryCode = defaultRegionMetadata.getCountryCode();
            String defaultCountryCodeString = String.valueOf(defaultCountryCode);
            String normalizedNumber = fullNumber.toString();
            if (normalizedNumber.startsWith(defaultCountryCodeString)) {
                StringBuilder potentialNationalNumber = new StringBuilder(normalizedNumber.substring(defaultCountryCodeString.length()));
                Phonemetadata.PhoneNumberDesc generalDesc = defaultRegionMetadata.getGeneralDesc();
                Pattern validNumberPattern = this.regexCache.getPatternForRegex(generalDesc.getNationalNumberPattern());
                maybeStripNationalPrefixAndCarrierCode(potentialNationalNumber, defaultRegionMetadata, null);
                Pattern possibleNumberPattern = this.regexCache.getPatternForRegex(generalDesc.getPossibleNumberPattern());
                if ((!validNumberPattern.matcher(fullNumber).matches() && validNumberPattern.matcher(potentialNationalNumber).matches()) || testNumberLengthAgainstPattern(possibleNumberPattern, fullNumber.toString()) == ValidationResult.TOO_LONG) {
                    nationalNumber.append((CharSequence) potentialNationalNumber);
                    if (keepRawInput) {
                        phoneNumber.setCountryCodeSource(Phonenumber.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITHOUT_PLUS_SIGN);
                    }
                    phoneNumber.setCountryCode(defaultCountryCode);
                    return defaultCountryCode;
                }
            }
        }
        phoneNumber.setCountryCode(0);
        return 0;
    }

    private boolean parsePrefixAsIdd(Pattern iddPattern, StringBuilder number) {
        Matcher m = iddPattern.matcher(number);
        if (!m.lookingAt()) {
            return false;
        }
        int matchEnd = m.end();
        Matcher digitMatcher = CAPTURING_DIGIT_PATTERN.matcher(number.substring(matchEnd));
        if (digitMatcher.find()) {
            String normalizedGroup = normalizeDigitsOnly(digitMatcher.group(1));
            if (normalizedGroup.equals("0")) {
                return false;
            }
        }
        number.delete(0, matchEnd);
        return true;
    }

    Phonenumber.PhoneNumber.CountryCodeSource maybeStripInternationalPrefixAndNormalize(StringBuilder number, String possibleIddPrefix) {
        if (number.length() == 0) {
            return Phonenumber.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY;
        }
        Matcher m = PLUS_CHARS_PATTERN.matcher(number);
        if (m.lookingAt()) {
            number.delete(0, m.end());
            normalize(number);
            return Phonenumber.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_PLUS_SIGN;
        }
        Pattern iddPattern = this.regexCache.getPatternForRegex(possibleIddPrefix);
        normalize(number);
        return parsePrefixAsIdd(iddPattern, number) ? Phonenumber.PhoneNumber.CountryCodeSource.FROM_NUMBER_WITH_IDD : Phonenumber.PhoneNumber.CountryCodeSource.FROM_DEFAULT_COUNTRY;
    }

    boolean maybeStripNationalPrefixAndCarrierCode(StringBuilder number, Phonemetadata.PhoneMetadata metadata, StringBuilder carrierCode) {
        int numberLength = number.length();
        String possibleNationalPrefix = metadata.getNationalPrefixForParsing();
        if (numberLength == 0 || possibleNationalPrefix.length() == 0) {
            return false;
        }
        Matcher prefixMatcher = this.regexCache.getPatternForRegex(possibleNationalPrefix).matcher(number);
        if (!prefixMatcher.lookingAt()) {
            return false;
        }
        Pattern nationalNumberRule = this.regexCache.getPatternForRegex(metadata.getGeneralDesc().getNationalNumberPattern());
        boolean isViableOriginalNumber = nationalNumberRule.matcher(number).matches();
        int numOfGroups = prefixMatcher.groupCount();
        String transformRule = metadata.getNationalPrefixTransformRule();
        if (transformRule == null || transformRule.length() == 0 || prefixMatcher.group(numOfGroups) == null) {
            if (isViableOriginalNumber && !nationalNumberRule.matcher(number.substring(prefixMatcher.end())).matches()) {
                return false;
            }
            if (carrierCode != null && numOfGroups > 0 && prefixMatcher.group(numOfGroups) != null) {
                carrierCode.append(prefixMatcher.group(1));
            }
            number.delete(0, prefixMatcher.end());
            return true;
        }
        StringBuilder transformedNumber = new StringBuilder(number);
        transformedNumber.replace(0, numberLength, prefixMatcher.replaceFirst(transformRule));
        if (isViableOriginalNumber && !nationalNumberRule.matcher(transformedNumber.toString()).matches()) {
            return false;
        }
        if (carrierCode != null && numOfGroups > 1) {
            carrierCode.append(prefixMatcher.group(1));
        }
        number.replace(0, number.length(), transformedNumber.toString());
        return true;
    }

    String maybeStripExtension(StringBuilder number) {
        Matcher m = EXTN_PATTERN.matcher(number);
        if (m.find() && isViablePhoneNumber(number.substring(0, m.start()))) {
            int length = m.groupCount();
            for (int i = 1; i <= length; i++) {
                if (m.group(i) != null) {
                    String extension = m.group(i);
                    number.delete(m.start(), number.length());
                    return extension;
                }
            }
        }
        return "";
    }

    private boolean checkRegionForParsing(String numberToParse, String defaultRegion) {
        return isValidRegionCode(defaultRegion) || !(numberToParse == null || numberToParse.length() == 0 || !PLUS_CHARS_PATTERN.matcher(numberToParse).lookingAt());
    }

    public Phonenumber.PhoneNumber parse(String numberToParse, String defaultRegion) throws NumberParseException {
        Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber();
        parse(numberToParse, defaultRegion, phoneNumber);
        return phoneNumber;
    }

    public void parse(String numberToParse, String defaultRegion, Phonenumber.PhoneNumber phoneNumber) throws NumberParseException {
        parseHelper(numberToParse, defaultRegion, false, true, phoneNumber);
    }

    static void setItalianLeadingZerosForPhoneNumber(String nationalNumber, Phonenumber.PhoneNumber phoneNumber) {
        if (nationalNumber.length() > 1 && nationalNumber.charAt(0) == '0') {
            phoneNumber.setItalianLeadingZero(true);
            int numberOfLeadingZeros = 1;
            while (numberOfLeadingZeros < nationalNumber.length() - 1 && nationalNumber.charAt(numberOfLeadingZeros) == '0') {
                numberOfLeadingZeros++;
            }
            if (numberOfLeadingZeros != 1) {
                phoneNumber.setNumberOfLeadingZeros(numberOfLeadingZeros);
            }
        }
    }

    private void parseHelper(String numberToParse, String defaultRegion, boolean keepRawInput, boolean checkRegion, Phonenumber.PhoneNumber phoneNumber) throws NumberParseException {
        int countryCode;
        if (numberToParse == null) {
            throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER, "The phone number supplied was null.");
        }
        if (numberToParse.length() > 250) {
            throw new NumberParseException(NumberParseException.ErrorType.TOO_LONG, "The string supplied was too long to parse.");
        }
        StringBuilder nationalNumber = new StringBuilder();
        buildNationalNumberForParsing(numberToParse, nationalNumber);
        if (!isViablePhoneNumber(nationalNumber.toString())) {
            throw new NumberParseException(NumberParseException.ErrorType.NOT_A_NUMBER, "The string supplied did not seem to be a phone number.");
        }
        if (checkRegion && !checkRegionForParsing(nationalNumber.toString(), defaultRegion)) {
            throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE, "Missing or invalid default region.");
        }
        if (keepRawInput) {
            phoneNumber.setRawInput(numberToParse);
        }
        String extension = maybeStripExtension(nationalNumber);
        if (extension.length() > 0) {
            phoneNumber.setExtension(extension);
        }
        Phonemetadata.PhoneMetadata regionMetadata = getMetadataForRegion(defaultRegion);
        StringBuilder normalizedNationalNumber = new StringBuilder();
        try {
            countryCode = maybeExtractCountryCode(nationalNumber.toString(), regionMetadata, normalizedNationalNumber, keepRawInput, phoneNumber);
        } catch (NumberParseException e) {
            Matcher matcher = PLUS_CHARS_PATTERN.matcher(nationalNumber.toString());
            if (e.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE && matcher.lookingAt()) {
                countryCode = maybeExtractCountryCode(nationalNumber.substring(matcher.end()), regionMetadata, normalizedNationalNumber, keepRawInput, phoneNumber);
                if (countryCode == 0) {
                    throw new NumberParseException(NumberParseException.ErrorType.INVALID_COUNTRY_CODE, "Could not interpret numbers after plus-sign.");
                }
            } else {
                throw new NumberParseException(e.getErrorType(), e.getMessage());
            }
        }
        if (countryCode != 0) {
            String phoneNumberRegion = getRegionCodeForCountryCode(countryCode);
            if (!phoneNumberRegion.equals(defaultRegion)) {
                regionMetadata = getMetadataForRegionOrCallingCode(countryCode, phoneNumberRegion);
            }
        } else {
            normalize(nationalNumber);
            normalizedNationalNumber.append((CharSequence) nationalNumber);
            if (defaultRegion != null) {
                phoneNumber.setCountryCode(regionMetadata.getCountryCode());
            } else if (keepRawInput) {
                phoneNumber.clearCountryCodeSource();
            }
        }
        if (normalizedNationalNumber.length() < 2) {
            throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_NSN, "The string supplied is too short to be a phone number.");
        }
        if (regionMetadata != null) {
            StringBuilder carrierCode = new StringBuilder();
            StringBuilder potentialNationalNumber = new StringBuilder(normalizedNationalNumber);
            maybeStripNationalPrefixAndCarrierCode(potentialNationalNumber, regionMetadata, carrierCode);
            if (!isShorterThanPossibleNormalNumber(regionMetadata, potentialNationalNumber.toString())) {
                normalizedNationalNumber = potentialNationalNumber;
                if (keepRawInput) {
                    phoneNumber.setPreferredDomesticCarrierCode(carrierCode.toString());
                }
            }
        }
        int lengthOfNationalNumber = normalizedNationalNumber.length();
        if (lengthOfNationalNumber < 2) {
            throw new NumberParseException(NumberParseException.ErrorType.TOO_SHORT_NSN, "The string supplied is too short to be a phone number.");
        }
        if (lengthOfNationalNumber > 17) {
            throw new NumberParseException(NumberParseException.ErrorType.TOO_LONG, "The string supplied is too long to be a phone number.");
        }
        setItalianLeadingZerosForPhoneNumber(normalizedNationalNumber.toString(), phoneNumber);
        phoneNumber.setNationalNumber(Long.parseLong(normalizedNationalNumber.toString()));
    }

    private void buildNationalNumberForParsing(String numberToParse, StringBuilder nationalNumber) {
        int indexOfPhoneContext = numberToParse.indexOf(";phone-context=");
        if (indexOfPhoneContext > 0) {
            int phoneContextStart = indexOfPhoneContext + ";phone-context=".length();
            if (numberToParse.charAt(phoneContextStart) == '+') {
                int phoneContextEnd = numberToParse.indexOf(59, phoneContextStart);
                if (phoneContextEnd > 0) {
                    nationalNumber.append(numberToParse.substring(phoneContextStart, phoneContextEnd));
                } else {
                    nationalNumber.append(numberToParse.substring(phoneContextStart));
                }
            }
            int indexOfRfc3966Prefix = numberToParse.indexOf("tel:");
            int indexOfNationalNumber = indexOfRfc3966Prefix >= 0 ? indexOfRfc3966Prefix + "tel:".length() : 0;
            nationalNumber.append(numberToParse.substring(indexOfNationalNumber, indexOfPhoneContext));
        } else {
            nationalNumber.append(extractPossibleNumber(numberToParse));
        }
        int indexOfIsdn = nationalNumber.indexOf(";isub=");
        if (indexOfIsdn > 0) {
            nationalNumber.delete(indexOfIsdn, nationalNumber.length());
        }
    }

    public MatchType isNumberMatch(Phonenumber.PhoneNumber firstNumberIn, Phonenumber.PhoneNumber secondNumberIn) {
        Phonenumber.PhoneNumber firstNumber = new Phonenumber.PhoneNumber();
        firstNumber.mergeFrom(firstNumberIn);
        Phonenumber.PhoneNumber secondNumber = new Phonenumber.PhoneNumber();
        secondNumber.mergeFrom(secondNumberIn);
        firstNumber.clearRawInput();
        firstNumber.clearCountryCodeSource();
        firstNumber.clearPreferredDomesticCarrierCode();
        secondNumber.clearRawInput();
        secondNumber.clearCountryCodeSource();
        secondNumber.clearPreferredDomesticCarrierCode();
        if (firstNumber.hasExtension() && firstNumber.getExtension().length() == 0) {
            firstNumber.clearExtension();
        }
        if (secondNumber.hasExtension() && secondNumber.getExtension().length() == 0) {
            secondNumber.clearExtension();
        }
        if (firstNumber.hasExtension() && secondNumber.hasExtension() && !firstNumber.getExtension().equals(secondNumber.getExtension())) {
            return MatchType.NO_MATCH;
        }
        int firstNumberCountryCode = firstNumber.getCountryCode();
        int secondNumberCountryCode = secondNumber.getCountryCode();
        if (firstNumberCountryCode != 0 && secondNumberCountryCode != 0) {
            if (firstNumber.exactlySameAs(secondNumber)) {
                return MatchType.EXACT_MATCH;
            }
            if (firstNumberCountryCode == secondNumberCountryCode && isNationalNumberSuffixOfTheOther(firstNumber, secondNumber)) {
                return MatchType.SHORT_NSN_MATCH;
            }
            return MatchType.NO_MATCH;
        }
        firstNumber.setCountryCode(secondNumberCountryCode);
        if (firstNumber.exactlySameAs(secondNumber)) {
            return MatchType.NSN_MATCH;
        }
        if (isNationalNumberSuffixOfTheOther(firstNumber, secondNumber)) {
            return MatchType.SHORT_NSN_MATCH;
        }
        return MatchType.NO_MATCH;
    }

    private boolean isNationalNumberSuffixOfTheOther(Phonenumber.PhoneNumber firstNumber, Phonenumber.PhoneNumber secondNumber) {
        String firstNumberNationalNumber = String.valueOf(firstNumber.getNationalNumber());
        String secondNumberNationalNumber = String.valueOf(secondNumber.getNationalNumber());
        return firstNumberNationalNumber.endsWith(secondNumberNationalNumber) || secondNumberNationalNumber.endsWith(firstNumberNationalNumber);
    }

    public MatchType isNumberMatch(String firstNumber, String secondNumber) {
        try {
            Phonenumber.PhoneNumber firstNumberAsProto = parse(firstNumber, "ZZ");
            return isNumberMatch(firstNumberAsProto, secondNumber);
        } catch (NumberParseException e) {
            if (e.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                try {
                    Phonenumber.PhoneNumber secondNumberAsProto = parse(secondNumber, "ZZ");
                    return isNumberMatch(secondNumberAsProto, firstNumber);
                } catch (NumberParseException e2) {
                    if (e2.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                        try {
                            Phonenumber.PhoneNumber firstNumberProto = new Phonenumber.PhoneNumber();
                            Phonenumber.PhoneNumber secondNumberProto = new Phonenumber.PhoneNumber();
                            parseHelper(firstNumber, null, false, false, firstNumberProto);
                            parseHelper(secondNumber, null, false, false, secondNumberProto);
                            return isNumberMatch(firstNumberProto, secondNumberProto);
                        } catch (NumberParseException e3) {
                            return MatchType.NOT_A_NUMBER;
                        }
                    }
                    return MatchType.NOT_A_NUMBER;
                }
            }
            return MatchType.NOT_A_NUMBER;
        }
    }

    public MatchType isNumberMatch(Phonenumber.PhoneNumber firstNumber, String secondNumber) {
        MatchType match;
        try {
            Phonenumber.PhoneNumber secondNumberAsProto = parse(secondNumber, "ZZ");
            return isNumberMatch(firstNumber, secondNumberAsProto);
        } catch (NumberParseException e) {
            if (e.getErrorType() == NumberParseException.ErrorType.INVALID_COUNTRY_CODE) {
                String firstNumberRegion = getRegionCodeForCountryCode(firstNumber.getCountryCode());
                try {
                    if (!firstNumberRegion.equals("ZZ")) {
                        Phonenumber.PhoneNumber secondNumberWithFirstNumberRegion = parse(secondNumber, firstNumberRegion);
                        match = isNumberMatch(firstNumber, secondNumberWithFirstNumberRegion);
                        if (match == MatchType.EXACT_MATCH) {
                            match = MatchType.NSN_MATCH;
                        }
                    } else {
                        Phonenumber.PhoneNumber secondNumberProto = new Phonenumber.PhoneNumber();
                        parseHelper(secondNumber, null, false, false, secondNumberProto);
                        match = isNumberMatch(firstNumber, secondNumberProto);
                    }
                    return match;
                } catch (NumberParseException e2) {
                    return MatchType.NOT_A_NUMBER;
                }
            }
            return MatchType.NOT_A_NUMBER;
        }
    }
}
