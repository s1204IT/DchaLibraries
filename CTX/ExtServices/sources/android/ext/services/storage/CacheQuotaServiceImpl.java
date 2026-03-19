package android.ext.services.storage;

import android.app.usage.CacheQuotaHint;
import android.app.usage.CacheQuotaService;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.util.ArrayMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CacheQuotaServiceImpl extends CacheQuotaService {
    private static Comparator<CacheQuotaHint> sCacheQuotaRequestComparator = new Comparator<CacheQuotaHint>() {
        @Override
        public int compare(CacheQuotaHint cacheQuotaHint, CacheQuotaHint cacheQuotaHint2) {
            long totalTimeInForeground = cacheQuotaHint2.getUsageStats().getTotalTimeInForeground();
            long totalTimeInForeground2 = cacheQuotaHint.getUsageStats().getTotalTimeInForeground();
            if (totalTimeInForeground < totalTimeInForeground2) {
                return -1;
            }
            return totalTimeInForeground == totalTimeInForeground2 ? 0 : 1;
        }
    };

    public List<CacheQuotaHint> onComputeCacheQuotaHints(List<CacheQuotaHint> list) {
        ArrayMap arrayMap = new ArrayMap();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            CacheQuotaHint cacheQuotaHint = list.get(i);
            String volumeUuid = cacheQuotaHint.getVolumeUuid();
            List arrayList = (List) arrayMap.get(volumeUuid);
            if (arrayList == null) {
                arrayList = new ArrayList();
                arrayMap.put(volumeUuid, arrayList);
            }
            arrayList.add(cacheQuotaHint);
        }
        final ArrayList arrayList2 = new ArrayList();
        arrayMap.entrySet().forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                CacheQuotaServiceImpl.lambda$onComputeCacheQuotaHints$3(this.f$0, arrayList2, (Map.Entry) obj);
            }
        });
        return (List) arrayList2.stream().filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CacheQuotaServiceImpl.lambda$onComputeCacheQuotaHints$4((CacheQuotaHint) obj);
            }
        }).collect(Collectors.toList());
    }

    public static void lambda$onComputeCacheQuotaHints$3(CacheQuotaServiceImpl cacheQuotaServiceImpl, List list, Map.Entry entry) {
        Map map = (Map) ((List) entry.getValue()).stream().collect(Collectors.groupingBy(new Function() {
            @Override
            public final Object apply(Object obj) {
                return Integer.valueOf(((CacheQuotaHint) obj).getUid());
            }
        }));
        map.values().forEach(new Consumer() {
            @Override
            public final void accept(Object obj) {
                CacheQuotaServiceImpl.lambda$onComputeCacheQuotaHints$0((List) obj);
            }
        });
        List list2 = (List) map.values().stream().map(new Function() {
            @Override
            public final Object apply(Object obj) {
                return CacheQuotaServiceImpl.lambda$onComputeCacheQuotaHints$1((List) obj);
            }
        }).filter(new Predicate() {
            @Override
            public final boolean test(Object obj) {
                return CacheQuotaServiceImpl.lambda$onComputeCacheQuotaHints$2((CacheQuotaHint) obj);
            }
        }).sorted(sCacheQuotaRequestComparator).collect(Collectors.toList());
        double sumOfFairShares = cacheQuotaServiceImpl.getSumOfFairShares(list2.size());
        long reservedCacheSize = cacheQuotaServiceImpl.getReservedCacheSize((String) entry.getKey());
        for (int i = 0; i < list2.size(); i++) {
            double fairShareForPosition = cacheQuotaServiceImpl.getFairShareForPosition(i) / sumOfFairShares;
            CacheQuotaHint.Builder builder = new CacheQuotaHint.Builder((CacheQuotaHint) list2.get(i));
            builder.setQuota(Math.round(fairShareForPosition * reservedCacheSize));
            list.add(builder.build());
        }
    }

    static void lambda$onComputeCacheQuotaHints$0(List list) {
        int size = list.size();
        if (size < 2) {
            return;
        }
        CacheQuotaHint cacheQuotaHint = (CacheQuotaHint) list.get(0);
        for (int i = 1; i < size; i++) {
            cacheQuotaHint.getUsageStats().mTotalTimeInForeground += ((CacheQuotaHint) list.get(i)).getUsageStats().mTotalTimeInForeground;
        }
    }

    static CacheQuotaHint lambda$onComputeCacheQuotaHints$1(List list) {
        return (CacheQuotaHint) list.get(0);
    }

    static boolean lambda$onComputeCacheQuotaHints$2(CacheQuotaHint cacheQuotaHint) {
        return cacheQuotaHint.getUsageStats().mTotalTimeInForeground != 0;
    }

    static boolean lambda$onComputeCacheQuotaHints$4(CacheQuotaHint cacheQuotaHint) {
        return cacheQuotaHint.getQuota() > 0;
    }

    private double getFairShareForPosition(int i) {
        double dLog = (1.0d / Math.log(i + 3)) - 0.285d;
        if (dLog > 0.01d) {
            return dLog;
        }
        return 0.01d;
    }

    private double getSumOfFairShares(int i) {
        double fairShareForPosition = 0.0d;
        for (int i2 = 0; i2 < i; i2++) {
            fairShareForPosition += getFairShareForPosition(i2);
        }
        return fairShareForPosition;
    }

    private long getReservedCacheSize(String str) {
        long usableSpace;
        StorageManager storageManager = (StorageManager) getSystemService(StorageManager.class);
        if (str == StorageManager.UUID_PRIVATE_INTERNAL) {
            usableSpace = Environment.getDataDirectory().getUsableSpace();
        } else {
            usableSpace = storageManager.findVolumeByUuid(str).getPath().getUsableSpace();
        }
        return Math.round(usableSpace * 0.15d);
    }
}
