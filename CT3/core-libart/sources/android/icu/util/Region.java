package android.icu.util;

import android.icu.impl.ICUResourceBundle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class Region implements Comparable<Region> {
    private static final String OUTLYING_OCEANIA_REGION_ID = "QO";
    private static final String UNKNOWN_REGION_ID = "ZZ";
    private static final String WORLD_ID = "001";
    private int code;
    private String id;
    private RegionType type;
    private static boolean regionDataIsLoaded = false;
    private static Map<String, Region> regionIDMap = null;
    private static Map<Integer, Region> numericCodeMap = null;
    private static Map<String, Region> regionAliases = null;
    private static ArrayList<Region> regions = null;
    private static ArrayList<Set<Region>> availableRegions = null;
    private Region containingRegion = null;
    private Set<Region> containedRegions = new TreeSet();
    private List<Region> preferredValues = null;

    public enum RegionType {
        UNKNOWN,
        TERRITORY,
        WORLD,
        CONTINENT,
        SUBCONTINENT,
        GROUPING,
        DEPRECATED;

        public static RegionType[] valuesCustom() {
            return values();
        }
    }

    private Region() {
    }

    private static synchronized void loadRegionData() {
        Region r;
        if (regionDataIsLoaded) {
            return;
        }
        regionAliases = new HashMap();
        regionIDMap = new HashMap();
        numericCodeMap = new HashMap();
        availableRegions = new ArrayList<>(RegionType.valuesCustom().length);
        UResourceBundle metadata = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "metadata", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle metadataAlias = metadata.get("alias");
        UResourceBundle territoryAlias = metadataAlias.get("territory");
        UResourceBundle supplementalData = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        UResourceBundle codeMappings = supplementalData.get("codeMappings");
        UResourceBundle idValidity = supplementalData.get("idValidity");
        UResourceBundle regionList = idValidity.get("region");
        UResourceBundle regionRegular = regionList.get("regular");
        UResourceBundle regionMacro = regionList.get("macroregion");
        UResourceBundle regionUnknown = regionList.get("unknown");
        UResourceBundle territoryContainment = supplementalData.get("territoryContainment");
        UResourceBundle worldContainment = territoryContainment.get(WORLD_ID);
        UResourceBundle groupingContainment = territoryContainment.get("grouping");
        String[] continentsArr = worldContainment.getStringArray();
        List<String> continents = Arrays.asList(continentsArr);
        String[] groupingArr = groupingContainment.getStringArray();
        List<String> groupings = Arrays.asList(groupingArr);
        List<String> regionCodes = new ArrayList<>();
        List<String> allRegions = new ArrayList<>();
        allRegions.addAll(Arrays.asList(regionRegular.getStringArray()));
        allRegions.addAll(Arrays.asList(regionMacro.getStringArray()));
        allRegions.add(regionUnknown.getString());
        for (String r2 : allRegions) {
            int rangeMarkerLocation = r2.indexOf("~");
            if (rangeMarkerLocation > 0) {
                StringBuilder regionName = new StringBuilder(r2);
                char endRange = regionName.charAt(rangeMarkerLocation + 1);
                regionName.setLength(rangeMarkerLocation);
                char lastChar = regionName.charAt(rangeMarkerLocation - 1);
                while (lastChar <= endRange) {
                    String newRegion = regionName.toString();
                    regionCodes.add(newRegion);
                    lastChar = (char) (lastChar + 1);
                    regionName.setCharAt(rangeMarkerLocation - 1, lastChar);
                }
            } else {
                regionCodes.add(r2);
            }
        }
        regions = new ArrayList<>(regionCodes.size());
        for (String id : regionCodes) {
            Region r3 = new Region();
            r3.id = id;
            r3.type = RegionType.TERRITORY;
            regionIDMap.put(id, r3);
            if (id.matches("[0-9]{3}")) {
                r3.code = Integer.valueOf(id).intValue();
                numericCodeMap.put(Integer.valueOf(r3.code), r3);
                r3.type = RegionType.SUBCONTINENT;
            } else {
                r3.code = -1;
            }
            regions.add(r3);
        }
        for (int i = 0; i < territoryAlias.getSize(); i++) {
            UResourceBundle res = territoryAlias.get(i);
            String aliasFrom = res.getKey();
            String aliasTo = res.get("replacement").getString();
            if (regionIDMap.containsKey(aliasTo) && !regionIDMap.containsKey(aliasFrom)) {
                regionAliases.put(aliasFrom, regionIDMap.get(aliasTo));
            } else {
                if (regionIDMap.containsKey(aliasFrom)) {
                    r = regionIDMap.get(aliasFrom);
                } else {
                    r = new Region();
                    r.id = aliasFrom;
                    regionIDMap.put(aliasFrom, r);
                    if (aliasFrom.matches("[0-9]{3}")) {
                        r.code = Integer.valueOf(aliasFrom).intValue();
                        numericCodeMap.put(Integer.valueOf(r.code), r);
                    } else {
                        r.code = -1;
                    }
                    regions.add(r);
                }
                r.type = RegionType.DEPRECATED;
                List<String> aliasToRegionStrings = Arrays.asList(aliasTo.split(" "));
                r.preferredValues = new ArrayList();
                for (String s : aliasToRegionStrings) {
                    if (regionIDMap.containsKey(s)) {
                        r.preferredValues.add(regionIDMap.get(s));
                    }
                }
            }
        }
        for (int i2 = 0; i2 < codeMappings.getSize(); i2++) {
            UResourceBundle mapping = codeMappings.get(i2);
            if (mapping.getType() == 8) {
                String[] codeMappingStrings = mapping.getStringArray();
                String codeMappingID = codeMappingStrings[0];
                Integer codeMappingNumber = Integer.valueOf(codeMappingStrings[1]);
                String codeMapping3Letter = codeMappingStrings[2];
                if (regionIDMap.containsKey(codeMappingID)) {
                    Region r4 = regionIDMap.get(codeMappingID);
                    r4.code = codeMappingNumber.intValue();
                    numericCodeMap.put(Integer.valueOf(r4.code), r4);
                    regionAliases.put(codeMapping3Letter, r4);
                }
            }
        }
        if (regionIDMap.containsKey(WORLD_ID)) {
            Region r5 = regionIDMap.get(WORLD_ID);
            r5.type = RegionType.WORLD;
        }
        if (regionIDMap.containsKey(UNKNOWN_REGION_ID)) {
            Region r6 = regionIDMap.get(UNKNOWN_REGION_ID);
            r6.type = RegionType.UNKNOWN;
        }
        for (String continent : continents) {
            if (regionIDMap.containsKey(continent)) {
                Region r7 = regionIDMap.get(continent);
                r7.type = RegionType.CONTINENT;
            }
        }
        for (String grouping : groupings) {
            if (regionIDMap.containsKey(grouping)) {
                Region r8 = regionIDMap.get(grouping);
                r8.type = RegionType.GROUPING;
            }
        }
        if (regionIDMap.containsKey(OUTLYING_OCEANIA_REGION_ID)) {
            Region r9 = regionIDMap.get(OUTLYING_OCEANIA_REGION_ID);
            r9.type = RegionType.SUBCONTINENT;
        }
        for (int i3 = 0; i3 < territoryContainment.getSize(); i3++) {
            UResourceBundle mapping2 = territoryContainment.get(i3);
            String parent = mapping2.getKey();
            if (!parent.equals("containedGroupings") && !parent.equals("deprecated")) {
                Region parentRegion = regionIDMap.get(parent);
                for (int j = 0; j < mapping2.getSize(); j++) {
                    String child = mapping2.getString(j);
                    Region childRegion = regionIDMap.get(child);
                    if (parentRegion != null && childRegion != null) {
                        parentRegion.containedRegions.add(childRegion);
                        if (parentRegion.getType() != RegionType.GROUPING) {
                            childRegion.containingRegion = parentRegion;
                        }
                    }
                }
            }
        }
        for (int i4 = 0; i4 < RegionType.valuesCustom().length; i4++) {
            availableRegions.add(new TreeSet());
        }
        for (Region ar : regions) {
            Set<Region> currentSet = availableRegions.get(ar.type.ordinal());
            currentSet.add(ar);
            availableRegions.set(ar.type.ordinal(), currentSet);
        }
        regionDataIsLoaded = true;
    }

    public static Region getInstance(String id) {
        if (id == null) {
            throw new NullPointerException();
        }
        loadRegionData();
        Region r = regionIDMap.get(id);
        if (r == null) {
            r = regionAliases.get(id);
        }
        if (r == null) {
            throw new IllegalArgumentException("Unknown region id: " + id);
        }
        if (r.type == RegionType.DEPRECATED && r.preferredValues.size() == 1) {
            return r.preferredValues.get(0);
        }
        return r;
    }

    public static Region getInstance(int code) {
        loadRegionData();
        Region r = numericCodeMap.get(Integer.valueOf(code));
        if (r == null) {
            String pad = "";
            if (code < 10) {
                pad = "00";
            } else if (code < 100) {
                pad = AndroidHardcodedSystemProperties.JAVA_VERSION;
            }
            String id = pad + Integer.toString(code);
            r = regionAliases.get(id);
        }
        if (r == null) {
            throw new IllegalArgumentException("Unknown region code: " + code);
        }
        if (r.type == RegionType.DEPRECATED && r.preferredValues.size() == 1) {
            return r.preferredValues.get(0);
        }
        return r;
    }

    public static Set<Region> getAvailable(RegionType type) {
        loadRegionData();
        return Collections.unmodifiableSet(availableRegions.get(type.ordinal()));
    }

    public Region getContainingRegion() {
        loadRegionData();
        return this.containingRegion;
    }

    public Region getContainingRegion(RegionType type) {
        loadRegionData();
        if (this.containingRegion == null) {
            return null;
        }
        if (this.containingRegion.type.equals(type)) {
            return this.containingRegion;
        }
        return this.containingRegion.getContainingRegion(type);
    }

    public Set<Region> getContainedRegions() {
        loadRegionData();
        return Collections.unmodifiableSet(this.containedRegions);
    }

    public Set<Region> getContainedRegions(RegionType type) {
        loadRegionData();
        Set<Region> result = new TreeSet<>();
        Set<Region> cr = getContainedRegions();
        for (Region r : cr) {
            if (r.getType() == type) {
                result.add(r);
            } else {
                result.addAll(r.getContainedRegions(type));
            }
        }
        return Collections.unmodifiableSet(result);
    }

    public List<Region> getPreferredValues() {
        loadRegionData();
        if (this.type == RegionType.DEPRECATED) {
            return Collections.unmodifiableList(this.preferredValues);
        }
        return null;
    }

    public boolean contains(Region other) {
        loadRegionData();
        if (this.containedRegions.contains(other)) {
            return true;
        }
        for (Region cr : this.containedRegions) {
            if (cr.contains(other)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return this.id;
    }

    public int getNumericCode() {
        return this.code;
    }

    public RegionType getType() {
        return this.type;
    }

    @Override
    public int compareTo(Region other) {
        return this.id.compareTo(other.id);
    }
}
