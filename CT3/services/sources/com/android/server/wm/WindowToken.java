package com.android.server.wm;

import android.os.IBinder;
import android.util.Slog;
import java.io.PrintWriter;

class WindowToken {
    AppWindowToken appWindowToken;
    final boolean explicit;
    boolean hasVisible;
    boolean hidden;
    boolean sendingToBottom;
    final WindowManagerService service;
    String stringName;
    final IBinder token;
    boolean waitingToShow;
    final int windowType;
    final WindowList windows = new WindowList();
    boolean paused = false;

    WindowToken(WindowManagerService _service, IBinder _token, int type, boolean _explicit) {
        this.service = _service;
        this.token = _token;
        this.windowType = type;
        this.explicit = _explicit;
    }

    void removeAllWindows() {
        for (int winNdx = this.windows.size() - 1; winNdx >= 0; winNdx--) {
            WindowState win = this.windows.get(winNdx);
            if (WindowManagerDebugConfig.DEBUG_WINDOW_MOVEMENT) {
                Slog.w("WindowManager", "removeAllWindows: removing win=" + win);
            }
            win.mService.removeWindowLocked(win);
        }
        this.windows.clear();
    }

    void dump(PrintWriter pw, String prefix) {
        pw.print(prefix);
        pw.print("windows=");
        pw.println(this.windows);
        pw.print(prefix);
        pw.print("windowType=");
        pw.print(this.windowType);
        pw.print(" hidden=");
        pw.print(this.hidden);
        pw.print(" hasVisible=");
        pw.println(this.hasVisible);
        if (!this.waitingToShow && !this.sendingToBottom) {
            return;
        }
        pw.print(prefix);
        pw.print("waitingToShow=");
        pw.print(this.waitingToShow);
        pw.print(" sendingToBottom=");
        pw.print(this.sendingToBottom);
    }

    public String toString() {
        if (this.stringName == null) {
            this.stringName = "WindowToken{" + Integer.toHexString(System.identityHashCode(this)) + " " + this.token + '}';
        }
        return this.stringName;
    }
}
