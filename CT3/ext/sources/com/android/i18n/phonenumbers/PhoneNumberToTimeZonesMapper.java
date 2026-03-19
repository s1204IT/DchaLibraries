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
    private static final Logger LOGGER;
    private static final String MAPPING_DATA_DIRECTORY = "/com/android/i18n/phonenumbers/timezones/data/";
    private static final String MAPPING_DATA_FILE_NAME = "map_data";
    private static final String UNKNOWN_TIMEZONE = "Etc/Unknown";
    static final List<String> UNKNOWN_TIME_ZONE_LIST = new ArrayList(1);
    private PrefixTimeZonesMap prefixTimeZonesMap;

    PhoneNumberToTimeZonesMapper(PrefixTimeZonesMap prefixTimeZonesMap, PhoneNumberToTimeZonesMapper phoneNumberToTimeZonesMapper) {
        this(prefixTimeZonesMap);
    }

    static {
        UNKNOWN_TIME_ZONE_LIST.add(UNKNOWN_TIMEZONE);
        LOGGER = Logger.getLogger(PhoneNumberToTimeZonesMapper.class.getName());
    }

    PhoneNumberToTimeZonesMapper(String prefixTimeZonesMapDataDirectory) {
        this.prefixTimeZonesMap = null;
        this.prefixTimeZonesMap = loadPrefixTimeZonesMapFromFile(prefixTimeZonesMapDataDirectory + MAPPING_DATA_FILE_NAME);
    }

    private PhoneNumberToTimeZonesMapper(PrefixTimeZonesMap prefixTimeZonesMap) {
        this.prefixTimeZonesMap = null;
        this.prefixTimeZonesMap = prefixTimeZonesMap;
    }

    private static PrefixTimeZonesMap loadPrefixTimeZonesMapFromFile(String path) throws Throwable {
        ObjectInputStream in;
        InputStream source = PhoneNumberToTimeZonesMapper.class.getResourceAsStream(path);
        ObjectInputStream in2 = null;
        PrefixTimeZonesMap map = new PrefixTimeZonesMap();
        try {
            try {
                in = new ObjectInputStream(source);
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            map.readExternal(in);
            close(in);
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
        return map;
    }

    private static void close(InputStream in) {
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.toString());
        }
    }

    private static class LazyHolder {
        private static final PhoneNumberToTimeZonesMapper INSTANCE;

        private LazyHolder() {
        }

        static {
            PrefixTimeZonesMap map = PhoneNumberToTimeZonesMapper.loadPrefixTimeZonesMapFromFile("/com/android/i18n/phonenumbers/timezones/data/map_data");
            INSTANCE = new PhoneNumberToTimeZonesMapper(map, null);
        }
    }

    public static synchronized PhoneNumberToTimeZonesMapper getInstance() {
        return LazyHolder.INSTANCE;
    }

    public List<String> getTimeZonesForGeographicalNumber(Phonenumber.PhoneNumber number) {
        return getTimeZonesForGeocodableNumber(number);
    }

    public List<String> getTimeZonesForNumber(Phonenumber.PhoneNumber number) {
        PhoneNumberUtil.PhoneNumberType numberType = PhoneNumberUtil.getInstance().getNumberType(number);
        if (numberType == PhoneNumberUtil.PhoneNumberType.UNKNOWN) {
            return UNKNOWN_TIME_ZONE_LIST;
        }
        if (!canBeGeocoded(numberType)) {
            return getCountryLevelTimeZonesforNumber(number);
        }
        return getTimeZonesForGeographicalNumber(number);
    }

    private boolean canBeGeocoded(PhoneNumberUtil.PhoneNumberType numberType) {
        return numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE || numberType == PhoneNumberUtil.PhoneNumberType.MOBILE || numberType == PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE;
    }

    public static String getUnknownTimeZone() {
        return UNKNOWN_TIMEZONE;
    }

    private List<String> getTimeZonesForGeocodableNumber(Phonenumber.PhoneNumber number) {
        List<String> timezones = this.prefixTimeZonesMap.lookupTimeZonesForNumber(number);
        if (timezones.isEmpty()) {
            timezones = UNKNOWN_TIME_ZONE_LIST;
        }
        return Collections.unmodifiableList(timezones);
    }

    private List<String> getCountryLevelTimeZonesforNumber(Phonenumber.PhoneNumber number) {
        List<String> timezones = this.prefixTimeZonesMap.lookupCountryLevelTimeZonesForNumber(number);
        if (timezones.isEmpty()) {
            timezones = UNKNOWN_TIME_ZONE_LIST;
        }
        return Collections.unmodifiableList(timezones);
    }
}
