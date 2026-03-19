package com.android.server.broadcastradio.hal2;

import android.hardware.broadcastradio.V2_0.AmFmBandRange;
import android.hardware.broadcastradio.V2_0.AmFmRegionConfig;
import android.hardware.broadcastradio.V2_0.DabTableEntry;
import android.hardware.broadcastradio.V2_0.Metadata;
import android.hardware.broadcastradio.V2_0.MetadataKey;
import android.hardware.broadcastradio.V2_0.ProgramFilter;
import android.hardware.broadcastradio.V2_0.ProgramIdentifier;
import android.hardware.broadcastradio.V2_0.ProgramInfo;
import android.hardware.broadcastradio.V2_0.ProgramListChunk;
import android.hardware.broadcastradio.V2_0.Properties;
import android.hardware.broadcastradio.V2_0.VendorKeyValue;
import android.hardware.radio.Announcement;
import android.hardware.radio.ProgramList;
import android.hardware.radio.ProgramSelector;
import android.hardware.radio.RadioManager;
import android.hardware.radio.RadioMetadata;
import android.os.ParcelableException;
import android.util.Slog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Convert {
    private static final String TAG = "BcRadio2Srv.convert";
    private static final Map<Integer, MetadataDef> metadataKeys = new HashMap();

    private enum MetadataType {
        INT,
        STRING
    }

    Convert() {
    }

    static void throwOnError(String str, int i) {
        switch (i) {
            case 0:
                return;
            case 1:
                throw new ParcelableException(new RuntimeException(str + ": UNKNOWN_ERROR"));
            case 2:
                throw new ParcelableException(new RuntimeException(str + ": INTERNAL_ERROR"));
            case 3:
                throw new IllegalArgumentException(str + ": INVALID_ARGUMENTS");
            case 4:
                throw new IllegalStateException(str + ": INVALID_STATE");
            case 5:
                throw new UnsupportedOperationException(str + ": NOT_SUPPORTED");
            case 6:
                throw new ParcelableException(new RuntimeException(str + ": TIMEOUT"));
            default:
                throw new ParcelableException(new RuntimeException(str + ": unknown error (" + i + ")"));
        }
    }

    static ArrayList<VendorKeyValue> vendorInfoToHal(Map<String, String> map) {
        if (map == null) {
            return new ArrayList<>();
        }
        ArrayList<VendorKeyValue> arrayList = new ArrayList<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            VendorKeyValue vendorKeyValue = new VendorKeyValue();
            vendorKeyValue.key = entry.getKey();
            vendorKeyValue.value = entry.getValue();
            if (vendorKeyValue.key == null || vendorKeyValue.value == null) {
                Slog.w(TAG, "VendorKeyValue contains null pointers");
            } else {
                arrayList.add(vendorKeyValue);
            }
        }
        return arrayList;
    }

    static Map<String, String> vendorInfoFromHal(List<VendorKeyValue> list) {
        if (list == null) {
            return Collections.emptyMap();
        }
        HashMap map = new HashMap();
        for (VendorKeyValue vendorKeyValue : list) {
            if (vendorKeyValue.key == null || vendorKeyValue.value == null) {
                Slog.w(TAG, "VendorKeyValue contains null pointers");
            } else {
                map.put(vendorKeyValue.key, vendorKeyValue.value);
            }
        }
        return map;
    }

    private static int identifierTypeToProgramType(int i) {
        switch (i) {
            case 1:
            case 2:
                return 2;
            case 3:
                return 4;
            case 4:
            case 11:
            default:
                if (i >= 1000 && i <= 1999) {
                    return i;
                }
                return 0;
            case 5:
            case 6:
            case 7:
            case 8:
                return 5;
            case 9:
            case 10:
                return 6;
            case 12:
            case 13:
                return 7;
        }
    }

    private static int[] identifierTypesToProgramTypes(int[] iArr) {
        HashSet hashSet = new HashSet();
        for (int i : iArr) {
            int iIdentifierTypeToProgramType = identifierTypeToProgramType(i);
            if (iIdentifierTypeToProgramType != 0) {
                hashSet.add(Integer.valueOf(iIdentifierTypeToProgramType));
                if (iIdentifierTypeToProgramType == 2) {
                    hashSet.add(1);
                }
                if (iIdentifierTypeToProgramType == 4) {
                    hashSet.add(3);
                }
            }
        }
        return hashSet.stream().mapToInt($$Lambda$Convert$UV1wDVoVlbcxpr8zevj_aMFtUGw.INSTANCE).toArray();
    }

    private static RadioManager.BandDescriptor[] amfmConfigToBands(AmFmRegionConfig amFmRegionConfig) {
        if (amFmRegionConfig == null) {
            return new RadioManager.BandDescriptor[0];
        }
        ArrayList arrayList = new ArrayList(amFmRegionConfig.ranges.size());
        for (AmFmBandRange amFmBandRange : amFmRegionConfig.ranges) {
            FrequencyBand band = Utils.getBand(amFmBandRange.lowerBound);
            if (band == FrequencyBand.UNKNOWN) {
                Slog.e(TAG, "Unknown frequency band at " + amFmBandRange.lowerBound + "kHz");
            } else if (band == FrequencyBand.FM) {
                arrayList.add(new RadioManager.FmBandDescriptor(0, 1, amFmBandRange.lowerBound, amFmBandRange.upperBound, amFmBandRange.spacing, true, true, true, true, true));
            } else {
                arrayList.add(new RadioManager.AmBandDescriptor(0, 0, amFmBandRange.lowerBound, amFmBandRange.upperBound, amFmBandRange.spacing, true));
            }
        }
        return (RadioManager.BandDescriptor[]) arrayList.toArray(new RadioManager.BandDescriptor[arrayList.size()]);
    }

    private static Map<String, Integer> dabConfigFromHal(List<DabTableEntry> list) {
        if (list == null) {
            return null;
        }
        return (Map) list.stream().collect(Collectors.toMap(new Function() {
            @Override
            public final Object apply(Object obj) {
                return ((DabTableEntry) obj).label;
            }
        }, new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((DabTableEntry) obj).frequency);
            }
        }));
    }

    static RadioManager.ModuleProperties propertiesFromHal(int i, String str, Properties properties, AmFmRegionConfig amFmRegionConfig, List<DabTableEntry> list) {
        Objects.requireNonNull(str);
        Objects.requireNonNull(properties);
        int[] array = properties.supportedIdentifierTypes.stream().mapToInt($$Lambda$Convert$UV1wDVoVlbcxpr8zevj_aMFtUGw.INSTANCE).toArray();
        return new RadioManager.ModuleProperties(i, str, 0, properties.maker, properties.product, properties.version, properties.serial, 1, 1, false, false, amfmConfigToBands(amFmRegionConfig), true, identifierTypesToProgramTypes(array), array, dabConfigFromHal(list), vendorInfoFromHal(properties.vendorInfo));
    }

    static void programIdentifierToHal(ProgramIdentifier programIdentifier, ProgramSelector.Identifier identifier) {
        programIdentifier.type = identifier.getType();
        programIdentifier.value = identifier.getValue();
    }

    static ProgramIdentifier programIdentifierToHal(ProgramSelector.Identifier identifier) {
        ProgramIdentifier programIdentifier = new ProgramIdentifier();
        programIdentifierToHal(programIdentifier, identifier);
        return programIdentifier;
    }

    static ProgramSelector.Identifier programIdentifierFromHal(ProgramIdentifier programIdentifier) {
        if (programIdentifier.type == 0) {
            return null;
        }
        return new ProgramSelector.Identifier(programIdentifier.type, programIdentifier.value);
    }

    static android.hardware.broadcastradio.V2_0.ProgramSelector programSelectorToHal(ProgramSelector programSelector) {
        android.hardware.broadcastradio.V2_0.ProgramSelector programSelector2 = new android.hardware.broadcastradio.V2_0.ProgramSelector();
        programIdentifierToHal(programSelector2.primaryId, programSelector.getPrimaryId());
        Stream map = Arrays.stream(programSelector.getSecondaryIds()).map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return Convert.programIdentifierToHal((ProgramSelector.Identifier) obj);
            }
        });
        final ArrayList<ProgramIdentifier> arrayList = programSelector2.secondaryIds;
        Objects.requireNonNull(arrayList);
        map.forEachOrdered(new Consumer() {
            @Override
            public final void accept(Object obj) {
                arrayList.add((ProgramIdentifier) obj);
            }
        });
        return programSelector2;
    }

    static ProgramSelector programSelectorFromHal(android.hardware.broadcastradio.V2_0.ProgramSelector programSelector) {
        return new ProgramSelector(identifierTypeToProgramType(programSelector.primaryId.type), (ProgramSelector.Identifier) Objects.requireNonNull(programIdentifierFromHal(programSelector.primaryId)), (ProgramSelector.Identifier[]) programSelector.secondaryIds.stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return Convert.programIdentifierFromHal((ProgramIdentifier) obj);
            }
        }).map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return (ProgramSelector.Identifier) Objects.requireNonNull((ProgramSelector.Identifier) obj);
            }
        }).toArray(new IntFunction() {
            @Override
            public final Object apply(int i) {
                return Convert.lambda$programSelectorFromHal$2(i);
            }
        }), (long[]) null);
    }

    static ProgramSelector.Identifier[] lambda$programSelectorFromHal$2(int i) {
        return new ProgramSelector.Identifier[i];
    }

    private static class MetadataDef {
        private String key;
        private MetadataType type;

        private MetadataDef(MetadataType metadataType, String str) {
            this.type = metadataType;
            this.key = str;
        }
    }

    static {
        metadataKeys.put(1, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.RDS_PS"));
        metadataKeys.put(2, new MetadataDef(MetadataType.INT, "android.hardware.radio.metadata.RDS_PTY"));
        metadataKeys.put(3, new MetadataDef(MetadataType.INT, "android.hardware.radio.metadata.RBDS_PTY"));
        metadataKeys.put(4, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.RDS_RT"));
        metadataKeys.put(5, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.TITLE"));
        metadataKeys.put(6, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.ARTIST"));
        metadataKeys.put(7, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.ALBUM"));
        metadataKeys.put(8, new MetadataDef(MetadataType.INT, "android.hardware.radio.metadata.ICON"));
        metadataKeys.put(9, new MetadataDef(MetadataType.INT, "android.hardware.radio.metadata.ART"));
        metadataKeys.put(10, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.PROGRAM_NAME"));
        metadataKeys.put(11, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.DAB_ENSEMBLE_NAME"));
        metadataKeys.put(12, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.DAB_ENSEMBLE_NAME_SHORT"));
        metadataKeys.put(13, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.DAB_SERVICE_NAME"));
        metadataKeys.put(14, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.DAB_SERVICE_NAME_SHORT"));
        metadataKeys.put(15, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.DAB_COMPONENT_NAME"));
        metadataKeys.put(16, new MetadataDef(MetadataType.STRING, "android.hardware.radio.metadata.DAB_COMPONENT_NAME_SHORT"));
    }

    private static RadioMetadata metadataFromHal(ArrayList<Metadata> arrayList) {
        RadioMetadata.Builder builder = new RadioMetadata.Builder();
        for (Metadata metadata : arrayList) {
            MetadataDef metadataDef = metadataKeys.get(Integer.valueOf(metadata.key));
            if (metadataDef != null) {
                if (metadataDef.type == MetadataType.STRING) {
                    builder.putString(metadataDef.key, metadata.stringValue);
                } else {
                    builder.putInt(metadataDef.key, (int) metadata.intValue);
                }
            } else {
                Slog.i(TAG, "Ignored unknown metadata entry: " + MetadataKey.toString(metadata.key));
            }
        }
        return builder.build();
    }

    static RadioManager.ProgramInfo programInfoFromHal(ProgramInfo programInfo) {
        return new RadioManager.ProgramInfo(programSelectorFromHal(programInfo.selector), programIdentifierFromHal(programInfo.logicallyTunedTo), programIdentifierFromHal(programInfo.physicallyTunedTo), (Collection) programInfo.relatedContent.stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return Convert.lambda$programInfoFromHal$3((ProgramIdentifier) obj);
            }
        }).collect(Collectors.toList()), programInfo.infoFlags, programInfo.signalQuality, metadataFromHal(programInfo.metadata), vendorInfoFromHal(programInfo.vendorInfo));
    }

    static ProgramSelector.Identifier lambda$programInfoFromHal$3(ProgramIdentifier programIdentifier) {
        return (ProgramSelector.Identifier) Objects.requireNonNull(programIdentifierFromHal(programIdentifier));
    }

    static ProgramFilter programFilterToHal(ProgramList.Filter filter) {
        if (filter == null) {
            filter = new ProgramList.Filter();
        }
        final ProgramFilter programFilter = new ProgramFilter();
        Stream stream = filter.getIdentifierTypes().stream();
        final ArrayList<Integer> arrayList = programFilter.identifierTypes;
        Objects.requireNonNull(arrayList);
        stream.forEachOrdered(new Consumer() {
            @Override
            public final void accept(Object obj) {
                arrayList.add((Integer) obj);
            }
        });
        filter.getIdentifiers().stream().forEachOrdered(new Consumer() {
            @Override
            public final void accept(Object obj) {
                programFilter.identifiers.add(Convert.programIdentifierToHal((ProgramSelector.Identifier) obj));
            }
        });
        programFilter.includeCategories = filter.areCategoriesIncluded();
        programFilter.excludeModifications = filter.areModificationsExcluded();
        return programFilter;
    }

    static ProgramList.Chunk programListChunkFromHal(ProgramListChunk programListChunk) {
        return new ProgramList.Chunk(programListChunk.purge, programListChunk.complete, (Set) programListChunk.modified.stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return Convert.programInfoFromHal((ProgramInfo) obj);
            }
        }).collect(Collectors.toSet()), (Set) programListChunk.removed.stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return Convert.lambda$programListChunkFromHal$6((ProgramIdentifier) obj);
            }
        }).collect(Collectors.toSet()));
    }

    static ProgramSelector.Identifier lambda$programListChunkFromHal$6(ProgramIdentifier programIdentifier) {
        return (ProgramSelector.Identifier) Objects.requireNonNull(programIdentifierFromHal(programIdentifier));
    }

    public static Announcement announcementFromHal(android.hardware.broadcastradio.V2_0.Announcement announcement) {
        return new Announcement(programSelectorFromHal(announcement.selector), announcement.type, vendorInfoFromHal(announcement.vendorInfo));
    }

    static <T> ArrayList<T> listToArrayList(List<T> list) {
        if (list == null) {
            return null;
        }
        return list instanceof ArrayList ? (ArrayList) list : new ArrayList<>(list);
    }
}
