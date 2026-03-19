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

    public List<V> put(String str, V v) {
        ArrayList<V> arrayList = this.map.get(str);
        if (arrayList == null) {
            arrayList = new ArrayList<>(10);
            this.map.put(str, arrayList);
        }
        arrayList.add(v);
        return arrayList;
    }

    @Override
    public boolean containsValue(Object obj) {
        Set<Map.Entry<String, ArrayList<V>>> setEntrySet = this.map.entrySet();
        if (setEntrySet == null) {
            return false;
        }
        Iterator<Map.Entry<String, ArrayList<V>>> it = setEntrySet.iterator();
        while (it.hasNext()) {
            if (it.next().getValue().contains(obj)) {
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
        ArrayList arrayList = new ArrayList(this.map.size());
        Iterator<Map.Entry<String, ArrayList<V>>> it = this.map.entrySet().iterator();
        while (it.hasNext()) {
            for (Object obj : it.next().getValue().toArray()) {
                arrayList.add(obj);
            }
        }
        return arrayList;
    }

    public Object clone() {
        MultiValueMapImpl multiValueMapImpl = new MultiValueMapImpl();
        multiValueMapImpl.map = (HashMap) this.map.clone();
        return multiValueMapImpl;
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean containsKey(Object obj) {
        return this.map.containsKey(obj);
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
    public List<V> get(Object obj) {
        return this.map.get(obj);
    }

    @Override
    public List<V> put(String str, List<V> list) {
        return this.map.put(str, (ArrayList) list);
    }

    @Override
    public List<V> remove(Object obj) {
        return this.map.remove(obj);
    }

    @Override
    public void putAll(Map<? extends String, ? extends List<V>> map) {
        for (String str : map.keySet()) {
            ArrayList<V> arrayList = new ArrayList<>();
            arrayList.addAll(map.get(str));
            this.map.put(str, arrayList);
        }
    }
}
