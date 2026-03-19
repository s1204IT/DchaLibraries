package com.mediatek.plugin.element;

import com.mediatek.plugin.builder.PluginDescriptorBuilder;
import com.mediatek.plugin.utils.Log;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Element {
    private static final String TAG = "PluginManager/Element";
    public String id;
    protected HashMap<Class<? extends Element>, HashMap<String, Element>> mChilds = new HashMap<>();
    protected Element mParent = null;

    public void addChild(Element element) {
        if (element == null) {
            return;
        }
        HashMap<String, Element> map = this.mChilds.get(element.getClass());
        if (map == null) {
            map = new HashMap<>();
            this.mChilds.put((Class<? extends Element>) element.getClass(), map);
        }
        map.put(element.id, element);
        Log.d(TAG, "<addChild> this = " + this + ", child class = " + element.getClass() + ", child id = " + element.id);
    }

    public void setParent(Element element) {
        this.mParent = element;
    }

    public Element getParent() {
        return this.mParent;
    }

    public final void printf() {
        printf("", true);
    }

    private void printf(String str, boolean z) {
        if (z) {
            Log.d(TAG, str + "class = " + toString());
        } else {
            printKeyValue(str, "child class", toString());
            str = str + "|    ";
        }
        printAllKeyValue(str);
        Iterator<Map.Entry<Class<? extends Element>, HashMap<String, Element>>> it = this.mChilds.entrySet().iterator();
        while (it.hasNext()) {
            Iterator<Map.Entry<String, Element>> it2 = it.next().getValue().entrySet().iterator();
            while (it2.hasNext()) {
                it2.next().getValue().printf(str, false);
            }
        }
    }

    protected final void printKeyValue(String str, String str2, String str3) {
        Log.d(TAG, str + "|");
        Log.d(TAG, str + "+----" + str2 + " = " + str3);
    }

    protected final void printValue(String str, String str2) {
        Log.d(TAG, str + "|");
        Log.d(TAG, str + "+----" + str2);
    }

    protected void printAllKeyValue(String str) {
        printKeyValue(str, PluginDescriptorBuilder.VALUE_ID, this.id);
    }
}
