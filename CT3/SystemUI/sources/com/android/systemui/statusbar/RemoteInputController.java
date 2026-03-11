package com.android.systemui.statusbar;

import android.util.ArraySet;
import com.android.internal.util.Preconditions;
import com.android.systemui.statusbar.NotificationData;
import com.android.systemui.statusbar.phone.StatusBarWindowManager;
import com.android.systemui.statusbar.policy.HeadsUpManager;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class RemoteInputController {
    private final HeadsUpManager mHeadsUpManager;
    private final ArrayList<WeakReference<NotificationData.Entry>> mOpen = new ArrayList<>();
    private final ArraySet<String> mSpinning = new ArraySet<>();
    private final ArrayList<Callback> mCallbacks = new ArrayList<>(3);

    public RemoteInputController(StatusBarWindowManager sbwm, HeadsUpManager headsUpManager) {
        addCallback(sbwm);
        this.mHeadsUpManager = headsUpManager;
    }

    public void addRemoteInput(NotificationData.Entry entry) {
        Preconditions.checkNotNull(entry);
        boolean found = pruneWeakThenRemoveAndContains(entry, null);
        if (!found) {
            this.mOpen.add(new WeakReference<>(entry));
        }
        apply(entry);
    }

    public void removeRemoteInput(NotificationData.Entry entry) {
        Preconditions.checkNotNull(entry);
        pruneWeakThenRemoveAndContains(null, entry);
        apply(entry);
    }

    public void addSpinning(String key) {
        this.mSpinning.add(key);
    }

    public void removeSpinning(String key) {
        this.mSpinning.remove(key);
    }

    public boolean isSpinning(String key) {
        return this.mSpinning.contains(key);
    }

    private void apply(NotificationData.Entry entry) {
        this.mHeadsUpManager.setRemoteInputActive(entry, isRemoteInputActive(entry));
        boolean remoteInputActive = isRemoteInputActive();
        int N = this.mCallbacks.size();
        for (int i = 0; i < N; i++) {
            this.mCallbacks.get(i).onRemoteInputActive(remoteInputActive);
        }
    }

    public boolean isRemoteInputActive(NotificationData.Entry entry) {
        return pruneWeakThenRemoveAndContains(entry, null);
    }

    public boolean isRemoteInputActive() {
        pruneWeakThenRemoveAndContains(null, null);
        return !this.mOpen.isEmpty();
    }

    private boolean pruneWeakThenRemoveAndContains(NotificationData.Entry contains, NotificationData.Entry remove) {
        boolean found = false;
        for (int i = this.mOpen.size() - 1; i >= 0; i--) {
            NotificationData.Entry item = this.mOpen.get(i).get();
            if (item == null || item == remove) {
                this.mOpen.remove(i);
            } else if (item == contains) {
                found = true;
            }
        }
        return found;
    }

    public void addCallback(Callback callback) {
        Preconditions.checkNotNull(callback);
        this.mCallbacks.add(callback);
    }

    public void remoteInputSent(NotificationData.Entry entry) {
        int N = this.mCallbacks.size();
        for (int i = 0; i < N; i++) {
            this.mCallbacks.get(i).onRemoteInputSent(entry);
        }
    }

    public void closeRemoteInputs() {
        if (this.mOpen.size() == 0) {
            return;
        }
        ArrayList<NotificationData.Entry> list = new ArrayList<>(this.mOpen.size());
        for (int i = this.mOpen.size() - 1; i >= 0; i--) {
            NotificationData.Entry item = this.mOpen.get(i).get();
            if (item != null && item.row != null) {
                list.add(item);
            }
        }
        for (int i2 = list.size() - 1; i2 >= 0; i2--) {
            NotificationData.Entry item2 = list.get(i2);
            if (item2.row != null) {
                item2.row.closeRemoteInput();
            }
        }
    }

    public interface Callback {
        default void onRemoteInputActive(boolean active) {
        }

        default void onRemoteInputSent(NotificationData.Entry entry) {
        }
    }
}
