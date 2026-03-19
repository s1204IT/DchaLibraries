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
        public int compare(Double o1, Double o2) {
            return -o1.compareTo(o2);
        }
    };

    LocalePriorityList(Map languageToWeight, LocalePriorityList localePriorityList) {
        this(languageToWeight);
    }

    public static Builder add(ULocale... languageCode) {
        return new Builder(null).add(languageCode);
    }

    public static Builder add(ULocale languageCode, double weight) {
        return new Builder(null).add(languageCode, weight);
    }

    public static Builder add(LocalePriorityList languagePriorityList) {
        return new Builder(null).add(languagePriorityList);
    }

    public static Builder add(String acceptLanguageString) {
        return new Builder(null).add(acceptLanguageString);
    }

    public Double getWeight(ULocale language) {
        return this.languagesAndWeights.get(language);
    }

    public String toString() {
        StringBuilder result = new StringBuilder();
        for (ULocale language : this.languagesAndWeights.keySet()) {
            if (result.length() != 0) {
                result.append(", ");
            }
            result.append(language);
            double weight = this.languagesAndWeights.get(language).doubleValue();
            if (weight != D1.doubleValue()) {
                result.append(";q=").append(weight);
            }
        }
        return result.toString();
    }

    @Override
    public Iterator<ULocale> iterator() {
        return this.languagesAndWeights.keySet().iterator();
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (this == o) {
            return true;
        }
        try {
            LocalePriorityList that = (LocalePriorityList) o;
            return this.languagesAndWeights.equals(that.languagesAndWeights);
        } catch (RuntimeException e) {
            return false;
        }
    }

    public int hashCode() {
        return this.languagesAndWeights.hashCode();
    }

    private LocalePriorityList(Map<ULocale, Double> languageToWeight) {
        this.languagesAndWeights = languageToWeight;
    }

    public static class Builder {
        private final Map<ULocale, Double> languageToWeight;

        Builder(Builder builder) {
            this();
        }

        private Builder() {
            this.languageToWeight = new LinkedHashMap();
        }

        public LocalePriorityList build() {
            return build(false);
        }

        public LocalePriorityList build(boolean preserveWeights) {
            LocalePriorityList localePriorityList = null;
            Map<Double, Set<ULocale>> doubleCheck = new TreeMap<>((Comparator<? super Double>) LocalePriorityList.myDescendingDouble);
            for (ULocale lang : this.languageToWeight.keySet()) {
                Double weight = this.languageToWeight.get(lang);
                Set<ULocale> s = doubleCheck.get(weight);
                if (s == null) {
                    s = new LinkedHashSet<>();
                    doubleCheck.put(weight, s);
                }
                s.add(lang);
            }
            Map<ULocale, Double> temp = new LinkedHashMap<>();
            for (Map.Entry<Double, Set<ULocale>> langEntry : doubleCheck.entrySet()) {
                Double weight2 = langEntry.getKey();
                Iterator lang$iterator = langEntry.getValue().iterator();
                while (lang$iterator.hasNext()) {
                    temp.put((ULocale) lang$iterator.next(), preserveWeights ? weight2 : LocalePriorityList.D1);
                }
            }
            return new LocalePriorityList(Collections.unmodifiableMap(temp), localePriorityList);
        }

        public Builder add(LocalePriorityList languagePriorityList) {
            for (ULocale language : languagePriorityList.languagesAndWeights.keySet()) {
                add(language, ((Double) languagePriorityList.languagesAndWeights.get(language)).doubleValue());
            }
            return this;
        }

        public Builder add(ULocale languageCode) {
            return add(languageCode, LocalePriorityList.D1.doubleValue());
        }

        public Builder add(ULocale... languageCodes) {
            for (ULocale languageCode : languageCodes) {
                add(languageCode, LocalePriorityList.D1.doubleValue());
            }
            return this;
        }

        public Builder add(ULocale languageCode, double weight) {
            if (this.languageToWeight.containsKey(languageCode)) {
                this.languageToWeight.remove(languageCode);
            }
            if (weight <= 0.0d) {
                return this;
            }
            if (weight > LocalePriorityList.D1.doubleValue()) {
                weight = LocalePriorityList.D1.doubleValue();
            }
            this.languageToWeight.put(languageCode, Double.valueOf(weight));
            return this;
        }

        public Builder add(String acceptLanguageList) {
            String[] items = LocalePriorityList.languageSplitter.split(acceptLanguageList.trim());
            Matcher itemMatcher = LocalePriorityList.weightSplitter.matcher("");
            for (String item : items) {
                if (itemMatcher.reset(item).matches()) {
                    ULocale language = new ULocale(itemMatcher.group(1));
                    double weight = Double.parseDouble(itemMatcher.group(2));
                    if (!(weight >= 0.0d && weight <= LocalePriorityList.D1.doubleValue())) {
                        throw new IllegalArgumentException("Illegal weight, must be 0..1: " + weight);
                    }
                    add(language, weight);
                } else if (item.length() != 0) {
                    add(new ULocale(item));
                }
            }
            return this;
        }
    }
}
