package com.android.i18n.phonenumbers.geocoding;

import com.android.i18n.phonenumbers.NumberParseException;
import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.i18n.phonenumbers.prefixmapper.PrefixFileReader;
import java.util.List;
import java.util.Locale;

public class PhoneNumberOfflineGeocoder {
    private static final String MAPPING_DATA_DIRECTORY = "/com/android/i18n/phonenumbers/geocoding/data/";
    private static PhoneNumberOfflineGeocoder instance = null;
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private PrefixFileReader prefixFileReader;

    PhoneNumberOfflineGeocoder(String phonePrefixDataDirectory) {
        this.prefixFileReader = null;
        this.prefixFileReader = new PrefixFileReader(phonePrefixDataDirectory);
    }

    public static synchronized PhoneNumberOfflineGeocoder getInstance() {
        if (instance == null) {
            instance = new PhoneNumberOfflineGeocoder(MAPPING_DATA_DIRECTORY);
        }
        return instance;
    }

    private String getCountryNameForNumber(Phonenumber.PhoneNumber number, Locale language) {
        List<String> regionCodes = this.phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
        if (regionCodes.size() == 1) {
            return getRegionDisplayName(regionCodes.get(0), language);
        }
        String regionWhereNumberIsValid = "ZZ";
        for (String regionCode : regionCodes) {
            if (this.phoneUtil.isValidNumberForRegion(number, regionCode)) {
                if (!regionWhereNumberIsValid.equals("ZZ")) {
                    return "";
                }
                regionWhereNumberIsValid = regionCode;
            }
        }
        return getRegionDisplayName(regionWhereNumberIsValid, language);
    }

    private String getRegionDisplayName(String regionCode, Locale language) {
        return (regionCode == null || regionCode.equals("ZZ") || regionCode.equals(PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY)) ? "" : new Locale("", regionCode).getDisplayCountry(language);
    }

    public String getDescriptionForValidNumber(Phonenumber.PhoneNumber number, Locale languageCode) throws Throwable {
        String areaDescription;
        Phonenumber.PhoneNumber copiedNumber;
        String langStr = languageCode.getLanguage();
        String regionStr = languageCode.getCountry();
        String mobileToken = PhoneNumberUtil.getCountryMobileToken(number.getCountryCode());
        String nationalNumber = this.phoneUtil.getNationalSignificantNumber(number);
        if (mobileToken.equals("") || !nationalNumber.startsWith(mobileToken)) {
            areaDescription = this.prefixFileReader.getDescriptionForNumber(number, langStr, "", regionStr);
        } else {
            String nationalNumber2 = nationalNumber.substring(mobileToken.length());
            String region = this.phoneUtil.getRegionCodeForCountryCode(number.getCountryCode());
            try {
                copiedNumber = this.phoneUtil.parse(nationalNumber2, region);
            } catch (NumberParseException e) {
                copiedNumber = number;
            }
            areaDescription = this.prefixFileReader.getDescriptionForNumber(copiedNumber, langStr, "", regionStr);
        }
        if (areaDescription.length() > 0) {
            return areaDescription;
        }
        String areaDescription2 = getCountryNameForNumber(number, languageCode);
        return areaDescription2;
    }

    public String getDescriptionForValidNumber(Phonenumber.PhoneNumber number, Locale languageCode, String userRegion) {
        String regionCode = this.phoneUtil.getRegionCodeForNumber(number);
        if (userRegion.equals(regionCode)) {
            return getDescriptionForValidNumber(number, languageCode);
        }
        return getRegionDisplayName(regionCode, languageCode);
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

    public String getDescriptionForNumber(Phonenumber.PhoneNumber number, Locale languageCode, String userRegion) {
        PhoneNumberUtil.PhoneNumberType numberType = this.phoneUtil.getNumberType(number);
        if (numberType == PhoneNumberUtil.PhoneNumberType.UNKNOWN) {
            return "";
        }
        if (!canBeGeocoded(numberType)) {
            return getCountryNameForNumber(number, languageCode);
        }
        return getDescriptionForValidNumber(number, languageCode, userRegion);
    }

    private boolean canBeGeocoded(PhoneNumberUtil.PhoneNumberType numberType) {
        return numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE || numberType == PhoneNumberUtil.PhoneNumberType.MOBILE || numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE;
    }
}
