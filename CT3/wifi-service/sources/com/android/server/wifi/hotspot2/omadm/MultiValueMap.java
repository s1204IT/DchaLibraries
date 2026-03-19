package com.android.server.wifi.hotspot2.omadm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MultiValueMap<T> {
    private final Map<String, ArrayList<T>> mMap = new LinkedHashMap();

    public void put(String key, T value) {
        String key2 = key.toLowerCase();
        ArrayList<T> values = this.mMap.get(key2);
        if (values == null) {
            values = new ArrayList<>();
            this.mMap.put(key2, values);
        }
        values.add(value);
    }

    public T get(String key) {
        List<T> values = this.mMap.get(key.toLowerCase());
        if (values == null) {
            return null;
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        throw new IllegalArgumentException("Cannot do get on multi-value");
    }

    public T replace(String key, T oldValue, T newValue) {
        List<T> values = this.mMap.get(key.toLowerCase());
        if (values == null) {
            return null;
        }
        for (int n = 0; n < values.size(); n++) {
            T value = values.get(n);
            if (value == oldValue) {
                values.set(n, newValue);
                return value;
            }
        }
        return null;
    }

    public T remove(String key, T value) {
        String key2 = key.toLowerCase();
        List<T> values = this.mMap.get(key2);
        if (values == null) {
            return null;
        }
        T result = null;
        Iterator<T> valueIterator = values.iterator();
        while (true) {
            if (!valueIterator.hasNext()) {
                break;
            }
            if (valueIterator.next() == value) {
                valueIterator.remove();
                result = value;
                break;
            }
        }
        if (values.isEmpty()) {
            this.mMap.remove(key2);
        }
        return result;
    }

    public T remove(T value) {
        T result = null;
        Iterator<Map.Entry<String, ArrayList<T>>> iterator = this.mMap.entrySet().iterator();
        while (true) {
            if (!iterator.hasNext()) {
                break;
            }
            ArrayList<T> values = iterator.next().getValue();
            Iterator<T> valueIterator = values.iterator();
            while (true) {
                if (!valueIterator.hasNext()) {
                    break;
                }
                if (valueIterator.next() == value) {
                    valueIterator.remove();
                    result = value;
                    break;
                }
            }
            if (result != null) {
                if (values.isEmpty()) {
                    iterator.remove();
                }
            }
        }
        return result;
    }

    public Collection<T> values() {
        ArrayList arrayList = new ArrayList(this.mMap.size());
        Iterator<T> it = this.mMap.values().iterator();
        while (it.hasNext()) {
            Iterator<T> it2 = ((ArrayList) it.next()).iterator();
            while (it2.hasNext()) {
                arrayList.add(it2.next());
            }
        }
        return arrayList;
    }

    public T getSingletonValue() {
        if (this.mMap.size() != 1) {
            throw new IllegalArgumentException("Map is not a single entry map");
        }
        List<T> values = this.mMap.values().iterator().next();
        if (values.size() != 1) {
            throw new IllegalArgumentException("Map is not a single entry map");
        }
        return values.iterator().next();
    }
}
