package com.android.providers.settings;

import android.os.Bundle;
import android.os.UserManager;
import android.util.MemoryIntArray;
import android.util.Slog;
import android.util.SparseIntArray;
import com.android.internal.annotations.GuardedBy;
import java.io.IOException;

final class GenerationRegistry {

    @GuardedBy("mLock")
    private MemoryIntArray mBackingStore;

    @GuardedBy("mLock")
    private final SparseIntArray mKeyToIndexMap = new SparseIntArray();
    private final Object mLock;

    public GenerationRegistry(Object obj) {
        this.mLock = obj;
    }

    public void incrementGeneration(int i) {
        int keyIndexLocked;
        synchronized (this.mLock) {
            MemoryIntArray backingStoreLocked = getBackingStoreLocked();
            if (backingStoreLocked != null) {
                try {
                    keyIndexLocked = getKeyIndexLocked(i, this.mKeyToIndexMap, backingStoreLocked);
                } catch (IOException e) {
                    Slog.e("GenerationRegistry", "Error updating generation id", e);
                    destroyBackingStore();
                }
                if (keyIndexLocked >= 0) {
                    backingStoreLocked.set(keyIndexLocked, backingStoreLocked.get(keyIndexLocked) + 1);
                }
            }
        }
    }

    public void addGenerationData(Bundle bundle, int i) {
        int keyIndexLocked;
        synchronized (this.mLock) {
            MemoryIntArray backingStoreLocked = getBackingStoreLocked();
            if (backingStoreLocked != null) {
                try {
                    keyIndexLocked = getKeyIndexLocked(i, this.mKeyToIndexMap, backingStoreLocked);
                } catch (IOException e) {
                    Slog.e("GenerationRegistry", "Error adding generation data", e);
                    destroyBackingStore();
                }
                if (keyIndexLocked >= 0) {
                    bundle.putParcelable("_track_generation", backingStoreLocked);
                    bundle.putInt("_generation_index", keyIndexLocked);
                    bundle.putInt("_generation", backingStoreLocked.get(keyIndexLocked));
                }
            }
        }
    }

    public void onUserRemoved(int i) {
        synchronized (this.mLock) {
            MemoryIntArray backingStoreLocked = getBackingStoreLocked();
            if (backingStoreLocked != null && this.mKeyToIndexMap.size() > 0) {
                try {
                    resetSlotForKeyLocked(SettingsProvider.makeKey(2, i), this.mKeyToIndexMap, backingStoreLocked);
                    resetSlotForKeyLocked(SettingsProvider.makeKey(1, i), this.mKeyToIndexMap, backingStoreLocked);
                } catch (IOException e) {
                    Slog.e("GenerationRegistry", "Error cleaning up for user", e);
                    destroyBackingStore();
                }
            }
        }
    }

    private MemoryIntArray getBackingStoreLocked() {
        if (this.mBackingStore == null) {
            try {
                this.mBackingStore = new MemoryIntArray(13 + (2 * UserManager.getMaxSupportedUsers()));
            } catch (IOException e) {
                Slog.e("GenerationRegistry", "Error creating generation tracker", e);
            }
        }
        return this.mBackingStore;
    }

    private void destroyBackingStore() {
        if (this.mBackingStore != null) {
            try {
                this.mBackingStore.close();
            } catch (IOException e) {
                Slog.e("GenerationRegistry", "Cannot close generation memory array", e);
            }
            this.mBackingStore = null;
        }
    }

    private static void resetSlotForKeyLocked(int i, SparseIntArray sparseIntArray, MemoryIntArray memoryIntArray) throws IOException {
        int i2 = sparseIntArray.get(i, -1);
        if (i2 >= 0) {
            sparseIntArray.delete(i);
            memoryIntArray.set(i2, 0);
        }
    }

    private static int getKeyIndexLocked(int i, SparseIntArray sparseIntArray, MemoryIntArray memoryIntArray) throws IOException {
        int iFindNextEmptyIndex = sparseIntArray.get(i, -1);
        if (iFindNextEmptyIndex < 0) {
            iFindNextEmptyIndex = findNextEmptyIndex(memoryIntArray);
            if (iFindNextEmptyIndex >= 0) {
                memoryIntArray.set(iFindNextEmptyIndex, 1);
                sparseIntArray.append(i, iFindNextEmptyIndex);
            } else {
                Slog.e("GenerationRegistry", "Could not allocate generation index");
            }
        }
        return iFindNextEmptyIndex;
    }

    private static int findNextEmptyIndex(MemoryIntArray memoryIntArray) throws IOException {
        int size = memoryIntArray.size();
        for (int i = 0; i < size; i++) {
            if (memoryIntArray.get(i) == 0) {
                return i;
            }
        }
        return -1;
    }
}
