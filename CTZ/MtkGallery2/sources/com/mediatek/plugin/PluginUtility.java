package com.mediatek.plugin;

import android.content.Context;
import android.content.res.Resources;
import com.mediatek.plugin.element.Extension;
import com.mediatek.plugin.element.ExtensionPoint;
import com.mediatek.plugin.element.PluginDescriptor;
import com.mediatek.plugin.utils.Log;
import com.mediatek.plugin.utils.ReflectUtils;
import com.mediatek.plugin.utils.TraceHelper;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class PluginUtility {
    private static final String TAG = "PluginManager/PluginUtility";

    public static String[] getAllPluginsName(PluginManager pluginManager) {
        return pluginManager.getRegistry().getAllPluginsName();
    }

    public static Class<?> loadExtClass(PluginManager pluginManager, String str) {
        String pluginIdByExtensionClass = pluginManager.getRegistry().getPluginIdByExtensionClass(str);
        if (pluginIdByExtensionClass == null) {
            Log.d(TAG, "<loadExtClass> pluginId is null, valid!");
            return null;
        }
        Plugin plugin = pluginManager.getPlugin(pluginIdByExtensionClass);
        if (plugin == null) {
            Log.d(TAG, "<loadExtClass> plugin is null!");
            return null;
        }
        try {
            return plugin.getClassLoader().loadClass(str);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG, "<loadExtClass> ClassNotFoundException! className: " + str);
            return null;
        }
    }

    public static String[] getExtClassByClassName(PluginManager pluginManager, String str) {
        ExtensionPoint extensionPointByClass = pluginManager.getRegistry().getExtensionPointByClass(str);
        int i = 0;
        if (extensionPointByClass == null) {
            Log.d(TAG, "<getExtClassByClassName> extentionPoint is null!");
            return new String[0];
        }
        HashMap<String, Extension> connectedExtensions = extensionPointByClass.getConnectedExtensions();
        if (connectedExtensions == null) {
            Log.d(TAG, "<getExtClassByClassName> connectedExtentions is null!");
            return new String[0];
        }
        String[] strArr = new String[connectedExtensions.size()];
        Iterator<Map.Entry<String, Extension>> it = connectedExtensions.entrySet().iterator();
        while (it.hasNext()) {
            Extension value = it.next().getValue();
            if (value != null) {
                strArr[i] = value.className;
                Log.d(TAG, "<getExtClassByClassName> className[" + i + "] " + strArr[i]);
                i++;
            }
        }
        Log.d(TAG, "<getExtClassByClassName> class name is " + strArr);
        return strArr;
    }

    public static String[] getExtClassByClassName(PluginManager pluginManager, String str, String str2) {
        ExtensionPoint extensionPointByClass = pluginManager.getRegistry().getExtensionPointByClass(str);
        int i = 0;
        if (extensionPointByClass == null) {
            Log.d(TAG, "<getExtClassByClassName> extentionPoint is null!");
            return new String[0];
        }
        HashMap<String, Extension> connectedExtensions = extensionPointByClass.getConnectedExtensions();
        if (connectedExtensions == null) {
            Log.d(TAG, "<getExtClassByClassName> connectedExtentions is null!");
            return new String[0];
        }
        String[] strArr = new String[connectedExtensions.size()];
        Iterator<Map.Entry<String, Extension>> it = connectedExtensions.entrySet().iterator();
        while (it.hasNext()) {
            Extension value = it.next().getValue();
            if (value != null && str2.equals(value.pluginId)) {
                strArr[i] = value.className;
                Log.d(TAG, "<getExtClassByClassName> className[" + i + "] " + strArr[i]);
                i++;
            }
        }
        Log.d(TAG, "<getExtClassByClassName> class name is " + strArr);
        return strArr;
    }

    public static String[] getExtClass(PluginManager pluginManager, Class<?> cls) {
        return getExtClassByClassName(pluginManager, cls.getName());
    }

    public static String[] getExtClass(PluginManager pluginManager, Class<?> cls, String str) {
        return getExtClassByClassName(pluginManager, cls.getName(), str);
    }

    public static String[] getExtClassByPointId(PluginManager pluginManager, String str) {
        return getExtClassByClassName(pluginManager, pluginManager.getRegistry().getExtensionPointById(str).className);
    }

    public static String[] getExtClassByPointId(PluginManager pluginManager, String str, String str2) {
        return getExtClassByClassName(pluginManager, pluginManager.getRegistry().getExtensionPointById(str).className, str2);
    }

    public static String[] getExtNameByClassName(PluginManager pluginManager, String str) {
        ExtensionPoint extensionPointByClass = pluginManager.getRegistry().getExtensionPointByClass(str);
        int i = 0;
        if (extensionPointByClass == null) {
            Log.d(TAG, "<getExtNameByClassName> extentionPoint is null!");
            return new String[0];
        }
        HashMap<String, Extension> connectedExtensions = extensionPointByClass.getConnectedExtensions();
        if (connectedExtensions == null) {
            Log.d(TAG, "<getExtNameByClassName> connectedExtentions is null!");
            return new String[0];
        }
        String[] strArr = new String[connectedExtensions.size()];
        Iterator<Map.Entry<String, Extension>> it = connectedExtensions.entrySet().iterator();
        while (it.hasNext()) {
            Extension value = it.next().getValue();
            if (value != null) {
                strArr[i] = value.name;
                Log.d(TAG, "<getExtNameByClassName> extName[" + i + "] " + strArr[i]);
                i++;
            }
        }
        Log.d(TAG, "<getExtNameByClassName> class name is " + strArr);
        return strArr;
    }

    public static String[] getExtNameByClassName(PluginManager pluginManager, String str, String str2) {
        ExtensionPoint extensionPointByClass = pluginManager.getRegistry().getExtensionPointByClass(str);
        int i = 0;
        if (extensionPointByClass == null) {
            Log.d(TAG, "<getExtNameByClassName> extentionPoint is null!");
            return new String[0];
        }
        HashMap<String, Extension> connectedExtensions = extensionPointByClass.getConnectedExtensions();
        if (connectedExtensions == null) {
            Log.d(TAG, "<getExtNameByClassName> connectedExtentions is null!");
            return new String[0];
        }
        String[] strArr = new String[connectedExtensions.size()];
        Iterator<Map.Entry<String, Extension>> it = connectedExtensions.entrySet().iterator();
        while (it.hasNext()) {
            Extension value = it.next().getValue();
            if (value != null && str2.equals(value.pluginId)) {
                strArr[i] = value.name;
                Log.d(TAG, "<getExtNameByClassName> extName[" + i + "] " + strArr[i]);
                i++;
            }
        }
        Log.d(TAG, "<getExtNameByClassName> class name is " + strArr);
        return strArr;
    }

    public static String[] getExtName(PluginManager pluginManager, Class<?> cls) {
        return getExtNameByClassName(pluginManager, cls.getName());
    }

    public static String[] getExtName(PluginManager pluginManager, Class<?> cls, String str) {
        return getExtNameByClassName(pluginManager, cls.getName(), str);
    }

    public static String[] getExtNameByPointId(PluginManager pluginManager, String str) {
        return getExtNameByClassName(pluginManager, pluginManager.getRegistry().getExtensionPointById(str).className);
    }

    public static String[] getExtNameByPointId(PluginManager pluginManager, String str, String str2) {
        return getExtNameByClassName(pluginManager, pluginManager.getRegistry().getExtensionPointById(str).className, str2);
    }

    public static Map<String, Extension> getExt(PluginManager pluginManager, Class<?> cls) {
        TraceHelper.beginSection(">>>>PluginUtility-getExt");
        ExtensionPoint extensionPointByClass = pluginManager.getRegistry().getExtensionPointByClass(cls.getName());
        if (extensionPointByClass == null) {
            TraceHelper.endSection();
            return new HashMap();
        }
        TraceHelper.endSection();
        return extensionPointByClass.getConnectedExtensions();
    }

    public static Map<String, Extension> getExt(PluginManager pluginManager, Class<?> cls, String str) {
        return getExt(pluginManager.getRegistry().getExtensionPointByClass(cls.getName()), str);
    }

    public static Map<String, Extension> getExtByPointId(PluginManager pluginManager, String str) {
        ExtensionPoint extensionPointById = pluginManager.getRegistry().getExtensionPointById(str);
        if (extensionPointById == null) {
            Log.d(TAG, "<loadExtClass> extensionPoint is null!");
            return new HashMap();
        }
        return extensionPointById.getConnectedExtensions();
    }

    public static Map<String, Extension> getExtByPointId(PluginManager pluginManager, String str, String str2) {
        return getExt(pluginManager.getRegistry().getExtensionPointById(str), str2);
    }

    public static Map<String, Extension> getExtByClassName(PluginManager pluginManager, String str) {
        ExtensionPoint extensionPointByClass = pluginManager.getRegistry().getExtensionPointByClass(str);
        if (extensionPointByClass == null) {
            Log.d(TAG, "<loadExtClass> extensionPoint is null!");
            return new HashMap();
        }
        return extensionPointByClass.getConnectedExtensions();
    }

    public static Map<String, Extension> getExtByClassName(PluginManager pluginManager, String str, String str2) {
        return getExt(pluginManager.getRegistry().getExtensionPointByClass(str), str2);
    }

    public static Object createInstance(PluginManager pluginManager, Extension extension, Object... objArr) {
        TraceHelper.beginSection(">>>>PluginUtility-createInstance");
        Log.d(TAG, "<createInstance> Extension class name " + extension.className);
        Plugin plugin = pluginManager.getPlugin(extension.getParent().id);
        if (plugin == null) {
            Log.d(TAG, "<createInstance> plugin is null!");
            TraceHelper.endSection();
            return null;
        }
        try {
            Class<?> clsLoadClass = plugin.getClassLoader().loadClass(extension.className);
            Log.d(TAG, "<createInstance> clazz " + clsLoadClass);
            int length = objArr.length;
            if (length == 0) {
                Object objCreateInstance = ReflectUtils.createInstance(ReflectUtils.getConstructor(clsLoadClass, (Class<?>[]) new Class[0]), new Object[0]);
                TraceHelper.endSection();
                return objCreateInstance;
            }
            Class[] clsArr = new Class[length];
            for (int i = 0; i < length; i++) {
                if (objArr[i] != null) {
                    clsArr[i] = objArr[i].getClass();
                    if (objArr[i] instanceof Context) {
                        clsArr[i] = Context.class;
                    }
                    if (objArr[i] instanceof Resources) {
                        objArr[i] = plugin.getDescriptor().getResources();
                        clsArr[i] = Resources.class;
                    }
                    Log.d(TAG, "<createInstance> parameterTypes " + i + ":" + clsArr[i]);
                }
            }
            Constructor<?> constructor = ReflectUtils.getConstructor(clsLoadClass, (Class<?>[]) clsArr);
            Log.d(TAG, "<createInstance> cons " + constructor);
            if (constructor == null) {
                TraceHelper.endSection();
                return null;
            }
            Object objCreateInstance2 = ReflectUtils.createInstance(constructor, objArr);
            TraceHelper.endSection();
            return objCreateInstance2;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            TraceHelper.endSection();
            return null;
        }
    }

    public static Resources getResources(PluginManager pluginManager, String str) {
        PluginDescriptor descriptor;
        Plugin plugin = pluginManager.getPlugin(str);
        if (plugin == null || (descriptor = plugin.getDescriptor()) == null) {
            return null;
        }
        return descriptor.getResources();
    }

    private static Map<String, Extension> getExt(ExtensionPoint extensionPoint, String str) {
        if (extensionPoint == null) {
            Log.d(TAG, "<getExt> extensionPoint is null!");
            return new HashMap();
        }
        HashMap<String, Extension> connectedExtensions = extensionPoint.getConnectedExtensions();
        if (connectedExtensions == null || connectedExtensions.isEmpty()) {
            Log.d(TAG, "<getExt> connectedExtentions is null or connectedExtentions is empty!");
            return new HashMap();
        }
        HashMap map = new HashMap();
        for (Map.Entry<String, Extension> entry : connectedExtensions.entrySet()) {
            Extension value = entry.getValue();
            if (value != null && str.equals(value.pluginId)) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        return map;
    }
}
