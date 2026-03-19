package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.Phonemetadata;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.i18n.phonenumbers.internal.MatcherApi;
import com.android.i18n.phonenumbers.internal.RegexBasedMatcher;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShortNumberInfo {

    private static final int[] f3x1c15fcc = null;
    private final Map<Integer, List<String>> countryCallingCodeToRegionCodeMap = CountryCodeToRegionCodeMap.getCountryCodeToRegionCodeMap();
    private final MatcherApi matcherApi;
    private static final Logger logger = Logger.getLogger(ShortNumberInfo.class.getName());
    private static final ShortNumberInfo INSTANCE = new ShortNumberInfo(RegexBasedMatcher.create());
    private static final Set<String> REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT = new HashSet();

    private static int[] m6xf95e7a70() {
        if (f3x1c15fcc != null) {
            return f3x1c15fcc;
        }
        int[] iArr = new int[ShortNumberCost.valuesCustom().length];
        try {
            iArr[ShortNumberCost.PREMIUM_RATE.ordinal()] = 1;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[ShortNumberCost.STANDARD_RATE.ordinal()] = 2;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[ShortNumberCost.TOLL_FREE.ordinal()] = 3;
        } catch (NoSuchFieldError e3) {
        }
        try {
            iArr[ShortNumberCost.UNKNOWN_COST.ordinal()] = 4;
        } catch (NoSuchFieldError e4) {
        }
        f3x1c15fcc = iArr;
        return iArr;
    }

    static {
        REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.add("BR");
        REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.add("CL");
        REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.add("NI");
    }

    public enum ShortNumberCost {
        TOLL_FREE,
        STANDARD_RATE,
        PREMIUM_RATE,
        UNKNOWN_COST;

        public static ShortNumberCost[] valuesCustom() {
            return values();
        }
    }

    public static ShortNumberInfo getInstance() {
        return INSTANCE;
    }

    ShortNumberInfo(MatcherApi matcherApi) {
        this.matcherApi = matcherApi;
    }

    private List<String> getRegionCodesForCountryCode(int countryCallingCode) {
        List<String> regionCodes = this.countryCallingCodeToRegionCodeMap.get(Integer.valueOf(countryCallingCode));
        if (regionCodes == null) {
            regionCodes = new ArrayList<>(0);
        }
        return Collections.unmodifiableList(regionCodes);
    }

    private boolean regionDialingFromMatchesNumber(Phonenumber.PhoneNumber number, String regionDialingFrom) {
        List<String> regionCodes = getRegionCodesForCountryCode(number.getCountryCode());
        return regionCodes.contains(regionDialingFrom);
    }

    @Deprecated
    public boolean isPossibleShortNumberForRegion(String shortNumber, String regionDialingFrom) {
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom);
        if (phoneMetadata == null) {
            return false;
        }
        return this.matcherApi.matchesPossibleNumber(shortNumber, phoneMetadata.getGeneralDesc());
    }

    public boolean isPossibleShortNumberForRegion(Phonenumber.PhoneNumber number, String regionDialingFrom) {
        Phonemetadata.PhoneMetadata phoneMetadata;
        if (regionDialingFromMatchesNumber(number, regionDialingFrom) && (phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom)) != null) {
            return this.matcherApi.matchesPossibleNumber(getNationalSignificantNumber(number), phoneMetadata.getGeneralDesc());
        }
        return false;
    }

    public boolean isPossibleShortNumber(Phonenumber.PhoneNumber number) {
        List<String> regionCodes = getRegionCodesForCountryCode(number.getCountryCode());
        String shortNumber = getNationalSignificantNumber(number);
        for (String region : regionCodes) {
            Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(region);
            if (phoneMetadata != null && this.matcherApi.matchesPossibleNumber(shortNumber, phoneMetadata.getGeneralDesc())) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public boolean isValidShortNumberForRegion(String shortNumber, String regionDialingFrom) {
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom);
        if (phoneMetadata == null) {
            return false;
        }
        Phonemetadata.PhoneNumberDesc generalDesc = phoneMetadata.getGeneralDesc();
        if (!matchesPossibleNumberAndNationalNumber(shortNumber, generalDesc)) {
            return false;
        }
        Phonemetadata.PhoneNumberDesc shortNumberDesc = phoneMetadata.getShortCode();
        return matchesPossibleNumberAndNationalNumber(shortNumber, shortNumberDesc);
    }

    public boolean isValidShortNumberForRegion(Phonenumber.PhoneNumber number, String regionDialingFrom) {
        Phonemetadata.PhoneMetadata phoneMetadata;
        if (!regionDialingFromMatchesNumber(number, regionDialingFrom) || (phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom)) == null) {
            return false;
        }
        String shortNumber = getNationalSignificantNumber(number);
        Phonemetadata.PhoneNumberDesc generalDesc = phoneMetadata.getGeneralDesc();
        if (!matchesPossibleNumberAndNationalNumber(shortNumber, generalDesc)) {
            return false;
        }
        Phonemetadata.PhoneNumberDesc shortNumberDesc = phoneMetadata.getShortCode();
        return matchesPossibleNumberAndNationalNumber(shortNumber, shortNumberDesc);
    }

    public boolean isValidShortNumber(Phonenumber.PhoneNumber number) {
        List<String> regionCodes = getRegionCodesForCountryCode(number.getCountryCode());
        String regionCode = getRegionCodeForShortNumberFromRegionList(number, regionCodes);
        if (regionCodes.size() <= 1 || regionCode == null) {
            return isValidShortNumberForRegion(number, regionCode);
        }
        return true;
    }

    @Deprecated
    public ShortNumberCost getExpectedCostForRegion(String shortNumber, String regionDialingFrom) {
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom);
        if (phoneMetadata == null) {
            return ShortNumberCost.UNKNOWN_COST;
        }
        if (matchesPossibleNumberAndNationalNumber(shortNumber, phoneMetadata.getPremiumRate())) {
            return ShortNumberCost.PREMIUM_RATE;
        }
        if (matchesPossibleNumberAndNationalNumber(shortNumber, phoneMetadata.getStandardRate())) {
            return ShortNumberCost.STANDARD_RATE;
        }
        if (matchesPossibleNumberAndNationalNumber(shortNumber, phoneMetadata.getTollFree())) {
            return ShortNumberCost.TOLL_FREE;
        }
        if (isEmergencyNumber(shortNumber, regionDialingFrom)) {
            return ShortNumberCost.TOLL_FREE;
        }
        return ShortNumberCost.UNKNOWN_COST;
    }

    public ShortNumberCost getExpectedCostForRegion(Phonenumber.PhoneNumber number, String regionDialingFrom) {
        if (!regionDialingFromMatchesNumber(number, regionDialingFrom)) {
            return ShortNumberCost.UNKNOWN_COST;
        }
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom);
        if (phoneMetadata == null) {
            return ShortNumberCost.UNKNOWN_COST;
        }
        String shortNumber = getNationalSignificantNumber(number);
        if (matchesPossibleNumberAndNationalNumber(shortNumber, phoneMetadata.getPremiumRate())) {
            return ShortNumberCost.PREMIUM_RATE;
        }
        if (matchesPossibleNumberAndNationalNumber(shortNumber, phoneMetadata.getStandardRate())) {
            return ShortNumberCost.STANDARD_RATE;
        }
        if (matchesPossibleNumberAndNationalNumber(shortNumber, phoneMetadata.getTollFree())) {
            return ShortNumberCost.TOLL_FREE;
        }
        if (isEmergencyNumber(shortNumber, regionDialingFrom)) {
            return ShortNumberCost.TOLL_FREE;
        }
        return ShortNumberCost.UNKNOWN_COST;
    }

    public ShortNumberCost getExpectedCost(Phonenumber.PhoneNumber number) {
        List<String> regionCodes = getRegionCodesForCountryCode(number.getCountryCode());
        if (regionCodes.size() == 0) {
            return ShortNumberCost.UNKNOWN_COST;
        }
        if (regionCodes.size() == 1) {
            return getExpectedCostForRegion(number, regionCodes.get(0));
        }
        ShortNumberCost cost = ShortNumberCost.TOLL_FREE;
        for (String regionCode : regionCodes) {
            ShortNumberCost costForRegion = getExpectedCostForRegion(number, regionCode);
            switch (m6xf95e7a70()[costForRegion.ordinal()]) {
                case 1:
                    return ShortNumberCost.PREMIUM_RATE;
                case 2:
                    if (cost != ShortNumberCost.UNKNOWN_COST) {
                        cost = ShortNumberCost.STANDARD_RATE;
                    }
                    break;
                case 3:
                    break;
                case 4:
                    cost = ShortNumberCost.UNKNOWN_COST;
                    break;
                default:
                    logger.log(Level.SEVERE, "Unrecognised cost for region: " + costForRegion);
                    break;
            }
        }
        return cost;
    }

    private String getRegionCodeForShortNumberFromRegionList(Phonenumber.PhoneNumber number, List<String> regionCodes) {
        if (regionCodes.size() == 0) {
            return null;
        }
        if (regionCodes.size() == 1) {
            return regionCodes.get(0);
        }
        String nationalNumber = getNationalSignificantNumber(number);
        for (String regionCode : regionCodes) {
            Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
            if (phoneMetadata != null && matchesPossibleNumberAndNationalNumber(nationalNumber, phoneMetadata.getShortCode())) {
                return regionCode;
            }
        }
        return null;
    }

    Set<String> getSupportedRegions() {
        return Collections.unmodifiableSet(MetadataManager.getShortNumberMetadataSupportedRegions());
    }

    String getExampleShortNumber(String regionCode) {
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
        if (phoneMetadata == null) {
            return "";
        }
        Phonemetadata.PhoneNumberDesc desc = phoneMetadata.getShortCode();
        if (desc.hasExampleNumber()) {
            return desc.getExampleNumber();
        }
        return "";
    }

    String getExampleShortNumberForCost(String regionCode, ShortNumberCost cost) {
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
        if (phoneMetadata == null) {
            return "";
        }
        Phonemetadata.PhoneNumberDesc desc = null;
        switch (m6xf95e7a70()[cost.ordinal()]) {
            case 1:
                desc = phoneMetadata.getPremiumRate();
                break;
            case 2:
                desc = phoneMetadata.getStandardRate();
                break;
            case 3:
                desc = phoneMetadata.getTollFree();
                break;
        }
        if (desc != null && desc.hasExampleNumber()) {
            return desc.getExampleNumber();
        }
        return "";
    }

    public boolean connectsToEmergencyNumber(String number, String regionCode) {
        return matchesEmergencyNumberHelper(number, regionCode, true);
    }

    public boolean isEmergencyNumber(String number, String regionCode) {
        return matchesEmergencyNumberHelper(number, regionCode, false);
    }

    private boolean matchesEmergencyNumberHelper(String number, String regionCode, boolean allowPrefixMatch) {
        Phonemetadata.PhoneMetadata metadata;
        String number2 = PhoneNumberUtil.extractPossibleNumber(number);
        if (PhoneNumberUtil.PLUS_CHARS_PATTERN.matcher(number2).lookingAt() || (metadata = MetadataManager.getShortNumberMetadataForRegion(regionCode)) == null || !metadata.hasEmergency()) {
            return false;
        }
        String normalizedNumber = PhoneNumberUtil.normalizeDigitsOnly(number2);
        Phonemetadata.PhoneNumberDesc emergencyDesc = metadata.getEmergency();
        boolean allowPrefixMatchForRegion = allowPrefixMatch && !REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.contains(regionCode);
        return this.matcherApi.matchesNationalNumber(normalizedNumber, emergencyDesc, allowPrefixMatchForRegion);
    }

    public boolean isCarrierSpecific(Phonenumber.PhoneNumber number) {
        List<String> regionCodes = getRegionCodesForCountryCode(number.getCountryCode());
        String regionCode = getRegionCodeForShortNumberFromRegionList(number, regionCodes);
        String nationalNumber = getNationalSignificantNumber(number);
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
        if (phoneMetadata != null) {
            return matchesPossibleNumberAndNationalNumber(nationalNumber, phoneMetadata.getCarrierSpecific());
        }
        return false;
    }

    private static String getNationalSignificantNumber(Phonenumber.PhoneNumber number) {
        StringBuilder nationalNumber = new StringBuilder();
        if (number.isItalianLeadingZero()) {
            char[] zeros = new char[number.getNumberOfLeadingZeros()];
            Arrays.fill(zeros, '0');
            nationalNumber.append(new String(zeros));
        }
        nationalNumber.append(number.getNationalNumber());
        return nationalNumber.toString();
    }

    private boolean matchesPossibleNumberAndNationalNumber(String number, Phonemetadata.PhoneNumberDesc numberDesc) {
        if (this.matcherApi.matchesPossibleNumber(number, numberDesc)) {
            return this.matcherApi.matchesNationalNumber(number, numberDesc, false);
        }
        return false;
    }
}
