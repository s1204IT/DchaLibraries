package android.app.servertransaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class ObjectPool {
    private static final int MAX_POOL_SIZE = 50;
    private static final Object sPoolSync = new Object();
    private static final Map<Class, ArrayList<? extends ObjectPoolItem>> sPoolMap = new HashMap();

    ObjectPool() {
    }

    public static <T extends ObjectPoolItem> T obtain(Class<T> cls) {
        synchronized (sPoolSync) {
            ArrayList<? extends ObjectPoolItem> arrayList = sPoolMap.get(cls);
            if (arrayList != null && !arrayList.isEmpty()) {
                return (T) arrayList.remove(arrayList.size() - 1);
            }
            return null;
        }
    }

    public static <T extends ObjectPoolItem> void recycle(T t) {
        synchronized (sPoolSync) {
            ArrayList<? extends ObjectPoolItem> arrayList = sPoolMap.get(t.getClass());
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                sPoolMap.put(t.getClass(), arrayList);
            }
            int size = arrayList.size();
            for (int i = 0; i < size; i++) {
                if (arrayList.get(i) == t) {
                    throw new IllegalStateException("Trying to recycle already recycled item");
                }
            }
            if (size < 50) {
                arrayList.add(t);
            }
        }
    }
}
