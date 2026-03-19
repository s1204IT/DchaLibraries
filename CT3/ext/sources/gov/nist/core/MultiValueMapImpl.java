package gov.nist.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MultiValueMapImpl<V> implements MultiValueMap<String, V>, Cloneable {
    private static final long serialVersionUID = 4275505380960964605L;
    private HashMap<String, ArrayList<V>> map = new HashMap<>();

    public List<V> put(String key, V value) {
        ArrayList<V> keyList = this.map.get(key);
        if (keyList == null) {
            keyList = new ArrayList<>(10);
            this.map.put(key, keyList);
        }
        keyList.add(value);
        return keyList;
    }

    @Override
    public boolean containsValue(Object value) {
        Set<Map.Entry<String, ArrayList<V>>> setEntrySet = this.map.entrySet();
        if (setEntrySet == null) {
            return false;
        }
        Iterator<Map.Entry<String, ArrayList<V>>> it = setEntrySet.iterator();
        while (it.hasNext()) {
            if (it.next().getValue().contains(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void clear() {
        Iterator<Map.Entry<String, ArrayList<V>>> it = this.map.entrySet().iterator();
        while (it.hasNext()) {
            it.next().getValue().clear();
        }
        this.map.clear();
    }

    @Override
    public Collection values() {
        ArrayList returnList = new ArrayList(this.map.size());
        Iterator<Map.Entry<String, ArrayList<V>>> it = this.map.entrySet().iterator();
        while (it.hasNext()) {
            Object[] values = it.next().getValue().toArray();
            for (Object obj : values) {
                returnList.add(obj);
            }
        }
        return returnList;
    }

    public Object clone() {
        MultiValueMapImpl obj = new MultiValueMapImpl();
        obj.map = (HashMap) this.map.clone();
        return obj;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public Set entrySet() {
        return this.map.entrySet();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public Set<String> keySet() {
        return this.map.keySet();
    }

    @Override
    public List<V> get(Object key) {
        return this.map.get(key);
    }

    @Override
    public List<V> put(String key, List<V> value) {
        return this.map.put(key, (ArrayList) value);
    }

    @Override
    public List<V> remove(Object key) {
        return this.map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<V>> mapToPut) {
        for (String k : mapToPut.keySet()) {
            ArrayList<V> al = new ArrayList<>();
            al.addAll(mapToPut.get(k));
            this.map.put(k, al);
        }
    }
}
