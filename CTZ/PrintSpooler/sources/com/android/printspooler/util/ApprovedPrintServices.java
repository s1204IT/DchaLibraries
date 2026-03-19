package com.android.printspooler.util;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ApprovedPrintServices {
    public static final Object sLock = new Object();
    private final SharedPreferences mPreferences;

    public ApprovedPrintServices(Context context) {
        this.mPreferences = context.getSharedPreferences("PRINT_SPOOLER_APPROVED_SERVICES", 0);
    }

    public Set<String> getApprovedServices() {
        return this.mPreferences.getStringSet("PRINT_SPOOLER_APPROVED_SERVICES", null);
    }

    public boolean isApprovedService(ComponentName componentName) {
        Set<String> approvedServices = getApprovedServices();
        if (approvedServices != null) {
            String strFlattenToShortString = componentName.flattenToShortString();
            Iterator<String> it = approvedServices.iterator();
            while (it.hasNext()) {
                if (it.next().equals(strFlattenToShortString)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public void addApprovedService(ComponentName componentName) {
        ArraySet arraySet;
        synchronized (sLock) {
            Set<String> stringSet = this.mPreferences.getStringSet("PRINT_SPOOLER_APPROVED_SERVICES", null);
            if (stringSet == null) {
                arraySet = new ArraySet(1);
            } else {
                arraySet = new ArraySet(stringSet);
            }
            arraySet.add(componentName.flattenToShortString());
            SharedPreferences.Editor editorEdit = this.mPreferences.edit();
            editorEdit.putStringSet("PRINT_SPOOLER_APPROVED_SERVICES", arraySet);
            editorEdit.apply();
        }
    }

    public void registerChangeListenerLocked(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        this.mPreferences.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    public void unregisterChangeListener(SharedPreferences.OnSharedPreferenceChangeListener onSharedPreferenceChangeListener) {
        this.mPreferences.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    }

    public void pruneApprovedServices(List<ComponentName> list) {
        synchronized (sLock) {
            Set<String> approvedServices = getApprovedServices();
            if (approvedServices == null) {
                return;
            }
            ArraySet arraySet = new ArraySet(approvedServices.size());
            int size = list.size();
            for (int i = 0; i < size; i++) {
                String strFlattenToShortString = list.get(i).flattenToShortString();
                if (approvedServices.contains(strFlattenToShortString)) {
                    arraySet.add(strFlattenToShortString);
                }
            }
            if (approvedServices.size() != arraySet.size()) {
                SharedPreferences.Editor editorEdit = this.mPreferences.edit();
                editorEdit.putStringSet("PRINT_SPOOLER_APPROVED_SERVICES", arraySet);
                editorEdit.apply();
            }
        }
    }
}
