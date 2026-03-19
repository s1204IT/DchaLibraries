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
        ICUResourceBundle bundle = (ICUResourceBundle) ICUResourceBundle.getBundleInstance("android/icu/impl/data/icudt56b/curr", "supplementalData", ICUResourceBundle.ICU_DATA_CLASS_LOADER);
        this.regionInfo = bundle.findTopLevel("CurrencyMap");
        this.digitInfo = bundle.findTopLevel("CurrencyMeta");
    }

    @Override
    public List<CurrencyMetaInfo.CurrencyInfo> currencyInfo(CurrencyMetaInfo.CurrencyFilter filter) {
        return collect(new InfoCollector(null), filter);
    }

    @Override
    public List<String> currencies(CurrencyMetaInfo.CurrencyFilter filter) {
        return collect(new CurrencyCollector(null), filter);
    }

    @Override
    public List<String> regions(CurrencyMetaInfo.CurrencyFilter filter) {
        return collect(new RegionCollector(null), filter);
    }

    @Override
    public CurrencyMetaInfo.CurrencyDigits currencyDigits(String isoCode) {
        return currencyDigits(isoCode, Currency.CurrencyUsage.STANDARD);
    }

    @Override
    public CurrencyMetaInfo.CurrencyDigits currencyDigits(String isoCode, Currency.CurrencyUsage currencyPurpose) {
        ICUResourceBundle b = this.digitInfo.findWithFallback(isoCode);
        if (b == null) {
            b = this.digitInfo.findWithFallback("DEFAULT");
        }
        int[] data = b.getIntVector();
        if (currencyPurpose == Currency.CurrencyUsage.CASH) {
            return new CurrencyMetaInfo.CurrencyDigits(data[2], data[3]);
        }
        if (currencyPurpose == Currency.CurrencyUsage.STANDARD) {
            return new CurrencyMetaInfo.CurrencyDigits(data[0], data[1]);
        }
        return new CurrencyMetaInfo.CurrencyDigits(data[0], data[1]);
    }

    private <T> List<T> collect(Collector<T> collector, CurrencyMetaInfo.CurrencyFilter filter) {
        if (filter == null) {
            filter = CurrencyMetaInfo.CurrencyFilter.all();
        }
        int needed = collector.collects();
        if (filter.region != null) {
            needed |= 1;
        }
        if (filter.currency != null) {
            needed |= 2;
        }
        if (filter.from != Long.MIN_VALUE || filter.to != Long.MAX_VALUE) {
            needed |= 4;
        }
        if (filter.tenderOnly) {
            needed |= 8;
        }
        if (needed != 0) {
            if (filter.region != null) {
                ICUResourceBundle b = this.regionInfo.findWithFallback(filter.region);
                if (b != null) {
                    collectRegion(collector, filter, needed, b);
                }
            } else {
                for (int i = 0; i < this.regionInfo.getSize(); i++) {
                    collectRegion(collector, filter, needed, this.regionInfo.at(i));
                }
            }
        }
        return collector.getList();
    }

    private <T> void collectRegion(Collector<T> collector, CurrencyMetaInfo.CurrencyFilter filter, int needed, ICUResourceBundle b) {
        String region = b.getKey();
        if (needed == 1) {
            String region2 = b.getKey();
            collector.collect(region2, null, 0L, 0L, -1, false);
            return;
        }
        for (int i = 0; i < b.getSize(); i++) {
            ICUResourceBundle r = b.at(i);
            if (r.getSize() != 0) {
                String currency = null;
                long from = Long.MIN_VALUE;
                long to = Long.MAX_VALUE;
                boolean tender = true;
                if ((needed & 2) != 0) {
                    ICUResourceBundle currBundle = r.at("id");
                    currency = currBundle.getString();
                    if (filter.currency == null || filter.currency.equals(currency)) {
                        if ((needed & 4) != 0) {
                            from = getDate(r.at("from"), Long.MIN_VALUE, false);
                            to = getDate(r.at("to"), Long.MAX_VALUE, true);
                            if (filter.from <= to && filter.to >= from) {
                                if ((needed & 8) != 0) {
                                    ICUResourceBundle tenderBundle = r.at("tender");
                                    tender = tenderBundle != null ? "true".equals(tenderBundle.getString()) : true;
                                    if (!filter.tenderOnly || tender) {
                                    }
                                } else {
                                    collector.collect(region, currency, from, to, i, tender);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private long getDate(ICUResourceBundle b, long defaultValue, boolean endOfDay) {
        if (b == null) {
            return defaultValue;
        }
        int[] values = b.getIntVector();
        return (((long) values[0]) << 32) | (((long) values[1]) & MASK);
    }

    private static class UniqueList<T> {
        private Set<T> seen = new HashSet();
        private List<T> list = new ArrayList();

        private UniqueList() {
        }

        private static <T> UniqueList<T> create() {
            return new UniqueList<>();
        }

        void add(T value) {
            if (this.seen.contains(value)) {
                return;
            }
            this.list.add(value);
            this.seen.add(value);
        }

        List<T> list() {
            return Collections.unmodifiableList(this.list);
        }
    }

    private static class InfoCollector implements Collector<CurrencyMetaInfo.CurrencyInfo> {
        private List<CurrencyMetaInfo.CurrencyInfo> result;

        InfoCollector(InfoCollector infoCollector) {
            this();
        }

        private InfoCollector() {
            this.result = new ArrayList();
        }

        @Override
        public void collect(String region, String currency, long from, long to, int priority, boolean tender) {
            this.result.add(new CurrencyMetaInfo.CurrencyInfo(region, currency, from, to, priority, tender));
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

        RegionCollector(RegionCollector regionCollector) {
            this();
        }

        private RegionCollector() {
            this.result = UniqueList.create();
        }

        @Override
        public void collect(String region, String currency, long from, long to, int priority, boolean tender) {
            this.result.add(region);
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

        CurrencyCollector(CurrencyCollector currencyCollector) {
            this();
        }

        private CurrencyCollector() {
            this.result = UniqueList.create();
        }

        @Override
        public void collect(String region, String currency, long from, long to, int priority, boolean tender) {
            this.result.add(currency);
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
