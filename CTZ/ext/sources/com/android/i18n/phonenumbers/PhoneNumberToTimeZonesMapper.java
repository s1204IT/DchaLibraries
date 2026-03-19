package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.PhoneNumberUtil;
import com.android.i18n.phonenumbers.Phonenumber;
import com.android.i18n.phonenumbers.prefixmapper.PrefixTimeZonesMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PhoneNumberToTimeZonesMapper {
    private static final String MAPPING_DATA_DIRECTORY = "/com/android/i18n/phonenumbers/timezones/data/";
    private static final String MAPPING_DATA_FILE_NAME = "map_data";
    private static final String UNKNOWN_TIMEZONE = "Etc/Unknown";
    static final List<String> UNKNOWN_TIME_ZONE_LIST = new ArrayList(1);
    private static final Logger logger;
    private PrefixTimeZonesMap prefixTimeZonesMap;

    static {
        UNKNOWN_TIME_ZONE_LIST.add(UNKNOWN_TIMEZONE);
        logger = Logger.getLogger(PhoneNumberToTimeZonesMapper.class.getName());
    }

    PhoneNumberToTimeZonesMapper(String str) {
        this.prefixTimeZonesMap = null;
        this.prefixTimeZonesMap = loadPrefixTimeZonesMapFromFile(str + MAPPING_DATA_FILE_NAME);
    }

    private PhoneNumberToTimeZonesMapper(PrefixTimeZonesMap prefixTimeZonesMap) {
        this.prefixTimeZonesMap = null;
        this.prefixTimeZonesMap = prefixTimeZonesMap;
    }

    private static PrefixTimeZonesMap loadPrefixTimeZonesMapFromFile(String str) throws Throwable {
        ObjectInputStream objectInputStream;
        InputStream resourceAsStream = PhoneNumberToTimeZonesMapper.class.getResourceAsStream(str);
        PrefixTimeZonesMap prefixTimeZonesMap = new PrefixTimeZonesMap();
        ObjectInputStream objectInputStream2 = null;
        try {
            try {
                objectInputStream = new ObjectInputStream(resourceAsStream);
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            prefixTimeZonesMap.readExternal(objectInputStream);
            close(objectInputStream);
        } catch (IOException e2) {
            e = e2;
            objectInputStream2 = objectInputStream;
            logger.log(Level.WARNING, e.toString());
            close(objectInputStream2);
        } catch (Throwable th2) {
            th = th2;
            objectInputStream2 = objectInputStream;
            close(objectInputStream2);
            throw th;
        }
        return prefixTimeZonesMap;
    }

    private static void close(InputStream inputStream) {
        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, e.toString());
            }
        }
    }

    private static class LazyHolder {
        private static final PhoneNumberToTimeZonesMapper INSTANCE = new PhoneNumberToTimeZonesMapper(PhoneNumberToTimeZonesMapper.loadPrefixTimeZonesMapFromFile("/com/android/i18n/phonenumbers/timezones/data/map_data"));

        private LazyHolder() {
        }
    }

    public static synchronized PhoneNumberToTimeZonesMapper getInstance() {
        return LazyHolder.INSTANCE;
    }

    public List<String> getTimeZonesForGeographicalNumber(Phonenumber.PhoneNumber phoneNumber) {
        return getTimeZonesForGeocodableNumber(phoneNumber);
    }

    public List<String> getTimeZonesForNumber(Phonenumber.PhoneNumber phoneNumber) {
        PhoneNumberUtil.PhoneNumberType numberType = PhoneNumberUtil.getInstance().getNumberType(phoneNumber);
        if (numberType == PhoneNumberUtil.PhoneNumberType.UNKNOWN) {
            return UNKNOWN_TIME_ZONE_LIST;
        }
        if (!PhoneNumberUtil.getInstance().isNumberGeographical(numberType, phoneNumber.getCountryCode())) {
            return getCountryLevelTimeZonesforNumber(phoneNumber);
        }
        return getTimeZonesForGeographicalNumber(phoneNumber);
    }

    public static String getUnknownTimeZone() {
        return UNKNOWN_TIMEZONE;
    }

    private List<String> getTimeZonesForGeocodableNumber(Phonenumber.PhoneNumber phoneNumber) {
        List<String> listLookupTimeZonesForNumber = this.prefixTimeZonesMap.lookupTimeZonesForNumber(phoneNumber);
        if (listLookupTimeZonesForNumber.isEmpty()) {
            listLookupTimeZonesForNumber = UNKNOWN_TIME_ZONE_LIST;
        }
        return Collections.unmodifiableList(listLookupTimeZonesForNumber);
    }

    private List<String> getCountryLevelTimeZonesforNumber(Phonenumber.PhoneNumber phoneNumber) {
        List<String> listLookupCountryLevelTimeZonesForNumber = this.prefixTimeZonesMap.lookupCountryLevelTimeZonesForNumber(phoneNumber);
        if (listLookupCountryLevelTimeZonesForNumber.isEmpty()) {
            listLookupCountryLevelTimeZonesForNumber = UNKNOWN_TIME_ZONE_LIST;
        }
        return Collections.unmodifiableList(listLookupCountryLevelTimeZonesForNumber);
    }
}
