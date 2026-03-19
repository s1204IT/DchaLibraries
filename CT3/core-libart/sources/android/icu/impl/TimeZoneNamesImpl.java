package android.icu.impl;

import android.icu.impl.TextTrieMap;
import android.icu.impl.UResource;
import android.icu.text.TimeZoneNames;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import libcore.icu.RelativeDateTimeFormatter;

public class TimeZoneNamesImpl extends TimeZoneNames {
    private static volatile Set<String> METAZONE_IDS = null;
    private static final String MZ_PREFIX = "meta:";
    private static final String ZONE_STRINGS_BUNDLE = "zoneStrings";
    private static final long serialVersionUID = -2179814848495897472L;
    private transient ConcurrentHashMap<String, ZNames> _mzNamesMap;
    private transient boolean _namesFullyLoaded;
    private transient TextTrieMap<NameInfo> _namesTrie;
    private transient boolean _namesTrieFullyLoaded;
    private transient ConcurrentHashMap<String, ZNames> _tzNamesMap;
    private transient ICUResourceBundle _zoneStrings;
    private static final TimeZoneNames.NameType[] NAME_TYPE_VALUES = TimeZoneNames.NameType.valuesCustom();
    private static final TZ2MZsCache TZ_TO_MZS_CACHE = new TZ2MZsCache(null);
    private static final MZ2TZsCache MZ_TO_TZS_CACHE = new MZ2TZsCache(0 == true ? 1 : 0);
    private static final Pattern LOC_EXCLUSION_PATTERN = Pattern.compile("Etc/.*|SystemV/.*|.*/Riyadh8[7-9]");

    public TimeZoneNamesImpl(ULocale locale) {
        initialize(locale);
    }

    @Override
    public Set<String> getAvailableMetaZoneIDs() {
        return _getAvailableMetaZoneIDs();
    }

    static Set<String> _getAvailableMetaZoneIDs() {
        if (METAZONE_IDS == null) {
            synchronized (TimeZoneNamesImpl.class) {
                if (METAZONE_IDS == null) {
                    UResourceBundle bundle = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "metaZones");
                    UResourceBundle mapTimezones = bundle.get("mapTimezones");
                    Set<String> keys = mapTimezones.keySet();
                    METAZONE_IDS = Collections.unmodifiableSet(keys);
                }
            }
        }
        return METAZONE_IDS;
    }

    @Override
    public Set<String> getAvailableMetaZoneIDs(String tzID) {
        return _getAvailableMetaZoneIDs(tzID);
    }

    static Set<String> _getAvailableMetaZoneIDs(String tzID) {
        if (tzID == null || tzID.length() == 0) {
            return Collections.emptySet();
        }
        List<MZMapEntry> maps = TZ_TO_MZS_CACHE.getInstance(tzID, tzID);
        if (maps.isEmpty()) {
            return Collections.emptySet();
        }
        Set<String> mzIDs = new HashSet<>(maps.size());
        for (MZMapEntry map : maps) {
            mzIDs.add(map.mzID());
        }
        return Collections.unmodifiableSet(mzIDs);
    }

    @Override
    public String getMetaZoneID(String tzID, long date) {
        return _getMetaZoneID(tzID, date);
    }

    static String _getMetaZoneID(String tzID, long date) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        List<MZMapEntry> maps = TZ_TO_MZS_CACHE.getInstance(tzID, tzID);
        for (MZMapEntry map : maps) {
            if (date >= map.from() && date < map.to()) {
                String mzID = map.mzID();
                return mzID;
            }
        }
        return null;
    }

    @Override
    public String getReferenceZoneID(String mzID, String region) {
        return _getReferenceZoneID(mzID, region);
    }

    static String _getReferenceZoneID(String mzID, String region) {
        if (mzID == null || mzID.length() == 0) {
            return null;
        }
        Map<String, String> regionTzMap = MZ_TO_TZS_CACHE.getInstance(mzID, mzID);
        if (regionTzMap.isEmpty()) {
            return null;
        }
        String refID = regionTzMap.get(region);
        if (refID == null) {
            return regionTzMap.get("001");
        }
        return refID;
    }

    @Override
    public String getMetaZoneDisplayName(String mzID, TimeZoneNames.NameType type) {
        if (mzID == null || mzID.length() == 0) {
            return null;
        }
        return loadMetaZoneNames(null, mzID).getName(type);
    }

    @Override
    public String getTimeZoneDisplayName(String tzID, TimeZoneNames.NameType type) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        return loadTimeZoneNames(null, tzID).getName(type);
    }

    @Override
    public String getExemplarLocationName(String tzID) {
        if (tzID == null || tzID.length() == 0) {
            return null;
        }
        String locName = loadTimeZoneNames(null, tzID).getName(TimeZoneNames.NameType.EXEMPLAR_LOCATION);
        return locName;
    }

    @Override
    public synchronized Collection<TimeZoneNames.MatchInfo> find(CharSequence text, int start, EnumSet<TimeZoneNames.NameType> nameTypes) {
        if (text != null) {
            if (text.length() != 0 && start >= 0 && start < text.length()) {
                NameSearchHandler handler = new NameSearchHandler(nameTypes);
                this._namesTrie.find(text, start, handler);
                if (handler.getMaxMatchLen() == text.length() - start || this._namesTrieFullyLoaded) {
                    return handler.getMatches();
                }
                addAllNamesIntoTrie();
                handler.resetResults();
                this._namesTrie.find(text, start, handler);
                if (handler.getMaxMatchLen() == text.length() - start) {
                    return handler.getMatches();
                }
                internalLoadAllDisplayNames();
                addAllNamesIntoTrie();
                Set<String> tzIDs = TimeZone.getAvailableIDs(TimeZone.SystemTimeZoneType.CANONICAL, null, null);
                for (String tzID : tzIDs) {
                    if (!this._tzNamesMap.containsKey(tzID)) {
                        String tzID2 = tzID.intern();
                        ZNames tznames = ZNames.getInstance(null, tzID2);
                        tznames.addNamesIntoTrie(null, tzID2, this._namesTrie);
                        this._tzNamesMap.put(tzID2, tznames);
                    }
                }
                this._namesTrieFullyLoaded = true;
                handler.resetResults();
                this._namesTrie.find(text, start, handler);
                return handler.getMatches();
            }
        }
        throw new IllegalArgumentException("bad input text or range");
    }

    @Override
    public synchronized void loadAllDisplayNames() {
        internalLoadAllDisplayNames();
    }

    @Override
    public void getDisplayNames(String tzID, TimeZoneNames.NameType[] types, long date, String[] dest, int destOffset) {
        if (tzID == null || tzID.length() == 0) {
            return;
        }
        ZNames tzNames = loadTimeZoneNames(null, tzID);
        ZNames mzNames = null;
        for (int i = 0; i < types.length; i++) {
            TimeZoneNames.NameType type = types[i];
            String name = tzNames.getName(type);
            if (name == null) {
                if (mzNames == null) {
                    String mzID = getMetaZoneID(tzID, date);
                    if (mzID == null || mzID.length() == 0) {
                        mzNames = ZNames.EMPTY_ZNAMES;
                    } else {
                        mzNames = loadMetaZoneNames(null, mzID);
                    }
                }
                name = mzNames.getName(type);
            }
            dest[destOffset + i] = name;
        }
    }

    private void internalLoadAllDisplayNames() {
        if (this._namesFullyLoaded) {
            return;
        }
        new ZoneStringsLoader(this, null).load();
        this._namesFullyLoaded = true;
    }

    private void addAllNamesIntoTrie() {
        for (Map.Entry<String, ZNames> entry : this._tzNamesMap.entrySet()) {
            entry.getValue().addNamesIntoTrie(null, entry.getKey(), this._namesTrie);
        }
        for (Map.Entry<String, ZNames> entry2 : this._mzNamesMap.entrySet()) {
            entry2.getValue().addNamesIntoTrie(entry2.getKey(), null, this._namesTrie);
        }
    }

    private final class ZoneStringsLoader extends UResource.TableSink {
        private static final int INITIAL_NUM_ZONES = 300;
        private HashMap<UResource.Key, ZNamesLoader> keyToLoader;
        private StringBuilder sb;

        ZoneStringsLoader(TimeZoneNamesImpl this$0, ZoneStringsLoader zoneStringsLoader) {
            this();
        }

        private ZoneStringsLoader() {
            this.keyToLoader = new HashMap<>(300);
            this.sb = new StringBuilder(32);
        }

        void load() {
            TimeZoneNamesImpl.this._zoneStrings.getAllTableItemsWithFallback("", this);
            for (Map.Entry<UResource.Key, ZNamesLoader> entry : this.keyToLoader.entrySet()) {
                UResource.Key key = entry.getKey();
                ZNamesLoader loader = entry.getValue();
                if (loader != ZNamesLoader.DUMMY_LOADER) {
                    if (key.startsWith(TimeZoneNamesImpl.MZ_PREFIX)) {
                        String mzID = mzIDFromKey(key).intern();
                        ZNames mzNames = ZNames.getInstance(loader.getNames(), null);
                        TimeZoneNamesImpl.this._mzNamesMap.put(mzID, mzNames);
                    } else {
                        String tzID = tzIDFromKey(key).intern();
                        ZNames tzNames = ZNames.getInstance(loader.getNames(), tzID);
                        TimeZoneNamesImpl.this._tzNamesMap.put(tzID, tzNames);
                    }
                }
            }
        }

        @Override
        public UResource.TableSink getOrCreateTableSink(UResource.Key key, int initialSize) {
            ZNamesLoader loader;
            ZNamesLoader loader2 = this.keyToLoader.get(key);
            if (loader2 != null) {
                if (loader2 == ZNamesLoader.DUMMY_LOADER) {
                    return null;
                }
                return loader2;
            }
            ZNamesLoader result = null;
            if (key.startsWith(TimeZoneNamesImpl.MZ_PREFIX)) {
                String mzID = mzIDFromKey(key);
                if (TimeZoneNamesImpl.this._mzNamesMap.containsKey(mzID)) {
                    loader = ZNamesLoader.DUMMY_LOADER;
                } else {
                    loader = ZNamesLoader.forMetaZoneNames();
                    result = loader;
                }
            } else {
                String tzID = tzIDFromKey(key);
                if (TimeZoneNamesImpl.this._tzNamesMap.containsKey(tzID)) {
                    loader = ZNamesLoader.DUMMY_LOADER;
                } else {
                    loader = ZNamesLoader.forTimeZoneNames();
                    result = loader;
                }
            }
            this.keyToLoader.put(key.m69clone(), loader);
            return result;
        }

        @Override
        public void putNoFallback(UResource.Key key) {
            if (this.keyToLoader.containsKey(key)) {
                return;
            }
            this.keyToLoader.put(key.m69clone(), ZNamesLoader.DUMMY_LOADER);
        }

        private String mzIDFromKey(UResource.Key key) {
            this.sb.setLength(0);
            for (int i = TimeZoneNamesImpl.MZ_PREFIX.length(); i < key.length(); i++) {
                this.sb.append(key.charAt(i));
            }
            return this.sb.toString();
        }

        private String tzIDFromKey(UResource.Key key) {
            this.sb.setLength(0);
            for (int i = 0; i < key.length(); i++) {
                char c = key.charAt(i);
                if (c == ':') {
                    c = '/';
                }
                this.sb.append(c);
            }
            return this.sb.toString();
        }
    }

    private void initialize(ULocale locale) {
        ICUResourceBundle bundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/zone", locale);
        this._zoneStrings = (ICUResourceBundle) bundle.get(ZONE_STRINGS_BUNDLE);
        this._tzNamesMap = new ConcurrentHashMap<>();
        this._mzNamesMap = new ConcurrentHashMap<>();
        this._namesFullyLoaded = false;
        this._namesTrie = new TextTrieMap<>(true);
        this._namesTrieFullyLoaded = false;
        TimeZone tz = TimeZone.getDefault();
        String tzCanonicalID = ZoneMeta.getCanonicalCLDRID(tz);
        if (tzCanonicalID == null) {
            return;
        }
        loadStrings(tzCanonicalID);
    }

    private synchronized void loadStrings(String tzCanonicalID) {
        if (tzCanonicalID != null) {
            if (tzCanonicalID.length() != 0) {
                loadTimeZoneNames(null, tzCanonicalID);
                ZNamesLoader loader = ZNamesLoader.forMetaZoneNames();
                Set<String> mzIDs = getAvailableMetaZoneIDs(tzCanonicalID);
                for (String mzID : mzIDs) {
                    loadMetaZoneNames(loader, mzID);
                }
                addAllNamesIntoTrie();
            }
        }
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        ULocale locale = this._zoneStrings.getULocale();
        out.writeObject(locale);
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        ULocale locale = (ULocale) in.readObject();
        initialize(locale);
    }

    private synchronized ZNames loadMetaZoneNames(ZNamesLoader loader, String mzID) {
        ZNames znames;
        znames = this._mzNamesMap.get(mzID);
        if (znames == null) {
            if (loader == null) {
                loader = ZNamesLoader.forMetaZoneNames();
            }
            znames = ZNames.getInstance(loader, this._zoneStrings, MZ_PREFIX + mzID, null);
            String mzID2 = mzID.intern();
            if (this._namesTrieFullyLoaded) {
                znames.addNamesIntoTrie(mzID2, null, this._namesTrie);
            }
            this._mzNamesMap.put(mzID2, znames);
        }
        return znames;
    }

    private synchronized ZNames loadTimeZoneNames(ZNamesLoader loader, String tzID) {
        ZNames tznames;
        tznames = this._tzNamesMap.get(tzID);
        if (tznames == null) {
            if (loader == null) {
                loader = ZNamesLoader.forTimeZoneNames();
            }
            tznames = ZNames.getInstance(loader, this._zoneStrings, tzID.replace('/', ':'), tzID);
            String tzID2 = tzID.intern();
            if (this._namesTrieFullyLoaded) {
                tznames.addNamesIntoTrie(null, tzID2, this._namesTrie);
            }
            this._tzNamesMap.put(tzID2, tznames);
        }
        return tznames;
    }

    private static class NameInfo {
        String mzID;
        TimeZoneNames.NameType type;
        String tzID;

        NameInfo(NameInfo nameInfo) {
            this();
        }

        private NameInfo() {
        }
    }

    private static class NameSearchHandler implements TextTrieMap.ResultHandler<NameInfo> {

        static final boolean f20assertionsDisabled;
        private Collection<TimeZoneNames.MatchInfo> _matches;
        private int _maxMatchLen;
        private EnumSet<TimeZoneNames.NameType> _nameTypes;

        static {
            f20assertionsDisabled = !NameSearchHandler.class.desiredAssertionStatus();
        }

        NameSearchHandler(EnumSet<TimeZoneNames.NameType> nameTypes) {
            this._nameTypes = nameTypes;
        }

        @Override
        public boolean handlePrefixMatch(int matchLength, Iterator<NameInfo> values) {
            TimeZoneNames.MatchInfo minfo;
            while (values.hasNext()) {
                NameInfo ninfo = values.next();
                if (this._nameTypes == null || this._nameTypes.contains(ninfo.type)) {
                    if (ninfo.tzID != null) {
                        minfo = new TimeZoneNames.MatchInfo(ninfo.type, ninfo.tzID, null, matchLength);
                    } else {
                        if (!f20assertionsDisabled) {
                            if (!(ninfo.mzID != null)) {
                                throw new AssertionError();
                            }
                        }
                        minfo = new TimeZoneNames.MatchInfo(ninfo.type, null, ninfo.mzID, matchLength);
                    }
                    if (this._matches == null) {
                        this._matches = new LinkedList();
                    }
                    this._matches.add(minfo);
                    if (matchLength > this._maxMatchLen) {
                        this._maxMatchLen = matchLength;
                    }
                }
            }
            return true;
        }

        public Collection<TimeZoneNames.MatchInfo> getMatches() {
            if (this._matches == null) {
                return Collections.emptyList();
            }
            return this._matches;
        }

        public int getMaxMatchLen() {
            return this._maxMatchLen;
        }

        public void resetResults() {
            this._matches = null;
            this._maxMatchLen = 0;
        }
    }

    private static final class ZNamesLoader extends UResource.TableSink {
        private String[] names;
        private int numNames;
        private static int NUM_META_ZONE_NAMES = 6;
        private static int NUM_TIME_ZONE_NAMES = 7;
        private static String NO_NAME = "";
        private static ZNamesLoader DUMMY_LOADER = new ZNamesLoader(0);

        private ZNamesLoader(int numNames) {
            this.numNames = numNames;
        }

        static ZNamesLoader forMetaZoneNames() {
            return new ZNamesLoader(NUM_META_ZONE_NAMES);
        }

        static ZNamesLoader forTimeZoneNames() {
            return new ZNamesLoader(NUM_TIME_ZONE_NAMES);
        }

        String[] load(ICUResourceBundle zoneStrings, String key) {
            if (zoneStrings == null || key == null || key.length() == 0) {
                return null;
            }
            try {
                zoneStrings.getAllTableItemsWithFallback(key, this);
                return getNames();
            } catch (MissingResourceException e) {
                return null;
            }
        }

        private static TimeZoneNames.NameType nameTypeFromKey(UResource.Key key) {
            if (key.length() != 2) {
                return null;
            }
            char c0 = key.charAt(0);
            char c1 = key.charAt(1);
            if (c0 == 'l') {
                if (c1 == 'g') {
                    return TimeZoneNames.NameType.LONG_GENERIC;
                }
                if (c1 == 's') {
                    return TimeZoneNames.NameType.LONG_STANDARD;
                }
                if (c1 == 'd') {
                    return TimeZoneNames.NameType.LONG_DAYLIGHT;
                }
                return null;
            }
            if (c0 == 's') {
                if (c1 == 'g') {
                    return TimeZoneNames.NameType.SHORT_GENERIC;
                }
                if (c1 == 's') {
                    return TimeZoneNames.NameType.SHORT_STANDARD;
                }
                if (c1 == 'd') {
                    return TimeZoneNames.NameType.SHORT_DAYLIGHT;
                }
                return null;
            }
            if (c0 == 'e' && c1 == 'c') {
                return TimeZoneNames.NameType.EXEMPLAR_LOCATION;
            }
            return null;
        }

        @Override
        public void put(UResource.Key key, UResource.Value value) {
            if (value.getType() != 0) {
                return;
            }
            if (this.names == null) {
                this.names = new String[this.numNames];
            }
            TimeZoneNames.NameType type = nameTypeFromKey(key);
            if (type == null || type.ordinal() >= this.numNames || this.names[type.ordinal()] != null) {
                return;
            }
            this.names[type.ordinal()] = value.getString();
        }

        @Override
        public void putNoFallback(UResource.Key key) {
            if (this.names == null) {
                this.names = new String[this.numNames];
            }
            TimeZoneNames.NameType type = nameTypeFromKey(key);
            if (type == null || type.ordinal() >= this.numNames || this.names[type.ordinal()] != null) {
                return;
            }
            this.names[type.ordinal()] = NO_NAME;
        }

        private String[] getNames() {
            if (this.names == null) {
                return null;
            }
            int length = 0;
            for (int i = 0; i < this.numNames; i++) {
                String name = this.names[i];
                if (name != null) {
                    if (name == NO_NAME) {
                        this.names[i] = null;
                    } else {
                        length = i + 1;
                    }
                }
            }
            if (length == 0) {
                return null;
            }
            if (length == this.numNames || this.numNames == NUM_TIME_ZONE_NAMES) {
                String[] result = this.names;
                this.names = null;
                return result;
            }
            String[] result2 = new String[length];
            do {
                length--;
                result2[length] = this.names[length];
                this.names[length] = null;
            } while (length > 0);
            return result2;
        }
    }

    private static class ZNames {
        private static final ZNames EMPTY_ZNAMES = new ZNames(null);
        private static final int EX_LOC_INDEX = TimeZoneNames.NameType.EXEMPLAR_LOCATION.ordinal();
        private String[] _names;
        private boolean didAddIntoTrie;

        protected ZNames(String[] names) {
            this._names = names;
            this.didAddIntoTrie = names == null;
        }

        public static ZNames getInstance(String[] names, String tzID) {
            String locationName;
            if (tzID != null && ((names == null || names[EX_LOC_INDEX] == null) && (locationName = TimeZoneNamesImpl.getDefaultExemplarLocationName(tzID)) != null)) {
                if (names == null) {
                    names = new String[EX_LOC_INDEX + 1];
                }
                names[EX_LOC_INDEX] = locationName;
            }
            if (names == null) {
                return EMPTY_ZNAMES;
            }
            return new ZNames(names);
        }

        public static ZNames getInstance(ZNamesLoader loader, ICUResourceBundle zoneStrings, String key, String tzID) {
            return getInstance(loader.load(zoneStrings, key), tzID);
        }

        public String getName(TimeZoneNames.NameType type) {
            if (this._names == null || type.ordinal() >= this._names.length) {
                return null;
            }
            return this._names[type.ordinal()];
        }

        public void addNamesIntoTrie(String mzID, String tzID, TextTrieMap<NameInfo> trie) {
            NameInfo nameInfo = null;
            if (this._names == null || this.didAddIntoTrie) {
                return;
            }
            for (int i = 0; i < this._names.length; i++) {
                String name = this._names[i];
                if (name != null) {
                    NameInfo info = new NameInfo(nameInfo);
                    info.mzID = mzID;
                    info.tzID = tzID;
                    info.type = TimeZoneNamesImpl.NAME_TYPE_VALUES[i];
                    trie.put(name, info);
                }
            }
            this.didAddIntoTrie = true;
        }
    }

    private static class MZMapEntry {
        private long _from;
        private String _mzID;
        private long _to;

        MZMapEntry(String mzID, long from, long to) {
            this._mzID = mzID;
            this._from = from;
            this._to = to;
        }

        String mzID() {
            return this._mzID;
        }

        long from() {
            return this._from;
        }

        long to() {
            return this._to;
        }
    }

    private static class TZ2MZsCache extends SoftCache<String, List<MZMapEntry>, String> {
        TZ2MZsCache(TZ2MZsCache tZ2MZsCache) {
            this();
        }

        private TZ2MZsCache() {
        }

        @Override
        protected List<MZMapEntry> createInstance(String key, String data) {
            UResourceBundle bundle = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "metaZones");
            UResourceBundle metazoneInfoBundle = bundle.get("metazoneInfo");
            String tzkey = data.replace('/', ':');
            try {
                UResourceBundle zoneBundle = metazoneInfoBundle.get(tzkey);
                List<MZMapEntry> mzMaps = new ArrayList<>(zoneBundle.getSize());
                for (int idx = 0; idx < zoneBundle.getSize(); idx++) {
                    try {
                        UResourceBundle mz = zoneBundle.get(idx);
                        String mzid = mz.getString(0);
                        String fromStr = "1970-01-01 00:00";
                        String toStr = "9999-12-31 23:59";
                        if (mz.getSize() == 3) {
                            fromStr = mz.getString(1);
                            toStr = mz.getString(2);
                        }
                        long from = parseDate(fromStr);
                        long to = parseDate(toStr);
                        mzMaps.add(new MZMapEntry(mzid, from, to));
                    } catch (MissingResourceException e) {
                        return Collections.emptyList();
                    }
                }
                return mzMaps;
            } catch (MissingResourceException e2) {
            }
        }

        private static long parseDate(String text) {
            int year = 0;
            int month = 0;
            int day = 0;
            int hour = 0;
            int min = 0;
            for (int idx = 0; idx <= 3; idx++) {
                int n = text.charAt(idx) - '0';
                if (n >= 0 && n < 10) {
                    year = (year * 10) + n;
                } else {
                    throw new IllegalArgumentException("Bad year");
                }
            }
            for (int idx2 = 5; idx2 <= 6; idx2++) {
                int n2 = text.charAt(idx2) - '0';
                if (n2 >= 0 && n2 < 10) {
                    month = (month * 10) + n2;
                } else {
                    throw new IllegalArgumentException("Bad month");
                }
            }
            for (int idx3 = 8; idx3 <= 9; idx3++) {
                int n3 = text.charAt(idx3) - '0';
                if (n3 >= 0 && n3 < 10) {
                    day = (day * 10) + n3;
                } else {
                    throw new IllegalArgumentException("Bad day");
                }
            }
            for (int idx4 = 11; idx4 <= 12; idx4++) {
                int n4 = text.charAt(idx4) - '0';
                if (n4 >= 0 && n4 < 10) {
                    hour = (hour * 10) + n4;
                } else {
                    throw new IllegalArgumentException("Bad hour");
                }
            }
            for (int idx5 = 14; idx5 <= 15; idx5++) {
                int n5 = text.charAt(idx5) - '0';
                if (n5 >= 0 && n5 < 10) {
                    min = (min * 10) + n5;
                } else {
                    throw new IllegalArgumentException("Bad minute");
                }
            }
            long date = (Grego.fieldsToDay(year, month - 1, day) * 86400000) + (((long) hour) * RelativeDateTimeFormatter.HOUR_IN_MILLIS) + (((long) min) * RelativeDateTimeFormatter.MINUTE_IN_MILLIS);
            return date;
        }
    }

    private static class MZ2TZsCache extends SoftCache<String, Map<String, String>, String> {
        MZ2TZsCache(MZ2TZsCache mZ2TZsCache) {
            this();
        }

        private MZ2TZsCache() {
        }

        @Override
        protected Map<String, String> createInstance(String key, String data) {
            UResourceBundle regionMap;
            Set<String> regions;
            Map<String, String> map;
            UResourceBundle bundle = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b", "metaZones");
            UResourceBundle mapTimezones = bundle.get("mapTimezones");
            try {
                regionMap = mapTimezones.get(key);
                regions = regionMap.keySet();
                map = new HashMap<>(regions.size());
            } catch (MissingResourceException e) {
            }
            try {
                for (String region : regions) {
                    String tzID = regionMap.getString(region).intern();
                    map.put(region.intern(), tzID);
                }
                return map;
            } catch (MissingResourceException e2) {
                return Collections.emptyMap();
            }
        }
    }

    public static String getDefaultExemplarLocationName(String tzID) {
        int sep;
        if (tzID == null || tzID.length() == 0 || LOC_EXCLUSION_PATTERN.matcher(tzID).matches() || (sep = tzID.lastIndexOf(47)) <= 0 || sep + 1 >= tzID.length()) {
            return null;
        }
        String location = tzID.substring(sep + 1).replace('_', ' ');
        return location;
    }
}
