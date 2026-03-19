package com.mediatek.plugin;

import dalvik.system.DexClassLoader;
import java.util.ArrayList;

class PluginClassLoader extends DexClassLoader {
    private ArrayList<ClassLoader> mRequiredLoaders;

    public PluginClassLoader(String str, String str2, String str3, ClassLoader classLoader) {
        super(str, str2, str3, classLoader);
    }

    @Override
    public Class<?> loadClass(String str) throws ClassNotFoundException {
        Class<?> clsLoadClass;
        int size;
        try {
            clsLoadClass = getParent().loadClass(str);
        } catch (ClassNotFoundException e) {
            clsLoadClass = null;
        }
        if (clsLoadClass != null) {
            return clsLoadClass;
        }
        try {
            clsLoadClass = super.loadClass(str);
        } catch (ClassNotFoundException e2) {
        }
        if (clsLoadClass != null) {
            return clsLoadClass;
        }
        if (this.mRequiredLoaders == null || (size = this.mRequiredLoaders.size()) <= 0) {
            return null;
        }
        for (int i = 0; i < size; i++) {
            try {
                clsLoadClass = this.mRequiredLoaders.get(i).loadClass(str);
            } catch (ClassNotFoundException e3) {
            }
            if (clsLoadClass != null) {
                return clsLoadClass;
            }
        }
        throw new ClassNotFoundException("Cannot find " + str, new Throwable());
    }

    public void setRequiredClassLoader(ArrayList<ClassLoader> arrayList) {
        this.mRequiredLoaders = arrayList;
    }
}
