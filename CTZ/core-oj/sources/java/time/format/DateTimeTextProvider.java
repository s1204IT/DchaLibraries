package java.time.format;

import android.icu.impl.ICUResourceBundle;
import android.icu.util.UResourceBundle;
import java.time.chrono.Chronology;
import java.time.chrono.IsoChronology;
import java.time.chrono.JapaneseChronology;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalField;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import sun.util.locale.provider.CalendarDataUtility;

class DateTimeTextProvider {
    private static final ConcurrentMap<Map.Entry<TemporalField, Locale>, Object> CACHE = new ConcurrentHashMap(16, 0.75f, 2);
    private static final Comparator<Map.Entry<String, Long>> COMPARATOR = new Comparator<Map.Entry<String, Long>>() {
        @Override
        public int compare(Map.Entry<String, Long> entry, Map.Entry<String, Long> entry2) {
            return entry2.getKey().length() - entry.getKey().length();
        }
    };

    DateTimeTextProvider() {
    }

    static DateTimeTextProvider getInstance() {
        return new DateTimeTextProvider();
    }

    public String getText(TemporalField temporalField, long j, TextStyle textStyle, Locale locale) {
        Object objFindStore = findStore(temporalField, locale);
        if (objFindStore instanceof LocaleStore) {
            return ((LocaleStore) objFindStore).getText(j, textStyle);
        }
        return null;
    }

    public String getText(Chronology chronology, TemporalField temporalField, long j, TextStyle textStyle, Locale locale) {
        int i;
        if (chronology == IsoChronology.INSTANCE || !(temporalField instanceof ChronoField)) {
            return getText(temporalField, j, textStyle, locale);
        }
        int i2 = 7;
        int i3 = 0;
        if (temporalField == ChronoField.ERA) {
            if (chronology != JapaneseChronology.INSTANCE) {
                i = (int) j;
            } else if (j == -999) {
                i2 = 0;
            } else {
                i = ((int) j) + 2;
            }
            i2 = 0;
            i3 = i;
        } else if (temporalField == ChronoField.MONTH_OF_YEAR) {
            i3 = ((int) j) - 1;
            i2 = 2;
        } else if (temporalField == ChronoField.DAY_OF_WEEK) {
            i3 = ((int) j) + 1;
            if (i3 > 7) {
                i3 = 1;
            }
        } else {
            if (temporalField != ChronoField.AMPM_OF_DAY) {
                return null;
            }
            i2 = 9;
            i3 = (int) j;
        }
        return CalendarDataUtility.retrieveJavaTimeFieldValueName(chronology.getCalendarType(), i2, i3, textStyle.toCalendarStyle(), locale);
    }

    public Iterator<Map.Entry<String, Long>> getTextIterator(TemporalField temporalField, TextStyle textStyle, Locale locale) {
        Object objFindStore = findStore(temporalField, locale);
        if (objFindStore instanceof LocaleStore) {
            return ((LocaleStore) objFindStore).getTextIterator(textStyle);
        }
        return null;
    }

    public Iterator<Map.Entry<String, Long>> getTextIterator(Chronology chronology, TemporalField temporalField, TextStyle textStyle, Locale locale) {
        int i;
        if (chronology == IsoChronology.INSTANCE || !(temporalField instanceof ChronoField)) {
            return getTextIterator(temporalField, textStyle, locale);
        }
        switch ((ChronoField) temporalField) {
            case ERA:
                i = 0;
                break;
            case MONTH_OF_YEAR:
                i = 2;
                break;
            case DAY_OF_WEEK:
                i = 7;
                break;
            case AMPM_OF_DAY:
                i = 9;
                break;
            default:
                return null;
        }
        Map<String, Integer> mapRetrieveJavaTimeFieldValueNames = CalendarDataUtility.retrieveJavaTimeFieldValueNames(chronology.getCalendarType(), i, textStyle != null ? textStyle.toCalendarStyle() : 0, locale);
        if (mapRetrieveJavaTimeFieldValueNames == null) {
            return null;
        }
        ArrayList arrayList = new ArrayList(mapRetrieveJavaTimeFieldValueNames.size());
        if (i == 0) {
            for (Map.Entry<String, Integer> entry : mapRetrieveJavaTimeFieldValueNames.entrySet()) {
                int iIntValue = entry.getValue().intValue();
                if (chronology == JapaneseChronology.INSTANCE) {
                    iIntValue = iIntValue == 0 ? -999 : iIntValue - 2;
                }
                arrayList.add(createEntry(entry.getKey(), Long.valueOf(iIntValue)));
            }
        } else if (i == 2) {
            Iterator<Map.Entry<String, Integer>> it = mapRetrieveJavaTimeFieldValueNames.entrySet().iterator();
            while (it.hasNext()) {
                arrayList.add(createEntry(it.next().getKey(), Long.valueOf(r6.getValue().intValue() + 1)));
            }
        } else if (i == 7) {
            Iterator<Map.Entry<String, Integer>> it2 = mapRetrieveJavaTimeFieldValueNames.entrySet().iterator();
            while (it2.hasNext()) {
                arrayList.add(createEntry(it2.next().getKey(), Long.valueOf(toWeekDay(r6.getValue().intValue()))));
            }
        } else {
            Iterator<Map.Entry<String, Integer>> it3 = mapRetrieveJavaTimeFieldValueNames.entrySet().iterator();
            while (it3.hasNext()) {
                arrayList.add(createEntry(it3.next().getKey(), Long.valueOf(r6.getValue().intValue())));
            }
        }
        return arrayList.iterator();
    }

    private Object findStore(TemporalField temporalField, Locale locale) {
        Map.Entry<TemporalField, Locale> entryCreateEntry = createEntry(temporalField, locale);
        Object obj = CACHE.get(entryCreateEntry);
        if (obj == null) {
            CACHE.putIfAbsent(entryCreateEntry, createStore(temporalField, locale));
            return CACHE.get(entryCreateEntry);
        }
        return obj;
    }

    private static int toWeekDay(int i) {
        if (i == 1) {
            return 7;
        }
        return i - 1;
    }

    private Object createStore(TemporalField temporalField, Locale locale) {
        Map<String, Integer> mapRetrieveJavaTimeFieldValueNames;
        String strRetrieveJavaTimeFieldValueName;
        Map<String, Integer> mapRetrieveJavaTimeFieldValueNames2;
        HashMap map = new HashMap();
        int i = 0;
        if (temporalField == ChronoField.ERA) {
            for (TextStyle textStyle : TextStyle.values()) {
                if (!textStyle.isStandalone() && (mapRetrieveJavaTimeFieldValueNames2 = CalendarDataUtility.retrieveJavaTimeFieldValueNames("gregory", 0, textStyle.toCalendarStyle(), locale)) != null) {
                    HashMap map2 = new HashMap();
                    Iterator<Map.Entry<String, Integer>> it = mapRetrieveJavaTimeFieldValueNames2.entrySet().iterator();
                    while (it.hasNext()) {
                        map2.put(Long.valueOf(r7.getValue().intValue()), it.next().getKey());
                    }
                    if (!map2.isEmpty()) {
                        map.put(textStyle, map2);
                    }
                }
            }
            return new LocaleStore(map);
        }
        if (temporalField == ChronoField.MONTH_OF_YEAR) {
            for (TextStyle textStyle2 : TextStyle.values()) {
                Map<String, Integer> mapRetrieveJavaTimeFieldValueNames3 = CalendarDataUtility.retrieveJavaTimeFieldValueNames("gregory", 2, textStyle2.toCalendarStyle(), locale);
                HashMap map3 = new HashMap();
                if (mapRetrieveJavaTimeFieldValueNames3 != null) {
                    Iterator<Map.Entry<String, Integer>> it2 = mapRetrieveJavaTimeFieldValueNames3.entrySet().iterator();
                    while (it2.hasNext()) {
                        map3.put(Long.valueOf(r8.getValue().intValue() + 1), it2.next().getKey());
                    }
                } else {
                    int i2 = 0;
                    while (i2 <= 11 && (strRetrieveJavaTimeFieldValueName = CalendarDataUtility.retrieveJavaTimeFieldValueName("gregory", 2, i2, textStyle2.toCalendarStyle(), locale)) != null) {
                        i2++;
                        map3.put(Long.valueOf(i2), strRetrieveJavaTimeFieldValueName);
                    }
                }
                if (!map3.isEmpty()) {
                    map.put(textStyle2, map3);
                }
            }
            return new LocaleStore(map);
        }
        if (temporalField == ChronoField.DAY_OF_WEEK) {
            TextStyle[] textStyleArrValues = TextStyle.values();
            int length = textStyleArrValues.length;
            while (i < length) {
                TextStyle textStyle3 = textStyleArrValues[i];
                Map<String, Integer> mapRetrieveJavaTimeFieldValueNames4 = CalendarDataUtility.retrieveJavaTimeFieldValueNames("gregory", 7, textStyle3.toCalendarStyle(), locale);
                HashMap map4 = new HashMap();
                if (mapRetrieveJavaTimeFieldValueNames4 != null) {
                    Iterator<Map.Entry<String, Integer>> it3 = mapRetrieveJavaTimeFieldValueNames4.entrySet().iterator();
                    while (it3.hasNext()) {
                        map4.put(Long.valueOf(toWeekDay(r7.getValue().intValue())), it3.next().getKey());
                    }
                } else {
                    for (int i3 = 1; i3 <= 7; i3++) {
                        String strRetrieveJavaTimeFieldValueName2 = CalendarDataUtility.retrieveJavaTimeFieldValueName("gregory", 7, i3, textStyle3.toCalendarStyle(), locale);
                        if (strRetrieveJavaTimeFieldValueName2 == null) {
                            break;
                        }
                        map4.put(Long.valueOf(toWeekDay(i3)), strRetrieveJavaTimeFieldValueName2);
                    }
                }
                if (!map4.isEmpty()) {
                    map.put(textStyle3, map4);
                }
                i++;
            }
            return new LocaleStore(map);
        }
        if (temporalField == ChronoField.AMPM_OF_DAY) {
            TextStyle[] textStyleArrValues2 = TextStyle.values();
            int length2 = textStyleArrValues2.length;
            while (i < length2) {
                TextStyle textStyle4 = textStyleArrValues2[i];
                if (!textStyle4.isStandalone() && (mapRetrieveJavaTimeFieldValueNames = CalendarDataUtility.retrieveJavaTimeFieldValueNames("gregory", 9, textStyle4.toCalendarStyle(), locale)) != null) {
                    HashMap map5 = new HashMap();
                    Iterator<Map.Entry<String, Integer>> it4 = mapRetrieveJavaTimeFieldValueNames.entrySet().iterator();
                    while (it4.hasNext()) {
                        map5.put(Long.valueOf(r6.getValue().intValue()), it4.next().getKey());
                    }
                    if (!map5.isEmpty()) {
                        map.put(textStyle4, map5);
                    }
                }
                i++;
            }
            return new LocaleStore(map);
        }
        if (temporalField == IsoFields.QUARTER_OF_YEAR) {
            ICUResourceBundle withFallback = UResourceBundle.getBundleInstance("android/icu/impl/data/icudt60b", locale).getWithFallback("calendar/gregorian/quarters");
            ICUResourceBundle withFallback2 = withFallback.getWithFallback("format");
            ICUResourceBundle withFallback3 = withFallback.getWithFallback("stand-alone");
            map.put(TextStyle.FULL, extractQuarters(withFallback2, "wide"));
            map.put(TextStyle.FULL_STANDALONE, extractQuarters(withFallback3, "wide"));
            map.put(TextStyle.SHORT, extractQuarters(withFallback2, "abbreviated"));
            map.put(TextStyle.SHORT_STANDALONE, extractQuarters(withFallback3, "abbreviated"));
            map.put(TextStyle.NARROW, extractQuarters(withFallback2, "narrow"));
            map.put(TextStyle.NARROW_STANDALONE, extractQuarters(withFallback3, "narrow"));
            return new LocaleStore(map);
        }
        return "";
    }

    private static Map<Long, String> extractQuarters(ICUResourceBundle iCUResourceBundle, String str) {
        String[] stringArray = iCUResourceBundle.getWithFallback(str).getStringArray();
        HashMap map = new HashMap();
        int i = 0;
        while (i < stringArray.length) {
            int i2 = i + 1;
            map.put(Long.valueOf(i2), stringArray[i]);
            i = i2;
        }
        return map;
    }

    private static <A, B> Map.Entry<A, B> createEntry(A a, B b) {
        return new AbstractMap.SimpleImmutableEntry(a, b);
    }

    static final class LocaleStore {
        private final Map<TextStyle, List<Map.Entry<String, Long>>> parsable;
        private final Map<TextStyle, Map<Long, String>> valueTextMap;

        LocaleStore(Map<TextStyle, Map<Long, String>> map) {
            this.valueTextMap = map;
            HashMap map2 = new HashMap();
            ArrayList arrayList = new ArrayList();
            for (Map.Entry<TextStyle, Map<Long, String>> entry : map.entrySet()) {
                HashMap map3 = new HashMap();
                for (Map.Entry<Long, String> entry2 : entry.getValue().entrySet()) {
                    if (map3.put(entry2.getValue(), DateTimeTextProvider.createEntry(entry2.getValue(), entry2.getKey())) != 0) {
                    }
                    while (r4.hasNext()) {
                    }
                }
                ArrayList arrayList2 = new ArrayList(map3.values());
                Collections.sort(arrayList2, DateTimeTextProvider.COMPARATOR);
                map2.put(entry.getKey(), arrayList2);
                arrayList.addAll(arrayList2);
                map2.put(null, arrayList);
            }
            Collections.sort(arrayList, DateTimeTextProvider.COMPARATOR);
            this.parsable = map2;
        }

        String getText(long j, TextStyle textStyle) {
            Map<Long, String> map = this.valueTextMap.get(textStyle);
            if (map != null) {
                return map.get(Long.valueOf(j));
            }
            return null;
        }

        Iterator<Map.Entry<String, Long>> getTextIterator(TextStyle textStyle) {
            List<Map.Entry<String, Long>> list = this.parsable.get(textStyle);
            if (list != null) {
                return list.iterator();
            }
            return null;
        }
    }
}
