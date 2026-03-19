package gov.nist.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NameValueList implements Serializable, Cloneable, Map<String, NameValue> {
    private static final long serialVersionUID = -6998271876574260243L;
    private Map<String, NameValue> hmap;
    private String separator;

    public NameValueList() {
        this.separator = Separators.SEMICOLON;
        this.hmap = new LinkedHashMap();
    }

    public NameValueList(boolean z) {
        this.separator = Separators.SEMICOLON;
        if (z) {
            this.hmap = new ConcurrentHashMap();
        } else {
            this.hmap = new LinkedHashMap();
        }
    }

    public void setSeparator(String str) {
        this.separator = str;
    }

    public String encode() {
        return encode(new StringBuffer()).toString();
    }

    public StringBuffer encode(StringBuffer stringBuffer) {
        if (!this.hmap.isEmpty()) {
            Iterator<NameValue> it = this.hmap.values().iterator();
            if (it.hasNext()) {
                while (true) {
                    NameValue next = it.next();
                    if (next instanceof GenericObject) {
                        next.encode(stringBuffer);
                    } else {
                        stringBuffer.append(next.toString());
                    }
                    if (!it.hasNext()) {
                        break;
                    }
                    stringBuffer.append(this.separator);
                }
            }
        }
        return stringBuffer;
    }

    public String toString() {
        return encode();
    }

    public void set(NameValue nameValue) {
        this.hmap.put(nameValue.getName().toLowerCase(), nameValue);
    }

    public void set(String str, Object obj) {
        this.hmap.put(str.toLowerCase(), new NameValue(str, obj));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !obj.getClass().equals(getClass())) {
            return false;
        }
        NameValueList nameValueList = (NameValueList) obj;
        if (this.hmap.size() != nameValueList.hmap.size()) {
            return false;
        }
        for (String str : this.hmap.keySet()) {
            NameValue nameValue = getNameValue(str);
            NameValue nameValue2 = nameValueList.hmap.get(str);
            if (nameValue2 == null || !nameValue2.equals(nameValue)) {
                return false;
            }
        }
        return true;
    }

    public Object getValue(String str) {
        NameValue nameValue = getNameValue(str.toLowerCase());
        if (nameValue != null) {
            return nameValue.getValueAsObject();
        }
        return null;
    }

    public NameValue getNameValue(String str) {
        return this.hmap.get(str.toLowerCase());
    }

    public boolean hasNameValue(String str) {
        return this.hmap.containsKey(str.toLowerCase());
    }

    public boolean delete(String str) {
        String lowerCase = str.toLowerCase();
        if (this.hmap.containsKey(lowerCase)) {
            this.hmap.remove(lowerCase);
            return true;
        }
        return false;
    }

    public Object clone() {
        NameValueList nameValueList = new NameValueList();
        nameValueList.setSeparator(this.separator);
        Iterator<NameValue> it = this.hmap.values().iterator();
        while (it.hasNext()) {
            nameValueList.set((NameValue) it.next().clone());
        }
        return nameValueList;
    }

    @Override
    public int size() {
        return this.hmap.size();
    }

    @Override
    public boolean isEmpty() {
        return this.hmap.isEmpty();
    }

    public Iterator<NameValue> iterator() {
        return this.hmap.values().iterator();
    }

    public Iterator<String> getNames() {
        return this.hmap.keySet().iterator();
    }

    public String getParameter(String str) {
        Object value = getValue(str);
        if (value == null) {
            return null;
        }
        if (value instanceof GenericObject) {
            return ((GenericObject) value).encode();
        }
        return value.toString();
    }

    @Override
    public void clear() {
        this.hmap.clear();
    }

    @Override
    public boolean containsKey(Object obj) {
        return this.hmap.containsKey(obj.toString().toLowerCase());
    }

    @Override
    public boolean containsValue(Object obj) {
        return this.hmap.containsValue(obj);
    }

    @Override
    public Set<Map.Entry<String, NameValue>> entrySet() {
        return this.hmap.entrySet();
    }

    @Override
    public NameValue get(Object obj) {
        return this.hmap.get(obj.toString().toLowerCase());
    }

    @Override
    public Set<String> keySet() {
        return this.hmap.keySet();
    }

    @Override
    public NameValue put(String str, NameValue nameValue) {
        return this.hmap.put(str, nameValue);
    }

    @Override
    public void putAll(Map<? extends String, ? extends NameValue> map) {
        this.hmap.putAll(map);
    }

    @Override
    public NameValue remove(Object obj) {
        return this.hmap.remove(obj.toString().toLowerCase());
    }

    @Override
    public Collection<NameValue> values() {
        return this.hmap.values();
    }

    @Override
    public int hashCode() {
        return this.hmap.keySet().hashCode();
    }
}
