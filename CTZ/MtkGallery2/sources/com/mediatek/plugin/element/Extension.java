package com.mediatek.plugin.element;

import android.graphics.drawable.Drawable;
import com.mediatek.plugin.builder.PluginDescriptorBuilder;
import com.mediatek.plugin.element.ParameterDef;
import com.mediatek.plugin.utils.Log;

public class Extension extends Element {
    private static final String TAG = "PluginManager/Extension";
    public String className;
    public Drawable drawable;
    public String extensionPointId;
    private ExtensionPoint mExtensionPoint;
    public String name;
    public String pluginId;

    public void setExtensionPoint(ExtensionPoint extensionPoint) {
        this.mExtensionPoint = extensionPoint;
    }

    public Element getParameter(String str) {
        return this.mChilds.get(Parameter.class).get(str);
    }

    public int getInt(String str, int i) {
        if (this.mExtensionPoint.getParameterType(str) == ParameterDef.ParameterType.INT) {
            Parameter parameter = (Parameter) getParameter(str);
            if (parameter != null && parameter.value != null) {
                try {
                    return Integer.valueOf(parameter.value).intValue();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "<getInt>", e);
                }
            }
            return i;
        }
        throw new RuntimeException("Please Check thes Type !");
    }

    public double getDouble(String str, double d) {
        if (this.mExtensionPoint.getParameterType(str) == ParameterDef.ParameterType.DOUBLE) {
            Parameter parameter = (Parameter) getParameter(str);
            if (parameter != null && parameter.value != null) {
                try {
                    return Double.valueOf(parameter.value).doubleValue();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "<getDouble>", e);
                }
            }
            return d;
        }
        throw new RuntimeException("Please Check the Type !");
    }

    public float getFloat(String str, float f) {
        if (this.mExtensionPoint.getParameterType(str) == ParameterDef.ParameterType.FLOAT) {
            Parameter parameter = (Parameter) getParameter(str);
            if (parameter != null && parameter.value != null) {
                try {
                    return Float.valueOf(parameter.value).floatValue();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "<getFloat>", e);
                }
            }
            return f;
        }
        throw new RuntimeException("Please Check the Type !");
    }

    public String getString(String str, String str2) {
        if (this.mExtensionPoint.getParameterType(str) == ParameterDef.ParameterType.STRING) {
            Parameter parameter = (Parameter) getParameter(str);
            if (parameter != null && parameter.value != null) {
                return parameter.value;
            }
            return str2;
        }
        throw new RuntimeException("Please Check the Type !");
    }

    public boolean getBoolean(String str, boolean z) {
        if (this.mExtensionPoint.getParameterType(str) == ParameterDef.ParameterType.BOOLEAN) {
            Parameter parameter = (Parameter) getParameter(str);
            if (parameter != null && parameter.value != null) {
                return Boolean.parseBoolean(parameter.value);
            }
            return z;
        }
        throw new RuntimeException("Please Check the Type !");
    }

    public long getLong(String str, long j) {
        if (this.mExtensionPoint.getParameterType(str) == ParameterDef.ParameterType.LONG) {
            Parameter parameter = (Parameter) getParameter(str);
            if (parameter != null && parameter.value != null) {
                try {
                    return Long.valueOf(parameter.value).longValue();
                } catch (NumberFormatException e) {
                    Log.e(TAG, "<getLong>", e);
                }
            }
            return j;
        }
        throw new RuntimeException("Please Check the Type !");
    }

    @Override
    public void printAllKeyValue(String str) {
        super.printAllKeyValue(str);
        printKeyValue(str, PluginDescriptorBuilder.VALUE_NAME, String.valueOf(this.name));
        printKeyValue(str, "pluginId", String.valueOf(this.pluginId));
        printKeyValue(str, "extensionPointId", String.valueOf(this.extensionPointId));
        printKeyValue(str, "drawable", String.valueOf(this.drawable));
        printKeyValue(str, "className", String.valueOf(this.className));
    }
}
