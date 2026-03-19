package com.mediatek.plugin.element;

public class Parameter extends Element {
    public String value;

    @Override
    public void printAllKeyValue(String str) {
        super.printAllKeyValue(str);
        printKeyValue(str, "value", String.valueOf(this.value));
    }
}
