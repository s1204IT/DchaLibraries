package com.google.i18n.phonenumbers;

import com.google.i18n.phonenumbers.Phonemetadata;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

final class MetadataManager {
    static final MetadataLoader DEFAULT_METADATA_LOADER = new MetadataLoader() {
        @Override
        public InputStream loadMetadata(String str) {
            return MetadataManager.class.getResourceAsStream(str);
        }
    };
    private static final Logger logger = Logger.getLogger(MetadataManager.class.getName());
    private static final ConcurrentHashMap<Integer, Phonemetadata.PhoneMetadata> alternateFormatsMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Phonemetadata.PhoneMetadata> shortNumberMetadataMap = new ConcurrentHashMap<>();
    private static final Set<Integer> alternateFormatsCountryCodes = AlternateFormatsCountryCodeSet.getCountryCodeSet();
    private static final Set<String> shortNumberMetadataRegionCodes = ShortNumbersRegionCodeSet.getRegionCodeSet();

    private MetadataManager() {
    }

    static <T> Phonemetadata.PhoneMetadata getMetadataFromMultiFilePrefix(T t, ConcurrentHashMap<T, Phonemetadata.PhoneMetadata> concurrentHashMap, String str, MetadataLoader metadataLoader) {
        Phonemetadata.PhoneMetadata phoneMetadata = concurrentHashMap.get(t);
        if (phoneMetadata != null) {
            return phoneMetadata;
        }
        String str2 = str + "_" + t;
        List<Phonemetadata.PhoneMetadata> metadataFromSingleFileName = getMetadataFromSingleFileName(str2, metadataLoader);
        if (metadataFromSingleFileName.size() > 1) {
            logger.log(Level.WARNING, "more than one metadata in file " + str2);
        }
        Phonemetadata.PhoneMetadata phoneMetadata2 = metadataFromSingleFileName.get(0);
        Phonemetadata.PhoneMetadata phoneMetadataPutIfAbsent = concurrentHashMap.putIfAbsent(t, phoneMetadata2);
        return phoneMetadataPutIfAbsent != null ? phoneMetadataPutIfAbsent : phoneMetadata2;
    }

    private static List<Phonemetadata.PhoneMetadata> getMetadataFromSingleFileName(String str, MetadataLoader metadataLoader) {
        InputStream inputStreamLoadMetadata = metadataLoader.loadMetadata(str);
        if (inputStreamLoadMetadata == null) {
            throw new IllegalStateException("missing metadata: " + str);
        }
        List<Phonemetadata.PhoneMetadata> metadataList = loadMetadataAndCloseInput(inputStreamLoadMetadata).getMetadataList();
        if (metadataList.size() == 0) {
            throw new IllegalStateException("empty metadata: " + str);
        }
        return metadataList;
    }

    private static Phonemetadata.PhoneMetadataCollection loadMetadataAndCloseInput(InputStream inputStream) throws Throwable {
        ObjectInputStream objectInputStream;
        Throwable th;
        try {
            try {
                objectInputStream = new ObjectInputStream(inputStream);
            } catch (Throwable th2) {
                objectInputStream = null;
                th = th2;
            }
            try {
                Phonemetadata.PhoneMetadataCollection phoneMetadataCollection = new Phonemetadata.PhoneMetadataCollection();
                try {
                    phoneMetadataCollection.readExternal(objectInputStream);
                    try {
                        objectInputStream.close();
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "error closing input stream (ignored)", (Throwable) e);
                    }
                    return phoneMetadataCollection;
                } catch (IOException e2) {
                    throw new RuntimeException("cannot load/parse metadata", e2);
                }
            } catch (Throwable th3) {
                th = th3;
                try {
                    if (objectInputStream != null) {
                        objectInputStream.close();
                    } else {
                        inputStream.close();
                    }
                } catch (IOException e3) {
                    logger.log(Level.WARNING, "error closing input stream (ignored)", (Throwable) e3);
                }
                throw th;
            }
        } catch (IOException e4) {
            throw new RuntimeException("cannot load/parse metadata", e4);
        }
    }
}
