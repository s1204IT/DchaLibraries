package android.icu.util;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalePriorityList implements Iterable<ULocale> {
    private static final double D0 = 0.0d;
    private final Map<ULocale, Double> languagesAndWeights;
    private static final Double D1 = Double.valueOf(1.0d);
    private static final Pattern languageSplitter = Pattern.compile("\\s*,\\s*");
    private static final Pattern weightSplitter = Pattern.compile("\\s*(\\S*)\\s*;\\s*q\\s*=\\s*(\\S*)");
    private static Comparator<Double> myDescendingDouble = new Comparator<Double>() {
        @Override
        public int compare(Double d, Double d2) {
            int iCompareTo = d.compareTo(d2);
            if (iCompareTo > 0) {
                return -1;
            }
            return iCompareTo < 0 ? 1 : 0;
        }
    };

    public static Builder add(ULocale... uLocaleArr) {
        return new Builder().add(uLocaleArr);
    }

    public static Builder add(ULocale uLocale, double d) {
        return new Builder().add(uLocale, d);
    }

    public static Builder add(LocalePriorityList localePriorityList) {
        return new Builder().add(localePriorityList);
    }

    public static Builder add(String str) {
        return new Builder().add(str);
    }

    public Double getWeight(ULocale uLocale) {
        return this.languagesAndWeights.get(uLocale);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ULocale uLocale : this.languagesAndWeights.keySet()) {
            if (sb.length() != 0) {
                sb.append(", ");
            }
            sb.append(uLocale);
            double dDoubleValue = this.languagesAndWeights.get(uLocale).doubleValue();
            if (dDoubleValue != D1.doubleValue()) {
                sb.append(";q=");
                sb.append(dDoubleValue);
            }
        }
        return sb.toString();
    }

    @Override
    public Iterator<ULocale> iterator() {
        return this.languagesAndWeights.keySet().iterator();
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        try {
            return this.languagesAndWeights.equals(((LocalePriorityList) obj).languagesAndWeights);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public int hashCode() {
        return this.languagesAndWeights.hashCode();
    }

    private LocalePriorityList(Map<ULocale, Double> map) {
        this.languagesAndWeights = map;
    }

    public static class Builder {
        private final Map<ULocale, Double> languageToWeight;

        private Builder() {
            this.languageToWeight = new LinkedHashMap();
        }

        public LocalePriorityList build() {
            return build(false);
        }

        public LocalePriorityList build(boolean z) {
            TreeMap treeMap = new TreeMap(LocalePriorityList.myDescendingDouble);
            for (ULocale uLocale : this.languageToWeight.keySet()) {
                Double d = this.languageToWeight.get(uLocale);
                Set linkedHashSet = (Set) treeMap.get(d);
                if (linkedHashSet == null) {
                    linkedHashSet = new LinkedHashSet();
                    treeMap.put(d, linkedHashSet);
                }
                linkedHashSet.add(uLocale);
            }
            LinkedHashMap linkedHashMap = new LinkedHashMap();
            for (Map.Entry entry : treeMap.entrySet()) {
                Double d2 = (Double) entry.getKey();
                Iterator it = ((Set) entry.getValue()).iterator();
                while (it.hasNext()) {
                    linkedHashMap.put((ULocale) it.next(), z ? d2 : LocalePriorityList.D1);
                }
            }
            return new LocalePriorityList(Collections.unmodifiableMap(linkedHashMap));
        }

        public Builder add(LocalePriorityList localePriorityList) {
            for (ULocale uLocale : localePriorityList.languagesAndWeights.keySet()) {
                add(uLocale, ((Double) localePriorityList.languagesAndWeights.get(uLocale)).doubleValue());
            }
            return this;
        }

        public Builder add(ULocale uLocale) {
            return add(uLocale, LocalePriorityList.D1.doubleValue());
        }

        public Builder add(ULocale... uLocaleArr) {
            for (ULocale uLocale : uLocaleArr) {
                add(uLocale, LocalePriorityList.D1.doubleValue());
            }
            return this;
        }

        public Builder add(ULocale uLocale, double d) {
            if (this.languageToWeight.containsKey(uLocale)) {
                this.languageToWeight.remove(uLocale);
            }
            if (d <= 0.0d) {
                return this;
            }
            if (d > LocalePriorityList.D1.doubleValue()) {
                d = LocalePriorityList.D1.doubleValue();
            }
            this.languageToWeight.put(uLocale, Double.valueOf(d));
            return this;
        }

        public Builder add(String str) {
            String[] strArrSplit = LocalePriorityList.languageSplitter.split(str.trim());
            Matcher matcher = LocalePriorityList.weightSplitter.matcher("");
            for (String str2 : strArrSplit) {
                if (matcher.reset(str2).matches()) {
                    ULocale uLocale = new ULocale(matcher.group(1));
                    double d = Double.parseDouble(matcher.group(2));
                    if (d < 0.0d || d > LocalePriorityList.D1.doubleValue()) {
                        throw new IllegalArgumentException("Illegal weight, must be 0..1: " + d);
                    }
                    add(uLocale, d);
                } else if (str2.length() != 0) {
                    add(new ULocale(str2));
                }
            }
            return this;
        }
    }
}
