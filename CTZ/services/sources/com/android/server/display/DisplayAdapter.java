package com.android.server.display;

import android.content.Context;
import android.os.Handler;
import android.view.Display;
import com.android.server.display.DisplayManagerService;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

abstract class DisplayAdapter {
    public static final int DISPLAY_DEVICE_EVENT_ADDED = 1;
    public static final int DISPLAY_DEVICE_EVENT_CHANGED = 2;
    public static final int DISPLAY_DEVICE_EVENT_REMOVED = 3;
    private static final AtomicInteger NEXT_DISPLAY_MODE_ID = new AtomicInteger(1);
    private final Context mContext;
    private final Handler mHandler;
    private final Listener mListener;
    private final String mName;
    private final DisplayManagerService.SyncRoot mSyncRoot;

    public interface Listener {
        void onDisplayDeviceEvent(DisplayDevice displayDevice, int i);

        void onTraversalRequested();
    }

    public DisplayAdapter(DisplayManagerService.SyncRoot syncRoot, Context context, Handler handler, Listener listener, String str) {
        this.mSyncRoot = syncRoot;
        this.mContext = context;
        this.mHandler = handler;
        this.mListener = listener;
        this.mName = str;
    }

    public final DisplayManagerService.SyncRoot getSyncRoot() {
        return this.mSyncRoot;
    }

    public final Context getContext() {
        return this.mContext;
    }

    public final Handler getHandler() {
        return this.mHandler;
    }

    public final String getName() {
        return this.mName;
    }

    public void registerLocked() {
    }

    public void dumpLocked(PrintWriter printWriter) {
    }

    protected final void sendDisplayDeviceEventLocked(final DisplayDevice displayDevice, final int i) {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                DisplayAdapter.this.mListener.onDisplayDeviceEvent(displayDevice, i);
            }
        });
    }

    protected final void sendTraversalRequestLocked() {
        this.mHandler.post(new Runnable() {
            @Override
            public void run() {
                DisplayAdapter.this.mListener.onTraversalRequested();
            }
        });
    }

    public static Display.Mode createMode(int i, int i2, float f) {
        return new Display.Mode(NEXT_DISPLAY_MODE_ID.getAndIncrement(), i, i2, f);
    }
}
