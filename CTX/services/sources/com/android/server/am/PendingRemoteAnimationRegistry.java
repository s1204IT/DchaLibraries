package com.android.server.am;

import android.app.ActivityOptions;
import android.os.Handler;
import android.util.ArrayMap;
import android.view.RemoteAnimationAdapter;
import com.android.server.am.PendingRemoteAnimationRegistry;

class PendingRemoteAnimationRegistry {
    private static final long TIMEOUT_MS = 3000;
    private final ArrayMap<String, Entry> mEntries = new ArrayMap<>();
    private final Handler mHandler;
    private final ActivityManagerService mService;

    PendingRemoteAnimationRegistry(ActivityManagerService activityManagerService, Handler handler) {
        this.mService = activityManagerService;
        this.mHandler = handler;
    }

    void addPendingAnimation(String str, RemoteAnimationAdapter remoteAnimationAdapter) {
        this.mEntries.put(str, new Entry(str, remoteAnimationAdapter));
    }

    ActivityOptions overrideOptionsIfNeeded(String str, ActivityOptions activityOptions) {
        Entry entry = this.mEntries.get(str);
        if (entry == null) {
            return activityOptions;
        }
        if (activityOptions == null) {
            activityOptions = ActivityOptions.makeRemoteAnimation(entry.adapter);
        } else {
            activityOptions.setRemoteAnimationAdapter(entry.adapter);
        }
        this.mEntries.remove(str);
        return activityOptions;
    }

    private class Entry {
        final RemoteAnimationAdapter adapter;
        final String packageName;

        Entry(final String str, RemoteAnimationAdapter remoteAnimationAdapter) {
            this.packageName = str;
            this.adapter = remoteAnimationAdapter;
            PendingRemoteAnimationRegistry.this.mHandler.postDelayed(new Runnable() {
                @Override
                public final void run() {
                    PendingRemoteAnimationRegistry.Entry.lambda$new$0(this.f$0, str);
                }
            }, PendingRemoteAnimationRegistry.TIMEOUT_MS);
        }

        public static void lambda$new$0(Entry entry, String str) {
            synchronized (PendingRemoteAnimationRegistry.this.mService) {
                try {
                    ActivityManagerService.boostPriorityForLockedSection();
                    if (((Entry) PendingRemoteAnimationRegistry.this.mEntries.get(str)) == entry) {
                        PendingRemoteAnimationRegistry.this.mEntries.remove(str);
                    }
                } catch (Throwable th) {
                    ActivityManagerService.resetPriorityAfterLockedSection();
                    throw th;
                }
            }
            ActivityManagerService.resetPriorityAfterLockedSection();
        }
    }
}
