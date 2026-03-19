package com.google.i18n.phonenumbers.geocoding;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.prefixmapper.PrefixFileReader;
import java.util.List;
import java.util.Locale;

public class PhoneNumberOfflineGeocoder {
    private static PhoneNumberOfflineGeocoder instance = null;
    private final PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
    private PrefixFileReader prefixFileReader;

    PhoneNumberOfflineGeocoder(String str) {
        this.prefixFileReader = null;
        this.prefixFileReader = new PrefixFileReader(str);
    }

    public static synchronized PhoneNumberOfflineGeocoder getInstance() {
        if (instance == null) {
            instance = new PhoneNumberOfflineGeocoder("/com/google/i18n/phonenumbers/geocoding/data/");
        }
        return instance;
    }

    private String getCountryNameForNumber(Phonenumber.PhoneNumber phoneNumber, Locale locale) {
        List<String> regionCodesForCountryCode = this.phoneUtil.getRegionCodesForCountryCode(phoneNumber.getCountryCode());
        if (regionCodesForCountryCode.size() == 1) {
            return getRegionDisplayName(regionCodesForCountryCode.get(0), locale);
        }
        String str = "ZZ";
        for (String str2 : regionCodesForCountryCode) {
            if (this.phoneUtil.isValidNumberForRegion(phoneNumber, str2)) {
                if (!str.equals("ZZ")) {
                    return "";
                }
                str = str2;
            }
        }
        return getRegionDisplayName(str, locale);
    }

    private String getRegionDisplayName(String str, Locale locale) {
        return (str == null || str.equals("ZZ") || str.equals("001")) ? "" : new Locale("", str).getDisplayCountry(locale);
    }

    public String getDescriptionForValidNumber(Phonenumber.PhoneNumber phoneNumber, Locale locale) {
        String descriptionForNumber;
        Phonenumber.PhoneNumber phoneNumber2;
        String language = locale.getLanguage();
        String country = locale.getCountry();
        String countryMobileToken = PhoneNumberUtil.getCountryMobileToken(phoneNumber.getCountryCode());
        String nationalSignificantNumber = this.phoneUtil.getNationalSignificantNumber(phoneNumber);
        if (countryMobileToken.equals("") || !nationalSignificantNumber.startsWith(countryMobileToken)) {
            descriptionForNumber = this.prefixFileReader.getDescriptionForNumber(phoneNumber, language, "", country);
        } else {
            try {
                phoneNumber2 = this.phoneUtil.parse(nationalSignificantNumber.substring(countryMobileToken.length()), this.phoneUtil.getRegionCodeForCountryCode(phoneNumber.getCountryCode()));
            } catch (NumberParseException e) {
                phoneNumber2 = phoneNumber;
            }
            descriptionForNumber = this.prefixFileReader.getDescriptionForNumber(phoneNumber2, language, "", country);
        }
        return descriptionForNumber.length() > 0 ? descriptionForNumber : getCountryNameForNumber(phoneNumber, locale);
    }

    public String getDescriptionForNumber(Phonenumber.PhoneNumber phoneNumber, Locale locale) {
        PhoneNumberUtil.PhoneNumberType numberType = this.phoneUtil.getNumberType(phoneNumber);
        if (numberType == PhoneNumberUtil.PhoneNumberType.UNKNOWN) {
            return "";
        }
        if (!this.phoneUtil.isNumberGeographical(numberType, phoneNumber.getCountryCode())) {
            return getCountryNameForNumber(phoneNumber, locale);
        }
        return getDescriptionForValidNumber(phoneNumber, locale);
    }
}
