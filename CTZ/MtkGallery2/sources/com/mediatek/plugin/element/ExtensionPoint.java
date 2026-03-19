package com.mediatek.plugin.element;

import com.mediatek.plugin.element.ParameterDef;
import java.util.HashMap;
import java.util.Map;

public class ExtensionPoint extends Element {
    public String className;
    private HashMap<String, Extension> mConnectedExtensions;

    public HashMap<String, Extension> getConnectedExtensions() {
        return this.mConnectedExtensions;
    }

    public void setConnectedExtensions(HashMap<String, Extension> map) {
        this.mConnectedExtensions = map;
    }

    public ParameterDef.ParameterType getParameterType(String str) {
        HashMap<String, Element> map = this.mChilds.get(ParameterDef.class);
        if (map != null) {
            return ((ParameterDef) map.get(str)).type;
        }
        return null;
    }

    @Override
    public void printAllKeyValue(String str) {
        super.printAllKeyValue(str);
        printKeyValue(str, "className", String.valueOf(this.className));
        if (this.mConnectedExtensions != null) {
            printKeyValue(str, "mConnectedExtensions size ", String.valueOf(this.mConnectedExtensions.size()));
            for (Map.Entry<String, Extension> entry : this.mConnectedExtensions.entrySet()) {
                str = str + "|    ";
                printValue(str, "id = " + entry.getKey() + " extension = " + entry.getValue().toString());
            }
            return;
        }
        printKeyValue(str, "mConnectedExtensions size ", String.valueOf(0));
    }
}
