package com.android.camera.app;

import android.app.ActivityManager;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.res.Configuration;
import com.android.camera.app.MediaSaver;
import com.android.camera.app.MemoryManager;
import com.android.camera.debug.Log;
import com.android.camera.util.GservicesHelper;
import java.util.HashMap;
import java.util.LinkedList;

public class MemoryManagerImpl implements MemoryManager, MediaSaver.QueueListener, ComponentCallbacks2 {
    private static final float MAX_MEM_ALLOWED = 0.7f;
    private static final Log.Tag TAG = new Log.Tag("MemoryManagerImpl");
    private static final int[] sCriticalStates = {80, 15};
    private final LinkedList<MemoryManager.MemoryListener> mListeners = new LinkedList<>();
    private final int mMaxAllowedNativeMemory;
    private final MemoryQuery mMemoryQuery;

    public static MemoryManagerImpl create(Context context, MediaSaver mediaSaver) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        int maxAllowedNativeMemory = getMaxAllowedNativeMemory(context);
        MemoryQuery mMemoryQuery = new MemoryQuery(activityManager);
        MemoryManagerImpl memoryManager = new MemoryManagerImpl(maxAllowedNativeMemory, mMemoryQuery);
        context.registerComponentCallbacks(memoryManager);
        mediaSaver.setQueueListener(memoryManager);
        return memoryManager;
    }

    private MemoryManagerImpl(int maxAllowedNativeMemory, MemoryQuery memoryQuery) {
        this.mMaxAllowedNativeMemory = maxAllowedNativeMemory;
        this.mMemoryQuery = memoryQuery;
        Log.d(TAG, "Max native memory: " + this.mMaxAllowedNativeMemory + " MB");
    }

    @Override
    public void addListener(MemoryManager.MemoryListener listener) {
        synchronized (this.mListeners) {
            if (!this.mListeners.contains(listener)) {
                this.mListeners.add(listener);
            } else {
                Log.w(TAG, "Listener already added.");
            }
        }
    }

    @Override
    public void removeListener(MemoryManager.MemoryListener listener) {
        synchronized (this.mListeners) {
            if (this.mListeners.contains(listener)) {
                this.mListeners.remove(listener);
            } else {
                Log.w(TAG, "Cannot remove listener that was never added.");
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
        notifyLowMemory();
    }

    @Override
    public void onTrimMemory(int level) {
        for (int i = 0; i < sCriticalStates.length; i++) {
            if (level == sCriticalStates[i]) {
                notifyLowMemory();
                return;
            }
        }
    }

    @Override
    public void onQueueStatus(boolean full) {
        notifyCaptureStateUpdate(full ? 1 : 0);
    }

    @Override
    public int getMaxAllowedNativeMemoryAllocation() {
        return this.mMaxAllowedNativeMemory;
    }

    @Override
    public HashMap queryMemory() {
        return this.mMemoryQuery.queryMemory();
    }

    private static int getMaxAllowedNativeMemory(Context context) {
        int maxAllowedOverrideMb = GservicesHelper.getMaxAllowedNativeMemoryMb(context);
        if (maxAllowedOverrideMb > 0) {
            Log.d(TAG, "Max native memory overridden: " + maxAllowedOverrideMb);
            return maxAllowedOverrideMb;
        }
        ActivityManager activityManager = (ActivityManager) context.getSystemService("activity");
        return (int) (Math.max(activityManager.getMemoryClass(), activityManager.getLargeMemoryClass()) * MAX_MEM_ALLOWED);
    }

    private void notifyLowMemory() {
        synchronized (this.mListeners) {
            for (MemoryManager.MemoryListener listener : this.mListeners) {
                listener.onLowMemory();
            }
        }
    }

    private void notifyCaptureStateUpdate(int captureState) {
        synchronized (this.mListeners) {
            for (MemoryManager.MemoryListener listener : this.mListeners) {
                listener.onMemoryStateChanged(captureState);
            }
        }
    }
}
