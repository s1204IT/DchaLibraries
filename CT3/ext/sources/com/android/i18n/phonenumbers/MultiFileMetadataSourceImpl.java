package com.android.i18n.phonenumbers;

import com.android.i18n.phonenumbers.Phonemetadata;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

final class MultiFileMetadataSourceImpl implements MetadataSource {
    private static final String META_DATA_FILE_PREFIX = "/com/android/i18n/phonenumbers/data/PhoneNumberMetadataProto";
    private static final Logger logger = Logger.getLogger(MultiFileMetadataSourceImpl.class.getName());
    private final Map<Integer, Phonemetadata.PhoneMetadata> countryCodeToNonGeographicalMetadataMap;
    private final String currentFilePrefix;
    private final MetadataLoader metadataLoader;
    private final Map<String, Phonemetadata.PhoneMetadata> regionToMetadataMap;

    public MultiFileMetadataSourceImpl(String currentFilePrefix, MetadataLoader metadataLoader) {
        this.regionToMetadataMap = Collections.synchronizedMap(new HashMap());
        this.countryCodeToNonGeographicalMetadataMap = Collections.synchronizedMap(new HashMap());
        this.currentFilePrefix = currentFilePrefix;
        this.metadataLoader = metadataLoader;
    }

    public MultiFileMetadataSourceImpl(MetadataLoader metadataLoader) {
        this(META_DATA_FILE_PREFIX, metadataLoader);
    }

    @Override
    public Phonemetadata.PhoneMetadata getMetadataForRegion(String regionCode) {
        synchronized (this.regionToMetadataMap) {
            if (!this.regionToMetadataMap.containsKey(regionCode)) {
                logger.log(Level.WARNING, "[DBG] regionToMetadataMap load from file");
                loadMetadataFromFile(this.currentFilePrefix, regionCode, 0, this.metadataLoader);
            }
        }
        Phonemetadata.PhoneMetadata ret = this.regionToMetadataMap.get(regionCode);
        if (ret == null) {
            Phonemetadata.PhoneMetadata ret2 = new Phonemetadata.PhoneMetadata();
            ret2.setNationalPrefixForParsing("34");
            ret2.setGeneralDesc(new Phonemetadata.PhoneNumberDesc().setNationalNumberPattern("\\d{4,8}"));
            logger.log(Level.WARNING, "[DBG] getMetadataForRegion return dummy one: " + ret2);
            return ret2;
        }
        return ret;
    }

    @Override
    public Phonemetadata.PhoneMetadata getMetadataForNonGeographicalRegion(int countryCallingCode) {
        synchronized (this.countryCodeToNonGeographicalMetadataMap) {
            if (!this.countryCodeToNonGeographicalMetadataMap.containsKey(Integer.valueOf(countryCallingCode))) {
                loadMetadataFromFile(this.currentFilePrefix, PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY, countryCallingCode, this.metadataLoader);
            }
        }
        Phonemetadata.PhoneMetadata ret = this.countryCodeToNonGeographicalMetadataMap.get(Integer.valueOf(countryCallingCode));
        if (ret == null) {
            Phonemetadata.PhoneMetadata ret2 = new Phonemetadata.PhoneMetadata();
            ret2.setNationalPrefixForParsing("34");
            ret2.setGeneralDesc(new Phonemetadata.PhoneNumberDesc().setNationalNumberPattern("\\d{4,8}"));
            logger.log(Level.WARNING, "[DBG] getMetadataForNonGeographicalRegion return dummy one: " + ret2);
            return ret2;
        }
        return ret;
    }

    void loadMetadataFromFile(String filePrefix, String regionCode, int countryCallingCode, MetadataLoader metadataLoader) {
        boolean isNonGeoRegion = PhoneNumberUtil.REGION_CODE_FOR_NON_GEO_ENTITY.equals(regionCode);
        String fileName = filePrefix + "_" + (isNonGeoRegion ? String.valueOf(countryCallingCode) : regionCode);
        InputStream source = metadataLoader.loadMetadata(fileName);
        if (source == null) {
            logger.log(Level.SEVERE, "missing metadata: " + fileName);
            throw new IllegalStateException("missing metadata: " + fileName);
        }
        ObjectInputStream in = null;
        int retryCount = 0;
        while (true) {
            ObjectInputStream in2 = in;
            if (retryCount < 100) {
                try {
                    in = new ObjectInputStream(source);
                    try {
                        Phonemetadata.PhoneMetadataCollection metadataCollection = loadMetadataAndCloseInput(in);
                        List<Phonemetadata.PhoneMetadata> metadataList = metadataCollection.getMetadataList();
                        if (metadataList.isEmpty()) {
                            logger.log(Level.SEVERE, "empty metadata: " + fileName);
                            throw new IllegalStateException("empty metadata: " + fileName);
                        }
                        if (metadataList.size() > 1) {
                            logger.log(Level.WARNING, "invalid metadata (too many entries): " + fileName);
                        }
                        Phonemetadata.PhoneMetadata metadata = metadataList.get(0);
                        if (isNonGeoRegion) {
                            this.countryCodeToNonGeographicalMetadataMap.put(Integer.valueOf(countryCallingCode), metadata);
                        } else {
                            this.regionToMetadataMap.put(regionCode, metadata);
                        }
                        logger.log(Level.WARNING, "[DBG] loadMetadataFromFile done, fileName: " + fileName);
                        return;
                    } catch (IOException e) {
                        e = e;
                    }
                } catch (IOException e2) {
                    e = e2;
                    in = in2;
                }
            } else {
                logger.log(Level.SEVERE, "[DBG] load fail, fileName: " + fileName + ", retryCount: " + retryCount);
                return;
            }
            logger.log(Level.SEVERE, "cannot load/parse metadata: " + fileName, (Throwable) e);
            retryCount++;
        }
    }

    private static Phonemetadata.PhoneMetadataCollection loadMetadataAndCloseInput(ObjectInputStream source) {
        Phonemetadata.PhoneMetadataCollection metadataCollection = new Phonemetadata.PhoneMetadataCollection();
        try {
            try {
                metadataCollection.readExternal(source);
            } catch (IOException e) {
                logger.log(Level.WARNING, "error reading input (ignored)", (Throwable) e);
                try {
                    source.close();
                } catch (IOException e2) {
                    logger.log(Level.WARNING, "error closing input stream (ignored)", (Throwable) e2);
                }
            }
            return metadataCollection;
        } finally {
            try {
                source.close();
            } catch (IOException e3) {
                logger.log(Level.WARNING, "error closing input stream (ignored)", (Throwable) e3);
            }
        }
    }
}
