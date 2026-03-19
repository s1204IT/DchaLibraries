package com.mediatek.vcalendar.property;

import android.content.ContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.component.Component;
import com.mediatek.vcalendar.parameter.Parameter;
import com.mediatek.vcalendar.utils.LogUtil;
import com.mediatek.vcalendar.valuetype.Text;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Property {
    protected Component mComponent;
    protected final String mName;
    protected LinkedHashMap<String, ArrayList<Parameter>> mParamsMap = new LinkedHashMap<>();
    protected String mValue;

    public Property(String str, String str2) {
        this.mName = str;
        this.mValue = str2;
    }

    public String getName() {
        return this.mName;
    }

    public String getValue() {
        return this.mValue;
    }

    public void setValue(String str, Parameter parameter) {
        this.mValue = str;
        if (parameter != null) {
            this.mValue = Text.decode(this.mValue, parameter.getValue());
        }
        if ("SUMMARY".equals(this.mName) || "DESCRIPTION".equals(this.mName) || "LOCATION".equals(this.mName)) {
            handleEscapedChar();
        }
    }

    public void setComponent(Component component) {
        this.mComponent = component;
        setComponentInParams();
    }

    protected void setComponentInParams() {
        Set<String> setKeySet = this.mParamsMap.keySet();
        if (!setKeySet.isEmpty()) {
            Iterator<String> it = setKeySet.iterator();
            while (it.hasNext()) {
                ArrayList<Parameter> arrayList = this.mParamsMap.get(it.next());
                if (!arrayList.isEmpty()) {
                    int size = arrayList.size();
                    for (int i = 0; i < size; i++) {
                        arrayList.get(i).setComponent(this.mComponent);
                    }
                }
            }
        }
    }

    public void addParameter(Parameter parameter) {
        ArrayList<Parameter> arrayList = this.mParamsMap.get(parameter.getName());
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mParamsMap.put(parameter.getName(), arrayList);
        }
        arrayList.add(parameter);
        parameter.setComponent(this.mComponent);
    }

    public Set<String> getParameterNames() {
        return this.mParamsMap.keySet();
    }

    public List<Parameter> getParameters(String str) {
        ArrayList<Parameter> arrayList = this.mParamsMap.get(str);
        if (arrayList == null) {
            return new ArrayList();
        }
        return arrayList;
    }

    public Parameter getFirstParameter(String str) {
        ArrayList<Parameter> arrayList = this.mParamsMap.get(str);
        if (arrayList == null || arrayList.isEmpty()) {
            return null;
        }
        return arrayList.get(0);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        sb.append(this.mName);
        Iterator<String> it = getParameterNames().iterator();
        while (it.hasNext()) {
            for (Parameter parameter : getParameters(it.next())) {
                sb.append(";");
                parameter.toString(sb);
            }
        }
        sb.append(":");
        if ("SUMMARY".equals(this.mName) || "DESCRIPTION".equals(this.mName) || "LOCATION".equals(this.mName)) {
            escapeChar();
        }
        String strEncoding = this.mValue;
        Parameter firstParameter = getFirstParameter("ENCODING");
        if (firstParameter != null) {
            strEncoding = Text.encoding(this.mValue, firstParameter.getValue());
        }
        sb.append(strEncoding);
    }

    public void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        if (contentValues == null) {
            LogUtil.e("Property", "writeInfoToContentValues(): the argument ContentValue must not be null.");
            throw new VCalendarException();
        }
    }

    public void writeInfoToContentValues(LinkedList<ContentValues> linkedList) throws VCalendarException {
        if (linkedList == null) {
            LogUtil.e("Property", "writeInfoToContentValues(): the argument ContentValue must not be null.");
            throw new VCalendarException();
        }
    }

    protected void handleEscapedChar() {
        LogUtil.d("Property", "handleEscapedChar(),before mValue:" + this.mValue);
        this.mValue = this.mValue.replace("\\\\", "\\");
        this.mValue = this.mValue.replace("\\;", ";");
        this.mValue = this.mValue.replace("\\,", ",");
        this.mValue = this.mValue.replace("\\N", "\n");
        this.mValue = this.mValue.replace("\\n", "\n");
        LogUtil.d("Property", "handleEscapedChar(), after mValue: " + this.mValue);
    }

    protected void escapeChar() {
        LogUtil.d("Property", "escapeChar(), before mValue: " + this.mValue);
        this.mValue = this.mValue.replace("\\", "\\\\");
        this.mValue = this.mValue.replace(";", "\\;");
        this.mValue = this.mValue.replace(",", "\\,");
        this.mValue = this.mValue.replace("\n", "\\n");
        LogUtil.d("Property", "escapeChar(), after mValue: " + this.mValue);
    }
}
