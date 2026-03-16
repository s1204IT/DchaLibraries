package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.Phonemetadata;
import com.android.i18n.phonenumbers.Phonenumber;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ShortNumberInfo {
    private final PhoneNumberUtil phoneUtil;
    private static final Logger logger = Logger.getLogger(ShortNumberInfo.class.getName());
    private static final ShortNumberInfo INSTANCE = new ShortNumberInfo(PhoneNumberUtil.getInstance());
    private static final Set<String> REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT = new HashSet();

    public enum ShortNumberCost {
        TOLL_FREE,
        STANDARD_RATE,
        PREMIUM_RATE,
        UNKNOWN_COST
    }

    static {
        REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.add("BR");
        REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.add("CL");
        REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.add("NI");
    }

    public static ShortNumberInfo getInstance() {
        return INSTANCE;
    }

    ShortNumberInfo(PhoneNumberUtil util) {
        this.phoneUtil = util;
    }

    public boolean isPossibleShortNumberForRegion(String shortNumber, String regionDialingFrom) {
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom);
        if (phoneMetadata == null) {
            return false;
        }
        Phonemetadata.PhoneNumberDesc generalDesc = phoneMetadata.getGeneralDesc();
        return this.phoneUtil.isNumberPossibleForDesc(shortNumber, generalDesc);
    }

    public boolean isPossibleShortNumber(Phonenumber.PhoneNumber number) {
        List<String> regionCodes = this.phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
        String shortNumber = this.phoneUtil.getNationalSignificantNumber(number);
        for (String region : regionCodes) {
            Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(region);
            if (this.phoneUtil.isNumberPossibleForDesc(shortNumber, phoneMetadata.getGeneralDesc())) {
                return true;
            }
        }
        return false;
    }

    public boolean isValidShortNumberForRegion(String shortNumber, String regionDialingFrom) {
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom);
        if (phoneMetadata == null) {
            return false;
        }
        Phonemetadata.PhoneNumberDesc generalDesc = phoneMetadata.getGeneralDesc();
        if (!generalDesc.hasNationalNumberPattern() || !this.phoneUtil.isNumberMatchingDesc(shortNumber, generalDesc)) {
            return false;
        }
        Phonemetadata.PhoneNumberDesc shortNumberDesc = phoneMetadata.getShortCode();
        if (!shortNumberDesc.hasNationalNumberPattern()) {
            logger.log(Level.WARNING, "No short code national number pattern found for region: " + regionDialingFrom);
            return false;
        }
        return this.phoneUtil.isNumberMatchingDesc(shortNumber, shortNumberDesc);
    }

    public boolean isValidShortNumber(Phonenumber.PhoneNumber number) {
        List<String> regionCodes = this.phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
        String shortNumber = this.phoneUtil.getNationalSignificantNumber(number);
        String regionCode = getRegionCodeForShortNumberFromRegionList(number, regionCodes);
        if (regionCodes.size() <= 1 || regionCode == null) {
            return isValidShortNumberForRegion(shortNumber, regionCode);
        }
        return true;
    }

    public ShortNumberCost getExpectedCostForRegion(String shortNumber, String regionDialingFrom) {
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionDialingFrom);
        if (phoneMetadata == null) {
            return ShortNumberCost.UNKNOWN_COST;
        }
        if (this.phoneUtil.isNumberMatchingDesc(shortNumber, phoneMetadata.getPremiumRate())) {
            return ShortNumberCost.PREMIUM_RATE;
        }
        if (this.phoneUtil.isNumberMatchingDesc(shortNumber, phoneMetadata.getStandardRate())) {
            return ShortNumberCost.STANDARD_RATE;
        }
        if (this.phoneUtil.isNumberMatchingDesc(shortNumber, phoneMetadata.getTollFree())) {
            return ShortNumberCost.TOLL_FREE;
        }
        if (isEmergencyNumber(shortNumber, regionDialingFrom)) {
            return ShortNumberCost.TOLL_FREE;
        }
        return ShortNumberCost.UNKNOWN_COST;
    }

    public ShortNumberCost getExpectedCost(Phonenumber.PhoneNumber number) {
        List<String> regionCodes = this.phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
        if (regionCodes.size() == 0) {
            return ShortNumberCost.UNKNOWN_COST;
        }
        String shortNumber = this.phoneUtil.getNationalSignificantNumber(number);
        if (regionCodes.size() == 1) {
            return getExpectedCostForRegion(shortNumber, regionCodes.get(0));
        }
        ShortNumberCost cost = ShortNumberCost.TOLL_FREE;
        for (String regionCode : regionCodes) {
            ShortNumberCost costForRegion = getExpectedCostForRegion(shortNumber, regionCode);
            switch (costForRegion) {
                case PREMIUM_RATE:
                    ShortNumberCost cost2 = ShortNumberCost.PREMIUM_RATE;
                    return cost2;
                case UNKNOWN_COST:
                    cost = ShortNumberCost.UNKNOWN_COST;
                    break;
                case STANDARD_RATE:
                    if (cost != ShortNumberCost.UNKNOWN_COST) {
                        cost = ShortNumberCost.STANDARD_RATE;
                    }
                    break;
                case TOLL_FREE:
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
        String nationalNumber = this.phoneUtil.getNationalSignificantNumber(number);
        for (String regionCode : regionCodes) {
            Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
            if (phoneMetadata != null && this.phoneUtil.isNumberMatchingDesc(nationalNumber, phoneMetadata.getShortCode())) {
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
        switch (cost) {
            case PREMIUM_RATE:
                desc = phoneMetadata.getPremiumRate();
                break;
            case STANDARD_RATE:
                desc = phoneMetadata.getStandardRate();
                break;
            case TOLL_FREE:
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
        Pattern emergencyNumberPattern = Pattern.compile(metadata.getEmergency().getNationalNumberPattern());
        String normalizedNumber = PhoneNumberUtil.normalizeDigitsOnly(number2);
        return (!allowPrefixMatch || REGIONS_WHERE_EMERGENCY_NUMBERS_MUST_BE_EXACT.contains(regionCode)) ? emergencyNumberPattern.matcher(normalizedNumber).matches() : emergencyNumberPattern.matcher(normalizedNumber).lookingAt();
    }

    public boolean isCarrierSpecific(Phonenumber.PhoneNumber number) {
        List<String> regionCodes = this.phoneUtil.getRegionCodesForCountryCode(number.getCountryCode());
        String regionCode = getRegionCodeForShortNumberFromRegionList(number, regionCodes);
        String nationalNumber = this.phoneUtil.getNationalSignificantNumber(number);
        Phonemetadata.PhoneMetadata phoneMetadata = MetadataManager.getShortNumberMetadataForRegion(regionCode);
        return phoneMetadata != null && this.phoneUtil.isNumberMatchingDesc(nationalNumber, phoneMetadata.getCarrierSpecific());
    }
}
