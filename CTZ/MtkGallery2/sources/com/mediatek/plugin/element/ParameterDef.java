package com.mediatek.plugin.element;

public class ParameterDef extends Element {
    public ParameterType type;

    public enum ParameterType {
        STRING,
        INT,
        LONG,
        FLOAT,
        DOUBLE,
        BOOLEAN
    }

    @Override
    public void printAllKeyValue(String str) {
        super.printAllKeyValue(str);
        printKeyValue(str, "type", String.valueOf(this.type));
    }
}
