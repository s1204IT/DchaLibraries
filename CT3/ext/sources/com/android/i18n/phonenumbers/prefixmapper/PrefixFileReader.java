package com.android.i18n.phonenumbers.prefixmapper;

import com.android.i18n.phonenumbers.Phonenumber;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrefixFileReader {
    private static final Logger LOGGER = Logger.getLogger(PrefixFileReader.class.getName());
    private final String phonePrefixDataDirectory;
    private MappingFileProvider mappingFileProvider = new MappingFileProvider();
    private Map<String, PhonePrefixMap> availablePhonePrefixMaps = new HashMap();

    public PrefixFileReader(String phonePrefixDataDirectory) throws Throwable {
        this.phonePrefixDataDirectory = phonePrefixDataDirectory;
        loadMappingFileProvider();
    }

    private void loadMappingFileProvider() throws Throwable {
        InputStream source = PrefixFileReader.class.getResourceAsStream(this.phonePrefixDataDirectory + "config");
        ObjectInputStream in = null;
        int retryCount = 0;
        while (true) {
            ObjectInputStream in2 = in;
            if (retryCount < 100) {
                try {
                    in = new ObjectInputStream(source);
                    try {
                        this.mappingFileProvider.readExternal(in);
                        LOGGER.log(Level.WARNING, "[DBG]loadMappingFileProvider success!");
                        close(in);
                        return;
                    } catch (IOException e) {
                        e = e;
                        retryCount++;
                        try {
                            close(in);
                            LOGGER.log(Level.WARNING, e.toString());
                            close(in);
                        } catch (Throwable th) {
                            th = th;
                            close(in);
                            throw th;
                        }
                    }
                } catch (IOException e2) {
                    e = e2;
                    in = in2;
                } catch (Throwable th2) {
                    th = th2;
                    in = in2;
                    close(in);
                    throw th;
                }
            } else {
                LOGGER.log(Level.WARNING, "[DBG]loadMappingFileProvider start retry count: " + retryCount);
                return;
            }
            close(in);
        }
    }

    private PhonePrefixMap getPhonePrefixDescriptions(int prefixMapKey, String language, String script, String region) throws Throwable {
        String fileName = this.mappingFileProvider.getFileName(prefixMapKey, language, script, region);
        if (fileName.length() == 0) {
            return null;
        }
        if (!this.availablePhonePrefixMaps.containsKey(fileName)) {
            loadPhonePrefixMapFromFile(fileName);
        }
        return this.availablePhonePrefixMaps.get(fileName);
    }

    private void loadPhonePrefixMapFromFile(String fileName) throws Throwable {
        ObjectInputStream in;
        InputStream source = PrefixFileReader.class.getResourceAsStream(this.phonePrefixDataDirectory + fileName);
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
            PhonePrefixMap map = new PhonePrefixMap();
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
        if (in == null) {
            return;
        }
        try {
            in.close();
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, e.toString());
        }
    }

    public String getDescriptionForNumber(Phonenumber.PhoneNumber number, String lang, String script, String region) throws Throwable {
        int countryCallingCode = number.getCountryCode();
        int phonePrefix = countryCallingCode != 1 ? countryCallingCode : ((int) (number.getNationalNumber() / 10000000)) + 1000;
        PhonePrefixMap phonePrefixDescriptions = getPhonePrefixDescriptions(phonePrefix, lang, script, region);
        String description = phonePrefixDescriptions != null ? phonePrefixDescriptions.lookup(number) : null;
        if ((description == null || description.length() == 0) && mayFallBackToEnglish(lang)) {
            PhonePrefixMap defaultMap = getPhonePrefixDescriptions(phonePrefix, "en", "", "");
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
