package android.icu.impl;

import android.icu.impl.TextTrieMap;
import android.icu.text.TimeZoneNames;
import android.icu.util.ULocale;
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
    private static final ICUResourceBundle ZONESTRINGS = (ICUResourceBundle) ICUResourceBundle.getBundleInstance(ICUData.ICU_ZONE_BASE_NAME, "tzdbNames").get("zoneStrings");
    private static final long serialVersionUID = 1;
    private ULocale _locale;
    private volatile transient String _region;

    public TZDBTimeZoneNames(ULocale uLocale) {
        this._locale = uLocale;
    }

    @Override
    public Set<String> getAvailableMetaZoneIDs() {
        return TimeZoneNamesImpl._getAvailableMetaZoneIDs();
    }

    @Override
    public Set<String> getAvailableMetaZoneIDs(String str) {
        return TimeZoneNamesImpl._getAvailableMetaZoneIDs(str);
    }

    @Override
    public String getMetaZoneID(String str, long j) {
        return TimeZoneNamesImpl._getMetaZoneID(str, j);
    }

    @Override
    public String getReferenceZoneID(String str, String str2) {
        return TimeZoneNamesImpl._getReferenceZoneID(str, str2);
    }

    @Override
    public String getMetaZoneDisplayName(String str, TimeZoneNames.NameType nameType) {
        if (str == null || str.length() == 0) {
            return null;
        }
        if (nameType != TimeZoneNames.NameType.SHORT_STANDARD && nameType != TimeZoneNames.NameType.SHORT_DAYLIGHT) {
            return null;
        }
        return getMetaZoneNames(str).getName(nameType);
    }

    @Override
    public String getTimeZoneDisplayName(String str, TimeZoneNames.NameType nameType) {
        return null;
    }

    @Override
    public Collection<TimeZoneNames.MatchInfo> find(CharSequence charSequence, int i, EnumSet<TimeZoneNames.NameType> enumSet) {
        if (charSequence == null || charSequence.length() == 0 || i < 0 || i >= charSequence.length()) {
            throw new IllegalArgumentException("bad input text or range");
        }
        prepareFind();
        TZDBNameSearchHandler tZDBNameSearchHandler = new TZDBNameSearchHandler(enumSet, getTargetRegion());
        TZDB_NAMES_TRIE.find(charSequence, i, tZDBNameSearchHandler);
        return tZDBNameSearchHandler.getMatches();
    }

    private static class TZDBNames {
        public static final TZDBNames EMPTY_TZDBNAMES = new TZDBNames(null, null);
        private static final String[] KEYS = {"ss", "sd"};
        private String[] _names;
        private String[] _parseRegions;

        private TZDBNames(String[] strArr, String[] strArr2) {
            this._names = strArr;
            this._parseRegions = strArr2;
        }

        static TZDBNames getInstance(ICUResourceBundle iCUResourceBundle, String str) {
            String[] stringArray;
            if (iCUResourceBundle == null || str == null || str.length() == 0) {
                return EMPTY_TZDBNAMES;
            }
            try {
                ICUResourceBundle iCUResourceBundle2 = (ICUResourceBundle) iCUResourceBundle.get(str);
                String[] strArr = new String[KEYS.length];
                boolean z = true;
                for (int i = 0; i < strArr.length; i++) {
                    try {
                        strArr[i] = iCUResourceBundle2.getString(KEYS[i]);
                        z = false;
                    } catch (MissingResourceException e) {
                        strArr[i] = null;
                    }
                }
                if (z) {
                    return EMPTY_TZDBNAMES;
                }
                try {
                    ICUResourceBundle iCUResourceBundle3 = (ICUResourceBundle) iCUResourceBundle2.get("parseRegions");
                    if (iCUResourceBundle3.getType() == 0) {
                        String[] strArr2 = new String[1];
                        try {
                            strArr2[0] = iCUResourceBundle3.getString();
                            stringArray = strArr2;
                        } catch (MissingResourceException e2) {
                            stringArray = strArr2;
                        }
                    } else {
                        stringArray = iCUResourceBundle3.getType() == 8 ? iCUResourceBundle3.getStringArray() : null;
                    }
                } catch (MissingResourceException e3) {
                    stringArray = null;
                }
                return new TZDBNames(strArr, stringArray);
            } catch (MissingResourceException e4) {
                return EMPTY_TZDBNAMES;
            }
        }

        String getName(TimeZoneNames.NameType nameType) {
            if (this._names == null) {
                return null;
            }
            switch (nameType) {
                case SHORT_STANDARD:
                    return this._names[0];
                case SHORT_DAYLIGHT:
                    return this._names[1];
                default:
                    return null;
            }
        }

        String[] getParseRegions() {
            return this._parseRegions;
        }
    }

    private static class TZDBNameInfo {
        final boolean ambiguousType;
        final String mzID;
        final String[] parseRegions;
        final TimeZoneNames.NameType type;

        TZDBNameInfo(String str, TimeZoneNames.NameType nameType, boolean z, String[] strArr) {
            this.mzID = str;
            this.type = nameType;
            this.ambiguousType = z;
            this.parseRegions = strArr;
        }
    }

    private static class TZDBNameSearchHandler implements TextTrieMap.ResultHandler<TZDBNameInfo> {
        static final boolean $assertionsDisabled = false;
        private Collection<TimeZoneNames.MatchInfo> _matches;
        private EnumSet<TimeZoneNames.NameType> _nameTypes;
        private String _region;

        TZDBNameSearchHandler(EnumSet<TimeZoneNames.NameType> enumSet, String str) {
            this._nameTypes = enumSet;
            this._region = str;
        }

        @Override
        public boolean handlePrefixMatch(int i, Iterator<TZDBNameInfo> it) {
            TZDBNameInfo tZDBNameInfo = null;
            TZDBNameInfo tZDBNameInfo2 = null;
            while (it.hasNext()) {
                TZDBNameInfo next = it.next();
                if (this._nameTypes == null || this._nameTypes.contains(next.type)) {
                    if (next.parseRegions == null) {
                        if (tZDBNameInfo == null) {
                            tZDBNameInfo = next;
                            tZDBNameInfo2 = tZDBNameInfo;
                        }
                    } else {
                        String[] strArr = next.parseRegions;
                        int length = strArr.length;
                        boolean z = false;
                        int i2 = 0;
                        while (true) {
                            if (i2 >= length) {
                                break;
                            }
                            if (!this._region.equals(strArr[i2])) {
                                i2++;
                            } else {
                                tZDBNameInfo2 = next;
                                z = true;
                                break;
                            }
                        }
                        if (z) {
                            break;
                        }
                        if (tZDBNameInfo2 == null) {
                            tZDBNameInfo2 = next;
                        }
                    }
                }
            }
            if (tZDBNameInfo2 != null) {
                TimeZoneNames.NameType nameType = tZDBNameInfo2.type;
                if (tZDBNameInfo2.ambiguousType && ((nameType == TimeZoneNames.NameType.SHORT_STANDARD || nameType == TimeZoneNames.NameType.SHORT_DAYLIGHT) && this._nameTypes.contains(TimeZoneNames.NameType.SHORT_STANDARD) && this._nameTypes.contains(TimeZoneNames.NameType.SHORT_DAYLIGHT))) {
                    nameType = TimeZoneNames.NameType.SHORT_GENERIC;
                }
                TimeZoneNames.MatchInfo matchInfo = new TimeZoneNames.MatchInfo(nameType, null, tZDBNameInfo2.mzID, i);
                if (this._matches == null) {
                    this._matches = new LinkedList();
                }
                this._matches.add(matchInfo);
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

    private static TZDBNames getMetaZoneNames(String str) {
        TZDBNames tZDBNames = TZDB_NAMES_MAP.get(str);
        if (tZDBNames != null) {
            return tZDBNames;
        }
        TZDBNames tZDBNames2 = TZDBNames.getInstance(ZONESTRINGS, "meta:" + str);
        TZDBNames tZDBNamesPutIfAbsent = TZDB_NAMES_MAP.putIfAbsent(str.intern(), tZDBNames2);
        return tZDBNamesPutIfAbsent == null ? tZDBNames2 : tZDBNamesPutIfAbsent;
    }

    private static void prepareFind() {
        boolean z;
        if (TZDB_NAMES_TRIE == null) {
            synchronized (TZDBTimeZoneNames.class) {
                if (TZDB_NAMES_TRIE == null) {
                    TextTrieMap<TZDBNameInfo> textTrieMap = new TextTrieMap<>(true);
                    for (String str : TimeZoneNamesImpl._getAvailableMetaZoneIDs()) {
                        TZDBNames metaZoneNames = getMetaZoneNames(str);
                        String name = metaZoneNames.getName(TimeZoneNames.NameType.SHORT_STANDARD);
                        String name2 = metaZoneNames.getName(TimeZoneNames.NameType.SHORT_DAYLIGHT);
                        if (name != null || name2 != null) {
                            String[] parseRegions = metaZoneNames.getParseRegions();
                            String strIntern = str.intern();
                            if (name == null || name2 == null || !name.equals(name2)) {
                                z = false;
                            } else {
                                z = true;
                            }
                            if (name != null) {
                                textTrieMap.put(name, new TZDBNameInfo(strIntern, TimeZoneNames.NameType.SHORT_STANDARD, z, parseRegions));
                            }
                            if (name2 != null) {
                                textTrieMap.put(name2, new TZDBNameInfo(strIntern, TimeZoneNames.NameType.SHORT_DAYLIGHT, z, parseRegions));
                            }
                        }
                    }
                    TZDB_NAMES_TRIE = textTrieMap;
                }
            }
        }
    }

    private String getTargetRegion() {
        if (this._region == null) {
            String country = this._locale.getCountry();
            if (country.length() == 0) {
                country = ULocale.addLikelySubtags(this._locale).getCountry();
                if (country.length() == 0) {
                    country = "001";
                }
            }
            this._region = country;
        }
        return this._region;
    }
}
