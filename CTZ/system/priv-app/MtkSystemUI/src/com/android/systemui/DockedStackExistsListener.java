package com.android.systemui;

import android.os.RemoteException;
import android.util.Log;
import android.view.IDockedStackListener;
import android.view.WindowManagerGlobal;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;

/* loaded from: classes.dex */
public class DockedStackExistsListener {
    private static boolean mLastExists;
    private static ArrayList<WeakReference<Consumer<Boolean>>> sCallbacks = new ArrayList<>();

    static {
        try {
            WindowManagerGlobal.getWindowManagerService().registerDockedStackListener(new IDockedStackListener.Stub() { // from class: com.android.systemui.DockedStackExistsListener.1
                public void onDividerVisibilityChanged(boolean z) throws RemoteException {
                }

                public void onDockedStackExistsChanged(boolean z) throws RemoteException {
                    DockedStackExistsListener.onDockedStackExistsChanged(z);
                }

                public void onDockedStackMinimizedChanged(boolean z, long j, boolean z2) throws RemoteException {
                }

                public void onAdjustedForImeChanged(boolean z, long j) throws RemoteException {
                }

                public void onDockSideChanged(int i) throws RemoteException {
                }
            });
        } catch (RemoteException e) {
            Log.e("DockedStackExistsListener", "Failed registering docked stack exists listener", e);
        }
    }

    private static void onDockedStackExistsChanged(final boolean z) {
        mLastExists = z;
        synchronized (sCallbacks) {
            sCallbacks.removeIf(new Predicate() { // from class: com.android.systemui.-$$Lambda$DockedStackExistsListener$fsI9l50cYy8em3Xlw9NfoEH95Z8
                @Override // java.util.function.Predicate
                public final boolean test(Object obj) {
                    return DockedStackExistsListener.lambda$onDockedStackExistsChanged$0(z, (WeakReference) obj);
                }
            });
        }
    }

    static /* synthetic */ boolean lambda$onDockedStackExistsChanged$0(boolean z, WeakReference weakReference) {
        Consumer consumer = (Consumer) weakReference.get();
        if (consumer != null) {
            consumer.accept(Boolean.valueOf(z));
        }
        return consumer == null;
    }

    public static void register(Consumer<Boolean> consumer) {
        consumer.accept(Boolean.valueOf(mLastExists));
        synchronized (sCallbacks) {
            sCallbacks.add(new WeakReference<>(consumer));
        }
    }
}
