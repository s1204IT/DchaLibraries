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
    private static final Logger logger = Logger.getLogger(PrefixFileReader.class.getName());
    private final String phonePrefixDataDirectory;
    private MappingFileProvider mappingFileProvider = new MappingFileProvider();
    private Map<String, PhonePrefixMap> availablePhonePrefixMaps = new HashMap();

    public PrefixFileReader(String str) throws Throwable {
        this.phonePrefixDataDirectory = str;
        loadMappingFileProvider();
    }

    private void loadMappingFileProvider() throws Throwable {
        ObjectInputStream objectInputStream;
        ObjectInputStream objectInputStream2 = null;
        try {
            try {
                objectInputStream = new ObjectInputStream(PrefixFileReader.class.getResourceAsStream(this.phonePrefixDataDirectory + "config"));
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            this.mappingFileProvider.readExternal(objectInputStream);
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
    }

    private PhonePrefixMap getPhonePrefixDescriptions(int i, String str, String str2, String str3) throws Throwable {
        String fileName = this.mappingFileProvider.getFileName(i, str, str2, str3);
        if (fileName.length() == 0) {
            return null;
        }
        if (!this.availablePhonePrefixMaps.containsKey(fileName)) {
            loadPhonePrefixMapFromFile(fileName);
        }
        return this.availablePhonePrefixMaps.get(fileName);
    }

    private void loadPhonePrefixMapFromFile(String str) throws Throwable {
        ObjectInputStream objectInputStream;
        ObjectInputStream objectInputStream2 = null;
        try {
            try {
                objectInputStream = new ObjectInputStream(PrefixFileReader.class.getResourceAsStream(this.phonePrefixDataDirectory + str));
            } catch (IOException e) {
                e = e;
            }
        } catch (Throwable th) {
            th = th;
        }
        try {
            PhonePrefixMap phonePrefixMap = new PhonePrefixMap();
            phonePrefixMap.readExternal(objectInputStream);
            this.availablePhonePrefixMaps.put(str, phonePrefixMap);
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

    public String getDescriptionForNumber(Phonenumber.PhoneNumber phoneNumber, String str, String str2, String str3) throws Throwable {
        int countryCode = phoneNumber.getCountryCode();
        if (countryCode == 1) {
            countryCode = 1000 + ((int) (phoneNumber.getNationalNumber() / 10000000));
        }
        PhonePrefixMap phonePrefixDescriptions = getPhonePrefixDescriptions(countryCode, str, str2, str3);
        String strLookup = phonePrefixDescriptions != null ? phonePrefixDescriptions.lookup(phoneNumber) : null;
        if ((strLookup == null || strLookup.length() == 0) && mayFallBackToEnglish(str)) {
            PhonePrefixMap phonePrefixDescriptions2 = getPhonePrefixDescriptions(countryCode, "en", "", "");
            if (phonePrefixDescriptions2 == null) {
                return "";
            }
            strLookup = phonePrefixDescriptions2.lookup(phoneNumber);
        }
        return strLookup != null ? strLookup : "";
    }

    private boolean mayFallBackToEnglish(String str) {
        return (str.equals("zh") || str.equals("ja") || str.equals("ko")) ? false : true;
    }
}
