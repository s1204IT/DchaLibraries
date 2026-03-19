package com.mediatek.vcalendar.component;

import android.content.ContentValues;
import android.content.Context;
import com.mediatek.vcalendar.SingleComponentContentValues;
import com.mediatek.vcalendar.VCalendarException;
import com.mediatek.vcalendar.property.Property;
import com.mediatek.vcalendar.utils.LogUtil;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Component {
    protected Context mContext;
    protected final String mName;
    private final Component mParent;
    protected LinkedList<Component> mChildrenList = new LinkedList<>();
    protected final LinkedHashMap<String, ArrayList<Property>> mPropsMap = new LinkedHashMap<>();

    public Component(String str, Component component) {
        this.mName = str;
        this.mParent = component;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public String getName() {
        return this.mName;
    }

    public Component getParent() {
        return this.mParent;
    }

    public void addChild(Component component) {
        this.mChildrenList.add(component);
    }

    public List<Component> getComponents() {
        return this.mChildrenList;
    }

    public void addProperty(Property property) {
        String name = property.getName();
        ArrayList<Property> arrayList = this.mPropsMap.get(name);
        if (arrayList == null) {
            arrayList = new ArrayList<>();
            this.mPropsMap.put(name, arrayList);
        }
        arrayList.add(property);
        property.setComponent(this);
    }

    public long getDtStart() {
        return -1L;
    }

    public long getDtEnd() {
        return -1L;
    }

    public Set<String> getPropertyNames() {
        return this.mPropsMap.keySet();
    }

    public List<Property> getProperties(String str) {
        ArrayList<Property> arrayList = this.mPropsMap.get(str);
        if (arrayList != null) {
            return arrayList;
        }
        return new ArrayList();
    }

    public Property getFirstProperty(String str) {
        ArrayList<Property> arrayList = this.mPropsMap.get(str);
        if (arrayList == null || arrayList.isEmpty()) {
            return null;
        }
        return arrayList.get(0);
    }

    public void writeInfoToContentValues(SingleComponentContentValues singleComponentContentValues) throws VCalendarException {
        LogUtil.i("Component", "writeInfoToContentValues() not implemented in: " + this.mName);
    }

    protected void writeInfoToContentValues(ContentValues contentValues) throws VCalendarException {
        if (contentValues == null) {
            throw new VCalendarException("writeInfoToContentValues(): ContentValues is null.");
        }
    }

    protected void writeInfoToContentValues(LinkedList<ContentValues> linkedList) throws VCalendarException {
        if (linkedList == null) {
            throw new VCalendarException("writeInfoToContentValues(): ContentValues list is null.");
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        toString(sb);
        sb.append("\r\n");
        return sb.toString();
    }

    public void toString(StringBuilder sb) {
        if (sb == null) {
            return;
        }
        sb.append("BEGIN");
        sb.append(":");
        sb.append(this.mName);
        sb.append("\r\n");
        Iterator<String> it = getPropertyNames().iterator();
        while (it.hasNext()) {
            Iterator<Property> it2 = getProperties(it.next()).iterator();
            while (it2.hasNext()) {
                it2.next().toString(sb);
                sb.append("\r\n");
            }
        }
        if (this.mChildrenList != null) {
            Iterator<Component> it3 = this.mChildrenList.iterator();
            while (it3.hasNext()) {
                it3.next().toString(sb);
                sb.append("\r\n");
            }
        }
        sb.append("END");
        sb.append(":");
        sb.append(this.mName);
    }
}
