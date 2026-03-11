package android.support.v4.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ArrayMap<K, V> extends SimpleArrayMap<K, V> implements Map<K, V> {
    MapCollections<K, V> mCollections;

    private MapCollections<K, V> getCollection() {
        if (this.mCollections == null) {
            this.mCollections = new MapCollections<K, V>(this) {
                final ArrayMap this$0;

                {
                    this.this$0 = this;
                }

                @Override
                protected void colClear() {
                    this.this$0.clear();
                }

                @Override
                protected Object colGetEntry(int i, int i2) {
                    return this.this$0.mArray[(i << 1) + i2];
                }

                @Override
                protected Map<K, V> colGetMap() {
                    return this.this$0;
                }

                @Override
                protected int colGetSize() {
                    return this.this$0.mSize;
                }

                @Override
                protected int colIndexOfKey(Object obj) {
                    return this.this$0.indexOfKey(obj);
                }

                @Override
                protected int colIndexOfValue(Object obj) {
                    return this.this$0.indexOfValue(obj);
                }

                @Override
                protected void colPut(K k, V v) {
                    this.this$0.put(k, v);
                }

                @Override
                protected void colRemoveAt(int i) {
                    this.this$0.removeAt(i);
                }

                @Override
                protected V colSetValue(int i, V v) {
                    return this.this$0.setValueAt(i, v);
                }
            };
        }
        return this.mCollections;
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return getCollection().getEntrySet();
    }

    @Override
    public Set<K> keySet() {
        return getCollection().getKeySet();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        ensureCapacity(this.mSize + map.size());
        for (Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    public boolean retainAll(Collection<?> collection) {
        return MapCollections.retainAllHelper(this, collection);
    }

    @Override
    public Collection<V> values() {
        return getCollection().getValues();
    }
}
