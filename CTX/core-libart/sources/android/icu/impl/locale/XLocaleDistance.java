package android.icu.impl.locale;

import android.icu.impl.ICUResourceBundle;
import android.icu.impl.Row;
import android.icu.impl.Utility;
import android.icu.impl.locale.XCldrStub;
import android.icu.impl.locale.XLikelySubtags;
import android.icu.text.LocaleDisplayNames;
import android.icu.text.PluralRules;
import android.icu.util.LocaleMatcher;
import android.icu.util.Output;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundleIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class XLocaleDistance {
    static final boolean $assertionsDisabled = false;
    public static final int ABOVE_THRESHOLD = 100;
    private static final Set<String> ALL_FINAL_REGIONS;

    @Deprecated
    public static final String ANY = "�";
    static final XCldrStub.Multimap<String, String> CONTAINER_TO_CONTAINED_FINAL;
    private static final XLocaleDistance DEFAULT;
    static final boolean PRINT_OVERRIDES = false;
    private final int defaultLanguageDistance;
    private final int defaultRegionDistance;
    private final int defaultScriptDistance;
    private final DistanceTable languageDesired2Supported;
    private final RegionMapper regionMapper;
    static final LocaleDisplayNames english = LocaleDisplayNames.getInstance(ULocale.ENGLISH);
    static final XCldrStub.Multimap<String, String> CONTAINER_TO_CONTAINED = xGetContainment();

    public enum DistanceOption {
        NORMAL,
        SCRIPT_FIRST
    }

    private interface IdMapper<K, V> {
        V toId(K k);
    }

    static {
        XCldrStub.TreeMultimap treeMultimapCreate = XCldrStub.TreeMultimap.create();
        for (Map.Entry<String, Set<String>> entry : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
            String key = entry.getKey();
            for (String str : entry.getValue()) {
                if (CONTAINER_TO_CONTAINED.get(str) == null) {
                    treeMultimapCreate.put(key, str);
                }
            }
        }
        CONTAINER_TO_CONTAINED_FINAL = XCldrStub.ImmutableMultimap.copyOf(treeMultimapCreate);
        ALL_FINAL_REGIONS = XCldrStub.ImmutableSet.copyOf(CONTAINER_TO_CONTAINED_FINAL.get("001"));
        String[][] strArr = {new String[]{"ar_*_$maghreb", "ar_*_$maghreb", "96"}, new String[]{"ar_*_$!maghreb", "ar_*_$!maghreb", "96"}, new String[]{"ar_*_*", "ar_*_*", "95"}, new String[]{"en_*_$enUS", "en_*_$enUS", "96"}, new String[]{"en_*_$!enUS", "en_*_$!enUS", "96"}, new String[]{"en_*_*", "en_*_*", "95"}, new String[]{"es_*_$americas", "es_*_$americas", "96"}, new String[]{"es_*_$!americas", "es_*_$!americas", "96"}, new String[]{"es_*_*", "es_*_*", "95"}, new String[]{"pt_*_$americas", "pt_*_$americas", "96"}, new String[]{"pt_*_$!americas", "pt_*_$!americas", "96"}, new String[]{"pt_*_*", "pt_*_*", "95"}, new String[]{"zh_Hant_$cnsar", "zh_Hant_$cnsar", "96"}, new String[]{"zh_Hant_$!cnsar", "zh_Hant_$!cnsar", "96"}, new String[]{"zh_Hant_*", "zh_Hant_*", "95"}, new String[]{"*_*_*", "*_*_*", "96"}};
        RegionMapper.Builder builderAddParadigms = new RegionMapper.Builder().addParadigms("en", "en-GB", "es", "es-419", "pt-BR", "pt-PT");
        for (String[] strArr2 : new String[][]{new String[]{"$enUS", "AS+GU+MH+MP+PR+UM+US+VI"}, new String[]{"$cnsar", "HK+MO"}, new String[]{"$americas", "019"}, new String[]{"$maghreb", "MA+DZ+TN+LY+MR+EH"}}) {
            builderAddParadigms.add(strArr2[0], strArr2[1]);
        }
        StringDistanceTable stringDistanceTable = new StringDistanceTable();
        RegionMapper regionMapperBuild = builderAddParadigms.build();
        XCldrStub.Splitter splitterOn = XCldrStub.Splitter.on('_');
        ArrayList[] arrayListArr = {new ArrayList(), new ArrayList(), new ArrayList()};
        for (Row.R4<String, String, Integer, Boolean> r4 : xGetLanguageMatcherData()) {
            String str2 = r4.get0();
            String str3 = r4.get1();
            List<String> listSplitToList = splitterOn.splitToList(str2);
            List<String> listSplitToList2 = splitterOn.splitToList(str3);
            Boolean bool = r4.get3();
            int iIntValue = str2.equals("*_*") ? 50 : r4.get2().intValue();
            int size = listSplitToList.size();
            if (size != 3) {
                arrayListArr[size - 1].add(Row.of(listSplitToList, listSplitToList2, Integer.valueOf(iIntValue), bool));
            }
        }
        for (ArrayList<Row.R4> arrayList : arrayListArr) {
            for (Row.R4 r42 : arrayList) {
                List list = (List) r42.get0();
                List list2 = (List) r42.get1();
                Integer num = (Integer) r42.get2();
                Boolean bool2 = (Boolean) r42.get3();
                add(stringDistanceTable, list, list2, num.intValue());
                if (bool2 != Boolean.TRUE && !list.equals(list2)) {
                    add(stringDistanceTable, list2, list, num.intValue());
                }
                printMatchXml(list, list2, num, bool2);
            }
        }
        for (String[] strArr3 : strArr) {
            ArrayList arrayList2 = new ArrayList(splitterOn.splitToList(strArr3[0]));
            ArrayList arrayList3 = new ArrayList(splitterOn.splitToList(strArr3[1]));
            Integer numValueOf = Integer.valueOf(100 - Integer.parseInt(strArr3[2]));
            printMatchXml(arrayList2, arrayList3, numValueOf, false);
            Collection<String> idsFromVariable = regionMapperBuild.getIdsFromVariable((String) arrayList2.get(2));
            if (idsFromVariable.isEmpty()) {
                throw new IllegalArgumentException("Bad region variable: " + ((String) arrayList2.get(2)));
            }
            Collection<String> idsFromVariable2 = regionMapperBuild.getIdsFromVariable((String) arrayList3.get(2));
            if (idsFromVariable2.isEmpty()) {
                throw new IllegalArgumentException("Bad region variable: " + ((String) arrayList3.get(2)));
            }
            Iterator<String> it = idsFromVariable.iterator();
            while (it.hasNext()) {
                arrayList2.set(2, it.next().toString());
                Iterator<String> it2 = idsFromVariable2.iterator();
                while (it2.hasNext()) {
                    arrayList3.set(2, it2.next().toString());
                    add(stringDistanceTable, arrayList2, arrayList3, numValueOf.intValue());
                    add(stringDistanceTable, arrayList3, arrayList2, numValueOf.intValue());
                }
            }
        }
        DEFAULT = new XLocaleDistance(stringDistanceTable.compact(), regionMapperBuild);
    }

    private static String fixAny(String str) {
        return "*".equals(str) ? ANY : str;
    }

    private static List<Row.R4<String, String, Integer, Boolean>> xGetLanguageMatcherData() {
        ArrayList arrayList = new ArrayList();
        UResourceBundleIterator iterator = ((ICUResourceBundle) LocaleMatcher.getICUSupplementalData().findTopLevel("languageMatchingNew").get("written")).getIterator();
        while (iterator.hasNext()) {
            ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) iterator.next();
            arrayList.add((Row.R4) Row.of(iCUResourceBundle.getString(0), iCUResourceBundle.getString(1), Integer.valueOf(Integer.parseInt(iCUResourceBundle.getString(2))), Boolean.valueOf(iCUResourceBundle.getSize() > 3 && "1".equals(iCUResourceBundle.getString(3)))).freeze());
        }
        return Collections.unmodifiableList(arrayList);
    }

    private static Set<String> xGetParadigmLocales() {
        return Collections.unmodifiableSet(new HashSet(Arrays.asList(((ICUResourceBundle) LocaleMatcher.getICUSupplementalData().findTopLevel("languageMatchingInfo").get("written").get("paradigmLocales")).getStringArray())));
    }

    private static Map<String, String> xGetMatchVariables() {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) LocaleMatcher.getICUSupplementalData().findTopLevel("languageMatchingInfo").get("written").get("matchVariable");
        HashMap map = new HashMap();
        Enumeration<String> keys = iCUResourceBundle.getKeys();
        while (keys.hasMoreElements()) {
            String strNextElement = keys.nextElement();
            map.put(strNextElement, iCUResourceBundle.getString(strNextElement));
        }
        return Collections.unmodifiableMap(map);
    }

    private static XCldrStub.Multimap<String, String> xGetContainment() {
        XCldrStub.TreeMultimap treeMultimapCreate = XCldrStub.TreeMultimap.create();
        treeMultimapCreate.putAll("001", "019", "002", "150", "142", "009").putAll("011", "BF", "BJ", "CI", "CV", "GH", "GM", "GN", "GW", "LR", "ML", "MR", "NE", "NG", "SH", "SL", "SN", "TG").putAll("013", "BZ", "CR", "GT", "HN", "MX", "NI", "PA", "SV").putAll("014", "BI", "DJ", "ER", "ET", "KE", "KM", "MG", "MU", "MW", "MZ", "RE", "RW", "SC", "SO", "SS", "TZ", "UG", "YT", "ZM", "ZW").putAll("142", "145", "143", "030", "034", "035").putAll("143", "TM", "TJ", "KG", "KZ", "UZ").putAll("145", "AE", "AM", "AZ", "BH", "CY", "GE", "IL", "IQ", "JO", "KW", "LB", "OM", "PS", "QA", "SA", "SY", "TR", "YE", "NT", "YD").putAll("015", "DZ", "EG", "EH", "LY", "MA", "SD", "TN", "EA", "IC").putAll("150", "154", "155", "151", "039").putAll("151", "BG", "BY", "CZ", "HU", "MD", "PL", "RO", "RU", "SK", "UA", "SU").putAll("154", "GG", "IM", "JE", "AX", "DK", "EE", "FI", "FO", "GB", "IE", "IS", "LT", "LV", "NO", "SE", "SJ").putAll("155", "AT", "BE", "CH", "DE", "FR", "LI", "LU", "MC", "NL", "DD", "FX").putAll("017", "AO", "CD", "CF", "CG", "CM", "GA", "GQ", "ST", "TD", "ZR").putAll("018", "BW", "LS", "NA", "SZ", "ZA").putAll("019", "021", "013", "029", "005", "003", "419").putAll("002", "015", "011", "017", "014", "018").putAll("021", "BM", "CA", "GL", "PM", "US").putAll("029", "AG", "AI", "AW", "BB", "BL", "BQ", "BS", "CU", "CW", "DM", "DO", "GD", "GP", "HT", "JM", "KN", "KY", "LC", "MF", "MQ", "MS", "PR", "SX", "TC", "TT", "VC", "VG", "VI", "AN").putAll("003", "021", "013", "029").putAll("030", "CN", "HK", "JP", "KP", "KR", "MN", "MO", "TW").putAll("035", "BN", "ID", "KH", "LA", "MM", "MY", "PH", "SG", "TH", "TL", "VN", "BU", "TP").putAll("039", "AD", "AL", "BA", "ES", "GI", "GR", "HR", "IT", "ME", "MK", "MT", "RS", "PT", "SI", "SM", "VA", "XK", "CS", "YU").putAll("419", "013", "029", "005").putAll("005", "AR", "BO", "BR", "CL", "CO", "EC", "FK", "GF", "GY", "PE", "PY", "SR", "UY", "VE").putAll("053", "AU", "NF", "NZ").putAll("054", "FJ", "NC", "PG", "SB", "VU").putAll("057", "FM", "GU", "KI", "MH", "MP", "NR", "PW").putAll("061", "AS", "CK", "NU", "PF", "PN", "TK", "TO", "TV", "WF", "WS").putAll("034", "AF", "BD", "BT", "IN", "IR", "LK", "MV", "NP", "PK").putAll("009", "053", "054", "057", "061", "QO").putAll("QO", "AQ", "BV", "CC", "CX", "GS", "HM", "IO", "TF", "UM", "AC", "CP", "DG", "TA");
        XCldrStub.TreeMultimap treeMultimapCreate2 = XCldrStub.TreeMultimap.create();
        fill("001", treeMultimapCreate, treeMultimapCreate2);
        return XCldrStub.ImmutableMultimap.copyOf(treeMultimapCreate2);
    }

    private static Set<String> fill(String str, XCldrStub.TreeMultimap<String, String> treeMultimap, XCldrStub.Multimap<String, String> multimap) {
        Set<String> set = treeMultimap.get(str);
        if (set == null) {
            return Collections.emptySet();
        }
        multimap.putAll(str, set);
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            multimap.putAll(str, fill(it.next(), treeMultimap, multimap));
        }
        return multimap.get(str);
    }

    @Deprecated
    public static abstract class DistanceTable {
        abstract Set<String> getCloser(int i);

        abstract int getDistance(String str, String str2, Output<DistanceTable> output, boolean z);

        abstract String toString(boolean z);

        public DistanceTable compact() {
            return this;
        }

        public DistanceNode getInternalNode(String str, String str2) {
            return null;
        }

        public Map<String, Set<String>> getInternalMatches() {
            return null;
        }

        public boolean isEmpty() {
            return true;
        }
    }

    @Deprecated
    public static class DistanceNode {
        final int distance;

        public DistanceNode(int i) {
            this.distance = i;
        }

        public DistanceTable getDistanceTable() {
            return null;
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && obj.getClass() == getClass() && this.distance == ((DistanceNode) obj).distance);
        }

        public int hashCode() {
            return this.distance;
        }

        public String toString() {
            return "\ndistance: " + this.distance;
        }
    }

    static class IdMakerFull<T> implements IdMapper<T, Integer> {
        private final List<T> intToObject;
        final String name;
        private final Map<T, Integer> objectToInt;

        IdMakerFull(String str) {
            this.objectToInt = new HashMap();
            this.intToObject = new ArrayList();
            this.name = str;
        }

        IdMakerFull() {
            this("unnamed");
        }

        IdMakerFull(String str, T t) {
            this(str);
            add(t);
        }

        public Integer add(T t) {
            Integer num = this.objectToInt.get(t);
            if (num == null) {
                Integer numValueOf = Integer.valueOf(this.intToObject.size());
                this.objectToInt.put(t, numValueOf);
                this.intToObject.add(t);
                return numValueOf;
            }
            return num;
        }

        @Override
        public Integer toId(T t) {
            return this.objectToInt.get(t);
        }

        public T fromId(int i) {
            return this.intToObject.get(i);
        }

        public T intern(T t) {
            return fromId(add(t).intValue());
        }

        public int size() {
            return this.intToObject.size();
        }

        public Integer getOldAndAdd(T t) {
            Integer num = this.objectToInt.get(t);
            if (num == null) {
                this.objectToInt.put(t, Integer.valueOf(this.intToObject.size()));
                this.intToObject.add(t);
            }
            return num;
        }

        public String toString() {
            return size() + PluralRules.KEYWORD_RULE_SEPARATOR + this.intToObject;
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && obj.getClass() == getClass() && this.intToObject.equals(((IdMakerFull) obj).intToObject));
        }

        public int hashCode() {
            return this.intToObject.hashCode();
        }
    }

    static class StringDistanceNode extends DistanceNode {
        final DistanceTable distanceTable;

        public StringDistanceNode(int i, DistanceTable distanceTable) {
            super(i);
            this.distanceTable = distanceTable;
        }

        @Override
        public boolean equals(Object obj) {
            if (this != obj) {
                if (obj != null && obj.getClass() == getClass()) {
                    StringDistanceNode stringDistanceNode = (StringDistanceNode) obj;
                    if (this.distance != stringDistanceNode.distance || !Utility.equals(this.distanceTable, stringDistanceNode.distanceTable) || !super.equals(stringDistanceNode)) {
                    }
                }
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            return this.distance ^ Utility.hashCode(this.distanceTable);
        }

        StringDistanceNode(int i) {
            this(i, new StringDistanceTable());
        }

        public void addSubtables(String str, String str2, CopyIfEmpty copyIfEmpty) {
            ((StringDistanceTable) this.distanceTable).addSubtables(str, str2, copyIfEmpty);
        }

        @Override
        public String toString() {
            return "distance: " + this.distance + "\n" + this.distanceTable;
        }

        public void copyTables(StringDistanceTable stringDistanceTable) {
            if (stringDistanceTable != null) {
                ((StringDistanceTable) this.distanceTable).copy(stringDistanceTable);
            }
        }

        @Override
        public DistanceTable getDistanceTable() {
            return this.distanceTable;
        }
    }

    public XLocaleDistance(DistanceTable distanceTable, RegionMapper regionMapper) {
        this.languageDesired2Supported = distanceTable;
        this.regionMapper = regionMapper;
        StringDistanceNode stringDistanceNode = (StringDistanceNode) ((StringDistanceTable) this.languageDesired2Supported).subtables.get(ANY).get(ANY);
        this.defaultLanguageDistance = stringDistanceNode.distance;
        StringDistanceNode stringDistanceNode2 = (StringDistanceNode) ((StringDistanceTable) stringDistanceNode.distanceTable).subtables.get(ANY).get(ANY);
        this.defaultScriptDistance = stringDistanceNode2.distance;
        this.defaultRegionDistance = ((StringDistanceTable) stringDistanceNode2.distanceTable).subtables.get(ANY).get(ANY).distance;
    }

    private static Map newMap() {
        return new TreeMap();
    }

    @Deprecated
    public static class StringDistanceTable extends DistanceTable {
        final Map<String, Map<String, DistanceNode>> subtables;

        StringDistanceTable(Map<String, Map<String, DistanceNode>> map) {
            this.subtables = map;
        }

        StringDistanceTable() {
            this(XLocaleDistance.newMap());
        }

        @Override
        public boolean isEmpty() {
            return this.subtables.isEmpty();
        }

        public boolean equals(Object obj) {
            return this == obj || (obj != null && obj.getClass() == getClass() && this.subtables.equals(((StringDistanceTable) obj).subtables));
        }

        public int hashCode() {
            return this.subtables.hashCode();
        }

        @Override
        public int getDistance(String str, String str2, Output<DistanceTable> output, boolean z) {
            boolean z2;
            Map<String, DistanceNode> map = this.subtables.get(str);
            boolean z3 = true;
            if (map == null) {
                map = this.subtables.get(XLocaleDistance.ANY);
                z2 = true;
            } else {
                z2 = false;
            }
            DistanceNode distanceNode = map.get(str2);
            if (distanceNode == null) {
                DistanceNode distanceNode2 = map.get(XLocaleDistance.ANY);
                if (distanceNode2 != null || z2) {
                    distanceNode = distanceNode2;
                } else {
                    Map<String, DistanceNode> map2 = this.subtables.get(XLocaleDistance.ANY);
                    DistanceNode distanceNode3 = map2.get(str2);
                    if (distanceNode3 == null) {
                        distanceNode2 = map2.get(XLocaleDistance.ANY);
                        distanceNode = distanceNode2;
                    } else {
                        distanceNode = distanceNode3;
                    }
                }
            } else {
                z3 = z2;
            }
            if (output != null) {
                output.value = ((StringDistanceNode) distanceNode).distanceTable;
            }
            if (z && z3 && str.equals(str2)) {
                return 0;
            }
            return distanceNode.distance;
        }

        public void copy(StringDistanceTable stringDistanceTable) {
            for (Map.Entry<String, Map<String, DistanceNode>> entry : stringDistanceTable.subtables.entrySet()) {
                for (Map.Entry<String, DistanceNode> entry2 : entry.getValue().entrySet()) {
                    addSubtable(entry.getKey(), entry2.getKey(), entry2.getValue().distance);
                }
            }
        }

        DistanceNode addSubtable(String str, String str2, int i) {
            Map<String, DistanceNode> map = this.subtables.get(str);
            if (map == null) {
                Map<String, Map<String, DistanceNode>> map2 = this.subtables;
                Map<String, DistanceNode> mapNewMap = XLocaleDistance.newMap();
                map2.put(str, mapNewMap);
                map = mapNewMap;
            }
            DistanceNode distanceNode = map.get(str2);
            if (distanceNode != null) {
                return distanceNode;
            }
            StringDistanceNode stringDistanceNode = new StringDistanceNode(i);
            map.put(str2, stringDistanceNode);
            return stringDistanceNode;
        }

        private DistanceNode getNode(String str, String str2) {
            Map<String, DistanceNode> map = this.subtables.get(str);
            if (map == null) {
                return null;
            }
            return map.get(str2);
        }

        public void addSubtables(String str, String str2, XCldrStub.Predicate<DistanceNode> predicate) {
            DistanceNode distanceNodeAddSubtable;
            DistanceNode node = getNode(str, str2);
            if (node == null) {
                Output<DistanceTable> output = new Output<>();
                distanceNodeAddSubtable = addSubtable(str, str2, getDistance(str, str2, output, true));
                if (output.value != null) {
                    ((StringDistanceNode) distanceNodeAddSubtable).copyTables((StringDistanceTable) output.value);
                }
            } else {
                distanceNodeAddSubtable = node;
            }
            predicate.test(distanceNodeAddSubtable);
        }

        public void addSubtables(String str, String str2, String str3, String str4, int i) {
            for (Map.Entry<String, Map<String, DistanceNode>> entry : this.subtables.entrySet()) {
                boolean zEquals = str.equals(entry.getKey());
                if (zEquals || str.equals(XLocaleDistance.ANY)) {
                    for (Map.Entry<String, DistanceNode> entry2 : entry.getValue().entrySet()) {
                        boolean zEquals2 = str2.equals(entry2.getKey());
                        if (zEquals) {
                        }
                        if (zEquals2 || str2.equals(XLocaleDistance.ANY)) {
                            ((StringDistanceTable) entry2.getValue().getDistanceTable()).addSubtable(str3, str4, i);
                        }
                    }
                }
            }
            StringDistanceTable stringDistanceTable = new StringDistanceTable();
            stringDistanceTable.addSubtable(str3, str4, i);
            addSubtables(str, str2, new CopyIfEmpty(stringDistanceTable));
        }

        public void addSubtables(String str, String str2, String str3, String str4, String str5, String str6, int i) {
            for (Map.Entry<String, Map<String, DistanceNode>> entry : this.subtables.entrySet()) {
                boolean zEquals = str.equals(entry.getKey());
                if (zEquals || str.equals(XLocaleDistance.ANY)) {
                    for (Map.Entry<String, DistanceNode> entry2 : entry.getValue().entrySet()) {
                        boolean zEquals2 = str2.equals(entry2.getKey());
                        if (zEquals) {
                        }
                        if (zEquals2 || str2.equals(XLocaleDistance.ANY)) {
                            ((StringDistanceTable) ((StringDistanceNode) entry2.getValue()).distanceTable).addSubtables(str3, str4, str5, str6, i);
                        }
                    }
                }
            }
            StringDistanceTable stringDistanceTable = new StringDistanceTable();
            stringDistanceTable.addSubtable(str5, str6, i);
            addSubtables(str, str2, new AddSub(str3, str4, stringDistanceTable));
        }

        public String toString() {
            return toString(false);
        }

        @Override
        public String toString(boolean z) {
            return toString(z, "", new IdMakerFull<>("interner"), new StringBuilder()).toString();
        }

        public StringBuilder toString(boolean z, String str, IdMakerFull<Object> idMakerFull, StringBuilder sb) {
            String str2 = str.isEmpty() ? "" : "\t";
            Integer oldAndAdd = z ? idMakerFull.getOldAndAdd(this.subtables) : null;
            if (oldAndAdd != null) {
                sb.append(str2);
                sb.append('#');
                sb.append(oldAndAdd);
                sb.append('\n');
            } else {
                for (Map.Entry<String, Map<String, DistanceNode>> entry : this.subtables.entrySet()) {
                    Map<String, DistanceNode> value = entry.getValue();
                    sb.append(str2);
                    sb.append(entry.getKey());
                    String str3 = "\t";
                    Integer oldAndAdd2 = z ? idMakerFull.getOldAndAdd(value) : null;
                    if (oldAndAdd2 != null) {
                        sb.append("\t");
                        sb.append('#');
                        sb.append(oldAndAdd2);
                        sb.append('\n');
                    } else {
                        for (Map.Entry<String, DistanceNode> entry2 : value.entrySet()) {
                            DistanceNode value2 = entry2.getValue();
                            sb.append(str3);
                            sb.append(entry2.getKey());
                            Integer oldAndAdd3 = z ? idMakerFull.getOldAndAdd(value2) : null;
                            if (oldAndAdd3 != null) {
                                sb.append('\t');
                                sb.append('#');
                                sb.append(oldAndAdd3);
                                sb.append('\n');
                            } else {
                                sb.append('\t');
                                sb.append(value2.distance);
                                Object distanceTable = value2.getDistanceTable();
                                if (distanceTable != null) {
                                    Integer oldAndAdd4 = z ? idMakerFull.getOldAndAdd(distanceTable) : null;
                                    if (oldAndAdd4 != null) {
                                        sb.append('\t');
                                        sb.append('#');
                                        sb.append(oldAndAdd4);
                                        sb.append('\n');
                                    } else {
                                        ((StringDistanceTable) distanceTable).toString(z, str + "\t\t\t", idMakerFull, sb);
                                    }
                                } else {
                                    sb.append('\n');
                                }
                            }
                            str3 = str + '\t';
                        }
                    }
                    str2 = str;
                }
            }
            return sb;
        }

        @Override
        public StringDistanceTable compact() {
            return new CompactAndImmutablizer().compact(this);
        }

        @Override
        public Set<String> getCloser(int i) {
            HashSet hashSet = new HashSet();
            for (Map.Entry<String, Map<String, DistanceNode>> entry : this.subtables.entrySet()) {
                String key = entry.getKey();
                Iterator<Map.Entry<String, DistanceNode>> it = entry.getValue().entrySet().iterator();
                while (true) {
                    if (!it.hasNext()) {
                        break;
                    }
                    if (it.next().getValue().distance < i) {
                        hashSet.add(key);
                        break;
                    }
                }
            }
            return hashSet;
        }

        public Integer getInternalDistance(String str, String str2) {
            DistanceNode distanceNode;
            Map<String, DistanceNode> map = this.subtables.get(str);
            if (map == null || (distanceNode = map.get(str2)) == null) {
                return null;
            }
            return Integer.valueOf(distanceNode.distance);
        }

        @Override
        public DistanceNode getInternalNode(String str, String str2) {
            Map<String, DistanceNode> map = this.subtables.get(str);
            if (map == null) {
                return null;
            }
            return map.get(str2);
        }

        @Override
        public Map<String, Set<String>> getInternalMatches() {
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            for (Map.Entry<String, Map<String, DistanceNode>> entry : this.subtables.entrySet()) {
                linkedHashMap.put(entry.getKey(), new LinkedHashSet(entry.getValue().keySet()));
            }
            return linkedHashMap;
        }
    }

    static class CopyIfEmpty implements XCldrStub.Predicate<DistanceNode> {
        private final StringDistanceTable toCopy;

        CopyIfEmpty(StringDistanceTable stringDistanceTable) {
            this.toCopy = stringDistanceTable;
        }

        @Override
        public boolean test(DistanceNode distanceNode) {
            StringDistanceTable stringDistanceTable = (StringDistanceTable) distanceNode.getDistanceTable();
            if (stringDistanceTable.subtables.isEmpty()) {
                stringDistanceTable.copy(this.toCopy);
                return true;
            }
            return true;
        }
    }

    static class AddSub implements XCldrStub.Predicate<DistanceNode> {
        private final String desiredSub;
        private final CopyIfEmpty r;
        private final String supportedSub;

        AddSub(String str, String str2, StringDistanceTable stringDistanceTable) {
            this.r = new CopyIfEmpty(stringDistanceTable);
            this.desiredSub = str;
            this.supportedSub = str2;
        }

        @Override
        public boolean test(DistanceNode distanceNode) {
            if (distanceNode == null) {
                throw new IllegalArgumentException("bad structure");
            }
            ((StringDistanceNode) distanceNode).addSubtables(this.desiredSub, this.supportedSub, this.r);
            return true;
        }
    }

    public int distance(ULocale uLocale, ULocale uLocale2, int i, DistanceOption distanceOption) {
        return distanceRaw(XLikelySubtags.LSR.fromMaximalized(uLocale), XLikelySubtags.LSR.fromMaximalized(uLocale2), i, distanceOption);
    }

    public int distanceRaw(XLikelySubtags.LSR lsr, XLikelySubtags.LSR lsr2, int i, DistanceOption distanceOption) {
        return distanceRaw(lsr.language, lsr2.language, lsr.script, lsr2.script, lsr.region, lsr2.region, i, distanceOption);
    }

    public int distanceRaw(String str, String str2, String str3, String str4, String str5, String str6, int i, DistanceOption distanceOption) {
        int distance;
        Output<DistanceTable> output = new Output<>();
        int distance2 = this.languageDesired2Supported.getDistance(str, str2, output, true);
        boolean z = distanceOption == DistanceOption.SCRIPT_FIRST;
        if (z) {
            distance2 >>= 2;
        }
        if (distance2 >= 0) {
            if (distance2 >= i) {
                return 100;
            }
        } else {
            distance2 = 0;
        }
        int distance3 = output.value.getDistance(str3, str4, output, true);
        if (z) {
            distance3 >>= 1;
        }
        int i2 = distance2 + distance3;
        if (i2 >= i) {
            return 100;
        }
        if (str5.equals(str6)) {
            return i2;
        }
        String id = this.regionMapper.toId(str5);
        String id2 = this.regionMapper.toId(str6);
        Set<String> setSingleton = id.isEmpty() ? this.regionMapper.macroToPartitions.get(str5) : null;
        Set<String> setSingleton2 = id2.isEmpty() ? this.regionMapper.macroToPartitions.get(str6) : null;
        if (setSingleton != null || setSingleton2 != null) {
            if (setSingleton == null) {
                setSingleton = Collections.singleton(id);
            }
            if (setSingleton2 == null) {
                setSingleton2 = Collections.singleton(id2);
            }
            int i3 = 0;
            for (String str7 : setSingleton) {
                Iterator<String> it = setSingleton2.iterator();
                while (it.hasNext()) {
                    int distance4 = output.value.getDistance(str7, it.next(), null, false);
                    if (i3 < distance4) {
                        i3 = distance4;
                    }
                }
            }
            distance = i3;
        } else {
            distance = output.value.getDistance(id, id2, null, false);
        }
        int i4 = i2 + distance;
        if (i4 >= i) {
            return 100;
        }
        return i4;
    }

    public static XLocaleDistance getDefault() {
        return DEFAULT;
    }

    private static void printMatchXml(List<String> list, List<String> list2, Integer num, Boolean bool) {
    }

    private static String fixedName(List<String> list) {
        ArrayList arrayList = new ArrayList(list);
        int size = arrayList.size();
        StringBuilder sb = new StringBuilder();
        if (size >= 3) {
            String str = (String) arrayList.get(2);
            if (str.equals("*") || str.startsWith("$")) {
                sb.append(str);
            } else {
                sb.append(english.regionDisplayName(str));
            }
        }
        if (size >= 2) {
            String str2 = (String) arrayList.get(1);
            if (str2.equals("*")) {
                sb.insert(0, str2);
            } else {
                sb.insert(0, english.scriptDisplayName(str2));
            }
        }
        if (size >= 1) {
            String str3 = (String) arrayList.get(0);
            if (str3.equals("*")) {
                sb.insert(0, str3);
            } else {
                sb.insert(0, english.languageDisplayName(str3));
            }
        }
        return XCldrStub.CollectionUtilities.join(arrayList, "; ");
    }

    public static void add(StringDistanceTable stringDistanceTable, List<String> list, List<String> list2, int i) {
        int size = list.size();
        if (size != list2.size() || size < 1 || size > 3) {
            throw new IllegalArgumentException();
        }
        String strFixAny = fixAny(list.get(0));
        String strFixAny2 = fixAny(list2.get(0));
        if (size == 1) {
            stringDistanceTable.addSubtable(strFixAny, strFixAny2, i);
            return;
        }
        String strFixAny3 = fixAny(list.get(1));
        String strFixAny4 = fixAny(list2.get(1));
        if (size == 2) {
            stringDistanceTable.addSubtables(strFixAny, strFixAny2, strFixAny3, strFixAny4, i);
        } else {
            stringDistanceTable.addSubtables(strFixAny, strFixAny2, strFixAny3, strFixAny4, fixAny(list.get(2)), fixAny(list2.get(2)), i);
        }
    }

    public String toString() {
        return toString(false);
    }

    public String toString(boolean z) {
        return this.regionMapper + "\n" + this.languageDesired2Supported.toString(z);
    }

    static Set<String> getContainingMacrosFor(Collection<String> collection, Set<String> set) {
        set.clear();
        for (Map.Entry<String, Set<String>> entry : CONTAINER_TO_CONTAINED.asMap().entrySet()) {
            if (collection.containsAll(entry.getValue())) {
                set.add(entry.getKey());
            }
        }
        return set;
    }

    static class RegionMapper implements IdMapper<String, String> {
        final XCldrStub.Multimap<String, String> macroToPartitions;
        final Set<ULocale> paradigms;
        final Map<String, String> regionToPartition;
        final XCldrStub.Multimap<String, String> variableToPartition;

        private RegionMapper(XCldrStub.Multimap<String, String> multimap, Map<String, String> map, XCldrStub.Multimap<String, String> multimap2, Set<ULocale> set) {
            this.variableToPartition = XCldrStub.ImmutableMultimap.copyOf(multimap);
            this.regionToPartition = XCldrStub.ImmutableMap.copyOf(map);
            this.macroToPartitions = XCldrStub.ImmutableMultimap.copyOf(multimap2);
            this.paradigms = XCldrStub.ImmutableSet.copyOf(set);
        }

        @Override
        public String toId(String str) {
            String str2 = this.regionToPartition.get(str);
            return str2 == null ? "" : str2;
        }

        public Collection<String> getIdsFromVariable(String str) {
            if (str.equals("*")) {
                return Collections.singleton("*");
            }
            Set<String> set = this.variableToPartition.get(str);
            if (set == null || set.isEmpty()) {
                throw new IllegalArgumentException("Variable not defined: " + str);
            }
            return set;
        }

        public Set<String> regions() {
            return this.regionToPartition.keySet();
        }

        public Set<String> variables() {
            return this.variableToPartition.keySet();
        }

        public String toString() {
            XCldrStub.TreeMultimap treeMultimap = (XCldrStub.TreeMultimap) XCldrStub.Multimaps.invertFrom(this.variableToPartition, XCldrStub.TreeMultimap.create());
            XCldrStub.TreeMultimap treeMultimapCreate = XCldrStub.TreeMultimap.create();
            for (Map.Entry<String, String> entry : this.regionToPartition.entrySet()) {
                treeMultimapCreate.put(entry.getValue(), entry.getKey());
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Partition ➠ Variables ➠ Regions (final)");
            for (Map.Entry entry2 : treeMultimap.asMap().entrySet()) {
                sb.append('\n');
                sb.append(((String) entry2.getKey()) + "\t" + entry2.getValue() + "\t" + treeMultimapCreate.get((String) entry2.getKey()));
            }
            sb.append("\nMacro ➠ Partitions");
            for (Map.Entry<String, Set<String>> entry3 : this.macroToPartitions.asMap().entrySet()) {
                sb.append('\n');
                sb.append(entry3.getKey() + "\t" + entry3.getValue());
            }
            return sb.toString();
        }

        static class Builder {
            private final XCldrStub.Multimap<String, String> regionToRawPartition = XCldrStub.TreeMultimap.create();
            private final RegionSet regionSet = new RegionSet();
            private final Set<ULocale> paradigms = new LinkedHashSet();

            Builder() {
            }

            void add(String str, String str2) {
                Iterator it = this.regionSet.parseSet(str2).iterator();
                while (it.hasNext()) {
                    this.regionToRawPartition.put((String) it.next(), str);
                }
                Set setInverse = this.regionSet.inverse();
                String str3 = "$!" + str.substring(1);
                Iterator it2 = setInverse.iterator();
                while (it2.hasNext()) {
                    this.regionToRawPartition.put((String) it2.next(), str3);
                }
            }

            public Builder addParadigms(String... strArr) {
                for (String str : strArr) {
                    this.paradigms.add(new ULocale(str));
                }
                return this;
            }

            RegionMapper build() {
                IdMakerFull idMakerFull = new IdMakerFull("partition");
                XCldrStub.TreeMultimap treeMultimapCreate = XCldrStub.TreeMultimap.create();
                TreeMap treeMap = new TreeMap();
                XCldrStub.TreeMultimap treeMultimapCreate2 = XCldrStub.TreeMultimap.create();
                for (Map.Entry<String, Set<String>> entry : this.regionToRawPartition.asMap().entrySet()) {
                    String key = entry.getKey();
                    Set<String> value = entry.getValue();
                    String strValueOf = String.valueOf((char) (945 + idMakerFull.add(value).intValue()));
                    treeMap.put(key, strValueOf);
                    treeMultimapCreate2.put(strValueOf, key);
                    Iterator<String> it = value.iterator();
                    while (it.hasNext()) {
                        treeMultimapCreate.put(it.next(), strValueOf);
                    }
                }
                XCldrStub.TreeMultimap treeMultimapCreate3 = XCldrStub.TreeMultimap.create();
                for (Map.Entry<String, Set<String>> entry2 : XLocaleDistance.CONTAINER_TO_CONTAINED.asMap().entrySet()) {
                    String key2 = entry2.getKey();
                    for (Map.Entry entry3 : treeMultimapCreate2.asMap().entrySet()) {
                        String str = (String) entry3.getKey();
                        if (!Collections.disjoint(entry2.getValue(), (Collection) entry3.getValue())) {
                            treeMultimapCreate3.put(key2, str);
                        }
                    }
                }
                return new RegionMapper(treeMultimapCreate, treeMap, treeMultimapCreate3, this.paradigms);
            }
        }
    }

    private static class RegionSet {
        private Operation operation;
        private final Set<String> tempRegions;

        private enum Operation {
            add,
            remove
        }

        private RegionSet() {
            this.tempRegions = new TreeSet();
            this.operation = null;
        }

        private Set<String> parseSet(String str) {
            this.operation = Operation.add;
            this.tempRegions.clear();
            int i = 0;
            int i2 = 0;
            while (i < str.length()) {
                char cCharAt = str.charAt(i);
                if (cCharAt == '+') {
                    add(str, i2, i);
                    i2 = i + 1;
                    this.operation = Operation.add;
                } else if (cCharAt == '-') {
                    add(str, i2, i);
                    i2 = i + 1;
                    this.operation = Operation.remove;
                }
                i++;
            }
            add(str, i2, i);
            return this.tempRegions;
        }

        private Set<String> inverse() {
            TreeSet treeSet = new TreeSet(XLocaleDistance.ALL_FINAL_REGIONS);
            treeSet.removeAll(this.tempRegions);
            return treeSet;
        }

        private void add(String str, int i, int i2) {
            if (i2 > i) {
                changeSet(this.operation, str.substring(i, i2));
            }
        }

        private void changeSet(Operation operation, String str) {
            Set<String> set = XLocaleDistance.CONTAINER_TO_CONTAINED_FINAL.get(str);
            if (set != null && !set.isEmpty()) {
                if (Operation.add == operation) {
                    this.tempRegions.addAll(set);
                    return;
                } else {
                    this.tempRegions.removeAll(set);
                    return;
                }
            }
            if (Operation.add == operation) {
                this.tempRegions.add(str);
            } else {
                this.tempRegions.remove(str);
            }
        }
    }

    public static <K, V> XCldrStub.Multimap<K, V> invertMap(Map<V, K> map) {
        return XCldrStub.Multimaps.invertFrom(XCldrStub.Multimaps.forMap(map), XCldrStub.LinkedHashMultimap.create());
    }

    public Set<ULocale> getParadigms() {
        return this.regionMapper.paradigms;
    }

    public int getDefaultLanguageDistance() {
        return this.defaultLanguageDistance;
    }

    public int getDefaultScriptDistance() {
        return this.defaultScriptDistance;
    }

    public int getDefaultRegionDistance() {
        return this.defaultRegionDistance;
    }

    static class CompactAndImmutablizer extends IdMakerFull<Object> {
        CompactAndImmutablizer() {
        }

        StringDistanceTable compact(StringDistanceTable stringDistanceTable) {
            if (toId(stringDistanceTable) != null) {
                return (StringDistanceTable) intern(stringDistanceTable);
            }
            return new StringDistanceTable(compact(stringDistanceTable.subtables, 0));
        }

        <K, T> Map<K, T> compact(Map<K, T> map, int i) {
            if (toId(map) != null) {
                return (Map) intern(map);
            }
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            for (Map.Entry<K, T> entry : map.entrySet()) {
                T value = entry.getValue();
                if (value instanceof Map) {
                    linkedHashMap.put(entry.getKey(), compact((Map) value, i + 1));
                } else {
                    linkedHashMap.put(entry.getKey(), compact((DistanceNode) value));
                }
            }
            return XCldrStub.ImmutableMap.copyOf(linkedHashMap);
        }

        DistanceNode compact(DistanceNode distanceNode) {
            if (toId(distanceNode) != null) {
                return (DistanceNode) intern(distanceNode);
            }
            DistanceTable distanceTable = distanceNode.getDistanceTable();
            if (distanceTable == null || distanceTable.isEmpty()) {
                return new DistanceNode(distanceNode.distance);
            }
            return new StringDistanceNode(distanceNode.distance, compact((StringDistanceTable) ((StringDistanceNode) distanceNode).distanceTable));
        }
    }

    @Deprecated
    public StringDistanceTable internalGetDistanceTable() {
        return (StringDistanceTable) this.languageDesired2Supported;
    }

    public static void main(String[] strArr) {
        DistanceTable distanceTable = getDefault().languageDesired2Supported;
        if (!distanceTable.equals(distanceTable.compact())) {
            throw new IllegalArgumentException("Compaction isn't equal");
        }
    }
}
