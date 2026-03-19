package java.util;

import java.io.Serializable;
import java.util.Map;

public final class $$Lambda$TreeMap$EntrySpliterator$YqCulUmBGNzQr1PJ_ERFnbxUmTQ implements Comparator, Serializable {
    public static final $$Lambda$TreeMap$EntrySpliterator$YqCulUmBGNzQr1PJ_ERFnbxUmTQ INSTANCE = new $$Lambda$TreeMap$EntrySpliterator$YqCulUmBGNzQr1PJ_ERFnbxUmTQ();

    private $$Lambda$TreeMap$EntrySpliterator$YqCulUmBGNzQr1PJ_ERFnbxUmTQ() {
    }

    @Override
    public final int compare(Object obj, Object obj2) {
        return ((Comparable) ((Map.Entry) obj).getKey()).compareTo(((Map.Entry) obj2).getKey());
    }
}
