package com.android.vcard;

import android.util.Log;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class VCardProperty {
    private byte[] mByteValue;
    private List<String> mGroupList;
    private String mName;
    private Map<String, Collection<String>> mParameterMap = new HashMap();
    private String mRawValue;
    private List<String> mValueList;

    public void setName(String str) {
        if (this.mName != null) {
            Log.w("MTK_vCard", String.format("Property name is re-defined (existing: %s, requested: %s", this.mName, str));
        }
        this.mName = str;
    }

    public void addGroup(String str) {
        if (this.mGroupList == null) {
            this.mGroupList = new ArrayList();
        }
        this.mGroupList.add(str);
    }

    public void addParameter(String str, String str2) {
        Collection<String> arrayList;
        if (!this.mParameterMap.containsKey(str)) {
            if (str.equals("TYPE")) {
                arrayList = new HashSet<>();
            } else {
                arrayList = new ArrayList<>();
            }
            this.mParameterMap.put(str, arrayList);
        } else {
            arrayList = this.mParameterMap.get(str);
        }
        arrayList.add(str2);
    }

    public void setRawValue(String str) {
        this.mRawValue = str;
    }

    public void setValues(String... strArr) {
        this.mValueList = Arrays.asList(strArr);
    }

    public void setValues(List<String> list) {
        this.mValueList = list;
    }

    public void setByteValue(byte[] bArr) {
        this.mByteValue = bArr;
    }

    public String getName() {
        return this.mName;
    }

    public Map<String, Collection<String>> getParameterMap() {
        return this.mParameterMap;
    }

    public Collection<String> getParameters(String str) {
        return this.mParameterMap.get(str);
    }

    public String getRawValue() {
        return this.mRawValue;
    }

    public List<String> getValueList() {
        return this.mValueList;
    }

    public byte[] getByteValue() {
        return this.mByteValue;
    }
}
