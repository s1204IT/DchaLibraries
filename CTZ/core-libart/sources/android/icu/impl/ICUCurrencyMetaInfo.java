package android.icu.impl;

import android.icu.text.CurrencyMetaInfo;
import android.icu.util.Currency;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ICUCurrencyMetaInfo extends CurrencyMetaInfo {
    private static final int Currency = 2;
    private static final int Date = 4;
    private static final int Everything = Integer.MAX_VALUE;
    private static final long MASK = 4294967295L;
    private static final int Region = 1;
    private static final int Tender = 8;
    private ICUResourceBundle digitInfo;
    private ICUResourceBundle regionInfo;

    private interface Collector<T> {
        void collect(String str, String str2, long j, long j2, int i, boolean z);

        int collects();

        List<T> getList();
    }

    public ICUCurrencyMetaInfo() {
        ICUResourceBundle iCUResourceBundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance(ICUData.ICU_CURR_BASE_NAME, "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        this.regionInfo = iCUResourceBundle.findTopLevel("CurrencyMap");
        this.digitInfo = iCUResourceBundle.findTopLevel("CurrencyMeta");
    }

    @Override
    public List<CurrencyMetaInfo.CurrencyInfo> currencyInfo(CurrencyMetaInfo.CurrencyFilter currencyFilter) {
        return collect(new InfoCollector(), currencyFilter);
    }

    @Override
    public List<String> currencies(CurrencyMetaInfo.CurrencyFilter currencyFilter) {
        return collect(new CurrencyCollector(), currencyFilter);
    }

    @Override
    public List<String> regions(CurrencyMetaInfo.CurrencyFilter currencyFilter) {
        return collect(new RegionCollector(), currencyFilter);
    }

    @Override
    public CurrencyMetaInfo.CurrencyDigits currencyDigits(String str) {
        return currencyDigits(str, Currency.CurrencyUsage.STANDARD);
    }

    @Override
    public CurrencyMetaInfo.CurrencyDigits currencyDigits(String str, Currency.CurrencyUsage currencyUsage) {
        ICUResourceBundle iCUResourceBundleFindWithFallback = this.digitInfo.findWithFallback(str);
        if (iCUResourceBundleFindWithFallback == null) {
            iCUResourceBundleFindWithFallback = this.digitInfo.findWithFallback("DEFAULT");
        }
        int[] intVector = iCUResourceBundleFindWithFallback.getIntVector();
        if (currencyUsage == Currency.CurrencyUsage.CASH) {
            return new CurrencyMetaInfo.CurrencyDigits(intVector[2], intVector[3]);
        }
        if (currencyUsage == Currency.CurrencyUsage.STANDARD) {
            return new CurrencyMetaInfo.CurrencyDigits(intVector[0], intVector[1]);
        }
        return new CurrencyMetaInfo.CurrencyDigits(intVector[0], intVector[1]);
    }

    private <T> List<T> collect(Collector<T> collector, CurrencyMetaInfo.CurrencyFilter currencyFilter) {
        if (currencyFilter == null) {
            currencyFilter = CurrencyMetaInfo.CurrencyFilter.all();
        }
        int iCollects = collector.collects();
        if (currencyFilter.region != null) {
            iCollects |= 1;
        }
        if (currencyFilter.currency != null) {
            iCollects |= 2;
        }
        if (currencyFilter.from != Long.MIN_VALUE || currencyFilter.to != Long.MAX_VALUE) {
            iCollects |= 4;
        }
        if (currencyFilter.tenderOnly) {
            iCollects |= 8;
        }
        if (iCollects != 0) {
            if (currencyFilter.region != null) {
                ICUResourceBundle iCUResourceBundleFindWithFallback = this.regionInfo.findWithFallback(currencyFilter.region);
                if (iCUResourceBundleFindWithFallback != null) {
                    collectRegion(collector, currencyFilter, iCollects, iCUResourceBundleFindWithFallback);
                }
            } else {
                for (int i = 0; i < this.regionInfo.getSize(); i++) {
                    collectRegion(collector, currencyFilter, iCollects, this.regionInfo.at(i));
                }
            }
        }
        return collector.getList();
    }

    private <T> void collectRegion(Collector<T> collector, CurrencyMetaInfo.CurrencyFilter currencyFilter, int i, ICUResourceBundle iCUResourceBundle) {
        boolean z;
        String key = iCUResourceBundle.getKey();
        boolean z2 = true;
        if (i == 1) {
            collector.collect(iCUResourceBundle.getKey(), null, 0L, 0L, -1, false);
            return;
        }
        boolean z3 = false;
        int i2 = 0;
        while (i2 < iCUResourceBundle.getSize()) {
            ICUResourceBundle iCUResourceBundleAt = iCUResourceBundle.at(i2);
            if (iCUResourceBundleAt.getSize() != 0) {
                String string = null;
                if ((i & 2) != 0) {
                    string = iCUResourceBundleAt.at("id").getString();
                    if (currencyFilter.currency == null || currencyFilter.currency.equals(string)) {
                        String str = string;
                        long date = Long.MAX_VALUE;
                        long date2 = Long.MIN_VALUE;
                        if ((i & 4) != 0) {
                            date2 = getDate(iCUResourceBundleAt.at("from"), Long.MIN_VALUE, z3);
                            date = getDate(iCUResourceBundleAt.at("to"), Long.MAX_VALUE, z2);
                            if (currencyFilter.from <= date && currencyFilter.to >= date2) {
                                long j = date;
                                long j2 = date2;
                                if ((i & 8) != 0) {
                                    ICUResourceBundle iCUResourceBundleAt2 = iCUResourceBundleAt.at("tender");
                                    boolean z4 = iCUResourceBundleAt2 == null || "true".equals(iCUResourceBundleAt2.getString());
                                    if (!currencyFilter.tenderOnly || z4) {
                                        z = z4;
                                    }
                                } else {
                                    z = true;
                                }
                                collector.collect(key, str, j2, j, i2, z);
                            }
                        }
                    }
                }
            }
            i2++;
            z2 = true;
            z3 = false;
        }
    }

    private long getDate(ICUResourceBundle iCUResourceBundle, long j, boolean z) {
        if (iCUResourceBundle == null) {
            return j;
        }
        int[] intVector = iCUResourceBundle.getIntVector();
        return (((long) intVector[0]) << 32) | (((long) intVector[1]) & MASK);
    }

    private static class UniqueList<T> {
        private Set<T> seen = new HashSet();
        private List<T> list = new ArrayList();

        private UniqueList() {
        }

        private static <T> UniqueList<T> create() {
            return new UniqueList<>();
        }

        void add(T t) {
            if (!this.seen.contains(t)) {
                this.list.add(t);
                this.seen.add(t);
            }
        }

        List<T> list() {
            return Collections.unmodifiableList(this.list);
        }
    }

    private static class InfoCollector implements Collector<CurrencyMetaInfo.CurrencyInfo> {
        private List<CurrencyMetaInfo.CurrencyInfo> result;

        private InfoCollector() {
            this.result = new ArrayList();
        }

        @Override
        public void collect(String str, String str2, long j, long j2, int i, boolean z) {
            this.result.add(new CurrencyMetaInfo.CurrencyInfo(str, str2, j, j2, i, z));
        }

        @Override
        public List<CurrencyMetaInfo.CurrencyInfo> getList() {
            return Collections.unmodifiableList(this.result);
        }

        @Override
        public int collects() {
            return Integer.MAX_VALUE;
        }
    }

    private static class RegionCollector implements Collector<String> {
        private final UniqueList<String> result;

        private RegionCollector() {
            this.result = UniqueList.create();
        }

        @Override
        public void collect(String str, String str2, long j, long j2, int i, boolean z) {
            this.result.add(str);
        }

        @Override
        public int collects() {
            return 1;
        }

        @Override
        public List<String> getList() {
            return this.result.list();
        }
    }

    private static class CurrencyCollector implements Collector<String> {
        private final UniqueList<String> result;

        private CurrencyCollector() {
            this.result = UniqueList.create();
        }

        @Override
        public void collect(String str, String str2, long j, long j2, int i, boolean z) {
            this.result.add(str2);
        }

        @Override
        public int collects() {
            return 2;
        }

        @Override
        public List<String> getList() {
            return this.result.list();
        }
    }
}
