package android.icu.impl;

import android.icu.impl.TextTrieMap;
import android.icu.text.TimeZoneNames;
import android.icu.util.ULocale;
import android.icu.util.UResourceBundle;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TZDBTimeZoneNames extends TimeZoneNames {
    private static final ConcurrentHashMap<String, TZDBNames> TZDB_NAMES_MAP = new ConcurrentHashMap<>();
    private static volatile TextTrieMap<TZDBNameInfo> TZDB_NAMES_TRIE = null;
    private static final ICUResourceBundle ZONESTRINGS;
    private static final long serialVersionUID = 1;
    private ULocale _locale;
    private volatile transient String _region;

    static {
        UResourceBundle bundle = ICUResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/zone", "tzdbNames");
        ZONESTRINGS = (ICUResourceBundle) bundle.get("zoneStrings");
    }

    public TZDBTimeZoneNames(ULocale loc) {
        this._locale = loc;
    }

    @Override
    public Set<String> getAvailableMetaZoneIDs() {
        return TimeZoneNamesImpl._getAvailableMetaZoneIDs();
    }

    @Override
    public Set<String> getAvailableMetaZoneIDs(String tzID) {
        return TimeZoneNamesImpl._getAvailableMetaZoneIDs(tzID);
    }

    @Override
    public String getMetaZoneID(String tzID, long date) {
        return TimeZoneNamesImpl._getMetaZoneID(tzID, date);
    }

    @Override
    public String getReferenceZoneID(String mzID, String region) {
        return TimeZoneNamesImpl._getReferenceZoneID(mzID, region);
    }

    @Override
    public String getMetaZoneDisplayName(String mzID, TimeZoneNames.NameType type) {
        if (mzID == null || mzID.length() == 0 || !(type == TimeZoneNames.NameType.SHORT_STANDARD || type == TimeZoneNames.NameType.SHORT_DAYLIGHT)) {
            return null;
        }
        return getMetaZoneNames(mzID).getName(type);
    }

    @Override
    public String getTimeZoneDisplayName(String tzID, TimeZoneNames.NameType type) {
        return null;
    }

    @Override
    public Collection<TimeZoneNames.MatchInfo> find(CharSequence text, int start, EnumSet<TimeZoneNames.NameType> nameTypes) {
        if (text == null || text.length() == 0 || start < 0 || start >= text.length()) {
            throw new IllegalArgumentException("bad input text or range");
        }
        prepareFind();
        TZDBNameSearchHandler handler = new TZDBNameSearchHandler(nameTypes, getTargetRegion());
        TZDB_NAMES_TRIE.find(text, start, handler);
        return handler.getMatches();
    }

    private static class TZDBNames {

        private static final int[] f16androidicutextTimeZoneNames$NameTypeSwitchesValues = null;
        public static final TZDBNames EMPTY_TZDBNAMES = new TZDBNames(null, null);
        private static final String[] KEYS = {"ss", "sd"};
        private String[] _names;
        private String[] _parseRegions;

        private static int[] m41getandroidicutextTimeZoneNames$NameTypeSwitchesValues() {
            if (f16androidicutextTimeZoneNames$NameTypeSwitchesValues != null) {
                return f16androidicutextTimeZoneNames$NameTypeSwitchesValues;
            }
            int[] iArr = new int[TimeZoneNames.NameType.valuesCustom().length];
            try {
                iArr[TimeZoneNames.NameType.EXEMPLAR_LOCATION.ordinal()] = 3;
            } catch (NoSuchFieldError e) {
            }
            try {
                iArr[TimeZoneNames.NameType.LONG_DAYLIGHT.ordinal()] = 4;
            } catch (NoSuchFieldError e2) {
            }
            try {
                iArr[TimeZoneNames.NameType.LONG_GENERIC.ordinal()] = 5;
            } catch (NoSuchFieldError e3) {
            }
            try {
                iArr[TimeZoneNames.NameType.LONG_STANDARD.ordinal()] = 6;
            } catch (NoSuchFieldError e4) {
            }
            try {
                iArr[TimeZoneNames.NameType.SHORT_DAYLIGHT.ordinal()] = 1;
            } catch (NoSuchFieldError e5) {
            }
            try {
                iArr[TimeZoneNames.NameType.SHORT_GENERIC.ordinal()] = 7;
            } catch (NoSuchFieldError e6) {
            }
            try {
                iArr[TimeZoneNames.NameType.SHORT_STANDARD.ordinal()] = 2;
            } catch (NoSuchFieldError e7) {
            }
            f16androidicutextTimeZoneNames$NameTypeSwitchesValues = iArr;
            return iArr;
        }

        private TZDBNames(String[] names, String[] parseRegions) {
            this._names = names;
            this._parseRegions = parseRegions;
        }

        static TZDBNames getInstance(ICUResourceBundle zoneStrings, String key) {
            if (zoneStrings == null || key == null || key.length() == 0) {
                return EMPTY_TZDBNAMES;
            }
            try {
                ICUResourceBundle table = (ICUResourceBundle) zoneStrings.get(key);
                boolean isEmpty = true;
                String[] names = new String[KEYS.length];
                for (int i = 0; i < names.length; i++) {
                    try {
                        names[i] = table.getString(KEYS[i]);
                        isEmpty = false;
                    } catch (MissingResourceException e) {
                        names[i] = null;
                    }
                }
                if (isEmpty) {
                    return EMPTY_TZDBNAMES;
                }
                String[] parseRegions = null;
                try {
                    ICUResourceBundle regionsRes = (ICUResourceBundle) table.get("parseRegions");
                    if (regionsRes.getType() == 0) {
                        parseRegions = new String[]{regionsRes.getString()};
                    } else if (regionsRes.getType() == 8) {
                        parseRegions = regionsRes.getStringArray();
                    }
                } catch (MissingResourceException e2) {
                }
                return new TZDBNames(names, parseRegions);
            } catch (MissingResourceException e3) {
                return EMPTY_TZDBNAMES;
            }
        }

        String getName(TimeZoneNames.NameType type) {
            if (this._names == null) {
                return null;
            }
            switch (m41getandroidicutextTimeZoneNames$NameTypeSwitchesValues()[type.ordinal()]) {
                case 1:
                    String name = this._names[1];
                    break;
                case 2:
                    String name2 = this._names[0];
                    break;
            }
            return null;
        }

        String[] getParseRegions() {
            return this._parseRegions;
        }
    }

    private static class TZDBNameInfo {
        boolean ambiguousType;
        String mzID;
        String[] parseRegions;
        TimeZoneNames.NameType type;

        TZDBNameInfo(TZDBNameInfo tZDBNameInfo) {
            this();
        }

        private TZDBNameInfo() {
        }
    }

    private static class TZDBNameSearchHandler implements TextTrieMap.ResultHandler<TZDBNameInfo> {

        static final boolean f15assertionsDisabled;
        private Collection<TimeZoneNames.MatchInfo> _matches;
        private EnumSet<TimeZoneNames.NameType> _nameTypes;
        private String _region;

        static {
            f15assertionsDisabled = !TZDBNameSearchHandler.class.desiredAssertionStatus();
        }

        TZDBNameSearchHandler(EnumSet<TimeZoneNames.NameType> nameTypes, String region) {
            this._nameTypes = nameTypes;
            if (!f15assertionsDisabled) {
                if (!(region != null)) {
                    throw new AssertionError();
                }
            }
            this._region = region;
        }

        @Override
        public boolean handlePrefixMatch(int matchLength, Iterator<TZDBNameInfo> values) {
            TZDBNameInfo match = null;
            TZDBNameInfo tZDBNameInfo = null;
            while (values.hasNext()) {
                TZDBNameInfo ninfo = values.next();
                if (this._nameTypes == null || this._nameTypes.contains(ninfo.type)) {
                    if (ninfo.parseRegions == null) {
                        if (tZDBNameInfo == null) {
                            tZDBNameInfo = ninfo;
                            match = ninfo;
                        }
                    } else {
                        boolean matchRegion = false;
                        String[] strArr = ninfo.parseRegions;
                        int i = 0;
                        int length = strArr.length;
                        while (true) {
                            if (i >= length) {
                                break;
                            }
                            String region = strArr[i];
                            if (!this._region.equals(region)) {
                                i++;
                            } else {
                                match = ninfo;
                                matchRegion = true;
                                break;
                            }
                        }
                        if (matchRegion) {
                            break;
                        }
                        if (match == null) {
                            match = ninfo;
                        }
                    }
                }
            }
            if (match != null) {
                TimeZoneNames.NameType ntype = match.type;
                if (match.ambiguousType && ((ntype == TimeZoneNames.NameType.SHORT_STANDARD || ntype == TimeZoneNames.NameType.SHORT_DAYLIGHT) && this._nameTypes.contains(TimeZoneNames.NameType.SHORT_STANDARD) && this._nameTypes.contains(TimeZoneNames.NameType.SHORT_DAYLIGHT))) {
                    ntype = TimeZoneNames.NameType.SHORT_GENERIC;
                }
                TimeZoneNames.MatchInfo minfo = new TimeZoneNames.MatchInfo(ntype, null, match.mzID, matchLength);
                if (this._matches == null) {
                    this._matches = new LinkedList();
                }
                this._matches.add(minfo);
                return true;
            }
            return true;
        }

        public Collection<TimeZoneNames.MatchInfo> getMatches() {
            if (this._matches == null) {
                return Collections.emptyList();
            }
            return this._matches;
        }
    }

    private static TZDBNames getMetaZoneNames(String mzID) {
        TZDBNames names = TZDB_NAMES_MAP.get(mzID);
        if (names == null) {
            TZDBNames names2 = TZDBNames.getInstance(ZONESTRINGS, "meta:" + mzID);
            TZDBNames tmpNames = TZDB_NAMES_MAP.putIfAbsent(mzID.intern(), names2);
            if (tmpNames != null) {
                return tmpNames;
            }
            return names2;
        }
        return names;
    }

    private static void prepareFind() {
        if (TZDB_NAMES_TRIE != null) {
            return;
        }
        synchronized (TZDBTimeZoneNames.class) {
            if (TZDB_NAMES_TRIE == null) {
                TextTrieMap<TZDBNameInfo> trie = new TextTrieMap<>(true);
                Set<String> mzIDs = TimeZoneNamesImpl._getAvailableMetaZoneIDs();
                for (String mzID : mzIDs) {
                    TZDBNames names = getMetaZoneNames(mzID);
                    String std = names.getName(TimeZoneNames.NameType.SHORT_STANDARD);
                    String dst = names.getName(TimeZoneNames.NameType.SHORT_DAYLIGHT);
                    if (std != null || dst != null) {
                        String[] parseRegions = names.getParseRegions();
                        String mzID2 = mzID.intern();
                        boolean zEquals = (std == null || dst == null) ? false : std.equals(dst);
                        if (std != null) {
                            TZDBNameInfo stdInf = new TZDBNameInfo(null);
                            stdInf.mzID = mzID2;
                            stdInf.type = TimeZoneNames.NameType.SHORT_STANDARD;
                            stdInf.ambiguousType = zEquals;
                            stdInf.parseRegions = parseRegions;
                            trie.put(std, stdInf);
                        }
                        if (dst != null) {
                            TZDBNameInfo dstInf = new TZDBNameInfo(null);
                            dstInf.mzID = mzID2;
                            dstInf.type = TimeZoneNames.NameType.SHORT_DAYLIGHT;
                            dstInf.ambiguousType = zEquals;
                            dstInf.parseRegions = parseRegions;
                            trie.put(dst, dstInf);
                        }
                    }
                }
                TZDB_NAMES_TRIE = trie;
            }
        }
    }

    private String getTargetRegion() {
        if (this._region == null) {
            String region = this._locale.getCountry();
            if (region.length() == 0) {
                ULocale tmp = ULocale.addLikelySubtags(this._locale);
                region = tmp.getCountry();
                if (region.length() == 0) {
                    region = "001";
                }
            }
            this._region = region;
        }
        return this._region;
    }
}
