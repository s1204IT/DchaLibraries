package com.android.server.wm;

import android.util.ArrayMap;
import android.util.Slog;
import java.io.PrintWriter;

class UnknownAppVisibilityController {
    private static final String TAG = "WindowManager";
    private static final int UNKNOWN_STATE_WAITING_RELAYOUT = 2;
    private static final int UNKNOWN_STATE_WAITING_RESUME = 1;
    private static final int UNKNOWN_STATE_WAITING_VISIBILITY_UPDATE = 3;
    private final WindowManagerService mService;
    private final ArrayMap<AppWindowToken, Integer> mUnknownApps = new ArrayMap<>();

    UnknownAppVisibilityController(WindowManagerService windowManagerService) {
        this.mService = windowManagerService;
    }

    boolean allResolved() {
        return this.mUnknownApps.isEmpty();
    }

    void clear() {
        this.mUnknownApps.clear();
    }

    String getDebugMessage() {
        StringBuilder sb = new StringBuilder();
        for (int size = this.mUnknownApps.size() - 1; size >= 0; size--) {
            sb.append("app=");
            sb.append(this.mUnknownApps.keyAt(size));
            sb.append(" state=");
            sb.append(this.mUnknownApps.valueAt(size));
            if (size != 0) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    void appRemovedOrHidden(AppWindowToken appWindowToken) {
        if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d("WindowManager", "App removed or hidden appWindow=" + appWindowToken);
        }
        this.mUnknownApps.remove(appWindowToken);
    }

    void notifyLaunched(AppWindowToken appWindowToken) {
        if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d("WindowManager", "App launched appWindow=" + appWindowToken);
        }
        this.mUnknownApps.put(appWindowToken, 1);
    }

    void notifyAppResumedFinished(AppWindowToken appWindowToken) {
        if (this.mUnknownApps.containsKey(appWindowToken) && this.mUnknownApps.get(appWindowToken).intValue() == 1) {
            if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
                Slog.d("WindowManager", "App resume finished appWindow=" + appWindowToken);
            }
            this.mUnknownApps.put(appWindowToken, 2);
        }
    }

    void notifyRelayouted(AppWindowToken appWindowToken) {
        if (!this.mUnknownApps.containsKey(appWindowToken)) {
            return;
        }
        if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d("WindowManager", "App relayouted appWindow=" + appWindowToken);
        }
        if (this.mUnknownApps.get(appWindowToken).intValue() == 2) {
            this.mUnknownApps.put(appWindowToken, 3);
            this.mService.notifyKeyguardFlagsChanged(new Runnable() {
                @Override
                public final void run() {
                    this.f$0.notifyVisibilitiesUpdated();
                }
            });
        }
    }

    private void notifyVisibilitiesUpdated() {
        if (WindowManagerDebugConfig.DEBUG_UNKNOWN_APP_VISIBILITY) {
            Slog.d("WindowManager", "Visibility updated DONE");
        }
        boolean z = false;
        for (int size = this.mUnknownApps.size() - 1; size >= 0; size--) {
            if (this.mUnknownApps.valueAt(size).intValue() == 3) {
                this.mUnknownApps.removeAt(size);
                z = true;
            }
        }
        if (z) {
            this.mService.mWindowPlacerLocked.performSurfacePlacement();
        }
    }

    void dump(PrintWriter printWriter, String str) {
        if (this.mUnknownApps.isEmpty()) {
            return;
        }
        printWriter.println(str + "Unknown visibilities:");
        for (int size = this.mUnknownApps.size() + (-1); size >= 0; size += -1) {
            printWriter.println(str + "  app=" + this.mUnknownApps.keyAt(size) + " state=" + this.mUnknownApps.valueAt(size));
        }
    }
}
