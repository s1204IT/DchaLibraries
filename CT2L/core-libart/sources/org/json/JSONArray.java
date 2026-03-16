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

    public JSONArray(Collection copyFrom) {
        this();
        if (copyFrom != null) {
            Iterator it = copyFrom.iterator();
            while (it.hasNext()) {
                put(JSONObject.wrap(it.next()));
            }
        }
    }

    public JSONArray(JSONTokener readFrom) throws JSONException {
        Object object = readFrom.nextValue();
        if (object instanceof JSONArray) {
            this.values = ((JSONArray) object).values;
            return;
        }
        throw JSON.typeMismatch(object, "JSONArray");
    }

    public JSONArray(String json) throws JSONException {
        this(new JSONTokener(json));
    }

    public JSONArray(Object array) throws JSONException {
        if (!array.getClass().isArray()) {
            throw new JSONException("Not a primitive array: " + array.getClass());
        }
        int length = Array.getLength(array);
        this.values = new ArrayList(length);
        for (int i = 0; i < length; i++) {
            put(JSONObject.wrap(Array.get(array, i)));
        }
    }

    public int length() {
        return this.values.size();
    }

    public JSONArray put(boolean value) {
        this.values.add(Boolean.valueOf(value));
        return this;
    }

    public JSONArray put(double value) throws JSONException {
        this.values.add(Double.valueOf(JSON.checkDouble(value)));
        return this;
    }

    public JSONArray put(int value) {
        this.values.add(Integer.valueOf(value));
        return this;
    }

    public JSONArray put(long value) {
        this.values.add(Long.valueOf(value));
        return this;
    }

    public JSONArray put(Object value) {
        this.values.add(value);
        return this;
    }

    void checkedPut(Object value) throws JSONException {
        if (value instanceof Number) {
            JSON.checkDouble(((Number) value).doubleValue());
        }
        put(value);
    }

    public JSONArray put(int index, boolean value) throws JSONException {
        return put(index, Boolean.valueOf(value));
    }

    public JSONArray put(int index, double value) throws JSONException {
        return put(index, Double.valueOf(value));
    }

    public JSONArray put(int index, int value) throws JSONException {
        return put(index, Integer.valueOf(value));
    }

    public JSONArray put(int index, long value) throws JSONException {
        return put(index, Long.valueOf(value));
    }

    public JSONArray put(int index, Object value) throws JSONException {
        if (value instanceof Number) {
            JSON.checkDouble(((Number) value).doubleValue());
        }
        while (this.values.size() <= index) {
            this.values.add(null);
        }
        this.values.set(index, value);
        return this;
    }

    public boolean isNull(int index) {
        Object value = opt(index);
        return value == null || value == JSONObject.NULL;
    }

    public Object get(int index) throws JSONException {
        try {
            Object value = this.values.get(index);
            if (value == null) {
                throw new JSONException("Value at " + index + " is null.");
            }
            return value;
        } catch (IndexOutOfBoundsException e) {
            throw new JSONException("Index " + index + " out of range [0.." + this.values.size() + ")");
        }
    }

    public Object opt(int index) {
        if (index < 0 || index >= this.values.size()) {
            return null;
        }
        return this.values.get(index);
    }

    public Object remove(int index) {
        if (index < 0 || index >= this.values.size()) {
            return null;
        }
        return this.values.remove(index);
    }

    public boolean getBoolean(int index) throws JSONException {
        Object object = get(index);
        Boolean result = JSON.toBoolean(object);
        if (result == null) {
            throw JSON.typeMismatch(Integer.valueOf(index), object, "boolean");
        }
        return result.booleanValue();
    }

    public boolean optBoolean(int index) {
        return optBoolean(index, false);
    }

    public boolean optBoolean(int index, boolean fallback) {
        Object object = opt(index);
        Boolean result = JSON.toBoolean(object);
        if (result == null) {
            return fallback;
        }
        boolean fallback2 = result.booleanValue();
        return fallback2;
    }

    public double getDouble(int index) throws JSONException {
        Object object = get(index);
        Double result = JSON.toDouble(object);
        if (result == null) {
            throw JSON.typeMismatch(Integer.valueOf(index), object, "double");
        }
        return result.doubleValue();
    }

    public double optDouble(int index) {
        return optDouble(index, Double.NaN);
    }

    public double optDouble(int index, double fallback) {
        Object object = opt(index);
        Double result = JSON.toDouble(object);
        if (result == null) {
            return fallback;
        }
        double fallback2 = result.doubleValue();
        return fallback2;
    }

    public int getInt(int index) throws JSONException {
        Object object = get(index);
        Integer result = JSON.toInteger(object);
        if (result == null) {
            throw JSON.typeMismatch(Integer.valueOf(index), object, "int");
        }
        return result.intValue();
    }

    public int optInt(int index) {
        return optInt(index, 0);
    }

    public int optInt(int index, int fallback) {
        Object object = opt(index);
        Integer result = JSON.toInteger(object);
        if (result == null) {
            return fallback;
        }
        int fallback2 = result.intValue();
        return fallback2;
    }

    public long getLong(int index) throws JSONException {
        Object object = get(index);
        Long result = JSON.toLong(object);
        if (result == null) {
            throw JSON.typeMismatch(Integer.valueOf(index), object, "long");
        }
        return result.longValue();
    }

    public long optLong(int index) {
        return optLong(index, 0L);
    }

    public long optLong(int index, long fallback) {
        Object object = opt(index);
        Long result = JSON.toLong(object);
        if (result == null) {
            return fallback;
        }
        long fallback2 = result.longValue();
        return fallback2;
    }

    public String getString(int index) throws JSONException {
        Object object = get(index);
        String result = JSON.toString(object);
        if (result == null) {
            throw JSON.typeMismatch(Integer.valueOf(index), object, "String");
        }
        return result;
    }

    public String optString(int index) {
        return optString(index, "");
    }

    public String optString(int index, String fallback) {
        Object object = opt(index);
        String result = JSON.toString(object);
        return result != null ? result : fallback;
    }

    public JSONArray getJSONArray(int index) throws JSONException {
        Object object = get(index);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        throw JSON.typeMismatch(Integer.valueOf(index), object, "JSONArray");
    }

    public JSONArray optJSONArray(int index) {
        Object object = opt(index);
        if (object instanceof JSONArray) {
            return (JSONArray) object;
        }
        return null;
    }

    public JSONObject getJSONObject(int index) throws JSONException {
        Object object = get(index);
        if (object instanceof JSONObject) {
            return (JSONObject) object;
        }
        throw JSON.typeMismatch(Integer.valueOf(index), object, "JSONObject");
    }

    public JSONObject optJSONObject(int index) {
        Object object = opt(index);
        if (object instanceof JSONObject) {
            return (JSONObject) object;
        }
        return null;
    }

    public JSONObject toJSONObject(JSONArray names) throws JSONException {
        JSONObject result = new JSONObject();
        int length = Math.min(names.length(), this.values.size());
        if (length == 0) {
            return null;
        }
        for (int i = 0; i < length; i++) {
            String name = JSON.toString(names.opt(i));
            result.put(name, opt(i));
        }
        return result;
    }

    public String join(String separator) throws JSONException {
        JSONStringer stringer = new JSONStringer();
        stringer.open(JSONStringer.Scope.NULL, "");
        int size = this.values.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                stringer.out.append(separator);
            }
            stringer.value(this.values.get(i));
        }
        stringer.close(JSONStringer.Scope.NULL, JSONStringer.Scope.NULL, "");
        return stringer.out.toString();
    }

    public String toString() {
        try {
            JSONStringer stringer = new JSONStringer();
            writeTo(stringer);
            return stringer.toString();
        } catch (JSONException e) {
            return null;
        }
    }

    public String toString(int indentSpaces) throws JSONException {
        JSONStringer stringer = new JSONStringer(indentSpaces);
        writeTo(stringer);
        return stringer.toString();
    }

    void writeTo(JSONStringer stringer) throws JSONException {
        stringer.array();
        for (Object value : this.values) {
            stringer.value(value);
        }
        stringer.endArray();
    }

    public boolean equals(Object o) {
        return (o instanceof JSONArray) && ((JSONArray) o).values.equals(this.values);
    }

    public int hashCode() {
        return this.values.hashCode();
    }
}
