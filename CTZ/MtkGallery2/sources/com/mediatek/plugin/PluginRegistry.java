package com.mediatek.plugin;

import com.mediatek.plugin.element.Extension;
import com.mediatek.plugin.element.ExtensionPoint;
import com.mediatek.plugin.element.PluginDescriptor;
import com.mediatek.plugin.utils.Log;
import com.mediatek.plugin.utils.TraceHelper;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PluginRegistry {
    private String TAG = "PluginManager/PluginRegistry";
    private HashMap<String, PluginEntry> mAllPlugins = new HashMap<>();

    public PluginDescriptor getPluginDescriptor(String str) {
        Log.d(this.TAG, "<getPluginDescriptor> pluginId is " + str);
        if (this.mAllPlugins.isEmpty()) {
            Log.d(this.TAG, "<getPluginDescriptor> mAllPlugins is empty!");
            return null;
        }
        if (this.mAllPlugins.containsKey(str)) {
            return this.mAllPlugins.get(str).getDescriptor();
        }
        return null;
    }

    public ExtensionPoint getExtensionPointById(String str) {
        Log.d(this.TAG, "<getExtensionPointById> pointId is " + str);
        ExtensionPoint extensionPoint = null;
        if (this.mAllPlugins.isEmpty()) {
            Log.d(this.TAG, "<getExtensionPointById> mAllPlugins is empty!");
            return null;
        }
        Iterator<Map.Entry<String, PluginEntry>> it = this.mAllPlugins.entrySet().iterator();
        while (it.hasNext()) {
            PluginDescriptor descriptor = it.next().getValue().getDescriptor();
            if (descriptor == null) {
                Log.d(this.TAG, "<getExtensionPointById> descriptor is null");
            } else if (descriptor.getExtensionPoints() == null) {
                Log.d(this.TAG, "<getExtensionPointById> descriptor.getExtensionPoints() is null");
            } else {
                extensionPoint = descriptor.getExtensionPoints().get(str);
                if (extensionPoint != null) {
                    Log.d(this.TAG, "<getExtensionPointById> extentionPoint is not null, id " + str);
                    return extensionPoint;
                }
            }
        }
        return extensionPoint;
    }

    public ExtensionPoint getExtensionPointByClass(String str) {
        Log.d(this.TAG, "<getExtensionPointByClass> pointClassName is " + str);
        if (this.mAllPlugins.isEmpty()) {
            Log.d(this.TAG, "<getExtensionPointByClass> mAllPlugins is empty!");
            return null;
        }
        Iterator<Map.Entry<String, PluginEntry>> it = this.mAllPlugins.entrySet().iterator();
        while (it.hasNext()) {
            HashMap<String, ExtensionPoint> extensionPoints = it.next().getValue().getDescriptor().getExtensionPoints();
            if (extensionPoints != null && !extensionPoints.isEmpty()) {
                for (Map.Entry<String, ExtensionPoint> entry : extensionPoints.entrySet()) {
                    if (entry.getValue().className.equals(str)) {
                        return entry.getValue();
                    }
                }
            }
        }
        return null;
    }

    public String getPluginIdByExtensionClass(String str) {
        Log.d(this.TAG, "<getPluginIdByExtensionClass> extensionClassName is " + str);
        if (this.mAllPlugins.isEmpty()) {
            Log.d(this.TAG, "<getPluginIdByExtensionClass> mAllPlugins is empty!");
            return null;
        }
        for (Map.Entry<String, PluginEntry> entry : this.mAllPlugins.entrySet()) {
            HashMap<String, Extension> extension = entry.getValue().getDescriptor().getExtension();
            if (extension != null && !extension.isEmpty()) {
                Iterator<Map.Entry<String, Extension>> it = extension.entrySet().iterator();
                while (it.hasNext()) {
                    if (it.next().getValue().className.equals(str)) {
                        return entry.getKey();
                    }
                }
            }
        }
        return null;
    }

    public Plugin getPlugin(String str) {
        Log.d(this.TAG, "<getPlugin> pluginId is " + str);
        if (this.mAllPlugins.isEmpty()) {
            Log.d(this.TAG, "<getPlugin> mAllPlugins is empty!");
            return null;
        }
        if (this.mAllPlugins.containsKey(str)) {
            return this.mAllPlugins.get(str).getPlugin();
        }
        return null;
    }

    public synchronized void addPluginDescriptor(PluginDescriptor pluginDescriptor) {
        if (pluginDescriptor == null) {
            Log.d(this.TAG, "<addPluginDescriptor> pluginDescriptor is null!");
            return;
        }
        if (!this.mAllPlugins.isEmpty() && this.mAllPlugins.containsKey(pluginDescriptor.id)) {
            PluginDescriptor descriptor = this.mAllPlugins.get(pluginDescriptor.id).getDescriptor();
            if (descriptor != null && descriptor.version >= pluginDescriptor.version) {
                Log.d(this.TAG, "<addPluginDescriptor> already contains this pluginDescriptor!");
                return;
            } else {
                if (descriptor != null) {
                    descriptor.release();
                }
                this.mAllPlugins.remove(pluginDescriptor.id);
            }
        }
        this.mAllPlugins.put(pluginDescriptor.id, new PluginEntry(pluginDescriptor));
    }

    public void setPlugin(String str, Plugin plugin) {
        Log.d(this.TAG, "<setPlugin> pluginDescriptorId is " + str + ", plugin is " + plugin);
        if (this.mAllPlugins.isEmpty()) {
            Log.d(this.TAG, "<setPlugin> mAllPlugins is empty!");
        } else {
            this.mAllPlugins.get(str).setPlugin(plugin);
        }
    }

    public String[] getAllPluginsName() {
        if (this.mAllPlugins.isEmpty()) {
            Log.d(this.TAG, "<getAllPluginsName> mAllPlugins is empty!");
            return null;
        }
        String[] strArr = new String[this.mAllPlugins.size()];
        int i = 0;
        Iterator<Map.Entry<String, PluginEntry>> it = this.mAllPlugins.entrySet().iterator();
        while (it.hasNext()) {
            PluginDescriptor descriptor = it.next().getValue().getDescriptor();
            if (descriptor != null) {
                strArr[i] = descriptor.name;
                Log.d(this.TAG, "<getAllPluginsName> pluginsName[" + i + "] , " + strArr[i]);
                i++;
            }
        }
        return strArr;
    }

    public Set<String> getAllPluginsId() {
        return this.mAllPlugins.keySet();
    }

    public void generateRelationship() {
        TraceHelper.beginSection(">>>>PluginRegistry-generateRelationship");
        if (this.mAllPlugins.isEmpty()) {
            Log.d(this.TAG, "<generateRelationship> mAllPlugins is empty!");
            TraceHelper.endSection();
            return;
        }
        HashMap<String, Extension> map = new HashMap<>();
        HashMap<String, ExtensionPoint> map2 = new HashMap<>();
        getAllExtensionsAndPoints(map, map2);
        if (map.isEmpty() && map2.isEmpty()) {
            TraceHelper.endSection();
        } else {
            setConnectedExtensions(map, map2);
            TraceHelper.endSection();
        }
    }

    private void getAllExtensionsAndPoints(HashMap<String, Extension> map, HashMap<String, ExtensionPoint> map2) {
        TraceHelper.beginSection(">>>>PluginRegistry-getAllExtensionsAndPoints");
        Iterator<Map.Entry<String, PluginEntry>> it = this.mAllPlugins.entrySet().iterator();
        while (it.hasNext()) {
            PluginDescriptor descriptor = it.next().getValue().getDescriptor();
            if (descriptor == null) {
                TraceHelper.endSection();
                return;
            }
            setRequirePluginDes(descriptor);
            boolean z = true;
            if (!(descriptor.getExtension() == null || descriptor.getExtension().isEmpty())) {
                map.putAll(descriptor.getExtension());
            }
            if (descriptor.getExtensionPoints() != null && !descriptor.getExtensionPoints().isEmpty()) {
                z = false;
            }
            if (!z) {
                map2.putAll(descriptor.getExtensionPoints());
            }
        }
        TraceHelper.endSection();
    }

    private void setConnectedExtensions(HashMap<String, Extension> map, HashMap<String, ExtensionPoint> map2) {
        TraceHelper.beginSection(">>>>PluginRegistry-setConnectedExtensions");
        for (Map.Entry<String, ExtensionPoint> entry : map2.entrySet()) {
            HashMap<String, Extension> map3 = new HashMap<>();
            ExtensionPoint value = entry.getValue();
            for (Map.Entry<String, Extension> entry2 : map.entrySet()) {
                if (entry.getValue().id.equals(entry2.getValue().extensionPointId)) {
                    Log.d(this.TAG, "<generateRelationship> Extension " + entry.getValue().id);
                    map3.put(entry2.getKey(), entry2.getValue());
                }
            }
            if (map3.size() != 0 && value != null) {
                Log.d(this.TAG, "<generateRelationship> extensionPoint " + value.id);
                value.setConnectedExtensions(map3);
            }
        }
        TraceHelper.endSection();
    }

    private void setRequirePluginDes(PluginDescriptor pluginDescriptor) {
        TraceHelper.beginSection(">>>>PluginRegistry-setRequirePluginDes");
        String[] strArr = pluginDescriptor.requiredPluginIds;
        if (strArr != null && strArr.length > 0) {
            HashMap<String, PluginDescriptor> map = new HashMap<>();
            int length = strArr.length;
            for (int i = 0; i < length; i++) {
                PluginDescriptor pluginDescriptor2 = getPluginDescriptor(strArr[i]);
                if (pluginDescriptor2 != null) {
                    map.put(strArr[i], pluginDescriptor2);
                }
            }
            pluginDescriptor.setRequirePluginDes(map);
        }
        TraceHelper.endSection();
    }

    private class PluginEntry {
        private PluginDescriptor mDescriptor;
        private Plugin mPlugin;

        public PluginEntry(PluginDescriptor pluginDescriptor) {
            this.mDescriptor = pluginDescriptor;
        }

        public void setPlugin(Plugin plugin) {
            this.mPlugin = plugin;
        }

        public Plugin getPlugin() {
            return this.mPlugin;
        }

        public PluginDescriptor getDescriptor() {
            return this.mDescriptor;
        }
    }
}
