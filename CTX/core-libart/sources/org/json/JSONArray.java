package org.json;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.json.JSONStringer;

public class JSONArray {
    private final List<Object> values;

    public JSONArray() {
        this.values = new ArrayList();
    }

    public JSONArray(Collection collection) {
        this();
        if (collection != null) {
            Iterator it = collection.iterator();
            while (it.hasNext()) {
                put(JSONObject.wrap(it.next()));
            }
        }
    }

    public JSONArray(JSONTokener jSONTokener) throws JSONException {
        Object objNextValue = jSONTokener.nextValue();
        if (objNextValue instanceof JSONArray) {
            this.values = ((JSONArray) objNextValue).values;
            return;
        }
        throw JSON.typeMismatch(objNextValue, "JSONArray");
    }

    public JSONArray(String str) throws JSONException {
        this(new JSONTokener(str));
    }

    public JSONArray(Object obj) throws JSONException {
        if (!obj.getClass().isArray()) {
            throw new JSONException("Not a primitive array: " + obj.getClass());
        }
        int length = Array.getLength(obj);
        this.values = new ArrayList(length);
        for (int i = 0; i < length; i++) {
            put(JSONObject.wrap(Array.get(obj, i)));
        }
    }

    public int length() {
        return this.values.size();
    }

    public JSONArray put(boolean z) {
        this.values.add(Boolean.valueOf(z));
        return this;
    }

    public JSONArray put(double d) throws JSONException {
        this.values.add(Double.valueOf(JSON.checkDouble(d)));
        return this;
    }

    public JSONArray put(int i) {
        this.values.add(Integer.valueOf(i));
        return this;
    }

    public JSONArray put(long j) {
        this.values.add(Long.valueOf(j));
        return this;
    }

    public JSONArray put(Object obj) {
        this.values.add(obj);
        return this;
    }

    void checkedPut(Object obj) throws JSONException {
        if (obj instanceof Number) {
            JSON.checkDouble(((Number) obj).doubleValue());
        }
        put(obj);
    }

    public JSONArray put(int i, boolean z) throws JSONException {
        return put(i, Boolean.valueOf(z));
    }

    public JSONArray put(int i, double d) throws JSONException {
        return put(i, Double.valueOf(d));
    }

    public JSONArray put(int i, int i2) throws JSONException {
        return put(i, Integer.valueOf(i2));
    }

    public JSONArray put(int i, long j) throws JSONException {
        return put(i, Long.valueOf(j));
    }

    public JSONArray put(int i, Object obj) throws JSONException {
        if (obj instanceof Number) {
            JSON.checkDouble(((Number) obj).doubleValue());
        }
        while (this.values.size() <= i) {
            this.values.add(null);
        }
        this.values.set(i, obj);
        return this;
    }

    public boolean isNull(int i) {
        Object objOpt = opt(i);
        return objOpt == null || objOpt == JSONObject.NULL;
    }

    public Object get(int i) throws JSONException {
        try {
            Object obj = this.values.get(i);
            if (obj == null) {
                throw new JSONException("Value at " + i + " is null.");
            }
            return obj;
        } catch (IndexOutOfBoundsException e) {
            throw new JSONException("Index " + i + " out of range [0.." + this.values.size() + ")", e);
        }
    }

    public Object opt(int i) {
        if (i < 0 || i >= this.values.size()) {
            return null;
        }
        return this.values.get(i);
    }

    public Object remove(int i) {
        if (i < 0 || i >= this.values.size()) {
            return null;
        }
        return this.values.remove(i);
    }

    public boolean getBoolean(int i) throws JSONException {
        Object obj = get(i);
        Boolean bool = JSON.toBoolean(obj);
        if (bool == null) {
            throw JSON.typeMismatch(Integer.valueOf(i), obj, "boolean");
        }
        return bool.booleanValue();
    }

    public boolean optBoolean(int i) {
        return optBoolean(i, false);
    }

    public boolean optBoolean(int i, boolean z) {
        Boolean bool = JSON.toBoolean(opt(i));
        return bool != null ? bool.booleanValue() : z;
    }

    public double getDouble(int i) throws JSONException {
        Object obj = get(i);
        Double d = JSON.toDouble(obj);
        if (d == null) {
            throw JSON.typeMismatch(Integer.valueOf(i), obj, "double");
        }
        return d.doubleValue();
    }

    public double optDouble(int i) {
        return optDouble(i, Double.NaN);
    }

    public double optDouble(int i, double d) {
        Double d2 = JSON.toDouble(opt(i));
        return d2 != null ? d2.doubleValue() : d;
    }

    public int getInt(int i) throws JSONException {
        Object obj = get(i);
        Integer integer = JSON.toInteger(obj);
        if (integer == null) {
            throw JSON.typeMismatch(Integer.valueOf(i), obj, "int");
        }
        return integer.intValue();
    }

    public int optInt(int i) {
        return optInt(i, 0);
    }

    public int optInt(int i, int i2) {
        Integer integer = JSON.toInteger(opt(i));
        return integer != null ? integer.intValue() : i2;
    }

    public long getLong(int i) throws JSONException {
        Object obj = get(i);
        Long l = JSON.toLong(obj);
        if (l == null) {
            throw JSON.typeMismatch(Integer.valueOf(i), obj, "long");
        }
        return l.longValue();
    }

    public long optLong(int i) {
        return optLong(i, 0L);
    }

    public long optLong(int i, long j) {
        Long l = JSON.toLong(opt(i));
        return l != null ? l.longValue() : j;
    }

    public String getString(int i) throws JSONException {
        Object obj = get(i);
        String string = JSON.toString(obj);
        if (string == null) {
            throw JSON.typeMismatch(Integer.valueOf(i), obj, "String");
        }
        return string;
    }

    public String optString(int i) {
        return optString(i, "");
    }

    public String optString(int i, String str) {
        String string = JSON.toString(opt(i));
        return string != null ? string : str;
    }

    public JSONArray getJSONArray(int i) throws JSONException {
        Object obj = get(i);
        if (obj instanceof JSONArray) {
            return (JSONArray) obj;
        }
        throw JSON.typeMismatch(Integer.valueOf(i), obj, "JSONArray");
    }

    public JSONArray optJSONArray(int i) {
        Object objOpt = opt(i);
        if (objOpt instanceof JSONArray) {
            return (JSONArray) objOpt;
        }
        return null;
    }

    public JSONObject getJSONObject(int i) throws JSONException {
        Object obj = get(i);
        if (obj instanceof JSONObject) {
            return (JSONObject) obj;
        }
        throw JSON.typeMismatch(Integer.valueOf(i), obj, "JSONObject");
    }

    public JSONObject optJSONObject(int i) {
        Object objOpt = opt(i);
        if (objOpt instanceof JSONObject) {
            return (JSONObject) objOpt;
        }
        return null;
    }

    public JSONObject toJSONObject(JSONArray jSONArray) throws JSONException {
        JSONObject jSONObject = new JSONObject();
        int iMin = Math.min(jSONArray.length(), this.values.size());
        if (iMin == 0) {
            return null;
        }
        for (int i = 0; i < iMin; i++) {
            jSONObject.put(JSON.toString(jSONArray.opt(i)), opt(i));
        }
        return jSONObject;
    }

    public String join(String str) throws JSONException {
        JSONStringer jSONStringer = new JSONStringer();
        jSONStringer.open(JSONStringer.Scope.NULL, "");
        int size = this.values.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                jSONStringer.out.append(str);
            }
            jSONStringer.value(this.values.get(i));
        }
        jSONStringer.close(JSONStringer.Scope.NULL, JSONStringer.Scope.NULL, "");
        return jSONStringer.out.toString();
    }

    public String toString() {
        try {
            JSONStringer jSONStringer = new JSONStringer();
            writeTo(jSONStringer);
            return jSONStringer.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public String toString(int i) throws JSONException {
        JSONStringer jSONStringer = new JSONStringer(i);
        writeTo(jSONStringer);
        return jSONStringer.toString();
    }

    void writeTo(JSONStringer jSONStringer) throws JSONException {
        jSONStringer.array();
        Iterator<Object> it = this.values.iterator();
        while (it.hasNext()) {
            jSONStringer.value(it.next());
        }
        jSONStringer.endArray();
    }

    public boolean equals(Object obj) {
        return (obj instanceof JSONArray) && ((JSONArray) obj).values.equals(this.values);
    }

    public int hashCode() {
        return this.values.hashCode();
    }
}
