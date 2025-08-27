package com.android.systemui.statusbar;

import android.app.Notification;
import android.app.RemoteInput;
import android.content.Context;
import android.os.SystemProperties;
import android.util.ArrayMap;
import android.util.Pair;
import com.android.internal.util.Preconditions;
import com.android.systemui.statusbar.NotificationData;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/* loaded from: classes.dex */
public class RemoteInputController {
    private static final boolean ENABLE_REMOTE_INPUT = SystemProperties.getBoolean("debug.enable_remote_input", true);
    private final Delegate mDelegate;
    private final ArrayList<Pair<WeakReference<NotificationData.Entry>, Object>> mOpen = new ArrayList<>();
    private final ArrayMap<String, Object> mSpinning = new ArrayMap<>();
    private final ArrayList<Callback> mCallbacks = new ArrayList<>(3);

    public interface Delegate {
        void lockScrollTo(NotificationData.Entry entry);

        void requestDisallowLongPressAndDismiss();

        void setRemoteInputActive(NotificationData.Entry entry, boolean z);
    }

    public RemoteInputController(Delegate delegate) {
        this.mDelegate = delegate;
    }

    public static void processForRemoteInput(Notification notification, Context context) {
        RemoteInput[] remoteInputs;
        if (ENABLE_REMOTE_INPUT && notification.extras != null && notification.extras.containsKey("android.wearable.EXTENSIONS")) {
            if (notification.actions == null || notification.actions.length == 0) {
                List<Notification.Action> actions = new Notification.WearableExtender(notification).getActions();
                int size = actions.size();
                Notification.Action action = null;
                for (int i = 0; i < size; i++) {
                    Notification.Action action2 = actions.get(i);
                    if (action2 != null && (remoteInputs = action2.getRemoteInputs()) != null) {
                        int length = remoteInputs.length;
                        int i2 = 0;
                        while (true) {
                            if (i2 >= length) {
                                break;
                            }
                            if (!remoteInputs[i2].getAllowFreeFormInput()) {
                                i2++;
                            } else {
                                action = action2;
                                break;
                            }
                        }
                        if (action != null) {
                            break;
                        }
                    }
                }
                if (action != null) {
                    Notification.Builder builderRecoverBuilder = Notification.Builder.recoverBuilder(context, notification);
                    builderRecoverBuilder.setActions(action);
                    builderRecoverBuilder.build();
                }
            }
        }
    }

    public void addRemoteInput(NotificationData.Entry entry, Object obj) {
        Preconditions.checkNotNull(entry);
        Preconditions.checkNotNull(obj);
        if (!pruneWeakThenRemoveAndContains(entry, null, obj)) {
            this.mOpen.add(new Pair<>(new WeakReference(entry), obj));
        }
        apply(entry);
    }

    public void removeRemoteInput(NotificationData.Entry entry, Object obj) {
        Preconditions.checkNotNull(entry);
        pruneWeakThenRemoveAndContains(null, entry, obj);
        apply(entry);
    }

    public void addSpinning(String str, Object obj) {
        Preconditions.checkNotNull(str);
        Preconditions.checkNotNull(obj);
        this.mSpinning.put(str, obj);
    }

    public void removeSpinning(String str, Object obj) {
        Preconditions.checkNotNull(str);
        if (obj == null || this.mSpinning.get(str) == obj) {
            this.mSpinning.remove(str);
        }
    }

    public boolean isSpinning(String str) {
        return this.mSpinning.containsKey(str);
    }

    public boolean isSpinning(String str, Object obj) {
        return this.mSpinning.get(str) == obj;
    }

    private void apply(NotificationData.Entry entry) {
        this.mDelegate.setRemoteInputActive(entry, isRemoteInputActive(entry));
        boolean zIsRemoteInputActive = isRemoteInputActive();
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            this.mCallbacks.get(i).onRemoteInputActive(zIsRemoteInputActive);
        }
    }

    public boolean isRemoteInputActive(NotificationData.Entry entry) {
        return pruneWeakThenRemoveAndContains(entry, null, null);
    }

    public boolean isRemoteInputActive() {
        pruneWeakThenRemoveAndContains(null, null, null);
        return !this.mOpen.isEmpty();
    }

    private boolean pruneWeakThenRemoveAndContains(NotificationData.Entry entry, NotificationData.Entry entry2, Object obj) {
        boolean z = false;
        for (int size = this.mOpen.size() - 1; size >= 0; size--) {
            NotificationData.Entry entry3 = (NotificationData.Entry) ((WeakReference) this.mOpen.get(size).first).get();
            Object obj2 = this.mOpen.get(size).second;
            boolean z2 = obj == null || obj2 == obj;
            if (entry3 == null || (entry3 == entry2 && z2)) {
                this.mOpen.remove(size);
            } else if (entry3 == entry) {
                if (obj == null || obj == obj2) {
                    z = true;
                } else {
                    this.mOpen.remove(size);
                }
            }
        }
        return z;
    }

    public void addCallback(Callback callback) {
        Preconditions.checkNotNull(callback);
        this.mCallbacks.add(callback);
    }

    public void remoteInputSent(NotificationData.Entry entry) {
        int size = this.mCallbacks.size();
        for (int i = 0; i < size; i++) {
            this.mCallbacks.get(i).onRemoteInputSent(entry);
        }
    }

    public void closeRemoteInputs() {
        if (this.mOpen.size() == 0) {
            return;
        }
        ArrayList arrayList = new ArrayList(this.mOpen.size());
        for (int size = this.mOpen.size() - 1; size >= 0; size--) {
            NotificationData.Entry entry = (NotificationData.Entry) ((WeakReference) this.mOpen.get(size).first).get();
            if (entry != null && entry.row != null) {
                arrayList.add(entry);
            }
        }
        for (int size2 = arrayList.size() - 1; size2 >= 0; size2--) {
            NotificationData.Entry entry2 = (NotificationData.Entry) arrayList.get(size2);
            if (entry2.row != null) {
                entry2.row.closeRemoteInput();
            }
        }
    }

    public void requestDisallowLongPressAndDismiss() {
        this.mDelegate.requestDisallowLongPressAndDismiss();
    }

    public void lockScrollTo(NotificationData.Entry entry) {
        this.mDelegate.lockScrollTo(entry);
    }

    public interface Callback {
        default void onRemoteInputActive(boolean z) {
        }

        default void onRemoteInputSent(NotificationData.Entry entry) {
        }
    }
}
