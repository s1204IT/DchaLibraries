package com.google.i18n.phonenumbers.geocoding;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhoneNumberOfflineGeocoder {
    private final String phonePrefixDataDirectory;
    private static PhoneNumberOfflineGeocoder instance = null;
    private static final Logger LOGGER = Logger.getLogger(PhoneNumberOfflineGeocoder.class.getName());
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private MappingFileProvider mappingFileProvider = new MappingFileProvider();
    private Map<String, AreaCodeMap> availablePhonePrefixMaps = new HashMap();

    PhoneNumberOfflineGeocoder(String phonePrefixDataDirectory) throws Throwable {
        this.phonePrefixDataDirectory = phonePrefixDataDirectory;
        loadMappingFileProvider();
    }

    private void loadMappingFileProvider() throws Throwable {
        ObjectInputStream in;
        InputStream source = PhoneNumberOfflineGeocoder.class.getResourceAsStream(this.phonePrefixDataDirectory + "config");
        ObjectInputStream in2 = null;
        try {
            try {
                in = new ObjectInputStream(source);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            this.mappingFileProvider.readExternal(in);
            close(in);
            in2 = in;
        } catch (IOException e2) {
            e = e2;
            in2 = in;
            LOGGER.log(Level.WARNING, e.toString());
            close(in2);
        } catch (Throwable th2) {
            th = th2;
            in2 = in;
            close(in2);
            throw th;
        }
    }

    private AreaCodeMap getPhonePrefixDescriptions(int prefixMapKey, String language, String script, String region) throws Throwable {
        String fileName = this.mappingFileProvider.getFileName(prefixMapKey, language, script, region);
        if (fileName.length() == 0) {
            return null;
        }
        if (!this.availablePhonePrefixMaps.containsKey(fileName)) {
            loadAreaCodeMapFromFile(fileName);
        }
        return this.availablePhonePrefixMaps.get(fileName);
    }

    private void loadAreaCodeMapFromFile(String fileName) throws Throwable {
        ObjectInputStream in;
        InputStream source = PhoneNumberOfflineGeocoder.class.getResourceAsStream(this.phonePrefixDataDirectory + fileName);
        ObjectInputStream in2 = null;
        try {
            try {
                in = new ObjectInputStream(source);
            } catch (Throwable th) {
                th = th;
            }
        } catch (IOException e) {
            e = e;
        }
        try {
            AreaCodeMap map = new AreaCodeMap();
            map.readExternal(in);
            this.availablePhonePrefixMaps.put(fileName, map);
            close(in);
            in2 = in;
        } catch (IOException e2) {
            e = e2;
            in2 = in;
            LOGGER.log(Level.WARNING, e.toString());
            close(in2);
        } catch (Throwable th2) {
            th = th2;
            in2 = in;
            close(in2);
            throw th;
        }
    }

    private static void close(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e.toString());
            }
        }
    }

    public static synchronized PhoneNumberOfflineGeocoder getInstance() {
        if (instance == null) {
            instance = new PhoneNumberOfflineGeocoder("/com/google/i18n/phonenumbers/geocoding/data/");
        }
        return instance;
    }

    private String getCountryNameForNumber(Phonenumber.PhoneNumber number, Locale language) {
        String regionCode = this.phoneUtil.getRegionCodeForNumber(number);
        return getRegionDisplayName(regionCode, language);
    }

    private String getRegionDisplayName(String regionCode, Locale language) {
        return (regionCode == null || regionCode.equals("ZZ") || regionCode.equals("001")) ? "" : new Locale("", regionCode).getDisplayCountry(language);
    }

    public String getDescriptionForValidNumber(Phonenumber.PhoneNumber number, Locale languageCode) throws Throwable {
        String langStr = languageCode.getLanguage();
        String regionStr = languageCode.getCountry();
        String areaDescription = getAreaDescriptionForNumber(number, langStr, "", regionStr);
        return areaDescription.length() > 0 ? areaDescription : getCountryNameForNumber(number, languageCode);
    }

    public String getDescriptionForNumber(Phonenumber.PhoneNumber number, Locale languageCode) {
        PhoneNumberUtil.PhoneNumberType numberType = this.phoneUtil.getNumberType(number);
        if (numberType == PhoneNumberUtil.PhoneNumberType.UNKNOWN) {
            return "";
        }
        if (!canBeGeocoded(numberType)) {
            return getCountryNameForNumber(number, languageCode);
        }
        return getDescriptionForValidNumber(number, languageCode);
    }

    private boolean canBeGeocoded(PhoneNumberUtil.PhoneNumberType numberType) {
        return numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE || numberType == PhoneNumberUtil.PhoneNumberType.MOBILE || numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE;
    }

    private String getAreaDescriptionForNumber(Phonenumber.PhoneNumber number, String lang, String script, String region) throws Throwable {
        int countryCallingCode = number.getCountryCode();
        int phonePrefix = countryCallingCode != 1 ? countryCallingCode : ((int) (number.getNationalNumber() / 10000000)) + 1000;
        AreaCodeMap phonePrefixDescriptions = getPhonePrefixDescriptions(phonePrefix, lang, script, region);
        String description = phonePrefixDescriptions != null ? phonePrefixDescriptions.lookup(number) : null;
        if ((description == null || description.length() == 0) && mayFallBackToEnglish(lang)) {
            AreaCodeMap defaultMap = getPhonePrefixDescriptions(phonePrefix, "en", "", "");
            if (defaultMap == null) {
                return "";
            }
            description = defaultMap.lookup(number);
        }
        return description != null ? description : "";
    }

    private boolean mayFallBackToEnglish(String lang) {
        return (lang.equals("zh") || lang.equals("ja") || lang.equals("ko")) ? false : true;
    }
}
